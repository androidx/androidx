/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * A standard login form with email and password text fields. Auto-capitalization and autocorrect
 * are disabled to make it easier to type credentials. Uses standard default IME actions to
 * automatically move focus on Next and Done.
 */
@Sampled
@Composable
fun BasicLoginFormSample() {
    val emailState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state = emailState,
            label = { Text("Email Address") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction =
                        ImeAction
                            .Next, // Soft keyboard automatically shifts focus to password field on
                    // Next
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedSecureTextField(
            state = passwordState,
            label = { Text("Password") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                    imeAction =
                        ImeAction.Done, // Soft keyboard automatically dismisses the keyboard on Done
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A masked 4-digit PIN entry row (like a lockscreen PIN pad). Uses customized cell box borders that
 * mask typed digits with dots.
 *
 * Demonstrates using [BasicSecureTextField] to implement custom cell-by-cell decoration.
 */
@Sampled
@Composable
fun PinCodeEntryRowSample() {
    val pinState = rememberTextFieldState()
    val pinLength = 4

    BasicSecureTextField(
        state = pinState,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        inputTransformation =
            InputTransformation.byValue { _, proposed -> proposed.filter { it.isDigit() } }
                .maxLength(pinLength),
        decorator = { innerTextField ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pinLength) { index ->
                    val char = pinState.text.getOrNull(index)
                    Box(
                        modifier =
                            Modifier.size(48.dp)
                                .border(
                                    1.dp,
                                    if (pinState.text.length == index) Color.Blue else Color.Gray,
                                    RoundedCornerShape(8.dp),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (char != null) {
                            Text("●")
                        }
                    }
                }
            }
        },
    )
}

/**
 * Decimal input fields for entering coordinates. Prompts soft keyboards to show a decimal point
 * key.
 */
@Sampled
@Composable
fun DecimalInputSample() {
    val latitudeState = rememberTextFieldState("37.7749")
    val longitudeState = rememberTextFieldState("-122.4194")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state = latitudeState,
            label = { Text("Latitude") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            state = longitudeState,
            label = { Text("Longitude") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * An integer item count settings field. Shows error validation warning text when non-digits or
 * invalid counts are inputted.
 */
@Sampled
@Composable
fun ItemCountSettingsSample() {
    val countState = rememberTextFieldState("100")
    val isError by remember {
        derivedStateOf {
            val parsed = countState.text.toString().toIntOrNull()
            parsed == null || parsed <= 0
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state = countState,
            label = { Text("List Item Display Count") },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isError) {
            Text("Value must be a positive integer", color = Color.Red)
        }
    }
}
