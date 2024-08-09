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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.ButtonSample
import androidx.wear.compose.material3.samples.ButtonWithOnLongClickSample
import androidx.wear.compose.material3.samples.ChildButtonSample
import androidx.wear.compose.material3.samples.ChildButtonWithOnLongClickSample
import androidx.wear.compose.material3.samples.CompactButtonSample
import androidx.wear.compose.material3.samples.CompactButtonWithOnLongClickSample
import androidx.wear.compose.material3.samples.FilledTonalButtonSample
import androidx.wear.compose.material3.samples.FilledTonalButtonWithOnLongClickSample
import androidx.wear.compose.material3.samples.FilledVariantButtonSample
import androidx.wear.compose.material3.samples.OutlinedButtonSample
import androidx.wear.compose.material3.samples.OutlinedButtonWithOnLongClickSample
import androidx.wear.compose.material3.samples.OutlinedCompactButtonSample
import androidx.wear.compose.material3.samples.SimpleChildButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledTonalButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledVariantButtonSample
import androidx.wear.compose.material3.samples.SimpleOutlinedButtonSample

@Composable
fun ButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 slot button") } }
        item {
            Button(
                onClick = {},
                label = {
                    Text(
                        "Filled Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.width(150.dp)
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = {
                    Text(
                        "Filled Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                enabled = false,
                modifier = Modifier.width(150.dp)
            )
        }
        item { ListHeader { Text("3 slot button") } }
        item { ButtonSample() }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            ButtonWithOnLongClickSample({ showOnClickToast(context) }) {
                showOnLongClickToast(context)
            }
        }
    }
}

@Composable
fun FilledTonalButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 slot button") } }
        item { SimpleFilledTonalButtonSample() }
        item {
            FilledTonalButtonWithOnLongClickSample({ showOnClickToast(context) }) {
                showOnLongClickToast(context)
            }
        }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                enabled = false
            )
        }
        item { ListHeader { Text("3 slot button") } }
        item { FilledTonalButtonSample() }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun FilledVariantButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("1 slot button") } }
        item { SimpleFilledVariantButtonSample() }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                enabled = false
            )
        }
        item { ListHeader { Text("3 slot button") } }
        item { FilledVariantButtonSample() }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun OutlinedButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 slot button") } }
        item { SimpleOutlinedButtonSample() }
        item {
            OutlinedButtonWithOnLongClickSample({ showOnClickToast(context) }) {
                showOnLongClickToast(context)
            }
        }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                enabled = false
            )
        }
        item { ListHeader { Text("3 slot button") } }
        item { OutlinedButtonSample() }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun ChildButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 slot button") } }
        item { SimpleChildButtonSample() }
        item {
            ChildButtonWithOnLongClickSample({ showOnClickToast(context) }) {
                showOnLongClickToast(context)
            }
        }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                enabled = false
            )
        }
        item { ListHeader { Text("3 slot button") } }
        item { ChildButtonSample() }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun CompactButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("Label only") } }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Compact Button", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Filled Variant", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Filled Tonal", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Outlined", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item { ListHeader { Text("Icon and Label") } }
        item { CompactButtonSample() }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.filledVariantButtonColors(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Filled Variant", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Filled Tonal", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Outlined", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.childButtonColors(),
                modifier = Modifier.width(150.dp)
            ) {
                Text("Child", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item { ListHeader { Text("Icon only") } }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) }
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.filledTonalButtonColors()
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { StandardIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.childButtonColors()
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            CompactButtonWithOnLongClickSample({ showOnClickToast(context) }) {
                showOnLongClickToast(context)
            }
        }
        item { ListHeader { Text("Expandable") } }
        item { OutlinedCompactButtonSample() }
    }
}

@Composable
fun MultilineButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("3 line label") } }
        item { MultilineButton(enabled = true) }
        item { MultilineButton(enabled = false) }
        item { MultilineButton(enabled = true, icon = { StandardIcon(ButtonDefaults.IconSize) }) }
        item { MultilineButton(enabled = false, icon = { StandardIcon(ButtonDefaults.IconSize) }) }
        item { ListHeader { Text("5 line button") } }
        item { Multiline3SlotButton(enabled = true) }
        item { Multiline3SlotButton(enabled = false) }
        item {
            Multiline3SlotButton(enabled = true, icon = { StandardIcon(ButtonDefaults.IconSize) })
        }
        item {
            Multiline3SlotButton(enabled = false, icon = { StandardIcon(ButtonDefaults.IconSize) })
        }
    }
}

@Composable
fun AvatarButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Label + Avatar") } }
        item { AvatarButton(enabled = true) }
        item { AvatarButton(enabled = false) }
        item { ListHeader { Text("Primary/Secondary + Avatar") } }
        item { Avatar3SlotButton(enabled = true) }
        item { Avatar3SlotButton(enabled = false) }
    }
}

@Composable
fun ButtonBackgroundImageDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Button (Image Background)") } }
        item { ButtonBackgroundImage(painterResource(R.drawable.card_background), enabled = true) }
        item { ButtonBackgroundImage(painterResource(R.drawable.card_background), enabled = false) }
    }
}

@Composable
private fun AvatarButton(enabled: Boolean) =
    MultilineButton(
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = { AvatarIcon() },
        label = { Text("Primary text") }
    )

@Composable
private fun Avatar3SlotButton(enabled: Boolean) =
    Multiline3SlotButton(
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = { AvatarIcon() },
        label = { Text("Primary text") },
        secondaryLabel = { Text("Secondary label") }
    )

@Composable
private fun MultilineButton(
    enabled: Boolean,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    icon: (@Composable BoxScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit = {
        Text(
            text = "Multiline label that include a lot of text and stretches to third line",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    },
) {
    Button(
        onClick = { /* Do something */ },
        icon = icon,
        label = label,
        enabled = enabled,
        colors = colors,
    )
}

@Composable
private fun Multiline3SlotButton(
    enabled: Boolean,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    icon: (@Composable BoxScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit = {
        Text(
            text =
                "Multiline label that include a lot of text and stretches to third line " +
                    "may be truncated",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    },
    secondaryLabel: @Composable RowScope.() -> Unit = {
        Text(
            text = "Secondary label over two lines and should be truncated if longer",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    },
) {
    Button(
        onClick = { /* Do something */ },
        icon = icon,
        label = label,
        secondaryLabel = secondaryLabel,
        enabled = enabled,
        colors = colors,
    )
}

@Composable
private fun ButtonBackgroundImage(painter: Painter, enabled: Boolean) =
    Button(
        modifier = Modifier.sizeIn(maxHeight = ButtonDefaults.Height),
        onClick = { /* Do something */ },
        label = { Text("Label", maxLines = 1) },
        enabled = enabled,
        colors = ButtonDefaults.imageBackgroundButtonColors(painter)
    )
