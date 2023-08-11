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

package androidx.compose.mpp.demo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickers() {
    Column(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("DatePicker:")
        Box(Modifier.border(1.dp, Color.Black)) {
            val state = rememberDatePickerState()
            DatePicker(state)
        }

        Text("TimePicker:")
        Box(Modifier.border(1.dp, Color.Black)) {
            val state = rememberTimePickerState()
            TimePicker(state)
        }

        var openDialog by remember { mutableStateOf(false) }
        Button(onClick = { openDialog = true }) {
            Text("DatePickerDialog")
        }
        if (openDialog) {
            val state = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { openDialog = false },
                confirmButton = {
                    Button(onClick = {
                        openDialog = false
                        println("Selected date timestamp: ${state.selectedDateMillis}")
                    }) {
                        Text("Confirm")
                    }
                },
                content = {
                    DatePicker(state)
                }
            )
        }
    }
}
