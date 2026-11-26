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
@file:OptIn(ExperimentalRemoteCreationApi::class)
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.dsl.d3

import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcHaptic
import androidx.compose.remote.creation.dsl.RcMesh
import androidx.compose.remote.creation.dsl.RcMeshMode
import androidx.compose.remote.creation.dsl.RcScene3DScope
import androidx.compose.remote.creation.dsl.RcTouchStopMode
import androidx.compose.remote.creation.dsl.max
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.touchPosX
import androidx.compose.remote.creation.dsl.touchPosY

/**
 * A touch-driven turntable angle (in radians) about the vertical (Y) axis, with momentum.
 *
 * Built on the same [RcScope.addTouch] touch API shown in `DslTouchDemo`: it maps the horizontal
 * touch position ([RemoteContext.FLOAT_TOUCH_POS_X]) to an angle. The touch value accumulates
 * relative to where each drag starts (no jump on re-touch), and [RcTouchStopMode.Gently] lets the
 * scene coast and decelerate when released — i.e. flick to spin. Player-side, so it costs nothing
 * in the document and no per-frame host work.
 *
 * Call this in the `Canvas { }` (drawing) scope and feed the result to [spunIdentity] for every
 * object in the [RcScene3DScope] block, so the whole scene orbits its vertical center.
 *
 * @param radiansPerPixel drag sensitivity — how many radians one pixel of horizontal drag adds.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.verticalSpinAngle(radiansPerPixel: Float = 0.012f): RcFloat =
    addTouch(
        defaultValue = 0f,
        // Effectively unbounded: the value accumulates across drags, so allow many turns.
        range = -100_000f..100_000f,
        stopMode = RcTouchStopMode.Gently,
        expression = touchPosX(),
        velocity = 0f,
        notchHaptic = RcHaptic.NoHaptics,
        notches = null,
        easing = null,
    ) * radiansPerPixel

/**
 * A touch-driven **tilt** (pitch) angle (in radians) about the horizontal (camera X) axis, with
 * momentum and clamped travel so the scene can't flip over.
 *
 * The vertical counterpart of [verticalSpinAngle]: it maps the vertical touch position
 * ([RemoteContext.FLOAT_TOUCH_POS_Y]) to a pitch angle, accumulating relative to where each drag
 * starts. The reachable angle is clamped to `[minAngleRad, maxAngleRad]` (the touch [range] is
 * derived from it), so a vertical flick coasts and then settles within those limits. Player-side,
 * so it costs nothing in the document.
 *
 * Call this in the `Canvas { }` scope alongside [verticalSpinAngle] and feed both into
 * [orbitIdentity] / [orbitWall] for every object, so horizontal drag spins the scene and vertical
 * drag tilts it — a turntable with pitch.
 *
 * @param radiansPerPixel drag sensitivity — radians of pitch per pixel of vertical drag.
 * @param minAngleRad most the scene can tilt one way (negative = tilt toward a side-on view).
 * @param maxAngleRad most the scene can tilt the other way (positive = tilt toward top-down).
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.verticalTiltAngle(
    radiansPerPixel: Float = 0.006f,
    minAngleRad: Float = -1.0f,
    maxAngleRad: Float = 1.0f,
): RcFloat =
    addTouch(
        defaultValue = 0f,
        // Clamp the *angle* by clamping the underlying touch travel (output = touch * radPerPx).
        range = (minAngleRad / radiansPerPixel)..(maxAngleRad / radiansPerPixel),
        stopMode = RcTouchStopMode.Gently,
        expression = touchPosY(),
        velocity = 0f,
        notchHaptic = RcHaptic.NoHaptics,
        notches = null,
        easing = null,
    ) * radiansPerPixel

/**
 * A touch-driven **open amount** (radians) from the vertical touch position, clamped to `[0,
 * maxAngleRad]`. Drag **down** to open, up to close; the value holds where released
 * ([RcTouchStopMode.Instantly], no coast). Reads [RemoteContext.FLOAT_TOUCH_POS_Y], so it composes
 * with [verticalSpinAngle] (which reads the X axis): one gesture can both turn a model (horizontal)
 * and open a hinged mouth/lid (vertical). Player-side, so it costs nothing in the document. Call in
 * the `Canvas { }` scope.
 *
 * A touch accumulator rests at the *middle* of its [range], so to rest fully **closed** the range
 * is symmetric (rest = 0); the output is then clamped to `[0, maxAngleRad]` in expression space —
 * an up-drag past closed parks the accumulator negative but the mouth just stays shut.
 *
 * @param radiansPerPixel sensitivity — radians of opening per pixel of vertical drag.
 * @param maxAngleRad the fully-open angle (dragging further has no effect).
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.verticalOpenAngle(
    radiansPerPixel: Float = 0.0016f,
    maxAngleRad: Float = 0.5f,
): RcFloat {
    val travel = maxAngleRad / radiansPerPixel
    val raw =
        addTouch(
            defaultValue = 0f,
            range = (-travel)..travel, // symmetric → rests at 0 (mouth closed)
            stopMode = RcTouchStopMode.Instantly,
            expression = touchPosY(),
            velocity = 0f,
            notchHaptic = RcHaptic.NoHaptics,
            notches = null,
            easing = null,
        ) * radiansPerPixel
    return min(max(0f, raw), maxAngleRad)
}

/**
 * Reset the model matrix and apply the touch-driven vertical-axis [spinY] (from
 * [verticalSpinAngle]). Drop-in replacement for `identity()` at the start of each object's
 * transform: because the spin is applied first (and later transforms post-multiply), every object
 * placed after it orbits the scene's vertical center together, as one rigid turntable.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScene3DScope.spunIdentity(spinY: RcFloat) {
    identity()
    rotate(spinY, 0f, 1f, 0f)
}

/**
 * Like [spunIdentity], but also applies a touch-driven [tiltX] pitch (from [verticalTiltAngle]).
 * The yaw is applied first (innermost) and the pitch about the world X axis wraps it, so horizontal
 * drag spins the scene about its vertical center and vertical drag tilts that spinning result
 * toward or away from the camera — a turntable with pitch. Drop-in replacement for `identity()`.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScene3DScope.orbitIdentity(spinY: RcFloat, tiltX: RcFloat) {
    identity()
    rotate(tiltX, 1f, 0f, 0f)
    rotate(spinY, 0f, 1f, 0f)
}

/**
 * Draws one **inward-facing** wall [quad] of a data box, spun with the scene by [spinY]. The quad
 * (a [primitivePlane], normal +Z by default) is oriented by `(ang, ax,ay,az)` so its normal points
 * toward the box center, positioned at `(tx,ty,tz)`, and scaled `(sx,sy)`. Set the texture/color
 * before calling.
 *
 * The point: draw **all** of a box's side walls inward-facing and let backface culling keep exactly
 * the ones *behind* the data for the current spin (culling the near ones). That way the grid stays
 * on the back walls as the scene rotates — unlike a couple of fixed walls placed for one camera
 * angle, which get culled (or end up in front of the data) once you turn the box around. This is
 * the approach `DslBubbleChart3D` uses for its data cube.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScene3DScope.spunWall(
    quad: RcMesh,
    mode: RcMeshMode,
    spinY: RcFloat,
    tx: Float,
    ty: Float,
    tz: Float,
    ang: Float,
    ax: Float,
    ay: Float,
    az: Float,
    sx: Float,
    sy: Float,
) {
    spunIdentity(spinY)
    translate(tx, ty, tz)
    rotate(angleRad = ang, ax = ax, ay = ay, az = az)
    scale(sx, sy, 1f)
    drawMesh(quad, mode = mode)
}

/**
 * Like [spunWall], but the wall is placed with [orbitIdentity] so it follows both the [spinY] yaw
 * and the [tiltX] pitch. Backface culling still keeps exactly the box walls *behind* the data for
 * the current view as it spins and tilts.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcScene3DScope.orbitWall(
    quad: RcMesh,
    mode: RcMeshMode,
    spinY: RcFloat,
    tiltX: RcFloat,
    tx: Float,
    ty: Float,
    tz: Float,
    ang: Float,
    ax: Float,
    ay: Float,
    az: Float,
    sx: Float,
    sy: Float,
) {
    orbitIdentity(spinY, tiltX)
    translate(tx, ty, tz)
    rotate(angleRad = ang, ax = ax, ay = ay, az = az)
    scale(sx, sy, 1f)
    drawMesh(quad, mode = mode)
}
