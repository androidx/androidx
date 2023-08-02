/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.OutlinedChip
import androidx.wear.compose.material.OutlinedCompactChip
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun StandardChips() {
    var enabled by remember { mutableStateOf(true) }
    var chipStyle by remember { mutableStateOf(ChipStyle.Primary) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        item {
            Text(
                text = "Chip with Label",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1,
                color = Color.White
            )
        }
        item {
            DemoLabelChip(
                style = chipStyle,
                label = "Single Label",
                colors = chipColors(chipStyle),
                enabled = enabled,
            )
        }
        item {
            DemoLabelChip(
                style = chipStyle,
                label = "Standard chip with long label to show truncation which does not fit into" +
                    " 2 lines",
                colors = chipColors(chipStyle),
                enabled = enabled,
            )
        }
        item {
            Text(
                "Chip with icon",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1
            )
        }
        item {
            DemoIconChip(
                style = chipStyle,
                colors = chipColors(chipStyle),
                label = "Label with icon",
                enabled = enabled,
            ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        }
        item {
            DemoIconChip(
                style = chipStyle,
                colors = chipColors(chipStyle),
                label = "Long label to show truncation which does not fit into" +
                    " 2 lines",
                enabled = enabled,
            ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        }
        item {
            Text(
                "Main + Secondary label",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption1
            )
        }
        item {
            DemoLabelChip(
                style = chipStyle,
                label = "Main label and",
                secondaryLabel = "Secondary label",
                colors = chipColors(chipStyle),
                enabled = enabled,
            )
        }
        item {
            DemoLabelChip(
                style = chipStyle,
                label = "Long label to show truncation which does not fit into" +
                    " 1 line",
                secondaryLabel = "Secondary Label",
                colors = chipColors(chipStyle),
                enabled = enabled,
            )
        }
        item {
            DemoIconChip(
                style = chipStyle,
                colors = chipColors(chipStyle),
                label = "Label with icon and",
                secondaryLabel = "Secondary Label",
                enabled = enabled,
            ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        }
        item {
            DemoIconChip(
                style = chipStyle,
                colors = chipColors(chipStyle),
                label = "Long label with truncation",
                secondaryLabel = "Long secondary label to show truncation which does not fit into" +
                    "1 line",
                enabled = enabled,
            ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        }
        item {
            DemoLabelChip(
                style = chipStyle,
                label = "Chip with custom shape",
                colors = chipColors(chipStyle),
                enabled = enabled,
                shape = CutCornerShape(4.dp)
            )
        }
        item {
            ChipCustomizer(
                enabled = enabled,
                chipStyle = chipStyle,
                onChipStyleChanged = { chipStyle = it },
                onEnabledChanged = { enabled = it },
            )
        }
    }
}

@Composable
fun SmallChips() {
    var enabled by remember { mutableStateOf(true) }
    var chipStyle by remember { mutableStateOf(ChipStyle.Primary) }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 15.dp,
            bottom = 50.dp
        )
    ) {
        item {
            Text(
                text = "Compact Chip",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
        }
        item {
            DemoIconCompactChip(
                onClick = {},
                colors = chipColors(chipStyle),
                label = "Label",
                enabled = enabled,
                style = chipStyle
            )
        }
        item {
            DemoIconCompactChip(
                onClick = {},
                colors = chipColors(chipStyle),
                label = "Long label to show truncation which does not fit into 1 line",
                enabled = enabled,
                style = chipStyle
            )
        }
        item {
            DemoIconCompactChip(
                onClick = {},
                colors = chipColors(chipStyle),
                label = "Label with icon",
                enabled = enabled,
                style = chipStyle
            ) {
                DemoIcon(
                    resourceId = R.drawable.ic_accessibility_24px,
                    modifier = Modifier
                        .size(ChipDefaults.SmallIconSize)
                        .wrapContentSize(align = Alignment.Center)
                )
            }
        }
        item {
            DemoIconCompactChip(
                onClick = {},
                colors = chipColors(chipStyle),
                label =
                "Label with icon to show truncation which does not fit into 1 line",
                enabled = enabled,
                style = chipStyle
            ) {
                DemoIcon(
                    resourceId = R.drawable.ic_accessibility_24px,
                    modifier = Modifier
                        .size(ChipDefaults.SmallIconSize)
                        .wrapContentSize(align = Alignment.Center)
                )
            }
        }
        item {
            DemoIconCompactChip(
                onClick = {},
                colors = chipColors(chipStyle),
                label = "Compact Chip with custom shape",
                enabled = enabled,
                style = chipStyle,
                shape = CutCornerShape(4.dp)
            )
        }
        item {
            ChipCustomizer(
                enabled = enabled,
                chipStyle = chipStyle,
                onChipStyleChanged = { chipStyle = it },
                onEnabledChanged = { enabled = it },
            )
        }
    }
}

@Composable
fun AvatarChips() {
    var enabled by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        item {
            ListHeader {
                Text(text = "Chips with avatars")
            }
        }
        item {
            DemoIconChip(
                style = ChipStyle.Secondary,
                label = "Chip with text icon",
                colors = ChipDefaults.secondaryChipColors(),
                enabled = enabled,
            ) {
                TextIcon(
                    text = "M",
                    size = ChipDefaults.LargeIconSize,
                    style = MaterialTheme.typography.title3
                )
            }
        }
        item {
            DemoIconChip(
                style = ChipStyle.Secondary,
                label = "Chip with text icon",
                secondaryLabel = "And secondary label",
                colors = ChipDefaults.secondaryChipColors(),
                enabled = enabled,
            ) {
                TextIcon(
                    text = "M",
                    size = ChipDefaults.LargeIconSize,
                    style = MaterialTheme.typography.title3
                )
            }
        }
        item {
            ListHeader {
                Text(text = "Small Avatar Chips")
            }
        }
        item {
            DemoIconChip(
                style = ChipStyle.Secondary,
                label = "App Title",
                secondaryLabel = "Defaults",
                colors = ChipDefaults.secondaryChipColors(),
                enabled = enabled,
            ) {
                DemoImage(resourceId = R.drawable.ic_maps_icon)
            }
        }
        item {
            DemoIconChip(
                style = ChipStyle.Secondary,
                label = "App title",
                secondaryLabel = "Default gradient",
                colors = ChipDefaults.gradientBackgroundChipColors(),
                enabled = enabled,
            ) {
                DemoImage(resourceId = R.drawable.ic_maps_icon)
            }
        }
        item {
            DemoIconChip(
                style = ChipStyle.Secondary,
                label = "Custom Gradient Color",
                secondaryLabel = "Matching Secondary Label Color",
                secondaryLabelColor = AlternatePrimaryColor3,
                colors = ChipDefaults.gradientBackgroundChipColors(
                    startBackgroundColor = AlternatePrimaryColor3.copy(alpha = 0.325f)
                        .compositeOver(MaterialTheme.colors.surface.copy(alpha = 0.75f)),
                ),
                enabled = enabled,
            ) {
                DemoImage(resourceId = R.drawable.ic_maps_icon)
            }
        }
        item {
            ToggleChip(
                checked = enabled,
                onCheckedChange = { enabled = it },
                label = {
                    Text("Chips enabled")
                },
                // For Switch  toggle controls the Wear Material UX guidance is to set the
                // unselected toggle control color to ToggleChipDefaults.switchUncheckedIconColor()
                // rather than the default.
                colors = ToggleChipDefaults.toggleChipColors(
                    uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
                ),
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = enabled),
                        contentDescription = if (enabled) "On" else "Off"
                    )
                }
            )
        }
    }
}

