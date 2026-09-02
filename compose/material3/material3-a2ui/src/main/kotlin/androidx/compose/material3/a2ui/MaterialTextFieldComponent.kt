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

@file:Suppress("DEPRECATION") // b/553995833

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"TextField"` component schema.
 *
 * Displays an editable text input field that establishes a two-way data binding with the surface's
 * data model.
 *
 * **Schema Properties:**
 * * `label` (Dynamic String, required): The text label for the input field.
 * * `value` (Dynamic String, optional): The value of the text field.
 * * `variant` (String Enum, optional): The type of input field to display. Valid options:
 *   `"shortText"`, `"longText"`, `"number"`, `"obscured"`. Defaults to `"shortText"`.
 * * `validationRegexp` (String, optional): A regular expression used for client-side validation of
 *   the input.
 */
public object MaterialTextFieldComponent : A2uiComponent {

    private val labelProp =
        A2uiProperty.dynamicString(
            key = "label",
            required = true,
            description = "The text label for the input field.",
        )

    private val valueProp =
        A2uiProperty.dynamicString(
            key = "value",
            required = false,
            description = "The value of the text field.",
        )

    private val variantProp =
        A2uiProperty.enum(
            key = "variant",
            enumValues = TextFieldVariant.entries,
            mapToString = { it.token },
            convertFromString = { token -> TextFieldVariant.TokenMap[token] },
            defaultValue = TextFieldVariant.ShortText,
            description = "The type of input field to display.",
        )

    private val validationRegexpProp =
        A2uiProperty.string(
            key = "validationRegexp",
            required = false,
            description = "A regular expression used for client-side validation of the input.",
        )

    override val name: String = "TextField"

    override val description: String = "A field for user text input."

    override val properties: List<A2uiProperty<*>> =
        listOf(labelProp, valueProp, variantProp, validationRegexpProp)

    /**
     * Ensures the component remains in [androidx.a2ui.compose.runtime.A2uiComponentState.Loading]
     * until the required dynamic [labelProp] payload arrives to the surface data model.
     */
    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(labelProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val label =
            checkNotNull(properties.bind(labelProp)) {
                "Required property '${labelProp.key}' is missing."
            }
        val value = properties.bind(valueProp).orEmpty()
        val onValueChange = properties.bindUpdater(valueProp)
        val variant = properties[variantProp] ?: TextFieldVariant.ShortText

        val validationRegexp = properties[validationRegexpProp]
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

        var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }

        if (value != textFieldValue.text) {
            SideEffect(value) {
                val safeSelection =
                    TextRange(
                        start = textFieldValue.selection.start.coerceAtMost(value.length),
                        end = textFieldValue.selection.end.coerceAtMost(value.length),
                    )
                textFieldValue = textFieldValue.copy(text = value, selection = safeSelection)
            }
        }

        val isError =
            remember(textFieldValue.text, regex) {
                (regex != null) &&
                    textFieldValue.text.isNotEmpty() &&
                    !regex.matches(textFieldValue.text)
            }

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != value) {
                    onValueChange?.invoke(newValue.text)
                }
            },
            readOnly = onValueChange == null,
            label = { Text(text = label) },
            isError = isError,
            singleLine = variant.singleLine,
            minLines = variant.minLines,
            visualTransformation = variant.visualTransformation,
            keyboardOptions = variant.keyboardOptions,
            modifier = modifier,
        )
    }

    /** Supported text field variants and their keyboard and visual configurations. */
    private enum class TextFieldVariant(
        val token: String,
        val singleLine: Boolean,
        val visualTransformation: VisualTransformation,
        val keyboardOptions: KeyboardOptions,
        val minLines: Int = 1,
    ) {
        ShortText(
            token = "shortText",
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        ),
        LongText(
            token = "longText",
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        ),
        Number(
            token = "number",
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        ),
        Obscured(
            token = "obscured",
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        );

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            val TokenMap: Map<String, TextFieldVariant> =
                buildMap(entries.size) {
                    TextFieldVariant.entries.fastForEach { variant -> put(variant.token, variant) }
                }
        }
    }
}
