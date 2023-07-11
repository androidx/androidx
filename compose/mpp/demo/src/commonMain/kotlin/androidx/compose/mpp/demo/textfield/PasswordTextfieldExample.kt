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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Face
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasswordTextfieldExample() {
    val digitPasswordIsVisible = remember { mutableStateOf(false) }
    val passwordIsVisible = remember { mutableStateOf(false) }

    LazyColumn {
        item {
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
                val textState = remember {
                    mutableStateOf("")
                }
                BasicText(
                    "Try password input",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                OutlinedTextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordIsVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            passwordIsVisible.value = !passwordIsVisible.value
                        }) {
                            Icon(
                                imageVector = if (passwordIsVisible.value) Icons.Outlined.Face else Icons.Filled.Lock,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    })
            }
        }
        item {
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
                val textState = remember {
                    mutableStateOf("")
                }
                BasicText(
                    "Try digit password input",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
                OutlinedTextField(value = textState.value,
                    onValueChange = { textState.value = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (digitPasswordIsVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = {
                            digitPasswordIsVisible.value = !digitPasswordIsVisible.value
                        }) {
                            Icon(
                                imageVector = if (digitPasswordIsVisible.value) Icons.Outlined.Face else Icons.Filled.Lock,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    })
            }
        }
    }
}