@Composable
fun RtlChips() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ScalingLazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 15.dp,
                bottom = 50.dp
            )
        ) {
            item {
                Text(
                    text = "Right to left chips",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption1,
                    color = Color.White
                )
            }
            item {
                DemoLabelChip(
                    style = ChipStyle.Primary,
                    label = "Standard chip",
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
            item {
                DemoLabelChip(
                    style = ChipStyle.Primary,
                    label = "Standard chip with long label to show truncation " +
                        "which does not fit into 2 lines",
                    colors = ChipDefaults.primaryChipColors(),
                )
            }
            item {
                DemoIconChip(
                    style = ChipStyle.Primary,
                    colors = ChipDefaults.primaryChipColors(),
                    label = "Standard chip with ",
                    secondaryLabel = "Secondary Label",
                ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
            }
            item {
                CompactChip(
                    onClick = {},
                    colors = ChipDefaults.primaryChipColors(),
                    label = {
                        Text(
                            "Compact chip with label & icon", maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        DemoIcon(
                            resourceId = R.drawable.ic_accessibility_24px,
                            modifier = Modifier.size(ChipDefaults.SmallIconSize)
                        )
                    },
                )
            }
            item {
                DemoIconChip(
                    style = ChipStyle.Secondary,
                    label = "Chip with text icon",
                    colors = ChipDefaults.secondaryChipColors(),
                ) {
                    TextIcon(
                        text = "M",
                        size = ChipDefaults.LargeIconSize,
                        style = MaterialTheme.typography.title3
                    )
                }
            }
            item {
                DemoIconChip(
                    style = ChipStyle.Secondary,
                    label = "Standard chip with",
                    secondaryLabel = "Default gradient color",
                    colors = ChipDefaults.gradientBackgroundChipColors(),
                ) {
                    DemoImage(resourceId = R.drawable.ic_maps_icon)
                }
            }
        }
    }
}

@Composable
fun CustomChips() {
    val applicationContext = LocalContext.current
    var enabled by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 15.dp,
            bottom = 50.dp
        )
    ) {
        item {
            MaterialTheme(colors = MaterialTheme.colors.copy(primary = AlternatePrimaryColor1)) {
                DemoIconChip(
                    style = ChipStyle.Primary,
                    label = "Overridden Theme Primary + Icon",
                    colors = ChipDefaults.primaryChipColors(),
                    enabled = enabled,
                ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                CompactChip(
                    onClick = {
                        Toast.makeText(
                            applicationContext, "Compact chip with custom color",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ChipDefaults.secondaryChipColors(
                        contentColor = AlternatePrimaryColor2
                    ),
                    icon = {
                        DemoIcon(
                            resourceId = R.drawable.ic_accessibility_24px,
                            modifier = Modifier.size(ChipDefaults.IconSize)
                        )
                    },
                    enabled = enabled,
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                CompactChip(
                    onClick = {
                        Toast.makeText(
                            applicationContext, "Fixed width chip with custom icon color",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.width(100.dp),
                    colors = ChipDefaults.secondaryChipColors(
                        contentColor = AlternatePrimaryColor3
                    ),
                    icon = {
                        DemoIcon(
                            resourceId = R.drawable.ic_accessibility_24px,
                            modifier = Modifier.size(ChipDefaults.IconSize)
                        )
                    },
                    enabled = enabled,
                )
            }
        }
        item {
            ToggleChip(
                checked = enabled,
                onCheckedChange = { enabled = it },
                label = {
                    Text("Chips enabled")
                },
                // For Switch  toggle controls the Wear Material UX guidance is to set the
                // unselected toggle control color to ToggleChipDefaults.switchUncheckedIconColor()
                // rather than the default.
                colors = ToggleChipDefaults.toggleChipColors(
                    uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
                ),
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = enabled),
                        contentDescription = if (enabled) "On" else "Off"
                    )
                }
            )
        }
    }
}

@Composable
fun ImageBackgroundChips() {
    var enabled by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 15.dp,
            bottom = 50.dp
        )
    ) {
        item {
            DemoLabelChip(
                style = ChipStyle.Secondary,
                label = "Custom background image",
                colors = ChipDefaults.imageBackgroundChipColors(
                    backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1),
                ),
                enabled = enabled,
            )
        }
        item {
            DemoLabelChip(
                style = ChipStyle.Secondary,
                label = "Custom background image",
                secondaryLabel = "with secondary label",
                colors = ChipDefaults.imageBackgroundChipColors(
                    backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1),
                ),
                enabled = enabled,
            )
        }
        item {
            ToggleChip(
                checked = enabled,
                onCheckedChange = { enabled = it },
                label = {
                    Text("Chips enabled")
                },
                // For Switch  toggle controls the Wear Material UX guidance is to set the
                // unselected toggle control color to ToggleChipDefaults.switchUncheckedIconColor()
                // rather than the default.
                colors = ToggleChipDefaults.toggleChipColors(
                    uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
                ),
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = enabled),
                        contentDescription = if (enabled) "On" else "Off"
                    )
                }
            )
        }
    }
}

