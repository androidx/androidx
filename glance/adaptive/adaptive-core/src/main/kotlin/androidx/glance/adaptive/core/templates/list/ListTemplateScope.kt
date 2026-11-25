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
 * Populates [ListTemplateScope.items] (`item`, `checkboxItem`, `detailCard`, and `divider`), with
 * optional header slots [ListTemplateScope.leading], [ListTemplateScope.title], and either
 * [ListTemplateScope.trailing] (header utility action) or [ListTemplateScope.actions] (adaptive
 * quick-action FAB / rail):
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
 * @throws IllegalArgumentException if a component argument is invalid, [ListTemplateScope.items] is
 *   missing, empty, contains only dividers, or has more than 25 items, or
 *   [ListTemplateScope.actions] is empty or has more than 6 actions
 * @throws IllegalStateException if a header slot block is empty, any slot block is configured more
 *   than once, or both [ListTemplateScope.trailing] and [ListTemplateScope.actions] are configured
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
    private var actions: List<IconButton> = emptyList()
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
     * Configures the header's optional trailing utility button, shown where the widget has room.
     * Mutually exclusive with [actions].
     *
     * @param content block adding a single component in [TrailingScope]
     * @throws IllegalStateException if [trailing] was already called, [actions] was already
     *   configured, or [content] adds no component
     */
    public fun trailing(content: TrailingScope.() -> Unit) {
        check(trailing == null) { "The trailing slot was already configured." }
        check(actions.isEmpty()) { "The trailing and actions slots cannot both be configured." }
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
     * Configures up to 6 optional adaptive quick actions, ordered by priority. Mutually exclusive
     * with [trailing].
     *
     * Compact sizes display the first action as a bottom-right floating action button, while
     * expanded sizes (`W3–W4 × H3+`) display a trailing vertical action rail led by the first
     * action.
     *
     * @param content block adding 1 to 6 actions in [ActionsScope]
     * @throws IllegalArgumentException if [content] adds no actions (and [listTemplate] throws if
     *   more than 6 actions are added)
     * @throws IllegalStateException if [actions] was already called or [trailing] was already
     *   configured
     */
    public fun actions(content: ActionsScope.() -> Unit) {
        check(actions.isEmpty()) { "The actions slot was already configured." }
        check(trailing == null) { "The trailing and actions slots cannot both be configured." }
        actions = ActionsScope().apply(content).build()
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
     * Configures an optional short title (for example, `"10/6"` or `"Brief"`) shown in narrow `W1`
     * headers or prefixed to the first item when the header is hidden at compact heights.
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
            actions = actions,
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
            component = buildTemplateText(value, options = options)
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

    /** Populates the [ListTemplateScope.actions] quick-action slot of a [ListTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ActionsScope internal constructor() {
        private val actions: MutableList<IconButton> = mutableListOf()

        /**
         * Appends a quick action button. Index `0` is always styled as the primary button (the
         * bottom-right floating action button on compact sizes and the top pill button of the
         * trailing action rail on expanded sizes), while subsequent actions render as uncontained
         * secondary rail icons.
         *
         * @param icon [TemplateIcon] of the action's glyph
         * @param label non-blank accessibility label describing the action
         * @param options optional block configuring [ClickableScope.onClick] and
         *   [ClickableScope.sensitive]
         * @throws IllegalArgumentException if [label] is blank
         */
        public fun action(
            icon: TemplateIcon,
            label: String,
            options: ClickableScope.() -> Unit = {},
        ) {
            require(label.isNotBlank()) { "An action label must not be blank." }
            val scope = ClickableScope().apply(options)
            actions.add(
                IconButton(
                    icon = icon.resId,
                    contentDescription = label,
                    action = scope.action,
                    sensitiveReplacement = scope.sensitiveReplacement,
                )
            )
        }

        internal fun build(): List<IconButton> {
            require(actions.isNotEmpty()) {
                "The actions block must contain at least one action (for example, action(...))."
            }
            return actions
        }
    }

    /** Populates the [ListTemplateScope.items] list of a [ListTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ItemsScope internal constructor() {
        private val items: MutableList<ListItem> = mutableListOf()

        /**
         * Appends a compact list item with a required [title] and optional metadata configured in
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
                ListItem.Compact(
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
                    tertiaryIcon = scope.tertiaryIcon,
                    leadingIcon = scope.leadingIcon,
                    trailingIcon = scope.trailingIcon,
                    trailingChip = scope.trailingChip,
                    action = scope.clickable.action,
                    style = scope.style,
                )
            )
        }

        /**
         * Appends a standalone checkable list row with [title] and [isChecked] state.
         *
         * Checked rows render with strikethrough by default unless
         * [CheckboxItemScope.strikethrough] explicitly overrides it.
         *
         * @param title non-blank label text
         * @param isChecked whether the checkbox is checked (defaults to `false`)
         * @param options optional block configuring [CheckboxItemScope] settings
         * @throws IllegalArgumentException if [title] is blank
         */
        public fun checkboxItem(
            title: String,
            isChecked: Boolean = false,
            options: CheckboxItemScope.() -> Unit = {},
        ) {
            require(title.isNotBlank()) { "A checkbox item title must not be blank." }
            val scope = CheckboxItemScope().apply(options)
            items.add(
                ListItem.Checkbox(
                    title =
                        TemplateText(
                            value = title,
                            sensitiveReplacement = scope.clickable.sensitiveReplacement,
                            isStruckThrough = scope.isStruckThrough ?: isChecked,
                        ),
                    isChecked = isChecked,
                    action = scope.clickable.action,
                )
            )
        }

        /**
         * Appends a multi-line detail card with [title] and optional body text or read-only inline
         * checklist preview configured in [options].
         *
         * @param title non-blank card headline
         * @param options optional block configuring [DetailCardScope] properties
         * @throws IllegalArgumentException if [title] is blank
         */
        public fun detailCard(title: String, options: DetailCardScope.() -> Unit = {}) {
            require(title.isNotBlank()) { "A detail card title must not be blank." }
            val scope = DetailCardScope().apply(options)
            items.add(
                ListItem.DetailCard(
                    title =
                        TemplateText(
                            value = title,
                            sensitiveReplacement = scope.clickable.sensitiveReplacement,
                        ),
                    body = scope.body,
                    checklistItems = scope.checklistItems,
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
            items.add(ListItem.Divider(title = buildTemplateText(label, options = options)))
        }

        internal fun build(): List<ListItem> {
            require(items.isNotEmpty()) {
                "The items block must contain at least one item (for example, item(...))."
            }
            return items
        }
    }

    /**
     * Configures optional settings on list item text fields that support both lock-screen redaction
     * and strikethrough styling (such as [ItemScope.subtitle], [ItemScope.tertiaryText],
     * [DetailCardScope.body], and [ChecklistScope.item]).
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

    /** Configures an individual compact item inside [ItemsScope]. */
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

        internal var tertiaryIcon: Icon? = null
            private set

        internal var leadingIcon: Icon? = null
            private set

        internal var trailingIcon: Icon? = null
            private set

        internal var trailingChip: Badge? = null
            private set

        private var styleOverride: ListItemStyle? = null
        internal val style: ListItemStyle
            get() = styleOverride ?: ListItemStyle.Default

        private var struckThroughOverride: Boolean? = null
        internal val isStruckThrough: Boolean
            get() = struckThroughOverride ?: false

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
            val (text, explicit) = buildItemTemplateText(value, options = options)
            subtitle = text
            subtitleStruckThroughExplicit = explicit
        }

        /**
         * Adds a third line of detail, such as a location (`"204-Phone-Room"`), shown on the
         * featured first item where height allows.
         *
         * @param value non-blank detail text
         * @param icon optional small [TemplateIcon] drawn before [value]
         * @param options optional block configuring [ItemTextScope] settings such as
         *   [ItemTextScope.sensitive] or [ItemTextScope.strikethrough]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if [tertiaryText] was already called in this scope
         */
        public fun tertiaryText(
            value: String,
            icon: TemplateIcon? = null,
            options: ItemTextScope.() -> Unit = {},
        ) {
            check(tertiaryText == null) { "Tertiary text was already set." }
            val (text, explicit) = buildItemTemplateText(value, options = options)
            tertiaryText = text
            tertiaryTextStruckThroughExplicit = explicit
            tertiaryIcon = icon?.let { Icon(icon = it.resId) }
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
            check(styleOverride == null) { "Item container style was already set." }
            styleOverride = style
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
            check(struckThroughOverride == null) { "Item strikethrough was already set." }
            struckThroughOverride = value
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

    /** Configures an individual [ListItem.Checkbox] inside [ItemsScope]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class CheckboxItemScope internal constructor() {
        internal val clickable: ClickableScope = ClickableScope()
        internal var isStruckThrough: Boolean? = null
            private set

        /**
         * Replaces the checkbox label with [replacement] on locked or privacy-restricted surfaces.
         *
         * @param replacement non-blank placeholder string shown when content is redacted
         * @throws IllegalArgumentException if [replacement] is blank
         * @throws IllegalStateException if [sensitive] was already called in this scope
         */
        public fun sensitive(replacement: String) {
            clickable.sensitive(replacement)
        }

        /**
         * Explicitly controls strikethrough on the checkbox label (defaults to matching
         * `isChecked`).
         *
         * @param value whether to apply strikethrough (defaults to `true`)
         * @throws IllegalStateException if [strikethrough] was already called in this scope
         */
        public fun strikethrough(value: Boolean = true) {
            check(isStruckThrough == null) { "Checkbox strikethrough was already set." }
            isStruckThrough = value
        }

        /**
         * Runs [action] when the checkbox row is tapped (for example, a
         * [GlanceAdaptiveAction.CallbackAction] toggling checked state).
         *
         * @param action what the tap does
         * @throws IllegalStateException if [onClick] was already called in this scope
         */
        public fun onClick(action: GlanceAdaptiveAction) {
            clickable.onClick(action)
        }
    }

    /** Configures an individual [ListItem.DetailCard] inside [ItemsScope]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class DetailCardScope internal constructor() {
        internal val clickable: ClickableScope = ClickableScope()
        internal var body: TemplateText? = null
            private set

        internal var checklistItems: List<ListItem.DetailCard.ChecklistEntry> = emptyList()
            private set

        private var styleOverride: ListItemStyle? = null
        internal val style: ListItemStyle
            get() = styleOverride ?: ListItemStyle.Default

        /**
         * Sets a multi-line body text snippet on the detail card. Mutually exclusive with
         * [checklist].
         *
         * @param value non-blank body text
         * @param options optional block configuring [ItemTextScope] settings
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if [body] or [checklist] was already called in this scope
         */
        public fun body(value: String, options: ItemTextScope.() -> Unit = {}) {
            check(body == null && checklistItems.isEmpty()) {
                "Detail card content was already set."
            }
            body = buildItemTemplateText(value, options = options).first
        }

        /**
         * Sets a read-only inline checklist preview on the detail card. Mutually exclusive with
         * [body].
         *
         * @param content block adding 1 to 4 entries in [ChecklistScope]
         * @throws IllegalArgumentException if [content] adds 0 or more than 4 entries
         * @throws IllegalStateException if [body] or [checklist] was already called in this scope
         */
        public fun checklist(content: ChecklistScope.() -> Unit) {
            check(body == null && checklistItems.isEmpty()) {
                "Detail card content was already set."
            }
            checklistItems = ChecklistScope().apply(content).build()
        }

        /**
         * Replaces the card's title with [replacement] on locked or privacy-restricted surfaces.
         *
         * @param replacement non-blank placeholder string
         * @throws IllegalArgumentException if [replacement] is blank
         * @throws IllegalStateException if [sensitive] was already called in this scope
         */
        public fun sensitive(replacement: String) {
            clickable.sensitive(replacement)
        }

        /**
         * Sets the container prominence [style] for this detail card (defaults to
         * [ListItemStyle.Default]).
         *
         * @param style container prominence style
         * @throws IllegalStateException if [style] was already called in this scope
         */
        public fun style(style: ListItemStyle) {
            check(styleOverride == null) { "Detail card style was already set." }
            styleOverride = style
        }

        /**
         * Runs [action] when the detail card is tapped.
         *
         * @param action what the tap does
         * @throws IllegalStateException if [onClick] was already called in this scope
         */
        public fun onClick(action: GlanceAdaptiveAction) {
            clickable.onClick(action)
        }
    }

    /**
     * Populates read-only inline [ListItem.DetailCard.ChecklistEntry] preview rows inside
     * [DetailCardScope.checklist].
     *
     * Unlike standalone [ItemsScope.checkboxItem] rows, inline checklist entries are non-tappable
     * previews; tapping the enclosing detail card invokes [DetailCardScope.onClick].
     */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class ChecklistScope internal constructor() {
        private val entries: MutableList<ListItem.DetailCard.ChecklistEntry> = mutableListOf()

        /**
         * Appends a read-only inline checklist entry. Checked entries apply strikethrough by
         * default unless overridden in [options].
         *
         * @param title non-blank entry text
         * @param isChecked whether the entry is checked (defaults to `false`)
         * @param options optional block configuring [ItemTextScope] settings
         * @throws IllegalArgumentException if [title] is blank
         */
        public fun item(
            title: String,
            isChecked: Boolean = false,
            options: ItemTextScope.() -> Unit = {},
        ) {
            require(title.isNotBlank()) { "A checklist item title must not be blank." }
            entries.add(
                ListItem.DetailCard.ChecklistEntry(
                    text =
                        buildItemTemplateText(
                                title,
                                defaultStruckThrough = isChecked,
                                options = options,
                            )
                            .first,
                    isChecked = isChecked,
                )
            )
        }

        internal fun build(): List<ListItem.DetailCard.ChecklistEntry> {
            require(entries.isNotEmpty()) {
                "A checklist block must contain at least one entry (for example, item(...))."
            }
            return entries
        }
    }
}

private fun buildItemTemplateText(
    value: String,
    defaultStruckThrough: Boolean = false,
    options: ListTemplateScope.ItemTextScope.() -> Unit,
): Pair<TemplateText, Boolean?> {
    require(value.isNotBlank()) { "Text must not be blank." }
    val textScope = ListTemplateScope.ItemTextScope().apply(options)
    return TemplateText(
        value = value,
        sensitiveReplacement = textScope.sensitiveReplacement,
        isStruckThrough = textScope.isStruckThrough ?: defaultStruckThrough,
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
