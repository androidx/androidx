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

package androidx.glance.adaptive.core.templates.list

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.action.GlanceAdaptiveAction
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.ListItem
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.slots.TrailingComponent
import androidx.glance.adaptive.core.templates.ClickableScope
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplateDsl
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.TextScope
import androidx.glance.adaptive.core.templates.buildTemplateText

/**
 * Builds a [ListTemplate] using scoped slot blocks.
 *
 * Populates [ListTemplateScope.items], with optional header slots [ListTemplateScope.leading],
 * [ListTemplateScope.title], and [ListTemplateScope.trailing]:
 * ```kotlin
 * val template = listTemplate {
 *     leading { icon(TemplateIcon.fromResource(R.drawable.ic_calendar)) }
 *     title { text("Oct 6, TUE") }
 *     trailing {
 *         iconButton(TemplateIcon.fromResource(R.drawable.ic_add), "Create event") {
 *             onClick(createEvent)
 *         }
 *     }
 *     items {
 *         item("Widget Weekly Sync") {
 *             subtitle("10:30 - 11:30")
 *             tertiaryText("204-Phone-Room")
 *             trailingChip("Now")
 *             onClick(openEvent)
 *         }
 *     }
 * }
 * ```
 *
 * @param content DSL block configuring the slots of the [ListTemplate]
 * @return validated [ListTemplate] instance
 * @throws IllegalArgumentException if a component argument is invalid, or [ListTemplateScope.items]
 *   is missing, empty, contains only dividers, or has more than 25 items
 * @throws IllegalStateException if a header slot block is empty or any slot block is configured
 *   more than once
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun listTemplate(content: ListTemplateScope.() -> Unit): ListTemplate =
    ListTemplateScope().apply(content).build()

/** Configures the semantic slots of a [ListTemplate]. */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListTemplateScope internal constructor() {
    private var leading: LeadingComponent? = null
    private var title: PrimaryComponent? = null
    private var trailing: TrailingComponent? = null
    private var compactTitle: TemplateText? = null
    private var items: List<ListItem> = emptyList()
    private var scrollableOverride: Boolean? = null
    private var showHeaderInCompactHeightOverride: Boolean? = null

    /**
     * Configures the header's optional leading icon slot.
     *
     * @param content block adding a single component in [LeadingScope]
     * @throws IllegalStateException if [leading] was already called or [content] adds no component
     */
    public fun leading(content: LeadingScope.() -> Unit) {
        check(leading == null) { "The leading slot was already configured." }
        val scope = LeadingScope().apply(content)
        leading =
            checkNotNull(scope.component) {
                "The leading block must contain a component (for example, icon(...))."
            }
    }

    /**
     * Configures the header's optional title slot.
     *
     * @param content block adding a single component in [TitleScope]
     * @throws IllegalStateException if [title] was already called or [content] adds no component
     */
    public fun title(content: TitleScope.() -> Unit) {
        check(title == null) { "The title slot was already configured." }
        val scope = TitleScope().apply(content)
        title =
            checkNotNull(scope.component) {
                "The title block must contain a component (for example, text(...))."
            }
    }

    /**
     * Configures the header's optional trailing button, shown where the widget has room.
     *
     * @param content block adding a single component in [TrailingScope]
     * @throws IllegalStateException if [trailing] was already called or [content] adds no component
     */
    public fun trailing(content: TrailingScope.() -> Unit) {
        check(trailing == null) { "The trailing slot was already configured." }
        val scope = TrailingScope().apply(content)
        trailing =
            checkNotNull(scope.component) {
                "The trailing block must contain a component (for example, iconButton(...))."
            }
    }

    /**
     * Configures the required list items, 1 to 25, ordered by priority.
     *
     * Non-scrollable surfaces keep the earlier items and drop trailing ones.
     *
     * @param content block adding 1 to 25 items in [ItemsScope]
     * @throws IllegalArgumentException if [content] adds no items (and [listTemplate] throws if
     *   only dividers or more than 25 items are added)
     * @throws IllegalStateException if [items] was already called
     */
    public fun items(content: ItemsScope.() -> Unit) {
        check(items.isEmpty()) { "The items slot was already configured." }
        items = ItemsScope().apply(content).build()
    }

    /**
     * Configures whether the list may scroll on scrollable surfaces (defaults to `true`;
     * non-scrollable surfaces always slice).
     *
     * @param value whether vertical scrolling is enabled on scrollable surfaces
     * @throws IllegalStateException if [scrollable] was already called
     */
    public fun scrollable(value: Boolean = true) {
        check(scrollableOverride == null) { "Scrollable was already configured." }
        scrollableOverride = value
    }

    /**
     * Configures whether the header row remains visible at compact heights (defaults to `true`).
     *
     * @param value whether to keep the header visible at compact heights
     * @throws IllegalStateException if [showHeaderInCompactHeight] was already called
     */
    public fun showHeaderInCompactHeight(value: Boolean = true) {
        check(showHeaderInCompactHeightOverride == null) {
            "showHeaderInCompactHeight was already configured."
        }
        showHeaderInCompactHeightOverride = value
    }

    /**
     * Configures an optional short title (for example, `"10/6"`) used in narrow headers or prefixed
     * to the first item when the header is hidden at compact heights.
     *
     * @param value non-blank compact title text
     * @param options optional block configuring [TextScope] settings such as [TextScope.sensitive]
     * @throws IllegalArgumentException if [value] is blank
     * @throws IllegalStateException if [compactTitle] was already called
     */
    public fun compactTitle(value: String, options: TextScope.() -> Unit = {}) {
        check(compactTitle == null) { "The compact title was already configured." }
        compactTitle = buildTemplateText(value, options)
    }

    internal fun build(): ListTemplate =
        ListTemplate(
            items = items,
            leading = leading,
            title = title,
            trailing = trailing,
            isScrollable = scrollableOverride ?: true,
            showHeaderInCompactHeight = showHeaderInCompactHeightOverride ?: true,
            compactTitle = compactTitle,
        )

    /** Populates the [ListTemplateScope.leading] slot of a [ListTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class LeadingScope internal constructor() {
        internal var component: LeadingComponent? = null

        /**
         * Places the header's icon in the leading slot.
         *
         * @param icon [TemplateIcon] of the header's glyph
         * @param contentDescription optional accessibility description for standalone header icons
         *   when [title] is absent
         * @throws IllegalArgumentException if [contentDescription] is blank
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun icon(icon: TemplateIcon, contentDescription: String? = null) {
            check(component == null) { "The leading slot accepts only one component." }
            component = Icon(icon = icon.resId, contentDescription = contentDescription)
        }
    }

    /** Populates the [ListTemplateScope.title] slot of a [ListTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class TitleScope internal constructor() {
        internal var component: PrimaryComponent? = null

        /**
         * Places the header's title text in the title slot.
         *
         * @param value non-blank header title
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun text(value: String, options: TextScope.() -> Unit = {}) {
            check(component == null) { "The title slot accepts only one component." }
            component = buildTemplateText(value, options)
        }
    }

    /** Populates the [ListTemplateScope.trailing] button slot of a [ListTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class TrailingScope internal constructor() {
        internal var component: TrailingComponent? = null

        /**
         * Places an icon button at the header's end.
         *
         * @param icon [TemplateIcon] of the button's glyph
         * @param label non-blank name of what the button does, announced by accessibility services
         * @param contained whether the glyph sits inside a filled container rather than drawing
         *   bare (defaults to `true`)
         * @param options optional block configuring [ClickableScope.onClick] and
         *   [ClickableScope.sensitive]
         * @throws IllegalArgumentException if [label] is blank
         * @throws IllegalStateException if a component was already added to this slot
         */
        public fun iconButton(
            icon: TemplateIcon,
            label: String,
            contained: Boolean = true,
            options: ClickableScope.() -> Unit = {},
        ) {
            check(component == null) { "The trailing slot accepts only one component." }
            require(label.isNotBlank()) { "An icon button label must not be blank." }
            val scope = ClickableScope().apply(options)
            component =
                IconButton(
                    icon = icon.resId,
                    contentDescription = label,
                    action = scope.action,
                    sensitiveReplacement = scope.sensitiveReplacement,
                    contained = contained,
                )
        }
    }

    /** Populates the [ListTemplateScope.items] list of a [ListTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ItemsScope internal constructor() {
        private val items: MutableList<ListItem> = mutableListOf()

        /**
         * Appends a list item with a required [title] and optional metadata configured in
         * [options].
         *
         * @param title non-blank item headline (for example, `"Widget Weekly Sync"`)
         * @param options optional block configuring [ItemScope] properties
         * @throws IllegalArgumentException if [title] is blank
         */
        public fun item(title: String, options: ItemScope.() -> Unit = {}) {
            require(title.isNotBlank()) { "A list item title must not be blank." }
            val scope = ItemScope().apply(options)
            val struck = scope.isStruckThrough
            items.add(
                ListItem(
                    title =
                        TemplateText(
                            value = title,
                            sensitiveReplacement = scope.clickable.sensitiveReplacement,
                            isStruckThrough = struck,
                        ),
                    subtitle =
                        scope.subtitle?.withDefaultStruckThrough(
                            explicit = scope.subtitleStruckThroughExplicit,
                            itemStruckThrough = struck,
                        ),
                    tertiaryText =
                        scope.tertiaryText?.withDefaultStruckThrough(
                            explicit = scope.tertiaryTextStruckThroughExplicit,
                            itemStruckThrough = struck,
                        ),
                    leadingIcon = scope.leadingIcon,
                    trailingIcon = scope.trailingIcon,
                    trailingChip = scope.trailingChip,
                    action = scope.clickable.action,
                    style = scope.style,
                )
            )
        }

        /**
         * Appends a non-clickable section divider row with [label] (for example, `"TOMORROW"`).
         *
         * @param label non-blank divider text
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [label] is blank
         */
        public fun divider(label: String, options: TextScope.() -> Unit = {}) {
            require(label.isNotBlank()) { "A list divider label must not be blank." }
            items.add(ListItem(title = buildTemplateText(label, options), isDivider = true))
        }

        internal fun build(): List<ListItem> {
            require(items.isNotEmpty()) {
                "The items block must contain at least one item (for example, item(...))."
            }
            return items.toList()
        }
    }

    /**
     * Configures optional settings on list item text fields that support both lock-screen redaction
     * and strikethrough styling (such as [ItemScope.subtitle] and [ItemScope.tertiaryText]).
     */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ItemTextScope internal constructor() {
        private val textScope: TextScope = TextScope()

        internal val sensitiveReplacement: String?
            get() = textScope.sensitiveReplacement

        internal var isStruckThrough: Boolean? = null
            private set

        /**
         * Replaces the text value with [replacement] on locked or privacy-restricted surfaces.
         *
         * @param replacement non-blank placeholder string shown when content is redacted
         * @throws IllegalArgumentException if [replacement] is blank
         * @throws IllegalStateException if [sensitive] was already called in this scope
         */
        public fun sensitive(replacement: String) {
            textScope.sensitive(replacement)
        }

        /**
         * Renders the text with strikethrough (for example, a declined event or completed task).
         *
         * @param value whether to apply strikethrough (defaults to `true`)
         * @throws IllegalStateException if [strikethrough] was already called in this scope
         */
        public fun strikethrough(value: Boolean = true) {
            check(isStruckThrough == null) { "Strikethrough was already set." }
            isStruckThrough = value
        }
    }

    /** Configures an individual item inside [ItemsScope]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ItemScope internal constructor() {
        internal val clickable: ClickableScope = ClickableScope()
        internal var subtitle: TemplateText? = null
            private set

        internal var subtitleStruckThroughExplicit: Boolean? = null
            private set

        internal var tertiaryText: TemplateText? = null
            private set

        internal var tertiaryTextStruckThroughExplicit: Boolean? = null
            private set

        internal var leadingIcon: Icon? = null
            private set

        internal var trailingIcon: Icon? = null
            private set

        internal var trailingChip: Badge? = null
            private set

        internal var style: ListItemStyle = ListItemStyle.Default
            private set

        private var styleSet: Boolean = false
        internal var isStruckThrough: Boolean = false
            private set

        private var strikethroughSet: Boolean = false

        /**
         * Adds a secondary label on the item, such as a time range (`"10:30 - 11:30"`), shown above
         * the title on focused rows and inline before the title on compact rows.
         *
         * @param value non-blank subtitle
         * @param options optional block configuring [ItemTextScope] settings such as
         *   [ItemTextScope.sensitive] or [ItemTextScope.strikethrough]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if [subtitle] was already called in this scope
         */
        public fun subtitle(value: String, options: ItemTextScope.() -> Unit = {}) {
            check(subtitle == null) { "Subtitle was already set." }
            val (text, explicit) = buildItemTemplateText(value, options)
            subtitleStruckThroughExplicit = explicit
            subtitle = text
        }

        /**
         * Adds a third line of detail, such as a location (`"204-Phone-Room"`), shown on the
         * featured first item where height allows.
         *
         * @param value non-blank detail text
         * @param options optional block configuring [ItemTextScope] settings such as
         *   [ItemTextScope.sensitive] or [ItemTextScope.strikethrough]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if [tertiaryText] was already called in this scope
         */
        public fun tertiaryText(value: String, options: ItemTextScope.() -> Unit = {}) {
            check(tertiaryText == null) { "Tertiary text was already set." }
            val (text, explicit) = buildItemTemplateText(value, options)
            tertiaryTextStruckThroughExplicit = explicit
            tertiaryText = text
        }

        /**
         * Sets a leading icon before the item's text column (for example, an OOO or status icon).
         *
         * @param icon [TemplateIcon] to display at the leading edge
         * @param contentDescription optional accessibility description
         * @throws IllegalArgumentException if [contentDescription] is blank
         * @throws IllegalStateException if [leadingIcon] was already called in this scope
         */
        public fun leadingIcon(icon: TemplateIcon, contentDescription: String? = null) {
            check(leadingIcon == null) { "Item leading icon was already set." }
            leadingIcon = Icon(icon = icon.resId, contentDescription = contentDescription)
        }

        /**
         * Sets a trailing icon on the item. Mutually exclusive with [trailingChip].
         *
         * @param icon [TemplateIcon] to display at the trailing edge
         * @param contentDescription optional accessibility description
         * @param contained whether the icon sits inside a filled circle rather than drawing bare
         *   (defaults to `false`)
         * @throws IllegalArgumentException if [contentDescription] is blank
         * @throws IllegalStateException if a trailing visual was already set in this scope
         */
        public fun trailingIcon(
            icon: TemplateIcon,
            contentDescription: String? = null,
            contained: Boolean = false,
        ) {
            check(trailingIcon == null && trailingChip == null) {
                "Item trailing visual was already set."
            }
            trailingIcon =
                Icon(
                    icon = icon.resId,
                    contentDescription = contentDescription,
                    contained = contained,
                )
        }

        /**
         * Sets a trailing status chip on the item (for example, `"Now"`). Mutually exclusive with
         * [trailingIcon].
         *
         * @param label non-blank chip text
         * @throws IllegalArgumentException if [label] is blank
         * @throws IllegalStateException if a trailing visual was already set in this scope
         */
        public fun trailingChip(label: String) {
            check(trailingIcon == null && trailingChip == null) {
                "Item trailing visual was already set."
            }
            trailingChip = Badge(label = label)
        }

        /**
         * Replaces the item's title with [replacement] on locked or privacy-restricted surfaces.
         *
         * @param replacement non-blank placeholder string shown when content is redacted
         * @throws IllegalArgumentException if [replacement] is blank
         * @throws IllegalStateException if [sensitive] was already called in this scope
         */
        public fun sensitive(replacement: String) {
            clickable.sensitive(replacement)
        }

        /**
         * Sets the visual container prominence [style] for this item (defaults to
         * [ListItemStyle.Default]).
         *
         * @param style container prominence style
         * @throws IllegalStateException if [style] was already called in this scope
         */
        public fun style(style: ListItemStyle) {
            check(!styleSet) { "Item container style was already set." }
            styleSet = true
            this.style = style
        }

        /**
         * Renders the item's text with strikethrough (for example, a declined event or completed
         * task). Individual [subtitle] or [tertiaryText] entries inherit this setting unless they
         * explicitly call [ItemTextScope.strikethrough].
         *
         * @param value whether to apply strikethrough (defaults to `true`)
         * @throws IllegalStateException if [strikethrough] was already called in this scope
         */
        public fun strikethrough(value: Boolean = true) {
            check(!strikethroughSet) { "Item strikethrough was already set." }
            strikethroughSet = true
            isStruckThrough = value
        }

        /**
         * Runs [action] when the item is tapped.
         *
         * @param action what the tap does
         * @throws IllegalStateException if [onClick] was already called in this scope
         */
        public fun onClick(action: GlanceAdaptiveAction) {
            clickable.onClick(action)
        }
    }
}

private fun buildItemTemplateText(
    value: String,
    options: ListTemplateScope.ItemTextScope.() -> Unit,
): Pair<TemplateText, Boolean?> {
    require(value.isNotBlank()) { "Text must not be blank." }
    val textScope = ListTemplateScope.ItemTextScope().apply(options)
    return TemplateText(
        value = value,
        sensitiveReplacement = textScope.sensitiveReplacement,
        isStruckThrough = textScope.isStruckThrough ?: false,
    ) to textScope.isStruckThrough
}

private fun TemplateText.withDefaultStruckThrough(
    explicit: Boolean?,
    itemStruckThrough: Boolean,
): TemplateText {
    val effective = explicit ?: itemStruckThrough
    return if (effective == isStruckThrough) {
        this
    } else {
        TemplateText(value, sensitiveReplacement, isStruckThrough = effective)
    }
}
