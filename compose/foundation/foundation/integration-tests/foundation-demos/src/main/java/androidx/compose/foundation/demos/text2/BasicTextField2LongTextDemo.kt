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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.demos.text.loremIpsumWords
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicTextField2LongTextDemo() {
    val text = remember { TextFieldState(generateString(charCount = 100_000)) }

    Column(Modifier.imePadding()) {
        BasicTextField2(
            state = text,
            modifier = Modifier
                .weight(1f)
                .then(demoTextFieldModifiers),
            lineLimits = MultiLine(maxHeightInLines = 20)
        )
        Text("Char count: ${text.text.length}")
    }
}

private fun generateString(charCount: Int): String = buildString(capacity = charCount) {
    val wordIterator = loremIpsumWords().iterator()
    while (length < charCount) {
        append(wordIterator.next().take(charCount - length - 1))
        append(' ')
    }
}
