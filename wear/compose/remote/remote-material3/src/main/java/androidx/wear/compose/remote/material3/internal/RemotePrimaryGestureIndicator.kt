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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3.internal

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.cubicEasing
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.wear.compose.remote.material3.LocalRemoteContentColor
import androidx.wear.compose.remote.material3.RemoteIcon

/**
 * The one-handed gesture primary (double-pinch) indicator, drawn as Remote Compose paths.
 *
 * This is a mechanical port of `drawPrimaryGesture()` from
 * `androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicatorCommon`, which draws
 * the same animation natively in Compose. The control points here were generated from that file, so
 * the two definitions share identical geometry; if UX changes the shape, both must be updated.
 *
 * This renders the animated double-pinch gesture hint across a 617ms animation cycle. Piecewise
 * easing curves are summed without branching — see [primaryGestureStageFromTime].
 *
 * @param progress The progress of the gesture animation in the range 0f..1f.
 * @param modifier The [RemoteModifier] to be applied to the indicator icon.
 * @param tint The tint [RemoteColor] to apply to the indicator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RemoteComposable
@Composable
@Suppress("RestrictedApiAndroidX") // createReference, RemoteFloat.times
public fun RemotePrimaryGestureIndicator(
    progress: RemoteFloat,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor = LocalRemoteContentColor.current,
) {
    val imageVector =
        remember(progress) {
            val stage =
                primaryGestureStageFromTime((progress * Tokens.DurationSeconds).createReference())
            primaryGestureImageVector(stage)
        }
    RemoteIcon(
        imageVector = imageVector,
        contentDescription = Tokens.ContentDescription.rs,
        modifier = modifier,
        tint = tint,
    )
}

/** Evaluates `stage` (0f..4f) at a given point in time [t] (in seconds). */
@Suppress("RestrictedApiAndroidX") // createReference
private fun primaryGestureStageFromTime(t: RemoteFloat): RemoteFloat {
    // Hoisting via createReference() assigns `stage` to a single document variable ID.
    // Because `stage` is referenced multiple times when deriving interpolation weights
    // (w0 and w2), this prevents duplicating the 4-segment compound easing expression
    // tree in the document buffer and ensures it is evaluated only once per frame.
    return (PrimaryTweens.PinchIn1.easedProgress(t) +
            PrimaryTweens.PinchOut1.easedProgress(t) +
            PrimaryTweens.PinchIn2.easedProgress(t) +
            PrimaryTweens.Return.easedProgress(t))
        .createReference()
}

/**
 * One of the `tween(durationMillis = ..., easing = CubicBezierEasing(...))` pairs of the Compose
 * original, rebased onto an absolute position in the cycle.
 */
private class EasedSegment(
    private val startSeconds: Float,
    private val durationSeconds: Float,
    private val easeX1: Float,
    private val easeY1: Float,
    private val easeX2: Float,
    private val easeY2: Float,
) {
    /**
     * 0 before this tween starts, its eased 0..1 progress while it runs, 1 once it has finished.
     */
    @Suppress("RestrictedApiAndroidX") // cubicEasing
    fun easedProgress(t: RemoteFloat): RemoteFloat =
        cubicEasing(
            easeX1.rf,
            easeY1.rf,
            easeX2.rf,
            easeY2.rf,
            clamp((t - startSeconds) / durationSeconds, 0f.rf, 1f.rf),
        )
}

/**
 * The four back to back tweens of `drawPrimaryGesture()`, in order.
 *
 * The names, durations and control points are those of the `animateTo` calls and `PrimaryEase*`
 * easings in `OneHandedGestureIndicatorCommon`, so the two can be compared directly.
 */
private object PrimaryTweens {
    /** Stage 0 -> 1: first pinch contact, 0 -> 131ms. */
    val PinchIn1 =
        EasedSegment(
            startSeconds = 0f,
            durationSeconds = 0.131f,
            easeX1 = 0.75f,
            easeY1 = 0f,
            easeX2 = 0.8f,
            easeY2 = 1f,
        )

    /** Stage 1 -> 2: partial release, 131 -> 300ms. */
    val PinchOut1 =
        EasedSegment(
            startSeconds = 0.131f,
            durationSeconds = 0.169f,
            easeX1 = 0.31f,
            easeY1 = 0f,
            easeX2 = 0.833f,
            easeY2 = 1f,
        )

