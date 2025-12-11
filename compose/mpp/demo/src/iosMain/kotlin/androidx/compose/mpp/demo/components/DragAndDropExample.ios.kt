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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.uikit.fromString
import androidx.compose.ui.uikit.loadString
import androidx.compose.ui.unit.dp
import platform.UIKit.UIDragItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun DragAndDropExample() {
    Column(
        modifier = Modifier.fillMaxSize().padding(PaddingValues(top = 16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var text by remember { mutableStateOf("Hello world!") }

        val logs = remember { mutableStateListOf<String>() }

        val addLog = { log: String ->
            logs.add(0, log)
            while (logs.size > 10) {
                logs.removeLast()
            }
        }

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Payload content") },
        )

        Text(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(1f)
                .height(200.dp)
                .dragAndDropSource {
                    addLog("Sent: $text")
                    DragAndDropTransferData(listOf(UIDragItem.fromString(text)))
                }
                .background(Color.DarkGray),
            color = Color.White,
            text = text
        )

        Spacer(modifier = Modifier.height(20.dp))

        var dropText by remember { mutableStateOf("Drop here") }

        Text(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(1f)
                .height(100.dp)
                .background(Color.DarkGray)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            addLog("Dropped")
                            event.forEachString {
                                dropText = it
                                addLog("Received: $it")
                            }
                            return true
                        }
                    }
                )
            ,
            color = Color.White,
            text = dropText
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs) { log ->
                Text(log)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.forEachString(block: (String) -> Unit) {
    items.forEach {
        it.loadString { s, _ ->
            s?.let(block)
        }
    }
}
