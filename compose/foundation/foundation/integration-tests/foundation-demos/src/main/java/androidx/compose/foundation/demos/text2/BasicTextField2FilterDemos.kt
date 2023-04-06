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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.samples.BasicTextField2ChangeIterationSample
import androidx.compose.foundation.samples.BasicTextField2ChangeReverseIterationSample
import androidx.compose.foundation.samples.BasicTextField2CustomFilterSample
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.allCaps
import androidx.compose.foundation.text2.input.maxLengthInChars
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.core.text.isDigitsOnly

@Composable
fun BasicTextField2FilterDemos() {
    Column(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine(tag = "allCaps")
        FilterDemo(filter = TextEditFilter.allCaps(Locale.current))

        TagLine(tag = "maxLength(5)")
        FilterDemo(filter = TextEditFilter.maxLengthInChars(5))

        TagLine(tag = "Digits Only BasicTextField2")
        DigitsOnlyBasicTextField2()

        TagLine(tag = "Custom (wrap the text in parentheses)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextField2CustomFilterSample()
        }

        TagLine(tag = "Change tracking (change logging sample)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextField2ChangeIterationSample()
        }

        TagLine(tag = "Change tracking (insert mode sample)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextField2ChangeReverseIterationSample()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DigitsOnlyBasicTextField2() {
    FilterDemo(filter = { _, new ->
        if (!new.isDigitsOnly()) {
            new.revertAllChanges()
        }
    })
}

@Composable
private fun FilterDemo(filter: TextEditFilter) {
    val state = remember { TextFieldState() }
    BasicTextField2(
        state = state,
        filter = filter,
        modifier = demoTextFieldModifiers
    )
}