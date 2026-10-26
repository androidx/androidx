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
package androidx.glance.adaptive.appwidget.ui.components.ring

import androidx.glance.adaptive.appwidget.ui.matrix.ContainedRing
import androidx.glance.adaptive.appwidget.ui.matrix.Ring

/**
 * Design values of the [Ring] and [ContainedRing] components that every backend draws them with.
 */
internal object RingDefaults {

    /** Ratio of icon edge to ring edge, from the design's 30.5 dp glyph inside a 56 dp ring. */
    const val IconRatio: Float = 0.545f

    /**
     * Ratio of icon edge to ring edge inside the contained ring, from the design's 16 dp glyph
     * inside an 80 dp ring.
     *
     * Far smaller than [IconRatio] because here the glyph shares the ring with the value rather
     * than filling it alone.
     */
    const val ContainedIconRatio: Float = 0.2f

    /** Opacity of the unfilled remainder of the progress ring. */
    const val TrackAlpha: Float = 0.18f

    /**
     * Ring thickness as a fraction of its diameter, from the design's 4 dp stroke on a 56 dp ring.
     *
     * The contained ring at the narrowest breakpoint is drawn far larger but keeps the same ratio,
     * so the stroke is derived rather than fixed.
     */
    const val StrokeRatio: Float = 4f / 56f
}
