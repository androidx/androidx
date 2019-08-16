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

package androidx.ui.framework.demos.autofill

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.autofill.AutofillNode
import androidx.ui.autofill.AutofillType
import androidx.ui.core.AutofillAmbient
import androidx.ui.core.AutofillTreeAmbient
import androidx.ui.core.TextField
import androidx.ui.core.Text
import androidx.ui.material.themeTextStyle
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.input.EditorModel
import androidx.ui.input.EditorStyle
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.layout.Column
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.HeightSpacer
import androidx.ui.material.MaterialTheme

class ExplicitAutofillTypesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(crossAxisAlignment = CrossAxisAlignment.Start) {

                    val nameState = +state { EditorModel(text = "Enter name here") }
                    val emailState = +state { EditorModel(text = "Enter email here") }
                    val autofill = +ambient(AutofillAmbient)
                    val labelStyle = +themeTextStyle { subtitle1.copy() }
                    val textStyle = +themeTextStyle { h6.copy() }

                    Text("Name", style = labelStyle)
                    Autofill(
                        autofillTypes = listOf(AutofillType.Name),
                        onFill = { nameState.value = EditorModel(it) }
                    ) { autofillNode ->
                        TextField(
                            value = nameState.value,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Unspecified,
                            onValueChange = { nameState.value = it },
                            onFocus = { autofill?.requestAutofillForNode(autofillNode) },
                            onBlur = { autofill?.cancelAutofillForNode(autofillNode) },
                            editorStyle = EditorStyle(textStyle = textStyle)
                        )
                    }

                    HeightSpacer(40.dp)

                    Text("Email", style = labelStyle)
                    Autofill(
                        autofillTypes = listOf(AutofillType.EmailAddress),
                        onFill = { emailState.value = EditorModel(it) }
                    ) { autofillNode ->
                        TextField(
                            value = emailState.value,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Unspecified,
                            onValueChange = { emailState.value = it },
                            onFocus = { autofill?.requestAutofillForNode(autofillNode) },
                            onBlur = { autofill?.cancelAutofillForNode(autofillNode) },
                            editorStyle = EditorStyle(textStyle = textStyle)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
    @Children children: @Composable() (AutofillNode) -> Unit
) {
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)

    val autofillTree = +ambient(AutofillTreeAmbient)
    autofillTree += autofillNode

    OnChildPositioned(onPositioned = { autofillNode.boundingBox = it.boundingBox() }) {
        children(autofillNode)
    }
}

private fun LayoutCoordinates.boundingBox() = localToGlobal(PxPosition.Origin).run {
    Rect(
        x.value.toInt(),
        y.value.toInt(),
        x.value.toInt() + size.width.value.toInt(),
        y.value.toInt() + size.height.value.toInt()
    )
}