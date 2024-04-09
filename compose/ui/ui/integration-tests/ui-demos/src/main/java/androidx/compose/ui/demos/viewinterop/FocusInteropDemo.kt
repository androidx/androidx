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

package androidx.compose.ui.demos.viewinterop

import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.LinearLayoutCompat.HORIZONTAL
import androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun FocusInteropDemo() {
    Row {
        Column {
            Button(onClick = {}) { Text("1") }
            Button(onClick = {}) { Text("2") }
            Button(onClick = {}) { Text("3") }
            Button(onClick = {}) { Text("4") }
        }
        AndroidView(
            factory = {
                LinearLayoutCompat(it).apply {
                    orientation = HORIZONTAL
                    addView(
                        LinearLayoutCompat(it).apply {
                            orientation = VERTICAL
                            addView(Button(it).apply { text = "5" })
                            addView(Button(it).apply { text = "6" })
                            addView(Button(it).apply { text = "7" })
                            addView(Button(it).apply { text = "8" })
                        }
                    )
                }
            }
        )
        Column {
            Button(onClick = {}) { Text("9") }
            Button(onClick = {}) { Text("10") }
            Button(onClick = {}) { Text("11") }
            Button(onClick = {}) { Text("12") }
        }
        AndroidView(
            factory = {
                LinearLayoutCompat(it).apply {
                    orientation = HORIZONTAL
                    addView(
                        LinearLayoutCompat(it).apply {
                            orientation = VERTICAL
                            addView(Button(it).apply { text = "13" })
                            addView(Button(it).apply { text = "14" })
                            addView(Button(it).apply { text = "15" })
                            addView(Button(it).apply { text = "16" })
                        }
                    )
                }
            }
        )
        Column {
            Button(onClick = {}) { Text("17") }
            Button(onClick = {}) { Text("18") }
            Button(onClick = {}) { Text("19") }
            Button(onClick = {}) { Text("20") }
        }
    }
}
