package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.normalizeLine

fun interface LinesProvider : suspend () -> List<BusLine>

private const val TAG = "EmtLineService"

internal suspend fun linesForToday(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    dateProvider: EmtDateProvider,
    validator: EmtResponseValidator,
): List<BusLine> {
    val dateRef = dateProvider()
    Log.d(TAG, "Loading EMT lines for dateRef=$dateRef")
    val response = api.lines(authTokenProvider.token(), dateRef)
    validator(response.code, response.description)

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
