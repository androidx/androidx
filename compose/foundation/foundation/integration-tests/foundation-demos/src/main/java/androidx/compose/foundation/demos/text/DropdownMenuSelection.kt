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

package androidx.compose.foundation.demos.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun DropdownMenuSelection() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            buildAnnotatedString {
                append("The DropdownMenu has a ")
                appendCode("SelectionContainer")
                append(" wrapping it. That makes the ")
                appendCode("Text")
                append("s making up the menu items selectable, but the ")
                appendCode("Text")
                append("s are in a popup.")
                append(" Attempting to select these texts should not crash the app.")
            },
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            DropdownMenuInSelectionContainer()
        }
        Text(
            buildAnnotatedString {
                append("The DropdownMenu has a ")
                appendCode("SelectionContainer")
                append(" in it. The ")
                appendCode("Text")
                append("s are selectable, but because the ")
                appendCode("SelectionContainer")
                append(" is in the popup, it shouldn't crash on long press.")
                append(" However, the selection features (handles, magnifier, toolbar) should")
                append(" appear in the correct places, attached to the selection.")
            },
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            SelectionContainerInDropdownMenu()
        }
    }
}

@Composable
private fun DropdownMenuInSelectionContainer() {
    var expanded by remember { mutableStateOf(false) }
    Button(onClick = { expanded = true }) { Text("DropdownMenu in SelectionContainer") }
    SelectionContainer {
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            repeat(4) { DropdownMenuItem(onClick = { expanded = false }) { Text("Item $it") } }
        }
    }
}

@Composable
private fun SelectionContainerInDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }
    Button(onClick = { expanded = true }) { Text("SelectionContainer in DropdownMenu") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SelectionContainer {
            Column {
                repeat(4) { DropdownMenuItem(onClick = { expanded = false }) { Text("Item $it") } }
            }
        }
    }
}
