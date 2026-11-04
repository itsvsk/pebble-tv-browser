package com.tvbrowser.core.adblock

import java.io.BufferedReader
import java.net.IDN
import java.util.Locale

/** A deliberately bounded EasyList subset; unsupported blocking rules are never widened. */
object EasyListParser {
    private val resourceOptions = setOf(
        "script", "image", "stylesheet", "object", "object-subrequest", "subdocument",
        "xmlhttprequest", "ping", "websocket", "webrtc", "font", "media", "other",
    )
    fun parse(reader: BufferedReader): ParsedFilterList {
        val rules = linkedMapOf<String, HostRule>()
        val disabled = mutableSetOf<String>()
        var unsupported = 0
        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim().removePrefix("\uFEFF")
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue
                val exception = line.startsWith("@@")
                val body = line.removePrefix("@@")
                if (!body.startsWith("||")) { unsupported++; continue }
                val pattern = body.substringBefore('$').removePrefix("||")
                val host = normalizeFilterHost(pattern.takeWhile { it != '^' && it != '/' })
                if (host == null) { unsupported++; continue }
                val suffix = pattern.dropWhile { it != '^' && it != '/' }
                val options = if ('$' in body) body.substringAfter('$').split(',') else emptyList()
                val includes = mutableSetOf<String>()
                val excludes = mutableSetOf<String>()
                var party: Boolean? = null
                var document = false
                var badfilter = false
                var unsupportedOption = false
                var invalid = false
                val canonicalOptions = mutableListOf<String>()
                for (rawOption in options) {
                    val option = rawOption.lowercase(Locale.ROOT)
                    when {
                        option == "badfilter" -> badfilter = true
                        option == "third-party" || option == "~third-party" -> {
                            val desired = option == "third-party"
                            if (party != null && party != desired) invalid = true
                            party = desired
                            canonicalOptions += option
                        }
                        option == "document" && exception -> {
                            document = true
                            canonicalOptions += option
                        }
                        option == "match-case" && exception -> {
                            // A host-only projection cannot retain path case sensitivity.
                            unsupportedOption = true
                            canonicalOptions += option
                        }
                        option.startsWith("domain=") -> {
                            val domains = option.substringAfter('=').split('|')
                            for (domain in domains) {
                                val normalized = normalizeFilterHost(domain.removePrefix("~"))
                                if (normalized == null) invalid = true
                                else if (domain.startsWith("~")) excludes += normalized else includes += normalized
                            }
                            canonicalOptions += "domain=" +
                                (includes + excludes.map { "~$it" }).sorted().joinToString("|")
                        }
                        option.removePrefix("~") in resourceOptions -> {
                            unsupportedOption = true
                            canonicalOptions += option
                        }
                        // Cosmetic/redirect/rewrite exceptions are not network allows.
                        // Unknown options cannot safely be projected into network policy.
                        else -> invalid = true
                    }
                }
                // Only ||host^ is an exact supported pattern. A scoped exception may be
                // widened to its host to avoid blocking content the list explicitly allows.
                val widened = suffix != "^" || unsupportedOption
                if (invalid || (widened && !exception)) { unsupported++; continue }
                val key = (if (exception) "@@" else "") + "||$host$suffix" +
                    canonicalOptions.distinct().sorted().joinToString(",").let { if (it.isEmpty()) "" else "$$it" }
                if (badfilter) {
                    disabled += key
                    continue
                }
                // Top-level navigation's initiator is unavailable to this matcher. Do
                // not guess its party relationship and accidentally discard an allow.
                val unknownDocumentParty = document && party != null
                if (widened || unknownDocumentParty) unsupported++
                rules[key] = HostRule(host, if (document) null else party, includes.toSet(), excludes.toSet(), exception, document, key)
            }
        }
        return ParsedFilterList(rules.values.toList(), disabled.toSet(), unsupported)
    }
}

class ParsedFilterList internal constructor(
    internal val rules: List<HostRule>,
    internal val disabledRules: Set<String>,
    val unsupportedRules: Int,
) {
    val ruleCount: Int = rules.count { it.key !in disabledRules }
    val blockRuleCount: Int = rules.count { !it.exception && it.key !in disabledRules }
}

internal data class HostRule(
    val host: String,
    val thirdParty: Boolean?,
    val includes: Set<String>,
    val excludes: Set<String>,
    val exception: Boolean,
    val document: Boolean,
    val key: String,
) {
    fun appliesTo(pageHost: String, isThirdParty: () -> Boolean): Boolean =
        (thirdParty == null || thirdParty == isThirdParty()) &&
            (includes.isEmpty() || includes.any { hostMatches(pageHost, it) }) &&
            excludes.none { hostMatches(pageHost, it) }
}

internal fun hostMatches(host: String, ruleHost: String): Boolean =
    host == ruleHost || host.endsWith(".$ruleHost")

internal fun normalizeFilterHost(value: String): String? = runCatching {
    val host = IDN.toASCII(value.trimEnd('.'), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
    host.takeIf { it.isNotEmpty() && it.length <= 253 && it.split('.').all { part -> part.isNotEmpty() && part.length <= 63 } }
}.getOrNull()
