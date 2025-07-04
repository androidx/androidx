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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.ButtonExtraLargeIconSample
import androidx.wear.compose.material3.samples.ButtonLargeIconSample
import androidx.wear.compose.material3.samples.ButtonSample
import androidx.wear.compose.material3.samples.ChildButtonSample
import androidx.wear.compose.material3.samples.CompactButtonSample
import androidx.wear.compose.material3.samples.CompactButtonWithOnLongClickSample
import androidx.wear.compose.material3.samples.FilledTonalButtonSample
import androidx.wear.compose.material3.samples.FilledVariantButtonSample
import androidx.wear.compose.material3.samples.OutlinedButtonSample
import androidx.wear.compose.material3.samples.OutlinedCompactButtonSample
import androidx.wear.compose.material3.samples.SimpleChildButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledTonalButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledVariantButtonSample
import androidx.wear.compose.material3.samples.SimpleOutlinedButtonSample
import androidx.wear.compose.material3.samples.icons.FavoriteIcon

@Composable
fun BaseButtonDemo() {
    // This demo shows how to use the Base Button overload, which has a single content slot
    // that can be used with a trailing lambda. It should vertically center content by default,
    // but that can easily be changed by using Modifier.align from RowScope in whatever is passed
    // to the content slot.
    ScalingLazyDemo {
        item { ListHeader { Text("Base Button") } }
        item { ListSubHeader { Text("Default alignment") } }
        item { Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Base Button") } }
        item { ListSubHeader { Text("Top Alignment") } }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Base Button", modifier = Modifier.align(Alignment.Top))
            }
        }
    }
}

@Composable
fun ButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 Slot Button") } }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Filled Button") },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Filled Button") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Centered Button") } }
        item {
            Button(
                onClick = {},
                label = {
                    Text(
                        "Filled Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = {
                    Text(
                        "Filled Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("2 Slot Button") } }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                secondaryLabel = { Text("Secondary label") },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                secondaryLabel = { Text("Secondary label") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Icon and Label") } }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("3 Slot Button") } }
        item { ButtonSample(modifier = Modifier.fillMaxWidth()) }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            Button(
                onClick = { showOnClickToast(context) },
                onLongClick = { showOnLongClickToast(context) },
                onLongClickLabel = "Long click",
                label = { Text("Button") },
                secondaryLabel = { Text("with long click") },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Also override the 'click label' to say 'Double tap to press' instead of
                        // the usual 'Double tap to activate'.
                        onClick("press") { false }
                    },
            )
        }
    }
}

@Composable
fun FilledTonalButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 Slot Button") } }
        item { SimpleFilledTonalButtonSample() }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("2 Slot Button") } }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                secondaryLabel = { Text("Secondary label") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                secondaryLabel = { Text("Secondary label") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Icon and Label") } }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
        item { ListHeader { Text("3 Slot Button") } }
        item { FilledTonalButtonSample() }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            FilledTonalButton(
                onClick = { showOnClickToast(context) },
                onLongClick = { showOnLongClickToast(context) },
                onLongClickLabel = "Long click",
                label = { Text("Filled Tonal Button") },
                secondaryLabel = { Text("with long click") },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Also override the 'click label' to say 'Double tap to press' instead of
                        // the usual 'Double tap to activate'.
                        onClick("press") { false }
                    },
            )
        }
    }
}

@Composable
fun FilledVariantButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 Slot Button") } }
        item { SimpleFilledVariantButtonSample() }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("2 Slot Button") } }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                secondaryLabel = { Text("Secondary label") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                secondaryLabel = { Text("Secondary label") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Icon and Label") } }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("3 Slot Button") } }
        item { FilledVariantButtonSample() }
        item {
            Button(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled Variant Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            Button(
                onClick = { showOnClickToast(context) },
                onLongClick = { showOnLongClickToast(context) },
                onLongClickLabel = "Long click",
                colors = ButtonDefaults.filledVariantButtonColors(),
                label = { Text("Filled VariantButton") },
                secondaryLabel = { Text("with long click") },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Also override the 'click label' to say 'Double tap to press' instead of
                        // the usual 'Double tap to activate'.
                        onClick("press") { false }
                    },
            )
        }
    }
}

