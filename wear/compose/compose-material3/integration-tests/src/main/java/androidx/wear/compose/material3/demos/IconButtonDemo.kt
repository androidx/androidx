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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.FilledIconButtonSample
import androidx.wear.compose.material3.samples.FilledTonalIconButtonSample
import androidx.wear.compose.material3.samples.IconButtonSample
import androidx.wear.compose.material3.samples.OutlinedIconButtonSample

@Composable
fun IconButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text("Icon Button")
        }
        item {
            Row {
                IconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                IconButton(
                    onClick = { },
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        item {
            Text("FilledTonalIconButton")
        }
        item {
            Row {
                FilledTonalIconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                FilledTonalIconButton(
                    onClick = { },
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        item {
            Text("FilledIconButton")
        }
        item {
            Row {
                FilledIconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                FilledIconButton(
                    onClick = { },
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
        item {
            Text("OutlinedIconButton")
        }
        item {
            Row {
                OutlinedIconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                OutlinedIconButton(
                    onClick = { },
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Localized description"
                    )
                }
            }
        }
    }
}