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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.allCaps
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp

/**
 * A complete profile registration screen sample. Shows how to handle standard name entries, login
 * emails, physical mailing address lines, secure password visibility eye-icon togglers, and natural
 * language biography summaries.
 */
@Sampled
@Composable
fun RegistrationFormSample() {
    val nameState = rememberTextFieldState()
    val emailState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    val urlState = rememberTextFieldState()
    val addressState = rememberTextFieldState()
    val biographyState = rememberTextFieldState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    ) {
        // 1. PersonName Input (Auto-Capitalizes Words)
        OutlinedTextField(
            state = nameState,
            label = { Text("Full Name") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.PersonName,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next, // Soft keyboard automatically moves focus on Next
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. Email Input (Restricted layout showing '@' and '.', auto-capitalization disabled)
        OutlinedTextField(
            state = emailState,
            label = { Text("Email") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 3. Password Input with Eye-Icon Toggle (masked vs visible password layouts,
        // capitalizations disabled)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedSecureTextField(
                state = passwordState,
                label = { Text("Password") },
                textObfuscationMode =
                    if (isPasswordVisible) TextObfuscationMode.Visible
                    else TextObfuscationMode.System,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            if (isPasswordVisible) KeyboardType.PasswordVisible
                            else KeyboardType.Password,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Next,
                    ),
                modifier = Modifier.weight(0.7f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = { isPasswordVisible = !isPasswordVisible },
                modifier = Modifier.weight(0.3f),
            ) {
                Text(if (isPasswordVisible) "Hide" else "Show")
            }
        }

        // 4. Uri Input (Prominent '/' and '.com' keys, auto-capitalization disabled)
        OutlinedTextField(
            state = urlState,
            label = { Text("Homepage URL") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 5. PostalAddress Input (Auto-Capitalizes Words, prompts standard shipping keys)
        OutlinedTextField(
            state = addressState,
            label = { Text("Shipping Address") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.PostalAddress,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 6. Free-Text Biography Input (Auto-Capitalizes Sentences, multi-line active)
        OutlinedTextField(
            state = biographyState,
            label = { Text("Biography") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction =
                        ImeAction.Done, // Soft keyboard automatically dismisses keyboard on Done
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A register cashier checkout panel sample. Shows how to build secure cashier PIN rows, automatic
 * dollar currency amounts input formatters, integer item quantities, customer phone dialers, and
 * capitalized promo voucher entry lines.
 */
@Sampled
@Composable
fun CheckoutRegisterFormSample() {
    val cashierPinState = rememberTextFieldState()
    val qtyState = rememberTextFieldState()
    val phoneState = rememberTextFieldState()
    val couponState = rememberTextFieldState()
    val balanceOffsetState = rememberTextFieldState()

    val pinLength = 4

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    ) {
        // 1. NumberPassword PIN Entry (Masked secure numeric pad (0-9))
        OutlinedSecureTextField(
            state = cashierPinState,
            label = { Text("Enter Cashier PIN (OTP)") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next,
                ),
            inputTransformation =
                InputTransformation.byValue { _, proposed -> proposed.filter { it.isDigit() } }
                    .maxLength(pinLength),
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. Number Plain Integer Input (Pure numeric key entry for quantities)
        OutlinedTextField(
            state = qtyState,
            label = { Text("Item Quantity Count") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            inputTransformation =
                InputTransformation.byValue { _, proposed -> proposed.filter { it.isDigit() } },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 4. Phone Contact Entry (Displays telephony dialer keyboard with '+', '*', '#')
        OutlinedTextField(
            state = phoneState,
            label = { Text("Customer Loyalty Phone Number") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 5. Ascii / KeyboardCapitalization.Characters (Uppercase restricted ASCII promo vouchers)
        OutlinedTextField(
            state = couponState,
            label = { Text("Promo Coupon Code") },
            inputTransformation = InputTransformation.allCaps(Locale.current),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 6. DecimalSigned Shared Offset Balance Adjustments (Allows decimal coordinates and
        // negative/positive signs)
        OutlinedTextField(
            state = balanceOffsetState,
            label = { Text("Add Balance Offset (Debits/Credits)") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.DecimalSigned,
                    imeAction = ImeAction.Done,
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * An alarm clock, timer, and calendar event scheduler card sample. Shows how to handle geographic
 * date selectors, preset time clocks, and combined scheduling timestamps.
 */
@Sampled
@Composable
fun DateTimeSchedulerFormSample() {
    val birthState = rememberTextFieldState()
    val timerState = rememberTextFieldState()
    val meetingState = rememberTextFieldState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    ) {
        // 1. Date Input (Prompt numeric layout tailored for dates containing '/' or '-')
        OutlinedTextField(
            state = birthState,
            label = { Text("Birthdate (YYYY/MM/DD)") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Date, imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. Time Input (Prompt numeric layout tailored for times containing ':')
        OutlinedTextField(
            state = timerState,
            label = { Text("Timer Preset Clock (HH:MM)") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Time, imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 3. DateTime Input (Prompts clock-calendar layout option)
        OutlinedTextField(
            state = meetingState,
            label = { Text("Combined Meeting Timestamp (YYYY/MM/DD HH:MM)") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.DateTime, imeAction = ImeAction.Done),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Demonstrates search filters, phonetic translation entries, and specialty masked numeric keypads.
 */
@Sampled
@Composable
fun SpecialtyInputsFormSample() {
    val filterState = rememberTextFieldState()
    val phoneticState = rememberTextFieldState()
    val maskedCoordsState = rememberTextFieldState()
    val maskedBalanceState = rememberTextFieldState()
    val maskedPasskeyState = rememberTextFieldState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
    ) {
        // 1. Filter Input (Optimized list filtering, auto-capitalization/suggestions disabled)
        OutlinedTextField(
            state = filterState,
            label = { Text("Search Query") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Filter,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. Phonetic Input (For phonetic readings/pronunciations, e.g. phonetic names in contacts)
        OutlinedTextField(
            state = phoneticState,
            label = { Text("Phonetic Name") },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Phonetic, imeAction = ImeAction.Next),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        // 3. DecimalPassword Input (Masked numeric pad showing decimal separators)
        OutlinedSecureTextField(
            state = maskedCoordsState,
            label = { Text("Secure Decimal Value") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.DecimalPassword,
                    imeAction = ImeAction.Next,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // 4. NumberPasswordSigned Input (Masked numeric pad showing positive/negative signs)
        OutlinedSecureTextField(
            state = maskedBalanceState,
            label = { Text("Secure Signed Value") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.NumberPasswordSigned,
                    imeAction = ImeAction.Next,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        // 5. DecimalPasswordSigned Input (Masked numeric pad showing both decimals and signs)
        OutlinedSecureTextField(
            state = maskedPasskeyState,
            label = { Text("Secure Decimal Signed Value") },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.DecimalPasswordSigned,
                    imeAction = ImeAction.Done,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
