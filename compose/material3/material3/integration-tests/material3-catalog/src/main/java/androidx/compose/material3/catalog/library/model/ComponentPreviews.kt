/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.model

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AdaptivePreview() {
    Surface(
        modifier = Modifier.size(width = 88.dp, height = 56.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Surface(
                modifier = Modifier.weight(0.4f).fillMaxHeight(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {}
            Surface(
                modifier = Modifier.weight(0.6f).fillMaxHeight(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {}
        }
    }
}

@Composable
internal fun BadgePreview() {
    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
        BadgedBox(badge = { Badge { Text("3") } }) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
internal fun BottomAppBarPreview() {
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Surface(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BottomSheetPreview() {
    Surface(
        modifier = Modifier.size(width = 84.dp, height = 54.dp),
        shape =
            RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp,
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(width = 24.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth(0.7f)
                        .align(Alignment.Start)
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

@Composable
internal fun ButtonsPreview() {
    Button(onClick = {}) { Text("Button") }
}

@Composable
internal fun ButtonGroupsPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalButton(
            onClick = {},
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("One")
        }
        Button(
            onClick = {},
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("Two")
        }
    }
}

@Composable
internal fun CardPreview() {
    ElevatedCard(modifier = Modifier.size(width = 84.dp, height = 56.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier.size(width = 36.dp, height = 8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Box(
                    modifier =
                        Modifier.fillMaxWidth(0.65f)
                            .height(5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }
    }
}

@Composable
internal fun CarouselPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(width = 44.dp, height = 54.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {}
        Surface(
            modifier = Modifier.size(width = 24.dp, height = 54.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {}
        Surface(
            modifier = Modifier.size(width = 12.dp, height = 54.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {}
    }
}

@Composable
internal fun CheckboxesPreview() {
    Checkbox(checked = true, onCheckedChange = {})
}

@Composable
internal fun ChipsPreview() {
    FilterChip(
        selected = true,
        onClick = {},
        label = { Text("Chip") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}

@Composable
internal fun DatePickersPreview() {
    OutlinedCard(modifier = Modifier.size(width = 76.dp, height = 60.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(2) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        repeat(4) { col ->
                            val selected = row == 0 && col == 2
                            Box(
                                modifier =
                                    Modifier.size(8.dp)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape,
                                        )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DialogsPreview() {
    Surface(
        modifier = Modifier.size(width = 84.dp, height = 58.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier.size(width = 40.dp, height = 7.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
            )
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier =
                        Modifier.size(width = 20.dp, height = 7.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

@Composable
internal fun ExtendedFabPreview() {
    ExtendedFloatingActionButton(
        onClick = {},
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("FAB") },
    )
}

@Composable
internal fun FloatingActionButtonsPreview() {
    FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, contentDescription = null) }
}

@Composable
internal fun FabMenuPreview() {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
            }
        }
        SmallFloatingActionButton(onClick = {}) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FloatingToolbarsPreview() {
    HorizontalFloatingToolbar(expanded = true) {
        IconButton(onClick = {}) { Icon(Icons.Filled.Edit, contentDescription = null) }
        IconButton(onClick = {}) { Icon(Icons.Filled.Favorite, contentDescription = null) }
    }
}

@Composable
internal fun IconButtonsPreview() {
    FilledIconButton(onClick = {}) { Icon(Icons.Filled.Lock, contentDescription = null) }
}

@Composable
internal fun ListsPreview() {
    Surface(
        modifier = Modifier.width(96.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.size(14.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(
                            modifier =
                                Modifier.size(width = 48.dp, height = 5.dp)
                                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        )
                        Box(
                            modifier =
                                Modifier.size(width = 32.dp, height = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape,
                                    )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoadingIndicatorsPreview() {
    ContainedLoadingIndicator(progress = { 0.75f })
}

@Composable
internal fun MenusPreview() {
    Surface(
        modifier = Modifier.width(84.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            repeat(2) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (index == 0) Icons.Filled.Edit else Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                    Box(
                        modifier =
                            Modifier.size(width = 36.dp, height = 5.dp)
                                .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                    )
                }
                if (index == 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
internal fun NavigationBarPreview() {
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 36.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp).size(14.dp),
                )
            }
            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun NavigationDrawerPreview() {
    Surface(
        modifier = Modifier.size(width = 88.dp, height = 56.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.width(48.dp).fillMaxHeight(),
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    CircleShape,
                                )
                    )
                    Box(
                        modifier =
                            Modifier.fillMaxWidth(0.8f)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
internal fun NavigationRailPreview() {
    Surface(
        modifier = Modifier.size(width = 40.dp, height = 64.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).size(14.dp),
                )
            }
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(14.dp))
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
internal fun NavigationSuiteScaffoldPreview() {
    Surface(
        modifier = Modifier.size(width = 88.dp, height = 56.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier.width(24.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Box(
                    modifier =
                        Modifier.size(8.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(6.dp),
                        )
            )
        }
    }
}

@Composable
internal fun ProgressIndicatorsPreview() {
    CircularProgressIndicator(progress = { 0.7f })
}

@Composable
internal fun PullToRefreshPreview() {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
internal fun RadioButtonsPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        RadioButton(selected = true, onClick = null)
        RadioButton(selected = false, onClick = null)
    }
}

@Composable
internal fun ScrollFieldPreview() {
    Surface(
        modifier = Modifier.size(width = 56.dp, height = 60.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                "11",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.alpha(0.4f),
            )
            Text(
                "12",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "13",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.alpha(0.4f),
            )
        }
    }
}

@Composable
internal fun SearchBarsPreview() {
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 38.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(
                modifier =
                    Modifier.size(width = 40.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

@Composable
internal fun SegmentedButtonsPreview() {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = true,
            onClick = {},
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            label = {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            icon = {},
        )
        SegmentedButton(
            selected = false,
            onClick = {},
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            label = {
                Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            icon = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlidersPreview() {
    val sliderState = rememberSliderState(value = 0.65f)
    Slider(state = sliderState, onValueChange = {}, modifier = Modifier.width(96.dp))
}

@Composable
internal fun SnackbarsPreview() {
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 36.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier.size(width = 44.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.inverseOnSurface, CircleShape)
            )
            Box(
                modifier =
                    Modifier.size(width = 18.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.inversePrimary, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SplitButtonsPreview() {
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = {}) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(checked = false, onCheckedChange = {}) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@Composable
internal fun SwitchesPreview() {
    Switch(
        checked = true,
        onCheckedChange = {},
        thumbContent = {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

@Composable
internal fun TabsPreview() {
    Surface(
        modifier = Modifier.width(96.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        PrimaryTabRow(selectedTabIndex = 0) {
            Tab(
                selected = true,
                onClick = {},
                icon = {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            Tab(
                selected = false,
                onClick = {},
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
internal fun TextFieldsPreview() {
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 40.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Abc|", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun TimePickersPreview() {
    Surface(
        modifier = Modifier.size(width = 88.dp, height = 44.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    "10",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Text(":", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    "30",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ToggleButtonsPreview() {
    ToggleButton(checked = true, onCheckedChange = {}) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        Text("Like")
    }
}

@Composable
internal fun TooltipsPreview() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Text(
                "Tooltip",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Box(
            modifier =
                Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
internal fun TopAppBarPreview() {
    Surface(
        modifier = Modifier.size(width = 96.dp, height = 38.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                Box(
                    modifier =
                        Modifier.size(width = 32.dp, height = 6.dp)
                            .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                )
            }
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun TransitionsPreview() {
    Box(modifier = Modifier.size(width = 76.dp, height = 54.dp)) {
        Surface(
            modifier = Modifier.size(width = 52.dp, height = 40.dp).align(Alignment.TopStart),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {}
        Surface(
            modifier =
                Modifier.size(width = 52.dp, height = 40.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {}
    }
}

@Composable
internal fun TypographyPreview() {
    Text(
        text = "Aa",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.SemiBold,
    )
}
