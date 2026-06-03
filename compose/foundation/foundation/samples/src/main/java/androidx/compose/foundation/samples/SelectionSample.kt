/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange

@Sampled
@Composable
fun SelectionSample() {
    SelectionContainer {
        Column {
            Text("Text 1")
            Text("Text 2")
            Text("טקסט 3")
        }
    }
}

@Sampled
@Composable
fun SelectAllSample() {
    val selectionState = rememberSelectionState()

    Column {
        Button(onClick = { selectionState.selectAll() }) { BasicText("Select All") }
        SelectionContainer(state = selectionState) { BasicText(text = "Text to be selected...") }
    }
}

@Sampled
@Composable
fun DisableSelectionSample() {
    SelectionContainer {
        Column {
            Text("Text 1")

            DisableSelection {
                Text("Text 2")
                Text("טקסט 3")
            }

            Text("Text 3")
        }
    }
}

@Sampled
@Composable
fun SelectionStateSample() {
    val selectionState = rememberSelectionState()

    val characterCount = selectionState.selectedTexts.sumOf { it.text.length }

    Column {
        SelectionContainer(state = selectionState) {
            Column { BasicText(text = "Text to be selected...") }
        }
        if (characterCount > 0) {
            BasicText(text = "Characters selected: $characterCount")
        }
    }
}

@Sampled
@Composable
fun SelectQuerySample() {
    val selectionState = rememberSelectionState()
    val query = "Compose is great!"

    Column {
        Button(
            onClick = {
                val index = selectionState.getSelectableTexts().joinToString("").indexOf(query)

                if (index != -1) {
                    selectionState.select(TextRange(index, index + query.length))
                }
            }
        ) {
            BasicText("Select Quote")
        }
        SelectionContainer(state = selectionState) {
            Column {
                BasicText(text = "This UI Toolkit Compose")
                BasicText(text = " is great! :)")
            }
        }
    }
}

@Sampled
@Composable
fun SelectThirdTextSample() {
    val selectionState = rememberSelectionState()

    Column {
        Button(
            onClick = {
                val texts = selectionState.getSelectableTexts()

                if (texts.size >= 3) {
                    val globalStart = texts[0].length + texts[1].length
                    val globalEnd = globalStart + texts[2].length

                    selectionState.select(TextRange(globalStart, globalEnd))
                }
            }
        ) {
            BasicText("Select Third Paragraph")
        }
        SelectionContainer(state = selectionState) {
            Column {
                BasicText(text = "Paragraph 1...")
                BasicText(text = "Paragraph 2...")
                BasicText(text = "Paragraph 3...")
            }
        }
    }
}

@Sampled
@Composable
fun ExtendSelectionSample() {
    val selectionState = rememberSelectionState()

    Column {
        Button(onClick = { selectionState.extendSelectionByWord() }) {
            BasicText("Extend Selection to Next Word")
        }
        SelectionContainer(state = selectionState) { BasicText("Text to select: word by word") }
    }
}
