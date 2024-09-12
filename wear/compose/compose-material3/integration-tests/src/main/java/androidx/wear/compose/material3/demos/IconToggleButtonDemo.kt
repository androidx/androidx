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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.IconToggleButtonSample
import androidx.wear.compose.material3.samples.icons.WifiOffIcon
import androidx.wear.compose.material3.samples.icons.WifiOnIcon
import androidx.wear.compose.material3.touchTargetAwareSize

@Composable
fun IconToggleButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Icon Toggle Button", textAlign = TextAlign.Center) } }
        item {
            Row {
                IconToggleButtonSample() // Enabled & checked
                Spacer(modifier = Modifier.width(5.dp))
                IconToggleButtonsDemo(enabled = true, initialChecked = false)
            }
        }
        item {
            Row {
                IconToggleButtonsDemo(enabled = false, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                IconToggleButtonsDemo(enabled = false, initialChecked = false)
            }
        }
        item { ListHeader { Text("Shape morphing", textAlign = TextAlign.Center) } }
        item {
            Row {
                AnimatedIconToggleButtonsDemo(enabled = true, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                AnimatedIconToggleButtonsDemo(enabled = true, initialChecked = false)
            }
        }
        item {
            Row {
                AnimatedIconToggleButtonsDemo(enabled = false, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                AnimatedIconToggleButtonsDemo(enabled = false, initialChecked = false)
            }
        }
        item { ListHeader { Text("Shape morphing variant", textAlign = TextAlign.Center) } }
        item {
            Row {
                VariantAnimatedIconToggleButtonsDemo(enabled = true, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                VariantAnimatedIconToggleButtonsDemo(enabled = true, initialChecked = false)
            }
        }
        item {
            Row {
                VariantAnimatedIconToggleButtonsDemo(enabled = false, initialChecked = true)
                Spacer(modifier = Modifier.width(5.dp))
                VariantAnimatedIconToggleButtonsDemo(enabled = false, initialChecked = false)
            }
        }
        item { ListHeader { Text("Sizes") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.ExtraLargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.ExtraLargeButtonSize
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.LargeButtonSize
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.DefaultButtonSize
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.SmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.SmallButtonSize
                )
            }
        }
        item { ListHeader { Text("Sizes Shape morphing") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.ExtraLargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.ExtraLargeButtonSize,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.LargeButtonSize,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.DefaultButtonSize,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.SmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                AnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.SmallButtonSize,
                )
            }
        }
        item { ListHeader { Text("Sizes Shape morphing variant") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.ExtraLargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.ExtraLargeButtonSize,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.LargeButtonSize,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.DefaultButtonSize,
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconToggleButtonDefaults.SmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                VariantAnimatedIconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconToggleButtonDefaults.SmallButtonSize,
                )
            }
        }
    }
}

@Composable
private fun IconToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = IconToggleButtonDefaults.DefaultButtonSize
) {
    var checked by remember { mutableStateOf(initialChecked) }
    IconToggleButton(
        checked = checked,
        enabled = enabled,
        modifier = Modifier.touchTargetAwareSize(size),
        onCheckedChange = { checked = !checked }
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Favorite icon",
            modifier = Modifier.size(IconToggleButtonDefaults.iconSizeFor(size))
        )
    }
}

@Composable
private fun AnimatedIconToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = IconToggleButtonDefaults.DefaultButtonSize
) {
    val checked = remember { mutableStateOf(initialChecked) }
    val interactionSource = remember { MutableInteractionSource() }
    IconToggleButton(
        checked = checked.value,
        enabled = enabled,
        modifier = Modifier.touchTargetAwareSize(size),
        onCheckedChange = { checked.value = !checked.value },
        shape = IconToggleButtonDefaults.animatedShape(interactionSource),
    ) {
        if (checked.value) {
            WifiOnIcon(Modifier.size(IconToggleButtonDefaults.iconSizeFor(size)))
        } else {
            WifiOffIcon(Modifier.size(IconToggleButtonDefaults.iconSizeFor(size)))
        }
    }
}

@Composable
private fun VariantAnimatedIconToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = IconToggleButtonDefaults.DefaultButtonSize
) {
    val checked = remember { mutableStateOf(initialChecked) }
    val interactionSource = remember { MutableInteractionSource() }
    IconToggleButton(
        checked = checked.value,
        enabled = enabled,
        modifier = Modifier.touchTargetAwareSize(size),
        onCheckedChange = { checked.value = !checked.value },
        shape =
            IconToggleButtonDefaults.variantAnimatedShape(
                interactionSource,
                checked = checked.value
            ),
    ) {
        if (checked.value) {
            WifiOnIcon(Modifier.size(IconToggleButtonDefaults.iconSizeFor(size)))
        } else {
            WifiOffIcon(Modifier.size(IconToggleButtonDefaults.iconSizeFor(size)))
        }
    }
}
