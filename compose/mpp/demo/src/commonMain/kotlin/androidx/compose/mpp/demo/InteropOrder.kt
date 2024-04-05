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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InteropOrder() {
    var red by remember { mutableStateOf(true) }
    var green by remember { mutableStateOf(true) }
    var blue by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = { red = !red }) {
            Text("Red")
        }
        Button(onClick = { green = !green }) {
            Text("Green")
        }
        Button(onClick = { blue = !blue }) {
            Text("Blue")
        }

        Box {
            if (red) {
                TestInteropView(Modifier.size(150.dp).offset(0.dp, 0.dp), Color.Red)
            }
            if (green) {
                TestInteropView(Modifier.size(150.dp).offset(75.dp, 75.dp), Color.Green)
            }
            if (blue) {
                TestInteropView(Modifier.size(150.dp).offset(150.dp, 150.dp), Color.Blue)
            }
        }
    }
}

@Composable
internal expect fun TestInteropView(modifier: Modifier, color: Color)
