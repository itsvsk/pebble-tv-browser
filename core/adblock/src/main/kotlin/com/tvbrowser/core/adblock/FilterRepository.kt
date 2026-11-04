package com.tvbrowser.core.adblock

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/** Owns initialization, persistence and refresh; request threads only read the immutable engine. */
internal class FilterRepository(
    private val store: FilterSnapshotStore,
    private val fetch: suspend (FilterSource) -> String,
    bundled: Map<FilterSource, String> = BundledFilters.lists,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class ActiveList(val parsed: ParsedFilterList, val status: FilterListStatus)
    private val lists = FilterSource.entries.associateWith { source ->
        val parsed = validateFilterText(bundled.getValue(source))
        ActiveList(parsed, FilterListStatus(source, parsed.ruleCount, parsed.unsupportedRules))
    }.toMutableMap()
    private val engine = AtomicReference(HostRuleEngine(lists.values.map { it.parsed }))
    private val mutableStatus = MutableStateFlow(FilterStatus(lists = lists.values.map { it.status }))
    val status = mutableStatus.asStateFlow()
    private val mutation = Mutex()
    private var initialized = false

    fun shouldBlock(url: String, pageUrl: String): Boolean = engine.get().shouldBlock(url, pageUrl)

    suspend fun initialize() = withContext(Dispatchers.IO) { mutation.withLock { initializeLocked() } }

    private fun initializeLocked() {
        if (initialized) return
        HostRuleEngine.warmUp()
        for (source in FilterSource.entries) {
            val saved = store.read(source) ?: continue
            val parsed = saved.parsed
            lists[source] = ActiveList(parsed, FilterListStatus(source, parsed.ruleCount,
                parsed.unsupportedRules, saved.updatedAtMillis, bundled = false))
        }
        initialized = true
        publish()
    }

    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        mutation.withLock {
            initializeLocked()
            mutableStatus.value = mutableStatus.value.copy(refreshing = true)
            var failed = false
            try {
                for (source in FilterSource.entries) {
                    currentCoroutineContext().ensureActive()
                    try {
                        val text = fetch(source)
                        currentCoroutineContext().ensureActive()
                        validateSubscriptionHeader(text)
                        val timestamp = now()
                        val snapshot = CachedFilterList(text, timestamp)
                        val parsed = snapshot.parsed
                        store.write(source, snapshot)
                        lists[source] = ActiveList(parsed, FilterListStatus(source, parsed.ruleCount,
                            parsed.unsupportedRules, timestamp, bundled = false))
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        failed = true
                        val old = lists.getValue(source)
                        lists[source] = old.copy(status = old.status.copy(error =
                            if (error is FilterHttpException) "Update failed (HTTP ${error.statusCode}). Previous protection kept."
                            else "Could not update. Previous protection kept; check your connection and try again."))
                    }
                }
                if (failed) Result.failure(java.io.IOException("Some filter lists could not be updated"))
                else Result.success(lists.values.sumOf { it.parsed.ruleCount })
            } finally { publish() }
        }
    }

    private fun publish() {
        engine.set(HostRuleEngine(lists.values.map { it.parsed }))
        mutableStatus.value = FilterStatus(ready = true, lists = FilterSource.entries.map { lists.getValue(it).status })
    }
}
