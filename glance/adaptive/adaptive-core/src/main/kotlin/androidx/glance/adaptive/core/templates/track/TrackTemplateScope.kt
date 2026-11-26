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

package androidx.glance.adaptive.core.templates.track

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.annotation.Size
import androidx.collection.MutableFloatList
import androidx.glance.adaptive.core.slots.ActionComponent
import androidx.glance.adaptive.core.slots.Badge
import androidx.glance.adaptive.core.slots.ChartLabel
import androidx.glance.adaptive.core.slots.Graph
import androidx.glance.adaptive.core.slots.HeroComponent
import androidx.glance.adaptive.core.slots.LeadingComponent
import androidx.glance.adaptive.core.slots.MIN_GRAPH_VALUES
import androidx.glance.adaptive.core.slots.PrimaryComponent
import androidx.glance.adaptive.core.slots.Ring
import androidx.glance.adaptive.core.slots.SecondaryComponent
import androidx.glance.adaptive.core.templates.BarChartScope
import androidx.glance.adaptive.core.templates.ChartBar
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplateDsl
import androidx.glance.adaptive.core.templates.TemplateIcon
import androidx.glance.adaptive.core.templates.TextScope
import androidx.glance.adaptive.core.templates.buildTemplateText

/** Configures the semantic slots of a [TrackTemplate]. */
@GlanceAdaptiveTemplateDsl
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TrackTemplateScope internal constructor() {
    internal var isClosed: Boolean = false

    private var leading: LeadingComponent? = null
    private var primary: PrimaryComponent? = null
    private var secondary: SecondaryComponent? = null
    private var hero: HeroComponent? = null
    private var supporting: List<ActionComponent> = emptyList()

    /**
     * Configures the header's leading visual slot.
     *
     * @param content block adding a single component in [LeadingScope]
     * @throws IllegalStateException if the leading slot was already configured, [content] adds no
     *   component, or this scope has already completed
     */
    public fun leading(content: LeadingScope.() -> Unit) {
        check(!isClosed) { "TrackTemplateScope cannot be modified after trackTemplate completes." }
        check(leading == null) { "The leading slot was already configured." }
        val scope = LeadingScope()
        try {
            scope.apply(content)
        } finally {
            scope.isClosed = true
        }
        leading =
            checkNotNull(scope.component) {
                "The leading block must contain a component (for example, progressRing(...))."
            }
    }

    /**
     * Configures the required primary headline metric slot.
     *
     * @param content block adding a single component in [PrimaryScope]
     * @throws IllegalStateException if the primary slot was already configured, [content] adds no
     *   component, or this scope has already completed
     */
    public fun primary(content: PrimaryScope.() -> Unit) {
        check(!isClosed) { "TrackTemplateScope cannot be modified after trackTemplate completes." }
        check(primary == null) { "The primary slot was already configured." }
        val scope = PrimaryScope()
        try {
            scope.apply(content)
        } finally {
            scope.isClosed = true
        }
        primary =
            checkNotNull(scope.component) {
                "The primary block must contain a component (for example, text(...))."
            }
    }

    /**
     * Configures the optional secondary unit or subtitle slot paired with [primary].
     *
     * @param content block adding a single component in [SecondaryScope]
     * @throws IllegalStateException if the secondary slot was already configured, [content] adds no
     *   component, or this scope has already completed
     */
    public fun secondary(content: SecondaryScope.() -> Unit) {
        check(!isClosed) { "TrackTemplateScope cannot be modified after trackTemplate completes." }
        check(secondary == null) { "The secondary slot was already configured." }
        val scope = SecondaryScope()
        try {
            scope.apply(content)
        } finally {
            scope.isClosed = true
        }
        secondary =
            checkNotNull(scope.component) {
                "The secondary block must contain a component (for example, text(...))."
            }
    }

    /**
     * Configures the optional hero chart slot.
     *
     * @param content block adding a single component in [HeroScope]
     * @throws IllegalStateException if the hero slot was already configured, [content] adds no
     *   component, or this scope has already completed
     */
    public fun hero(content: HeroScope.() -> Unit) {
        check(!isClosed) { "TrackTemplateScope cannot be modified after trackTemplate completes." }
        check(hero == null) { "The hero slot was already configured." }
        val scope = HeroScope()
        try {
            scope.apply(content)
        } finally {
            scope.isClosed = true
        }
        hero =
            checkNotNull(scope.component) {
                "The hero block must contain a component (for example, barChart(...))."
            }
    }

    /**
     * Configures the optional bottom supporting row with up to 3 items, ordered by priority.
     *
     * Narrower surfaces keep earlier items first and drop trailing items when horizontal space is
     * constrained.
     *
     * @param content block adding 1 to 3 items in [SupportingScope]
     * @throws IllegalStateException if the supporting slot was already configured, [content] adds
     *   no items, or this scope has already completed
     */
    public fun supporting(content: SupportingScope.() -> Unit) {
        // TODO(b/584617288): Confirm supporting-slot ordering, combinations, and compaction with
        // UX.
        check(!isClosed) { "TrackTemplateScope cannot be modified after trackTemplate completes." }
        check(supporting.isEmpty()) { "The supporting slot was already configured." }
        val scope = SupportingScope()
        try {
            scope.apply(content)
        } finally {
            scope.isClosed = true
        }
        check(scope.components.isNotEmpty()) {
            "The supporting block must contain at least one component " +
                "(for example, text(...) or badge(...))."
        }
        supporting = scope.components.toList()
    }

    internal fun build(): TrackTemplate =
        TrackTemplate(
            leading = leading,
            primary = primary,
            secondary = secondary,
            hero = hero,
            supporting = supporting,
        )

    /** Populates the [TrackTemplateScope.leading] slot of a [TrackTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class LeadingScope internal constructor() {
        internal var isClosed: Boolean = false
        internal var component: LeadingComponent? = null

        /**
         * Places a determinate progress ring in the leading slot.
         *
         * @param progress normalized progress in `[0.0f, 1.0f]`
         * @param icon optional centered decorative [TemplateIcon] tinted with the active surface's
         *   accent color
         * @throws IllegalArgumentException if [progress] is not finite or outside `[0.0f, 1.0f]`
         * @throws IllegalStateException if a component was already added to this slot or this scope
         *   has already completed
         */
        public fun progressRing(
            @FloatRange(from = 0.0, to = 1.0) progress: Float,
            icon: TemplateIcon? = null,
        ) {
            check(!isClosed) { "LeadingScope cannot be modified after its block completes." }
            check(component == null) { "The leading slot accepts only one component." }
            component = Ring(progress = progress, icon = icon?.resId)
        }
    }

    /** Populates the [TrackTemplateScope.primary] headline slot of a [TrackTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class PrimaryScope internal constructor() {
        internal var isClosed: Boolean = false
        internal var component: PrimaryComponent? = null

        /**
         * Places the headline metric text in the primary slot.
         *
         * @param value non-blank formatted metric string (for example, `"11,056"`)
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if a component was already added to this slot or this scope
         *   has already completed
         */
        public fun text(value: String, options: TextScope.() -> Unit = {}) {
            check(!isClosed) { "PrimaryScope cannot be modified after its block completes." }
            check(component == null) { "The primary slot accepts only one component." }
            component = buildTemplateText(value, options)
        }
    }

    /** Populates the [TrackTemplateScope.secondary] label slot of a [TrackTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class SecondaryScope internal constructor() {
        internal var isClosed: Boolean = false
        internal var component: SecondaryComponent? = null

        /**
         * Places a unit label or subtitle in the secondary slot.
         *
         * @param value non-blank label string (for example, `"Steps"`)
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if a component was already added to this slot or this scope
         *   has already completed
         */
        public fun text(value: String, options: TextScope.() -> Unit = {}) {
            check(!isClosed) { "SecondaryScope cannot be modified after its block completes." }
            check(component == null) { "The secondary slot accepts only one component." }
            component = buildTemplateText(value, options)
        }
    }

    /** Populates the [TrackTemplateScope.hero] visual slot of a [TrackTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class HeroScope internal constructor() {
        internal var isClosed: Boolean = false
        internal var component: HeroComponent? = null

        /**
         * Places a proportional bar chart in the hero slot.
         *
         * Axis labels on [ChartBar] entries are optional; when only some bars define
         * [ChartBar.label], unlabeled columns keep empty label space so labels stay aligned.
         *
         * @param bars ordered list of at least 2 [ChartBar] columns, from leading to trailing
         * @param options optional block configuring [BarChartScope.startLabel] and
         *   [BarChartScope.endLabel]
         * @throws IllegalArgumentException if [bars] contains fewer than 2 entries
         * @throws IllegalStateException if a component was already added to this slot or this scope
         *   has already completed
         */
        public fun barChart(
            @Size(min = MIN_GRAPH_VALUES.toLong()) bars: List<ChartBar>,
            options: BarChartScope.() -> Unit = {},
        ) {
            check(!isClosed) { "HeroScope cannot be modified after its block completes." }
            check(component == null) { "The hero slot accepts only one component." }
            // Validate before running options so invalid bar counts fail prior to nested calls.
            require(bars.size >= MIN_GRAPH_VALUES) {
                "A bar chart needs at least $MIN_GRAPH_VALUES bars, had ${bars.size}."
            }
            val scope = BarChartScope()
            try {
                scope.apply(options)
            } finally {
                scope.isClosed = true
            }
            val values = MutableFloatList(bars.size).apply { bars.forEach { add(it.value) } }
            val labels =
                if (bars.any { it.label != null }) {
                    List(bars.size) { index -> bars[index].label.orEmpty() }
                } else {
                    emptyList()
                }
            val emphasized = BooleanArray(bars.size) { index -> bars[index].isEmphasized }
            component =
                Graph(
                    values = values,
                    labels = labels,
                    // ChartBar values are normalized to [0, 1]; without goal = 1f, Graph would
                    // scale bars relative to max(values) instead of 1f.
                    goal = 1.0f,
                    emphasized = emphasized,
                    startLabel =
                        scope.startLabelText?.let { ChartLabel(it, scope.startLabelIcon?.resId) },
                    endLabel =
                        scope.endLabelText?.let { ChartLabel(it, scope.endLabelIcon?.resId) },
                )
        }
    }

    /** Populates the [TrackTemplateScope.supporting] bottom row of a [TrackTemplate]. */
    @GlanceAdaptiveTemplateDsl
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class SupportingScope internal constructor() {
        internal var isClosed: Boolean = false
        internal val components: MutableList<ActionComponent> = mutableListOf()

        /**
         * Appends a supporting metric or status string to the supporting row.
         *
         * @param value non-blank supporting text (for example, `"55%"` or `"3,118 to go"`)
         * @param options optional block configuring [TextScope] settings such as
         *   [TextScope.sensitive]
         * @throws IllegalArgumentException if [value] is blank
         * @throws IllegalStateException if this scope has already completed
         */
        public fun text(value: String, options: TextScope.() -> Unit = {}) {
            check(!isClosed) { "SupportingScope cannot be modified after its block completes." }
            components.add(buildTemplateText(value, options))
        }

        /**
         * Appends a system-themed outlined pill badge to the supporting row.
         *
         * @param label non-blank short label inside the pill (for example, `"Z2"`)
         * @throws IllegalArgumentException if [label] is blank
         * @throws IllegalStateException if this scope has already completed
         */
        public fun badge(label: String) {
            check(!isClosed) { "SupportingScope cannot be modified after its block completes." }
            components.add(Badge(label = label))
        }
    }
}
