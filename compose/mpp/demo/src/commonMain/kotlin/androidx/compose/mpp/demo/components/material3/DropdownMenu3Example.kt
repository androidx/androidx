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

package androidx.compose.mpp.demo.components.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@Composable
internal fun DropdownMenu3Example() {
    Column(Modifier.padding(5.dp)) {

        Spacer(Modifier.height(50.dp))
        Text("DropdownMenu")

        val horizontalScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .background(Color.LightGray)
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(100.dp)
        ) {
            repeat(10) {
                ButtonWithDropdown(it + 5)
            }
        }

        Spacer(Modifier.height(50.dp))
        Text("ExposedDropdownMenu")
        TextFieldWithExposedDropdown()
    }
}

@Composable
internal fun ButtonWithDropdown(elementsCount: Int, width: Dp = 180.dp) {
    Column {
        var expanded by remember { mutableStateOf(false) }
        Button(
            onClick = { expanded = true },
            modifier = Modifier.width(width)
        ) {
            Text("DropdownMenu3")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(width),
            properties = PopupProperties(focusable = false)
        ) {
            repeat(elementsCount) {
                DropdownMenuItem(
                    text = { Text("Item $it") },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TextFieldWithExposedDropdown() {
    val options = List(5) { "Item $it" }
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(options[0]) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text("ExposedDropdownMenuBox3") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
