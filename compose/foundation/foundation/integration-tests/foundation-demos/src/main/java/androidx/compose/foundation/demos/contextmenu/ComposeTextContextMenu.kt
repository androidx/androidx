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

@file:OptIn(ExperimentalMaterialApi::class)

package androidx.compose.foundation.demos.contextmenu

import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun TextContextMenusDemo() {
    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
            verticalArrangement = spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Try opening the context menu in any of the components below.")

            LabeledItem("NOT Selectable Text (No Context Menu)") {
                MyText("This single text is NOT selectable.")
            }

            LabeledItem("Selectable Text - Single") {
                SelectionContainer { MyText("This single text is selectable.") }
            }

            LabeledItem("Selectable Text - Multiple") {
                SelectionContainer {
                    Column(Modifier.outline()) {
                        MyText("These four texts")
                        MyText("in a column")
                        MyText("are all")
                        MyText("selectable together.")
                    }
                }
            }

            LabeledItem("BTF1 Fully Enabled") {
                MyTextFieldOne(initialText = "Basic Text Field One")
            }

            LabeledItem("BTF1 Read-only") {
                MyTextFieldOne(initialText = "Basic Text Field One", readOnly = true)
            }

            LabeledItem("BTF1 Disabled (No Context Menu)") {
                MyTextFieldOne(initialText = "Basic Text Field One", enabled = false)
            }

            LabeledItem("BTF1 Password") {
                MyTextFieldOne(
                    initialText = "Basic Text Field One",
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            LabeledItem("BTF2 Fully Enabled") {
                MyTextFieldTwo(initialText = "Basic Text Field Two")
            }

            LabeledItem("BTF2 Read-only") {
                MyTextFieldTwo(initialText = "Basic Text Field Two", readOnly = true)
            }

            LabeledItem("BTF2 Disabled (No Context Menu)") {
                MyTextFieldTwo(initialText = "Basic Text Field Two", enabled = false)
            }

            LabeledItem("BTF2 Password") {
                val tfs = rememberTextFieldState("Basic Text Field Two")
                val interactionSource = remember { MutableInteractionSource() }
                BasicSecureTextField(
                    state = tfs,
                    textStyle =
                        MaterialTheme.typography.body1.copy(color = LocalContentColor.current),
                    interactionSource = interactionSource,
                    decorator = { innerTextField ->
                        TextFieldDefaults.OutlinedTextFieldDecorationBox(
                            value = tfs.text.toString(),
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = false,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = interactionSource
                        )
                    },
                )
            }

            val textColor = LocalContentColor.current.toArgb()
            LabeledItem("Platform TextView - NOT Selectable\n(No Context Menu)") {
                AndroidView(
                    modifier = Modifier.outline(),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = "Platform NON-selectable text."
                            setTextColor(textColor)
                        }
                    },
                )
            }

            LabeledItem("Platform TextView - Selectable") {
                AndroidView(
                    modifier = Modifier.outline(),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            text = "Platform selectable text."
                            setTextColor(textColor)
                            setTextIsSelectable(true)
                        }
                    },
                )
            }

            LabeledItem("Platform EditText") {
                AndroidView(
                    modifier = Modifier.outline(),
                    factory = { ctx ->
                        EditText(ctx).apply {
                            setText("Platform editable text.")
                            setTextIsSelectable(true)
                            setTextColor(textColor)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MyText(text: String) {
    Text(text = text, modifier = Modifier.outline())
}

@Composable
private fun MyTextFieldOne(
    initialText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var tfv by remember { mutableStateOf(TextFieldValue(initialText)) }
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = tfv,
        onValueChange = { tfv = it },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle =
            MaterialTheme.typography.body1.copy(color = LocalContentColor.current).merge(textStyle),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = tfv.text,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = false,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource
            )
        },
    )
}

@Composable
private fun MyTextFieldTwo(
    initialText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
) {
    val tfs = rememberTextFieldState(initialText)
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        state = tfs,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle =
            MaterialTheme.typography.body1.copy(color = LocalContentColor.current).merge(textStyle),
        interactionSource = interactionSource,
        decorator = { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = tfs.text.toString(),
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = false,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource
            )
        },
    )
}

@Composable
private fun LabeledItem(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), spacedBy(4.dp)) {
        Text(label, color = LocalContentColor.current.copy(alpha = 0.5f))
        content()
    }
}

private fun Modifier.outline(color: Color = Color.LightGray): Modifier =
    this.border(1.dp, color, RoundedCornerShape(4.dp)).padding(2.dp)
