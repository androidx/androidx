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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults

@Composable
fun AnimatedShapeButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Animated Text Button") } }
        item {
            Row {
                TextButton(
                    onClick = {},
                    shapes = TextButtonDefaults.animatedShapes(),
                ) {
                    Text(text = "ABC")
                }

                Spacer(modifier = Modifier.width(5.dp))

                TextButton(
                    onClick = {},
                    shapes =
                        TextButtonDefaults.animatedShapes(
                            shape = CutCornerShape(15.dp),
                            pressedShape = RoundedCornerShape(15.dp),
                        ),
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("Animated Icon Button") } }
        item {
            Row {
                IconButton(
                    onClick = {},
                    shapes = IconButtonDefaults.animatedShapes(),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }

                Spacer(modifier = Modifier.width(5.dp))

                IconButton(
                    onClick = {},
                    shapes =
                        IconButtonDefaults.animatedShapes(
                            shape = CutCornerShape(15.dp),
                            pressedShape = RoundedCornerShape(15.dp)
                        ),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }
            }
        }
        item { ListHeader { Text("Animated Filled Icon Button") } }
        item {
            Row {
                FilledIconButton(
                    onClick = {},
                    shapes = IconButtonDefaults.animatedShapes(),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }

                Spacer(modifier = Modifier.width(5.dp))

                FilledIconButton(
                    onClick = {},
                    shapes =
                        IconButtonDefaults.animatedShapes(
                            shape = CutCornerShape(15.dp),
                            pressedShape = RoundedCornerShape(15.dp)
                        ),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }
            }
        }
        item { ListHeader { Text("Animated Filled Tonal Icon Button") } }
        item {
            Row {
                FilledTonalIconButton(
                    onClick = {},
                    shapes = IconButtonDefaults.animatedShapes(),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }

                Spacer(modifier = Modifier.width(5.dp))

                FilledTonalIconButton(
                    onClick = {},
                    shapes =
                        IconButtonDefaults.animatedShapes(
                            shape = CutCornerShape(15.dp),
                            pressedShape = RoundedCornerShape(15.dp)
                        ),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }
            }
        }
        item { ListHeader { Text("Animated Outlined Icon Button") } }
        item {
            Row {
                OutlinedIconButton(
                    onClick = {},
                    shapes = IconButtonDefaults.animatedShapes(),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }

                Spacer(modifier = Modifier.width(5.dp))

                OutlinedIconButton(
                    onClick = {},
                    shapes =
                        IconButtonDefaults.animatedShapes(
                            shape = CutCornerShape(15.dp),
                            pressedShape = RoundedCornerShape(15.dp)
                        ),
                ) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
                }
            }
        }
    }
}
