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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.FilledIconButtonSample
import androidx.wear.compose.material3.samples.FilledTonalIconButtonSample
import androidx.wear.compose.material3.samples.IconButtonSample
import androidx.wear.compose.material3.samples.OutlinedIconButtonSample
import androidx.wear.compose.material3.touchTargetAwareSize

@Composable
fun IconButtonDemo() {
    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("Icon button")
            }
        }
        item {
            Row {
                IconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                IconButton(
                    onClick = { },
                    enabled = false
                ) {
                    StandardIcon(ButtonDefaults.IconSize)
                }
            }
        }
        item {
            ListHeader {
                Text("Filled Tonal")
            }
        }
        item {
            Row {
                FilledTonalIconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                FilledTonalIconButton(
                    onClick = { },
                    enabled = false
                ) {
                    StandardIcon(ButtonDefaults.IconSize)
                }
            }
        }
        item {
            ListHeader {
                Text("Filled")
            }
        }
        item {
            Row {
                FilledIconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                FilledIconButton(
                    onClick = { },
                    enabled = false
                ) {
                    StandardIcon(ButtonDefaults.IconSize)
                }
            }
        }
        item {
            ListHeader {
                Text("Outlined")
            }
        }
        item {
            Row {
                OutlinedIconButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                OutlinedIconButton(
                    onClick = { },
                    enabled = false
                ) {
                    StandardIcon(ButtonDefaults.IconSize)
                }
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
                IconButtonWithSize(IconButtonDefaults.LargeButtonSize)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconButtonWithSize(IconButtonDefaults.DefaultButtonSize)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.SmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconButtonWithSize(IconButtonDefaults.SmallButtonSize)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${IconButtonDefaults.ExtraSmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                IconButtonWithSize(IconButtonDefaults.ExtraSmallButtonSize)
            }
        }
    }
}

@Composable
private fun IconButtonWithSize(size: Dp) {
    FilledTonalIconButton(
        modifier = Modifier.touchTargetAwareSize(size),
        onClick = { /* Do something */ }) {
        StandardIcon(IconButtonDefaults.iconSizeFor(size))
    }
}
