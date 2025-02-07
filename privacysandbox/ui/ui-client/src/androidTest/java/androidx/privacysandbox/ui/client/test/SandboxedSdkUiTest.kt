/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.privacysandbox.ui.client.test

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.ui.client.test.SandboxedSdkViewTest.Companion.SHORTEST_TIME_BETWEEN_SIGNALS_MS
import androidx.privacysandbox.ui.client.test.SandboxedSdkViewTest.Companion.TIMEOUT
import androidx.privacysandbox.ui.client.test.SandboxedSdkViewTest.Companion.UI_INTENSIVE_TIMEOUT
import androidx.privacysandbox.ui.client.view.SandboxedSdkUi
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.integration.testingutils.TestEventListener
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/374919355): Create a common test framework for testing Compose and View UI lib constructs
@RunWith(AndroidJUnit4::class)
@LargeTest
class SandboxedSdkUiTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<UiLibComposeActivity>()
    private var testSandboxedUiAdapter by mutableStateOf(TestSandboxedUiAdapter())
    private var eventListener by mutableStateOf(TestEventListener())
    private var providerUiOnTop by mutableStateOf(true)
    private var size by mutableStateOf(20.dp)
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        testSandboxedUiAdapter = TestSandboxedUiAdapter()
        eventListener = TestEventListener()
        providerUiOnTop = true
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun eventListenerErrorTest() {
        composeTestRule.setContent {
            SandboxedSdkUi(
                sandboxedUiAdapter = FailingTestSandboxedUiAdapter(),
                modifier = Modifier,
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener
            )
        }
        assertThat(eventListener.errorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(eventListener.error?.message).isEqualTo("Error in openSession()")
    }

    @Test
    fun addEventListenerTest() {
        // Initially no events are received when the session is not open.
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        // When session is open, the events are received
        addNodeToLayout()
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun sessionNotOpenedWhenWindowIsNotVisibleTest() {
        // the window is not visible when the activity is in the CREATED state.
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionNotOpened()
        // the window becomes visible when the activity is in the STARTED state.
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        testSandboxedUiAdapter.assertSessionOpened()
    }

    @Test
    fun onAttachedToWindowTest() {
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(hasChildCount(1)))
            .check { view, exception ->
                if (
                    view.layoutParams.width != WRAP_CONTENT ||
                        view.layoutParams.height != WRAP_CONTENT
                ) {
                    throw exception
                }
            }
    }

    @Test
    fun childViewRemovedOnErrorTest() {
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check(matches(hasChildCount(1)))
        composeTestRule.activityRule.withActivity {
            testSandboxedUiAdapter.internalClient!!.onSessionError(Exception())
        }
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check(matches(hasChildCount(0)))
    }

    @Test
    fun onZOrderChangedTest() {
        addNodeToLayout()
        // When session is opened, the provider should not receive a Z-order notification.
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession
        assertThat(session?.zOrderChangedLatch?.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isTrue()
        // When state changes to false, the provider should be notified.
        providerUiOnTop = false
        composeTestRule.waitForIdle()
        assertThat(session?.zOrderChangedLatch?.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
        // When state changes back to true, the provider should be notified.
        session?.zOrderChangedLatch = CountDownLatch(1)
        providerUiOnTop = true
        composeTestRule.waitForIdle()
        assertThat(session?.zOrderChangedLatch?.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isTrue()
    }

    @Test
    fun setZOrderNotOnTopBeforeOpeningSessionTest() {
        providerUiOnTop = false
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession
        // The initial Z-order state is passed to the session, but notifyZOrderChanged is not called
        assertThat(session?.zOrderChangedLatch?.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun setZOrderNotOnTopWhileSessionLoadingTest() {
        testSandboxedUiAdapter.delayOpenSessionCallback = true
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        providerUiOnTop = false
        composeTestRule.waitForIdle()
        val session = testSandboxedUiAdapter.testSession!!
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        composeTestRule.activityRule.withActivity { testSandboxedUiAdapter.sendOnSessionOpened() }
        // After session has opened, the pending Z order changed made while loading is notified
        // to the session.
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun onConfigurationChangedSessionRemainsOpened() {
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        // newWindow() will be triggered by a window state change, even if the activity handles
        // orientation changes without recreating the activity.
        uiDevice.performActionAndWait(
            { uiDevice.setOrientationLeft() },
            Until.newWindow(),
            UI_INTENSIVE_TIMEOUT
        )
        testSandboxedUiAdapter.assertSessionNotClosed()
        uiDevice.performActionAndWait(
            { uiDevice.setOrientationNatural() },
            Until.newWindow(),
            UI_INTENSIVE_TIMEOUT
        )
        testSandboxedUiAdapter.assertSessionNotClosed()
    }

    @Test
    fun onConfigurationChangedTestSameConfigurationTest() {
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        composeTestRule.activityRule.withActivity {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        assertThat(testSandboxedUiAdapter.wasOnConfigChangedCalled()).isFalse()
    }

    @Test
    fun onPaddingSetTest() {
        var padding by mutableStateOf(0.dp)
        composeTestRule.setContent {
            SandboxedSdkUi(
                sandboxedUiAdapter = testSandboxedUiAdapter,
                modifier = Modifier.padding(all = padding),
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener
            )
        }
        testSandboxedUiAdapter.assertSessionOpened()
        padding = 10.dp
        composeTestRule.waitForIdle()
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isTrue()
    }

    @Test
    fun signalsSentWhenPaddingApplied() {
        var padding by mutableStateOf(0.dp)
        composeTestRule.setContent {
            SandboxedSdkUi(
                sandboxedUiAdapter = testSandboxedUiAdapter,
                modifier = Modifier.padding(all = padding),
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener
            )
        }
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession
        session?.runAndRetrieveNextUiChange {
            padding = 10.dp
            composeTestRule.waitForIdle()
        }
        assertThat(session?.shortestGapBetweenUiChangeEvents)
            .isAtLeast(SHORTEST_TIME_BETWEEN_SIGNALS_MS)
    }

    @Test
    fun onLayoutTestWithSizeChangeTest() {
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(hasChildCount(1)))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
        size = 30.dp
        composeTestRule.waitForIdle()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(hasChildCount(1)))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isTrue()
    }

    @Test
    fun onLayoutTestNoSizeChangeTest() {
        size = 20.dp
        addNodeToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        size = 20.dp
        composeTestRule.waitForIdle()
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isFalse()
    }

    @Test
    fun onLayoutTestViewShiftWithoutSizeChangeTest() {
        var offset by mutableStateOf(0.dp)
        composeTestRule.setContent {
            SandboxedSdkUi(
                sandboxedUiAdapter = testSandboxedUiAdapter,
                modifier = Modifier.offset(offset),
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener
            )
        }
        testSandboxedUiAdapter.assertSessionOpened()
        offset = 10.dp
        composeTestRule.waitForIdle()
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isFalse()
    }

    @Test
    fun onSdkRequestsResizeTest() {
        val boxSize = 300.dp
        composeTestRule.setContent {
            Box(modifier = Modifier.requiredSize(boxSize)) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier,
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }
        testSandboxedUiAdapter.assertSessionOpened()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(view.width).isEqualTo(boxSize.toPx(view.context.resources.displayMetrics))
            assertThat(view.height).isEqualTo(boxSize.toPx(view.context.resources.displayMetrics))
        }
        composeTestRule.activityRule.withActivity {
            testSandboxedUiAdapter.testSession?.requestResize(200, 200) as Any
        }
        composeTestRule.waitForIdle()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(view.width).isEqualTo(200)
            assertThat(view.height).isEqualTo(200)
        }
    }

    @Test
    fun requestResizeWithMeasureSpecAtMostExceedsParentBoundsTest() {
        val boxSize = 300.dp
        composeTestRule.setContent {
            Box(modifier = Modifier.requiredSize(boxSize)) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier,
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }
        testSandboxedUiAdapter.assertSessionOpened()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(view.width).isEqualTo(boxSize.toPx(view.context.resources.displayMetrics))
            assertThat(view.height).isEqualTo(boxSize.toPx(view.context.resources.displayMetrics))
        }
        composeTestRule.activityRule.withActivity {
            val newSize = (boxSize + 10.dp).toPx(resources.displayMetrics)
            testSandboxedUiAdapter.testSession?.requestResize(newSize, newSize) as Any
        }
        composeTestRule.waitForIdle()
        // the resize is constrained by the parent's size
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(view.width).isEqualTo(boxSize.toPx(view.context.resources.displayMetrics))
            assertThat(view.height).isEqualTo(boxSize.toPx(view.context.resources.displayMetrics))
        }
    }

    @Test
    fun requestResizeWithMeasureSpecExactlyTest() {
        var boxWidth = 0.dp
        var boxHeight = 0.dp
        composeTestRule.setContent {
            // Get local density from composable
            val localDensity = LocalDensity.current
            Box(
                modifier =
                    Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                        boxWidth = with(localDensity) { coordinates.size.width.toDp() }
                        boxHeight = with(localDensity) { coordinates.size.height.toDp() }
                    }
            ) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier.fillMaxSize(),
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }
        testSandboxedUiAdapter.assertSessionOpened()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(view.width).isEqualTo(boxWidth.toPx(view.context.resources.displayMetrics))
            assertThat(view.height).isEqualTo(boxHeight.toPx(view.context.resources.displayMetrics))
        }
        composeTestRule.activityRule.withActivity {
            val newBoxWidth = (boxWidth - 10.dp).toPx(resources.displayMetrics)
            val newBoxHeight = (boxHeight - 10.dp).toPx(resources.displayMetrics)
            testSandboxedUiAdapter.testSession?.requestResize(newBoxWidth, newBoxHeight) as Any
        }
        composeTestRule.waitForIdle()
        // the request is a no-op when the MeasureSpec is EXACTLY
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(view.width).isEqualTo((boxWidth).toPx(view.context.resources.displayMetrics))
            assertThat(view.height)
                .isEqualTo((boxHeight).toPx(view.context.resources.displayMetrics))
        }
    }

    @Test
    fun sandboxedSdkViewIsTransitionGroupTest() {
        addNodeToLayout()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, exception ->
            if (!(view as ViewGroup).isTransitionGroup) {
                throw exception
            }
        }
    }

    @Test
    fun signalsNotSentWhenViewUnchangedTest() {
        addNodeToLayout()
        val session = testSandboxedUiAdapter.testSession
        session?.runAndRetrieveNextUiChange {}
        session?.assertNoSubsequentUiChanges()
    }

    /**
     * Shifts the view partially off screen and verifies that the reported onScreenGeometry is
     * cropped accordingly.
     */
    @Test
    fun correctSignalsSentForOnScreenGeometryWhenViewOffScreenTest() {
        var xOffset by mutableStateOf(0.dp)
        var yOffset by mutableStateOf(0.dp)
        val clippedWidth = 300.dp
        val clippedHeight = 400.dp
        composeTestRule.setContent {
            Box(modifier = Modifier.size(width = clippedWidth, height = clippedHeight)) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier.offset(x = xOffset, y = yOffset),
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }
        var initialHeight = 0
        var initialWidth = 0
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            initialHeight = view.height
            initialWidth = view.width
        }
        val session = testSandboxedUiAdapter.testSession
        val sandboxedSdkViewUiInfo =
            session?.runAndRetrieveNextUiChange {
                xOffset = 100.dp
                yOffset = 200.dp
                composeTestRule.waitForIdle()
            }
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, _ ->
            assertThat(sandboxedSdkViewUiInfo?.uiContainerWidth)
                .isEqualTo(clippedWidth.toPx(view.context.resources.displayMetrics))
            assertThat(sandboxedSdkViewUiInfo?.uiContainerHeight)
                .isEqualTo(clippedHeight.toPx(view.context.resources.displayMetrics))
            assertThat(sandboxedSdkViewUiInfo?.onScreenGeometry?.height()?.toFloat())
                .isEqualTo(initialHeight - yOffset.toPx(view.context.resources.displayMetrics))
            assertThat(sandboxedSdkViewUiInfo?.onScreenGeometry?.width()?.toFloat())
                .isEqualTo(initialWidth - xOffset.toPx(view.context.resources.displayMetrics))
        }
    }

    @Test
    fun signalsSentWhenPositionChangesTest() {
        var offset by mutableStateOf(0.dp)
        composeTestRule.setContent {
            SandboxedSdkUi(
                sandboxedUiAdapter = testSandboxedUiAdapter,
                modifier = Modifier.offset(x = offset),
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener
            )
        }
        val session = testSandboxedUiAdapter.testSession
        val sandboxedSdkViewUiInfo =
            session?.runAndRetrieveNextUiChange {
                offset = 10.dp
                composeTestRule.waitForIdle()
            }
        val containerWidth = sandboxedSdkViewUiInfo?.uiContainerWidth ?: 0
        val onScreenWidth = sandboxedSdkViewUiInfo?.onScreenGeometry?.width()?.toFloat()
        Espresso.onView(instanceOf(SandboxedSdkView::class.java)).check { view, exception ->
            val expectedWidth =
                (containerWidth - offset.toPx(view.context.resources.displayMetrics)).toFloat()
            if (expectedWidth != onScreenWidth) {
                throw exception
            }
        }
    }

    /**
     * Creates many UI changes and ensures that these changes are not sent more frequently than
     * expected.
     */
    @Test
    @SuppressLint("BanThreadSleep") // Deliberate delay for testing
    fun signalsNotSentMoreFrequentlyThanLimitTest() {
        addNodeToLayout()
        val session = testSandboxedUiAdapter.testSession!!
        for (i in 1..10) {
            size += (i * 10).dp
            composeTestRule.waitForIdle()
            Thread.sleep(100)
        }
        assertThat(session.shortestGapBetweenUiChangeEvents)
            .isAtLeast(SHORTEST_TIME_BETWEEN_SIGNALS_MS)
    }

    @Test
    fun signalsSentWhenHostActivityStateChangesTest() {
        addNodeToLayout()
        val session = testSandboxedUiAdapter.testSession
        session?.runAndRetrieveNextUiChange {}
        // Replace the first activity with a new activity. The onScreenGeometry should now be empty.
        var sandboxedSdkViewUiInfo =
            session?.runAndRetrieveNextUiChange {
                composeTestRule.activityRule.scenario.onActivity {
                    val intent = Intent(it, SecondActivity::class.java)
                    it.startActivity(intent)
                }
            }
        assertThat(sandboxedSdkViewUiInfo?.onScreenGeometry?.isEmpty).isTrue()
        // Return to the first activity. The onScreenGeometry should now be non-empty.
        sandboxedSdkViewUiInfo = session?.runAndRetrieveNextUiChange { uiDevice.pressBack() }
        assertThat(sandboxedSdkViewUiInfo?.onScreenGeometry?.isEmpty).isFalse()
    }

    @Test
    fun sessionRemainsOpenWhenSandboxedSdkUiIsDetachedAndNotReleased() {
        var attached by mutableStateOf(true)

        composeTestRule.setContent {
            ReusableContentHost(attached) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier.requiredSize(size),
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }

        testSandboxedUiAdapter.assertSessionOpened()

        attached = false
        testSandboxedUiAdapter.assertSessionNotClosed()

        attached = true
        testSandboxedUiAdapter.assertSessionNotClosed()
    }

    @Test
    fun sessionClosesWhenSandboxedSdkUiIsRemovedFromComposition() {
        var showContent by mutableStateOf(true)

        composeTestRule.setContent {
            if (showContent) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier.requiredSize(size),
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }

        testSandboxedUiAdapter.assertSessionOpened()

        showContent = false
        composeTestRule.waitForIdle()

        testSandboxedUiAdapter.assertSessionClosed()
    }

    @Test
    fun reAddEventListenerWhenSandboxedSdkUiIsReAttachedInLazyList() {
        var attached by mutableStateOf(true)

        composeTestRule.setContent {
            ReusableContentHost(attached) {
                SandboxedSdkUi(
                    sandboxedUiAdapter = testSandboxedUiAdapter,
                    modifier = Modifier.requiredSize(size),
                    providerUiOnTop = providerUiOnTop,
                    sandboxedSdkViewEventListener = eventListener
                )
            }
        }

        // When session is open, the events are received
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()

        attached = false
        composeTestRule.waitForIdle()

        attached = true
        composeTestRule.waitForIdle()

        // When SandboxedSdkUi is re-attached event listener is added back
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun onUiClosedWhenSandboxedSdkUiIsRemovedFromComposition() {
        var showContent by mutableStateOf(true)
        var attached by mutableStateOf(true)

        composeTestRule.setContent {
            if (showContent) {
                ReusableContentHost(attached) {
                    SandboxedSdkUi(
                        sandboxedUiAdapter = testSandboxedUiAdapter,
                        modifier = Modifier.requiredSize(size),
                        providerUiOnTop = providerUiOnTop,
                        sandboxedSdkViewEventListener = eventListener
                    )
                }
            }
        }

        // verify onUiDisplayed() is called
        assertThat(eventListener.uiDisplayedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()

        // detach SandboxedSdkUi from composition hierarchy of ReusableContentHost
        attached = false
        composeTestRule.waitForIdle()

        // verify onUiClosed() is not called
        assertThat(eventListener.sessionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()

        // remove SandboxedSdkUi from composition hierarchy
        showContent = false
        composeTestRule.waitForIdle()

        // verify onUiClosed() is called
        assertThat(eventListener.sessionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun addNodeToLayout() {
        composeTestRule.setContent {
            SandboxedSdkUi(
                sandboxedUiAdapter = testSandboxedUiAdapter,
                modifier = Modifier.requiredSize(size),
                providerUiOnTop = providerUiOnTop,
                sandboxedSdkViewEventListener = eventListener
            )
        }
    }

    private fun Dp.toPx(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).roundToInt()
}
