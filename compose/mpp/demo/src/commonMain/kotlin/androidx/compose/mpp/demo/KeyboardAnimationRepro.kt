/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val messageList = MutableStateFlow<List<String>>(emptyList())

private suspend fun sendMessage(text: String) {
    val newList = messageList.value + text
    messageList.emit(newList)
}

@Composable
fun KeyboardAnimationRepro() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val coroutineScope = rememberCoroutineScope()
        val messages = messageList.collectAsState().value
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(messages) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    text = it
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        var text by remember { mutableStateOf<String>("") }
        Row {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = text,
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.White
                ),
                onValueChange = {
                    text = it
                }
            )
            Button(
                modifier = Modifier.wrapContentSize().padding(8.dp),
                onClick = {
                    coroutineScope.launch {
                        sendMessage(text)
                        text = ""
                    }
                }
            ) {
                Text(text = "Send")
            }
        }
    }
}