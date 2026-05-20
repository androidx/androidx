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
import androidx.compose.remote.creation.dsl.RcTextFromFloatSpec
import androidx.compose.remote.creation.dsl.RcTileMode
import androidx.compose.remote.creation.dsl.cos
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.hypot
import androidx.compose.remote.creation.dsl.min
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import kotlin.math.PI

/**
 * DSL conversion of `examples/ExampleTimer.kt` `basicTimer()`.
 *
 * A circular timer with radial-gradient background, sweep arc proportional to seconds, scaling
 * countdown digit, and a clock-style hand. Same drawing primitives as [dslDemoUseOfGlobal]'s
 * `clockPanel` but without the Column wrap.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslBasicTimer(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = componentWidth()
                val h = componentHeight()
                val cx = w / 2f
                val cy = h / 2f
                val rad = min(cx, cy)
                val rad2 = hypot(cx, cy)

                paint {
                    radialGradient(
                        centerX = cx.toFloat(),
                        centerY = cy.toFloat(),
                        radius = rad2.toFloat(),
                        colors = intArrayOf(0xFFCCCCCC.toInt(), 0xFF444444.toInt()),
                        positions = floatArrayOf(0f, 2f),
                        tileMode = RcTileMode.Clamp,
                    )
                }
                drawRoundRect(0f.rf, 0f.rf, w, h, rad / 4f, rad / 4f)

                paint {
                    color(0x99888888.toInt())
                    raw.setShader(0)
                    strokeWidth(32f)
                    strokeCap(RcStrokeCap.Round)
                }
                drawSector(
                    rad * -1f,
                    rad * -1f,
                    w + rad,
                    h + rad,
                    -90f.rf,
                    (continuousSeconds() * 360f) % 360f,
                )

                paint {
                    color(0xFF000000.toInt())
                    textSize(512f)
                }
                val id =
                    createTextFromFloat(
                        (continuousSeconds() % 10f) * -1f + 9.999f,
                        1,
                        0,
                        RcTextFromFloatSpec.Default,
                    )
                save {
                    scale(continuousSeconds() % 1f, continuousSeconds() % 1f, cx, cy)
                    drawTextAnchored(id, cx, cy, 0f.rf, 0f.rf, 0)
                }

                paint {
                    color(0xFFFFFFFF.toInt())
                    strokeWidth(4f)
                }
                drawLine(
                    cx,
                    cy,
                    w / 2f + rad * sin(continuousSeconds() * (2 * PI.toFloat())),
                    h / 2f - rad * cos(continuousSeconds() * (2 * PI.toFloat())),
                )
            }
        }
    }
}
