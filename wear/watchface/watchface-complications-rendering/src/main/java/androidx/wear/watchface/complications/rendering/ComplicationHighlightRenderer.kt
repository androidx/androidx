/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.Px
import kotlin.math.floor

/** Helper for rendering a thick outline around a complication to highlight it. */
public class ComplicationHighlightRenderer(
    @Px private val outlineExpansion: Float,
    @Px outlineStrokeWidth: Float
) {
    private val transparentWhitePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(0, 255, 255, 255) // Transparent white
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    private val outlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = outlineStrokeWidth
        isAntiAlias = true
    }

    /**
     * Intended for use by [CanvasComplicationDrawable.drawHighlight]. Draws a thick line around the
     * complication with [color] and with the given bounds.  Fills the center of the complication
     * with transparent white. When composited on top of the underlying watchface the complication's
     * original pixels will be preserved with their original brightness.
     */
    public fun drawComplicationHighlight(
        canvas: Canvas,
        bounds: Rect,
        @ColorInt color: Int
    ) {
        outlinePaint.color = color
        val radius = bounds.height() / 2.0f
        if (bounds.width() == bounds.height()) {
            // Round the center coordinates to the nearest whole integer.
            val ctrX = floor(bounds.exactCenterX() + 0.5f)
            val ctrY = floor(bounds.exactCenterY() + 0.5f)

            canvas.drawCircle(
                ctrX,
                ctrY,
                radius + outlineExpansion,
                transparentWhitePaint
            )

            canvas.drawCircle(
                ctrX,
                ctrY,
                radius + outlineExpansion,
                outlinePaint
            )
        } else {
            canvas.drawRoundRect(
                RectF(
                    bounds.left.toFloat() - outlineExpansion,
                    bounds.top.toFloat() - outlineExpansion,
                    bounds.right.toFloat() + outlineExpansion,
                    bounds.bottom.toFloat() + outlineExpansion
                ),
                radius,
                radius,
                transparentWhitePaint
            )

            canvas.drawRoundRect(
                RectF(
                    bounds.left.toFloat() - outlineExpansion,
                    bounds.top.toFloat() - outlineExpansion,
                    bounds.right.toFloat() + outlineExpansion,
                    bounds.bottom.toFloat() + outlineExpansion
                ),
                radius,
                radius,
                outlinePaint
            )
        }
    }
}
