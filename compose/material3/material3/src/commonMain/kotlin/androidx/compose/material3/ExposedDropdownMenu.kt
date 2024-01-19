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

package androidx.compose.material3

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.tokens.FilledAutocompleteTokens
import androidx.compose.material3.tokens.OutlinedAutocompleteTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/menus/overview" class="external" target="_blank">Material Design Exposed Dropdown Menu</a>.
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * Exposed dropdown menus display the currently selected item in a text field to which the menu is
 * anchored. In some cases, it can accept and display user input (whether or not it’s listed as a
 * menu choice). If the text field input is used to filter results in the menu, the component is
 * also known as "autocomplete" or a "combobox".
 *
 * ![Exposed dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu.png)
 *
 * The [ExposedDropdownMenuBox] is expected to contain a [TextField] (or [OutlinedTextField]) and
 * [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] as content.
 *
 * An example of read-only Exposed Dropdown Menu:
 * @sample androidx.compose.material3.samples.ExposedDropdownMenuSample
 *
 * An example of editable Exposed Dropdown Menu:
 * @sample androidx.compose.material3.samples.EditableExposedDropdownMenuSample
 *
 * @param expanded whether the menu is expanded or not
 * @param onExpandedChange called when the exposed dropdown menu is clicked and the expansion state
 * changes.
 * @param modifier the [Modifier] to be applied to this exposed dropdown menu
 * @param content the content of this exposed dropdown menu, typically a [TextField] and an
 * [ExposedDropdownMenuBoxScope.ExposedDropdownMenu]. The [TextField] within [content] should be
 * passed the [ExposedDropdownMenuBoxScope.menuAnchor] modifier for proper menu behavior.
 */
@ExperimentalMaterial3Api
@Composable
expect fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
)

/**
 * Scope for [ExposedDropdownMenuBox].
 */
@ExperimentalMaterial3Api
interface ExposedDropdownMenuBoxScope {
    /**
     * Modifier which should be applied to a [TextField] (or [OutlinedTextField]) placed inside the
     * scope. It's responsible for properly anchoring the [ExposedDropdownMenu], handling semantics
     * of the component, and requesting focus.
     */
    fun Modifier.menuAnchor(): Modifier

    /**
     * Modifier which should be applied to an [ExposedDropdownMenu] placed inside the scope. It's
     * responsible for setting the width of the [ExposedDropdownMenu], which will match the width of
     * the [TextField] (if [matchTextFieldWidth] is set to true). It will also change the height of
     * [ExposedDropdownMenu], so it'll take the largest possible height to not overlap the
     * [TextField] and the software keyboard.
     *
     * @param matchTextFieldWidth whether the menu should match the width of the text field to which
     * it's attached. If set to `true`, the width will match the width of the text field.
     */
    fun Modifier.exposedDropdownSize(
        matchTextFieldWidth: Boolean = true
    ): Modifier

    /**
     * Popup which contains content for Exposed Dropdown Menu. Should be used inside the content of
     * [ExposedDropdownMenuBox].
     *
     * @param expanded whether the menu is expanded
     * @param onDismissRequest called when the user requests to dismiss the menu, such as by
     * tapping outside the menu's bounds
     * @param modifier the [Modifier] to be applied to this menu
     * @param content the content of the menu
     */
    @Suppress("ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE")
    @Composable
    fun ExposedDropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable ColumnScope.() -> Unit
    ) {
        ExposedDropdownMenuDefaultImpl(
            expanded, onDismissRequest, modifier, content
        )
    }
}

@Composable
internal expect fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
)

@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.expandable(
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    menuDescription: String = getString(Strings.ExposedDropdownMenu),
    expandedDescription: String = getString(Strings.MenuExpanded),
    collapsedDescription: String = getString(Strings.MenuCollapsed),
) = composed {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    pointerInput(Unit) {
        awaitEachGesture {
            // Must be PointerEventPass.Initial to observe events before the text field consumes them
            // in the Main pass
            awaitFirstDown(pass = PointerEventPass.Initial)
            val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (upEvent != null) {
                currentOnExpandedChange()
            }
        }
    }.semantics {
        stateDescription = if (expanded) expandedDescription else collapsedDescription
        contentDescription = menuDescription
        onClick {
            currentOnExpandedChange()
            true
        }
    }
}

/**
 * Contains default values used by Exposed Dropdown Menu.
 */
