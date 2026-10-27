package com.tvbrowser.app

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ExternalAppLinkTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun unsupportedAppRedirectKeepsTheCurrentWebPageInsteadOfShowingASchemeError() {
        compose.onNodeWithTag("top-nav-browser").performClick()
        var browser: WebView? = null
        compose.waitUntil(10_000) {
            compose.runOnUiThread { browser = findWebView(compose.activity.window.decorView) }
            browser != null
        }
        val webView = checkNotNull(browser)
        compose.runOnUiThread {
            webView.loadDataWithBaseURL(
                "https://pebble.invalid/external-app-link-fixture",
                """
                    <!doctype html><html><head>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    </head><body><main id="fixture">Original web page remains visible</main>
                    <p id="after-attempt">Waiting for unsupported redirect</p></body></html>
                """.trimIndent(),
                "text/html", "UTF-8", null,
            )
        }
        compose.waitUntil(10_000) { metrics(webView).optBoolean("ready") }
        evaluate(webView, """
            setTimeout(function() { location.href = 'fb://profile/123'; }, 50);
            setTimeout(function() {
                var marker = document.getElementById('after-attempt');
                if (marker) marker.textContent = 'Navigation attempt finished';
            }, 400);
            JSON.stringify({ scheduled: true });
        """.trimIndent())

        var after = JSONObject()
        compose.waitUntil(10_000) {
            after = metrics(webView)
            after.optBoolean("unsupportedError") || after.optBoolean("afterAttempt")
        }
        assertFalse("An app-only redirect must not replace the page with a WebView error: $after",
            after.getBoolean("unsupportedError"))
        assertTrue("The original web document must remain usable after the redirect: $after",
            after.getBoolean("ready") && after.getBoolean("afterAttempt"))
    }

    private fun metrics(webView: WebView): JSONObject = evaluate(webView, """
        (function() {
            var fixture = document.getElementById('fixture');
            var marker = document.getElementById('after-attempt');
            var body = document.body ? document.body.innerText : '';
            return JSON.stringify({
                ready: document.readyState === 'complete' && !!fixture,
                afterAttempt: !!marker && marker.textContent === 'Navigation attempt finished',
                unsupportedError: body.indexOf('ERR_UNKNOWN_URL_SCHEME') >= 0,
                url: location.href,
                body: body.substring(0, 500)
            });
        })()
    """.trimIndent())

    private fun evaluate(webView: WebView, script: String): JSONObject {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        compose.runOnUiThread {
            webView.evaluateJavascript(script) { value -> result.set(value); callback.countDown() }
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
