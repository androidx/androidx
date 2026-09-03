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

package androidx.compose.ui.demos.autofill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.unit.dp

@Composable
fun ExplicitAutofillTypesDemo() {
    val nameState = rememberTextFieldState()
    val emailState = rememberTextFieldState()

    Column {
        Autofill(
            autofillTypes =
                listOf(
                    @Suppress("Deprecation")
                    androidx.compose.ui.autofill.AutofillType.PersonFullName
                ),
            onFill = { nameState.setTextAndPlaceCursorAtEnd(it) },
        ) {
            OutlinedTextField(state = nameState, label = { Text("Name") })
        }

        Spacer(Modifier.height(10.dp))

        Autofill(
            autofillTypes =
                listOf(
                    @Suppress("Deprecation") androidx.compose.ui.autofill.AutofillType.EmailAddress
                ),
            onFill = { emailState.setTextAndPlaceCursorAtEnd(it) },
        ) {
            OutlinedTextField(state = emailState, label = { Text("Email") })
        }
    }
}

@Composable
private fun Autofill(
    autofillTypes: List<@Suppress("Deprecation") androidx.compose.ui.autofill.AutofillType>,
    onFill: ((String) -> Unit),
    content: @Composable BoxScope.() -> Unit,
) {
    val autofill = @Suppress("Deprecation") LocalAutofill.current
    val autofillTree = @Suppress("Deprecation") LocalAutofillTree.current
    val autofillNode =
        remember(autofillTypes, onFill) {
            @Suppress("Deprecation")
            androidx.compose.ui.autofill.AutofillNode(
                onFill = onFill,
                autofillTypes = autofillTypes,
            )
        }

    Box(
        modifier =
            Modifier.onFocusChanged {
                    if (it.isFocused) {
                        autofill?.requestAutofillForNode(autofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(autofillNode)
                    }
                }
                .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() },
        content = content,
    )

    DisposableEffect(autofillNode) {
        autofillTree.children[autofillNode.id] = autofillNode
        onDispose { autofillTree.children.remove(autofillNode.id) }
    }
}
