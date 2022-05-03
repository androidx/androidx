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

package androidx.wear.compose.foundation

import android.graphics.Typeface
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt

/**
 * Used to cache computations and objects with expensive construction (Android's Paint & Path)
 */
internal actual class CurvedTextDelegate {
    private var text: String = ""
    private var clockwise: Boolean = true
    private var fontSizePx: Float = 0f
    private var fontWeight: FontWeight? = null

    actual var textWidth by mutableStateOf(0f)
    actual var textHeight by mutableStateOf(0f)
    actual var baseLinePosition = 0f

    private val paint = android.graphics.Paint().apply { isAntiAlias = true }
    private val backgroundPath = android.graphics.Path()
    private val textPath = android.graphics.Path()

    var lastLayoutInfo: CurvedLayoutInfo? = null

    actual fun updateIfNeeded(
        text: String,
        clockwise: Boolean,
        fontSizePx: Float,
        fontWeight: FontWeight?
    ) {
        if (
            text != this.text ||
            clockwise != this.clockwise ||
            fontSizePx != this.fontSizePx ||
            fontWeight != this.fontWeight
        ) {
            this.text = text
            this.clockwise = clockwise
            this.fontSizePx = fontSizePx
            this.fontWeight = fontWeight
            doUpdate()
            lastLayoutInfo = null // Ensure paths are recomputed
        }
    }

    private fun doUpdate() {
        paint.textSize = fontSizePx
        // Set the typeface for this fontWeight, or null to clear any previously specified
        // typeface if fontWeight is null.
        paint.typeface = fontWeight?.let { getNativeTypeface(it) }

        val rect = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, rect)

        textWidth = rect.width().toFloat()
        textHeight = -paint.fontMetrics.top + paint.fontMetrics.bottom
        baseLinePosition =
            if (clockwise) -paint.fontMetrics.top else paint.fontMetrics.bottom
    }

    private fun DrawScope.updatePathsIfNeeded(layoutInfo: CurvedLayoutInfo) {
        if (layoutInfo != lastLayoutInfo) {
            lastLayoutInfo = layoutInfo
            with(layoutInfo) {
                val clockwiseFactor = if (clockwise) 1f else -1f

                val sweepDegree = sweepRadians.toDegrees().coerceAtMost(360f)

                val centerX = centerOffset.x
                val centerY = centerOffset.y

                // TODO: move background drawing to a CurvedModifier
                backgroundPath.reset()
                backgroundPath.arcTo(
                    centerX - outerRadius,
                    centerY - outerRadius,
                    centerX + outerRadius,
                    centerY + outerRadius,
                    startAngleRadians.toDegrees(),
                    sweepDegree, false
                )
                backgroundPath.arcTo(
                    centerX - innerRadius,
                    centerY - innerRadius,
                    centerX + innerRadius,
                    centerY + innerRadius,
                    startAngleRadians.toDegrees() + sweepDegree,
                    -sweepDegree, false
                )
                backgroundPath.close()

                textPath.reset()
                textPath.addArc(
                    centerX - measureRadius,
                    centerY - measureRadius,
                    centerX + measureRadius,
                    centerY + measureRadius,
                    startAngleRadians.toDegrees() +
                        (if (clockwise) 0f else sweepDegree),
                    clockwiseFactor * sweepDegree
                )
            }
        }
    }

    actual fun DrawScope.doDraw(
        layoutInfo: CurvedLayoutInfo,
        parentSweepRadians: Float,
        overflow: TextOverflow,
        color: Color,
        background: Color
    ) {
        updatePathsIfNeeded(layoutInfo)

        drawIntoCanvas { canvas ->
            if (background.isSpecified && background != Color.Transparent) {
                paint.color = background.toArgb()
                canvas.nativeCanvas.drawPath(backgroundPath, paint)
            }

            paint.color = color.toArgb()
            val actualText = if (
                // Float arithmetic can make the parentSweepRadians slightly smaller
                layoutInfo.sweepRadians <= parentSweepRadians + 0.001f ||
                overflow == TextOverflow.Visible
            ) {
                text
            } else {
                ellipsize(
                    text, TextPaint(paint), overflow == TextOverflow.Ellipsis,
                    (parentSweepRadians * layoutInfo.measureRadius).roundToInt()
                )
            }
            canvas.nativeCanvas.drawTextOnPath(actualText, textPath, 0f, 0f, paint)
        }
    }

    private fun ellipsize(
        text: String,
        paint: TextPaint,
        addEllipsis: Boolean,
        ellipsizedWidth: Int,
    ): String {
        // Code reused from wear/wear/src/main/java/androidx/wear/widget/CurvedTextView.java
        val layoutBuilder =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, ellipsizedWidth)
        layoutBuilder.setEllipsize(if (addEllipsis) TextUtils.TruncateAt.END else null)
        layoutBuilder.setMaxLines(1)
        val layout = layoutBuilder.build()

        // Cut text that it's too big when in TextOverFlow.Clip mode.
        if (!addEllipsis) {
            return text.substring(0, layout.getLineEnd(0))
        }
        val ellipsisCount = layout.getEllipsisCount(0)
        if (ellipsisCount == 0) {
            return text
        }
        val ellipsisStart = layout.getEllipsisStart(0)
        // "\u2026" is unicode's ellipsis "..."
        return text.replaceRange(ellipsisStart, ellipsisStart + ellipsisCount, "\u2026")
    }

    private fun getNativeTypeface(
        fontWeight: FontWeight
    ): Typeface {
        return if (Build.VERSION.SDK_INT < 28) {
            Typeface.defaultFromStyle(
                if (fontWeight >= FontWeight.W600 /* AndroidBold */)
                    Typeface.BOLD
                else
                    Typeface.NORMAL
            )
        } else {
            TypefaceHelperMethodsApi28.create(
                Typeface.DEFAULT,
                fontWeight.weight,
                /* italic = */ false
            )
        }
    }
}

@RequiresApi(28)
internal object TypefaceHelperMethodsApi28 {
    @RequiresApi(28)
    @DoNotInline
    fun create(typeface: Typeface, finalFontWeight: Int, finalFontStyle: Boolean) =
        Typeface.create(typeface, finalFontWeight, finalFontStyle)
}