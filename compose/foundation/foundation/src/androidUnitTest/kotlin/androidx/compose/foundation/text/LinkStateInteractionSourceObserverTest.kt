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

package androidx.compose.foundation.text

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextLinkScopeTest {
    private lateinit var observer: LinkStateInteractionSourceObserver

    @Before
    fun setup() {
        observer = LinkStateInteractionSourceObserver()
    }

    @Test
    fun calculateStateForLink_focused() {
        val interactionSource = constructInteractionSource(FocusInteraction.Focus())

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isFocused).isTrue()
        }
    }

    @Test
    fun calculateStateForLink_unFocused() {
        val focusInt = FocusInteraction.Focus()
        val interactionSource =
            constructInteractionSource(focusInt, FocusInteraction.Unfocus(focusInt))

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isFocused).isFalse()
        }
    }

    @Test
    fun calculateStateForLink_hovered() {
        val interactionSource = constructInteractionSource(HoverInteraction.Enter())

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isHovered).isTrue()
        }
    }

    @Test
    fun calculateStateForLink_unHovered() {
        val hoverInt = HoverInteraction.Enter()
        val interactionSource =
            constructInteractionSource(hoverInt, HoverInteraction.Exit(hoverInt))

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isHovered).isFalse()
        }
    }

    @Test
    fun calculateStateForLink_pressed() {
        val interactionSource = constructInteractionSource(PressInteraction.Press(Offset.Zero))

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isPressed).isTrue()
        }
    }

    @Test
    fun calculateStateForLink_unPressed_cancel() {
        val pressInt = PressInteraction.Press(Offset.Zero)
        val interactionSource =
            constructInteractionSource(pressInt, PressInteraction.Cancel(pressInt))

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isPressed).isFalse()
        }
    }

    @Test
    fun calculateStateForLink_unPressed_release() {
        val pressInt = PressInteraction.Press(Offset.Zero)
        val interactionSource =
            constructInteractionSource(pressInt, PressInteraction.Release(pressInt))

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isPressed).isFalse()
        }
    }

    @Test
    fun calculateStateForLink_noFocusHoverPress_differentInteraction() {
        val interactionSource = constructInteractionSource(object : Interaction {})

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isFocused).isFalse()
            assertThat(observer.isHovered).isFalse()
            assertThat(observer.isPressed).isFalse()
        }
    }

    @Test
    fun calculateStateForLink_allFocusHoverPress() {
        val interactionSource =
            constructInteractionSource(
                FocusInteraction.Focus(),
                PressInteraction.Press(Offset.Zero),
                HoverInteraction.Enter()
            )

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isFocused).isTrue()
            assertThat(observer.isHovered).isTrue()
            assertThat(observer.isPressed).isTrue()
        }
    }

    @Test
    fun calculateStateForLink_noFocusHoverPress() {
        val pressInt = PressInteraction.Press(Offset.Zero)
        val focusInt = FocusInteraction.Focus()
        val hoverInt = HoverInteraction.Enter()

        val interactionSource =
            constructInteractionSource(
                pressInt,
                focusInt,
                hoverInt,
                HoverInteraction.Exit(hoverInt),
                PressInteraction.Release(pressInt),
                FocusInteraction.Unfocus(focusInt)
            )

        runTest {
            observer.collectInteractionsForLinks(interactionSource)
            assertThat(observer.isFocused).isFalse()
            assertThat(observer.isHovered).isFalse()
            assertThat(observer.isPressed).isFalse()
        }
    }

    private fun constructInteractionSource(vararg interactions: Interaction) =
        object : InteractionSource {
            override val interactions: Flow<Interaction>
                get() = flowOf(elements = interactions)
        }
}
