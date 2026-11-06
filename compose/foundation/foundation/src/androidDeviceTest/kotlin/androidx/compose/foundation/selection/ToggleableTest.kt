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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.autofill.createFromBoolean
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
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
class ToggleableTest {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun toggleableTest_defaultSemantics() = runComposeUiTest {
        setContent {
            Column {
                Box(
                    Modifier.triStateToggleable(state = ToggleableState.On, onClick = {})
                        .testTag("checkedToggleable"),
                    content = { BasicText("ToggleableText") },
                )
                Box(
                    Modifier.triStateToggleable(state = ToggleableState.Off, onClick = {})
                        .testTag("unCheckedToggleable"),
                    content = { BasicText("ToggleableText") },
                )
                Box(
                    Modifier.triStateToggleable(
                            state = ToggleableState.Indeterminate,
                            onClick = {},
                        )
                        .testTag("indeterminateToggleable"),
                    content = { BasicText("ToggleableText") },
                )
            }
        }

        fun hasIndeterminateState(): SemanticsMatcher =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ToggleableState,
                ToggleableState.Indeterminate,
            )

        fun autofillFillActionSet(): SemanticsMatcher =
            SemanticsMatcher.keyIsDefined(SemanticsActions.OnFillData)

        fun autofillDataToggleSet(): SemanticsMatcher =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDataType,
                ContentDataType.Toggle,
            )

        fun hasFillableData(expected: Boolean): SemanticsMatcher =
            SemanticsMatcher("fillableData is $expected") { node ->
                val fillableData = node.config.getOrNull(SemanticsProperties.FillableData)
                fillableData?.booleanValue == expected
            }

        fun autofillFillDataNotDefined(): SemanticsMatcher =
            SemanticsMatcher.keyNotDefined(SemanticsProperties.FillableData)

        fun roleNotSet(): SemanticsMatcher =
            SemanticsMatcher.keyNotDefined(SemanticsProperties.Role)

        fun SemanticsNodeInteraction.assertAutofill(
            expectedFillable: Boolean?
        ): SemanticsNodeInteraction {
            this.assert(autofillDataToggleSet()).assert(autofillFillActionSet())
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                // FillableData only available on API 26+.
                if (expectedFillable != null) {
                    this.assert(hasFillableData(expectedFillable))
                } else {
                    this.assert(autofillFillDataNotDefined())
                }
            }
            return this
        }

        onNodeWithTag("checkedToggleable")
            .assert(roleNotSet())
            .assertIsEnabled()
            .assertIsOn()
            .assertAutofill(expectedFillable = true)
            .assertHasClickAction()
        onNodeWithTag("unCheckedToggleable")
            .assert(roleNotSet())
            .assertIsEnabled()
            .assertIsOff()
            .assertAutofill(expectedFillable = false)
            .assertHasClickAction()
        onNodeWithTag("indeterminateToggleable")
            .assert(roleNotSet())
            .assertIsEnabled()
            .assert(hasIndeterminateState())
            .assertAutofill(expectedFillable = null)
            .assertHasClickAction()
    }

    @Test
    fun toggleableTest_booleanOverload_defaultSemantics() = runComposeUiTest {
        setContent {
            Column {
                Box(
                    Modifier.toggleable(value = true, onValueChange = {})
                        .testTag("checkedToggleable"),
                    content = { BasicText("ToggleableText") },
                )
                Box(
                    Modifier.toggleable(value = false, onValueChange = {})
                        .testTag("unCheckedToggleable"),
                    content = { BasicText("ToggleableText") },
                )
            }
        }

        onNodeWithTag("checkedToggleable").assertIsEnabled().assertIsOn().assertHasClickAction()
        onNodeWithTag("unCheckedToggleable").assertIsEnabled().assertIsOff().assertHasClickAction()
    }

    @Test
    fun toggleableTest_autofill_updatesState() = runComposeUiTest {
        var checkedState = false
        setContent {
            Box(
                Modifier.toggleable(value = checkedState, onValueChange = { checkedState = it })
                    .testTag("toggleableNode")
            )
        }

        val fillableData = FillableData.createFromBoolean(true)
        if (fillableData != null) {
            // Attempt to trigger Autofill action OnFillData
            onNodeWithTag("toggleableNode").performSemanticsAction(SemanticsActions.OnFillData) {
                it(fillableData)
            }

            waitForIdle()

            // Verify if the backing application state variable `checkedState` was updated
            assertThat(checkedState).isTrue()
        }
    }

    @Test
    fun triStateToggleable_autofill_updatesState() = runComposeUiTest {
        var state by mutableStateOf(ToggleableState.Off)
        setContent {
            Box(
                Modifier.triStateToggleable(
                        state = state,
                        onClick = {
                            state =
                                when (state) {
                                    ToggleableState.Off -> ToggleableState.On
                                    ToggleableState.On -> ToggleableState.Off
                                    ToggleableState.Indeterminate -> ToggleableState.On
                                }
                        },
                    )
                    .testTag("triStateToggleableNode")
            )
        }

        val fillableData = FillableData.createFromBoolean(true)
        if (fillableData != null) {
            // Attempt to trigger Autofill action OnFillData to set state to On (true)
            onNodeWithTag("triStateToggleableNode").performSemanticsAction(
                SemanticsActions.OnFillData
            ) {
                it(fillableData)
            }

            waitForIdle()

            // Verify if the backing application state variable `state` was updated to
            // ToggleableState.On
            assertThat(state).isEqualTo(ToggleableState.On)
        }
    }

    @Test
    fun triStateToggleable_autofill_updatesState_toOff() = runComposeUiTest {
        var state by mutableStateOf(ToggleableState.On)
        setContent {
            Box(
                Modifier.triStateToggleable(
                        state = state,
                        onClick = {
                            state =
                                when (state) {
                                    ToggleableState.Off -> ToggleableState.On
                                    ToggleableState.On -> ToggleableState.Off
                                    ToggleableState.Indeterminate -> ToggleableState.On
                                }
                        },
                    )
                    .testTag("triStateToggleableNode")
            )
        }

        val fillableData = FillableData.createFromBoolean(false)
        if (fillableData != null) {
            // Attempt to trigger Autofill action OnFillData to set state to Off (false)
            onNodeWithTag("triStateToggleableNode").performSemanticsAction(
                SemanticsActions.OnFillData
            ) {
                it(fillableData)
            }

            waitForIdle()

            // Verify if the backing application state variable `state` was updated to
            // ToggleableState.Off
            assertThat(state).isEqualTo(ToggleableState.Off)
        }
    }

    @Test
    fun triStateToggleable_autofill_indeterminate_ignored() = runComposeUiTest {
        var state by mutableStateOf(ToggleableState.Indeterminate)
        var onClickCalled = false
        setContent {
            Box(
                Modifier.triStateToggleable(state = state, onClick = { onClickCalled = true })
                    .testTag("triStateToggleableNode")
            )
        }

        val fillableData = FillableData.createFromBoolean(true)
        if (fillableData != null) {
            var handled = true
            onNodeWithTag("triStateToggleableNode").performSemanticsAction(
                SemanticsActions.OnFillData
            ) {
                handled = it(fillableData)
            }

            waitForIdle()

            assertThat(handled).isFalse()
            assertThat(onClickCalled).isFalse()
            assertThat(state).isEqualTo(ToggleableState.Indeterminate)
        }
    }

    @Test
    fun toggleableTest_disabledSemantics() = runComposeUiTest {
        setContent {
            Box {
                Box(
                    Modifier.triStateToggleable(
                        state = ToggleableState.On,
                        onClick = {},
                        enabled = false,
                    ),
                    content = { BasicText("ToggleableText") },
                )
            }
        }

        onNode(isToggleable()).assertIsNotEnabled().assertHasClickAction()
    }

    @Test
    fun toggleableTest_toggle() = runComposeUiTest {
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }

        setContent {
            Box {
                Box(
                    Modifier.toggleable(value = checked, onValueChange = onCheckedChange),
                    content = { BasicText("ToggleableText") },
                )
            }
        }

        onNode(isToggleable()).performClick()

        runOnIdle { assertThat(checked).isEqualTo(false) }
    }

    @Test
    fun toggleableTest_toggle_consumedWhenDisabled() = runComposeUiTest {
        val enabled = mutableStateOf(false)
        var checked = true
        val onCheckedChange: (Boolean) -> Unit = { checked = it }
        var outerChecked = true
        val outerOnCheckedChange: (Boolean) -> Unit = { outerChecked = it }

        setContent {
            Box(Modifier.toggleable(value = outerChecked, onValueChange = outerOnCheckedChange)) {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("myToggleable")
                            .toggleable(
                                value = checked,
                                onValueChange = onCheckedChange,
                                enabled = enabled.value,
                            ),
                )
            }
        }

        onNodeWithTag("myToggleable").performClick()

        runOnIdle {
            assertThat(checked).isTrue()
            assertThat(outerChecked).isTrue()
            enabled.value = true
        }

        onNodeWithTag("myToggleable").performClick()

        runOnIdle {
            assertThat(checked).isFalse()
            assertThat(outerChecked).isTrue()
        }
    }

    @Test
    fun toggleableTest_interactionSource_noScrollableContainer() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        mainClock.autoAdvance = false

        setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = {},
                    )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithText("ToggleableText").performTouchInput { down(center) }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        onNodeWithText("ToggleableText").performTouchInput { up() }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_resetWhenDisposed_noScrollableContainer() =
        runComposeUiTest {
            val interactionSource = MutableInteractionSource()
            var emitToggleableText by mutableStateOf(true)

            lateinit var scope: CoroutineScope

            mainClock.autoAdvance = false

            setContent {
                scope = rememberCoroutineScope()
                Box {
                    if (emitToggleableText) {
                        Box(
                            Modifier.toggleable(
                                value = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onValueChange = {},
                            )
                        ) {
                            BasicText("ToggleableText")
                        }
                    }
                }
            }

            val interactions = mutableListOf<Interaction>()

            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            runOnIdle { assertThat(interactions).isEmpty() }

            onNodeWithText("ToggleableText").performTouchInput { down(center) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            // Dispose toggleable
            runOnIdle { emitToggleableText = false }

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
    fun toggleableTest_interactionSource_scrollableContainer() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        mainClock.autoAdvance = false

        setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = {},
                    )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithText("ToggleableText").performTouchInput { down(center) }

        // Advance past the tap timeout
        mainClock.advanceTimeBy(TapIndicationDelay)

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        onNodeWithText("ToggleableText").performTouchInput { up() }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_resetWhenDisposed_scrollableContainer() =
        runComposeUiTest {
            val interactionSource = MutableInteractionSource()
            var emitToggleableText by mutableStateOf(true)

            lateinit var scope: CoroutineScope

            mainClock.autoAdvance = false

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.verticalScroll(rememberScrollState())) {
                    if (emitToggleableText) {
                        Box(
                            Modifier.toggleable(
                                value = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onValueChange = {},
                            )
                        ) {
                            BasicText("ToggleableText")
                        }
                    }
                }
            }

            val interactions = mutableListOf<Interaction>()

            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            runOnIdle { assertThat(interactions).isEmpty() }

            onNodeWithText("ToggleableText").performTouchInput { down(center) }

            // Advance past the tap timeout
            mainClock.advanceTimeBy(TapIndicationDelay)

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            // Dispose toggleable
            runOnIdle { emitToggleableText = false }

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
    fun toggleableTest_interactionSource_hover() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        setContent {
            scope = rememberCoroutineScope()
            Box {
                Box(
                    Modifier.toggleable(
                        value = true,
                        interactionSource = interactionSource,
                        indication = null,
                        onValueChange = {},
                    )
                ) {
                    BasicText("ToggleableText")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        runOnIdle { assertThat(interactions).isEmpty() }

        onNodeWithText("ToggleableText").performMouseInput { enter(center) }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        onNodeWithText("ToggleableText").performMouseInput { exit(Offset(-1f, -1f)) }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
        }
    }

    @Test
    fun toggleableTest_interactionSource_focus_inTouchMode() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Touch)) {
            val interactionSource = MutableInteractionSource()
            lateinit var scope: CoroutineScope
            val focusRequester = FocusRequester()

            setContent {
                scope = rememberCoroutineScope()
                Box {
                    Box(
                        Modifier.focusRequester(focusRequester)
                            .toggleable(
                                value = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onValueChange = {},
                            )
                    ) {
                        BasicText("ToggleableText")
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
    fun toggleableTest_interactionSource_focus_inKeyboardMode() =
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
                            .toggleable(
                                value = true,
                                interactionSource = interactionSource,
                                indication = null,
                                onValueChange = {},
                            )
                    ) {
                        BasicText("ToggleableText")
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
    fun toggleableText_testInspectorValue_noIndication() = runComposeUiTest {
        setContent {
            val modifier = Modifier.toggleable(value = true, onValueChange = {}) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("toggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "value",
                    "interactionSource",
                    "indicationNodeFactory",
                    "enabled",
                    "role",
                    "onValueChange",
                )
        }
    }

    @Test
    fun toggleableTest_testInspectorValue_fullParams() = runComposeUiTest {
        setContent {
            val modifier =
                Modifier.toggleable(
                        value = true,
                        onValueChange = {},
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    )
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("toggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "value",
                    "indicationNodeFactory",
                    "interactionSource",
                    "enabled",
                    "role",
                    "onValueChange",
                )
        }
    }

    @Test
    fun toggleableTest_testInspectorValueTriState_noIndication() = runComposeUiTest {
        setContent {
            val modifier =
                Modifier.triStateToggleable(state = ToggleableState.On, onClick = {})
                    as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("triStateToggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "interactionSource",
                    "indicationNodeFactory",
                    "enabled",
                    "role",
                    "onClick",
                )
        }
    }

    @Test
    fun toggleableTest_testInspectorValueTriState_fullParams() = runComposeUiTest {
        setContent {
            val modifier =
                Modifier.triStateToggleable(
                        state = ToggleableState.On,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("triStateToggleable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "indicationNodeFactory",
                    "interactionSource",
                    "enabled",
                    "role",
                    "onClick",
                )
        }
    }

    @Test
    fun toggleable_minTouchTarget_clickOutsideLayoutBounds() = runComposeUiTest {
        var toggled by mutableStateOf(false)
        val interactionSource = MutableInteractionSource()
        testToggleableMinTouchTarget {
            Modifier.toggleable(
                value = toggled,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { toggled = true },
            )
        }
    }

    @Test
    fun triStateToggleable_minTouchTarget_clickOutsideLayoutBounds() = runComposeUiTest {
        var toggleableState by mutableStateOf(ToggleableState.Off)
        val interactionSource = MutableInteractionSource()
        testToggleableMinTouchTarget {
            Modifier.triStateToggleable(
                state = toggleableState,
                interactionSource = interactionSource,
                indication = null,
                onClick = { toggleableState = ToggleableState.On },
            )
        }
    }

    @Test
    fun triStateToggleable_noInteractionSource_minTouchTarget_clickOutsideLayoutBounds() =
        runComposeUiTest {
            var toggleableState by mutableStateOf(ToggleableState.Off)
            testToggleableMinTouchTarget {
                Modifier.triStateToggleable(
                    state = toggleableState,
                    onClick = { toggleableState = ToggleableState.On },
                )
            }
        }

    private fun ComposeUiTest.testToggleableMinTouchTarget(modifier: () -> Modifier) {
        with(density) {
            val tag = "toggleable"
            setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(modifier().requiredSize(2.dp).testTag(tag)) { BasicText("ToggleableText") }
                }
            }

            onNodeWithTag(tag)
                .assertIsOff()
                .assertWidthIsEqualTo(2.dp)
                .assertHeightIsEqualTo(2.dp)
                .assertTouchWidthIsEqualTo(48.dp)
                .assertTouchHeightIsEqualTo(48.dp)
                .performTouchInput { click(position = Offset(-1f, -1f)) }
                .assertIsOn()
        }
    }

    @Test
    fun toggleableTest_clickWithEnterKey() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val focusRequester = FocusRequester()
            var toggled by mutableStateOf(false)
            setContent {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = toggled
                        ) {
                            toggled = it
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            val toggleableNode = onNodeWithTag("toggleable")
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyDown(Key.Enter) }
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyUp(Key.Enter) }
            runOnIdle { assertThat(toggled).isTrue() }
        }

    @Test
    fun toggleableTest_clickWithSpaceKey() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val focusRequester = FocusRequester()
            var toggled by mutableStateOf(false)
            setContent {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = toggled
                        ) {
                            toggled = it
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            val toggleableNode = onNodeWithTag("toggleable")
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyDown(Key.Spacebar) }
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyUp(Key.Spacebar) }
            runOnIdle { assertThat(toggled).isTrue() }
        }

    @Test
    fun toggleableTest_clickWithNumPadEnterKey() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val focusRequester = FocusRequester()
            var toggled by mutableStateOf(false)
            setContent {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = toggled
                        ) {
                            toggled = it
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            val toggleableNode = onNodeWithTag("toggleable")
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyDown(Key.NumPadEnter) }
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyUp(Key.NumPadEnter) }
            runOnIdle { assertThat(toggled).isTrue() }
        }

    @Test
    fun toggleableTest_clickWithDpadCenter() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val focusRequester = FocusRequester()
            var toggled by mutableStateOf(false)
            setContent {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable").focusRequester(focusRequester).toggleable(
                            value = toggled
                        ) {
                            toggled = it
                        },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            val toggleableNode = onNodeWithTag("toggleable")
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyDown(Key.DirectionCenter) }
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyUp(Key.DirectionCenter) }
            runOnIdle { assertThat(toggled).isTrue() }
        }

    @Test
    fun toggleableTest_clickWithEnterKey_triStateToggleable() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val focusRequester = FocusRequester()
            var toggled by mutableStateOf(false)
            setContent {
                BasicText(
                    "ToggleableText",
                    modifier =
                        Modifier.testTag("toggleable")
                            .focusRequester(focusRequester)
                            .triStateToggleable(ToggleableState(toggled)) { toggled = !toggled },
                )
            }

            runOnIdle { focusRequester.requestFocus() }

            val toggleableNode = onNodeWithTag("toggleable")
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyDown(Key.Enter) }
            runOnIdle { assertThat(toggled).isFalse() }

            toggleableNode.performKeyInput { keyUp(Key.Enter) }
            runOnIdle { assertThat(toggled).isTrue() }
        }

    @Test
    fun toggleableTest_enterKey_emitsIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .toggleable(
                                    value = false,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("toggleable").performKeyInput { keyDown(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("toggleable").performKeyInput { keyUp(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun toggleableTest_numPadEnterKey_emitsIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .toggleable(
                                    value = false,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("toggleable").performKeyInput { keyDown(Key.NumPadEnter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("toggleable").performKeyInput { keyUp(Key.NumPadEnter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun toggleableTest_dpadCenter_emitsIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .toggleable(
                                    value = false,
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

            onNodeWithTag("toggleable").performKeyInput { keyDown(Key.DirectionCenter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            onNodeWithTag("toggleable").performKeyInput { keyUp(Key.DirectionCenter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun toggleableTest_otherKey_doesNotEmitIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .toggleable(
                                    value = false,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("toggleable").performKeyInput { pressKey(Key.Backspace) }
            runOnIdle { assertThat(interactions).isEmpty() }
        }

    @Test
    fun toggleableTest_doubleEnterKey_emitsFurtherInteractions() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .toggleable(
                                    value = false,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            val toggleableNode = onNodeWithTag("toggleable")

            toggleableNode.performKeyInput { pressKey(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            }

            toggleableNode.performKeyInput { keyDown(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(3)
                assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
                assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            }

            toggleableNode.performKeyInput { keyUp(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(4)
                assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
                assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun toggleableTest_repeatKeyEvents_doNotEmitFurtherInteractions() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            lateinit var scope: CoroutineScope
            var repeatCounter = 0
            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .onKeyEvent {
                                    if (it.nativeKeyEvent.repeatCount != 0) repeatCounter++
                                    false
                                }
                                .toggleable(
                                    value = false,
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {},
                    )
                }
            }

            runOnIdle { focusRequester.requestFocus() }

            val interactions = mutableListOf<Interaction>()
            scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

            onNodeWithTag("toggleable").performKeyInput {
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

            onNodeWithTag("toggleable").performKeyInput { keyUp(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(2)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
                assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
            }
        }

    @Test
    fun toggleableTest_interruptedClick_emitsCancelIndication() =
        runComposeUiTest(ComposeUiTestConfig(inputMode = Keyboard)) {
            val interactionSource = MutableInteractionSource()
            val focusRequester = FocusRequester()
            val enabled = mutableStateOf(true)
            lateinit var scope: CoroutineScope

            setContent {
                scope = rememberCoroutineScope()
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ToggleableText",
                        modifier =
                            Modifier.testTag("toggleable")
                                .focusRequester(focusRequester)
                                .toggleable(
                                    value = false,
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

            val toggleableNode = onNodeWithTag("toggleable")

            toggleableNode.performKeyInput { keyDown(Key.Enter) }

            runOnIdle {
                assertThat(interactions).hasSize(1)
                assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            }

            enabled.value = false

            toggleableNode.assertIsNotEnabled()

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
            toggleableNode.performKeyInput { keyUp(Key.Enter) }

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
    fun toggleableTest_localIndication_interactionSource_eagerlyCreated() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "Toggleable",
                        modifier =
                            Modifier.testTag("toggleable").toggleable(
                                value = false,
                                interactionSource = interactionSource,
                            ) {},
                    )
                }
            }
        }
        runOnIdle { assertThat(created).isTrue() }
    }

    @Test
    fun toggleableTest_noInteractionSource_lazilyCreated_pointerInput() = runComposeUiTest {
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
                    "Toggleable",
                    modifier =
                        Modifier.testTag("toggleable").toggleable(
                            value = false,
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        onNodeWithTag("toggleable").performTouchInput { down(center) }

        runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun triStateToggleable_localIndication_interactionSource_eagerlyCreated() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var created = false
        val state = ToggleableState(value = false)
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "Toggleable",
                        modifier =
                            Modifier.testTag("toggleable").triStateToggleable(
                                state = state,
                                interactionSource = interactionSource,
                            ) {},
                    )
                }
            }
        }
        runOnIdle { assertThat(created).isTrue() }
    }

    @Test
    fun triStateToggleable_noInteractionSource_lazilyCreated_pointerInput() = runComposeUiTest {
        var created = false
        val state = ToggleableState(value = false)
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
                    "Toggleable",
                    modifier =
                        Modifier.testTag("toggleable").triStateToggleable(
                            state = state,
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        onNodeWithTag("toggleable").performTouchInput { down(center) }

        runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun toggleable_nullInteractionSource_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = null,
                onValueChange = onValueChange,
            )
        }
    }

    @Test
    fun toggleable_nonNullInteractionSource_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = interactionSource,
                onValueChange = onValueChange,
            )
        }
    }

    @Test
    fun toggleable_nullInteractionSourceNullIndication_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = null,
                indication = null,
                onValueChange = onValueChange,
            )
        }
    }

    @Test
    fun toggleable_nonNullInteractionSourceNullIndication_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onValueChange,
            )
        }
    }

    @Test
    fun toggleable_nullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = null,
                indication = indication,
                onValueChange = onValueChange,
            )
        }
    }

    @Test
    fun toggleable_nullInteractionSourceNonNullIndication_nonEquality() {
        val onValueChange: (Boolean) -> Unit = {}
        val indication = TestIndication {}
        val modifier1 =
            Modifier.toggleable(
                value = true,
                interactionSource = null,
                indication = indication,
                onValueChange = onValueChange,
            )
        val modifier2 =
            Modifier.toggleable(
                value = true,
                interactionSource = null,
                indication = indication,
                onValueChange = onValueChange,
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun toggleable_nonNullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.toggleable(
                value = toggleInput,
                interactionSource = interactionSource,
                indication = indication,
                onValueChange = onValueChange,
            )
        }
    }

    @Test
    fun toggleable_nonNullInteractionSourceNonNullIndication_nonEquality() {
        val onValueChange: (Boolean) -> Unit = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndication {}
        val modifier1 =
            Modifier.toggleable(
                value = true,
                interactionSource = interactionSource,
                indication = indication,
                onValueChange = onValueChange,
            )
        val modifier2 =
            Modifier.toggleable(
                value = true,
                interactionSource = interactionSource,
                indication = indication,
                onValueChange = onValueChange,
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun triStateToggleable_nullInteractionSource_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = null,
                onClick = onClick,
            )
        }
    }

    @Test
    fun triStateToggleable_nonNullInteractionSource_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = interactionSource,
                onClick = onClick,
            )
        }
    }

    @Test
    fun triStateToggleable_nullInteractionSourceNullIndication_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
        }
    }

    @Test
    fun triStateToggleable_nonNullInteractionSourceNullIndication_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
        }
    }

    @Test
    fun triStateToggleable_nullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = null,
                indication = indication,
                onClick = onClick,
            )
        }
    }

    @Test
    fun triStateToggleable_nullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val indication = TestIndication {}
        val modifier1 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = null,
                indication = indication,
                onClick = onClick,
            )
        val modifier2 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = null,
                indication = indication,
                onClick = onClick,
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun triStateToggleable_nonNullInteractionSourceNonNullIndicationNodeFactory_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndicationNodeFactory({}, { _, _ -> })
        assertModifierIsPure { toggleInput ->
            Modifier.triStateToggleable(
                state = ToggleableState(toggleInput),
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )
        }
    }

    @Test
    fun triStateToggleable_nonNullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        val indication = TestIndication {}
        val modifier1 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )
        val modifier2 =
            Modifier.triStateToggleable(
                state = ToggleableState.On,
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }
}
