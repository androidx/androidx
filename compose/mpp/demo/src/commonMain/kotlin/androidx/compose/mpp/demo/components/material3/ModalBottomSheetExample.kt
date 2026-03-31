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

package androidx.compose.mpp.demo.components.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal val ModalBottomSheetExample = Screen.Selection(
    "ModalBottomSheet",
    Screen.Example("ModalBottomSheet3") { ModalBottomSheet3Example() },
    Screen.Example("ModalBottomSheet+IME") { ModalBottomSheetImeExample() }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheet3Example() {
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()
    Button(onClick = { openBottomSheet = true }) {
        Text(text = "ModalBottomSheet3")
    }
    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Button(onClick = {
                scope.launch {
                    bottomSheetState.hide()
                    openBottomSheet = false
                }
            }) {
                Text("Hide")
            }
            LazyColumn {
                items(30) {
                    ListItem({ Text("Item $it") })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheetImeExample() {
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    Button(onClick = { openBottomSheet = true }) {
        Text(text = "ModalBottomSheet+IME")
    }
    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { openBottomSheet = false },
            contentWindowInsets = { BottomSheetDefaults.modalWindowInsets },
            content = {
                Column(modifier = Modifier.height(300.dp).fillMaxWidth()) {
                    Column(Modifier.weight(1f).padding(16.dp)) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(modifier = Modifier
                        .height(30.dp)
                        .fillMaxWidth()
                        .background(color = Color.Yellow)
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Pinned above safe drawing inset"
                        )
                    }
                }
            }
        )
    }
}
