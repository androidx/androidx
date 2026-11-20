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
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.ListItem
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.Requirement
import androidx.glance.adaptive.core.slots.TemplateRule
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.slots.TrailingComponent
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate

/** Visual prominence / container style of a list item in a [ListTemplate]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class ListItemStyle private constructor(private val value: Int) {
    override fun toString(): String =
        when (this) {
            Default -> "Default"
            Focused -> "Focused"
            Neutral -> "Neutral"
            Outlined -> "Outlined"
            else -> "Unknown($value)"
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Standard filled container. */
        public val Default: ListItemStyle = ListItemStyle(0)

        /** Prominent accent container for featured or active rows. */
        public val Focused: ListItemStyle = ListItemStyle(1)

        /** Subtle neutral container for secondary or out-of-office rows. */
        public val Neutral: ListItemStyle = ListItemStyle(2)

        /** Transparent container with a hairline outline for tentative or declined rows. */
        public val Outlined: ListItemStyle = ListItemStyle(3)
    }
}

/**
 * Adaptive template for a vertical list of items under an optional header, such as an agenda or
 * upcoming trips list.
 *
 * Host renderers adapt the populated semantic slots across
 * [androidx.glance.adaptive.core.selection.GlanceSurface] breakpoints, scrolling on phone home
 * screens when [isScrollable] is `true` and slicing to fit non-scrollable surfaces (such as
 * Lockscreen, XR, and Auto):
 * - [items] (required): 1 to 25 items in display order, including at least one non-divider item.
 * - [leading] (optional): the header's icon.
 * - [title] (optional): the header's title.
 * - [trailing] (optional): a button at the header's end.
 *
 * ```kotlin
 * val template = listTemplate {
 *     leading { icon(TemplateIcon.fromResource(R.drawable.ic_calendar)) }
 *     title { text("Oct 6, TUE") }
 *     items {
 *         item("Widget Weekly Sync") {
 *             subtitle("10:30 - 11:30")
 *             trailingChip("Now")
 *         }
 *     }
 * }
 * ```
 *
 * @param items The list items, in display order, copied so later changes to the caller's list are
 *   not seen.
 * @throws IllegalArgumentException if [items] has fewer than 1 or more than 25 entries or contains
 *   only dividers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ListTemplate
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    items: List<ListItem>,
    /** Optional header icon, such as an [Icon]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val leading: LeadingComponent? = null,
    /** Optional header title, such as a [TemplateText]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val title: PrimaryComponent? = null,
    /** Optional button at the header's end, such as an [IconButton]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val trailing: TrailingComponent? = null,
    /**
     * Whether the list may scroll on scrollable surfaces (non-scrollable surfaces always slice).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val isScrollable: Boolean = true,
    /** Whether the header row remains visible at compact heights. */
    @get:JvmName("shouldShowHeaderInCompactHeight")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val showHeaderInCompactHeight: Boolean = true,
    /**
     * Optional short title (e.g. `TemplateText("10/6")`) used in narrow headers or prefixed to the
     * first item when the header is hidden at compact heights.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val compactTitle: TemplateText? = null,
) : GlanceAdaptiveTemplate(templateId = TEMPLATE_ID) {

    /** The list items, in display order (1 to 25). */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val items: List<ListItem> = items.toList()

    init {
        Rule.check(this)
    }

    /** Whether the template has any header slot populated. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val hasHeader: Boolean
        get() = leading != null || title != null || trailing != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListTemplate) return false
        return leading == other.leading &&
            title == other.title &&
            trailing == other.trailing &&
            items == other.items &&
            isScrollable == other.isScrollable &&
            showHeaderInCompactHeight == other.showHeaderInCompactHeight &&
            compactTitle == other.compactTitle
    }

    override fun hashCode(): Int {
        var result = leading?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (trailing?.hashCode() ?: 0)
        result = 31 * result + items.hashCode()
        result = 31 * result + isScrollable.hashCode()
        result = 31 * result + showHeaderInCompactHeight.hashCode()
        result = 31 * result + (compactTitle?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ListTemplate(leading=$leading, title=$title, trailing=$trailing, items=$items, " +
            "isScrollable=$isScrollable, showHeaderInCompactHeight=$showHeaderInCompactHeight, " +
            "compactTitle=$compactTitle)"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Stable integer telemetry identifier for [ListTemplate]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TEMPLATE_ID: Int = 3

        /** Most items a [ListTemplate] holds to stay well within Binder IPC limits. */
        internal const val MAX_ITEMS: Int = 25

        internal val Rule: TemplateRule<ListTemplate> =
            TemplateRule(
                templateName = "ListTemplate",
                Requirement("1 to $MAX_ITEMS items (call items { item(...) })") {
                    it.items.size in 1..MAX_ITEMS
                },
                Requirement("at least one non-divider item (call items { item(...) })") {
                    it.items.isEmpty() || it.items.any { item -> !item.isDivider }
                },
            )
    }
}
