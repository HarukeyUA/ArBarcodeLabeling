package com.rainy.barcodelabeling

import android.app.Activity
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SparseIntArray
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity

// https://github.com/googlesamples/mlkit/blob/cdaf07eb180610ef816a46b5f6365c45af627a74/android/android-snippets/app/src/main/java/com/google/example/mlkit/kotlin/MLKitVisionImage.kt#L93-L118
object RotationUtil {
    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @Throws(CameraAccessException::class)
    fun getRotationCompensation(
        cameraId: String,
        activity: Activity,
        isFrontFacing: Boolean = false
    ): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // Get the device's sensor orientation.

        val cameraManager =
            activity.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360
        }
        return rotationCompensation
    }
}
