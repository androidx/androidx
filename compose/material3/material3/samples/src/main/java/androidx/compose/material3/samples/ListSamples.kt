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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Sampled
@Composable
fun OneLineListItem() {
    Column {
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("One line list item with 24x24 icon") },
            leadingContent = {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            },
        )
        HorizontalDivider()
    }
}

@Preview
@Sampled
@Composable
fun TwoLineListItem() {
    Column {
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Two line list item with trailing") },
            supportingContent = { Text("Secondary text") },
            trailingContent = { Text("meta") },
            leadingContent = {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            },
        )
        HorizontalDivider()
    }
}

@Preview
@Sampled
@Composable
fun ThreeLineListItemWithOverlineAndSupporting() {
    Column {
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Three line list item") },
            overlineContent = { Text("OVERLINE") },
            supportingContent = { Text("Secondary text") },
            leadingContent = {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            },
            trailingContent = { Text("meta") },
        )
        HorizontalDivider()
    }
}

@Preview
@Sampled
@Composable
fun ThreeLineListItemWithExtendedSupporting() {
    Column {
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Three line list item") },
            supportingContent = { Text("Secondary text that\nspans multiple lines") },
            leadingContent = {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            },
            trailingContent = { Text("meta") },
        )
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ClickableListItemSample() {
    Column {
        HorizontalDivider()

        repeat(3) { idx ->
            var count by rememberSaveable { mutableIntStateOf(0) }
            ListItem(
                onClick = { count++ },
                leadingContent = { Icon(Icons.Default.Home, contentDescription = null) },
                trailingContent = { Text("$count") },
                supportingContent = { Text("Additional info") },
                content = { Text("Item ${idx + 1}") },
            )

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ClickableListItemWithClickableChildSample() {
    Column {
        HorizontalDivider()

        repeat(3) { idx ->
            ListItem(
                onClick = { /* ListItem onClick callback */ },
                leadingContent = { Icon(Icons.Default.Home, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { /* Child onClick callback */ }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Localized description")
                    }
                },
                supportingContent = { Text("The trailing icon has a separate click action") },
                content = { Text("Item ${idx + 1}") },
            )

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SingleSelectionListItemSample() {
    Column(Modifier.selectableGroup()) {
        HorizontalDivider()

        var selectedIndex: Int? by rememberSaveable { mutableStateOf(null) }
        repeat(3) { idx ->
            val selected = selectedIndex == idx
            ListItem(
                selected = selected,
                onClick = { selectedIndex = if (selected) null else idx },
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                trailingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
                supportingContent = { Text("Additional info") },
                content = { Text("Item ${idx + 1}") },
            )

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun MultiSelectionListItemSample() {
    Column {
        HorizontalDivider()

        repeat(3) { idx ->
            var checked by rememberSaveable { mutableStateOf(false) }
            ListItem(
                checked = checked,
                onCheckedChange = { checked = it },
                leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                trailingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
                supportingContent = { Text("Additional info") },
                content = { Text("Item ${idx + 1}") },
            )

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun ListItemWithModeChangeOnLongClickSample() {
    Column {
        HorizontalDivider()

        var inClickMode by rememberSaveable { mutableStateOf(true) }
        val counts = rememberSaveable { mutableStateListOf(0, 0, 0) }
        val checked = rememberSaveable { mutableStateListOf(false, false, false) }

        repeat(3) { idx ->
            if (inClickMode) {
                ListItem(
                    onClick = { counts[idx]++ },
                    onLongClick = {
                        checked[idx] = true
                        inClickMode = false
                    },
                    leadingContent = { Icon(Icons.Default.Home, contentDescription = null) },
                    trailingContent = { Text("${counts[idx]}") },
                    supportingContent = { Text("Long-click to change interaction mode.") },
                    content = { Text("Item ${idx + 1}") },
                )
            } else {
                ListItem(
                    checked = checked[idx],
                    onCheckedChange = { checked[idx] = it },
                    onLongClick = {
                        inClickMode = true
                        checked.clear()
                        checked.addAll(listOf(false, false, false))
                    },
                    leadingContent = { Checkbox(checked = checked[idx], onCheckedChange = null) },
                    trailingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    supportingContent = { Text("Long-click to change interaction mode.") },
                    content = { Text("Item ${idx + 1}") },
                )
            }

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SingleSelectionSegmentedListItemSample() {
    val count = 4
    val colors =
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        var selectedIndex: Int? by rememberSaveable { mutableStateOf(null) }
        repeat(count) { idx ->
            val selected = selectedIndex == idx
            SegmentedListItem(
                selected = selected,
                onClick = { selectedIndex = if (selected) null else idx },
                colors = colors,
                shapes = ListItemDefaults.segmentedShapes(index = idx, count = count),
                leadingContent = { RadioButton(selected = selected, onClick = null) },
                trailingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
                supportingContent = { Text("Additional info") },
                content = { Text("Item ${idx + 1}") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun MultiSelectionSegmentedListItemSample() {
    val count = 4
    val colors =
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        repeat(count) { idx ->
            var checked by rememberSaveable { mutableStateOf(false) }
            SegmentedListItem(
                checked = checked,
                onCheckedChange = { checked = it },
                colors = colors,
                shapes = ListItemDefaults.segmentedShapes(index = idx, count = count),
                leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                trailingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
                supportingContent = { Text("Additional info") },
                content = { Text("Item ${idx + 1}") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SegmentedListItemWithExpansionSample() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val numChildren = 3
    val itemCount = 1 + if (expanded) numChildren else 0
    val childrenChecked = rememberSaveable { mutableStateListOf(*Array(numChildren) { false }) }

    val colors =
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        Spacer(Modifier.height(100.dp))
        SegmentedListItem(
            onClick = { expanded = !expanded },
            modifier =
                Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            colors = colors,
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            leadingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
            trailingContent = {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            },
            content = { Text("Click to expand/collapse") },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                repeat(numChildren) { idx ->
                    SegmentedListItem(
                        checked = childrenChecked[idx],
                        onCheckedChange = { childrenChecked[idx] = it },
                        colors = colors,
                        shapes =
                            ListItemDefaults.segmentedShapes(index = idx + 1, count = itemCount),
                        leadingContent = {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                        },
                        trailingContent = {
                            Checkbox(checked = childrenChecked[idx], onCheckedChange = null)
                        },
                        content = { Text("Child ${idx + 1}") },
                    )
                }
            }
        }
    }
}
