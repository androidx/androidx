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

package androidx.compose.mpp.demo.components.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

internal val HalfScreenPopup = alignPopupWithModifierExample(
    "HalfScreenPopup",
    Modifier.fillMaxSize(0.5f),
)

internal val FixedSizePopup = alignPopupWithModifierExample(
    "FixedSizePopup",
    Modifier.size(250.dp, 400.dp),
)

@OptIn(ExperimentalLayoutApi::class)
private fun alignPopupWithModifierExample(
    name: String,
    modifier: Modifier = Modifier,
) = Screen.Example(name) {
    var show: Boolean by remember { mutableStateOf(true) }

    val possibleAlignments = listOf(
        "BottomEnd" to Alignment.BottomEnd,
        "BottomCenter" to Alignment.BottomCenter,
        "BottomStart" to Alignment.BottomStart,
        "CenterEnd" to Alignment.CenterEnd,
        "Center" to Alignment.Center,
        "CenterStart" to Alignment.CenterStart,
        "TopEnd" to Alignment.TopEnd,
        "TopCenter" to Alignment.TopCenter,
        "TopStart" to Alignment.TopStart,
    )
    var alignment: Alignment by remember { mutableStateOf(Alignment.Center) }

    if (show) {
        Popup(
            alignment = alignment,
            onDismissRequest = { show = false }
        ) {
            Box(
                modifier
                    .fillMaxSize(0.5f)
                    .background(Color.Yellow.copy(0.5f))
                    .border(2.dp, Color.Green)
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(possibleAlignments) {
                        Button({ alignment = it.second }, enabled = it.second != alignment) {
                            Text(it.first)
                        }
                    }
                }
            }
        }
    }
}
