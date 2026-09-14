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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/**
 * Glance renderer for [TrackTemplate] on platform AppWidget surfaces.
 *
 * Substitutes `@Composable () -> Unit` for the host content type of [TemplateRenderer]: [render]
 * resolves the archetype eagerly — outside composition, so it stays unit testable — and returns the
 * Glance content to emit for it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TrackTemplateRenderer :
    TemplateRenderer<TrackTemplate, AppWidgetGlanceSurface, @Composable () -> Unit> {

    /**
     * Resolves the archetype for [template] under [constraints] and returns the Glance content
     * rendering it.
     *
     * @param template The template data payload.
     * @param constraints Container size in DP plus the target AppWidget surface.
     * @return Glance content for the resolved [TrackArchetype].
     */
    override fun render(
        template: TrackTemplate,
        constraints: HostConstraints<AppWidgetGlanceSurface>,
    ): @Composable () -> Unit {
        val archetype = TrackSizeSelector.select(template, constraints)
        return { Render(template, archetype) }
    }

    /**
     * Emits the Glance hierarchy for [template] using the already resolved [archetype].
     *
     * @param template The template data payload.
     * @param archetype The resolved layout archetype.
     */
    @Composable
    @VisibleForTesting
    internal fun Render(template: TrackTemplate, archetype: TrackArchetype) {
        when (archetype) {
            TrackArchetype.THIN -> ThinTrack(template)
            TrackArchetype.STANDARD -> StandardTrack(template)
        }
    }
}

/** Width-constrained Track layout for [TrackArchetype.THIN]. */
@Composable
@VisibleForTesting
@Suppress("UNUSED_PARAMETER")
internal fun ThinTrack(template: TrackTemplate) {
    // TODO: Emit the thin Track Glance hierarchy.
}

/** Default Track layout for [TrackArchetype.STANDARD]. */
@Composable
@VisibleForTesting
@Suppress("UNUSED_PARAMETER")
internal fun StandardTrack(template: TrackTemplate) {
    // TODO: Emit the standard Track Glance hierarchy.
}
