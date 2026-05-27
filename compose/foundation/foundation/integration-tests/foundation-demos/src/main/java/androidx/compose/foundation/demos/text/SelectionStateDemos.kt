/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun NestedSelectionContainerDemo() {
    val selectionState = rememberSelectionState()

    SelectionContainer(state = selectionState) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DisableSelection {
                Button(onClick = { selectionState.selectAll() }) { BasicText("Select All Outer") }
            }
            BasicText(text = "Outer")
            SelectionContainer { BasicText(text = "Inner") }
            BasicText(text = "Outer")
        }
    }
}

@Preview
@Composable
fun SelectAllButtonDemo() {
    val selectionState = rememberSelectionState()

    Column(modifier = Modifier.safeDrawingPadding()) {
        Button(onClick = { selectionState.selectAll() }) { BasicText("Select All") }
        SelectionContainer(state = selectionState) { BasicText(text = "Text to be selected...") }
    }
}

@Preview
@Composable
fun ExtendSelectionButtonDemo() {
    val selectionState = rememberSelectionState()

    Column(modifier = Modifier.safeDrawingPadding()) {
        Button(onClick = { selectionState.extendSelectionByWord() }) {
            BasicText("Extend Selection to Next Word")
        }
        SelectionContainer(state = selectionState) { BasicText("Text to select: word by word") }
    }
}

@Preview
@Composable
fun SelectionTranslationDemo() {
    val selectionState = rememberSelectionState()

    var translatedText by remember { mutableStateOf("") }

    val textToTranslate = selectionState.selectedTexts.joinToString(separator = "\n") { it.text }

    LaunchedEffect(textToTranslate) {
        translatedText =
            if (textToTranslate.isNotBlank()) {
                translate(textToTranslate, "en")
            } else {
                ""
            }
    }

    Column {
        SelectionContainer(state = selectionState) { BasicText(text = "Text to translate.") }

        if (translatedText.isNotEmpty()) {
            BasicText(text = "English: $translatedText")
        }
    }
}

fun translate(text: String, language: String): String {
    return text
}

@Preview
@Composable
fun ShareSelectionDemo() {
    val selectionState = rememberSelectionState()
    val context = LocalContext.current

    Column {
        Button(
            onClick = {
                val annotatedTexts = selectionState.selectedTexts
                if (annotatedTexts.isNotEmpty()) {
                    val textToShare = annotatedTexts.joinToString(separator = "\n") { it.text }
                    shareText(context, textToShare)
                }
            }
        ) {
            BasicText("Share Selection")
        }
        SelectionContainer(state = selectionState) { BasicText("Text to share") }
    }
}

fun shareText(context: Context, text: String) {
    val sendIntent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

    val shareIntent = Intent.createChooser(sendIntent, "Share text via")

    context.startActivity(shareIntent)
}

@Preview
@Composable
fun SelectionCharacterCountDemo() {
    val selectionState = rememberSelectionState()

    val characterCount = selectionState.selectedTexts.sumOf { it.text.length }

    Column {
        SelectionContainer(state = selectionState) { BasicText(text = "Text to be selected...") }
        if (characterCount > 0) {
            BasicText(text = "Characters selected: $characterCount")
        }
    }
}

@Preview
@Composable
fun SelectQueryDemo() {
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

@Preview
@Composable
fun SelectThirdTextDemo() {
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
                BasicText(text = "Paragraph 4...")
            }
        }
    }
}
