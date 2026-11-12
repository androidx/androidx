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
import androidx.glance.adaptive.core.slots.ActionComponent
import androidx.glance.adaptive.core.slots.Capability
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.HeroComponent
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.Requirement
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.SecondaryComponent
import androidx.glance.adaptive.core.slots.TemplateRule
import androidx.glance.adaptive.core.slots.TemplateText

private const val MAX_SUPPORTING_ITEMS = 3

/**
 * Displays progress tracking, fitness metrics, and historical activity summaries across adaptive
 * surfaces.
 *
 * Host renderers adapt the populated slots across
 * [androidx.glance.adaptive.core.selection.GlanceSurface] breakpoints:
 * - primary (required): headline metric value.
 * - leading or hero (at least one required): progress ring or historical bar chart.
 * - secondary (optional): unit label paired with the headline metric.
 * - supporting (optional): up to 3 supporting text values or status badges.
 *
 * @throws IllegalArgumentException if the populated slots do not satisfy these requirements
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TrackTemplate
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    /** Optional leading visual component, such as a [Ring]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val leading: LeadingComponent? = null,
    /** Required headline metric component, such as [TemplateText]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val primary: PrimaryComponent? = null,
    /** Optional secondary unit or subtitle component paired with [primary]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val secondary: SecondaryComponent? = null,
    /** Optional hero chart component, such as a [Graph]. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val hero: HeroComponent? = null,
    supporting: List<ActionComponent> = emptyList(),
) : GlanceAdaptiveTemplate(templateId = TEMPLATE_ID) {

    /** Optional supporting action row items (at most 3). */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val supporting: List<ActionComponent> = supporting.toList()

    init {
        Rule.check(this)
    }

    internal fun hasCapability(capability: Capability): Boolean {
        if (leading != null && capability in leading.capabilities) return true
        if (primary != null && capability in primary.capabilities) return true
        if (secondary != null && capability in secondary.capabilities) return true
        if (hero != null && capability in hero.capabilities) return true
        return supporting.any { capability in it.capabilities }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Stable integer telemetry identifier for [TrackTemplate]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TEMPLATE_ID: Int = 1

        internal val Rule: TemplateRule<TrackTemplate> =
            TemplateRule(
                templateName = "TrackTemplate",
                Requirement("a primary text (call primary { text(...) })") { it.primary != null },
                Requirement(
                    "a tracking visual (call leading { progressRing(...) } or hero { barChart(...) })"
                ) {
                    it.hasCapability(Capability.Tracking)
                },
                Requirement("at most $MAX_SUPPORTING_ITEMS supporting items") {
                    it.supporting.size <= MAX_SUPPORTING_ITEMS
                },
            )
    }
}
