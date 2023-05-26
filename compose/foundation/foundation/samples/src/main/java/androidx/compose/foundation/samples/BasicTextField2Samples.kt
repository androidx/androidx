/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)
@file:Suppress("UNUSED_PARAMETER", "unused", "LocalVariableName", "RedundantSuspendModifier")

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.forEachChange
import androidx.compose.foundation.text2.input.forEachChangeReversed
import androidx.compose.foundation.text2.input.forEachTextValue
import androidx.compose.foundation.text2.input.insert
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.foundation.text2.input.selectCharsIn
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text2.input.textAsFlow
import androidx.compose.foundation.text2.input.then
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.substring
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@Sampled
fun BasicTextField2StateCompleteSample() {
    class SearchViewModel(
        val searchFieldState: TextFieldState = TextFieldState()
    ) {
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
            searchFieldState.forEachTextValue { queryText ->
                // Start a new search every time the user types something valid. If the previous
                // search is still being processed when the text is changed, it will be cancelled
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
                BasicTextField2(viewModel.searchFieldState)
                IconButton(onClick = { viewModel.clearQuery() }) {
                    Icon(Icons.Default.Clear, contentDescription = "clear search query")
                }
            }
            if (!viewModel.isQueryValid) {
                Text("Invalid query", style = TextStyle(color = Color.Red))
            }
            LazyColumn {
                items(viewModel.searchResults) {
                    TODO()
                }
            }
        }
    }
}

@Sampled
fun BasicTextField2TextDerivedStateSample() {
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
            BasicTextField2(viewModel.inputState)
            if (!viewModel.isInputValid) {
                Text("Input is invalid.", style = TextStyle(color = Color.Red))
            }
        }
    }
}

@Sampled
fun BasicTextField2StateEditSample() {
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
        selectCharsIn(TextRange(9, 16)) // "goodbye, ̲C̲o̲m̲p̲o̲s̲e"
    }
}

@Sampled
@Composable
fun BasicTextField2CustomFilterSample() {
    val state = remember { TextFieldState() }
    BasicTextField2(state, filter = { _, new ->
        // A filter that always places newly-input text at the start of the string, after a
        // prompt character, like a shell.
        val promptChar = '>'

        fun CharSequence.countPrefix(char: Char): Int {
            var i = 0
            while (i < length && get(i) == char) i++
            return i
        }

        // Step one: Figure out the insertion point.
        val newPromptChars = new.countPrefix(promptChar)
        val insertionPoint = if (newPromptChars == 0) 0 else 1

        // Step two: Ensure text is placed at the insertion point.
        if (new.changes.changeCount == 1) {
            val insertedRange = new.changes.getRange(0)
            val replacedRange = new.changes.getOriginalRange(0)
            if (!replacedRange.collapsed && insertedRange.collapsed) {
                // Text was deleted, delete forwards from insertion point.
                new.delete(insertionPoint, insertionPoint + replacedRange.length)
            }
        }
        // Else text was replaced or there were multiple changes - don't handle.

        // Step three: Ensure the prompt character is there.
        if (newPromptChars == 0) {
            new.insert(0, ">")
        }

        // Step four: Ensure the cursor is ready for the next input.
        new.placeCursorAfterCharAt(0)
    })
}

@Sampled
fun BasicTextField2FilterChainingSample() {
    val removeFirstEFilter = TextEditFilter { _, new ->
        val index = new.indexOf('e')
        if (index != -1) {
            new.replace(index, index + 1, "")
        }
    }
    val printECountFilter = TextEditFilter { _, new ->
        println("found ${new.count { it == 'e' }} 'e's in the string")
    }

    // Returns a filter that always prints 0 e's.
    removeFirstEFilter.then(printECountFilter)

    // Returns a filter that prints the number of e's before the first one is removed.
    printECountFilter.then(removeFirstEFilter)
}

@Sampled
@Composable
fun BasicTextField2ChangeIterationSample() {
    // Print a log message every time the text is changed.
    BasicTextField2(state = rememberTextFieldState(), filter = { _, new ->
        new.changes.forEachChange { sourceRange, replacedLength ->
            val newString = new.substring(sourceRange)
            println("""$replacedLength characters were replaced with "$newString"""")
        }
    })
}

@Sampled
@Composable
fun BasicTextField2ChangeReverseIterationSample() {
    // Make a text field behave in "insert mode" – inserted text overwrites the text ahead of it
    // instead of being inserted.
    BasicTextField2(state = rememberTextFieldState(), filter = { _, new ->
        new.changes.forEachChangeReversed { range, originalRange ->
            if (!range.collapsed && originalRange.collapsed) {
                // New text was inserted, delete the text ahead of it.
                new.delete(
                    range.end.coerceAtMost(new.length),
                    (range.end + range.length).coerceAtMost(new.length)
                )
            }
        }
    })
}

@Sampled
fun BasicTextField2ForEachTextValueSample() {
    class SearchViewModel {
        val searchFieldState = TextFieldState()
        var searchResults: List<String> by mutableStateOf(emptyList())
            private set

        /** Called while the view model is active, e.g. from a LaunchedEffect. */
        suspend fun run() {
            searchFieldState.forEachTextValue { queryText ->
                // Start a new search every time the user types something. If the previous search
                // is still being processed when the text is changed, it will be cancelled and this
                // code will run again with the latest query text.
                searchResults = performSearch(query = queryText)
            }
        }

        private suspend fun performSearch(query: CharSequence): List<String> {
            TODO()
        }
    }

    @Composable
    fun SearchScreen(viewModel: SearchViewModel) {
        Column {
            BasicTextField2(viewModel.searchFieldState)
            LazyColumn {
                items(viewModel.searchResults) {
                    TODO()
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Suppress("RedundantSuspendModifier")
@Sampled
fun BasicTextField2TextValuesSample() {
    class SearchViewModel {
        val searchFieldState = TextFieldState()
        var searchResults: List<String> by mutableStateOf(emptyList())
            private set

        /** Called while the view model is active, e.g. from a LaunchedEffect. */
        suspend fun run() {
            searchFieldState.textAsFlow()
                // Let fast typers get multiple keystrokes in before kicking off a search.
                .debounce(500)
                // collectLatest cancels the previous search if it's still running when there's a
                // new change.
                .collectLatest { queryText ->
                    searchResults = performSearch(query = queryText)
                }
        }

        private suspend fun performSearch(query: CharSequence): List<String> {
            TODO()
        }
    }

    @Composable
    fun SearchScreen(viewModel: SearchViewModel) {
        Column {
            BasicTextField2(viewModel.searchFieldState)
            LazyColumn {
                items(viewModel.searchResults) {
                    TODO()
                }
            }
        }
    }
}