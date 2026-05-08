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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.creation.dsl.*
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun HostileActorImagePreview() {
    RemoteDocPreview(RemoteDocument(demoImage()))
}

@Suppress("RestrictedApiAndroidX")
@Composable
@Preview
fun HostileActorImageColorPreview() {
    RemoteDocPreview(RemoteDocument(demoImageColor()))
}

@Suppress("RestrictedApiAndroidX")
fun demoImage(): ByteArray {
    return createRawRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val w = windowWidth()
        val h = windowHeight()
        val cx = w * 0.5f

        // Simulating: count = max( (sec * 200) % 2000, 100 )
        val sec = continuousSeconds()
        val count = max((sec * 200f) % 2000f, 100f.rf).flush()

        val ball = remoteBitmap(initBall(0x10101))

        rcLoop(0f.rf, 1f, count) { index ->
            val rx = ((index * 12.34f + sec).sin() + 1f) * 0.5f * w
            val ry = ((index * 56.78f + sec).sin() + 1f) * 0.5f * h
            drawScaledBitmap(
                ball,
                0f.rf,
                0f.rf,
                50f.rf,
                50f.rf,
                rx,
                ry,
                rx + 50f,
                ry + 50f,
                RcContentScale.Fit,
                3f.rf,
                "test image",
            )
        }

        val text = count.format(5, 2, 0)
        applyPaint {
            setTextSize(32f)
            setColor(Color.RED)
        }
        drawTextAnchored(text, cx, 20f.rf, 0f.rf, 0f.rf, DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE)
    }
}

@Suppress("RestrictedApiAndroidX")
fun demoImageColor(): ByteArray {
    return createRawRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val w = windowWidth()
        val h = windowHeight()
        val cx = w * 0.5f

        val sec = continuousSeconds()
        val count = max((sec * 200f) % 1000f, 20f.rf).flush()

        val redBall = remoteBitmap(initBall(0x10000))
        val greenBall = remoteBitmap(initBall(0x100))
        val blueBall = remoteBitmap(initBall(0x1))

        rcLoop(0f.rf, 1f, count) { index ->
            val rx1 = ((index * 12.34f + sec).sin() + 1f) * 0.5f * w
            val ry1 = ((index * 56.78f + sec).sin() + 1f) * 0.5f * h
            drawScaledBitmap(
                redBall,
                0f.rf,
                0f.rf,
                50f.rf,
                50f.rf,
                rx1,
                ry1,
                rx1 + 50f,
                ry1 + 50f,
                RcContentScale.Fit,
                3f.rf,
                "test image",
            )

            val rx2 = ((index * 34.56f + sec * 1.2f).sin() + 1f) * 0.5f * w
            val ry2 = ((index * 78.90f + sec * 1.2f).sin() + 1f) * 0.5f * h
            drawScaledBitmap(
                greenBall,
                0f.rf,
                0f.rf,
                50f.rf,
                50f.rf,
                rx2,
                ry2,
                rx2 + 50f,
                ry2 + 50f,
                RcContentScale.Fit,
                3f.rf,
                "test image",
            )

            val rx3 = ((index * 90.12f + sec * 1.5f).sin() + 1f) * 0.5f * w
            val ry3 = ((index * 23.45f + sec * 1.5f).sin() + 1f) * 0.5f * h
            drawScaledBitmap(
                blueBall,
                0f.rf,
                0f.rf,
                50f.rf,
                50f.rf,
                rx3,
                ry3,
                rx3 + 50f,
                ry3 + 50f,
                RcContentScale.Fit,
                3f.rf,
                "test image",
            )
        }

        val text = count.format(5, 2, 0)
        applyPaint {
            setTextSize(32f)
            setColor(Color.RED)
        }
        drawTextAnchored(text, cx, 20f.rf, 0f.rf, 0f.rf, DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE)

        applyPaint {
            setColor(Color.RED)
            setStyle(Paint.Style.STROKE)
            setStrokeWidth(3f)
        }
        drawRoundRect(0f.rf, 0f.rf, w, h, 25f.rf, 25f.rf)
    }
}

@Suppress("RestrictedApiAndroidX")
private fun initBall(color: Int): Bitmap {
    val ball = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    val w = ball.width
    val h = ball.height
    val cx = w / 2f
    val cy = h / 2f
    val radius = cx * 0.9f
    val radius2 = radius * radius
    val data = IntArray(w * h)
    for (i in data.indices) {
        val x = i % w
        val y = i / w
        val dx = x - cx
        val dy = y - cy
        val dist2 = dx * dx + dy * dy
        if (dist2 > radius2) {
            continue
        }
        val norm2 = radius * radius - dist2
        val bright = (norm2 * 255 / radius2).toInt()
        data[i] = 0x33000000 + color * bright
    }
    ball.setPixels(data, 0, w, 0, 0, w, h)
    return ball
}
