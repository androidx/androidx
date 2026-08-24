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
import androidx.a2ui.compose.runtime.A2uiComponentReference
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.ChildListA2uiProperty
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
 * @property card The [Card] component implementation.
 * @property row The [Row] component implementation.
 * @property column The [Column] component implementation.
 * @property functions The list of [A2uiFunction]s supported by this catalog, recommended default is
 *   to create the function list using
 *   [androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions]
 */
public class A2uiBasicCatalogV1(
    public val text: Text,
    public val card: Card,
    public val row: Row,
    public val column: Column,
    // TODO(b/547851648): Add the rest of the basic catalog component types.
    public val functions: List<A2uiFunction>,
) {

    /** The unique identifier for this catalog. */
    public val catalogId: String = CatalogId

    /** The [A2uiSchema] defining the dynamic theme schema for this catalog. */
    public val themeSchema: A2uiSchema = ThemeSchema

    /** The list of [A2uiComponent]s supported by this catalog. */
    public val components: List<A2uiComponent> =
        listOf(
            text,
            card,
            row,
            column,
            // TODO(b/547851648): Add the rest of the basic catalog component types.
        )

    public companion object {
        /** The catalog ID for the A2UI Basic Catalog V1. */
        // TODO(b/547900174): update to v1.0 once the support for it is implemented.
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
                                    "The primary brand color used for highlights (e.g., primary " +
                                        "buttons, active borders). Renderers may generate " +
                                        "variants of this color for different contexts. Format: " +
                                        "Hexadecimal code (e.g., '#00BFFF')."
                            ),
                        "iconUrl" to
                            A2uiStringSchema(
                                description =
                                    "A URL for an image that identifies the agent or tool " +
                                        "associated with the surface."
                            ),
                        "agentDisplayName" to
                            A2uiStringSchema(
                                description =
                                    "Text to be displayed next to the surface to identify the " +
                                        "agent or tool that created it."
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
                    description =
                        "The text content to display. While simple Markdown formatting " +
                            "is supported (i.e. without HTML, images, or links), utilizing dedicated " +
                            "UI components is generally preferred for a richer and more structured " +
                            "presentation.",
                )
            public val variantProperty: StaticA2uiProperty<Variant> =
                A2uiProperty.enum(
                    key = "variant",
                    enumValues = Variant.entries,
                    mapToString = { it.value },
                    convertFromString = Variant::fromValue,
                    description = "A hint for the base text style.",
                )
            internal val componentProperties: List<A2uiProperty<*>> =
                listOf(textProperty, variantProperty)
        }

        override val properties: List<A2uiProperty<*>>
            get() = componentProperties

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

    /**
     * The A2UI `"Card"` component for displaying content in a styled container.
     *
     * **Schema Properties:**
     * * `child` (Component ID String, required): The ID of the child component to be rendered
     *   inside the card. Multiple elements must be wrapped in a layout container (e.g. Row or
     *   Column).
     */
    public interface Card : A2uiComponent {
        override val name: String
            get() = "Card"

        override val description: String
            get() = "A layout component that wraps its child content in a styled card container."

        public companion object {
            public val ChildProperty: StaticA2uiProperty<String> =
                A2uiProperty.componentId(
                    key = "child",
                    required = true,
                    description =
                        "The ID of the single child component to be rendered inside the card. To " +
                            "display multiple elements, you MUST wrap them in a layout component " +
                            "(like Column or Row) and pass that container's ID here. Do NOT pass " +
                            "multiple IDs or a non-existent ID.",
                )
            internal val ComponentProperties: List<A2uiProperty<*>> = listOf(ChildProperty)
        }

        override val properties: List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val childId =
                checkNotNull(properties[ChildProperty]) {
                    "Required property '${ChildProperty.key}' is missing."
                }
            TypedContent(childId = childId, modifier = modifier)
        }

        @Composable public fun A2uiComponentScope.TypedContent(childId: String, modifier: Modifier)
    }

    /**
     * The A2UI `"Row"` component for displaying content in a horizontal layout.
     *
     * **Schema Properties:**
     * * `children` (ChildList, required): Defines the children, accepting either an array of
     *   strings for a fixed set of children, or a template object to generate children from a data
     *   list.
     * * `justify` (String Enum, optional): Defines the arrangement of children along the main axis
     *   (horizontally). Valid options: `"center"`, `"end"`, `"spaceAround"`, `"spaceBetween"`,
     *   `"spaceEvenly"`, `"start"`, `"stretch"`. Defaults to `"start"`.
     * * `align` (String Enum, optional): Defines the alignment of children along the cross axis
     *   (vertically). Valid options: `"start"`, `"center"`, `"end"`, `"stretch"`. Defaults to
     *   `"stretch"`.
     */
    public interface Row : A2uiComponent {
        override val name: String
            get() = "Row"

        override val description: String
            get() =
                "A layout component that arranges its children horizontally. To create a grid " +
                    "layout, nest Columns within this Row."

        /** Arrangement of children along the main axis. */
        public enum class Justify(public val value: String) {
            Center("center"),
            End("end"),
            SpaceAround("spaceAround"),
            SpaceBetween("spaceBetween"),
            SpaceEvenly("spaceEvenly"),
            Start("start"),
            Stretch("stretch");

            public companion object {
                /** Returns the [Justify] matching [value], or [Start] if unknown. */
                public fun fromValue(value: String): Justify =
                    entries.fastFirstOrNull { it.value == value } ?: Start
            }
        }

        /** Alignment of children along the cross axis. */
        public enum class Align(public val value: String) {
            Start("start"),
            Center("center"),
            End("end"),
            Stretch("stretch");

            public companion object {
                /** Returns the [Align] matching [value], or [Stretch] if unknown. */
                public fun fromValue(value: String): Align =
                    entries.fastFirstOrNull { it.value == value } ?: Stretch
            }
        }

        public companion object {
            /** The [A2uiProperty] for the `"children"` property of a [Row]. */
            public val ChildrenProperty: ChildListA2uiProperty =
                A2uiProperty.childList(
                    key = "children",
                    required = true,
                    description =
                        "Defines the children. Use an array of strings for a fixed set of " +
                            "children, or a template object to generate children from a data " +
                            "list. Children cannot be defined inline, they must be referred to " +
                            "by ID.",
                )

            /** The [A2uiProperty] for the `"justify"` property of a [Row]. */
            public val JustifyProperty: StaticA2uiProperty<Justify> =
                A2uiProperty.enum(
                    key = "justify",
                    enumValues = Justify.entries,
                    mapToString = { it.value },
                    convertFromString = Justify::fromValue,
                    defaultValue = Justify.Start,
                    description =
                        "Defines the arrangement of children along the main axis (horizontally). " +
                            "Use 'spaceBetween' to push items to the edges, or " +
                            "'start'/'end'/'center' to pack them together.",
                )

            /** The [A2uiProperty] for the `"align"` property of a [Row]. */
            public val AlignProperty: StaticA2uiProperty<Align> =
                A2uiProperty.enum(
                    key = "align",
                    enumValues = Align.entries,
                    mapToString = { it.value },
                    convertFromString = Align::fromValue,
                    defaultValue = Align.Stretch,
                    description =
                        "Defines the alignment of children along the cross axis (vertically). " +
                            "This is similar to the CSS 'align-items' property, but uses " +
                            "camelCase values (e.g., 'start').",
                )

            internal val ComponentProperties: List<A2uiProperty<*>> =
                listOf(ChildrenProperty, JustifyProperty, AlignProperty)
        }

        override val properties: List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            properties.bindChildReferences(ChildrenProperty) != null

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val children =
                checkNotNull(properties.bindChildReferences(ChildrenProperty)) {
                    "Required property '${ChildrenProperty.key}' is missing or could not be " +
                        "resolved."
                }
            val justify = properties[JustifyProperty] ?: Justify.Start
            val align = properties[AlignProperty] ?: Align.Stretch

            TypedContent(children = children, justify = justify, align = align, modifier = modifier)
        }

        /**
         * Renders the [Row] with its resolved [children], [justify], and [align] properties.
         *
         * @param children list of child [A2uiComponentReference]s to render in this row
         * @param justify [Justify] arrangement of children along the horizontal main axis
         * @param align [Align] alignment of children along the vertical cross axis
         * @param modifier [Modifier] to apply to the layout
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            justify: Justify,
            align: Align,
            modifier: Modifier,
        )
    }

    /**
     * The A2UI `"Column"` component for arranging children vertically.
     *
     * **Schema Properties:**
     * * `children` (ChildList, required): Defines the children, accepting either an array of
     *   strings for a fixed set of children, or a template object to generate children from a data
     *   list.
     * * `justify` (String Enum, optional): Defines the arrangement of children along the main axis
     *   (vertically). Valid options: `"start"`, `"center"`, `"end"`, `"spaceBetween"`,
     *   `"spaceAround"`, `"spaceEvenly"`, `"stretch"`. Defaults to `"start"`.
     * * `align` (String Enum, optional): Defines the alignment of children along the cross axis
     *   (horizontally). Valid options: `"center"`, `"end"`, `"start"`, `"stretch"`. Defaults to
     *   `"stretch"`.
     */
    public interface Column : A2uiComponent {
        override val name: String
            get() = "Column"

        override val description: String
            get() =
                "A layout component that arranges its children vertically. To create a grid " +
                    "layout, nest Rows within this Column."

        /** Arrangement of children along the main axis. */
        public enum class Justify(public val value: String) {
            Start("start"),
            Center("center"),
            End("end"),
            SpaceBetween("spaceBetween"),
            SpaceAround("spaceAround"),
            SpaceEvenly("spaceEvenly"),
            Stretch("stretch");

            public companion object {
                /** Returns the [Justify] matching [value], or [Start] if unknown. */
                public fun fromValue(value: String): Justify =
                    entries.fastFirstOrNull { it.value == value } ?: Start
            }
        }

        /** Alignment of children along the cross axis. */
        public enum class Align(public val value: String) {
            Center("center"),
            End("end"),
            Start("start"),
            Stretch("stretch");

            public companion object {
                /** Returns the [Align] matching [value], or [Stretch] if unknown. */
                public fun fromValue(value: String): Align =
                    entries.fastFirstOrNull { it.value == value } ?: Stretch
            }
        }

        public companion object {
            /** The [A2uiProperty] for the `"children"` property of a [Column]. */
            public val ChildrenProperty: ChildListA2uiProperty =
                A2uiProperty.childList(
                    key = "children",
                    required = true,
                    description =
                        "Defines the children. Use an array of strings for a fixed set of " +
                            "children, or a template object to generate children from a data " +
                            "list. Children cannot be defined inline, they must be referred to " +
                            "by ID.",
                )

            /** The [A2uiProperty] for the `"justify"` property of a [Column]. */
            public val JustifyProperty: StaticA2uiProperty<Justify> =
                A2uiProperty.enum(
                    key = "justify",
                    enumValues = Justify.entries,
                    mapToString = { it.value },
                    convertFromString = Justify::fromValue,
                    defaultValue = Justify.Start,
                    description =
                        "Defines the arrangement of children along the main axis (vertically). " +
                            "Use 'spaceBetween' to push items to the edges (e.g. header at top, " +
                            "footer at bottom), or 'start'/'end'/'center' to pack them together.",
                )

            /** The [A2uiProperty] for the `"align"` property of a [Column]. */
            public val AlignProperty: StaticA2uiProperty<Align> =
                A2uiProperty.enum(
                    key = "align",
                    enumValues = Align.entries,
                    mapToString = { it.value },
                    convertFromString = Align::fromValue,
                    defaultValue = Align.Stretch,
                    description =
                        "Defines the alignment of children along the cross axis (horizontally). " +
                            "This is similar to the CSS 'align-items' property.",
                )

            internal val ComponentProperties: List<A2uiProperty<*>> =
                listOf(ChildrenProperty, JustifyProperty, AlignProperty)
        }

        override val properties: List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            properties.bindChildReferences(ChildrenProperty) != null

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val children =
                checkNotNull(properties.bindChildReferences(ChildrenProperty)) {
                    "Required property '${ChildrenProperty.key}' is missing or could not be " +
                        "resolved."
                }
            val justify = properties[JustifyProperty] ?: Justify.Start
            val align = properties[AlignProperty] ?: Align.Stretch

            TypedContent(children = children, justify = justify, align = align, modifier = modifier)
        }

        /**
         * Renders the [Column] with its resolved [children], [justify], and [align] properties.
         *
         * @param children list of child [A2uiComponentReference]s to render in this column
         * @param justify [Justify] arrangement of children along the vertical main axis
         * @param align [Align] alignment of children along the horizontal cross axis
         * @param modifier [Modifier] to apply to the layout
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            children: List<A2uiComponentReference>,
            justify: Justify,
            align: Align,
            modifier: Modifier,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is A2uiBasicCatalogV1) return false
        return catalogId == other.catalogId &&
            components == other.components &&
            functions == other.functions &&
            themeSchema == other.themeSchema
    }

    override fun hashCode(): Int {
        var result = catalogId.hashCode()
        result = 31 * result + components.hashCode()
        result = 31 * result + functions.hashCode()
        result = 31 * result + themeSchema.hashCode()
        return result
    }

    override fun toString(): String {
        return "A2uiBasicCatalogV1(" +
            "catalogId=$catalogId, " +
            "components=$components, " +
            "functions=$functions, " +
            "themeSchema=$themeSchema" +
            ")"
    }
}