    /** Stage 2 -> 3: second pinch contact, 300 -> 433ms. */
    val PinchIn2 =
        EasedSegment(
            startSeconds = 0.300f,
            durationSeconds = 0.133f,
            easeX1 = 0.167f,
            easeY1 = 0.167f,
            easeX2 = 0.8f,
            easeY2 = 1f,
        )

    /** Stage 3 -> 4: return to rest, 433 -> 617ms. */
    val Return =
        EasedSegment(
            startSeconds = 0.433f,
            durationSeconds = 0.184f,
            easeX1 = 0.31f,
            easeY1 = 0f,
            easeX2 = 0.25f,
            easeY2 = 1f,
        )
}

/**
 * The Remote Compose equivalent of `lerpPrimary(p0, p1, p2, stage)`.
 *
 * `lerpPrimary` ping-pongs p0 -> p1 -> p2 -> p1 -> p0 over the four stages using a `when`. That is
 * the same thing as the weighted sum `w0*p0 + w1*p1 + w2*p2`, where the weights depend only on
 * `stage` and not on the coordinate:
 * ```
 * w0 = clamp(1 - stage) + clamp(stage - 3)
 * w2 = clamp(stage - 1) - clamp(stage - 2)
 * w1 = 1 - w0 - w2
 * ```
 *
 * That matters for document size. Emitting the branch per coordinate would write a four-way select
 * 114 times; hoisting the three weights into document variables via `createReference()` leaves each
 * coordinate as three multiplies and two adds against ids that are evaluated once per frame.
 */
@Suppress("RestrictedApiAndroidX") // createReference
private class PrimaryGestureStage(stage: RemoteFloat) {
    private val w0 =
        (clamp((stage - 1f) * -1f, 0f.rf, 1f.rf) + clamp(stage - 3f, 0f.rf, 1f.rf))
            .createReference()

    private val w2 =
        (clamp(stage - 1f, 0f.rf, 1f.rf) - clamp(stage - 2f, 0f.rf, 1f.rf)).createReference()

    private val w1 = ((w0 + w2 - 1f) * -1f).createReference()

    // x() and y() are deliberately the same computation: the viewport is square and every offset in
    // the Compose original is measured from its centre. They are kept separate so that the call
    // sites below read as coordinate pairs.

    /** An x coordinate, relative to the centre of the viewport. */
    fun x(p0: Float, p1: Float, p2: Float): RemoteFloat = interpolate(p0, p1, p2) + Tokens.Center

    /** A y coordinate, relative to the centre of the viewport. */
    fun y(p0: Float, p1: Float, p2: Float): RemoteFloat = interpolate(p0, p1, p2) + Tokens.Center

    private fun interpolate(p0: Float, p1: Float, p2: Float): RemoteFloat =
        w0 * p0 + w1 * p1 + w2 * p2
}

private object Tokens {
    /** Matches the 36x36 viewport the Compose version scales into. */
    const val Viewport = 36f

    /** All coordinate offsets in the Compose original are measured from the viewport centre. */
    const val Center = Viewport / 2f

    /**
     * The size the native indicator draws the hint at: `OneHandedGestureDefaults.indicatorSize` is
     * `OneHandedGestureIndicatorSize.Medium`, which is 36dp. That matches [Viewport], so one
     * viewport unit is one dp.
     */
    const val DefaultSizeDp = 36

    const val StrokeWidth = 2f

    /** 617ms of animation for the full double-pinch gesture hint. */
    const val DurationSeconds = 0.617f

    /**
     * Fixed here because a demo has nothing better. A real hint should take this from
     * `GestureDescriptor.getAccessibilityDescription()`, since the gesture bound to the primary
     * action is a runtime value and may be a wrist turn rather than a double pinch.
     */
    const val ContentDescription = "Double pinch to act"

    /** The stroke is drawn white and recoloured by the icon tint, as vector assets are. */
    val StrokeBrush = SolidColor(Color.White)
}

/**
 * Builds the three stroked contours of the primary gesture at the given [stage].
 *
 * Generated from `drawPrimaryGesture()`; see the class docs above.
 */
