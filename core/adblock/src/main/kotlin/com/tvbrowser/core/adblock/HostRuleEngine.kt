package com.tvbrowser.core.adblock

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Immutable request matcher. Call [warmUp] on IO before publishing party-scoped subscriptions. */
class HostRuleEngine(lists: List<ParsedFilterList>) {
    private val blocks: Map<String, List<HostRule>>
    private val exceptions: Map<String, List<HostRule>>
    private val documentExceptions: Map<String, List<HostRule>>
    val ruleCount: Int

    init {
        val disabled = lists.flatMap { it.disabledRules }.toSet()
        val rules = lists.flatMap { it.rules }.filter { it.key !in disabled }.distinctBy { it.key }
        ruleCount = rules.size
        blocks = rules.filter { !it.exception }.groupBy { it.host }
        exceptions = rules.filter { it.exception && !it.document }.groupBy { it.host }
        documentExceptions = rules.filter { it.document }.groupBy { it.host }
    }

    fun shouldBlock(requestUrl: String, pageUrl: String): Boolean {
        if (blocks.isEmpty()) return false
        val request = requestUrl.toHttpUrlOrNull() ?: return false
        val page = pageUrl.toHttpUrlOrNull() ?: return false
        val host = request.host.trimEnd('.')
        val pageHost = page.host.trimEnd('.')
        // The bundled unconditional seed never touches public-suffix resources.
        var cachedParty: Boolean? = null
        val thirdParty = {
            cachedParty ?: ((request.topPrivateDomain()?.trimEnd('.') ?: host) !=
                (page.topPrivateDomain()?.trimEnd('.') ?: pageHost)).also { cachedParty = it }
        }
        if (!blocks.matches(host, pageHost, thirdParty)) return false
        if (documentExceptions.matches(pageHost, pageHost) { false }) return false
        return !exceptions.matches(host, pageHost, thirdParty)
    }

    private fun Map<String, List<HostRule>>.matches(host: String, pageHost: String, thirdParty: () -> Boolean): Boolean {
        var candidate = host
        while (candidate.isNotEmpty()) {
            if (get(candidate)?.any { it.appliesTo(pageHost, thirdParty) } == true) return true
            val separator = candidate.indexOf('.')
            if (separator < 0) break
            candidate = candidate.substring(separator + 1)
        }
        return false
    }
    companion object {
        /** OkHttp loads bundled public-suffix data lazily; run before accepting rich lists. */
        fun warmUp() {
            "https://example.com".toHttpUrlOrNull()?.topPrivateDomain()
        }
    }
}

/** Match document callbacks despite normalized host/default-port forms and stripped fragments. */
fun protectionDocumentUrl(url: String): String = url.toHttpUrlOrNull()?.newBuilder()
    ?.fragment(null)?.build()?.toString() ?: url.substringBefore('#')
