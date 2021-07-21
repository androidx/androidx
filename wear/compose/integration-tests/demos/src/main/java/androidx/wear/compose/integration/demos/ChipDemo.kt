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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun StandardChips() {
    val scrollState: ScrollState = rememberScrollState()
    var enabled by remember { mutableStateOf(true) }
    var chipStyle by remember { mutableStateOf(ChipStyle.Primary) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Chip with Label",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        DemoLabelChip(
            label = "Single Label",
            colors = chipColors(chipStyle),
            enabled = enabled,
        )
        DemoLabelChip(
            label = "Standard chip with long label to show truncation which does not fit into" +
                " 2 lines",
            colors = chipColors(chipStyle),
            enabled = enabled,
        )
        Text(
            "Chip with icon",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1
        )
        DemoIconChip(
            colors = chipColors(chipStyle),
            label = "Label with icon",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }

        DemoIconChip(
            colors = chipColors(chipStyle),
            label = "Long label to show truncation which does not fit into" +
                " 2 lines",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        Text(
            "Main + Secondary label",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1
        )
        DemoLabelChip(
            label = "Main label and",
            secondaryLabel = "Secondary label",
            colors = chipColors(chipStyle),
            enabled = enabled,
        )
        DemoLabelChip(
            label = "Long label to show truncation which does not fit into" +
                " 1 line",
            secondaryLabel = "Secondary Label",
            colors = chipColors(chipStyle),
            enabled = enabled,
        )
        DemoIconChip(
            colors = chipColors(chipStyle),
            label = "Label with icon and",
            secondaryLabel = "Secondary Label",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        DemoIconChip(
            colors = chipColors(chipStyle),
            label = "Long label with truncation",
            secondaryLabel = "Long secondary label to show truncation which does not fit into" +
                "1 line",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        ChipCustomizer(
            enabled = enabled,
            chipStyle = chipStyle,
            onChipStyleChanged = { chipStyle = it },
            onEnabledChanged = { enabled = it },
        )
    }
}

@Composable
fun SmallChips() {
    val scrollState: ScrollState = rememberScrollState()
    var enabled by remember { mutableStateOf(true) }
    var chipStyle by remember { mutableStateOf(ChipStyle.Primary) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Compact Chip",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.White
        )

        CompactChip(
            onClick = {},
            colors = chipColors(chipStyle),
            label = { Text("Label") },
            enabled = enabled,
        )
        CompactChip(
            onClick = {},
            colors = chipColors(chipStyle),
            label = {
                Text(
                    "Long label to show truncation which does not fit into 1 line",
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            enabled = enabled,
        )
        CompactChip(
            onClick = {},
            colors = chipColors(chipStyle),
            label = {
                Text("Label with icon", maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            icon = { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) },
            enabled = enabled,
        )
        CompactChip(
            onClick = {},
            colors = chipColors(chipStyle),
            label = {
                Text(
                    "Label with icon to show truncation which does not fit into 1 line",
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            },
            enabled = enabled,
            icon = { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) },
        )
        ChipCustomizer(
            enabled = enabled,
            chipStyle = chipStyle,
            onChipStyleChanged = { chipStyle = it },
            onEnabledChanged = { enabled = it },
        )
    }
}

@Composable
fun AvatarChips() {
    val scrollState: ScrollState = rememberScrollState()
    var enabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Chips with avatars",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.White
        )
        DemoIconChip(
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
        DemoIconChip(
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
        Text(
            text = "Small Avatar Chips",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = Color.White
        )
        DemoIconChip(
            label = "App Title",
            secondaryLabel = "Custom background & content color",
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = Color(0x775FB2FF),
                contentColor = MaterialTheme.colors.onPrimary
            ),
            enabled = enabled,
        ) {
            DemoImage(resourceId = R.drawable.ic_maps_icon)
        }
        DemoIconChip(
            label = "App title",
            secondaryLabel = "Default color with gradient",
            colors = ChipDefaults.gradientBackgroundChipColors(),
            enabled = enabled,
        ) {
            DemoImage(resourceId = R.drawable.ic_maps_icon)
        }
        DemoIconChip(
            label = "App title",
            secondaryLabel = "Gradient background and onPrimary content",
            colors = ChipDefaults.gradientBackgroundChipColors(
                startBackgroundColor = Color(0x775FB2FF),
                contentColor = MaterialTheme.colors.onPrimary
            ),
            enabled = enabled,
        ) {
            DemoImage(resourceId = R.drawable.ic_maps_icon)
        }
        DemoIconChip(
            label = "App title",
            secondaryLabel = "Gradient background and custom content",
            colors = ChipDefaults.gradientBackgroundChipColors(
                startBackgroundColor = Color(0x775FB2FF),
                contentColor = Color.LightGray
            ),
            enabled = enabled,
        ) {
            DemoImage(resourceId = R.drawable.ic_maps_icon)
        }
        ToggleChip(
            checked = enabled,
            onCheckedChange = { enabled = it },
            label = {
                Text("Chips enabled")
            },
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = enabled)
            }
        )
    }
}

@Composable
fun RtlChips() {
    val scrollState: ScrollState = rememberScrollState()

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Right to left chips",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1,
            color = Color.White
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            DemoLabelChip(
                label = "Standard chip",
                colors = ChipDefaults.primaryChipColors(),
            )
            DemoLabelChip(
                label = "Standard chip with long label to show truncation which does not fit into" +
                    " 2 lines",
                colors = ChipDefaults.primaryChipColors(),
            )
            DemoIconChip(
                colors = ChipDefaults.primaryChipColors(),
                label = "Standard chip with ",
                secondaryLabel = "Secondary Label",
            ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
            CompactChip(
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                label = {
                    Text(
                        "Compact chip with label & icon", maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                icon = { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) },
            )
            DemoIconChip(
                label = "Chip with text icon",
                colors = ChipDefaults.secondaryChipColors(),
            ) {
                TextIcon(
                    text = "M",
                    size = ChipDefaults.LargeIconSize,
                    style = MaterialTheme.typography.title3
                )
            }
            DemoIconChip(
                label = "Standard chip with",
                secondaryLabel = "Default gradient color",
                colors = ChipDefaults.gradientBackgroundChipColors(),
            ) {
                DemoImage(resourceId = R.drawable.ic_maps_icon)
            }
        }
    }
}

@Composable
fun CustomChips() {
    val applicationContext = LocalContext.current
    val scrollState: ScrollState = rememberScrollState()
    var enabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {

        MaterialTheme(colors = MaterialTheme.colors.copy(primary = Color.Cyan)) {
            DemoIconChip(
                label = "Overridden Theme Primary + Icon",
                colors = ChipDefaults.primaryChipColors(),
                enabled = enabled,
            ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        }
        DemoLabelChip(
            label = "Custom background",
            secondaryLabel = "With secondary label",
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = Color.Yellow.copy(alpha = 0.5f)
            ),
            enabled = enabled,
        )
        Chip(
            onClick = { },
            colors = ChipDefaults.primaryChipColors(),
            label = {
                Text(
                    text = "Custom label color", maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Yellow,
                )
            },
            secondaryLabel = {
                Text(
                    text = "Custom secondary label color",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Yellow,
                )
            },
            icon = { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) },
            enabled = enabled
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            DemoLabelChip(
                label = "Chip with fixed width",
                modifier = Modifier.width(100.dp),
                colors = ChipDefaults.primaryChipColors(),
                enabled = enabled,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            CompactChip(
                onClick = {
                    Toast.makeText(
                        applicationContext, "Wrap content chip with custom background color",
                        Toast.LENGTH_LONG
                    ).show()
                },
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = Color.Yellow,
                    contentColor = MaterialTheme.colors.surface
                ),
                icon = {
                    DemoIcon(
                        resourceId = R.drawable.ic_accessibility_24px,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                enabled = enabled,
            )
        }
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
                colors = ChipDefaults.secondaryChipColors(contentColor = Color.Yellow),
                icon = {
                    DemoIcon(
                        resourceId = R.drawable.ic_accessibility_24px,
                        modifier = Modifier.fillMaxSize()
                    )
                },
                enabled = enabled,
            )
        }
        ToggleChip(
            checked = enabled,
            onCheckedChange = { enabled = it },
            label = {
                Text("Chips enabled")
            },
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = enabled)
            }
        )
    }
}

