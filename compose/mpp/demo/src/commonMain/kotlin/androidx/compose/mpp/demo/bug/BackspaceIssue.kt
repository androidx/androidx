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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent

// https://github.com/JetBrains/compose-multiplatform/issues/3855
val BackspaceIssue = Screen.Example("BackspaceIssue") {
    var text by remember { mutableStateOf("Focus to TextField and press backspace") }
    var backSpaceCounter by remember { mutableStateOf(0) }

    Column {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Backspace) {
                        backSpaceCounter++
                        true
                    } else {
                        false
                    }
                },
            value = text,
            onValueChange = {
                text = it
            }
        )
        Text("Backspace pressed $backSpaceCounter times")
    }
}
