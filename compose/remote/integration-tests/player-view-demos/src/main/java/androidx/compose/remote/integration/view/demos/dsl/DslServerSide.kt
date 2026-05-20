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
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcStrokeCap
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import kotlin.math.PI

/**
 * DSL conversion of `examples/ServerSide.kt` `serverClock()`.
 *
 * Demonstrates a clock face composed of three rotated hands (minute, hour, second). The original
 * was a "server-side" demo proving the writer doesn't depend on Android — it constructed a custom
 * `RcPlatformServices` with `TODO()` overrides. In the DSL, the standard
 * `RcPlatformProfiles.ANDROIDX` profile works fine because the drawing commands themselves don't
 * query the platform (paths, bitmaps would).
 */
@Suppress("RestrictedApiAndroidX")
public fun dslServerClock(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val cx = w / 2f
                val cy = h / 2f
                val rad = min(cx, cy)
                val rounding = rad / 4f

                paint { color(0xFF0000FF.toInt()) }
                drawRoundRect(0f.rf, 0f.rf, w, h, rounding, rounding)

                paint {
                    color(0xFF888888.toInt())
                    strokeWidth(32f)
                    strokeCap(RcStrokeCap.Round)
                }
                save {
                    rotate(minutes() * 6f, cx, cy)
                    drawLine(cx, cy, cx, cy - rad * 0.8f)
                }

                paint {
                    color(0xFFCCCCCC.toInt())
                    strokeWidth(16f)
                    strokeCap(RcStrokeCap.Round)
                }
                save {
                    rotate(hour() * 30f, cx, cy)
                    drawLine(cx, cy, cx, cy - rad / 2f)
                }

                paint {
                    color(0xFFFFFFFF.toInt())
                    strokeWidth(4f)
                }
                drawLine(
                    cx,
                    cy,
                    w / 2f + rad * sin(continuousSeconds() * (2 * PI.toFloat() / 60f)),
                    h / 2f - rad * cos(continuousSeconds() * (2 * PI.toFloat() / 60f)),
                )
            }
        }
    }
}
