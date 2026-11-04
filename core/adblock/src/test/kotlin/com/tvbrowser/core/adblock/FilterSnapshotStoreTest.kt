package com.tvbrowser.core.adblock

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class FilterSnapshotStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun persistsOriginalRestrictionsAndTimestamp() {
        val store = FilterSnapshotStore(temporary.root)
        val text = "! list\n||ads.example^\n||cdn.example^\$third-party\n"
        store.write(FilterSource.Ads, text, 1000)
        assertEquals(CachedFilterList(text, 1000), store.read(FilterSource.Ads))
    }
    @Test fun corruptedOrUncommittedDataIsNotTrusted() {
        val store = FilterSnapshotStore(temporary.root)
        store.write(FilterSource.Ads, "||ads.example^", 1000)
        temporary.root.resolve("easylist.snapshot").appendText("tampered")
        assertNull(store.read(FilterSource.Ads))
        temporary.root.resolve("easyprivacy.snapshot.tmp").writeText("unfinished")
        assertNull(store.read(FilterSource.Trackers))
    }
    @Test fun emptyReplacementPreservesPreviousSnapshot() {
        val store = FilterSnapshotStore(temporary.root)
        store.write(FilterSource.Ads, "||ads.example^", 1000)
        try { store.write(FilterSource.Ads, "<html>unavailable</html>", 2000); fail("Invalid replacement accepted") }
        catch (_: IOException) { }
        assertEquals(1000L, store.read(FilterSource.Ads)?.updatedAtMillis)
    }

    @Test fun byteLimitAcceptsExactBoundaryAndRejectsOversizeUtf8WithoutReplacingSnapshot() {
        val store = FilterSnapshotStore(temporary.root)
        val prefix = "||ads.example^\n!"
        val text = prefix + "x".repeat(MAX_FILTER_BYTES - prefix.length)
        store.write(FilterSource.Ads, text, 1000)
        assertEquals(text, store.read(FilterSource.Ads)?.text)

        try {
            store.write(FilterSource.Ads, text + "é", 2000)
            fail("Oversize UTF-8 replacement accepted")
        } catch (_: IOException) { }
        assertEquals(1000L, store.read(FilterSource.Ads)?.updatedAtMillis)
    }

    @Test fun invalidTimestampAndInterruptedReplacementPreserveCommittedGeneration() {
        val store = FilterSnapshotStore(temporary.root)
        store.write(FilterSource.Ads, "||ads.example^", 1000)
        try {
            store.write(FilterSource.Ads, "||replacement.example^", 0)
            fail("Invalid timestamp accepted")
        } catch (_: IOException) { }
        temporary.root.resolve("easylist.snapshot.tmp").writeText("unfinished replacement")

        assertEquals(CachedFilterList("||ads.example^", 1000), store.read(FilterSource.Ads))
    }

    @Test fun cachedRulesKeepTheirConditionalBehaviorAfterReload() {
        val store = FilterSnapshotStore(temporary.root)
        store.write(FilterSource.Ads, "||cdn.example^\$third-party", 1000)
        val restored = store.read(FilterSource.Ads)!!
        val engine = HostRuleEngine(listOf(EasyListParser.parse(restored.text.reader().buffered())))

        assertFalse(engine.shouldBlock("https://cdn.example/file", "https://cdn.example/page"))
        assertTrue(engine.shouldBlock("https://cdn.example/file", "https://news.example"))
    }
}
