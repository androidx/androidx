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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.draw

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.DrawBitmapFontText
import androidx.compose.remote.core.operations.DrawBitmapFontTextOnPath
import androidx.compose.remote.core.operations.DrawBitmapTextAnchored
import androidx.compose.remote.core.operations.DrawText
import androidx.compose.remote.core.operations.DrawTextAnchored
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.DrawTextOnPath
import androidx.compose.remote.player.compose.embedded.resources.resolveBitmap
import androidx.compose.remote.player.compose.utils.getPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Canvas text run (`DRAW_TEXT`). Mirrors `DrawText.paint` / the View player's `drawTextRun`:
 * resolve text by id, substring by `[start, end)`, measure via [textMeasurer], and draw aligned on
 * the baseline. (Layout text uses Compose `BasicText`; this is the canvas/[DrawScope] path.)
 */
internal fun DrawScope.drawTextOp(
    op: DrawText,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
    textMeasurer: TextMeasurer,
) {
    val full = read.getText(op.mTextID)
    if (full != null && !paintState.textSize.isNaN()) {
        val len = full.length
        val start = op.mStart.coerceIn(0, len)
        val end = if (op.mEnd == -1 || op.mEnd > len) len else op.mEnd.coerceIn(start, len)
        val text = full.substring(start, end)
        val x = resolveFloat(op.mX, op.mOutX, read)
        val y = resolveFloat(op.mY, op.mOutY, read)
        val textStyle = paintState.toTextStyle(this, read)
        val textLayoutResult = textMeasurer.measure(text = text, style = textStyle)

        // Assuming y is baseline
        val baseline = textLayoutResult.getLineBaseline(0)
        val topLeft = Offset(x, y - baseline)

        drawText(textLayoutResult = textLayoutResult, topLeft = topLeft)
    }
}

/**
 * Lay text along a path (`DRAW_TEXT_ON_PATH`). `updateVariables` has already resolved the offsets
 * into `mOut*`. The path id may be a `PTR_DEREFERENCE` indirection, mirroring
 * `PaintOperation.getId`.
 */
internal fun DrawScope.drawTextOnPathOp(
    op: DrawTextOnPath,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val full = read.getText(op.mTextId)
    if (full != null && !paintState.textSize.isNaN()) {
        val pathId = derefId(op.pathId, read)
        val path = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
        drawContext.canvas.nativeCanvas.drawTextOnPath(
            full,
            path.asAndroidPath(),
            op.outHOffset,
            op.outVOffset,
            paintState.toNativeTextPaint(read),
        )
    }
}

/**
 * Draw text positioned about an anchor point `(mX, mY)` with pan in `[-1, 1]`; `updateVariables`
 * resolved them into `mOut*`. Replicates `DrawTextAnchored.getHorizontalOffset`/`getVerticalOffset`
 * using Compose [TextMeasurer] and [drawText].
 */
internal inline fun DrawScope.drawTextAnchoredOp(
    op: DrawTextAnchored,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
    textMeasurer: TextMeasurer,
    evalPaintContext: () -> GraphPaintContext,
) {
    val textId = op.textID
    val full = read.getText(textId)
    if (full != null && !paintState.textSize.isNaN()) {
        val flags = op.flags
        val baselineRelative = (flags and DrawTextAnchored.BASELINE_RELATIVE) != 0
        val bounds = FloatArray(4)
        evalPaintContext().getTextBounds(textId, 0, -1, flags, bounds)
        val textStyle = paintState.toTextStyle(this, read)
        val textLayoutResult =
            textMeasurer.measure(
                text = full,
                style = textStyle,
                softWrap = false,
                maxLines = 1,
            )
        val outX = op.outX
        val outY = op.outY
        val outPanX = op.outPanX
        val outPanY = op.outPanY
        val textWidth = bounds[2] - bounds[0]
        val textHeight = bounds[3] - bounds[1]
        val hOffset = (0f - textWidth) * (1f + outPanX) / 2f - bounds[0]
        val x = outX + hOffset
        val y =
            if (outPanY.isNaN()) {
                outY
            } else {
                outY +
                    (0f - textHeight) * (1f - outPanY) / 2f +
                    (if (baselineRelative) textHeight / 2f else -bounds[1])
            }
        val baseline = textLayoutResult.firstBaseline
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(x, y - baseline),
        )
    }
}

