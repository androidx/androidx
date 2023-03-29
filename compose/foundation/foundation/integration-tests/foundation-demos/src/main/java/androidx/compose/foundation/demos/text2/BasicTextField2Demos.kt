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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.text.isDigitsOnly

@Composable
fun BasicTextField2Demos() {
    Column(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine(tag = "Plain BasicTextField2")
        PlainBasicTextField2()

        TagLine(tag = "Single Line BasicTextField2")
        SingleLineBasicTextField2()

        TagLine(tag = "State toggling BasicTextField2")
        StateTogglingBasicTextField2()

        TagLine(tag = "Digits Only BasicTextField2")
        DigitsOnlyBasicTextField2()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlainBasicTextField2() {
    val state = remember { TextFieldState() }
    BasicTextField2(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineBasicTextField2() {
    val state = remember { TextFieldState() }
    BasicTextField2(
        state = state,
        modifier = demoTextFieldModifiers,
        textStyle = LocalTextStyle.current,
        maxLines = 1
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StateTogglingBasicTextField2() {
    var counter by remember { mutableStateOf(0) }
    val states = remember { listOf(TextFieldState(), TextFieldState()) }
    val state = states[counter]
    Text("Click to toggle state: $counter", modifier = Modifier.clickable {
        counter++
        counter %= 2
    })

    BasicTextField2(state, demoTextFieldModifiers, textStyle = LocalTextStyle.current)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DigitsOnlyBasicTextField2() {
    val state = remember { TextFieldState() }
    BasicTextField2(
        state = state,
        filter = { old, new ->
            if (new.text.isDigitsOnly()) new else old
        },
        modifier = demoTextFieldModifiers,
        textStyle = LocalTextStyle.current
    )
}
