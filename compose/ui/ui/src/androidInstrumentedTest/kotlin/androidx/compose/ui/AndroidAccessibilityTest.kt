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

import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_EXIT
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.DrawerValue
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.FilterChip
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.expectError
import androidx.compose.ui.AndroidComposeViewAccessibilityDelegateCompatTest.Companion.AccessibilityEventComparator
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ClassName
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.InvalidId
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.TextFieldClassName
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions.SetText
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
import androidx.compose.ui.semantics.SemanticsProperties.EditableText
import androidx.compose.ui.semantics.SemanticsProperties.Focused
import androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.pageUp
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isHiddenFromAccessibility
import androidx.compose.ui.test.isInHiddenAccessibilitySubtree
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_APPEARED
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_TITLE
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_SUBTREE
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_UNDEFINED
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_FOCUS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_LONG_CLICK
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SET_SELECTION
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SET_TEXT
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_IME_ENTER
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.FOCUS_INPUT
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.Serializable
import java.lang.reflect.Method
import java.util.Date
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@LargeTest
@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class AndroidAccessibilityTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val accessibilityEventLoopIntervalMs = 100L
    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var container: OpenComposeView
    private lateinit var delegate: AndroidComposeViewAccessibilityDelegateCompat
    private lateinit var provider: AccessibilityNodeProviderCompat
    private lateinit var resources: Resources

    private val accessibilityManager: AccessibilityManager
        get() =
            androidComposeView.context.getSystemService(ACCESSIBILITY_SERVICE)
                as AccessibilityManager

    private val tag = "Tag"
    private val argument =
        ArgumentCaptor.forClass(android.view.accessibility.AccessibilityEvent::class.java)

    @Before
    fun setup() {
        // Use uiAutomation to enable accessibility manager.
        InstrumentationRegistry.getInstrumentation().uiAutomation
        resources = InstrumentationRegistry.getInstrumentation().context.resources

        rule.activityRule.scenario.onActivity { activity ->
            container =
                spy(OpenComposeView(activity)) {
                        on { onRequestSendAccessibilityEvent(any(), any()) } doReturn false
                    }
                    .apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }

            activity.setContentView(container)
            androidComposeView = container.getChildAt(0) as AndroidComposeView
            delegate =
                ViewCompat.getAccessibilityDelegate(androidComposeView)
                    as AndroidComposeViewAccessibilityDelegateCompat
            delegate.accessibilityForceEnabledForTesting = true
            provider = delegate.getAccessibilityNodeProvider(androidComposeView)
        }
    }

    @After
    fun teardown() {
        delegate.requestFromAccessibilityToolForTesting = null
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forToggleable() {
        // Arrange.
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(value = checked, onValueChange = { checked = it }).testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.view.View")
                assertThat(isClickable).isTrue()
                assertThat(isVisibleToUser).isTrue()
                assertThat(isCheckable).isTrue()
                // TODO(b/406574577): Remove suppression once 1.17.0 stable is released.
                @Suppress("DEPRECATION") assertThat(isChecked).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, "toggle"),
                        AccessibilityActionCompat(ACTION_FOCUS, "toggle"),
                        AccessibilityActionCompat(ACTION_CLICK, "toggle"),
                    )
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forSwitch() {
        // Arrange.
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(
                        value = checked,
                        role = Role.Switch,
                        onValueChange = { checked = it },
                    )
                    .testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }
        val toggleableNode =
            rule.onNodeWithTag(tag, true).fetchSemanticsNode("couldn't find node with tag $tag")
        val switchRoleNode = toggleableNode.replacedChildren.last()

        // Act.
        rule.waitForIdle()
        val info = createAccessibilityNodeInfo(toggleableNode.id)
        val switchRoleInfo = createAccessibilityNodeInfo(switchRoleNode.id)

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(stateDescription).isEqualTo("On")
                assertThat(isClickable).isTrue()
                assertThat(isVisibleToUser).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                        AccessibilityActionCompat(ACTION_CLICK, null),
                    )
            }

            // We temporary send Switch role as a separate fake node
            with(switchRoleInfo) { assertThat(className).isEqualTo("android.view.View") }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forSecureTextField() {
        // Arrange.
        setContent {
            Surface {
                // BasicSecureTextField is considered a password field.
                BasicSecureTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(tag),
                )
            }
        }

        val passwordFieldId = rule.onNodeWithTag(tag, true).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(passwordFieldId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.widget.EditText")
                assertThat(isPassword).isTrue()
                assertThat(isFocusable).isTrue()
                assertThat(isFocused).isFalse()
                assertThat(isEditable).isTrue()
                assertThat(isVisibleToUser).isTrue()
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forDropdown() {
        // Arrange.
        setContent {
            var expanded by remember { mutableStateOf(false) }
            IconButton(
                modifier = Modifier.semantics { role = Role.DropdownList }.testTag(tag),
                onClick = { expanded = true },
            ) {
                Icon(Icons.Default.MoreVert, null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(24.dp, 0.dp),
            ) {
                repeat(5) { DropdownMenuItem(onClick = {}) { Text("Menu Item $it") } }
            }
        }
        val virtualId = rule.onNodeWithTag(tag, true).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.widget.Spinner")
                assertThat(isClickable).isTrue()
                assertThat(isVisibleToUser).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                        AccessibilityActionCompat(ACTION_CLICK, null),
                    )
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forSelectable() {
        // Arrange.
        setContent {
            Box(Modifier.selectable(selected = true, onClick = {}).testTag(tag)) {
                BasicText("Text")
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.view.View")
                assertThat(stateDescription).isEqualTo("Selected")
                assertThat(isClickable).isTrue()
                assertThat(isCheckable).isTrue()
                assertThat(isVisibleToUser).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                        AccessibilityActionCompat(ACTION_CLICK, null),
                    )
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forTab() {
        // Arrange.
        setContent {
            Box(Modifier.selectable(selected = true, onClick = {}, role = Role.Tab).testTag(tag)) {
                BasicText("Text")
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.view.View")
                assertThat(stateDescription).isNull()
                assertThat(isClickable).isFalse()
                assertThat(isVisibleToUser).isTrue()
                assertThat(isSelected).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                    )
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forRadioButton() {
        // Arrange.
        setContent {
            Box(
                Modifier.selectable(selected = true, onClick = {}, role = Role.RadioButton)
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.view.View")
                assertThat(isClickable).isFalse()
                assertThat(isVisibleToUser).isTrue()
                // TODO(b/406574577): Remove suppression once 1.17.0 stable is released.
                @Suppress("DEPRECATION") assertThat(isChecked).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                    )
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forFilterButton() {
        // Arrange.
        setContent {
            FilterChip(selected = true, onClick = {}, modifier = Modifier.testTag(tag)) {
                Text("Filter chip")
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                // We don't check for a CheckBox role since the role is found in a fake descendant.
                assertThat(stateDescription).isEqualTo("Selected")
                assertThat(isClickable).isTrue()
                assertThat(isCheckable).isTrue()
                // TODO(b/406574577): Remove suppression once 1.17.0 stable is released.
                @Suppress("DEPRECATION") assertThat(isChecked).isTrue()
                assertThat(isVisibleToUser).isTrue()
                assertThat(actionList).contains(AccessibilityActionCompat(ACTION_CLICK, null))
            }
        }
    }

    @SdkSuppress(minSdkVersion = 29) // Pager a11y actions are only available from API 29
    @Test
    fun testCreateAccessibilityNodeInfo_pagerActionsWithoutRole_shouldSetPagerActions() {
        // Arrange.
        setContent {
            Box(Modifier.semantics { pageUp { true } }.testTag(tag)) { BasicText("Text") }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(actionList)
                    .contains(
                        AccessibilityActionCompat(android.R.id.accessibilityActionPageUp, null)
                    )
            }
        }
    }

    @SdkSuppress(minSdkVersion = 29) // Pager a11y actions are only available from API 29
    @Test
    fun testCreateAccessibilityNodeInfo_pagerActionsRole_shouldNotSetPagerActions() {
        // Arrange.
        setContent {
            Box(
                Modifier.semantics { role = Role.Carousel }
                    .semantics { pageUp { true } }
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(actionList)
                    .doesNotContain(
                        AccessibilityActionCompat(android.R.id.accessibilityActionPageUp, null)
                    )
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_numberPicker_expectedClassName() {
        // Arrange.
        setContent { Box(Modifier.semantics { role = Role.ValuePicker }.testTag(tag)) }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) { assertThat(className).isEqualTo("android.widget.NumberPicker") }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_progressIndicator_determinate() {
        // Arrange.
        setContent { Box(Modifier.progressSemantics(0.5f).testTag(tag)) { BasicText("Text") } }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.widget.ProgressBar")
                assertThat(stateDescription).isEqualTo("50 percent.")
                assertThat(rangeInfo.type).isEqualTo(RANGE_TYPE_FLOAT)
                assertThat(rangeInfo.current).isEqualTo(0.5f)
                assertThat(rangeInfo.min).isEqualTo(0f)
                assertThat(rangeInfo.max).isEqualTo(1f)

                assertThat(actionList)
                    .containsExactly(AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null))
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_progressIndicator_determinate_indeterminate() {
        // Arrange.
        setContent { Box(Modifier.progressSemantics().testTag(tag)) { BasicText("Text") } }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.widget.ProgressBar")
                assertThat(stateDescription).isEqualTo("In progress")
                assertThat(rangeInfo).isNull()
                assertThat(actionList)
                    .containsExactly(AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null))
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forTextField() {
        // Arrange.
        setContent {
            var value by remember { mutableStateOf(TextFieldValue("hello")) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it },
            )
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(className).isEqualTo("android.widget.EditText")
                assertThat(text.toString()).isEqualTo("hello")
                assertThat(isFocusable).isTrue()
                assertThat(isFocused).isFalse()
                assertThat(isEditable).isTrue()
                assertThat(isVisibleToUser).isTrue()
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_CLICK, null),
                        AccessibilityActionCompat(ACTION_LONG_CLICK, null),
                        AccessibilityActionCompat(ACTION_SET_TEXT, null),
                        AccessibilityActionCompat(ACTION_IME_ENTER.id, null),
                        AccessibilityActionCompat(ACTION_SET_SELECTION, null),
                        AccessibilityActionCompat(ACTION_NEXT_AT_MOVEMENT_GRANULARITY, null),
                        AccessibilityActionCompat(ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, null),
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                    )
                if (Build.VERSION.SDK_INT >= 26) {
                    assertThat(availableExtraData)
                        .containsExactly(
                            "androidx.compose.ui.semantics.id",
                            // TODO(b/272068594): This looks like a bug. This should be
                            //  AccessibilityNodeInfoCompat.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
                            EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
                            "androidx.compose.ui.semantics.testTag",
                        )
                }
            }
        }
    }

    @Test
    fun emptyTextField_hasStateDescription() {
        setContent { BasicTextField(rememberTextFieldState(), modifier = Modifier.testTag(tag)) }

        val virtualId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        rule.runOnIdle {
            with(info) {
                assertThat(stateDescription).isEqualTo(resources.getString(R.string.state_empty))
            }
        }
    }

    @Test
    fun emptyTextField_noSpeakableChild_hasStateDescription() {
        setContent {
            BasicTextField("", {}, modifier = Modifier.testTag(tag)) {
                Column {
                    it()
                    Button(onClick = {}) {}
                    Box(Modifier.size(10.dp).semantics { testTag = "unspeakable child" })
                }
            }
        }

        val virtualId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        rule.runOnIdle {
            with(info) {
                assertThat(stateDescription).isEqualTo(resources.getString(R.string.state_empty))
            }
        }
    }

    @Test
    fun emptyTextField_hasSpeakableChild_noStateDescription_() {
        setContent {
            BasicTextField(
                rememberTextFieldState(),
                modifier = Modifier.testTag(tag),
                decorator = {
                    Row {
                        it()
                        BasicText(text = "Label")
                    }
                },
            )
        }

        val virtualId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        rule.runOnIdle { with(info) { assertThat(stateDescription).isNull() } }
    }

    @Test
    fun emptyTextField_hasSpeakableIndirectChild_noStateDescription_() {
        setContent {
            BasicTextField(
                rememberTextFieldState(),
                modifier = Modifier.testTag(tag),
                decorator = {
                    Row {
                        it()
                        Box(
                            modifier =
                                Modifier.wrapContentSize().semantics { testTag = "box test tag" }
                        ) {
                            BasicText(text = "Label")
                        }
                    }
                },
            )
        }

        val virtualId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        rule.runOnIdle { with(info) { assertThat(stateDescription).isNull() } }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forText() {
        // Arrange.
        val text = "Test"
        setContent { BasicText(text = text) }
        val virtualId = rule.onNodeWithText(text).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle { with(info) { assertThat(className).isEqualTo("android.widget.TextView") } }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forFocusable_notFocused() {
        // Arrange.
        setContent { Box(Modifier.testTag(tag).focusable()) { BasicText("focusable") } }
        val virtualId = rule.onNodeWithTag(tag).assert(expectValue(Focused, false)).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_FOCUS, null),
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                    )
                @Suppress("DEPRECATION") recycle()
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_forFocusable_focused() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            Box(Modifier.testTag(tag).focusRequester(focusRequester).focusable()) {
                BasicText("focusable")
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        val virtualId = rule.onNodeWithTag(tag).assert(expectValue(Focused, true)).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info) {
                assertThat(actionList)
                    .containsExactly(
                        AccessibilityActionCompat(ACTION_CLEAR_FOCUS, null),
                        AccessibilityActionCompat(ACTION_ACCESSIBILITY_FOCUS, null),
                    )
                @Suppress("DEPRECATION") recycle()
            }
        }
    }

    @Test
    fun testCreateAccessibilityNodeInfo_visibleToToolIfSensitiveData() {
        delegate.requestFromAccessibilityToolForTesting = true

        val colTag = "column"
        val text = "Test"
        container.setContent {
            Column(Modifier.testTag(colTag).semantics { isTraversalGroup = false }) {
                BasicText(text = text, modifier = Modifier.semantics { isSensitiveData = true })
            }
        }
        val columnNode = rule.onNodeWithTag(colTag).fetchSemanticsNode()
        val columnNodeInfo = provider.createAccessibilityNodeInfo(columnNode.id)
        val textNode = rule.onNodeWithText(text).fetchSemanticsNode()
        val textNodeInfo = provider.createAccessibilityNodeInfo(textNode.id)

        assertThat(columnNodeInfo).isNotNull()
        assertThat(columnNodeInfo?.childCount).isEqualTo(1)
        assertThat(textNodeInfo).isNotNull()
    }

    @Test
    fun testCreateAccessibilityNodeInfo_visibleToNontoolIfNotSensitiveData() {
        delegate.requestFromAccessibilityToolForTesting = false

        val colTag = "column"
        val text = "Test"
        container.setContent {
            Column(Modifier.testTag(colTag).semantics { isTraversalGroup = false }) {
                BasicText(text = text, modifier = Modifier.semantics { isSensitiveData = false })
            }
        }
        val columnNode = rule.onNodeWithTag(colTag).fetchSemanticsNode()
        val columnNodeInfo = provider.createAccessibilityNodeInfo(columnNode.id)
        val textNode = rule.onNodeWithText(text).fetchSemanticsNode()
        val textNodeInfo = provider.createAccessibilityNodeInfo(textNode.id)

        assertThat(columnNodeInfo).isNotNull()
        assertThat(columnNodeInfo?.childCount).isEqualTo(1)
        assertThat(textNodeInfo).isNotNull()
    }

    @Test
    fun testCreateAccessibilityNodeInfo_hiddenFromNontoolIfSensitiveData() {
        delegate.requestFromAccessibilityToolForTesting = false

        val colTag = "column"
        val text = "Test"
        container.setContent {
            Column(Modifier.testTag(colTag).semantics { isTraversalGroup = false }) {
                BasicText(text = text, modifier = Modifier.semantics { isSensitiveData = true })
            }
        }
        val columnNode = rule.onNodeWithTag(colTag).fetchSemanticsNode()
        val columnNodeInfo = provider.createAccessibilityNodeInfo(columnNode.id)
        val textNode = rule.onNodeWithText(text).fetchSemanticsNode()
        val textNodeInfo = provider.createAccessibilityNodeInfo(textNode.id)

        assertThat(columnNodeInfo).isNotNull()
        assertThat(columnNodeInfo?.childCount).isEqualTo(0)
        assertThat(textNodeInfo).isNull()
    }

    @Composable
    fun LastElementOverLaidColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        var yPosition = 0
        Layout(modifier = modifier, content = content) { measurables, constraints ->
            val placeables = measurables.map { measurable -> measurable.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { placeable ->
                    if (placeable != placeables[placeables.lastIndex]) {
                        placeable.placeRelative(x = 0, y = yPosition)
                        yPosition += placeable.height
                    } else {
                        // if the element is our last element (our overlaid node)
                        // then we'll put it over the middle of our previous elements
                        placeable.placeRelative(x = 0, y = yPosition / 2)
                    }
                }
            }
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_forTraversalBefore_overlaidNodeLayout() {
        // Arrange.
        val overlaidText = "Overlaid node text"
        val text1 = "Lorem1 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text2 = "Lorem2 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text3 = "Lorem3 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        setContent {
            LastElementOverLaidColumn(modifier = Modifier.padding(8.dp)) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row { Text(overlaidText) }
            }
        }
        val node3VirtualId = rule.onNodeWithText(text3).semanticsId()
        val overlaidNodeVirtualId = rule.onNodeWithText(overlaidText).semanticsId()

        // Act.
        val ani3 = rule.runOnIdle { createAccessibilityNodeInfo(node3VirtualId) }

        // Assert.
        // Nodes 1, 2, and 3 are all children of a larger column; this means with a hierarchy
        // comparison (like SemanticsSort), the third text node should come before the overlaid node
        // — OverlaidNode should be read last
        rule.runOnIdle {
            assertThat(ani3.extras.traversalBefore).isNotEqualTo(0)
            assertThat(ani3.extras.traversalBefore).isEqualTo(overlaidNodeVirtualId)
            assertThat(getAccessibilityNodeInfoSourceSemanticsNodeId(ani3) == node3VirtualId)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_forTraversalAfter_overlaidNodeLayout() {
        // Arrange.
        val overlaidText = "Overlaid node text"
        val text1 = "Lorem1 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text2 = "Lorem2 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        val text3 = "Lorem3 ipsum dolor sit amet, consectetur adipiscing elit.\n"
        setContent {
            LastElementOverLaidColumn(modifier = Modifier.padding(8.dp)) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row { Text(overlaidText) }
            }
        }
        val node3VirtualId = rule.onNodeWithText(text3).semanticsId()
        val overlaidNodeVirtualId = rule.onNodeWithText(overlaidText).semanticsId()

        // Act.
        val ani3 = rule.runOnIdle { createAccessibilityNodeInfo(node3VirtualId) }
        val overlaidANI = rule.runOnIdle { createAccessibilityNodeInfo(overlaidNodeVirtualId) }

        // Assert.
        rule.runOnIdle {
            // Nodes 1, 2, and 3 are all children of a larger column; this means with a hierarchy
            // comparison (like SemanticsSort), the third text node should come before the overlaid
            // node
            // — OverlaidNode should be read last
            assertThat(ani3.extras.traversalBefore).isNotEqualTo(0)
            assertThat(ani3.extras.traversalBefore).isEqualTo(overlaidNodeVirtualId)

            // Older versions of Samsung voice assistant crash if both traversalBefore
            // and traversalAfter redundantly express the same ordering relation, so
            // we should only have traversalBefore here.
            assertThat(overlaidANI.extras.traversalAfter).isEqualTo(0)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_readableTraversalGroups() {
        // Arrange.
        val clickableRowTag = "readableRow"
        val clickableButtonTag = "readableButton"
        setContent {
            Column {
                Row(
                    Modifier.testTag(clickableRowTag)
                        .semantics { isTraversalGroup = true }
                        .clickable {}
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "fab icon")
                    Button(onClick = {}, modifier = Modifier.testTag(clickableButtonTag)) {
                        Text("First button")
                    }
                }
            }
        }
        val rowVirtualId = rule.onNodeWithTag(clickableRowTag).semanticsId()
        val buttonId = rule.onNodeWithTag(clickableButtonTag).semanticsId()

        // Act.
        val rowANI = rule.runOnIdle { createAccessibilityNodeInfo(rowVirtualId) }

        // Assert - Since the column is screenReaderFocusable, it comes before the button.
        rule.runOnIdle { assertThat(rowANI.extras.traversalBefore).isEqualTo(buttonId) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalGroupClipping() {
        // Arrange.
        val clickableTitle = "clickableTitle"
        val clickableFirstListElement = "firstListElement"

        setContent {
            Box {
                LazyColumn {
                    items(50) { index ->
                        Box(
                            Modifier.fillMaxWidth()
                                .clickable {}
                                .padding(16.dp)
                                .then(
                                    if (index == 0) Modifier.testTag(clickableFirstListElement)
                                    else Modifier
                                )
                        ) {
                            Text("Item #${index + 1}")
                        }
                    }
                }
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Black.copy(alpha = 0.9f))
                            .semantics { isTraversalGroup = true }
                ) {
                    Text(
                        "Testing Box Covering First Elements",
                        Modifier.align(Alignment.Center).testTag(clickableTitle),
                    )
                }
            }
        }

        val titleId = rule.onNodeWithTag(clickableTitle).semanticsId()
        val firstElementId = rule.onNodeWithTag(clickableFirstListElement).semanticsId()

        // Act.
        val titleANI = rule.runOnIdle { createAccessibilityNodeInfo(titleId) }

        // Assert - both the title and element are readable; though the title box covers the first
        // element, it does not clip the items below.
        rule.runOnIdle { assertThat(titleANI.extras.traversalBefore).isEqualTo(firstElementId) }
    }

    @Composable
    fun CardRow(
        modifier: Modifier,
        columnNumber: Int,
        topSampleText: String,
        bottomSampleText: String,
    ) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Column {
                Text(topSampleText + columnNumber)
                Text(bottomSampleText + columnNumber)
            }
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_peerTraversalGroups_traversalIndex() {
        // Arrange.
        val topSampleText = "Top text in column "
        val bottomSampleText = "Bottom text in column "
        setContent {
            Column(Modifier.testTag("Test Tag").semantics { isTraversalGroup = false }) {
                Row {
                    Modifier.semantics { isTraversalGroup = false }
                    CardRow(
                        // Setting a bigger traversalIndex here means that this CardRow will be
                        // read second, even though it is visually to the left of the other CardRow
                        Modifier.semantics { isTraversalGroup = true }
                            .semantics { traversalIndex = 1f },
                        1,
                        topSampleText,
                        bottomSampleText,
                    )
                    CardRow(
                        Modifier.semantics { isTraversalGroup = true },
                        2,
                        topSampleText,
                        bottomSampleText,
                    )
                }
            }
        }
        val topText1 = rule.onNodeWithText(topSampleText + 1).semanticsId()
        val topText2 = rule.onNodeWithText(topSampleText + 2).semanticsId()
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).semanticsId()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).semanticsId()

        // Act.
        rule.waitForIdle()
        val topText1ANI = createAccessibilityNodeInfo(topText1)
        val topText2ANI = createAccessibilityNodeInfo(topText2)
        val bottomText2ANI = createAccessibilityNodeInfo(bottomText2)

        // Assert.
        // Expected behavior: "Top text in column 2" -> "Bottom text in column 2" ->
        // "Top text in column 1" -> "Bottom text in column 1"
        rule.runOnIdle {
            assertThat(topText2ANI.extras.traversalBefore).isAtMost(bottomText2)
            assertThat(bottomText2ANI.extras.traversalBefore).isAtMost(topText1)
            assertThat(topText1ANI.extras.traversalBefore).isAtMost(bottomText1)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedTraversalGroups_outerFalse() {
        // Arrange.
        val topSampleText = "Top text in column "
        val bottomSampleText = "Bottom text in column "
        setContent {
            Column(Modifier.testTag("Test Tag").semantics { isTraversalGroup = false }) {
                Row {
                    Modifier.semantics { isTraversalGroup = false }
                    CardRow(
                        Modifier.semantics { isTraversalGroup = true },
                        1,
                        topSampleText,
                        bottomSampleText,
                    )
                    CardRow(
                        Modifier.semantics { isTraversalGroup = true },
                        2,
                        topSampleText,
                        bottomSampleText,
                    )
                }
            }
        }
        val topText1 = rule.onNodeWithText(topSampleText + 1).semanticsId()
        val topText2 = rule.onNodeWithText(topSampleText + 2).semanticsId()
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).semanticsId()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).semanticsId()

        // Act.
        rule.waitForIdle()
        val topText1ANI = createAccessibilityNodeInfo(topText1)
        val topText2ANI = createAccessibilityNodeInfo(topText2)

        // Assert.
        // Here we have the following hierarchy of containers:
        // `isTraversalGroup = false`
        //    `isTraversalGroup = false`
        //       `isTraversalGroup = true`
        //       `isTraversalGroup = true`
        // meaning the behavior should be as if the first two `isTraversalGroup = false` are not
        // present and all of column 1 should be read before column 2.
        rule.runOnIdle {
            assertThat(topText1ANI.extras.traversalBefore).isEqualTo(bottomText1)
            assertThat(topText2ANI.extras.traversalBefore).isEqualTo(bottomText2)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedTraversalGroups_outerTrue() {
        // Arrange.
        val topSampleText = "Top text in column "
        val bottomSampleText = "Bottom text in column "
        setContent {
            Column(Modifier.testTag("Test Tag").semantics { isTraversalGroup = true }) {
                Row {
                    Modifier.semantics { isTraversalGroup = true }
                    CardRow(
                        Modifier.testTag("Row 1").semantics { isTraversalGroup = false },
                        1,
                        topSampleText,
                        bottomSampleText,
                    )
                    CardRow(
                        Modifier.testTag("Row 2").semantics { isTraversalGroup = false },
                        2,
                        topSampleText,
                        bottomSampleText,
                    )
                }
            }
        }
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).semanticsId()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).semanticsId()

        // Act.
        val bottomText1ANI = rule.runOnIdle { createAccessibilityNodeInfo(bottomText1) }

        // Assert.
        // Here we have the following hierarchy of traversal groups:
        // `isTraversalGroup = true`
        //    `isTraversalGroup = true`
        //       `isTraversalGroup = false`
        //       `isTraversalGroup = false`
        // In this case, we expect all the top text to be read first, then all the bottom text
        rule.runOnIdle { assertThat(bottomText1ANI.extras.traversalBefore).isEqualTo(bottomText2) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_tripleNestedTraversalGroups() {
        // Arrange.
        val topSampleText = "Top "
        val bottomSampleText = "Bottom "
        setContent {
            Row {
                CardRow(
                    Modifier.semantics { isTraversalGroup = false },
                    1,
                    topSampleText,
                    bottomSampleText,
                )
                CardRow(
                    Modifier.semantics { isTraversalGroup = false },
                    2,
                    topSampleText,
                    bottomSampleText,
                )
                CardRow(
                    Modifier.semantics { isTraversalGroup = true },
                    3,
                    topSampleText,
                    bottomSampleText,
                )
            }
        }
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).semanticsId()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).semanticsId()
        val bottomText3 = rule.onNodeWithText(bottomSampleText + 3).semanticsId()
        val topText3 = rule.onNodeWithText(topSampleText + 3).semanticsId()

        // Act.
        rule.waitForIdle()
        val bottomText1ANI = createAccessibilityNodeInfo(bottomText1)
        val topText3ANI = createAccessibilityNodeInfo(topText3)

        // Assert.
        // Here we have the following hierarchy of traversal groups:
        // `isTraversalGroup = false`
        // `isTraversalGroup = false`
        // `isTraversalGroup = true`
        // In this case, we expect to read in the order of: Top 1, Top 2, Bottom 1, Bottom 2,
        // then Top 3, Bottom 3. The first two traversal groups are effectively merged since they
        // are both
        // set to false, while the third traversal group is structurally significant.
        rule.runOnIdle {
            assertThat(bottomText1ANI.extras.traversalBefore).isEqualTo(bottomText2)
            assertThat(topText3ANI.extras.traversalBefore).isEqualTo(bottomText3)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedTraversalGroups_hierarchy() {
        // Arrange.
        val topSampleText = "Top text in column "
        val bottomSampleText = "Bottom text in column "
        setContent {
            Row {
                CardRow(
                    Modifier
                        // adding a vertical scroll here makes the column scrollable, which would
                        // normally make it structurally significant
                        .verticalScroll(rememberScrollState())
                        // but adding in `traversalGroup = false` should negate that
                        .semantics { isTraversalGroup = false },
                    1,
                    topSampleText,
                    bottomSampleText,
                )
                CardRow(
                    Modifier
                        // adding a vertical scroll here makes the column scrollable, which would
                        // normally make it structurally significant
                        .verticalScroll(rememberScrollState())
                        // but adding in `isTraversalGroup = false` should negate that
                        .semantics { isTraversalGroup = false },
                    2,
                    topSampleText,
                    bottomSampleText,
                )
            }
        }
        val bottomText1 = rule.onNodeWithText(bottomSampleText + 1).semanticsId()
        val bottomText2 = rule.onNodeWithText(bottomSampleText + 2).semanticsId()

        // Act.
        val bottomText1ANI = rule.runOnIdle { createAccessibilityNodeInfo(bottomText1) }

        // Assert.
        // In this case, we expect all the top text to be read first, then all the bottom text
        rule.runOnIdle { assertThat(bottomText1ANI.extras.traversalBefore).isAtMost(bottomText2) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalIndex() {
        // Arrange.
        val overlaidText = "Overlaid node text"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        setContent {
            LastElementOverLaidColumn(
                // None of the elements below should inherit `traversalIndex = 5f`
                modifier = Modifier.padding(8.dp).semantics { traversalIndex = 5f }
            ) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                // Since default traversalIndex is 0, `traversalIndex = -1f` here means that the
                // overlaid node is read first, even though visually it's below the other text.
                Row {
                    Text(
                        text = overlaidText,
                        modifier = Modifier.semantics { traversalIndex = -1f },
                    )
                }
            }
        }
        val node1 = rule.onNodeWithText(text1).semanticsId()
        val overlaidNode = rule.onNodeWithText(overlaidText).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(overlaidNode) }

        // Assert.
        // Because the overlaid node has a smaller traversal index, it should be read before node 1
        rule.runOnIdle { assertThat(info.extras.traversalBefore).isAtMost(node1) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_nestedAndPeerTraversalIndex() {
        // Arrange.
        val text0 = "Text 0\n"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        val text4 = "Text 4\n"
        val text5 = "Text 5\n"
        setContent {
            Column(
                Modifier
                    // Having a traversal index here as 8f shouldn't affect anything; this column
                    // has no other peers that its compared to
                    .semantics {
                        traversalIndex = 8f
                        isTraversalGroup = true
                    }
                    .padding(8.dp)
            ) {
                Row(
                    Modifier.semantics {
                        traversalIndex = 3f
                        isTraversalGroup = true
                    }
                ) {
                    Column(modifier = Modifier.testTag("Tag1")) {
                        Row { Text(text3) }
                        Row {
                            Text(
                                text = text5,
                                modifier = Modifier.semantics { traversalIndex = 1f },
                            )
                        }
                        Row { Text(text4) }
                    }
                }
                Row { Text(text = text2, modifier = Modifier.semantics { traversalIndex = 2f }) }
                Row { Text(text = text1, modifier = Modifier.semantics { traversalIndex = 1f }) }
                Row { Text(text = text0) }
            }
        }
        val virtualViewId0 = rule.onNodeWithText(text0).semanticsId()
        val virtualViewId1 = rule.onNodeWithText(text1).semanticsId()
        val virtualViewId2 = rule.onNodeWithText(text2).semanticsId()
        val virtualViewId3 = rule.onNodeWithText(text3).semanticsId()
        val virtualViewId4 = rule.onNodeWithText(text4).semanticsId()
        val virtualViewId5 = rule.onNodeWithText(text5).semanticsId()

        // Act.
        rule.waitForIdle()
        val ani0 = createAccessibilityNodeInfo(virtualViewId0)
        val ani1 = createAccessibilityNodeInfo(virtualViewId1)
        val ani2 = createAccessibilityNodeInfo(virtualViewId2)
        val ani3 = createAccessibilityNodeInfo(virtualViewId3)
        val ani4 = createAccessibilityNodeInfo(virtualViewId4)

        // Assert - We want to read the texts in order: 0 -> 1 -> 2 -> 3 -> 4 -> 5
        rule.runOnIdle {
            assertThat(ani0.extras.traversalBefore).isAtMost(virtualViewId1)
            assertThat(ani1.extras.traversalBefore).isAtMost(virtualViewId2)
            assertThat(ani2.extras.traversalBefore).isAtMost(virtualViewId3)
            assertThat(ani3.extras.traversalBefore).isAtMost(virtualViewId4)
            assertThat(ani4.extras.traversalBefore).isAtMost(virtualViewId5)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalIndexInherited_indexFirst() {
        // Arrange.
        val overlaidText = "Overlaid node text"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        setContent {
            LastElementOverLaidColumn(
                modifier =
                    Modifier.semantics { traversalIndex = -1f }
                        .semantics { isTraversalGroup = true }
            ) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row {
                    Text(
                        text = overlaidText,
                        modifier =
                            Modifier.semantics { traversalIndex = 1f }
                                .semantics { isTraversalGroup = true },
                    )
                }
            }
        }
        val node3Id = rule.onNodeWithText(text3).semanticsId()
        val overlayId = rule.onNodeWithText(overlaidText).semanticsId()

        // Act.
        val node3ANI = rule.runOnIdle { createAccessibilityNodeInfo(node3Id) }

        // Assert - Nodes 1 through 3 are read, and then overlaid node is read last
        rule.runOnIdle { assertThat(node3ANI.extras.traversalBefore).isAtMost(overlayId) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_traversalIndexInherited_indexSecond() {
        // Arrange.
        val overlaidText = "Overlaid node text"
        val text1 = "Text 1\n"
        val text2 = "Text 2\n"
        val text3 = "Text 3\n"
        // This test is identical to the one above, except with `isTraversalGroup` coming first in
        // the modifier chain. Behavior-wise, this shouldn't change anything.
        setContent {
            LastElementOverLaidColumn(
                modifier =
                    Modifier.semantics { isTraversalGroup = true }
                        .semantics { traversalIndex = -1f }
            ) {
                Row {
                    Column {
                        Row { Text(text1) }
                        Row { Text(text2) }
                        Row { Text(text3) }
                    }
                }
                Row {
                    Text(
                        text = overlaidText,
                        modifier =
                            Modifier.semantics { isTraversalGroup = true }
                                .semantics { traversalIndex = 1f },
                    )
                }
            }
        }
        val node3Id = rule.onNodeWithText(text3).semanticsId()
        val overlayId = rule.onNodeWithText(overlaidText).semanticsId()

        // Act.
        val node3ANI = rule.runOnIdle { createAccessibilityNodeInfo(node3Id) }

        // Assert - Nodes 1 through 3 are read, and then overlaid node is read last
        rule.runOnIdle { assertThat(node3ANI.extras.traversalBefore).isAtMost(overlayId) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_SimpleTopAppBar() {
        // Arrange.
        val topAppBarText = "Top App Bar"
        val textBoxTag = "Text Box"
        setContent {
            Box(Modifier.testTag(textBoxTag)) { Text(text = "Lorem ipsum ".repeat(200)) }

            TopAppBar(title = { Text(text = topAppBarText) })
        }
        val textBoxId = rule.onNodeWithTag(textBoxTag).semanticsId()
        val topAppBarId = rule.onNodeWithText(topAppBarText).semanticsId()

        // Act.
        val topAppBarANI = rule.runOnIdle { createAccessibilityNodeInfo(topAppBarId) }

        // Assert.
        rule.runOnIdle { assertThat(topAppBarANI.extras.traversalBefore).isLessThan(textBoxId) }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_SimpleScrollingTopAppBar() {
        // Arrange.
        val topAppBarText = "Top App Bar"
        val sampleText = "Sample text "
        val sampleText1 = "Sample text 1"
        val sampleText2 = "Sample text 2"
        var counter = 1
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                TopAppBar(title = { Text(text = topAppBarText) })
                repeat(100) { Text(sampleText + counter++) }
            }
        }
        val topAppBarId = rule.onNodeWithText(topAppBarText).semanticsId()
        val node1Id = rule.onNodeWithText(sampleText1).semanticsId()
        val node2Id = rule.onNodeWithText(sampleText2).semanticsId()

        // Act.
        rule.waitForIdle()
        val topAppBarANI = createAccessibilityNodeInfo(topAppBarId)
        val ani1 = createAccessibilityNodeInfo(node1Id)

        // Assert that the top bar comes before the first node (node 1) and that the first node
        // comes before the second (node 2)
        rule.runOnIdle {
            assertThat(topAppBarANI.extras.traversalBefore).isEqualTo(node1Id)
            assertThat(ani1.extras.traversalBefore).isEqualTo(node2Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_ScaffoldTopBar() {
        // Arrange.
        val topAppBarText = "Top App Bar"
        val contentText = "Content"
        val bottomAppBarText = "Bottom App Bar"
        setContent {
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { TopAppBar(title = { Text(topAppBarText) }) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    FloatingActionButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "fab icon")
                    }
                },
                drawerContent = { Text(text = "Drawer Menu 1") },
                content = { padding -> Text(contentText, modifier = Modifier.padding(padding)) },
                bottomBar = { BottomAppBar { Text(bottomAppBarText) } },
            )
        }
        val topAppBarId = rule.onNodeWithText(topAppBarText).semanticsId()
        val contentId = rule.onNodeWithText(contentText).semanticsId()
        val bottomAppBarId = rule.onNodeWithText(bottomAppBarText).semanticsId()

        // Act.
        rule.waitForIdle()
        val topAppBarANI = createAccessibilityNodeInfo(topAppBarId)
        val contentANI = createAccessibilityNodeInfo(contentId)

        // Assert.
        rule.runOnIdle {
            assertThat(topAppBarANI.extras.traversalBefore).isEqualTo(contentId)
            assertThat(contentANI.extras.traversalBefore).isLessThan(bottomAppBarId)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_clearSemantics() {
        // Arrange.
        val content1 = "Face 1"
        val content2 = "Face 2"
        val content3 = "Face 3"
        val contentText = "Content"
        setContent {
            Scaffold(
                topBar = {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Face, contentDescription = content1)
                        }
                        IconButton(onClick = {}, modifier = Modifier.clearAndSetSemantics {}) {
                            Icon(Icons.Default.Face, contentDescription = content2)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Face, contentDescription = content3)
                        }
                    }
                },
                content = { padding -> Text(contentText, modifier = Modifier.padding(padding)) },
            )
        }
        val face1Id = rule.onNodeWithContentDescription(content1).semanticsId()
        val face3Id = rule.onNodeWithContentDescription(content3).semanticsId()
        val contentId = rule.onNodeWithText(contentText).semanticsId()

        // Act.
        rule.waitForIdle()
        val ani1 = createAccessibilityNodeInfo(face1Id)
        val ani3 = createAccessibilityNodeInfo(face3Id)

        // Assert.
        // On screen we have three faces in a top app bar, and then a content node:
        //
        //     Face1       Face2      Face3
        //               Content
        //

        // Since `clearAndSetSemantics` is set on Face2, it should not generate any semantics node.
        rule.onNodeWithTag(content2).assertDoesNotExist()

        // The traversal order for the elements on screen should then be Face1 -> Face3 -> content.
        rule.runOnIdle {
            assertThat(ani1.extras.traversalBefore).isEqualTo(face3Id)
            assertThat(ani3.extras.traversalBefore).isEqualTo(contentId)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_zOcclusion() {
        // Arrange.
        val parentBox1Tag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        val childThreeTag = "ChildThree"
        setContent {
            Column {
                Box(Modifier.testTag(parentBox1Tag)) {
                    with(LocalDensity.current) {
                        BasicText(
                            "Child One",
                            Modifier
                                // A child with larger [zIndex] will be drawn on top of all the
                                // children with smaller [zIndex]. So child 1 covers child 2.
                                .zIndex(1f)
                                .testTag(childOneTag)
                                .requiredSize(50.toDp()),
                        )
                        BasicText(
                            "Child Two",
                            Modifier.testTag(childTwoTag).requiredSize(50.toDp()),
                        )
                    }
                }
                Box { BasicText("Child Three", Modifier.testTag(childThreeTag)) }
            }
        }
        val parentBox1Id = rule.onNodeWithTag(parentBox1Tag).semanticsId()
        val childOneId = rule.onNodeWithTag(childOneTag, useUnmergedTree = true).semanticsId()
        val childTwoId = rule.onNodeWithTag(childTwoTag, useUnmergedTree = true).semanticsId()
        val childThreeId = rule.onNodeWithTag(childThreeTag, useUnmergedTree = true).semanticsId()

        // Act.
        rule.waitForIdle()
        val parentANI = createAccessibilityNodeInfo(parentBox1Id)
        val ani1 = createAccessibilityNodeInfo(childOneId)
        val ani2 = provider.createAccessibilityNodeInfo(childTwoId)

        // Assert.
        rule.runOnIdle {
            // Since child 2 is completely covered, it should not generate any ANI. The first box
            // parent should only have one child (child 1).
            assertThat(parentANI.childCount).isEqualTo(1)
            assertThat(ani2).isNull()

            // The traversal order for the elements on screen should then be child 1 -> child 3,
            // completely skipping over child 2.
            assertThat(ani1.extras.traversalBefore).isEqualTo(childThreeId)
        }
    }

    @Composable
    fun ScrollColumn(padding: PaddingValues, firstElement: String, lastElement: String) {
        var counter = 0
        val sampleText = "Sample text in column"
        Column(Modifier.verticalScroll(rememberScrollState()).padding(padding)) {
            Text(text = firstElement, modifier = Modifier.testTag(firstElement))
            repeat(100) { Text(sampleText + counter++) }
            Text(text = lastElement, modifier = Modifier.testTag(lastElement))
        }
    }

    @Composable
    fun ScrollColumnNoPadding(firstElement: String, lastElement: String) {
        var counter = 0
        val sampleText = "Sample text in column"
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(text = firstElement, modifier = Modifier.testTag(firstElement))
            repeat(30) { Text(sampleText + counter++) }
            Text(text = lastElement, modifier = Modifier.testTag(lastElement))
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_ScaffoldScrollingTopBar() {
        // Arrange.
        val topAppBarText = "Top App Bar"
        val firstContentText = "First content text"
        val lastContentText = "Last content text"
        val bottomAppBarText = "Bottom App Bar"
        setContent {
            ScaffoldedSubcomposeLayout(
                modifier = Modifier,
                topBar = { TopAppBar(title = { Text(text = topAppBarText) }) },
                content = { ScrollColumnNoPadding(firstContentText, lastContentText) },
                bottomBar = { BottomAppBar { Text(bottomAppBarText) } },
            )
        }

        val topAppBarId = rule.onNodeWithText(topAppBarText).semanticsId()
        val firstContentId = rule.onNodeWithTag(firstContentText).semanticsId()
        val lastContentId = rule.onNodeWithTag(lastContentText).semanticsId()

        // Act.
        rule.waitForIdle()
        val topAppBarANI = createAccessibilityNodeInfo(topAppBarId)
        val firstContentANI = createAccessibilityNodeInfo(firstContentId)

        // Assert.
        rule.runOnIdle {

            // First content comes right after the top bar, so the `before` value equals the first
            // content node id.
            assertThat(topAppBarANI.extras.traversalBefore).isNotEqualTo(0)
            assertThat(topAppBarANI.extras.traversalBefore).isEqualTo(firstContentId)

            // The scrolling content comes in between the first text element and the last, so
            // check that the traversal value is not 0, then assert the first text comes before the
            // last text.
            assertThat(firstContentANI.extras.traversalBefore).isNotEqualTo(0)
            assertThat(firstContentANI.extras.traversalBefore).isLessThan(lastContentId)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_zIndex() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        setContent {
            Column(Modifier.testTag(rootTag)) {
                SimpleTestLayout(Modifier.requiredSize(50.dp).zIndex(1f).testTag(childTag1)) {}
                SimpleTestLayout(Modifier.requiredSize(50.dp).testTag(childTag2)) {}
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)

        // Assert - We want child1 to come before child2
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child1ANI.extras.traversalBefore).isLessThan(child2Id)
            assertThat(child2ANI.extras.traversalAfter).isLessThan(child1Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_zIndex() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        setContent {
            Row(Modifier.testTag(rootTag)) {
                SimpleTestLayout(Modifier.requiredSize(50.dp).zIndex(1f).testTag(childTag1)) {}
                SimpleTestLayout(Modifier.requiredSize(50.dp).testTag(childTag2)) {}
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)

        // Assert - We want child1 to come before child2
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child1ANI.extras.traversalBefore).isLessThan(child2Id)
            assertThat(child2ANI.extras.traversalAfter).isLessThan(child1Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_offset() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        setContent {
            Box(Modifier.testTag(rootTag)) {
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).offset(x = 0.dp, y = 50.dp).testTag(childTag1)
                ) {}
                SimpleTestLayout(Modifier.requiredSize(50.dp).testTag(childTag2)) {}
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)

        // Assert - We want child2 to come before child1
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child2ANI.extras.traversalBefore).isLessThan(child1Id)
            assertThat(child1ANI.extras.traversalAfter).isLessThan(child2Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_offset() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        setContent {
            Box(Modifier.testTag(rootTag)) {
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).offset(x = 50.dp, y = 0.dp).testTag(childTag1)
                ) {}
                SimpleTestLayout(Modifier.requiredSize(50.dp).testTag(childTag2)) {}
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)

        // Assert - We want child2 to come before child1
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child2ANI.extras.traversalBefore).isLessThan(child1Id)
            assertThat(child1ANI.extras.traversalAfter).isLessThan(child2Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_offset_overlapped() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        setContent {
            Box(Modifier.testTag(rootTag)) {
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp).offset(x = 0.dp, y = 20.dp).testTag(childTag1)
                ) {}
                SimpleTestLayout(Modifier.requiredSize(50.dp).testTag(childTag2)) {}
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1 = rule.onNodeWithTag(childTag1).semanticsId()
        val child2 = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1)
        val child2ANI = createAccessibilityNodeInfo(child2)

        // Assert - We want child2 to come before child1
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child2ANI.extras.traversalBefore).isLessThan(child1)
            assertThat(child1ANI.extras.traversalAfter).isLessThan(child2)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_offset_overlapped() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        setContent {
            Box(Modifier.testTag(rootTag)) {
                // Layouts need to have `.clickable` on them in order to make the nodes
                // speakable and therefore sortable
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp)
                        .offset(x = 20.dp, y = 0.dp)
                        .testTag(childTag1)
                        .clickable(onClick = {})
                ) {}
                SimpleTestLayout(
                    Modifier.requiredSize(50.dp)
                        .offset(x = 0.dp, y = 20.dp)
                        .testTag(childTag2)
                        .clickable(onClick = {})
                ) {}
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        val child2ANI = rule.runOnIdle { createAccessibilityNodeInfo(child2Id) }

        // Assert - We want child2 to come before child1
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child2ANI.extras.traversalBefore).isEqualTo(child1Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_vertical_subcompose() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        val density = Density(1f)
        val size = with(density) { 100.dp.roundToPx() }.toFloat()
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                SimpleSubcomposeLayout(
                    Modifier.testTag(rootTag),
                    { SimpleTestLayout(Modifier.requiredSize(100.dp).testTag(childTag1)) {} },
                    Offset(0f, size),
                    { SimpleTestLayout(Modifier.requiredSize(100.dp).testTag(childTag2)) {} },
                    Offset(0f, 0f),
                )
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)

        // Assert - We want child2 to come before child1
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child2ANI.extras.traversalBefore).isLessThan(child1Id)
            assertThat(child1ANI.extras.traversalAfter).isLessThan(child2Id)
        }
    }

    @Test
    fun testSortedAccessibilityNodeInfo_horizontal_subcompose() {
        // Arrange.
        val rootTag = "root"
        val childTag1 = "child1"
        val childTag2 = "child2"
        val density = Density(1f)
        val size = with(density) { 100.dp.roundToPx() }.toFloat()
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                SimpleSubcomposeLayout(
                    Modifier.testTag(rootTag),
                    { SimpleTestLayout(Modifier.requiredSize(100.dp).testTag(childTag1)) {} },
                    Offset(size, 0f),
                    { SimpleTestLayout(Modifier.requiredSize(100.dp).testTag(childTag2)) {} },
                    Offset(0f, 0f),
                )
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithTag(childTag1).semanticsId()
        val child2Id = rule.onNodeWithTag(childTag2).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)

        // Assert - We want child2 to come before child1
        rule.runOnIdle {
            assertThat(root.replacedChildren.size).isEqualTo(2)
            assertThat(child2ANI.extras.traversalBefore).isLessThan(child1Id)
            assertThat(child1ANI.extras.traversalAfter).isLessThan(child2Id)
        }
    }

    @Test
    fun testChildrenSortedByBounds_rtl() {
        // Arrange.
        val rootTag = "root"
        val childText1 = "child1"
        val childText2 = "child2"
        val childText3 = "child3"
        val rtlChildText1 = "rtlChild1"
        val rtlChildText2 = "rtlChild2"
        val rtlChildText3 = "rtlChild3"
        setContent {
            Column(Modifier.testTag(rootTag)) {
                // Will display child1, child2, child3, and should be read
                // from child1 => child2 => child3.
                Row(Modifier.semantics { isTraversalGroup = true }) {
                    SimpleTestLayout(Modifier.requiredSize(100.dp)) { Text(childText1) }
                    SimpleTestLayout(Modifier.requiredSize(100.dp)) { Text(childText2) }
                    SimpleTestLayout(Modifier.requiredSize(100.dp)) { Text(childText3) }
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    // Will display rtlChild3 rtlChild2 rtlChild1, but should be read
                    // from child1 => child2 => child3.
                    Row(Modifier.semantics { isTraversalGroup = true }) {
                        SimpleTestLayout(Modifier.requiredSize(100.dp)) { Text(rtlChildText1) }
                        SimpleTestLayout(Modifier.requiredSize(100.dp)) { Text(rtlChildText2) }
                        SimpleTestLayout(Modifier.requiredSize(100.dp)) { Text(rtlChildText3) }
                    }
                }
            }
        }
        val root = rule.onNodeWithTag(rootTag).fetchSemanticsNode()
        val child1Id = rule.onNodeWithText(childText1).semanticsId()
        val child2Id = rule.onNodeWithText(childText2).semanticsId()
        val child3Id = rule.onNodeWithText(childText3).semanticsId()

        val rtlChild1Id = rule.onNodeWithText(rtlChildText1).semanticsId()
        val rtlChild2Id = rule.onNodeWithText(rtlChildText2).semanticsId()
        val rtlChild3Id = rule.onNodeWithText(rtlChildText3).semanticsId()

        // Act.
        rule.waitForIdle()
        val child1ANI = createAccessibilityNodeInfo(child1Id)
        val child2ANI = createAccessibilityNodeInfo(child2Id)
        val child3ANI = createAccessibilityNodeInfo(child3Id)

        val rtlChild1ANI = createAccessibilityNodeInfo(rtlChild1Id)
        val rtlChild2ANI = createAccessibilityNodeInfo(rtlChild2Id)

        // Assert - Rtl
        rule.runOnIdle {
            // There should be two traversal groups in the scene.
            assertThat(root.replacedChildren.size).isEqualTo(2)

            assertThat(child1ANI.extras.traversalBefore).isNotEqualTo(0)
            assertThat(child2ANI.extras.traversalBefore).isNotEqualTo(0)
            assertThat(child3ANI.extras.traversalBefore).isNotEqualTo(0)

            assertThat(rtlChild1ANI.extras.traversalBefore).isNotEqualTo(0)
            assertThat(rtlChild2ANI.extras.traversalBefore).isNotEqualTo(0)

            // The LTR children should be read from child1 => child2 => child3.
            assertThat(child1ANI.extras.traversalBefore).isEqualTo(child2Id)
            assertThat(child2ANI.extras.traversalBefore).isEqualTo(child3Id)
            assertThat(child3ANI.extras.traversalBefore).isEqualTo(rtlChild1Id)

            // We also want the RTL children to be read from child1 => child2 => child3.
            assertThat(rtlChild1ANI.extras.traversalBefore).isEqualTo(rtlChild2Id)
            assertThat(rtlChild2ANI.extras.traversalBefore).isEqualTo(rtlChild3Id)
        }
    }

    @Composable
    fun InteropColumn(
        padding: PaddingValues,
        columnTag: String,
        interopText: String,
        firstButtonText: String,
        lastButtonText: String,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(padding).testTag(columnTag)) {
            Button(onClick = {}) { Text(firstButtonText) }

            AndroidView(::TextView) { it.text = interopText }

            Button(onClick = {}) { Text(lastButtonText) }
        }
    }

    @Test
    fun testChildrenSortedByBounds_ViewInterop() {
        // Arrange.
        val topAppBarText = "Top App Bar"
        val columnTag = "Column Tag"
        val interopText = "This is a text in a TextView"
        val firstButtonText = "First Button"
        val lastButtonText = "Last Button"
        val fabContentText = "FAB Icon"
        setContent {
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { TopAppBar(title = { Text(topAppBarText) }) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    FloatingActionButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = fabContentText)
                    }
                },
                drawerContent = { Text(text = "Drawer Menu 1") },
                content = { padding ->
                    InteropColumn(padding, columnTag, interopText, firstButtonText, lastButtonText)
                },
                bottomBar = { BottomAppBar { Text("Bottom App Bar") } },
            )
        }
        val colSemanticsNode =
            rule.onNodeWithTag(columnTag).fetchSemanticsNode("can't find node with tag $columnTag")
        val viewHolder =
            androidComposeView.androidViewsHandler.layoutNodeToHolder[
                    colSemanticsNode.replacedChildren[1].layoutNode]
        checkNotNull(viewHolder)
        val firstButtonId = rule.onNodeWithText(firstButtonText).semanticsId()
        val lastButtonId = rule.onNodeWithText(lastButtonText).semanticsId()

        // Act.
        rule.waitForIdle()
        val colAccessibilityNode = createAccessibilityNodeInfo(colSemanticsNode.id)
        val firstButtonANI = createAccessibilityNodeInfo(firstButtonId)
        val lastButtonANI = createAccessibilityNodeInfo(lastButtonId)
        val viewANI = viewHolder.createAccessibilityNodeInfo()

        // Assert.
        // Desired ordering: Top App Bar -> first button -> android view -> last button -> FAB.
        // First check that the View exists
        rule.runOnIdle {
            assertThat(colAccessibilityNode.childCount).isEqualTo(3)
            assertThat(colSemanticsNode.replacedChildren.size).isEqualTo(3)
            // Then verify that the first button comes before the View
            assertThat(firstButtonANI.extras.traversalBefore)
                .isEqualTo(viewHolder.layoutNode.semanticsId)
            // And the last button comes after the View
            assertThat(lastButtonANI.extras.traversalAfter)
                .isEqualTo(viewHolder.layoutNode.semanticsId)

            // Check the View's `before` and `after` values have also been set
            assertThat(viewANI.extras.traversalAfter).isEqualTo(firstButtonId)
            assertThat(viewANI.extras.traversalBefore).isEqualTo(lastButtonId)
        }
    }

    @Composable
    fun InteropColumnBackwards(
        padding: PaddingValues,
        columnTag: String,
        interopText: String,
        firstButtonText: String,
        thirdButtonText: String,
        fourthButtonText: String,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(padding).testTag(columnTag)) {
            Button(modifier = Modifier.semantics { traversalIndex = 3f }, onClick = {}) {
                Text(firstButtonText)
            }

            AndroidView(::TextView, modifier = Modifier.semantics { traversalIndex = 2f }) {
                it.text = interopText
            }

            Button(modifier = Modifier.semantics { traversalIndex = 1f }, onClick = {}) {
                Text(thirdButtonText)
            }

            Button(modifier = Modifier.semantics { traversalIndex = 0f }, onClick = {}) {
                Text(fourthButtonText)
            }
        }
    }

    @Test
    fun testChildrenSortedByBounds_ViewInteropBackwards() {
        // Arrange.
        val topAppBarText = "Top App Bar"
        val columnTag = "Column Tag"
        val interopText = "This is a text in a TextView"
        val firstButtonText = "First Button"
        val thirdButtonText = "Third Button"
        val fourthButtonText = "Fourth Button"
        val fabContentText = "FAB Icon"
        setContent {
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = { TopAppBar(title = { Text(topAppBarText) }) },
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    FloatingActionButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = fabContentText)
                    }
                },
                drawerContent = { Text(text = "Drawer Menu 1") },
                content = { padding ->
                    InteropColumnBackwards(
                        padding,
                        columnTag,
                        interopText,
                        firstButtonText,
                        thirdButtonText,
                        fourthButtonText,
                    )
                },
                bottomBar = { BottomAppBar { Text("Bottom App Bar") } },
            )
        }
        val colSemanticsNode =
            rule.onNodeWithTag(columnTag).fetchSemanticsNode("can't find node with tag $columnTag")
        val viewHolder =
            androidComposeView.androidViewsHandler.layoutNodeToHolder[
                    colSemanticsNode.replacedChildren[1].layoutNode]
        checkNotNull(viewHolder) // Check that the View exists
        val firstButtonId = rule.onNodeWithText(firstButtonText).semanticsId()
        val thirdButtonId = rule.onNodeWithText(thirdButtonText).semanticsId()
        val fourthButtonId = rule.onNodeWithText(fourthButtonText).semanticsId()

        // Act.
        rule.waitForIdle()
        val colAccessibilityNode = createAccessibilityNodeInfo(colSemanticsNode.id)
        val firstButtonANI = createAccessibilityNodeInfo(firstButtonId)
        val thirdButtonANI = createAccessibilityNodeInfo(thirdButtonId)
        val fourthButtonANI = createAccessibilityNodeInfo(fourthButtonId)
        val viewANI = viewHolder.createAccessibilityNodeInfo()

        // Assert.
        rule.runOnIdle {
            assertThat(colAccessibilityNode.childCount).isEqualTo(4)
            assertThat(colSemanticsNode.replacedChildren.size).isEqualTo(4)
            // Desired ordering:
            // Top App Bar -> fourth button -> third button -> android view -> first button -> FAB.
            // Fourth button comes before the third button
            assertThat(fourthButtonANI.extras.traversalBefore).isEqualTo(thirdButtonId)
            // Then verify that the third button comes before Android View
            assertThat(thirdButtonANI.extras.traversalBefore)
                .isEqualTo(viewHolder.layoutNode.semanticsId)
            // And the first button comes after the View
            assertThat(firstButtonANI.extras.traversalAfter)
                .isEqualTo(viewHolder.layoutNode.semanticsId)
            // Check the View's `before` and `after` values have also been set
            assertThat(viewANI.extras.traversalAfter).isEqualTo(thirdButtonId)
            assertThat(viewANI.extras.traversalBefore).isEqualTo(firstButtonId)
        }
    }

    private companion object {
        private val Bundle.traversalAfter: Int
            get() = getInt("android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALAFTER_VAL")

        private val Bundle.traversalBefore: Int
            get() = getInt("android.view.accessibility.extra.EXTRA_DATA_TEST_TRAVERSALBEFORE_VAL")
    }

    @Test
    fun testPerformAction_showOnScreen() {
        rule.mainClock.autoAdvance = false

        val scrollState = ScrollState(initial = 0)
        val target1Tag = "target1"
        val target2Tag = "target2"
        setContent {
            Box {
                with(LocalDensity.current) {
                    Column(Modifier.size(200.toDp()).verticalScroll(scrollState)) {
                        BasicText("Backward", Modifier.testTag(target2Tag).size(150.toDp()))
                        BasicText("Forward", Modifier.testTag(target1Tag).size(150.toDp()))
                    }
                }
            }
        }
        assertThat(scrollState.value).isEqualTo(0)

        val showOnScreen = android.R.id.accessibilityActionShowOnScreen
        val target1Id = rule.onNodeWithTag(target1Tag).semanticsId()
        rule.runOnUiThread {
            assertThat(provider.performAction(target1Id, showOnScreen, null)).isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(scrollState.value).isGreaterThan(99)

        val target2Id = rule.onNodeWithTag(target2Tag).semanticsId()
        rule.runOnUiThread {
            assertThat(provider.performAction(target2Id, showOnScreen, null)).isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(scrollState.value).isEqualTo(0)
    }

    @Test
    fun testPerformAction_showOnScreen_lazy() {
        rule.mainClock.autoAdvance = false

        val lazyState = LazyListState()
        val target1Tag = "target1"
        val target2Tag = "target2"
        setContent {
            Box {
                with(LocalDensity.current) {
                    LazyColumn(modifier = Modifier.size(200.toDp()), state = lazyState) {
                        item {
                            BasicText("Backward", Modifier.testTag(target2Tag).size(150.toDp()))
                        }
                        item { BasicText("Forward", Modifier.testTag(target1Tag).size(150.toDp())) }
                    }
                }
            }
        }
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)

        val showOnScreen = android.R.id.accessibilityActionShowOnScreen
        val target1Id = rule.onNodeWithTag(target1Tag).semanticsId()
        rule.runOnUiThread {
            assertThat(provider.performAction(target1Id, showOnScreen, null)).isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isGreaterThan(99)

        val target2Id = rule.onNodeWithTag(target2Tag).semanticsId()
        rule.runOnUiThread {
            assertThat(provider.performAction(target2Id, showOnScreen, null)).isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun testPerformAction_showOnScreen_lazynested() {
        val parentLazyState = LazyListState()
        val lazyState = LazyListState()
        val target1Tag = "target1"
        val target2Tag = "target2"
        setContent {
            Box {
                with(LocalDensity.current) {
                    LazyRow(modifier = Modifier.size(250.toDp()), state = parentLazyState) {
                        item {
                            LazyColumn(modifier = Modifier.size(200.toDp()), state = lazyState) {
                                item {
                                    BasicText(
                                        "Backward",
                                        Modifier.testTag(target2Tag).size(150.toDp()),
                                    )
                                }
                                item {
                                    BasicText(
                                        "Forward",
                                        Modifier.testTag(target1Tag).size(150.toDp()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)

        // Test that child column scrolls to make it fully visible in its context, without being
        // influenced by or influencing the parent row.
        // TODO(b/190865803): Is this the ultimate right behavior we want?
        val showOnScreen = android.R.id.accessibilityActionShowOnScreen
        val target1Id = rule.onNodeWithTag(target1Tag).semanticsId()
        rule.runOnUiThread {
            assertThat(provider.performAction(target1Id, showOnScreen, null)).isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isGreaterThan(99)
        assertThat(parentLazyState.firstVisibleItemScrollOffset).isEqualTo(0)

        val target2Id = rule.onNodeWithTag(target2Tag).semanticsId()
        rule.runOnUiThread {
            assertThat(provider.performAction(target2Id, showOnScreen, null)).isTrue()
        }
        rule.mainClock.advanceTimeBy(5000)
        assertThat(lazyState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(lazyState.firstVisibleItemScrollOffset).isEqualTo(0)
        assertThat(parentLazyState.firstVisibleItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun testPerformAction_focus() {
        // Arrange.
        setContent { Box(Modifier.testTag(tag).focusable()) { BasicText("focusable") } }
        val virtualViewId =
            rule.onNodeWithTag(tag).assert(expectValue(Focused, false)).semanticsId()

        // Act.
        rule.runOnUiThread {
            assertThat(provider.performAction(virtualViewId, ACTION_FOCUS, null)).isTrue()
        }

        // Assert.
        rule.onNodeWithTag(tag).assert(expectValue(Focused, true))
    }

    @Test
    fun testPerformAction_clearFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            Row {
                // Initially focused item.
                Box(Modifier.size(10.dp).focusable())
                Box(Modifier.testTag(tag).focusRequester(focusRequester).focusable()) {
                    BasicText("focusable")
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        val virtualViewId = rule.onNodeWithTag(tag).assert(expectValue(Focused, true)).semanticsId()

        // Act.
        rule.runOnUiThread {
            assertThat(provider.performAction(virtualViewId, ACTION_CLEAR_FOCUS, null)).isTrue()
        }

        // Assert.
        rule.onNodeWithTag(tag).assert(expectValue(Focused, false))
    }

    @Test
    fun testPerformAction_succeedOnEnabledNodes() {
        // Arrange.
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(value = checked, onValueChange = { checked = it }).testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }
        rule.onNodeWithTag(tag).assertIsDisplayed().assertIsOn()
        val toggleableNodeId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val actionPerformed =
            rule.runOnUiThread { provider.performAction(toggleableNodeId, ACTION_CLICK, null) }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.onNodeWithTag(tag).assertIsOff()
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun testPerformAction_failOnDisabledNodes() {
        // Arrange.
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(
                        value = checked,
                        enabled = false,
                        onValueChange = { checked = it },
                    )
                    .testTag(tag),
                content = { BasicText("ToggleableText") },
            )
        }
        val toggleableId = rule.onNodeWithTag(tag).assertIsDisplayed().assertIsOn().semanticsId()

        // Act.
        val actionPerformed =
            rule.runOnUiThread { provider.performAction(toggleableId, ACTION_CLICK, null) }

        // Assert.
        rule.onNodeWithTag(tag).assertIsOn()
        assertThat(actionPerformed).isFalse()
    }

    @Test
    fun testPerformAction_succeedFromToolIfSensitiveData() {
        delegate.requestFromAccessibilityToolForTesting = true

        val tag = "node"
        container.setContent {
            Box(Modifier.testTag(tag).semantics { isSensitiveData = true }.focusable()) {
                BasicText("focusable")
            }
        }

        val focusableNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        rule.runOnUiThread {
            assertThat(provider.performAction(focusableNode.id, ACTION_FOCUS, null)).isTrue()
        }
        rule.onNodeWithTag(tag).assert(expectValue(SemanticsProperties.Focused, true))
    }

    @Test
    fun testPerformAction_failFromNontoolIfSensitiveData() {
        delegate.requestFromAccessibilityToolForTesting = false

        val tag = "node"
        container.setContent {
            Box(Modifier.testTag(tag).semantics { isSensitiveData = true }.focusable()) {
                BasicText("focusable")
            }
        }

        val focusableNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
        rule.runOnUiThread {
            assertThat(provider.performAction(focusableNode.id, ACTION_FOCUS, null)).isFalse()
        }
        rule.onNodeWithTag(tag).assert(expectValue(SemanticsProperties.Focused, false))
    }

    @Test
    fun testTextField_performClickAction_succeedOnEnabledNode() {
        // Arrange.
        setContent {
            BasicTextField(modifier = Modifier.testTag(tag), value = "value", onValueChange = {})
        }
        val textFieldNodeId = rule.onNodeWithTag(tag).assertIsDisplayed().semanticsId()

        // Act.
        val actionPerformed =
            rule.runOnUiThread { provider.performAction(textFieldNodeId, ACTION_CLICK, null) }

        // Assert.
        rule.onNodeWithTag(tag).assert(expectValue(Focused, true))
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun testTextField_performSetSelectionAction_succeedOnEnabledNode() {
        // Arrange.
        var textFieldSelectionOne = false
        setContent {
            var value by remember { mutableStateOf(TextFieldValue("hello")) }
            BasicTextField(
                modifier =
                    Modifier.semantics {
                            // Make sure this block will be executed when selection changes.
                            this.textSelectionRange = value.selection
                            if (value.selection == TextRange(1)) {
                                textFieldSelectionOne = true
                            }
                        }
                        .testTag(tag),
                value = value,
                onValueChange = { value = it },
            )
        }
        val textFieldId = rule.onNodeWithTag(tag).assertIsDisplayed().semanticsId()
        val argument = Bundle()
        argument.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, 1)
        argument.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, 1)

        // Act.
        val actionPerformed =
            rule.runOnUiThread {
                textFieldSelectionOne = false
                provider.performAction(textFieldId, ACTION_SET_SELECTION, argument)
            }
        rule.waitUntil(5_000) { textFieldSelectionOne }

        // Assert.
        rule.onNodeWithTag(tag).assert(expectValue(TextSelectionRange, TextRange(1)))
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun testTextField_testFocusClearFocusAction() {
        // Arrange.
        setContent {
            Row {
                // Initially focused item.
                Box(Modifier.size(10.dp).focusable())
                BasicTextField(
                    modifier = Modifier.testTag(tag),
                    value = "value",
                    onValueChange = {},
                )
            }
        }
        val textFieldId = rule.onNodeWithTag(tag).assert(expectValue(Focused, false)).semanticsId()

        // Act.
        var actionPerformed =
            rule.runOnUiThread { provider.performAction(textFieldId, ACTION_FOCUS, null) }

        // Assert.
        rule.onNodeWithTag(tag).assert(expectValue(Focused, true))
        assertThat(actionPerformed).isTrue()

        // Act.
        actionPerformed =
            rule.runOnUiThread { provider.performAction(textFieldId, ACTION_CLEAR_FOCUS, null) }

        // Assert.
        rule.onNodeWithTag(tag).assert(expectValue(Focused, false))
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun testFindFocus_noInputFocus() {
        // Arrange.
        setContent {
            Row {
                // No focused item.
                Box(Modifier.size(10.dp).focusable())
                Box(Modifier.size(10.dp).focusable())
            }
        }

        // Act.
        val focusedNode = rule.runOnUiThread { provider.findFocus(FOCUS_INPUT) }

        // Assert.
        assertThat(focusedNode).isNull()
    }

    @Test
    fun testFindFocus_hasInputFocus() {
        // Arrange.
        val focusRequester = FocusRequester()
        setContent {
            Row {
                // Initially focused item.
                Box(Modifier.size(10.dp).focusable())
                Box(Modifier.testTag(tag).focusRequester(focusRequester).focusable()) {
                    BasicText("focusable")
                }
            }
        }
        rule.runOnIdle { focusRequester.requestFocus() }
        val virtualViewId = rule.onNodeWithTag(tag).assert(expectValue(Focused, true)).semanticsId()
        val expectedNode = provider.createAccessibilityNodeInfo(virtualViewId)

        // Act.
        val actualNode = rule.runOnUiThread { provider.findFocus(FOCUS_INPUT) }

        // Assert.
        assertThat(actualNode).isEqualTo(expectedNode)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Suppress("DEPRECATION")
    fun testAddExtraDataToAccessibilityNodeInfo_notMerged() {
        lateinit var textLayoutResult: TextLayoutResult
        setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "texy",
                onValueChange = {},
                onTextLayout = { textLayoutResult = it },
            )
        }
        val textFieldNode =
            rule.onNodeWithTag(tag).fetchSemanticsNode("couldn't find node with tag $tag")
        val info = AccessibilityNodeInfoCompat.obtain()
        val argument =
            Bundle().apply {
                putInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
                putInt(EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, 1)
            }

        // TODO(b/272068594): This looks like a bug. This should be
        //  AccessibilityNodeInfoCompat.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
        provider.addExtraDataToAccessibilityNodeInfo(
            textFieldNode.id,
            info,
            EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
            argument,
        )

        // TODO(b/272068594): This looks like a bug. This should be
        //  AccessibilityNodeInfoCompat.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
        val data = info.extras.getParcelableArray(EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
        assertThat(data!!.size).isEqualTo(1)

        val rectF = data[0] as RectF // result in screen coordinates
        val expectedRectInLocalCoords =
            textLayoutResult.getBoundingBox(0).translate(textFieldNode.positionInWindow)
        val expectedTopLeftInScreenCoords =
            androidComposeView.localToScreen(expectedRectInLocalCoords.topLeft)
        assertThat(rectF.left).isEqualTo(expectedTopLeftInScreenCoords.x)
        assertThat(rectF.top).isEqualTo(expectedTopLeftInScreenCoords.y)
        assertThat(rectF.width()).isEqualTo(expectedRectInLocalCoords.width)
        assertThat(rectF.height()).isEqualTo(expectedRectInLocalCoords.height)

        val testTagKey = "androidx.compose.ui.semantics.testTag"
        provider.addExtraDataToAccessibilityNodeInfo(textFieldNode.id, info, testTagKey, argument)
        assertThat(info.extras.getCharSequence(testTagKey)).isEqualTo(tag)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getSemanticsNodeIdFromExtraData() {
        // Arrange.
        setContent { BasicText("texy") }
        val textId = rule.onNodeWithText("texy").semanticsId()
        val info = AccessibilityNodeInfoCompat.obtain()
        val argument = Bundle()
        val idKey = "androidx.compose.ui.semantics.id"

        // Act.
        rule.runOnIdle {
            provider.addExtraDataToAccessibilityNodeInfo(textId, info, idKey, argument)
        }

        // Assert.
        rule.runOnIdle { assertThat(info.extras.getInt(idKey)).isEqualTo(textId) }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getCustomExtrasFromExtraData() {
        // Arrange.
        val date = Date()
        setContent {
            Box(
                Modifier.size(10.dp).testTag(tag).semantics {
                    customIntAccessibilityExtra = 123
                    customStringAccessibilityExtra = "test"
                    customParcelableAccessibilityExtra = Uri.parse("http://www.google.com")
                    customSerializableAccessibilityExtra = date
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = provider.createAccessibilityNodeInfo(virtualViewId)!!
        rule.runOnIdle {
            assertThat(info.availableExtraData)
                .containsAtLeast(
                    CustomIntAccessibilityExtraKey,
                    CustomStringAccessibilityExtraKey,
                    CustomParcelableAccessibilityExtraKey,
                    CustomSerializableAccessibilityExtraKey,
                )
        }

        fun populateExtra(extraKey: String) {
            provider.addExtraDataToAccessibilityNodeInfo(virtualViewId, info, extraKey, Bundle())
        }

        // Act.
        rule.runOnIdle {
            populateExtra(CustomIntAccessibilityExtraKey)
            populateExtra(CustomStringAccessibilityExtraKey)
            populateExtra(CustomParcelableAccessibilityExtraKey)
            populateExtra(CustomSerializableAccessibilityExtraKey)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.getInt(CustomIntAccessibilityExtraKey)).isEqualTo(123)
            assertThat(info.extras.getString(CustomStringAccessibilityExtraKey)).isEqualTo("test")
            @Suppress("DEPRECATION")
            assertThat(info.extras.getParcelable<Uri>(CustomParcelableAccessibilityExtraKey))
                .isEqualTo(Uri.parse("http://www.google.com"))
            @Suppress("DEPRECATION")
            assertThat(info.extras.getSerializable(CustomSerializableAccessibilityExtraKey))
                .isEqualTo(date)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getCustomExtrasFromExtraData_sameExtraSetMultipleTimes_sameModifier_lastOneWins() {
        // Arrange.
        setContent {
            Box(
                Modifier.size(10.dp).testTag(tag).semantics {
                    customIntAccessibilityExtra = 1
                    customIntAccessibilityExtra = 2
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = provider.createAccessibilityNodeInfo(virtualViewId)!!

        // Act.
        rule.runOnIdle {
            provider.addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                CustomIntAccessibilityExtraKey,
                Bundle(),
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.getInt(CustomIntAccessibilityExtraKey)).isEqualTo(2)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getCustomExtrasFromExtraData_sameExtraSetMultipleTimes_twoModifiers_outerOneWins() {
        // Arrange.
        setContent {
            Box(
                Modifier.size(10.dp)
                    .testTag(tag)
                    .semantics { customIntAccessibilityExtra = 1 }
                    .semantics { customIntAccessibilityExtra = 2 }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = provider.createAccessibilityNodeInfo(virtualViewId)!!

        // Act.
        rule.runOnIdle {
            provider.addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                CustomIntAccessibilityExtraKey,
                Bundle(),
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.getInt(CustomIntAccessibilityExtraKey)).isEqualTo(1)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getCustomExtrasFromExtraData_multipleSemanticsModifiers_mergesExtras() {
        // Arrange.
        val date = Date()
        setContent {
            Box(
                Modifier.size(10.dp)
                    .testTag(tag)
                    .semantics {
                        customIntAccessibilityExtra = 123
                        customStringAccessibilityExtra = "test"
                        customParcelableAccessibilityExtra = Uri.parse("http://www.google.com")
                    }
                    .semantics { customSerializableAccessibilityExtra = date }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = provider.createAccessibilityNodeInfo(virtualViewId)!!
        rule.runOnIdle {
            assertThat(info.availableExtraData)
                .containsAtLeast(
                    CustomIntAccessibilityExtraKey,
                    CustomStringAccessibilityExtraKey,
                    CustomParcelableAccessibilityExtraKey,
                    CustomSerializableAccessibilityExtraKey,
                )
        }

        fun populateExtra(extraKey: String) {
            provider.addExtraDataToAccessibilityNodeInfo(virtualViewId, info, extraKey, Bundle())
        }

        // Act.
        rule.runOnIdle {
            populateExtra(CustomIntAccessibilityExtraKey)
            populateExtra(CustomStringAccessibilityExtraKey)
            populateExtra(CustomParcelableAccessibilityExtraKey)
            populateExtra(CustomSerializableAccessibilityExtraKey)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.getInt(CustomIntAccessibilityExtraKey)).isEqualTo(123)
            assertThat(info.extras.getString(CustomStringAccessibilityExtraKey)).isEqualTo("test")
            @Suppress("DEPRECATION")
            assertThat(info.extras.getParcelable<Uri>(CustomParcelableAccessibilityExtraKey))
                .isEqualTo(Uri.parse("http://www.google.com"))
            @Suppress("DEPRECATION")
            assertThat(info.extras.getSerializable(CustomSerializableAccessibilityExtraKey))
                .isEqualTo(date)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun getInvalidExtraFromExtraData_throws() {
        // Arrange.
        setContent {
            Box(
                Modifier.size(10.dp).testTag(tag).semantics {
                    invalidAccessibilityExtra = InvalidAccessibilityExtraValue(123)
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = provider.createAccessibilityNodeInfo(virtualViewId)!!

        // Act/Assert.
        expectError<IllegalStateException>(
            expectedMessage =
                "Accessibility extra values must be either Serializable or Parcelable."
        ) {
            rule.runOnIdle {
                provider.addExtraDataToAccessibilityNodeInfo(
                    virtualViewId,
                    info,
                    InvalidAccessibilityExtraKey,
                    Bundle(),
                )
            }
        }
    }

    @Test
    fun sendClickedEvent_whenClick() {
        // Arrange.
        setContent { Box(Modifier.clickable(onClick = {}).testTag(tag)) { BasicText("Text") } }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val actionPerformed =
            rule.runOnUiThread { provider.performAction(virtualViewId, ACTION_CLICK, null) }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            assertThat(actionPerformed).isTrue()
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_VIEW_CLICKED
                        }
                    ),
                )
        }
    }

    @Test
    fun sendStateChangeEvent_whenStateChange() {
        // Arrange.
        var state by mutableStateOf("state one")
        setContent {
            Box(Modifier.semantics { stateDescription = state }.testTag(tag)) { BasicText("Text") }
        }
        val virtualViewId = rule.onNodeWithTag(tag).assertValueEquals("state one").semanticsId()

        // Act.
        rule.runOnIdle { state = "state two" }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        }
                    ),
                )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_UNDEFINED
                        }
                    ),
                )
        }
    }

    @Test
    fun sendStateChangeEvent_whenClickToggleable() {
        // Arrange.
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(value = checked, onValueChange = { checked = it }).testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).assertIsDisplayed().assertIsOn().semanticsId()

        // Act.
        rule.onNodeWithTag(tag).performClick()

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.onNodeWithTag(tag).assertIsOff()
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        }
                    ),
                )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_UNDEFINED
                        }
                    ),
                )
        }
    }

    @Test
    fun sendClickedAndChangeSubtree_whenDescendantClicked() {
        val textTag = "text_tag"
        // Arrange.
        setContent {
            var selected by remember { mutableStateOf(true) }
            Box {
                Box(Modifier.testTag(textTag)) {
                    if (selected) {
                        BasicText(text = "DisappearingText")
                    }
                }
                Box(Modifier.clickable(onClick = { selected = !selected }).testTag(tag)) {
                    BasicText("ToggleableComponent")
                }
            }
        }
        val toggleableVirtualViewId = rule.onNodeWithTag(tag).assertIsDisplayed().semanticsId()

        // Act.
        val actionPerformed =
            rule.runOnUiThread {
                provider.performAction(toggleableVirtualViewId, ACTION_CLICK, null)
            }

        // Assert that `TYPE_VIEW_CLICKED` event was sent.
        rule.runOnIdle {
            assertThat(actionPerformed).isTrue()
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) ==
                                toggleableVirtualViewId && it.eventType == TYPE_VIEW_CLICKED
                        }
                    ),
                )
        }

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert that `TYPE_WINDOW_CONTENT_CHANGED` event was also sent.
        rule.onNodeWithTag(textTag).assertIsNotDisplayed()
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }
    }

    @Test
    fun sendStateChangeEvent_whenSelectedChange() {
        // Arrange.
        setContent {
            var selected by remember { mutableStateOf(false) }
            Box(
                Modifier.selectable(selected = selected, onClick = { selected = true }).testTag(tag)
            ) {
                BasicText("Text")
            }
        }
        val virtualViewId =
            rule.onNodeWithTag(tag).assertIsDisplayed().assertIsNotSelected().semanticsId()

        // Act.
        rule.onNodeWithTag(tag).performClick()

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.onNodeWithTag(tag).assertIsSelected()
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        }
                    ),
                )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_UNDEFINED
                        }
                    ),
                )
        }
    }

    @Test
    fun sendViewSelectedEvent_whenSelectedChange_forTab() {
        // Arrange.
        setContent {
            var selected by remember { mutableStateOf(false) }
            Box(
                Modifier.selectable(
                        selected = selected,
                        onClick = { selected = true },
                        role = Role.Tab,
                    )
                    .testTag(tag)
            ) {
                BasicText("Text")
            }
        }
        val virtualViewId =
            rule.onNodeWithTag(tag).assertIsDisplayed().assertIsNotSelected().semanticsId()

        // Act.
        rule.onNodeWithTag(tag).performClick()

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.onNodeWithTag(tag).assertIsSelected()
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_VIEW_SELECTED &&
                                it.text.size == 1 &&
                                it.text[0].toString() == "Text"
                        }
                    ),
                )
        }
    }

    @Test
    fun sendStateChangeEvent_whenRangeInfoChange() {
        // Arrange.
        var current by mutableStateOf(0.5f)
        setContent { Box(Modifier.progressSemantics(current).testTag(tag)) { BasicText("Text") } }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        rule.runOnIdle { current = 0.9f }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_STATE_DESCRIPTION
                        }
                    ),
                )
            // Temporary(b/192295060) fix, sending CONTENT_CHANGE_TYPE_UNDEFINED to
            // force ViewRootImpl to update its accessibility-focused virtual-node.
            // If we have an androidx fix, we can remove this event.
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == virtualViewId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_UNDEFINED
                        }
                    ),
                )
        }
    }

    @FlakyTest(bugId = 354750986)
    @Test
    fun sendTextEvents_whenSetText() {
        // Arrange.
        val locale = LocaleList("en_US")
        val initialText = "h"
        val finalText = "hello"
        setContent {
            var value by remember { mutableStateOf(TextFieldValue(initialText)) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it },
                visualTransformation = {
                    TransformedText(it.toUpperCase(locale), OffsetMapping.Identity)
                },
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertIsDisplayed()
            .assert(expectValue(EditableText, AnnotatedString("H")))

        // TODO(b/272068594): Extra TYPE_WINDOW_CONTENT_CHANGED sent 100ms after setup.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle { clearInvocations(container) }

        // Act.
        rule.onNodeWithTag(tag).performSemanticsAction(SetText) { it(AnnotatedString(finalText)) }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        val virtualId =
            rule
                .onNodeWithTag(tag)
                .assert(expectValue(EditableText, AnnotatedString("HELLO")))
                .semanticsId()
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(eq(androidComposeView), argument.capture())
            assertThat(argument.allValues)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_CHANGED
                        className = TextFieldClassName
                        setSource(androidComposeView, virtualId)
                        isPassword = false
                        fromIndex = initialText.length
                        removedCount = 0
                        addedCount = finalText.length - initialText.length
                        beforeText = initialText.toUpperCase(locale)
                        this.text.add(finalText.toUpperCase(locale))
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        setSource(androidComposeView, virtualId)
                        isPassword = false
                        fromIndex = finalText.length
                        toIndex = finalText.length
                        itemCount = finalText.length
                        this.text.add(finalText.toUpperCase(locale))
                    },
                )
                .inOrder()
        }
    }

    @Test
    @Ignore("b/177656801")
    fun sendSubtreeChangeEvents_whenNodeRemoved() {
        val columnTag = "topColumn"
        val textFieldTag = "TextFieldTag"
        var isTextFieldVisible by mutableStateOf(true)
        setContent {
            Column(Modifier.testTag(columnTag)) {
                if (isTextFieldVisible) {
                    BasicTextField(
                        modifier = Modifier.testTag(textFieldTag),
                        value = "text",
                        onValueChange = {},
                    )
                }
            }
        }
        val columnId = rule.onNodeWithTag(columnTag).semanticsId()

        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == columnId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }
        clearInvocations(container)

        // Act- TextField is removed compared to setup.
        rule.runOnIdle { isTextFieldVisible = false }

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.onNodeWithTag(textFieldTag).assertDoesNotExist()
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == columnId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }
    }

    @Test
    @FlakyTest(bugId = 364709885)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionEventBeforeTraverseEvent_whenTraverseTextField() {
        val text = "h"
        setContent {
            var value by remember { mutableStateOf(TextFieldValue(text)) }
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = value,
                onValueChange = { value = it },
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = {
                    BasicText("Label")
                    it()
                },
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).assertIsDisplayed().semanticsId()

        // TODO(b/272068594): Extra TYPE_WINDOW_CONTENT_CHANGED sent 100ms after setup.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        clearInvocations(container)

        // Act.
        rule.runOnUiThread {
            provider.performAction(
                virtualViewId,
                ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                createMovementGranularityCharacterArgs(),
            )
        }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(eq(androidComposeView), argument.capture())
            assertThat(argument.allValues)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        isPassword = true
                        setSource(androidComposeView, virtualViewId)
                        fromIndex = 1
                        toIndex = 1
                        itemCount = 1
                        this.text.add("•")
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        isPassword = true
                        setSource(androidComposeView, virtualViewId)
                        action = ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        movementGranularity = 1
                        fromIndex = 0
                        toIndex = 1
                        this.text.add("•")
                    },
                )
                .inOrder()
        }
    }

    @FlakyTest(bugId = 356384247)
    @Test
    fun selectionEventBeforeTraverseEvent_whenTraverseText() {
        // Arrange.
        val text = "h"
        setContent { BasicText(text, Modifier.testTag(tag)) }
        val virtualViewId = rule.onNodeWithTag(tag).assertIsDisplayed().semanticsId()

        // TODO(b/272068594): Extra TYPE_WINDOW_CONTENT_CHANGED sent 100ms after setup.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        clearInvocations(container)

        // Act.
        rule.runOnUiThread {
            provider.performAction(
                virtualViewId,
                ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                createMovementGranularityCharacterArgs(),
            )
        }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(eq(androidComposeView), argument.capture())
            assertThat(argument.allValues)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        setSource(androidComposeView, virtualViewId)
                        fromIndex = 1
                        toIndex = 1
                        itemCount = 1
                        this.text.add("h")
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        setSource(androidComposeView, virtualViewId)
                        action = ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        movementGranularity = 1
                        fromIndex = 0
                        toIndex = 1
                        this.text.add("h")
                    },
                )
                .inOrder()
        }
    }

    @Test
    @Ignore("b/177656801")
    fun semanticsNodeBeingMergedLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(value = checked, onValueChange = { checked = it }).testTag(tag)
            ) {
                BasicText("ToggleableText")
                Box { BasicText("TextNode") }
            }
        }
        val toggleableId = rule.onNodeWithTag(tag).semanticsId()
        val textNode =
            rule
                .onNodeWithText("TextNode", useUnmergedTree = true)
                .fetchSemanticsNode("couldn't find node with text TextNode")

        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == toggleableId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }

        rule.runOnUiThread {
            // Directly call onLayoutChange because this guarantees short time.
            repeat(10) { delegate.onLayoutChange(textNode.layoutNode) }
        }

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == toggleableId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }
    }

    @Test
    @Ignore("b/177656801")
    fun layoutNodeWithoutSemanticsLayoutChange_sendThrottledSubtreeEventsForMergedSemanticsNode() {
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(value = checked, onValueChange = { checked = it }).testTag(tag)
            ) {
                BasicText("ToggleableText")
                Box { BasicText("TextNode") }
            }
        }

        val toggleableId = rule.onNodeWithTag(tag).semanticsId()
        val textNode =
            rule
                .onNodeWithText("TextNode", useUnmergedTree = true)
                .fetchSemanticsNode("couldn't find node with text TextNode")

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == toggleableId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }

        rule.runOnUiThread {
            // Directly call onLayoutChange because this guarantees short time.
            repeat(10) {
                // layout change for the parent box node
                delegate.onLayoutChange(textNode.layoutNode.parent!!)
            }
        }

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            // One from initialization and one from layout changes.
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == toggleableId &&
                                it.eventType == TYPE_WINDOW_CONTENT_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_SUBTREE
                        }
                    ),
                )
        }
    }

    @Test
    fun testSemanticsHitTest() {
        // Arrange.
        setContent {
            var checked by remember { mutableStateOf(true) }
            Box(
                Modifier.toggleable(value = checked, onValueChange = { checked = it }).testTag(tag)
            ) {
                BasicText("ToggleableText")
            }
        }
        val toggleableId = rule.onNodeWithTag(tag).semanticsId()
        val toggleableBounds =
            with(rule.density) { rule.onNodeWithTag(tag).getBoundsInRoot().toRect() }

        // Act.
        val toggleableNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    (toggleableBounds.left + toggleableBounds.right) / 2,
                    (toggleableBounds.top + toggleableBounds.bottom) / 2,
                )
            }

        // Assert.
        assertThat(toggleableId).isEqualTo(toggleableNodeId)
    }

    @Test
    fun testSemanticsHitTest_overlappedChildren() {
        // Arrange.
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        setContent {
            Box {
                with(LocalDensity.current) {
                    BasicText(
                        "Child One",
                        Modifier.zIndex(1f).testTag(childOneTag).requiredSize(50.toDp()),
                    )
                    BasicText("Child Two", Modifier.testTag(childTwoTag).requiredSize(50.toDp()))
                }
            }
        }
        val childOneId = rule.onNodeWithTag(childOneTag).semanticsId()
        val childTwoId = rule.onNodeWithTag(childTwoTag).semanticsId()
        val overlappedChildNodeBounds =
            with(rule.density) { rule.onNodeWithTag(childTwoTag).getBoundsInRoot().toRect() }

        // Act.
        val overlappedChildNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    (overlappedChildNodeBounds.left + overlappedChildNodeBounds.right) / 2,
                    (overlappedChildNodeBounds.top + overlappedChildNodeBounds.bottom) / 2,
                )
            }

        // Assert.
        assertThat(childOneId).isEqualTo(overlappedChildNodeId)
        assertThat(childTwoId).isNotEqualTo(overlappedChildNodeId)
    }

    @Test
    fun testSemanticsHitTest_scrolled() {
        val scrollState = ScrollState(initial = 0)
        var scope: CoroutineScope? = null
        setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }

            Box {
                with(LocalDensity.current) {
                    Column(Modifier.size(200.toDp()).verticalScroll(scrollState)) {
                        BasicText("Before scroll", Modifier.size(200.toDp()))
                        BasicText("After scroll", Modifier.testTag(tag).size(200.toDp()))
                    }
                }
            }
        }
        assertThat(scrollState.value).isEqualTo(0)

        scope!!.launch {
            // Scroll to the bottom
            scrollState.scrollBy(10000f)
        }
        rule.waitForIdle()

        assertThat(scrollState.value).isGreaterThan(199)

        val vitrualViewId = rule.onNodeWithTag(tag).semanticsId()
        val childNodeBounds =
            with(rule.density) { rule.onNodeWithTag(tag).getBoundsInRoot().toRect() }
        val hitTestedId =
            delegate.hitTestSemanticsAt(
                (childNodeBounds.left + childNodeBounds.right) / 2,
                (childNodeBounds.top + childNodeBounds.bottom) / 2,
            )
        assertThat(vitrualViewId).isEqualTo(hitTestedId)
    }

    @Test
    fun testSemanticsHitTest_hideFromAccessibilitySemantics() {
        // Arrange.
        setContent {
            Box(
                Modifier.size(100.dp)
                    .clickable {}
                    .testTag(tag)
                    .semantics { hideFromAccessibility() }
            ) {
                BasicText("")
            }
        }
        val bounds = with(rule.density) { rule.onNodeWithTag(tag).getBoundsInRoot().toRect() }

        // Act.
        val hitNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2,
                )
            }

        // Assert.
        rule.runOnIdle { assertThat(hitNodeId).isEqualTo(InvalidId) }
    }

    @Test
    fun testHideFromAccessibilityMatchers() {
        // Arrange.
        setContent {
            Box(Modifier.testTag("testTag1").semantics { hideFromAccessibility() }) {
                BasicText(modifier = Modifier.testTag("testTag2"), text = "text")
                BasicText(
                    modifier = Modifier.testTag("testTag3").semantics { hideFromAccessibility() },
                    text = "text",
                )
            }
        }
        // TestTag2 is not hidden from accessibility
        rule.onNodeWithTag("testTag2").assert(isHiddenFromAccessibility().not())
        // TestTag2 is in a subtree hidden from accessibility
        rule.onNodeWithTag("testTag2").assert(isInHiddenAccessibilitySubtree())

        // TestTag3 is hidden from accessibility
        rule.onNodeWithTag("testTag3").assert(isHiddenFromAccessibility())
        // TestTag3 is in a subtree hidden from accessibility
        rule.onNodeWithTag("testTag3").assert(isInHiddenAccessibilitySubtree())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun viewInteropIsInvisibleToUser() {
        setContent {
            AndroidView({
                TextView(it).apply {
                    text = "Test"
                    isScreenReaderFocusable = true
                }
            })
        }
        Espresso.onView(instanceOf(TextView::class.java)).check(matches(isDisplayed())).check {
            view,
            exception ->
            val viewParent = view.parent
            if (viewParent !is View) {
                throw exception
            }
            val delegate = ViewCompat.getAccessibilityDelegate(viewParent) ?: throw exception
            val info =
                AccessibilityNodeInfoCompat.wrap(android.view.accessibility.AccessibilityNodeInfo())
            delegate.onInitializeAccessibilityNodeInfo(view, info)
            // This is expected to be false, unlike
            // AndroidViewTest.androidViewAccessibilityDelegate, because this test suite sets
            // `accessibilityForceEnabledForTesting` to true.
            if (info.isVisibleToUser) {
                throw exception
            }
        }
    }

    @Test
    fun testSemanticsHitTest_transparentNode() {
        // Arrange.
        setContent {
            Box(Modifier.alpha(0f).size(100.dp).clickable {}.testTag(tag)) { BasicText("") }
        }
        val bounds = with(rule.density) { rule.onNodeWithTag(tag).getBoundsInRoot().toRect() }

        // Act.
        val hitNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2,
                )
            }

        // Assert.
        rule.runOnIdle { assertThat(hitNodeId).isEqualTo(InvalidId) }
    }

    @Test
    fun testSemanticsHitTest_clearAndSet() {
        // Arrange.
        val outertag = "outerbox"
        val innertag = "innerbox"
        setContent {
            Box(Modifier.size(100.dp).clickable {}.testTag(outertag).clearAndSetSemantics {}) {
                Box(Modifier.size(100.dp).clickable {}.testTag(innertag)) { BasicText("") }
            }
        }
        val outerNodeId = rule.onNodeWithTag(outertag).semanticsId()
        val bounds =
            with(rule.density) { rule.onNodeWithTag(innertag, true).getBoundsInRoot().toRect() }

        // Act.
        val hitNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2,
                )
            }

        // Assert.
        rule.runOnIdle { assertThat(outerNodeId).isEqualTo(hitNodeId) }
    }

    @Test
    fun testSemanticsHitTest_unimportantForAccessibilityOverlay() {
        // Arrange.
        setContent {
            Box {
                Box(Modifier.size(100.dp).clickable {}.testTag(tag)) { BasicText("") }
                Box(Modifier.size(100.dp).semantics {})
            }
        }
        val clickableNodeId = rule.onNodeWithTag(tag).semanticsId()
        val bounds = with(rule.density) { rule.onNodeWithTag(tag).getBoundsInRoot().toRect() }

        // Act.
        val hitNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2,
                )
            }

        // Assert.
        rule.runOnIdle { assertThat(hitNodeId).isEqualTo(clickableNodeId) }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    fun testViewInterop_findViewByAccessibilityId() {
        setContent {
            Column {
                AndroidView(
                    { context ->
                        LinearLayout(context).apply {
                            addView(TextView(context).apply { text = "Text1" })
                            addView(TextView(context).apply { text = "Text2" })
                        }
                    },
                    Modifier.testTag(tag),
                )
                BasicText("text")
            }
        }

        val getViewRootImplMethod = View::class.java.getDeclaredMethod("getViewRootImpl")
        getViewRootImplMethod.isAccessible = true
        val rootView = getViewRootImplMethod.invoke(container)

        val forName = Class::class.java.getMethod("forName", String::class.java)
        val getDeclaredMethod =
            Class::class
                .java
                .getMethod("getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java)

        val viewRootImplClass = forName.invoke(null, "android.view.ViewRootImpl") as Class<*>
        val getAccessibilityInteractionControllerMethod =
            getDeclaredMethod.invoke(
                viewRootImplClass,
                "getAccessibilityInteractionController",
                arrayOf<Class<*>>(),
            ) as Method
        getAccessibilityInteractionControllerMethod.isAccessible = true
        val accessibilityInteractionController =
            getAccessibilityInteractionControllerMethod.invoke(rootView)

        val accessibilityInteractionControllerClass =
            forName.invoke(null, "android.view.AccessibilityInteractionController") as Class<*>
        val findViewByAccessibilityIdMethod =
            getDeclaredMethod.invoke(
                accessibilityInteractionControllerClass,
                "findViewByAccessibilityId",
                arrayOf<Class<*>>(Int::class.java),
            ) as Method
        findViewByAccessibilityIdMethod.isAccessible = true

        val androidView =
            rule.onNodeWithTag(tag).fetchSemanticsNode("can't find node with tag $tag")
        val viewGroup =
            androidComposeView.androidViewsHandler.layoutNodeToHolder[androidView.layoutNode]!!.view
                as ViewGroup
        val getAccessibilityViewIdMethod =
            View::class.java.getDeclaredMethod("getAccessibilityViewId")
        getAccessibilityViewIdMethod.isAccessible = true

        val textTwo = viewGroup.getChildAt(1)
        val textViewTwoId = getAccessibilityViewIdMethod.invoke(textTwo)
        val foundView =
            findViewByAccessibilityIdMethod.invoke(
                accessibilityInteractionController,
                textViewTwoId,
            )
        assertThat(foundView).isNotNull()
        assertThat(textTwo).isEqualTo(foundView)
    }

    @Test
    fun testViewInterop_viewChildExists() {
        // Arrange.
        val buttonText = "button text"
        setContent {
            Column(Modifier.testTag(tag)) {
                AndroidView(::Button) {
                    it.text = buttonText
                    it.setOnClickListener {}
                }
                BasicText("text")
            }
        }
        val colSemanticsNode =
            rule.onNodeWithTag(tag).fetchSemanticsNode("can't find node with tag $tag")
        val colAccessibilityNode = createAccessibilityNodeInfo(colSemanticsNode.id)

        // Act.
        val buttonHolder =
            rule.runOnIdle {
                androidComposeView.androidViewsHandler.layoutNodeToHolder[
                        colSemanticsNode.replacedChildren[0].layoutNode]
            }
        checkNotNull(buttonHolder)

        // Assert.
        rule.runOnIdle {
            assertThat(colAccessibilityNode.childCount).isEqualTo(2)
            assertThat(colSemanticsNode.replacedChildren.size).isEqualTo(2)
            assertThat(buttonHolder.importantForAccessibility)
                .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
            assertThat((buttonHolder.getChildAt(0) as Button).text).isEqualTo(buttonText)
        }
    }

    @Test
    fun testViewInterop_hoverEnterExit() {
        val colTag = "ColTag"
        val textTag = "TextTag"
        val buttonText = "button text"
        setContent {
            Column(Modifier.testTag(colTag)) {
                AndroidView(::Button) {
                    it.text = buttonText
                    it.setOnClickListener {}
                }
                BasicText(text = "text", modifier = Modifier.testTag(textTag))
            }
        }

        val colSemanticsNode =
            rule.onNodeWithTag(colTag).fetchSemanticsNode("can't find node with tag $colTag")
        rule.runOnUiThread {
            val bounds = colSemanticsNode.replacedChildren[0].boundsInRoot
            val hoverEnter =
                createHoverMotionEvent(
                    action = ACTION_HOVER_ENTER,
                    x = (bounds.left + bounds.right) / 2f,
                    y = (bounds.top + bounds.bottom) / 2f,
                )
            assertThat(androidComposeView.dispatchHoverEvent(hoverEnter)).isTrue()
            assertThat(delegate.hoveredVirtualViewId).isEqualTo(InvalidId)
        }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(ArgumentMatcher { it.eventType == TYPE_VIEW_HOVER_ENTER }),
                )
        }

        val virtualViewId = rule.onNodeWithTag(textTag).semanticsId()
        val bounds = with(rule.density) { rule.onNodeWithTag(textTag).getBoundsInRoot().toRect() }
        rule.runOnUiThread {
            val hoverEnter =
                createHoverMotionEvent(
                    action = ACTION_HOVER_MOVE,
                    x = (bounds.left + bounds.right) / 2,
                    y = (bounds.top + bounds.bottom) / 2,
                )
            assertThat(androidComposeView.dispatchHoverEvent(hoverEnter)).isTrue()
            assertThat(delegate.hoveredVirtualViewId).isEqualTo(virtualViewId)
        }
        // verify hover exit accessibility event is sent from the previously hovered view
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(ArgumentMatcher { it.eventType == TYPE_VIEW_HOVER_EXIT }),
                )
        }
    }

    @Test
    fun testViewInterop_dualHoverEnterExit() {
        val colTag = "ColTag"
        val textTag = "TextTag"
        val buttonText = "button text"
        val events = mutableListOf<PointerEvent>()
        setContent {
            Column(
                Modifier.testTag(colTag).pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes[0].consume()
                            events += event
                        }
                    }
                }
            ) {
                AndroidView(::Button) {
                    it.text = buttonText
                    it.setOnClickListener {}
                }
                BasicText(text = "text", modifier = Modifier.testTag(textTag))
            }
        }

        val colSemanticsNode =
            rule.onNodeWithTag(colTag).fetchSemanticsNode("can't find node with tag $colTag")
        rule.runOnUiThread {
            val bounds = colSemanticsNode.replacedChildren[0].boundsInRoot
            val hoverEnter =
                createHoverMotionEvent(
                    action = ACTION_HOVER_ENTER,
                    x = (bounds.left + bounds.right) / 2f,
                    y = (bounds.top + bounds.bottom) / 2f,
                )
            assertThat(androidComposeView.dispatchHoverEvent(hoverEnter)).isTrue()
            assertThat(delegate.hoveredVirtualViewId).isEqualTo(InvalidId)
            // Assert that the hover event has also been dispatched
            assertThat(events).hasSize(1)
            // and that the hover event is an enter event
            assertHoverEvent(events[0], isEnter = true)
        }

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(ArgumentMatcher { it.eventType == TYPE_VIEW_HOVER_ENTER }),
                )
        }
    }

    private fun assertHoverEvent(
        event: PointerEvent,
        isEnter: Boolean = false,
        isExit: Boolean = false,
    ) {
        assertThat(event.changes).hasSize(1)
        val change = event.changes[0]
        assertThat(change.pressed).isFalse()
        assertThat(change.previousPressed).isFalse()
        val expectedHoverType =
            when {
                isEnter -> PointerEventType.Enter
                isExit -> PointerEventType.Exit
                else -> PointerEventType.Move
            }
        assertThat(event.type).isEqualTo(expectedHoverType)
    }

    private fun createHoverMotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        val pointerProperties =
            MotionEvent.PointerProperties().apply { toolType = MotionEvent.TOOL_TYPE_FINGER }
        val pointerCoords =
            MotionEvent.PointerCoords().also {
                it.x = x
                it.y = y
            }
        return MotionEvent.obtain(
            0L /* downTime */,
            0L /* eventTime */,
            action,
            1 /* pointerCount */,
            arrayOf(pointerProperties),
            arrayOf(pointerCoords),
            0 /* metaState */,
            0 /* buttonState */,
            0f /* xPrecision */,
            0f /* yPrecision */,
            0 /* deviceId */,
            0 /* edgeFlags */,
            InputDevice.SOURCE_TOUCHSCREEN,
            0, /* flags */
        )
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned_completelyCovered() {
        // Arrange.
        val parentTag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        setContent {
            Box(Modifier.testTag(parentTag)) {
                with(LocalDensity.current) {
                    BasicText(
                        "Child One",
                        Modifier.zIndex(1f).testTag(childOneTag).requiredSize(50.toDp()),
                    )
                    BasicText("Child Two", Modifier.testTag(childTwoTag).requiredSize(50.toDp()))
                }
            }
        }
        val parentNodeId = rule.onNodeWithTag(parentTag).semanticsId()
        val overlappedChildOneNodeId = rule.onNodeWithTag(childOneTag).semanticsId()
        val overlappedChildTwoNodeId = rule.onNodeWithTag(childTwoTag).semanticsId()

        // Assert.
        rule.runOnIdle {
            assertThat(createAccessibilityNodeInfo(parentNodeId).childCount).isEqualTo(1)
            assertThat(createAccessibilityNodeInfo(overlappedChildOneNodeId).text.toString())
                .isEqualTo("Child One")
            assertThat(provider.createAccessibilityNodeInfo(overlappedChildTwoNodeId)).isNull()
        }
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned_partiallyCovered() {
        // Arrange.
        val parentTag = "parent"
        val density = Density(2f)
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                Box(Modifier.testTag(parentTag)) {
                    with(LocalDensity.current) {
                        BasicText("Child One", Modifier.zIndex(1f).requiredSize(100.toDp()))
                        BasicText("Child Two", Modifier.requiredSize(200.toDp(), 100.toDp()))
                    }
                }
            }
        }
        val parentNodeId = rule.onNodeWithTag(parentTag).semanticsId()
        val childTwoId = rule.onNodeWithText("Child Two").semanticsId()
        val childTwoBounds = Rect()

        // Act.
        rule.waitForIdle()
        val parentInfo = createAccessibilityNodeInfo(parentNodeId)
        createAccessibilityNodeInfo(childTwoId).getBoundsInScreen(childTwoBounds)

        // Assert.
        rule.runOnIdle {
            assertThat(parentInfo.childCount).isEqualTo(2)
            assertThat(childTwoBounds.height()).isEqualTo(100)
            assertThat(childTwoBounds.width()).isEqualTo(100)
        }
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned_testTagOnlyDoesNotPrune() {
        // Arrange.
        val parentTag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        setContent {
            Box(Modifier.testTag(parentTag)) {
                with(LocalDensity.current) {
                    Box(Modifier.zIndex(1f).testTag(childOneTag).requiredSize(50.toDp()))
                    BasicText("Child Two", Modifier.testTag(childTwoTag).requiredSize(50.toDp()))
                }
            }
        }
        val parentNodeId = rule.onNodeWithTag(parentTag).semanticsId()
        val overlappedChildTwoNodeId = rule.onNodeWithTag(childTwoTag).semanticsId()

        rule.runOnIdle {
            assertThat(createAccessibilityNodeInfo(parentNodeId).childCount).isEqualTo(2)
            assertThat(createAccessibilityNodeInfo(overlappedChildTwoNodeId).text.toString())
                .isEqualTo("Child Two")
        }
    }

    @Test
    fun testPaneAppear() {
        var isPaneVisible by mutableStateOf(false)
        val paneTestTitle by mutableStateOf("pane title")

        setContent {
            if (isPaneVisible) {
                Box(Modifier.testTag(tag).semantics { paneTitle = paneTestTitle }) {}
            }
        }

        rule.onNodeWithTag(tag).assertDoesNotExist()

        isPaneVisible = true
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        val paneId =
            rule
                .onNodeWithTag(tag)
                .assert(expectValue(SemanticsProperties.PaneTitle, "pane title"))
                .assertIsDisplayed()
                .semanticsId()
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == paneId &&
                                it.eventType == TYPE_WINDOW_STATE_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_PANE_APPEARED
                        }
                    ),
                )
        }
    }

    @Test
    fun testPaneTitleChange() {
        var isPaneVisible by mutableStateOf(false)
        var paneTestTitle by mutableStateOf("pane title")
        setContent {
            if (isPaneVisible) {
                Box(Modifier.testTag(tag).semantics { paneTitle = paneTestTitle }) {}
            }
        }

        rule.onNodeWithTag(tag).assertDoesNotExist()

        isPaneVisible = true
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        rule
            .onNodeWithTag(tag)
            .assert(expectValue(SemanticsProperties.PaneTitle, "pane title"))
            .assertIsDisplayed()

        paneTestTitle = "new pane title"
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        val paneId =
            rule
                .onNodeWithTag(tag)
                .assert(expectValue(SemanticsProperties.PaneTitle, "new pane title"))
                .semanticsId()
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            getAccessibilityEventSourceSemanticsNodeId(it) == paneId &&
                                it.eventType == TYPE_WINDOW_STATE_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_PANE_TITLE
                        }
                    ),
                )
        }
    }

    @Test
    fun testPaneDisappear() {
        var isPaneVisible by mutableStateOf(false)
        val paneTestTitle by mutableStateOf("pane title")
        setContent {
            if (isPaneVisible) {
                Box(Modifier.testTag(tag).semantics { paneTitle = paneTestTitle }) {}
            }
        }

        rule.onNodeWithTag(tag).assertDoesNotExist()

        isPaneVisible = true
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        rule
            .onNodeWithTag(tag)
            .assert(expectValue(SemanticsProperties.PaneTitle, "pane title"))
            .assertIsDisplayed()

        isPaneVisible = false
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        rule.onNodeWithTag(tag).assertDoesNotExist()
        rule.runOnIdle {
            verify(container, times(1))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            it.eventType == TYPE_WINDOW_STATE_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
                        }
                    ),
                )
        }
    }

    @Test
    fun testMultiPanesDisappear() {
        val firstPaneTag = "Pane 1"
        val secondPaneTag = "Pane 2"
        var isPaneVisible by mutableStateOf(false)
        val firstPaneTestTitle by mutableStateOf("first pane title")
        val secondPaneTestTitle by mutableStateOf("second pane title")

        setContent {
            if (isPaneVisible) {
                Column {
                    with(LocalDensity.current) {
                        Box(
                            Modifier.size(100.toDp()).testTag(firstPaneTag).semantics {
                                paneTitle = firstPaneTestTitle
                            }
                        ) {}
                        Box(
                            Modifier.size(100.toDp()).testTag(secondPaneTag).semantics {
                                paneTitle = secondPaneTestTitle
                            }
                        ) {}
                    }
                }
            }
        }

        rule.onNodeWithTag(firstPaneTag).assertDoesNotExist()
        rule.onNodeWithTag(secondPaneTag).assertDoesNotExist()

        isPaneVisible = true
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        rule
            .onNodeWithTag(firstPaneTag)
            .assert(expectValue(SemanticsProperties.PaneTitle, "first pane title"))
            .assertIsDisplayed()
        rule
            .onNodeWithTag(secondPaneTag)
            .assert(expectValue(SemanticsProperties.PaneTitle, "second pane title"))
            .assertIsDisplayed()

        isPaneVisible = false
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        rule.onNodeWithTag(firstPaneTag).assertDoesNotExist()
        rule.onNodeWithTag(secondPaneTag).assertDoesNotExist()
        rule.runOnIdle {
            verify(container, times(2))
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(
                        ArgumentMatcher {
                            it.eventType == TYPE_WINDOW_STATE_CHANGED &&
                                it.contentChangeTypes == CONTENT_CHANGE_TYPE_PANE_DISAPPEARED
                        }
                    ),
                )
        }
    }

    @Test
    fun testEventForPasswordTextField() {
        // Arrange.
        setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag),
                value = "value",
                onValueChange = {},
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        // Act.
        rule.onNodeWithTag(tag).performSemanticsAction(SetText) { it(AnnotatedString("new value")) }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    eq(androidComposeView),
                    argThat(ArgumentMatcher { it.isPassword }),
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun testCreateEvent_SensitiveDataFieldMatchesNode() {
        setContent {
            BasicTextField(
                modifier = Modifier.testTag(tag).semantics { isSensitiveData = true },
                value = "value",
                onValueChange = {},
            )
        }

        rule.onNodeWithTag(tag).performSemanticsAction(SetText) { it(AnnotatedString("new value")) }

        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            verify(container, atLeastOnce())
                .requestSendAccessibilityEvent(
                    any(),
                    argThat(ArgumentMatcher { it.isAccessibilityDataSensitive }),
                )
        }
    }

    @Test
    fun testLayerParamChange_setCorrectBounds_syntaxOne() {
        var scale by mutableStateOf(1f)
        setContent {
            // testTag must not be on the same node with graphicsLayer, otherwise we will have
            // semantics change notification.
            with(LocalDensity.current) {
                Box(
                    Modifier.graphicsLayer(scaleX = scale, scaleY = scale).requiredSize(300.toDp())
                ) {
                    Box(Modifier.matchParentSize().testTag("node"))
                }
            }
        }
        val virtualViewId = rule.onNodeWithTag("node").semanticsId()

        var info = AccessibilityNodeInfoCompat.obtain()
        rule.runOnUiThread { info = createAccessibilityNodeInfo(virtualViewId) }
        val rect = Rect()
        info.getBoundsInScreen(rect)
        assertThat(rect.width()).isEqualTo(300)
        assertThat(rect.height()).isEqualTo(300)

        scale = 0.5f
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        @Suppress("DEPRECATION") info.recycle()
        rule.runOnIdle { info = createAccessibilityNodeInfo(virtualViewId) }
        info.getBoundsInScreen(rect)
        assertThat(rect.width()).isEqualTo(150)
        assertThat(rect.height()).isEqualTo(150)
    }

    @Test
    fun testLayerParamChange_setCorrectBounds_syntaxTwo() {
        var scale by mutableStateOf(1f)
        setContent {
            // testTag must not be on the same node with graphicsLayer, otherwise we will have
            // semantics change notification.
            with(LocalDensity.current) {
                Box(
                    Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .requiredSize(300.toDp())
                ) {
                    Box(Modifier.matchParentSize().testTag(tag))
                }
            }
        }

        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        var info = AccessibilityNodeInfoCompat.obtain()
        rule.runOnUiThread { info = createAccessibilityNodeInfo(virtualViewId) }
        val rect = Rect()
        info.getBoundsInScreen(rect)
        assertThat(rect.width()).isEqualTo(300)
        assertThat(rect.height()).isEqualTo(300)

        scale = 0.5f
        @Suppress("DEPRECATION") info.recycle()
        rule.runOnIdle { info = createAccessibilityNodeInfo(virtualViewId) }
        info.getBoundsInScreen(rect)
        assertThat(rect.width()).isEqualTo(150)
        assertThat(rect.height()).isEqualTo(150)
    }

    @Test
    fun testSemanticsHitTest_unimportantTraversalProperties() {
        // Arrange.
        setContent {
            Box(
                Modifier.size(100.dp).testTag(tag).semantics {
                    isTraversalGroup = true
                    traversalIndex = 1f
                }
            ) {}
        }
        val bounds = with(rule.density) { rule.onNodeWithTag(tag).getBoundsInRoot().toRect() }

        // Act.
        val hitNodeId =
            rule.runOnIdle {
                delegate.hitTestSemanticsAt(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height / 2,
                )
            }

        // Assert it doesn't hit the tagged node since it only has unimportant properties.
        rule.runOnIdle { assertThat(hitNodeId).isEqualTo(InvalidId) }
    }

    @Test
    fun testAccessibilityNodeInfoTreePruned_hideFromAccessibilityDoesNotPrune() {
        // Arrange.
        val parentTag = "ParentForOverlappedChildren"
        val childOneTag = "OverlappedChildOne"
        val childTwoTag = "OverlappedChildTwo"
        setContent {
            Box(Modifier.testTag(parentTag)) {
                with(LocalDensity.current) {
                    BasicText(
                        "Child One",
                        Modifier.zIndex(1f)
                            .testTag(childOneTag)
                            .semantics { hideFromAccessibility() }
                            .requiredSize(50.toDp()),
                    )
                    BasicText("Child Two", Modifier.testTag(childTwoTag).requiredSize(50.toDp()))
                }
            }
        }
        val parentNodeId = rule.onNodeWithTag(parentTag).semanticsId()
        val overlappedChildTwoNodeId = rule.onNodeWithTag(childTwoTag).semanticsId()

        rule.runOnIdle {
            assertThat(createAccessibilityNodeInfo(parentNodeId).childCount).isEqualTo(2)
            assertThat(createAccessibilityNodeInfo(overlappedChildTwoNodeId).text.toString())
                .isEqualTo("Child Two")
        }
    }

    @Test
    fun testDialog_setCorrectBounds() {
        var dialogComposeView: AndroidComposeView? = null
        setContent {
            Dialog(onDismissRequest = {}) {
                dialogComposeView = LocalView.current as AndroidComposeView
                delegate =
                    ViewCompat.getAccessibilityDelegate(dialogComposeView!!)
                        as AndroidComposeViewAccessibilityDelegateCompat
                provider = delegate.getAccessibilityNodeProvider(dialogComposeView!!)

                with(LocalDensity.current) {
                    Box(Modifier.size(300.toDp())) {
                        BasicText(
                            text = "text",
                            modifier = Modifier.offset(100.toDp(), 100.toDp()).fillMaxSize(),
                        )
                    }
                }
            }
        }
        val virtualViewId = rule.onNodeWithText("text").semanticsId()

        var info = AccessibilityNodeInfoCompat.obtain()
        rule.runOnUiThread { info = createAccessibilityNodeInfo(virtualViewId) }

        val viewPosition = intArrayOf(0, 0)
        dialogComposeView!!.getLocationOnScreen(viewPosition)
        val offset = 100
        val size = 200
        val textPositionOnScreenX = viewPosition[0] + offset
        val textPositionOnScreenY = viewPosition[1] + offset

        val textRect = Rect()
        info.getBoundsInScreen(textRect)
        assertThat(textRect)
            .isEqualTo(
                Rect(
                    textPositionOnScreenX,
                    textPositionOnScreenY,
                    textPositionOnScreenX + size,
                    textPositionOnScreenY + size,
                )
            )
    }

    @Test
    fun testTestTagsAsResourceId() {
        // Arrange.
        val tag1 = "box1"
        val tag2 = "box2"
        val tag3 = "box3"
        val tag4 = "box4"
        val tag5 = "box5"
        val tag6 = "box6"
        val tag7 = "box7"
        setContent {
            with(LocalDensity.current) {
                Column {
                    Box(Modifier.size(100.toDp()).testTag(tag1))
                    Box(Modifier.semantics { testTagsAsResourceId = true }) {
                        Box(Modifier.size(100.toDp()).testTag(tag2))
                    }
                    Box(Modifier.semantics { testTagsAsResourceId = false }) {
                        Box(Modifier.size(100.toDp()).testTag(tag3))
                    }
                    Box(Modifier.semantics { testTagsAsResourceId = true }) {
                        Box(Modifier.semantics { testTagsAsResourceId = false }) {
                            Box(Modifier.size(100.toDp()).testTag(tag4))
                        }
                    }
                    Box(Modifier.semantics { testTagsAsResourceId = false }) {
                        Box(Modifier.semantics { testTagsAsResourceId = true }) {
                            Box(Modifier.size(100.toDp()).testTag(tag5))
                        }
                    }
                    Box(Modifier.semantics(true) { testTagsAsResourceId = true }) {
                        Box(Modifier.semantics { testTagsAsResourceId = false }) {
                            Box(Modifier.size(100.toDp()).testTag(tag6))
                        }
                    }
                    Box(Modifier.semantics(true) { testTagsAsResourceId = false }) {
                        Box(Modifier.semantics { testTagsAsResourceId = true }) {
                            Box(Modifier.size(100.toDp()).testTag(tag7))
                        }
                    }
                }
            }
        }
        val box1Id = rule.onNodeWithTag(tag1).semanticsId()
        val box2Id = rule.onNodeWithTag(tag2).semanticsId()
        val box3Id = rule.onNodeWithTag(tag3).semanticsId()
        val box4Id = rule.onNodeWithTag(tag4).semanticsId()
        val box5Id = rule.onNodeWithTag(tag5).semanticsId()
        val box6Id = rule.onNodeWithTag(tag6, true).semanticsId()
        val box7Id = rule.onNodeWithTag(tag7, true).semanticsId()

        // Act.
        rule.waitForIdle()
        val info1 = createAccessibilityNodeInfo(box1Id)
        val info2 = createAccessibilityNodeInfo(box2Id)
        val info3 = createAccessibilityNodeInfo(box3Id)
        val info4 = createAccessibilityNodeInfo(box4Id)
        val info5 = createAccessibilityNodeInfo(box5Id)
        val info6 = createAccessibilityNodeInfo(box6Id)
        val info7 = createAccessibilityNodeInfo(box7Id)

        // Assert.
        rule.runOnIdle {
            assertThat(info1.viewIdResourceName).isNull()
            assertThat(info2.viewIdResourceName).isEqualTo(tag2)
            assertThat(info3.viewIdResourceName).isNull()
            assertThat(info4.viewIdResourceName).isNull()
            assertThat(info5.viewIdResourceName).isEqualTo(tag5)
            assertThat(info6.viewIdResourceName).isNull()
            assertThat(info7.viewIdResourceName).isEqualTo(tag7)
        }
    }

    @Test
    fun testContentDescription_notMergingDescendants_withOwnContentDescription() {
        // Arrange.
        setContent {
            Column(Modifier.semantics { contentDescription = "Column" }.testTag(tag)) {
                with(LocalDensity.current) {
                    BasicText("Text")
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Box" })
                }
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle { assertThat(info.contentDescription).isEqualTo("Column") }
    }

    @Test
    fun testContentDescription_notMergingDescendants_withoutOwnContentDescription() {
        // Arrange.
        setContent {
            Column(Modifier.semantics {}.testTag(tag)) {
                BasicText("Text")
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Box" })
                }
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.contentDescription).isNull() }
    }

    @Test
    fun testContentDescription_singleNode_notMergingDescendants() {
        // Arrange.
        setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).testTag(tag).semantics { contentDescription = "Box" })
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.contentDescription).isEqualTo("Box") }
    }

    @Test
    fun testContentDescription_singleNode_mergingDescendants() {
        // Arrange.
        setContent {
            with(LocalDensity.current) {
                Box(
                    Modifier.size(100.toDp()).testTag(tag).semantics(true) {
                        contentDescription = "Box"
                    }
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.contentDescription).isEqualTo("Box") }
    }

    @Test
    fun testContentDescription_replacingSemanticsNode() {
        // Arrange.
        setContent {
            with(LocalDensity.current) {
                Column(
                    Modifier.size(100.toDp()).testTag(tag).clearAndSetSemantics {
                        contentDescription = "Replacing description"
                    }
                ) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Box one" })
                    Box(
                        Modifier.size(100.toDp()).semantics(true) { contentDescription = "Box two" }
                    )
                }
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.contentDescription).isEqualTo("Replacing description") }
    }

    @Test
    fun testRole_doesNotMerge() {
        // Arrange.
        setContent {
            Row(Modifier.semantics(true) {}.testTag("Row")) {
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { role = Role.Button })
                    Box(Modifier.size(100.toDp()).semantics { role = Role.Image })
                }
            }
        }
        val virtualViewId = rule.onNodeWithTag("Row").semanticsId()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo(ClassName) }
    }

    @Test
    fun testReportedBounds_clickableNode_includesPadding(): Unit =
        with(rule.density) {
            // Arrange.
            val size = 100.dp.roundToPx()
            setContent {
                with(LocalDensity.current) {
                    Column {
                        Box(
                            Modifier.testTag("tag")
                                .clickable {}
                                .size(size.toDp())
                                .padding(10.toDp())
                                .semantics { contentDescription = "Button" }
                        )
                    }
                }
            }
            val virtualViewId = rule.onNodeWithTag("tag").semanticsId()

            // Act.
            val accessibilityNodeInfo =
                rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

            // Assert.
            rule.runOnIdle {
                val rect = Rect()
                accessibilityNodeInfo.getBoundsInScreen(rect)
                val resultWidth = rect.right - rect.left
                val resultHeight = rect.bottom - rect.top
                assertThat(resultWidth).isEqualTo(size)
                assertThat(resultHeight).isEqualTo(size)
            }
        }

    @Test
    fun testReportedBounds_clickableNode_excludesPadding(): Unit =
        with(rule.density) {
            // Arrange.
            val size = 100.dp.roundToPx()
            val density = Density(2f)
            setContent {
                CompositionLocalProvider(LocalDensity provides density) {
                    Column {
                        with(density) {
                            Box(
                                Modifier.testTag("tag")
                                    .semantics { contentDescription = "Test" }
                                    .size(size.toDp())
                                    .padding(10.toDp())
                                    .clickable {}
                            )
                        }
                    }
                }
            }

            val virtualViewId = rule.onNodeWithTag("tag").semanticsId()

            // Act.
            val accessibilityNodeInfo =
                rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

            // Assert.
            rule.runOnIdle {
                val rect = Rect()
                accessibilityNodeInfo.getBoundsInScreen(rect)
                val resultWidth = rect.right - rect.left
                val resultHeight = rect.bottom - rect.top
                assertThat(resultWidth).isEqualTo(size - 20)
                assertThat(resultHeight).isEqualTo(size - 20)
            }
        }

    @Test
    fun testReportedBounds_withClearAndSetSemantics() {
        // Arrange.
        val size = 100
        setContent {
            with(LocalDensity.current) {
                Column {
                    Box(
                        Modifier.testTag("tag")
                            .size(size.toDp())
                            .padding(10.toDp())
                            .clearAndSetSemantics {}
                            .clickable {}
                    )
                }
            }
        }

        val virtualViewId = rule.onNodeWithTag("tag").semanticsId()

        // Act.
        val accessibilityNodeInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            val rect = Rect()
            accessibilityNodeInfo.getBoundsInScreen(rect)
            val resultWidth = rect.right - rect.left
            val resultHeight = rect.bottom - rect.top
            assertThat(resultWidth).isEqualTo(size)
            assertThat(resultHeight).isEqualTo(size)
        }
    }

    @Test
    fun testReportedBounds_withTwoClickable_outermostWins(): Unit =
        with(rule.density) {
            // Arrange.
            val size = 100.dp.roundToPx()
            setContent {
                with(LocalDensity.current) {
                    Column {
                        Box(
                            Modifier.testTag(tag)
                                .clickable {}
                                .size(size.toDp())
                                .padding(10.toDp())
                                .clickable {}
                        )
                    }
                }
            }
            val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

            // Act.
            val accessibilityNodeInfo =
                rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

            // Assert.
            rule.runOnIdle {
                val rect = Rect()
                accessibilityNodeInfo.getBoundsInScreen(rect)
                val resultWidth = rect.right - rect.left
                val resultHeight = rect.bottom - rect.top
                assertThat(resultWidth).isEqualTo(size)
                assertThat(resultHeight).isEqualTo(size)
            }
        }

    @Test
    fun testReportedBounds_outerMostSemanticsUsed() {
        // Arrange.
        val size = 100
        setContent {
            with(LocalDensity.current) {
                Column {
                    Box(
                        Modifier.testTag(tag)
                            .semantics { contentDescription = "Test1" }
                            .size(size.toDp())
                            .padding(10.toDp())
                            .semantics { contentDescription = "Test2" }
                    )
                }
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val accessibilityNodeInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            val rect = Rect()
            accessibilityNodeInfo.getBoundsInScreen(rect)
            val resultWidth = rect.right - rect.left
            val resultHeight = rect.bottom - rect.top
            assertThat(resultWidth).isEqualTo(size)
            assertThat(resultHeight).isEqualTo(size)
        }
    }

    @Test
    fun testReportedBounds_withOffset() {
        // Arrange.
        val size = 100
        val offset = 10
        val density = Density(1f)
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                with(LocalDensity.current) {
                    Column {
                        Box(
                            Modifier.size(size.toDp())
                                .offset(offset.toDp(), offset.toDp())
                                .testTag(tag)
                                .semantics { contentDescription = "Test" }
                        )
                    }
                }
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val accessibilityNodeInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            val rect = Rect()
            accessibilityNodeInfo.getBoundsInScreen(rect)
            val resultWidth = rect.right - rect.left
            val resultHeight = rect.bottom - rect.top
            val resultInLocalCoords = androidComposeView.screenToLocal(rect.toComposeRect().topLeft)
            assertThat(resultWidth).isEqualTo(size)
            assertThat(resultHeight).isEqualTo(size)
            assertThat(resultInLocalCoords.x).isWithin(0.001f).of(10f)
            assertThat(resultInLocalCoords.y).isWithin(0.001f).of(10f)
        }
    }

    @Test
    fun testSemanticsNodePositionAndBounds_doesNotThrow_whenLayoutNodeNotAttached() {
        // Assert.
        var emitNode by mutableStateOf(true)
        setContent {
            if (emitNode) {
                with(LocalDensity.current) { Box(Modifier.size(100.toDp()).testTag(tag)) }
            }
        }
        val semanticNode = rule.onNodeWithTag(tag).fetchSemanticsNode()

        // Act.
        rule.runOnIdle { emitNode = false }

        // Assert.
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        rule.runOnIdle {
            with(semanticNode) {
                assertThat(positionInRoot).isEqualTo(Offset.Zero)
                assertThat(positionInWindow).isEqualTo(Offset.Zero)
                assertThat(boundsInRoot).isEqualTo(androidx.compose.ui.geometry.Rect.Zero)
                assertThat(boundsInWindow).isEqualTo(androidx.compose.ui.geometry.Rect.Zero)
            }
        }
    }

    @Test
    fun testSemanticsSort_doesNotThrow_whenCoordinatorNotAttached() {
        // Arrange.
        setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).testTag("parent")) {
                    Box(Modifier.size(100.toDp()).testTag("child"))
                }
            }
        }
        val parent = rule.onNodeWithTag("parent").fetchSemanticsNode()
        val child = rule.onNodeWithTag("child").fetchSemanticsNode()

        // Act.
        rule.runOnIdle { child.layoutNode.innerCoordinator.onRelease() }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.unmergedChildren(includeFakeNodes = true).size).isEqualTo(1)
            assertThat(child.unmergedChildren(includeFakeNodes = true).size).isEqualTo(0)
        }
    }

    @Test
    fun testSemanticsSort_doesNotThrow_whenCoordinatorNotAttached_compare() {
        // Arrange.
        setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).testTag("parent")) {
                    Box(Modifier.size(100.toDp()).testTag("child1")) {
                        Box(Modifier.size(50.toDp()).testTag("grandChild1"))
                    }
                    Box(Modifier.size(100.toDp()).testTag("child2")) {
                        Box(Modifier.size(50.toDp()).testTag("grandChild2"))
                    }
                }
            }
        }
        val parent = rule.onNodeWithTag("parent").fetchSemanticsNode()
        val grandChild1 = rule.onNodeWithTag("grandChild1").fetchSemanticsNode()
        val grandChild2 = rule.onNodeWithTag("grandChild2").fetchSemanticsNode()

        // Act.
        rule.runOnIdle {
            grandChild1.layoutNode.innerCoordinator.onRelease()
            grandChild2.layoutNode.innerCoordinator.onRelease()
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.unmergedChildren(includeFakeNodes = true).size).isEqualTo(2)
        }
    }

    @Test
    fun testFakeNodeCreated_forContentDescriptionSemantics() {
        // Arrange.
        setContent {
            Column(Modifier.semantics(true) { contentDescription = "Test" }.testTag(tag)) {
                BasicText("Text")
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Hello" })
                }
            }
        }
        val columnNode = rule.onNodeWithTag(tag, true).fetchSemanticsNode()

        // Act.
        val firstChild = rule.runOnIdle { columnNode.replacedChildren.first() }

        // Assert.
        rule.runOnIdle {
            assertThat(firstChild.isFake).isTrue()
            assertThat(firstChild.unmergedConfig[ContentDescription].first()).isEqualTo("Test")
        }
    }

    @Test
    fun testFakeNode_forContentDescriptionSemantics_id() {
        setContent {
            Column(Modifier.semantics(true) { contentDescription = "Test" }.testTag(tag)) {
                BasicText("Text")
                with(LocalDensity.current) {
                    Box(Modifier.size(100.toDp()).semantics { contentDescription = "Hello" })
                }
            }
        }
        val columnNode = rule.onNodeWithTag(tag, true).fetchSemanticsNode()
        val fakeNode = rule.runOnIdle { columnNode.replacedChildren.first() }
        val fakeNodeId = fakeNode.id

        val fakeNodeInfo = rule.runOnIdle { createAccessibilityNodeInfo(fakeNodeId) }
        val fakeNodeInfoId = getAccessibilityNodeInfoSourceSemanticsNodeId(fakeNodeInfo)

        // Assert.
        rule.runOnIdle { assertThat(fakeNodeInfoId).isEqualTo(fakeNodeId) }
    }

    @Test
    fun testFakeNode_createdForButton() {
        // Arrange.
        setContent {
            Column(Modifier.clickable(role = Role.Button) {}.testTag(tag)) { BasicText("Text") }
        }
        val buttonNode = rule.onNodeWithTag(tag, true).fetchSemanticsNode()

        // Act.
        val lastChild = rule.runOnIdle { buttonNode.replacedChildren.lastOrNull() }

        // Assert.
        rule.runOnIdle {
            assertThat(lastChild?.isFake).isTrue()
            assertThat(lastChild?.unmergedConfig?.getOrNull(SemanticsProperties.Role))
                .isEqualTo(Role.Button)
        }
    }

    @Test
    fun testFakeNode_createdForButton_id() {
        // Arrange.
        setContent {
            Column(Modifier.clickable(role = Role.Button) {}.testTag(tag)) { BasicText("Text") }
        }

        val buttonNode = rule.onNodeWithTag(tag, true).fetchSemanticsNode()

        val fakeNode = rule.runOnIdle { buttonNode.replacedChildren.lastOrNull() }
        val fakeNodeId = fakeNode?.id

        val fakeNodeInfo = rule.runOnIdle { fakeNodeId?.let { createAccessibilityNodeInfo(it) } }
        val fakeNodeInfoId = fakeNodeInfo?.let { getAccessibilityNodeInfoSourceSemanticsNodeId(it) }

        // Assert.
        rule.runOnIdle { assertThat(fakeNodeInfoId).isEqualTo(fakeNodeId) }
    }

    @Test
    fun testFakeNode_notCreatedForButton_whenNoChildren() {
        // Arrange.
        setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp()).clickable(role = Role.Button) {}.testTag("button"))
            }
        }
        val buttonNode = rule.onNodeWithTag("button").fetchSemanticsNode()
        assertThat(buttonNode.unmergedChildren().any { it.isFake }).isFalse()

        // Act.
        val info = rule.runOnIdle { createAccessibilityNodeInfo(buttonNode.id) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.Button") }
    }

    @Test
    fun testFakeNode_reportParentBoundsAsFakeNodeBounds() {
        // Arrange.
        val density = Density(2f)
        setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                with(density) {
                    Box(Modifier.size(100.toDp()).clickable(role = Role.Button) {}.testTag(tag)) {
                        BasicText("Example")
                    }
                }
            }
        }

        // Button node
        val parentNode = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val parentBounds = Rect()
        createAccessibilityNodeInfo(parentNode.id).getBoundsInScreen(parentBounds)

        // Button role fake node
        val fakeRoleNode = parentNode.unmergedChildren(includeFakeNodes = true).last()
        val fakeRoleNodeBounds = Rect()
        createAccessibilityNodeInfo(fakeRoleNode.id).getBoundsInScreen(fakeRoleNodeBounds)

        assertThat(fakeRoleNodeBounds).isEqualTo(parentBounds)
    }

    @Test
    fun testContentDescription_withFakeNode_mergedCorrectly() {
        // Arrange.
        setContent {
            Column(Modifier.testTag(tag).semantics(true) { contentDescription = "Hello" }) {
                Box(Modifier.semantics { contentDescription = "World" })
            }
        }

        // Assert.
        rule.onNodeWithTag(tag).assertContentDescriptionEquals("Hello", "World")
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testScreenReaderFocusable_notSet_whenAncestorMergesDescendants() {
        // Arrange.
        setContent {
            Column(Modifier.semantics(true) {}) { BasicText("test", Modifier.testTag(tag)) }
        }
        val virtualViewId = rule.onNodeWithTag(tag, useUnmergedTree = true).semanticsId()

        // Act.
        val childInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(childInfo.isScreenReaderFocusable).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testScreenReaderFocusable_set_whenAncestorDoesNotMerge() {
        // Arrange.
        setContent {
            Column(Modifier.semantics(false) {}) { BasicText("test", Modifier.testTag(tag)) }
        }
        val virtualViewId = rule.onNodeWithTag(tag, useUnmergedTree = true).semanticsId()

        // Act.
        val childInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(childInfo.isScreenReaderFocusable).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testScreenReaderFocusable_notSet_whenChildNotSpeakable() {
        // Arrange.
        setContent {
            Column(Modifier.semantics(false) {}) { Box(Modifier.testTag(tag).size(100.dp)) }
        }
        val virtualViewId = rule.onNodeWithTag(tag, useUnmergedTree = true).semanticsId()

        // Act.
        val childInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(childInfo.isScreenReaderFocusable).isFalse() }
    }

    @Test
    fun testImageRole_notSet_whenAncestorMergesDescendants() {
        // Arrange.
        setContent {
            Column(Modifier.semantics(true) {}) {
                Image(ImageBitmap(100, 100), "Image", Modifier.testTag(tag))
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag, true).semanticsId()

        // Act.
        val imageInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(imageInfo.className).isEqualTo(ClassName) }
    }

    @Test
    fun testImageRole_set_whenAncestorDoesNotMerge() {
        // Arrange.
        setContent {
            Column(Modifier.semantics { isEnabled() }) {
                Image(ImageBitmap(100, 100), "Image", Modifier.testTag(tag))
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag, true).semanticsId()

        // Act.
        val imageInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(imageInfo.className).isEqualTo("android.widget.ImageView") }
    }

    @Test
    fun testImageRole_set_whenImageItseldMergesDescendants() {
        // Arrange.
        setContent {
            Column(Modifier.semantics(true) {}) {
                Image(
                    ImageBitmap(100, 100),
                    "Image",
                    Modifier.testTag(tag).semantics(true) { /* imitate clickable node */ },
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag, true).semanticsId()

        // Act.
        val imageInfo = rule.runOnIdle { createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(imageInfo.className).isEqualTo("android.widget.ImageView") }
    }

    @Test
    fun testScrollableContainer_scrollViewClassNotSet_whenCollectionInfo() {
        // Arrange.
        val tagColumn = "lazy column"
        val tagRow = "scrollable row"
        setContent {
            LazyColumn(Modifier.testTag(tagColumn)) {
                item {
                    Row(
                        Modifier.testTag(tagRow)
                            .scrollable(rememberScrollState(), Orientation.Horizontal)
                    ) {
                        BasicText("test")
                    }
                }
            }
        }
        val columnId = rule.onNodeWithTag(tagColumn).semanticsId()
        val rowId = rule.onNodeWithTag(tagRow).semanticsId()

        // Act.
        rule.waitForIdle()
        val columnInfo = createAccessibilityNodeInfo(columnId)
        val rowInfo = createAccessibilityNodeInfo(rowId)

        // Assert.
        rule.runOnIdle {
            assertThat(columnInfo.className).isNotEqualTo("android.widget.ScrollView")
            assertThat(rowInfo.className).isNotEqualTo("android.widget.HorizontalScrollView")
        }
    }

    @Test
    fun testAndroidViewSemanticBounds_whenAddedInRecomposition() {
        // disable to ensure that androidViewHandler is not created by accessibility observers
        delegate.accessibilityForceEnabledForTesting = false

        var state by mutableStateOf(false)
        val viewId = 42
        val viewSize = 50
        setContent {
            Box(Modifier.size(100.dp)) {
                if (state) {
                    AndroidView(
                        factory = {
                            FrameLayout(it).apply {
                                addView(
                                    View(it).apply {
                                        layoutParams = FrameLayout.LayoutParams(viewSize, viewSize)
                                        setBackgroundColor(Color.Red.toArgb())
                                        id = viewId
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
        rule.mainClock.autoAdvance = true // ensure complete recomposition

        rule.runOnIdle { state = true }

        rule.waitForIdle()
        val info = androidComposeView.findViewById<View>(viewId).createAccessibilityNodeInfo()
        val bounds = Rect().apply { info.getBoundsInScreen(this) }
        assertThat(bounds.width()).isEqualTo(viewSize)
        assertThat(bounds.height()).isEqualTo(viewSize)
    }

    @Test
    fun testTransparentNode_withAlphaModifier_notAccessible() {
        // Arrange.
        setContent {
            Column(Modifier.testTag("parent")) {
                val modifier = Modifier.size(100.dp)
                Box(Modifier.alpha(0f)) {
                    Box(
                        modifier.semantics {
                            testTag = "child1"
                            contentDescription = "test"
                        }
                    )
                }
                Box(
                    Modifier.alpha(0f).then(modifier).semantics {
                        testTag = "child2"
                        contentDescription = "test"
                    }
                )
                Box(
                    Modifier.alpha(0f)
                        .semantics {
                            testTag = "child3"
                            contentDescription = "test"
                        }
                        .then(modifier)
                )
                Box(
                    modifier.alpha(0f).semantics {
                        testTag = "child4"
                        contentDescription = "test"
                    }
                )
                Box(
                    Modifier.size(100.dp).alpha(0f).shadow(2.dp).semantics {
                        testTag = "child5"
                        contentDescription = "test"
                    }
                )
            }
        }
        val parentId = rule.onNodeWithTag("parent").semanticsId()
        val child1Id = rule.onNodeWithTag("child1").semanticsId()
        val child2Id = rule.onNodeWithTag("child2").semanticsId()
        val child3Id = rule.onNodeWithTag("child3").semanticsId()
        val child4Id = rule.onNodeWithTag("child4").semanticsId()
        val child5Id = rule.onNodeWithTag("child5").semanticsId()

        // Act.
        rule.waitForIdle()
        val parent = createAccessibilityNodeInfo(parentId)
        val child1 = createAccessibilityNodeInfo(child1Id)
        val child2 = createAccessibilityNodeInfo(child2Id)
        val child3 = createAccessibilityNodeInfo(child3Id)
        val child4 = createAccessibilityNodeInfo(child4Id)
        val child5 = createAccessibilityNodeInfo(child5Id)

        // Assert.
        rule.runOnIdle {
            assertThat(parent.childCount).isEqualTo(5)
            assertThat(child1.isVisibleToUser).isFalse()
            assertThat(child2.isVisibleToUser).isFalse()
            assertThat(child3.isVisibleToUser).isFalse()
            assertThat(child4.isVisibleToUser).isFalse()
            assertThat(child5.isVisibleToUser).isFalse()
        }
    }

    @Test
    fun testVisibleNode_withAlphaModifier_accessible() {
        // Arrange.
        setContent {
            Column(Modifier.testTag("parent")) {
                val modifier = Modifier.size(100.dp)
                Box(
                    Modifier.semantics {
                            testTag = "child1"
                            contentDescription = "test"
                        }
                        .then(modifier)
                        .alpha(0f)
                )
                Box(
                    Modifier.semantics {
                            testTag = "child2"
                            contentDescription = "test"
                        }
                        .alpha(0f)
                        .then(modifier)
                )
                Box(
                    modifier
                        .semantics {
                            testTag = "child3"
                            contentDescription = "test"
                        }
                        .alpha(0f)
                )
            }
        }
        val parentId = rule.onNodeWithTag("parent").semanticsId()
        val child1Id = rule.onNodeWithTag("child1").semanticsId()
        val child2Id = rule.onNodeWithTag("child2").semanticsId()
        val child3Id = rule.onNodeWithTag("child3").semanticsId()

        // Act.
        rule.waitForIdle()
        val parent = createAccessibilityNodeInfo(parentId)
        val child1 = createAccessibilityNodeInfo(child1Id)
        val child2 = createAccessibilityNodeInfo(child2Id)
        val child3 = createAccessibilityNodeInfo(child3Id)

        // Assert.
        rule.runOnIdle {
            assertThat(parent.childCount).isEqualTo(3)
            assertThat(child1.isVisibleToUser).isTrue()
            assertThat(child2.isVisibleToUser).isTrue()
            assertThat(child3.isVisibleToUser).isTrue()
        }
    }

    @Test
    fun testTransparentNode_withAlphaAndClickableModifiers_notAccessible() {
        // Arrange.
        setContent {
            Column(Modifier.testTag("parent")) {
                Box(
                    modifier =
                        Modifier.alpha(0f).clickable(onClick = {}).semantics {
                            testTag = "child"
                            contentDescription = "Test"
                        }
                )
            }
        }
        val parentId = rule.onNodeWithTag("parent").semanticsId()
        val childId = rule.onNodeWithTag("child").semanticsId()

        // Act.
        rule.waitForIdle()
        val parent = createAccessibilityNodeInfo(parentId)
        val child = createAccessibilityNodeInfo(childId)

        // Assert.
        rule.runOnIdle {
            assertThat(parent.childCount).isEqualTo(1)
            assertThat(child.isVisibleToUser).isFalse()
        }
    }

    @Test
    fun testTransparentNode_withAlphaAndMultipleClickableModifiers_accessible() {
        // Arrange.
        setContent {
            Column(Modifier.testTag("parent")) {
                Box(
                    modifier =
                        Modifier.clickable(onClick = {})
                            .alpha(0f)
                            .clickable(onClick = {})
                            .semantics {
                                testTag = "child"
                                contentDescription = "Test"
                            }
                )
            }
        }
        val parentId = rule.onNodeWithTag("parent").semanticsId()
        val childId = rule.onNodeWithTag("child").semanticsId()

        // Act.
        rule.waitForIdle()
        val parent = createAccessibilityNodeInfo(parentId)
        val child = createAccessibilityNodeInfo(childId)

        // Assert.
        rule.runOnIdle {
            assertThat(parent.childCount).isEqualTo(1)
            assertThat(child.isVisibleToUser).isTrue()
        }
    }

    @Test
    fun testTransparentNode_withMultipleAlphaAndNestedClickableNodes_notAccessible() {
        // Arrange.
        setContent {
            Column(Modifier.testTag("parent")) {
                Box(
                    Modifier.alpha(1f).alpha(0f).alpha(1f).clickable(onClick = {}).semantics {
                        testTag = "child1"
                        contentDescription = "test"
                    }
                ) {
                    Box(
                        Modifier.clickable(onClick = {}).semantics {
                            testTag = "child2"
                            contentDescription = "test"
                        }
                    )
                }
            }
        }
        val parentId = rule.onNodeWithTag("parent").semanticsId()
        val child1Id = rule.onNodeWithTag("child1").semanticsId()
        val child2Id = rule.onNodeWithTag("child2").semanticsId()

        // Act.
        rule.waitForIdle()
        val parent = createAccessibilityNodeInfo(parentId)
        val child1 = createAccessibilityNodeInfo(child1Id)
        val child2 = createAccessibilityNodeInfo(child2Id)

        // Assert.
        rule.runOnIdle {
            assertThat(parent.childCount).isEqualTo(1)
            assertThat(child1.isVisibleToUser).isFalse()
            assertThat(child2.isVisibleToUser).isFalse()
        }
    }

    @Test
    fun testNonTransparentNode_withMultipleAlphaAndNestedClickableNodes_parentAccessible() {
        // Arrange.
        setContent {
            Column(Modifier.testTag("parent")) {
                Box(
                    Modifier.alpha(1f).clickable(onClick = {}).alpha(0f).alpha(1f).semantics {
                        testTag = "child1"
                        contentDescription = "test"
                    }
                ) {
                    Box(
                        Modifier.clickable(onClick = {}).semantics {
                            testTag = "child2"
                            contentDescription = "test"
                        }
                    )
                }
            }
        }
        val parentId = rule.onNodeWithTag("parent").semanticsId()
        val child1Id = rule.onNodeWithTag("child1").semanticsId()
        val child2Id = rule.onNodeWithTag("child2").semanticsId()

        // Act.
        rule.waitForIdle()
        val parent = createAccessibilityNodeInfo(parentId)
        val child1 = createAccessibilityNodeInfo(child1Id)
        val child2 = createAccessibilityNodeInfo(child2Id)

        // Assert.
        rule.runOnIdle {
            assertThat(parent.childCount).isEqualTo(1)
            assertThat(child1.isVisibleToUser).isTrue()
            assertThat(child2.isVisibleToUser).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun progressSemantics_mergesSemantics_forTalkback() {
        // Arrange.
        setContent {
            Box(Modifier.progressSemantics(0.5f).testTag("box")) {
                BasicText("test", Modifier.testTag("child"))
            }
        }
        val boxId = rule.onNodeWithTag("box", useUnmergedTree = true).semanticsId()
        val textId = rule.onNodeWithTag("child", useUnmergedTree = true).semanticsId()

        // Act.
        rule.waitForIdle()
        val info = createAccessibilityNodeInfo(boxId)
        val childInfo = createAccessibilityNodeInfo(textId)

        // Assert.
        rule.runOnIdle {
            assertThat(info.isScreenReaderFocusable).isTrue()
            assertThat(childInfo.isScreenReaderFocusable).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun indeterminateProgressSemantics_mergesSemantics_forTalkback() {
        // Arrange.
        setContent {
            Box(Modifier.progressSemantics().testTag("box")) {
                BasicText("test", Modifier.testTag("child"))
            }
        }
        val boxId = rule.onNodeWithTag("box", useUnmergedTree = true).semanticsId()
        val textId = rule.onNodeWithTag("child", useUnmergedTree = true).semanticsId()

        // Act.
        rule.waitForIdle()
        val info = createAccessibilityNodeInfo(boxId)
        val childInfo = createAccessibilityNodeInfo(textId)

        // Assert.
        rule.runOnIdle {
            assertThat(info.isScreenReaderFocusable).isTrue()
            assertThat(childInfo.isScreenReaderFocusable).isFalse()
        }
    }

    @Test
    fun accessibilityStateChangeListenerRemoved_onDetach() {
        // Arrange.
        delegate.accessibilityForceEnabledForTesting = false
        rule.runOnIdle { assertThat(androidComposeView.isAttachedToWindow).isTrue() }

        // Act.
        rule.runOnUiThread { container.removeView(androidComposeView) }

        // Assert.
        rule.runOnIdle {
            // This test implies that the listener was removed
            // and the enabled services were set to empty.
            assertThat(androidComposeView.isAttachedToWindow).isFalse()
            assertThat(delegate.isEnabled).isFalse()
        }
    }

    @Test
    fun touchExplorationChangeListenerRemoved_onDetach() {
        // Arrange.
        delegate.accessibilityForceEnabledForTesting = false
        rule.runOnIdle { assertThat(androidComposeView.isAttachedToWindow).isTrue() }

        // Act.
        rule.runOnUiThread { container.removeView(androidComposeView) }

        // Assert.
        rule.runOnIdle {
            // This test implies that the listener was removed
            // and the enabled services were set to empty.
            assertThat(androidComposeView.isAttachedToWindow).isFalse()
            assertThat(delegate.isEnabled).isFalse()
        }
    }

    @Test
    fun isEnabled_returnsFalse_whenUIAutomatorIsTheOnlyEnabledService() {
        // Arrange.
        delegate.accessibilityForceEnabledForTesting = false

        // Assert.
        rule.runOnIdle {
            // This test implies that UIAutomator is enabled and is the only enabled a11y service
            assertThat(accessibilityManager.isEnabled).isTrue()
            assertThat(delegate.isEnabled).isFalse()
        }
    }

    @Test
    fun canScroll_returnsFalse_whenAccessedOutsideOfMainThread() {
        setContent {
            Box(Modifier.semantics(mergeDescendants = true) {}) {
                Column(Modifier.size(50.dp).verticalScroll(rememberScrollState())) {
                    repeat(10) { Box(Modifier.size(30.dp)) }
                }
            }
        }

        rule.runOnIdle {
            androidComposeView.dispatchTouchEvent(
                createHoverMotionEvent(MotionEvent.ACTION_DOWN, 10f, 10f)
            )
            assertThat(androidComposeView.canScrollVertically(1)).isTrue()
        }

        assertThat(androidComposeView.canScrollVertically(1)).isFalse()
    }

    private fun getAccessibilityNodeInfoSourceSemanticsNodeId(
        node: AccessibilityNodeInfoCompat
    ): Int =
        Class.forName("android.view.accessibility.AccessibilityNodeInfo")
            .getDeclaredMethod("getSourceNodeId")
            .run {
                isAccessible = true
                invoke(node.unwrap()) as Long shr 32
            }
            .toInt()

    private fun getAccessibilityEventSourceSemanticsNodeId(
        event: android.view.accessibility.AccessibilityEvent
    ): Int =
        Class.forName("android.view.accessibility.AccessibilityRecord")
            .getDeclaredMethod("getSourceNodeId")
            .run {
                isAccessible = true
                invoke(event) as Long shr 32
            }
            .toInt()

    private fun createMovementGranularityCharacterArgs(): Bundle {
        return Bundle().apply {
            this.putInt(
                AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                MOVEMENT_GRANULARITY_CHARACTER,
            )
            this.putBoolean(
                AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                false,
            )
        }
    }

    private fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfoCompat {
        return checkNotNull(provider.createAccessibilityNodeInfo(virtualViewId)) {
            "Could not find view with id = $virtualViewId"
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        container.setContent(content)
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)
        clearInvocations(container)
    }
}

/**
 * A simple test layout that does the bare minimum required to lay out an arbitrary number of
 * children reasonably. Useful for Semantics hierarchy testing
 */
@Composable
private fun SimpleTestLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        if (measurables.isEmpty()) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeables = measurables.map { it.measure(constraints) }
            val (width, height) =
                with(placeables) {
                    Pair(
                        max(maxByOrNull { it.width }?.width ?: 0, constraints.minWidth),
                        max(maxByOrNull { it.height }?.height ?: 0, constraints.minHeight),
                    )
                }
            layout(width, height) {
                for (placeable in placeables) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

/**
 * A simple SubComposeLayout which lays [contentOne] at [positionOne] and lays [contentTwo] at
 * [positionTwo]. [contentOne] is placed first and [contentTwo] is placed second. Therefore, the
 * semantics node for [contentOne] is before semantics node for [contentTwo] in
 * [SemanticsNode.children].
 */
@Composable
private fun SimpleSubcomposeLayout(
    modifier: Modifier = Modifier,
    contentOne: @Composable () -> Unit,
    positionOne: Offset,
    contentTwo: @Composable () -> Unit,
    positionTwo: Offset,
) {
    SubcomposeLayout(modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copyMaxDimensions()

        layout(layoutWidth, layoutHeight) {
            val placeablesOne =
                subcompose(TestSlot.First, contentOne).fastMap { it.measure(looseConstraints) }

            val placeablesTwo =
                subcompose(TestSlot.Second, contentTwo).fastMap { it.measure(looseConstraints) }

            // Placing to control drawing order to match default elevation of each placeable
            placeablesOne.fastForEach { it.place(positionOne.x.toInt(), positionOne.y.toInt()) }
            placeablesTwo.fastForEach { it.place(positionTwo.x.toInt(), positionTwo.y.toInt()) }
        }
    }
}

/**
 * A simple layout which lays the first placeable in a top bar position, the last placeable in a
 * bottom bar position, and all the content in between.
 */
@Composable
fun ScaffoldedSubcomposeLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var yPosition = 0
    SubcomposeLayout(modifier) { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copyMaxDimensions()
        layout(layoutWidth, layoutHeight) {
            val topPlaceables =
                subcompose(ScaffoldedSlots.Top, topBar).fastMap { it.measure(looseConstraints) }

            val contentPlaceables =
                subcompose(ScaffoldedSlots.Content, content).fastMap {
                    it.measure(looseConstraints)
                }

            val bottomPlaceables =
                subcompose(ScaffoldedSlots.Bottom, bottomBar).fastMap {
                    it.measure(looseConstraints)
                }

            topPlaceables.fastForEach {
                it.place(0, yPosition)
                yPosition += it.height
            }
            contentPlaceables.fastForEach {
                it.place(0, yPosition)
                yPosition += it.height
            }
            bottomPlaceables.fastForEach {
                it.place(0, yPosition)
                yPosition += it.height
            }
        }
    }
}

private enum class TestSlot {
    First,
    Second,
}

private enum class ScaffoldedSlots {
    Top,
    Content,
    Bottom,
}

// TODO(b/304359126): Move this to AccessibilityEventCompat and use it wherever we use obtain().
private fun AccessibilityEvent(): android.view.accessibility.AccessibilityEvent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.accessibility.AccessibilityEvent()
        } else {
            @Suppress("DEPRECATION") android.view.accessibility.AccessibilityEvent.obtain()
        }
        .apply {
            packageName = "androidx.compose.ui.tests"
            className = "android.view.View"
            isEnabled = true
        }
}

const val CustomIntAccessibilityExtraKey =
    "androidx.compose.ui.semantics.customIntAccessibilityExtra"
val CustomIntAccessibilityExtra =
    SemanticsPropertyKey<Int>("CustomIntAccessibilityExtra", CustomIntAccessibilityExtraKey)
var SemanticsPropertyReceiver.customIntAccessibilityExtra by CustomIntAccessibilityExtra

const val CustomStringAccessibilityExtraKey =
    "androidx.compose.ui.semantics.customStringAccessibilityExtra"
val CustomStringAccessibilityExtra =
    SemanticsPropertyKey<String>(
        "CustomStringAccessibilityExtra",
        CustomStringAccessibilityExtraKey,
    )
var SemanticsPropertyReceiver.customStringAccessibilityExtra by CustomStringAccessibilityExtra

const val CustomParcelableAccessibilityExtraKey =
    "androidx.compose.ui.semantics.customParcelableAccessibilityExtra"
val CustomParcelableAccessibilityExtra =
    SemanticsPropertyKey<Parcelable>(
        "CustomParcelableAccessibilityExtra",
        CustomParcelableAccessibilityExtraKey,
    )
var SemanticsPropertyReceiver.customParcelableAccessibilityExtra by
    CustomParcelableAccessibilityExtra

const val CustomSerializableAccessibilityExtraKey =
    "androidx.compose.ui.semantics.customSerializableAccessibilityExtra"
val CustomSerializableAccessibilityExtra =
    SemanticsPropertyKey<Serializable>(
        "CustomSerializableAccessibilityExtra",
        CustomSerializableAccessibilityExtraKey,
    )
var SemanticsPropertyReceiver.customSerializableAccessibilityExtra by
    CustomSerializableAccessibilityExtra

data class InvalidAccessibilityExtraValue(val value: Int)

const val InvalidAccessibilityExtraKey = "androidx.compose.ui.semantics.invalidAccessibilityExtra"
val InvalidAccessibilityExtra =
    SemanticsPropertyKey<InvalidAccessibilityExtraValue>(
        "InvalidAccessibilityExtra",
        InvalidAccessibilityExtraKey,
    )
var SemanticsPropertyReceiver.invalidAccessibilityExtra by InvalidAccessibilityExtra
