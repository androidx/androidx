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

import androidx.annotation.CheckResult
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
 * Builds a [TrackTemplate] using [TrackTemplateScope]:
 * ```kotlin
 * val weeklySteps = listOf(
 *     ChartBar(0.65f, "M"), ChartBar(1.0f, "T"), ChartBar(0.80f, "W"),
 *     ChartBar(1.0f, "T"), ChartBar(0.45f, "F"), ChartBar(1.0f, "S"), ChartBar(0.55f, "S"),
 * )
 *
 * val template = trackTemplate {
 *     leading {
 *         progressRing(
 *             progress = 0.55f,
 *             icon = TemplateIcon.fromResource(R.drawable.ic_steps),
 *         )
 *     }
 *     primary {
 *         text("11,056") { sensitive("--") }
 *     }
 *     secondary {
 *         text("Steps")
 *     }
 *     hero {
 *         barChart(bars = weeklySteps) {
 *             startLabel("7 Day Avg")
 *             endLabel("13,843", icon = TemplateIcon.fromResource(R.drawable.ic_arrow_up))
 *         }
 *     }
 *     supporting {
 *         text("55%")
 *         text("3,118 to go")
 *         badge("Z2")
 *     }
 * }
 * ```
 *
 * @param content DSL block configuring the slots of the [TrackTemplate]
 * @return validated [TrackTemplate] instance
 * @throws IllegalArgumentException if arguments are invalid or required slots are missing
 * @throws IllegalStateException if a slot block is empty, configured more than once, or modified
 *   after its block completes
 */
@CheckResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun trackTemplate(content: TrackTemplateScope.() -> Unit): TrackTemplate {
    val scope = TrackTemplateScope()
    try {
        scope.apply(content)
    } finally {
        scope.isClosed = true
    }
    return scope.build()
}

/**
 * Adaptive [GlanceAdaptiveTemplate] for tracking progress, metrics, and historical bar charts.
 *
 * Build an instance with [trackTemplate]. Host renderers adapt the populated slots across
 * [androidx.glance.adaptive.core.selection.GlanceSurface] breakpoints.
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackTemplate) return false
        return leading == other.leading &&
            primary == other.primary &&
            secondary == other.secondary &&
            hero == other.hero &&
            supporting == other.supporting
    }

    override fun hashCode(): Int {
        var result = leading?.hashCode() ?: 0
        result = 31 * result + (primary?.hashCode() ?: 0)
        result = 31 * result + (secondary?.hashCode() ?: 0)
        result = 31 * result + (hero?.hashCode() ?: 0)
        result = 31 * result + supporting.hashCode()
        return result
    }

    override fun toString(): String =
        "TrackTemplate(leading=$leading, primary=$primary, secondary=$secondary, " +
            "hero=$hero, supporting=$supporting)"

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /** Stable integer telemetry identifier for TrackTemplate. */
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
