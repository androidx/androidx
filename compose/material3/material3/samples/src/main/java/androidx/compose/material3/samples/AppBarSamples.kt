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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * A sample for a simple use of small [TopAppBar].
 *
 * The top app bar here does not react to any scroll events in the content under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SimpleTopAppBar() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Simple TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SimpleTopAppBarWithAdaptiveActions() {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    // Material guidelines state 3 items max in compact, and 5 items max elsewhere.
    // To test this, try a resizable emulator, or a phone in landscape and portrait orientation.
    val maxItemCount =
        if (sizeClass.minWidthDp >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) {
            5
        } else {
            3
        }
    val icons =
        listOf(
            Icons.Filled.Attachment,
            Icons.Filled.Edit,
            Icons.Outlined.Star,
            Icons.Filled.Snooze,
            Icons.Outlined.MarkEmailUnread,
        )
    val items = listOf("Attachment", "Edit", "Star", "Snooze", "Mark unread")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Simple TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    AppBarRow(
                        maxItemCount = maxItemCount,
                        overflowIndicator = {
                            TooltipBox(
                                positionProvider =
                                    TooltipDefaults.rememberTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text("Overflow") } },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(onClick = { it.show() }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Overflow",
                                    )
                                }
                            }
                        },
                    ) {
                        // TODO: These icons should have tooltips.
                        items.forEachIndexed { index, item ->
                            clickableItem(
                                onClick = {},
                                icon = {
                                    TooltipBox(
                                        positionProvider =
                                            TooltipDefaults.rememberTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text(item) } },
                                        state = rememberTooltipState(),
                                    ) {
                                        Icon(imageVector = icons[index], contentDescription = item)
                                    }
                                },
                                label = item,
                            )
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a simple use of small [TopAppBar] with a subtitle.
 *
 * The top app bar here does not react to any scroll events in the content under it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SimpleTopAppBarWithSubtitle() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text("Simple TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                subtitle = { Text("Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a simple use of [CenterAlignedTopAppBar].
 *
 * The top app bar here does not react to any scroll events in the content under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SimpleCenterAlignedTopAppBar() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Centered TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a simple use of small [CenterAlignedTopAppBar] with a subtitle.
 *
 * The top app bar here does not react to any scroll events in the content under it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SimpleCenterAlignedTopAppBarWithSubtitle() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text("Simple TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                subtitle = { Text("Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a pinned small [TopAppBar].
 *
 * The top app bar here is pinned to its location and changes its container color when the content
 * under it is scrolled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun PinnedTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    // RowScope here, so these icons will be placed horizontally
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to starred") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Star, contentDescription = "Star")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a small [TopAppBar] that collapses when the content is scrolled up, and appears when
 * the content scrolled down.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun EnterAlwaysTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text("Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [MediumTopAppBar] that collapses when the content is scrolled up, and appears when
 * the content is completely scrolled back down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun ExitUntilCollapsedMediumTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text("Medium TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [MediumFlexibleTopAppBar] that collapses when the content is scrolled up, and
 * appears when the content is completely scrolled back down, centered with subtitle.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitUntilCollapsedCenterAlignedMediumFlexibleTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text("Medium TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                subtitle = { Text("Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [LargeTopAppBar] that collapses when the content is scrolled up, and appears when
 * the content is completely scrolled back down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun ExitUntilCollapsedLargeTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Large TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [LargeFlexibleTopAppBar] that collapses when the content is scrolled up, and
 * appears when the content is completely scrolled back down, centered with subtitle.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitUntilCollapsedCenterAlignedLargeFlexibleTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Large TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text("Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Add to favorites") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Add to favorites",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [TwoRowsTopAppBar] that collapses when the content is scrolled up, and appears
 * when the content is completely scrolled back down.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun CustomTwoRowsTopAppBar() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TwoRowsTopAppBar(
                title = { expanded ->
                    if (expanded) {
                        Text("Expanded TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("Collapsed TopAppBar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                subtitle = { expanded ->
                    if (expanded) {
                        Text(
                            "Expanded Subtitle",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 24.dp),
                        )
                    } else {
                        Text("Collapsed Subtitle", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                collapsedHeight = 64.dp,
                expandedHeight = 156.dp,
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Menu") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            Column(
                Modifier.fillMaxWidth().padding(innerPadding).verticalScroll(rememberScrollState())
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge
                ) {
                    Text(text = remember { LoremIpsum().values.first() })
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SimpleBottomAppBar() {
    BottomAppBar(
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Menu") } },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu button")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun BottomAppBarWithFAB() {
    BottomAppBar(
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Check") } },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Check, contentDescription = "Check")
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Edit") } },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
        },
        floatingActionButton = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Add") } },
                state = rememberTooltipState(),
            ) {
                FloatingActionButton(
                    onClick = { /* do something */ },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                ) {
                    Icon(Icons.Filled.Add, "Add")
                }
            }
        },
    )
}

/**
 * A sample for a [BottomAppBar] that collapses when the content is scrolled up, and appears when
 * the content scrolled down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun ExitAlwaysBottomAppBar() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            BottomAppBar(
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Check") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Check")
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Edit") } },
                        state = rememberTooltipState(),
                    ) {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Add") } },
                state = rememberTooltipState(),
            ) {
                FloatingActionButton(
                    modifier = Modifier.offset(y = 4.dp),
                    onClick = { /* do something */ },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                ) {
                    Icon(Icons.Filled.Add, "Add")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay,
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [FlexibleBottomAppBar] that collapses when the content is scrolled up, and appears
 * when the content scrolled down. The content is spaced around.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitAlwaysBottomAppBarSpacedAround() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val icons =
        listOf(
            Icons.AutoMirrored.Filled.ArrowBack,
            Icons.AutoMirrored.Filled.ArrowForward,
            Icons.Filled.Add,
            Icons.Filled.Check,
            Icons.Filled.Edit,
        )
    val buttons = listOf("Back", "Forward", "Add", "Check", "Edit")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            FlexibleBottomAppBar(
                horizontalArrangement = Arrangement.SpaceAround,
                contentPadding = PaddingValues(horizontal = 0.dp),
                scrollBehavior = scrollBehavior,
                content = {
                    buttons.forEachIndexed { index, button ->
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(button) } },
                            state = rememberTooltipState(),
                        ) {
                            if (index == 2) {
                                FilledIconButton(
                                    modifier = Modifier.width(56.dp),
                                    onClick = { /* doSomething() */ },
                                ) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            } else {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            }
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [FlexibleBottomAppBar] that collapses when the content is scrolled up, and appears
 * when the content scrolled down. The content is spaced between.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitAlwaysBottomAppBarSpacedBetween() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val icons =
        listOf(
            Icons.AutoMirrored.Filled.ArrowBack,
            Icons.AutoMirrored.Filled.ArrowForward,
            Icons.Filled.Add,
            Icons.Filled.Check,
            Icons.Filled.Edit,
        )
    val buttons = listOf("Back", "Forward", "Add", "Check", "Edit")
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            FlexibleBottomAppBar(
                horizontalArrangement = Arrangement.SpaceBetween,
                scrollBehavior = scrollBehavior,
                content = {
                    buttons.forEachIndexed { index, button ->
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(button) } },
                            state = rememberTooltipState(),
                        ) {
                            if (index == 2) {
                                FilledIconButton(
                                    modifier = Modifier.width(56.dp),
                                    onClick = { /* doSomething() */ },
                                ) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            } else {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            }
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [FlexibleBottomAppBar] that collapses when the content is scrolled up, and appears
 * when the content scrolled down. The content is spaced evenly.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitAlwaysBottomAppBarSpacedEvenly() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val icons =
        listOf(
            Icons.AutoMirrored.Filled.ArrowBack,
            Icons.AutoMirrored.Filled.ArrowForward,
            Icons.Filled.Add,
            Icons.Filled.Check,
            Icons.Filled.Edit,
        )
    val buttons = listOf("Back", "Forward", "Add", "Check", "Edit")
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            FlexibleBottomAppBar(
                horizontalArrangement = Arrangement.SpaceEvenly,
                contentPadding = PaddingValues(horizontal = 0.dp),
                scrollBehavior = scrollBehavior,
                content = {
                    buttons.forEachIndexed { index, button ->
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(button) } },
                            state = rememberTooltipState(),
                        ) {
                            if (index == 2) {
                                FilledIconButton(
                                    modifier = Modifier.width(56.dp),
                                    onClick = { /* doSomething() */ },
                                ) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            } else {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            }
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a [FlexibleBottomAppBar] that collapses when the content is scrolled up, and appears
 * when the content scrolled down. The content arrangement is fixed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitAlwaysBottomAppBarFixed() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val icons =
        listOf(
            Icons.AutoMirrored.Filled.ArrowBack,
            Icons.AutoMirrored.Filled.ArrowForward,
            Icons.Filled.Add,
            Icons.Filled.Check,
            Icons.Filled.Edit,
        )
    val buttons = listOf("Back", "Forward", "Add", "Check", "Edit")
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            FlexibleBottomAppBar(
                horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
                scrollBehavior = scrollBehavior,
                content = {
                    buttons.forEachIndexed { index, button ->
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(button) } },
                            state = rememberTooltipState(),
                        ) {
                            if (index == 2) {
                                FilledIconButton(
                                    modifier = Modifier.width(56.dp),
                                    onClick = { /* doSomething() */ },
                                ) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            } else {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            }
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/**
 * A sample for a vibrant [FlexibleBottomAppBar] that collapses when the content is scrolled up, and
 * appears when the content scrolled down. The content arrangement is fixed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ExitAlwaysBottomAppBarFixedVibrant() {
    val scrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val icons =
        listOf(
            Icons.AutoMirrored.Filled.ArrowBack,
            Icons.AutoMirrored.Filled.ArrowForward,
            Icons.Filled.Add,
            Icons.Filled.Check,
            Icons.Filled.Edit,
        )
    val buttons = listOf("Back", "Forward", "Add", "Check", "Edit")
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            FlexibleBottomAppBar(
                horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
                scrollBehavior = scrollBehavior,
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer, // TODO(b/356885344): tokens
                content = {
                    buttons.forEachIndexed { index, button ->
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(button) } },
                            state = rememberTooltipState(),
                        ) {
                            if (index == 2) {
                                FilledIconButton(
                                    modifier = Modifier.width(56.dp),
                                    onClick = { /* doSomething() */ },
                                ) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            } else {
                                IconButton(onClick = { /* doSomething() */ }) {
                                    Icon(icons[index], contentDescription = button)
                                }
                            }
                        }
                    }
                },
            )
        },
        content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

/** A sample for a [FlexibleBottomAppBar] with an overflow behavior when the content doesn't fit. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun BottomAppBarWithOverflow() {
    val icons =
        listOf(
            Icons.AutoMirrored.Filled.ArrowBack,
            Icons.AutoMirrored.Filled.ArrowForward,
            Icons.Filled.Add,
            Icons.Filled.Check,
            Icons.Filled.Edit,
            Icons.Filled.Favorite,
        )
    val items = listOf("Back", "Forward", "Add", "Check", "Edit", "Favorite")
    FlexibleBottomAppBar(
        contentPadding = PaddingValues(horizontal = 96.dp),
        horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
    ) {
        AppBarRow(
            overflowIndicator = { menuState ->
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Overflow") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = {
                            if (menuState.isExpanded) {
                                menuState.dismiss()
                            } else {
                                menuState.show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Overflow")
                    }
                }
            }
        ) {
            // TODO: These items should have tooltips.
            items.forEachIndexed { index, item ->
                clickableItem(
                    onClick = { /* doSomething() */ },
                    icon = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(item) } },
                            state = rememberTooltipState(),
                        ) {
                            Icon(icons[index], contentDescription = item)
                        }
                    },
                    label = item,
                )
            }
        }
    }
}
