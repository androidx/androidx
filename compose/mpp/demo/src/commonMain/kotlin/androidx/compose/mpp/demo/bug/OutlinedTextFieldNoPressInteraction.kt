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

package androidx.compose.mpp.demo.bug

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// https://github.com/JetBrains/compose-multiplatform/issues/4087
val NoPressInteractionInOutlinedTextField = Screen.Example(
    "No PressInteraction in OutlinedTextField"
) {
    Column {
        Spacer(modifier = Modifier.height(100.dp))

        var interactions by remember { mutableStateOf("Interactions log:\n") }

        val interactionSource = remember { MutableInteractionSource() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions
                .collect {
                    interactions += "$it\n"
                }
        }

        OutlinedTextField(
            value = "Try click me",
            onValueChange = {},
            interactionSource = interactionSource,
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Until the bug is fixed, it doesn't print PressInteractions")
        Text(interactions)
    }
}