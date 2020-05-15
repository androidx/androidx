/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.tooling

import androidx.compose.Composable
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.material.Button
import androidx.ui.material.Surface
import androidx.ui.tooling.preview.Preview

/**
 * Method used to verify the Composable -> Source Code mapping.
 * If the line numbers in this file change, the ComposeViewAdapter#lineNumberMapping test will
 * break!!!
 */
@Preview
@Composable
fun LineNumberPreview() {
    Surface(color = Color.Red) {
        Column {
            Text("Hello world1")
            repeat(3) { // This line does not generate source code information in the tree
                Text("Hello world2")
            }

            Row {
                Button(onClick = {}) {}
                Button(onClick = {}) {}
            }
        }
    }
}