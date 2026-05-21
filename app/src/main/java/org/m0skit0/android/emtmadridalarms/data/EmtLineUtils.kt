package org.m0skit0.android.emtmadridalarms.data

fun normalizeLine(line: String): String {
    val value = line.trim().uppercase()
    if (value.isEmpty()) return value
    if (value.all(Char::isDigit)) return value.trimStart('0').ifEmpty { "0" }

    val match = Regex("^([A-Z]+)0+([0-9]+)$").matchEntire(value)
    return if (match != null) {
        match.groupValues[1] + match.groupValues[2].trimStart('0').ifEmpty { "0" }
    } else {
        value
    }
}

fun linesMatch(userLine: String, apiLine: String): Boolean = normalizeLine(userLine) == normalizeLine(apiLine)
