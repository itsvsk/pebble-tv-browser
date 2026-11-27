package com.tvbrowser.core.webview

/** Budgets are conservative: the low-RAM flag alone misses many inexpensive TVs. */
internal object TabCachePolicy {
    fun capacity(lowRam: Boolean, memoryClassMb: Int, totalMemoryBytes: Long): Int = when {
        lowRam || memoryClassMb <= 128 || totalMemoryBytes in 1..(2L * 1024 * 1024 * 1024) -> 2
        memoryClassMb <= 192 || totalMemoryBytes in 1..(3L * 1024 * 1024 * 1024) -> 4
        else -> 8
    }

    fun evictions(oldestFirst: List<String>, protected: Set<String>, capacity: Int): List<String> {
        var remaining = oldestFirst.size
        return oldestFirst.filter { id ->
            (remaining > capacity && id !in protected).also { if (it) remaining-- }
        }
    }
}
