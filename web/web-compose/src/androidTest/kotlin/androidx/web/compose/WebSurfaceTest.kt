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
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.web.WebContent
import androidx.web.WebContentView
import androidx.web.WebFeature
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testReusedSurfaceLaysOutNewBridge() {
        var bridge: WebViewBridge? = null
        var key by mutableStateOf(0)
        composeTestRule.setContent {
            // A new key reuses the layout node: the surface is detached and attached again, at the
            // same size, and gets a new bridge.
            ReusableContent(key) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(150.dp),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        lateinit var firstBridge: WebViewBridge
        var width = 0
        var height = 0
        composeTestRule.runOnIdle {
            firstBridge = checkNotNull(bridge)
            // Trigger synchronous Chromium startup so queued WebView initialization completes
            // before WebContent is closed in tearDown.
            firstBridge.settings
            width = firstBridge.width
            height = firstBridge.height
            assertTrue(width > 0 && height > 0)
        }

        composeTestRule.runOnIdle { key++ }

        composeTestRule.runOnIdle {
            val reusedBridge = checkNotNull(bridge)
            reusedBridge.settings
            assertTrue(reusedBridge !== firstBridge)
            assertEquals(width, reusedBridge.width)
            assertEquals(height, reusedBridge.height)
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

    @Test
    @Suppress("DEPRECATION")
    fun testAnchorViewsInBridgeAreLaidOut() {
        lateinit var bridge: WebViewBridge
        composeTestRule.setContent {
            WebSurface(
                content = webContent,
                modifier = Modifier.size(150.dp),
                bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
            )
        }
        bridge.loadHtml("<html><body></body></html>")

        // Chromium positions <datalist> and tablet <select> popups by anchoring a native
        // PopupWindow to a child View added to the WebView with AbsoluteLayout.LayoutParams.
        // We mimic that child View insertion directly instead of testing <datalist>/<select>
        // end-to-end in HTML because the resulting PopupWindow is a separate Android window whose
        // bounds aren't observable from the DOM or Compose (and <select> uses a dialog on phones).
        val anchor = View(bridge.context)
        composeTestRule.runOnIdle {
            bridge.addView(anchor, android.widget.AbsoluteLayout.LayoutParams(50, 40, 10, 20))
        }

        composeTestRule.waitUntil(TIMEOUT_MS) { anchor.width == 50 && anchor.height == 40 }
        composeTestRule.runOnIdle {
            assertEquals(10, anchor.left)
            assertEquals(20, anchor.top)
        }
    }

    @Test
    fun testAccessibilityTree() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        lateinit var bridge: TestWebViewBridge
        lateinit var provider: FakeAccessibilityNodeProvider
        composeTestRule.setContent {
            WebSurface(
                content = webContent,
                modifier = Modifier.size(200.dp),
                bridgeFactory = { ctx ->
                    TestWebViewBridge(ctx)
                        .apply {
                            provider = FakeAccessibilityNodeProvider(this, "Accessible Button")
                            fakeAccessibilityNodeProvider = provider
                        }
                        .also { bridge = it }
                },
            )
        }

        bridge.loadHtml("<html><body></body></html>")

        var buttonNode: AccessibilityNodeInfo? = null
        composeTestRule.waitUntil(TIMEOUT_MS) {
            buttonNode = findNodeByText(uiAutomation.rootInActiveWindow, "Accessible Button")
            buttonNode != null
        }

        assertTrue(buttonNode!!.performAction(AccessibilityNodeInfo.ACTION_FOCUS))
        composeTestRule.runOnIdle {
            assertEquals(listOf(AccessibilityNodeInfo.ACTION_FOCUS), provider.performedActions)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFocusAndKeyInterop() {
        lateinit var bridge: WebViewBridge
        val surfaceFocusRequester = FocusRequester()
        val otherFocusRequester = FocusRequester()
        var parentHasFocus = false
        var surfaceFocused = false
        var keyReceivedByModifier = false

        composeTestRule.setContent {
            Column {
                Box(modifier = Modifier.onFocusChanged { parentHasFocus = it.hasFocus }) {
                    WebSurface(
                        content = webContent,
                        modifier =
                            Modifier.size(150.dp)
                                .testTag("surface")
                                .focusRequester(surfaceFocusRequester)
                                .onFocusChanged { surfaceFocused = it.isFocused }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.A) {
                                        keyReceivedByModifier = true
                                    }
                                    false
                                },
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                    )
                }
                Box(
                    modifier =
                        Modifier.size(50.dp)
                            .testTag("other")
                            .focusRequester(otherFocusRequester)
                            .focusable()
                )
            }
        }

        bridge.loadHtml(
            """
            <html>
            <body onkeydown='window.lastKey = event.key;'></body>
            </html>
            """
        )

        composeTestRule.runOnIdle { surfaceFocusRequester.requestFocus() }
        composeTestRule.runOnIdle {
            assertTrue(parentHasFocus)
            assertTrue(surfaceFocused)
            assertTrue(bridge.hasFocus())
        }

        composeTestRule.onNodeWithTag("surface").performKeyInput { pressKey(Key.A) }
        composeTestRule.waitUntil(TIMEOUT_MS) {
            keyReceivedByModifier && bridge.evaluateJs("window.lastKey") == "\"a\""
        }

        composeTestRule.runOnIdle { otherFocusRequester.requestFocus() }
        composeTestRule.runOnIdle {
            assertFalse(parentHasFocus)
            assertFalse(surfaceFocused)
            assertFalse(bridge.hasFocus())
            assertNotNull(composeTestRule.activity.currentFocus)
        }
    }

    @Test
    fun testFocusTransferBetweenTwoWebSurfaces() {
        val secondContent = WebContent()
        try {
            lateinit var bridge1: WebViewBridge
            lateinit var bridge2: WebViewBridge

            composeTestRule.setContent {
                Column {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(100.dp),
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge1 = b } },
                    )
                    WebSurface(
                        content = secondContent,
                        modifier = Modifier.size(100.dp),
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge2 = b } },
                    )
                }
            }

            composeTestRule.runOnIdle {
                bridge1.requestFocus()
                assertTrue(bridge1.hasFocus())
                bridge2.requestFocus()
                assertFalse(bridge1.hasFocus())
                assertTrue(bridge2.hasFocus())
            }
        } finally {
            composeTestRule.runOnIdle { secondContent.close() }
        }
    }

    @Test
    fun testSwappingContentWhileFocusedPreservesViewFocus() {
        val secondContent = WebContent()
        try {
            var currentContent by mutableStateOf(webContent)
            lateinit var currentBridge: WebViewBridge

            composeTestRule.setContent {
                WebSurface(
                    content = currentContent,
                    modifier = Modifier.size(100.dp),
                    bridgeFactory = { WebViewBridge(it).also { b -> currentBridge = b } },
                )
            }

            composeTestRule.runOnIdle {
                currentBridge.requestFocus()
                assertTrue(currentBridge.hasFocus())
                currentContent = secondContent
            }
            composeTestRule.runOnIdle { assertTrue(currentBridge.hasFocus()) }
        } finally {
            composeTestRule.runOnIdle { secondContent.close() }
        }
    }

    @Test
    fun testDetachingComposeViewRemovesFocusListener() {
        lateinit var container: ViewGroup
        var token: Any? = Any()
        val tokenRef = WeakReference(token)
        var childComposeView: ComposeView? = null

        composeTestRule.setContent {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).also { frame ->
                        container = frame
                        val captured = token
                        childComposeView =
                            ComposeView(ctx).apply {
                                setContent {
                                    WebSurface(
                                        content = webContent,
                                        modifier = Modifier.size(100.dp),
                                        bridgeFactory = ::WebViewBridge,
                                        bridgeUpdate = { captured.hashCode() },
                                    )
                                }
                            }
                        frame.addView(childComposeView)
                    }
                }
            )
        }
        token = null

        composeTestRule.runOnIdle {
            assertNotNull(tokenRef.get())
            container.removeView(childComposeView)
            childComposeView = null
        }
        composeTestRule.waitForIdle()

        Runtime.getRuntime().gc()
        System.runFinalization()
        Runtime.getRuntime().gc()

        assertNull(tokenRef.get())
    }

    @Test
    fun testPageTakesLongPressBeforeComposeLongClick() {
        lateinit var bridge: WebViewBridge
        var longClicked = false

        composeTestRule.setContent {
            WebSurface(
                content = webContent,
                modifier =
                    Modifier.size(150.dp)
                        .testTag("surface")
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { longClicked = true },
                        ),
                bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
            )
        }

        bridge.loadHtml(
            """
            <html>
            <body style='margin:0;'
                  ontouchstart='window.touches = (window.touches || 0) + 1;'>
                <div style='width:100vw;height:100vh;'></div>
            </body>
            </html>
            """
        )

        // Like click listeners on a WebView, click handlers on the surface itself don't take the
        // page's touches.
        composeTestRule.onNodeWithTag("surface").performTouchInput { longClick() }
        composeTestRule.waitUntil(TIMEOUT_MS) { bridge.evaluateJs("window.touches") == "1" }
        composeTestRule.runOnIdle { assertFalse(longClicked) }
    }

    @Test
    fun testPageTakesTapsBeforeClickableAncestor() {
        lateinit var bridge: WebViewBridge
        var ancestorClicks = 0

        composeTestRule.setContent {
            Column(modifier = Modifier.clickable { ancestorClicks++ }) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(150.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
                Box(modifier = Modifier.size(50.dp).testTag("outside"))
            }
        }

        bridge.loadHtml(
            """
            <html>
            <head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            <body style='margin:0;' onclick='window.clicks = (window.clicks || 0) + 1;'>
                <div style='width:100vw;height:100vh;'></div>
            </body>
            </html>
            """
        )

        // Like a child View, the page takes taps on the surface before click handlers around it.
        composeTestRule.onNodeWithTag("surface", useUnmergedTree = true).performClick()
        composeTestRule.waitUntil(TIMEOUT_MS) { bridge.evaluateJs("window.clicks") == "1" }
        composeTestRule.runOnIdle { assertEquals(0, ancestorClicks) }

        // Taps outside the surface still reach the ancestor.
        composeTestRule.onNodeWithTag("outside", useUnmergedTree = true).performClick()
        composeTestRule.runOnIdle { assertEquals(1, ancestorClicks) }
    }

    @Test
    fun testNestedScrollInLazyColumn() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(200.dp, 300.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp).testTag("surface"),
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                    )
                }
                item { Box(modifier = Modifier.size(200.dp)) }
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))

        assertOverscrollReachesParent(
            bridge = bridge,
            listState = listState,
            pageScrollJs = "window.scrollY",
            swipeTowardsEnd = { swipeUp() },
            swipeTowardsStart = { swipeDown() },
        )
    }

    @Test
    fun testNestedScrollDisabledKeepsGesturesThePageAsksFor() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(200.dp, 300.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp).testTag("surface"),
                        bridgeFactory = {
                            WebViewBridge(it).also { b ->
                                b.isNestedScrollingEnabled = false
                                bridge = b
                            }
                        },
                    )
                }
                item { Box(modifier = Modifier.size(200.dp)) }
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))

        // Chromium asks its parents not to intercept when it handles a touch itself, e.g. a text
        // selection drag. Like a View child, the page then keeps the gesture.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput { down(center) }
        composeTestRule.runOnIdle { bridge.requestDisallowInterceptTouchEvent(true) }
        surface.performTouchInput {
            repeat(10) { i -> moveTo(center.copy(y = center.y - 20f * (i + 1))) }
            up()
        }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            (bridge.evaluateJs("Math.round(window.scrollY)").toIntOrNull() ?: 0) > 0
        }
        composeTestRule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }

        // The request only lasts for that gesture, so the list takes the next drag.
        surface.performTouchInput { swipeUp() }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    @Test
    fun testNestedScrollDisabledKeepsTextSelectionDrags() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(200.dp, 300.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp).testTag("surface"),
                        bridgeFactory = {
                            WebViewBridge(it).also { b ->
                                b.isNestedScrollingEnabled = false
                                bridge = b
                            }
                        },
                    )
                }
                item { Box(modifier = Modifier.size(200.dp)) }
            }
        }

        bridge.loadHtml(
            """
            <html>
            <head><meta name='viewport' content='width=device-width,initial-scale=1'></head>
            <body style='margin:0;font:20px/40px sans-serif;'>
                <div>alpha</div><div>bravo</div><div>charlie</div><div>delta</div>
            </body>
            </html>
            """
        )

        // Long press on "delta", then drag up to "bravo" without lifting. Chromium asks for the
        // drag once it sees the first moves, which stay within touch slop like a real finger's,
        // so the list doesn't get it and the selection grows.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput { down(center.copy(x = 25.dp.toPx(), y = 140.dp.toPx())) }
        composeTestRule.waitUntil(TIMEOUT_MS) {
            bridge.evaluateJs("getSelection().toString()") == "\"delta\""
        }
        surface.performTouchInput {
            val x = 25.dp.toPx()
            for (dy in listOf(2, 4, 6)) moveTo(center.copy(x = x, y = (140 - dy).dp.toPx()))
            for (i in 1..8) moveTo(center.copy(x = x, y = (134 - 9 * i).dp.toPx()))
            up()
        }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            bridge.evaluateJs("getSelection().toString()").contains("charlie")
        }
        composeTestRule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun testNestedScrollDisabledLetsParentTakeDrags() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(200.dp, 300.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp).testTag("surface"),
                        bridgeFactory = {
                            WebViewBridge(it).also { b ->
                                // On by default, unlike plain Views.
                                assertTrue(b.isNestedScrollingEnabled)
                                b.isNestedScrollingEnabled = false
                                bridge = b
                            }
                        },
                    )
                }
                item { Box(modifier = Modifier.size(200.dp)) }
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))

        // Even though the page could scroll, the list takes the drag, like a ScrollView taking a
        // drag from a WebView, and the page gets a cancel.
        composeTestRule.onNodeWithTag("surface").performTouchInput { swipeUp() }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
        assertEquals("0", bridge.evaluateJs("Math.round(window.scrollY)"))
    }

    @Test
    fun testNestedScrollInLazyRow() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyRow(state = listState, modifier = Modifier.size(300.dp, 200.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp).testTag("surface"),
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                    )
                }
                item { Box(modifier = Modifier.size(200.dp)) }
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "200vw", height = "100vh"))

        assertOverscrollReachesParent(
            bridge = bridge,
            listState = listState,
            pageScrollJs = "window.scrollX",
            swipeTowardsEnd = { swipeLeft() },
            swipeTowardsStart = { swipeRight() },
        )
    }

    @Test
    fun testNestedScrollInPullToRefreshBox() {
        lateinit var bridge: WebViewBridge
        lateinit var pullToRefreshState: PullToRefreshState
        var isRefreshing by mutableStateOf(false)
        var refreshCount = 0

        composeTestRule.setContent {
            pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    refreshCount++
                },
                state = pullToRefreshState,
                threshold = 50.dp,
            ) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(200.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))

        // Pull down at the top of the page so overscroll pulls the indicator past its threshold.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(centerX, 20.dp.toPx()))
            repeat(10) { moveBy(Offset(0f, 15.dp.toPx())) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { pullToRefreshState.distanceFraction > 1f }

        // Pushing back up hands the rest of the gesture to PullToRefreshBox: pushing past where
        // the pull started retracts it without scrolling the page, and releasing cancels refresh.
        surface.performTouchInput {
            repeat(15) { moveBy(Offset(0f, -15.dp.toPx())) }
            up()
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { pullToRefreshState.distanceFraction == 0f }
        composeTestRule.runOnIdle { assertEquals(0, refreshCount) }
        assertEquals("0", bridge.evaluateJs("Math.round(window.scrollY)"))

        // The page gets the next gesture again, so pulling down past the threshold and releasing
        // refreshes.
        surface.performTouchInput {
            down(Offset(centerX, 20.dp.toPx()))
            repeat(10) { moveBy(Offset(0f, 15.dp.toPx())) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { pullToRefreshState.distanceFraction > 1f }
        surface.performTouchInput { up() }
        composeTestRule.waitUntil(TIMEOUT_MS) { refreshCount == 1 && isRefreshing }
    }

    @Test
    fun testNestedScrollParentIsOnlyOfferedDragsAlongAxesItTookOverscroll() {
        lateinit var bridge: WebViewBridge
        var tookOverscrollX = false
        val parent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    if (available.y < 0f) available.copy(x = 0f) else Offset.Zero

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.x == 0f) return Offset.Zero
                    tookOverscrollX = true
                    return Offset(available.x, 0f)
                }
            }

        composeTestRule.setContent {
            Box(modifier = Modifier.nestedScroll(parent)) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(200.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "200vw", height = "200vh"))

        // Drag right at the left edge so the parent takes the page's horizontal overscroll, then
        // drag up in the same gesture. Because the parent took no vertical overscroll, it isn't
        // offered the vertical drag and the page scrolls instead.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(50.dp.toPx(), 150.dp.toPx()))
            repeat(5) { moveBy(Offset(15.dp.toPx(), 0f)) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { tookOverscrollX }

        surface.performTouchInput {
            repeat(5) { moveBy(Offset(0f, -15.dp.toPx())) }
            up()
        }
        composeTestRule.waitUntil(TIMEOUT_MS) {
            (bridge.evaluateJs("Math.round(window.scrollY)").toIntOrNull() ?: 0) > 0
        }
    }

    @Test
    fun testNestedScrollPageKeepsDragWhenParentOnlyTakesOverscroll() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(200.dp, 300.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp).testTag("surface"),
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                    )
                }
                item { Box(modifier = Modifier.size(200.dp)) }
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))
        bridge.evaluateJs("window.scrollTo(0, document.body.scrollHeight)")
        composeTestRule.waitUntil(TIMEOUT_MS) {
            (bridge.evaluateJs("Math.round(window.scrollY)").toIntOrNull() ?: 0) > 0
        }
        val pageBottom = bridge.evaluateJs("Math.round(window.scrollY)").toInt()

        // Drag up past the bottom of the page, so the list takes the overscroll...
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(centerX, 150.dp.toPx()))
            moveBy(Offset(0f, -30.dp.toPx()))
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { listState.firstVisibleItemScrollOffset > 0 }

        // ...but it only takes what the page leaves, so dragging back down scrolls the page.
        // We check before lifting the finger: Chromium swaps the test clock's event times for
        // the current time, so our moves look a millisecond apart and the release fling can go
        // either way.
        surface.performTouchInput { repeat(4) { moveBy(Offset(0f, 15.dp.toPx())) } }
        composeTestRule.waitUntil(TIMEOUT_MS) {
            (bridge.evaluateJs("Math.round(window.scrollY)").toIntOrNull() ?: 0) < pageBottom
        }
        surface.performTouchInput { up() }
    }

    @Test
    fun testNestedScrollHandoverFlingsParent() {
        lateinit var bridge: WebViewBridge
        var overscrolled = false
        val flings = mutableListOf<Velocity>()
        val parent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    if (available.y < 0f) available.copy(x = 0f) else Offset.Zero

                // Takes downward overscroll, like a pull-to-refresh indicator.
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y <= 0f) return Offset.Zero
                    overscrolled = true
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    flings += available
                    return Velocity.Zero
                }
            }

        composeTestRule.setContent {
            Box(modifier = Modifier.nestedScroll(parent)) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(200.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))

        // Pull down at the top so the parent takes the page's overscroll, then flick back up so it
        // claims the drag and gets a single fling with the flick's velocity when released.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(centerX, 20.dp.toPx()))
            repeat(5) { moveBy(Offset(0f, 15.dp.toPx())) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { overscrolled }

        surface.performTouchInput {
            repeat(3) { moveBy(Offset(0f, -15.dp.toPx()), delayMillis = 8L) }
            up()
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { flings.isNotEmpty() }
        composeTestRule.runOnIdle {
            assertEquals(1, flings.size)
            assertTrue(flings.single().y < 0f)
        }
    }

    @Test
    fun testNestedScrollParentIsNotOfferedDragsWithoutTakingOverscroll() {
        lateinit var bridge: WebViewBridge
        var overscrolled = false
        var offeredDrags = 0
        // Like a collapsing top app bar: takes upward drags in pre scroll, but no overscroll.
        val parent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.x != 0f || available.y != 0f) offeredDrags++
                    return if (available.y < 0f) available.copy(x = 0f) else Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y < 0f) overscrolled = true
                    return Offset.Zero
                }
            }

        composeTestRule.setContent {
            Box(modifier = Modifier.nestedScroll(parent)) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(200.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))
        bridge.evaluateJs("window.scrollTo(0, document.body.scrollHeight)")
        composeTestRule.waitUntil(TIMEOUT_MS) {
            (bridge.evaluateJs("Math.round(window.scrollY)").toIntOrNull() ?: 0) > 0
        }

        // Drag up past the bottom of the page so it overscrolls, then keep dragging up in the same
        // gesture. The parent didn't take the overscroll, so it isn't offered the drag.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(centerX, 150.dp.toPx()))
            repeat(3) { moveBy(Offset(0f, -15.dp.toPx())) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { overscrolled }

        surface.performTouchInput {
            repeat(5) { moveBy(Offset(0f, -15.dp.toPx())) }
            up()
        }
        composeTestRule.runOnIdle { assertEquals(0, offeredDrags) }
    }

    @Test
    fun testNestedScrollHandoverOnlyDispatchesAxesTheParentTookOverscroll() {
        lateinit var bridge: WebViewBridge
        var tookOverscrollX = false
        var horizontalDrags = 0
        var verticalDispatches = 0
        // Like a HorizontalPager: takes horizontal overscroll, then horizontal drags in pre scroll.
        val parent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y != 0f) verticalDispatches++
                    if (!tookOverscrollX || available.x == 0f) return Offset.Zero
                    horizontalDrags++
                    return Offset(available.x, 0f)
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y != 0f) verticalDispatches++
                    if (available.x == 0f) return Offset.Zero
                    tookOverscrollX = true
                    return Offset(available.x, 0f)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (available.y != 0f) verticalDispatches++
                    return Velocity.Zero
                }
            }

        composeTestRule.setContent {
            Box(modifier = Modifier.nestedScroll(parent)) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(200.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "200vw", height = "200vh"))

        // Drag right at the left edge so the parent takes the page's horizontal overscroll and then
        // the drag, then drag back while drifting down. The parent only gets the horizontal part:
        // the page didn't overscroll vertically, and no longer has the drag either.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(50.dp.toPx(), 100.dp.toPx()))
            repeat(5) { moveBy(Offset(15.dp.toPx(), 0f)) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { tookOverscrollX }

        surface.performTouchInput {
            repeat(5) { moveBy(Offset(-15.dp.toPx(), 5.dp.toPx())) }
            up()
        }
        composeTestRule.runOnIdle {
            // More than the move that handed the drag over, so later moves reached it too.
            assertTrue(horizontalDrags > 1)
            assertEquals(0, verticalDispatches)
        }
    }

    @Test
    fun testNestedScrollHandoverFlingFollowsRemainingPointer() {
        lateinit var bridge: WebViewBridge
        var overscrolled = false
        val flings = mutableListOf<Velocity>()
        // Like pull-to-refresh: takes downward overscroll, and upward drags in pre scroll.
        val parent =
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    if (available.y < 0f) available.copy(x = 0f) else Offset.Zero

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y <= 0f) return Offset.Zero
                    overscrolled = true
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    flings += available
                    return Velocity.Zero
                }
            }

        composeTestRule.setContent {
            Box(modifier = Modifier.nestedScroll(parent)) {
                WebSurface(
                    content = webContent,
                    modifier = Modifier.size(200.dp).testTag("surface"),
                    bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                )
            }
        }

        bridge.loadHtml(scrollablePageHtml(width = "100vw", height = "200vh"))

        // Pull down at the top so the parent takes the page's overscroll.
        val surface = composeTestRule.onNodeWithTag("surface")
        surface.performTouchInput {
            down(Offset(centerX, 20.dp.toPx()))
            repeat(5) { moveBy(Offset(0f, 15.dp.toPx())) }
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { overscrolled }

        // Push back up so the parent claims the drag, then put a second finger down above the
        // first, lift the first, and drag the second down. The release fling follows the second
        // finger, rather than the jump up from the first finger to the second.
        surface.performTouchInput {
            moveBy(Offset(0f, -15.dp.toPx()))
            down(1, Offset(centerX, 10.dp.toPx()))
            up(0)
            repeat(3) { moveBy(1, Offset(0f, 5.dp.toPx())) }
            up(1)
        }
        composeTestRule.waitUntil(TIMEOUT_MS) { flings.isNotEmpty() }
        composeTestRule.runOnIdle {
            assertEquals(1, flings.size)
            assertTrue(flings.single().y >= 0f)
        }
    }

    @Test
    fun testFocusedWebSurfaceInLazyColumnStaysPinnedWhenScrolledOffscreen() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState
        lateinit var scope: CoroutineScope
        val otherFocusRequester = FocusRequester()

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            Column {
                listState = rememberLazyListState()
                LazyColumn(state = listState, modifier = Modifier.size(200.dp, 150.dp)) {
                    item {
                        WebSurface(
                            content = webContent,
                            modifier = Modifier.size(200.dp, 150.dp),
                            bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                        )
                    }
                    items(10) { Box(modifier = Modifier.size(200.dp, 150.dp)) }
                }
                Box(modifier = Modifier.size(50.dp).focusRequester(otherFocusRequester).focusable())
            }
        }

        composeTestRule.runOnIdle {
            bridge.requestFocus()
            scope.launch { listState.scrollToItem(10) }
        }
        composeTestRule.runOnIdle {
            assertTrue(webContent.isAttached())
            assertTrue(bridge.hasFocus())
            otherFocusRequester.requestFocus()
        }
        composeTestRule.runOnIdle { assertFalse(webContent.isAttached()) }
    }

    @Test
    fun testWebSurfaceInLazyColumnUnpinsIfFocusLostDuringFocusUpdate() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState
        lateinit var scope: CoroutineScope
        val surfaceFocusRequester = FocusRequester()
        val otherFocusRequester = FocusRequester()

        composeTestRule.setContent {
            scope = rememberCoroutineScope()
            Column {
                listState = rememberLazyListState()
                LazyColumn(state = listState, modifier = Modifier.size(200.dp, 150.dp)) {
                    item {
                        WebSurface(
                            content = webContent,
                            modifier =
                                Modifier.size(200.dp, 150.dp).focusRequester(surfaceFocusRequester),
                            bridgeFactory = {
                                WebViewBridge(it)
                                    .apply {
                                        viewTreeObserver.addOnGlobalFocusChangeListener {
                                            _,
                                            newFocus ->
                                            if (newFocus === this) (this as View).clearFocus()
                                        }
                                    }
                                    .also { b -> bridge = b }
                            },
                        )
                    }
                    items(10) { Box(modifier = Modifier.size(200.dp, 150.dp)) }
                }
                Box(modifier = Modifier.size(50.dp).focusRequester(otherFocusRequester).focusable())
            }
        }

        composeTestRule.runOnIdle {
            otherFocusRequester.requestFocus()
            surfaceFocusRequester.requestFocus()
            assertFalse(bridge.hasFocus())
            scope.launch { listState.scrollToItem(10) }
        }
        composeTestRule.runOnIdle { assertFalse(webContent.isAttached()) }
    }

    @Test
    fun testRequestRectangleOnScreenBringsWebSurfaceIntoView() {
        lateinit var bridge: WebViewBridge
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            listState = rememberLazyListState()
            LazyColumn(state = listState, modifier = Modifier.size(200.dp, 150.dp)) {
                item {
                    WebSurface(
                        content = webContent,
                        modifier = Modifier.size(200.dp, 300.dp),
                        bridgeFactory = { WebViewBridge(it).also { b -> bridge = b } },
                    )
                }
            }
        }

        // Scroll the WebView content and request the top of the visible viewport in content
        // coordinates (which include scrollY). Since the top of WebSurface is already visible in
        // the LazyColumn, the list should not scroll.
        composeTestRule.runOnIdle {
            (bridge as View).scrollTo(0, 500)
            bridge.requestRectangleOnScreen(
                Rect(0, bridge.scrollY, 100, bridge.scrollY + 10),
                false,
            )
        }
        composeTestRule.runOnIdle { assertEquals(0, listState.firstVisibleItemScrollOffset) }

        // Requesting the bottom of the WebSurface viewport (also in content coordinates) should
        // scroll the LazyColumn to reveal it.
        composeTestRule.runOnIdle {
            bridge.requestRectangleOnScreen(
                Rect(
                    0,
                    bridge.scrollY + bridge.height - 10,
                    100,
                    bridge.scrollY + bridge.height,
                ),
                false,
            )
        }
        composeTestRule.runOnIdle { assertTrue(listState.firstVisibleItemScrollOffset > 0) }
    }

    /**
     * Swipes the surface past the end of the page, which should scroll the list, then swipes back
     * and expects both at the start again.
     */
    private fun assertOverscrollReachesParent(
        bridge: WebViewBridge,
        listState: LazyListState,
        pageScrollJs: String,
        swipeTowardsEnd: TouchInjectionScope.() -> Unit,
        swipeTowardsStart: TouchInjectionScope.() -> Unit,
    ) {
        repeat(3) { composeTestRule.onNodeWithTag("surface").performTouchInput(swipeTowardsEnd) }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            (bridge.evaluateJs("Math.round($pageScrollJs)").toIntOrNull() ?: 0) >= 150 &&
                (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        }

        repeat(3) { composeTestRule.onNodeWithTag("surface").performTouchInput(swipeTowardsStart) }

        composeTestRule.waitUntil(TIMEOUT_MS) {
            bridge.evaluateJs("Math.round($pageScrollJs)") == "0" &&
                listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }
    }

    // minimum-scale stops WebView zooming out to fit content wider than the viewport, which would
    // leave nothing to scroll horizontally.
    private fun scrollablePageHtml(width: String, height: String) =
        """
        <html>
        <head>
            <meta name='viewport' content='width=device-width,initial-scale=1,minimum-scale=1'>
        </head>
        <body style='margin:0;'>
            <div style='width:$width;height:$height;'></div>
        </body>
        </html>
        """

    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString() == text || node.contentDescription?.toString() == text) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findNodeByText(node.getChild(i), text)
            if (found != null) return found
        }
        return null
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
