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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FadingExpandingLabel
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.placeholderShimmer
import androidx.wear.compose.material3.rememberPlaceholderState
import androidx.wear.compose.material3.samples.ButtonWithIconAndLabelAndPlaceholders
import androidx.wear.compose.material3.samples.ButtonWithIconAndLabelCachedData
import androidx.wear.compose.material3.samples.TextPlaceholder
import kotlinx.coroutines.delay

val PlaceholderDemos =
    listOf(
        ComposableDemo("Content Placeholders") {
            Centralize(Modifier.padding(horizontal = 10.dp)) {
                ButtonWithIconAndLabelAndPlaceholders()
            }
        },
        ComposableDemo("Cached Content") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { ButtonWithIconAndLabelCachedData() }
        },
        ComposableDemo("Simple Text Placeholder") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { TextPlaceholder() }
        },
        ComposableDemo("Button List") { PlaceholderButtonList() },
        ComposableDemo("Card and Button List") { PlaceholderCardAndButton() },
        ComposableDemo("Shimmer Color") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { PlaceholderComplexSample() }
        },
    )

@Suppress("PrimitiveInCollection")
@Composable
fun PlaceholderComplexSample() {
    var resetCount by remember { mutableIntStateOf(0) }
    var refreshCount by remember { mutableIntStateOf(0) }
    val ITEMS = 10
    val showContent = remember { Array(ITEMS) { mutableStateOf(false) } }

    val shimmerColors = listOf(Color.White, Color.Green, Color.Red)
    var shimmerColor by remember { mutableIntStateOf(1) }

    LaunchedEffect(resetCount) {
        showContent.forEach { it.value = false }
        delay(4000L)
        refreshCount++
        showContent.forEach {
            it.value = true
            delay(300)
        }
    }

    val slcState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    ScalingLazyColumn(
        state = slcState,
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 35.dp),
    ) {
        item {
            Centralize {
                Row {
                    Button(label = { Text("Reset") }, onClick = { resetCount++ })
                    Spacer(Modifier.size(5.dp))
                    CompactButton(
                        onClick = { shimmerColor = (shimmerColor + 1) % shimmerColors.size },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = shimmerColors[shimmerColor]
                            ),
                    )
                }
            }
        }

        items(ITEMS) {
            val state = rememberPlaceholderState(isVisible = !showContent[it].value)
            AppCard(
                onClick = {},
                appName = { Text("AppName $it") },
                title = { Text("AppCard") },
                time = { Text("now", Modifier.placeholder(state)) },
                modifier =
                    Modifier.fillMaxWidth()
                        .placeholderShimmer(
                            state,
                            color = shimmerColors[shimmerColor],
                            shape = CardDefaults.shape,
                        ),
            ) {
                // Simulated loading of content.
                val text =
                    if (showContent[it].value)
                        "Actual content that will take several lines. It will animate its size as content is loaded."
                    else ""
                FadingExpandingLabel(text, Modifier.fillMaxWidth().placeholder(state))

                Text("$it) Refresh count: $refreshCount")
            }
        }
    }
}

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
                ButtonWithPlaceholder(label = labelText, textAlignment = TextAlign.Center)
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ButtonWithPlaceholder(
                    label = labelText,
                    textAlignment = TextAlign.Center,
                    colors = ButtonDefaults.buttonColors(),
                )
                LaunchedEffect(resetCount) {
                    labelText = ""
                    delay(3000)
                    labelText = "Primary Label"
                }
            }
            item {
                var labelText by remember { mutableStateOf("") }
                ButtonWithPlaceholder(label = labelText, textAlignment = TextAlign.Center)
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
                ButtonWithPlaceholder(label = label)
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label = "Primary Label"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ButtonWithPlaceholder(label = label)
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
                ButtonWithPlaceholder(label = label, icon = Icons.Filled.Home)
                LaunchedEffect(resetCount) {
                    label = ""
                    delay(3000)
                    label = "Primary Label with icon"
                }
            }
            item {
                var label by remember { mutableStateOf("") }
                ButtonWithPlaceholder(label = label, icon = Icons.Filled.Home)
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
                ButtonWithPlaceholder(label = label, secondaryLabel = secondaryLabel)
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
                ButtonWithPlaceholder(label = label, secondaryLabel = secondaryLabel)
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
                ButtonWithPlaceholder(
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
                ButtonWithPlaceholder(
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
                ButtonWithPlaceholder(
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
                ButtonWithPlaceholder(
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

        FloatingResetButton(onClick = { resetCount++ })
    }
}

@Composable
fun FloatingResetButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompactButton(label = { Text("Reset") }, onClick = onClick)
    }
}

@Composable
fun PlaceholderCardAndButton() {
    var resetCount by remember { mutableIntStateOf(0) }
    var refreshCount by remember { mutableIntStateOf(0) }
    val numCards = 4
    val numButtons = 10
    val showContent = remember { Array(numCards + 2 * numButtons) { mutableStateOf(false) } }

    // Use the spec derived from default small and large screen specs.
    val transformationSpec = rememberTransformationSpec()

    LaunchedEffect(resetCount) {
        showContent.forEach { it.value = false }
        delay(4000)
        refreshCount++
        showContent.forEach {
            it.value = true
            delay(300)
        }
    }

    ScreenScaffold { padding ->
        TransformingLazyColumn(contentPadding = padding) {
            item { ListHeader { Text("Placeholders on Cards", textAlign = TextAlign.Center) } }
            repeat(4) { itemIndex ->
                item {
                    CardWithPlaceholder(
                        modifier =
                            Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        placeholderVisible = { !showContent[itemIndex].value },
                        content = {
                            Text("Some content $refreshCount")
                            Text("Some more content")
                        },
                    )
                }
            }
            repeat(10) { itemIndex ->
                item {
                    val placeholderState =
                        rememberPlaceholderState(
                            isVisible = !showContent[itemIndex + numCards].value
                        )
                    FilledTonalButton(
                        onClick = {},
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.placeholderShimmer(placeholderState)
                                .transformedHeight(this, transformationSpec),
                    ) {
                        Text("Filled Tonal Button")
                    }
                }
                item {
                    val placeholderState =
                        rememberPlaceholderState(
                            isVisible = !showContent[itemIndex + numCards + numButtons].value
                        )
                    OutlinedButton(
                        onClick = {},
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.placeholderShimmer(placeholderState)
                                .transformedHeight(this, transformationSpec),
                    ) {
                        Text("Outline Button")
                    }
                }
            }
        }
    }

    FloatingResetButton(onClick = { resetCount++ })
}

@Composable
fun ButtonWithPlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    secondaryLabel: String? = null,
    icon: ImageVector? = null,
    textAlignment: TextAlign = TextAlign.Start,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
) {
    var iconReady by remember { mutableStateOf(icon == null) }
    val maxLabelLines = if (secondaryLabel != null) 1 else 2
    val buttonPlaceholderState =
        rememberPlaceholderState(
            isVisible =
                label.isEmpty() ||
                    (secondaryLabel != null && secondaryLabel.isEmpty()) ||
                    !iconReady
        )

    Button(
        modifier = modifier.fillMaxWidth().placeholderShimmer(buttonPlaceholderState),
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
                        .placeholder(buttonPlaceholderState),
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
                        modifier = Modifier.fillMaxWidth().placeholder(buttonPlaceholderState),
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
                        contentDescription = null,
                        Modifier.placeholder(buttonPlaceholderState),
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
        colors = colors,
    )
}

