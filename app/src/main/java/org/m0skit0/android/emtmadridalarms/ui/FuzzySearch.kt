package org.m0skit0.android.emtmadridalarms.ui

/**
 * Fuzzy-matches [query] against [target], returning a score or null if there is no match.
 *
 * Scoring (lower is better — suitable for use with sortedBy):
 *   0 — exact match (target == query, case-insensitive)
 *   1 — substring match (target contains query, case-insensitive)
 *   2 — all query characters appear in [target] in order, case-insensitive (fuzzy match)
 *
 * Returns null when not all query characters can be found in order.
 */
internal fun fuzzyScore(query: String, target: String): Int? {
    val q = query.lowercase()
    val t = target.lowercase()
    if (t == q) return 0
    if (t.contains(q)) return 1
    val qNoSpaces = q.replace(" ", "")
    return if (qNoSpaces.isNotEmpty() && isFuzzyMatch(qNoSpaces, t)) 2 else null
}

private fun isFuzzyMatch(query: String, target: String): Boolean {
    var queryIndex = 0
    for (char in target) {
        if (queryIndex < query.length && char == query[queryIndex]) queryIndex++
    }
    return queryIndex == query.length
}

/**
 * Filters and sorts [options] by fuzzy relevance against [query].
 * Exact matches come first, then substring matches, then character-order matches.
 * Items with no match are excluded.
 * When [query] is blank, [options] is returned unchanged.
 */
internal fun <T> fuzzyFilter(options: List<T>, query: String, text: (T) -> String): List<T> {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return options
    return options
        .mapNotNull { option -> fuzzyScore(trimmed, text(option))?.let { score -> option to score } }
        .sortedBy { (_, score) -> score }
        .map { (option, _) -> option }
}
