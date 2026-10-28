package com.tvbrowser.app

import android.net.Uri
import android.view.View
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceError
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.tvbrowser.core.webview.TabWebViewFactory
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.net.ServerSocket

/** Feed platform failure callbacks to the real client; fixtures never need a live server. */
@RunWith(AndroidJUnit4::class)
class BrowserRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun failedImageDoesNotReplaceAnOtherwiseUsablePageWithAnErrorPanel() {
        val browser = openFixture()
        compose.runOnUiThread {
            TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(false, currentAddress(browser)), httpFailure())
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
    fun failedProvisionalNavigationShowsRecoveryWhileNativeUrlStillNamesThePreviousPage() {
        val browser = openFixture()
        val pendingUrl = "https://pending.pebble.invalid/article"
        compose.runOnUiThread {
            val client = TabWebViewFactory.clientOf(browser)
            client.onPageStarted(browser, pendingUrl, null)
            client.onReceivedHttpError(browser, Request(true, pendingUrl), httpFailure())
        }
        waitForError()
        compose.onNodeWithTag("browser-error-retry").assertIsDisplayed()
        compose.onNodeWithTag("browser-error-edit").assertIsDisplayed()
    }

    @Test
    fun previousPageFailureCannotReplaceANewerPendingNavigation() {
        val browser = openFixture()
        val previousUrl = currentAddress(browser)
        compose.runOnUiThread {
            val client = TabWebViewFactory.clientOf(browser)
            client.onPageStarted(browser, "https://pending.pebble.invalid/new-page", null)
            client.onReceivedHttpError(browser, Request(true, previousUrl), httpFailure())
        }
        compose.waitForIdle()
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
    }

    @Test
    fun authenticationResponseKeepsTheWebsiteContentAccessible() {
        val browser = openFixture()
        compose.runOnUiThread {
            for (status in listOf(401, 403, 404, 429)) {
                TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(true, currentAddress(browser)),
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
        val failedAddress = currentAddress(browser)
        evaluate(browser, "window.reloadProbe = 'changed'; true")
        compose.runOnUiThread {
            TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(true, currentAddress(browser)), httpFailure())
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
        assertEquals("Retry must preserve the failed native address", failedAddress, currentAddress(browser))
    }

    @Test
    fun downFromErrorAddressEditorReturnsToRetryWithoutReactivatingThePointer() {
        val browser = openFixture()
        evaluate(browser, "window.reloadProbe = 'changed'; true")
        compose.runOnUiThread {
            TabWebViewFactory.clientOf(browser).onReceivedHttpError(browser, Request(true, currentAddress(browser)), httpFailure())
        }
        waitForError()
        compose.onNodeWithTag("browser-error-edit").performClick()
        compose.onNodeWithTag("browser-address-editor", useUnmergedTree = true).assertIsFocused()
        // Exercise the Activity remote path after IME handling; a visible keyboard
        // legitimately consumes D-pad input before the Activity receives it.
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
        }
        compose.waitForIdle()
        compose.runOnUiThread {
            assertFalse("Error actions must retain remote input", compose.activity.cursorController.state.value.active)
        }
        compose.onNodeWithTag("browser-error-retry").assertIsFocused()
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        }
        compose.waitUntil(10_000) {
            evaluate(browser, "document.readyState === 'complete' && typeof window.reloadProbe === 'undefined'") == true
        }
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
    }

    @Test
    fun mainFrameNetworkFailureShowsTheSameRecoverableNativeActions() {
        val browser = openFixture()
        val closedPort = ServerSocket(0).use { it.localPort }
        val events = mutableListOf<String>()
        compose.runOnUiThread {
            val client = TabWebViewFactory.clientOf(browser)
            browser.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    events += "start=$url native=${view.url}"
                    client.onPageStarted(view, url, favicon)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    events += "finish=$url native=${view.url}"
                    client.onPageFinished(view, url)
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    events += "error=${request.url} main=${request.isForMainFrame} code=${error.errorCode} native=${view.url}"
                    client.onReceivedError(view, request, error)
                }
            }
            // A refused local connection exercises the platform callback without waiting
            // for external DNS or accepting an untrusted TLS certificate.
            browser.loadUrl("https://127.0.0.1:$closedPort/offline-fixture")
        }
        try {
            waitForError()
        } catch (failure: Throwable) {
            var trace = ""
            compose.runOnUiThread { trace = events.joinToString("; ") }
            throw AssertionError("Native recovery callbacks: $trace", failure)
        }
        compose.onNodeWithTag("browser-error-edit").assertIsDisplayed()
        compose.onNodeWithTag("browser-error-home").assertIsDisplayed()
        compose.onNodeWithTag("top-nav-browser").assertIsDisplayed()
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun rendererLossKeepsNativeNavigationAndCreatesAFreshViewOnlyAfterRetry() {
        val oldBrowser = openFixture()
        val failedAddress = currentAddress(oldBrowser)
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
        assertEquals(failedAddress, currentAddress(replacement))
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
        compose.waitUntil(10_000) {
            var finished = false
            compose.runOnUiThread { finished = !browser.url.isNullOrBlank() && browser.progress == 100 }
            finished
        }
        compose.waitForIdle()
        return browser
    }

    private fun currentAddress(browser: WebView): String {
        var url: String? = null
        compose.runOnUiThread { url = browser.url }
        return checkNotNull(url)
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

    private class Request(private val mainFrame: Boolean, private val url: String) : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = mainFrame
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }
}
