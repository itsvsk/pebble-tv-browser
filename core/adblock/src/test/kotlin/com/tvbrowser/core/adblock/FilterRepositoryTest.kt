package com.tvbrowser.core.adblock

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class FilterRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val seed = mapOf(FilterSource.Ads to "||seed-ad.example^", FilterSource.Trackers to "||seed-tracker.example^")
    @Test fun protectsImmediatelyAndKeepsLastGoodSourceOnPartialRefresh() = runBlocking {
        var failAds = false
        var generation = 1
        val repository = FilterRepository(FilterSnapshotStore(temporary.root), { source ->
            if (source == FilterSource.Ads && failAds) throw IOException("offline")
            "[Adblock Plus 2.0]\n||${source.name.lowercase()}$generation.example^"
        }, seed, { 1000L + generation })
        assertTrue(repository.shouldBlock("https://seed-ad.example/a", "https://page.example"))
        assertTrue(repository.refresh().isSuccess)
        failAds = true; generation = 2
        assertTrue(repository.refresh().isFailure)
        assertTrue(repository.shouldBlock("https://ads1.example/a", "https://page.example"))
        assertTrue(repository.shouldBlock("https://trackers2.example/a", "https://page.example"))
        assertNotNull(repository.status.value.lists.first { it.source == FilterSource.Ads }.error)
        assertFalse(repository.status.value.refreshing)
    }
    @Test fun corruptCacheFallsBackAndLegacyHostProjectionIsIgnored() = runBlocking {
        temporary.root.resolve("easylist.snapshot").writeText("corrupt")
        temporary.root.resolve("hosts.txt").writeText("||wrong.example^")
        val repository = FilterRepository(FilterSnapshotStore(temporary.root), { throw IOException("offline") }, seed)
        repository.initialize()
        assertTrue(repository.shouldBlock("https://seed-ad.example/a", "https://page.example"))
        assertFalse(repository.shouldBlock("https://wrong.example/a", "https://page.example"))
        assertTrue(repository.status.value.lists.all { it.bundled })
        assertTrue(repository.refresh().isFailure)
        assertTrue(repository.shouldBlock("https://seed-tracker.example/a", "https://page.example"))
    }
    @Test fun http200ErrorBodyCannotReplaceGoodDataAndSavedRulesSurviveRestart() = runBlocking {
        var broken = false
        val repository = FilterRepository(FilterSnapshotStore(temporary.root), {
            if (broken) "<html>upstream error</html>\n||misleading.example^"
            else "[Adblock Plus 2.0]\n||working.example^"
        }, seed, { 1000L })
        assertTrue(repository.refresh().isSuccess)
        broken = true
        assertTrue(repository.refresh().isFailure)
        assertTrue(repository.shouldBlock("https://working.example/ad", "https://page.example"))
        assertFalse(repository.shouldBlock("https://misleading.example/ad", "https://page.example"))
        val restored = FilterRepository(FilterSnapshotStore(temporary.root), { error("No fetch during initialization") }, seed)
        restored.initialize()
        assertTrue(restored.shouldBlock("https://working.example/ad", "https://page.example"))
        assertTrue(restored.status.value.lists.none { it.bundled })
    }

    @Test fun cancellingSuspendedFetchDoesNotPublishOrSavePartialBody() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val repository = FilterRepository(FilterSnapshotStore(temporary.root), {
            entered.complete(Unit)
            awaitCancellation()
        }, seed)
        val job = launch { repository.refresh() }
        entered.await()
        job.cancelAndJoin()
        assertFalse(repository.status.value.refreshing)
        assertTrue(repository.shouldBlock("https://seed-ad.example/ad", "https://page.example"))
        assertNull(FilterSnapshotStore(temporary.root).read(FilterSource.Ads))
    }

    @Test fun concurrentRefreshesCannotOverwriteEachOtherAndCancellationClearsBusyState() = runBlocking {
        var active = 0; var maxActive = 0
        val repository = FilterRepository(FilterSnapshotStore(temporary.root), {
            active++; maxActive = maxOf(maxActive, active)
            Thread.sleep(20)
            active--
            "[Adblock Plus 2.0]\n||ads.example^"
        }, seed)
        awaitAll(async(Dispatchers.Default) { repository.refresh() }, async(Dispatchers.Default) { repository.refresh() })
        assertEquals(1, maxActive)
        val cancelled = FilterRepository(FilterSnapshotStore(temporary.newFolder()), { throw CancellationException("cancel") }, seed)
        try { cancelled.refresh(); fail("Cancellation swallowed") } catch (_: CancellationException) { }
        assertFalse(cancelled.status.value.refreshing)
    }
}
