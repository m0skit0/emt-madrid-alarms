package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import java.io.IOException

fun requireEmtSuccess(code: String?, description: String?, requestName: String, tag: String) {
    if (code != null && code != "00") {
        Log.w(tag, "$requestName request returned code=$code description=$description")
        throw IOException(description ?: "EMT $requestName request failed with code $code")
    }
}
