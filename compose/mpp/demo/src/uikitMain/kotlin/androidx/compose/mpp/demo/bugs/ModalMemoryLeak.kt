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

package androidx.compose.mpp.demo.bugs

import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import platform.posix.free
import platform.posix.malloc

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
val ModalMemoryLeak = Screen.Example("ModalMemoryLeak") {
    MaterialTheme {
        var openBottomSheet by remember { mutableStateOf(false) }
        Button(onClick = {
            openBottomSheet = true
        }) {
            Text("ModalBottomSheet3")
        }

        if (openBottomSheet) {
            val sheetState = rememberModalBottomSheetState()

            ModalBottomSheet(
                onDismissRequest = {
                    openBottomSheet = false
                },
                sheetState = sheetState,
            ) {
                DisposableEffect(Unit) {
                    val a = malloc((20_000 * 1024).toULong())

                    onDispose {
                        println("Disposed")
                        free(a)
                    }
                }
                Text("ModalBottomSheet3", Modifier.height(500.dp))
            }
        }
    }
}