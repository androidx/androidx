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

package androidx.ui.core.demos.autofill

import android.graphics.Rect
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.autofill.AutofillNode
import androidx.ui.autofill.AutofillType
import androidx.ui.core.AutofillAmbient
import androidx.ui.core.AutofillTreeAmbient
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.onChildPositioned
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.layout.Column
import androidx.ui.layout.Spacer
import androidx.ui.layout.preferredHeight
import androidx.ui.material.MaterialTheme
import androidx.ui.foundation.TextFieldValue
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

@Composable
fun ExplicitAutofillTypesDemo() {
    Column {
        val nameState = state { TextFieldValue("Enter name here") }
        val emailState = state { TextFieldValue("Enter email here") }
        val autofill = AutofillAmbient.current
        val labelStyle = MaterialTheme.typography.subtitle1
        val textStyle = MaterialTheme.typography.h6

        Text("Name", style = labelStyle)
        Autofill(
            autofillTypes = listOf(AutofillType.PersonFullName),
            onFill = { nameState.value = TextFieldValue(it) }
        ) { autofillNode ->
            TextField(
                value = nameState.value,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Unspecified,
                onValueChange = { nameState.value = it },
                onFocusChange = { focused ->
                    if (focused) {
                        autofill?.requestAutofillForNode(autofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(autofillNode)
                    }
                },
                textStyle = textStyle
            )
        }

        Spacer(Modifier.preferredHeight(40.dp))

        Text("Email", style = labelStyle)
        Autofill(
            autofillTypes = listOf(AutofillType.EmailAddress),
            onFill = { emailState.value = TextFieldValue(it) }
        ) { autofillNode ->
            TextField(
                value = emailState.value,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Unspecified,
                onValueChange = { emailState.value = it },
                onFocusChange = { focused ->
                    if (focused) {
                        autofill?.requestAutofillForNode(autofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(autofillNode)
                    }
                },
                textStyle = textStyle
            )
        }
    }
}

@Composable
private fun Autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
    children: @Composable (AutofillNode) -> Unit
) {
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)

    val autofillTree = AutofillTreeAmbient.current
    autofillTree += autofillNode

    @Suppress("DEPRECATION")
    PassThroughLayout(onChildPositioned { autofillNode.boundingBox = it.boundingBox() }) {
        children(autofillNode)
    }
}

private fun LayoutCoordinates.boundingBox() = localToGlobal(PxPosition.Origin).run {
    Rect(
        x.toInt(),
        y.toInt(),
        x.toInt() + size.width.value,
        y.toInt() + size.height.value
    )
}