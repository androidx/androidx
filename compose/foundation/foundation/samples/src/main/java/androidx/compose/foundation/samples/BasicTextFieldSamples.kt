/*
 * Copyright 2020 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)
@file:Suppress("UNUSED_PARAMETER", "unused", "LocalVariableName", "RedundantSuspendModifier")

package androidx.compose.foundation.samples

import android.text.TextUtils
import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.ExpandPolicy
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.forEachChange
import androidx.compose.foundation.text.input.forEachChangeReversed
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@Sampled
@Composable
fun BasicTextFieldSample() {
    var value by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    BasicTextField(
        value = value,
        onValueChange = {
            // it is crucial that the update is fed back into BasicTextField in order to
            // see updates on the text
            value = it
        },
    )
}

@Sampled
@Composable
fun BasicTextFieldWithStringSample() {
    var value by rememberSaveable { mutableStateOf("initial value") }
    BasicTextField(
        value = value,
        onValueChange = {
            // it is crucial that the update is fed back into BasicTextField in order to
            // see updates on the text
            value = it
        },
    )
}

@Sampled
@Composable
fun PlaceholderBasicTextFieldSample() {
    var value by rememberSaveable { mutableStateOf("initial value") }
    Box {
        BasicTextField(value = value, onValueChange = { value = it })
        if (value.isEmpty()) {
            Text(text = "Placeholder")
        }
    }
}

@Sampled
@Composable
fun TextFieldWithIconSample() {
    var value by rememberSaveable { mutableStateOf("initial value") }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        decorationBox = { innerTextField ->
            // Because the decorationBox is used, the whole Row gets the same behaviour as the
            // internal input field would have otherwise. For example, there is no need to add a
            // Modifier.clickable to the Row anymore to bring the text field into focus when user
            // taps on a larger text field area which includes paddings and the icon areas.
            Row(
                Modifier.background(Color.LightGray, RoundedCornerShape(percent = 30))
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = null)
                Spacer(Modifier.width(16.dp))
                innerTextField()
            }
        },
    )
}

@Sampled
@Composable
fun CreditCardSample() {
    /** The offset translator used for credit card input field */
    val creditCardOffsetTranslator =
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset < 4 -> offset
                    offset < 8 -> offset + 1
                    offset < 12 -> offset + 2
                    offset <= 16 -> offset + 3
                    else -> 19
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 4 -> offset
                    offset <= 9 -> offset - 1
                    offset <= 14 -> offset - 2
                    offset <= 19 -> offset - 3
                    else -> 16
                }
            }
        }

    /**
     * Converts up to 16 digits to hyphen connected 4 digits string. For example, "1234567890123456"
     * will be shown as "1234-5678-9012-3456"
     */
    val creditCardTransformation = VisualTransformation { text ->
        val trimmedText = if (text.text.length > 16) text.text.substring(0..15) else text.text
        var transformedText = ""
        trimmedText.forEachIndexed { index, char ->
            transformedText += char
            if ((index + 1) % 4 == 0 && index != 15) transformedText += "-"
        }
        TransformedText(AnnotatedString(transformedText), creditCardOffsetTranslator)
    }

    var text by rememberSaveable { mutableStateOf("") }
    BasicTextField(
        value = text,
        onValueChange = { input ->
            if (input.length <= 16 && input.none { !it.isDigit() }) {
                text = input
            }
        },
        modifier = Modifier.size(170.dp, 30.dp).background(Color.LightGray).wrapContentSize(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = creditCardTransformation,
    )
}

