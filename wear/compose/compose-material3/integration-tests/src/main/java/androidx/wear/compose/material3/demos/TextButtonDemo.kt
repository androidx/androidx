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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.samples.FilledTextButtonSample
import androidx.wear.compose.material3.samples.FilledTonalTextButtonSample
import androidx.wear.compose.material3.samples.FilledVariantTextButtonSample
import androidx.wear.compose.material3.samples.LargeFilledTonalTextButtonSample
import androidx.wear.compose.material3.samples.OutlinedTextButtonSample
import androidx.wear.compose.material3.samples.TextButtonSample
import androidx.wear.compose.material3.samples.TextButtonWithOnLongClickSample
import androidx.wear.compose.material3.touchTargetAwareSize

@Composable
fun TextButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("Text Button") } }
        item {
            Row {
                TextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(onClick = {}, enabled = false) { Text(text = "ABC") }
            }
        }
        item { ListHeader { Text("Filled Tonal") } }
        item {
            Row {
                FilledTonalTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = {},
                    enabled = false,
                    colors = TextButtonDefaults.filledTonalTextButtonColors()
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("Filled") } }
        item {
            Row {
                FilledTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = {},
                    enabled = false,
                    colors = TextButtonDefaults.filledTextButtonColors()
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("Filled Variant") } }
        item {
            Row {
                FilledVariantTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = {},
                    enabled = false,
                    colors = TextButtonDefaults.filledVariantTextButtonColors()
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("Outlined") } }
        item {
            Row {
                OutlinedTextButtonSample()
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = {},
                    enabled = false,
                    colors = TextButtonDefaults.outlinedTextButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = false)
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("With onLongClick") } }
        item { TextButtonWithOnLongClickSample { showOnLongClickToast(context) } }
        item { ListHeader { Text("Corner Animation") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {},
                    colors = TextButtonDefaults.filledTextButtonColors(),
                    shapes = TextButtonDefaults.animatedShapes(),
                ) {
                    Text(text = "ABC")
                }
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = {},
                    colors = TextButtonDefaults.filledVariantTextButtonColors(),
                    shapes = TextButtonDefaults.animatedShapes(),
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("Morphed Animation") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {},
                    colors = TextButtonDefaults.filledTextButtonColors(),
                    shapes =
                        TextButtonDefaults.animatedShapes(
                            shape = CutCornerShape(5.dp),
                            pressedShape = RoundedCornerShape(5.dp)
                        ),
                ) {
                    Text(text = "ABC")
                }
                Spacer(modifier = Modifier.width(5.dp))
                TextButton(
                    onClick = {},
                    colors = TextButtonDefaults.filledVariantTextButtonColors(),
                    shapes =
                        TextButtonDefaults.animatedShapes(
                            shape = CutCornerShape(5.dp),
                            pressedShape = RoundedCornerShape(5.dp)
                        ),
                ) {
                    Text(text = "ABC")
                }
            }
        }
        item { ListHeader { Text("Sizes") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextButtonDefaults.LargeButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                LargeFilledTonalTextButtonSample()
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextButtonDefaults.DefaultButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                TextButtonWithSize(TextButtonDefaults.DefaultButtonSize)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${TextButtonDefaults.SmallButtonSize.value.toInt()}dp")
                Spacer(Modifier.width(4.dp))
                TextButtonWithSize(TextButtonDefaults.SmallButtonSize)
            }
        }
    }
}

@Composable
private fun TextButtonWithSize(size: Dp) {
    TextButton(
        modifier = Modifier.touchTargetAwareSize(size),
        onClick = {},
        enabled = true,
        colors = TextButtonDefaults.filledTonalTextButtonColors()
    ) {
        Text(text = "ABC", style = textStyleFor(size))
    }
}

@Composable
private fun textStyleFor(size: Dp): TextStyle =
    when {
        size >= TextButtonDefaults.LargeButtonSize -> TextButtonDefaults.largeButtonTextStyle
        size <= TextButtonDefaults.SmallButtonSize -> TextButtonDefaults.smallButtonTextStyle
        else -> TextButtonDefaults.defaultButtonTextStyle
    }
