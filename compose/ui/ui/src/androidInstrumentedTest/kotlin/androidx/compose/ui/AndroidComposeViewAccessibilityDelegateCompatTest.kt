
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

package androidx.compose.ui

import android.graphics.Rect
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.text.SpannableString
import android.util.LongSparseArray
import android.view.View
import android.view.ViewStructure
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponseValue
import android.view.translation.ViewTranslationRequest
import android.view.translation.ViewTranslationRequest.ID_TEXT
import android.view.translation.ViewTranslationResponse
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.coreshims.ContentCaptureSessionCompat
import androidx.compose.ui.platform.coreshims.ViewStructureCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.clearTextSubstitution
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.isShowingTextSubstitution
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.setTextSubstitution
import androidx.compose.ui.semantics.showTextSubstitution
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.semantics.textSubstitution
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
import androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_COLLAPSE
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_DISMISS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_EXPAND
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_PASTE
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.doOnDetach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidComposeViewAccessibilityDelegateCompatTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private val tag = "tag"
    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var contentCaptureSessionCompat: ContentCaptureSessionCompat
    private lateinit var viewStructureCompat: ViewStructureCompat
    private val dispatchedAccessibilityEvents = mutableListOf<AccessibilityEvent>()
    private val accessibilityEventLoopIntervalMs = 100L
    private val contentCaptureEventLoopIntervalMs = 100L

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun testPopulateAccessibilityNodeInfoProperties_general() {
        // Arrange.
        val clickActionLabel = "click"
        val dismissActionLabel = "dismiss"
        val expandActionLabel = "expand"
        val collapseActionLabel = "collapse"
        val state = "checked"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics {
                        stateDescription = "checked"
                        testTag = tag
                        testTagsAsResourceId = true
                        heading()
                        onClick(clickActionLabel) { true }
                        dismiss(dismissActionLabel) { true }
                        expand(expandActionLabel) { true }
                        collapse(collapseActionLabel) { true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className).isEqualTo("android.view.View")
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsAtLeast(
                    AccessibilityActionCompat(ACTION_CLICK, clickActionLabel),
                    AccessibilityActionCompat(ACTION_DISMISS, dismissActionLabel),
                    AccessibilityActionCompat(ACTION_EXPAND, expandActionLabel),
                    AccessibilityActionCompat(ACTION_COLLAPSE, collapseActionLabel)
                )
            assertThat(info.stateDescription).isEqualTo(state)
            assertThat(info.viewIdResourceName).isEqualTo(tag)
            assertThat(info.isHeading).isTrue()
            assertThat(info.isClickable).isTrue()
            assertThat(info.isVisibleToUser).isTrue()
            assertThat(info.isImportantForAccessibility).isTrue()
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_screenReaderFocusable_mergingDescendants() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) { testTag = tag }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isScreenReaderFocusable).isTrue() }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_screenReaderFocusable_notMergingDescendants() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) { testTag = tag }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isScreenReaderFocusable).isFalse() }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_screenReaderFocusable_speakable() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        testTag = tag
                        text = AnnotatedString("Example text")
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isScreenReaderFocusable).isTrue() }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_disabled() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LocalClipboardManager.current.setText(AnnotatedString("test"))
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        disabled()
                        editableText = AnnotatedString("text")
                        horizontalScrollAxisRange = ScrollAxisRange({ 0f }, { 5f })
                        onClick { true }
                        onLongClick { true }
                        copyText { true }
                        pasteText { true }
                        cutText { true }
                        setText { true }
                        setSelection { _, _, _ -> true }
                        dismiss { true }
                        expand { true }
                        collapse { true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isClickable).isTrue()
            assertThat(info.isLongClickable).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsNoneOf(
                    AccessibilityActionCompat.ACTION_CLICK,
                    AccessibilityActionCompat.ACTION_COLLAPSE,
                    AccessibilityActionCompat.ACTION_CUT,
                    AccessibilityActionCompat.ACTION_DISMISS,
                    AccessibilityActionCompat.ACTION_EXPAND,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    AccessibilityActionCompat.ACTION_PAGE_DOWN,
                    AccessibilityActionCompat.ACTION_PAGE_LEFT,
                    AccessibilityActionCompat.ACTION_PAGE_RIGHT,
                    AccessibilityActionCompat.ACTION_PAGE_UP,
                    AccessibilityActionCompat.ACTION_PASTE,
                    AccessibilityActionCompat.ACTION_SET_TEXT,
                    AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
                    AccessibilityActionCompat.ACTION_SCROLL_RIGHT,
                )
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsAtLeast(
                    AccessibilityActionCompat.ACTION_COPY,
                    // This is the default ACTION_SET_SELECTION.
                    AccessibilityActionCompat.ACTION_SET_SELECTION,
                )
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_buttonRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.Button
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.Button") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_switchRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.Switch
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.view.View") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_switchRoleDescription() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.Switch
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.roleDescription).isEqualTo("Switch") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_checkBoxRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.Checkbox
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.CheckBox") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_radioButtonRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.RadioButton
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.RadioButton") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_tabRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.Tab
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.roleDescription).isEqualTo("Tab") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_imageRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        role = Role.Image
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.ImageView") }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_empty() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) { testTag = tag }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsImportant_emptyMerging() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) { testTag = tag }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    @OptIn(ExperimentalComposeUiApi::class)
    fun testIsNotImportant_testOnlyProperties() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        testTag = tag
                        testTagsAsResourceId = true
                        invisibleToUser()
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsImportant_accessibilitySpeakableProperties_stateDescription() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        testTag = tag
                        stateDescription = "stateDescription"
                        heading()
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsImportant_accessibilitySpeakableProperties_onClick() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        testTag = tag
                        onClick("clickLabel") { true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isTrue() }
    }

    private val PickedDateKey = SemanticsPropertyKey<Long>("PickedDate")
    private var SemanticsPropertyReceiver.pickedDate by PickedDateKey

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_customSemanticsProperty() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        testTag = tag
                        pickedDate = 1445378400 // 2015-10-21
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_clearedWithTestTag() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .clearAndSetSemantics { testTag = tag }
                    .semantics(mergeDescendants = true) { stateDescription = "stateDescription" }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("foo") })
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    fun nodeWithTextAndLayoutResult_className_textView() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        text = AnnotatedString("")
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.TextView") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_liveRegion() {
        // Arrange.
        val testTag1 = "TestTag1"
        val testTag2 = "TestTag2"
        rule.setContentWithAccessibilityEnabled {
            Row {
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics(mergeDescendants = true) {
                            testTag = testTag1
                            liveRegion = LiveRegionMode.Polite
                        }
                )
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics(mergeDescendants = true) {
                            testTag = testTag2
                            liveRegion = LiveRegionMode.Assertive
                        }
                )
            }
        }
        val virtualViewId1 = rule.onNodeWithTag(testTag1).semanticsId
        val virtualViewId2 = rule.onNodeWithTag(testTag2).semanticsId

        // Act.
        lateinit var info1: AccessibilityNodeInfoCompat
        lateinit var info2: AccessibilityNodeInfoCompat
        rule.runOnIdle {
            info1 = androidComposeView.createAccessibilityNodeInfo(virtualViewId1)
            info2 = androidComposeView.createAccessibilityNodeInfo(virtualViewId2)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(info1.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            assertThat(info2.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE)
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_liveRegionUpdate() {
        // Arrange.
        var liveRegionMode by mutableStateOf(LiveRegionMode.Polite)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        liveRegion = liveRegionMode
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId
        val info1 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        dispatchedAccessibilityEvents.clear()

        // Act.
        rule.runOnIdle { liveRegionMode = LiveRegionMode.Assertive }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info1.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
            assertThat(info2.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE)
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_SeekBar() {
        // Arrange.
        val setProgressActionLabel = "setProgress"
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        progressBarRangeInfo = ProgressBarRangeInfo(0.5f, 0f..1f, 6)
                        setProgress(setProgressActionLabel) { true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className).isEqualTo("android.widget.SeekBar")
            assertThat(info.rangeInfo.type).isEqualTo(RANGE_TYPE_FLOAT)
            assertThat(info.rangeInfo.current).isEqualTo(0.5f)
            assertThat(info.rangeInfo.min).isEqualTo(0f)
            assertThat(info.rangeInfo.max).isEqualTo(1f)
            if (SDK_INT >= Build.VERSION_CODES.N) {
                assertThat(info.actionList)
                    .comparingElementsUsing(IdAndLabel)
                    .contains(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionSetProgress,
                            setProgressActionLabel
                        )
                    )
            }
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_textField() {
        // Arrange.
        val setSelectionActionLabel = "setSelection"
        val setTextActionLabel = "setText"
        val text = "hello"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        editableText = AnnotatedString(text)
                        textSelectionRange = TextRange(1)
                        focused = true
                        getTextLayoutResult { true }
                        setText(setTextActionLabel) { true }
                        setSelection(setSelectionActionLabel) { _, _, _ -> true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className).isEqualTo("android.widget.EditText")
            assertThat(info.text).isEqualTo(SpannableString(text))
            assertThat(info.isFocusable).isTrue()
            assertThat(info.isFocused).isTrue()
            assertThat(info.isEditable).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsAtLeast(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                        setSelectionActionLabel
                    ),
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_TEXT,
                        setTextActionLabel
                    ),
                    AccessibilityActionCompat
                        .ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                    AccessibilityActionCompat
                        .ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                )
            if (SDK_INT >= 26) {
                assertThat(info.unwrap().availableExtraData)
                    .containsExactly(
                        "androidx.compose.ui.semantics.id",
                        "androidx.compose.ui.semantics.testTag",
                        AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
                    )
            }
        }
    }

    @Test
    fun testMovementGranularities_textField_focused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        editableText = AnnotatedString("text")
                        textSelectionRange = TextRange(1)
                        focused = true
                        getTextLayoutResult { true }
                        setText { true }
                        setSelection { _, _, _ -> true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.movementGranularities).isEqualTo(
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
            )
        }
    }

    @Test
    fun testMovementGranularities_textField_notFocused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        editableText = AnnotatedString("text")
                        textSelectionRange = TextRange(1)
                        getTextLayoutResult { true }
                        setText { true }
                        setSelection { _, _, _ -> true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.movementGranularities).isEqualTo(
                AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
            )
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_setContentInvalid_customDescription() {
        // Arrange.
        val errorDescription = "Invalid format"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        error(errorDescription)
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isContentInvalid).isTrue()
            assertThat(info.error).isEqualTo(errorDescription)
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_setContentInvalid_emptyDescription() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        error("")
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isContentInvalid).isTrue()
            assertThat(info.error.isEmpty()).isTrue()
        }
    }

    @Test
    fun test_PasteAction_ifFocused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LocalClipboardManager.current.setText(AnnotatedString("test"))
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        focused = true
                        pasteText { true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isFocused).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .contains(AccessibilityActionCompat(ACTION_PASTE, null))
        }
    }

    @Test
    fun test_noPasteAction_ifUnfocused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LocalClipboardManager.current.setText(AnnotatedString("test"))
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        pasteText { true }
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isFocused).isFalse()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .doesNotContain(AccessibilityActionCompat(ACTION_PASTE, null))
        }
    }

    @Test
    fun testActionCanBeNull() {
        // Arrange.
        val actionLabel = "send"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        onClick(label = actionLabel, action = null)
                    }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        // When action is null here, should we still think it is clickable? Should we add the action
        // to AccessibilityNodeInfo?
        rule.runOnIdle {
            assertThat(info.isClickable).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .contains(AccessibilityActionCompat(ACTION_CLICK, actionLabel))
        }
    }

    @Test
    fun sendScrollEvent_byStateObservation_horizontal() {
        // Arrange.
        var scrollValue by mutableStateOf(0f, structuralEqualityPolicy())
        val scrollMaxValue = 100f
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Row(
                Modifier
                    .size(20.toDp(), 10.toDp())
                    .semantics(mergeDescendants = false) {
                        horizontalScrollAxisRange = ScrollAxisRange(
                            { scrollValue },
                            { scrollMaxValue }
                        )
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
                androidComposeView.accessibilityNodeProvider
                    .performAction(virtualViewId, ACTION_ACCESSIBILITY_FOCUS, null)
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
            val focusedANI = androidComposeView.accessibilityNodeProvider
                .findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            assertThat(Rect().also { focusedANI?.getBoundsInScreen(it) })
                .isEqualTo(Rect(10, 0, 20, 10))
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_ACCESSIBILITY_FOCUSED
                    },
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
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        verticalScrollAxisRange = ScrollAxisRange(
                            { scrollValue },
                            { scrollMaxValue }
                        )
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
    fun sendWindowContentChangeUndefinedEventByDefault_whenPropertyAdded() {
        // Arrange.
        var addProperty by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
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
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
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
    fun sendWindowContentChangeUndefinedEventByDefault_onlyOnce_whenMultiplePropertiesChange() {
        // Arrange.
        var propertiesChanged by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
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
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
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
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
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
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        customActions = if (!newAction) {
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
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = false) {
                        customActions = if (!newAction) {
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

    @Test
    fun testUncoveredNodes_notPlacedNodes_notIncluded() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Row(Modifier.size(10.toDp())) {
                Box(Modifier.size(10.toDp()).semantics {})
                Box(Modifier.size(10.toDp()).semantics {})
                Box(Modifier.size(10.toDp()).semantics {})
            }
        }

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo() }

        // Assert.
        rule.runOnIdle { assertThat(info.childCount).isEqualTo(1) }
    }

    @Test
    fun testUncoveredNodes_zeroBoundsRoot_included() {
        // Arrange.
        val bounds = Rect(-1, -1, -1, -1)
        rule.setContentWithAccessibilityEnabled {
            Box { }
        }

        // Act.
        rule.runOnIdle {
            val info = androidComposeView.createAccessibilityNodeInfo()
            info.getBoundsInScreen(bounds)
        }

        // Assert.
        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(0, 0, 0, 0)) }
    }

    @Test
    fun testContentDescriptionCastSuccess() {
        // Arrange.
        var hasContentDescription by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        if (hasContentDescription) {
                            contentDescription = "Hello" // To trigger content description casting.
                        }
                    }
            )
        }

        // Act.
        rule.runOnIdle { hasContentDescription = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
                        contentDescription = "Hello"
                    }
                )
        }
    }

    @Test
    fun canScroll_returnsFalse_whenPositionInvalid() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(100.dp)
                    .semantics(mergeDescendants = true) {
                        horizontalScrollAxisRange = ScrollAxisRange(
                            value = { 0f },
                            maxValue = { 1f },
                            reverseScrolling = false
                        )
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        horizontalScrollAxisRange = ScrollAxisRange(
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        verticalScrollAxisRange = ScrollAxisRange(
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        horizontalScrollAxisRange = ScrollAxisRange(
                            value = { 1f },
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        horizontalScrollAxisRange = ScrollAxisRange(
                            value = { 0f },
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        verticalScrollAxisRange = ScrollAxisRange(
                            value = { 1f },
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        verticalScrollAxisRange = ScrollAxisRange(
                            value = { 0f },
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        horizontalScrollAxisRange = ScrollAxisRange(
                            value = { 0f },
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
                Modifier
                    .size(100.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        horizontalScrollAxisRange = ScrollAxisRange(
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
    fun canScroll_returnsFalse_whenTouchIsOutsideBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(50.toDp())
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        horizontalScrollAxisRange = ScrollAxisRange(
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

    // TODO(b/272068594): Asserting that a list does not contain an element can be an incorrect test
    //  because this would pass even if the event was present, (For example when isEnabled = false).
    //  Keeping this here to show parity for code review. This can be removed because the test
    //  passwordVisibilityToggle_fromInvisibleToVisible_sendTwoSelectionEvents covers this case.
    @Test
    fun passwordVisibilityToggle_fromInvisibleToVisible_doNotSendTextChangeEvent() {
        // Arrange.
        var passwordVisible by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        setText { true }
                        if (passwordVisible) password()
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                    }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .doesNotContain(AccessibilityEvent().apply { eventType = TYPE_VIEW_TEXT_CHANGED })
        }
    }

    // TODO(b/272068594): Asserting that a list does not contain an element can be an incorrect test
    //  because this would pass even if the event was present, (For example when isEnabled = false).
    //  Keeping this here to show parity for code review. This can be removed because the test
    //  passwordVisibilityToggle_fromVisibleToInvisible_sendTwoSelectionEvents covers this case.
    @Test
    fun passwordVisibilityToggle_fromVisibleToInvisible_doNotSendTextChangeEvent() {
        // Arrange.
        var passwordVisible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        setText { true }
                        if (passwordVisible) password()
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                    }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = false }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .doesNotContain(AccessibilityEvent().apply { eventType = TYPE_VIEW_TEXT_CHANGED })
        }
    }

    @Test
    fun passwordVisibilityToggle_fromInvisibleToVisible_sendTwoSelectionEvents() {
        // Arrange.
        var passwordVisible by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        setText { true }
                        if (passwordVisible) password()
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                    }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("1234")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                        isPassword = true
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("1234")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                        isPassword = true
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        isPassword = true
                    },
                )
        }
    }

    @Test
    fun passwordVisibilityToggle_fromVisibleToInvisible_sendTwoSelectionEvents() {
        // Arrange.
        var passwordVisible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        setText { true }
                        if (passwordVisible) password()
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                    }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = false }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("****")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("****")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                    },
                )
        }
    }

    @Test
    fun textChanged_sendTextChangeEvent() {
        // Arrange.
        var textChanged by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        testTag = tag
                        setText { true }
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (!textChanged) "1234" else "1235")
                    }
            )
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        rule.runOnIdle { textChanged = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_CHANGED
                        className = "android.widget.EditText"
                        text.add("1235")
                        beforeText = "1234"
                        fromIndex = 3
                        addedCount = 1
                        removedCount = 1
                        setSource(androidComposeView, virtualId)
                    }
                )
        }
    }

    @Test
    fun textChanged_passwordNode_sendTextChangeEvent() {
        // Arrange.
        var textChanged by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier
                    .size(10.dp)
                    .semantics(mergeDescendants = true) {
                        setText { true }
                        password()
                        textSelectionRange = TextRange(4)
                        editableText = AnnotatedString(if (!textChanged) "1234" else "1235")
                    }
            )
        }

        // Act.
        rule.runOnIdle { textChanged = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_CHANGED
                        className = "android.widget.EditText"
                        text.add("1235")
                        beforeText = "1234"
                        fromIndex = 3
                        addedCount = 1
                        removedCount = 1
                        isPassword = true
                    }
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testInitContentCaptureSemanticsStructureChangeEvents_onStart() {
        // Arrange.
        rule.setContentWithContentCaptureEnabled(retainInteractionsDuringInitialization = true) {}

        // Act - Wait for initialization that is triggered by onStart().

        // Assert = verify the root node appeared.
        rule.runOnIdle {
            verify(contentCaptureSessionCompat).newVirtualViewStructure(any(), any())
            verify(contentCaptureSessionCompat).notifyViewsAppeared(any())
            verify(viewStructureCompat).setDimens(any(), any(), any(), any(), any(), any())
            verify(viewStructureCompat).toViewStructure()
            verifyNoMoreInteractions(contentCaptureSessionCompat)
            verifyNoMoreInteractions(viewStructureCompat)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testInitContentCaptureSemanticsStructureChangeEvents_onStop() {
        // Arrange.
        rule.setContentWithContentCaptureEnabled {}

        // Act.
        rule.runOnIdle {
            androidComposeView.doOnDetach {

                // Assert.
                verify(contentCaptureSessionCompat).notifyViewsDisappeared(any())
                verifyNoMoreInteractions(contentCaptureSessionCompat)
                verifyNoMoreInteractions(viewStructureCompat)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_appeared() {
        // Arrange.
        var appeared by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Row(Modifier.size(100.dp).semantics {}) {
                if (appeared) {
                    Box(Modifier.size(10.dp).semantics { text = AnnotatedString("foo") })
                    Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") })
                }
            }
        }

        // Act.
        rule.runOnIdle { appeared = true }
        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<CharSequence>()) {
                verify(viewStructureCompat, times(2)).setText(capture())
                assertThat(firstValue).isEqualTo("foo")
                assertThat(secondValue).isEqualTo("bar")
            }
            verify(contentCaptureSessionCompat, times(0)).notifyViewsDisappeared(any())
            with(argumentCaptor<List<ViewStructure>>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsAppeared(capture())
                assertThat(firstValue.count()).isEqualTo(2)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_disappeared() {
        // Arrange.
        var disappeared by mutableStateOf(false)

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            if (!disappeared) {
                Row(Modifier.size(100.dp).semantics { }) {
                    Box(Modifier.size(10.dp).semantics { })
                    Box(Modifier.size(10.dp).semantics { })
                }
            }
        }

        // Act.
        rule.runOnIdle { disappeared = true }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<LongArray>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsDisappeared(capture())
                assertThat(firstValue.count()).isEqualTo(3)
            }
            verify(contentCaptureSessionCompat, times(0)).notifyViewsAppeared(any())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_appearedAndDisappeared() {
        // Arrange.
        var appeared by mutableStateOf(false)

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            if (appeared) {
                Row(Modifier.size(100.dp).semantics { }) {
                    Box(Modifier.size(10.dp).semantics { })
                    Box(Modifier.size(10.dp).semantics { })
                }
            }
        }

        // Act.
        rule.runOnIdle { appeared = true }
        // TODO(b/272068594): This test was written to ensure that if the items appeared and
        //  disappeared before the 100ms, it would still report the items that were added and the
        //  items that were removed The items were (As long as the items had different IDs). However
        //  it is not possible for a items with different IDs to disappear as they are not existing.
        //  The mocks also limit us to write this test since we can't mock AutofillIDs since
        //  AutofillId is a final class, and these tests just use the autofill id of the parent
        //  view.
        rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
        rule.runOnIdle { appeared = false }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for
        //  two invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<LongArray>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsDisappeared(capture())
                assertThat(firstValue.count()).isEqualTo(3)
            }
            with(argumentCaptor<List<ViewStructure>>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsAppeared(capture())
                assertThat(firstValue.count()).isEqualTo(3)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun testSendContentCaptureSemanticsStructureChangeEvents_sameNodeAppearedThenDisappeared() {
        // Arrange.
        var appeared by mutableStateOf(false)

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { }) {
                if (appeared) {
                    Box(Modifier.size(10.dp).semantics { })
                }
            }
        }

        // Act.
        rule.runOnIdle { appeared = true }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            verify(contentCaptureSessionCompat, times(0)).notifyViewsDisappeared(any())
            with(argumentCaptor<List<ViewStructure>>()) {
                verify(contentCaptureSessionCompat, times(1)).notifyViewsAppeared(capture())
                assertThat(firstValue.count()).isEqualTo(1)
            }
            clearInvocations(contentCaptureSessionCompat)
        }

        rule.runOnIdle { appeared = false }

        // TODO(b/272068594): After refactoring this code, ensure that we don't need to wait for two
        //  invocations of boundsUpdatesEventLoop.
        repeat(2) {
            rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)
            rule.waitForIdle()
        }

        // Assert.
        rule.runOnIdle {
            verify(contentCaptureSessionCompat, times(0)).notifyViewsDisappeared(any())
            verify(contentCaptureSessionCompat, times(0)).notifyViewsAppeared(any())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testUpdateTranslationOnAppeared_showOriginal() {
        // Arrange.
        var appeared by mutableStateOf(false)
        var result = true

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { }) {
                if (appeared) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .semantics {
                                text = AnnotatedString("foo")
                                isShowingTextSubstitution = true
                                showTextSubstitution {
                                    result = it
                                    true
                                }
                            }
                    )
                }
            }
        }
        rule.runOnIdle { androidComposeView.composeAccessibilityDelegate.onHideTranslation() }

        // Act.
        rule.runOnIdle { appeared = true }
        rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle { assertThat(result).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testUpdateTranslationOnAppeared_showTranslated() {
        // Arrange.
        var appeared by mutableStateOf(false)
        var result = false

        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { }) {
                if (appeared) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .semantics {
                                text = AnnotatedString("foo")
                                isShowingTextSubstitution = false
                                showTextSubstitution {
                                    result = it
                                    true
                                }
                            }
                    )
                }
            }
        }
        rule.runOnIdle { androidComposeView.composeAccessibilityDelegate.onShowTranslation() }

        // Act.
        rule.runOnIdle { appeared = true }
        rule.mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle { assertThat(result).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnCreateVirtualViewTranslationRequests() {
        // Arrange.
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics {
                            testTag = tag
                            text = AnnotatedString("foo")
                        }
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        val ids = LongArray(1).apply { this[0] = virtualViewId.toLong() }
        val requestsCollector: Consumer<ViewTranslationRequest?> = mock()

        // Act.
        rule.runOnIdle {
            androidComposeView.onCreateVirtualViewTranslationRequests(
                ids,
                IntArray(0),
                requestsCollector
            )
        }

        // Assert.
        rule.runOnIdle {
            with(argumentCaptor<ViewTranslationRequest>()) {
                verify(requestsCollector).accept(capture())
                assertThat(firstValue).isEqualTo(
                    ViewTranslationRequest
                        .Builder(androidComposeView.autofillId, virtualViewId.toLong())
                        .setValue(ID_TEXT, TranslationRequestValue.forText(AnnotatedString("foo")))
                        .build()
                )
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnVirtualViewTranslationResponses() {
        // Arrange.
        var result: AnnotatedString? = null
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics {
                            testTag = tag
                            text = AnnotatedString("foo")
                            setTextSubstitution {
                                result = it
                                true
                            }
                        }
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        rule.runOnIdle {
            androidComposeView.onVirtualViewTranslationResponses(
                LongSparseArray<ViewTranslationResponse?>().apply {
                    append(
                        virtualViewId.toLong(),
                        ViewTranslationResponse
                            .Builder(androidComposeView.autofillId)
                            .setValue(
                                ID_TEXT,
                                TranslationResponseValue.Builder(0).setText("bar").build()
                            )
                            .build()
                    )
                }
            )
        }

        // Assert.
        rule.runOnIdle { assertThat(result).isEqualTo(AnnotatedString("bar")) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnShowTranslation() {
        // Arrange.
        var result = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics {
                            textSubstitution = AnnotatedString("foo")
                            isShowingTextSubstitution = false
                            showTextSubstitution {
                                result = it
                                true
                            }
                        }
                )
            }
        }

        // Act.
        rule.runOnIdle { androidComposeView.composeAccessibilityDelegate.onShowTranslation() }

        // Assert.
        rule.runOnIdle { assertThat(result).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnHideTranslation() {
        // Arrange.
        var result = true
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics {
                            text = AnnotatedString("bar")
                            textSubstitution = AnnotatedString("foo")
                            isShowingTextSubstitution = true
                            showTextSubstitution {
                                result = it
                                true
                            }
                        }
                )
            }
        }

        // Act.
        rule.runOnIdle { androidComposeView.composeAccessibilityDelegate.onHideTranslation() }

        // Assert.
        rule.runOnIdle { assertThat(result).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun testOnClearTranslation() {
        // Arrange.
        var result = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithContentCaptureEnabled {
            Box(Modifier.size(10.dp).semantics { text = AnnotatedString("bar") }) {
                Box(
                    Modifier
                        .size(10.dp)
                        .semantics {
                            text = AnnotatedString("bar")
                            isShowingTextSubstitution = true
                            clearTextSubstitution {
                                result = true
                                true
                            }
                        }
                )
            }
        }

        // Act.
        rule.runOnIdle { androidComposeView.composeAccessibilityDelegate.onClearTranslation() }

        // Assert.
        rule.runOnIdle { assertThat(result).isTrue() }
    }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            with(androidComposeView.composeAccessibilityDelegate) {
                accessibilityForceEnabledForTesting = true
                onSendAccessibilityEvent = { dispatchedAccessibilityEvents += it; false }
            }
            content()
        }

        // Advance the clock past the first accessibility event loop, and clear the initial
        // events as we are want the assertions to check the events that were generated later.
        runOnIdle { mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs) }
        runOnIdle { dispatchedAccessibilityEvents.clear() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.setContentWithContentCaptureEnabled(
        retainInteractionsDuringInitialization: Boolean = false,
        content: @Composable () -> Unit
    ) {
        contentCaptureSessionCompat = mock()
        viewStructureCompat = mock()
        val viewStructure: ViewStructure = mock()

        whenever(contentCaptureSessionCompat.newVirtualViewStructure(any(), any()))
            .thenReturn(viewStructureCompat)
        whenever(viewStructureCompat.toViewStructure())
            .thenReturn(viewStructure)

        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            with(androidComposeView.composeAccessibilityDelegate) {
                accessibilityForceEnabledForTesting = true
                contentCaptureForceEnabledForTesting = true
                contentCaptureSession = contentCaptureSessionCompat
                onSendAccessibilityEvent = { dispatchedAccessibilityEvents += it; false }
            }

            whenever(contentCaptureSessionCompat.newAutofillId(any())).thenAnswer {
                androidComposeView.autofillId
            }

            content()
        }

        // Advance the clock past the first accessibility event loop, and clear the initial
        // as we are want the assertions to check the events that were generated later.
        runOnIdle { mainClock.advanceTimeBy(contentCaptureEventLoopIntervalMs) }

        runOnIdle {
            if (!retainInteractionsDuringInitialization) {
                clearInvocations(contentCaptureSessionCompat, viewStructureCompat)
            }
        }
    }

    private fun AndroidComposeView.createAccessibilityNodeInfo(
        semanticsId: Int
    ): AccessibilityNodeInfoCompat {
        onSemanticsChange()
        val accNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(semanticsId)
        checkNotNull(accNodeInfo) { "Could not find semantics node with id = $semanticsId" }
        return AccessibilityNodeInfoCompat.wrap(accNodeInfo)
    }

    companion object {
        internal val IdAndLabel =
            Correspondence.from<AccessibilityActionCompat, AccessibilityActionCompat>(
                { actual, expected ->
                    actual != null && expected != null &&
                        actual.id == expected.id &&
                        actual.label == expected.label
                },
                "has same id and label as"
            )

        internal val AccessibilityEventComparator = Correspondence
            .from<AccessibilityEvent, AccessibilityEvent>(
                { actual, expected ->
                    actual != null && expected != null &&
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
        get() = ViewCompat.getAccessibilityDelegate(this)
            as AndroidComposeViewAccessibilityDelegateCompat

    // TODO(b/272068594): Add api to fetch the semantics id from SemanticsNodeInteraction directly.
    private val SemanticsNodeInteraction.semanticsId: Int get() = fetchSemanticsNode().id

    // TODO(b/304359126): Move this to AccessibilityEventCompat and use it wherever we use obtain().
    private fun AccessibilityEvent(): AccessibilityEvent = if (SDK_INT >= R) {
        android.view.accessibility.AccessibilityEvent()
    } else {
        @Suppress("DEPRECATION")
        AccessibilityEvent.obtain()
    }.apply {
        packageName = "androidx.compose.ui.test"
        className = "android.view.View"
        isEnabled = true
    }
}
