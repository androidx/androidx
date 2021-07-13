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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
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

        ChipCustomizerHeader(
            enabled = enabled,
            chipStyle = chipStyle,
            onChipStyleChanged = { chipStyle = it },
            onEnabledChanged = { enabled = it },
        )
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
            label = "Label that is long, to show truncation, we shouldn't be able to see more " +
                "than 2 " +
                "lines",
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
            label = "Label that is long, to show truncation, we shouldn't be able to see more " +
                "than 2 " +
                "lines",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        Text(
            "Primary + Secondary Label",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption1
        )
        DemoLabelChip(
            label = "Label",
            secondaryLabel = "Secondary Label",
            colors = chipColors(chipStyle),
            enabled = enabled,
        )
        DemoLabelChip(
            label = "Label that is long, to show truncation, we shouldn't be able to see more " +
                "than 1 line",
            secondaryLabel = "Secondary Label",
            colors = chipColors(chipStyle),
            enabled = enabled,
        )
        DemoIconChip(
            colors = chipColors(chipStyle),
            label = "Label with icon",
            secondaryLabel = "Secondary Label",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
        DemoIconChip(
            colors = chipColors(chipStyle),
            label = "Label that is long, to show truncation, we shouldn't be able to see more " +
                "than 1 line",
            secondaryLabel = "Long Secondary that is long, to show truncation, we shouldn't " +
                "be able to see more than 1 line",
            enabled = enabled,
        ) { DemoIcon(resourceId = R.drawable.ic_accessibility_24px) }
    }
}

@Composable
private fun DemoIconChip(
    colors: ChipColors,
    label: String,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    enabled: Boolean = true,
    content: @Composable (() -> Unit)? = null
) {
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    Chip(
        onClick = {},
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
    enabled: Boolean = true
) {
    DemoIconChip(colors, label, modifier, secondaryLabel, enabled, null)
}

@Composable
private fun ChipCustomizerHeader(
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
