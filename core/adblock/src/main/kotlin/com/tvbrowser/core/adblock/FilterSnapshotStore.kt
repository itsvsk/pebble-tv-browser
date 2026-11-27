package com.tvbrowser.core.adblock

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

internal const val MAX_FILTER_BYTES = 8 * 1024 * 1024
internal data class CachedFilterList(val text: String, val updatedAtMillis: Long) {
    val parsed: ParsedFilterList by lazy { validateFilterText(text) }
}

/** One rename publishes source text and metadata together; an interrupted write keeps the old file. */
internal class FilterSnapshotStore(private val directory: File) {
    fun read(source: FilterSource): CachedFilterList? = runCatching {
        val file = File(directory, source.cacheName)
        if (!file.isFile || file.length() > MAX_FILTER_BYTES + 256) return null
        val content = file.readText(Charsets.UTF_8).split('\n', limit = 4)
        if (content.size != 4 || content[0] != "PEBBLE-FILTER-1") return null
        val timestamp = content[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val text = content[3]
        if (content[2] != digest(text)) return null
        CachedFilterList(text, timestamp).also { it.parsed }
    }.getOrNull()

    fun write(source: FilterSource, text: String, updatedAtMillis: Long) =
        write(source, CachedFilterList(text, updatedAtMillis))

    fun write(source: FilterSource, snapshot: CachedFilterList) {
        snapshot.parsed
        val text = snapshot.text
        val updatedAtMillis = snapshot.updatedAtMillis
        if (updatedAtMillis <= 0) throw IOException("Invalid update time")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Filter storage unavailable")
        val target = File(directory, source.cacheName)
        val temporary = File(directory, source.cacheName + ".tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write("PEBBLE-FILTER-1\n$updatedAtMillis\n${digest(text)}\n$text".toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            if (!temporary.renameTo(target)) throw IOException("Could not save filters")
        } finally { temporary.delete() }
    }

    private fun digest(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

internal fun validateFilterText(text: String): ParsedFilterList {
    if (text.toByteArray(Charsets.UTF_8).size > MAX_FILTER_BYTES) throw IOException("Filter list too large")
    val parsed = EasyListParser.parse(text.reader().buffered())
    if (parsed.blockRuleCount == 0) throw IOException("No supported blocking rules")
    return parsed
}

/** Fixed upstream sources are ABP lists; an HTTP-200 error/template is not a valid update. */
internal fun validateSubscriptionHeader(text: String) {
    val first = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.removePrefix("\uFEFF")
    if (first == null || !Regex("""\[Adblock Plus [0-9]+(?:\.[0-9]+)*\]""").matches(first))
        throw IOException("Unexpected filter format")
}