/**
 * Curved text (`DRAW_TEXT_ON_CIRCLE`): lay the string along an arc of the given circle and draw via
 * the native canvas. Mirrors `DrawTextOnCircle.paint` / the View player's `drawTextOnCircle` —
 * build an arc [Path] sized to the measured text and `drawTextOnPath` onto it.
 */
internal fun DrawScope.drawTextOnCircleOp(
    op: DrawTextOnCircle,
    paintState: ComposeLocalPaint,
    read: RemoteContext,
    textMeasurer: TextMeasurer,
) {
    val full = read.getText(op.mTextId)
    if (full != null && !paintState.textSize.isNaN()) {
        val centerX = op.centerX
        val centerY = op.centerY
        val radius = op.radius
        val startAngle = op.startAngle
        val warpRadiusOffset = op.warpRadiusOffset
        val alignment = op.alignment
        val placement = op.placement
        val textWidth =
            textMeasurer
                .measure(
                    text = full,
                    style = paintState.toTextStyle(this, read),
                    softWrap = false,
                    maxLines = 1,
                )
                .getLineRight(0)
        val nativePaint = paintState.toNativeTextPaint(read)
        val finalRadius = radius + warpRadiusOffset
        val clockwise = placement == DrawTextOnCircle.Placement.OUTSIDE
        var sweepDegrees = Math.toDegrees((textWidth / finalRadius).toDouble()).toFloat()
        var finalStartAngle = startAngle
        if (!clockwise) {
            sweepDegrees = -sweepDegrees
            when (alignment) {
                DrawTextOnCircle.Alignment.CENTER ->
                    finalStartAngle = startAngle + abs(sweepDegrees) / 2f
                DrawTextOnCircle.Alignment.END -> finalStartAngle = startAngle + abs(sweepDegrees)
                else -> {}
            }
        } else {
            when (alignment) {
                DrawTextOnCircle.Alignment.CENTER ->
                    finalStartAngle = startAngle - sweepDegrees / 2f
                DrawTextOnCircle.Alignment.END -> finalStartAngle = startAngle - sweepDegrees
                else -> {}
            }
        }
        val textPath =
            Path().apply {
                addArc(
                    Rect(
                        centerX - finalRadius,
                        centerY - finalRadius,
                        centerX + finalRadius,
                        centerY + finalRadius,
                    ),
                    finalStartAngle,
                    sweepDegrees,
                )
            }
        drawContext.canvas.nativeCanvas.drawTextOnPath(
            full,
            textPath.asAndroidPath(),
            0f,
            0f,
            nativePaint,
        )
    }
}

/**
 * Bitmap-font text (`DRAW_BITMAP_FONT_TEXT`): walk the string glyph-by-glyph via
 * [BitmapFontData.lookupGlyph] and blit each glyph's bitmap into its laid-out dst rect (with
 * margins, kerning, and glyph spacing). Mirrors `DrawBitmapFontText.paint`.
 */
internal fun DrawScope.drawBitmapFontTextOp(
    op: DrawBitmapFontText,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val textId = op.textID
    val fontId = op.bitmapFontID
    val full = read.getText(textId)
    val font = remoteContext.getObject(fontId) as? BitmapFontData
    if (full != null && font != null) {
        val start = op.start
        val end = op.end
        val text =
            when {
                end == -1 || end > full.length -> full.substring(start.coerceIn(0, full.length))
                else ->
                    full.substring(
                        start.coerceIn(0, full.length),
                        end.coerceIn(start, full.length),
                    )
            }
        val kerning = font.getKerningTable()
        val y = op.outY
        val glyphSpacing = op.outGlyphSpacing
        var xPos = op.outX
        var pos = 0
        var prevGlyph = ""
        while (pos < text.length) {
            val glyph = font.lookupGlyph(text, pos)
            if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                pos++
                prevGlyph = ""
                continue
            }
            pos += glyph.mChars!!.length
            if (glyph.mBitmapId == -1) {
                // A glyph id of -1 represents a space: advance by its margins only.
                xPos += glyph.mMarginLeft + glyph.mMarginRight
                prevGlyph = glyph.mChars!!
                continue
            }
            xPos += glyph.mMarginLeft
            kerning?.get(prevGlyph + glyph.mChars)?.let { xPos += it }
            val glyphBitmap = resolveBitmap(remoteContext, glyph.mBitmapId)
            if (glyphBitmap != null && glyph.mBitmapWidth > 0 && glyph.mBitmapHeight > 0) {
                drawImage(
                    image = glyphBitmap.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(glyphBitmap.width, glyphBitmap.height),
                    dstOffset = IntOffset(xPos.toInt(), (y + glyph.mMarginTop).toInt()),
                    dstSize =
                        IntSize(
                            glyph.mBitmapWidth.toInt(),
                            glyph.mBitmapHeight.toInt(),
                        ),
                    blendMode = paintState.blendMode,
                )
            }
            xPos += glyph.mBitmapWidth + glyph.mMarginRight + glyphSpacing
            prevGlyph = glyph.mChars!!
        }
    }
}

