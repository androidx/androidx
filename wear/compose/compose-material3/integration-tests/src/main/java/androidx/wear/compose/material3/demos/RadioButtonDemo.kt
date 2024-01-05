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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Text

@Composable
fun RadioButtonDemo() {
    var selectedRadioIndex by remember { mutableIntStateOf(0) }
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxSize().selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader { Text("Radio Button") }
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = selectedRadioIndex == 0,
                onSelected = { selectedRadioIndex = 0 }
            )
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = selectedRadioIndex == 1,
                onSelected = { selectedRadioIndex = 1 }
            )
        }
        item {
            ListHeader { Text("Disabled Radio Button") }
        }
        item {
            DemoRadioButton(enabled = false, selected = true)
        }
        item {
            DemoRadioButton(enabled = false, selected = false)
        }
        item {
            ListHeader { Text("Icon") }
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = true,
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = true,
                secondary = "Secondary label"
            ) {
                Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorite icon")
            }
        }
        item {
            ListHeader { Text("Multi-line") }
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = true,
                primary = "8:15AM",
                secondary = "Monday"
            )
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = true,
                primary = "Primary Label with 3 lines of content max"
            )
        }
        item {
            DemoRadioButton(
                enabled = true,
                selected = true,
                primary = "Primary Label with 3 lines of content max",
                secondary = "Secondary label with 2 lines"
            )
        }
    }
}

@Composable
private fun DemoRadioButton(
    enabled: Boolean,
    selected: Boolean,
    onSelected: () -> Unit = {},
    primary: String = "Primary label",
    secondary: String? = null,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    RadioButton(
        modifier = Modifier.fillMaxWidth(),
        icon = content,
        label = {
            Text(
                primary,
                Modifier.fillMaxWidth(),
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = secondary?.let {
            {
                Text(
                    secondary,
                    Modifier.fillMaxWidth(),
                    maxLines = 2,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        selected = selected,
        onSelected = onSelected,
        enabled = enabled,
    )
}
