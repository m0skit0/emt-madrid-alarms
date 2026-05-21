package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "EmtAuthTokenProvider"

data class EmtAuthTokenState(
    val mutex: Mutex = Mutex(),
    var accessToken: String? = null,
    var tokenExpiresAtMillis: Long = 0L,
)

fun interface EmtAuthTokenProvider : suspend () -> String

internal suspend fun provideToken(
    api: EmtApi,
    credentials: EmtCredentials,
    state: EmtAuthTokenState,
): String = state.mutex.withLock {
    cachedToken(state) ?: fetchAndStoreToken(api, credentials, state)
}

private fun cachedToken(state: EmtAuthTokenState): String? {
    val cached = state.accessToken
    if (cached.isNullOrBlank() || System.currentTimeMillis() >= state.tokenExpiresAtMillis - 60_000L) return null
    Log.d(
        TAG,
        "Using cached EMT token expiresInMs=${state.tokenExpiresAtMillis - System.currentTimeMillis()}"
    )
    return cached
}

private suspend fun fetchAndStoreToken(
    api: EmtApi,
    credentials: EmtCredentials,
    state: EmtAuthTokenState,
): String {
    requireCredentials(credentials)
    val (token, expiresAtMillis) = fetchToken(api, credentials)
    storeToken(state, token, expiresAtMillis)
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

private fun storeToken(state: EmtAuthTokenState, token: String, expiresAtMillis: Long) {
    state.accessToken = token
    state.tokenExpiresAtMillis = expiresAtMillis
    Log.d(TAG, "Stored EMT token expiresAtMillis=$expiresAtMillis")
}
