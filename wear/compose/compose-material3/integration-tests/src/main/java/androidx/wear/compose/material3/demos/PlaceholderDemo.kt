/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardColors
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PlaceholderDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.placeholderShimmer
import androidx.wear.compose.material3.rememberPlaceholderState
import androidx.wear.compose.material3.samples.ButtonWithIconAndLabelAndPlaceholders
import androidx.wear.compose.material3.samples.ButtonWithIconAndLabelsAndOverlaidPlaceholder
import androidx.wear.compose.material3.samples.TextPlaceholder
import kotlinx.coroutines.delay

val PlaceholderDemos =
    listOf(
        ComposableDemo("Content Placeholders") {
            Centralize(Modifier.padding(horizontal = 10.dp)) {
                ButtonWithIconAndLabelAndPlaceholders()
            }
        },
        ComposableDemo("Overlaid Placeholder") {
            Centralize(Modifier.padding(horizontal = 10.dp)) {
                ButtonWithIconAndLabelsAndOverlaidPlaceholder()
            }
        },
        ComposableDemo("Simple Text Placeholder") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { TextPlaceholder() }
        },
        ComposableDemo("Button List") { PlaceholderButtonList() },
        ComposableDemo("Card List") { PlaceholderCardList() },
    )

@Suppress("PrimitiveInCollection")
@Composable
fun PlaceholderButtonList() {
    var resetCount by remember { mutableIntStateOf(0) }
    Box {
        ScalingLazyColumn {
            item {
                ListHeader {
                    Text(text = "Primary Label Center Aligned", textAlign = TextAlign.Center)
                }
            }

            item {
                var labelText by remember { mutableStateOf("") }
                ButtonWithContentPlaceholders(label = labelText, textAlignment = TextAlign.Center)
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ButtonWithContentPlaceholders(
                    label = labelText,
                    textAlignment = TextAlign.Center,
                    colors = ButtonDefaults.buttonColors()
                )
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ButtonWithOverlaidPlaceholder(label = labelText, textAlignment = TextAlign.Center)
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
                ButtonWithOverlaidPlaceholder(
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
                ButtonWithOverlaidPlaceholder(
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
                ButtonWithOverlaidPlaceholder(
                    label = label,
                    icon = Icons.Filled.Home,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label = "Primary Label with icon"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ButtonWithOverlaidPlaceholder(
                    label = label,
                    icon = Icons.Filled.Home,
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
                ButtonWithOverlaidPlaceholder(
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
                ButtonWithOverlaidPlaceholder(
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
                ButtonWithOverlaidPlaceholder(
                    label = label,
                    icon = Icons.Filled.Home,
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
                ButtonWithOverlaidPlaceholder(
                    label = label,
                    icon = Icons.Filled.Home,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            iconColor = Color.Magenta.copy(alpha = 0.5f),
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
                ButtonWithOverlaidPlaceholder(
                    label = label,
                    icon = Icons.Filled.Home,
                    secondaryLabel = secondaryLabel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta),
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
                ButtonWithOverlaidPlaceholder(
                    label = label,
                    icon = Icons.Filled.Home,
                    secondaryLabel = secondaryLabel,
                )
                LaunchedEffect(resetCount) {
                    label = ""
                    secondaryLabel = ""
                    delay(3000)
                    label =
                        "Primary that is long, to show truncation, we shouldn't be able to see " +
                            "more than 1 line"
                    secondaryLabel =
                        "Long Secondary that is long, to show truncation, we " +
                            "shouldn't be able to see more than 1 line"
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactButton(label = { Text("Reset") }, onClick = { resetCount++ })
        }
    }
}

@Composable
fun PlaceholderCardList() {
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

    ScalingLazyColumn {
        item { ListHeader { Text("Overlaid Placeholders", textAlign = TextAlign.Center) } }
        item { Centralize { Button(label = { Text("Reset") }, onClick = { resetCount++ }) } }
        repeat(4) { itemIndex ->
            item {
                CardWithOverlaidPlaceholder(
                    contentReady = { showContent[itemIndex].value },
                    content = {
                        Text("Some content $refreshCount")
                        Text("Some more content")
                    }
                )
            }
        }
    }
}

@Composable
fun ButtonWithOverlaidPlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    secondaryLabel: String? = null,
    icon: ImageVector? = null,
    textAlignment: TextAlign = TextAlign.Start,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
) {
    val hasSecondaryLabel = secondaryLabel != null
    val hasIcon = icon != null
    var iconReady by remember { mutableStateOf(icon == null) }
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    val buttonPlaceholderState = rememberPlaceholderState {
        label.isNotEmpty() && ((secondaryLabel == null) || secondaryLabel.isNotEmpty()) && iconReady
    }

    Box {
        Button(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            label = {
                Text(
                    text = label,
                    textAlign = textAlignment,
                    maxLines = maxLabelLines,
                    overflow = TextOverflow.Clip,
                    modifier =
                        Modifier.fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                )
            },
            secondaryLabel =
                if (secondaryLabel != null) {
                    {
                        Text(
                            text = secondaryLabel,
                            textAlign = textAlignment,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    null
                },
            icon =
                if (icon != null) {
                    {
                        Icon(imageVector = icon, contentDescription = null)
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
            colors =
                PlaceholderDefaults.placeholderButtonColors(
                    originalButtonColors = colors,
                    placeholderState = buttonPlaceholderState
                )
        )
        if (!buttonPlaceholderState.isHidden) {
            Button(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .placeholderShimmer(
                            placeholderState = buttonPlaceholderState,
                        ),
                onClick = onClick,
                label = {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(16.dp)
                                .padding(top = 1.dp, bottom = 1.dp)
                                .placeholder(placeholderState = buttonPlaceholderState)
                    )
                },
                secondaryLabel =
                    if (hasSecondaryLabel) {
                        {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(16.dp)
                                        .padding(top = 1.dp, bottom = 1.dp)
                                        .placeholder(buttonPlaceholderState)
                            )
                        }
                    } else {
                        null
                    },
                icon =
                    if (hasIcon) {
                        {
                            Box(
                                modifier =
                                    Modifier.size(ButtonDefaults.IconSize)
                                        .placeholder(buttonPlaceholderState)
                            )
                        }
                    } else {
                        null
                    },
                enabled = true,
                colors =
                    PlaceholderDefaults.placeholderButtonColors(
                        placeholderState = buttonPlaceholderState
                    )
            )
        }
    }
    LaunchedEffect(buttonPlaceholderState) { buttonPlaceholderState.animatePlaceholder() }
}

@Composable
fun ButtonWithContentPlaceholders(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    secondaryLabel: String? = null,
    icon: ImageVector? = null,
    textAlignment: TextAlign = TextAlign.Start,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
) {
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    var iconReady by remember { mutableStateOf(icon == null) }
    val buttonPlaceholderState = rememberPlaceholderState {
        label.isNotEmpty() && ((secondaryLabel == null) || secondaryLabel.isNotEmpty()) && iconReady
    }

    Button(
        modifier = modifier.fillMaxWidth().placeholderShimmer(buttonPlaceholderState),
        onClick = onClick,
        label = {
            Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                Text(
                    text = label,
                    textAlign = textAlignment,
                    maxLines = maxLabelLines,
                    overflow = TextOverflow.Clip,
                    modifier =
                        Modifier.fillMaxSize()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .placeholder(placeholderState = buttonPlaceholderState)
                )
            }
        },
        secondaryLabel =
            if (secondaryLabel != null) {
                {
                    Text(
                        text = secondaryLabel,
                        textAlign = textAlignment,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().placeholder(buttonPlaceholderState)
                    )
                }
            } else {
                null
            },
        icon =
            if (icon != null) {
                {
                    Icon(
                        imageVector = icon,
                        modifier = Modifier.placeholder(buttonPlaceholderState),
                        contentDescription = null
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
        colors =
            PlaceholderDefaults.placeholderButtonColors(
                originalButtonColors = colors,
                placeholderState = buttonPlaceholderState
            )
    )
    LaunchedEffect(buttonPlaceholderState) { buttonPlaceholderState.animatePlaceholder() }
}

@Composable
fun CardWithOverlaidPlaceholder(
    contentReady: () -> Boolean,
    content: @Composable (ColumnScope.() -> Unit)?,
) {
    val cardPlaceholderState = rememberPlaceholderState(isContentReady = contentReady)
    val defaultCardColors = CardDefaults.cardColors()

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
        if (!cardPlaceholderState.isHidden) {
            AppCard(
                onClick = {},
                appName = {
                    Text(
                        " ",
                        modifier = Modifier.weight(2f, true).placeholder(cardPlaceholderState)
                    )
                },
                title = {
                    Text(" ", modifier = Modifier.fillMaxWidth().placeholder(cardPlaceholderState))
                },
                time = {
                    Text(
                        " ",
                        modifier = Modifier.weight(1f, true).placeholder(cardPlaceholderState)
                    )
                },
                modifier =
                    Modifier.fillMaxHeight()
                        .placeholderShimmer(cardPlaceholderState, MaterialTheme.shapes.large),
                colors =
                    CardColors(
                        containerPainter =
                            PlaceholderDefaults.placeholderBackgroundBrush(
                                placeholderState = cardPlaceholderState
                            ),
                        contentColor = defaultCardColors.contentColor,
                        appNameColor = defaultCardColors.appNameColor,
                        timeColor = defaultCardColors.timeColor,
                        titleColor = defaultCardColors.titleColor,
                        subtitleColor = defaultCardColors.subtitleColor
                    )
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .placeholder(cardPlaceholderState, MaterialTheme.shapes.small)
                )
            }
        }
    }
    LaunchedEffect(cardPlaceholderState) { cardPlaceholderState.animatePlaceholder() }
}
