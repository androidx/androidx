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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

@LargeTest
class TextFieldInteractionSourcePressTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private val TAG = "BasicTextField"

    private val state = TextFieldState("Hello World")

    private val fontSize = 10.sp

    private val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    private lateinit var coroutineScope: CoroutineScope

    @Test
    fun normalClick_sendsPressAndReleaseEvents() {
        val interactionSource = MutableInteractionSource()
        rule.setTextFieldTestContent {
            coroutineScope = rememberCoroutineScope()
            BasicTextField(
                state,
                modifier = Modifier.testTag(TAG),
                textStyle = defaultTextStyle,
                interactionSource = interactionSource
            )
        }

        val interactions = mutableListOf<Interaction>()

        coroutineScope.launch {
            interactionSource.interactions.filterIsInstance<PressInteraction>().collect {
                interactions.add(it)
            }
        }

        rule.onNodeWithTag(TAG).performClick()

        rule.runOnIdle {
            assertThat(interactions.size).isEqualTo(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun touchDown_thenSwipeOutOfBounds_sendsPressAndCancelEvents() {
        val interactionSource = MutableInteractionSource()
        rule.setTextFieldTestContent {
            coroutineScope = rememberCoroutineScope()
            BasicTextField(
                state,
                modifier = Modifier.testTag(TAG),
                textStyle = defaultTextStyle,
                interactionSource = interactionSource
            )
        }

        val interactions = mutableListOf<Interaction>()

        coroutineScope.launch {
            interactionSource.interactions.filterIsInstance<PressInteraction>().collect {
                interactions.add(it)
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput {
            down(center)
            moveBy(Offset(500f, 500f))
            up()
        }

        rule.runOnIdle {
            assertThat(interactions.size).isEqualTo(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
        }
    }

    /**
     * Tests whether inner text field is involved in press event interactions. Decoration box itself
     * should be enough to send these events.
     */
    @Test
    fun decorationBox_sendsEvents() {
        val interactionSource = MutableInteractionSource()
        rule.setTextFieldTestContent {
            coroutineScope = rememberCoroutineScope()
            BasicTextField(
                state,
                modifier = Modifier.testTag(TAG),
                textStyle = defaultTextStyle,
                interactionSource = interactionSource,
                decorator = { Box(modifier = Modifier.size(100.dp)) }
            )
        }

        val interactions = mutableListOf<Interaction>()

        coroutineScope.launch {
            interactionSource.interactions.filterIsInstance<PressInteraction>().collect {
                interactions.add(it)
            }
        }

        rule.onNodeWithTag(TAG).performClick()

        rule.runOnIdle {
            assertThat(interactions.size).isEqualTo(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun interactionSourceChanges_eventsAreSentToNewSource() {
        var interactionSource by mutableStateOf(MutableInteractionSource())
        rule.setTextFieldTestContent {
            coroutineScope = rememberCoroutineScope()
            BasicTextField(
                state,
                modifier = Modifier.testTag(TAG),
                textStyle = defaultTextStyle,
                interactionSource = interactionSource
            )
        }

        val interactions = mutableListOf<Interaction>()

        coroutineScope.launch {
            interactionSource.interactions.filterIsInstance<PressInteraction>().collect {
                interactions.add(it)
            }
        }

        rule.onNodeWithTag(TAG).performClick()

        rule.runOnIdle { assertThat(interactions.size).isEqualTo(2) }

        interactions.clear()
        val newInteractionSource = MutableInteractionSource()
        interactionSource = newInteractionSource

        coroutineScope.launch {
            newInteractionSource.interactions.filterIsInstance<PressInteraction>().collect {
                interactions.add(it)
            }
        }

        rule.onNodeWithTag(TAG).performClick()

        rule.runOnIdle { assertThat(interactions.size).isEqualTo(2) }
    }

    @Test
    fun doubleTap_sendsTwoPressEvents() {
        val interactionSource = MutableInteractionSource()
        rule.setTextFieldTestContent {
            coroutineScope = rememberCoroutineScope()
            BasicTextField(
                state,
                modifier = Modifier.testTag(TAG),
                textStyle = defaultTextStyle,
                interactionSource = interactionSource
            )
        }

        val interactions = mutableListOf<Interaction>()

        coroutineScope.launch {
            interactionSource.interactions.filterIsInstance<PressInteraction>().collect {
                interactions.add(it)
            }
        }

        rule.onNodeWithTag(TAG).performTouchInput { doubleClick() }

        rule.runOnIdle {
            assertThat(interactions.size).isEqualTo(4)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            // second tap is consumed to select the tapped word, so this will be cancelled.
            assertThat(interactions[3]).isInstanceOf(PressInteraction.Cancel::class.java)
        }
    }
}
