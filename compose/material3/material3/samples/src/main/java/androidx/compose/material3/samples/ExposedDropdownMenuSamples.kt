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

@file:OptIn(ExperimentalMaterial3Api::class)

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.substring
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Sampled
@Composable
fun ExposedDropdownMenuSample() {
    val options: List<String> = SampleData.take(5)
    var expanded by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(options[0])

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            // The `menuAnchor` modifier must be passed to the text field to handle
            // expanding/collapsing the menu on click. A read-only text field has
            // the anchor type `PrimaryNotEditable`.
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            state = textFieldState,
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("Label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview
@Sampled
@Composable
fun EditableExposedDropdownMenuSample() {
    val options: List<String> = SampleData
    val textFieldState = rememberTextFieldState()

    // The text that the user inputs into the text field can be used to filter the options.
    // This sample uses string subsequence matching.
    val filteredOptions = options.filteredBy(textFieldState.text)

    val (allowExpanded, setExpanded) = remember { mutableStateOf(false) }
    val expanded = allowExpanded && filteredOptions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = setExpanded,
    ) {
        TextField(
            // The `menuAnchor` modifier must be passed to the text field to handle
            // expanding/collapsing the menu on click. An editable text field has
            // the anchor type `PrimaryEditable`.
            modifier =
                Modifier.width(280.dp).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("Label") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                    // If the text field is editable, it is recommended to make the
                    // trailing icon a `menuAnchor` of type `SecondaryEditable`. This
                    // provides a better experience for certain accessibility services
                    // to choose a menu option without typing.
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            modifier = Modifier.heightIn(max = 280.dp),
            expanded = expanded,
            onDismissRequest = { setExpanded(false) },
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(option.text)
                        setExpanded(false)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview
@Sampled
@Composable
fun MultiAutocompleteExposedDropdownMenuSample() {
    /**
     * Returns the TextRange of the current token around the cursor, where commas define token
     * boundaries.
     */
    fun TextFieldState.currentTokenRange(): TextRange? {
        if (!selection.collapsed) return null

        val cursor = selection.start
        var start = cursor
        while (start > 0 && text[start - 1] != ',') {
            start--
        }
        while (start < cursor && text[start] == ' ') {
            start++
        }

        var end = cursor
        while (end < text.length && text[end] != ',') {
            end++
        }
        return TextRange(start, end)
    }

    fun TextFieldState.replaceThenAddComma(start: Int, end: Int, text: CharSequence) = edit {
        replace(start, end, text)
        val afterText = start + text.length
        if (afterText == this.length || this.charAt(afterText) != ',') {
            insert(afterText, ", ")
            placeCursorBeforeCharAt(afterText + 2)
        } else {
            placeCursorAfterCharAt(afterText)
        }
    }

    val allOptions: List<String> = SampleData
    val textFieldState = rememberTextFieldState()
    val tokenSelection = textFieldState.currentTokenRange()
    val tokenAtCursor =
        if (tokenSelection != null) textFieldState.text.substring(tokenSelection) else ""
    val filteredOptions =
        if (tokenAtCursor.isBlank()) emptyList() else allOptions.filteredBy(tokenAtCursor)

    val (allowExpanded, setExpanded) = remember { mutableStateOf(false) }
    val expanded = allowExpanded && filteredOptions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = setExpanded,
    ) {
        TextField(
            modifier =
                Modifier.width(280.dp).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("Label") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            modifier = Modifier.heightIn(max = 280.dp),
            expanded = expanded,
            onDismissRequest = { setExpanded(false) },
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        if (tokenSelection != null) {
                            textFieldState.replaceThenAddComma(
                                tokenSelection.start,
                                tokenSelection.end,
                                option
                            )
                        }
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/**
 * Returns the elements of [this] list that contain [text] as a subsequence, with the subsequence
 * underlined as an [AnnotatedString].
 */
private fun List<String>.filteredBy(text: CharSequence): List<AnnotatedString> {
    fun underlineSubsequence(needle: CharSequence, haystack: String): AnnotatedString? {
        return buildAnnotatedString {
            var i = 0
            for (char in needle) {
                val start = i
                haystack.indexOf(char, startIndex = i, ignoreCase = true).let {
                    if (it < 0) return null else i = it
                }
                append(haystack.substring(start, i))
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(haystack[i])
                }
                i += 1
            }
            append(haystack.substring(i, haystack.length))
        }
    }
    return this.mapNotNull { option -> underlineSubsequence(text, option) }
}

private val SampleData =
    listOf(
        "Android",
        "Base",
        "Cupcake",
        "Donut",
        "Eclair",
        "Froyo",
        "Gingerbread",
        "Honeycomb",
        "Ice Cream Sandwich",
        "Jelly Bean",
        "KitKat",
        "Lollipop",
        "Marshmallow",
        "Nougat",
        "Oreo",
        "Pie",
        "Quince Tart",
        "Red Velvet Cake",
        "Snow Cone",
        "Tiramisu",
        "Upside Down Cake",
        "Vanilla Ice Cream",
        "W123",
        "X456",
        "Y789",
        "Z000"
    )
