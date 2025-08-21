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

package androidx.compose.foundation.text.input

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputDevice.SOURCE_DPAD
import android.view.InputDevice.SOURCE_KEYBOARD
import android.view.KeyEvent
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.DefaultCursorThickness
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.internal.selection.assertThatRect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.getFocusedRect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
internal class TextFieldFocusTest {
    @get:Rule val rule = createComposeRule()
    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val testKeyboardController = TestSoftwareKeyboardController(rule)

    @Composable
    private fun TextFieldApp(dataList: List<FocusTestData>) {
        for (data in dataList) {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier =
                    Modifier.focusRequester(data.focusRequester)
                        .onFocusChanged { data.focused = it.isFocused }
                        .requiredWidth(10.dp),
            )
        }
    }

    data class FocusTestData(val focusRequester: FocusRequester, var focused: Boolean = false)

    @Test
    fun requestFocus() {
        lateinit var testDataList: List<FocusTestData>

        rule.setContent {
            testDataList =
                listOf(
                    FocusTestData(FocusRequester()),
                    FocusTestData(FocusRequester()),
                    FocusTestData(FocusRequester()),
                )

            TextFieldApp(testDataList)
        }

        rule.runOnIdle { testDataList[0].focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(testDataList[0].focused).isTrue()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isFalse()
        }

        rule.runOnIdle { testDataList[1].focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isTrue()
            assertThat(testDataList[2].focused).isFalse()
        }

        rule.runOnIdle { testDataList[2].focusRequester.requestFocus() }
        rule.runOnIdle {
            assertThat(testDataList[0].focused).isFalse()
            assertThat(testDataList[1].focused).isFalse()
            assertThat(testDataList[2].focused).isTrue()
        }
    }

    @Test
    fun interactionSource_emitsFocusEvents() {
        val interactionSource = MutableInteractionSource()
        val tag = "TextField"
        lateinit var scope: CoroutineScope
        lateinit var focusManager: FocusManager

        rule.setContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(tag),
                interactionSource = interactionSource,
            )
            Box(modifier = Modifier.size(10.dp).focusable())
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag(tag).requestFocus()

        rule.waitForIdle()
        assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).isNotEmpty()
        val focusEvent = interactions.filterIsInstance<FocusInteraction.Focus>().first()

        rule.runOnUiThread { focusManager.moveFocus(FocusDirection.Next) }

        rule.waitForIdle()
        assertThat(interactions.filterIsInstance<FocusInteraction.Unfocus>()).hasSize(1)
        assertThat(interactions.filterIsInstance<FocusInteraction.Unfocus>().first().focus)
            .isSameInstanceAs(focusEvent)
    }

    @Test
    fun interactionSource_emitsUnfocusEvent_whenDisabled() {
        val interactionSource = MutableInteractionSource()
        val tag = "TextField"
        var enabled by mutableStateOf(true)
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(tag),
                interactionSource = interactionSource,
                enabled = enabled,
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag(tag).requestFocus()

        rule.waitForIdle()
        assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).isNotEmpty()
        val focusEvent = interactions.filterIsInstance<FocusInteraction.Focus>().first()

        enabled = false

        rule.waitForIdle()
        assertThat(interactions.filterIsInstance<FocusInteraction.Unfocus>()).hasSize(1)
        assertThat(interactions.filterIsInstance<FocusInteraction.Unfocus>().first().focus)
            .isSameInstanceAs(focusEvent)
    }

    @Test
    fun interactionSource_focusEventsAreSentTo_newInteractionSource() {
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        val interactionSourceState = mutableStateOf(interactionSource1)
        val tag = "TextField"
        lateinit var scope: CoroutineScope
        lateinit var focusManager: FocusManager

        rule.setContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(tag),
                interactionSource = interactionSourceState.value,
            )
        }

        val interactions1 = mutableListOf<Interaction>()
        val interactions2 = mutableListOf<Interaction>()

        scope.launch { interactionSource1.interactions.collect { interactions1.add(it) } }
        scope.launch { interactionSource2.interactions.collect { interactions2.add(it) } }

        rule.onNodeWithTag(tag).requestFocus()

        rule.waitForIdle()
        assertThat(interactions1.filterIsInstance<FocusInteraction.Focus>()).isNotEmpty()
        assertThat(interactions2.filterIsInstance<FocusInteraction.Focus>()).isEmpty()
        val focusEvent = interactions1.filterIsInstance<FocusInteraction.Focus>().first()

        interactionSourceState.value = interactionSource2

        rule.runOnIdle { focusManager.clearFocus() }

        rule.onNodeWithTag(tag).requestFocus()

        assertThat(interactions1.filterIsInstance<FocusInteraction.Unfocus>()).hasSize(1)
        assertThat(interactions1.filterIsInstance<FocusInteraction.Unfocus>().first().focus)
            .isSameInstanceAs(focusEvent)

        assertThat(interactions2.filterIsInstance<FocusInteraction.Focus>()).isNotEmpty()
    }

    @Test
    fun interactionSourceChanges_whenFieldDisabled_eventsAreSentToNewInteractionSource() {
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        val interactionSourceState = mutableStateOf(interactionSource1)
        val tag = "TextField"
        lateinit var scope: CoroutineScope
        var enabled by mutableStateOf(false)

        rule.setContent {
            scope = rememberCoroutineScope()
            BasicTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(tag),
                enabled = enabled,
                interactionSource = interactionSourceState.value,
            )
        }

        val interactions1 = mutableListOf<Interaction>()
        val interactions2 = mutableListOf<Interaction>()

        scope.launch { interactionSource1.interactions.collect { interactions1.add(it) } }
        scope.launch { interactionSource2.interactions.collect { interactions2.add(it) } }

        rule.runOnIdle { interactionSourceState.value = interactionSource2 }

        rule.runOnIdle { enabled = true }

        rule.onNodeWithTag(tag).requestFocus()

        assertThat(interactions1.filterIsInstance<FocusInteraction.Focus>()).isEmpty()
        assertThat(interactions2.filterIsInstance<FocusInteraction.Focus>()).isNotEmpty()
    }

    @Test
    fun noCrashWhenSwitchingBetweenEnabledFocusedAndDisabledTextField() {
        val enabled = mutableStateOf(true)
        var focused = false
        val tag = "textField"
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                enabled = enabled.value,
                modifier =
                    Modifier.testTag(tag)
                        .onFocusChanged { focused = it.isFocused }
                        .requiredWidth(10.dp),
            )
        }
        // bring enabled text field into focus
        rule.onNodeWithTag(tag).performClick()
        rule.runOnIdle { assertThat(focused).isTrue() }

        // make text field disabled
        enabled.value = false
        rule.runOnIdle { assertThat(focused).isFalse() }

        // make text field enabled again, it must not crash
        enabled.value = true
        rule.runOnIdle { assertThat(focused).isFalse() }
    }

    @Test
    fun focusBounds_cursor() {
        val state = TextFieldState()
        val node = FocusRectNode()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    modifier = Modifier.elementFor(node),
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle {
            assertThatRect(node.getFocusedRect())
                .isEqualToWithTolerance(
                    Rect(left = 0f, top = 0f, right = DefaultCursorThickness.value, bottom = 20f)
                )
        }
    }

    @Test
    fun focusBounds_selection() {
        val state = TextFieldState("abc", initialSelection = TextRange(1, 3))
        val node = FocusRectNode()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    modifier = Modifier.elementFor(node),
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle {
            assertThatRect(node.getFocusedRect())
                .isEqualToWithTolerance(Rect(left = 20f, top = 0f, right = 60f, bottom = 20f))
        }
    }

    @Test
    fun focusBounds_nonFocused() {
        //              |x| -> Box
        // | a            | -> TextField
        //              |x| -> Box
        // If we request a focus move from top box to down, TextField should gain focus since in its
        // unfocused state it should use its entire bounding box

        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    Modifier.width(10.toDp())
                        .height(10.toDp())
                        .focusable()
                        .focusRequester(focusRequester)
                )
                BasicTextField(
                    rememberTextFieldState("a"),
                    modifier = Modifier.width(100.toDp()).height(10.toDp()),
                )
                Box(Modifier.width(10.toDp()).height(10.toDp()).focusable())
            }
        }

        focusRequester.requestFocus()

        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Down) }

        rule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun focusBounds_afterEdit() {
        val state = TextFieldState()
        val focusedRects = mutableListOf<Rect?>()
        val node = FocusRectNode()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    modifier = Modifier.elementFor(node),
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle { focusedRects += node.getFocusedRect() }

        state.edit { append("a") }

        rule.runOnIdle {
            focusedRects += node.getFocusedRect()
            assertThat(focusedRects).hasSize(2)
            assertThatRect(focusedRects[0])
                .isEqualToWithTolerance(
                    Rect(left = 0f, top = 0f, right = DefaultCursorThickness.value, bottom = 20f)
                )
            assertThatRect(focusedRects[1])
                .isEqualToWithTolerance(
                    Rect(
                        left = 20f,
                        top = 0f,
                        right = 20f + DefaultCursorThickness.value,
                        bottom = 20f,
                    )
                )
        }
    }

    @Test
    fun focusBounds_whenFontSizeChanges_rectUpdates() {
        val state = TextFieldState("a", initialSelection = TextRange(1))
        val focusedRects = mutableListOf<Rect?>()
        val node = FocusRectNode()
        var fontSize by mutableStateOf(20.sp)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize),
                    modifier = Modifier.elementFor(node),
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle { focusedRects += node.getFocusedRect() }

        fontSize = 40.sp

        rule.runOnIdle {
            focusedRects += node.getFocusedRect()
            assertThat(focusedRects).hasSize(2)
            assertThatRect(focusedRects[0])
                .isEqualToWithTolerance(
                    Rect(
                        left = 20f,
                        top = 0f,
                        right = 20f + DefaultCursorThickness.value,
                        bottom = 20f,
                    )
                )
            assertThatRect(focusedRects[1])
                .isEqualToWithTolerance(
                    Rect(
                        left = 40f,
                        top = 0f,
                        right = 40f + DefaultCursorThickness.value,
                        bottom = 40f,
                    )
                )
        }
    }

    @Test
    fun focusBounds_multiLine() {
        val state = TextFieldState("a\nb", initialSelection = TextRange(3))
        val node = FocusRectNode()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    modifier = Modifier.elementFor(node),
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle {
            assertThatRect(node.getFocusedRect())
                .isEqualToWithTolerance(
                    Rect(
                        left = 20f,
                        top = 20f,
                        right = 20f + DefaultCursorThickness.value,
                        bottom = 40f,
                    )
                )
        }
    }

    @Test
    fun focusBounds_scrollable() {
        val state = TextFieldState("abc ".repeat(10), initialSelection = TextRange.Zero)
        val focusedRects = mutableListOf<Rect?>()
        val scrollState = ScrollState(0)
        val node = FocusRectNode()
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            coroutineScope = rememberCoroutineScope()
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.elementFor(node).width(100.dp),
                    scrollState = scrollState,
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle { focusedRects += node.getFocusedRect() }

        coroutineScope.launch { scrollState.scrollBy(100f) }

        rule.runOnIdle {
            focusedRects += node.getFocusedRect()
            assertThat(focusedRects).hasSize(2)
            assertThatRect(focusedRects[0]!!)
                .isEqualToWithTolerance(
                    Rect(left = 0f, top = 0f, right = DefaultCursorThickness.value, bottom = 20f)
                )
            assertThatRect(focusedRects[1]!!)
                .isEqualToWithTolerance(
                    Rect(
                        left = -100f,
                        top = 0f,
                        right = -100f + DefaultCursorThickness.value,
                        bottom = 20f,
                    )
                )
        }
    }

    @Test
    fun focusBounds_decorated() {
        val state = TextFieldState("abc ".repeat(10), initialSelection = TextRange.Zero)
        val focusedRects = mutableListOf<Rect?>()
        val node = FocusRectNode()
        var decorationPadding by mutableStateOf(PaddingValues(20.dp))

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BasicTextField(
                    state = state,
                    textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = 20.sp),
                    modifier = Modifier.elementFor(node),
                    decorator = { Box(Modifier.padding(decorationPadding)) { it() } },
                )
            }
        }

        rule.onNode(hasSetTextAction()).requestFocus()

        rule.runOnIdle { focusedRects += node.getFocusedRect() }

        decorationPadding = PaddingValues(40.dp)

        rule.runOnIdle {
            focusedRects += node.getFocusedRect()
            val normalRect =
                Rect(left = 0f, top = 0f, right = DefaultCursorThickness.value, bottom = 20f)
            assertThat(focusedRects).hasSize(2)
            assertThatRect(focusedRects[0])
                .isEqualToWithTolerance(normalRect.translate(Offset(20f, 20f)))
            assertThatRect(focusedRects[1])
                .isEqualToWithTolerance(normalRect.translate(Offset(40f, 40f)))
        }
    }

    @Test
    fun textInputStarted_forFieldInActivity_whenFocusRequestedImmediately_fromLaunchedEffect() {
        textInputStarted_whenFocusRequestedImmediately_fromEffect(
            runEffect = { LaunchedEffect(Unit) { it() } }
        )
    }

    @Test
    fun textInputStarted_forFieldInActivity_whenFocusRequestedImmediately_fromDisposableEffect() {
        textInputStarted_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            }
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun textInputStarted_forFieldInDialog_whenFocusRequestedImmediately_fromLaunchedEffect() {
        textInputStarted_whenFocusRequestedImmediately_fromEffect(
            runEffect = { LaunchedEffect(Unit) { it() } },
            wrapContent = {
                Dialog(onDismissRequest = {}) {
                    // Need to explicitly install the interceptor in the dialog as well.
                    inputMethodInterceptor.Content(it)
                }
            },
        )
    }

    // TODO(b/229378542) We can't accurately detect IME visibility from dialogs before API 30 so
    //  this test can't assert.
    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun textInputStarted_forFieldInDialog_whenFocusRequestedImmediately_fromDisposableEffect() {
        textInputStarted_whenFocusRequestedImmediately_fromEffect(
            runEffect = {
                DisposableEffect(Unit) {
                    it()
                    onDispose {}
                }
            },
            wrapContent = {
                Dialog(onDismissRequest = {}) {
                    // Need to explicitly install the interceptor in the dialog as well.
                    inputMethodInterceptor.Content(it)
                }
            },
        )
    }

    private fun textInputStarted_whenFocusRequestedImmediately_fromEffect(
        runEffect: @Composable (body: () -> Unit) -> Unit,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
    ) {
        val focusRequester = FocusRequester()
        val state = TextFieldState()

        inputMethodInterceptor.setContent {
            wrapContent {
                runEffect { focusRequester.requestFocus() }

                BasicTextField(state = state, modifier = Modifier.focusRequester(focusRequester))
            }
        }

        inputMethodInterceptor.assertSessionActive()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft_DPadDevice_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on left
        if (!keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_LEFT)) return

        // Check if the element to the left of text field gains focus
        rule.onNodeWithTag("test-button-left").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight_DPadDevice_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on right
        if (!keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_RIGHT)) return

        // Check if the element to the right of text field gains focus
        rule.onNodeWithTag("test-button-right").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp_DPadDevice_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on top
        if (!keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_UP)) return

        // Check if the element on the left of text field gains focus
        // due to the new way the focus is represented in BTF, the up would switch focus to the left
        rule.onNodeWithTag("test-button-left").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown_DPadDevice_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on bottom
        if (!keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_DOWN)) return

        // Check if the element to the bottom of text field gains focus
        rule.onNodeWithTag("test-button-bottom").assertIsFocused()
    }

    @Test
    fun basicTextField_checkKeyboardShown_onDPadCenter_DPadDevice_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        testKeyboardController.assertHidden()

        // Check if keyboard is enabled on Dpad center key press
        if (!keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_CENTER)) return
        testKeyboardController.assertShown()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft_hardwareKeyboard_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move the cursor to the left
        if (!keyPressOnKeyboardInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_LEFT)) return

        // Check if the element to the left of text field does not gain focus
        rule.onNodeWithTag("test-button-left").assertIsNotFocused()
        rule.onNodeWithTag("test-text-field-1").assertIsFocused()

        // Check if the cursor has actually moved to the left -> "ab|c"
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(2))
    }

    @FlakyTest(bugId = 348380475)
    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight_hardwareKeyboard_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()
        // Carry the cursor to the start after typing -> "|abc"
        rule.onNodeWithTag("test-text-field-1").performTextInputSelection(TextRange.Zero)

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move the cursor to the left
        if (!keyPressOnKeyboardInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_RIGHT)) return

        // Check if the element to the right of text field does not gain focus
        rule.onNodeWithTag("test-button-right").assertIsNotFocused()
        rule.onNodeWithTag("test-text-field-1").assertIsFocused()

        // Check if the cursor has actually moved to the right -> "a|bc"
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(1))
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp_hardwareKeyboard_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on top
        if (!keyPressOnKeyboardInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_UP)) return

        // Check if the element on the top of text field does not gain focus
        rule.onNodeWithTag("test-button-top").assertIsNotFocused()
        rule.onNodeWithTag("test-text-field-1").assertIsFocused()

        // Check if the cursor has actually moved up -> "a\nb|\nc"
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(3))
    }

    @FlakyTest(bugId = 348380475)
    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown_hardwareKeyboard_beforeFix() {
        Assume.assumeFalse(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()
        // Carry the cursor to the start after typing -> "|a\nb\nc"
        rule.onNodeWithTag("test-text-field-1").performTextInputSelection(TextRange.Zero)

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on bottom
        if (!keyPressOnKeyboardInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_DOWN)) return

        // Check if the element to the bottom of text field does not gain focus
        rule.onNodeWithTag("test-button-bottom").assertIsNotFocused()
        rule.onNodeWithTag("test-text-field-1").assertIsFocused()

        // Check if the cursor has actually moved down -> "a\n|b\nc"
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(2))
    }

    @Test
    fun basicTextField_checkKeyboardShown_onDPadCenter_DPadDevice_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)
        testKeyboardController.assertHidden()

        // Check if keyboard is enabled on Dpad center key press
        if (!keyPressOnDpadInputDevice(rule, NativeKeyEvent.KEYCODE_DPAD_CENTER)) return
        testKeyboardController.assertShown()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft_DPadDevice_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationLeft(SOURCE_DPAD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight_DPadDevice_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationRight(SOURCE_DPAD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp_DPadDevice_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationUp(SOURCE_DPAD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown_DPadDevice_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationDown(SOURCE_DPAD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft_hardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationLeft(SOURCE_KEYBOARD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight_hardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationRight(SOURCE_KEYBOARD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp_hardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationUp(SOURCE_KEYBOARD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown_hardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationDown(SOURCE_DPAD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadLeft_DpadHardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationLeft(SOURCE_DPAD or SOURCE_KEYBOARD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadRight_DpadHardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationRight(SOURCE_DPAD or SOURCE_KEYBOARD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadUp_DpadHardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationUp(SOURCE_DPAD or SOURCE_KEYBOARD)
    }

    @Test
    fun basicTextField_checkFocusNavigation_onDPadDown_DpadHardwareKeyboard_afterFix() {
        Assume.assumeTrue(ComposeFoundationFlags.isTextFieldDpadNavigationEnabled)
        checkFocusNavigationDown(SOURCE_DPAD or SOURCE_KEYBOARD)
    }

    fun checkFocusNavigationLeft(source: Int) {
        fun pressLeft() =
            keyPressOnPhysicalDevice(rule, NativeKeyEvent.KEYCODE_DPAD_LEFT, source, 1)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        if (!pressLeft()) return

        rule.onNodeWithTag("test-text-field-1").assertIsFocused()
        // first move to the left
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(2))

        // Move the cursor to the left twice first. Then left arrow should move focus.
        repeat(3) {
            rule.onNodeWithTag("test-button-left").assertIsNotFocused()
            if (!pressLeft()) return
        }

        // Check if the element to the left of text field gains focus
        rule.onNodeWithTag("test-button-left").assertIsFocused()
    }

    fun checkFocusNavigationRight(source: Int) {
        fun pressRight() =
            keyPressOnPhysicalDevice(rule, NativeKeyEvent.KEYCODE_DPAD_RIGHT, source, 1)
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()
        // move the selection to the beginning
        rule.onNodeWithTag("test-text-field-1").performTextInputSelection(TextRange(0))

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        if (!pressRight()) return

        rule.onNodeWithTag("test-text-field-1").assertIsFocused()
        // first move to the right
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(1))

        // Move the cursor to the right twice first. Then right arrow should move focus.
        repeat(3) {
            rule.onNodeWithTag("test-button-right").assertIsNotFocused()
            if (!pressRight()) return
        }

        // Check if the element to the right of text field gains focus
        rule.onNodeWithTag("test-button-right").assertIsFocused()
    }

    fun checkFocusNavigationUp(source: Int) {
        fun pressUp() = keyPressOnPhysicalDevice(rule, NativeKeyEvent.KEYCODE_DPAD_UP, source, 1)
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        // Move focus to the focusable element on top
        if (!pressUp()) return

        // Check if the element on the top of text field does not gain focus
        rule.onNodeWithTag("test-text-field-1").assertIsFocused()

        // Check if the cursor has actually moved up -> "a\nb|\nc"
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(3))

        // Move the cursor up twice first. Then up arrow should move focus.
        repeat(3) {
            rule.onNodeWithTag("test-button-top").assertIsNotFocused()
            if (!pressUp()) return
        }

        rule.onNodeWithTag("test-button-top").assertIsFocused()
    }

    fun checkFocusNavigationDown(source: Int) {
        fun pressDown() =
            keyPressOnPhysicalDevice(rule, NativeKeyEvent.KEYCODE_DPAD_DOWN, source, 1)
        setupAndEnableBasicTextField()
        inputMultilineTextInBasicTextField()
        // move the selection to the beginning
        rule.onNodeWithTag("test-text-field-1").performTextInputSelection(TextRange(0))

        // Dismiss keyboard on back press
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_BACK)

        if (!pressDown()) return

        rule.onNodeWithTag("test-text-field-1").assertIsFocused()
        // first move down
        rule.onNodeWithTag("test-text-field-1").assertSelection(TextRange(2))

        // Move the cursor down twice first. Then down arrow should move focus.
        repeat(3) {
            rule.onNodeWithTag("test-button-bottom").assertIsNotFocused()
            if (!pressDown()) return
        }

        // Check if the element to the bottom of text field gains focus
        rule.onNodeWithTag("test-button-bottom").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onTab() {
        setupAndEnableBasicTextField(singleLine = true)
        inputSingleLineTextInBasicTextField()

        // Move focus to the next focusable element via tab
        assertThat(keyPressOnKeyboardInputDevice(rule, NativeKeyEvent.KEYCODE_TAB)).isTrue()

        // Check if the element to the right of text field gains focus
        rule.onNodeWithTag("test-button-right").assertIsFocused()
    }

    @Test
    fun basicTextField_withImeActionNext_checkFocusNavigation_onEnter() {
        setupAndEnableBasicTextField(singleLine = true)
        inputSingleLineTextInBasicTextField()

        // Move focus to the next focusable element via IME action
        assertThat(keyPressOnKeyboardInputDevice(rule, NativeKeyEvent.KEYCODE_ENTER)).isTrue()

        // Check if the element to the right of text field gains focus
        rule.onNodeWithTag("test-button-right").assertIsFocused()
    }

    @Test
    fun basicTextField_checkFocusNavigation_onShiftTab() {
        setupAndEnableBasicTextField(singleLine = true)
        inputSingleLineTextInBasicTextField()

        // Move focus to the next focusable element via shift+tab
        assertThat(
                keyPressOnKeyboardInputDevice(
                    rule,
                    NativeKeyEvent.KEYCODE_TAB,
                    metaState = KeyEvent.META_SHIFT_ON,
                )
            )
            .isTrue()

        // Check if the element to the left of text field gains focus
        rule.onNodeWithTag("test-button-left").assertIsFocused()
    }

    @Test
    fun basicTextField_handlesInvalidDevice() {
        setupAndEnableBasicTextField()
        inputSingleLineTextInBasicTextField()

        // -2 shouldn't be a valid device – we verify this below by asserting the device in the
        // event is actually null.
        val invalidDeviceId = -2
        val keyCode = NativeKeyEvent.KEYCODE_DPAD_CENTER
        val keyEventDown =
            KeyEvent(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                0,
                invalidDeviceId,
                0,
            )
        assertThat(keyEventDown.device).isNull()
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventDown))
        rule.waitForIdle()
        val keyEventUp =
            KeyEvent(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                0,
                invalidDeviceId,
                0,
            )
        rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventUp))
        rule.waitForIdle()
    }

    private fun setupAndEnableBasicTextField(singleLine: Boolean = false) {
        setupContent(singleLine)

        rule.onNodeWithTag("test-text-field-1").assertIsFocused()
    }

    private fun inputSingleLineTextInBasicTextField() {
        // Input "abc"
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_A)
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_B)
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_C)
    }

    private fun inputMultilineTextInBasicTextField() {
        // Input "a\nb\nc"
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_A)
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_ENTER)
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_B)
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_ENTER)
        keyPressOnVirtualKeyboard(NativeKeyEvent.KEYCODE_C)
    }

    private fun setupContent(singleLine: Boolean = false) {
        rule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides testKeyboardController
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TestFocusableElement(id = "top")
                    Row {
                        TestFocusableElement(id = "left")
                        TestBasicTextField(id = "1", singleLine = singleLine, requestFocus = true)
                        TestFocusableElement(id = "right")
                    }
                    TestFocusableElement(id = "bottom")
                }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun TestFocusableElement(id: String) {
        var isFocused by remember { mutableStateOf(false) }
        BasicText(
            text = "test-button-$id",
            modifier =
                Modifier.testTag("test-button-$id")
                    .padding(10.dp)
                    .onFocusChanged { isFocused = it.hasFocus }
                    .focusable()
                    .border(2.dp, if (isFocused) Color.Green else Color.Cyan),
        )
    }

    @Composable
    private fun TestBasicTextField(id: String, singleLine: Boolean, requestFocus: Boolean = false) {
        val state = rememberTextFieldState()
        var isFocused by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val modifier = if (requestFocus) Modifier.focusRequester(focusRequester) else Modifier

        BasicTextField(
            state = state,
            lineLimits =
                if (singleLine) {
                    TextFieldLineLimits.SingleLine
                } else {
                    TextFieldLineLimits.Default
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier =
                modifier
                    .testTag("test-text-field-$id")
                    .padding(10.dp)
                    .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
                    .border(2.dp, if (isFocused) Color.Red else Color.Transparent),
        )

        LaunchedEffect(requestFocus, focusRequester) {
            if (requestFocus) focusRequester.requestFocus()
        }
    }

    /** Triggers a key press on the root node from a non-virtual dpad device (if supported). */
    private fun keyPressOnDpadInputDevice(
        rule: ComposeContentTestRule,
        keyCode: Int,
        count: Int = 1,
    ) = keyPressOnPhysicalDevice(rule, keyCode, InputDevice.SOURCE_DPAD, count)

    /** Triggers a key press on the root node from a non-virtual keyboard device (if supported). */
    private fun keyPressOnKeyboardInputDevice(
        rule: ComposeContentTestRule,
        keyCode: Int,
        count: Int = 1,
        metaState: Int = 0,
    ): Boolean = keyPressOnPhysicalDevice(rule, keyCode, SOURCE_KEYBOARD, count, metaState)

    private fun keyPressOnPhysicalDevice(
        rule: ComposeContentTestRule,
        keyCode: Int,
        source: Int,
        count: Int = 1,
        metaState: Int = 0,
    ): Boolean {
        val deviceId =
            InputDevice.getDeviceIds().firstOrNull { id ->
                InputDevice.getDevice(id)?.isVirtual?.not() ?: false &&
                    InputDevice.getDevice(id)?.supportsSource(source) ?: false
            } ?: return false
        val keyEventDown =
            KeyEvent(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                metaState,
                deviceId,
                0,
                0,
                source,
            )
        val keyEventUp =
            KeyEvent(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                metaState,
                deviceId,
                0,
                0,
                source,
            )

        repeat(count) {
            rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventDown))
            rule.waitForIdle()
            rule.onRoot().performKeyPress(androidx.compose.ui.input.key.KeyEvent(keyEventUp))
        }
        return true
    }

    /** Triggers a key press on the virtual keyboard. */
    private fun keyPressOnVirtualKeyboard(keyCode: Int, count: Int = 1) {
        rule.waitForIdle()
        repeat(count) { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode) }
        rule.waitForIdle()
    }

    private fun FocusRectNode() =
        object : DelegatingNode() {
            val focusNode = delegate(FocusTargetModifierNode(Focusability.Never))

            fun getFocusedRect() = focusNode.getFocusedRect()
        }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }
}

internal fun Modifier.elementFor(node: Modifier.Node): Modifier {
    return this then NodeElement(node)
}

internal data class NodeElement(val node: Modifier.Node) : ModifierNodeElement<Modifier.Node>() {
    override fun create(): Modifier.Node = node

    override fun update(node: Modifier.Node) {}
}
