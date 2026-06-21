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

import android.graphics.Paint
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcSoundType
import androidx.compose.remote.creation.dsl.RcWaveform
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import kotlin.math.PI

@Suppress("RestrictedApiAndroidX")
fun dslMetronomeDemo(): ByteArray {
    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val tick =
            soundExpression(
                type = RcSoundType.Tone,
                frequency = 880f,
                durationSeconds = 0.05f,
                waveform = RcWaveform.Sine,
            )

        Canvas(modifier = Modifier.fillMaxSize().background(0xFF1A1A2E.toInt())) {
            val w = componentWidth()
            val h = componentHeight()
            val cx = w / 2f
            val lineY = h * 0.80f
            val topY = h * 0.12f
            val amplitude = lineY - topY

            val t = continuousSeconds()
            // topY + amplitude*abs(cos(t*π)): abs(cos)=1 at integer t → ballY=lineY (hit); 0 at
            // half → topY
            val ballY = lineY - amplitude * abs(cos(t * PI.toFloat()))

            // Normal green line
            paint {
                color(0xFF4CAF50.toInt())
                strokeWidth(6f)
                raw.setStyle(Paint.Style.STROKE)
            }
            drawLine(cx - 120f, lineY, cx + 120f, lineY)

            // Red flash + sound + haptic when ball is at line
            debug(" line = ", lineY - ballY)
            conditionalOperations(RcConditionOp.Lt, lineY - ballY, 64.rf) {
                paint {
                    color(0xFFFF0000.toInt())
                    strokeWidth(20f)
                    raw.setStyle(Paint.Style.STROKE)
                }
                drawLine(cx - 120f, lineY, cx + 120f, lineY)
                //                performHaptic(RcHaptic.LongPress)
                playSound(tick)
            }

            // Ball
            paint {
                color(0xFF2196F3.toInt())
                raw.setStyle(Paint.Style.FILL)
            }
            drawCircle(cx, ballY, 24f.rf)
        }
    }
}
