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

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowContentChangeTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var androidComposeView: AndroidComposeView
    private val dispatchedAccessibilityEvents = mutableListOf<AccessibilityEvent>()
    private val accessibilityEventLoopIntervalMs = 100L

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_whenPropertyAdded() {
        // Arrange.
        var addProperty by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    if (addProperty) disabled()
                }
            )
        }

        // Act.
        rule.runOnIdle { addProperty = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
        }
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_whenPropertyRemoved() {
        // Arrange.
        var removeProperty by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    if (!removeProperty) disabled()
                }
            )
        }

        // Act.
        rule.runOnIdle { removeProperty = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
        }
    }

    @Test
    @Ignore("b/307823561")
    fun sendWindowContentChangeUndefinedEventByDefault_onlyOnce_whenMultiplePropertiesChange() {
        // Arrange.
        var propertiesChanged by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    if (!propertiesChanged) {
                        disabled()
                    } else {
                        onClick { true }
                    }
                }
            )
        }

        // Act.
        rule.runOnIdle { propertiesChanged = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
        }
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_standardActionWithTheSameLabel() {
        // Arrange.
        var newAction by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    if (!newAction) {
                        onClick(label = "action") { true }
                    } else {
                        onClick(label = "action") { true }
                    }
                }
            )
        }

        // Act.
        rule.runOnIdle { newAction = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle { assertThat(dispatchedAccessibilityEvents).isEmpty() }
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_standardActionWithDifferentLabels() {
        // Arrange.
        var newAction by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    if (!newAction) {
                        onClick(label = "action1") { true }
                    } else {
                        onClick(label = "action2") { true }
                    }
                }
            )
        }

        // Act.
        rule.runOnIdle { newAction = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
        }
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_customActionWithTheSameLabel() {
        // Arrange.
        var newAction by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    customActions =
                        if (!newAction) {
                            listOf(CustomAccessibilityAction("action") { true })
                        } else {
                            listOf(CustomAccessibilityAction("action") { false })
                        }
                }
            )
        }

        // Act.
        rule.runOnIdle { newAction = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle { assertThat(dispatchedAccessibilityEvents).isEmpty() }
    }

    @Test
    fun sendWindowContentChangeUndefinedEventByDefault_customActionWithDifferentLabels() {
        // Arrange.
        var newAction by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    customActions =
                        if (!newAction) {
                            listOf(CustomAccessibilityAction("action1") { true })
                        } else {
                            listOf(CustomAccessibilityAction("action2") { true })
                        }
                }
            )
        }

        // Act.
        rule.runOnIdle { newAction = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
        }
    }

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
