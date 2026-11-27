package com.tvbrowser.core.webview

import org.junit.Assert.*
import org.junit.Test

class TabCachePolicyTest {
    @Test fun constrainedDevicesKeepTwoViewsEvenWhenLowRamFlagIsMissing() {
        assertEquals(2, TabCachePolicy.capacity(false, 192, 1536L * 1024 * 1024))
        assertEquals(2, TabCachePolicy.capacity(true, 256, 4L * 1024 * 1024 * 1024))
        assertEquals(2, TabCachePolicy.capacity(false, 128, 4L * 1024 * 1024 * 1024))
        assertEquals(4, TabCachePolicy.capacity(false, 192, 3L * 1024 * 1024 * 1024))
        assertEquals(8, TabCachePolicy.capacity(false, 256, 4L * 1024 * 1024 * 1024))
    }

    @Test fun evictionKeepsVisibleTransitioningAndBackgroundPlaybackTabs() {
        val lru = listOf("visible", "old", "media", "warm", "incoming")
        assertEquals(listOf("old", "warm"), TabCachePolicy.evictions(lru,
            setOf("visible", "media", "incoming"), 2))
    }

    @Test fun pressureEvictsOldestFirstAndBackgroundReleasesEveryUnprotectedView() {
        val lru = listOf("old", "warm", "current")
        assertEquals(listOf("old"), TabCachePolicy.evictions(lru, setOf("current"), 2))
        assertEquals(listOf("old", "warm"), TabCachePolicy.evictions(lru, setOf("current"), 0))
    }
}
