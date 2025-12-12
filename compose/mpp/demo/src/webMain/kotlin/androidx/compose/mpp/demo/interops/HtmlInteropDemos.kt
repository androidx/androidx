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

package androidx.compose.mpp.demo.interops

import Map
import Directions
import LazyDirections
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.TextField
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTextAreaElement

val HtmlInteropDemos = Screen.Selection(
    "HtmlInteropDemos",
    Screen.Example("Directions") { Directions() },
    Screen.Example("LazyDirections") { LazyDirections() },
    Screen.Example("Map") { Map() },
    Screen.Example("SyncTextState") { SyncTextState() },
    Screen.Example("SyncTextStateViaParameter") { SyncTextStateViaParameter() },
    Screen.Example("Nested Compose Viewport") {
        NestedComposeViewportDemo()
    }
)


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SyncTextState() {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        val textState = rememberTextFieldState("text line 1\ntext line 2")

        TextField(textState)

        HtmlElementView(
            factory = {
                (document.createElement("textarea") as HTMLTextAreaElement).apply {
                    style.apply {
                        boxSizing = "border-box"
                    }
                }
            },
            update = { input ->
                input.value = textState.text.toString()
            },
            modifier = Modifier.size(300.dp).padding(50.dp)
        )
    }
}

@Composable
fun SyncTextStateViaParameter() {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        val textState = rememberTextFieldState("text line 1\ntext line 2")
        TextField(textState)

        TextInDiv(textState.text.toString())
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TextInDiv(text: String) {
    HtmlElementView(
        factory = {
            (document.createElement("div") as HTMLDivElement).apply {
                innerText = text
            }
        },
        modifier = Modifier.size(300.dp).padding(50.dp),
        update = { div -> div.innerText = text }
    )
}