@Composable
fun OutlinedButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 Slot Button") } }
        item { SimpleOutlinedButtonSample() }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("2 Slot Button") } }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                secondaryLabel = { Text("Secondary label") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                secondaryLabel = { Text("Secondary label") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Icon and Label") } }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("3 Slot Button") } }
        item { OutlinedButtonSample() }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            OutlinedButton(
                onClick = { showOnClickToast(context) },
                onLongClick = { showOnLongClickToast(context) },
                onLongClickLabel = "Long click",
                label = { Text("Outlined Button") },
                secondaryLabel = { Text("with long click") },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Also override the 'click label' to say 'Double tap to press' instead of
                        // the usual 'Double tap to activate'.
                        onClick("press") { false }
                    },
            )
        }
    }
}

@Composable
fun ChildButtonDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("1 Slot Button") } }
        item { SimpleChildButtonSample() }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = {
                    Text(
                        "Child Button",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("2 Slot Button") } }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                secondaryLabel = { Text("Secondary label") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                secondaryLabel = { Text("Secondary label") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Icon and Label") } }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("3 Slot Button") } }
        item { ChildButtonSample() }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            ChildButton(
                onClick = { showOnClickToast(context) },
                onLongClick = { showOnLongClickToast(context) },
                onLongClickLabel = "Long click",
                label = { Text("Child Button") },
                secondaryLabel = { Text("with long click") },
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Also override the 'click label' to say 'Double tap to press' instead of
                        // the usual 'Double tap to activate'.
                        onClick("press") { false }
                    },
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
            ) {
                Text("Compact Button")
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledVariantButtonColors(),
            ) {
                Text("Filled Variant")
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Text("Filled Tonal")
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Text("Outlined")
            }
        }
        item { ListHeader { Text("Icon and Label") } }
        item { CompactButtonSample() }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.ExtraSmallIconSize) },
                colors = ButtonDefaults.filledVariantButtonColors(),
            ) {
                Text("Filled Variant", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.ExtraSmallIconSize) },
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Text("Filled Tonal", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.ExtraSmallIconSize) },
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Text("Outlined", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.ExtraSmallIconSize) },
                colors = ButtonDefaults.childButtonColors(),
            ) {
                Text("Child", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item { ListHeader { Text("Icon only") } }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.SmallIconSize) },
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.filledVariantButtonColors(),
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            )
        }
        item {
            CompactButton(
                onClick = { /* Do something */ },
                icon = { FavoriteIcon(ButtonDefaults.SmallIconSize) },
                colors = ButtonDefaults.childButtonColors(),
            )
        }
        item { ListHeader { Text("Long Click") } }
        item {
            CompactButtonWithOnLongClickSample(
                onClickHandler = { showOnClickToast(context) },
                onLongClickHandler = { showOnLongClickToast(context) },
            )
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
        item { MultilineButton(enabled = true, icon = { FavoriteIcon(ButtonDefaults.IconSize) }) }
        item { MultilineButton(enabled = false, icon = { FavoriteIcon(ButtonDefaults.IconSize) }) }
        item { ListHeader { Text("5 line button") } }
        item { Multiline3SlotButton(enabled = true) }
        item { Multiline3SlotButton(enabled = false) }
        item {
            Multiline3SlotButton(enabled = true, icon = { FavoriteIcon(ButtonDefaults.IconSize) })
        }
        item {
            Multiline3SlotButton(enabled = false, icon = { FavoriteIcon(ButtonDefaults.IconSize) })
        }
    }
}

@Composable
fun ButtonBackgroundImageDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Button (Image Background)") } }
        item { ButtonBackgroundImage(painterResource(R.drawable.card_background), enabled = true) }
        item { ButtonBackgroundImage(painterResource(R.drawable.card_background), enabled = false) }
        item { ListHeader { Text("2 Slot Button") } }
        item {
            Button(
                modifier = Modifier.sizeIn(maxHeight = ButtonDefaults.Height).fillMaxWidth(),
                containerPainter =
                    ButtonDefaults.containerPainter(painterResource(R.drawable.card_background)),
                onClick = { /* Do something */ },
                label = { Text("Label", maxLines = 1) },
                secondaryLabel = { Text("Secondary label", maxLines = 1) },
            )
        }
        item {
            Button(
                modifier = Modifier.sizeIn(maxHeight = ButtonDefaults.Height).fillMaxWidth(),
                containerPainter =
                    ButtonDefaults.containerPainter(painterResource(R.drawable.card_background)),
                onClick = { /* Do something */ },
                label = { Text("Label", maxLines = 1) },
                secondaryLabel = { Text("Secondary label", maxLines = 1) },
                enabled = false,
            )
        }
    }
}

