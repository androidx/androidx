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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabPosition
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Preview
@Composable
@Sampled
@OptIn(ExperimentalMaterial3Api::class)
fun PrimaryTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3 with lots of text")
    Column {
        PrimaryTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Primary tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PrimaryIconTabs() {
    var state by remember { mutableStateOf(0) }
    val icons = listOf(Icons.Filled.Favorite, Icons.Filled.Favorite, Icons.Filled.Favorite)
    Column {
        PrimaryTabRow(selectedTabIndex = state) {
            icons.forEachIndexed { index, icon ->
                // Icon-only tab should have a tooltip associated with it.
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                    tooltip = { PlainTooltip { Text("Favorite") } },
                    state = rememberTooltipState(),
                ) {
                    Tab(
                        selected = state == index,
                        onClick = { state = index },
                        icon = { Icon(icon, contentDescription = "Favorite") },
                    )
                }
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Icon tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@Sampled
@OptIn(ExperimentalMaterial3Api::class)
fun SecondaryTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3 with lots of text")
    Column {
        SecondaryTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Secondary tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3 with lots of text")
    Column {
        PrimaryTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Text tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SecondaryIconTabs() {
    var state by remember { mutableStateOf(0) }
    val icons = listOf(Icons.Filled.Favorite, Icons.Filled.Favorite, Icons.Filled.Favorite)
    Column {
        SecondaryTabRow(selectedTabIndex = state) {
            icons.forEachIndexed { index, icon ->
                // Icon-only tab should have a tooltip associated with it.
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                    tooltip = { PlainTooltip { Text("Favorite") } },
                    state = rememberTooltipState(),
                ) {
                    Tab(
                        selected = state == index,
                        onClick = { state = index },
                        icon = { Icon(icon, contentDescription = "Favorite") },
                    )
                }
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Icon tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TextAndIconTabs() {
    var state by remember { mutableStateOf(0) }
    val titlesAndIcons =
        listOf(
            "Tab 1" to Icons.Filled.Favorite,
            "Tab 2" to Icons.Filled.Favorite,
            "Tab 3 with lots of text" to Icons.Filled.Favorite,
        )
    Column {
        PrimaryTabRow(selectedTabIndex = state) {
            titlesAndIcons.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(icon, contentDescription = null) },
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Text and icon tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LeadingIconTabs() {
    var state by remember { mutableStateOf(0) }
    val titlesAndIcons =
        listOf(
            "Tab" to Icons.Filled.Favorite,
            "Tab & icon" to Icons.Filled.Favorite,
            "Tab 3 with lots of text" to Icons.Filled.Favorite,
        )
    Column {
        PrimaryTabRow(selectedTabIndex = state) {
            titlesAndIcons.forEachIndexed { index, (title, icon) ->
                LeadingIconTab(
                    selected = state == index,
                    onClick = { state = index },
                    text = {
                        BadgedBox(badge = { Badge(modifier = Modifier) { Text("999+") } }) {
                            Text(title)
                        }
                    },
                    icon = { Icon(icon, contentDescription = null) },
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Leading icon tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScrollingPrimaryTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles =
        listOf(
            "Tab 1",
            "Tab 2",
            "Tab 3 with lots of text",
            "Tab 4",
            "Tab 5",
            "Tab 6 with lots of text",
            "Tab 7",
            "Tab 8",
            "Tab 9 with lots of text",
            "Tab 10",
        )
    Column {
        PrimaryScrollableTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(selected = state == index, onClick = { state = index }, text = { Text(title) })
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling primary tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScrollingSecondaryTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles =
        listOf(
            "Tab 1",
            "Tab 2",
            "Tab 3 with lots of text",
            "Tab 4",
            "Tab 5",
            "Tab 6 with lots of text",
            "Tab 7",
            "Tab 8",
            "Tab 9 with lots of text",
            "Tab 10",
        )
    Column {
        SecondaryScrollableTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(selected = state == index, onClick = { state = index }, text = { Text(title) })
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling secondary tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScrollingTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles =
        listOf(
            "Tab 1",
            "Tab 2",
            "Tab 3 with lots of text",
            "Tab 4",
            "Tab 5",
            "Tab 6 with lots of text",
            "Tab 7",
            "Tab 8",
            "Tab 9 with lots of text",
            "Tab 10",
        )
    Column {
        PrimaryScrollableTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(selected = state == index, onClick = { state = index }, text = { Text(title) })
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling text tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FancyTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3")
    Column {
        SecondaryTabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                FancyTab(title = title, onClick = { state = index }, selected = (index == state))
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FancyIndicatorTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3")

    Column {
        SecondaryTabRow(
            selectedTabIndex = state,
            indicator = {
                FancyIndicator(
                    MaterialTheme.colorScheme.primary,
                    Modifier.tabIndicatorOffset(state),
                )
            },
        ) {
            titles.forEachIndexed { index, title ->
                Tab(selected = state == index, onClick = { state = index }, text = { Text(title) })
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy indicator tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Sampled
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FancyIndicatorContainerTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("Tab 1", "Tab 2", "Tab 3")

    Column {
        SecondaryTabRow(
            selectedTabIndex = state,
            indicator = { FancyAnimatedIndicatorWithModifier(state) },
        ) {
            titles.forEachIndexed { index, title ->
                Tab(selected = state == index, onClick = { state = index }, text = { Text(title) })
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy transition tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Sampled
@Composable
fun FancyIndicator(color: Color, modifier: Modifier = Modifier) {
    // Draws a rounded rectangular with border around the Tab, with a 5.dp padding from the edges
    // Color is passed in as a parameter [color]
    Box(
        modifier
            .padding(5.dp)
            .fillMaxSize()
            .border(BorderStroke(2.dp, color), RoundedCornerShape(5.dp))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Sampled
@Composable
fun TabIndicatorScope.FancyAnimatedIndicatorWithModifier(index: Int) {
    val colors =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
        )
    var startAnimatable by remember { mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null) }
    var endAnimatable by remember { mutableStateOf<Animatable<Dp, AnimationVector1D>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val indicatorColor: Color by animateColorAsState(colors[index % colors.size], label = "")

    Box(
        Modifier.tabIndicatorLayout {
                measurable: Measurable,
                constraints: Constraints,
                tabPositions: List<TabPosition> ->
                val newStart = tabPositions[index].left
                val newEnd = tabPositions[index].right
                val startAnim =
                    startAnimatable
                        ?: Animatable(newStart, Dp.VectorConverter).also { startAnimatable = it }

                val endAnim =
                    endAnimatable
                        ?: Animatable(newEnd, Dp.VectorConverter).also { endAnimatable = it }

                if (endAnim.targetValue != newEnd) {
                    coroutineScope.launch {
                        endAnim.animateTo(
                            newEnd,
                            animationSpec =
                                if (endAnim.targetValue < newEnd) {
                                    spring(dampingRatio = 1f, stiffness = 1000f)
                                } else {
                                    spring(dampingRatio = 1f, stiffness = 50f)
                                },
                        )
                    }
                }

                if (startAnim.targetValue != newStart) {
                    coroutineScope.launch {
                        startAnim.animateTo(
                            newStart,
                            animationSpec =
                                // Handle directionality here, if we are moving to the right, we
                                // want the right side of the indicator to move faster, if we are
                                // moving to the left, we want the left side to move faster.
                                if (startAnim.targetValue < newStart) {
                                    spring(dampingRatio = 1f, stiffness = 50f)
                                } else {
                                    spring(dampingRatio = 1f, stiffness = 1000f)
                                },
                        )
                    }
                }

                val indicatorEnd = endAnim.value.roundToPx()
                val indicatorStart = startAnim.value.roundToPx()

                // Apply an offset from the start to correctly position the indicator around the tab
                val placeable =
                    measurable.measure(
                        constraints.copy(
                            maxWidth = indicatorEnd - indicatorStart,
                            minWidth = indicatorEnd - indicatorStart,
                        )
                    )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(indicatorStart, 0)
                }
            }
            .padding(5.dp)
            .fillMaxSize()
            .drawWithContent {
                drawRoundRect(
                    color = indicatorColor,
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
    )
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScrollingFancyIndicatorContainerTabs() {
    var state by remember { mutableStateOf(0) }
    val titles =
        listOf(
            "Tab 1",
            "Tab 2",
            "Tab 3 with lots of text",
            "Tab 4",
            "Tab 5",
            "Tab 6 with lots of text",
            "Tab 7",
            "Tab 8",
            "Tab 9 with lots of text",
            "Tab 10",
        )

    Column {
        SecondaryScrollableTabRow(
            selectedTabIndex = state,
            indicator = { FancyAnimatedIndicatorWithModifier(state) },
        ) {
            titles.forEachIndexed { index, title ->
                Tab(selected = state == index, onClick = { state = index }, text = { Text(title) })
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling fancy transition tab ${state + 1} selected",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Sampled
@Composable
fun FancyTab(title: String, onClick: () -> Unit, selected: Boolean) {
    Tab(selected, onClick) {
        Column(
            Modifier.padding(10.dp).height(50.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.size(10.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color =
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.background
                    )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
