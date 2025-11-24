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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class ClickableFocusTest {
    @Test
    fun focus_is_requested_when_click_with_mouse_on_clickable() = runComposeUiTest {
        if (!isRequestFocusOnClickEnabled()) return@runComposeUiTest

        setContent {
            Column {
                Box(Modifier.testTag("box1").size(40.dp).clickable {  })
                Box(Modifier.testTag("box2").size(40.dp).clickable {  })
            }
        }

        onNodeWithTag("box1").performClick()
        onNodeWithTag("box1").assertIsFocused()

        onNodeWithTag("box2").performClick()
        onNodeWithTag("box2").assertIsFocused()

        onNodeWithTag("box1").performClick()
        onNodeWithTag("box1").assertIsFocused()
    }

    @Test
    fun focus_is_requested_when_click_with_mouse_on_toggleable() = runComposeUiTest {
        if (!isRequestFocusOnClickEnabled()) return@runComposeUiTest

        setContent {
            Column {
                Box(Modifier.testTag("box1").size(40.dp).toggleable(true, onValueChange = {}))
                Box(Modifier.testTag("box2").size(40.dp).toggleable(true, onValueChange = {}))
            }
        }

        onNodeWithTag("box1").performClick()
        onNodeWithTag("box1").assertIsFocused()

        onNodeWithTag("box2").performClick()
        onNodeWithTag("box2").assertIsFocused()

        onNodeWithTag("box1").performClick()
        onNodeWithTag("box1").assertIsFocused()
    }

    @Test
    fun focus_is_requested_when_click_with_mouse_on_selectable() = runComposeUiTest {
        if (!isRequestFocusOnClickEnabled()) return@runComposeUiTest

        setContent {
            Column {
                Box(Modifier.testTag("box1").size(40.dp).selectable(true, onClick = {}))
                Box(Modifier.testTag("box2").size(40.dp).selectable(true, onClick = {}))
            }
        }

        onNodeWithTag("box1").performClick()
        onNodeWithTag("box1").assertIsFocused()

        onNodeWithTag("box2").performClick()
        onNodeWithTag("box2").assertIsFocused()

        onNodeWithTag("box1").performClick()
        onNodeWithTag("box1").assertIsFocused()
    }

    private fun focus_indication_is_hidden_when_click_with_mouse_and_shown_after_Tab(
        content: @Composable (indication1: TestFocusIndicationNodeFactory, indication2: TestFocusIndicationNodeFactory) -> Unit
    ) = runComposeUiTest {
        // the test depends on "Request focus on click" feature
        if (!isRequestFocusOnClickEnabled()) return@runComposeUiTest

        val indication1 = TestFocusIndicationNodeFactory()
        val indication2 = TestFocusIndicationNodeFactory()

        lateinit var scope: RecomposeScope

        setContent {
            // TODO remove after fixing https://youtrack.jetbrains.com/issue/CMP-5819/Change-of-state-in-onDraw-doesnt-invalidate-the-window
            scope = currentRecomposeScope

            content(indication1, indication2)
        }

        waitForIdle()
        assertFalse(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)

        onRoot().performKeyInput { pressKey(Key.Tab) }
        scope.invalidate()
        waitForIdle()
        assertTrue(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)

        onRoot().performKeyInput { pressKey(Key.Tab) }
        scope.invalidate()
        waitForIdle()
        assertFalse(indication1.isFocusedDrawn)
        assertTrue(indication2.isFocusedDrawn)

        onRoot().performKeyInput { pressKey(Key.Tab) }
        scope.invalidate()
        waitForIdle()
        assertTrue(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)

        onNodeWithTag("box1").performClick()
        scope.invalidate()
        waitForIdle()
        // the focus isn't reset if it is already focused (for simplicity reasons)
        assertTrue(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)

        onNodeWithTag("box2").performClick()
        scope.invalidate()
        waitForIdle()
        assertFalse(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)

        onRoot().performKeyInput { pressKey(Key.Tab) }
        scope.invalidate()
        waitForIdle()
        assertTrue(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)

        onNodeWithTag("box2").requestFocus()
        scope.invalidate()
        waitForIdle()
        assertFalse(indication1.isFocusedDrawn)
        assertTrue(indication2.isFocusedDrawn)

        onNodeWithTag("box1").performClick()
        scope.invalidate()
        waitForIdle()
        assertFalse(indication1.isFocusedDrawn)
        assertFalse(indication2.isFocusedDrawn)
    }


    @Test
    fun focus_specified_indication_is_hidden_when_click_with_mouse_and_shown_after_Tab() =
        focus_indication_is_hidden_when_click_with_mouse_and_shown_after_Tab { ind1, ind2 ->
            Column {
                Box(Modifier.testTag("box1").size(40.dp)
                    .clickable(indication = ind1, interactionSource = null) {  })
                Box(Modifier.testTag("box2").size(40.dp)
                    .clickable(indication = ind2, interactionSource = null) {  })
            }
        }

    @Test
    fun focus_local_indication_is_hidden_when_click_with_mouse_and_shown_after_Tab() =
        focus_indication_is_hidden_when_click_with_mouse_and_shown_after_Tab { ind1, ind2 ->
            Column {
                CompositionLocalProvider(LocalIndication provides ind1) {
                    Box(
                        Modifier
                            .testTag("box1")
                            .size(40.dp)
                            .clickable { }
                    )
                }
                CompositionLocalProvider(LocalIndication provides ind2) {
                    Box(
                        Modifier
                            .testTag("box2")
                            .size(40.dp)
                            .clickable { }
                    )
                }
            }
        }

    private class TestFocusIndicationNodeFactory : IndicationNodeFactory {
        var isFocusedDrawn = false

        override fun create(interactionSource: InteractionSource): DelegatableNode {
            return object : Modifier.Node(), DrawModifierNode {
                private var isFocused by mutableStateOf(false)

                override fun onAttach() {
                    coroutineScope.launch {
                        val focusInteractions = mutableListOf<FocusInteraction.Focus>()
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is FocusInteraction.Focus ->
                                    focusInteractions.add(interaction)
                                is FocusInteraction.Unfocus ->
                                    focusInteractions.remove(interaction.focus)
                            }
                            isFocused = focusInteractions.isNotEmpty()
                        }
                    }
                }

                override fun ContentDrawScope.draw() {
                    drawContent()
                    isFocusedDrawn = isFocused
                    if (isFocused) {
                        drawRect(color = Color.Black.copy(alpha = 0.3f), size = size)
                    }
                }
            }
        }

        // these overrides are required by [IndicationNodeFactory]
        override fun hashCode() = super.hashCode()
        override fun equals(other: Any?) = super.equals(other)
    }

    @Test
    fun mouseClickOutsideClearsFocus() = runComposeUiTest {
        val focusRequester = FocusRequester()
        setContent {
            Column(Modifier.size(300.dp, 400.dp)) {
                BasicTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag("textField")
                        .focusRequester(focusRequester)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Box(Modifier.testTag("box").fillMaxWidth().weight(1f))
            }
        }

        onNodeWithTag("textField").assertIsFocused()
        onNodeWithTag("box").performMouseInput { click() }
        onNodeWithTag("textField").assertIsNotFocused()
        onNode(isFocused()).assertDoesNotExist()
    }
}