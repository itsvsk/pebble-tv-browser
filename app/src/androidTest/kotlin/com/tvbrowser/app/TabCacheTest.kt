package com.tvbrowser.app

import android.content.ComponentCallbacks
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.common.SettingsRepository
import com.tvbrowser.core.db.AppDatabase
import com.tvbrowser.core.db.repo.TabRepository
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabManager
import com.tvbrowser.core.webview.WebVideoController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Offline real-provider coverage: budget changes must not destroy an owned page or its history. */
@RunWith(AndroidJUnit4::class)
class TabCacheTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun evictedTabRestoresBackForwardHistoryAndExplicitNavigationReplacesIt() = fixture { manager, _ ->
        lateinit var original: WebView
        main { original = manager.getOrCreate("history") }
        load(manager, original, "history", "First")
        load(manager, original, "history", "Second")
        main { assertEquals(2, original.copyBackForwardList().size) }
        fillCache(manager)
        main { assertFalse(manager.isLiveView("history", original)) }

        lateinit var restored: WebView
        main {
            restored = manager.getOrCreate("history")
            assertNotSame(original, restored)
            assertTrue(restored.canGoBack())
        }
        awaitTitle(restored, "Second")
        main { restored.goBack() }
        awaitTitle(restored, "First")
        main { assertTrue(restored.canGoForward()) }
        fillCache(manager)
        main { assertFalse(manager.isLiveView("history", restored)) }
        main { manager.navigate("history", page("Fresh"), TabConfig()) }
        main { restored = manager.getOrCreate("history") }
        awaitTitle(restored, "Fresh")
        main { assertFalse("Explicit navigation must not revive the evicted history", restored.canGoBack()) }
    }

    @Test fun pressureProtectsTransitioningHostAndOptedInPlaybackThenReleasesIdleViews() = fixture { manager, context ->
        lateinit var hosted: WebView
        lateinit var media: WebView
        lateinit var idle: WebView
        lateinit var parent: FrameLayout
        lateinit var memoryCallback: ComponentCallbacks2
        main {
            hosted = manager.getOrCreate("hosted")
            memoryCallback = context.callbacks!!
            parent = FrameLayout(context)
            parent.addView(hosted) // onRelease/transition can retain a parent after foreground=false.
            manager.setTabForeground("hosted", false)
            media = manager.getOrCreate("media")
            manager.setTabBackgroundPlayback("media", true)
            idle = manager.getOrCreate("idle")
        }
        fillCache(manager)
        main {
            assertTrue(manager.isLiveView("hosted", hosted))
            assertTrue(manager.isLiveView("media", media))
            context.callbacks!!.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        }
        instrumentation.waitForIdleSync()
        main {
            assertTrue(manager.isLiveView("hosted", hosted))
            assertTrue(manager.isLiveView("media", media))
            assertFalse(manager.isLiveView("idle", idle))
            parent.removeView(hosted)
            manager.setTabBackgroundPlayback("media", false)
            context.callbacks!!.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        }
        instrumentation.waitForIdleSync()
        main {
            assertFalse(manager.isLiveView("hosted", hosted))
            assertFalse(manager.isLiveView("media", media))
            assertFalse("An empty manager must stop receiving callbacks", memoryCallback in context.registered)
        }
    }

    @Test fun closingAnEvictedPrivateTabDropsSavedNavigation() = fixture { manager, _ ->
        lateinit var original: WebView
        main { original = manager.getOrCreate("private", TabConfig(isIncognito = true)) }
        load(manager, original, "private", "Private", TabConfig(isIncognito = true))
        fillCache(manager)
        main {
            assertFalse(manager.isLiveView("private", original))
            manager.release("private")
            val clean = manager.getOrCreate("private", TabConfig(isIncognito = true))
            assertNull(clean.url)
            assertEquals(0, clean.copyBackForwardList().size)
        }
    }

    @Test fun latestBackgroundOptInReplacesEarlierAllowancesUnderMemoryPressure() = fixture { manager, context ->
        val olderTabs = listOf("first", "second")
        lateinit var latestView: WebView
        val earlierViews = mutableMapOf<String, WebView>()
        main {
            for (id in olderTabs) {
                earlierViews[id] = manager.getOrCreate(id)
                manager.setTabMuted(id, true)
                manager.setTabBackgroundPlayback(id, true)
            }
            latestView = manager.getOrCreate("latest")
            manager.setTabBackgroundPlayback("latest", true)
            context.callbacks!!.onLowMemory()
        }
        instrumentation.waitForIdleSync()
        main {
            for ((id, view) in earlierViews) {
                assertFalse(manager.mediaState(id).backgroundPlayback)
                assertTrue("Transferring playback must preserve mute", manager.mediaState(id).muted)
                assertFalse(manager.isLiveView(id, view))
            }
            assertTrue(manager.mediaState("latest").backgroundPlayback)
            assertTrue(manager.isLiveView("latest", latestView))
        }
    }

    @Test fun pressureEvictionRestoresLastUrlWithoutBackHistory() = fixture { manager, context ->
        val id = "pressure-history"
        lateinit var original: WebView
        lateinit var lastUrl: String
        main { original = manager.getOrCreate(id) }
        load(manager, original, id, "First")
        load(manager, original, id, "Second")
        main {
            assertTrue(original.canGoBack())
            lastUrl = checkNotNull(original.url)
            context.callbacks!!.onLowMemory()
        }
        instrumentation.waitForIdleSync()
        lateinit var restored: WebView
        main {
            assertFalse(manager.isLiveView(id, original))
            restored = manager.getOrCreate(id)
            assertNotSame(original, restored)
        }
        awaitTitle(restored, "Second")
        main {
            assertEquals(lastUrl, restored.url)
            assertFalse(restored.canGoBack())
        }
    }

    private fun fillCache(manager: TabManager) {
        main { repeat(12) { manager.getOrCreate("filler-$it") } }
    }

    private fun load(manager: TabManager, view: WebView, id: String, title: String, config: TabConfig = TabConfig()) {
        main { manager.navigate(id, page(title), config) }
        awaitTitle(view, title)
    }

    private fun awaitTitle(view: WebView, title: String) {
        val deadline = android.os.SystemClock.uptimeMillis() + 10_000
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            var ready = false
            main { ready = view.title == title && view.progress == 100 }
            if (ready) return
            Thread.sleep(30)
        }
        fail("Page did not finish: $title")
    }

    private fun page(title: String): String = "data:text/html;base64," + Base64.encodeToString(
        "<html><head><title>$title</title></head><body>$title</body></html>".toByteArray(), Base64.NO_WRAP)

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun fixture(check: (TabManager, MemoryContext) -> Unit) {
        val app = instrumentation.targetContext.applicationContext
        val context = MemoryContext(app)
        val database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        lateinit var manager: TabManager
        main { manager = TabManager(context, TabRepository(database.tabDao()),
            AdBlockInterceptor(app), WebVideoController(), SettingsRepository(app)) }
        try { check(manager, context) }
        finally {
            main { manager.destroyAll() }
            database.close()
        }
    }

    private class MemoryContext(base: Context) : ContextWrapper(base) {
        val registered = linkedSetOf<ComponentCallbacks>()
        val callbacks: ComponentCallbacks2? get() = registered.filterIsInstance<ComponentCallbacks2>().firstOrNull()
        override fun registerComponentCallbacks(callback: ComponentCallbacks) { registered.add(callback) }
        override fun unregisterComponentCallbacks(callback: ComponentCallbacks) { registered.remove(callback) }
    }
}
