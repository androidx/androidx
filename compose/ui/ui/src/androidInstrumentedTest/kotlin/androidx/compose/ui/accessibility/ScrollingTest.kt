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

package androidx.compose.ui.accessibility

import android.graphics.Rect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.getScrollViewportLength
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollingTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val tag = "tag"
    private lateinit var androidComposeView: AndroidComposeView
    private val dispatchedAccessibilityEvents = mutableListOf<AccessibilityEvent>()
    private val accessibilityEventLoopIntervalMs = 100L

    @SdkSuppress(maxSdkVersion = 33) // b/322354981
    @Test
    fun sendScrollEvent_byStateObservation_horizontal() {
        // Arrange.
        var scrollValue by mutableStateOf(0f, structuralEqualityPolicy())
        val scrollMaxValue = 100f
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Row(
                Modifier.size(20.toDp(), 10.toDp()).semantics(mergeDescendants = false) {
                    horizontalScrollAxisRange = ScrollAxisRange({ scrollValue }, { scrollMaxValue })
                }
            ) {
                Text("foo", Modifier.size(10.toDp()))
                Text("bar", Modifier.size(10.toDp()).testTag(tag))
            }
        }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId
        rule.runOnIdle { dispatchedAccessibilityEvents.clear() }

        // Act.
        try {
            androidComposeView.snapshotObserver.startObserving()
            rule.runOnIdle {
                androidComposeView.accessibilityNodeProvider.performAction(
                    virtualViewId,
                    ACTION_ACCESSIBILITY_FOCUS,
                    null
                )
                Snapshot.notifyObjectsInitialized()
                scrollValue = 2f
                Snapshot.sendApplyNotifications()
            }
        } finally {
            androidComposeView.snapshotObserver.stopObserving()
        }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            val focusedANI =
                androidComposeView.accessibilityNodeProvider.findFocus(
                    AccessibilityNodeInfo.FOCUS_ACCESSIBILITY
                )
            assertThat(Rect().also { focusedANI?.getBoundsInScreen(it) })
                .isEqualTo(Rect(10, 0, 20, 10))
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply { eventType = TYPE_VIEW_ACCESSIBILITY_FOCUSED },
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_SUBTREE
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_SCROLLED
                        scrollX = 2
                        maxScrollX = 100
                    },
                )
        }
    }

    @Test
    fun sendScrollEvent_byStateObservation_vertical() {
        // Arrange.
        var scrollValue by mutableStateOf(0f, structuralEqualityPolicy())
        val scrollMaxValue = 100f
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    verticalScrollAxisRange = ScrollAxisRange({ scrollValue }, { scrollMaxValue })
                }
            )
        }

        // TODO(b/272068594): We receive an extra TYPE_WINDOW_CONTENT_CHANGED event 100ms after
        //  setup. So we wait an extra 100ms here so that this test is not affected by that extra
        //  event.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        dispatchedAccessibilityEvents.clear()

        // Act.
        try {
            androidComposeView.snapshotObserver.startObserving()
            rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
            rule.runOnIdle {
                Snapshot.notifyObjectsInitialized()
                scrollValue = 2f
                Snapshot.sendApplyNotifications()
            }
        } finally {
            androidComposeView.snapshotObserver.stopObserving()
        }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_SUBTREE
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_SCROLLED
                        scrollY = 2
                        maxScrollY = 100
                    },
                )
        }
    }

    @Test
    fun canScroll_returnsFalse_whenPositionInvalid() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.dp).semantics(mergeDescendants = true) {
                    horizontalScrollAxisRange =
                        ScrollAxisRange(value = { 0f }, maxValue = { 1f }, reverseScrolling = false)
                }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollHorizontally(1)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(0)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(-1)).isFalse()
        }
    }

    @Test
    fun canScroll_returnsTrue_whenHorizontalScrollableNotAtLimit() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(
                            value = { 0.5f },
                            maxValue = { 1f },
                            reverseScrolling = false
                        )
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            // Should be scrollable in both directions.
            assertThat(androidComposeView.canScrollHorizontally(1)).isTrue()
            assertThat(androidComposeView.canScrollHorizontally(0)).isTrue()
            assertThat(androidComposeView.canScrollHorizontally(-1)).isTrue()
        }
    }

    @Test
    fun canScroll_returnsTrue_whenVerticalScrollableNotAtLimit() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    verticalScrollAxisRange =
                        ScrollAxisRange(
                            value = { 0.5f },
                            maxValue = { 1f },
                            reverseScrolling = false
                        )
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            // Should be scrollable in both directions.
            assertThat(androidComposeView.canScrollVertically(1)).isTrue()
            assertThat(androidComposeView.canScrollVertically(0)).isTrue()
            assertThat(androidComposeView.canScrollVertically(-1)).isTrue()
        }
    }

    @Test
    fun canScroll_returnsFalse_whenHorizontalScrollable_whenScrolledRightAndAtLimit() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(value = { 1f }, maxValue = { 1f }, reverseScrolling = false)
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollHorizontally(1)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(0)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(-1)).isTrue()
        }
    }

    @Test
    fun canScroll_returnsFalse_whenHorizontalScrollable_whenScrolledLeftAndAtLimit() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(value = { 0f }, maxValue = { 1f }, reverseScrolling = false)
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollHorizontally(1)).isTrue()
            assertThat(androidComposeView.canScrollHorizontally(0)).isTrue()
            assertThat(androidComposeView.canScrollHorizontally(-1)).isFalse()
        }
    }

    @Test
    fun canScroll_returnsFalse_whenVerticalScrollable_whenScrolledDownAndAtLimit() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    verticalScrollAxisRange =
                        ScrollAxisRange(value = { 1f }, maxValue = { 1f }, reverseScrolling = false)
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollVertically(1)).isFalse()
            assertThat(androidComposeView.canScrollVertically(0)).isFalse()
            assertThat(androidComposeView.canScrollVertically(-1)).isTrue()
        }
    }

    @Test
    fun canScroll_returnsFalse_whenVerticalScrollable_whenScrolledUpAndAtLimit() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    verticalScrollAxisRange =
                        ScrollAxisRange(value = { 0f }, maxValue = { 1f }, reverseScrolling = false)
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollVertically(1)).isTrue()
            assertThat(androidComposeView.canScrollVertically(0)).isTrue()
            assertThat(androidComposeView.canScrollVertically(-1)).isFalse()
        }
    }

    @Test
    fun canScroll_respectsReverseDirection() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(value = { 0f }, maxValue = { 1f }, reverseScrolling = true)
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollHorizontally(1)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(0)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(-1)).isTrue()
        }
    }

    @Test
    fun canScroll_returnsFalse_forVertical_whenScrollableIsHorizontal() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(100.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(
                            value = { 0.5f },
                            maxValue = { 1f },
                            reverseScrolling = true
                        )
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(50f, 50f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollVertically(1)).isFalse()
            assertThat(androidComposeView.canScrollVertically(0)).isFalse()
            assertThat(androidComposeView.canScrollVertically(-1)).isFalse()
        }
    }

    @Test
    fun scrollViewPort_notProvided_shouldUseFallbackViewPort() {
        // Arrange.
        var actualScrolledAmount = 0f
        val viewPortSize = 100
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(viewPortSize.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(
                            value = { 0.5f },
                            maxValue = { 1f },
                            reverseScrolling = true
                        )

                    scrollBy { x, _ ->
                        actualScrolledAmount += x
                        false
                    }
                }
            )
        }

        val virtualViewId = rule.onNodeWithTag(tag).semanticsId
        rule.runOnIdle {
            androidComposeView.accessibilityNodeProvider.performAction(
                virtualViewId,
                ACTION_SCROLL_BACKWARD,
                null
            )
        }
        assertThat(actualScrolledAmount).isEqualTo(viewPortSize)
    }

    @Test
    fun scrollViewPort_provided_shouldUseScrollProvidedValues() {
        // Arrange.
        var actualScrolledAmount = 0f
        val viewPortSize = 100
        val contentPadding = 5f
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(viewPortSize.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(
                            value = { 0.5f },
                            maxValue = { 1f },
                            reverseScrolling = true
                        )

                    scrollBy { x, _ ->
                        actualScrolledAmount += x
                        false
                    }

                    getScrollViewportLength { viewPortSize - contentPadding }
                }
            )
        }

        val virtualViewId = rule.onNodeWithTag(tag).semanticsId
        rule.runOnIdle {
            androidComposeView.accessibilityNodeProvider.performAction(
                virtualViewId,
                ACTION_SCROLL_BACKWARD,
                null
            )
        }
        assertThat(actualScrolledAmount).isEqualTo(viewPortSize - contentPadding)
    }

    @Test
    fun canScroll_returnsFalse_whenTouchIsOutsideBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(50.toDp()).semantics(mergeDescendants = true) {
                    testTag = tag
                    horizontalScrollAxisRange =
                        ScrollAxisRange(
                            value = { 0.5f },
                            maxValue = { 1f },
                            reverseScrolling = true
                        )
                }
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performTouchInput { down(Offset(100f, 100f)) }

        // Assert.
        rule.runOnIdle {
            assertThat(androidComposeView.canScrollHorizontally(1)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(0)).isFalse()
            assertThat(androidComposeView.canScrollHorizontally(-1)).isFalse()
        }
    }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            with(androidComposeView.composeAccessibilityDelegate) {
                accessibilityForceEnabledForTesting = true
                onSendAccessibilityEvent = {
                    dispatchedAccessibilityEvents += it
                    false
                }
            }
            content()
        }

        // Advance the clock past the first accessibility event loop, and clear the initial
        // events as we are want the assertions to check the events that were generated later.
        runOnIdle { mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs) }
        runOnIdle { dispatchedAccessibilityEvents.clear() }
    }

    companion object {

        internal val AccessibilityEventComparator =
            Correspondence.from<AccessibilityEvent, AccessibilityEvent>(
                { actual, expected ->
                    actual != null &&
                        expected != null &&
                        actual.eventType == expected.eventType &&
                        actual.eventTime == expected.eventTime &&
                        actual.packageName == expected.packageName &&
                        actual.movementGranularity == expected.movementGranularity &&
                        actual.action == expected.action &&
                        actual.contentChangeTypes == expected.contentChangeTypes &&
                        (SDK_INT < P || actual.windowChanges == expected.windowChanges) &&
                        actual.className.contentEquals(expected.className) &&
                        actual.text.toString() == expected.text.toString() &&
                        actual.contentDescription.contentEquals(expected.contentDescription) &&
                        actual.itemCount == expected.itemCount &&
                        actual.currentItemIndex == expected.currentItemIndex &&
                        actual.isEnabled == expected.isEnabled &&
                        actual.isPassword == expected.isPassword &&
                        actual.isChecked == expected.isChecked &&
                        actual.isFullScreen == expected.isFullScreen &&
                        actual.isScrollable == expected.isScrollable &&
                        actual.beforeText.contentEquals(expected.beforeText) &&
                        actual.fromIndex == expected.fromIndex &&
                        actual.toIndex == expected.toIndex &&
                        actual.scrollX == expected.scrollX &&
                        actual.scrollY == expected.scrollY &&
                        actual.maxScrollX == expected.maxScrollX &&
                        actual.maxScrollY == expected.maxScrollY &&
                        (SDK_INT < P || actual.scrollDeltaX == expected.scrollDeltaX) &&
                        (SDK_INT < P || actual.scrollDeltaY == expected.scrollDeltaY) &&
                        actual.addedCount == expected.addedCount &&
                        actual.removedCount == expected.removedCount &&
                        actual.parcelableData == expected.parcelableData &&
                        actual.recordCount == expected.recordCount
                },
                "has same properties as"
            )
    }

    private val View.composeAccessibilityDelegate: AndroidComposeViewAccessibilityDelegateCompat
        get() =
            ViewCompat.getAccessibilityDelegate(this)
                as AndroidComposeViewAccessibilityDelegateCompat

    // TODO(b/272068594): Add api to fetch the semantics id from SemanticsNodeInteraction directly.
    private val SemanticsNodeInteraction.semanticsId: Int
        get() = fetchSemanticsNode().id

    // TODO(b/304359126): Move this to AccessibilityEventCompat and use it wherever we use obtain().
    private fun AccessibilityEvent(): AccessibilityEvent =
        if (SDK_INT >= R) {
                android.view.accessibility.AccessibilityEvent()
            } else {
                @Suppress("DEPRECATION") AccessibilityEvent.obtain()
            }
            .apply {
                packageName = "androidx.compose.ui.test"
                className = "android.view.View"
                isEnabled = true
            }
}
