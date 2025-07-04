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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Sampled
@Preview
@Composable
fun ListHeaderSample() {
    val scrollState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = scrollState, modifier = Modifier.background(Color.Black)) {
        contentPadding ->
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = contentPadding,
        ) {
            item { ListHeader { Text("Settings") } }
            item {
                ListSubHeader(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_connectivity),
                            contentDescription = "Connectivity",
                        )
                    },
                    label = { Text("Connectivity") },
                )
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_bluetooth),
                            contentDescription = "Bluetooth",
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                ) {
                    Text("Bluetooth")
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_wifi),
                            contentDescription = "Wifi",
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                ) {
                    Text("Wifi")
                }
            }
            item { ListSubHeader { Text("Display") } }
            item {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                    Text("Change Watchface")
                }
            }
            item { Button(modifier = Modifier.fillMaxWidth(), onClick = {}) { Text("Brightness") } }
        }
    }
}
