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

package androidx.compose.ui.demos

import android.annotation.SuppressLint
import android.content.LocusId
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@SuppressLint("ClassVerificationFailure")
class SimpleChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // in ActivityCompat the `setLocusContext` method is a no-op on lower version so we just
        // check the version here instead of using the compat version of activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Always set locus id as 1
            this.setLocusContext(LocusId("1"), null)
        }
        setContent { MaterialTheme { SimpleChatPage() } }
    }
}

private data class Message(val content: String, val isReceived: Boolean = true)

@SuppressLint("NullAnnotationGroup")
@Composable
private fun SimpleChatPage() {
    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(
                elevation = 4.dp,
                title = {
                    Text(
                        "Conversation Page",
                        modifier = Modifier.testTag("tool_bar_name"),
                        fontSize = 30.sp
                    )
                }
            )
        },
        bottomBar = {
            MessageUpdater(
                onMessageAdded = { message, isReceived ->
                    messages.add(Message(message, isReceived = isReceived))
                    coroutineScope.launch { listState.animateScrollToItem(messages.size) }
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            // testTagsAsResourceId and testTag is for compose to map testTag to resource-id.
            // https://developer.android.com/jetpack/compose/testing#uiautomator-interop
            SelectionContainer() { Column { Conversation(messages, listState) } }
        }
    }
}

@Composable
private fun Conversation(messages: List<Message>, state: LazyListState) {
    LazyColumn(
        modifier = Modifier.testTag("messages").fillMaxSize(),
        state = state,
        verticalArrangement = Arrangement.Bottom,
    ) {
        items(messages) { MessageCard(it) }
    }
}

@Composable
private fun MessageCard(message: Message) {
    Row {
        if (!message.isReceived) {
            Spacer(modifier = Modifier.weight(1.0f))
        }
        Column {
            Text(
                message.content,
                fontSize = 20.sp,
                modifier =
                    Modifier.testTag(if (message.isReceived) "message_received" else "message_sent")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageUpdater(onMessageAdded: (message: String, isReceived: Boolean) -> Unit) {
    Row {
        var text by remember { mutableStateOf("") }

        TextField(
            modifier = Modifier.weight(1.0f),
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Input message here") }
        )

        Button(
            onClick = {
                if (text.isNotEmpty()) {
                    onMessageAdded(text, true)
                    text = ""
                }
            }
        ) {
            Text("Receive")
        }

        Button(
            onClick = {
                if (text.isNotEmpty()) {
                    onMessageAdded(text, false)
                    text = ""
                }
            }
        ) {
            Text("Send")
        }
    }
}
