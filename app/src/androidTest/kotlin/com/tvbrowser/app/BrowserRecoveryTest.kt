package com.tvbrowser.app

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.tvbrowser.core.webview.TabWebViewFactory
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Feed platform failure callbacks to the real client; fixtures never need a live server. */
@RunWith(AndroidJUnit4::class)
class BrowserRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun failedImageDoesNotReplaceAnOtherwiseUsablePageWithAnErrorPanel() {
        val browser = openFixture()
        compose.runOnUiThread {
            TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(false), httpFailure())
        }
        compose.waitForIdle()
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        assertEquals("usable", evaluate(browser, "document.getElementById('fixture').textContent"))
    }

    @Test
    fun oldMainFrameFailureCannotReplaceTheNewDocument() {
        val browser = openFixture()
        compose.runOnUiThread {
            TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser,
                Request(true, "https://previous.pebble.invalid/"), httpFailure())
        }
        compose.waitForIdle()
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        assertEquals("usable", evaluate(browser, "document.getElementById('fixture').textContent"))
    }

    @Test
    fun authenticationResponseKeepsTheWebsiteContentAccessible() {
        val browser = openFixture()
        compose.runOnUiThread {
            for (status in listOf(401, 403, 404, 429)) {
                TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(true),
                    WebResourceResponse("text/html", "UTF-8", status, "Website response", emptyMap(), "Login page".byteInputStream()))
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        assertEquals("usable", evaluate(browser, "document.getElementById('fixture').textContent"))
    }

    @Test
    fun mainFrameHttpFailureOffersRetryAndReloadsTheOriginalAddress() {
        val browser = openFixture()
        evaluate(browser, "window.reloadProbe = 'changed'; true")
        compose.runOnUiThread {
            TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(true), httpFailure())
        }
        waitForError()
        compose.onNodeWithTag("browser-error-retry").assertIsDisplayed()
        compose.onNodeWithTag("browser-error-edit").assertIsDisplayed()
        compose.onNodeWithTag("browser-error-home").assertIsDisplayed()
        compose.onNodeWithTag("browser-error-retry").performClick()
        compose.waitUntil(10_000) {
            evaluate(browser, "document.readyState === 'complete' && typeof window.reloadProbe === 'undefined'") == true
        }
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        assertEquals("Retry must preserve the failed address", "about:blank", evaluate(browser, "location.href"))
    }

    @Test
    fun mainFrameNetworkFailureShowsTheSameRecoverableNativeActions() {
        val browser = openFixture()
        compose.runOnUiThread {
            // .invalid is reserved and cannot resolve to a public website. This exercises
            // the platform network-error callback without a server or SSL bypass.
            browser.loadUrl("https://pebble.invalid/offline-fixture")
        }
        waitForError()
        compose.onNodeWithTag("browser-error-edit").assertIsDisplayed()
        compose.onNodeWithTag("browser-error-home").assertIsDisplayed()
        compose.onNodeWithTag("top-nav-browser").assertIsDisplayed()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun rendererLossKeepsNativeNavigationAndCreatesAFreshViewOnlyAfterRetry() {
        val oldBrowser = openFixture()
        compose.runOnUiThread {
            val consumed = TabWebViewFactory.clientOf(oldBrowser).onRenderProcessGone(
                oldBrowser,
                object : RenderProcessGoneDetail() {
                    override fun didCrash() = true
                    override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
                },
            )
            assertTrue("The client must handle renderer termination instead of propagating it", consumed)
        }
        waitForError()
        compose.onNodeWithTag("top-nav-browser").assertIsDisplayed()
        compose.runOnUiThread {
            assertTrue("A dead view must be detached before it can be replaced", oldBrowser.parent == null)
            assertTrue("Recovery must wait for explicit Retry", findWebView(compose.activity.window.decorView) == null)
        }
        compose.onNodeWithTag("browser-error-retry").performClick()
        val replacement = waitForBrowser()
        assertNotSame("A terminated WebView cannot be reused", oldBrowser, replacement)
        compose.waitUntil(10_000) { evaluate(replacement, "document.readyState === 'complete'") == true }
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        assertEquals("about:blank", evaluate(replacement, "location.href"))
    }

    private fun openFixture(): WebView {
        compose.onNodeWithTag("top-nav-browser").performClick()
        val browser = waitForBrowser()
        compose.runOnUiThread {
            browser.loadDataWithBaseURL("about:blank", """
                <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body><main id="fixture">usable</main></body></html>
            """.trimIndent(), "text/html", "UTF-8", null)
        }
        compose.waitUntil(10_000) {
            evaluate(browser, "document.readyState === 'complete' && !!document.getElementById('fixture')") == true
        }
        return browser
    }

    private fun waitForBrowser(): WebView {
        var found: WebView? = null
        compose.waitUntil(10_000) {
            compose.runOnUiThread { found = findWebView(compose.activity.window.decorView) }
            found != null
        }
        return checkNotNull(found)
    }

    private fun waitForError() {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("browser-page-error").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("browser-page-error").assertIsDisplayed()
    }

    private fun evaluate(browser: WebView, script: String): Any? {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        compose.runOnUiThread { browser.evaluateJavascript(script) { result.set(it); callback.countDown() } }
        assertTrue("WebView probe timed out", callback.await(5, TimeUnit.SECONDS))
        return JSONTokener(result.get() ?: "null").nextValue()
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findWebView(view.getChildAt(i))?.let { return it }
        return null
    }

    private fun httpFailure() = WebResourceResponse(
        "text/html", "UTF-8", 503, "Service Unavailable", emptyMap(), "Unavailable".byteInputStream(),
    )

    private class Request(private val mainFrame: Boolean, private val url: String = "about:blank") : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = mainFrame
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }
}
