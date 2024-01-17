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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.demos.text.fontSize8
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material.Checkbox
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Preview
@Composable
fun KeyboardActionsDemos() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    Box(Modifier.imePadding()) {
        var executeDefaultActions by remember { mutableStateOf(true) }
        val onImeAction: KeyboardActionScope.(ImeAction) -> Unit = remember(executeDefaultActions) {
            {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("ImeAction $it is executed")
                }
                if (executeDefaultActions) {
                    defaultKeyboardAction(it)
                }
            }
        }
        val keyboardActions = remember(onImeAction) {
            KeyboardActions(
                onDone = { onImeAction(ImeAction.Done) },
                onGo = { onImeAction(ImeAction.Go) },
                onSearch = { onImeAction(ImeAction.Search) },
                onSend = { onImeAction(ImeAction.Send) },
                onPrevious = { onImeAction(ImeAction.Previous) },
                onNext = { onImeAction(ImeAction.Next) },
            )
        }
        LazyColumn {
            item {
                Row(Modifier.padding(8.dp)) {
                    Text("Execute default actions")
                    Checkbox(
                        checked = executeDefaultActions,
                        onCheckedChange = { executeDefaultActions = it }
                    )
                }
            }
            imeActions.forEach {
                item {
                    KeyboardActionDemoItem(
                        imeAction = it,
                        keyboardActions = keyboardActions,
                        singleLine = true
                    )
                }

                item {
                    KeyboardActionDemoItem(
                        imeAction = it,
                        keyboardActions = keyboardActions,
                        singleLine = false
                    )
                }
            }
        }
        SnackbarHost(snackbarHostState) {
            Snackbar(it, backgroundColor = SnackbarDefaults.backgroundColor.copy(alpha = 1f))
        }
    }
}

@Suppress("PrimitiveInCollection")
private val imeActions = listOf(
    ImeAction.Default,
    ImeAction.None,
    ImeAction.Go,
    ImeAction.Search,
    ImeAction.Send,
    ImeAction.Previous,
    ImeAction.Next,
    ImeAction.Done
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyboardActionDemoItem(
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    singleLine: Boolean
) {
    TagLine(tag = "Ime Action: $imeAction, singleLine: $singleLine")
    val state = remember { TextFieldState() }
    BasicTextField2(
        modifier = demoTextFieldModifiers,
        state = state,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction
        ),
        lineLimits = if (singleLine) {
            TextFieldLineLimits.SingleLine
        } else {
            TextFieldLineLimits.Default
        },
        keyboardActions = keyboardActions,
        textStyle = TextStyle(fontSize = fontSize8),
    )
}
