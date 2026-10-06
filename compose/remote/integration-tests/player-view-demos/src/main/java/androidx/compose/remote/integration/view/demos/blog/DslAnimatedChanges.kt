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

package androidx.compose.remote.integration.view.demos.blog

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcAnimationCurve
import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.RcFontType
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.RcWeight
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.round
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/** How long each pendulum takes to reach a new target, in seconds. */
private const val DURATION = 5f

/** One pendulum that eases toward the shared target with its own curve. */
@Suppress("RestrictedApiAndroidX")
private class Easing(
    val name: String,
    val color: Int,
    val curve: RcAnimationCurve,
    val spec: FloatArray? = null,
)

/** The pendulums, longest first. */
@Suppress("RestrictedApiAndroidX")
private val EASINGS =
    listOf(
        Easing("Standard", 0xFFFF0000.toInt(), RcAnimationCurve.CubicStandard),
        Easing("Accelerate", 0xFFFF6A00.toInt(), RcAnimationCurve.CubicAccelerate),
        Easing("Decelerate", 0xFFFFBB00.toInt(), RcAnimationCurve.CubicDecelerate),
        Easing("Anticipate", 0xFFFFFB00.toInt(), RcAnimationCurve.CubicAnticipate),
        Easing("Overshoot", 0xFFD9FF00.toInt(), RcAnimationCurve.CubicOvershoot),
        Easing("Bounce", 0xFF73FF00.toInt(), RcAnimationCurve.EaseOutBounce),
        Easing("Elastic", 0xFF00FFB2.toInt(), RcAnimationCurve.EaseOutElastic),
        Easing(
            "Cubic (0, 1, 0, 1)",
            0xFF00B2FF.toInt(),
            RcAnimationCurve.CubicCustom,
            floatArrayOf(0f, 1f, 0f, 1f),
        ),
        Easing(
            "Spline: stairs",
            0xFF0800FF.toInt(),
            RcAnimationCurve.SplineCustom,
            floatArrayOf(0f, 0f, 0.25f, 0.25f, 0.5f, 0.5f, 0.75f, 0.75f, 1f),
        ),
        Easing(
            "Spline: dip and overshoot",
            0xFF7B00FF.toInt(),
            RcAnimationCurve.SplineCustom,
            floatArrayOf(0f, 0f, -0.25f, 0f, 1f, 1.1f, 0.9f, 1f),
        ),
    )

private const val PAGE_COLOR = 0xFF121212.toInt()
private const val DISK_COLOR = 0xFF444444.toInt()
private const val GUIDE_COLOR = 0xFFAAAAAA.toInt()
private const val TARGET_COLOR = 0xFF888888.toInt()
private const val REFERENCE_COLOR = 0xFFFFFFFF.toInt()

/**
 * Animated changes: every easing curve side by side, as pendulums hung from the top centre.
 *
 * All the pendulums follow one target angle, which jumps between −20° and +20° every 8 seconds.
 * - The wide grey bar is the target itself, so it snaps.
 * - The thin white pendulum swings steadily between the two angles, for reference.
 * - Each coloured pendulum is the target wrapped in `anim(DURATION, curve)`. Whenever the target's
 *   value changes, the player eases from the old value to the new one over 5 seconds, following
 *   that curve.
 *
 * Nothing is driven from the host: the target is an expression of time, and the player runs every
 * transition itself.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslAnimatedChanges(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Animated changes"),
        experimental = true,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().background(PAGE_COLOR)) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = (w / 2f).flush()
            // Hung from the top, a pendulum of length L swings L·sin 20° ≈ 0.34 L to each side,
            // so cap L at 1.4 w to keep the swing on screen.
            val reach = min(h, w * 1.4f).flush()
            val bob = (min(w, h) * 0.025f).flush()
            val zero = 0f.rf

            // A new expression each call: anim() attaches the animation to the expression it is
            // called on, so every pendulum needs its own copy of the target.
            fun target(): RcFloat = round(seconds() / 8f) % 2f * 40f - 20f

            // Backdrop: a disk centred on the pivot, and the two extreme angles.
            paint {
                color(DISK_COLOR)
                style(RcPaintStyle.Fill)
            }
            drawCircle(cx, zero, reach)
            for (angle in floatArrayOf(-20f, 20f)) {
                save {
                    rotate(angle.rf, cx, zero)
                    paint {
                        color(GUIDE_COLOR)
                        style(RcPaintStyle.Stroke)
                        strokeWidth(1f)
                    }
                    drawLine(cx, zero, cx, reach)
                }
            }

            // The target, which snaps.
            save {
                rotate(target(), cx, zero)
                paint {
                    color(TARGET_COLOR)
                    style(RcPaintStyle.Stroke)
                    strokeWidth(w * 33f / 1000f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawLine(cx, 20f.rf, cx, reach - 20f)
            }

            // A steady swing for reference: a triangle wave with an 8-second half period.
            val steady = abs((continuousSeconds() / 2f - 2f) / 4f % 2f - 1f) * 40f - 20f
            pendulum(steady, REFERENCE_COLOR, cx, reach * 0.95f, bob)

            EASINGS.forEachIndexed { i, easing ->
                val angle = target().anim(DURATION, easing.curve, easing.spec)
                pendulum(angle, easing.color, cx, reach * (0.95f - 0.05f * i), bob)
            }

            legend(w)
        }
    }
}

/** A pendulum hung from `(cx, 0)`: a line of [length] ending in a bob, turned by [angle]. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.pendulum(
    angle: RcFloat,
    color: Int,
    cx: RcFloat,
    length: RcFloat,
    bob: RcFloat,
) {
    save {
        rotate(angle, cx, 0f.rf)
        paint {
            color(color)
            style(RcPaintStyle.Stroke)
            strokeWidth(3f)
        }
        drawLine(cx, 0f.rf, cx, length)
        paint {
            color(color)
            style(RcPaintStyle.Fill)
        }
        drawCircle(cx, length, bob)
    }
}

/** Each pendulum's name in its colour, top left, longest pendulum first. */
@Suppress("RestrictedApiAndroidX")
private fun RcCanvasScope.legend(w: RcFloat) {
    val size = w * 24f / 1000f
    val rows =
        listOf("Target (snaps)" to TARGET_COLOR, "Steady swing" to REFERENCE_COLOR) +
            EASINGS.map { it.name to it.color }
    rows.forEachIndexed { i, (name, color) ->
        paint {
            color(color)
            style(RcPaintStyle.Fill)
            textSize(size)
            typeface(RcFontType.Default, RcWeight.Bold, italic = false)
        }
        // pan (-1, -1) anchors the text's top-left corner at (x, y).
        drawTextAnchored(
            remoteText(name),
            16f,
            (i.toFloat() * size * 1.5f + 16f).toFloat(),
            -1f,
            0f,
        )
    }
}
