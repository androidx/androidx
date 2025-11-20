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

package androidx.xr.compose.material3.integration.testapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.SpaceToggleButton

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
internal fun XrSettingsPane(
    selectedNavSuiteType: NavigationSuiteType?,
    selectedOrbiterPosition: OrbiterPosition,
    onNavSuiteTypeChanged: (NavigationSuiteType?) -> Unit,
    onOrbiterPositionChanged: (OrbiterPosition) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ListItem(
                headlineContent = {
                    SpaceToggleButton(
                        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)
                    )
                }
            )
            ListItem(
                headlineContent = {
                    NavigationSuiteTypeDropdown(selectedNavSuiteType, onNavSuiteTypeChanged)
                }
            )
            ListItem(
                headlineContent = {
                    XrNavigationOrbiterPositionDropdown(
                        selectedOrbiterPosition,
                        onOrbiterPositionChanged,
                    )
                }
            )
        }
    }
}

@Composable
private fun NavigationSuiteTypeDropdown(
    navSuiteType: NavigationSuiteType?,
    onNavSuiteTypeChanged: (NavigationSuiteType?) -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    SimpleDropdown(
        dropdownLabel = "NavigationSuiteType",
        items =
            listOf(
                null,
                NavigationSuiteType.NavigationRail,
                NavigationSuiteType.WideNavigationRailExpanded,
                NavigationSuiteType.WideNavigationRailCollapsed,
                NavigationSuiteType.NavigationBar,
                NavigationSuiteType.ShortNavigationBarCompact,
                NavigationSuiteType.ShortNavigationBarMedium,
            ),
        selectedItem = navSuiteType,
        expanded = expanded,
        itemLabel = {
            when (it) {
                null -> "Default"
                NavigationSuiteType.NavigationRail -> "Rail"
                NavigationSuiteType.WideNavigationRailExpanded -> "Expressive Rail (Expanded)"
                NavigationSuiteType.WideNavigationRailCollapsed -> "Expressive Rail (Collapsed)"
                NavigationSuiteType.NavigationBar -> "Bar"
                NavigationSuiteType.ShortNavigationBarCompact -> "Expressive Bar (Compact)"
                NavigationSuiteType.ShortNavigationBarMedium -> "Expressive Bar (Medium)"
                else -> error("Unexpected NavigationSuiteType: $it")
            }
        },
        onSelectedChange = onNavSuiteTypeChanged,
    )
}

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
private fun XrNavigationOrbiterPositionDropdown(
    selectedItem: OrbiterPosition,
    onOrbiterPositionChanged: (OrbiterPosition) -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }
    SimpleDropdown(
        dropdownLabel = "NavigationSuite Orbiter Position",
        items = OrbiterPosition.entries.toList(),
        selectedItem = selectedItem,
        expanded = expanded,
        itemLabel = { it.name },
        onSelectedChange = onOrbiterPositionChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SimpleDropdown(
    dropdownLabel: String,
    items: List<T>,
    selectedItem: T,
    expanded: MutableState<Boolean>,
    itemLabel: (T) -> String,
    onSelectedChange: (T) -> Unit,
) {
    var state = rememberTextFieldState(itemLabel(selectedItem))
    Column {
        Text(text = dropdownLabel, style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = it },
        ) {
            TextField(
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                state = state,
                readOnly = true,
                lineLimits = TextFieldLineLimits.SingleLine,
                label = { itemLabel(selectedItem) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )

            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
            ) {
                for (item in items) {
                    DropdownMenuItem(
                        text = { Text(itemLabel(item)) },
                        onClick = {
                            state.setTextAndPlaceCursorAtEnd(itemLabel(item))
                            onSelectedChange(item)
                            expanded.value = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}
