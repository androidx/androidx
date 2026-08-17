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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.DynamicA2uiProperty
import androidx.a2ui.compose.runtime.StaticA2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastFirstOrNull

/**
 * Defines the API contract and schemas for the A2UI Basic Catalog V1 following the specification in
 * https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json.
 *
 * This class acts as the API contract for any design system (e.g. Material 3, Jetpack Compose
 * Glimmer, TV Compose) wishing to implement the basic catalog specification.
 *
 * @property text The [Text] component implementation.
 * @property functions The list of [A2uiFunction]s supported by this catalog, recommended default is
 *   to create the function list using
 *   [androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions]
 */
public class A2uiBasicCatalogV1(
    public val text: Text,
    // TODO(b/547851648): Add the rest of the basic catalog component types.
    public val functions: List<A2uiFunction>,
) {

    /** The unique identifier for this catalog. */
    public val catalogId: String = CatalogId

    /** The [A2uiSchema] defining the dynamic theme schema for this catalog. */
    public val themeSchema: A2uiSchema = ThemeSchema

    /** The list of [A2uiComponent]s supported by this catalog. */
    public val components: List<A2uiComponent> =
        arrayListOf(
            text
            // TODO(b/547851648): Add the rest of the basic catalog component types.
        )

    public companion object {
        /** The catalog ID for the A2UI Basic Catalog V1. */
        // TODO(b/547900174): update to v1.0 once the support for it implemented.
        public const val CatalogId: String =
            "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json"

        /** The standard theme schema for the A2UI Basic Catalog V1. */
        public val ThemeSchema: A2uiSchema =
            A2uiObjectSchema(
                properties =
                    mapOf(
                        "primaryColor" to
                            A2uiStringSchema(
                                description =
                                    "The primary brand color used for highlights (e.g., primary buttons, active borders). Renderers may generate variants of this color for different contexts. Format: Hexadecimal code (e.g., '#00BFFF')."
                            ),
                        "iconUrl" to
                            A2uiStringSchema(
                                description =
                                    "A URL for an image that identifies the agent or tool associated with the surface."
                            ),
                        "agentDisplayName" to
                            A2uiStringSchema(
                                description =
                                    "Text to be displayed next to the surface to identify the agent or tool that created it."
                            ),
                    )
            )
    }

    /**
     * The A2UI `"Text"` component for displaying text.
     *
     * **Schema Properties:**
     * * `text` (Dynamic String, required): The text content to display. Accepts either a static
     *   string literal or a dynamic data binding.
     * * `variant` (String Enum, optional): A hint for the base text style. This is a static
     *   configuration and does not support dynamic data bindings. Valid options: `"h1"`, `"h2"`,
     *   `"h3"`, `"h4"`, `"h5"`, `"caption"`, `"body"`. Defaults to `"body"`.
     */
    public interface Text : A2uiComponent {
        override val name: String
            get() = "Text"

        override val description: String
            get() = "Displays dynamic text."

        /** Text variant determining typography styling. */
        public enum class Variant(public val value: String) {
            H1("h1"),
            H2("h2"),
            H3("h3"),
            H4("h4"),
            H5("h5"),
            Caption("caption"),
            Body("body");

            public companion object {
                public fun fromValue(value: String): Variant =
                    entries.fastFirstOrNull { it.value == value } ?: Body
            }
        }

        public companion object {
            public val textProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicString(
                    key = "text",
                    required = true,
                    description = "The text content to display.",
                )
            public val variantProperty: StaticA2uiProperty<Variant> =
                A2uiProperty.enum(
                    key = "variant",
                    enumValues = Variant.entries,
                    valueMapper = { it.value },
                    fromValue = Variant::fromValue,
                    description = "A hint for the base text style.",
                )
        }

        override val properties: List<A2uiProperty<*>>
            get() = listOf(textProperty, variantProperty)

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            properties.bind(textProperty) != null

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val textValue =
                checkNotNull(properties.bind(textProperty)) {
                    "Required property '${textProperty.key}' is missing."
                }
            val variant = properties[variantProperty] ?: Variant.Body
            TypedContent(text = textValue, variant = variant, modifier = modifier)
        }

        @Composable
        public fun A2uiComponentScope.TypedContent(
            text: String,
            variant: Variant,
            modifier: Modifier,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiBasicCatalogV1) return false
        return catalogId == other.catalogId &&
            text == other.text &&
            functions == other.functions &&
            themeSchema == other.themeSchema
    }

    override fun hashCode(): Int {
        var result = catalogId.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + functions.hashCode()
        result = 31 * result + themeSchema.hashCode()
        return result
    }

    override fun toString(): String {
        return "A2uiBasicCatalogV1(" +
            "catalogId=$catalogId, " +
            "text=$text, " +
            "functions=$functions, " +
            "themeSchema=$themeSchema" +
            ")"
    }
}
