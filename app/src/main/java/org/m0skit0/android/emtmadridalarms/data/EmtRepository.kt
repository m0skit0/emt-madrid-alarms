package org.m0skit0.android.emtmadridalarms.data

import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.m0skit0.android.emtmadridalarms.BuildConfig
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.linesMatch

class EmtRepository(
    private val api: EmtApi = EmtNetwork.api,
    private val credentials: EmtCredentials = EmtCredentials.fromBuildConfig(),
) {
    private val loginMutex = Mutex()
    private var accessToken: String? = null
    private var tokenExpiresAtMillis: Long = 0L

    suspend fun arrivalsFor(request: BusAlarmRequest): List<BusArrival> {
        val token = token()
        val response = api.arrivals(
            accessToken = token,
            stopId = request.stopId,
            lineArrive = request.line.trim(),
            body = ArrivalsRequestBody(),
        )

        if (response.code != null && response.code != "00") {
            throw IOException(response.description ?: "EMT arrivals request failed with code ${response.code}")
        }

        return response.data
            .flatMap { it.arrivals }
            .map {
                BusArrival(
                    line = it.line,
                    stopId = it.stop,
                    destination = it.destination,
                    estimateSeconds = it.estimateSeconds,
                    distanceMeters = it.distanceMeters,
                )
            }
            .filter { linesMatch(request.line, it.line) }
            .sortedBy { it.estimateSeconds }
    }

    private suspend fun token(): String = loginMutex.withLock {
        val cached = accessToken
        if (!cached.isNullOrBlank() && System.currentTimeMillis() < tokenExpiresAtMillis - 60_000L) {
            return@withLock cached
        }

        if (!credentials.hasUsableCredentials) {
            throw IOException("Missing EMT credentials. Add EMT_EMAIL and EMT_PASSWORD, or EMT_CLIENT_ID and EMT_PASS_KEY, to local.properties.")
        }

        val response = api.login(
            email = credentials.email.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
            password = credentials.password.takeIf { it.isNotBlank() && credentials.passKey.isBlank() },
            clientId = credentials.clientId.takeIf { it.isNotBlank() },
            passKey = credentials.passKey.takeIf { it.isNotBlank() },
        )

        val tokenData = response.data.firstOrNull()
        val newToken = tokenData?.accessToken
        if (response.code != "00" || newToken.isNullOrBlank()) {
            throw IOException(response.description ?: "EMT login failed")
        }

        accessToken = newToken
        tokenExpiresAtMillis = System.currentTimeMillis() + ((tokenData.tokenSecExpiration ?: 900) * 1_000L)
        newToken
    }
}

data class EmtCredentials(
    val email: String,
    val password: String,
    val clientId: String,
    val passKey: String,
) {
    val hasUsableCredentials: Boolean =
        (email.isNotBlank() && password.isNotBlank()) || (clientId.isNotBlank() && passKey.isNotBlank())

    companion object {
        fun fromBuildConfig(): EmtCredentials = EmtCredentials(
            email = BuildConfig.EMT_EMAIL,
            password = BuildConfig.EMT_PASSWORD,
            clientId = BuildConfig.EMT_CLIENT_ID,
            passKey = BuildConfig.EMT_PASS_KEY,
        )
    }
}
