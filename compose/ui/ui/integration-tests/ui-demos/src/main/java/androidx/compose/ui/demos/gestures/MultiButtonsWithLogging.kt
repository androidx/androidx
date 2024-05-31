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
package androidx.compose.ui.demos.gestures

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MultiButtonsWithLoggingUsingOnClick() {
    var output by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    Toast.makeText(context, "Button 1 Clicked", Toast.LENGTH_SHORT).show()
                    val newString = "Button 1 Clicked\n$output"
                    output = newString
                }
            ) {
                Text("Button 1")
            }
            Button(
                onClick = {
                    Toast.makeText(context, "Button 2 Clicked", Toast.LENGTH_SHORT).show()
                    val newString = "Button 2 Clicked\n$output"
                    output = newString
                }
            ) {
                Text("Button 2")
            }
            Button(onClick = { output = "" }) { Text("Clear Output") }
        }

        Text(modifier = Modifier.fillMaxWidth(), text = output)
    }
}

@Composable
fun MultiButtonsWithLoggingUsingPointerInput() {
    var output by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                modifier =
                    Modifier.padding(10.dp).background(Color.Red).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val pointerEvent = awaitPointerEvent()
                                val newString = "Button 1 type: ${pointerEvent.type}\n$output"
                                output = newString
                            }
                        }
                    },
                text = "Button 1"
            )

            Text(
                modifier =
                    Modifier.padding(10.dp).background(Color.Red).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val pointerEvent = awaitPointerEvent()
                                val newString = "Button 2 type: ${pointerEvent.type}\n$output"
                                output = newString
                            }
                        }
                    },
                text = "Button 2"
            )

            Text(
                modifier =
                    Modifier.padding(10.dp).background(Color.Red).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                                output = ""
                            }
                        }
                    },
                text = "Clear output"
            )
        }

        Text(modifier = Modifier.fillMaxWidth(), text = output)
    }
}
