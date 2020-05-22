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
package androidx.ui.desktop.examples.example1

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.graphics.Color
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.layout.preferredHeight
import androidx.ui.material.Button
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.ExtendedFloatingActionButton
import androidx.ui.material.FilledTextField
import androidx.ui.material.Scaffold
import androidx.ui.material.Slider
import androidx.ui.material.TopAppBar
import androidx.ui.unit.dp

import androidx.ui.desktop.examples.mainWith

private const val title = "Desktop Compose Elements"

fun main() = mainWith(title) @Composable {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("BUTTON") },
                onClick = {
                    println("Floating button clicked")
                }
            )
        },
        bodyContent = {
            val amount = state { 0 }
            val text = state { "Hello" }
            Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly) {
                Text(
                    text = "Привет! 你好! Desktop Compose ${amount.value}",
                    color = Color.Black,
                    modifier = Modifier
                        .drawBackground(Color.Blue)
                        .preferredHeight(56.dp)
                        .wrapContentSize(Alignment.Center)
                )
                Button(onClick = {
                    amount.value++
                }) {
                    Text("Base")
                }
                CircularProgressIndicator()
                Slider(value = amount.value.toFloat() / 100f,
                    onValueChange = { amount.value = (it * 100).toInt() })
                FilledTextField(
                    value = amount.value.toString(),
                    onValueChange = { amount.value = it.toIntOrNull() ?: 42 },
                    label = { Text(text = "Input1") }
                )
                FilledTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = { Text(text = "Input2") }
                )
            }
        }
    )
}
