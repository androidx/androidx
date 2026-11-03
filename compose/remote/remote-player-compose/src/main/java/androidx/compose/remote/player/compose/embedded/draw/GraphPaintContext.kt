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

package androidx.compose.remote.player.compose.embedded.draw

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.ui.text.TextMeasurer
import java.util.HashMap
import kotlin.math.roundToInt

/**
 * A draw-nothing [PaintContext] for evaluating value-producing [PaintOperation]s (such as
 * `TextMeasure`, `ColorAttribute`, and `ImageAttribute`) in [GraphContext] and the embedded draw
 * pass.
 */
internal class GraphPaintContext(
    context: RemoteContext,
    private val readContext: RemoteContext = context,
    internal var textMeasurer: TextMeasurer? = null,
) : PaintContext(context) {
    internal var paintState: ComposeLocalPaint = ComposeLocalPaint()
    private val paintStack = ArrayDeque<ComposeLocalPaint>()
    private val tmpRect = Rect()
    private var cachedFontMetrics: Paint.FontMetrics? = null

    override fun drawBitmap(
        imageId: Int,
        srcLeft: Int,
        srcTop: Int,
        srcRight: Int,
        srcBottom: Int,
        dstLeft: Int,
        dstTop: Int,
        dstRight: Int,
        dstBottom: Int,
        cdId: Int,
    ) {}

    override fun scale(scaleX: Float, scaleY: Float) {}

    override fun translate(translateX: Float, translateY: Float) {}

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {}

    override fun drawSector(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {}

    override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {}

    override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {}

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {}

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {}

    override fun drawPath(id: Int, start: Float, end: Float) {}

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {}

    override fun savePaint() {
        paintStack.addLast(paintState.copy())
    }

    override fun restorePaint() {
        if (paintStack.isNotEmpty()) {
            paintState = paintStack.removeLast()
        }
    }

    override fun replacePaint(paintBundle: PaintBundle) {
        paintState = ComposeLocalPaint()
        updatePaintFromBundle(paintBundle, paintState, readContext)
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
    ) {}

    override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {}

    override fun getTextBounds(textId: Int, start: Int, end: Int, flags: Int, bounds: FloatArray) {
        val str = getText(textId)
        if (str == null) {
            bounds[0] = 0f
            bounds[1] = 0f
            bounds[2] = 0f
            bounds[3] = 0f
            return
        }
        val safeStart = start.coerceIn(0, str.length)
        val safeEnd =
            if (end == -1 || end > str.length) {
                str.length
            } else {
                end.coerceIn(safeStart, str.length)
            }
        val paint = paintState.toNativeTextPaint(readContext)
        val fontMetrics = cachedFontMetrics ?: paint.fontMetrics.also { cachedFontMetrics = it }
        paint.getFontMetrics(fontMetrics)
        paint.getTextBounds(str, safeStart, safeEnd, tmpRect)
        if ((flags and TEXT_MEASURE_SPACES) != 0) {
            bounds[0] = 0f
            bounds[2] = paint.measureText(str, safeStart, safeEnd)
        } else {
            bounds[0] = tmpRect.left.toFloat()
            if ((flags and TEXT_MEASURE_MONOSPACE_WIDTH) != 0) {
                bounds[2] = paint.measureText(str, safeStart, safeEnd) - tmpRect.left
            } else {
                bounds[2] = tmpRect.right.toFloat()
            }
        }
        if ((flags and TEXT_MEASURE_FONT_HEIGHT) != 0) {
            bounds[1] = fontMetrics.ascent.roundToInt().toFloat()
            bounds[3] = fontMetrics.descent.roundToInt().toFloat()
        } else {
            bounds[1] = tmpRect.top.toFloat()
            bounds[3] = tmpRect.bottom.toFloat()
        }
    }

    override fun layoutComplexText(
        textId: Int,
        start: Int,
        end: Int,
        alignment: Int,
        overflow: Int,
        maxLines: Int,
        maxWidth: Float,
        maxHeight: Float,
        letterSpacing: Float,
        lineHeightAdd: Float,
        lineHeightMultiplier: Float,
        lineBreakStrategy: Int,
        hyphenationFrequency: Int,
        justificationMode: Int,
        useUnderline: Boolean,
        strikethrough: Boolean,
        flags: Int,
    ): RcPlatformServices.ComputedTextLayout? = null

    override fun drawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {}

    override fun drawComplexText(computedTextLayout: RcPlatformServices.ComputedTextLayout?) {}

    override fun drawTweenPath(
        path1Id: Int,
        path2Id: Int,
        tween: Float,
        start: Float,
        end: Float,
    ) {}

    override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}

    override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}

    override fun applyPaint(mPaintData: PaintBundle) {
        updatePaintFromBundle(mPaintData, paintState, readContext)
    }

    override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}

    override fun matrixTranslate(translateX: Float, translateY: Float) {}

    override fun matrixSkew(skewX: Float, skewY: Float) {}

    override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}

    override fun matrixSave() {}

    override fun matrixRestore() {}

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {}

    override fun clipPath(pathId: Int, regionOp: Int) {}

    override fun roundedClipRect(
        width: Float,
        height: Float,
        topStart: Float,
        topEnd: Float,
        bottomStart: Float,
        bottomEnd: Float,
    ) {}

    override fun reset() {}

    override fun startGraphicsLayer(w: Int, h: Int) {}

    override fun setGraphicsLayer(attributes: HashMap<Int, Any>) {}

    override fun endGraphicsLayer() {}

    override fun getText(id: Int): String? = readContext.getText(id)

    /**
     * Receives `MatrixFromPath.paint` so the embedded draw pass can apply the path-derived matrix
     * to its own canvas; when null (graph evaluation) the op is ignored like the other matrix ops
     * here.
     */
    internal var onMatrixFromPath:
        ((pathId: Int, fraction: Float, vOffset: Float, flags: Int) -> Unit)? =
        null

    override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {
        onMatrixFromPath?.invoke(pathId, fraction, vOffset, flags)
    }

    override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
}
