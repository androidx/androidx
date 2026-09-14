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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.ChoicePicker.DisplayStyle
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.ChoicePicker.Option
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.ChoicePicker.Variant
import androidx.collection.ScatterSet
import androidx.collection.toScatterSet
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.R
import androidx.compose.material3.a2ui.icons.Close
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach

/**
 * A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"ChoicePicker"` component.
 */
internal object MaterialA2uiBasicCatalogV1ChoicePicker : A2uiBasicCatalogV1.ChoicePicker {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String?,
        options: List<Option>,
        value: List<String>,
        variant: Variant,
        displayStyle: DisplayStyle,
        filterable: Boolean,
        onValueChange: (List<String>) -> Unit,
        enabled: Boolean,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        checks: List<A2uiBasicCatalogV1.CheckRule>,
        modifier: Modifier,
    ) {
        val selectedSet = remember(value) { value.toScatterSet() }

        var filterQuery by rememberSaveable { mutableStateOf("") }
        val visibleOptions =
            remember(filterable, filterQuery, options) {
                if (filterable && filterQuery.isNotEmpty()) {
                    options.fastFilter { it.label.contains(filterQuery, ignoreCase = true) }
                } else {
                    options
                }
            }

        val currentSelectedValues by rememberUpdatedState(value)
        val onToggleOption: (String) -> Unit =
            remember(variant, onValueChange) {
                { optionValue ->
                    onValueChange(variant.toggle(currentSelectedValues, optionValue))
                }
            }

        Column(modifier = modifier.a2uiAccessibility(accessibility)) {
            if (!label.isNullOrEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = GroupLabelModifier,
                )
            }

            when (displayStyle) {
                DisplayStyle.Checkbox -> {
                    ChoicePickerDropdown(
                        variant = variant,
                        allOptions = options,
                        visibleOptions = visibleOptions,
                        selectedSet = selectedSet,
                        filterQuery = filterQuery,
                        onFilterQueryChange = { filterQuery = it },
                        isFilterable = filterable,
                        hasHeaderLabel = !label.isNullOrEmpty(),
                        isEnabled = enabled,
                        onToggleOption = onToggleOption,
                    )
                }
                DisplayStyle.Chips -> {
                    ChoicePickerChips(
                        visibleOptions = visibleOptions,
                        selectedSet = selectedSet,
                        filterQuery = filterQuery,
                        onFilterQueryChange = { filterQuery = it },
                        isFilterable = filterable,
                        isEnabled = enabled,
                        onToggleOption = onToggleOption,
                    )
                }
            }
        }
    }

    @Composable
    private fun ChoicePickerDropdown(
        variant: Variant,
        allOptions: List<Option>,
        visibleOptions: List<Option>,
        selectedSet: ScatterSet<String>,
        filterQuery: String,
        onFilterQueryChange: (String) -> Unit,
        isFilterable: Boolean,
        hasHeaderLabel: Boolean,
        isEnabled: Boolean,
        onToggleOption: (String) -> Unit,
    ) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        if (!isEnabled) {
            expanded = false
        }

        val selectedOptions =
            remember(allOptions, selectedSet) {
                allOptions.fastFilter { selectedSet.contains(it.value) }
            }
        val chosenOption = selectedOptions.firstOrNull()
        val isMutuallyExclusive = variant == Variant.MutuallyExclusive
        val isMultipleSelection = variant == Variant.MultipleSelection

        val textFieldValue =
            when {
                isFilterable -> filterQuery
                isMutuallyExclusive ->
                    remember(selectedOptions) { formatSelectedSummary(selectedOptions) }
                else -> ""
            }

        val placeholderText =
            when {
                isFilterable -> stringResource(R.string.choice_picker_placeholder_filter)
                !hasHeaderLabel -> stringResource(R.string.choice_picker_placeholder_select_options)
                else -> stringResource(R.string.choice_picker_placeholder_show_options)
            }

        Column {
            ExposedDropdownMenuBox(
                expanded = expanded && isEnabled,
                onExpandedChange = { newExpanded ->
                    if (isEnabled) {
                        expanded = newExpanded
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ChoicePickerTriggerField(
                    textFieldValue = textFieldValue,
                    isFilterable = isFilterable,
                    placeholderText = placeholderText,
                    filterQuery = filterQuery,
                    onFilterQueryChange = onFilterQueryChange,
                    onExpandedChange = { expanded = it },
                    isMutuallyExclusive = isMutuallyExclusive,
                    expanded = expanded,
                    chosenOption = chosenOption,
                    isEnabled = isEnabled,
                    onToggleOption = onToggleOption,
                )

                ChoicePickerMenu(
                    expanded = expanded && isEnabled,
                    onDismissRequest = { expanded = false },
                    visibleOptions = visibleOptions,
                    selectedSet = selectedSet,
                    isMutuallyExclusive = isMutuallyExclusive,
                    isFilterable = isFilterable,
                    isEnabled = isEnabled,
                    onFilterQueryChange = onFilterQueryChange,
                    onToggleOption = onToggleOption,
                    onExpandedChange = { expanded = it },
                )
            }

            if (isMultipleSelection && selectedOptions.isNotEmpty()) {
                SelectedChipsRow(
                    selectedOptions = selectedOptions,
                    isEnabled = isEnabled,
                    onToggleOption = onToggleOption,
                )
            }
        }
    }

    @Composable
    private fun ExposedDropdownMenuBoxScope.ChoicePickerTriggerField(
        textFieldValue: String,
        isFilterable: Boolean,
        placeholderText: String,
        filterQuery: String,
        onFilterQueryChange: (String) -> Unit,
        onExpandedChange: (Boolean) -> Unit,
        isMutuallyExclusive: Boolean,
        expanded: Boolean,
        chosenOption: Option?,
        isEnabled: Boolean,
        onToggleOption: (String) -> Unit,
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                if (isFilterable) {
                    onFilterQueryChange(newValue)
                    onExpandedChange(true)
                }
            },
            readOnly = !isFilterable,
            placeholder = { SingleLineText(text = placeholderText) },
            trailingIcon = {
                ChoicePickerTrailingIcon(
                    isFilterable = isFilterable,
                    filterQuery = filterQuery,
                    onClearFilter = { onFilterQueryChange("") },
                    isMutuallyExclusive = isMutuallyExclusive,
                    expanded = expanded,
                    chosenOption = chosenOption,
                    isEnabled = isEnabled,
                    onToggleOption = onToggleOption,
                )
            },
            enabled = isEnabled,
            singleLine = true,
            modifier =
                Modifier.menuAnchor(
                        if (isFilterable) {
                            ExposedDropdownMenuAnchorType.PrimaryEditable
                        } else {
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        },
                        isEnabled,
                    )
                    .fillMaxWidth(),
        )
    }

    @Composable
    private fun ExposedDropdownMenuBoxScope.ChoicePickerMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        visibleOptions: List<Option>,
        selectedSet: ScatterSet<String>,
        isMutuallyExclusive: Boolean,
        isFilterable: Boolean,
        isEnabled: Boolean,
        onFilterQueryChange: (String) -> Unit,
        onToggleOption: (String) -> Unit,
        onExpandedChange: (Boolean) -> Unit,
    ) {
        val scrollState = rememberScrollState()
        val scrollIndicatorState = scrollState.scrollIndicatorState
        val scrollbarModifier =
            if (scrollIndicatorState != null) {
                Modifier.nonInteractiveScrollbar(
                    scrollIndicatorState,
                    orientation = Orientation.Vertical,
                )
            } else {
                Modifier
            }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            scrollState = scrollState,
            modifier = DropdownMenuHeightModifier.then(scrollbarModifier),
        ) {
            if (visibleOptions.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.choice_picker_no_matching_options),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    enabled = false,
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            } else {
                visibleOptions.fastForEach { option ->
                    key(option.value) {
                        val isSelected = selectedSet.contains(option.value)
                        DropdownMenuItem(
                            text = { Text(text = option.label) },
                            onClick = {
                                onToggleOption(option.value)
                                if (isMutuallyExclusive) {
                                    if (isFilterable) {
                                        onFilterQueryChange("")
                                    }
                                    onExpandedChange(false)
                                }
                            },
                            modifier =
                                (if (isSelected) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    } else {
                                        Modifier
                                    })
                                    .semantics { selected = isSelected },
                            colors =
                                if (isSelected) {
                                    MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    MenuDefaults.itemColors()
                                },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            enabled = isEnabled,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ChoicePickerTrailingIcon(
        isFilterable: Boolean,
        filterQuery: String,
        onClearFilter: () -> Unit,
        isMutuallyExclusive: Boolean,
        expanded: Boolean,
        chosenOption: Option?,
        isEnabled: Boolean,
        onToggleOption: (String) -> Unit,
    ) {
        when {
            !isFilterable -> {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
            !isMutuallyExclusive && filterQuery.isNotEmpty() -> {
                ClearTextIconButton(onClear = onClearFilter, isEnabled = isEnabled)
            }
            isMutuallyExclusive && chosenOption != null -> {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                ) {
                    InputChip(
                        selected = true,
                        onClick = { onToggleOption(chosenOption.value) },
                        label = { SingleLineText(chosenOption.label) },
                        enabled = isEnabled,
                        modifier = Modifier.widthIn(max = 140.dp).padding(end = 8.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun ClearTextIconButton(onClear: () -> Unit, isEnabled: Boolean) {
        IconButton(onClick = onClear, enabled = isEnabled) {
            Icon(
                imageVector = Close,
                contentDescription = stringResource(R.string.choice_picker_clear_search),
            )
        }
    }

    @Composable
    private fun SelectedChipsRow(
        selectedOptions: List<Option>,
        isEnabled: Boolean,
        onToggleOption: (String) -> Unit,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            FlowRow(
                horizontalArrangement = ChipSpacingArrangement,
                verticalArrangement = ChipSpacingArrangement,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                selectedOptions.fastForEach { option ->
                    InputChip(
                        selected = true,
                        onClick = { onToggleOption(option.value) },
                        label = { SingleLineText(option.label) },
                        trailingIcon = {
                            Icon(
                                imageVector = Close,
                                contentDescription =
                                    stringResource(
                                        R.string.choice_picker_remove_option,
                                        option.label,
                                    ),
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                        enabled = isEnabled,
                    )
                }
            }
        }
    }

    @Composable
    private fun ChoicePickerChips(
        visibleOptions: List<Option>,
        selectedSet: ScatterSet<String>,
        filterQuery: String,
        onFilterQueryChange: (String) -> Unit,
        isFilterable: Boolean,
        isEnabled: Boolean,
        onToggleOption: (String) -> Unit,
    ) {
        if (isFilterable) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = onFilterQueryChange,
                placeholder = {
                    SingleLineText(stringResource(R.string.choice_picker_placeholder_filter))
                },
                trailingIcon =
                    if (filterQuery.isNotEmpty()) {
                        {
                            ClearTextIconButton(
                                onClear = { onFilterQueryChange("") },
                                isEnabled = isEnabled,
                            )
                        }
                    } else {
                        null
                    },
                singleLine = true,
                modifier = FilterTextFieldModifier,
                enabled = isEnabled,
            )
        }

        if (visibleOptions.isEmpty()) {
            Text(
                text = stringResource(R.string.choice_picker_no_matching_options),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                FlowRow(
                    horizontalArrangement = ChipSpacingArrangement,
                    verticalArrangement = ChipSpacingArrangement,
                ) {
                    visibleOptions.fastForEach { option ->
                        key(option.value) {
                            val isSelected = selectedSet.contains(option.value)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleOption(option.value) },
                                label = { SingleLineText(option.label) },
                                enabled = isEnabled,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SingleLineText(text: String, modifier: Modifier = Modifier) {
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    }
}

private val GroupLabelModifier = Modifier.padding(bottom = 8.dp)
private val FilterTextFieldModifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
private val DropdownMenuHeightModifier = Modifier.heightIn(max = 192.dp)
private val ChipSpacingArrangement = Arrangement.spacedBy(4.dp)

private fun formatSelectedSummary(selectedOptions: List<Option>): String = buildString {
    var first = true
    selectedOptions.fastForEach { option ->
        if (!first) append(", ")
        append(option.label)
        first = false
    }
}

private fun Variant.toggle(current: List<String>, value: String): List<String> =
    when (this) {
        Variant.MutuallyExclusive -> listOf(value)
        Variant.MultipleSelection ->
            if (value in current) {
                current.fastFilter { it != value }
            } else {
                current + value
            }
    }
