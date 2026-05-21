package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EmtAuthTokenProvider(
    private val api: EmtApi,
    private val credentials: EmtCredentials,
) {
    private val loginMutex = Mutex()
    private var accessToken: String? = null
    private var tokenExpiresAtMillis: Long = 0L

    suspend fun token(): String = loginMutex.withLock {
        val cached = accessToken
        if (!cached.isNullOrBlank() && System.currentTimeMillis() < tokenExpiresAtMillis - 60_000L) {
            Log.d(TAG, "Using cached EMT token expiresInMs=${tokenExpiresAtMillis - System.currentTimeMillis()}")
            return@withLock cached
        }

        if (!credentials.hasUsableCredentials) {
            Log.w(TAG, "Missing EMT credentials")
            throw IOException("Missing EMT credentials. Add EMT_EMAIL and EMT_PASSWORD, or EMT_CLIENT_ID and EMT_PASS_KEY, to local.properties.")
        }

        Log.d(TAG, "Requesting new EMT token authMode=${if (credentials.passKey.isNotBlank()) "passKey" else "email"}")
        val response = api.login(
            email = credentials.email.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
            password = credentials.password.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
            clientId = credentials.clientId.takeIf { it.isNotBlank() },
            passKey = credentials.passKey.takeIf { it.isNotBlank() },
        )

        val tokenData = response.data.firstOrNull()
        val newToken = tokenData?.accessToken
        if (newToken.isNullOrBlank()) {
            Log.w(TAG, "Login returned no token code=${response.code} description=${response.description}")
            throw IOException(response.description ?: "EMT login failed")
        }

        accessToken = newToken
        tokenExpiresAtMillis = System.currentTimeMillis() + ((tokenData.tokenSecExpiration ?: 900) * 1_000L)
        Log.d(TAG, "Stored EMT token code=${response.code} expiresInSec=${tokenData.tokenSecExpiration ?: 900}")
        newToken
    }

    private companion object {
        const val TAG = "EmtAuthTokenProvider"
    }
}
