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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"TextField"` component.
 */
internal object MaterialA2uiBasicCatalogV1TextField : A2uiBasicCatalogV1.TextField {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String,
        value: String?,
        variant: A2uiBasicCatalogV1.TextField.Variant,
        validationRegexp: String?,
        onValueChange: (String) -> Unit,
        enabled: Boolean,
        modifier: Modifier,
    ) {
        val (regex, regexError) =
            remember(validationRegexp) {
                if (validationRegexp != null) {
                    try {
                        Regex(validationRegexp) to null
                    } catch (e: Exception) {
                        null to
                            A2uiRuntimeException(
                                "Invalid validationRegexp '$validationRegexp': ${e.message}"
                            )
                    }
                } else {
                    null to null
                }
            }

        if (regexError != null) {
            SideEffect(regexError) { reportError(regexError) }
        }

        val textValue = value.orEmpty()
        var textFieldValue by remember { mutableStateOf(TextFieldValue(text = textValue)) }
        var lastExternalText by remember { mutableStateOf(textValue) }
        var lastEmittedText by remember { mutableStateOf<String?>(null) }

        if (textValue != lastExternalText) {
            lastExternalText = textValue
            if (textValue != lastEmittedText) {
                textFieldValue =
                    TextFieldValue(
                        text = textValue,
                        selection = textFieldValue.selection.coerceIn(0, textValue.length),
                    )
            }
        }

        val isError =
            regex != null &&
                remember(textFieldValue.text, regex) {
                    textFieldValue.text.isNotEmpty() && !regex.matches(textFieldValue.text)
                }

        val singleLine = variant != A2uiBasicCatalogV1.TextField.Variant.LongText
        @Suppress("DEPRECATION") // b/553995833
        val visualTransformation =
            if (variant == A2uiBasicCatalogV1.TextField.Variant.Obscured) {
                PasswordTransformation
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            }
        val keyboardOptions =
            when (variant) {
                A2uiBasicCatalogV1.TextField.Variant.Number -> NumberKeyboardOptions
                A2uiBasicCatalogV1.TextField.Variant.Obscured -> PasswordKeyboardOptions
                A2uiBasicCatalogV1.TextField.Variant.ShortText,
                A2uiBasicCatalogV1.TextField.Variant.LongText -> TextKeyboardOptions
            }

        @Suppress("DEPRECATION") // b/553995833
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val textChanged = newValue.text != textFieldValue.text
                textFieldValue = newValue
                if (textChanged) {
                    lastEmittedText = newValue.text
                    onValueChange(newValue.text)
                }
            },
            readOnly = !enabled,
            label = { Text(text = label) },
            isError = isError,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            modifier = modifier,
        )
    }
}

private val TextKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
private val NumberKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
private val PasswordKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
@Suppress("DEPRECATION") // b/553995833
private val PasswordTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
