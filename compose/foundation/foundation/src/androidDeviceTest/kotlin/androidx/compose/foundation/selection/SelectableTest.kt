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

package androidx.compose.foundation.selection

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.TapIndicationDelay
import androidx.compose.foundation.TestIndication
import androidx.compose.foundation.TestIndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.setFocusableContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectableTest {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun selectable_defaultSemantics() = runComposeUiTest {
        setContent {
            BasicText(
                "Text in item",
                modifier = Modifier.selectable(selected = true, onClick = {}),
            )
        }

        onAllNodes(isSelectable())
            .assertCountEquals(1)
            .onFirst()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsSelected()
    }

    @Test
    fun selectable_defaultClicks() = runComposeUiTest {
        setContent {
            val state = remember { mutableStateOf(false) }
            BasicText(
                "Text in item",
                modifier =
                    Modifier.selectable(
                        selected = state.value,
                        onClick = { state.value = !state.value },
                    ),
            )
        }

        onNode(isSelectable())
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
            .performClick()
            .assertIsNotSelected()
    }

    @Test
    fun selectable_noClicksNoChanges() = runComposeUiTest {
        setContent {
            val (selected, _) = remember { mutableStateOf(false) }
            BasicText(
                "Text in item",
                modifier = Modifier.selectable(selected = selected, onClick = {}),
            )
        }

        onNode(isSelectable()).assertIsNotSelected().performClick().assertIsNotSelected()
    }

    @Test
    fun selectable_clicks_noPropagationWhenDisabled() = runComposeUiTest {
        val enabled = mutableStateOf(false)
        setContent {
            val state = remember { mutableStateOf(false) }
            val outerState = remember { mutableStateOf(false) }
            Box(
                Modifier.testTag("outerBox")
                    .selectable(
                        selected = outerState.value,
                        onClick = { outerState.value = !outerState.value },
                    )
            ) {
                BasicText(
                    "Text in item",
                    modifier =
                        Modifier.selectable(
                            selected = state.value,
                            onClick = { state.value = !state.value },
                            enabled = enabled.value,
                        ),
                )
            }
        }

        onNodeWithText("Text in item").assertIsNotSelected().performClick().assertIsNotSelected()

        onNodeWithTag("outerBox").assertIsNotSelected()
        runOnIdle { enabled.value = true }

        onNodeWithText("Text in item").performClick().assertIsSelected()

        onNodeWithTag("outerBox").assertIsNotSelected()
    }

    @Test
    fun selectableTest_interactionSource_noScrollableContainer() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        mainClock.autoAdvance = false

        setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.selectable(
                        selected = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                    )
                ) {
                    BasicText("SelectableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithText("SelectableText").performTouchInput { down(center) }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        onNodeWithText("SelectableText").performTouchInput { up() }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun selectableTest_interactionSource_resetWhenDisposed_noScrollableContainer() =
        runComposeUiTest {
            val interactionSource = MutableInteractionSource()
            var emitSelectableText by mutableStateOf(true)

            lateinit var scope: CoroutineScope

            mainClock.autoAdvance = false

            setContent {
                scope = rememberCoroutineScope()
                Box {
                    if (emitSelectableText) {
                        Box(
                            Modifier.selectable(
                                selected = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {},
                            )
                        ) {
                            BasicText("SelectableText")
                        }
                    }
                }
            }

            val interactions = mutableListOf<Interaction>()

            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            runOnIdle { assertThat(interactions).isEmpty() }

            onNodeWithText("SelectableText").performTouchInput { down(center) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            // Dispose selectable
            runOnIdle { emitSelectableText = false }

            mainClock.advanceTimeByFrame()

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
                assertThat((interactions[1] as PressInteraction.Cancel).press)
                    .isEqualTo(interactions[0])
            }
        }

    @Test
    fun selectableTest_interactionSource_scrollableContainer() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        mainClock.autoAdvance = false

        setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.selectable(
                        selected = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                    )
                ) {
                    BasicText("SelectableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithText("SelectableText").performTouchInput { down(center) }

        // Advance past the tap timeout
        mainClock.advanceTimeBy(TapIndicationDelay)

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        onNodeWithText("SelectableText").performTouchInput { up() }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun selectableTest_interactionSource_resetWhenDisposed_scrollableContainer() =
        runComposeUiTest {
            val interactionSource = MutableInteractionSource()
            var emitSelectableText by mutableStateOf(true)

            lateinit var scope: CoroutineScope

            mainClock.autoAdvance = false

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.verticalScroll(rememberScrollState())) {
                    if (emitSelectableText) {
                        Box(
                            Modifier.selectable(
                                selected = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {},
                            )
                        ) {
                            BasicText("SelectableText")
                        }
                    }
                }
            }

            val interactions = mutableListOf<Interaction>()

            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            runOnIdle { assertThat(interactions).isEmpty() }

            onNodeWithText("SelectableText").performTouchInput { down(center) }

            // Advance past the tap timeout
            mainClock.advanceTimeBy(TapIndicationDelay)

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            // Dispose selectable
            runOnIdle { emitSelectableText = false }

            mainClock.advanceTimeByFrame()

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
                assertThat((interactions[1] as PressInteraction.Cancel).press)
                    .isEqualTo(interactions[0])
            }
        }

    @Test
    fun selectableTest_interactionSource_hover() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.selectable(
                        selected = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {},
                    )
                ) {
                    BasicText("SelectableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithText("SelectableText").performMouseInput { enter(center) }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        onNodeWithText("SelectableText").performMouseInput { exit(Offset(-1f, -1f)) }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
        }
    }

    @Test
    fun selectableTest_interactionSource_focus_inTouchMode() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Touch)) {
            val interactionSource = MutableInteractionSource()
            lateinit var scope: CoroutineScope
            val focusRequester = FocusRequester()

            setContent {
                scope = rememberCoroutineScope()
                Box {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .selectable(
                                selected = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {},
                            )
                    ) {
                        BasicText("SelectableText")
                    }
                }
            }

            val interactions = mutableListOf<Interaction>()

            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            runOnIdle { assertThat(interactions).isEmpty() }

            runOnIdle { focusRequester.requestFocus() }

            // Touch mode, so we shouldn't be focused
            runOnIdle { assertThat(interactions).isEmpty() }
        }

    @Test
    fun selectableTest_interactionSource_focus_inKeyboardMode() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            lateinit var scope: CoroutineScope
            val focusRequester = FocusRequester()
            lateinit var focusManager: FocusManager

            setFocusableContent {
                scope = rememberCoroutineScope()
                focusManager = LocalFocusManager.current
                Box {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .selectable(
                                selected = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {},
                            )
                    ) {
                        BasicText("SelectableText")
                    }
                }
            }

            val interactions = mutableListOf<Interaction>()

            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            runOnIdle { assertThat(interactions).isEmpty() }

            runOnIdle { focusRequester.requestFocus() }

            // Keyboard mode, so we should now be focused and see an interaction
            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            }

            runOnIdle { focusManager.clearFocus() }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
                assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
                assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                    .isEqualTo(interactions[0])
            }
        }

    // TODO: b/202871171 - add test for changing between keyboard mode and touch mode, making sure
    // it resets existing focus

    @Test
    fun selectableTest_testInspectorValue_noIndication() = runComposeUiTest {
        setContent {
            val modifier = Modifier.selectable(false) {} as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("selectable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "selected",
                    "interactionSource",
                    "indicationNodeFactory",
                    "enabled",
                    "role",
                    "onClick",
                )
        }
    }

    @Test
    fun selectableTest_testInspectorValue_fullParams() = runComposeUiTest {
        setContent {
            val modifier =
                Modifier.selectable(
                        false,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("selectable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "selected",
                    "interactionSource",
                    "indicationNodeFactory",
                    "enabled",
                    "role",
                    "onClick",
                )
        }
    }

    @Test
    fun selectableTest_clickWithEnterKey() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            var counter = 0
            val focusRequester = FocusRequester()

            setContent {
                BasicText(
                    "SelectableText",
                    modifier =
                        Modifier.testTag("selectable").focusRequester(focusRequester).selectable(
                            selected = false
                        ) {
                            counter++
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            onNodeWithTag("selectable").performKeyInput { keyDown(Key.Enter) }

            runOnIdle { assertThat(counter).isEqualTo(0) }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.Enter) }

            runOnIdle { assertThat(counter).isEqualTo(1) }
        }

    @Test
    fun selectableTest_clickWithNumPadEnterKey() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            var counter = 0
            val focusRequester = FocusRequester()

            setContent {
                BasicText(
                    "SelectableText",
                    modifier =
                        Modifier.testTag("selectable").focusRequester(focusRequester).selectable(
                            selected = false
                        ) {
                            counter++
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            onNodeWithTag("selectable").performKeyInput { keyDown(Key.NumPadEnter) }

            runOnIdle { assertThat(counter).isEqualTo(0) }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.NumPadEnter) }

            runOnIdle { assertThat(counter).isEqualTo(1) }
        }

    @Test
    fun selectableTest_clickWithDPadCenter() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            var counter = 0
            val focusRequester = FocusRequester()

            setContent {
                BasicText(
                    "SelectableText",
                    modifier =
                        Modifier.testTag("selectable").focusRequester(focusRequester).selectable(
                            selected = false
                        ) {
                            counter++
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            onNodeWithTag("selectable").performKeyInput { keyDown(Key.DirectionCenter) }

            runOnIdle { assertThat(counter).isEqualTo(0) }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.DirectionCenter) }

            runOnIdle { assertThat(counter).isEqualTo(1) }
        }

    @Test
    fun selectableTest_enterKey_emitsIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("selectable").performKeyInput { keyDown(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun selectableTest_numPadEnterKey_emitsIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("selectable").performKeyInput { keyDown(Key.NumPadEnter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.NumPadEnter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun selectableTest_dpadCenter_emitsIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }
            waitForIdle()

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("selectable").performKeyInput { keyDown(Key.DirectionCenter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.DirectionCenter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun selectableTest_otherKey_doesNotEmitIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("selectable").performKeyInput { pressKey(Key.Backspace) }
            runOnIdle { assertThat(interactions).isEmpty() }
        }

    @Test
    fun selectableTest_doubleEnterKey_emitsFurtherInteractions() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            val selectableNode = onNodeWithTag("selectable")

            selectableNode.performKeyInput { pressKey(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            }

            selectableNode.performKeyInput { keyDown(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(3)
                assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
                assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            }

            selectableNode.performKeyInput { keyUp(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(4)
                assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
                assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun selectableTest_repeatKeyEvents_doNotEmitFurtherInteractions() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope
            var repeatCounter = 0

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .onKeyEvent {
                                    if (it.nativeKeyEvent.repeatCount != 0) repeatCounter++
                                    false
                                }
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("selectable").performKeyInput {
                keyDown(Key.Enter)

                advanceEventTime(500) // First repeat
                advanceEventTime(50) // Second repeat
            }

            runOnIdle {
                // Ensure that expected number of repeats occurred and did not cause press
                // interactions.
                assertThat(repeatCounter).isEqualTo(2)
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("selectable").performKeyInput { keyUp(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun selectableTest_interruptedClick_emitsCancelIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            val enabled = mutableStateOf(true)
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable")
                                .focusRequester(focusRequester)
                                .selectable(
                                    selected = true,
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = enabled.value,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            val selectableNode = onNodeWithTag("selectable")

            selectableNode.performKeyInput { keyDown(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            enabled.value = false

            selectableNode.assertIsNotEnabled()

            runOnIdle {
                // Filter out focus interactions.
                val pressInteractions = interactions.filterIsInstance<PressInteraction>()
                assertThat(pressInteractions).hasSize(2)
                assertThat(pressInteractions.first())
                    .isInstanceOf(PressInteraction.Press::class.java)
                assertThat(pressInteractions.last())
                    .isInstanceOf(PressInteraction.Cancel::class.java)
            }

            // Key releases should not result in interactions.
            selectableNode.performKeyInput { keyUp(Key.Enter) }

            // Make sure nothing has changed.
            runOnIdle {
                val pressInteractions = interactions.filterIsInstance<PressInteraction>()
                assertThat(pressInteractions).hasSize(2)
                assertThat(pressInteractions.first())
                    .isInstanceOf(PressInteraction.Press::class.java)
                assertThat(pressInteractions.last())
                    .isInstanceOf(PressInteraction.Cancel::class.java)
            }
        }

    @Test
    fun selectableTest_localIndication_interactionSource_eagerlyCreated() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "SelectableText",
                        modifier =
                            Modifier.testTag("selectable").selectable(
                                selected = false,
                                interactionSource = interactionSource,
                            ) {},
                    )
                }
            }
        }
        runOnIdle { assertThat(created).isTrue() }
    }

    @Test
    fun selectableTest_noInteractionSource_lazilyCreated_pointerInput() = runComposeUiTest {
        var created = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "SelectableText",
                    modifier =
                        Modifier.testTag("selectable").selectable(
                            selected = false,
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        onNodeWithTag("selectable").performTouchInput { down(center) }

        runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun nullInteractionSource_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.selectable(
                selected = toggleInput,
                interactionSource = null,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nonNullInteractionSource_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.selectable(
                selected = toggleInput,
                interactionSource = interactionSource,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nullInteractionSourceNullIndication_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.selectable(
                selected = toggleInput,
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nonNullInteractionSourceNullIndication_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.selectable(
                selected = toggleInput,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.selectable(
                selected = toggleInput,
                interactionSource = null,
                indication = indication,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val indication = TestIndication {}
        val modifier1 =
            Modifier.selectable(
                selected = true,
                interactionSource = null,
                indication = indication,
                onClick = onClick,
            )
        val modifier2 =
            Modifier.selectable(
                selected = true,
                interactionSource = null,
                indication = indication,
                onClick = onClick,
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
            Modifier.selectable(
                selected = toggleInput,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nonNullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndication {}
        val modifier1 =
            Modifier.selectable(
                selected = true,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )
        val modifier2 =
            Modifier.selectable(
                selected = true,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }
}