@Sampled
fun BasicTextFieldStateCompleteSample() {
    class SearchViewModel(val searchFieldState: TextFieldState = TextFieldState()) {
        private val queryValidationRegex = """\w+""".toRegex()

        // Use derived state to avoid recomposing every time the text changes, and only recompose
        // when the input becomes valid or invalid.
        val isQueryValid by derivedStateOf {
            // This lambda will be re-executed every time inputState.text changes.
            searchFieldState.text.matches(queryValidationRegex)
        }

        var searchResults: List<String> by mutableStateOf(emptyList())
            private set

        /** Called while the view model is active, e.g. from a LaunchedEffect. */
        suspend fun run() {
            snapshotFlow { searchFieldState.text }
                .collectLatest { queryText ->
                    // Start a new search every time the user types something valid. If the previous
                    // search is still being processed when the text is changed, it will be
                    // cancelled
                    // and this code will run again with the latest query text.
                    if (isQueryValid) {
                        searchResults = performSearch(query = queryText)
                    }
                }
        }

        fun clearQuery() {
            searchFieldState.setTextAndPlaceCursorAtEnd("")
        }

        private suspend fun performSearch(query: CharSequence): List<String> {
            TODO()
        }
    }

    @Composable
    fun SearchScreen(viewModel: SearchViewModel) {
        Column {
            Row {
                BasicTextField(viewModel.searchFieldState)
                IconButton(onClick = { viewModel.clearQuery() }) {
                    Icon(Icons.Default.Clear, contentDescription = "clear search query")
                }
            }
            if (!viewModel.isQueryValid) {
                Text("Invalid query", style = TextStyle(color = Color.Red))
            }
            LazyColumn { items(viewModel.searchResults) { TODO() } }
        }
    }
}

@Sampled
fun BasicTextFieldTextDerivedStateSample() {
    class ViewModel {
        private val inputValidationRegex = """\w+""".toRegex()

        val inputState = TextFieldState()

        // Use derived state to avoid recomposing every time the text changes, and only recompose
        // when the input becomes valid or invalid.
        val isInputValid by derivedStateOf {
            // This lambda will be re-executed every time inputState.text changes.
            inputState.text.matches(inputValidationRegex)
        }
    }

    @Composable
    fun Screen(viewModel: ViewModel) {
        Column {
            BasicTextField(viewModel.inputState)
            if (!viewModel.isInputValid) {
                Text("Input is invalid.", style = TextStyle(color = Color.Red))
            }
        }
    }
}

@Sampled
fun BasicTextFieldStateEditSample() {
    val state = TextFieldState("hello world!")
    state.edit {
        // Insert a comma after "hello".
        insert(5, ",") // = "hello, world!"

        // Delete the exclamation mark.
        delete(12, 13) // = "hello, world"

        // Add a different name.
        append("Compose") // = "hello, Compose"

        // Say goodbye.
        replace(0, 5, "goodbye") // "goodbye, Compose"

        // Select the new name so the user can change it by just starting to type.
        selection = TextRange(9, 16) // "goodbye, ̲C̲o̲m̲p̲o̲s̲e"
    }
}

@Sampled
@Composable
fun BasicTextFieldCustomInputTransformationSample() {
    // Demonstrates how to create a custom and relatively complex InputTransformation.
    val state = remember { TextFieldState() }
    BasicTextField(
        state,
        inputTransformation =
            InputTransformation {
                // A filter that always places newly-input text at the start of the string, after a
                // prompt character, like a shell.
                val promptChar = '>'

                fun CharSequence.countPrefix(char: Char): Int {
                    var i = 0
                    while (i < length && get(i) == char) i++
                    return i
                }

                // Step one: Figure out the insertion point.
                val newPromptChars = asCharSequence().countPrefix(promptChar)
                val insertionPoint = if (newPromptChars == 0) 0 else 1

                // Step two: Ensure text is placed at the insertion point.
                if (changes.changeCount == 1) {
                    val insertedRange = changes.getRange(0)
                    val replacedRange = changes.getOriginalRange(0)
                    if (!replacedRange.collapsed && insertedRange.collapsed) {
                        // Text was deleted, delete forwards from insertion point.
                        delete(insertionPoint, insertionPoint + replacedRange.length)
                    }
                }
                // Else text was replaced or there were multiple changes - don't handle.

                // Step three: Ensure the prompt character is there.
                if (newPromptChars == 0) {
                    insert(0, ">")
                }

                // Step four: Ensure the cursor is ready for the next input.
                placeCursorAfterCharAt(0)
            },
    )
}

