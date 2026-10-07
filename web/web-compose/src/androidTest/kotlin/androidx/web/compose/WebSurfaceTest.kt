/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.web.compose

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WebSurfaceTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var webContent: WebContent

    @Before
    fun setUp() {
        assumeTrue(WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT))
        assumeTrue(WebFeature.isFeatureSupported(WebFeature.WEB_SURFACE))
        webContent = WebContent()
    }

    @After
    fun tearDown() {
        composeTestRule.activityRule.scenario.close()
        if (::webContent.isInitialized) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { webContent.close() }
        }
    }

    @Test
    fun testRendersAndInvalidatesOnDomChange() {
        lateinit var bridge: WebViewBridge
        composeTestRule.setContent {
            WebSurface(
                content = webContent,
                modifier = Modifier.size(150.dp).testTag("surface"),
                bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
            )
        }

        bridge.loadHtml(
            """
            <html>
            <body style='margin:0;background:rgb(255,0,0);'></body>
            </html>
            """
        )

        val location = IntArray(2)
        composeTestRule.runOnIdle { bridge.getLocationOnScreen(location) }
        val centerX = location[0] + bridge.width / 2
        val centerY = location[1] + bridge.height / 2

        waitForPixelToBe(centerX, centerY) { it == Color.RED }

        bridge.evaluateJs("document.body.style.background = 'rgb(0,255,0)';")

        waitForPixelToBe(centerX, centerY) { it == Color.GREEN }
    }

    @Test
    fun testAttachesAndDetachesWithComposition() {
        lateinit var bridge: WebViewBridge
        var released = false
        var visible by mutableStateOf(true)

        composeTestRule.setContent {
            if (visible) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(150.dp),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                    bridgeRelease = { released = true },
                )
            }
        }

        composeTestRule.runOnIdle {
            // Trigger synchronous Chromium startup so queued WebView initialization completes
            // before WebContent is closed in tearDown.
            bridge.settings
            assertTrue(bridge.isAttachedToWindow)
            assertFalse(released)
            assertThrows(IllegalStateException::class.java) { webContent.detach() }
        }

        composeTestRule.runOnIdle { visible = false }

        composeTestRule.runOnIdle {
            assertFalse(bridge.isAttachedToWindow)
            assertTrue(released)
            assertFalse(webContent.isAttached())
        }
    }

    @Test
    fun testUpdatesViewportOnResize() {
        lateinit var bridge: WebViewBridge
        var size by mutableStateOf(100.dp)
        composeTestRule.setContent {
            WebSurface(
                content = webContent,
                modifier = Modifier.size(size),
                bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
            )
        }

        bridge.loadHtml(
            """
            <html>
            <head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            </html>
            """
        )

        composeTestRule.waitUntil(TIMEOUT_MS) {
            abs((bridge.evaluateJs("window.innerWidth").toIntOrNull() ?: 0) - 100) <= 2
        }

        composeTestRule.runOnIdle { size = 200.dp }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            abs((bridge.evaluateJs("window.innerWidth").toIntOrNull() ?: 0) - 200) <= 2
        }
    }

    @Test
    fun testClickInputWithOffset() {
        lateinit var bridge: WebViewBridge
        composeTestRule.setContent {
            Box(modifier = Modifier.padding(start = 50.dp, top = 80.dp)) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(100.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(
            """
            <html>
            <head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            <body style='margin:0;'
                  onclick='window.clickX = Math.round(event.clientX);
                           window.clickY = Math.round(event.clientY);'>
                <div style='width:100vw;height:100vh;'></div>
            </body>
            </html>
            """
        )

        composeTestRule.onNodeWithTag("surface").performClick()

        composeTestRule.waitUntil(TIMEOUT_MS) { bridge.evaluateJs("window.clickX") != "null" }
        assertTrue(abs(bridge.evaluateJs("window.clickX").toInt() - 50) <= 3)
        assertTrue(abs(bridge.evaluateJs("window.clickY").toInt() - 50) <= 3)
    }

    @Test
    fun testImeInput() {
        val inputConnectionLatch = CountDownLatch(1)
        lateinit var bridge: TestWebViewBridge
        composeTestRule.setContent {
            WebSurface(
                content = webContent,
                modifier = Modifier.size(200.dp).testTag("surface"),
                bridgeFactory = { ctx ->
                    TestWebViewBridge(ctx)
                        .apply {
                            onCreateInputConnectionCallback = { inputConnectionLatch.countDown() }
                        }
                        .also { bridge = it }
                },
            )
        }

        bridge.loadHtml(
            """
            <html>
            <body style='margin:0;'>
                <input id='input' style='width:100vw;height:100vh;' />
            </body>
            </html>
            """
        )

        composeTestRule.onNodeWithTag("surface").performClick()
        assertTrue(inputConnectionLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS))

        composeTestRule.runOnIdle {
            assertNotNull(bridge.inputConnection)
            bridge.inputConnection!!.commitText("hello", 1)
        }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            bridge.evaluateJs("document.getElementById('input').value") == "\"hello\""
        }
    }

    private fun waitForPixelToBe(centerX: Int, centerY: Int, predicate: (Int) -> Boolean) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        composeTestRule.waitUntil(TIMEOUT_MS) {
            val pixel = uiAutomation.takeScreenshot().getPixel(centerX, centerY)
            predicate(pixel)
        }
    }

    private fun WebContentView.loadHtml(html: String) = loadHtml(composeTestRule, html)

    companion object {
        internal const val TIMEOUT_MS = 5_000L
    }
}
