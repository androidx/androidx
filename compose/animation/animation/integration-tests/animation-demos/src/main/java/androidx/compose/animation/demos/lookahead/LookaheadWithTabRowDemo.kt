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

package androidx.compose.animation.demos.lookahead

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Preview
@Composable
fun LookaheadWithTabRowDemo() {
    LookaheadScope {
        val isWide by produceState(false) {
            while (true) {
                delay(5000)
                value = !value
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .animateBounds(
                    if (isWide) Modifier else Modifier.padding(end = 100.dp)
                )
                .fillMaxHeight()
                .background(Color(0xFFfffbd0))
        ) {
            FancyTabs()
            ScrollingTextTabs()
            ScrollingFancyIndicatorContainerTabs()
        }
    }
}

@Composable
fun FancyTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf("TAB 1", "TAB 2", "TAB 3")
    Column {
        TabRow(
            selectedTabIndex = state,
        ) {
            titles.forEachIndexed { index, title ->
                FancyTab(title = title, onClick = { state = index }, selected = (index == state))
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Fancy tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun FancyTab(title: String, onClick: () -> Unit, selected: Boolean) {
    Tab(selected, onClick) {
        Column(
            Modifier
                .padding(10.dp)
                .height(50.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(color = if (selected) Color.Red else Color.White)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScrollingTextTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf(
        "Tab 1",
        "Tab 2",
        "Tab 3 with lots of text",
        "Tab 4",
        "Tab 5",
        "Tab 6 with lots of text",
        "Tab 7",
        "Tab 8",
        "Tab 9 with lots of text",
        "Tab 10"
    )
    Column {
        ScrollableTabRow(selectedTabIndex = state) {
            LookaheadScope {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = state == index,
                        onClick = { state = index },
                        text = { Text(title) }
                    )
                }
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling text tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
fun ScrollingFancyIndicatorContainerTabs() {
    var state by remember { mutableStateOf(0) }
    val titles = listOf(
        "Tab 1",
        "Tab 2",
        "Tab 3 with lots of text",
        "Tab 4",
        "Tab 5",
        "Tab 6 with lots of text",
        "Tab 7",
        "Tab 8",
        "Tab 9 with lots of text",
        "Tab 10"
    )

    // Reuse the default offset animation modifier, but use our own indicator
    val indicator = @Composable { tabPositions: List<TabPosition> ->
        FancyIndicator(Color.White, Modifier.tabIndicatorOffset(tabPositions[state]))
    }

    Column {
        ScrollableTabRow(
            selectedTabIndex = state,
            indicator = indicator
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = state == index,
                    onClick = { state = index },
                    text = { Text(title) }
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "Scrolling fancy transition tab ${state + 1} selected",
            style = MaterialTheme.typography.body1
        )
    }
}

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
