package org.m0skit0.android.emtmadridalarms.ui

import org.m0skit0.android.emtmadridalarms.data.normalizeLine
import org.m0skit0.android.emtmadridalarms.domain.BusLine
import org.m0skit0.android.emtmadridalarms.domain.BusStop

internal fun linesForStop(lines: List<BusLine>, stop: BusStop): List<BusLine> {
    if (stop.lineLabels.isEmpty()) return lines
    val labels = stop.lineLabels.map(::normalizeLine).toSet()
    return lines.filter { normalizeLine(it.label) in labels || normalizeLine(it.id) in labels }
}

internal fun stopServesLine(stop: BusStop, line: BusLine): Boolean {
    if (stop.lineLabels.isEmpty()) return false
    val labels = stop.lineLabels.map(::normalizeLine).toSet()
    return normalizeLine(line.label) in labels || normalizeLine(line.id) in labels
}
