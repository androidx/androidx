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

import android.graphics.Rect
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.autofill.AutofillNode
import androidx.ui.autofill.AutofillType
import androidx.ui.core.AutofillAmbient
import androidx.ui.core.AutofillTreeAmbient
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.Text
import androidx.ui.core.TextField
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.Spacer
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

@Composable
fun ExplicitAutofillTypesDemo() {
    Column {
        val nameState = state { "Enter name here" }
        val emailState = state { "Enter email here" }
        val autofill = AutofillAmbient.current
        val labelStyle = MaterialTheme.typography().subtitle1
        val textStyle = MaterialTheme.typography().h6

        Text("Name", style = labelStyle)
        Autofill(
            autofillTypes = listOf(AutofillType.Name),
            onFill = { nameState.value = it }
        ) { autofillNode ->
            TextField(
                value = nameState.value,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Unspecified,
                onValueChange = { nameState.value = it },
                onFocus = { autofill?.requestAutofillForNode(autofillNode) },
                onBlur = { autofill?.cancelAutofillForNode(autofillNode) },
                textStyle = textStyle
            )
        }

        Spacer(LayoutHeight(40.dp))

        Text("Email", style = labelStyle)
        Autofill(
            autofillTypes = listOf(AutofillType.EmailAddress),
            onFill = { emailState.value = it }
        ) { autofillNode ->
            TextField(
                value = emailState.value,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Unspecified,
                onValueChange = { emailState.value = it },
                onFocus = { autofill?.requestAutofillForNode(autofillNode) },
                onBlur = { autofill?.cancelAutofillForNode(autofillNode) },
                textStyle = textStyle
            )
        }
    }
}

@Composable
private fun Autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
    children: @Composable() (AutofillNode) -> Unit
) {
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)

    val autofillTree = AutofillTreeAmbient.current
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