private fun primaryGestureImageVector(stage: RemoteFloat): RemoteImageVector {
    val s = PrimaryGestureStage(stage)
    return RemoteImageVector.Builder(
            defaultWidth = Tokens.DefaultSizeDp.rdp,
            defaultHeight = Tokens.DefaultSizeDp.rdp,
            viewportWidth = Tokens.Viewport.rf,
            viewportHeight = Tokens.Viewport.rf,
            tintColor = Color.White.rc,
            name = "PrimaryGesture",
        )
        // 1. Index finger & thumb contour loop
        .path(
            name = "handBody",
            stroke = Tokens.StrokeBrush,
            strokeLineWidth = Tokens.StrokeWidth.rf,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(s.x(-2.00f, -1.60f, -1.91f), s.y(-4.58f, -4.28f, -4.51f))
            cubicTo(
                s.x(0.96f, 2.27f, 1.26f),
                s.y(-8.38f, -7.65f, -8.21f),
                s.x(5.49f, 6.02f, 5.61f),
                s.y(-10.17f, -7.65f, -9.59f),
                s.x(7.00f, 8.38f, 7.31f),
                s.y(-10.45f, -7.17f, -9.70f),
            )
            cubicTo(
                s.x(8.82f, 10.37f, 9.17f),
                s.y(-10.87f, -6.76f, -9.93f),
                s.x(10.72f, 12.38f, 11.10f),
                s.y(-11.07f, -5.24f, -9.74f),
                s.x(12.47f, 13.43f, 12.68f),
                s.y(-10.91f, -3.84f, -9.29f),
            )
            cubicTo(
                s.x(13.41f, 14.00f, 13.54f),
                s.y(-10.82f, -3.08f, -9.05f),
                s.x(14.65f, 14.66f, 14.65f),
                s.y(-10.74f, -1.93f, -8.73f),
                s.x(15.15f, 14.47f, 15.00f),
                s.y(-9.86f, -0.99f, -7.84f),
            )
            cubicTo(
                s.x(15.66f, 14.32f, 15.36f),
                s.y(-8.98f, -0.24f, -6.99f),
                s.x(15.67f, 13.69f, 15.21f),
                s.y(-8.28f, 0.32f, -6.32f),
                s.x(14.92f, 12.62f, 14.40f),
                s.y(-7.60f, 0.20f, -5.82f),
            )
            cubicTo(
                s.x(14.47f, 11.98f, 13.90f),
                s.y(-7.29f, 0.13f, -5.59f),
                s.x(13.54f, 11.13f, 12.99f),
                s.y(-7.06f, -0.51f, -5.56f),
                s.x(13.04f, 10.63f, 12.49f),
                s.y(-7.07f, -0.92f, -5.67f),
            )
            cubicTo(
                s.x(11.04f, 8.84f, 10.54f),
                s.y(-7.14f, -2.40f, -6.06f),
                s.x(7.57f, 7.97f, 7.66f),
                s.y(-6.65f, -3.25f, -5.87f),
                s.x(5.04f, 4.87f, 5.00f),
                s.y(-4.83f, -2.40f, -4.28f),
            )
            cubicTo(
                s.x(3.65f, 3.22f, 3.55f),
                s.y(-3.84f, -1.96f, -3.41f),
                s.x(2.02f, 2.15f, 2.05f),
                s.y(-2.15f, -0.88f, -1.86f),
                s.x(1.74f, 1.54f, 1.69f),
                s.y(-0.40f, 0.00f, -0.31f),
            )
            cubicTo(
                s.x(1.32f, 0.32f, 1.09f),
                s.y(2.16f, 1.75f, 2.07f),
                s.x(2.28f, 0.74f, 1.93f),
                s.y(3.98f, 3.45f, 3.86f),
                s.x(3.66f, 1.91f, 3.26f),
                s.y(4.57f, 4.27f, 4.50f),
            )
            cubicTo(
                s.x(5.90f, 5.12f, 5.72f),
                s.y(5.53f, 6.50f, 5.75f),
                s.x(10.10f, 8.68f, 9.77f),
                s.y(2.87f, 1.57f, 2.57f),
                s.x(12.29f, 11.11f, 12.02f),
                s.y(3.93f, 1.40f, 3.35f),
            )
            cubicTo(
                s.x(12.64f, 11.50f, 12.38f),
                s.y(4.10f, 1.37f, 3.48f),
                s.x(13.11f, 12.14f, 12.89f),
                s.y(4.57f, 1.54f, 3.88f),
                s.x(13.23f, 12.45f, 13.05f),
                s.y(4.99f, 1.85f, 4.27f),
            )
            cubicTo(
                s.x(13.84f, 14.09f, 13.90f),
                s.y(7.21f, 3.46f, 6.35f),
                s.x(11.15f, 11.90f, 11.32f),
                s.y(7.50f, 5.06f, 6.94f),
                s.x(9.93f, 11.04f, 10.18f),
                s.y(7.89f, 6.01f, 7.46f),
            )
            cubicTo(
                s.x(8.53f, 10.05f, 8.88f),
                s.y(8.35f, 7.10f, 8.06f),
                s.x(6.65f, 6.65f, 6.65f),
                s.y(9.67f, 9.67f, 9.67f),
                s.x(4.17f, 4.17f, 4.17f),
                s.y(10.85f, 10.85f, 10.85f),
            )
            cubicTo(
                s.x(2.19f, 2.19f, 2.19f),
                s.y(11.78f, 11.78f, 11.78f),
                s.x(-0.20f, 0.22f, -0.11f),
                s.y(12.60f, 12.39f, 12.55f),
                s.x(-2.69f, -2.27f, -2.59f),
                s.y(12.71f, 12.50f, 12.66f),
            )
            cubicTo(
                s.x(-4.31f, -3.89f, -4.22f),
                s.y(12.77f, 12.56f, 12.73f),
                s.x(-5.97f, -5.82f, -5.94f),
                s.y(12.54f, 12.38f, 12.50f),
                s.x(-7.58f, -7.37f, -7.53f),
                s.y(11.84f, 11.49f, 11.76f),
            )
        }
        // 2. Inner knuckle/crease
        .path(
            name = "knuckle",
            stroke = Tokens.StrokeBrush,
            strokeLineWidth = Tokens.StrokeWidth.rf,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(s.x(-5.12f, -4.47f, -4.97f), s.y(-7.51f, -7.01f, -7.40f))
            cubicTo(
                s.x(-2.05f, 0.67f, -1.43f),
                s.y(-10.56f, -11.75f, -10.83f),
                s.x(3.77f, 7.62f, 4.65f),
                s.y(-15.11f, -12.90f, -14.60f),
                s.x(9.72f, 9.52f, 9.68f),
                s.y(-11.20f, -7.15f, -10.28f),
            )
        }
        // 3. Lower wrist contour
        .path(
            name = "lowerWrist",
            stroke = Tokens.StrokeBrush,
            strokeLineWidth = Tokens.StrokeWidth.rf,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(s.x(0.99f, 1.89f, 1.19f), s.y(-12.01f, -10.86f, -11.74f))
            cubicTo(
                s.x(-1.14f, 0.32f, -0.80f),
                s.y(-12.35f, -12.00f, -12.27f),
                s.x(-3.70f, -3.00f, -3.54f),
                s.y(-13.14f, -12.44f, -12.98f),
                s.x(-7.61f, -6.91f, -7.45f),
                s.y(-10.01f, -9.31f, -9.85f),
            )
            cubicTo(
                s.x(-8.75f, -8.05f, -8.59f),
                s.y(-9.10f, -8.40f, -8.93f),
                s.x(-10.69f, -10.36f, -10.61f),
                s.y(-7.01f, -6.29f, -6.85f),
                s.x(-12.01f, -11.68f, -11.93f),
                s.y(-3.14f, -2.42f, -2.98f),
            )
            cubicTo(
                s.x(-13.33f, -13.00f, -13.25f),
                s.y(0.73f, 1.45f, 0.89f),
                s.x(-13.74f, -13.41f, -13.67f),
                s.y(2.77f, 2.79f, 2.77f),
                s.x(-15.15f, -14.82f, -15.07f),
                s.y(4.67f, 4.69f, 4.68f),
            )
        }
        .build()
}
