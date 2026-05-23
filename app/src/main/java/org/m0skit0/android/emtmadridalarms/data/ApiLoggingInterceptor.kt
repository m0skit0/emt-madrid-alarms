package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class ApiLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = System.nanoTime()
        val url = request.url
        val safeUrl = buildString {
            append(url.scheme)
            append("://")
            append(url.host)
            append(url.encodedPath)
            if (url.encodedQuery != null) append("?").append(url.encodedQuery)
        }

        Log.d(Tag, "--> ${request.method} $safeUrl")
        return try {
            val response = chain.proceed(request)
            val elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000
            Log.d(Tag, "<-- ${request.method} $safeUrl ${response.code} (${elapsedMillis}ms)")
            response
        } catch (error: IOException) {
            val elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000
            Log.w(
                Tag,
                "<-- ${request.method} $safeUrl FAILED (${elapsedMillis}ms): ${error.message}"
            )
            throw error
        }
    }

    private companion object {
        const val Tag = "EmtApi"
    }
}
