/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PlaceholderDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.placeholder
import androidx.wear.compose.material.placeholderShimmer
import androidx.wear.compose.material.rememberPlaceholderState
import kotlinx.coroutines.delay

@Composable
fun PlaceholderChips() {
    var resetCount by remember { mutableIntStateOf(0) }
    Box {
        ScalingLazyColumnWithRSB {
            item {
                ListHeader {
                    Text(text = "Primary Label Center Aligned", textAlign = TextAlign.Center)
                }
            }

            item {
                var labelText by remember { mutableStateOf("") }
                ChipWithContentPlaceholders(
                    label = labelText,
                    textAlignment = TextAlign.Center
                )
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ChipWithContentPlaceholders(
                    label = labelText,
                    textAlignment = TextAlign.Center,
                    colors = ChipDefaults.primaryChipColors()
                )
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ChipWithContentPlaceholders(
                    label = labelText,
                    textAlignment = TextAlign.Center,
                    colors = ChipDefaults.gradientBackgroundChipColors()
                )
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = labelText,
                    textAlignment = TextAlign.Center
                )
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label Center"
                }
            }

            item {
                ListHeader {
                    Text(text = "Primary Label Left Aligned", textAlign = TextAlign.Center)
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label = "Primary Label"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label =
                        "Primary that is long, to show truncation, we shouldn't be able to see " +
                            "more than 2 lines"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    icon = R.drawable.ic_accessibility_24px,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label = "Primary Label with icon"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    icon = R.drawable.ic_accessibility_24px,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label =
                        "Primary that is long, to show truncation, we shouldn't be able to see " +
                            "more than 2 lines"
                }
            }
            item {
                ListHeader {
                    Text(text = "Primary + Secondary Label", textAlign = TextAlign.Center)
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                var secondaryLabel by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    secondaryLabel = secondaryLabel,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    secondaryLabel = ""
                    delay(3000)
                    label = "Primary Label"
                    secondaryLabel = "Secondary Label"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                var secondaryLabel by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    secondaryLabel = secondaryLabel,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    secondaryLabel = ""
                    delay(3000)
                    label =
                        "Primary that is long, to show truncation, we shouldn't be able to see " +
                            "more than 1 line"
                    secondaryLabel = "Secondary Label"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                var secondaryLabel by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    icon = R.drawable.ic_accessibility_24px,
                    secondaryLabel = secondaryLabel,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    secondaryLabel = ""
                    delay(3000)
                    label = "Primary Label with icon"
                    secondaryLabel = "Secondary Label"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    icon = R.drawable.ic_accessibility_24px,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        iconColor = AlternatePrimaryColor2.copy(alpha = 0.5f),
                    ),
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label = "Primary Label with icon"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                var secondaryLabel by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    icon = R.drawable.ic_accessibility_24px,
                    secondaryLabel = secondaryLabel,
                    colors = ChipDefaults.chipColors(
                        backgroundColor = AlternatePrimaryColor2
                    ),
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    secondaryLabel = ""
                    delay(3000)
                    label = "Primary Label with icon"
                    secondaryLabel = "Content color override"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                var secondaryLabel by remember { mutableStateOf("") }
                ChipWithOverlaidPlaceholder(
                    label = label,
                    icon = R.drawable.ic_accessibility_24px,
                    secondaryLabel = secondaryLabel,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    secondaryLabel = ""
                    delay(3000)
                    label =
                        "Primary that is long, to show truncation, we shouldn't be able to see " +
                            "more than 1 line"
                    secondaryLabel = "Long Secondary that is long, to show truncation, we " +
                        "shouldn't be able to see more than 1 line"
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactChip(
                label = { Text("Reset") },
                onClick = { resetCount++ }
            )
        }
    }
}

