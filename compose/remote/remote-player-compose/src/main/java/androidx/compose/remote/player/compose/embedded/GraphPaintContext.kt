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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Typeface
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.util.HashMap

/**
 * A draw-nothing [PaintContext] for evaluating value-producing
 * [androidx.compose.remote.core.PaintOperation]s (such as `TextMeasure`, `ColorAttribute`, and
 * `ImageAttribute`) in [GraphContext] and the embedded draw pass, using Compose's [TextMeasurer]
 * for cached text measurement.
 */
internal class GraphPaintContext(
    context: RemoteContext,
    private val readContext: RemoteContext = context,
    internal var textMeasurer: TextMeasurer? = null,
) : PaintContext(context) {
    internal var paintState: ComposeLocalPaint = ComposeLocalPaint()
    private val paintStack = ArrayDeque<ComposeLocalPaint>()
    private var fallbackTextMeasurer: TextMeasurer? = null

    @Suppress("DEPRECATION")
    private fun obtainFallbackTextMeasurer(): TextMeasurer =
        fallbackTextMeasurer
            ?: TextMeasurer(
                    defaultFontFamilyResolver = obtainFallbackFontFamilyResolver(),
                    defaultDensity = Density(1f),
                    defaultLayoutDirection = LayoutDirection.Ltr,
                )
                .also { fallbackTextMeasurer = it }

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
        updatePaintFromBundle(paintBundle, paintState, mContext)
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
        val substring = str.substring(safeStart, safeEnd)
        val density = Density(readContext.density.takeIf { it > 0f } ?: 1f)
        val measurer = textMeasurer ?: obtainFallbackTextMeasurer()
        val result =
            measurer.measure(
                text = substring,
                style = paintState.toTextStyle(density, readContext),
                softWrap = false,
                maxLines = 1,
                density = density,
            )
        val left = if (result.lineCount > 0) result.getLineLeft(0) else 0f
        val lineWidth =
            if (result.lineCount > 0) {
                result.getLineRight(0) - left
            } else {
                result.size.width.toFloat()
            }
        val width =
            if ((flags and TEXT_MEASURE_SPACES) != 0) {
                result.size.width.toFloat().coerceAtLeast(lineWidth)
            } else {
                lineWidth
            }
        val baseline = result.firstBaseline
        val top = (if (result.lineCount > 0) result.getLineTop(0) else 0f) - baseline
        val bottom =
            (if (result.lineCount > 0) result.getLineBottom(0) else result.size.height.toFloat()) -
                baseline
        bounds[0] = if ((flags and TEXT_MEASURE_SPACES) != 0) 0f else left
        bounds[1] = top
        bounds[2] = bounds[0] + width
        bounds[3] = bottom
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
        updatePaintFromBundle(mPaintData, paintState, mContext)
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

    override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

    override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
}

private var fallbackFontFamilyResolver: FontFamily.Resolver? = null

@Suppress("DEPRECATION")
private fun obtainFallbackFontFamilyResolver(): FontFamily.Resolver =
    fallbackFontFamilyResolver
        ?: run {
            val stubContext =
                object : ContextWrapper(null) {
                    override fun getApplicationContext(): Context = this
                }
            val resourceLoader =
                object : Font.ResourceLoader {
                    @Deprecated("Replaced by FontFamily.Resolver")
                    override fun load(font: Font): Any = Typeface.DEFAULT
                }
            createFontFamilyResolver(resourceLoader, stubContext)
        }
            .also { fallbackFontFamilyResolver = it }
