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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Default
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FocusRestorationDemo() {
    Column {
        Text(
            """
                Use the DPad to move focus among these three rows
                and notice how focus is restored to the previously
                focused item.
            """.trimIndent()
        )
        // Adding a focusRestorer and a focus group.
        Row(Modifier.focusRestorer().focusGroup()) {
            key(1) { Button("1") }
            key(2) { Button("2") }
            key(3) { Button("3") }
            key(4) { Button("4") }
        }
        // Adding a focusRestorer to a component with an existing focus group.
        LazyRow(Modifier.focusRestorer()) {
            item { Button("1") }
            item { Button("2") }
            item { Button("3") }
            item { Button("4") }
        }
        // Using a focusRequester to manually restore focus.
        val focusRequester = remember { FocusRequester() }
        LazyRow(
            Modifier
                .focusRequester(focusRequester)
                .focusProperties {
                    exit = { focusRequester.saveFocusedChild(); Default }
                    enter = { if (focusRequester.restoreFocusedChild()) Cancel else Default }
                }
        ) {
            item { Button("1") }
            item { Button("2") }
            item { Button("3") }
            item { Button("4") }
        }
    }
}
@Composable
private fun Button(text: String) {
    Button(onClick = {}) { Text(text) }
}
