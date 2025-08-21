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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.testutils.first
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import kotlin.test.Ignore
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class ClickableTest {

    @get:Rule val rule = createComposeRule()

    private val InstanceOf =
        Correspondence.from<Any, KClass<*>>(
            { obj, clazz -> clazz?.isInstance(obj) ?: false },
            "is an instance of",
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
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    @Test
    fun defaultSemantics() {
        rule.setContent {
            Box {
                BasicText("ClickableText", modifier = Modifier.testTag("myClickable").clickable {})
            }
        }

        rule
            .onNodeWithTag("myClickable")
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
                    modifier = Modifier.testTag("myClickable").clickable(enabled = false) {},
                )
            }
        }

        rule
            .onNodeWithTag("myClickable")
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
                    modifier =
                        Modifier.testTag("myClickable").clickable(enabled = enabled, role = role) {},
                )
            }
        }

        rule
            .onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()

        rule.runOnIdle { role = null }

        rule
            .onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasClickAction()

        rule.runOnIdle { enabled = false }

        rule
            .onNodeWithTag("myClickable")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasClickAction()
    }

    @Test
    fun click() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").clickable(onClick = onClick),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    fun click_withIndirectTouchEvent() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .clickable(onClick = onClick),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
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

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithText("Bar", substring = true).performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    fun requestFocus_touchMode() {
        // Arrange.
        val tag = "testClickable"
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.testTag(tag).size(10.dp).focusRequester(focusRequester).clickable {})
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Touch) }

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
            Box(Modifier.testTag(tag).size(10.dp).focusRequester(focusRequester).clickable {})
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }

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
            Box(Modifier.testTag(tag).size(10.dp).clickable {})
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Touch) }

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
            Box(Modifier.focusRequester(focusRequester).testTag(tag).size(10.dp).clickable {})
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // No scrollable container, so there should be no delay and we should instantly appear
        // pressed
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(currentTime = 0L)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent(currentTime = 16L)

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
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
    fun interactionSource_immediateRelease_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager

        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current

            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent(16L)

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
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
    fun interactionSource_immediateCancel_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchCancelEvent(sendMoveEvents = false)

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
                    modifier =
                        Modifier.testTag("myClickable")
                            .draggable(
                                state = rememberDraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .clickable(interactionSource = interactionSource, indication = null) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
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
    fun interactionSource_immediateDrag_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchMoveEvents(
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchReleaseEvent(halfTapIndicationDelay + 16L)

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
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
    fun interactionSource_immediateRelease_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent(16L)

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            cancel()
        }

        // We haven't reached the tap delay, and a cancel was emitted, so no press should ever be
        // shown
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_immediateCancel_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchCancelEvent(sendMoveEvents = false)

        // We haven't reached the tap delay, and a cancel was emitted, so no press should ever be
        // shown
        rule.runOnIdle { assertThat(interactions).isEmpty() }
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
                    modifier =
                        Modifier.testTag("myClickable")
                            .draggable(
                                state = rememberDraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .clickable(interactionSource = interactionSource, indication = null) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(centerLeft)
            moveTo(centerRight)
        }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_immediateDrag_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)
        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L, pressPosition)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchMoveEvents(
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle { assertThat(interactions).isEmpty() }
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
                    modifier =
                        Modifier.testTag("myClickable")
                            .draggable(
                                state = rememberDraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .clickable(interactionSource = interactionSource, indication = null) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(centerLeft) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { moveTo(centerRight) }

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
    fun interactionSource_dragAfterTimeout_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)
        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L, pressPosition)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchMoveEvents(
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

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
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { cancel() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_cancelledGesture_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)
        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L, pressPosition)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchCancelEvent(sendMoveEvents = false)

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
                        modifier =
                            Modifier.testTag("myClickable").clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose clickable
        rule.runOnIdle { emitClickableText = false }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_resetWhenReused() {
        val interactionSource = MutableInteractionSource()
        var key by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                ReusableContent(key) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable").clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Change the key to trigger reuse
        rule.runOnIdle { key = false }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_resetWhenMoved() {
        val interactionSource = MutableInteractionSource()
        var moveContent by mutableStateOf(false)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        val content = movableContentOf {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable").clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {},
            )
        }

        rule.setContent {
            scope = rememberCoroutineScope()
            if (moveContent) {
                Box { content() }
            } else {
                Box { content() }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Move the content
        rule.runOnIdle { moveContent = true }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_hover() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_hover_and_press() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput {
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
            assertThat((interactions[3] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
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
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Touch) }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Touch mode by default, so we shouldn't be focused
        rule.runOnIdle { assertThat(interactions).isEmpty() }
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
                    modifier =
                        Modifier.testTag("myClickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Keyboard mode, so we should now be focused and see an interaction
        rule.runOnIdle {
            assertThat(interactions)
                .comparingElementsUsing(InstanceOf)
                .containsExactly(FocusInteraction.Focus::class)
        }

        rule.runOnIdle { focusManager.clearFocus() }

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
                    modifier =
                        Modifier.testTag("myClickable")
                            .clickable(enabled = enabled.value, onClick = onClick),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerCounter).isEqualTo(0)
        }
    }

    // Helper functions for next several tests
    private fun Modifier.dynamicPointerInputModifier(
        enabled: Boolean,
        key: Any? = Unit,
        onEnter: () -> Unit = {},
        onMove: () -> Unit = {},
        onPress: () -> Unit = {},
        onRelease: () -> Unit = {},
        onExit: () -> Unit = {},
    ) =
        if (enabled) {
            pointerInput(key) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                onEnter()
                            }
                            PointerEventType.Press -> {
                                onPress()
                            }
                            PointerEventType.Move -> {
                                onMove()
                            }
                            PointerEventType.Release -> {
                                onRelease()
                            }
                            PointerEventType.Exit -> {
                                onExit()
                            }
                        }
                    }
                }
            }
        } else this

    private fun Modifier.dynamicPointerInputModifierWithDetectTapGestures(
        enabled: Boolean,
        key: Any? = Unit,
        onTap: () -> Unit = {},
    ) =
        if (enabled) {
            pointerInput(key) { detectTapGestures { onTap() } }
        } else {
            this
        }

    private fun Modifier.dynamicClickableModifier(enabled: Boolean, onClick: () -> Unit) =
        if (enabled) {
            clickable(interactionSource = null, indication = null) { onClick() }
        } else this

    // !!!!! MOUSE & TOUCH EVENTS TESTS WITH DYNAMIC MODIFIER INPUT TESTS SECTION (START) !!!!!
    // The next ~20 tests test enabling/disabling dynamic input modifiers (both pointer input and
    // clickable) using various combinations (touch vs. mouse, Unit vs. unique keys, nested UI
    // elements vs. all modifiers on one UI element, etc.)

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for the non-dynamic
     * pointer input and clickable{} for the dynamic pointer input (both on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicClickableModifier_addsAbovePointerInputWithKeyTouchEvents_correctEvents() {
        // This is part of a dynamic modifier
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var pointerInputPressCounter by mutableStateOf(0)
        var activateDynamicClickable by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicClickableModifier(activateDynamicClickable) { clickableClickCounter++ }
                    .pointerInput("unique_key_123") {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    pointerInputPressCounter++
                                } else if (event.type == PointerEventType.Release) {
                                    activateDynamicClickable = true
                                }
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, pointerInputPressCounter)
            assertEquals(0, clickableClickCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(2, pointerInputPressCounter)
            assertEquals(1, clickableClickCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for the non-dynamic
     * pointer input and clickable{} for the dynamic pointer input (both on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicClickableModifier_addsAbovePointerInputWithUnitKeyTouchEvents_correctEvents() {
        // This is part of a dynamic modifier
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var pointerInputPressCounter by mutableStateOf(0)
        var activateDynamicClickable by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicClickableModifier(activateDynamicClickable) { clickableClickCounter++ }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    pointerInputPressCounter++
                                } else if (event.type == PointerEventType.Release) {
                                    activateDynamicClickable = true
                                }
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, pointerInputPressCounter)
            assertEquals(0, clickableClickCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(2, pointerInputPressCounter)
            assertEquals(1, clickableClickCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for the non-dynamic
     * pointer input and clickable{} for the dynamic pointer input (both on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse enter
     * 2. Mouse "click()" (press/release)
     * 3. Mouse exit
     * 4. Assert
     */
    @Test
    fun dynamicClickableModifier_addsAbovePointerInputWithKeyMouseEvents_correctEvents() {
        // This is part of a dynamic modifier
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var pointerInputPressCounter by mutableStateOf(0)
        var activateDynamicClickable by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicClickableModifier(activateDynamicClickable) { clickableClickCounter++ }
                    .pointerInput("unique_key_123") {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    pointerInputPressCounter++
                                } else if (event.type == PointerEventType.Release) {
                                    activateDynamicClickable = true
                                }
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }

        rule.runOnIdle {
            assertEquals(1, pointerInputPressCounter)
            assertEquals(0, clickableClickCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }

        rule.runOnIdle {
            assertEquals(2, pointerInputPressCounter)
            assertEquals(1, clickableClickCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for the non-dynamic
     * pointer input and clickable{} for the dynamic pointer input (both on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse enter
     * 2. Mouse "click()" (press/release)
     * 3. Mouse exit
     * 4. Assert
     */
    @Test
    fun dynamicClickableModifier_addsAbovePointerInputWithUnitKeyMouseEvents_correctEvents() {
        // This is part of a dynamic modifier
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var pointerInputPressCounter by mutableStateOf(0)
        var activateDynamicClickable by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicClickableModifier(activateDynamicClickable) { clickableClickCounter++ }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    pointerInputPressCounter++
                                } else if (event.type == PointerEventType.Release) {
                                    activateDynamicClickable = true
                                }
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }

        rule.runOnIdle {
            assertEquals(1, pointerInputPressCounter)
            assertEquals(0, clickableClickCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }

        rule.runOnIdle {
            assertEquals(2, pointerInputPressCounter)
            assertEquals(1, clickableClickCounter)
        }
    }

    /* Uses clickable{} for the non-dynamic pointer input and pointer input
     * block (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer input (both
     * on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicInputModifier_addsAboveClickableWithKeyTouchEvents_correctEvents() {
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var dynamicPressCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        key = "unique_key_123",
                        onPress = { dynamicPressCounter++ },
                    )
                    .clickable {
                        clickableClickCounter++
                        activateDynamicPointerInput = true
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, clickableClickCounter)
            assertEquals(0, dynamicPressCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(2, clickableClickCounter)
            assertEquals(1, dynamicPressCounter)
        }
    }

    /* Uses clickable{} for the non-dynamic pointer input and pointer input
     * block (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer input (both
     * on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicInputModifier_addsAboveClickableWithUnitKeyTouchEvents_correctEvents() {
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var dynamicPressCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onPress = { dynamicPressCounter++ },
                    )
                    .clickable {
                        clickableClickCounter++
                        activateDynamicPointerInput = true
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, clickableClickCounter)
            assertEquals(0, dynamicPressCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(2, clickableClickCounter)
            assertEquals(1, dynamicPressCounter)
        }
    }

    /* Uses pointer input block for the non-dynamic pointer input and TWO pointer input
     * blocks (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer
     * inputs (both on same Box).
     * Both dynamic Pointers are disabled to start and then enabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     * 3. Touch down
     * 4. Assert
     * 5. Touch move
     * 6. Assert
     * 7. Touch up
     * 8. Assert
     */
    @Test
    fun twoDynamicInputModifiers_addsAbovePointerInputWithUnitKeyTouchEventsWithMove() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputPressCounter by mutableStateOf(0)
        var originalPointerInputMoveCounter by mutableStateOf(0)
        var originalPointerInputReleaseCounter by mutableStateOf(0)

        var activeDynamicPointerInput by mutableStateOf(false)
        var dynamicPointerInputPressCounter by mutableStateOf(0)
        var dynamicPointerInputMoveCounter by mutableStateOf(0)
        var dynamicPointerInputReleaseCounter by mutableStateOf(0)

        var activeDynamicPointerInput2 by mutableStateOf(false)
        var dynamicPointerInput2PressCounter by mutableStateOf(0)
        var dynamicPointerInput2ReleaseCounter by mutableStateOf(0)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activeDynamicPointerInput,
                        onPress = { dynamicPointerInputPressCounter++ },
                        onMove = { dynamicPointerInputMoveCounter++ },
                        onRelease = {
                            dynamicPointerInputReleaseCounter++
                            activeDynamicPointerInput2 = true
                        },
                    )
                    .dynamicPointerInputModifier(
                        enabled = activeDynamicPointerInput2,
                        onPress = { dynamicPointerInput2PressCounter++ },
                        onRelease = { dynamicPointerInput2ReleaseCounter++ },
                    )
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        originalPointerInputPressCounter++
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveCounter++
                                    }
                                    PointerEventType.Release -> {
                                        originalPointerInputReleaseCounter++
                                        activeDynamicPointerInput = true
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Even though we are enabling the dynamic pointer input, it will NOT receive events until
        // the next event stream (after the click is over) which is why you see zeros below.
        // Only two events are triggered for click (down/up)
        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            // With these events, we enable the dynamic pointer input
            assertEquals(1, originalPointerInputPressCounter)
            assertEquals(0, originalPointerInputMoveCounter)
            assertEquals(1, originalPointerInputReleaseCounter)

            assertEquals(0, dynamicPointerInputPressCounter)
            assertEquals(0, dynamicPointerInputMoveCounter)
            assertEquals(0, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicPointerInput2PressCounter)
            assertEquals(0, dynamicPointerInput2ReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(Offset(0f, 0f)) }

        rule.runOnIdle {
            // Each time a new dynamic pointer input is added DIRECTLY above an existing one, the
            // previously existing pointer input lambda will be restarted.
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputPressCounter)
            assertEquals(0, originalPointerInputMoveCounter)
            assertEquals(1, originalPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInputPressCounter)
            assertEquals(0, dynamicPointerInputMoveCounter)
            assertEquals(0, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicPointerInput2PressCounter)
            assertEquals(0, dynamicPointerInput2ReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { moveTo(Offset(1f, 1f)) }

        rule.runOnIdle {
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputPressCounter)
            assertEquals(1, originalPointerInputMoveCounter)
            assertEquals(1, originalPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInputPressCounter)
            assertEquals(1, dynamicPointerInputMoveCounter)
            assertEquals(0, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicPointerInput2PressCounter)
            assertEquals(0, dynamicPointerInput2ReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputPressCounter)
            assertEquals(1, originalPointerInputMoveCounter)
            assertEquals(2, originalPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInputPressCounter)
            assertEquals(1, dynamicPointerInputMoveCounter)
            // With this release counter, we enable the dynamic clickable{}
            assertEquals(1, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicPointerInput2PressCounter)
            assertEquals(0, dynamicPointerInput2ReleaseCounter)
        }

        // Only two events are triggered for click (down/up)
        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            // Each time a new dynamic pointer input is added DIRECTLY above an existing one, the
            // previously existing pointer input lambda will be restarted.
            assertEquals(3, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputPressCounter)
            assertEquals(1, originalPointerInputMoveCounter)
            assertEquals(3, originalPointerInputReleaseCounter)

            assertEquals(2, dynamicPointerInputPressCounter)
            assertEquals(1, dynamicPointerInputMoveCounter)
            assertEquals(2, dynamicPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInput2PressCounter)
            assertEquals(1, dynamicPointerInput2ReleaseCounter)
        }
    }

    /* Uses two pointer input blocks and a dynamic background color property to recompose UI.
     * It should NOT restart the pointer input lambdas.
     * Event sequences:
     * 1. "click" (down/up)
     * 2. Assert
     * 3. Recompose
     * 4. Assert
     * 5. Touch down
     * 6. Assert
     * 7. Touch move
     * 8. Assert
     * 9. Touch up
     * 10. Assert
     * 11. Recompose
     * 12. Assert
     */
    @Test
    fun twoPointerInputModifiers_recomposeShouldNotRestartPointerInputLambda() {
        var backgroundModifierColor by mutableStateOf(Color.Red)

        var firstPointerInputLambdaExecutionCount by mutableStateOf(0)
        var firstPointerInputPressCounter by mutableStateOf(0)
        var firstPointerInputMoveCounter by mutableStateOf(0)
        var firstPointerInputReleaseCounter by mutableStateOf(0)

        var secondPointerInputLambdaExecutionCount by mutableStateOf(0)
        var secondPointerInputPressCounter by mutableStateOf(0)
        var secondPointerInputMoveCounter by mutableStateOf(0)
        var secondPointerInputReleaseCounter by mutableStateOf(0)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .pointerInput(Unit) {
                        firstPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        firstPointerInputPressCounter++
                                    }
                                    PointerEventType.Move -> {
                                        firstPointerInputMoveCounter++
                                    }
                                    PointerEventType.Release -> {
                                        firstPointerInputReleaseCounter++
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        secondPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        secondPointerInputPressCounter++
                                    }
                                    PointerEventType.Move -> {
                                        secondPointerInputMoveCounter++
                                    }
                                    PointerEventType.Release -> {
                                        secondPointerInputReleaseCounter++
                                    }
                                }
                            }
                        }
                    }
                    .background(backgroundModifierColor)
            )
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(Color.Red, backgroundModifierColor)

            assertEquals(1, firstPointerInputLambdaExecutionCount)
            assertEquals(1, firstPointerInputPressCounter)
            assertEquals(0, firstPointerInputMoveCounter)
            assertEquals(1, firstPointerInputReleaseCounter)

            assertEquals(1, secondPointerInputLambdaExecutionCount)
            assertEquals(1, secondPointerInputPressCounter)
            assertEquals(0, secondPointerInputMoveCounter)
            assertEquals(1, secondPointerInputReleaseCounter)
        }

        // Recompose
        backgroundModifierColor = Color.Green

        rule.runOnIdle {
            assertEquals(Color.Green, backgroundModifierColor)

            assertEquals(1, firstPointerInputLambdaExecutionCount)
            assertEquals(1, firstPointerInputPressCounter)
            assertEquals(0, firstPointerInputMoveCounter)
            assertEquals(1, firstPointerInputReleaseCounter)

            assertEquals(1, secondPointerInputLambdaExecutionCount)
            assertEquals(1, secondPointerInputPressCounter)
            assertEquals(0, secondPointerInputMoveCounter)
            assertEquals(1, secondPointerInputReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(Offset(0f, 0f)) }

        rule.runOnIdle {
            assertEquals(Color.Green, backgroundModifierColor)

            assertEquals(1, firstPointerInputLambdaExecutionCount)
            assertEquals(2, firstPointerInputPressCounter)
            assertEquals(0, firstPointerInputMoveCounter)
            assertEquals(1, firstPointerInputReleaseCounter)

            assertEquals(1, secondPointerInputLambdaExecutionCount)
            assertEquals(2, secondPointerInputPressCounter)
            assertEquals(0, secondPointerInputMoveCounter)
            assertEquals(1, secondPointerInputReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { moveTo(Offset(1f, 1f)) }

        rule.runOnIdle {
            assertEquals(Color.Green, backgroundModifierColor)

            assertEquals(1, firstPointerInputLambdaExecutionCount)
            assertEquals(2, firstPointerInputPressCounter)
            assertEquals(1, firstPointerInputMoveCounter)
            assertEquals(1, firstPointerInputReleaseCounter)

            assertEquals(1, secondPointerInputLambdaExecutionCount)
            assertEquals(2, secondPointerInputPressCounter)
            assertEquals(1, secondPointerInputMoveCounter)
            assertEquals(1, secondPointerInputReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertEquals(Color.Green, backgroundModifierColor)

            assertEquals(1, firstPointerInputLambdaExecutionCount)
            assertEquals(2, firstPointerInputPressCounter)
            assertEquals(1, firstPointerInputMoveCounter)
            assertEquals(2, firstPointerInputReleaseCounter)

            assertEquals(1, secondPointerInputLambdaExecutionCount)
            assertEquals(2, secondPointerInputPressCounter)
            assertEquals(1, secondPointerInputMoveCounter)
            assertEquals(2, secondPointerInputReleaseCounter)
        }

        // Recompose
        backgroundModifierColor = Color.Red

        rule.runOnIdle {
            assertEquals(Color.Red, backgroundModifierColor)

            assertEquals(1, firstPointerInputLambdaExecutionCount)
            assertEquals(2, firstPointerInputPressCounter)
            assertEquals(1, firstPointerInputMoveCounter)
            assertEquals(2, firstPointerInputReleaseCounter)

            assertEquals(1, secondPointerInputLambdaExecutionCount)
            assertEquals(2, secondPointerInputPressCounter)
            assertEquals(1, secondPointerInputMoveCounter)
            assertEquals(2, secondPointerInputReleaseCounter)
        }
    }

    /* Uses pointer input block for the non-dynamic pointer input and BOTH a clickable{} and
     * pointer input block (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer
     * inputs (both on same Box).
     * Both the dynamic Pointer and clickable{} are disabled to start and then enabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     * 3. Touch down
     * 4. Assert
     * 5. Touch move
     * 6. Assert
     * 7. Touch up
     * 8. Assert
     */
    @Test
    fun dynamicInputAndClickableModifier_addsAbovePointerInputWithUnitKeyTouchEventsWithMove() {
        var activeDynamicClickable by mutableStateOf(false)
        var dynamicClickableCounter by mutableStateOf(0)

        var activeDynamicPointerInput by mutableStateOf(false)
        var dynamicPointerInputPressCounter by mutableStateOf(0)
        var dynamicPointerInputMoveCounter by mutableStateOf(0)
        var dynamicPointerInputReleaseCounter by mutableStateOf(0)

        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputPressCounter by mutableStateOf(0)
        var originalPointerInputMoveCounter by mutableStateOf(0)
        var originalPointerInputReleaseCounter by mutableStateOf(0)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activeDynamicPointerInput,
                        onPress = { dynamicPointerInputPressCounter++ },
                        onMove = { dynamicPointerInputMoveCounter++ },
                        onRelease = {
                            dynamicPointerInputReleaseCounter++
                            activeDynamicClickable = true
                        },
                    )
                    .dynamicClickableModifier(activeDynamicClickable) { dynamicClickableCounter++ }
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        originalPointerInputPressCounter++
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveCounter++
                                    }
                                    PointerEventType.Release -> {
                                        originalPointerInputReleaseCounter++
                                        activeDynamicPointerInput = true
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // Even though we are enabling the dynamic pointer input, it will NOT receive events until
        // the next event stream (after the click is over) which is why you see zeros below.
        // Only two events are triggered for click (down/up)
        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            // With these events, we enable the dynamic pointer input
            assertEquals(1, originalPointerInputPressCounter)
            assertEquals(0, originalPointerInputMoveCounter)
            assertEquals(1, originalPointerInputReleaseCounter)

            assertEquals(0, dynamicPointerInputPressCounter)
            assertEquals(0, dynamicPointerInputMoveCounter)
            assertEquals(0, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicClickableCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(Offset(0f, 0f)) }

        rule.runOnIdle {
            // Each time a new dynamic pointer input is added DIRECTLY above an existing one, the
            // previously existing pointer input lambda will be restarted.
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputPressCounter)
            assertEquals(0, originalPointerInputMoveCounter)
            assertEquals(1, originalPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInputPressCounter)
            assertEquals(0, dynamicPointerInputMoveCounter)
            assertEquals(0, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicClickableCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { moveTo(Offset(1f, 1f)) }

        rule.runOnIdle {
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputPressCounter)
            assertEquals(1, originalPointerInputMoveCounter)
            assertEquals(1, originalPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInputPressCounter)
            assertEquals(1, dynamicPointerInputMoveCounter)
            assertEquals(0, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicClickableCounter)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputPressCounter)
            assertEquals(1, originalPointerInputMoveCounter)
            assertEquals(2, originalPointerInputReleaseCounter)

            assertEquals(1, dynamicPointerInputPressCounter)
            assertEquals(1, dynamicPointerInputMoveCounter)
            // With this release counter, we enable the dynamic clickable{}
            assertEquals(1, dynamicPointerInputReleaseCounter)

            assertEquals(0, dynamicClickableCounter)
        }

        // Only two events are triggered for click (down/up)
        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            // In this case, the lambda is not re-executed because the modifier added before it was
            // not directly a pointer input.
            assertEquals(2, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputPressCounter)
            assertEquals(1, originalPointerInputMoveCounter)
            assertEquals(3, originalPointerInputReleaseCounter)

            assertEquals(2, dynamicPointerInputPressCounter)
            assertEquals(1, dynamicPointerInputMoveCounter)
            assertEquals(2, dynamicPointerInputReleaseCounter)

            assertEquals(1, dynamicClickableCounter)
        }
    }

    /* Uses clickable{} for the non-dynamic pointer input and pointer input
     * block (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer input (both
     * on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse enter
     * 2. Mouse "click()" (press/release)
     * 3. Mouse exit
     * 4. Assert
     */
    @Test
    fun dynamicInputModifier_addsAboveClickableWithKeyMouseEvents_correctEvents() {
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var dynamicPressCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        key = "unique_key_123",
                        onPress = { dynamicPressCounter++ },
                    )
                    .clickable {
                        clickableClickCounter++
                        activateDynamicPointerInput = true
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }

        rule.runOnIdle {
            assertEquals(1, clickableClickCounter)
            assertEquals(0, dynamicPressCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }
        rule.runOnIdle {
            assertEquals(2, clickableClickCounter)
            assertEquals(1, dynamicPressCounter)
        }
    }

    /* Uses clickable{} for the non-dynamic pointer input and pointer input
     * block (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer input (both
     * on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse enter
     * 2. Mouse "click()" (press/release)
     * 3. Mouse exit
     * 4. Assert
     */
    @Test
    fun dynamicInputModifier_addsAboveClickableWithUnitKeyMouseEvents_correctEvents() {
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var dynamicPressCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onPress = { dynamicPressCounter++ },
                    )
                    .clickable {
                        clickableClickCounter++
                        activateDynamicPointerInput = true
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }

        rule.runOnIdle {
            assertEquals(1, clickableClickCounter)
            assertEquals(0, dynamicPressCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter()
            click()
            exit()
        }
        rule.runOnIdle {
            assertEquals(2, clickableClickCounter)
            assertEquals(1, dynamicPressCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for both the
     * non-dynamic pointer input and the dynamic pointer input (both on same Box).
     *
     * Tests dynamically adding a pointer input ABOVE an existing pointer input DURING an
     * event stream (specifically, Hover).
     *
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse enter
     * 2. Assert
     * 3. Mouse press
     * 4. Assert
     * 5. Mouse release
     * 6. Assert
     * 7. Mouse exit
     * 8. Assert
     */
    @Test
    fun dynamicInputModifier_addsAbovePointerInputWithUnitKeyMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEventCounter by mutableStateOf(0)

        var dynamicPressCounter by mutableStateOf(0)
        var dynamicReleaseCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onPress = { dynamicPressCounter++ },
                        onRelease = { dynamicReleaseCounter++ },
                    )
                    .background(Color.Green)
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                originalPointerInputEventCounter++
                                activateDynamicPointerInput = true
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEventCounter)
            assertEquals(0, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { press() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { release() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(4, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }
    }

    /* Uses clickable{} for the non-dynamic pointer input and pointer input
     * block (awaitPointerEventScope + awaitPointerEvent) for the dynamic pointer input (both
     * on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputModifier_addsAboveClickableIncompleteMouseEvents_correctEvents() {
        var clickableClickCounter by mutableStateOf(0)
        // Note: I'm tracking press instead of release because clickable{} consumes release
        var dynamicPressCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onPress = { dynamicPressCounter++ },
                    )
                    .clickable {
                        clickableClickCounter++
                        activateDynamicPointerInput = true
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertEquals(1, clickableClickCounter)
            assertEquals(0, dynamicPressCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertEquals(2, clickableClickCounter)
            assertEquals(1, dynamicPressCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for both the
     * non-dynamic pointer input and the dynamic pointer input (both on same Box).
     *
     * Tests dynamically adding a pointer input AFTER an existing pointer input DURING an
     * event stream (specifically, Hover).
     * Hover is the only scenario where you can add a new pointer input modifier during the event
     * stream AND receive events in the same active stream from that new pointer input modifier.
     * It isn't possible in the down/up scenario because you add the new modifier during the down
     * but you don't get another down until the next event stream.
     *
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse enter
     * 2. Assert
     * 3. Mouse press
     * 4. Assert
     * 5. Mouse release
     * 6. Assert
     * 7. Mouse exit
     * 8. Assert
     */
    @Test
    fun dynamicInputModifier_addsBelowPointerInputWithUnitKeyMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEventCounter by mutableStateOf(0)

        var dynamicPressCounter by mutableStateOf(0)
        var dynamicReleaseCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .background(Color.Green)
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                originalPointerInputEventCounter++
                                activateDynamicPointerInput = true
                            }
                        }
                    }
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onPress = { dynamicPressCounter++ },
                        onRelease = { dynamicReleaseCounter++ },
                    )
            )
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEventCounter)
            assertEquals(0, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { press() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            // Both the original and enabled dynamic pointer input modifiers will get the event
            // since they are on the same Box.
            assertEquals(2, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput {
            assertTrue(activateDynamicPointerInput)
            release()
        }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(4, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for both the
     * non-dynamic pointer input and the dynamic pointer input (both on same Box).
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputModifier_addsBelowPointerInputUnitKeyIncompleteMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEventCounter by mutableStateOf(0)

        var dynamicPressCounter by mutableStateOf(0)
        var dynamicReleaseCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .background(Color.Green)
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                originalPointerInputEventCounter++
                                activateDynamicPointerInput = true
                            }
                        }
                    }
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onPress = { dynamicPressCounter++ },
                        onRelease = { dynamicReleaseCounter++ },
                    )
            )
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter) // Enter, Press, Release
            assertEquals(0, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            // Because the mouse is still within the box area, Compose doesn't need to trigger an
            // Exit. Instead, it just triggers two events (Press and Release) which is why the
            // total is only 5.
            assertEquals(5, originalPointerInputEventCounter) // Press, Release
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }
    }

    /* The next set of tests uses two nested boxes inside a box. The two nested boxes each contain
     * their own pointer input modifier (vs. the tests above that apply two pointer input modifiers
     * to the same box).
     */

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for both the
     * non-dynamic pointer input and the dynamic pointer input.
     * The Dynamic Pointer is disabled to start and then enabled.
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBox_addsBelowPointerInputUnitKeyIncompleteMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEventCounter by mutableStateOf(0)

        var dynamicPressCounter by mutableStateOf(0)
        var dynamicReleaseCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                originalPointerInputEventCounter++
                                activateDynamicPointerInput = true
                            }
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifier(
                            enabled = activateDynamicPointerInput,
                            onPress = { dynamicPressCounter++ },
                            onRelease = { dynamicReleaseCounter++ },
                        )
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter)
            assertEquals(0, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }
    }

    /* Uses pointer input block (awaitPointerEventScope + awaitPointerEvent) for both the
     * non-dynamic pointer input and the dynamic pointer input.
     * The Dynamic Pointer is disabled to start, then enabled, and finally disabled.
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBox_togglesBelowPointerInputUnitKeyIncompleteMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEventCounter by mutableStateOf(0)

        var dynamicPressCounter by mutableStateOf(0)
        var dynamicReleaseCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                originalPointerInputEventCounter++

                                // Note: We only set the activateDynamicPointerInput to true on
                                // Release because we do not want it set on just any event.
                                // Specifically, we do not want it set on Exit, because, in the
                                // case of this event, the exit will be triggered around the same
                                // time as the other dynamic pointer input receives a press (when
                                // it is enabled) because, as soon as that gets that event, Compose
                                // sees this box no longer the hit target (the box with the dynamic
                                // pointer input is now), so it triggers an exit on this original
                                // non-dynamic pointer input. If we allowed
                                // activateDynamicPointerInput to be set during any event, it would
                                // undo us setting activateDynamicPointerInput to false in the other
                                // pointer input handler.
                                if (event.type == PointerEventType.Release) {
                                    activateDynamicPointerInput = true
                                }
                            }
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Cyan)
                        .dynamicPointerInputModifier(
                            enabled = activateDynamicPointerInput,
                            onPress = { dynamicPressCounter++ },
                            onRelease = {
                                dynamicReleaseCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter)
            assertEquals(0, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertFalse(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(3, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }
    }

    /* Uses Foundation's detectTapGestures{} for the non-dynamic pointer input. The dynamic pointer
     * input uses the lower level pointer input commands.
     * The Dynamic Pointer is disabled to start, then enabled, and finally disabled.
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBoxGesture_togglesBelowWithUnitKeyIncompleteMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEventCounter by mutableStateOf(0)

        var dynamicPressCounter by mutableStateOf(0)
        var dynamicReleaseCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        detectTapGestures {
                            originalPointerInputEventCounter++
                            activateDynamicPointerInput = true
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifier(
                            enabled = activateDynamicPointerInput,
                            onPress = { dynamicPressCounter++ },
                            onRelease = {
                                dynamicReleaseCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEventCounter)
            assertEquals(0, dynamicPressCounter)
            assertEquals(0, dynamicReleaseCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertFalse(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEventCounter)
            assertEquals(1, dynamicPressCounter)
            assertEquals(1, dynamicReleaseCounter)
        }
    }

    /* Uses Foundation's detectTapGestures{} for both the non-dynamic pointer input and the
     * dynamic pointer input (vs. the lower level pointer input commands).
     * The Dynamic Pointer is disabled to start, then enabled, and finally disabled.
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBoxGesture_togglesBelowWithKeyTouchEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputTapGestureCounter by mutableStateOf(0)

        var dynamicTapGestureCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput("myUniqueKey1") {
                        originalPointerInputLambdaExecutionCount++

                        detectTapGestures {
                            originalPointerInputTapGestureCounter++
                            activateDynamicPointerInput = true
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifierWithDetectTapGestures(
                            enabled = activateDynamicPointerInput,
                            key = "myUniqueKey2",
                            onTap = {
                                dynamicTapGestureCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        // This command is the same as
        // rule.onNodeWithTag("myClickable").performTouchInput { click() }
        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputTapGestureCounter)
            assertEquals(0, dynamicTapGestureCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputTapGestureCounter)
            assertFalse(activateDynamicPointerInput)
            assertEquals(1, dynamicTapGestureCounter)
        }
    }

    /*
     * The next four tests are based on the test above (nested boxes using a pointer input
     * modifier blocks with the Foundation Gesture detectTapGestures{}).
     *
     * The difference is the dynamic pointer input modifier is enabled to start (while in the
     * other tests it is disabled to start).
     *
     * The tests below tests out variations (mouse vs. touch and Unit keys vs. unique keys).
     */

    /* Dynamic Pointer enabled to start, disabled, then re-enabled (uses UNIQUE key)
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBoxGesture_togglesBelowOffWithKeyTouchEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputTapGestureCounter by mutableStateOf(0)

        var dynamicTapGestureCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput("myUniqueKey1") {
                        originalPointerInputLambdaExecutionCount++

                        detectTapGestures {
                            originalPointerInputTapGestureCounter++
                            activateDynamicPointerInput = true
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifierWithDetectTapGestures(
                            enabled = activateDynamicPointerInput,
                            key = "myUniqueKey2",
                            onTap = {
                                dynamicTapGestureCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertTrue(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertFalse(activateDynamicPointerInput)
            assertEquals(0, originalPointerInputLambdaExecutionCount)
            assertEquals(0, originalPointerInputTapGestureCounter)
            // Since second box is created following first box, it will get the event
            assertEquals(1, dynamicTapGestureCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputTapGestureCounter)
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, dynamicTapGestureCounter)
        }
    }

    /* Dynamic Pointer enabled to start, disabled, then re-enabled (uses UNIT for key)
     * Event sequences:
     * 1. Touch "click" (down/move/up)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBoxGesture_togglesBelowOffWithUnitKeyTouchEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputTapGestureCounter by mutableStateOf(0)

        var dynamicTapGestureCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++

                        detectTapGestures {
                            originalPointerInputTapGestureCounter++
                            activateDynamicPointerInput = true
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifierWithDetectTapGestures(
                            enabled = activateDynamicPointerInput,
                            key = Unit,
                            onTap = {
                                dynamicTapGestureCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertTrue(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertFalse(activateDynamicPointerInput)
            assertEquals(0, originalPointerInputLambdaExecutionCount)
            assertEquals(0, originalPointerInputTapGestureCounter)
            // Since second box is created following first box, it will get the event
            assertEquals(1, dynamicTapGestureCounter)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputTapGestureCounter)
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, dynamicTapGestureCounter)
        }
    }

    /* Dynamic Pointer enabled to start, disabled, then re-enabled (uses UNIQUE key)
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBoxGesture_togglesBelowOffWithKeyIncompleteMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputTapGestureCounter by mutableStateOf(0)

        var dynamicTapGestureCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput("myUniqueKey1") {
                        originalPointerInputLambdaExecutionCount++

                        detectTapGestures {
                            originalPointerInputTapGestureCounter++
                            activateDynamicPointerInput = true
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifierWithDetectTapGestures(
                            enabled = activateDynamicPointerInput,
                            key = "myUniqueKey2",
                            onTap = {
                                dynamicTapGestureCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertTrue(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertFalse(activateDynamicPointerInput)
            assertEquals(0, originalPointerInputLambdaExecutionCount)
            assertEquals(0, originalPointerInputTapGestureCounter)
            // Since second box is created following first box, it will get the event
            assertEquals(1, dynamicTapGestureCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputTapGestureCounter)
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, dynamicTapGestureCounter)
        }
    }

    /* Dynamic Pointer enabled to start, disabled, then re-enabled (uses Unit for key)
     * Event sequences:
     * 1. Mouse "click" (incomplete [down/up only], does not include expected hover in/out)
     * 2. Assert
     */
    @Test
    fun dynamicInputNestedBoxGesture_togglesBelowOffUnitKeyIncompleteMouseEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputTapGestureCounter by mutableStateOf(0)

        var dynamicTapGestureCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++

                        detectTapGestures {
                            originalPointerInputTapGestureCounter++
                            activateDynamicPointerInput = true
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifierWithDetectTapGestures(
                            enabled = activateDynamicPointerInput,
                            key = Unit,
                            onTap = {
                                dynamicTapGestureCounter++
                                activateDynamicPointerInput = false
                            },
                        )
                )
            }
        }

        rule.runOnIdle { assertTrue(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertFalse(activateDynamicPointerInput)
            assertEquals(0, originalPointerInputLambdaExecutionCount)
            assertEquals(0, originalPointerInputTapGestureCounter)
            // Since second box is created following first box, it will get the event
            assertEquals(1, dynamicTapGestureCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { click() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputTapGestureCounter)
            assertTrue(activateDynamicPointerInput)
            assertEquals(1, dynamicTapGestureCounter)
        }
    }

    // !!!!! MOUSE & TOUCH EVENTS TESTS WITH DYNAMIC MODIFIER INPUT TESTS SECTION (END) !!!!!

    // !!!!! HOVER EVENTS ONLY WITH DYNAMIC MODIFIER INPUT TESTS SECTION (START) !!!!!
    /* These tests dynamically add a pointer input BEFORE or AFTER an existing pointer input DURING
     * an event stream (specifically, Hover). Some tests use unique keys while others use UNIT as
     * the key. Finally, some of the tests apply the modifiers to the same Box while others use
     * sibling blocks (read the test name for details).
     *
     * Test name explains the test.
     * All tests start with the dynamic pointer disabled and enable it on the first hover enter
     *
     * Event sequences:
     * 1. Hover enter
     * 2. Assert
     * 3. Move
     * 4. Assert
     * 5. Move
     * 6. Assert
     * 7. Hover exit
     * 8. Assert
     */

    @Test
    fun dynamicInputModifier_addsAbovePointerInputWithUnitKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onEnter = { dynamicEnterCounter++ },
                        onMove = { dynamicMoveCounter++ },
                        onExit = { dynamicExitCounter++ },
                    )
                    .background(Color.Green)
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        // Because the dynamic pointer input is added after the "enter" event (and it is part of the
        // same modifier chain), it will receive events as well now.
        // (Because the dynamic modifier is added BEFORE the original modifier, it WILL reset the
        // event stream for the original modifier.)
        // Original pointer input:  Hover Exit event then a Hover Enter event
        // Dynamic pointer input:   Hover enter event
        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(2, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(1, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputModifier_addsAbovePointerInputWithKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .dynamicPointerInputModifier(
                        key = "unique_key_1234",
                        enabled = activateDynamicPointerInput,
                        onEnter = { dynamicEnterCounter++ },
                        onMove = { dynamicMoveCounter++ },
                        onExit = { dynamicExitCounter++ },
                    )
                    .background(Color.Green)
                    .pointerInput("unique_key_5678") {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        // Because the dynamic pointer input is added after the "enter" event (and it is part of the
        // same modifier chain), it will receive events as well now.
        // (Because the dynamic modifier is added BEFORE the original modifier, it WILL reset the
        // event stream for the original modifier.)
        // Original pointer input:  Hover Exit event then a Hover Enter event
        // Dynamic pointer input:   Hover enter event
        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(2, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(2, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(1, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputModifier_addsBelowPointerInputWithUnitKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
                    .background(Color.Green)
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        onEnter = { dynamicEnterCounter++ },
                        onMove = { dynamicMoveCounter++ },
                        onExit = { dynamicExitCounter++ },
                    )
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        // Because the dynamic pointer input is added after the "enter" event (and it is part of the
        // same modifier chain), it will receive events as well now.
        // (Because the dynamic modifier is added BELOW the original modifier, it will not reset the
        // event stream for the original modifier.)
        // Original pointer input:  Move event
        // Dynamic pointer input:   Hover enter event
        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(1, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputModifier_addsBelowPointerInputWithKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.size(200.dp)
                    .testTag("myClickable")
                    .pointerInput("unique_key_5678") {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
                    .background(Color.Green)
                    .dynamicPointerInputModifier(
                        key = "unique_key_1234",
                        enabled = activateDynamicPointerInput,
                        onEnter = { dynamicEnterCounter++ },
                        onMove = { dynamicMoveCounter++ },
                        onExit = { dynamicExitCounter++ },
                    )
            )
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        // Because the dynamic pointer input is added after the "enter" event (and it is part of the
        // same modifier chain), it will receive events as well now.
        // (Because the dynamic modifier is added BELOW the original modifier, it will not reset the
        // event stream for the original modifier.)
        // Original pointer input:  Move event
        // Dynamic pointer input:   Hover enter event
        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(1, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputNestedBox_addsBelowPointerInputUnitKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifier(
                            enabled = activateDynamicPointerInput,
                            onEnter = { dynamicEnterCounter++ },
                            onMove = { dynamicMoveCounter++ },
                            onExit = { dynamicExitCounter++ },
                        )
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(1, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputNestedBox_addsBelowPointerInputKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput("unique_key_1234") {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
                )
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifier(
                            key = "unique_key_5678",
                            enabled = activateDynamicPointerInput,
                            onEnter = { dynamicEnterCounter++ },
                            onMove = { dynamicMoveCounter++ },
                            onExit = { dynamicExitCounter++ },
                        )
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(1, dynamicEnterCounter)
            assertEquals(1, dynamicMoveCounter)
            assertEquals(1, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputNestedBox_addsAbovePointerInputUnitKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifier(
                            enabled = activateDynamicPointerInput,
                            onEnter = { dynamicEnterCounter++ },
                            onMove = { dynamicMoveCounter++ },
                            onExit = { dynamicExitCounter++ },
                        )
                )
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput(Unit) {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }
    }

    @Test
    fun dynamicInputNestedBox_addsAbovePointerInputKeyHoverEvents_correctEvents() {
        var originalPointerInputLambdaExecutionCount by mutableStateOf(0)
        var originalPointerInputEnterEventCounter by mutableStateOf(0)
        var originalPointerInputMoveEventCounter by mutableStateOf(0)
        var originalPointerInputExitEventCounter by mutableStateOf(0)

        var dynamicEnterCounter by mutableStateOf(0)
        var dynamicMoveCounter by mutableStateOf(0)
        var dynamicExitCounter by mutableStateOf(0)
        var activateDynamicPointerInput by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.size(100.dp).testTag("myClickable")) {
                Box(
                    Modifier.fillMaxSize()
                        .dynamicPointerInputModifier(
                            key = "unique_key_5678",
                            enabled = activateDynamicPointerInput,
                            onEnter = { dynamicEnterCounter++ },
                            onMove = { dynamicMoveCounter++ },
                            onExit = { dynamicExitCounter++ },
                        )
                )
                Box(
                    Modifier.fillMaxSize().background(Color.Green).pointerInput("unique_key_1234") {
                        originalPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        originalPointerInputEnterEventCounter++
                                        activateDynamicPointerInput = true
                                    }
                                    PointerEventType.Move -> {
                                        originalPointerInputMoveEventCounter++
                                    }
                                    PointerEventType.Exit -> {
                                        originalPointerInputExitEventCounter++
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        rule.runOnIdle { assertFalse(activateDynamicPointerInput) }

        rule.onNodeWithTag("myClickable").performMouseInput { enter() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(0, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(1, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { moveBy(Offset(1.0f, 1.0f)) }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(0, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit() }

        rule.runOnIdle {
            assertEquals(1, originalPointerInputLambdaExecutionCount)
            assertEquals(1, originalPointerInputEnterEventCounter)
            assertEquals(2, originalPointerInputMoveEventCounter)
            assertEquals(1, originalPointerInputExitEventCounter)
            assertEquals(0, dynamicEnterCounter)
            assertEquals(0, dynamicMoveCounter)
            assertEquals(0, dynamicExitCounter)
        }
    }

    // !!!!! HOVER EVENTS ONLY WITH DYNAMIC MODIFIER INPUT TESTS SECTION (END) !!!!!

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
                modifier =
                    Modifier.testTag("myClickable")
                        .clickable(
                            enabled = enabled.value,
                            onClick = {},
                            interactionSource = interactionSource,
                            indication = null,
                        ),
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            interactions.clear()
            enabled.value = false
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle { enabled.value = true }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }
    }

    @Test
    fun noFocus_whenDisabled() {
        val requester = FocusRequester()
        // Force clickable to always be in non-touch mode, so it should be focusable
        val keyboardMockManager =
            object : InputModeManager {
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
                        modifier =
                            Modifier.testTag("myClickable")
                                .focusRequester(requester)
                                .onFocusEvent { focusState = it }
                                .clickable(enabled = enabled.value) {},
                    )
                }
            }
        }

        rule.runOnIdle {
            requester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        rule.runOnIdle { enabled.value = false }

        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    /** Test for b/269319898 */
    @Test
    fun noFocusPropertiesSet_whenDisabled() {
        val requester = FocusRequester()
        // Force clickable to always be in non-touch mode, so it should be focusable
        val keyboardMockManager =
            object : InputModeManager {
                override val inputMode = Keyboard

                override fun requestInputMode(inputMode: InputMode) = true
            }

        val enabled = mutableStateOf(true)
        lateinit var focusState: FocusState

        rule.setContent {
            CompositionLocalProvider(LocalInputModeManager provides keyboardMockManager) {
                Box(Modifier.clickable(enabled = enabled.value, onClick = {})) {
                    Box(
                        Modifier.size(10.dp)
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

        rule.runOnIdle { enabled.value = false }

        rule.runOnIdle {
            // Clickable is disabled, it should not apply properties down the tree
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun testInspectorValue_noIndicationOverload() {
        val onClick: () -> Unit = {}
        rule.setContent {
            val modifier = Modifier.clickable(onClick = onClick) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("clickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "enabled",
                    "onClickLabel",
                    "role",
                    "interactionSource",
                    "indicationNodeFactory",
                    "onClick",
                )
        }
    }

    @Test
    fun testInspectorValue_fullParamsOverload() {
        val onClick: () -> Unit = {}
        rule.setContent {
            val modifier =
                Modifier.clickable(
                        onClick = onClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    )
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("clickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "enabled",
                    "onClickLabel",
                    "onClick",
                    "role",
                    "indicationNodeFactory",
                    "interactionSource",
                )
        }
    }

    // integration test for b/184872415
    @Test
    fun tapGestureTest_tryAwaitRelease_ReturnsTrue() {
        val wasSuccess = mutableStateOf(false)
        rule.setContent {
            Box(
                Modifier.size(100.dp).testTag("myClickable").pointerInput(Unit) {
                    detectTapGestures(onPress = { wasSuccess.value = tryAwaitRelease() })
                }
            )
        }

        rule.onNodeWithTag("myClickable").performClick()

        assertThat(wasSuccess.value).isTrue()
    }

    @Test
    fun clickInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier.requiredHeight(20.dp)
                    .requiredWidth(20.dp)
                    .clipToBounds()
                    .clickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performTouchInput { click(Offset(-1f, -1f)) }

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    fun clickInVerticalTargetInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier.requiredHeight(50.dp)
                    .requiredWidth(20.dp)
                    .clipToBounds()
                    .clickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(50.dp)
            .assertTouchHeightIsEqualTo(50.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performTouchInput { click(Offset(-1f, 0f)) }

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    fun clickInHorizontalTargetInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier.requiredHeight(20.dp)
                    .requiredWidth(50.dp)
                    .clipToBounds()
                    .clickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(50.dp)
            .performTouchInput { click(Offset(0f, -1f)) }

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun otherKey_doesNotEmitInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").performKeyInput { pressKey(Key.Backspace) }
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Ignore("Needs aosp/3542299 to land to avoid teardown exceptions from crashing the test")
    @Test
    fun localIndication_indication_crashes() {
        val indication = TestIndication {}
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent {
                CompositionLocalProvider(LocalIndication provides indication) {
                    Box(Modifier.padding(10.dp)) {
                        BasicText(
                            "ClickableText",
                            modifier = Modifier.testTag("clickable").clickable {},
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun localIndication_indication_flagDisabled_stillSupported() {
        try {
            var created = false
            val indication = TestIndication { created = true }
            ComposeFoundationFlags.isNonComposedClickableEnabled = false
            rule.setContent {
                CompositionLocalProvider(LocalIndication provides indication) {
                    Box(Modifier.padding(10.dp)) {
                        BasicText(
                            "ClickableText",
                            modifier = Modifier.testTag("clickable").clickable {},
                        )
                    }
                }
            }
            rule.runOnIdle { assertThat(created).isTrue() }
        } finally {
            ComposeFoundationFlags.isNonComposedClickableEnabled = true
        }
    }

    @Test
    fun localIndication_interactionSource_eagerlyCreated() {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(
                                interactionSource = interactionSource
                            ) {},
                    )
                }
            }
        }
        rule.runOnIdle { assertThat(created).isTrue() }
    }

    @Test
    fun localIndication_noInteractionSource_lazilyCreated_pointerInput() {
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

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(interactionSource = null) {},
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun localIndication_noInteractionSource_lazilyCreated_focus() {
        var created = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
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

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                inputModeManager = LocalInputModeManager.current
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                                interactionSource = null
                            ) {},
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

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
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun localIndication_noInteractionSource_lazilyCreated_keyInput() {
        var created = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
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

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                inputModeManager = LocalInputModeManager.current
                // Add focusable to the top so that when initial focus is dispatched, the clickable
                // doesn't become focused
                Box(Modifier.padding(10.dp).focusable()) {
                    Box(
                        modifier =
                            Modifier.testTag("clickable").clickable(interactionSource = null) {}
                    ) {
                        Box(Modifier.focusRequester(focusRequester).focusable())
                    }
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
     * Test case for initializing indication when an IndirectTouchEvent is received. Focus is
     * required for indirect touch, so normally just focusing the clickable will cause indication to
     * be initialized via focus logic, but if a focused child receives indirect touch and doesn't
     * consume it, it will still be passed up to a non-focused parent, so we test this scenario here
     * and make sure that this indirect touch event bubbling up causes indication to be created.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun localIndication_noInteractionSource_lazilyCreated_indirectTouch() {
        var created = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
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

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                inputModeManager = LocalInputModeManager.current
                // Add focusable to the top so that when initial focus is dispatched, the clickable
                // doesn't become focused
                Box(Modifier.padding(10.dp).focusable()) {
                    Box(
                        modifier =
                            Modifier.testTag("clickable").clickable(interactionSource = null) {}
                    ) {
                        Box(Modifier.focusRequester(focusRequester).focusable())
                    }
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

        // The indirect touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").sendIndirectTouchPressEvent(0L, Offset.Zero)

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for changing from an IndicationNodeFactory instance to an Indication instance - we
     * should crash as Indication is not supported.
     */
    @Test
    fun localIndication_changingIndicationNodeFactoryToIndication_interactionSource_crashes() {
        var nodeCreated = false
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()
        val testIndicationNodeFactory = TestIndicationNodeFactory { _, coroutineScope ->
            nodeCreated = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }
        val testIndication = TestIndication {}

        var indication: Indication by mutableStateOf(testIndicationNodeFactory)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(
                                interactionSource = interactionSource
                            ) {},
                    )
                }
            }
        }

        val exception =
            assertFailsWith(IllegalArgumentException::class) {
                rule.runOnIdle {
                    assertThat(nodeCreated).isTrue()
                    indication = testIndication
                }

                rule.waitForIdle()
            }

        assertThat(exception.message)
            .startsWith("clickable only supports IndicationNodeFactory instances")
    }

    /**
     * Test case for null InteractionSource with a provided indication: the indication should be
     * lazily created. If we change indication before creation, the new indication should be created
     * lazily too.
     */
    @Test
    fun localIndication_changingIndication_beforeCreation() {
        var created1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created1 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var indication by mutableStateOf(indication1)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(interactionSource = null) {},
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created1).isFalse() }

        rule.runOnIdle { indication = indication2 }

        rule.runOnIdle {
            // We should still not be created
            assertThat(created2).isFalse()
        }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created2).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for null InteractionSource with a provided indication: the indication should be
     * lazily created, but then if we change indication after creation, the new indication should be
     * created immediately
     */
    @Test
    fun localIndication_changingIndication_afterCreation() {
        var created1 = false
        var detached1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 =
            TestIndicationNodeFactory(onDetach = { detached1 = true }) { source, coroutineScope ->
                interactionSource = source
                created1 = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var indication by mutableStateOf(indication1)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(interactionSource = null) {},
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created1).isFalse() }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created1).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performTouchInput { up() }

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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
    fun localIndication_changingInteractionSourceToAnotherInteractionSource() {
        var created = false
        var detached = false
        var interactionSource: MutableInteractionSource by
            mutableStateOf(MutableInteractionSource())
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detached = true }) { _, coroutineScope ->
                created = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(
                                interactionSource = interactionSource
                            ) {},
                    )
                }
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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
    fun localIndication_changingInteractionSourceFromNull() {
        var created = false
        var interactionSource: MutableInteractionSource? by mutableStateOf(null)
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { _, coroutineScope ->
            created = true
            coroutineScope.launch {
                interactionSource!!.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(
                                interactionSource = interactionSource
                            ) {},
                    )
                }
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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
    fun localIndication_changingInteractionSourceToNull() {
        var created = false
        var detached = false
        var interactionSource: MutableInteractionSource? by
            mutableStateOf(MutableInteractionSource())
        var internalInteractionSource: InteractionSource?
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detached = true }) { source, coroutineScope ->
                internalInteractionSource = source
                created = true
                coroutineScope.launch {
                    internalInteractionSource?.interactions?.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(
                                interactionSource = interactionSource
                            ) {},
                    )
                }
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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun localIndication_nullInteractionSource_resetWhenReused_pressed() {
        var attachedCount = 0
        var detachedCount = 0
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var key by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box {
                    ReusableContent(key) {
                        BasicText(
                            "ClickableText",
                            modifier =
                                Modifier.testTag("clickable").clickable(interactionSource = null) {},
                        )
                    }
                }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Change the key to trigger reuse
        rule.runOnIdle {
            interactions.clear()
            key = false
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Cancel interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        // The touch event should cause a new indication node and interaction source to be created
        rule.onNodeWithTag("clickable").performTouchInput {
            // Need to reset the previous down
            up()
            down(center)
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new press
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun localIndication_nullInteractionSource_resetWhenReused_focused() {
        var attachedCount = 0
        var detachedCount = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var key by mutableStateOf(true)

        // setFocusableContent so when we are reused, the focus system won't automatically try and
        // set focus on us again
        rule.setFocusableContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                inputModeManager = LocalInputModeManager.current
                Box {
                    ReusableContent(key) {
                        BasicText(
                            "ClickableText",
                            modifier =
                                Modifier.testTag("clickable")
                                    .focusRequester(focusRequester)
                                    .clickable(interactionSource = null) {},
                        )
                    }
                }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        rule.runOnIdle {
            // Clickable is only focusable in non-touch mode
            inputModeManager.requestInputMode(Keyboard)
            // The focus event should cause the indication node to be created
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Change the key to trigger reuse
        rule.runOnIdle {
            interactions.clear()
            key = false
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Unfocus interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        rule.runOnIdle {
            // Request focus again
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new focus interaction
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }
    }

    @Test
    fun localIndication_nullInteractionSource_resetWhenMoved_pressed() {
        var attachedCount = 0
        var detachedCount = 0
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var moveContent by mutableStateOf(false)

        val content = movableContentOf {
            CompositionLocalProvider(LocalIndication provides indication) {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("clickable").clickable(interactionSource = null) {},
                )
            }
        }

        rule.setContent {
            if (moveContent) {
                Box { content() }
            } else {
                Box { content() }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Move the content
        rule.runOnIdle {
            interactions.clear()
            moveContent = true
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Unfocus interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        // The touch event should cause a new indication node and interaction source to be created
        rule.onNodeWithTag("clickable").performTouchInput {
            // Need to reset the previous down
            up()
            down(center)
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new press
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun localIndication_nullInteractionSource_resetWhenMoved_focused() {
        var attachedCount = 0
        var detachedCount = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var moveContent by mutableStateOf(false)

        val content = movableContentOf {
            CompositionLocalProvider(LocalIndication provides indication) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                            interactionSource = null
                        ) {},
                )
            }
        }

        // setFocusableContent so when we are reused, the focus system won't automatically try and
        // set focus on us again
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            if (moveContent) {
                Box { content() }
            } else {
                Box { content() }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        rule.runOnIdle {
            // Clickable is only focusable in non-touch mode
            inputModeManager.requestInputMode(Keyboard)
            // The focus event should cause the indication node to be created
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Move the content
        rule.runOnIdle {
            interactions.clear()
            moveContent = true
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Unfocus interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        rule.runOnIdle {
            // Request focus again
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new focus interaction
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }
    }

    @Test
    fun indication_interactionSource_eagerlyCreated() {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndication { created = true }
        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = interactionSource,
                            indication = indication,
                        ) {},
                )
            }
        }
        rule.runOnIdle { assertThat(created).isTrue() }
    }

    @Test
    fun indicationNodeFactory_interactionSource_eagerlyCreated() {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = interactionSource,
                            indication = indication,
                        ) {},
                )
            }
        }
        rule.runOnIdle { assertThat(created).isTrue() }
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
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

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
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

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
    @OptIn(ExperimentalTestApi::class)
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
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            // Add focusable to the top so that when initial focus is dispatched, the clickable
            // doesn't become focused
            Box(Modifier.padding(10.dp).focusable()) {
                Box(
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = null,
                            indication = indication,
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
     * Test case for initializing indication when an IndirectTouchEvent is received. Focus is
     * required for indirect touch, so normally just focusing the clickable will cause indication to
     * be initialized via focus logic, but if a focused child receives indirect touch and doesn't
     * consume it, it will still be passed up to a non-focused parent, so we test this scenario here
     * and make sure that this indirect touch event bubbling up causes indication to be created.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun indicationNodeFactory_noInteractionSource_lazilyCreated_indirectTouch() {
        var created = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
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

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            // Add focusable to the top so that when initial focus is dispatched, the clickable
            // doesn't become focused
            Box(Modifier.padding(10.dp).focusable()) {
                Box(
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = null,
                            indication = indication,
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

        // The indirect touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").sendIndirectTouchPressEvent(0L, Offset.Zero)

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
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var indication: Indication by mutableStateOf(testIndication)

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = interactionSource,
                            indication = indication,
                        ) {},
                )
            }
        }

        rule.runOnIdle {
            assertThat(indicationCreated).isTrue()
            indication = testIndicationNodeFactory
        }

        rule.runOnIdle { assertThat(nodeCreated).isTrue() }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var indication by mutableStateOf(indication1)

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        rule.runOnIdle { assertThat(created1).isFalse() }

        rule.runOnIdle { indication = indication2 }

        rule.runOnIdle {
            // We should still not be created
            assertThat(created2).isFalse()
        }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created2).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for null InteractionSource with a provided indication: the indication should be
     * lazily created, but then if we change indication after creation, the new indication should be
     * created immediately
     */
    @Test
    fun indicationNodeFactory_changingIndication_afterCreation() {
        var created1 = false
        var detached1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 =
            TestIndicationNodeFactory(onDetach = { detached1 = true }) { source, coroutineScope ->
                interactionSource = source
                created1 = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var indication by mutableStateOf(indication1)

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = null,
                            indication = indication,
                        ) {},
                )
            }
        }

        rule.runOnIdle { assertThat(created1).isFalse() }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created1).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performTouchInput { up() }

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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
        var interactionSource: MutableInteractionSource by
            mutableStateOf(MutableInteractionSource())
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detached = true }) { _, coroutineScope ->
                created = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = interactionSource,
                            indication = indication,
                        ) {},
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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
                interactionSource!!.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = interactionSource,
                            indication = indication,
                        ) {},
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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

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
        var interactionSource: MutableInteractionSource? by
            mutableStateOf(MutableInteractionSource())
        var internalInteractionSource: InteractionSource?
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detached = true }) { source, coroutineScope ->
                internalInteractionSource = source
                created = true
                coroutineScope.launch {
                    internalInteractionSource?.interactions?.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        rule.setContent {
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable").clickable(
                            interactionSource = interactionSource,
                            indication = indication,
                        ) {},
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

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun indicationNodeFactory_nullInteractionSource_resetWhenReused_pressed() {
        var attachedCount = 0
        var detachedCount = 0
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var key by mutableStateOf(true)

        rule.setContent {
            Box {
                ReusableContent(key) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").clickable(
                                interactionSource = null,
                                indication = indication,
                            ) {},
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Change the key to trigger reuse
        rule.runOnIdle {
            interactions.clear()
            key = false
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Cancel interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        // The touch event should cause a new indication node and interaction source to be created
        rule.onNodeWithTag("clickable").performTouchInput {
            // Need to reset the previous down
            up()
            down(center)
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new press
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun indicationNodeFactory_nullInteractionSource_resetWhenReused_focused() {
        var attachedCount = 0
        var detachedCount = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var key by mutableStateOf(true)

        // setFocusableContent so when we are reused, the focus system won't automatically try and
        // set focus on us again
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            Box {
                ReusableContent(key) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                                interactionSource = null,
                                indication = indication,
                            ) {},
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        rule.runOnIdle {
            // Clickable is only focusable in non-touch mode
            inputModeManager.requestInputMode(Keyboard)
            // The focus event should cause the indication node to be created
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Change the key to trigger reuse
        rule.runOnIdle {
            interactions.clear()
            key = false
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Unfocus interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        rule.runOnIdle {
            // Request focus again
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new focus interaction
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }
    }

    @Test
    fun indicationNodeFactory_nullInteractionSource_resetWhenMoved_pressed() {
        var attachedCount = 0
        var detachedCount = 0
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var moveContent by mutableStateOf(false)

        val content = movableContentOf {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("clickable").clickable(
                        interactionSource = null,
                        indication = indication,
                    ) {},
            )
        }

        rule.setContent {
            if (moveContent) {
                Box { content() }
            } else {
                Box { content() }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Move the content
        rule.runOnIdle {
            interactions.clear()
            moveContent = true
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Unfocus interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        // The touch event should cause a new indication node and interaction source to be created
        rule.onNodeWithTag("clickable").performTouchInput {
            // Need to reset the previous down
            up()
            down(center)
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new press
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun indicationNodeFactory_nullInteractionSource_resetWhenMoved_focused() {
        var attachedCount = 0
        var detachedCount = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        val interactionSources = mutableListOf<InteractionSource>()
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = { detachedCount++ }) {
                interactionSource,
                coroutineScope ->
                attachedCount++
                interactionSources += interactionSource
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var moveContent by mutableStateOf(false)

        val content = movableContentOf {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("clickable").focusRequester(focusRequester).clickable(
                        interactionSource = null,
                        indication = indication,
                    ) {},
            )
        }

        // setFocusableContent so when we are reused, the focus system won't automatically try and
        // set focus on us again
        rule.setFocusableContent {
            inputModeManager = LocalInputModeManager.current
            if (moveContent) {
                Box { content() }
            } else {
                Box { content() }
            }
        }

        rule.runOnIdle { assertThat(attachedCount).isEqualTo(0) }

        rule.runOnIdle {
            // Clickable is only focusable in non-touch mode
            inputModeManager.requestInputMode(Keyboard)
            // The focus event should cause the indication node to be created
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        // Move the content
        rule.runOnIdle {
            interactions.clear()
            moveContent = true
        }

        rule.runOnIdle {
            // The indication instance should be disposed
            assertThat(detachedCount).isEqualTo(1)
            // Because we collect the interactionSource using the node scope, this will be cancelled
            // before the new interaction is emitted, so the node won't see the Unfocus interaction.
            // Since the node has been removed though, this doesn't really matter.
            // No new node should be created, since we have reset, and are lazily creating the node
            assertThat(attachedCount).isEqualTo(1)
            assertThat(interactionSources.size).isEqualTo(1)
        }

        rule.runOnIdle {
            // Request focus again
            focusRequester.requestFocus()
        }

        rule.runOnIdle {
            // The new node should be created now
            assertThat(attachedCount).isEqualTo(2)
            assertThat(interactionSources.size).isEqualTo(2)
            // It should be using a different interaction source
            assertThat(interactionSources[0]).isNotEqualTo(interactionSources[1])
            assertThat(detachedCount).isEqualTo(1)
            // There should be a new focus interaction
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }
    }

    /**
     * Test case for changing between the overload that uses LocalIndication and the overload that
     * takes an explicit Indication, to make sure we update the node correctly in this case.
     */
    @Test
    fun localIndicationOverload_changingToExplicitIndication_indicationNodeFactory() {
        var created1 = false
        var detached1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 =
            TestIndicationNodeFactory(onDetach = { detached1 = true }) { source, coroutineScope ->
                interactionSource = source
                created1 = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var useExplicitIndication by mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication1) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable")
                                .then(
                                    if (useExplicitIndication) {
                                        Modifier.clickable(
                                            interactionSource = null,
                                            indication = indication2,
                                        ) {}
                                    } else {
                                        Modifier.clickable(interactionSource = null) {}
                                    }
                                ),
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created1).isFalse() }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created1).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performTouchInput { up() }

        rule.runOnIdle {
            interactions.clear()
            useExplicitIndication = true
        }

        rule.runOnIdle {
            // We should be created because we created the previous node already
            assertThat(created2).isTrue()
            // The previous node should be detached
            assertThat(detached1).isTrue()
        }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for changing between the overload that uses LocalIndication and the overload that
     * takes an explicit Indication, to make sure we update the node correctly in this case.
     */
    @Test
    fun localIndicationOverload_changingToExplicitIndication_nullIndication() {
        var created = false
        var detached = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 =
            TestIndicationNodeFactory(onDetach = { detached = true }) { source, coroutineScope ->
                interactionSource = source
                created = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var useExplicitIndication by mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication1) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable")
                                .then(
                                    if (useExplicitIndication) {
                                        Modifier.clickable(
                                            interactionSource = null,
                                            indication = null,
                                        ) {}
                                    } else {
                                        Modifier.clickable(interactionSource = null) {}
                                    }
                                ),
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performTouchInput { up() }

        rule.runOnIdle {
            interactions.clear()
            useExplicitIndication = true
        }

        rule.runOnIdle {
            // The previous node should be detached
            assertThat(detached).isTrue()
        }
    }

    /**
     * Test case for changing between the overload that takes an explicit Indication and the
     * overload that uses LocalIndication, to make sure we update the node correctly in this case.
     */
    @Test
    fun explicitIndicationOverload_indicationNodeFactory_changingToLocalIndication() {
        var created1 = false
        var detached1 = false
        var created2 = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication1 =
            TestIndicationNodeFactory(onDetach = { detached1 = true }) { source, coroutineScope ->
                interactionSource = source
                created1 = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }
        val indication2 = TestIndicationNodeFactory { source, coroutineScope ->
            interactionSource = source
            created2 = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        var useExplicitIndication by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication2) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable")
                                .then(
                                    if (useExplicitIndication) {
                                        Modifier.clickable(
                                            interactionSource = null,
                                            indication = indication1,
                                        ) {}
                                    } else {
                                        Modifier.clickable(interactionSource = null) {}
                                    }
                                ),
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(created1).isFalse() }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created1).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performTouchInput { up() }

        rule.runOnIdle {
            interactions.clear()
            useExplicitIndication = false
        }

        rule.runOnIdle {
            // We should be created because we created the previous node already
            assertThat(created2).isTrue()
            // The previous node should be detached
            assertThat(detached1).isTrue()
        }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    /**
     * Test case for changing between the overload that takes an explicit Indication and the
     * overload that uses LocalIndication, to make sure we update the node correctly in this case.
     */
    @Test
    fun explicitIndicationOverload_nullIndication_changingToLocalIndication() {
        var created = false
        lateinit var interactionSource: InteractionSource
        val interactions = mutableListOf<Interaction>()
        val indication =
            TestIndicationNodeFactory(onDetach = {}) { source, coroutineScope ->
                interactionSource = source
                created = true
                coroutineScope.launch {
                    interactionSource.interactions.collect { interaction ->
                        interactions.add(interaction)
                    }
                }
            }

        var useExplicitIndication by mutableStateOf(true)

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable")
                                .then(
                                    if (useExplicitIndication) {
                                        Modifier.clickable(
                                            interactionSource = null,
                                            indication = null,
                                        ) {}
                                    } else {
                                        Modifier.clickable(interactionSource = null) {}
                                    }
                                ),
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(created).isFalse()
            useExplicitIndication = false
        }

        rule.runOnIdle {
            // We should still not be created
            assertThat(created).isFalse()
        }

        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
    }

    @Test
    fun nullInteractionSource_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(enabled = toggleInput, onClick = onClick)
        }
    }

    @Test
    fun nonNullInteractionSource_equality() {
        val onClick = {}
        val interactionSource = MutableInteractionSource()
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(
                interactionSource = interactionSource,
                enabled = toggleInput,
                onClick = onClick,
            )
        }
    }

    @Test
    fun nullInteractionSourceNullIndication_equality() {
        val onClick = {}
        assertModifierIsPure { toggleInput ->
            Modifier.clickable(
                interactionSource = null,
                indication = null,
                enabled = toggleInput,
                onClick = onClick,
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
                onClick = onClick,
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
                onClick = onClick,
            )
        }
    }

    @Test
    fun nullInteractionSourceNonNullIndication_nonEquality() {
        val onClick = {}
        val indication = TestIndication {}
        val modifier1 =
            Modifier.clickable(interactionSource = null, indication = indication, onClick = onClick)
        val modifier2 =
            Modifier.clickable(interactionSource = null, indication = indication, onClick = onClick)

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
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )
        val modifier2 =
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )

        // Indication requires composed, so cannot compare equal
        assertThat(modifier1).isNotEqualTo(modifier2)
    }

    @Test
    fun focusUsingSemanticAction() {
        // Arrange.
        val tag = "testClickable"
        rule.setContent {
            Box(Modifier.size(10.dp).testTag(tag).focusProperties { canFocus = true }.clickable {})
        }

        // Act.
        rule.onNodeWithTag(tag).requestFocus()

        // Assert.
        rule.onNodeWithTag(tag).assertIsFocused()
    }

    @Test
    fun focusUsingSemanticActionWhenNotEnabled() {
        // Arrange.
        val tag = "testClickable"
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .testTag(tag)
                    .focusProperties { canFocus = true }
                    .clickable(enabled = false) {}
            )
        }

        // Act.
        assertFailsWith(
            exceptionClass = AssertionError::class,
            message = "Failed to perform action RequestFocus, the node is missing [RequestFocus]",
        ) {
            rule.onNodeWithTag(tag).requestFocus()
        }
    }

    @Test
    fun focusUsingSemanticActionWhileChangingEnabledParam() {
        // Arrange.
        val tag = "testClickable"
        lateinit var focusManager: FocusManager
        var enabled by mutableStateOf(true)
        rule.setContent {
            focusManager = LocalFocusManager.current
            Box(
                Modifier.size(10.dp)
                    .testTag(tag)
                    .focusProperties { canFocus = true }
                    .clickable(enabled = enabled) {}
            )
        }

        // Request focus succeeds when enabled = true.
        rule.onNodeWithTag(tag).requestFocus()
        rule.onNodeWithTag(tag).assertIsFocused()

        // Request focus fails when enabled = false.
        rule.runOnIdle {
            enabled = false
            focusManager.clearFocus()
        }
        assertFailsWith(
            exceptionClass = AssertionError::class,
            message = "Failed to perform action RequestFocus, the node is missing [RequestFocus]",
        ) {
            rule.onNodeWithTag(tag).requestFocus()
        }
        assertFailsWith(
            exceptionClass = AssertionError::class,
            message = "Failed to assert the following: (Focused = 'false')",
        ) {
            rule.onNodeWithTag(tag).assertIsNotFocused()
        }

        // Request focus succeeds when enabled = true.
        rule.runOnIdle {
            enabled = true
            focusManager.clearFocus()
        }
        rule.onNodeWithTag(tag).requestFocus()
        rule.onNodeWithTag(tag).assertIsFocused()
    }

    // Regression test for b/332814226
    @Test
    fun movableContentWithSubcomposition_updatingSemanticsShouldNotCrash() {
        var moveContent by mutableStateOf(false)
        rule.setContent {
            val content = remember {
                movableContentOf {
                    BoxWithConstraints {
                        BasicText(
                            "ClickableText",
                            modifier =
                                Modifier.testTag("clickable").clickable(
                                    role = if (moveContent) Role.Button else Role.Checkbox,
                                    onClickLabel = moveContent.toString(),
                                ) {},
                        )
                    }
                }
            }

            key(moveContent) { content() }
        }

        rule
            .onNodeWithTag("clickable")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertOnClickLabelMatches("false")

        rule.runOnIdle { moveContent = true }

        rule
            .onNodeWithTag("clickable")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertOnClickLabelMatches("true")
    }

    @Test // https://youtrack.jetbrains.com/issue/CMP-5069
    fun clickableInScrollContainerWithMouse() {
        var isClicked = false
        rule.setContent {
            Row(modifier = Modifier.testTag("container")) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.size(100.dp).clickable { isClicked = true })
                }
            }
        }

        rule.onNodeWithTag("container").performMouseInput {
            moveBy(Offset(10f, 10f))
            press()
            moveBy(Offset(50f, 50f))
            release()
        }

        rule.runOnIdle {
            assertTrue(isClicked, "The Box is expected to receive a click when using Mouse")
        }
    }

    @Test // https://youtrack.jetbrains.com/issue/CMP-5069
    fun clickableInScrollContainerWithTouch() {
        var isClicked = false
        rule.setContent {
            Row(modifier = Modifier.testTag("container")) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.size(100.dp).clickable { isClicked = true })
                }
            }
        }

        rule.onNodeWithTag("container").performTouchInput {
            down(Offset(10f, 10f))
            // drag a bit
            moveBy(Offset(50f, 50f))
            up()
        }

        rule.runOnIdle {
            assertFalse(
                isClicked,
                "The Box is NOT expected to receive a Click while dragging using touch",
            )
        }

        rule.onNodeWithTag("container").performTouchInput {
            down(Offset(50f, 50f))
            // no drag
            up()
        }

        rule.runOnIdle {
            assertTrue(
                isClicked,
                "The Box is expected to receive a Click, there was no dragging using touch",
            )
        }
    }

    /** Regression test for b/358572550 */
    @Test
    fun disabledParentClickable_doesNotCrashOnFocusEvent() {
        val tag = "testClickable"
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.size(100.dp).clickable(enabled = false, onClick = {})) {
                // Any focus target inside would work, but because clickable merges descendants it
                // prevents itself from being merged into the parent node so it is easier to test
                Box(Modifier.size(10.dp).focusRequester(focusRequester).testTag(tag).clickable {})
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }

        // Should not crash
        rule.runOnIdle { focusRequester.requestFocus() }

        rule.onNodeWithTag(tag).assertIsFocused()
    }

    @Test
    fun lazilyCreatedIndicatorReceivesPressedInteraction() {
        var created = false
        val interactions = mutableListOf<Interaction>()
        val indication = TestIndicationNodeFactory { interactionSource, coroutineScope ->
            created = true
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    interactions.add(interaction)
                }
            }
        }

        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(modifier = Modifier.testTag("clickable").clickable {})
            }
        }

        rule.runOnIdle { assertThat(created).isFalse() }

        // The touch event should cause the indication node to be created
        rule.onNodeWithTag("clickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(created).isTrue()
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }
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
    val onAttach: ((InteractionSource, CoroutineScope) -> Unit),
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

        if (onAttach !== other.onAttach) return false

        return true
    }

    override fun hashCode(): Int {
        return onAttach.hashCode()
    }
}

internal fun SemanticsNodeInteraction.assertOnClickLabelMatches(
    expectedValue: String
): SemanticsNodeInteraction {
    return assert(
        SemanticsMatcher("onClickLabel = '$expectedValue'") {
            it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == expectedValue
        }
    )
}
