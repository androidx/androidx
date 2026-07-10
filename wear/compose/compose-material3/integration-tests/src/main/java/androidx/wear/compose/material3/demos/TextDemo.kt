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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text

@Composable
fun TextBlockDemo() {
    val textBlockTypography = MaterialTheme.typography.bodySmall
    val textBlockColor = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current

    var selectedRadioButtonIndex by remember { mutableIntStateOf(0) }

    ScalingLazyDemo {
        item { ListHeader { Text(text = "Text Block") } }

        items(2) { index ->
            val label = "Button $index"
            val secondaryLabel = "with secondary label"
            val additionalText = "Description text in separate block with no truncation"

            Column(
                modifier =
                    Modifier.clearAndSetSemantics {
                        contentDescription = "$label, $secondaryLabel, $additionalText"
                        onClick(
                            action = {
                                showOnClickToast(context)
                                true
                            }
                        )
                        role = Role.Button
                    }
            ) {
                Button(
                    onClick = { showOnClickToast(context) },
                    label = { Text(text = label) },
                    secondaryLabel = { Text(secondaryLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = additionalText,
                    style = textBlockTypography,
                    color = textBlockColor,
                    modifier = Modifier.padding(ButtonDefaults.ContentPadding).fillMaxWidth(),
                )
            }
        }

        items(2) { index ->
            val label = "Checkbox button $index"
            val secondaryLabel = "with secondary label"
            val additionalText =
                "Includes info while entering and exiting lists, grids and other containers"
            var checked by remember { mutableStateOf(false) }

            Column(
                modifier =
                    Modifier.toggleable(
                            value = checked,
                            onValueChange = { checked = it },
                            role = Role.Checkbox,
                        )
                        .clearAndSetSemantics {
                            contentDescription = "$label, $secondaryLabel, $additionalText"
                        }
            ) {
                CheckboxButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    label = { Text(label) },
                    secondaryLabel = { Text(secondaryLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = additionalText,
                    style = textBlockTypography,
                    color = textBlockColor,
                    modifier = Modifier.padding(ButtonDefaults.ContentPadding).fillMaxWidth(),
                )
            }
        }

        items(2) { index ->
            val label = "Switch button $index"
            val secondaryLabel = "with secondary label"
            val additionalText =
                "Bedtime will turn on automatically based on sleep sensing. Speak count for all scrolling items."
            var checked by remember { mutableStateOf(false) }

            Column(
                modifier =
                    Modifier.toggleable(
                            value = checked,
                            onValueChange = { checked = it },
                            role = Role.Switch,
                        )
                        .clearAndSetSemantics {
                            contentDescription = "$label, $secondaryLabel, $additionalText"
                        }
            ) {
                SwitchButton(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    label = { Text(label) },
                    secondaryLabel = { Text(secondaryLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = additionalText,
                    style = textBlockTypography,
                    color = textBlockColor,
                    modifier = Modifier.padding(ButtonDefaults.ContentPadding).fillMaxWidth(),
                )
            }
        }

        items(2) { index ->
            val label = "Radio button $index"
            val secondaryLabel = "with secondary label"
            val additionalText =
                "Bedtime will turn on automatically based on sleep sensing. Speak count for all scrolling items."
            val selected = index == selectedRadioButtonIndex

            Column(
                Modifier.selectable(
                        selected = selected,
                        onClick = { selectedRadioButtonIndex = index },
                        role = Role.RadioButton,
                    )
                    .clearAndSetSemantics {
                        contentDescription = "$label, $secondaryLabel, $additionalText"
                    }
            ) {
                RadioButton(
                    selected = selected,
                    onSelect = { selectedRadioButtonIndex = index },
                    label = { Text(label) },
                    secondaryLabel = { Text(secondaryLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = additionalText,
                    style = textBlockTypography,
                    color = textBlockColor,
                    modifier = Modifier.padding(ButtonDefaults.ContentPadding).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun TextWeightDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text(text = "Custom Weight") } }
        item { ListSubHeader { Text(text = "Labels") } }
        item { Text(text = "Label Small", style = MaterialTheme.typography.labelSmall) }
        item {
            Text(
                text = "Label Small",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight(800),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Label Medium", style = MaterialTheme.typography.labelMedium) }
        item {
            Text(
                text = "Label Medium",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight(800),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Label Large", style = MaterialTheme.typography.labelLarge) }
        item {
            Text(
                text = "Label Large",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight(800),
            )
        }

        item { ListSubHeader { Text(text = "Body") } }
        item { Text(text = "Body Extra Small", style = MaterialTheme.typography.bodyExtraSmall) }
        item {
            Text(
                text = "Body Extra Small",
                style = MaterialTheme.typography.bodyExtraSmall.copy(fontWeight = FontWeight(800)),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Body Small", style = MaterialTheme.typography.bodySmall) }
        item {
            Text(
                text = "Body Small",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight(800)),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { Text(text = "Body Medium", style = MaterialTheme.typography.bodyMedium) }
        item {
            Text(
                text = "Body Medium",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight(800)),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Body Large", style = MaterialTheme.typography.bodyLarge) }
        item {
            Text(
                text = "Body Large",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(800)),
            )
        }
    }
}
