package org.m0skit0.android.emtmadridalarms.data

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.m0skit0.android.emtmadridalarms.BuildConfig
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.domain.BusArrival
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop
import org.m0skit0.android.emtmadridalarms.domain.normalizeLine
import org.m0skit0.android.emtmadridalarms.domain.linesMatch

class EmtRepository(
    private val api: EmtApi = EmtNetwork.api,
    private val credentials: EmtCredentials = EmtCredentials.fromBuildConfig(),
) {
    private val loginMutex = Mutex()
    private var accessToken: String? = null
    private var tokenExpiresAtMillis: Long = 0L

    suspend fun lines(): List<BusLine> {
        val response = api.lines(token(), todayDateRef())
        if (response.code != null && response.code != "00") {
            throw IOException(response.description ?: "EMT lines request failed with code ${response.code}")
        }

        return response.data
            .mapNotNull { dto ->
                val label = normalizeLine(dto.label.ifBlank { dto.line })
                val id = label.ifBlank { normalizeLine(dto.line) }
                if (id.isBlank() || label.isBlank()) return@mapNotNull null
                BusLine(id = id, label = label, nameA = dto.nameA, nameB = dto.nameB)
            }
            .distinctBy { normalizeLine(it.label) }
            .sortedWith(compareBy<BusLine> { !it.label.all(Char::isDigit) }.thenBy { it.label.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.label })
    }

    suspend fun stopsForLine(line: BusLine): List<BusStop> {
        val accessToken = token()
        return listOf(1, 2)
            .flatMap { direction ->
                val response = api.lineStops(accessToken, line.id, direction)
                if (response.code != null && response.code != "00") {
                    throw IOException(response.description ?: "EMT stops request failed with code ${response.code}")
                }
                response.data.flatMap { it.stops }
            }
            .mapNotNull { dto ->
                if (dto.stop.isBlank()) return@mapNotNull null
                BusStop(id = dto.stop, name = dto.name, address = dto.postalAddress)
            }
            .distinctBy { it.id }
            .sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
    }

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
        if (newToken.isNullOrBlank()) {
            throw IOException(response.description ?: "EMT login failed")
        }

        accessToken = newToken
        tokenExpiresAtMillis = System.currentTimeMillis() + ((tokenData.tokenSecExpiration ?: 900) * 1_000L)
        newToken
    }

    private fun todayDateRef(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
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