/**
 * Bitmap-font text anchored about `(outX, outY)` with pan in `[-1, 1]`. Measures the run (`xMax` +
 * tallest glyph) to derive the pan offsets, then blits each glyph. Mirrors
 * `DrawBitmapTextAnchored.measure`/`getHorizontalOffset`/`getVerticalOffset`/`paint`.
 */
internal fun DrawScope.drawBitmapTextAnchoredOp(
    op: DrawBitmapTextAnchored,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val textId = op.textID
    val fontId = op.bitmapFontID
    val full = read.getText(textId)
    val font = remoteContext.getObject(fontId) as? BitmapFontData
    if (full != null && font != null) {
        val start = op.outStart.toInt()
        val end = op.outEnd.toInt()
        val text =
            full.substring(
                start.coerceAtLeast(0),
                if (end < 0 || end > full.length) full.length else end,
            )
        val kerning = font.getKerningTable()
        val glyphSpacing = op.outGlyphSpacing
        // measure(): xMax is the advance width; yMin/yMax bound the glyph heights.
        var xMeasure = 0f
        var yMin = 1000f
        var yMax = -Float.MAX_VALUE
        run {
            var pos = 0
            while (pos < text.length) {
                val glyph = font.lookupGlyph(text, pos)
                if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                    pos++
                    continue
                }
                pos += glyph.mChars!!.length
                xMeasure += glyph.mMarginLeft + glyph.mMarginRight
                if (glyph.mBitmapId != -1) xMeasure += glyph.mBitmapWidth
                yMax =
                    maxOf(
                        yMax,
                        (glyph.mBitmapHeight + glyph.mMarginTop + glyph.mMarginBottom).toFloat(),
                    )
                yMin = minOf(yMin, glyph.mMarginTop.toFloat())
                xMeasure += glyphSpacing
            }
        }
        val outPanX = op.outPanX
        val outPanY = op.outPanY
        val textWidth = xMeasure
        val textHeight = yMax - yMin
        val hOffset = (0f - textWidth) * (1f + outPanX) / 2f
        val vOffset = (0f - textHeight) * (1f - outPanY) / 2f - yMin
        var xPos = op.outX + hOffset
        val yPos = op.outY + vOffset
        var pos = 0
        var prevGlyph = ""
        while (pos < text.length) {
            val glyph = font.lookupGlyph(text, pos)
            if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                pos++
                prevGlyph = ""
                continue
            }
            pos += glyph.mChars!!.length
            if (glyph.mBitmapId == -1) {
                xPos += glyph.mMarginLeft + glyph.mMarginRight
                prevGlyph = glyph.mChars!!
                continue
            }
            xPos += glyph.mMarginLeft
            kerning?.get(prevGlyph + glyph.mChars)?.let { xPos += it }
            val glyphBitmap = resolveBitmap(remoteContext, glyph.mBitmapId)
            if (glyphBitmap != null && glyph.mBitmapWidth > 0 && glyph.mBitmapHeight > 0) {
                drawImage(
                    image = glyphBitmap.asImageBitmap(),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(glyphBitmap.width, glyphBitmap.height),
                    dstOffset = IntOffset(xPos.toInt(), (yPos + glyph.mMarginTop).toInt()),
                    dstSize =
                        IntSize(
                            glyph.mBitmapWidth.toInt(),
                            glyph.mBitmapHeight.toInt(),
                        ),
                    blendMode = paintState.blendMode,
                )
            }
            xPos += glyph.mBitmapWidth + glyph.mMarginRight + glyphSpacing
            prevGlyph = glyph.mChars!!
        }
    }
}

