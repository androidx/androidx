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
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NullAnnotationGroup")
@Preview
@Composable
fun BTFResetCredentialsDemo() {
    val autofillManager = LocalAutofillManager.current

    Column(modifier = Modifier.background(color = Color.Black)) {
        Text(text = "Enter your new username and password below.", color = Color.White)

        BasicTextField(
            state = remember { TextFieldState() },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.NewUsername
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.White),
            cursorBrush = SolidColor(Color.White)
        )
        BasicTextField(
            state = remember { TextFieldState() },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.NewPassword
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.White),
            cursorBrush = SolidColor(Color.White)
        )

        // Submit button
        Button(onClick = { autofillManager?.commit() }) { Text("Reset credentials") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NullAnnotationGroup")
@Preview
@Composable
fun BasicTextFieldAutofill() {
    val autofillManager = LocalAutofillManager.current

    Column(modifier = Modifier.background(color = Color.Black)) {
        Text(text = "Enter your username and password below.", color = Color.White)

        BasicTextField(
            state = remember { TextFieldState() },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.Username
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.LightGray),
            cursorBrush = SolidColor(Color.White)
        )

        BasicTextField(
            state = remember { TextFieldState() },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.Password
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.LightGray),
            cursorBrush = SolidColor(Color.White)
        )

        // Submit button
        Button(onClick = { autofillManager?.commit() }) { Text("Submit credentials") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NullAnnotationGroup")
@Preview
@Composable
fun BasicSecureTextFieldAutofillDemo() {
    var visible by remember { mutableStateOf(false) }

    val autofillManager = LocalAutofillManager.current

    Column(modifier = Modifier.background(color = Color.Black)) {
        Text(text = "Enter your username and password below.", color = Color.White)
        BasicTextField(
            state = remember { TextFieldState() },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.Username
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.LightGray),
            cursorBrush = SolidColor(Color.White)
        )
        // TODO(mnuzen): Check if `Password` ContentType should automatically
        //  be applied to a BasicSecureTextField.
        BasicSecureTextField(
            state = remember { TextFieldState() },
            textObfuscationMode =
                if (visible) {
                    TextObfuscationMode.Visible
                } else {
                    TextObfuscationMode.RevealLastTyped
                },
            modifier =
                Modifier.fillMaxWidth().border(1.dp, Color.LightGray).semantics {
                    contentType = ContentType.Password
                },
            textStyle = MaterialTheme.typography.body1.copy(color = Color.White),
            cursorBrush = SolidColor(Color.White)
        )

        Checkbox(checked = visible, onCheckedChange = { visible = it })

        IconToggleButton(checked = visible, onCheckedChange = { visible = it }) {
            // TODO(MNUZEN): double check to make sure adding icon toggle does not break anything
            if (visible) {
                Icon(Icons.Default.Warning, "")
            } else {
                Icon(Icons.Default.Info, "")
            }
        }

        // Submit button
        Button(onClick = { autofillManager?.commit() }) { Text("Submit credentials") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NullAnnotationGroup")
@Preview
@Composable
fun LegacyTextFieldAutofillDemo() {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val autofillManager = LocalAutofillManager.current

    Column {
        // Username textfield
        TextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = { Text("Enter username here") },
            modifier = Modifier.semantics { contentType = ContentType.Username }
        )

        // Password textfield
        TextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Enter password here") },
            modifier = Modifier.semantics { contentType = ContentType.Password }
        )

        // Submit button
        Button(onClick = { autofillManager?.commit() }) { Text("Submit credentials") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("NullAnnotationGroup")
@Preview
@Composable
fun OutlinedTextFieldAutofillDemo() {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val autofillManager = LocalAutofillManager.current

    Column {
        // Username textfield
        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = { Text("Enter username here") },
            modifier = Modifier.semantics { contentType = ContentType.Username }
        )

        // Password textfield
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Enter password here") },
            modifier = Modifier.semantics { contentType = ContentType.Password }
        )

        // Submit button
        Button(onClick = { autofillManager?.commit() }) { Text("Submit credentials") }
    }
}
