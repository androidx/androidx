/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.ProcessTextKey
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

private data object ClearKeyDataObject

@Sampled
@Composable
fun AppendComponentsToTextContextMenu() {
    val textFieldState = rememberTextFieldState()
    BasicTextField(
        state = textFieldState,
        modifier =
            Modifier.appendTextContextMenuComponents {
                separator()
                item(key = ClearKeyDataObject, label = "Clear") {
                    textFieldState.clearText()
                    close()
                }
                separator()
            },
    )
}

@Sampled
@Composable
fun AddFilterToTextContextMenu() {
    val textFieldState = rememberTextFieldState()
    BasicTextField(
        state = textFieldState,
        modifier =
            Modifier.filterTextContextMenuComponents(
                filter = { component -> component.key === ClearKeyDataObject }
            ),
    )
}

@Sampled
@Composable
fun AppendItemToTextContextMenuAndroid() {
    val textFieldState = rememberTextFieldState()
    val label = stringResource(R.string.context_menu_clear)
    BasicTextField(
        state = textFieldState,
        modifier =
            Modifier.appendTextContextMenuComponents {
                separator()
                item(
                    key = ClearKeyDataObject,
                    label = label,
                    leadingIcon = R.drawable.ic_sample_vector,
                ) {
                    textFieldState.clearText()
                    close()
                }
                separator()
            },
    )
}

@Sampled
@Composable
fun FilterProcessTextItemsInTextContextMenu() {
    val textFieldState = rememberTextFieldState()
    BasicTextField(
        state = textFieldState,
        modifier =
            Modifier.filterTextContextMenuComponents(
                filter = { component -> component.key is ProcessTextKey }
            ),
    )
}
