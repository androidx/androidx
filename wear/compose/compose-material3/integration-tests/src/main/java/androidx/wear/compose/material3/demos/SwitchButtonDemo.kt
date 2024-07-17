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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text

@Composable
fun SwitchButtonDemo() {
    ScalingLazyDemo() {
        item { ListHeader { Text("Switch") } }
        item { DemoSwitchButton(enabled = true, initiallyChecked = true) }
        item { DemoSwitchButton(enabled = true, initiallyChecked = false) }
        item { ListHeader { Text("Disabled Switch") } }
        item { DemoSwitchButton(enabled = false, initiallyChecked = true) }
        item { DemoSwitchButton(enabled = false, initiallyChecked = false) }
        item { ListHeader { Text("Icon") } }
        item {
            DemoSwitchButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label",
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item {
            DemoSwitchButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary label",
                secondary = "Secondary label"
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item { ListHeader { Text("Multi-line") } }
        item {
            DemoSwitchButton(
                enabled = true,
                initiallyChecked = true,
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoSwitchButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with 3 lines of content max"
            )
        }
        item {
            DemoSwitchButton(
                enabled = true,
                initiallyChecked = true,
                primary = "Primary Label with 3 lines of content max",
                secondary = "Secondary label with 2 lines"
            )
        }
    }
}

@Composable
private fun DemoSwitchButton(
    enabled: Boolean,
    initiallyChecked: Boolean,
    primary: String = "Primary label",
    secondary: String = "",
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    var checked by remember { mutableStateOf(initiallyChecked) }
    SwitchButton(
        modifier = Modifier.fillMaxWidth(),
        icon = content,
        label = {
            Text(
                primary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )
        },
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
        },
        checked = checked,
        onCheckedChange = { checked = it },
        enabled = enabled,
    )
}