@Composable
private fun ChipCustomizer(
    enabled: Boolean,
    chipStyle: ChipStyle,
    onEnabledChanged: ((enabled: Boolean) -> Unit),
    onChipStyleChanged: ((chipStyle: ChipStyle) -> Unit),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Chip color",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.White
        )
        var i = 0
        while (i < ChipStyle.values().size) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(35.dp),
            ) {
                ChipStyleChip(
                    chipStyle = ChipStyle.values()[i],
                    selectedChipStyle = chipStyle,
                    onChipStyleChanged = onChipStyleChanged
                )
                if (++i < ChipStyle.values().size) {
                    ChipStyleChip(
                        chipStyle = ChipStyle.values()[i],
                        selectedChipStyle = chipStyle,
                        onChipStyleChanged = onChipStyleChanged
                    )
                    i++
                }
            }
        }
        ToggleChip(
            checked = enabled,
            onCheckedChange = onEnabledChanged,
            label = {
                Text("Chips enabled")
            },
            // For Switch  toggle controls the Wear Material UX guidance is to set the
            // unselected toggle control color to ToggleChipDefaults.switchUncheckedIconColor()
            // rather than the default.
            colors = ToggleChipDefaults.toggleChipColors(
                uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
            ),
            toggleControl = {
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked = enabled),
                    contentDescription = if (enabled) "On" else "Off"
                )
            }
        )
    }
}

