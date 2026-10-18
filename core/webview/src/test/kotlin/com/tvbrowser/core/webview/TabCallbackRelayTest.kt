package com.tvbrowser.core.webview

import org.junit.Assert.assertEquals
import org.junit.Test

class TabCallbackRelayTest {
    @Test
    fun pageCompletionPersistsWithoutAnAttachedBrowserScreen() {
        val persisted = mutableListOf<String>()
        val relay = TabCallbackRelay { url, _ -> persisted += url }

        relay.pageFinished("https://example.com/next", "Next")

        assertEquals(listOf("https://example.com/next"), persisted)
    }

    @Test
    fun oldScreenCannotClearNewScreensCallbacks() {
        val events = mutableListOf<String>()
        val relay = TabCallbackRelay { url, _ -> events += "persist:$url" }
        val firstOwner = Any()
        val secondOwner = Any()
        relay.attach(firstOwner, callbacks("first", events))
        relay.pageStarted("one")

        relay.attach(secondOwner, callbacks("second", events))
        relay.detach(firstOwner)
        relay.progress(50)
        relay.pageFinished("two", "Two")
        relay.detach(secondOwner)
        relay.pageFinished("three", "Three")

        assertEquals(
            listOf("first:start:one", "second:progress:50", "persist:two", "second:finish:two", "persist:three"),
            events,
        )
    }

    private fun callbacks(label: String, events: MutableList<String>) = TabUiCallbacks(
        onPageStarted = { events += "$label:start:$it" },
        onPageFinished = { url, _ -> events += "$label:finish:$url" },
        onProgress = { events += "$label:progress:$it" },
        onDownload = { url, _, _, _ -> events += "$label:download:$url" },
    )
}
