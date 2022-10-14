package com.rainy.barcodelabeling

import android.content.Context
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ModelProvider(private val context: Context) {

    private val transparentGreen by lazy { Color(0f, 1f, 0f, 0.5f) }
    private val overlaySize by lazy { Vector3(0.05f, 0.03f, 0f) }

    suspend fun constructOverlayModel() = suspendCancellableCoroutine<Renderable> { continuation ->
        MaterialFactory.makeTransparentWithColor(
            context,
            transparentGreen
        ).thenAccept { material ->
            continuation.resume(
                ShapeFactory.makeCube(
                    overlaySize, Vector3(),
                    material
                )
            )
        }
    }

}
