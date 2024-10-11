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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.Text

@Composable
fun CheckboxButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Checkbox") } }
        item { DemoCheckboxButton(enabled = true, initiallyChecked = true) }
        item { DemoCheckboxButton(enabled = true, initiallyChecked = false) }
        item { ListHeader { Text("Disabled Checkbox") } }
        item { DemoCheckboxButton(enabled = false, initiallyChecked = true) }
        item { DemoCheckboxButton(enabled = false, initiallyChecked = false) }
        item { ListHeader { Text("Checkbox with Icon") } }
        item {
            DemoCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label",
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item {
            DemoCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label",
                secondary = "Secondary label"
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item { ListHeader { Text("Disabled Checkbox with Icon") } }
        for (initiallyChecked in booleanArrayOf(true, false)) {
            item {
                DemoCheckboxButton(
                    enabled = false,
                    initiallyChecked = initiallyChecked,
                    primary = "Primary label",
                ) {
                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
                }
            }
            item {
                DemoCheckboxButton(
                    enabled = false,
                    initiallyChecked = initiallyChecked,
                    primary = "Primary label",
                    secondary = "Secondary label"
                ) {
                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
                }
            }
        }
        item { ListHeader { Text("Multi-line") } }
        item {
            DemoCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with at most three lines of content"
            )
        }
        item {
            DemoCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with at most three lines of content",
                secondary = "Secondary label with at most two lines of text"
            )
        }
        item {
            DemoCheckboxButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Override the maximum number of primary label content to be four",
                primaryMaxLines = 4,
            )
        }
    }
}

@Composable
private fun DemoCheckboxButton(
    enabled: Boolean,
    initiallyChecked: Boolean,
    primary: String = "Primary label",
    primaryMaxLines: Int? = null,
    secondary: String = "",
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    CheckboxButton(
        modifier = Modifier.fillMaxWidth(),
        icon = content,
        label = {
            Text(
                primary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = primaryMaxLines ?: LocalTextConfiguration.current.maxLines
            )
        },
        secondaryLabel = {
            if (secondary.isNotEmpty()) {
                Text(
                    secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        checked = checked,
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}