@Composable
fun CardWithPlaceholder(
    placeholderVisible: () -> Boolean,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    content: @Composable (ColumnScope.() -> Unit)?,
) {
    val cardPlaceholderState = rememberPlaceholderState(isVisible = placeholderVisible())

    // Simulated loading.
    val (appName, title, time) =
        if (placeholderVisible()) listOf(" ", " ", " ") else listOf("AppName", "AppCard", "now")

    Box(modifier = modifier.height(120.dp)) {
        AppCard(
            onClick = {},
            modifier =
                Modifier.fillMaxHeight()
                    .placeholderShimmer(cardPlaceholderState, MaterialTheme.shapes.large),
            appName = {
                Text(
                    appName,
                    modifier = Modifier.weight(1f, true).placeholder(cardPlaceholderState),
                )
            },
            title = {
                Text(title, modifier = Modifier.fillMaxWidth().placeholder(cardPlaceholderState))
            },
            time = {
                Spacer(Modifier.weight(0.5f, true))
                Text(
                    time,
                    modifier = Modifier.weight(0.5f, true).placeholder(cardPlaceholderState),
                    textAlign = TextAlign.Right,
                )
            },
            transformation = transformation,
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            content?.let {
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .placeholder(cardPlaceholderState, MaterialTheme.shapes.small),
                    content = it,
                )
            }
        }
    }
}
