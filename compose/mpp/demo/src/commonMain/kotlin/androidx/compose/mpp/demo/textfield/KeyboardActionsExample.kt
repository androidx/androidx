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

package androidx.compose.mpp.demo.textfield

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyboardActionsExample() {
    val localKeyboardController = LocalSoftwareKeyboardController.current

    val definedKeyboardActions = KeyboardActions(
        onDone = {
            println("onDone callback")
            localKeyboardController?.hide()
        },
        onGo = {
            println("onGo callback")
            localKeyboardController?.hide()
        },
        onNext = {
            println("onNext callback")
            localKeyboardController?.hide()
        },
        onPrevious = {
            println("onPrevious callback")
            localKeyboardController?.hide()
        },
        onSearch = {
            println("onSearch callback")
            localKeyboardController?.hide()
        },
        onSend = {
            println("onSend callback")
            localKeyboardController?.hide()
        }
    )

    LazyColumn() {
        item {
            TextBlock(
                imeActionName = ImeAction.Default,
                KeyboardActions(onAny = { localKeyboardController?.hide() })
            )
        }
        item { TextBlock(imeActionName = ImeAction.Done, keyboardActions = definedKeyboardActions) }
        item { TextBlock(imeActionName = ImeAction.Go, keyboardActions = definedKeyboardActions) }
        item { TextBlock(imeActionName = ImeAction.Next, keyboardActions = definedKeyboardActions) }
        item {
            TextBlock(
                imeActionName = ImeAction.Previous,
                keyboardActions = definedKeyboardActions
            )
        }
        item {
            TextBlock(
                imeActionName = ImeAction.Search,
                keyboardActions = definedKeyboardActions
            )
        }
        item { TextBlock(imeActionName = ImeAction.Send, keyboardActions = definedKeyboardActions) }
    }
}

@Composable
private fun TextBlock(
    imeActionName: ImeAction,
    keyboardActions: KeyboardActions = KeyboardActions()
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
        val textState = remember {
            mutableStateOf("")
        }
        BasicText(
            imeActionName.toString(),
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
        )
        OutlinedTextField(
            value = textState.value,
            onValueChange = { textState.value = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
            keyboardOptions = KeyboardOptions(imeAction = imeActionName),
            keyboardActions = keyboardActions
        )
    }
}