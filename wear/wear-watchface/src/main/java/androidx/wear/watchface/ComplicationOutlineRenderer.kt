/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import androidx.annotation.ColorInt

/**
 * Helper for rendering a thick outline around a complication. Intended for use with
 * [LayerMode#DRAW_OUTLINED].
 */
public class ComplicationOutlineRenderer {
    public companion object {
        internal const val EXPANSION_PX = 6
        internal const val STROKE_WIDTH_DP = 3.0f
        internal val outlinePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                STROKE_WIDTH_DP,
                Resources.getSystem().displayMetrics
            )
            isAntiAlias = true
        }

        /** Draws a thick line around the complication with the given bounds. */
        @JvmStatic
        public fun drawComplicationOutline(
            canvas: Canvas,
            bounds: Rect,
            @ColorInt color: Int
        ) {
            outlinePaint.color = color
            val radius = bounds.height() / 2.0f
            if (bounds.width() == bounds.height()) {
                canvas.drawCircle(
                    bounds.exactCenterX() + 1.0f, // Offset necessary to properly center.
                    bounds.exactCenterY(),
                    radius + EXPANSION_PX,
                    outlinePaint
                )
            } else {
                canvas.drawRoundRect(
                    RectF(
                        (bounds.left - EXPANSION_PX).toFloat(),
                        (bounds.top - EXPANSION_PX).toFloat(),
                        (bounds.right + EXPANSION_PX).toFloat(),
                        (bounds.bottom + EXPANSION_PX).toFloat()
                    ),
                    radius,
                    radius,
                    outlinePaint
                )
            }
        }
    }
}
