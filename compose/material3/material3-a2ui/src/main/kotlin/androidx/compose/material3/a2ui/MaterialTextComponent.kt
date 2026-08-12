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

// TODO(b/544548540): Add Markdown support as per the basic catalog specification.
package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Text"` component schema.
 *
 * Displays formatted text according to Material 3 typography tokens.
 *
 * **Schema Properties:**
 * * `text` (Dynamic String, required): The text content to display. Accepts either a static string
 *   literal or a dynamic data binding.
 * * `variant` (String Enum, optional): A hint for the base text style. This is a static
 *   configuration and does not support dynamic data bindings. Valid options: `"h1"`, `"h2"`,
 *   `"h3"`, `"h4"`, `"h5"`, `"caption"`, `"body"`. Defaults to `"body"`.
 */
public object MaterialTextComponent : A2uiComponent {

    private val textProp =
        A2uiProperty.dynamicString(
            key = "text",
            required = true,
            description = "The text content to display.",
        )

    private val variantProp =
        A2uiProperty.stringEnum(
            key = "variant",
            enumValues = TextVariant.AllTokens,
            required = false,
            description = "A hint for the base text style.",
        )

    override val name: String = "Text"

    override val description: String =
        "Displays read-only text, titles, or status messages. Supports dynamic data bindings."

    override val properties: List<A2uiProperty<*>> = listOf(textProp, variantProp)

    /**
     * Ensures the component remains in [androidx.a2ui.compose.runtime.A2uiComponentState.Loading]
     * until the required dynamic [textProp] payload arrives to the surface data model.
     */
    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(textProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val textValue =
            checkNotNull(properties.bind(textProp)) {
                "Required property '${textProp.key}' is missing."
            }
        val variant = TextVariant.fromToken(properties[variantProp])

        var textModifier = modifier
        if (variant.topPadding > 0.dp || variant.bottomPadding > 0.dp) {
            textModifier =
                textModifier.padding(top = variant.topPadding, bottom = variant.bottomPadding)
        }
        if (variant.isHeading) {
            textModifier = textModifier.semantics { heading() }
        }

        Text(text = textValue, style = variant.textStyle, modifier = textModifier)
    }

    /** Supported Material 3 typography variant tokens and their intrinsic layout spacing. */
    private enum class TextVariant(
        val token: String,
        val topPadding: Dp,
        val bottomPadding: Dp,
        private val textStyleProvider: @Composable () -> TextStyle,
    ) {
        H1("h1", 16.dp, 8.dp, { MaterialTheme.typography.headlineLarge }),
        H2("h2", 12.dp, 6.dp, { MaterialTheme.typography.headlineMedium }),
        H3("h3", 8.dp, 4.dp, { MaterialTheme.typography.headlineSmall }),
        H4("h4", 4.dp, 2.dp, { MaterialTheme.typography.titleLarge }),
        H5("h5", 4.dp, 2.dp, { MaterialTheme.typography.titleMedium }),
        Caption("caption", 0.dp, 0.dp, { MaterialTheme.typography.labelMedium }),
        Body("body", 0.dp, 0.dp, { MaterialTheme.typography.bodyLarge });

        val isHeading: Boolean
            get() = this == H1 || this == H2 || this == H3 || this == H4 || this == H5

        val textStyle: TextStyle
            @Composable get() = textStyleProvider()

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            private val TokenMap: Map<String, TextVariant> =
                buildMap(entries.size) {
                    TextVariant.entries.fastForEach { variant -> put(variant.token, variant) }
                }

            /** Resolves a string token to a [TextVariant]. Returns [Body] if [token] is null. */
            fun fromToken(token: String?): TextVariant = token?.let { TokenMap[it] } ?: Body
        }
    }
}
