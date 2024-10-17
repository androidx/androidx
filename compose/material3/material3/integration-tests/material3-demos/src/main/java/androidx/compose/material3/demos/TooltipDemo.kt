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

package androidx.compose.material3.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipDemo() {
    val listData = remember { mutableStateListOf<ItemInfo>() }

    Column(
        modifier = Modifier.padding(horizontal = 1.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Add items to the list")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var textFieldValue by remember { mutableStateOf("") }
            var textFieldTooltipText by remember { mutableStateOf("") }
            val textFieldTooltipState = rememberTooltipState()
            val scope = rememberCoroutineScope()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(textFieldTooltipText) } },
                state = textFieldTooltipState
            ) {
                OutlinedTextField(
                    value = textFieldValue,
                    placeholder = { Text("Item Name") },
                    onValueChange = { newVal -> textFieldValue = newVal }
                )
            }

            OutlinedButton(
                onClick = {
                    if (textFieldValue.isBlank()) {
                        textFieldTooltipText = "Please give the item a name!"
                        scope.launch { textFieldTooltipState.show() }
                    } else {
                        val listItem = ItemInfo(textFieldValue, TooltipState())
                        listData.add(listItem)
                        textFieldValue = ""
                        scope.launch { listItem.addedTooltipState.show() }
                    }
                }
            ) {
                Text("Add")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(listData) { item ->
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("${item.itemName} added to list") } },
                    state = item.addedTooltipState
                ) {
                    ListItemCard(itemName = item.itemName, onDelete = { listData.remove(item) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItemCard(itemName: String, onDelete: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxSize()) {
        ListItem(
            headlineContent = { Text(itemName) },
            trailingContent = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Delete $itemName") } },
                    state = rememberTooltipState(),
                    enableUserInput = true
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete $itemName from list."
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class ItemInfo(val itemName: String, val addedTooltipState: TooltipState)
