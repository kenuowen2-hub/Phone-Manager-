package com.example.filter

import com.example.data.local.BlockedRule
import java.net.URI
import java.net.URLDecoder
import java.util.regex.Pattern

data class FilterCheckResult(
    val isBlocked: Boolean,
    val matchedRule: BlockedRule? = null,
    val matchedText: String? = null
)

object SearchFilterEngine {

    private val searchParamNames = setOf("q", "query", "p", "search_query", "text", "wd", "k")

    fun checkQuery(query: String, rules: List<BlockedRule>): FilterCheckResult {
        val trimmed = query.trim()
        if (trimmed.isBlank() || rules.isEmpty()) {
            return FilterCheckResult(isBlocked = false)
        }

        for (rule in rules) {
            val patternStr = rule.pattern.trim()
            if (patternStr.isBlank()) continue

            if (rule.isWholeWord) {
                // Word boundary check: \b\Qpattern\E\b
                val regexPattern = "\\b" + Pattern.quote(patternStr) + "\\b"
                val matcher = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE).matcher(trimmed)
                if (matcher.find()) {
                    return FilterCheckResult(
                        isBlocked = true,
                        matchedRule = rule,
                        matchedText = matcher.group()
                    )
                }
            } else {
                // Phrase substring check
                if (trimmed.contains(patternStr, ignoreCase = true)) {
                    return FilterCheckResult(
                        isBlocked = true,
                        matchedRule = rule,
                        matchedText = patternStr
                    )
                }
            }
        }

        return FilterCheckResult(isBlocked = false)
    }

    fun extractSearchQueryFromUrl(url: String): String? {
        return try {
            val rawQuery = URI(url).rawQuery ?: return null
            for (pair in rawQuery.split("&")) {
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8").lowercase()
                    if (key in searchParamNames) {
                        val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8").trim()
                        if (value.isNotEmpty()) return value
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
