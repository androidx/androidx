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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SelectionContainerTest {
    @Test
    fun selectionWorksWhenDraggingFromBelowText() = runComposeUiTest {
        var selection by mutableStateOf<Selection?>(null)
        val text = "Line 1\nLine2"
        setContent {
            SelectionContainer(
                modifier = Modifier.size(500.dp).testTag("selection_container"),
                selection = selection,
                onSelectionChange = {
                    selection = it
                }
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
            actual = selection?.toTextRange()
        )
    }
}