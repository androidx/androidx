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

package androidx.glance.adaptive.core.templates

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.glance.adaptive.core.slots.IconTile
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.Requirement
import androidx.glance.adaptive.core.slots.SecondaryComponent
import androidx.glance.adaptive.core.slots.TemplateRule
import androidx.glance.adaptive.core.slots.TemplateText
import androidx.glance.adaptive.core.slots.TrailingComponent

/**
 * Adaptive template for a grid of tappable actions under an optional header, such as a "Start a
 * workout" quick launcher or a grid of favorite contacts.
 *
 * Host renderers adapt the populated semantic slots across
 * [androidx.glance.adaptive.core.selection.GlanceSurface] breakpoints, and decide how many of the
 * [actions] fit at a size:
 * - [actions] (required): 1 to 8 actions, in display order.
 * - [leading] and [primary] (required together, for a header): the header's icon and title.
 * - [secondary] (optional): the header's subtitle.
 * - [trailing] (optional): a button at the header's end.
 *
 * The header is all or nothing: a template that sets any header slot sets [leading] and [primary].
 * Without a header, the actions take the whole widget. Taps are described as
 * [androidx.glance.adaptive.core.action.GlanceWidgetAction]s, never as `Intent`s or
 * `PendingIntent`s, so the template keeps value equality and can be sent to surfaces without
 * `PendingIntent` support.
 *
 * @param actions The actions, in display order, copied so later changes to the caller's list are
 *   not seen.
 * @throws IllegalArgumentException if [actions] has fewer than 1 or more than 8 entries, or a
 *   header slot is set without [leading] and [primary].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DashboardTemplate
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(
    actions: List<IconTile>,
    /** Optional header icon, such as an [Icon]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val leading: LeadingComponent? = null,
    /** Header title, such as a [TemplateText]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val primary: PrimaryComponent? = null,
    /** Optional header subtitle, shown beneath [primary]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val secondary: SecondaryComponent? = null,
    /** Optional button at the header's end, such as an [IconButton]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val trailing: TrailingComponent? = null,
) : GlanceAdaptiveTemplate(templateId = TEMPLATE_ID) {

    /** The actions, in display order (1 to 8). */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val actions: List<IconTile> = actions.toList()

    init {
        Rule.check(this)
    }

    /** Whether the template has a header, which a valid template draws above its actions. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val hasHeader: Boolean
        get() = leading != null || primary != null || secondary != null || trailing != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashboardTemplate) return false
        return leading == other.leading &&
            primary == other.primary &&
            secondary == other.secondary &&
            trailing == other.trailing &&
            actions == other.actions
    }

    override fun hashCode(): Int {
        var result = leading?.hashCode() ?: 0
        result = 31 * result + (primary?.hashCode() ?: 0)
        result = 31 * result + (secondary?.hashCode() ?: 0)
        result = 31 * result + (trailing?.hashCode() ?: 0)
        result = 31 * result + actions.hashCode()
        return result
    }

    override fun toString(): String =
        "DashboardTemplate(leading=$leading, primary=$primary, secondary=$secondary, " +
            "trailing=$trailing, actions=$actions)"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Stable integer telemetry identifier for [DashboardTemplate]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TEMPLATE_ID: Int = 2

        /** Most actions a [DashboardTemplate] holds: two rows of four. */
        internal const val MAX_ACTIONS: Int = 8

        internal val Rule: TemplateRule<DashboardTemplate> =
            TemplateRule(
                templateName = "DashboardTemplate",
                Requirement("1 to $MAX_ACTIONS actions (call actions { action(...) })") {
                    it.actions.size in 1..MAX_ACTIONS
                },
                Requirement("a header icon with its header (call leading { icon(...) })") {
                    !it.hasHeader || it.leading != null
                },
                Requirement("a header title with its header (call primary { text(...) })") {
                    !it.hasHeader || it.primary != null
                },
            )
    }
}
