package org.m0skit0.android.emtmadridalarms.data

import timber.log.Timber
import java.io.IOException

fun interface EmtResponseValidator : (String?, String?) -> Unit

private const val TAG = "EmtResponseValidator"

internal fun requireEmtSuccess(): EmtResponseValidator = EmtResponseValidator { code, description ->
    if (code != null && code != "00") {
        Timber.w("EMT request returned code=$code description=$description")
        throw IOException(description ?: "EMT request failed with code $code")
    }
}
