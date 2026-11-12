package com.tvbrowser.app

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.adblock.EasyListLoader
import com.tvbrowser.core.common.normalizedSiteHost
import com.tvbrowser.core.webview.PageProtectionStats
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabWebViewClient
import com.tvbrowser.core.webview.TabWebViewFactory
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Real WebView requests and JavaScript, with every HTTP response owned by this fixture. */
@RunWith(AndroidJUnit4::class)
class AdblockWebViewTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun bundledProtectionBlocksAnAdScriptWhileAllowedScriptAndMainDocumentLoad() = withFixture { fixture ->
        navigate(fixture, "https://pebble.invalid/fixture")
        assertProtected(fixture)

        // A host in the blocklist must remain directly navigable, rather than showing
        // an unexplained empty main document. Its ad subresources are still filtered.
        navigate(fixture, "https://doubleclick.net/fixture")
        assertProtected(fixture)
        assertEquals("main document loaded", metrics(fixture).getString("body"))
    }

    @Test
    fun temporaryPauseAllowsRequestsAndResumeRestoresBlocking() = withFixture { fixture ->
        navigate(fixture, "https://pebble.invalid/fixture")
        assertProtected(fixture)
        instrumentation.runOnMainSync { fixture.client.setProtectionPaused(true) }
        reload(fixture)
        assertTrue(stats(fixture).paused)
        assertEquals(0, stats(fixture).blockedRequests)
        assertEquals(1, metrics(fixture).getInt("adRuns"))
        assertEquals(1, metrics(fixture).getInt("allowedRuns"))

        instrumentation.runOnMainSync { fixture.client.setProtectionPaused(false) }
        reload(fixture)
        assertProtected(fixture)
        assertFalse(stats(fixture).paused)
    }

    @Test
    fun reloadResetsDocumentCountsAndCrossHostNavigationClearsTemporaryPause() = withFixture { fixture ->
        navigate(fixture, "https://pebble.invalid/fixture")
        assertProtected(fixture)
        evaluate(fixture.browser, """
            window.extraSettled = false;
            var extra = document.createElement('script');
            extra.src = 'https://doubleclick.net/ad.js?extra=${UUID.randomUUID()}';
            extra.onload = extra.onerror = function() { window.extraSettled = true; };
            document.head.appendChild(extra);
            true;
        """.trimIndent())
        await("The second filtered resource must settle") {
            evaluate(fixture.browser, "window.extraSettled") == true && stats(fixture).blockedRequests == 2
        }
        reload(fixture)
        assertProtected(fixture)

        instrumentation.runOnMainSync { fixture.client.setProtectionPaused(true) }
        assertTrue(stats(fixture).paused)
        navigate(fixture, "https://other.pebble.invalid/fixture")
        assertFalse("A visit exception must not leak to another host", stats(fixture).paused)
        assertProtected(fixture)
    }

    @Test
    fun changingTheBlockingDefaultReachesRealSubresourceRequests() = withFixture { fixture ->
        navigate(fixture, "https://pebble.invalid/fixture")
        assertProtected(fixture)
        fixture.defaultProtection.set(false)
        reload(fixture)
        assertEquals(1, metrics(fixture).getInt("adRuns"))
        assertEquals(0, stats(fixture).blockedRequests)

        fixture.defaultProtection.set(true)
        reload(fixture)
        assertProtected(fixture)
    }

    @Test
    fun unpreparedCrossSiteNavigationAppliesTheDestinationPolicy() = withFixture(
        siteProtection = { normalizedSiteHost(it) != "allowed.pebble.invalid" },
    ) { fixture ->
        navigate(fixture, "https://pebble.invalid/fixture")
        assertProtected(fixture)

        navigate(fixture, "https://allowed.pebble.invalid/fixture", prepare = false)
        assertEquals(1, metrics(fixture).getInt("adRuns"))
        assertEquals(0, stats(fixture).blockedRequests)

        navigate(fixture, "https://pebble.invalid/fixture", prepare = false)
        assertProtected(fixture)
    }

    @Test
    fun cachedRulesRetainThirdPartyRestrictionsAndWebsiteExceptionsInRealWebViewRequests() = withFixture(
        adRules = """
            [Adblock Plus 2.0]
            ||doubleclick.net^${'$'}third-party
            @@||doubleclick.net^${'$'}domain=allowed.pebble.invalid
        """.trimIndent(),
    ) { fixture ->
        navigate(fixture, "https://doubleclick.net/fixture")
        assertEquals("A third-party-only rule must allow a first-party script", 1, metrics(fixture).getInt("adRuns"))
        assertEquals(1, metrics(fixture).getInt("allowedRuns"))
        assertEquals(0, stats(fixture).blockedRequests)

        navigate(fixture, "https://pebble.invalid/fixture")
        assertProtected(fixture)

        navigate(fixture, "https://allowed.pebble.invalid/fixture")
        assertEquals("The cached website-scoped exception must override the block", 1, metrics(fixture).getInt("adRuns"))
        assertEquals(1, metrics(fixture).getInt("allowedRuns"))
        assertEquals(0, stats(fixture).blockedRequests)
    }

    private class EventTrace {
        private val events = ArrayDeque<String>()
        @Synchronized fun record(event: String) {
            events.addLast("${SystemClock.elapsedRealtime()} [${Thread.currentThread().name}] $event")
            if (events.size > 80) events.removeFirst()
        }
        @Synchronized fun dump(): String = events.joinToString("\n")
    }

    private data class Fixture(
        val browser: WebView,
        val client: TabWebViewClient,
        val trace: EventTrace,
        val defaultProtection: AtomicBoolean,
    )

    private fun withFixture(
        adRules: String? = null,
        siteProtection: (String) -> Boolean = { true },
        check: (Fixture) -> Unit,
    ) {
        lateinit var fixture: Fixture
        lateinit var directory: File
        lateinit var loader: EasyListLoader
        activity.scenario.onActivity { host ->
            directory = File(host.cacheDir, "adblock-webview-${UUID.randomUUID()}").apply { mkdirs() }
            // Use a private fixture directory so real subscription caches cannot
            // change the rules under this test. This loader initializes from disk only;
            // it never schedules or performs a network refresh.
            if (adRules != null) {
                writeSnapshot(directory, "easylist.snapshot", adRules)
                writeSnapshot(directory, "easyprivacy.snapshot", "[Adblock Plus 2.0]\n||unrelated-tracker.invalid^")
            }
            val isolatedContext = object : ContextWrapper(host.applicationContext) {
                override fun getFilesDir(): File = directory
            }
            loader = EasyListLoader(isolatedContext)
            val defaultProtection = AtomicBoolean(true)
            val browser = TabWebViewFactory.create(
                host, TabConfig(), AdBlockInterceptor(loader), host.videoController,
                protectionEnabledForPage = { defaultProtection.get() && siteProtection(it) },
            )
            browser.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            val client = TabWebViewFactory.clientOf(browser)
            val generation = AtomicInteger()
            val trace = EventTrace()
            fun record(event: String) = trace.record("$event; page=${client.protectionPageUrl()}; stats=${client.protectionStats()}")
            browser.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    record("onPageStarted before $url")
                    client.onPageStarted(view, url, favicon)
                    record("onPageStarted after $url")
                }
                override fun onPageCommitVisible(view: WebView, url: String) {
                    record("onPageCommitVisible $url")
                    client.onPageCommitVisible(view, url)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    record("onPageFinished before $url")
                    client.onPageFinished(view, url)
                    record("onPageFinished after $url")
                }
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    // Exercise the actual app interception path before serving a response.
                    record("intercept before main=${request.isForMainFrame} ${request.url}")
                    val filtered = client.shouldInterceptRequest(view, request)
                    record("intercept after blocked=${filtered != null} main=${request.isForMainFrame} ${request.url}")
                    filtered?.let { return it }
                    if (request.isForMainFrame) {
                        return response("text/html", document(generation.incrementAndGet()))
                    }
                    return when (request.url.path) {
                        "/allowed.js" -> response("application/javascript", "window.allowedRuns += 1;")
                        "/ad.js" -> response("application/javascript", "window.adRuns += 1;")
                        else -> response("text/plain", "")
                    }
                }
            }
            val root = FrameLayout(host)
            root.addView(browser, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            host.setContentView(root)
            fixture = Fixture(browser, client, trace, defaultProtection)
        }
        try {
            if (adRules != null) await("The controlled subscription snapshots must initialize") {
                val status = loader.status.value
                status.ready && status.lists.all { !it.bundled }
            }
            check(fixture)
        } catch (error: Throwable) {
            android.util.Log.e("PebbleAdblockFixture", fixture.trace.dump())
            throw error
        } finally {
            instrumentation.runOnMainSync {
                fixture.browser.stopLoading()
                (fixture.browser.parent as? ViewGroup)?.removeView(fixture.browser)
                fixture.browser.destroy()
            }
            directory.deleteRecursively()
        }
    }

    /** Exercise the production cache boundary without exposing a test-only engine hook. */
    private fun writeSnapshot(directory: File, name: String, text: String) {
        val sources = File(directory, "adblock/sources-v1").apply { mkdirs() }
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        File(sources, name).writeText("PEBBLE-FILTER-1\n1000\n$digest\n$text", Charsets.UTF_8)
    }

    private fun document(generation: Int): String = """
        <!doctype html><html><head><meta name='viewport' content='width=device-width'><link rel='icon' href='data:,'>
        <script>window.fixtureGeneration=$generation; window.allowedRuns=0; window.adRuns=0;</script>
        <script src='https://assets.pebble.invalid/allowed.js?generation=$generation'></script>
        <script src='https://doubleclick.net/ad.js?generation=$generation'></script>
        </head><body><p id='fixture'>main document loaded</p></body></html>
    """.trimIndent()

    private fun response(type: String, text: String) = WebResourceResponse(type, "UTF-8", 200, "OK",
        mapOf("Cache-Control" to "no-store"), text.byteInputStream())

    private fun navigate(fixture: Fixture, url: String, prepare: Boolean = true) {
        val before = metrics(fixture).optInt("generation")
        instrumentation.runOnMainSync {
            if (prepare) fixture.client.prepareProtection(url)
            fixture.trace.record("load $url; prepared=$prepare; stats=${fixture.client.protectionStats()}")
            fixture.browser.loadUrl(url)
        }
        awaitLoaded(fixture, before)
    }

    private fun reload(fixture: Fixture) {
        val before = metrics(fixture).optInt("generation")
        instrumentation.runOnMainSync {
            fixture.trace.record("reload requested; page=${fixture.client.protectionPageUrl()}; stats=${fixture.client.protectionStats()}")
            fixture.browser.reload()
        }
        awaitLoaded(fixture, before)
    }

    private fun awaitLoaded(fixture: Fixture, previousGeneration: Int) =
        await("The owned HTML document and its resources must finish loading") {
            val state = metrics(fixture)
            state.optBoolean("complete") && state.optInt("generation") > previousGeneration
        }

    private fun assertProtected(fixture: Fixture) {
        val state = metrics(fixture)
        assertEquals("The allowed resource must execute\n${fixture.trace.dump()}", 1, state.getInt("allowedRuns"))
        assertEquals("The filtered resource must not execute\n${fixture.trace.dump()}", 0, state.getInt("adRuns"))
        assertEquals("Only the ad subresource must increment the document counter\n${fixture.trace.dump()}",
            1, stats(fixture).blockedRequests)
    }

    private fun stats(fixture: Fixture): PageProtectionStats {
        val result = AtomicReference<PageProtectionStats>()
        instrumentation.runOnMainSync { result.set(fixture.client.protectionStats()) }
        return result.get()
    }

    private fun metrics(fixture: Fixture): JSONObject {
        val value = evaluate(fixture.browser, """
            JSON.stringify({complete:document.readyState==='complete', generation:window.fixtureGeneration||0,
                allowedRuns:window.allowedRuns||0, adRuns:window.adRuns||0,
                body:document.getElementById('fixture') ? document.getElementById('fixture').textContent : ''})
        """.trimIndent())
        return if (value is String) JSONObject(value) else JSONObject()
    }

    private fun evaluate(browser: WebView, script: String): Any? {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        instrumentation.runOnMainSync { browser.evaluateJavascript(script) { result.set(it); callback.countDown() } }
        assertTrue("WebView protection probe timed out", callback.await(5, TimeUnit.SECONDS))
        return JSONTokener(result.get() ?: "null").nextValue()
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue(message, condition())
    }
}
