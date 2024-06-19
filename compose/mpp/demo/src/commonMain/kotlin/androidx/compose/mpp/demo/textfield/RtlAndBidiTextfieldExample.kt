/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo.textfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RtlAndBidiTextfieldExample() {
    val ltrTextValue = remember { mutableStateOf("Text example") }
    val rtlTextValue = remember { mutableStateOf("مثال نصي") }
    val bidiTextValue = remember { mutableStateOf("Bidi text example مثال نص بيدي") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            BasicText("OutlinedTextField, LTR Text")
            OutlinedTextField(
                value = ltrTextValue.value,
                onValueChange = { ltrTextValue.value = it })
        }
        Column {
            BasicText("OutlinedTextField, RTL Text")
            OutlinedTextField(
                value = rtlTextValue.value,
                onValueChange = { ltrTextValue.value = it })
        }
        Column {
            BasicText("OutlinedTextField, BiDi Text")
            OutlinedTextField(
                value = bidiTextValue.value,
                onValueChange = { ltrTextValue.value = it })
        }
    }
}