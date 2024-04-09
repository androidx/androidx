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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.samples.BasicTextFieldChangeIterationSample
import androidx.compose.foundation.samples.BasicTextFieldChangeReverseIterationSample
import androidx.compose.foundation.samples.BasicTextFieldCustomInputTransformationSample
import androidx.compose.foundation.samples.BasicTextFieldInputTransformationByValueChooseSample
import androidx.compose.foundation.samples.BasicTextFieldInputTransformationByValueReplaceSample
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.allCaps
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.core.text.isDigitsOnly

@Composable
fun BasicTextFieldFilterDemos() {
    Column(
        Modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        TagLine(tag = "allCaps")
        FilterDemo(filter = InputTransformation.allCaps(Locale.current))

        TagLine(tag = "maxLength(5)")
        FilterDemo(filter = InputTransformation.maxLength(5))

        TagLine(tag = "Digits Only BasicTextField")
        DigitsOnlyDemo()

        TagLine(tag = "Change filter")
        ChangeFilterDemo()

        TagLine(tag = "Custom (type backwards with prompt)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextFieldCustomInputTransformationSample()
        }

        TagLine(tag = "Custom (string,string->string with replacement)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextFieldInputTransformationByValueReplaceSample()
        }

        TagLine(tag = "Custom (string,string->string with choice)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextFieldInputTransformationByValueChooseSample()
        }

        TagLine(tag = "Change tracking (change logging sample)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextFieldChangeIterationSample()
        }

        TagLine(tag = "Change tracking (insert mode sample)")
        Box(demoTextFieldModifiers, propagateMinConstraints = true) {
            BasicTextFieldChangeReverseIterationSample()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DigitsOnlyDemo() {
    FilterDemo(filter = object : InputTransformation {
        override val keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )

        override fun TextFieldBuffer.transformInput() {
            if (!asCharSequence().isDigitsOnly()) {
                revertAllChanges()
            }
        }
    })
}

@Composable
private fun FilterDemo(filter: InputTransformation) {
    val state = remember { TextFieldState() }
    BasicTextField(
        state = state,
        inputTransformation = filter,
        modifier = demoTextFieldModifiers
    )
}

@Composable
private fun ChangeFilterDemo() {
    var filter: InputTransformation? by remember { mutableStateOf(null) }
    val state = remember { TextFieldState() }

    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Filter enabled?")
            Switch(checked = filter != null, onCheckedChange = {
                filter = if (filter == null) InputTransformation.allCaps(Locale.current) else null
            })
        }
        BasicTextField(
            state = state,
            inputTransformation = filter,
            modifier = demoTextFieldModifiers
        )
    }
}
