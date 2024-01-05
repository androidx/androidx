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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton

@Composable
fun ToggleButtonDemo() {
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader { Text("Checkbox") }
        }
        item {
            DemoToggleCheckbox(enabled = true, initiallyChecked = true)
        }
        item {
            DemoToggleCheckbox(enabled = true, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Disabled Checkbox") }
        }
        item {
            DemoToggleCheckbox(enabled = false, initiallyChecked = true)
        }
        item {
            DemoToggleCheckbox(enabled = false, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Switch") }
        }
        item {
            DemoToggleSwitch(enabled = true, initiallyChecked = true)
        }
        item {
            DemoToggleSwitch(enabled = true, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Disabled Switch") }
        }
        item {
            DemoToggleSwitch(enabled = false, initiallyChecked = true)
        }
        item {
            DemoToggleSwitch(enabled = false, initiallyChecked = false)
        }
        item {
            ListHeader { Text("Icon") }
        }
        item {
            DemoToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label",
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item {
            DemoToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label",
                secondary = "Secondary label"
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item {
            ListHeader { Text("Multi-line") }
        }
        item {
            DemoToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with 3 lines of content max"
            )
        }
        item {
            DemoToggleCheckbox(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with 3 lines of content max",
                secondary = "Secondary label with 2 lines"
            )
        }
    }
}

@Composable
private fun DemoToggleCheckbox(
    enabled: Boolean,
    initiallyChecked: Boolean,
    primary: String = "Primary label",
    secondary: String = "",
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    ToggleButton(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                primary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = content,
        checked = checked,
        toggleControl = { Checkbox() },
        onCheckedChange = { checked = it },
        enabled = enabled,
        secondaryLabel = {
            if (secondary.isNotEmpty()) {
                Text(
                    secondary,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun DemoToggleSwitch(enabled: Boolean, initiallyChecked: Boolean) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    ToggleButton(
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                "Primary label",
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )
        },
        checked = checked,
        toggleControl = { Switch() },
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}
