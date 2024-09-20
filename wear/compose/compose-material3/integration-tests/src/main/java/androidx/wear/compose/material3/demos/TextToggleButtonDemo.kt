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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.TextToggleButtonDefaults
import androidx.wear.compose.material3.samples.LargeTextToggleButtonSample
import androidx.wear.compose.material3.samples.TextToggleButtonSample
import androidx.wear.compose.material3.touchTargetAwareSize

@Composable
fun TextToggleButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Text Toggle Button", textAlign = TextAlign.Center) } }
        item {
            Row {
                TextToggleButtonSample() // Enabled and checked
                Spacer(modifier = Modifier.width(5.dp))
                TextToggleButtonsDemo(enabled = true, initialChecked = false)
            }
        }
        item {
            Row {
                TextToggleButtonsDemo(enabled = false, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                TextToggleButtonsDemo(enabled = false, initialChecked = false)
            }
        }
        item {
            ListHeader { Text("Text Toggle Button Shape morphing", textAlign = TextAlign.Center) }
        }
        item {
            Row {
                AnimatedTextToggleButtonsDemo(enabled = true, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                AnimatedTextToggleButtonsDemo(enabled = true, initialChecked = false)
            }
        }
        item {
            Row {
                AnimatedTextToggleButtonsDemo(enabled = false, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                AnimatedTextToggleButtonsDemo(enabled = false, initialChecked = false)
            }
        }
        item {
            ListHeader {
                Text("Text Toggle Button Shape morphing variant", textAlign = TextAlign.Center)
            }
        }
        item {
            Row {
                VariantAnimatedTextToggleButtonsDemo(enabled = true, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                VariantAnimatedTextToggleButtonsDemo(enabled = true, initialChecked = false)
            }
        }
        item {
            Row {
                VariantAnimatedTextToggleButtonsDemo(enabled = false, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                VariantAnimatedTextToggleButtonsDemo(enabled = false, initialChecked = false)
            }
        }
        item { ListHeader { Text("Sizes") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.ExtraLargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                TextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.ExtraLargeButtonSize,
                    textStyle = TextToggleButtonDefaults.extraLargeButtonTextStyle,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                LargeTextToggleButtonSample()
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                TextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.DefaultButtonSize,
                )
            }
        }
        item { ListHeader { Text("Sizes Shape morphing") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.ExtraLargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedTextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.ExtraLargeButtonSize,
                    textStyle = TextToggleButtonDefaults.extraLargeButtonTextStyle,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedTextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.LargeButtonSize,
                    textStyle = TextToggleButtonDefaults.largeButtonTextStyle,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedTextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.DefaultButtonSize,
                )
            }
        }
        item { ListHeader { Text("Sizes Shape morphing variant") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.ExtraLargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedTextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.ExtraLargeButtonSize,
                    textStyle = TextToggleButtonDefaults.extraLargeButtonTextStyle,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedTextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.LargeButtonSize,
                    textStyle = TextToggleButtonDefaults.largeButtonTextStyle,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextToggleButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedTextToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = TextToggleButtonDefaults.DefaultButtonSize,
                )
            }
        }
    }
}

@Composable
private fun TextToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = TextToggleButtonDefaults.DefaultButtonSize,
    textStyle: TextStyle = TextToggleButtonDefaults.defaultButtonTextStyle,
) {
    var checked by remember { mutableStateOf(initialChecked) }
    TextToggleButton(
        checked = checked,
        enabled = enabled,
        modifier = Modifier.touchTargetAwareSize(size),
        onCheckedChange = { checked = !checked },
    ) {
        Text(text = if (checked) "On" else "Off", style = textStyle)
    }
}

@Composable
private fun AnimatedTextToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = TextToggleButtonDefaults.DefaultButtonSize,
    textStyle: TextStyle = TextToggleButtonDefaults.defaultButtonTextStyle
) {
    val checked = remember { mutableStateOf(initialChecked) }
    val interactionSource = remember { MutableInteractionSource() }
    TextToggleButton(
        checked = checked.value,
        enabled = enabled,
        modifier = Modifier.touchTargetAwareSize(size),
        onCheckedChange = { checked.value = !checked.value },
        shape = TextToggleButtonDefaults.animatedShape(interactionSource),
        interactionSource = interactionSource,
    ) {
        Text(text = if (checked.value) "On" else "Off", style = textStyle)
    }
}

@Composable
private fun VariantAnimatedTextToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = TextToggleButtonDefaults.DefaultButtonSize,
    textStyle: TextStyle = TextToggleButtonDefaults.defaultButtonTextStyle
) {
    val checked = remember { mutableStateOf(initialChecked) }
    val interactionSource = remember { MutableInteractionSource() }
    TextToggleButton(
        checked = checked.value,
        enabled = enabled,
        modifier = Modifier.touchTargetAwareSize(size),
        onCheckedChange = { checked.value = !checked.value },
        shape =
            TextToggleButtonDefaults.variantAnimatedShape(
                interactionSource,
                checked = checked.value
            ),
        interactionSource = interactionSource,
    ) {
        Text(text = if (checked.value) "On" else "Off", style = textStyle)
    }
}
