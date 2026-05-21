package org.m0skit0.android.emtmadridalarms.data

import android.util.Log
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.normalizeLine

class EmtLineService(
    private val api: EmtApi,
    private val authTokenProvider: EmtAuthTokenProvider,
    private val dateProvider: EmtDateProvider,
) {
    suspend fun lines(): List<BusLine> {
        val dateRef = dateProvider.todayDateRef()
        Log.d(TAG, "Loading EMT lines for dateRef=$dateRef")
        val response = api.lines(authTokenProvider.token(), dateRef)
        requireEmtSuccess(response.code, response.description, "lines", TAG)

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

    private companion object {
        const val TAG = "EmtLineService"
    }
}
