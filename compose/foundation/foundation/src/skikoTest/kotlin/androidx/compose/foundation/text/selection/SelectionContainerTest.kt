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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SelectionContainerTest {
    @Test
    fun selectionWorksWhenDraggingFromBelowText() = runComposeUiTest {
        val selectionState = SelectionState()
        val text = "Line 1\nLine2"
        setContent {
            SelectionContainer(
                state = selectionState,
                modifier = Modifier.size(500.dp).testTag("selection_container"),
            ) {
                BasicText(
                    text = text,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        onNodeWithTag("selection_container").performMouseInput {
            dragAndDrop(
                start = Offset(250f, 499f),
                end = Offset.Zero
            )
        }

        assertEquals(
            expected = TextRange(text.length, 0),
            actual = selectionState.selection?.toTextRange()
        )
    }

    @Test
    fun clickOnDisabledSelectionClearsSelection() = runComposeUiTest {
        val selectionState = SelectionState()
        setContent {
            SelectionContainer(
                state = selectionState,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column {
                    BasicText(
                        text = "word1 word2",
                        modifier = Modifier.testTag("selectable")
                    )
                    BasicText(
                        text = "word3 word4",
                        modifier = Modifier.testTag("unselectable")
                    )
                }
            }
        }

        onNodeWithTag("selectable").performMouseInput {
            doubleClick(Offset(1f, 1f))
        }
        assertTrue(selectionState.selection.exists())

        onNodeWithTag("unselectable").performMouseInput {
            click()
        }
        assertFalse(selectionState.selection.exists())
    }

    @Test
    fun dragToSelect() = runComposeUiTest {
        val selectionState = SelectionState()
        var size: IntSize = IntSize.Zero
        setContent {
            SelectionContainer(
                state = selectionState,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column {
                    BasicText(
                        text = "word1 word2",
                        modifier = Modifier
                            .testTag("selectable")
                            .onGloballyPositioned {
                                size = it.size
                            }
                    )
                }
            }
        }

        onNodeWithTag("selectable").performMouseInput {
            dragAndDrop(
                start = Offset(0f, size.height/2f),
                end =  Offset(size.width.toFloat(), size.height/2f)
            )
        }
        assertTrue(selectionState.selection.exists())
    }

    @Test
    fun selectionMagnifierShouldNotCrash() {
        val sm = SelectionManager(SelectionRegistrarImpl())
        Modifier.selectionMagnifier(sm)
    }

    @Test
    fun dragOutsideScrollsAndSelects() = androidx.compose.ui.test.v2.runComposeUiTest {
        val scrollState by mutableStateOf(ScrollState(0))
        val selectionState = SelectionState()
        setContent {
            Box(Modifier.testTag("container").size(200.dp).verticalScroll(scrollState)) {
                SelectionContainer(
                    state = selectionState,
                ) {
                    Column(Modifier.testTag("content")) {
                        repeat(50) { BasicText("Line $it", Modifier.testTag("tag$it")) }
                    }
                }
            }
        }

        onNodeWithTag("content").performMouseInput {
            dragAndDrop(
                start = Offset(0f, 0f),
                end =  Offset(width.toFloat(), height + 100f),
                durationMillis = 10_000
            )
        }

        // Verify that it was scrolled to the bottom
        val contentSize = onNodeWithTag("content").fetchSemanticsNode().size
        assertTrue(scrollState.value > 0)
        assertEquals(contentSize.height, scrollState.value + scrollState.viewportSize)

        // Verify that the selection is the entire content
        selectionState.selection.let {
            assertNotNull(it)
            assertEquals(1, it.start.selectableId)
            assertEquals(0, it.start.offset)
            assertEquals(50, it.end.selectableId)
            assertEquals(7, it.end.offset)
        }
    }
}

private fun Selection?.exists() = (this != null) && !this.toTextRange().collapsed
