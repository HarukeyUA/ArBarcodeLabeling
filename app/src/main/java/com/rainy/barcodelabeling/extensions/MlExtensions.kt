package com.rainy.barcodelabeling.extensions

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val e = it.exception
            if (e == null) {
                if (it.isCanceled) cont.cancel() else cont.resume(it.result as T)
            } else {
                cont.resumeWithException(e)
            }
        }
    }
}
