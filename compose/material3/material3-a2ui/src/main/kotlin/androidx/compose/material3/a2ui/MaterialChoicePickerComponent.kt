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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.schema.A2uiArraySchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
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
import androidx.compose.material3.a2ui.icons.Close
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.util.fastMap

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"ChoicePicker"` component schema.
 *
 * A component that allows selecting one or more options from a list.
 *
 * **Schema Properties:**
 * * `label` (Dynamic String, optional): The label for the group of options.
 * * `variant` (String Enum, optional): A hint for how the choice picker should be displayed and
 *   behave. Valid options: `"multipleSelection"`, `"mutuallyExclusive"`. Defaults to
 *   `"mutuallyExclusive"`.
 * * `options` (NestedList, required): The list of available options to choose from. Each item
 *   defines `label` (Dynamic String, required) and `value` (String, required).
 * * `value` (Dynamic String List, required): The list of currently selected values. This should be
 *   bound to a string array in the data model.
 * * `displayStyle` (String Enum, optional): The display style of the component. Valid options:
 *   `"checkbox"`, `"chips"`. Defaults to `"checkbox"`.
 * * `filterable` (Boolean, optional): If true, displays a search input to filter the options.
 *   Defaults to `false`.
 */
public object MaterialChoicePickerComponent : A2uiComponent {

    private val labelProp =
        A2uiProperty.dynamicString(
            key = "label",
            required = false,
            description = "The label for the group of options.",
        )

    private val variantProp =
        A2uiProperty.enum(
            key = "variant",
            enumValues = ChoicePickerVariant.entries,
            mapToString = { it.token },
            convertFromString = { ChoicePickerVariant.fromToken(it) },
            defaultValue = ChoicePickerVariant.MutuallyExclusive,
            description = "A hint for how the choice picker should be displayed and behave.",
        )

    private val optionsProp =
        A2uiProperty.dynamicCustom(
            key = "options",
            schema =
                A2uiArraySchema(
                    items =
                        A2uiObjectSchema(
                            properties =
                                mapOf(
                                    "label" to
                                        A2uiDynamicStringSchema(
                                            description = "The text to display for this option."
                                        ),
                                    "value" to
                                        A2uiStringSchema(
                                            description =
                                                "The stable value associated with this option."
                                        ),
                                ),
                            required = setOf("label", "value"),
                            isAdditionalPropertiesAllowed = false,
                        ),
                    description = "The list of available options to choose from.",
                ),
            safeCast = { value ->
                (value as? List<*>)?.let { list ->
                    buildList(list.size) {
                        list.fastForEach { item ->
                            when (item) {
                                is OptionItem -> add(item)
                                is Map<*, *> -> {
                                    val label =
                                        item["label"]?.toString() ?: return@dynamicCustom null
                                    val optionValue =
                                        item["value"]?.toString() ?: return@dynamicCustom null
                                    add(OptionItem(label = label, value = optionValue))
                                }
                                else -> return@dynamicCustom null
                            }
                        }
                    }
                }
            },
            required = true,
        )

    private val valueProp =
        A2uiProperty.dynamicStringList(
            key = "value",
            required = true,
            description =
                "The list of currently selected values. This should be bound to a string array in the data model.",
        )

    private val displayStyleProp =
        A2uiProperty.enum(
            key = "displayStyle",
            enumValues = ChoicePickerDisplayStyle.entries,
            mapToString = { it.token },
            convertFromString = { ChoicePickerDisplayStyle.fromToken(it) },
            defaultValue = ChoicePickerDisplayStyle.Checkbox,
            description = "The display style of the component.",
        )

    private val filterableProp =
        A2uiProperty.boolean(
            key = "filterable",
            required = false,
            description = "If true, displays a search input to filter the options.",
        )

    override val name: String = "ChoicePicker"
    override val description: String =
        "A component that allows selecting one or more options from a list."
    override val properties: List<A2uiProperty<*>> =
        listOf(labelProp, variantProp, optionsProp, valueProp, displayStyleProp, filterableProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(valueProp) != null && properties.bind(optionsProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val groupLabel = properties.bind(labelProp)
        val variant = properties[variantProp] ?: ChoicePickerVariant.MutuallyExclusive
        val displayStyle = properties[displayStyleProp] ?: ChoicePickerDisplayStyle.Checkbox
        val isFilterable = properties[filterableProp] ?: false

        val options =
            checkNotNull(properties.bind(optionsProp)) {
                "Required property '${optionsProp.key}' is missing."
            }
        val selectedValues =
            checkNotNull(properties.bind(valueProp)) {
                "Required property '${valueProp.key}' is missing."
            }
        val onSelectedValuesChange = properties.bindUpdater(valueProp)
        val isEnabled = onSelectedValuesChange != null

        ReportDuplicateOptions(options)
        val selectedSet = remember(selectedValues) { selectedValues.toScatterSet() }

        var filterQuery by rememberSaveable { mutableStateOf("") }
        val visibleOptions =
            remember(isFilterable, filterQuery, options) {
                if (isFilterable && filterQuery.isNotEmpty()) {
                    options.fastFilter {
                        it.label.contains(filterQuery, ignoreCase = true)
                    }
                } else {
                    options
                }
            }

        val currentSelectedValues by rememberUpdatedState(selectedValues)
        val onToggleOption: (String) -> Unit =
            remember(variant, onSelectedValuesChange) {
                { optionValue ->
                    onSelectedValuesChange?.invoke(
                        variant.toggle(currentSelectedValues, optionValue)
                    )
                }
            }

        Column(modifier = modifier) {
            if (!groupLabel.isNullOrEmpty()) {
                Text(
                    text = groupLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = GroupLabelModifier,
                )
            }

            when (displayStyle) {
                ChoicePickerDisplayStyle.Checkbox -> {
                    ChoicePickerDropdown(
                        variant = variant,
                        allOptions = options,
                        visibleOptions = visibleOptions,
                        selectedSet = selectedSet,
                        filterQuery = filterQuery,
                        onFilterQueryChange = { filterQuery = it },
                        isFilterable = isFilterable,
                        hasHeaderLabel = !groupLabel.isNullOrEmpty(),
                        isEnabled = isEnabled,
                        onToggleOption = onToggleOption,
                    )
                }
                ChoicePickerDisplayStyle.Chips -> {
                    ChoicePickerChips(
                        visibleOptions = visibleOptions,
                        selectedSet = selectedSet,
                        filterQuery = filterQuery,
                        onFilterQueryChange = { filterQuery = it },
                        isFilterable = isFilterable,
                        isEnabled = isEnabled,
                        onToggleOption = onToggleOption,
                    )
                }
            }
        }
    }

    @Composable
    private fun A2uiComponentScope.ReportDuplicateOptions(options: List<OptionItem>) {
        val duplicates =
            remember(options) {
                val seen = mutableScatterSetOf<String>()
                val dups = mutableScatterSetOf<String>()
                options.fastForEach { option ->
                    if (!seen.add(option.value)) {
                        dups.add(option.value)
                    }
                }
                dups
            }

        if (duplicates.isNotEmpty()) {
            LaunchedEffect(duplicates) {
                val duplicatesString = buildString {
                    var first = true
                    duplicates.forEach { duplicate ->
                        if (!first) append(", ")
                        append("'$duplicate'")
                        first = false
                    }
                }
                reportError(
                    A2uiRuntimeException(
                        "Duplicate option values [$duplicatesString] found in ChoicePicker options."
                    )
                )
            }
        }
    }

    @Composable
    private fun ChoicePickerDropdown(
        variant: ChoicePickerVariant,
        allOptions: List<OptionItem>,
        visibleOptions: List<OptionItem>,
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
        val isMutuallyExclusive = variant == ChoicePickerVariant.MutuallyExclusive
        val isMultipleSelection = variant == ChoicePickerVariant.MultipleSelection

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
        chosenOption: OptionItem?,
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
        visibleOptions: List<OptionItem>,
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
        chosenOption: OptionItem?,
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
        selectedOptions: List<OptionItem>,
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
        visibleOptions: List<OptionItem>,
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
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }

    private enum class ChoicePickerVariant(val token: String) {
        MultipleSelection("multipleSelection") {
            override fun toggle(current: List<String>, value: String): List<String> =
                if (value in current) current.fastFilter { it != value } else current + value
        },
        MutuallyExclusive("mutuallyExclusive") {
            override fun toggle(current: List<String>, value: String): List<String> = listOf(value)
        };

        abstract fun toggle(current: List<String>, value: String): List<String>

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            fun fromToken(token: String?): ChoicePickerVariant =
                when (token) {
                    MultipleSelection.token -> MultipleSelection
                    else -> MutuallyExclusive
                }
        }
    }

    private enum class ChoicePickerDisplayStyle(val token: String) {
        Checkbox("checkbox"),
        Chips("chips");

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            fun fromToken(token: String?): ChoicePickerDisplayStyle =
                when (token) {
                    Chips.token -> Chips
                    else -> Checkbox
                }
        }
    }
}

private data class OptionItem(val label: String, val value: String)

private val GroupLabelModifier = Modifier.padding(bottom = 8.dp)
private val FilterTextFieldModifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
private val DropdownMenuHeightModifier = Modifier.heightIn(max = 192.dp)
private val ChipSpacingArrangement = Arrangement.spacedBy(4.dp)

private fun formatSelectedSummary(selectedOptions: List<OptionItem>): String = buildString {
    var first = true
    selectedOptions.fastForEach { option ->
        if (!first) append(", ")
        append(option.label)
        first = false
    }
}
