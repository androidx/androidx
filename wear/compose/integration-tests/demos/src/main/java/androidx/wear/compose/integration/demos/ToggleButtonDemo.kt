/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleButtonDefaults

@Composable
fun ToggleButtons() {
    var toggleButtonsEnabled by remember { mutableStateOf(true) }
    var singularButton1Enabled by remember { mutableStateOf(true) }
    var singularButton2Enabled by remember { mutableStateOf(true) }
    var groupButtonState by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Singular",
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
            Spacer(modifier = Modifier.size(4.dp))
            ToggleButton(
                checked = singularButton1Enabled,
                onCheckedChange = {
                    singularButton1Enabled = it
                },
                enabled = toggleButtonsEnabled,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedBackgroundColor = Color.Yellow,
                    checkedContentColor = Color.Black
                ),
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
            ) {
                if (singularButton1Enabled) {
                    DemoIcon(R.drawable.ic_volume_up_24px)
                } else {
                    DemoIcon(R.drawable.ic_volume_off_24px)
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            ToggleButton(
                checked = singularButton2Enabled,
                onCheckedChange = {
                    singularButton2Enabled = it
                },
                enabled = toggleButtonsEnabled,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedBackgroundColor = Color.Yellow,
                    checkedContentColor = Color.Black
                ),
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            ) {
                DemoIcon(R.drawable.ic_airplanemode_active_24px)
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Grouped",
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
            Spacer(modifier = Modifier.size(4.dp))
            ToggleButton(
                checked = !groupButtonState,
                onCheckedChange = {
                    groupButtonState = !it
                },
                enabled = toggleButtonsEnabled,
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            ) {
                DemoIcon(R.drawable.ic_check_24px)
            }
            Spacer(modifier = Modifier.size(4.dp))
            ToggleButton(
                checked = groupButtonState,
                onCheckedChange = {
                    groupButtonState = it
                },
                enabled = toggleButtonsEnabled,
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            ) {
                DemoIcon(R.drawable.ic_clear_24px)
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Buttons Enabled",
                style = MaterialTheme.typography.caption2,
                color = Color.White
            )
            Spacer(modifier = Modifier.size(4.dp))
            ToggleButton(
                checked = toggleButtonsEnabled,
                onCheckedChange = {
                    toggleButtonsEnabled = it
                },
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            ) {
                DemoIcon(R.drawable.ic_check_24px)
            }
        }
    }
}