@Sampled
@Composable
fun BasicTextFieldOutputTransformationSample() {
    @Stable
    data class PhoneNumberOutputTransformation(private val pad: Boolean) : OutputTransformation {
        override fun TextFieldBuffer.transformOutput() {
            if (pad) {
                // Pad the text with placeholder chars if too short.
                // (___) ___-____
                val padCount = 10 - length
                repeat(padCount) { append('_') }
            }

            // (123) 456-7890
            if (length > 0) insert(0, "(")
            if (length > 4) insert(4, ") ")
            if (length > 9) insert(9, "-")
        }
    }

    val state = rememberTextFieldState()
    BasicTextField(
        state,
        inputTransformation =
            InputTransformation.maxLength(10).then {
                if (!TextUtils.isDigitsOnly(asCharSequence())) {
                    revertAllChanges()
                }
            },
        outputTransformation = PhoneNumberOutputTransformation(false),
    )
}

@Sampled
@Composable
fun BasicTextFieldAnnotatedOutputTransformationSample() {
    val state = rememberTextFieldState()
    BasicTextField(
        state,
        inputTransformation =
            InputTransformation.maxLength(10).then {
                if (!TextUtils.isDigitsOnly(asCharSequence())) {
                    revertAllChanges()
                }
            },
        outputTransformation =
            OutputTransformation {
                // Find hashtags
                val regex = Regex("#\\w+")
                regex
                    .findAll(asCharSequence())
                    .map { it.range }
                    .forEach {
                        addStyle(SpanStyle(color = Color.Blue), it.start, it.endInclusive + 1)
                    }
            },
    )
}

@Sampled
@Composable
fun BasicTextFieldInputTransformationByValueReplaceSample() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state,
        // Convert tabs to spaces.
        inputTransformation =
            InputTransformation.byValue { _, proposed ->
                proposed.replace("""\t""".toRegex(), "  ")
            },
    )
}

@Sampled
@Composable
fun BasicTextFieldInputTransformationByValueChooseSample() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state,
        // Reject whitespace.
        inputTransformation =
            InputTransformation.byValue { current, proposed ->
                if ("""\s""".toRegex() in proposed) current else proposed
            },
    )
}

@Sampled
fun BasicTextFieldInputTransformationChainingSample() {
    val removeFirstEFilter = InputTransformation {
        val index = asCharSequence().indexOf('e')
        if (index != -1) {
            replace(index, index + 1, "")
        }
    }
    val printECountFilter = InputTransformation {
        println("found ${asCharSequence().count { it == 'e' }} 'e's in the string")
    }

    // Returns a filter that always prints 0 e's.
    removeFirstEFilter.then(printECountFilter)

    // Returns a filter that prints the number of e's before the first one is removed.
    printECountFilter.then(removeFirstEFilter)
}

@Sampled
@Composable
fun BasicTextFieldInputTransformationMaxLengthCustom() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state,
        inputTransformation =
            object : InputTransformation {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    maxLength(14)
                }

                override fun TextFieldBuffer.transformInput() {
                    if (length > 10) revertAllChanges()
                }
            },
        outputTransformation =
            OutputTransformation {
                if (length > 0) insert(0, "(")
                if (length > 4) insert(4, ") ")
                if (length > 9) insert(9, "-")
            },
    )
}

