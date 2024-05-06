/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.view.effects

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.effects.OverlayEffect
import androidx.camera.view.PreviewView
import androidx.core.util.Consumer

private const val TAG = "BouncyLogo"

/**
 * An overlay effect that draws a logo bouncing around on the screen.
 *
 * <p>This is like the classic DVD logo bouncing around the screen, but with a CameraX logo.
 */
class BouncyLogoEffect(
    targets: Int,
    private val logoText: String,
    private val previewView: PreviewView
) :
    OverlayEffect(
        targets,
        0,
        Handler(Looper.getMainLooper()),
        Consumer { t -> Log.d(TAG, "Effect error", t) }
    ) {

    private var overlayEffectLogo: BouncyLogo? = null

    private val textPaint =
        Paint().apply {
            color = Color.YELLOW
            textSize = 100F
            typeface = Typeface.DEFAULT_BOLD
        }
    private val logoPaint =
        Paint().apply {
            color = Color.BLUE
            style = Paint.Style.FILL
        }

    init {
        setOnDrawListener { frame ->
            val sensorToUi = previewView.sensorToViewTransform
            if (sensorToUi != null) {
                // Transform the Canvas to use PreviewView coordinates.
                val sensorToEffect = frame.sensorToBufferTransform
                val uiToSensor = Matrix()
                sensorToUi.invert(uiToSensor)
                uiToSensor.postConcat(sensorToEffect)
                val canvas = frame.overlayCanvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                canvas.setMatrix(uiToSensor)

                // Gets the next position of the logo.
                if (overlayEffectLogo == null) {
                    overlayEffectLogo = BouncyLogo(previewView.width, previewView.height)
                }
                val position = overlayEffectLogo!!.getNextPosition()

                // measure the size of the text
                val bounds = Rect()
                textPaint.getTextBounds(logoText, 0, logoText.length, bounds)

                // Draw an oval and the text within.
                canvas.drawOval(
                    position.x.toFloat() - bounds.width().toFloat() * 0.8F,
                    position.y.toFloat() - bounds.height().toFloat() * 1.2F,
                    position.x.toFloat() + bounds.width().toFloat() * 0.8F,
                    position.y.toFloat() + bounds.height().toFloat() * 1.2F,
                    logoPaint
                )
                canvas.drawText(
                    logoText,
                    position.x.toFloat() - bounds.width().toFloat() / 2,
                    position.y.toFloat() + bounds.height().toFloat() / 2,
                    textPaint
                )
            }
            true
        }
    }
}
