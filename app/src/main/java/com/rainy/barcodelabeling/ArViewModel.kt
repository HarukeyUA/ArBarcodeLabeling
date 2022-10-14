package com.rainy.barcodelabeling

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Renderable
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rainy.barcodelabeling.extensions.await
import com.rainy.barcodelabeling.extensions.tryAcquireCameraImage
import com.rainy.barcodelabeling.arbarcodeassignment.model.Coors2d
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ArViewModel(
    private val modelProvider: ModelProvider
) : ViewModel() {

    private val scannedBarcodes = mutableSetOf<String>()
    private var scanJob: Job? = null

    private var lastScanMillis = 0L

    private val barcodeAnalyzer by lazy { BarcodeScanning.getClient() }

    private val messageEvent = Channel<String>(Channel.UNLIMITED)
    val messageEventFlow = messageEvent.receiveAsFlow()

    private val placeOverlayEvent = Channel<Node>(Channel.UNLIMITED)
    val placeOverlayEventFlow = placeOverlayEvent.receiveAsFlow()

    private var overlayRenderable: Renderable? = null
    private val overlayNodeOffset by lazy { Vector3(-0.02f, -0.02f, 0f) }
    private val overlayNodeHorizontalRotation by lazy { Quaternion(Vector3(0f, 1f, 0f), 180f) }

    init {
        viewModelScope.launch {
            overlayRenderable = modelProvider.constructOverlayModel()
        }
    }

    fun onNewFrame(session: Session, rotation: Int) {
        if (System.currentTimeMillis() - lastScanMillis > AR_FRAME_SCAN_TIMEOUT) {
            lastScanMillis = System.currentTimeMillis()
            performScan(session.update(), rotation)
        }
    }

    private fun performScan(arFrame: Frame, rotation: Int) {
        if (scanJob != null && scanJob?.isActive == true)
            return
        scanJob = viewModelScope.launch {
            arFrame.tryAcquireCameraImage()?.use { cameraImage ->
                val inputImage = InputImage.fromMediaImage(cameraImage, rotation)
                val barcodeResult = barcodeAnalyzer.process(inputImage).await()
                val centerScreenBarcode =
                    findBarcodeInScanArea(barcodeResult, cameraImage.height, cameraImage.width)
                performAnchoringIfNotPresent(centerScreenBarcode, arFrame)
            }

        }

    }

    private fun performAnchoringIfNotPresent(
        centerScreenBarcode: Barcode?,
        arFrame: Frame
    ) {
        if (centerScreenBarcode != null && !scannedBarcodes.contains(centerScreenBarcode.rawValue)) {
            val barcodeCoors = getBarcodeArCoors(centerScreenBarcode, arFrame)
            val hit = arFrame.hitTest(barcodeCoors.x, barcodeCoors.y).firstOrNull()
            if (hit != null) {
                overlayRenderable?.also { renderable ->
                    scannedBarcodes.add(centerScreenBarcode.rawValue ?: "")
                    val anchor = hit.trackable.createAnchor(hit.hitPose)
                    placeOverlayEvent.trySend(
                        constructNode(
                            anchor,
                            renderable
                        )
                    )
                    messageEvent.trySend(centerScreenBarcode.rawValue ?: "")
                }

            }
        }
    }

    private fun constructNode(
        anchor: Anchor,
        overlayRenderable: Renderable
    ): RotationlessAnchorNode {
        return RotationlessAnchorNode(anchor).apply {
            localRotation = overlayNodeHorizontalRotation
            addChild(Node()).apply {
                localRotation = overlayNodeHorizontalRotation
                localPosition = overlayNodeOffset
                addChild(Node()).apply {
                    renderable = overlayRenderable
                    renderable?.isShadowCaster = false
                    renderable?.isShadowReceiver = false
                }
            }

        }
    }

    private fun getBarcodeArCoors(centerScreenBarcode: Barcode, arFrame: Frame): Coors2d {
        val convertFloatsInput = FloatArray(2)
        convertFloatsInput[0] = centerScreenBarcode.boundingBox?.centerX()?.toFloat() ?: 0f
        convertFloatsInput[1] = centerScreenBarcode.boundingBox?.centerY()?.toFloat() ?: 0f
        val convertFloatsOutput = FloatArray(2)
        arFrame.transformCoordinates2d(
            Coordinates2d.IMAGE_PIXELS,
            convertFloatsInput,
            Coordinates2d.VIEW,
            convertFloatsOutput
        )
        return Coors2d(convertFloatsOutput[0], convertFloatsOutput[1])
    }

    // Find barcode at the center of input image
    private fun findBarcodeInScanArea(
        barcodes: List<Barcode>,
        inputHeight: Int,
        inputWidth: Int
    ): Barcode? {
        return barcodes.find { barcode ->
            barcode.boundingBox?.let { barcodeRect ->
                val arFrameRect = Rect(
                    0,
                    (inputWidth * WIDTH_SCAN_AREA_START).toInt(),
                    inputHeight,
                    (inputWidth * WIDTH_SCAN_AREA_END).toInt()
                )
                arFrameRect.contains(barcodeRect)
                true
            } ?: false
        }
    }

    companion object {
        private const val WIDTH_SCAN_AREA_START = 0.35f
        private const val WIDTH_SCAN_AREA_END = 0.65f
        private const val AR_FRAME_SCAN_TIMEOUT = 300L
    }
}
