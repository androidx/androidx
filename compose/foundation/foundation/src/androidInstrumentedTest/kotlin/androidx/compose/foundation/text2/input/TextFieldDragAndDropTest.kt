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

package androidx.compose.foundation.text2.input

import android.net.Uri
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.DragAndDropScope
import androidx.compose.foundation.content.createClipData
import androidx.compose.foundation.content.testDragAndDrop
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldDragAndDropTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nonTextContent_isNotAccepted() {
        rule.setContentAndTestDragAndDrop {
            val startSelection = state.text.selectionInChars
            drag(
                Offset(fontSize.toPx() * 2, 10f),
                Uri.parse("content://com.example/content.jpg")
            )
            assertThat(state.text.selectionInChars).isEqualTo(startSelection)
        }
    }

    @Test
    fun textContent_isAccepted() {
        rule.setContentAndTestDragAndDrop {
            drag(Offset(fontSize.toPx() * 2, 10f), "hello")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
        }
    }

    @Test
    fun draggingText_updatesSelection() {
        rule.setContentAndTestDragAndDrop {
            drag(Offset(fontSize.toPx() * 1, 10f), "hello")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(1))
            drag(Offset(fontSize.toPx() * 2, 10f), "hello")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(2))
            drag(Offset(fontSize.toPx() * 3, 10f), "hello")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(3))
        }
    }

    @Test
    fun draggingText_toEndPadding_updatesSelection() {
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            modifier = Modifier.width(300.dp)
        ) {
            drag(Offset.Zero, "hello")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0))
            drag(Offset(295.dp.toPx(), 10f), "hello")
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(4))
        }
    }

    @Test
    fun interactionSource_receivesHoverEnter_whenDraggingTextEnters() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()
        }
    }

    @Test
    fun interactionSource_receivesHoverExit_whenDraggingTextExits() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()

            drag(Offset(1000f, 1f), "hello")
            assertThat(isHovered).isFalse()
        }
    }

    @Test
    fun interactionSource_receivesHoverExit_whenDraggingTextEnds() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()

            cancelDrag()
            assertThat(isHovered).isFalse()
        }
    }

    @Test
    fun interactionSource_receivesHoverExit_whenDraggingTextDrops() {
        val interactionSource = MutableInteractionSource()
        rule.setContentAndTestDragAndDrop(
            style = TextStyle(textAlign = TextAlign.Center),
            interactionSource = interactionSource,
            modifier = Modifier.width(200.dp)
        ) {
            drag(Offset(1f, 1f), "hello")
            assertThat(isHovered).isTrue()

            drop()
            assertThat(isHovered).isFalse()
        }
    }

    @Test
    fun droppedText_insertsAtCursor() {
        rule.setContentAndTestDragAndDrop("Hello World!") {
            drag(
                Offset(fontSize.toPx() * 5, 10f),
                " Awesome"
            )
            drop()
            assertThat(state.text.selectionInChars).isEqualTo(TextRange("Hello Awesome".length))
            assertThat(state.text.toString()).isEqualTo("Hello Awesome World!")
        }
    }

    @Test
    fun multipleClipDataItems_concatsByNewLine() {
        rule.setContentAndTestDragAndDrop("aaaa") {
            drag(
                Offset(fontSize.toPx() * 2, 10f),
                createClipData {
                    addText("Hello")
                    addText("World")
                }
            )
            drop()
            assertThat(state.text.toString()).isEqualTo("aaHello\nWorldaa")
        }
    }

    private fun ComposeContentTestRule.setContentAndTestDragAndDrop(
        textContent: String = "aaaa",
        isWindowFocused: Boolean = false,
        style: TextStyle = TextStyle.Default,
        interactionSource: MutableInteractionSource? = null,
        modifier: Modifier = Modifier,
        block: DragAndDropTestScope.() -> Unit
    ) {
        val state = TextFieldState(
            textContent,
            initialSelectionInChars = TextRange.Zero
        )
        var view: View? = null
        val density = Density(1f, 1f)
        val mergedStyle = TextStyle(
            fontFamily = TEST_FONT_FAMILY,
            fontSize = 20.sp
        ).merge(style)
        var isHovered: State<Boolean>? = null
        setContent { // Do not use setTextFieldTestContent for DnD tests.
            view = LocalView.current
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalWindowInfo provides object : WindowInfo {
                    override val isWindowFocused = isWindowFocused
                }
            ) {
                isHovered = interactionSource?.collectIsHoveredAsState()
                BasicTextField2(
                    state = state,
                    textStyle = mergedStyle,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    interactionSource = interactionSource,
                    modifier = modifier
                )
            }
        }

        testDragAndDrop(view!!, density) {
            DragAndDropTestScope(state, mergedStyle.fontSize, isHovered, this).block()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private class DragAndDropTestScope(
        val state: TextFieldState,
        val fontSize: TextUnit,
        isHovered: State<Boolean>?,
        dragAndDropScopeImpl: DragAndDropScope,
    ) : DragAndDropScope by dragAndDropScopeImpl {
        val isHovered: Boolean by (isHovered ?: mutableStateOf(false))
    }
}
