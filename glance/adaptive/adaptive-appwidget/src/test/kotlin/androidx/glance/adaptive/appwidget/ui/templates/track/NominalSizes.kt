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

package androidx.glance.adaptive.appwidget.ui.templates.track

import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.HeightTier
import androidx.glance.adaptive.core.ui.selection.SizeTiers
import androidx.glance.adaptive.core.ui.selection.WidthTier

/**
 * Container width the design nominates for each width tier, in DP.
 *
 * A tier covers a range, so a test that only names the tier has to pick a size within it. Using the
 * design's own figures keeps the hero sizing — which reads the container, not the tier — anchored
 * to the layout that was actually drawn.
 */
private val NominalWidthDp =
    mapOf(WidthTier.W1 to 88, WidthTier.W2 to 176, WidthTier.W3 to 264, WidthTier.W4 to 348)

/** Container height the design nominates for each height tier, in DP. See [NominalWidthDp]. */
private val NominalHeightDp =
    mapOf(
        HeightTier.H0 to 56,
        HeightTier.H1 to 88,
        HeightTier.H2 to 132,
        HeightTier.H3 to 176,
        HeightTier.H4 to 264,
    )

/** The container size the design nominates for a cell of the breakpoint matrix. */
internal fun nominalDimensions(width: WidthTier, height: HeightTier): Dimensions =
    Dimensions(NominalWidthDp.getValue(width), NominalHeightDp.getValue(height))

/** Resolves the Track plan for a cell of the matrix at its [nominalDimensions]. */
internal fun nominalSlots(width: WidthTier, height: HeightTier): TrackSlots =
    TrackSlots.from(SizeTiers(width, height), nominalDimensions(width, height))
