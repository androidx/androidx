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

package androidx.tv.material3.samples

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface

@Composable
fun SampleImmersiveList() {
    val items = remember { listOf(Color.Red, Color.Green, Color.Yellow) }
    val selectedItem = remember { mutableStateOf<Color?>(null) }

    // Container
    Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
        val bgColor = selectedItem.value

        // Background
        if (bgColor != null) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(20f / 7).background(bgColor)) {}
        }

        // Rows
        LazyRow(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            items(items) { color ->
                Surface(
                    onClick = {},
                    modifier =
                        Modifier.width(200.dp).aspectRatio(16f / 9).onFocusChanged {
                            if (it.hasFocus) {
                                selectedItem.value = color
                            }
                        },
                    colors =
                        ClickableSurfaceDefaults.colors(
                            containerColor = color,
                            focusedContainerColor = color,
                        ),
                    border =
                        ClickableSurfaceDefaults.border(
                            focusedBorder =
                                Border(
                                    border = BorderStroke(2.dp, Color.White),
                                    inset = 4.dp,
                                )
                        )
                ) {}
            }
        }
    }
}