@Composable
private fun ChipStyleChip(
    chipStyle: ChipStyle,
    selectedChipStyle: ChipStyle,
    onChipStyleChanged: ((chipStyle: ChipStyle) -> Unit),
) {
    ToggleButton(
        checked = selectedChipStyle == chipStyle,
        onCheckedChange = {
            onChipStyleChanged(chipStyle)
        },
    ) {
        Text(
            style = MaterialTheme.typography.caption2,
            modifier = Modifier.padding(4.dp),
            text = chipStyle.toString(),
        )
    }
}

@Composable
private fun chipColors(chipStyle: ChipStyle) =
    when (chipStyle) {
        ChipStyle.Primary -> ChipDefaults.primaryChipColors()
        ChipStyle.Secondary -> ChipDefaults.secondaryChipColors()
        ChipStyle.Child -> ChipDefaults.childChipColors()
        ChipStyle.Outlined -> ChipDefaults.outlinedChipColors()
    }

enum class ChipStyle {
    Primary,
    Secondary,
    Child,
    Outlined
}

@Composable
internal fun DemoIconCompactChip(
    colors: ChipColors,
    label: String,
    style: ChipStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit) = {},
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    val maxLabelLines = 1
    if (style != ChipStyle.Outlined) {
        CompactChip(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            label = {
                Text(
                    text = label, maxLines = maxLabelLines,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = content,
            shape = shape,
            enabled = enabled,
        )
    } else {
        OutlinedCompactChip(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            label = {
                Text(
                    text = label, maxLines = maxLabelLines,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = content,
            shape = shape,
            enabled = enabled,
        )
    }
}

@Composable
internal fun DemoIconChip(
    colors: ChipColors,
    label: String,
    style: ChipStyle,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    secondaryLabelColor: Color? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit) = {},
    shape: Shape = MaterialTheme.shapes.small,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    if (style != ChipStyle.Outlined) {
        Chip(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            label = {
                Text(
                    text = label, maxLines = maxLabelLines,
                    overflow = TextOverflow.Ellipsis
                )
            },
            secondaryLabel = secondaryLabel?.let {
                {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            (secondaryLabelColor ?: colors.contentColor(enabled = enabled).value)
                    ) {
                        Text(
                            text = secondaryLabel,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            icon = content,
            shape = shape,
            enabled = enabled,
        )
    } else {
        OutlinedChip(
            onClick = onClick,
            modifier = modifier,
            colors = colors,
            label = {
                Text(
                    text = label, maxLines = maxLabelLines,
                    overflow = TextOverflow.Ellipsis
                )
            },
            secondaryLabel = secondaryLabel?.let {
                {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            (secondaryLabelColor ?: colors.contentColor(enabled = enabled).value)
                    ) {
                        Text(
                            text = secondaryLabel,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            icon = content,
            shape = shape,
            enabled = enabled,
        )
    }
}

@Composable
private fun DemoLabelChip(
    colors: ChipColors,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onClick: (() -> Unit) = {},
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    style: ChipStyle
) {
    DemoIconChip(
        colors = colors,
        label = label,
        modifier = modifier,
        secondaryLabel = secondaryLabel,
        enabled = enabled,
        onClick = onClick,
        style = style,
        shape = shape,
        content = null
    )
}
