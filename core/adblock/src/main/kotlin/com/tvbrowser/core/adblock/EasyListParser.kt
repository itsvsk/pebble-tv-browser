package com.tvbrowser.core.adblock

import java.io.BufferedReader

/**
 * Parses EasyList-format lines. v1 supports only host-based rules of the form
 *   ||example.com^
 *   ||sub.example.com^$third-party
 * Ignores: cosmetic filters (## / #@#), exception rules (@@), regex rules, element-hide.
 */
object EasyListParser {

    private val hostRule = Regex("""^\|\|([a-z0-9.\-_]+)\^""")

    fun parse(reader: BufferedReader): Set<String> {
        val out = HashSet<String>()
        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue
                if (line.startsWith("@@")) continue
                if (line.contains("##") || line.contains("#@#")) continue
                val m = hostRule.find(line) ?: continue
                out.add(m.groupValues[1].lowercase())
            }
        }
        return out
    }
}