@Composable
fun ImageButtonBuilder() {
    var alignment by remember { mutableStateOf(Alignment.Center) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var enabled by remember { mutableStateOf(true) }
    var sizeToIntrinsics by remember { mutableStateOf(false) }

    ScalingLazyDemo {
        item { ListHeader { Text("Image Button") } }

        item { ListSubHeader { Text("Alignment") } }
        val alignments =
            listOf(
                "Top Start" to Alignment.TopStart,
                "Top Center" to Alignment.TopCenter,
                "Center Start" to Alignment.CenterStart,
                "Center" to Alignment.Center,
                "CenterEnd" to Alignment.CenterEnd,
                "Bottom Center" to Alignment.BottomCenter,
                "Bottom End" to Alignment.BottomEnd,
            )
        items(alignments.size) {
            val (label, alignmentValue) = alignments[it]
            RadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = alignment == alignmentValue,
                onSelect = { alignment = alignmentValue },
                label = { Text(label) },
            )
        }

        item { ListSubHeader { Text("Content Scale") } }
        val contentScales =
            listOf(
                "Crop" to ContentScale.Crop,
                "Fit" to ContentScale.Fit,
                "Inside" to ContentScale.Inside,
                "None" to ContentScale.None,
                "Fill Bounds" to ContentScale.FillBounds,
                "Fill Height" to ContentScale.FillHeight,
                "Fill Width" to ContentScale.FillWidth,
                "Fixed X2" to FixedScale(2f),
            )
        items(contentScales.size) {
            val (label, contentScaleValue) = contentScales[it]
            RadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = contentScale == contentScaleValue,
                onSelect = { contentScale = contentScaleValue },
                label = { Text(label) },
            )
        }

        item { ListSubHeader { Text("Alpha=$alpha") } }
        item {
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                valueRange = 0f..1f,
                steps = 99,
                segmented = false,
            )
        }

        item { ListSubHeader { Text("Enabled") } }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                checked = enabled,
                onCheckedChange = { enabled = it },
                label = { Text("Enabled") },
            )
        }

        item { ListSubHeader { Text("Intrinsic size") } }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                checked = sizeToIntrinsics,
                onCheckedChange = { sizeToIntrinsics = it },
                label = { Text("Used") },
            )
        }

        item { ListHeader { Text("Image Button") } }
        item {
            val painter =
                painterResource(androidx.wear.compose.material3.samples.R.drawable.backgroundimage)

            Button(
                modifier = Modifier.fillMaxWidth(),
                containerPainter =
                    ButtonDefaults.containerPainter(
                        painter,
                        sizeToIntrinsics = sizeToIntrinsics,
                        alignment = alignment,
                        contentScale = contentScale,
                        alpha = alpha,
                    ),
                onClick = { /* Do something */ },
                label = { Text("Label", maxLines = 1) },
                secondaryLabel = { Text("Secondary label", maxLines = 1) },
                enabled = enabled,
            )
        }
    }
}

@Composable
fun AppButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Large Icon") } }
        item { ButtonLargeIcon(enabled = true) }
        item { ButtonLargeIcon(enabled = false) }
        item { ButtonLargeIconSample(enabled = true) }
        item { ButtonLargeIconSample(enabled = false) }
    }
}

@Composable
fun AvatarButtonDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Extra Large Icon") } }
        item { ButtonExtraLargeIcon(enabled = true) }
        item { ButtonExtraLargeIcon(enabled = false) }
        item { ButtonExtraLargeIconSample(enabled = true) }
        item { ButtonExtraLargeIconSample(enabled = false) }
    }
}

