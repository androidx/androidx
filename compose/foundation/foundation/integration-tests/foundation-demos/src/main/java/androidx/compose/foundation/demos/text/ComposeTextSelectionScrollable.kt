/*
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@SuppressLint("PrimitiveInLambda")
@Preview
@Composable
fun TextScrollableColumnSelectionDemo() {
    val spacing = 16.dp
    Column(
        modifier = Modifier.padding(spacing),
        verticalArrangement = spacedBy(spacing)
    ) {
        Text(
            text = "We expect that selection works, " +
                "regardless of how many times each text goes in or out of view. " +
                "The selection handles and text toolbar also should follow the selection " +
                "when it is scrolled.",
            style = MaterialTheme.typography.body1.merge(),
        )
        val (selectedOption, onOptionSelected) = remember {
            mutableStateOf(Options.LongScrollableText)
        }
        Column(Modifier.selectableGroup()) {
            Options.values().forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == selectedOption,
                        onClick = { onOptionSelected(option) }
                    )
                    Text(
                        text = option.displayText,
                        style = MaterialTheme.typography.body1.merge(),
                    )
                }
            }
        }
        selectedOption.Content()
    }
}

@Suppress("unused") // enum values used in .values()
private enum class Options(val displayText: String, val content: @Composable () -> Unit) {
    LongScrollableText("Long Scrollable Single Text", {
        MyText(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            text = (0..100).joinToString(separator = "\n") { it.toString() },
        )
    }),
    LongTextScrollableColumn("Long Single Text in Scrollable Column", {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            MyText((0..100).joinToString(separator = "\n") { it.toString() })
        }
    }),
    MultiTextScrollableColumn("Multiple Texts in Scrollable Column", {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            repeat(100) { MyText(it.toString()) }
        }
    }),
    MultiTextLazyColumn("Multiple Texts in LazyColumn", {
        LazyColumn {
            items(100) { MyText(it.toString()) }
        }
    });

    @Composable
    fun Content() {
        SelectionContainer(content = content)
    }
}

@Composable
private fun MyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TextStyle(fontSize = fontSize8, textAlign = TextAlign.Center),
        modifier = modifier.fillMaxWidth()
    )
}
