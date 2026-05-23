package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

private const val TAG = "EmtAuthTokenProvider"

/**
 * Data class representing the state for authentication.
 */
data class EmtAuthTokenState(
    val token: String? = null,
    val expiresAtMillis: Long = 0L,
) {
    /**
     * This mutex is defined in the body, not the constructor, for a critical reason:
     * properties in the body are NOT part of the `copy()` method. This ensures that
     * a single, stable mutex instance is shared across all copies of the AuthState,
     * preserving the integrity of the lock.
     */
    val mutex = Mutex()
}

fun interface EmtAuthTokenProvider : suspend () -> String

internal fun provideToken(
    api: EmtApi,
    credentials: EmtCredentials,
    globalState: GlobalStateHolder,
): EmtAuthTokenProvider = EmtAuthTokenProvider {
    globalState.state.emtAuthToken.mutex.withLock {
        cachedToken(globalState) ?: fetchAndStoreToken(api, credentials, globalState)
    }
}

private fun cachedToken(globalState: GlobalStateHolder): String? {
    val authState = globalState.state.emtAuthToken
    val cached = authState.token
    if (cached.isNullOrBlank() || System.currentTimeMillis() >= authState.expiresAtMillis - 60_000L) return null
    Log.d(
        TAG,
        "Using cached EMT token expiresInMs=${authState.expiresAtMillis - System.currentTimeMillis()}"
    )
    return cached
}

private suspend fun fetchAndStoreToken(
    api: EmtApi,
    credentials: EmtCredentials,
    globalState: GlobalStateHolder,
): String {
    requireCredentials(credentials)
    val (token, expiresAtMillis) = fetchToken(api, credentials)
    storeToken(globalState, token, expiresAtMillis)
    return token
}

private fun requireCredentials(credentials: EmtCredentials) {
    if (!credentials.hasUsableCredentials) {
        Log.w(TAG, "Missing EMT credentials")
        throw IOException("Missing EMT credentials. Add EMT_EMAIL and EMT_PASSWORD, or EMT_CLIENT_ID and EMT_PASS_KEY, to local.properties.")
    }
}

private suspend fun fetchToken(api: EmtApi, credentials: EmtCredentials): Pair<String, Long> {
    Log.d(
        TAG,
        "Requesting new EMT token authMode=${if (credentials.passKey.isNotBlank()) "passKey" else "email"}"
    )
    val response = api.login(
        email = credentials.email.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
        password = credentials.password.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
        clientId = credentials.clientId.takeIf { it.isNotBlank() },
        passKey = credentials.passKey.takeIf { it.isNotBlank() },
    )
    val tokenData = response.data.firstOrNull()
    val newToken = tokenData?.accessToken
    if (newToken.isNullOrBlank()) {
        Log.w(
            TAG,
            "Login returned no token code=${response.code} description=${response.description}"
        )
        throw IOException(response.description ?: "EMT login failed")
    }
    val expiresAtMillis =
        System.currentTimeMillis() + ((tokenData.tokenSecExpiration ?: 900) * 1_000L)
    Log.d(
        TAG,
        "Fetched EMT token code=${response.code} expiresInSec=${tokenData.tokenSecExpiration ?: 900}"
    )
    return newToken to expiresAtMillis
}

private fun storeToken(globalState: GlobalStateHolder, token: String, expiresAtMillis: Long) {
    globalState.update { appState ->
        appState.copy(
            emtAuthToken = appState.emtAuthToken.copy(
                token = token,
                expiresAtMillis = expiresAtMillis
            )
        )
    }
    Log.d(TAG, "Stored EMT token expiresAtMillis=$expiresAtMillis")
}
