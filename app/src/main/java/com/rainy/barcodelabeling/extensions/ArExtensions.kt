package com.rainy.barcodelabeling.extensions

import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException

fun Frame.tryAcquireCameraImage() = try {
    acquireCameraImage()
} catch (e: NotYetAvailableException) {
    null
} catch (e: Throwable) {
    throw e
}
