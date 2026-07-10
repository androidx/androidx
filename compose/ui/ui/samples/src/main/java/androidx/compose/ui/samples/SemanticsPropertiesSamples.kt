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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.hintText
import androidx.compose.ui.semantics.semantics

@Sampled
@Composable
fun HintTextSample() {
    val label = "Label" // In an application, this hint would typically be a localized String.
    var text by remember { mutableStateOf("") }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.semantics { hintText = label },
        decorationBox = { innerTextField ->
            Box {
                innerTextField()
                if (text.isEmpty()) {
                    // Hide this visual placeholder from talkback to prevent duplicated
                    // announcements, as the parent BasicTextField already provides the hintText.
                    Text(text = label, modifier = Modifier.semantics { hideFromAccessibility() })
                }
            }
        },
    )
}
