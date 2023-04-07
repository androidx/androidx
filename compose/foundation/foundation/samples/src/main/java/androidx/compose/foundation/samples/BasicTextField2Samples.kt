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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.forEachChange
import androidx.compose.foundation.text2.input.forEachChangeReversed
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.foundation.text2.input.selectCharsIn
import androidx.compose.foundation.text2.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.substring

@Sampled
fun BasicTextField2StateEditSample() {
    val state = TextFieldState("hello world")
    state.edit {
        // Insert a comma after "hello".
        replace(5, 5, ",") // = "hello, world"

        // Delete "world".
        replace(7, 12, "") // = "hello, "

        // Add a different name.
        append("Compose") // = "hello, Compose"

        // Select the new name so the user can change it by just starting to type.
        selectCharsIn(TextRange(7, 14)) // "hello, ̲C̲o̲m̲p̲o̲s̲e"
    }
}

@Sampled
@Composable
fun BasicTextField2CustomFilterSample() {
    val state = remember { TextFieldState() }
    BasicTextField2(state, filter = { old, new ->
        // If the old text was wrapped in parentheses, keep the text wrapped and preserve the
        // cursor position or selection.
        if (old.startsWith('(') && old.endsWith(')')) {
            val selection = new.selectionInChars
            if (!new.endsWith(')')) {
                new.append(')')
                new.selectCharsIn(TextRange(selection.start, selection.end))
            }
            if (!new.startsWith('(')) {
                new.replace(0, 0, "(")
                new.selectCharsIn(TextRange(selection.start + 1, selection.end + 1))
            }
        }
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

@Composable
@Sampled
fun BasicTextField2ChangeIterationSample() {
    // Print a log message every time the text is changed.
    BasicTextField2(state = rememberTextFieldState(), filter = { _, new ->
        new.changes.forEachChange { sourceRange, replacedLength ->
            val newString = new.substring(sourceRange)
            println("""$replacedLength characters were replaced with "$newString"""")
        }
    })
}

@Composable
@Sampled
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