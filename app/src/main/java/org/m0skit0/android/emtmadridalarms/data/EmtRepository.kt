package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
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
        val dateRef = todayDateRef()
        Log.d(TAG, "Loading EMT lines for dateRef=$dateRef")
        val response = api.lines(token(), dateRef)
        if (response.code != null && response.code != "00") {
            Log.w(TAG, "Lines request returned code=${response.code} description=${response.description}")
            throw IOException(response.description ?: "EMT lines request failed with code ${response.code}")
        }

        val lines = response.data
            .mapNotNull { dto ->
                val label = normalizeLine(dto.label.ifBlank { dto.line })
                val id = label.ifBlank { normalizeLine(dto.line) }
                if (id.isBlank() || label.isBlank()) return@mapNotNull null
                BusLine(id = id, label = label, nameA = dto.nameA, nameB = dto.nameB)
            }
            .distinctBy { normalizeLine(it.label) }
            .sortedWith(compareBy<BusLine> { !it.label.all(Char::isDigit) }.thenBy { it.label.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.label })
        Log.d(TAG, "Loaded EMT lines raw=${response.data.size} mapped=${lines.size}")
        return lines
    }

    suspend fun stopsForLine(line: BusLine): List<BusStop> {
        val accessToken = token()
        Log.d(TAG, "Loading stops for line=${line.label} id=${line.id}")
        val stops = listOf(1, 2)
            .flatMap { direction ->
                Log.d(TAG, "Loading stops for line=${line.label} direction=$direction")
                val response = api.lineStops(accessToken, line.id, direction)
                if (response.code != null && response.code != "00") {
                    Log.w(TAG, "Stops request returned code=${response.code} description=${response.description}")
                    throw IOException(response.description ?: "EMT stops request failed with code ${response.code}")
                }
                val directionStops = response.data.flatMap { it.stops }
                Log.d(TAG, "Loaded stops direction=$direction raw=${directionStops.size}")
                directionStops
            }
            .mapNotNull { dto ->
                if (dto.stop.isBlank()) return@mapNotNull null
                BusStop(id = dto.stop, name = dto.name, address = dto.postalAddress)
            }
            .distinctBy { it.id }
            .sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
        Log.d(TAG, "Loaded stops for line=${line.label} mapped=${stops.size}")
        return stops
    }

    suspend fun arrivalsFor(request: BusAlarmRequest): List<BusArrival> {
        val token = token()
        val dateRef = todayDateRef()
        Log.d(TAG, "Loading arrivals line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes} dateRef=$dateRef")
        val response = api.arrivals(
            accessToken = token,
            stopId = request.stopId,
            lineArrive = request.line.trim(),
            body = ArrivalsRequestBody(incidencesDate = dateRef),
        )

        if (response.code != null && response.code != "00") {
            Log.w(TAG, "Arrivals request returned code=${response.code} description=${response.description}")
            throw IOException(response.description ?: "EMT arrivals request failed with code ${response.code}")
        }

        val rawArrivals = response.data.flatMap { it.arrivals }
        val arrivals = rawArrivals
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
        Log.d(
            TAG,
            "Loaded arrivals raw=${rawArrivals.size} matching=${arrivals.size} estimates=${arrivals.take(4).joinToString { "${it.line}:${it.estimateSeconds}s" }}",
        )
        return arrivals
    }

    private suspend fun token(): String = loginMutex.withLock {
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

    private fun todayDateRef(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private companion object {
        const val TAG = "EmtRepository"
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
