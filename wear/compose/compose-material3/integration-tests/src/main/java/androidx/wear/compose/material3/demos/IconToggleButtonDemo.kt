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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.IconToggleButtonSample
import androidx.wear.compose.material3.touchTargetAwareSize

@Composable
fun IconToggleButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("Icon Toggle Button")
            }
        }
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
        item {
            ListHeader {
                Text("Sizes")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconButtonDefaults.LargeButtonSize
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconButtonDefaults.DefaultButtonSize
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.SmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconButtonDefaults.SmallButtonSize
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.ExtraSmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconToggleButtonsDemo(
                    enabled = true,
                    initialChecked = true,
                    size = IconButtonDefaults.ExtraSmallButtonSize
                )
            }
        }
    }
}

@Composable
private fun IconToggleButtonsDemo(
    enabled: Boolean,
    initialChecked: Boolean,
    size: Dp = IconButtonDefaults.DefaultButtonSize
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
            contentDescription = "Flight Mode",
            modifier = Modifier.size(IconButtonDefaults.iconSizeFor(size))
        )
    }
}
