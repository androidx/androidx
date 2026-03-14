/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.window

import android.view.KeyEvent
import android.view.View
import android.view.View.MEASURED_STATE_TOO_SMALL
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.round
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.window.layout.WindowMetricsCalculator
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PopupTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())

    private val testTag = "testedPopup"
    private val offset = IntOffset(10, 10)
    private val popupSize = IntSize(40, 20)

    @Test
    fun isShowing() {
        rule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(Modifier.size(50.dp), content = {})
                    }
                }
            }
        }

        rule.popupMatches(testTag, isDisplayed())
    }

    @Test
    fun hasActualSize() {
        val popupWidthDp = with(rule.density) { popupSize.width.toDp() }
        val popupHeightDp = with(rule.density) { popupSize.height.toDp() }

        rule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(width = popupWidthDp, height = popupHeightDp, content = {})
                    }
                }
            }
        }

        rule.popupMatches(testTag, matchesSize(popupSize.width, popupSize.height))
    }

    @Test
    fun changeParams_assertNoLeaks() {
        class PopupsCounterMatcher : TypeSafeMatcher<Root>() {
            var popupsFound = 0

            override fun describeTo(description: Description?) {
                description?.appendText("PopupLayoutMatcher")
            }

            // TODO(b/141101446): Find a way to match the window used by the popup
            override fun matchesSafely(item: Root?): Boolean {
                val isPopup = item != null && isPopupLayout(item.decorView, testTag)
                if (isPopup) {
                    popupsFound++
                }
                return isPopup
            }
        }

        val measureLatch = CountDownLatch(1)
        var focusable by mutableStateOf(false)
        rule.setContent {
            Box {
                PopupTestTag(testTag) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = offset,
                        properties = PopupProperties(focusable = focusable),
                    ) {
                        // This is called after the OnChildPosition method in Popup() which
                        // updates the popup to its final position
                        Box(
                            modifier =
                                Modifier.requiredWidth(200.dp)
                                    .requiredHeight(200.dp)
                                    .onGloballyPositioned { measureLatch.countDown() }
                        ) {}
                    }
                }
            }
        }
        measureLatch.await(1, TimeUnit.SECONDS)

        fun assertSinglePopupExists() {
            rule.runOnIdle {}
            val counterMatcher = PopupsCounterMatcher()
            Espresso.onView(instanceOf(Owner::class.java))
                .inRoot(counterMatcher)
                .check(matches(isDisplayed()))

            assertThat(counterMatcher.popupsFound).isEqualTo(1)
        }

        assertSinglePopupExists()

        rule.runOnUiThread { focusable = true }

        // If we have a leak, this will crash on multiple popups found
        assertSinglePopupExists()
    }

    @Test
    fun hasViewTreeLifecycleOwner() {
        rule.setContent { PopupTestTag(testTag) { Popup {} } }

        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(PopupLayoutMatcher(testTag))
            .check(
                matches(
                    object : TypeSafeMatcher<View>() {
                        override fun describeTo(description: Description?) {
                            description?.appendText("view.findViewTreeLifecycleOwner() != null")
                        }

                        override fun matchesSafely(item: View): Boolean {
                            return item.findViewTreeLifecycleOwner() != null
                        }
                    }
                )
            )
    }

    @Test
    fun preservesCompositionLocals() {
        val compositionLocal = compositionLocalOf<Float> { error("unset") }
        var value = 0f
        rule.setContent {
            CompositionLocalProvider(compositionLocal provides 1f) {
                Popup { value = compositionLocal.current }
            }
        }
        rule.runOnIdle { assertThat(value).isEqualTo(1f) }
    }

    @Test
    fun preservesLayoutDirection() {
        var value = LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Popup { value = LocalLayoutDirection.current }
            }
        }
        rule.runOnIdle { assertThat(value).isEqualTo(LayoutDirection.Rtl) }
    }

    @Test
    fun isDismissedOnTapOutside() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(alignment = Alignment.Center, onDismissRequest = { showPopup = false }) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        // Click outside the popup
        val outsideX = 0
        val outsideY =
            with(rule.density) {
                rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() /
                    2
            }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // Popup should not exist
        rule.onNodeWithTag(testTag).assertDoesNotExist()
    }

    @Test
    fun isDismissedOnBackPress() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        properties =
                            PopupProperties(
                                // Needs to be focusable to intercept back press
                                focusable = true
                            ),
                        alignment = Alignment.Center,
                        onDismissRequest = { showPopup = false },
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        Espresso.pressBack()

        // Popup should not exist
        rule.onNodeWithTag(testTag).assertDoesNotExist()
    }

    @Test
    fun isDismissedOnEscapePress() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        properties =
                            PopupProperties(
                                // Needs to be focusable to intercept key press
                                focusable = true
                            ),
                        alignment = Alignment.Center,
                        onDismissRequest = { showPopup = false },
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        UiDevice.getInstance(getInstrumentation()).pressKeyCode(KeyEvent.KEYCODE_ESCAPE)

        // Popup should not exist
        rule.onNodeWithTag(testTag).assertDoesNotExist()
    }

    @Test
    fun isNotDismissedOnTapOutside_dismissOnClickOutsideFalse() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        alignment = Alignment.Center,
                        properties = PopupProperties(dismissOnClickOutside = false),
                        onDismissRequest = { showPopup = false },
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        // Click outside the popup
        val outsideX = 0
        val outsideY =
            with(rule.density) {
                rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() /
                    2
            }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // Popup should still be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @Test
    fun isNotDismissedOnBackPress_dismissOnBackPressFalse() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        properties =
                            PopupProperties(
                                // Needs to be focusable to intercept back press
                                focusable = true,
                                dismissOnBackPress = false,
                            ),
                        alignment = Alignment.Center,
                        onDismissRequest = { showPopup = false },
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        Espresso.pressBack()

        // Popup should still be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @Test
    fun isNotDismissedOnEscapePress_dismissOnBackPressFalse() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        properties =
                            PopupProperties(
                                // Needs to be focusable to intercept key press
                                focusable = true,
                                dismissOnBackPress = false,
                            ),
                        alignment = Alignment.Center,
                        onDismissRequest = { showPopup = false },
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        UiDevice.getInstance(getInstrumentation()).pressKeyCode(KeyEvent.KEYCODE_ESCAPE)

        // Popup should still be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @Test
    fun canFillScreenWidth_dependingOnProperty() {
        var box1Width = 0
        var box2Width = 0
        rule.setContent {
            Popup { Box(Modifier.fillMaxSize().onSizeChanged { box1Width = it.width }) }
            Popup(properties = PopupProperties(usePlatformDefaultWidth = true)) {
                Box(Modifier.fillMaxSize().onSizeChanged { box2Width = it.width })
            }
        }
        rule.runOnIdle {
            val metrics =
                WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(rule.activity)
            assertThat(box1Width).isEqualTo(metrics.bounds.width())
            assertThat(box2Width).isLessThan(box1Width)
        }
    }

    @Test
    fun canChangeSize() {
        var width by mutableStateOf(10.dp)
        var usePlatformDefaultWidth by mutableStateOf(false)
        var actualWidth = 0

        rule.setContent {
            Popup(properties = PopupProperties(usePlatformDefaultWidth = usePlatformDefaultWidth)) {
                Box(Modifier.size(width, 150.dp).onSizeChanged { actualWidth = it.width })
            }
        }
        rule.runOnIdle {
            assertThat(actualWidth).isEqualTo((10 * rule.density.density).roundToInt())
        }
        width = 20.dp
        rule.runOnIdle {
            assertThat(actualWidth).isEqualTo((20 * rule.density.density).roundToInt())
        }

        usePlatformDefaultWidth = true

        width = 30.dp
        rule.runOnIdle {
            assertThat(actualWidth).isEqualTo((30 * rule.density.density).roundToInt())
        }
        width = 40.dp
        rule.runOnIdle {
            assertThat(actualWidth).isEqualTo((40 * rule.density.density).roundToInt())
        }
    }

    @Test
    fun customFlags() {
        val flags =
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

        rule.setContent {
            PopupTestTag(testTag) {
                Popup(properties = PopupProperties(flags = flags, inheritSecurePolicy = false)) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        // Make sure that current measurement/drawing is finished
        rule.runOnIdle {}
        val popupMatcher = PopupLayoutMatcher(testTag)
        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(popupMatcher)
            .check(matches(isDisplayed()))
        val capturedFlags = popupMatcher.lastSeenWindowParams!!.flags

        assertThat(capturedFlags and flags).isEqualTo(flags)
    }

    @Test
    fun didNotMeasureTooSmallLast() {
        rule.setContent { PopupTestTag(testTag) { Popup { Box(Modifier.fillMaxWidth()) } } }

        rule.popupMatches(
            testTag,
            object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description?) {
                    description?.appendText("Did not end up in MEASURE_STATE_TOO_SMALL")
                }

                override fun matchesSafely(item: View): Boolean {
                    val popupLayout = item.parent as ViewGroup
                    return popupLayout.measuredState != MEASURED_STATE_TOO_SMALL
                }
            },
        )
    }

    @Test
    fun doesNotMeasureContentMultipleTimes() {
        var measurements = 0
        rule.setContent {
            Popup {
                Box {
                    Layout({}) { _, constraints ->
                        ++measurements
                        // We size to maxWidth to make ViewRootImpl measure multiple times.
                        layout(constraints.maxWidth, 0) {}
                    }
                }
            }
        }
        rule.runOnIdle { assertThat(measurements).isEqualTo(1) }
    }

    @Test
    fun resizesWhenContentResizes() {
        val size1 = 20
        val size2 = 30
        var size by mutableStateOf(size1)
        rule.setContent {
            PopupTestTag(testTag) {
                Popup { Box(Modifier.size(with(rule.density) { size.toDp() })) }
            }
        }
        rule.popupMatches(testTag, matchesSize(20, 20))
        rule.runOnIdle { size = size2 }
        rule.popupMatches(testTag, matchesSize(30, 30))
    }

    @Test
    fun doesNotCrashWhenAnchorDetachedFirst() {
        var parent: FrameLayout? = null
        rule.setContent {
            AndroidView(
                factory = { context ->
                    FrameLayout(context)
                        .apply {
                            addView(
                                ComposeView(context).apply {
                                    setContent { Box { Popup { Box(Modifier.size(20.dp)) } } }
                                }
                            )
                        }
                        .also { parent = it }
                }
            )
        }

        rule.runOnIdle { parent!!.removeAllViews() }

        rule.waitForIdle()

        // Should not have crashed.
    }

    @Test
    fun nestedPopup_isPositioned_relativeToParentPopup() {
        val anchorTag = "anchor"
        val outerPopupTag = "outerPopup"
        val innerPopupTag = "innerPopup"
        var outerPopupPosition: Offset? = null
        var innerPopupPosition: Offset? = null

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                // An anchor Box in the center of the screen to host the popups.
                Box(Modifier.align(Alignment.Center).testTag(anchorTag)) {
                    // The outer popup is aligned to the TopStart of the anchor.
                    Popup(alignment = Alignment.TopStart) {
                        Box(
                            Modifier.size(100.dp).testTag(outerPopupTag).onGloballyPositioned {
                                // Capture the absolute screen coordinates of the outer popup.
                                outerPopupPosition = it.positionOnScreen()
                            }
                        ) {
                            // The nested popup is aligned to the TopStart of its parent Box.
                            Popup(alignment = Alignment.TopStart) {
                                Box(
                                    Modifier.size(20.dp)
                                        .testTag(innerPopupTag)
                                        .onGloballyPositioned {
                                            // Capture the absolute screen coordinates of the inner
                                            // popup.
                                            innerPopupPosition = it.positionOnScreen()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Wait for composition and layout to complete.
        rule.onNodeWithTag(anchorTag).assertExists()
        rule.onNodeWithTag(outerPopupTag).assertIsDisplayed()
        rule.onNodeWithTag(innerPopupTag).assertIsDisplayed()

        rule.runOnIdle {
            // Ensure that both popups were measured and their positions captured.
            assertThat(outerPopupPosition).isNotNull()
            assertThat(innerPopupPosition).isNotNull()

            // Since both popups are Top-aligned (TopStart), their x- and y-coordinates
            // on the screen should be the same. A small tolerance is used for floating-point
            // values.
            assertThat(innerPopupPosition!!.y).isWithin(0.1f).of(outerPopupPosition!!.y)
            assertThat(innerPopupPosition.x).isWithin(0.1f).of(outerPopupPosition.x)
        }
    }

    @Test
    fun nestedPopupInScrollingContainer_scrollsWithContainer_andInnerAnchorsToOuter() {
        val scrollTag = "scroll"
        val outerPopupTag = "outerPopup"
        val innerPopupTag = "innerPopup"
        var outerPopupPositionOnScreen: Offset? = null
        var innerPopupPositionOnScreen: Offset? = null

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { Box(Modifier.requiredHeight(350.dp).testTag(scrollTag)) }
                    item {
                        Box(Modifier.size(200.dp)) {
                            Popup(alignment = Alignment.TopStart) {
                                Box(
                                    Modifier.size(100.dp)
                                        .testTag(outerPopupTag)
                                        .onGloballyPositioned {
                                            outerPopupPositionOnScreen = it.positionOnScreen()
                                        }
                                ) {
                                    Popup(alignment = Alignment.TopStart) {
                                        Box(
                                            Modifier.size(50.dp)
                                                .testTag(innerPopupTag)
                                                .onGloballyPositioned {
                                                    innerPopupPositionOnScreen =
                                                        it.positionOnScreen()
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Box(Modifier.requiredHeight(1200.dp)) }
                }
            }
        }

        // Wait for composition and layout to complete.
        rule.onNodeWithTag(scrollTag).assertExists()
        rule.onNodeWithTag(outerPopupTag).assertIsDisplayed()
        rule.onNodeWithTag(innerPopupTag).assertIsDisplayed()

        // Capture initial positions
        val (initialOuterPopupPosition, initialInnerPopupPosition) =
            rule.runOnIdle {
                val outerPos = outerPopupPositionOnScreen
                val innerPos = innerPopupPositionOnScreen
                assertThat(outerPos).isNotNull()
                assertThat(innerPos).isNotNull()

                // Verify inner popup anchors to outer popup initially
                assertThat(innerPos!!.x).isWithin(1f).of(outerPos!!.x)
                assertThat(innerPos.y).isWithin(1f).of(outerPos.y)

                outerPos to innerPos
            }

        // Scroll the container by simulating a user swipe gesture.
        var scrollDistance = 0f
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(scrollTag).performTouchInput {
            scrollDistance = this.bottom - this.top
            swipeUp()
        }
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()

        // Now that we've synchronized, capture the new, stable positions.
        val (scrolledOuterPopupPosition, scrolledInnerPopupPosition) =
            rule.runOnIdle { outerPopupPositionOnScreen!! to innerPopupPositionOnScreen!! }

        // Verify outer popup position updated by the scroll amount
        assertThat(scrolledOuterPopupPosition.y)
            .isWithin(1f) // Use a 1px tolerance for pixel comparisons
            .of(initialOuterPopupPosition.y - scrollDistance)

        // Verify inner popup position also updated by the scroll amount
        assertThat(scrolledInnerPopupPosition.y)
            .isWithin(1f)
            .of(initialInnerPopupPosition.y - scrollDistance)

        // Verify inner popup still anchors to outer popup after scroll
        assertThat(scrolledInnerPopupPosition.x).isWithin(1f).of(scrolledOuterPopupPosition.x)
        assertThat(scrolledInnerPopupPosition.y).isWithin(1f).of(scrolledOuterPopupPosition.y)
    }

    @Test
    fun nonNestedPopup_LocalView_isAndroidComposeView() {
        var localView: View? = null

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Popup(alignment = Alignment.Center) {
                    localView = LocalView.current
                    Box(Modifier.size(10.dp).testTag("popupContent"))
                }
            }
        }
        rule.runOnIdle {
            assertThat(localView).isNotNull()
            assertThat(localView).isInstanceOf(AndroidComposeView::class.java)
        }
    }

    @Test
    fun anyPopup_Content_LocalIsInPopupLayout_isTrue() {
        var isInPopupLayout: Boolean? = null

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Popup(alignment = Alignment.Center) {
                    isInPopupLayout = LocalIsInPopupLayout.current
                    Box(Modifier.size(10.dp))
                }
            }
        }
        rule.runOnIdle { assertThat(isInPopupLayout).isTrue() }
    }

    /**
     * Verifies the LocalIsInPopupLayout values at different nesting levels and how they influence
     * the isNested flag for PopupLayout.
     */
    @Test
    fun nestedPopup_LocalIsInPopupLayout_TrueInNested() {
        var isInPopupOnL1Entry: Boolean? = null
        var isInPopupInL1Content: Boolean? = null
        var isInPopupOnL2Entry: Boolean? = null
        var isInPopupInL2Content: Boolean? = null

        rule.setContent {
            // Capture the value of LocalIsInPopupLayout *before* L1 Popup's provider.
            // This value determines PopupLayout L1's 'isNested' flag.
            isInPopupOnL1Entry = LocalIsInPopupLayout.current

            Popup(alignment = Alignment.TopStart) { // L1
                // Capture the value of LocalIsInPopupLayout *within* L1's content,
                // after L1's CompositionLocalProvider has set it to true.
                isInPopupInL1Content = LocalIsInPopupLayout.current

                // Capture the value of LocalIsInPopupLayout *before* L2 Popup's provider.
                // This value determines PopupLayout L2's 'isNested' flag.
                isInPopupOnL2Entry = LocalIsInPopupLayout.current

                Popup(alignment = Alignment.TopStart) { // L2
                    // Capture the value of LocalIsInPopupLayout *within* L2's content,
                    // after L2's CompositionLocalProvider has set it to true.
                    isInPopupInL2Content = LocalIsInPopupLayout.current
                    Box(Modifier.size(10.dp))
                }
            }
        }
        rule.runOnIdle {
            // L1 Popup is not nested within another Popup, so on entry, LocalIsInPopupLayout is
            // false.
            // This means PopupLayout for L1 gets isNested = false.
            assertThat(isInPopupOnL1Entry).isFalse()
            // Inside L1's content, its provider sets LocalIsInPopupLayout to true.
            assertThat(isInPopupInL1Content).isTrue()

            // L2 Popup is called from within L1's content, so on entry, LocalIsInPopupLayout is
            // true.
            // This means PopupLayout for L2 gets isNested = true.
            assertThat(isInPopupOnL2Entry).isTrue()
            // Inside L2's content, its provider also sets LocalIsInPopupLayout to true.
            assertThat(isInPopupInL2Content).isTrue()
        }
    }

    /**
     * Tests that a non-nested Popup is positioned correctly based on the absolute screen
     * coordinates returned by the PopupPositionProvider, assuming the root ComposeView is at the
     * screen origin (0,0). This is the default for most test setups and Edge-to-Edge enabled apps.
     */
    @Test
    fun nonNestedPopup_positioningIsBasedOnScreenCoordinates() {
        val popupTag = "popupContent"
        val popupSize = 20.dp
        // Define an arbitrary desired absolute screen position for the popup.
        val desiredScreenPos = IntOffset(123, 456)

        var actualPopupScreenOffset by mutableStateOf(IntOffset.Zero)

        // Custom position provider that always returns the desiredScreenPos.
        val fixedScreenPositionProvider =
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    return desiredScreenPos
                }
            }

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Popup(popupPositionProvider = fixedScreenPositionProvider, onDismissRequest = {}) {
                    Box(
                        Modifier.size(popupSize)
                            .background(Color.Green)
                            .testTag(popupTag)
                            .onGloballyPositioned {
                                actualPopupScreenOffset = it.positionOnScreen().round()
                            }
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.runOnIdle {
            // In a default test setup, the ComposeView's on-screen location is (0,0).
            // PopupLayout should calculate the window origin as (0,0).
            // Thus, params.x/y should equal desiredScreenPos.x/y.
            // The WindowManager places the popup at the window's origin (0,0) + params,
            // resulting in the popup appearing at desiredScreenPos on the screen.
            assertThat(actualPopupScreenOffset.x).isEqualTo(desiredScreenPos.x)
            assertThat(actualPopupScreenOffset.y).isEqualTo(desiredScreenPos.y)
        }
    }

    /**
     * Verifies that for a non-nested popup (standard case), the PopupPositionProvider receives
     * coordinates relative to the window, not absolute screen coordinates. This ensures that
     * floating windows (which are smaller than the screen) don't confuse the provider into thinking
     * the anchor is off-screen or positioned incorrectly relative to the window content.
     */
    @Test
    fun popupPositionProvider_receivesWindowRelativeCoordinates_whenNotNested() {
        val anchorTag = "anchor"
        var suppliedAnchorBounds: IntRect? = null
        var anchorPositionInWindow: Offset? = null

        val capturingProvider =
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    suppliedAnchorBounds = anchorBounds
                    return IntOffset.Zero
                }
            }

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.Center)
                        .size(50.dp)
                        .testTag(anchorTag)
                        .onGloballyPositioned { anchorPositionInWindow = it.positionInWindow() }
                ) {
                    Popup(popupPositionProvider = capturingProvider) { Box(Modifier.size(10.dp)) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(suppliedAnchorBounds).isNotNull()
            assertThat(anchorPositionInWindow).isNotNull()

            val anchorPos = anchorPositionInWindow!!
            // Verification: The bounds passed to the provider must match the window-relative
            // coordinates, NOT absolute screen coordinates.
            assertThat(suppliedAnchorBounds!!.left).isEqualTo(anchorPos.x.roundToInt())
            assertThat(suppliedAnchorBounds.top).isEqualTo(anchorPos.y.roundToInt())
        }
    }

    /**
     * Verifies that for a nested popup (a popup inside another popup), the PopupPositionProvider
     * receives absolute screen coordinates. This is required because nested popups are implemented
     * as sub-panels which the WindowManager expects to be positioned in absolute screen
     * coordinates.
     */
    @Test
    fun popupPositionProvider_receivesScreenCoordinates_whenNested() {
        val anchorTag = "anchor"
        var suppliedAnchorBounds: IntRect? = null
        var anchorPositionOnScreen: Offset? = null

        val capturingProvider =
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    suppliedAnchorBounds = anchorBounds
                    return IntOffset.Zero
                }
            }

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Popup(alignment = Alignment.Center) {
                    Box(
                        Modifier.size(50.dp).testTag(anchorTag).onGloballyPositioned {
                            anchorPositionOnScreen = it.positionOnScreen()
                        }
                    ) {
                        Popup(popupPositionProvider = capturingProvider) {
                            Box(Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(suppliedAnchorBounds).isNotNull()
            assertThat(anchorPositionOnScreen).isNotNull()

            val anchorPos = anchorPositionOnScreen!!
            // Verification: The bounds passed to the provider must match the absolute screen
            // coordinates.
            assertThat(suppliedAnchorBounds!!.left).isEqualTo(anchorPos.x.roundToInt())
            assertThat(suppliedAnchorBounds.top).isEqualTo(anchorPos.y.roundToInt())
        }
    }

    /**
     * Validates that non-nested Popups are correctly positioned on the screen, even when the host
     * ComposeView is offset from the screen origin.
     *
     * This test simulates legacy Android system inset behavior (relevant to b/454527215) by
     * embedding the ComposeView within a padded Android FrameLayout.
     *
     * It asserts that the Popup appears at the desired *absolute* screen coordinates returned by a
     * fixed PopupPositionProvider, confirming that the PopupLayout correctly calculates the parent
     * window's on-screen origin (0,0 in this test setup) and provides the appropriate
     * window-relative coordinates to the WindowManager.
     */
    @Test
    fun nonNestedPopup_withOffsetRootView_isPositionedCorrectlyOnScreen() {
        val popupTag = "popupContent"
        // An arbitrary absolute screen position a custom PopupPositionProvider will return.
        val desiredScreenPos = IntOffset(111, 222)
        // Simulate a status bar pushing the content down.
        val rootPaddingTop = 55.dp
        var actualPopupScreenOffset = IntOffset.Zero
        val composeViewLocationOnScreen = IntArray(2)

        // This provider always requests the popup to be at desiredScreenPos on the screen.
        val fixedScreenPositionProvider =
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    return desiredScreenPos
                }
            }

        var composeView: View? = null

        rule.setContent {
            // Use AndroidView to host a FrameLayout, which will contain the ComposeView.
            // Applying padding to the FrameLayout will offset the ComposeView on the screen.
            AndroidView(
                factory = { context ->
                    FrameLayout(context).apply {
                        val paddingPx = with(rule.density) { rootPaddingTop.roundToPx() }
                        setPadding(0, paddingPx, 0, 0) // Apply padding to the FrameLayout

                        val cv =
                            ComposeView(context).apply {
                                composeView = this // Capture the ComposeView instance
                                setContent {
                                    Popup(
                                        popupPositionProvider = fixedScreenPositionProvider,
                                        onDismissRequest = {},
                                    ) {
                                        Box(
                                            Modifier.size(10.dp)
                                                .background(Color.Red)
                                                .testTag(popupTag)
                                                .onGloballyPositioned { coordinates ->
                                                    actualPopupScreenOffset =
                                                        coordinates.positionOnScreen().round()
                                                }
                                        )
                                    }
                                }
                            }
                        addView(cv) // Add ComposeView to the padded FrameLayout
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        rule.waitForIdle() // Wait for layout and composition

        rule.runOnIdle {
            assertThat(composeView).isNotNull()
            // Get the on-screen location of the ComposeView
            composeView!!.getLocationOnScreen(composeViewLocationOnScreen)
            val composeViewY = composeViewLocationOnScreen[1]

            // Verify the ComposeView is offset from the screen top.
            val expectedOffset = with(rule.density) { rootPaddingTop.roundToPx() }
            assertThat(composeViewY).isEqualTo(expectedOffset)
            assertThat(composeViewY).isGreaterThan(0)

            // Despite the ComposeView being offset, the Popup should still appear at the
            // 'desiredScreenPos' on the screen. This confirms the PopupLayout
            // correctly calculated the window's on-screen origin (which should be (0,0)
            // in this test setup) and computed the WindowManager.LayoutParams relatively.
            assertThat(actualPopupScreenOffset.x).isEqualTo(desiredScreenPos.x)
            assertThat(actualPopupScreenOffset.y).isEqualTo(desiredScreenPos.y)
        }
    }

    @Test
    fun customWindowType() {
        val customType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

        rule.setContent {
            PopupTestTag(testTag) {
                Popup(properties = PopupProperties(windowType = customType)) {
                    Box(Modifier.size(50.dp))
                }
            }
        }

        // Ensure the popup is composed and the window is created
        rule.runOnIdle {}
        val popupMatcher = PopupLayoutMatcher(testTag)
        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(popupMatcher)
            .check(matches(isDisplayed()))

        // Verify the window type was correctly passed to the LayoutParams
        assertThat(popupMatcher.lastSeenWindowParams!!.type).isEqualTo(customType)
    }

    private fun matchesSize(width: Int, height: Int): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return item?.width == width && item.height == height
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with width = $width height = $height")
            }
        }
    }
}
