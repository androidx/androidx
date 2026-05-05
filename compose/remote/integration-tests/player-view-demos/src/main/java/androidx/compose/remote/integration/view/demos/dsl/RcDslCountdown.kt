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

import androidx.compose.remote.core.RemoteComposeBuffer.PAD_AFTER_ZERO
import androidx.compose.remote.creation.RcPaint
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.createRawRcBuffer
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun RcDslCountdownPreview() {
    RemoteDocPreview(RemoteDocument(dslCountdown()))
}

/** Reimplementation of Countdown using the new type-safe DSL. */
@Suppress("RestrictedApiAndroidX")
fun dslCountdown(): ByteArray {
    val colors =
        intArrayOf(
            Color.hsv(0f, 0.9f, 0.9f).toArgb(),
            Color.hsv(60f, 0.9f, 0.9f).toArgb(),
            Color.hsv(120f, 0.9f, 0.9f).toArgb(),
            Color.hsv(180f, 0.9f, 0.9f).toArgb(),
            Color.hsv(240f, 0.9f, 0.9f).toArgb(),
            Color.hsv(300f, 0.9f, 0.9f).toArgb(),
            Color.hsv(360f, 0.9f, 0.9f).toArgb(),
        )
    return createRawRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = false) {
        applyPaint { setTextSize(50f) }
        val pi2 = (PI * 2).toFloat()
        val sec = continuousSeconds()
        val x = ((sec * 3f).cos() * 200f + 300f).flush()
        val y = ((sec * 3f).sin() * 200f + 300f).flush()
        val hue = (((sec * 3f) / pi2) % 1f).flush()
        // debug("hue ", hue)
        applyPaint { setSweepGradient(300f, 300f, colors, null) }

        drawCircle(300f, 300f, 200f)

        applyPaint { setShader(0) }

        val color1 = remoteColorExpression(0x8F, hue, 0.9f, 0.9f)
        applyPaint { setColor(color1) }
        drawCircle(x, y, 100f.rf)

        applyPaint {
            setColor(Color.Blue.toArgb())
            setTextSize(100f)
            setTypeface(RcPaint.FONT_TYPE_MONOSPACE)
        }

        val textId = hue.format(1, 2, PAD_AFTER_ZERO)
        val merge = remoteText("Hue:") + textId
        save()
        scale(hue, hue, 300f.rf, 300f.rf)
        drawTextAnchored(merge, 300f, 300f, 0f, 0f, 2)
        restore()

        for (i in 0..5) {
            val color2 = remoteColorExpression(0x8F, i / 6f, 0.9f, 0.9f)
            applyPaint { setColor(color2) }
            val angle = i * Math.PI * 2f / 6
            val cx = 300 + (cos(angle) * 200).toFloat()
            val cy = 300 + (sin(angle) * 200).toFloat()
            drawCircle(cx, cy, 100f)
        }
    }
}
