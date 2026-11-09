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
package androidx.glance.adaptive.appwidget.components.ring

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.adaptive.core.slots.ContainedRing
import androidx.glance.adaptive.core.slots.Ring

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

    /**
     * Total inset of the contained ring inside its circle, from the design's 80 dp ring in 88 dp.
     */
    private const val ContainedRingInsetDp: Float = 8f

    /**
     * Diameter of the contained ring the design draws, at which its value is [ContainedRingTextSp].
     */
    private const val ContainedRingDesignSizeDp: Float = 80f

    /** Type size of the contained ring's value at [ContainedRingDesignSizeDp]. */
    private const val ContainedRingTextSp: Float = 20f

    /**
     * Fraction of the contained ring's diameter its value may span: the chord narrows towards the
     * circle's edges, and two stroke widths in from each side is where the design's value sits.
     */
    private const val ContainedRingTextRatio: Float = 0.86f

    /**
     * Resolves the layout of a [ContainedRing] inside a circle [containerSize] across.
     *
     * The value shrinks with a ring drawn smaller than the design's, so the glyph and the value
     * stacked inside it still clear the ring.
     *
     * @param containerSize Diameter of the circle the bulb gives the ring.
     * @return The ring's size, and the room and type size of its value.
     */
    fun containedRingGeometry(containerSize: Dp): ContainedRingGeometry {
        val ringDp = (containerSize.value - ContainedRingInsetDp).coerceAtLeast(0f)
        return ContainedRingGeometry(
            ringSize = ringDp.dp,
            valueMaxWidth = (ringDp * ContainedRingTextRatio).dp,
            fontSize =
                (ContainedRingTextSp * (ringDp / ContainedRingDesignSizeDp).coerceAtMost(1f)).sp,
        )
    }
}

/**
 * A [ContainedRing] laid out for its circle (see [RingDefaults.containedRingGeometry]).
 *
 * @property ringSize Outer diameter of the ring.
 * @property valueMaxWidth Room the value has across the ring.
 * @property fontSize Type size of the value.
 */
internal class ContainedRingGeometry(
    val ringSize: Dp,
    val valueMaxWidth: Dp,
    val fontSize: TextUnit,
)