@Composable
fun ImageBackgroundChips() {
    val scrollState: ScrollState = rememberScrollState()
    var enabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.verticalScroll(scrollState)
            .padding(
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 15.dp,
                    bottom = 50.dp
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        DemoLabelChip(
            label = "Custom background image",
            colors = ChipDefaults.imageBackgroundChipColors(
                backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1),
            ),
            enabled = enabled,
        )
        DemoLabelChip(
            label = "Custom background image",
            secondaryLabel = "with secondary label",
            colors = ChipDefaults.imageBackgroundChipColors(
                backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1),
            ),
            enabled = enabled,
        )
        ToggleChip(
            checked = enabled,
            onCheckedChange = { enabled = it },
            label = {
                Text("Chips enabled")
            },
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = enabled)
            }
        )
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            modifier = Modifier.align(Alignment.CenterHorizontally).height(35.dp),
        ) {
            ChipStyle.values().forEach { style ->
                ToggleButton(
                    checked = chipStyle == style,
                    onCheckedChange = {
                        onChipStyleChanged(style)
                    },
                ) {
                    Text(
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier.padding(4.dp),
                        text = style.toString(),
                    )
                }
            }
        }
        ToggleChip(
            checked = enabled,
            onCheckedChange = onEnabledChanged,
            label = {
                Text("Chips enabled")
            },
            toggleIcon = {
                ToggleChipDefaults.SwitchIcon(checked = enabled)
            }
        )
    }
}

@Composable
private fun chipColors(chipStyle: ChipStyle) =
    when (chipStyle) {
        ChipStyle.Primary -> ChipDefaults.primaryChipColors()
        ChipStyle.Secondary -> ChipDefaults.secondaryChipColors()
        ChipStyle.Child -> ChipDefaults.childChipColors()
    }

enum class ChipStyle {
    Primary,
    Secondary,
    Child
}

@Composable
private fun DemoIconChip(
    colors: ChipColors,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit) = {},
    content: @Composable (() -> Unit)? = null
) {
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
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
                Text(
                    text = secondaryLabel,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        },
        icon = content, enabled = enabled
    )
}

@Composable
private fun DemoLabelChip(
    colors: ChipColors,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onClick: (() -> Unit) = {},
    enabled: Boolean = true
) {
    DemoIconChip(colors, label, modifier, secondaryLabel, enabled, onClick, null)
}