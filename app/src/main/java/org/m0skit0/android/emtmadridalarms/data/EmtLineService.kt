package org.m0skit0.android.emtmadridalarms.data

import timber.log.Timber
import org.m0skit0.android.emtmadridalarms.domain.BusLine

fun interface LinesProvider : suspend () -> List<BusLine>

private const val TAG = "EmtLineService"

internal fun linesForToday(
    api: EmtApi,
    authTokenProvider: EmtAuthTokenProvider,
    dateProvider: EmtDateProvider,
    validator: EmtResponseValidator,
): LinesProvider = LinesProvider {
    val dateRef = dateProvider()
    Timber.d("Loading EMT lines for dateRef=$dateRef")
    val response = api.lines(authTokenProvider(), dateRef)
    validator(response.code, response.description)

    val lines = response.data
        .mapNotNull { dto ->
            val label = normalizeLine(dto.label.ifBlank { dto.line })
            val id = label.ifBlank { normalizeLine(dto.line) }
            if (id.isBlank() || label.isBlank()) return@mapNotNull null
            BusLine(id = id, label = label, nameA = dto.nameA, nameB = dto.nameB)
        }
        .distinctBy { normalizeLine(it.label) }
        .sortedWith(busLineComparator())
    Timber.d("Loaded EMT lines raw=${response.data.size} mapped=${lines.size}")
    lines
}

private fun busLineComparator(): Comparator<BusLine> = compareBy<BusLine> {
    !it.label.all(Char::isDigit)
}.thenBy {
    it.label.toIntOrNull() ?: Int.MAX_VALUE
}.thenBy { it.label }
