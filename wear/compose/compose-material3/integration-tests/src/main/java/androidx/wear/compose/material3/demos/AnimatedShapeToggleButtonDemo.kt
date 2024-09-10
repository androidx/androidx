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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.TextToggleButtonDefaults

@Composable
fun AnimatedShapeToggleButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Default Toggle") } }
        item {
            Row {
                val checked = remember { mutableStateOf(false) }

                val interactionSource1 = remember { MutableInteractionSource() }

                TextToggleButton(
                    onCheckedChange = { checked.value = !checked.value },
                    shape =
                        TextButtonDefaults.animatedShape(
                            interactionSource1,
                        ),
                    checked = checked.value,
                    interactionSource = interactionSource1,
                ) {
                    Text(text = "ABC")
                }

                Spacer(modifier = Modifier.width(5.dp))

                IconToggleButton(
                    onCheckedChange = { checked.value = !checked.value },
                    shape =
                        IconButtonDefaults.animatedShape(
                            interactionSource1,
                        ),
                    checked = checked.value,
                    interactionSource = interactionSource1,
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }
            }
        }
        item { ListHeader { Text("Toggle Variant") } }
        item {
            Row {
                val checked = remember { mutableStateOf(false) }

                val interactionSource1 = remember { MutableInteractionSource() }
                TextToggleButton(
                    onCheckedChange = { checked.value = !checked.value },
                    shape =
                        TextToggleButtonDefaults.animatedToggleButtonShape(
                            interactionSource1,
                            checked = checked.value,
                        ),
                    checked = checked.value,
                    interactionSource = interactionSource1,
                ) {
                    Text(text = "ABC")
                }

                Spacer(modifier = Modifier.width(5.dp))

                IconToggleButton(
                    onCheckedChange = { checked.value = !checked.value },
                    shape =
                        IconToggleButtonDefaults.animatedToggleButtonShape(
                            interactionSource1,
                            checked = checked.value,
                        ),
                    checked = checked.value,
                    interactionSource = interactionSource1,
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }
            }
        }
    }
}
