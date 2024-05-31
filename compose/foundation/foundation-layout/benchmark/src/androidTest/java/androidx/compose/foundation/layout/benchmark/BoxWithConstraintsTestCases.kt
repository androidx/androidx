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

package androidx.compose.foundation.layout.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Test case simulating an app that uses [BoxWithConstraints] to make complex layout changes. */
class BoxWithConstraintsAppTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private val phoneWidth = 360.dp
    private val tabletWidth = 900.dp

    private val screenWidth = mutableStateOf(phoneWidth)

    @Composable
    override fun MeasuredContent() {
        val width = screenWidth.value
        Box(Modifier.requiredWidth(width)) { App() }
    }

    @Composable
    private fun App() {
        BoxWithConstraints {
            val width = maxWidth
            App(width)
        }
    }

    override fun toggleState() {
        screenWidth.value = if (screenWidth.value == phoneWidth) tabletWidth else phoneWidth
    }
}

/**
 * Test case simulating an app that uses a theoretical screen width CompositionLocal to make complex
 * layout changes.
 */
class CompositionLocalAppTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private val phoneWidth = 360.dp
    private val tabletWidth = 900.dp

    private val screenWidth = mutableStateOf(phoneWidth)

    private val LocalScreenWidth = compositionLocalOf { phoneWidth }

    @Composable
    override fun MeasuredContent() {
        val width = screenWidth.value
        CompositionLocalProvider(LocalScreenWidth provides width) {
            Box(Modifier.requiredWidth(width)) { App() }
        }
    }

    @Composable
    private fun App() {
        val width = LocalScreenWidth.current
        App(width)
    }

    override fun toggleState() {
        screenWidth.value = if (screenWidth.value == phoneWidth) tabletWidth else phoneWidth
    }
}

@Composable
private fun App(width: Dp) {
    val gridColumns = width.value.toInt() / 150
    if (width > 800.dp) {
        TabletScreen(gridColumns)
    } else {
        PhoneScreen(gridColumns)
    }
}

@Composable
private fun PhoneScreen(gridColumns: Int) {
    Column {
        Grid(gridColumns, Modifier.weight(1f))
        var selectedItem by remember { mutableStateOf(0) }
        val items = listOf("Home", "Search", "Settings")
        val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings)
        BottomNavigation {
            items.forEachIndexed { index, item ->
                BottomNavigationItem(
                    icon = { Icon(icons[index], contentDescription = item) },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    }
}

@Composable
private fun TabletScreen(gridColumns: Int) {
    Row {
        var selectedItem by remember { mutableStateOf(0) }
        val items = listOf("Home", "Search", "Settings")
        val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings)
        NavigationRail {
            items.forEachIndexed { index, item ->
                NavigationRailItem(
                    icon = { Icon(icons[index], contentDescription = item) },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
        Grid(gridColumns)
    }
}

@Composable
private fun Grid(gridColumns: Int, modifier: Modifier = Modifier) {
    LazyVerticalGrid(modifier = modifier, columns = GridCells.Fixed(gridColumns)) {
        items(100) {
            Text(
                text = "$it",
                fontSize = 20.sp,
                modifier =
                    Modifier.background(Color.Gray.copy(alpha = (it % 10) / 10f)).padding(8.dp)
            )
        }
    }
}

/** A simpler test case just using normal [Box] */
class NoWithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun Content() {
        val size = remember { mutableStateOf(200.dp) }
        this.state = size
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Spacer(Modifier.size(width = size.value, height = size.value))
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

/** A simple test case just using normal [BoxWithConstraints] */
class BoxWithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun Content() {
        val size = remember { mutableStateOf(200.dp) }
        this.state = size
        BoxWithConstraints {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Spacer(Modifier.size(width = size.value, height = size.value))
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}
