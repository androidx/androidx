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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.samples.FilledTextButtonSample
import androidx.wear.compose.material3.samples.FilledTonalTextButtonSample
import androidx.wear.compose.material3.samples.OutlinedTextButtonSample
import androidx.wear.compose.material3.samples.TextButtonSample

@Composable
fun TextButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("Text Button")
            }
        }
        item {
            Row {
                TextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(onClick = { }, enabled = false) {
                    Text(text = "ABC")
                }
            }
        }
        item {
            ListHeader {
                Text("FilledTonalTextButton")
            }
        }
        item {
            Row {
                FilledTonalTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = { },
                    enabled = false,
                    colors = TextButtonDefaults.filledTonalTextButtonColors()
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item {
            ListHeader {
                Text("FilledTextButton")
            }
        }
        item {
            Row {
                FilledTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = { },
                    enabled = false,
                    colors = TextButtonDefaults.filledTextButtonColors()
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item {
            ListHeader {
                Text("OutlinedTextButton")
            }
        }
        item {
            Row {
                OutlinedTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = { },
                    enabled = false,
                    colors = TextButtonDefaults.outlinedTextButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = false)
                ) {
                    Text(text = "ABC")
                }
            }
        }
    }
}