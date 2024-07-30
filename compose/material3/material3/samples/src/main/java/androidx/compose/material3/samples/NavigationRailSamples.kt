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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalExpandedNavigationRail
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailArrangement
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.rememberModalExpandedNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun WideNavigationRailResponsiveSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    var expanded by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth()) {
        WideNavigationRail(
            expanded = expanded,
            header = {
                IconButton(
                    modifier =
                        Modifier.padding(start = 24.dp).semantics {
                            // The button must announce the expanded or collapsed state of the rail
                            // for accessibility.
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                        },
                    onClick = { expanded = !expanded }
                ) {
                    if (expanded) Icon(Icons.AutoMirrored.Filled.MenuOpen, "Collapse rail")
                    else Icon(Icons.Filled.Menu, "Expand rail")
                }
            }
        ) {
            items.forEachIndexed { index, item ->
                WideNavigationRailItem(
                    railExpanded = expanded,
                    icon = {
                        Icon(
                            if (selectedItem == index) selectedIcons[index]
                            else unselectedIcons[index],
                            contentDescription = null
                        )
                    },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }

        val textString = if (expanded) "expanded" else "collapsed"
        Column {
            Text(modifier = Modifier.padding(16.dp), text = "The rail is $textString.")
            Text(
                modifier = Modifier.padding(16.dp),
                text =
                    "Note: This demo is for demonstrative purposes. Use a Navigation Bar " +
                        "instead on compact screens."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ModalExpandedNavigationRailSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    var openModalRail by rememberSaveable { mutableStateOf(false) }
    var dismissRailOnItemSelection by rememberSaveable { mutableStateOf(true) }
    val modalRailState = rememberModalExpandedNavigationRailState()
    val scope = rememberCoroutineScope()

    Row(Modifier.fillMaxSize()) {
        if (openModalRail) {
            ModalExpandedNavigationRail(
                onDismissRequest = { openModalRail = false },
                railState = modalRailState
            ) {
                items.forEachIndexed { index, item ->
                    WideNavigationRailItem(
                        railExpanded = true,
                        icon = {
                            Icon(
                                if (selectedItem == index) selectedIcons[index]
                                else unselectedIcons[index],
                                contentDescription = null
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            if (dismissRailOnItemSelection) {
                                // Note: If you provide logic outside of onDismissRequest to close
                                // the rail, you must additionally handle intended state cleanup, if
                                // any.
                                scope
                                    .launch {
                                        // Add a minimum delay so that the selected state of the
                                        // item is properly announced to screen readers before the
                                        // rail closes.
                                        delay(250)
                                        modalRailState.close()
                                    }
                                    .invokeOnCompletion { openModalRail = false }
                            }
                        }
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { openModalRail = !openModalRail }, Modifier.padding(32.dp)) {
                Text(text = "Open modal rail")
            }
            Row(
                Modifier.toggleable(
                    value = dismissRailOnItemSelection,
                    role = Role.Checkbox,
                    onValueChange = { checked -> dismissRailOnItemSelection = checked }
                )
            ) {
                Checkbox(checked = dismissRailOnItemSelection, onCheckedChange = null)
                Spacer(Modifier.width(16.dp))
                Text("Dismiss rail on item selection.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun WideNavigationRailCollapsedSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    WideNavigationRail {
        items.forEachIndexed { index, item ->
            WideNavigationRailItem(
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = null
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun WideNavigationRailExpandedSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    WideNavigationRail(expanded = true) {
        items.forEachIndexed { index, item ->
            WideNavigationRailItem(
                railExpanded = true,
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = null
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun WideNavigationRailArrangementsSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    var expanded by remember { mutableStateOf(false) }
    var arrangement by remember { mutableStateOf(WideNavigationRailArrangement.Center) }

    Row(Modifier.fillMaxWidth()) {
        WideNavigationRail(
            expanded = expanded,
            arrangement = arrangement,
            header = {
                IconButton(
                    modifier =
                        Modifier.padding(start = 24.dp).semantics {
                            // The button must announce the expanded or collapsed state of the rail
                            // for accessibility.
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                        },
                    onClick = { expanded = !expanded }
                ) {
                    if (expanded) Icon(Icons.AutoMirrored.Filled.MenuOpen, "Collapse rail")
                    else Icon(Icons.Filled.Menu, "Expand rail")
                }
            }
        ) {
            items.forEachIndexed { index, item ->
                WideNavigationRailItem(
                    railExpanded = expanded,
                    icon = {
                        Icon(
                            if (selectedItem == index) selectedIcons[index]
                            else unselectedIcons[index],
                            contentDescription = null
                        )
                    },
                    label = { Text(item) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }

        val isArrangementCenter = arrangement == WideNavigationRailArrangement.Center
        val changeToString = if (isArrangementCenter) "Bottom" else "Center"
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(modifier = Modifier.padding(16.dp), text = "Change arrangement to:")
            Button(
                modifier = Modifier.padding(4.dp),
                onClick = {
                    if (isArrangementCenter) {
                        arrangement = WideNavigationRailArrangement.Bottom
                    } else {
                        arrangement = WideNavigationRailArrangement.Center
                    }
                }
            ) {
                Text(changeToString)
            }
            Text(
                modifier = Modifier.padding(16.dp),
                text =
                    "Note: This demo is for demonstrative purposes. Use a Navigation Bar " +
                        "instead on compact screens."
            )
        }
    }
}

@Preview
@Sampled
@Composable
fun NavigationRailSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    NavigationRail {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = item
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

@Composable
fun NavigationRailWithOnlySelectedLabelsSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)
    NavigationRail {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = item
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index },
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
fun NavigationRailBottomAlignSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Search", "Settings")
    val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Favorite, Icons.Filled.Star)
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.FavoriteBorder, Icons.Outlined.StarBorder)

    NavigationRail {
        // A Spacer that pushes the NavigationRail items to the bottom of the NavigationRail.
        Spacer(Modifier.weight(1f))
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                icon = {
                    Icon(
                        if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                        contentDescription = item
                    )
                },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index },
                alwaysShowLabel = false
            )
        }
    }
}
