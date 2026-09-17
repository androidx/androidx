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

package androidx.glance.adaptive.appwidget.ui.templates

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.selection.from
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/**
 * Resolves the [TrackSlots] layout plan for a [TrackTemplate] on platform AppWidget surfaces.
 *
 * Track has no family of archetypes to choose between: every breakpoint renders the same component
 * with different slots turned on. The slot plan *is* the archetype, so selection reduces to mapping
 * the container onto its breakpoints and asking [TrackSlots] what fits.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TrackSizeSelector :
    ArchetypeSelector<TrackTemplate, AppWidgetGlanceSurface, TrackSlots> {

    /**
     * Selects the slot plan for [template] under [constraints].
     *
     * [template] does not currently influence the result, but is part of the contract and allows
     * data-aware fallbacks (e.g. degrading when optional fields are absent) to be added without an
     * API change.
     *
     * @param template The template data payload.
     * @param constraints Container size in DP plus the target AppWidget surface, which decides
     *   which breakpoints apply.
     * @return Resolved [TrackSlots].
     */
    override fun select(
        template: TrackTemplate,
        constraints: HostConstraints<AppWidgetGlanceSurface>,
    ): TrackSlots =
        TrackSlots.from(
            tiers = SizeTiers.from(constraints.dimensions, constraints.surface),
            dimensions = constraints.dimensions,
        )
}
