/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class CombinedClickableTest {

    @get:Rule
    val rule = createComposeRule()

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
                    modifier = Modifier.testTag("myClickable").combinedClickable {}
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
                    modifier = Modifier.testTag("myClickable").combinedClickable(enabled = false) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun longClickSemantics() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(onLongClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performSemanticsAction(SemanticsActions.OnLongClick)

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }
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
                    modifier = Modifier.testTag("myClickable").combinedClickable(onClick = onClick)
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
                        .combinedClickable { counter++ }
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
                    .combinedClickable { counter++ }
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
                    .combinedClickable { counter++ }
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
            Box(modifier = Modifier.combinedClickable(onClick = onClick)) {
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
    @LargeTest
    fun longClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(onLongClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(2)
        }
    }

    @Test
    fun click_withLongClick() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                click()
            }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun click_withDoubleClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = onDoubleClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun click_withDoubleClick_andLongClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    fun doubleClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(onDoubleClick = onClick) {}
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { counter == 1 }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { counter == 2 }
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                            .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {}
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
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle {
            focusManager.clearFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1])
                .isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    // TODO: b/202871171 - add test for changing between keyboard mode and touch mode, making sure
    // it resets existing focus

    /**
     * Regression test for b/186223077
     *
     * Tests that if a long click causes the long click lambda to change instances, we will still
     * correctly wait for the up event and emit [PressInteraction.Release].
     */
    @Test
    @LargeTest
    fun longClick_interactionSource_continuesTrackingPressAfterLambdasChange() {
        val interactionSource = MutableInteractionSource()

        var onLongClick by mutableStateOf({})
        val finalLongClick = {}
        val initialLongClick = { onLongClick = finalLongClick }
        // Simulate the long click causing a recomposition, and changing the lambda instance
        onLongClick = initialLongClick

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onLongClick = onLongClick,
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
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        // Simulate a long click
        rule.mainClock.advanceTimeBy(1000)
        // Run another frame to trigger recomposition caused by the long click
        rule.mainClock.advanceTimeByFrame()

        // We should have a press interaction, with no release, even though the lambda instance
        // has changed
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(onLongClick).isEqualTo(finalLongClick)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { up() }

        // The up should now cause a release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    /**
     * Regression test for b/186223077
     *
     * Tests that if a long click causes the long click lambda to become null, we will emit
     * [PressInteraction.Cancel].
     */
    @Test
    @LargeTest
    fun longClick_interactionSource_cancelsIfLongClickBecomesNull() {
        val interactionSource = MutableInteractionSource()

        var onLongClick: (() -> Unit)? by mutableStateOf(null)
        val initialLongClick = { onLongClick = null }
        // Simulate the long click causing a recomposition, and changing the lambda to be null
        onLongClick = initialLongClick

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            onLongClick = onLongClick,
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
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        // Initial press
        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        // Long click
        rule.mainClock.advanceTimeBy(1000)
        // Run another frame to trigger recomposition caused by the long click
        rule.mainClock.advanceTimeByFrame()

        // The new onLongClick lambda should be null, and so we should cancel the existing press.
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
            assertThat(onLongClick).isNull()
        }
    }

    @Test
    @LargeTest
    fun click_withDoubleClick_andLongClick_disabled() {
        val enabled = mutableStateOf(false)
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            enabled = enabled.value,
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun clicks_consumedWhenDisabled() {
        val enabled = mutableStateOf(false)
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }
        var outerClickCounter = 0
        var outerDoubleClickCounter = 0
        var outerLongClickCounter = 0
        val outerOnClick: () -> Unit = { ++outerClickCounter }
        val outerOnDoubleClick: () -> Unit = { ++outerDoubleClickCounter }
        val outerOnLongClick: () -> Unit = { ++outerLongClickCounter }

        rule.setContent {
            Box(
                Modifier.combinedClickable(
                    onDoubleClick = outerOnDoubleClick,
                    onLongClick = outerOnLongClick,
                    onClick = outerOnClick
                )
            ) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier
                        .testTag("myClickable")
                        .combinedClickable(
                            enabled = enabled.value,
                            onDoubleClick = onDoubleClick,
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                )
            }
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable")
            .performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                doubleClick()
            }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput {
                longClick()
            }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
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
                    .combinedClickable(
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
                            .combinedClickable(enabled = enabled.value) {}
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
                Box(Modifier.combinedClickable(enabled = enabled.value, onClick = {})) {
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
            val modifier = Modifier.combinedClickable(onClick = onClick) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("combinedClickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "role",
                "onClick",
                "onDoubleClick",
                "onLongClick",
                "onLongClickLabel"
            )
        }
    }

    @Test
    fun testInspectorValue_fullParamsOverload() {
        val onClick: () -> Unit = { }
        rule.setContent {
            val modifier = Modifier.combinedClickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ).first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("combinedClickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "enabled",
                "onClickLabel",
                "onClick",
                "role",
                "onDoubleClick",
                "onLongClick",
                "onLongClickLabel",
                "indicationNodeFactory",
                "interactionSource"
            )
        }
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
                    .combinedClickable { clicked = true }
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
                    .combinedClickable { clicked = true }
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
                    .combinedClickable { clicked = true }
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
    fun enterKey_emitsIndication() {
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
                        .combinedClickable(
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
    fun numPadEnterKey_emitsIndication() {
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
                        .combinedClickable(
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
    fun dpadCenter_emitsIndication() {
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
                        .combinedClickable(
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
    fun otherKey_doesNotEmitIndication() {
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
                        .combinedClickable(
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
                        .combinedClickable(
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
                        .combinedClickable(
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
    fun interruptedClick_emitsCancelIndication() {
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
                        .combinedClickable(
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
}
