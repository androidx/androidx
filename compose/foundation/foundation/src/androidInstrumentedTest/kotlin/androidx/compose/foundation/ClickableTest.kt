/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.first
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableTest {

    @get:Rule
    val rule = createComposeRule()

    private val InstanceOf = Correspondence.from<Any, KClass<*>>(
        { obj, clazz -> clazz?.isInstance(obj) ?: false },
        "is an instance of"
    )

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() = with(InstrumentationRegistry.getInstrumentation()) {
        if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
    }

    @Test
    fun defaultSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun disabledSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(enabled = false) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun semanticsInvalidation() {
        var enabled by mutableStateOf(true)
        var role by mutableStateOf<Role?>(Role.Button)
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable")
                        .clickable(enabled = enabled, role = role) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()

        rule.runOnIdle {
            role = null
        }

        rule.onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasClickAction()

        rule.runOnIdle {
            enabled = false
        }

        rule.onNodeWithTag("myClickable")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasClickAction()
    }

    @Test
    fun click() {
        var counter = 0
        val onClick: () -> Unit = {
            ++counter
        }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(onClick = onClick)
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    fun clickWithEnterKey() {
        var counter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .clickable { counter++ }
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    fun clickWithNumPadEnterKey() {
        var counter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier = Modifier
                    .testTag("myClickable")
                    .focusRequester(focusRequester)
                    .clickable { counter++ }
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(Key.NumPadEnter) }

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(Key.NumPadEnter) }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    fun clickWithDPadCenter() {
        var counter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier = Modifier
                    .testTag("myClickable")
                    .focusRequester(focusRequester)
                    .clickable { counter++ }
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(Key.DirectionCenter) }

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(Key.DirectionCenter) }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun clickOnChildBasicText() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box(modifier = Modifier.clickable(onClick = onClick)) {
                BasicText("Foo")
                BasicText("Bar")
            }
        }

        rule.onNodeWithText("Foo", substring = true).assertExists()
        rule.onNodeWithText("Bar", substring = true).assertExists()

        rule.onNodeWithText("Foo", substring = true).performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithText("Bar", substring = true).performClick()

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun requestFocus_touchMode() {
        // Arrange.
        val tag = "testClickable"
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier
                    .testTag(tag)
                    .size(10.dp)
                    .focusRequester(focusRequester)
                    .clickable {}
            )
        }
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            inputModeManager.requestInputMode(Touch)
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.onNodeWithTag(tag).assertIsNotFocused()
    }

    @Test
    fun requestFocus_keyboardMode() {
        // Arrange.
        val tag = "testClickable"
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier
                    .testTag(tag)
                    .size(10.dp)
                    .focusRequester(focusRequester)
                    .clickable {}
            )
        }
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            inputModeManager.requestInputMode(Keyboard)
        }

        // Act.
        rule.runOnIdle { focusRequester.requestFocus() }

        // Assert.
        rule.onNodeWithTag(tag).assertIsFocused()
    }

    @Test
    fun requestFocus_withTestApi_touchMode() {
        // Arrange.
        val tag = "testClickable"
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier
                    .testTag(tag)
                    .size(10.dp)
                    .clickable {}
            )
        }
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            inputModeManager.requestInputMode(Touch)
        }

        // Act.
        rule.onNodeWithTag(tag).requestFocus()

        // Assert.
        rule.onNodeWithTag(tag).assertIsNotFocused()
    }

    @Test
    fun requestFocus_withTestApi_keyboardMode() {
        // Arrange.
        val tag = "testClickable"
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier
                    .focusRequester(focusRequester)
                    .testTag(tag)
                    .size(10.dp)
                    .clickable {}
            )
        }
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            inputModeManager.requestInputMode(Keyboard)
        }

        // Act.
        rule.onNodeWithTag(tag).requestFocus()

        // Assert.
        rule.onNodeWithTag(tag).assertIsFocused()
    }

    @Test
    fun interactionSource_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        // No scrollable container, so there should be no delay and we should instantly appear
        // pressed
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateRelease_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(center)
                up()
            }

        // Press finished so we should see both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateCancel_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(center)
                cancel()
            }

        // We are not in a scrollable container, so we should see a press and immediate cancel
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateDrag_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .draggable(
                            state = rememberDraggableState {},
                            orientation = Orientation.Horizontal
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(centerLeft)
                moveTo(centerRight)
            }

        // The press should fire, and then the drag should instantly cancel it
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateRelease_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(center)
                up()
            }

        // We haven't reached the tap delay, but we have finished a press so we should have
        // emitted both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateCancel_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(center)
                cancel()
            }

        // We haven't reached the tap delay, and a cancel was emitted, so no press should ever be
        // shown
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    fun interactionSource_immediateDrag_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .draggable(
                            state = rememberDraggableState {},
                            orientation = Orientation.Horizontal
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(centerLeft)
                moveTo(centerRight)
            }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    fun interactionSource_dragAfterTimeout_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .draggable(
                            state = rememberDraggableState {},
                            orientation = Orientation.Horizontal
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                down(centerLeft)
            }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                moveTo(centerRight)
            }

        // The drag should cancel the press
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_cancelledGesture_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { cancel() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitClickableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitClickableText) {
                    BasicText(
                        "ClickableText",
                        modifier = Modifier
                            .testTag("myClickable")
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {}
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose clickable
        rule.runOnIdle {
            emitClickableText = false
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun interactionSource_hover() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1])
                .isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun interactionSource_hover_and_press() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput {
                enter(center)
                click()
                exit(Offset(-1f, -1f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions[0]).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[3]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
            assertThat((interactions[3] as HoverInteraction.Exit).enter)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_focus_inTouchMode() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            inputModeManager.requestInputMode(Touch)
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Touch mode by default, so we shouldn't be focused
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    fun interactionSource_focus_inKeyboardMode() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        lateinit var inputModeManager: InputModeManager
        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .clickable(interactionSource = interactionSource, indication = null) {}
                )
            }
        }
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            inputModeManager.requestInputMode(Keyboard)
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        // Keyboard mode, so we should now be focused and see an interaction
        rule.runOnIdle {
            assertThat(interactions)
                .comparingElementsUsing(InstanceOf)
                .containsExactly(FocusInteraction.Focus::class)
        }

        rule.runOnIdle {
            focusManager.clearFocus()
        }

        rule.runOnIdle {
            // TODO(b/308811852): Simplify the other assertions in FocusableTest, ClickableTest and
            //  CombinedClickable by using InstanceOf (like we do here).
            assertThat(interactions)
                .comparingElementsUsing(InstanceOf)
                .containsExactly(FocusInteraction.Focus::class, FocusInteraction.Unfocus::class)
                .inOrder()
        }
    }

    @Test
    @LargeTest
    fun click_consumedWhenDisabled() {
        val enabled = mutableStateOf(false)
        var clickCounter = 0
        var outerCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onOuterClick: () -> Unit = { ++outerCounter }

        rule.setContent {
            Box(Modifier.clickable(onClick = onOuterClick)) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .clickable(enabled = enabled.value, onClick = onClick)
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerCounter).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @LargeTest
    fun noHover_whenDisabled() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        val enabled = mutableStateOf(true)

        rule.setContent {
            scope = rememberCoroutineScope()
            BasicText(
            "ClickableText",
                modifier = Modifier
                    .testTag("myClickable")
                    .clickable(
                        enabled = enabled.value,
                        onClick = {},
                        interactionSource = interactionSource,
                        indication = null
                    )
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            interactions.clear()
            enabled.value = false
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performMouseInput { exit(Offset(-1f, -1f)) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun noFocus_whenDisabled() {
        val requester = FocusRequester()
        // Force clickable to always be in non-touch mode, so it should be focusable
        val keyboardMockManager = object : InputModeManager {
            override val inputMode = Keyboard
            override fun requestInputMode(inputMode: InputMode) = true
        }

        val enabled = mutableStateOf(true)
        lateinit var focusState: FocusState

        rule.setContent {
            CompositionLocalProvider(LocalInputModeManager provides keyboardMockManager) {
                Box {
                    BasicText(
                        "ClickableText",
                        modifier = Modifier
                            .testTag("myClickable")
                            .focusRequester(requester)
                            .onFocusEvent { focusState = it }
                            .clickable(enabled = enabled.value) {}
                    )
                }
            }
        }

        rule.runOnIdle {
            requester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        rule.runOnIdle {
            enabled.value = false
        }

        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    /**
     * Test for b/269319898
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun noFocusPropertiesSet_whenDisabled() {
        val requester = FocusRequester()
        // Force clickable to always be in non-touch mode, so it should be focusable
        val keyboardMockManager = object : InputModeManager {
            override val inputMode = Keyboard
            override fun requestInputMode(inputMode: InputMode) = true
        }

        val enabled = mutableStateOf(true)
        lateinit var focusState: FocusState

        rule.setContent {
            CompositionLocalProvider(LocalInputModeManager provides keyboardMockManager) {
                Box(Modifier.clickable(enabled = enabled.value, onClick = {})) {
                    Box(
                        Modifier
                            .size(10.dp)
                            // If clickable is setting canFocus to true without a focus target, then
                            // that would override this property
                            .focusProperties { canFocus = false }
                            .focusRequester(requester)
                            .onFocusEvent { focusState = it }
                            .focusable()
                    )
                }
            }
        }

        // b/314129026 we can't read canFocus, so instead try and request focus and make sure
        // that we are not focused
        rule.runOnIdle {
            // Clickable is enabled, it should correctly apply properties to its focus node
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }

        rule.runOnIdle {
            enabled.value = false
        }

        rule.runOnIdle {
            // Clickable is disabled, it should not apply properties down the tree
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun testInspectorValue_noIndicationOverload() {
        val onClick: () -> Unit = { }
        rule.setContent {
            val modifier = Modifier.clickable(onClick = onClick) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("clickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "role",
                "onClick"
            )
        }
    }

    @Test
    fun testInspectorValue_fullParamsOverload() {
        val onClick: () -> Unit = { }
        rule.setContent {
            val modifier = Modifier.clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("clickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "onClick",
                "role",
                "indicationNodeFactory",
                "interactionSource"
            )
        }
    }

    // integration test for b/184872415
    @Test
    fun tapGestureTest_tryAwaitRelease_ReturnsTrue() {
        val wasSuccess = mutableStateOf(false)
        rule.setContent {
            Box(
                Modifier
                    .size(100.dp)
                    .testTag("myClickable")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                wasSuccess.value = tryAwaitRelease()
                            }
                        )
                    }
            )
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        assertThat(wasSuccess.value).isTrue()
    }

    @Test
    fun clickInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier
                    .requiredHeight(20.dp)
                    .requiredWidth(20.dp)
                    .clipToBounds()
                    .clickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performTouchInput {
                click(Offset(-1f, -1f))
            }

        rule.runOnIdle {
            assertThat(clicked).isTrue()
        }
    }

    @Test
    fun clickInVerticalTargetInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier
                    .requiredHeight(50.dp)
                    .requiredWidth(20.dp)
                    .clipToBounds()
                    .clickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(50.dp)
            .assertTouchHeightIsEqualTo(50.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performTouchInput {
                click(Offset(-1f, 0f))
            }

        rule.runOnIdle {
            assertThat(clicked).isTrue()
        }
    }

    @Test
    fun clickInHorizontalTargetInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier
                    .requiredHeight(20.dp)
                    .requiredWidth(50.dp)
                    .clipToBounds()
                    .clickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(50.dp)
            .performTouchInput {
                click(Offset(0f, -1f))
            }

        rule.runOnIdle {
            assertThat(clicked).isTrue()
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun enterKey_emitsInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun numPadEnterKey_emitsInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyDown(Key.NumPadEnter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(Key.NumPadEnter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun dpadCenter_emitsInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }
        rule.waitForIdle()

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyDown(Key.DirectionCenter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(Key.DirectionCenter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun otherKey_doesNotEmitInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.onNodeWithTag("clickable").performKeyInput { pressKey(Key.Spacebar) }
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun doubleEnterKey_emitsFurtherInteractions() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        val clickableNode = rule.onNodeWithTag("clickable")

        clickableNode.performKeyInput { pressKey(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
        }

        clickableNode.performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
        }

        clickableNode.performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun repeatKeyEvents_doNotEmitFurtherInteractions() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        var repeatCounter = 0
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .onKeyEvent {
                            if (it.nativeKeyEvent.repeatCount != 0)
                                repeatCounter++
                            false
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.onNodeWithTag("clickable").performKeyInput {
            keyDown(Key.Enter)

            advanceEventTime(500) // First repeat
            advanceEventTime(50) // Second repeat
        }

        rule.runOnIdle {
            // Ensure that expected number of repeats occurred and did not cause press interactions.
            assertThat(repeatCounter).isEqualTo(2)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalTestApi::class)
    fun interruptedClick_emitsCancelInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        val enabled = mutableStateOf(true)
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled.value
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        val clickableNode = rule.onNodeWithTag("clickable")

        clickableNode.performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        enabled.value = false

        clickableNode.assertIsNotEnabled()

        rule.runOnIdle {
            // Filter out focus interactions.
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }

        // Key releases should not result in interactions.
        clickableNode.performKeyInput { keyUp(Key.Enter) }

        // Make sure nothing has changed.
        rule.runOnIdle {
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }
    }

    @Test
    fun indication_interactionSource_eagerlyCreated() {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndication { created = true }
        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = indication
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            assertThat(created).isTrue()
        }
    }

    @Test
    fun indicationNodeFactory_interactionSource_eagerlyCreated() {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = indication
                        ) {}
                )
            }
        }
        rule.runOnIdle {
            assertThat(created).isTrue()
        }
    }

    // Indication (not IndicationNodeFactory) is always eagerly created
    @Test
    fun indication_noInteractionSource_eagerlyCreated() {
        var created = false
        lateinit var interactionSource: InteractionSource
        lateinit var scope: CoroutineScope
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndication {
            interactionSource = it
            created = true
            scope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun indicationNodeFactory_noInteractionSource_lazilyCreated_pointerInput() {
        var created = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(created).isFalse()
        }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun indicationNodeFactory_noInteractionSource_lazilyCreated_focus() {
        var created = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .focusRequester(focusRequester)
                        .clickable(
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(created).isFalse()
        }

        rule.runOnIdle {
            // Clickable is only focusable in non-touch mode
            inputModeManager.requestInputMode(Keyboard)
            // The focus event should cause the indication node to be created
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }
    }

    /**
     * Test case for initializing indication when a KeyEvent is received. Focus is required for key
     * events, so normally just focusing the clickable will cause indication to be initialized via
     * focus logic, but if a focused child receives a key event and doesn't consume it, it will
     * still be passed up to a non-focused parent, so we test this scenario here and make sure that
     * this key event bubbling up causes indication to be created.
     */
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
    @Test
    fun indicationNodeFactory_noInteractionSource_lazilyCreated_keyInput() {
        var created = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            // Add focusable to the top so that when initial focus is dispatched, the clickable
            // doesn't become focused
            Box(Modifier.padding(10.dp).focusable()) {
                Box(
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = null,
                            indication = indication
                        ) {}
                ) {
                    Box(Modifier.focusRequester(focusRequester).focusable())
                }
            }
        }

        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            // We are focusing the child, not the clickable, so we shouldn't create indication yet
            assertThat(created).isFalse()
        }

        // The key input event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performKeyInput { keyDown(Key.Enter) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for changing from an Indication instance to an IndicationNodeFactory instance with
     * a provided InteractionSource - the IndicationNodeFactory should be immediately created.
     */
    @Test
    fun indicationNodeFactory_changingIndicationToIndicationNodeFactory_interactionSource() {
        var indicationCreated = false
        var nodeCreated = false
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()
        val testIndication = TestIndication { indicationCreated = true }
        val testIndicationNodeFactory = TestIndicationNodeFactory { _, coroutineScope ->
            nodeCreated = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        var indication: Indication by mutableStateOf(testIndication)

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(indicationCreated).isTrue()
            indication = testIndicationNodeFactory
        }

        rule.runOnIdle {
            assertThat(nodeCreated).isTrue()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for null InteractionSource with a provided indication: the indication should be
     * lazily created. If we change indication before creation, the new indication should be created
     * lazily too.
     */
    @Test
    fun indicationNodeFactory_changingIndication_beforeCreation() {
        var created1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created1 = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                        interaction -> interactions.add(interaction)
                }
            }
        }

        var indication by mutableStateOf(indication1)

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(created1).isFalse()
        }

        rule.runOnIdle {
            indication = indication2
        }

        rule.runOnIdle {
            // We should still not be created
            assertThat(created2).isFalse()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created2).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for null InteractionSource with a provided indication: the indication should be
     * lazily created, but then if we change indication after creation, the new indication should
     * be created immediately
     */
    @Test
    fun indicationNodeFactory_changingIndication_afterCreation() {
        var created1 = false
        var detached1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 = TestIndicationNodeFactory(
            onDetach = { detached1 = true }
        ) { source, coroutineScope ->
            interactionSource = source
            created1 = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        var indication by mutableStateOf(indication1)

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = null,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(created1).isFalse()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created1).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { up() }

        rule.runOnIdle {
            interactions.clear()
            indication = indication2
        }

        rule.runOnIdle {
            // We should be created because we created the previous node already
            assertThat(created2).isTrue()
            // The previous node should be detached
            assertThat(detached1).isTrue()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for a provided InteractionSource with a provided indication, and changing the
     * InteractionSource to a new one. This should cause the indication to be recreated immediately.
     */
    @Test
    fun indicationNodeFactory_changingInteractionSourceToAnotherInteractionSource() {
        var created = false
        var detached = false
        var interactionSource: MutableInteractionSource by mutableStateOf(
            MutableInteractionSource()
        )
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory(
            onDetach = { detached = true }
        ) { _, coroutineScope ->
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            // We should be eagerly created
            assertThat(created).isTrue()
            interactionSource = MutableInteractionSource()
        }

        rule.runOnIdle {
            // Changing InteractionSource should cause the node to be detached, and a new one
            // created
            assertThat(detached).isTrue()
            assertThat(created).isTrue()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for null InteractionSource with a provided indication, and changing the
     * InteractionSource to non-null. This should cause the indication to be created immediately.
     */
    @Test
    fun indicationNodeFactory_changingInteractionSourceFromNull() {
        var created = false
        var interactionSource: MutableInteractionSource? by mutableStateOf(null)
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { _, coroutineScope ->
            created = true
            coroutineScope.launch {
                interactionSource!!.interactions.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            assertThat(created).isFalse()
            interactionSource = MutableInteractionSource()
        }

        rule.runOnIdle {
            // Changing InteractionSource should cause us to be created
            assertThat(created).isTrue()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for a provided InteractionSource with a provided indication, and changing the
     * InteractionSource to null. This should cause the indication to be recreated immediately.
     */
    @Test
    fun indicationNodeFactory_changingInteractionSourceToNull() {
        var created = false
        var detached = false
        var interactionSource: MutableInteractionSource? by mutableStateOf(
            MutableInteractionSource()
        )
        var internalInteractionSource: InteractionSource?
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory(
            onDetach = { detached = true }
        ) { source, coroutineScope ->
            internalInteractionSource = source
            created = true
            coroutineScope.launch {
                internalInteractionSource?.interactions?.collect {
                    interaction -> interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText("ClickableText",
                    modifier = Modifier
                        .testTag("clickable")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = indication
                        ) {}
                )
            }
        }

        rule.runOnIdle {
            // We should be eagerly created
            assertThat(created).isTrue()
            interactionSource = null
        }

        rule.runOnIdle {
            // Changing InteractionSource should cause the node to be detached, and a new one
            // created
            assertThat(detached).isTrue()
            assertThat(created).isTrue()
        }

        rule.onNodeWithTag("clickable")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun composedOverload_nonEquality() {
        val onClick = {}
        val modifier1 = Modifier.clickable(onClick = onClick)
        val modifier2 = Modifier.clickable(onClick = onClick)

        // The composed overload can never compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun nullInteractionSourceNullIndication_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(
                interactionSource = null,
                indication = null,
                enabled = toggleInput,
                onClick = onClick
            )
        }
    }

    @Test
    fun nonNullInteractionSourceNullIndication_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = toggleInput,
                onClick = onClick
            )
        }
    }

    @Test
    fun nullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(
                interactionSource = null,
                indication = indication,
                enabled = toggleInput,
                onClick = onClick
            )
        }
    }

    @Test
    fun nullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val indication = TestIndication {}
        val modifier1 = Modifier.clickable(
            interactionSource = null,
            indication = indication,
            onClick = onClick
        )
        val modifier2 = Modifier.clickable(
            interactionSource = null,
            indication = indication,
            onClick = onClick
        )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun nonNullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = toggleInput,
                onClick = onClick
            )
        }
    }

    @Test
    fun nonNullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndication {}
        val modifier1 = Modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick
        )
        val modifier2 = Modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick
        )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }
}

/**
 * No-op [Indication] for testing purposes.
 *
 * @param onCreate lambda executed when the instance is created with [rememberUpdatedInstance]
 */
@Suppress("DEPRECATION_ERROR")
internal class TestIndication(val onCreate: (InteractionSource) -> Unit) : Indication {
    @Deprecated("Super method is deprecated")
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        onCreate(interactionSource)
        return Instance
    }

    object Instance : IndicationInstance {
        override fun ContentDrawScope.drawIndication() {
            drawContent()
        }
    }
}

/**
 * No-op [IndicationNodeFactory] for testing purposes.
 *
 * @param onDetach lambda executed when the instance is detached
 * @param onAttach lambda executed when the instance is created with [create]
 */
internal class TestIndicationNodeFactory(
    val onDetach: () -> Unit = {},
    val onAttach: ((InteractionSource, CoroutineScope) -> Unit)
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node() {
            override fun onAttach() {
                onAttach(interactionSource, coroutineScope)
            }

            override fun onDetach() {
                this@TestIndicationNodeFactory.onDetach()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestIndicationNodeFactory) return false

        if (onAttach != other.onAttach) return false

        return true
    }

    override fun hashCode(): Int {
        return onAttach.hashCode()
    }
}
