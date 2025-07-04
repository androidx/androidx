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
import androidx.compose.material3.Button
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
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.scenecore.scene

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
internal fun XrSettingsPane(
    onNavSuiteTypeChanged: (NavigationSuiteType?) -> Unit,
    onOrbiterEdgeOffsetChanged: (OrbiterOffsetType?) -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ListItem(headlineContent = { XrModeButton() })
            ListItem(headlineContent = { NavigationSuiteTypeButton(onNavSuiteTypeChanged) })
            ListItem(
                headlineContent = { XrNavigationOrbiterEdgeOffset(onOrbiterEdgeOffsetChanged) }
            )
        }
    }
}

@Composable
private fun XrModeButton() {
    val session = LocalSession.current
    val isDeviceXr = session != null
    val isFullSpaceMode = LocalSpatialCapabilities.current.isSpatialUiEnabled

    Button(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        enabled = isDeviceXr,
        onClick = {
            if (isFullSpaceMode) {
                session?.scene?.requestHomeSpaceMode()
            } else {
                session?.scene?.requestFullSpaceMode()
            }
        },
    ) {
        Text(
            text = if (isDeviceXr) "Toggle FullSpace/HomeSpace Mode" else "XR unsupported",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NavigationSuiteTypeButton(onNavSuiteTypeChanged: (NavigationSuiteType?) -> Unit) {
    val navSuiteType: MutableState<NavigationSuiteType?> = remember { mutableStateOf(null) }
    val expanded = remember { mutableStateOf(false) }
    SimpleDropdown(
        dropdownLabel = "NavigationSuiteType",
        items =
            listOf(
                null,
                NavigationSuiteType.NavigationRail,
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
                NavigationSuiteType.NavigationBar -> "Bar"
                NavigationSuiteType.ShortNavigationBarCompact -> "Expressive Bar (Compact)"
                NavigationSuiteType.ShortNavigationBarMedium -> "Expressive Bar (Medium)"
                else -> error("Unexpected NavigationSuiteType: $it")
            }
        },
        onSelectedChange = onNavSuiteTypeChanged,
    )
}

private enum class OrbiterEdgeOffsetTypeChoices {
    /** The default Orbiter EdgeOffset, as defined in the implementation. */
    Default,
    /** An inner Orbiter EdgeOffset. */
    Inner,
    /** An overlapping inner Orbiter EdgeOffset. */
    Overlap,
}

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
private fun XrNavigationOrbiterEdgeOffset(
    onOrbiterEdgeOffsetChanged: (OrbiterOffsetType?) -> Unit
) {
    val selectedItem = remember { mutableStateOf(OrbiterEdgeOffsetTypeChoices.Default) }
    val expanded = remember { mutableStateOf(false) }

    val selectedEdgeOffsetType =
        when (selectedItem.value) {
            OrbiterEdgeOffsetTypeChoices.Default -> OrbiterOffsetType.OuterEdge
            OrbiterEdgeOffsetTypeChoices.Inner -> OrbiterOffsetType.InnerEdge
            OrbiterEdgeOffsetTypeChoices.Overlap -> OrbiterOffsetType.Overlap
        }

    SimpleDropdown(
        dropdownLabel = "NavigationRail Orbiter EdgeOffset",
        items = OrbiterEdgeOffsetTypeChoices.values().asList(),
        selectedItem = selectedItem,
        expanded = expanded,
        itemLabel = { it.name },
        onSelectedChange = { onOrbiterEdgeOffsetChanged(selectedEdgeOffsetType) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SimpleDropdown(
    dropdownLabel: String,
    items: List<T>,
    selectedItem: MutableState<T>,
    expanded: MutableState<Boolean>,
    itemLabel: (T) -> String,
    onSelectedChange: (T) -> Unit,
) {
    var state = rememberTextFieldState(itemLabel(selectedItem.value))
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
                label = { itemLabel(selectedItem.value) },
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
                            selectedItem.value = item
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
