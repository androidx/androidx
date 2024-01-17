/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun FocusTextFieldImmediatelyDemo() {
    var value by remember { mutableStateOf("") }
    var launchedEffect by remember { mutableStateOf(false) }

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Disposable effect")
            Switch(checked = launchedEffect, onCheckedChange = { launchedEffect = it })
            Text("Launched effect")
        }

        if (launchedEffect) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }

            TextField(
                value,
                onValueChange = { value = it },
                modifier = Modifier
                    .wrapContentSize()
                    .focusRequester(focusRequester)
            )
        } else {
            val focusRequester = remember { FocusRequester() }
            DisposableEffect(focusRequester) {
                focusRequester.requestFocus()
                onDispose {}
            }

            TextField(
                value,
                onValueChange = { value = it },
                modifier = Modifier
                    .wrapContentSize()
                    .focusRequester(focusRequester)
            )
        }
    }
}