/**
 * Bitmap-font text laid along a path (`DRAW_BITMAP_FONT_TEXT_ON_PATH`): each glyph is positioned +
 * rotated by the path matrix at its mid-run fraction (position + tangent). Mirrors
 * `DrawBitmapFontTextOnPath.measureWidth`/`paint`. Path id may be a `PTR_DEREFERENCE`.
 */
internal fun DrawScope.drawBitmapFontTextOnPathOp(
    op: DrawBitmapFontTextOnPath,
    paintState: ComposeLocalPaint,
    remoteContext: RemoteContext,
    read: RemoteContext,
) {
    val textId = op.textID
    val fontId = op.bitmapFontID
    val full = read.getText(textId)
    val font = remoteContext.getObject(fontId) as? BitmapFontData
    if (full != null && font != null) {
        val start = op.start
        val end = op.end
        val text =
            when {
                end == -1 || end > full.length -> full.substring(start.coerceIn(0, full.length))
                else ->
                    full.substring(
                        start.coerceIn(0, full.length),
                        end.coerceIn(start, full.length),
                    )
            }
        val kerning = font.getKerningTable()
        val yAdj = op.outYAdj
        val glyphSpacing = op.outGlyphSpacing
        // measureWidth(): total advance (margins + kerning + glyph widths, no spacing).
        var width = 0f
        run {
            var pos = 0
            var prev = ""
            while (pos < text.length) {
                val glyph = font.lookupGlyph(text, pos)
                if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                    pos++
                    prev = ""
                    continue
                }
                pos += glyph.mChars!!.length
                if (glyph.mBitmapId == -1) {
                    width += glyph.mMarginLeft + glyph.mMarginRight
                    prev = ""
                    continue
                }
                width += glyph.mMarginLeft
                kerning?.get(prev + glyph.mChars)?.let { width += it }
                width += glyph.mBitmapWidth + glyph.mMarginRight
                prev = glyph.mChars!!
            }
        }
        val pathId = derefId(op.pathID, read)
        val composePath = remoteContext.mRemoteComposeState.getPath(pathId, 0f, 1f)
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(composePath, false)
        val pathLength = pathMeasure.length
        if (width > 0f && pathLength > 0f) {
            var progress = 0f
            var pos = 0
            var prevGlyph = ""
            while (pos < text.length) {
                val glyph = font.lookupGlyph(text, pos)
                if (glyph == null || glyph.mChars.isNullOrEmpty()) {
                    pos++
                    prevGlyph = ""
                    continue
                }
                pos += glyph.mChars!!.length
                if (glyph.mBitmapId == -1) {
                    progress += glyph.mMarginLeft + glyph.mMarginRight
                    prevGlyph = ""
                    continue
                }
                progress += glyph.mMarginLeft
                kerning?.get(prevGlyph + glyph.mChars)?.let { progress += it }
                val halfGlyphWidth = 0.5f * glyph.mBitmapWidth
                val fraction = (progress + halfGlyphWidth) / width
                val glyphBitmap = resolveBitmap(remoteContext, glyph.mBitmapId)
                if (glyphBitmap != null && glyph.mBitmapWidth > 0 && glyph.mBitmapHeight > 0) {
                    val position = pathMeasure.getPosition(fraction * pathLength)
                    val tangent = pathMeasure.getTangent(fraction * pathLength)
                    val angle =
                        Math.toDegrees(atan2(tangent.y.toDouble(), tangent.x.toDouble())).toFloat()

                    val composeMatrix =
                        Matrix().apply {
                            translate(position.x, position.y)
                            rotateZ(angle)
                        }

                    withTransform({
                        transform(composeMatrix)
                        translate(-halfGlyphWidth, yAdj + glyph.mMarginTop)
                        val scaleX = glyph.mBitmapWidth / glyphBitmap.width.toFloat()
                        val scaleY = glyph.mBitmapHeight / glyphBitmap.height.toFloat()
                        scale(scaleX, scaleY, pivot = Offset.Zero)
                    }) {
                        drawImage(
                            image = glyphBitmap.asImageBitmap(),
                            alpha = paintState.alpha,
                        )
                    }
                }
                progress += glyph.mBitmapWidth + glyph.mMarginRight + glyphSpacing
                prevGlyph = glyph.mChars!!
            }
        }
    }
}