@Composable
fun PlaceholderCards() {
    var resetCount by remember { mutableIntStateOf(0) }
    var refreshCount by remember { mutableIntStateOf(0) }
    val showContent = remember { Array(4) { mutableStateOf(false) } }

    LaunchedEffect(resetCount) {
        showContent.forEach { it.value = false }
        delay(4000)
        refreshCount++
        showContent.forEach {
            it.value = true
            delay(300)
        }
    }

    ScalingLazyColumnWithRSB {
        item {
            ListHeader {
                Text("Overlaid Placeholders", textAlign = TextAlign.Center)
            }
        }
        item {
            Centralize {
                Chip(
                    label = { Text("Reset") },
                    onClick = { resetCount++ }
                )
            }
        }
        repeat(4) { itemIndex ->
            item {
                CardWithOverlaidPlaceholder(
                    contentReady = { showContent[itemIndex].value },
                    content =
                    {
                        Text("Some content $refreshCount")
                        Text("Some more content")
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun ChipWithOverlaidPlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    secondaryLabel: String? = null,
    icon: Int? = null,
    textAlignment: TextAlign = TextAlign.Start,
    colors: ChipColors = ChipDefaults.secondaryChipColors(),
) {
    val hasSecondaryLabel = secondaryLabel != null
    val hasIcon = icon != null
    var iconReady by remember { mutableStateOf(icon == null) }
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    val chipPlaceholderState = rememberPlaceholderState {
        label.isNotEmpty() &&
            ((secondaryLabel == null) || secondaryLabel.isNotEmpty()) && iconReady
    }

    Box {
        Chip(
            modifier = modifier
                .fillMaxWidth(),
            onClick = onClick,
            label = {
                Text(
                    text = label,
                    textAlign = textAlignment,
                    maxLines = maxLabelLines,
                    overflow = TextOverflow.Clip,

                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                )
            },
            secondaryLabel = if (secondaryLabel != null) {
                {
                    Text(
                        text = secondaryLabel,
                        textAlign = textAlignment,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            } else {
                null
            },
            icon = if (icon != null) {
                {
                    DemoIcon(
                        resourceId = icon,
                    )
                    if (!iconReady) {
                        LaunchedEffect(Unit) {
                            delay(2000)
                            iconReady = true
                        }
                    }
                }
            } else {
                null
            },
            enabled = true,
            colors = PlaceholderDefaults.placeholderChipColors(
                originalChipColors = colors,
                placeholderState = chipPlaceholderState
            )
        )
        if (! chipPlaceholderState.isShowContent) {
            Chip(
                modifier = modifier
                    .fillMaxWidth()
                    .placeholderShimmer(
                        placeholderState = chipPlaceholderState,
                    ),
                onClick = onClick,
                label = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .padding(top = 1.dp, bottom = 1.dp)
                            .placeholder(placeholderState = chipPlaceholderState)
                    )
                },
                secondaryLabel = if (hasSecondaryLabel) {
                    {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .padding(top = 1.dp, bottom = 1.dp)
                                .placeholder(chipPlaceholderState)
                        )
                    }
                } else {
                    null
                },
                icon = if (hasIcon) {
                    {
                        Box(
                            modifier = Modifier
                                .size(ChipDefaults.IconSize)
                                .placeholder(chipPlaceholderState)
                        )
                    }
                } else {
                    null
                },
                enabled = true,
                colors = PlaceholderDefaults.placeholderChipColors(
                    placeholderState = chipPlaceholderState
                )
            )
        }
    }
    LaunchedEffect(chipPlaceholderState) {
        chipPlaceholderState.startPlaceholderAnimation()
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun ChipWithContentPlaceholders(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    secondaryLabel: String? = null,
    icon: Int? = null,
    textAlignment: TextAlign = TextAlign.Start,
    colors: ChipColors = ChipDefaults.secondaryChipColors(),
) {
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    var iconReady by remember { mutableStateOf(icon == null) }
    val chipPlaceholderState = rememberPlaceholderState {
        label.isNotEmpty() &&
            ((secondaryLabel == null) || secondaryLabel.isNotEmpty()) && iconReady
    }

    Chip(
        modifier = modifier
            .fillMaxWidth()
            .placeholderShimmer(chipPlaceholderState),
        onClick = onClick,
        label = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
            ) {
                Text(
                    text = label,
                    textAlign = textAlignment,
                    maxLines = maxLabelLines,
                    overflow = TextOverflow.Clip,

                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .placeholder(placeholderState = chipPlaceholderState)
                )
            }
        },
        secondaryLabel = if (secondaryLabel != null) {
            {
                Text(
                    text = secondaryLabel,
                    textAlign = textAlignment,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(chipPlaceholderState)
                )
            }
        } else {
            null
        },
        icon = if (icon != null) {
            {
                DemoIcon(
                    resourceId = icon,
                    modifier = Modifier
                        .placeholder(chipPlaceholderState)
                )
                if (!iconReady) {
                    LaunchedEffect(Unit) {
                        delay(2000)
                        iconReady = true
                    }
                }
            }
        } else {
            null
        },
        enabled = true,
        colors = PlaceholderDefaults.placeholderChipColors(
            originalChipColors = colors,
            placeholderState = chipPlaceholderState
        )
    )
    LaunchedEffect(chipPlaceholderState) {
        chipPlaceholderState.startPlaceholderAnimation()
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun CardWithOverlaidPlaceholder(
    contentReady: () -> Boolean,
    content: @Composable (ColumnScope.() -> Unit)?,
) {
    val cardPlaceholderState = rememberPlaceholderState(isContentReady = contentReady)

    Box(modifier = Modifier.height(120.dp)) {
        AppCard(
            onClick = {},
            appName = { Text("AppName") },
            title = { Text("AppCard") },
            time = { Text("now") },
            modifier = Modifier.fillMaxHeight()

        ) {
            if (content != null) content()
        }
        if (! cardPlaceholderState.isShowContent) {
            AppCard(
                onClick = {},
                appName = {
                    Text(" ",
                        modifier = Modifier
                            .weight(2f, true)
                            .placeholder(cardPlaceholderState)
                    )
                },
                title = {
                    Text(" ",
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(cardPlaceholderState)
                    )
                },
                time = {
                    Text(" ",
                        modifier = Modifier
                            .weight(1f, true)
                            .placeholder(cardPlaceholderState)
                    )
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .placeholderShimmer(
                        cardPlaceholderState,
                        MaterialTheme.shapes.large
                    ),
                backgroundPainter = PlaceholderDefaults.placeholderBackgroundBrush(
                    placeholderState = cardPlaceholderState
                )
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier
                    .fillMaxSize()
                    .placeholder(
                        cardPlaceholderState,
                        MaterialTheme.shapes.medium
                    )
                )
            }
       }
    }
    LaunchedEffect(cardPlaceholderState) {
        cardPlaceholderState.startPlaceholderAnimation()
    }
}
