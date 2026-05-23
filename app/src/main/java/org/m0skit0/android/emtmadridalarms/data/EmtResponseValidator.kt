package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import java.io.IOException

fun interface EmtResponseValidator : (String?, String?) -> Unit

private const val TAG = "EmtResponseValidator"

internal fun requireEmtSuccess(): EmtResponseValidator = EmtResponseValidator { code, description ->
    if (code != null && code != "00") {
        Log.w(TAG, "EMT request returned code=$code description=$description")
        throw IOException(description ?: "EMT request failed with code $code")
    }
}
