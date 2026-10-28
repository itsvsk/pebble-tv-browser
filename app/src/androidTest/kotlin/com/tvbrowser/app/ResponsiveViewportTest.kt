package com.tvbrowser.app

import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tvbrowser.core.adblock.AdBlockInterceptor
import com.tvbrowser.core.webview.TabConfig
import com.tvbrowser.core.webview.TabWebViewFactory
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ResponsiveViewportTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun articleGalleryFormAndVideoAdaptToResizedNativeViewports() {
        lateinit var container: FrameLayout
        lateinit var browser: WebView
        activity.scenario.onActivity { host ->
            val root = FrameLayout(host)
            container = FrameLayout(host)
            root.addView(container, FrameLayout.LayoutParams(720, 480))
            host.setContentView(root)
            browser = TabWebViewFactory.create(
                host,
                TabConfig(desktopUa = false, adblockEnabled = false),
                AdBlockInterceptor(host.applicationContext),
                host.videoController,
            )
            container.addView(browser)
            browser.loadDataWithBaseURL(
                "https://pebble.invalid/responsive-viewport-fixture",
                fixture,
                "text/html", "UTF-8", null,
            )
        }
        try {
            waitUntil("The responsive fixture did not finish loading") {
                metrics(browser, allowUninitializedDocument = true).optBoolean("ready")
            }
            // The final step verifies shrinking after expansion, not just the first layout.
            for ((width, height) in listOf(720 to 480, 1280 to 720, 1920 to 1080, 720 to 480)) {
                instrumentation.runOnMainSync {
                    container.layoutParams = FrameLayout.LayoutParams(width, height)
                }
                waitUntil("WebView did not adopt the native ${width}x$height bounds") {
                    var matches = false
                    instrumentation.runOnMainSync { matches = browser.width == width && browser.height == height }
                    matches
                }
                awaitVisualUpdate(browser)
                val state = metrics(browser)
                val description = "native=${width}x$height, CSS=$state"
                assertTrue("The fixture must remain loaded after resizing: $description", state.getBoolean("ready"))
                val ratio = state.getDouble("dpr")
                assertTrue("Device pixel ratio must be positive: $description", ratio > 0)
                assertEquals("CSS width must track the bounded native viewport: $description",
                    width / ratio, state.getDouble("width"), 1.5)
                assertEquals("CSS height must track the bounded native viewport: $description",
                    height / ratio, state.getDouble("height"), 1.5)
                assertEquals("100vh must resize with the native viewport: $description",
                    height / ratio, state.getDouble("vh"), 1.5)
                assertTrue("Responsive content must not create horizontal document overflow: $description",
                    state.getDouble("scrollWidth") <= state.getDouble("width") + 1.5)
                assertTrue("Article content must remain within the viewport: $description",
                    state.getDouble("articleWidth") <= state.getDouble("width"))
                assertTrue("Form controls must remain inside the article: $description", state.getBoolean("formFits"))
                assertTrue("Responsive images must remain inside their tiles: $description", state.getBoolean("imagesFit"))
                assertTrue("Video surface must have a positive bounded size: $description",
                    state.getDouble("videoWidth") > 0 && state.getDouble("videoHeight") > 0 &&
                        state.getDouble("videoWidth") <= state.getDouble("articleWidth") + 1)
                assertEquals("The responsive player must preserve its 16:9 surface: $description",
                    16.0 / 9.0, state.getDouble("videoWidth") / state.getDouble("videoHeight"), 0.02)
            }
        } finally {
            instrumentation.runOnMainSync {
                (browser.parent as? ViewGroup)?.removeView(browser)
                browser.destroy()
            }
        }
    }

    private fun metrics(browser: WebView, allowUninitializedDocument: Boolean = false): JSONObject {
        val result = AtomicReference<String>()
        val callback = CountDownLatch(1)
        instrumentation.runOnMainSync {
            browser.evaluateJavascript("""
                (function() {
                    var article = document.getElementById('article');
                    var probe = document.getElementById('vh-probe');
                    var player = document.getElementById('player');
                    var form = document.getElementById('form');
                    var images = document.querySelectorAll('.tile img');
                    var imagesReady = images.length === 3, imagesFit = imagesReady;
                    for (var i = 0; i < images.length; i++) {
                        var image = images[i], imageBox = image.getBoundingClientRect();
                        var tile = image.parentElement.getBoundingClientRect();
                        imagesReady = imagesReady && image.complete && image.naturalWidth > 0;
                        imagesFit = imagesFit && imageBox.width > 0 && imageBox.left >= tile.left - 1 && imageBox.right <= tile.right + 1;
                    }
                    var formFits = !!form;
                    if (form) {
                        var formBox = form.getBoundingClientRect();
                        var controls = form.querySelectorAll('input, textarea, select, button');
                        for (var j = 0; j < controls.length; j++) {
                            var r = controls[j].getBoundingClientRect();
                            formFits = formFits && r.width > 0 && r.left >= formBox.left - 1 && r.right <= formBox.right + 1;
                        }
                    }
                    var videoBox = player ? player.getBoundingClientRect() : {width: 0, height: 0};
                    return JSON.stringify({
                        ready: document.readyState === 'complete' && !!article && !!probe && !!player && imagesReady,
                        width: innerWidth, height: innerHeight, dpr: devicePixelRatio || 1,
                        vh: probe ? probe.getBoundingClientRect().height : 0,
                        scrollWidth: document.documentElement.scrollWidth,
                        articleWidth: article ? article.getBoundingClientRect().width : 0,
                        videoWidth: videoBox.width, videoHeight: videoBox.height,
                        formFits: formFits, imagesFit: imagesFit
                    });
                })()
            """.trimIndent()) { value -> result.set(value); callback.countDown() }
        }
        assertTrue("WebView JavaScript callback timed out", callback.await(5, TimeUnit.SECONDS))
        val raw = result.get()
        val encoded = if (raw == null) JSONObject.NULL else JSONTokener(raw).nextValue()
        // A newly navigating WebView may briefly have no JavaScript document. Only the
        // initial readiness poll accepts that state; malformed or later results still fail.
        if (allowUninitializedDocument && encoded === JSONObject.NULL) return JSONObject().put("ready", false)
        assertTrue("Expected JSON-stringified viewport metrics, received: $raw", encoded is String)
        return JSONObject(encoded as String)
    }

    private fun awaitVisualUpdate(browser: WebView) {
        val drawn = CountDownLatch(1)
        instrumentation.runOnMainSync {
            browser.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) {
                    browser.postOnAnimation { browser.postOnAnimation { drawn.countDown() } }
                }
            })
        }
        assertTrue("Resized content must be rendered before measuring it", drawn.await(5, TimeUnit.SECONDS))
    }

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue(message, condition())
    }

    private val fixture = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            * { box-sizing: border-box; }
            html, body { margin: 0; width: 100%; font: 16px sans-serif; }
            main { width: 100%; padding: 16px; }
            article { width: 100%; max-width: 960px; margin: 0 auto; }
            p { line-height: 1.5; overflow-wrap: break-word; word-wrap: break-word; }
            .image-grid { font-size: 0; }
            .tile { display: inline-block; vertical-align: top; width: 100%; margin: 0; padding: 4px; font-size: 16px; }
            img { display: block; width: 100%; max-width: 100%; height: auto; }
            @media (min-width: 480px) { .tile { width: 50%; } }
            @media (min-width: 900px) { .tile { width: 33.333%; } }
            @supports (display: grid) {
                .image-grid { display: grid; grid-template-columns: minmax(0, 1fr); grid-gap: 8px; font-size: 16px; }
                .tile { display: block; width: auto; padding: 0; }
                @media (min-width: 480px) { .image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
                @media (min-width: 900px) { .image-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
            }
            form, input, textarea, select, button { width: 100%; max-width: 100%; min-width: 0; font: inherit; }
            label { display: block; margin-bottom: 8px; }
            .video-frame { position: relative; width: 100%; height: 0; padding-bottom: 56.25%; }
            video { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: contain; }
            #vh-probe { position: fixed; top: 0; left: 0; width: 1px; height: 100vh; opacity: 0; pointer-events: none; }
        </style></head><body><main><article id="article">
        <h1>Responsive article, gallery, form and video</h1>
        <p>A normal article should wrap naturally as the browser viewport becomes narrower or wider.
        https://pebble.invalid/a-very-long-address-with-a-continuous-segment-abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789</p>
        <div class="image-grid">
        <figure class="tile"><img alt="Offline sample one" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='640' height='360'%3E%3Crect width='640' height='360' fill='teal'/%3E%3C/svg%3E"></figure>
        <figure class="tile"><img alt="Offline sample two" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='640' height='360'%3E%3Crect width='640' height='360' fill='navy'/%3E%3C/svg%3E"></figure>
        <figure class="tile"><img alt="Offline sample three" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='640' height='360'%3E%3Crect width='640' height='360' fill='seagreen'/%3E%3C/svg%3E"></figure>
        </div>
        <form id="form"><label>Address<input type="text" placeholder="Search or enter a URL"></label>
        <label>Comment<textarea rows="2"></textarea></label><select><option>Public fixture option</option></select>
        <button type="button">Local action</button></form>
        <div class="video-frame"><video id="player" controls preload="none"></video></div>
        </article></main><div id="vh-probe"></div></body></html>
    """.trimIndent()
}
