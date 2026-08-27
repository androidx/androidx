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
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.a2ui.model.schema.commontypes.A2uiDataBindingSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMap
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Defines the API contract and schemas for the A2UI Basic Catalog V1 following the specification in
 * https://a2ui.org/specification/v1_0/catalogs/basic/catalog.json.
 *
 * This class acts as the API contract for any design system (e.g. Material 3, Jetpack Compose
 * Glimmer, TV Compose) wishing to implement the basic catalog specification.
 *
 * @property text The [Text] component implementation.
 * @property image The [Image] component implementation.
 * @property icon The [Icon] component implementation.
 * @property card The [Card] component implementation.
 * @property row The [Row] component implementation.
 * @property column The [Column] component implementation.
 * @property list The [List] component implementation.
 * @property tabs The [Tabs] component implementation.
 * @property button The [Button] component implementation.
 * @property dateTimeInput The [DateTimeInput] component implementation.
 * @property functions The list of [A2uiFunction]s supported by this catalog, recommended default is
 *   to create the function list using
 *   [androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions]
 */
public class A2uiBasicCatalogV1(
    public val text: Text,
    public val image: Image,
    public val icon: Icon,
    public val card: Card,
    public val row: Row,
    public val column: Column,
    public val list: List,
    public val tabs: Tabs,
    public val button: Button,
    public val dateTimeInput: DateTimeInput,
    // TODO(b/547851648): Add the rest of the basic catalog component types.
    public val functions: kotlin.collections.List<A2uiFunction>,
) {

    /** The unique identifier for this catalog. */
    public val catalogId: String = CatalogId

    /** The [A2uiSchema] defining the dynamic theme schema for this catalog. */
    public val themeSchema: A2uiSchema = ThemeSchema

    /** The list of [A2uiComponent]s supported by this catalog. */
    public val components: kotlin.collections.List<A2uiComponent> =
        listOf(
            text,
            image,
            icon,
            card,
            row,
            column,
            list,
            tabs,
            button,
            dateTimeInput,
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

    /** Accessibility attributes for an element in the A2UI Basic Catalog V1. */
    @Immutable
    public class AccessibilityAttributes(
        public val label: String? = null,
        public val description: String? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AccessibilityAttributes) return false

            if (label != other.label) return false
            if (description != other.description) return false

            return true
        }

        override fun hashCode(): Int {
            var result = label.hashCode()
            result = 31 * result + description.hashCode()
            return result
        }

        override fun toString(): String =
            "AccessibilityAttributes(label=$label, description=$description)"
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
            internal val componentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(textProperty, variantProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
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
     * The A2UI `"Image"` component for displaying an image from a URL.
     *
     * **Schema Properties:**
     * * `url` (Dynamic String, required): The URL of the image to display.
     * * `description` (Dynamic String, optional): Accessibility text for the image.
     * * `fit` (String Enum, optional): Specifies how the image should be resized to fit its
     *   container. This corresponds to the CSS 'object-fit' property. Valid options: `"contain"`,
     *   `"cover"`, `"fill"`, `"none"`, `"scaleDown"`. Defaults to `"fill"`.
     * * `variant` (String Enum, optional): A hint for the image size and style. Valid options:
     *   `"icon"`, `"avatar"`, `"smallFeature"`, `"mediumFeature"`, `"largeFeature"`, `"header"`.
     *   Defaults to `"mediumFeature"`.
     */
    public interface Image : A2uiComponent {
        override val name: String
            get() = "Image"

        override val description: String
            get() = "Displays an image from a URL."

        /** Specifies how the image should be resized to fit its container. */
        public enum class Fit(public val value: String) {
            Contain("contain"),
            Cover("cover"),
            Fill("fill"),
            None("none"),
            ScaleDown("scaleDown");

            public companion object {
                /** Returns the [Fit] matching [value], or [Fill] if unknown. */
                public fun fromValue(value: String): Fit =
                    entries.fastFirstOrNull { it.value == value } ?: Fill
            }
        }

        /** A visual variant determining the image size and style hint. */
        public enum class Variant(public val value: String) {
            Icon("icon"),
            Avatar("avatar"),
            SmallFeature("smallFeature"),
            MediumFeature("mediumFeature"),
            LargeFeature("largeFeature"),
            Header("header");

            public companion object {
                /** Returns the [Variant] matching [value], or [MediumFeature] if unknown. */
                public fun fromValue(value: String): Variant =
                    entries.fastFirstOrNull { it.value == value } ?: MediumFeature
            }
        }

        public companion object {
            /** The [A2uiProperty] for the `"url"` property of an [Image]. */
            public val UrlProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicString(
                    key = "url",
                    required = true,
                    description = "The URL of the image to display.",
                )

            /** The [A2uiProperty] for the `"description"` property of an [Image]. */
            public val DescriptionProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicString(
                    key = "description",
                    required = false,
                    description = "Accessibility text for the image.",
                )

            /** The [A2uiProperty] for the `"fit"` property of an [Image]. */
            public val FitProperty: StaticA2uiProperty<Fit> =
                A2uiProperty.enum(
                    key = "fit",
                    enumValues = Fit.entries,
                    mapToString = { it.value },
                    convertFromString = Fit::fromValue,
                    defaultValue = Fit.Fill,
                    description =
                        "Specifies how the image should be resized to fit its container. " +
                            "This corresponds to the CSS 'object-fit' property.",
                )

            /** The [A2uiProperty] for the `"variant"` property of an [Image]. */
            public val VariantProperty: StaticA2uiProperty<Variant> =
                A2uiProperty.enum(
                    key = "variant",
                    enumValues = Variant.entries,
                    mapToString = { it.value },
                    convertFromString = Variant::fromValue,
                    defaultValue = Variant.MediumFeature,
                    description = "A hint for the image size and style.",
                )

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(UrlProperty, DescriptionProperty, FitProperty, VariantProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            properties.bind(UrlProperty) != null

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val url =
                checkNotNull(properties.bind(UrlProperty)) {
                    "Required property '${UrlProperty.key}' is missing."
                }
            val description = properties.bind(DescriptionProperty)
            val fit = properties[FitProperty] ?: Fit.Fill
            val variant = properties[VariantProperty] ?: Variant.MediumFeature

            TypedContent(
                url = url,
                description = description,
                fit = fit,
                variant = variant,
                modifier = modifier,
            )
        }

        /**
         * Renders the [Image] with its resolved [url], [description], [fit], and [variant]
         * properties.
         *
         * @param url The URL of the image to display.
         * @param description Accessibility text for the image.
         * @param fit Specifies how the image should be resized to fit its container.
         * @param variant A hint for the image size and style.
         * @param modifier [Modifier] to apply to the layout.
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            url: String,
            description: String?,
            fit: Fit,
            variant: Variant,
            modifier: Modifier,
        )
    }

    /**
     * The A2UI `"Icon"` component for displaying an icon.
     *
     * **Schema Properties:**
     * * `name` (Dynamic Custom, required): The name of the icon to display. Accepts either a static
     *   string literal from the predefined list, an object with an `svgPath` string, or a dynamic
     *   data binding.
     * * `accessibility` (Dynamic Custom, optional): Accessibility attributes for the icon.
     */
    public interface Icon : A2uiComponent {
        override val name: String
            get() = "Icon"

        override val description: String
            get() = "Displays an icon from a predefined set of icons or an SVG path."

        /** The visual source for the [Icon]. */
        public sealed interface Source

        /** Indicates an icon should be drawn from a bespoke client-provided SVG path. */
        @Immutable
        public class SvgPath(public val svgPath: String) : Source {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is SvgPath) return false
                return svgPath == other.svgPath
            }

            override fun hashCode(): Int = svgPath.hashCode()

            override fun toString(): String = "SvgPath(svgPath='$svgPath')"
        }

        /** Supported built-in icon tokens in the A2UI basic catalog schema. */
        public enum class BuiltIn(public val value: String) : Source {
            AccountCircle("accountCircle"),
            Add("add"),
            ArrowBack("arrowBack"),
            ArrowForward("arrowForward"),
            AttachFile("attachFile"),
            CalendarToday("calendarToday"),
            Call("call"),
            Camera("camera"),
            Check("check"),
            Close("close"),
            Delete("delete"),
            Download("download"),
            Edit("edit"),
            Error("error"),
            Event("event"),
            FastForward("fastForward"),
            Favorite("favorite"),
            FavoriteOff("favoriteOff"),
            Folder("folder"),
            Help("help"),
            Home("home"),
            Info("info"),
            LocationOn("locationOn"),
            Lock("lock"),
            LockOpen("lockOpen"),
            Mail("mail"),
            Menu("menu"),
            MoreHoriz("moreHoriz"),
            MoreVert("moreVert"),
            Notifications("notifications"),
            NotificationsOff("notificationsOff"),
            Pause("pause"),
            Payment("payment"),
            Person("person"),
            Phone("phone"),
            Photo("photo"),
            Play("play"),
            Print("print"),
            Refresh("refresh"),
            Rewind("rewind"),
            Search("search"),
            Send("send"),
            Settings("settings"),
            Share("share"),
            ShoppingCart("shoppingCart"),
            SkipNext("skipNext"),
            SkipPrevious("skipPrevious"),
            Star("star"),
            StarHalf("starHalf"),
            StarOff("starOff"),
            Stop("stop"),
            Upload("upload"),
            Visibility("visibility"),
            VisibilityOff("visibilityOff"),
            VolumeDown("volumeDown"),
            VolumeMute("volumeMute"),
            VolumeOff("volumeOff"),
            VolumeUp("volumeUp"),
            Warning("warning");

            public companion object {
                /** Returns the [BuiltIn] matching [value], or null if unknown. */
                public fun fromValue(value: String): BuiltIn? =
                    entries.fastFirstOrNull { it.value == value }
            }
        }

        /**
         * Indicates an icon specified by an unrecognized name string that does not match any
         * predefined [BuiltIn] token.
         */
        @Immutable
        public class Unrecognized(public val name: String) : Source {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Unrecognized) return false
                return name == other.name
            }

            override fun hashCode(): Int = name.hashCode()

            override fun toString(): String = "Unrecognized(name='$name')"
        }

        public companion object {
            /** The [A2uiProperty] for the `"accessibility"` property of an [Icon]. */
            public val AccessibilityProperty: DynamicA2uiProperty<AccessibilityAttributes> =
                A2uiProperty.dynamicCustom(
                    key = "accessibility",
                    schema = A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE,
                    safeCast = { value ->
                        val map = value as? Map<*, *> ?: return@dynamicCustom null
                        AccessibilityAttributes(
                            label = map["label"]?.toString(),
                            description = map["description"]?.toString(),
                        )
                    },
                )

            private val nameSchema: A2uiSchema =
                A2uiAnySchema(
                    description = "The name of the icon to display.",
                    keywords =
                        listOf(
                            A2uiSchemaKeyword.OneOf(
                                listOf(
                                    A2uiStringSchema(
                                        keywords =
                                            listOf(
                                                A2uiSchemaKeyword.Enum(
                                                    BuiltIn.entries.fastMap { it.value }
                                                )
                                            )
                                    ),
                                    A2uiObjectSchema(
                                        properties = mapOf("svgPath" to A2uiStringSchema.INSTANCE),
                                        required = setOf("svgPath"),
                                        isAdditionalPropertiesAllowed = false,
                                    ),
                                    A2uiDataBindingSchema.DEFAULT_INSTANCE,
                                )
                            )
                        ),
                )

            /** The [A2uiProperty] for the `"name"` property of an [Icon]. */
            public val NameProperty: DynamicA2uiProperty<Source> =
                A2uiProperty.dynamicCustom(
                    key = "name",
                    required = true,
                    schema = nameSchema,
                    safeCast = { value ->
                        when (value) {
                            is String -> BuiltIn.fromValue(value) ?: Unrecognized(value)
                            is Map<*, *> -> (value["svgPath"] as? String)?.let { SvgPath(it) }
                            else -> null
                        }
                    },
                )

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(NameProperty, AccessibilityProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
            properties.bind(NameProperty) != null

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val nameValue =
                checkNotNull(properties.bind(NameProperty)) {
                    "Required property '${NameProperty.key}' is missing."
                }
            val accessibility = properties.bind(AccessibilityProperty)
            TypedContent(source = nameValue, accessibility = accessibility, modifier = modifier)
        }

        /**
         * Renders the [Icon] with its resolved [source] built-in name or SVG path and optional
         * [accessibility] attributes.
         *
         * @param source The resolved [Source] identifying the visual to draw.
         * @param accessibility Accessibility attributes for the icon.
         * @param modifier [Modifier] to apply to the layout.
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            source: Source,
            accessibility: AccessibilityAttributes?,
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
            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(ChildProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
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

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(ChildrenProperty, JustifyProperty, AlignProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
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
            children: kotlin.collections.List<A2uiComponentReference>,
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

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(ChildrenProperty, JustifyProperty, AlignProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
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
            children: kotlin.collections.List<A2uiComponentReference>,
            justify: Justify,
            align: Align,
            modifier: Modifier,
        )
    }

    /**
     * The A2UI `"List"` component for displaying a scrollable list of components.
     *
     * **Schema Properties:**
     * * `children` (ChildList, required): Defines the children. Use an array of strings for a fixed
     *   set of children, or a template object to generate children from a data list.
     * * `direction` (String Enum, optional): The direction in which the list items are laid out.
     *   Valid options: `"vertical"`, `"horizontal"`. Defaults to `"vertical"`.
     * * `align` (String Enum, optional): Defines the alignment of children along the cross axis.
     *   Valid options: `"start"`, `"center"`, `"end"`, `"stretch"`. Defaults to `"stretch"`.
     */
    public interface List : A2uiComponent {
        override val name: String
            get() = "List"

        override val description: String
            get() = "A scrollable list of components laid out vertically or horizontally."

        /** The direction in which the list items are laid out. */
        public enum class Direction(public val value: String) {
            Vertical("vertical"),
            Horizontal("horizontal");

            public companion object {
                /** The default [Direction] value. */
                public val Default: Direction = Vertical

                /** Returns the [Direction] matching [value], or [Default] if unknown. */
                public fun fromValue(value: String): Direction =
                    entries.fastFirstOrNull { it.value == value } ?: Default
            }
        }

        /** Defines the alignment of children along the cross axis. */
        public enum class Align(public val value: String) {
            Start("start"),
            Center("center"),
            End("end"),
            Stretch("stretch");

            public companion object {
                /** The default [Align] value. */
                public val Default: Align = Stretch

                /** Returns the [Align] matching [value], or [Default] if unknown. */
                public fun fromValue(value: String): Align =
                    entries.fastFirstOrNull { it.value == value } ?: Default
            }
        }

        public companion object {
            /** The [A2uiProperty] for the `"children"` property of a [List]. */
            public val ChildrenProperty: ChildListA2uiProperty =
                A2uiProperty.childList(
                    key = "children",
                    required = true,
                    description =
                        "Defines the children. Use an array of strings for a fixed set of " +
                            "children, or a template object to generate children from a data list.",
                )

            /** The [A2uiProperty] for the `"direction"` property of a [List]. */
            public val DirectionProperty: StaticA2uiProperty<Direction> =
                A2uiProperty.enum(
                    key = "direction",
                    enumValues = Direction.entries,
                    mapToString = { it.value },
                    convertFromString = Direction::fromValue,
                    defaultValue = Direction.Default,
                    description = "The direction in which the list items are laid out.",
                )

            /** The [A2uiProperty] for the `"align"` property of a [List]. */
            public val AlignProperty: StaticA2uiProperty<Align> =
                A2uiProperty.enum(
                    key = "align",
                    enumValues = Align.entries,
                    mapToString = { it.value },
                    convertFromString = Align::fromValue,
                    defaultValue = Align.Default,
                    description = "Defines the alignment of children along the cross axis.",
                )

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(ChildrenProperty, DirectionProperty, AlignProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
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
            val direction = properties[DirectionProperty] ?: Direction.Default
            val align = properties[AlignProperty] ?: Align.Default

            TypedContent(
                children = children,
                direction = direction,
                align = align,
                modifier = modifier,
            )
        }

        /**
         * Renders the [List] with its resolved [children], [direction], and [align] properties.
         *
         * @param children list of child [A2uiComponentReference]s to render in this list
         * @param direction [Direction] layout direction of the list items
         * @param align [Align] alignment of children along the cross axis
         * @param modifier [Modifier] to apply to the layout
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            children: kotlin.collections.List<A2uiComponentReference>,
            direction: Direction,
            align: Align,
            modifier: Modifier,
        )
    }

    /**
     * The A2UI `"Tabs"` component for displaying a set of tabs.
     *
     * **Schema Properties:**
     * * `tabs` (NestedList, required): An array of objects, where each object defines a tab with a
     *   `title` (Dynamic String) and a `child` (ComponentId) component ID.
     */
    public interface Tabs : A2uiComponent {
        override val name: String
            get() = "Tabs"

        override val description: String
            get() = "A set of tabs, each with a title and a corresponding child component."

        /** Represents a resolved tab with its evaluated title and child component ID. */
        @Immutable
        public class Tab(public val title: String, public val childId: String) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Tab) return false
                if (title != other.title) return false
                if (childId != other.childId) return false
                return true
            }

            override fun hashCode(): Int {
                var result = title.hashCode()
                result = 31 * result + childId.hashCode()
                return result
            }

            override fun toString(): String {
                return "Tab(title='$title', childId='$childId')"
            }
        }

        public companion object {
            /** The [A2uiProperty] for the `"title"` property of a Tab in [Tabs]. */
            public val TitleProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicString(
                    key = "title",
                    required = true,
                    description = "The tab title.",
                )

            /** The [A2uiProperty] for the `"child"` property of a Tab in [Tabs]. */
            public val ChildProperty: StaticA2uiProperty<String> =
                A2uiProperty.componentId(
                    key = "child",
                    required = true,
                    description = "The ID of the child component.",
                )

            /** The [A2uiProperty] for the `"tabs"` property of [Tabs]. */
            public val TabsProperty:
                StaticA2uiProperty<kotlin.collections.List<A2uiComponentProperties>> =
                A2uiProperty.nestedList(
                    key = "tabs",
                    properties = listOf(TitleProperty, ChildProperty),
                    required = true,
                    description =
                        "An array of objects, where each object defines a tab with a title and a " +
                            "child component.",
                    minItems = 1,
                    isAdditionalPropertiesAllowed = false,
                )

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(TabsProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
            val tabsList =
                checkNotNull(properties[TabsProperty]) {
                    "Required property '${TabsProperty.key}' is missing."
                }
            return tabsList.fastAll { tabProps -> tabProps.bind(TitleProperty) != null }
        }

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val tabsList =
                checkNotNull(properties[TabsProperty]) {
                    "Required property '${TabsProperty.key}' is missing."
                }

            val resolvedTabs = ArrayList<Tab>(tabsList.size)
            for (i in tabsList.indices) {
                val tabProps = tabsList[i]
                val title =
                    checkNotNull(tabProps.bind(TitleProperty)) {
                        "Required property '${TitleProperty.key}' is missing."
                    }
                val childId =
                    checkNotNull(tabProps[ChildProperty]) {
                        "Required property '${ChildProperty.key}' is missing."
                    }
                resolvedTabs.add(Tab(title, childId))
            }

            TypedContent(tabs = resolvedTabs, modifier = modifier)
        }

        /**
         * Renders the [Tabs] with its resolved [tabs].
         *
         * @param tabs list of [Tab] objects to render
         * @param modifier [Modifier] to apply to the layout
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            tabs: kotlin.collections.List<Tab>,
            modifier: Modifier,
        )
    }

    /**
     * The A2UI `"Button"` component for clickable buttons that dispatch actions.
     *
     * **Schema Properties:**
     * * `child` (Component ID String, required): The ID of the child component inside the button
     *   (e.g., a `"Text"` or `"Icon"` component).
     * * `variant` (String Enum, optional): A hint for the button style. This is a static
     *   configuration and does not support dynamic data bindings. Valid options: `"default"`,
     *   `"primary"`, `"borderless"`. Defaults to `"default"`.
     * * `action` (Action Object, required): The action payload dispatched when the button is
     *   clicked.
     */
    public interface Button : A2uiComponent {
        override val name: String
            get() = "Button"

        override val description: String
            get() = "A clickable button that dispatches an action."

        /** Visual style variant for a [Button]. */
        public enum class Variant(public val value: String) {
            Default("default"),
            Primary("primary"),
            Borderless("borderless");

            public companion object {
                /** Returns the [Variant] matching [value], or [Default] if unknown. */
                public fun fromValue(value: String): Variant =
                    entries.fastFirstOrNull { it.value == value } ?: Default
            }
        }

        public companion object {
            /** The [A2uiProperty] for the `"child"` property of a [Button]. */
            public val ChildProperty: StaticA2uiProperty<String> =
                A2uiProperty.componentId(
                    key = "child",
                    required = true,
                    description =
                        "The ID of the child component. Use a 'Text' component for a labeled " +
                            "button. Only use an 'Icon' if the requirements explicitly ask for " +
                            "an icon-only button.",
                )

            /** The [A2uiProperty] for the `"variant"` property of a [Button]. */
            public val VariantProperty: StaticA2uiProperty<Variant> =
                A2uiProperty.enum(
                    key = "variant",
                    enumValues = Variant.entries,
                    mapToString = { it.value },
                    convertFromString = Variant::fromValue,
                    defaultValue = Variant.Default,
                    description =
                        "A hint for the button style. If omitted, a default button style is " +
                            "used. 'primary' indicates this is the main call-to-action button. " +
                            "'borderless' means the button has no visual border or background, " +
                            "making its child content appear like a clickable link.",
                )

            /** The [A2uiProperty] for the `"action"` property of a [Button]. */
            public val ActionProperty: StaticA2uiProperty<Map<String, Any?>> =
                A2uiProperty.action(key = "action", required = true)

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(ChildProperty, VariantProperty, ActionProperty)
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean = true

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val childId =
                checkNotNull(properties[ChildProperty]) {
                    "Required property '${ChildProperty.key}' is missing."
                }
            val variant = properties[VariantProperty] ?: Variant.Default
            val action =
                checkNotNull(properties[ActionProperty]) {
                    "Required property '${ActionProperty.key}' is missing."
                }

            TypedContent(childId = childId, variant = variant, action = action, modifier = modifier)
        }

        /**
         * Renders the [Button] with its resolved [childId], [variant], and [action] properties.
         *
         * @param childId ID of the child component to render inside this button
         * @param variant [Variant] visual style variant of the button
         * @param action action payload [Map] dispatched when clicked
         * @param modifier [Modifier] to apply to the layout
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            childId: String,
            variant: Variant,
            action: Map<String, Any?>,
            modifier: Modifier,
        )
    }

    /**
     * The A2UI `"DateTimeInput"` component for selecting date and/or time.
     *
     * **Schema Properties:**
     * * `value` (Dynamic String, required): The selected date and/or time value in ISO 8601 format.
     *   If not yet set, initialize with an empty string.
     * * `enableDate` (Boolean, optional): If true, allows the user to select a date. Defaults to
     *   `false`.
     * * `enableTime` (Boolean, optional): If true, allows the user to select a time. Defaults to
     *   `false`.
     * * `min` (Dynamic String, optional): The minimum allowed date/time in ISO 8601 format.
     * * `max` (Dynamic String, optional): The maximum allowed date/time in ISO 8601 format.
     * * `label` (Dynamic String, optional): The text label for the component.
     */
    public interface DateTimeInput : A2uiComponent {
        override val name: String
            get() = "DateTimeInput"

        override val description: String
            get() = "Allows the user to select a date and/or time."

        public companion object {
            /** The [A2uiProperty] for the `"value"` property of a [DateTimeInput]. */
            public val ValueProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicString(
                    key = "value",
                    required = true,
                    description =
                        "The selected date and/or time value in ISO 8601 format. " +
                            "If not yet set, initialize with an empty string.",
                )

            /** The [A2uiProperty] for the `"enableDate"` property of a [DateTimeInput]. */
            public val EnableDateProperty: StaticA2uiProperty<Boolean> =
                A2uiProperty.booleanWithDefault(
                    key = "enableDate",
                    defaultValue = false,
                    description = "If true, allows the user to select a date.",
                )

            /** The [A2uiProperty] for the `"enableTime"` property of a [DateTimeInput]. */
            public val EnableTimeProperty: StaticA2uiProperty<Boolean> =
                A2uiProperty.booleanWithDefault(
                    key = "enableTime",
                    defaultValue = false,
                    description = "If true, allows the user to select a time.",
                )

            /** The [A2uiProperty] for the `"min"` property of a [DateTimeInput]. */
            public val MinProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicCustom(
                    key = "min",
                    schema =
                        A2uiAnySchema(
                            description = "The minimum allowed date/time in ISO 8601 format.",
                            keywords =
                                listOf(
                                    A2uiSchemaKeyword.AllOf(
                                        // TODO(b/553193771): Support the `format` schema values
                                        //  once supported in the schema API.
                                        listOf(A2uiDynamicStringSchema.DEFAULT_INSTANCE)
                                    )
                                ),
                        ),
                    safeCast = { it as? String },
                    required = false,
                )

            /** The [A2uiProperty] for the `"max"` property of a [DateTimeInput]. */
            public val MaxProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicCustom(
                    key = "max",
                    schema =
                        A2uiAnySchema(
                            description = "The maximum allowed date/time in ISO 8601 format.",
                            keywords =
                                listOf(
                                    A2uiSchemaKeyword.AllOf(
                                        // TODO(b/553193771): Support the `format` schema values
                                        //  once supported in the schema API.
                                        listOf(A2uiDynamicStringSchema.DEFAULT_INSTANCE)
                                    )
                                ),
                        ),
                    safeCast = { it as? String },
                    required = false,
                )

            /** The [A2uiProperty] for the `"label"` property of a [DateTimeInput]. */
            public val LabelProperty: DynamicA2uiProperty<String> =
                A2uiProperty.dynamicString(
                    key = "label",
                    required = false,
                    description = "The text label for the component.",
                )

            internal val ComponentProperties: kotlin.collections.List<A2uiProperty<*>> =
                listOf(
                    ValueProperty,
                    EnableDateProperty,
                    EnableTimeProperty,
                    MinProperty,
                    MaxProperty,
                    LabelProperty,
                )
        }

        override val properties: kotlin.collections.List<A2uiProperty<*>>
            get() = ComponentProperties

        @Composable
        override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
            val value = properties.bind(ValueProperty) ?: return false
            if (value.isEmpty()) return true
            val valueMillis = parseIsoDateTimeToUtcMillis(value)
            if (valueMillis == null) {
                SideEffect(value) {
                    reportError(
                        A2uiException.A2uiRuntimeException("Invalid date-time format: $value")
                    )
                }
                return false
            }
            return true
        }

        @Composable
        override fun A2uiComponentScope.Content(
            properties: A2uiComponentProperties,
            modifier: Modifier,
        ) {
            val value =
                checkNotNull(properties.bind(ValueProperty)) {
                    "Required property '${ValueProperty.key}' is missing."
                }
            val valueMillis =
                if (value.isNotEmpty()) {
                    checkNotNull(remember(value) { parseIsoDateTimeToUtcMillis(value) }) {
                        "Required property '${ValueProperty.key}' could not be parsed: '$value'."
                    }
                } else {
                    null
                }

            val onValueChange = properties.bindUpdater(ValueProperty)
            val enableDate = properties[EnableDateProperty] ?: false
            val enableTime = properties[EnableTimeProperty] ?: false
            val min = properties.bind(MinProperty)
            val max = properties.bind(MaxProperty)
            val label = properties.bind(LabelProperty)

            val minMillis = remember(min) { parseIsoDateTimeToUtcMillis(min) }
            val maxMillis = remember(max) { parseIsoDateTimeToUtcMillis(max) }

            val onValueChangeMillis: ((Long?) -> Unit)? =
                if (onValueChange != null) {
                    { newMillis ->
                        val formatted = formatUtcMillisToIso(newMillis, enableDate, enableTime)
                        onValueChange(formatted)
                    }
                } else null

            TypedContent(
                value = valueMillis,
                onValueChange = onValueChangeMillis,
                enableDate = enableDate,
                enableTime = enableTime,
                min = minMillis,
                max = maxMillis,
                label = label,
                modifier = modifier,
            )
        }

        /**
         * Renders the [DateTimeInput] component with resolved property values.
         *
         * @param value selected date and/or time in UTC epoch milliseconds, or `null` if not yet
         *   set
         * @param onValueChange callback invoked when the user selects a date or time in UTC epoch
         *   milliseconds, or `null` if the input is read-only
         * @param enableDate whether date selection is enabled
         * @param enableTime whether time selection is enabled
         * @param min minimum allowed date/time in UTC epoch milliseconds, or `null` if unbounded
         * @param max maximum allowed date/time in UTC epoch milliseconds, or `null` if unbounded
         * @param label text label describing the input, or `null` if none
         * @param modifier [Modifier] applied to the component layout
         */
        @Composable
        public fun A2uiComponentScope.TypedContent(
            @Suppress("AutoBoxing") value: Long?,
            onValueChange: ((Long?) -> Unit)?,
            enableDate: Boolean,
            enableTime: Boolean,
            @Suppress("AutoBoxing") min: Long?,
            @Suppress("AutoBoxing") max: Long?,
            label: String?,
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

private val dateTimePatterns =
    arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd",
        "HH:mm:ss.SSSX",
        "HH:mm:ss.SSS",
        "HH:mm:ssX",
        "HH:mm:ss",
        "HH:mm",
    )

internal fun parseIsoDateTimeToUtcMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    val utcZone = TimeZone.getTimeZone("UTC")

    for (pattern in dateTimePatterns) {
        try {
            val parser =
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = utcZone
                    isLenient = false
                }
            val date = parser.parse(trimmed) ?: continue
            return date.time
        } catch (_: ParseException) {
            // Ignore and try the next pattern
        }
    }
    return null
}

internal fun formatUtcMillisToIso(
    millis: Long?,
    enableDate: Boolean,
    enableTime: Boolean,
): String? {
    if (millis == null) return null
    val utcZone = TimeZone.getTimeZone("UTC")
    val date = Date(millis)
    val pattern =
        when {
            enableDate && !enableTime -> "yyyy-MM-dd"
            !enableDate && enableTime -> "HH:mm:ss"
            else -> "yyyy-MM-dd'T'HH:mm:ss'Z'"
        }
    val formatter = SimpleDateFormat(pattern, Locale.US).apply { timeZone = utcZone }
    return formatter.format(date)
}
