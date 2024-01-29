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

package androidx.compose.mpp.demo.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionExample() {
    SelectionContainer(
        Modifier.padding(24.dp).fillMaxWidth()
    ) {
        Column {
            Text(
                "I'm a selection container. Double tap on word to select a word." +
                    " Triple tap on content to select whole paragraph.\nAnother paragraph for testing.\n" +
                    "And another one."
            )
            Text("I'm another Text() block. Let's try to select me!")
            Text("I'm yet another Text() with multiparagraph structure block.\nLet's try to select me!")
        }
    }
}