@Sampled
@Composable
fun BasicTextFieldChangeIterationSample() {
    // Print a log message every time the text is changed.
    BasicTextField(
        state = rememberTextFieldState(),
        inputTransformation = {
            changes.forEachChange { sourceRange, replacedLength ->
                val newString = asCharSequence().substring(sourceRange)
                println("""$replacedLength characters were replaced with "$newString"""")
            }
        },
    )
}

@Sampled
@Composable
fun BasicTextFieldChangeReverseIterationSample() {
    // Make a text field behave in "insert mode" – inserted text overwrites the text ahead of it
    // instead of being inserted.
    BasicTextField(
        state = rememberTextFieldState(),
        inputTransformation = {
            changes.forEachChangeReversed { range, originalRange ->
                if (!range.collapsed && originalRange.collapsed) {
                    // New text was inserted, delete the text ahead of it.
                    delete(
                        range.end.coerceAtMost(length),
                        (range.end + range.length).coerceAtMost(length),
                    )
                }
            }
        },
    )
}

@Sampled
@Composable
fun BasicTextFieldTrackedRangeTextRangeSetterSample() {
    // Wipe the bold style on a given range using the TrackedRange.textRange API
    val state = TextFieldState("Hello World")

    state.edit {
        // Assume we want to "wipe" all bold styles from the first 5 characters.
        val rangeToWipe = TextRange(0, 5)

        // Get all span styles that intersect with the wipe range.
        getSpanStyles(rangeToWipe.start, rangeToWipe.end).forEach { trackedRange ->
            if (trackedRange.spanStyle.fontWeight == FontWeight.Bold) {
                val current = trackedRange.textRange

                if (rangeToWipe.start <= current.start && current.end <= rangeToWipe.end) {
                    // Case 1: The bold style is entirely within the wipe range, remove it.
                    removeStyle(trackedRange)
                } else if (current.start < rangeToWipe.start && rangeToWipe.end < current.end) {
                    // Case 2: The wipe range is in the middle: split the style into two parts.
                    val oldEnd = current.end
                    // Truncate the original style to end at the start of the wipe range.
                    trackedRange.textRange = TextRange(current.start, rangeToWipe.start)
                    // Add a new bold style starting after the wipe range.
                    addStyle(trackedRange.spanStyle, rangeToWipe.end, oldEnd)
                } else if (current.start < rangeToWipe.start) {
                    // Case 3: Overlap at the start of wipe: truncate the style's end.
                    trackedRange.textRange = TextRange(current.start, rangeToWipe.start)
                } else {
                    // Case 4: Overlap at the end of wipe: truncate the style's start.
                    trackedRange.textRange = TextRange(rangeToWipe.end, current.end)
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Suppress("RedundantSuspendModifier")
@Sampled
fun BasicTextFieldTextValuesSample() {
    class SearchViewModel {
        val searchFieldState = TextFieldState()
        var searchResults: List<String> by mutableStateOf(emptyList())
            private set

        /** Called while the view model is active, e.g. from a LaunchedEffect. */
        suspend fun run() {
            snapshotFlow { searchFieldState.text }
                // Let fast typers get multiple keystrokes in before kicking off a search.
                .debounce(500)
                // collectLatest cancels the previous search if it's still running when there's a
                // new change.
                .collectLatest { queryText -> searchResults = performSearch(query = queryText) }
        }

        private suspend fun performSearch(query: CharSequence): List<String> {
            TODO()
        }
    }

    @Composable
    fun SearchScreen(viewModel: SearchViewModel) {
        Column {
            BasicTextField(viewModel.searchFieldState)
            LazyColumn { items(viewModel.searchResults) { TODO() } }
        }
    }
}

@Sampled
@Composable
fun BasicTextFieldUndoSample() {
    val state = rememberTextFieldState()

    Column(Modifier.padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material.Button(
                onClick = { state.undoState.undo() },
                enabled = state.undoState.canUndo,
            ) {
                Text("Undo")
            }

            androidx.compose.material.Button(
                onClick = { state.undoState.redo() },
                enabled = state.undoState.canRedo,
            ) {
                Text("Redo")
            }

            androidx.compose.material.Button(
                onClick = { state.undoState.clearHistory() },
                enabled = state.undoState.canUndo || state.undoState.canRedo,
            ) {
                Text("Clear History")
            }
        }

        BasicTextField(
            state = state,
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                    .padding(8.dp),
            textStyle = TextStyle(fontSize = 16.sp),
        )
    }
}

@Sampled
@Composable
fun BasicTextFieldDecoratorSample() {
    // Demonstrates how to use the decorator API on BasicTextField
    val state = rememberTextFieldState("Hello, World!")
    BasicTextField(
        state = state,
        decorator = { innerTextField ->
            // Because the decorator is used, the whole Row gets the same behaviour as the internal
            // input field would have otherwise. For example, there is no need to add a
            // `Modifier.clickable` to the Row anymore to bring the text field into focus when user
            // taps on a larger text field area which includes paddings and the icon areas.
            Row(
                Modifier.background(Color.LightGray, RoundedCornerShape(percent = 30))
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.MailOutline, contentDescription = "Mail Icon")
                Spacer(Modifier.width(16.dp))
                innerTextField()
            }
        },
    )
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun TextFieldStateApplyOutputTransformation() {
    val state = TextFieldState("Hello, World")
    val outputTransformation = OutputTransformation { insert(0, "> ") }

    val buffer = state.toTextFieldBuffer()
    with(outputTransformation) { buffer.transformOutput() }

    val transformedText = buffer.asCharSequence()
    val transformedSelection = buffer.selection
}

@Sampled
@Composable
fun BasicTextFieldTrackedRangeSample() {
    // This sample demonstrates how to use the `TrackedRange` API to track and modify text ranges
    // dynamically. It implements a basic Markdown-like behavior where text typed inside double
    // asterisks (e.g., **bold**) is automatically bolded, and the asterisks are removed.
    val state = rememberTextFieldState("")

    fun IntRange.toTextRange(): TextRange {
        // Unlike IntRange, TextRange is exclusive at the end.
        return TextRange(first, last + 1)
    }

    val inputTransformation = remember {
        InputTransformation {
            val text = asCharSequence().toString()
            val matches = "\\*\\*([^*]+)\\*\\*".toRegex().findAll(text).toList()
            matches
                .map { match ->
                    val contentRange = match.groups[0]!!.range
                    // Apply bold style to the text inside asterisks (including the
                    // asterisks for now).
                    addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        contentRange.toTextRange(),
                        ExpandPolicy.InsideOnly,
                    )
                }
                .forEach { trackedRange ->
                    // Remove the asterisks here.

                    // `trackedRange` simplifies this logic: normally, deleting characters at
                    // the start would shift the end index. However, because `trackedRange`
                    // automatically tracks text updates and adjusts its offsets
                    // dynamically, we can safely delete the target range without having to
                    // calculate the offset manually.
                    delete(trackedRange.textRange.start, trackedRange.textRange.start + 2)
                    delete(trackedRange.textRange.end - 2, trackedRange.textRange.end)
                }
        }
    }

    Column {
        Text("Type **text** below to automatically bold it.")

        BasicTextField(
            state = state,
            textStyle = LocalTextStyle.current,
            inputTransformation = inputTransformation,
        )
    }
}

@Sampled
@Composable
fun BasicTextFieldTrackedRangeToggleBoldSample() {
    // This sample demonstrates a realistic rich-text editor scenario using the `TrackedRange` and
    // `TextFieldTextStyles` APIs. It implements a "Toggle Bold" formatting function on the current
    // selection.

    // For simplicity, this sample keeps bold styles non-overlapping and contiguous, assuming they
    // are
    // applied exclusively through this method.
    val state = rememberTextFieldState("Hello World")

    // This derived state calculates whether the current selection is completely covered by
    // bold text styles. This ensures the "Bold" toggle button accurately reflects the
    // state of the selected text.
    val isSelection100PercentBold by derivedStateOf {
        val selection = state.selection
        if (selection.collapsed) {
            false
        } else {
            val spanStyles = state.textStyles.getSpanStyles(selection.min, selection.max)
            var boldCoverage = 0
            for (style in spanStyles) {
                if (style.item.fontWeight == FontWeight.Bold) {
                    val overlapStart = maxOf(style.start, selection.min)
                    val overlapEnd = minOf(style.end, selection.max)
                    if (overlapEnd > overlapStart) {
                        boldCoverage += (overlapEnd - overlapStart)
                    }
                }
            }
            boldCoverage == selection.length
        }
    }

    fun TextFieldBuffer.unBoldSelection() {
        // Query existing bold styles in the selection
        val intersectingStyles =
            getSpanStyles(selection.min, selection.max).filter {
                it.spanStyle.fontWeight == FontWeight.Bold
            }
        // We modify or remove existing styles to exclude the selected range
        for (style in intersectingStyles) {
            val range = style.textRange
            if (range.start >= selection.min && range.end <= selection.max) {
                // The style is fully inside the selection. Remove it.
                removeStyle(style)
            } else if (range.start < selection.min && range.end > selection.max) {
                // The style completely covers the selection. We need to split it.
                val oldEnd = range.end
                // Truncate the start part
                style.textRange = TextRange(range.start, selection.min)
                // Add a new style for the end part
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    TextRange(selection.max, oldEnd),
                    ExpandPolicy.AtEnd,
                )
            } else if (range.start < selection.min) {
                // The style overlaps with the start of the selection. Truncate it.
                style.textRange = TextRange(range.start, selection.min)
            } else {
                // The style overlaps with the end of the selection. Truncate it.
                style.textRange = TextRange(selection.max, range.end)
            }
        }
    }

    fun TextFieldBuffer.boldSelection() {
        // Query existing bold styles in the selection
        val intersectingStyles =
            getSpanStyles(selection.min, selection.max).filter {
                it.spanStyle.fontWeight == FontWeight.Bold
            }
        // To keep bold styles non-overlapping, we merge any intersecting bold
        // styles with the new selection range into a single contiguous bold style.
        var mergedStart = selection.min
        var mergedEnd = selection.max

        for (style in intersectingStyles) {
            mergedStart = minOf(mergedStart, style.textRange.start)
            mergedEnd = maxOf(mergedEnd, style.textRange.end)
            // Remove the fragmented style
            removeStyle(style)
        }

        addStyle(
            SpanStyle(fontWeight = FontWeight.Bold),
            TextRange(mergedStart, mergedEnd),
            ExpandPolicy.AtEnd,
        )
    }

    Column {
        Button(
            onClick = {
                state.edit {
                    val selection = this.selection
                    if (selection.collapsed) return@edit
                    if (isSelection100PercentBold) {
                        unBoldSelection()
                    } else {
                        boldSelection()
                    }
                }
            }
        ) {
            Text(
                "B",
                fontWeight = if (isSelection100PercentBold) FontWeight.Bold else FontWeight.Normal,
            )
        }

        BasicTextField(state = state, textStyle = LocalTextStyle.current)
    }
}

@Sampled
@Composable
fun BasicTextFieldTrackedRangePropertiesSample() {
    // This sample demonstrates the use of [TrackedRange.valid] and [TrackedRange.expandPolicy]. It
    // shows how mutating the text can cause a TrackedRange to become invalid, and how to
    // dynamically
    // update the behavior of a range.
    val state = TextFieldState("Hello World")

    state.edit {
        // Query the existing styles on the text
        val existingStyles = getSpanStyles(0, length)

        existingStyles.forEach { trackedRange ->
            // Read and update the expand policy of a style
            if (trackedRange.expandPolicy == ExpandPolicy.InsideOnly) {
                trackedRange.expandPolicy = ExpandPolicy.AtEnd
            }
        }

        // Do some edits that might delete the styled text
        delete(0, 5)

        // After the edits, we can check if the previously queried ranges are still valid
        existingStyles.forEach { trackedRange ->
            // The style's range might have collapsed to zero length, making it no longer valid.
            // It is recommended to check validity before accessing properties like textRange.
            if (trackedRange.valid) {
                // Style is still valid, it's up-to-date range can be accessed via
                // trackedRange.textRange
            } else {
                // Style was completely deleted.
            }
        }
    }
}
