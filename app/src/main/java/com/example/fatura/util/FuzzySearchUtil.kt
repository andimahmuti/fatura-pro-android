package com.example.fatura.util

object FuzzySearchUtil {
    /**
     * Checks if the query matches the text using a simple fuzzy approach.
     * 1. Direct contains (ignore case)
     * 2. Acronym match (e.g. "kf" matches "kafe")
     * 3. Levenshtein distance (optional, but starts with simple character sequence)
     */
    fun matches(query: String, text: String): Boolean {
        if (query.isEmpty()) return true
        val q = query.lowercase().trim()
        val t = text.lowercase().trim()

        // 1. Direct match
        if (t.contains(q)) return true

        // 2. Initial match (e.g. "kf" matches "kafe")
        val initials = t.split(" ").filter { it.isNotEmpty() }.map { it[0] }.joinToString("")
        if (initials.contains(q)) return true

        // 3. Subsequence match (characters in query appear in order in text)
        var textIdx = 0
        var queryIdx = 0
        while (textIdx < t.length && queryIdx < q.length) {
            if (t[textIdx] == q[queryIdx]) {
                queryIdx++
            }
            textIdx++
        }
        return queryIdx == q.length
    }
}
