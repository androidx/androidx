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

package bugs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.ComposeUITextField
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.UIKit.*

val UIKitRenderSync = Screen.Example("UIKitRenderSync") {
    var text by remember { mutableStateOf("Type something") }
    LazyColumn(Modifier.fillMaxSize()) {
        items(100) { index ->
            when (index % 4) {
                0 -> Text("material.Text $index", Modifier.fillMaxSize().height(40.dp))
                1 -> UIKitView(
                    factory = {
                        val label = UILabel(frame = CGRectZero.readValue())
                        label.text = "UILabel $index"
                        label.textColor = UIColor.blackColor
                        label
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                )
                2 -> TextField(text, onValueChange = { text = it}, Modifier.fillMaxWidth())
                else -> ComposeUITextField(text, onValueChange = { text = it }, Modifier.fillMaxWidth().height(40.dp))
            }
        }
    }
}