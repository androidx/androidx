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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.End
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExpandableHorizontalFloatingToolbarSample() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        content = { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                LazyColumn(
                    // Apply a floatingToolbarVerticalNestedScroll Modifier toggle the expanded
                    // state of the HorizontalFloatingToolbar.
                    modifier =
                        Modifier.floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false },
                        ),
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val list = (0..75).map { it.toString() }
                    items(count = list.size) {
                        Text(
                            text = list[it],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }
                HorizontalFloatingToolbar(
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                    expanded = expanded,
                    leadingContent = { LeadingContent() },
                    trailingContent = { TrailingContent() },
                    content = {
                        FilledIconButton(
                            modifier = Modifier.width(64.dp),
                            onClick = { /* doSomething() */ }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ScrollableHorizontalFloatingToolbarSample() {
    val exitAlwaysScrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
    Scaffold(
        modifier = Modifier.nestedScroll(exitAlwaysScrollBehavior),
        content = { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val list = (0..75).map { it.toString() }
                    items(count = list.size) {
                        Text(
                            text = list[it],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }
                HorizontalFloatingToolbar(
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                    expanded = true,
                    leadingContent = { LeadingContent() },
                    trailingContent = { TrailingContent() },
                    content = {
                        FilledIconButton(
                            modifier = Modifier.width(64.dp),
                            onClick = { /* doSomething() */ }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                    },
                    scrollBehavior = exitAlwaysScrollBehavior
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExpandableVerticalFloatingToolbarSample() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        content = { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                LazyColumn(
                    // Apply a floatingToolbarVerticalNestedScroll Modifier toggle the expanded
                    // state of the HorizontalFloatingToolbar.
                    modifier =
                        Modifier.floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false },
                        ),
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val list = (0..75).map { it.toString() }
                    items(count = list.size) {
                        Text(
                            text = list[it],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }
                VerticalFloatingToolbar(
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = -ScreenOffset),
                    expanded = expanded,
                    leadingContent = { LeadingContent() },
                    trailingContent = { TrailingContent() },
                    content = {
                        FilledIconButton(
                            modifier = Modifier.height(64.dp),
                            onClick = { /* doSomething() */ }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                    },
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ScrollableVerticalFloatingToolbarSample() {
    val exitAlwaysScrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = End)
    Scaffold(
        modifier = Modifier.nestedScroll(exitAlwaysScrollBehavior),
        content = { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val list = (0..75).map { it.toString() }
                    items(count = list.size) {
                        Text(
                            text = list[it],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }
                VerticalFloatingToolbar(
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = -ScreenOffset),
                    expanded = true,
                    leadingContent = { LeadingContent() },
                    trailingContent = { TrailingContent() },
                    content = {
                        FilledIconButton(
                            modifier = Modifier.height(64.dp),
                            onClick = { /* doSomething() */ }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                    },
                    scrollBehavior = exitAlwaysScrollBehavior
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun HorizontalFloatingToolbarWithFabSample() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    Scaffold { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Apply a floatingToolbarVerticalNestedScroll Modifier to the Column to toggle
                    // the expanded state of the HorizontalFloatingToolbar.
                    .floatingToolbarVerticalNestedScroll(
                        expanded = expanded,
                        onExpand = { expanded = true },
                        onCollapse = { expanded = false }
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = remember { LoremIpsum().values.first() })
            }
            HorizontalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = {
                    // Match the FAB to the vibrantColors. See also StandardFloatingActionButton.
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { /* doSomething() */ },
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .offset(x = -ScreenOffset, y = -ScreenOffset),
                colors = vibrantColors,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun CenteredHorizontalFloatingToolbarWithFabSample() {
    val exitAlwaysScrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    Scaffold(modifier = Modifier.nestedScroll(exitAlwaysScrollBehavior)) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = remember { LoremIpsum().values.first() })
            }
            HorizontalFloatingToolbar(
                // Always expanded as the toolbar is bottom-centered. We will use a
                // FloatingToolbarScrollBehavior to hide both the toolbar and its FAB on scroll.
                expanded = true,
                floatingActionButton = {
                    // Match the FAB to the vibrantColors. See also StandardFloatingActionButton.
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { /* doSomething() */ },
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset),
                colors = vibrantColors,
                scrollBehavior = exitAlwaysScrollBehavior,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun VerticalFloatingToolbarWithFabSample() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    Scaffold { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Apply a floatingToolbarVerticalNestedScroll Modifier to the Column to toggle
                    // the expanded state of the VerticalFloatingToolbar.
                    .then(
                        Modifier.floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false }
                        )
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = remember { LoremIpsum().values.first() })
            }
            VerticalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = {
                    // Match the FAB to the vibrantColors. See also StandardFloatingActionButton.
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { /* doSomething() */ },
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
                modifier =
                    Modifier.align(Alignment.BottomEnd)
                        .offset(x = -ScreenOffset, y = -ScreenOffset),
                colors = vibrantColors,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun CenteredVerticalFloatingToolbarWithFabSample() {
    val exitAlwaysScrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = End)
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    Scaffold(modifier = Modifier.nestedScroll(exitAlwaysScrollBehavior)) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = remember { LoremIpsum().values.first() })
            }
            VerticalFloatingToolbar(
                // Always expanded as the toolbar is right-centered. We will use a
                // FloatingToolbarScrollBehavior to hide both the toolbar and its FAB on scroll.
                expanded = true,
                floatingActionButton = {
                    // Match the FAB to the vibrantColors. See also StandardFloatingActionButton.
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { /* doSomething() */ },
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = -ScreenOffset),
                colors = vibrantColors,
                scrollBehavior = exitAlwaysScrollBehavior,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun HorizontalFloatingToolbarAsScaffoldFabSample() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    Scaffold(
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = {
                    // Match the FAB to the vibrantColors. See also StandardFloatingActionButton.
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(Icons.Filled.Add, "Localized description")
                    }
                },
                colors = vibrantColors,
                content = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Person, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Localized description")
                    }
                },
            )
        },
        // When setting this to `FabPosition.Start` remember to set a
        // `floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.Start` at the
        // HorizontalFloatingToolbar as well.
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Apply a floatingToolbarVerticalNestedScroll Modifier to the Column to toggle
                    // the expanded state of the HorizontalFloatingToolbar.
                    .then(
                        Modifier.floatingToolbarVerticalNestedScroll(
                            expanded = expanded,
                            onExpand = { expanded = true },
                            onCollapse = { expanded = false },
                        )
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = remember { LoremIpsum().values.first() })
            }
        }
    }
}

@Composable
private fun LeadingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Check, contentDescription = "Localized description")
    }
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
    }
}

@Composable
private fun TrailingContent() {
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Download, contentDescription = "Localized description")
    }
    IconButton(onClick = { /* doSomething() */ }) {
        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
    }
}
