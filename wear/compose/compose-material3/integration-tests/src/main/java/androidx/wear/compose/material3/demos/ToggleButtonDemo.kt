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

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material3.Checkbox
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SplitToggleButton
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton
import androidx.wear.compose.material3.samples.SplitToggleButtonWithCheckbox
import androidx.wear.compose.material3.samples.SplitToggleButtonWithRadioButton
import androidx.wear.compose.material3.samples.SplitToggleButtonWithSwitch
import androidx.wear.compose.material3.samples.ToggleButtonWithCheckbox
import androidx.wear.compose.material3.samples.ToggleButtonWithRadioButton
import androidx.wear.compose.material3.samples.ToggleButtonWithSwitch

val toggleButtonDemos = listOf(
    DemoCategory(
        "Samples",
        listOf(
            ComposableDemo("ToggleButtonWithCheckbox sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    ToggleButtonWithCheckbox()
                }
            },
            ComposableDemo("ToggleButtonWithSwitch sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    ToggleButtonWithSwitch()
                }
            },
            ComposableDemo("ToggleButtonWithRadioButton sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    ToggleButtonWithRadioButton()
                }
            },
            ComposableDemo("SplitToggleButtonWithCheckbox sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    SplitToggleButtonWithCheckbox()
                }
            },
            ComposableDemo("SplitToggleButtonWithSwitch sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    SplitToggleButtonWithSwitch()
                }
            },
            ComposableDemo("SplitToggleButtonWithRadioButton sample") {
                Centralize(Modifier.padding(horizontal = 10.dp)) {
                    SplitToggleButtonWithRadioButton()
                }
            },
        )
    ),
    DemoCategory("Demos", listOf(
        ComposableDemo("ToggleButton demos") {
            ToggleButtonDemos()
        },
        ComposableDemo("SplitToggleButton demos") {
            SplitToggleButtonDemos()
        }
    ))
)

@Composable
private fun ToggleButtonDemos() {
    var enabledState by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            val buttonState = if (enabledState) "Enabled" else "Disabled"
            ListHeader { Text(text = "State: $buttonState") }
        }
        item {
            Switch(checked = enabledState, onCheckedChange = { enabledState = it })
        }
        item {
            ListHeader { Text(text = "State:") }
        }
        addToggleButtonVariations(enabled = enabledState, primaryLabel = "Primary Label")

        addToggleButtonVariations(enabled = enabledState, icon = {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "icon")
        }, primaryLabel = "Primary Label")

        addToggleButtonVariations(enabled = enabledState, icon = {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "icon")
        }, primaryLabel = "Primary", secondaryLabel = "Secondary")

        addToggleButtonVariations(
            enabled = enabledState,
            primaryLabel = "Primary Label",
            secondaryLabel = "Secondary"
        )
        addToggleButtonVariations(
            enabled = enabledState,
            primaryLabel = "Primary Label with 3 lines of content max"
        )
        addToggleButtonVariations(
            enabled = enabledState,
            primaryLabel = "Primary Label with 3 lines of content max",
            secondaryLabel = "Secondary label with 2 lines"
        )
    }
}

@Composable
private fun SplitToggleButtonDemos() {
    val context = LocalContext.current
    var enabledState by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            val buttonState = if (enabledState) "Enabled" else "Disabled"
            ListHeader { Text(text = "State: $buttonState") }
        }
        item {
            Switch(checked = enabledState, onCheckedChange = { enabledState = it })
        }
        item {
            ListHeader { Text(text = "State:") }
        }
        addSplitToggleButtonVariations(
            enabled = enabledState,
            primaryLabel = "8:15AM",
            secondaryLabel = "Mon, Tue, Wed",
            context = context
        )

        addSplitToggleButtonVariations(
            enabled = enabledState,
            primaryLabel = "Primary Label with 3 lines of content max",
            secondaryLabel = "Secondary label 2 lines",
            context = context
        )
    }
}

private fun ScalingLazyListScope.addToggleButtonVariations(
    enabled: Boolean,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    primaryLabel: String,
    secondaryLabel: String? = null
) {
    val selectionControls =
        listOf(ToggleControl.CHECKBOX, ToggleControl.SWITCH, ToggleControl.RADIO)
    for (selectionControl in selectionControls) {
        item {
            var checked by remember {
                mutableStateOf(true)
            }

            ToggleButton(checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    checked = it
                },
                icon = icon,
                label = {
                    Text(primaryLabel)
                }, selectionControl = {

                    when (selectionControl) {
                        ToggleControl.CHECKBOX -> Checkbox(checked = checked, enabled = enabled)
                        ToggleControl.SWITCH -> Switch(checked = checked, enabled = enabled)
                        ToggleControl.RADIO -> RadioButton(selected = checked, enabled = enabled)
                    }
                },
                secondaryLabel = {
                    secondaryLabel?.let {
                        Text(secondaryLabel)
                    }
                }
            )
        }
    }
}

private fun ScalingLazyListScope.addSplitToggleButtonVariations(
    enabled: Boolean,
    primaryLabel: String,
    secondaryLabel: String? = null,
    context: Context
) {
    val selectionControls =
        listOf(ToggleControl.CHECKBOX, ToggleControl.SWITCH, ToggleControl.RADIO)
    for (selectionControl in selectionControls) {
        item {
            var checked by remember {
                mutableStateOf(true)
            }

            SplitToggleButton(checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    checked = it
                },
                onClick = {
                    Toast.makeText(
                        context, "Text was clicked",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                label = {
                    Text(primaryLabel)
                }, selectionControl = {

                    when (selectionControl) {
                        ToggleControl.CHECKBOX -> Checkbox(checked = checked, enabled = enabled)
                        ToggleControl.SWITCH -> Switch(checked = checked, enabled = enabled)
                        ToggleControl.RADIO -> RadioButton(selected = checked, enabled = enabled)
                    }
                },
                secondaryLabel = {
                    secondaryLabel?.let {
                        Text(secondaryLabel)
                    }
                }
            )
        }
    }
}

private enum class ToggleControl {
    CHECKBOX, RADIO, SWITCH
}
