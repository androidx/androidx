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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcAnimationCurve
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcPathType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import kotlin.math.PI

/**
 * DSL conversion of `examples/Demo.kt` `plot3()` and `plot4()`.
 *
 * Demonstrates the typed path-expression API:
 * - `remoteXYPath(expressionX, expressionY, start, end, count, type)` for XY plots
 * - `remotePolarPath(expression, start, end, count, centerX, centerY, type)` for polar plots
 *
 * Plus `RcFloat.anim(duration, RcAnimationCurve)` for animation, `rFun { x -> … }` for single-var
 * functions, and `value.format(whole, decimal, spec)` returning typed `RcText` (replacing legacy
 * `genTextId(…)` Int returns).
 *
 * `plot1()`/`plot2()` are skipped — they use `writer.addThemedColor(…)` returning Short colorIds
 * (used as `painter.setColorId(short)`) which the DSL doesn't expose cleanly today — would need
 * `RcColor`-receiver overloads on the typed APIs.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslPlot3(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF127799.toInt())) {
                paint {
                    color(0xFFFF9966.toInt())
                    textSize(64f)
                }
                val minX = -10f
                val maxX = 10f
                val scale = (((seconds() / 2f) % 2f) + 1f).anim(0.5f)
                val scaleY = scale * (componentHeight() - 100f) / -10f
                val offsetY = componentHeight() / 2f
                val scaleX = (componentWidth() - 100f) / (maxX - minX)
                // Equivalent to `50f - minX * scaleX`, rearranged so RcFloat is on the
                // left and we don't need the `Float.minus(RcFloat)` / `Float.times(RcFloat)`
                // top-level extensions to be imported.
                val offsetX = scaleX * (-minX) + 50f

                Text(scale.format(1, 1, 0))

                paint {
                    strokeWidth(10f)
                    style(RcPaintStyle.Stroke)
                }
                val equ = rFun { x -> sin(x + continuousSeconds() * 3f) }
                val pathId =
                    remoteXYPath(
                        expressionX = rFun { x -> x * scaleX + offsetX },
                        expressionY = equ * scaleY + offsetY,
                        start = minX,
                        end = maxX,
                        count = 64,
                        type = RcPathType.Spline,
                    )
                drawPath(pathId)
            }
        }
    }
}

/** Polar-path variant of [dslPlot3]. */
@Suppress("RestrictedApiAndroidX")
public fun dslPlot4(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize().background(0xFF127799.toInt())) {
                paint {
                    color(0xFFFF9966.toInt())
                    textSize(64f)
                }
                val minX = 0f
                val maxX = PI.toFloat() * 2f
                val scale =
                    (((seconds() / 2f) % 2f) + 1f).anim(0.5f, RcAnimationCurve.CubicDecelerate)
                Text(scale.format(1, 1, 0))

                // Rearranged so RcFloat is on the left of each operator (avoids needing
                // `Float.times(RcFloat)` and `Float.plus(RcFloat)` extensions imported).
                val equ = rFun { x -> sin(x * 10f + continuousSeconds() * 3f) * 10f + 100f }
                val pathId =
                    remotePolarPath(
                        expression = equ * scale,
                        start = minX,
                        end = maxX,
                        count = 64,
                        centerX = componentWidth() / 2f,
                        centerY = componentHeight() / 2f,
                        type = RcPathType.Spline,
                    )
                drawPath(pathId)
            }
        }
    }
}
