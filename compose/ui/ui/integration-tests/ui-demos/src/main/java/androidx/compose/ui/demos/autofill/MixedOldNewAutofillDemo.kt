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

package androidx.compose.ui.demos.autofill

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.collections.set

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NullAnnotationGroup")
@Preview
@Composable
fun MixedOldNewAutofillDemo() {
    Column(modifier = Modifier.background(color = Color.Black)) {
        Text(text = "Enter your username and password below.", color = Color.White)

        // Text field using new autofill API.
        BasicTextField(
            state = remember { TextFieldState() },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.Username
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.LightGray),
            cursorBrush = SolidColor(Color.White)
        )

        // Text field using old autofill API.
        val autofill = @Suppress("DEPRECATION") LocalAutofill.current
        val autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
        val textState = rememberTextFieldState()
        val autofillNode = remember {
            @Suppress("DEPRECATION")
            androidx.compose.ui.autofill.AutofillNode(
                onFill = { textState.edit { replace(0, length, it) } },
                autofillTypes = listOf(androidx.compose.ui.autofill.AutofillType.Password),
            )
        }
        BasicTextField(
            state = textState,
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, Color.LightGray)
                    .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
                    .onFocusChanged {
                        if (it.isFocused) {
                            autofill?.requestAutofillForNode(autofillNode)
                        } else {
                            autofill?.cancelAutofillForNode(autofillNode)
                        }
                    },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.LightGray),
            cursorBrush = SolidColor(Color.White)
        )
        DisposableEffect(autofillNode) {
            autofillTree.children[autofillNode.id] = autofillNode
            onDispose { autofillTree.children.remove(autofillNode.id) }
        }

        // Submit button (Only available using the new autofill APIs.
        val autofillManager = LocalAutofillManager.current
        Button(onClick = { autofillManager?.commit() }) { Text("Submit credentials") }
    }
}
