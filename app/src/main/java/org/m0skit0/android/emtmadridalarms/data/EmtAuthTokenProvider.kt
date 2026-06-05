package org.m0skit0.android.emtmadridalarms.data

import timber.log.Timber
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder
import org.m0skit0.android.emtmadridalarms.utils.orDefault

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
    loginFailedMessage: String = "EMT login failed",
): EmtAuthTokenProvider = EmtAuthTokenProvider {
    globalState.state.emtAuthToken.mutex.withLock {
        cachedToken(globalState).orDefault { fetchAndStoreToken(api, credentials, globalState, loginFailedMessage) }
    }
}

private fun cachedToken(globalState: GlobalStateHolder): String? {
    val authState = globalState.state.emtAuthToken
    val cached = authState.token
    if (cached.isNullOrBlank() || System.currentTimeMillis() >= authState.expiresAtMillis - 60_000L) return null
    Timber.d("Using cached EMT token expiresInMs=${authState.expiresAtMillis - System.currentTimeMillis()}")
    return cached
}

private suspend fun fetchAndStoreToken(
    api: EmtApi,
    credentials: EmtCredentials,
    globalState: GlobalStateHolder,
    loginFailedMessage: String,
): String {
    val (token, expiresAtMillis) = fetchToken(api, credentials, loginFailedMessage)
    storeToken(globalState, token, expiresAtMillis)
    return token
}

private suspend fun fetchToken(
    api: EmtApi,
    credentials: EmtCredentials,
    loginFailedMessage: String
): Pair<String, Long> {
    Timber.d("Requesting new EMT token authMode=${if (credentials.passKey.isNotBlank()) "passKey" else "email"}")
    val response = api.login(
        email = credentials.email.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
        password = credentials.password.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
        clientId = credentials.clientId.takeIf { it.isNotBlank() },
        passKey = credentials.passKey.takeIf { it.isNotBlank() },
    )
    val tokenData = response.data.firstOrNull()
    val newToken = tokenData?.accessToken
    if (newToken.isNullOrBlank()) {
        Timber.w("Login returned no token code=${response.code} description=${response.description}")
        throw IOException(response.description.orDefault { loginFailedMessage })
    }
    val expiresAtMillis = System.currentTimeMillis() + (tokenData.tokenSecExpiration.orDefault { 900 } * 1_000L)
    Timber.d("Fetched EMT token code=${response.code} expiresInSec=${tokenData.tokenSecExpiration.orDefault { 900 }}")
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
    Timber.d("Stored EMT token expiresAtMillis=$expiresAtMillis")
}