@ExperimentalMaterial3Api
object ExposedDropdownMenuDefaults {
    /**
     * Default trailing icon for Exposed Dropdown Menu.
     *
     * @param expanded whether [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] is expanded or not.
     * Affects the appearance of the icon.
     */
    @ExperimentalMaterial3Api
    @Composable
    fun TrailingIcon(expanded: Boolean) {
        Icon(
            Icons.Filled.ArrowDropDown,
            null,
            Modifier.rotate(if (expanded) 180f else 0f)
        )
    }

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in a [TextField] within an
     * [ExposedDropdownMenuBox].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     * focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     * state
     * @param focusedContainerColor the container color for this text field when focused
     * @param unfocusedContainerColor the container color for this text field when not focused
     * @param disabledContainerColor the container color for this text field when disabled
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
     * @param focusedIndicatorColor the indicator color for this text field when focused
     * @param unfocusedIndicatorColor the indicator color for this text field when not focused
     * @param disabledIndicatorColor the indicator color for this text field when disabled
     * @param errorIndicatorColor the indicator color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
     * focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @Composable
    fun textFieldColors(
        focusedTextColor: Color = FilledAutocompleteTokens.FieldFocusInputTextColor.toColor(),
        unfocusedTextColor: Color = FilledAutocompleteTokens.FieldInputTextColor.toColor(),
        disabledTextColor: Color = FilledAutocompleteTokens.FieldDisabledInputTextColor.toColor()
            .copy(alpha = FilledAutocompleteTokens.FieldDisabledInputTextOpacity),
        errorTextColor: Color = FilledAutocompleteTokens.FieldErrorInputTextColor.toColor(),
        focusedContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.toColor(),
        unfocusedContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.toColor(),
        disabledContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.toColor(),
        errorContainerColor: Color = FilledAutocompleteTokens.TextFieldContainerColor.toColor(),
        cursorColor: Color = FilledAutocompleteTokens.TextFieldCaretColor.toColor(),
        errorCursorColor: Color = FilledAutocompleteTokens.TextFieldErrorFocusCaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldFocusActiveIndicatorColor.toColor(),
        unfocusedIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldActiveIndicatorColor.toColor(),
        disabledIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorColor.toColor()
                .copy(alpha = FilledAutocompleteTokens.TextFieldDisabledActiveIndicatorOpacity),
        errorIndicatorColor: Color =
            FilledAutocompleteTokens.TextFieldErrorActiveIndicatorColor.toColor(),
        focusedLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusLeadingIconColor.toColor(),
        unfocusedLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldLeadingIconColor.toColor(),
        disabledLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledLeadingIconColor.toColor()
                .copy(alpha = FilledAutocompleteTokens.TextFieldDisabledLeadingIconOpacity),
        errorLeadingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorLeadingIconColor.toColor(),
        focusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldFocusTrailingIconColor.toColor(),
        unfocusedTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldTrailingIconColor.toColor(),
        disabledTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldDisabledTrailingIconColor.toColor()
                .copy(alpha = FilledAutocompleteTokens.TextFieldDisabledTrailingIconOpacity),
        errorTrailingIconColor: Color =
            FilledAutocompleteTokens.TextFieldErrorTrailingIconColor.toColor(),
        focusedLabelColor: Color = FilledAutocompleteTokens.FieldFocusLabelTextColor.toColor(),
        unfocusedLabelColor: Color = FilledAutocompleteTokens.FieldLabelTextColor.toColor(),
        disabledLabelColor: Color = FilledAutocompleteTokens.FieldDisabledLabelTextColor.toColor(),
        errorLabelColor: Color = FilledAutocompleteTokens.FieldErrorLabelTextColor.toColor(),
        focusedPlaceholderColor: Color =
            FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        unfocusedPlaceholderColor: Color =
            FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        disabledPlaceholderColor: Color =
            FilledAutocompleteTokens.FieldDisabledSupportingTextColor.toColor()
                .copy(alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity),
        errorPlaceholderColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        focusedPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        unfocusedPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        disabledPrefixColor: Color = FilledAutocompleteTokens.FieldDisabledSupportingTextColor
            .toColor().copy(alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity),
        errorPrefixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        focusedSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        unfocusedSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
        disabledSuffixColor: Color = FilledAutocompleteTokens.FieldDisabledSupportingTextColor
            .toColor().copy(alpha = FilledAutocompleteTokens.FieldDisabledSupportingTextOpacity),
        errorSuffixColor: Color = FilledAutocompleteTokens.FieldSupportingTextColor.toColor(),
    ): TextFieldColors =
        TextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in an [OutlinedTextField] within an
     * [ExposedDropdownMenuBox].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     * focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     * state
     * @param focusedContainerColor the container color for this text field when focused
     * @param unfocusedContainerColor the container color for this text field when not focused
     * @param disabledContainerColor the container color for this text field when disabled
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
     * @param focusedBorderColor the border color for this text field when focused
     * @param unfocusedBorderColor the border color for this text field when not focused
     * @param disabledBorderColor the border color for this text field when disabled
     * @param errorBorderColor the border color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @Composable
    fun outlinedTextFieldColors(
        focusedTextColor: Color = OutlinedAutocompleteTokens.FieldFocusInputTextColor.toColor(),
        unfocusedTextColor: Color = OutlinedAutocompleteTokens.FieldInputTextColor.toColor(),
        disabledTextColor: Color = OutlinedAutocompleteTokens.FieldDisabledInputTextColor.toColor()
            .copy(alpha = OutlinedAutocompleteTokens.FieldDisabledInputTextOpacity),
        errorTextColor: Color = OutlinedAutocompleteTokens.FieldErrorInputTextColor.toColor(),
        focusedContainerColor: Color = Color.Transparent,
        unfocusedContainerColor: Color = Color.Transparent,
        disabledContainerColor: Color = Color.Transparent,
        errorContainerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedAutocompleteTokens.TextFieldCaretColor.toColor(),
        errorCursorColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorFocusCaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldFocusOutlineColor.toColor(),
        unfocusedBorderColor: Color = OutlinedAutocompleteTokens.TextFieldOutlineColor.toColor(),
        disabledBorderColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledOutlineColor.toColor()
                .copy(alpha = OutlinedAutocompleteTokens.TextFieldDisabledOutlineOpacity),
        errorBorderColor: Color = OutlinedAutocompleteTokens.TextFieldErrorOutlineColor.toColor(),
        focusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusLeadingIconColor.toColor(),
        unfocusedLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldLeadingIconColor.toColor(),
        disabledLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconColor.toColor()
                .copy(alpha = OutlinedAutocompleteTokens.TextFieldDisabledLeadingIconOpacity),
        errorLeadingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorLeadingIconColor.toColor(),
        focusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldFocusTrailingIconColor.toColor(),
        unfocusedTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldTrailingIconColor.toColor(),
        disabledTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconColor.toColor()
                .copy(alpha = OutlinedAutocompleteTokens.TextFieldDisabledTrailingIconOpacity),
        errorTrailingIconColor: Color =
            OutlinedAutocompleteTokens.TextFieldErrorTrailingIconColor.toColor(),
        focusedLabelColor: Color = OutlinedAutocompleteTokens.FieldFocusLabelTextColor.toColor(),
        unfocusedLabelColor: Color = OutlinedAutocompleteTokens.FieldLabelTextColor.toColor(),
        disabledLabelColor: Color = OutlinedAutocompleteTokens.FieldDisabledLabelTextColor.toColor()
            .copy(alpha = OutlinedAutocompleteTokens.FieldDisabledLabelTextOpacity),
        errorLabelColor: Color = OutlinedAutocompleteTokens.FieldErrorLabelTextColor.toColor(),
        focusedPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        unfocusedPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        disabledPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor.toColor()
                .copy(alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity),
        errorPlaceholderColor: Color =
            OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        focusedPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        unfocusedPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        disabledPrefixColor: Color = OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor
            .toColor().copy(alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity),
        errorPrefixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        focusedSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        unfocusedSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
        disabledSuffixColor: Color = OutlinedAutocompleteTokens.FieldDisabledSupportingTextColor
            .toColor().copy(alpha = OutlinedAutocompleteTokens.FieldDisabledSupportingTextOpacity),
        errorSuffixColor: Color = OutlinedAutocompleteTokens.FieldSupportingTextColor.toColor(),
    ): TextFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            selectionColors = selectionColors,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            disabledBorderColor = disabledBorderColor,
            errorBorderColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    /**
     * Padding for [DropdownMenuItem]s within [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] to
     * align them properly with [TextField] components.
     */
    val ItemContentPadding: PaddingValues = PaddingValues(
        horizontal = ExposedDropdownMenuItemHorizontalPadding,
        vertical = 0.dp
    )
}

private val ExposedDropdownMenuItemHorizontalPadding = 16.dp
