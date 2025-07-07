/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WebClipboardDemo() {
    val clipboard = LocalClipboard.current

    Column {
        val textFieldState1 = rememberTextFieldState("TextToBeCopied")
        val textFieldState2 = rememberTextFieldState("")
        val coroutineScope = rememberCoroutineScope()

        Row {
            TextField(textFieldState1)
            Spacer(modifier = Modifier.width(48.dp))
            Button(modifier = Modifier.testTag("copyButton"), onClick = {
                coroutineScope.launch {
                    clipboard.setClipEntry(createClipEntryWithPlainText(textFieldState1.text.toString()))
                }
            }) {
                Text("Copy")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row {
            TextField(textFieldState2)
            Spacer(modifier = Modifier.width(48.dp))
            Button(modifier = Modifier.testTag("pasteButton"), onClick = {
                coroutineScope.launch {
                    val text = clipboard.getClipEntry().getPlainText() // uses readText, which is suppressed internal!
                    println("clipboard text: $text")
                    textFieldState2.setTextAndPlaceCursorAtEnd(
                        text ?: "null"
                    )
                }
            }) {
                Text("Paste")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = {
            coroutineScope.launch {
                clipboard.setClipEntry(null)
            }
        }) {
            Text("Clear clipboard")
        }
    }
}