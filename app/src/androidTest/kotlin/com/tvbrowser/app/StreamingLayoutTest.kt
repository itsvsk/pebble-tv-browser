package com.tvbrowser.app

import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class StreamingLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun htmlVideoFullscreenCoversTheWindowAndBackReturnsToThePage() {
        compose.onNodeWithTag("top-nav-browser").performClick()
        var browser: WebView? = null
        compose.waitUntil(10_000) {
            compose.runOnUiThread { browser = findWebView(compose.activity.window.decorView) }
            browser != null
        }
        val webView = checkNotNull(browser)
        compose.runOnUiThread {
            webView.loadDataWithBaseURL("https://pebble.invalid/fullscreen-fixture", """
                <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
                <style>html,body,#root {margin:0;width:100%;height:100%} #player {width:100%;height:100%}
                #viewport {position:fixed;height:100vh;width:1px} button {position:fixed;top:0;right:0;bottom:0;left:0}</style></head>
                <body><main id="root"><video id="player" controls></video></main><div id="viewport"></div>
                <button onclick="window.clicked=true; var v=document.getElementById('player'); Promise.resolve((v.requestFullscreen || v.webkitRequestFullscreen).call(v)).then(function(){ window.requestResult='entered'; }, function(e){ window.requestResult=String(e); })">Enter fullscreen</button></body></html>
            """.trimIndent(), "text/html", "UTF-8", null)
        }
        compose.waitUntil(10_000) { evaluate(webView).optBoolean("ready") }
        // DOM readiness can precede removal of the previous native error panel.
        // Wait for both Compose and Chromium to draw before sending a real screen tap.
        compose.waitForIdle()
        compose.onNodeWithTag("browser-page-error").assertDoesNotExist()
        val rendered = CountDownLatch(1)
        compose.runOnUiThread {
            webView.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    webView.postOnAnimation { webView.postOnAnimation { rendered.countDown() } }
                }
            })
        }
        assertTrue("Fullscreen fixture must be rendered before tapping", rendered.await(5, TimeUnit.SECONDS))
        val location = IntArray(2)
        var x = 0f
        var y = 0f
        compose.runOnUiThread {
            webView.getLocationOnScreen(location)
            x = location[0] + webView.width / 2f
            y = location[1] + webView.height / 2f
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            try { instrumentation.sendPointerSync(event) } finally { event.recycle() }
        }
        try {
            compose.waitUntil(10_000) { compose.activity.videoController.fullscreen.value != null }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Fullscreen request diagnostics: ${evaluate(webView)}", failure)
        }
        compose.onNodeWithTag("browser-fullscreen-video").assertIsDisplayed()
        compose.runOnUiThread {
            val view = compose.activity.videoController.fullscreen.value!!.view
            assertTrue("Fullscreen must cover the native top bars", view.height >= compose.activity.window.decorView.height * 0.95)
        }
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithTag("browser-fullscreen-video").assertDoesNotExist()
        compose.waitUntil(10_000) { !evaluate(webView).optBoolean("fullscreen") }
    }

    @Test
    fun viewportAndPercentageHeightPlayersFillTheVisibleBrowser() {
        compose.onNodeWithTag("top-nav-browser").performClick()
        var browser: WebView? = null
        compose.waitUntil(10_000) {
            compose.runOnUiThread { browser = findWebView(compose.activity.window.decorView) }
            browser != null
        }
        val webView = checkNotNull(browser)
        compose.runOnUiThread {
            webView.loadDataWithBaseURL(
                "https://pebble.invalid/streaming-layout-fixture",
                """
                    <!doctype html>
                    <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        html, body, #root { margin: 0; width: 100%; height: 100%; }
                        html, body, #root { display: flex; flex-direction: column; }
                        body { overflow: hidden; }
                        #header { height: 48px; flex: none; }
                        #player { flex: 1; min-height: 0; background: black; }
                        #viewport { position: fixed; height: 100vh; width: 1px; }
                    </style></head><body><main id="root">
                    <header id="header">Offline live-player layout fixture</header>
                    <section id="player">Video surface</section></main><div id="viewport"></div>
                    </body></html>
                """.trimIndent(),
                "text/html", "UTF-8", null,
            )
        }
        var metrics = JSONObject()
        compose.waitUntil(10_000) {
            metrics = evaluate(webView)
            metrics.optBoolean("ready")
        }
        var nativeDescription = ""
        compose.runOnUiThread {
            nativeDescription = "native=${webView.width}x${webView.height}, " +
                "layoutParams=${webView.layoutParams?.width}x${webView.layoutParams?.height}"
        }
        val description = "$nativeDescription, CSS=$metrics"
        val viewportHeight = metrics.getDouble("innerHeight")
        assertTrue("WebView must occupy a positive viewport: $description", viewportHeight > 100)
        assertTrue("100%-height streaming roots must fill the viewport: $description",
            metrics.getDouble("rootHeight") >= viewportHeight * 0.95)
        assertTrue("100vh must match the visible viewport: $description",
            metrics.getDouble("vhHeight") >= viewportHeight * 0.95)
        assertTrue("The player must fill the space below its header: $description",
            metrics.getDouble("playerHeight") >= (viewportHeight - 48) * 0.95)
    }

    private fun evaluate(webView: WebView): JSONObject {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        compose.runOnUiThread {
            webView.evaluateJavascript(
                """
                    JSON.stringify({
                        ready: document.readyState === 'complete' && !!document.getElementById('viewport'),
                        url: location.href, title: document.title, clicked: !!window.clicked, requestResult: window.requestResult || null,
                        fullscreen: !!(document.fullscreenElement || document.webkitFullscreenElement),
                        innerHeight: innerHeight,
                        rootHeight: document.getElementById('root') ? document.getElementById('root').getBoundingClientRect().height : 0,
                        vhHeight: document.getElementById('viewport') ? document.getElementById('viewport').getBoundingClientRect().height : 0,
                        playerHeight: document.getElementById('player') ? document.getElementById('player').getBoundingClientRect().height : 0
                    })
                """.trimIndent(),
            ) { value -> result.set(value); callback.countDown() }
        }
        assertTrue("WebView JavaScript callback timed out", callback.await(5, TimeUnit.SECONDS))
        return JSONObject(JSONTokener(result.get()).nextValue() as String)
    }

    private fun findWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findWebView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
