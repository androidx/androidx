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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DialogExample() {

    val scrollState = rememberScrollState()
    Column(Modifier
        .padding(5.dp)
        .verticalScroll(scrollState)
    ) {
        val properties = EditDialogProperties()
        val fillMaxSize = EditBooleanSetting("fillMaxSize", false)
        val windowInsets = EditBooleanSetting("windowInsets", false)

        var open by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Button(onClick = { open = true }) {
                Text("Open Dialog")
            }
        }
        if (open) {
            Dialog(
                onDismissRequest = { open = false },
                properties = properties
            ) {
                var modifier = if (fillMaxSize) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.size(400.dp, 300.dp)
                }
                if (windowInsets) {
                    modifier = modifier.windowInsetsPadding(WindowInsets.systemBars)
                }
                Box(modifier
                    .background(Color.Yellow)
                    .clickable { open = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditDialogProperties(): DialogProperties {
    var dialogProperties by remember { mutableStateOf(DialogProperties()) }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, RoundedCornerShape(5.dp))
            .padding(5.dp)
    ) {
        Text("DialogProperties")
        Spacer(Modifier.height(5.dp))

        val dismissOnBackPress = EditBooleanSetting("dismissOnBackPress", true)
        val dismissOnClickOutside = EditBooleanSetting("dismissOnClickOutside", true)
        val usePlatformDefaultWidth = EditBooleanSetting("usePlatformDefaultWidth", false)
        val usePlatformInsets = EditBooleanSetting("usePlatformInsets", true)
        dialogProperties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            usePlatformInsets = usePlatformInsets
        )
    }
    return dialogProperties
}

@Composable
private fun EditBooleanSetting(label: String, defaultValue: Boolean): Boolean {
    var value by remember { mutableStateOf(defaultValue) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(value, { value = it })
        Text(label)
    }
    return value
}