@Composable
fun ButtonUpdateAnimationDemo() {
    var text by remember { mutableStateOf("Button Text") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = { text = if (text == "Button Text") "New Text" else "Button Text" },
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(targetState = text) { targetText ->
                Text(
                    text = targetText,
                    modifier = Modifier.animateContentSize().fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ButtonBackgroundImage(painter: Painter, enabled: Boolean) =
    Button(
        modifier = Modifier.sizeIn(maxHeight = ButtonDefaults.Height).fillMaxWidth(),
        containerPainter = ButtonDefaults.containerPainter(painter),
        onClick = { /* Do something */ },
        label = { Text("Label", maxLines = 1) },
        enabled = enabled,
    )

@Composable
private fun ButtonLargeIcon(enabled: Boolean = true) {
    Button(
        onClick = { /* Do something */ },
        label = { Text("Button") },
        icon = { FavoriteIcon(ButtonDefaults.LargeIconSize) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.ButtonWithLargeIconContentPadding,
    )
}

@Composable
private fun ButtonExtraLargeIcon(enabled: Boolean = true) {
    Button(
        onClick = { /* Do something */ },
        label = { Text("Button") },
        icon = { FavoriteIcon(ButtonDefaults.ExtraLargeIconSize) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = ButtonDefaults.ButtonWithExtraLargeIconContentPadding,
    )
}

@Composable
fun ButtonStackDemo() {
    val cornerExtraSmall = MaterialTheme.shapes.extraSmall.topStart
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ScalingLazyDemo {
            item { ListHeader { Text(text = "Button Stack") } }

            item { ButtonStack(cornerExtraSmall) }

            // Additional spacing between button stacks to emphasise the groupings
            item { Spacer(Modifier.height(8.dp)) }

            item { ButtonStack(cornerExtraSmall) }
        }
    }
}

@Composable
private fun ButtonStack(cornerSize: CornerSize) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
            onClick = { /* Do something */ },
            modifier = Modifier.fillMaxWidth(),
            shape = ButtonDefaults.shape.bottom(cornerSize),
            label = { Text("Top", Modifier.fillMaxWidth()) },
        )
        Button(
            onClick = { /* Do something */ },
            modifier = Modifier.fillMaxWidth(),
            shape = ButtonDefaults.shape.copy(cornerSize),
            label = { Text("Middle", Modifier.fillMaxWidth()) },
        )
        Button(
            onClick = { /* Do something */ },
            modifier = Modifier.fillMaxWidth(),
            shape = ButtonDefaults.shape.top(cornerSize),
            label = { Text("Bottom", Modifier.fillMaxWidth()) },
        )
    }
}

@Composable
fun ButtonMergeDemo() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ScalingLazyDemo {
            item { ListHeader { Text(text = "Button Merge") } }

            buttonMerge()

            // Additional spacing to emphasise the groupings
            item { Spacer(Modifier.height(8.dp)) }

            buttonMerge()
        }
    }
}

private fun ScalingLazyListScope.buttonMerge() {
    item {
        val noCorner = CornerSize(0)
        Column {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(ButtonDefaults.CompactButtonHeight),
                shape = ButtonDefaults.shape.bottom(noCorner),
                label = { Text("Top", Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
            ButtonDivider(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.surfaceContainer,
            )
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = ButtonDefaults.shape.copy(noCorner),
                label = { Text("Middle", Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
            ButtonDivider(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.colorScheme.surfaceContainer,
            )
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = ButtonDefaults.shape.top(noCorner),
                label = { Text("Bottom", Modifier.fillMaxWidth()) },
                colors = ButtonDefaults.filledTonalButtonColors(),
            )
        }
    }
}

@Composable
private fun ButtonDivider(height: Dp, contentColor: Color, containerColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().background(containerColor),
        horizontalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(height).fillMaxWidth(0.8f).background(contentColor))
    }
}

private fun CornerBasedShape.top(cornerSize: CornerSize): CornerBasedShape =
    copy(topStart = cornerSize, topEnd = cornerSize)

private fun CornerBasedShape.bottom(cornerSize: CornerSize): CornerBasedShape =
    copy(bottomStart = cornerSize, bottomEnd = cornerSize)
