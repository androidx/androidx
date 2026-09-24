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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.state.RemoteFloat

// Geometry of the checkmark tick drawn by the selection controls, forked from the file of the same
// name in Wear Material 3 so that both draw the same shape.

/**
 * Where the tick starts, in dp within the 24dp box every selection control draws into. The tick is
 * two segments: a short base running down and right from here to ([TICK_BASE_END_X_DP],
 * [TICK_BASE_END_Y_DP]), then a long stick running up and right to ([TICK_STICK_END_X_DP],
 * [TICK_STICK_END_Y_DP]). Both sit at 45 degrees, so each segment's horizontal and vertical extents
 * are equal.
 */
internal const val TICK_BASE_START_X_DP: Float = 7.4f

/** See [TICK_BASE_START_X_DP]. */
internal const val TICK_BASE_START_Y_DP: Float = 13.0f

/** Where the base segment ends and the stick segment begins. See [TICK_BASE_START_X_DP]. */
internal const val TICK_BASE_END_X_DP: Float = 9.9f

/** See [TICK_BASE_START_X_DP]. */
internal const val TICK_BASE_END_Y_DP: Float = 15.5f

/** See [TICK_BASE_START_X_DP]. */
internal const val TICK_STICK_END_X_DP: Float = 16.5f

/** See [TICK_BASE_START_X_DP]. */
internal const val TICK_STICK_END_Y_DP: Float = 9.1f

/** Centre of the 24dp design box, which the tick rotates about while it is drawn. */
internal const val TICK_DESIGN_CENTER_DP: Float = 12f

/** Angle in degrees the tick starts rotated by, decaying to zero as it is drawn. */
internal const val TICK_ROTATION_DEGREES: Float = 15f

/**
 * Share of the animation that draws the base segment. The tick is drawn at a constant rate along
 * its own length, so each segment takes the fraction of the animation that it occupies of the whole
 * tick.
 */
internal const val TICK_BASE_PROGRESS_FRACTION: Float =
    (TICK_BASE_END_X_DP - TICK_BASE_START_X_DP) / (TICK_STICK_END_X_DP - TICK_BASE_START_X_DP)

/** Rotates this [RemoteOffset] by the angle with [cosAngle] and [sinAngle] around [pivot]. */
internal fun RemoteOffset.rotate(
    cosAngle: RemoteFloat,
    sinAngle: RemoteFloat,
    pivot: RemoteOffset,
): RemoteOffset {
    val dx = x - pivot.x
    val dy = y - pivot.y
    return RemoteOffset(
        x = pivot.x + dx * cosAngle - dy * sinAngle,
        y = pivot.y + dx * sinAngle + dy * cosAngle,
    )
}
