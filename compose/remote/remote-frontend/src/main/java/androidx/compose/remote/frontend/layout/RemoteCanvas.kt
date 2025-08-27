/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.capture.RemoteComposePath
import androidx.compose.remote.frontend.capture.RemoteDrawScope
import androidx.compose.remote.frontend.capture.RemoteDrawScope.Companion.DefaultBlendMode
import androidx.compose.remote.frontend.capture.RemoteDrawScope.Companion.DefaultFilterQuality
import androidx.compose.remote.frontend.capture.shaders.RemoteBrush
import androidx.compose.remote.frontend.capture.withTransform
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.toComposeUiLayout
import androidx.compose.remote.frontend.state.FallbackCreationState
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive

/** Utility modifier to record the layout information */
class RemoteComposeCanvasModifier(val modifier: RecordingModifier) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startCanvas(modifier)
                    drawContent()
                    it.document.endCanvas()
                }
            } else {
                drawContent()
            }
        }
    }
}

/**
 * RemoteCanvas implements a Canvas layout, delegating to the foundation Canvas layout as needed.
 * This allows RemoteCanvas to both work as a normal Column when called within a normal Compose
 * tree, and capture the layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
fun RemoteCanvas(
    modifier: RemoteModifier = RemoteModifier,
    content: RemoteCanvasDrawScope.() -> Unit,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    if (captureMode is NoRemoteCompose) {
        LaunchedEffect(Unit) {
            while (isActive) {
                withFrameMillis { captureMode.time.value = it }
            }
        }
        //    androidx.compose.foundation.Canvas(modifier = modifier.toComposeUi()) { content() }
        val observer = remember { SnapshotStateObserver { it() } }
        androidx.compose.foundation.layout.Spacer(
            modifier =
                modifier.toComposeUi().drawBehind {
                    RemoteCanvasDrawScope(captureMode, drawScope = this).content()
                }
        )
    } else {
        androidx.compose.foundation.layout.Spacer(
            modifier =
                RemoteComposeCanvasModifier(modifier.toRemoteCompose())
                    .drawBehind { RemoteCanvasDrawScope(captureMode, drawScope = this).content() }
                    .then(modifier.toComposeUiLayout())
        )
    }
}

fun RemoteCanvasDrawScope.rotate(
    angle: Number,
    pivotX: Number,
    pivotY: Number,
    function: RemoteCanvasDrawScope.() -> Unit,
) {
    val canvas = drawContext.canvas.nativeCanvas
    if (canvas is RecordingCanvas) {
        canvas.save()
        canvas.rotate(angle, pivotX, pivotY)
        this@rotate.function()
        canvas.restore()
    } else {
        val iAngle: Float =
            if (angle is RemoteFloat) angle.getFloatIdForCreationState(FallbackCreationState.state)
            else angle.toFloat()
        val iPivotX: Float =
            if (pivotX is RemoteFloat)
                pivotX.getFloatIdForCreationState(FallbackCreationState.state)
            else pivotX.toFloat()
        val iPivotY: Float =
            if (pivotY is RemoteFloat)
                pivotY.getFloatIdForCreationState(FallbackCreationState.state)
            else pivotY.toFloat()
        val iPivotXNeg: Float =
            if (pivotX is RemoteFloat)
                (pivotX * -1f).getFloatIdForCreationState(FallbackCreationState.state)
            else pivotX.toFloat() * -1f
        val iPivotYNeg: Float =
            if (pivotY is RemoteFloat)
                (pivotY * -1f).getFloatIdForCreationState(FallbackCreationState.state)
            else pivotY.toFloat() * -1f

        withTransform({
            translate(iPivotX, iPivotY)
            rotate(iAngle, pivot = Offset(0f, 0f))
            translate(iPivotXNeg, iPivotYNeg)
        }) {
            this@rotate.function()
        }
    }
}

fun RemoteCanvasDrawScope.clipRect(
    left: Number,
    top: Number,
    right: Number,
    bottom: Number,
    clipOp: ClipOp = ClipOp.Intersect,
    block: RemoteCanvasDrawScope.() -> Unit,
) {
    val iLeft: Float =
        if (left is RemoteFloat) left.getFloatIdForCreationState(FallbackCreationState.state)
        else left.toFloat()
    val iTop: Float =
        if (top is RemoteFloat) top.getFloatIdForCreationState(FallbackCreationState.state)
        else top.toFloat()
    val iRight: Float =
        if (right is RemoteFloat) right.getFloatIdForCreationState(FallbackCreationState.state)
        else right.toFloat()
    val iBottom: Float =
        if (bottom is RemoteFloat) bottom.getFloatIdForCreationState(FallbackCreationState.state)
        else bottom.toFloat()

    withTransform({ clipRect(iLeft, iTop, iRight, iBottom, clipOp) }) { this@clipRect.block() }
}

fun DrawTransform.translate(x: Number, y: Number) {
    val ix: Float =
        if (x is RemoteFloat) x.getFloatIdForCreationState(FallbackCreationState.state)
        else x.toFloat()
    val iy: Float =
        if (y is RemoteFloat) y.getFloatIdForCreationState(FallbackCreationState.state)
        else y.toFloat()
    this.translate(ix, iy)
}

fun RemoteCanvasDrawScope.remoteDrawAnchoredText(
    text: CharSequence,
    brush: RemoteBrush,
    anchor: Offset,
    panx: Number,
    pany: Number,
    alpha: Number,
    drawStyle: DrawStyle = Fill,
    typeface: android.graphics.Typeface? = null,
    textSize: Number = 18f,
) {
    val iAlpha: Float =
        if (alpha is RemoteFloat) alpha.getFloatIdForCreationState(FallbackCreationState.state)
        else alpha.toFloat()
    val iTextSize: Float =
        if (textSize is RemoteFloat)
            textSize.getFloatIdForCreationState(FallbackCreationState.state)
        else textSize.toFloat()

    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = RemoteDrawScope.DefaultBlendMode

    val canvas = drawContext.canvas.nativeCanvas

    if (canvas is RecordingCanvas) {
        val paint =
            toPaint(brush, drawStyle, iAlpha, colorFilter, blendMode, size = drawContext.size)

        val ap = paint.asFrameworkPaint()

        if (typeface != null) {
            ap.setTypeface(typeface)
        } else {
            ap.setTypeface(android.graphics.Typeface.DEFAULT)
        }
        ap.textSize = iTextSize
        canvas.drawAnchoredText(
            text.toString(),
            anchorX = anchor.x,
            anchorY = anchor.y,
            panx = panx,
            pany = pany,
            flags = 0,
            paint = ap,
        )
    } else {
        val iPanx: Float =
            if (panx is RemoteFloat) panx.getFloatIdForCreationState(FallbackCreationState.state)
            else panx.toFloat()
        val iPany: Float =
            if (pany is RemoteFloat) pany.getFloatIdForCreationState(FallbackCreationState.state)
            else pany.toFloat()

        val paint =
            configurePaint(
                size = drawContext.size,
                brush.toComposeUi(),
                drawStyle,
                iAlpha,
                colorFilter,
                blendMode,
            )
        val ap = paint.asFrameworkPaint()
        if (typeface != null) {
            ap.setTypeface(typeface)
        } else {
            ap.setTypeface(android.graphics.Typeface.DEFAULT)
        }
        ap.textSize = iTextSize
        val rec = Rect()
        ap.getTextBounds(text, 0, text.length, rec)
        val monospace = false
        val bounds = FloatArray(4)
        bounds[0] = rec.left.toFloat()
        bounds[1] = rec.top.toFloat()
        bounds[2] =
            if (monospace) (ap.measureText(text, 0, text.length) - rec.left)
            else rec.right.toFloat()
        bounds[3] = rec.bottom.toFloat()
        val x = anchor.x + getHorizontalOffset(iPanx, bounds)
        val y = anchor.y + getVerticalOffset(iPany, bounds)
        canvas.drawText(text.toString(), x, y, ap)
    }
}

fun RemoteCanvasDrawScope.remoteDrawAnchoredText(
    text: RemoteString,
    brush: RemoteBrush,
    anchor: Offset,
    panx: Number,
    pany: Number,
    alpha: Number,
    drawStyle: DrawStyle = Fill,
    typeface: android.graphics.Typeface? = null,
    textSize: Number = 18f,
) {
    val iAlpha: Float =
        if (alpha is RemoteFloat) alpha.getFloatIdForCreationState(FallbackCreationState.state)
        else alpha.toFloat()
    val iTextSize: Float =
        if (textSize is RemoteFloat)
            textSize.getFloatIdForCreationState(FallbackCreationState.state)
        else textSize.toFloat()

    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = RemoteDrawScope.DefaultBlendMode

    val canvas = drawContext.canvas.nativeCanvas

    if (canvas is RecordingCanvas) {
        val paint =
            toPaint(brush, drawStyle, iAlpha, colorFilter, blendMode, size = drawContext.size)

        val ap = paint.asFrameworkPaint()

        if (typeface != null) {
            ap.setTypeface(typeface)
        } else {
            ap.setTypeface(android.graphics.Typeface.DEFAULT)
        }
        ap.textSize = iTextSize
        canvas.drawAnchoredText(
            text,
            anchorX = anchor.x,
            anchorY = anchor.y,
            panx = panx,
            pany = pany,
            flags = 0,
            paint = ap,
        )
    }
}

fun RemoteCanvasDrawScope.remoteDrawAnchoredText(
    text: CharSequence,
    color: Color,
    anchor: Offset,
    panx: Number,
    pany: Number,
    alpha: Number,
    drawStyle: DrawStyle = Fill,
    typeface: android.graphics.Typeface? = null,
    textSize: Number = 18f,
) {
    val iAlpha: Float =
        if (alpha is RemoteFloat) alpha.getFloatIdForCreationState(FallbackCreationState.state)
        else alpha.toFloat()
    val iTextSize: Float =
        if (textSize is RemoteFloat)
            textSize.getFloatIdForCreationState(FallbackCreationState.state)
        else textSize.toFloat()

    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = RemoteDrawScope.DefaultBlendMode

    val canvas = drawContext.canvas.nativeCanvas

    if (canvas is RecordingCanvas) {
        val paint = configurePaint(color, drawStyle, iAlpha, colorFilter, blendMode)
        val ap = paint.asFrameworkPaint()

        if (typeface != null) {
            ap.setTypeface(typeface)
        } else {
            ap.setTypeface(android.graphics.Typeface.DEFAULT)
        }
        ap.textSize = iTextSize
        canvas.drawAnchoredText(
            text.toString(),
            anchorX = anchor.x,
            anchorY = anchor.y,
            panx = panx,
            pany = pany,
            flags = 0,
            paint = ap,
        )
    } else {
        val iPanx: Float =
            if (panx is RemoteFloat) panx.getFloatIdForCreationState(FallbackCreationState.state)
            else panx.toFloat()
        val iPany: Float =
            if (pany is RemoteFloat) pany.getFloatIdForCreationState(FallbackCreationState.state)
            else pany.toFloat()

        val paint = configurePaint(color, drawStyle, iAlpha, colorFilter, blendMode)
        val ap = paint.asFrameworkPaint()
        if (typeface != null) {
            ap.setTypeface(typeface)
        } else {
            ap.setTypeface(android.graphics.Typeface.DEFAULT)
        }
        ap.textSize = iTextSize
        val rec = Rect()
        ap.getTextBounds(text, 0, text.length, rec)
        val monospace = false
        val bounds = FloatArray(4)
        bounds[0] = rec.left.toFloat()
        bounds[1] = rec.top.toFloat()
        bounds[2] =
            if (monospace) (ap.measureText(text, 0, text.length) - rec.left)
            else rec.right.toFloat()
        bounds[3] = rec.bottom.toFloat()
        val x = anchor.x + getHorizontalOffset(iPanx, bounds)
        val y = anchor.y + getVerticalOffset(iPany, bounds)
        canvas.drawText(text.toString(), x, y, ap)
    }
}

fun RemoteCanvasDrawScope.remoteDrawAnchoredText(
    text: RemoteString,
    color: Color,
    anchor: Offset,
    panx: Number,
    pany: Number,
    alpha: Number,
    drawStyle: DrawStyle = Fill,
    typeface: android.graphics.Typeface? = null,
    textSize: Number = 18f,
) {
    val iAlpha: Float =
        if (alpha is RemoteFloat) alpha.getFloatIdForCreationState(FallbackCreationState.state)
        else alpha.toFloat()
    val iTextSize: Float =
        if (textSize is RemoteFloat)
            textSize.getFloatIdForCreationState(FallbackCreationState.state)
        else textSize.toFloat()

    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = RemoteDrawScope.DefaultBlendMode

    val canvas = drawContext.canvas.nativeCanvas

    if (canvas is RecordingCanvas) {
        val paint = configurePaint(color, drawStyle, iAlpha, colorFilter, blendMode)
        val ap = paint.asFrameworkPaint()

        if (typeface != null) {
            ap.setTypeface(typeface)
        } else {
            ap.setTypeface(android.graphics.Typeface.DEFAULT)
        }
        ap.textSize = iTextSize
        canvas.drawAnchoredText(
            text,
            anchorX = anchor.x,
            anchorY = anchor.y,
            panx = panx,
            pany = pany,
            flags = 0,
            paint = ap,
        )
    }
}

private fun getHorizontalOffset(mOutPanX: Float, mBounds: FloatArray): Float {
    val scale = 1.0f
    val textWidth: Float = scale * (mBounds.get(2) - mBounds.get(0))
    val boxWidth = 0f
    return ((boxWidth - textWidth) * (1 + mOutPanX) / 2f - (scale * mBounds.get(0)))
}

private fun getVerticalOffset(mOutPanY: Float, mBounds: FloatArray): Float {
    val scale = 1.0f
    val boxHeight = 0f
    val textHeight: Float = scale * (mBounds.get(3) - mBounds.get(1))
    return ((boxHeight - textHeight) * (1 - mOutPanY) / 2 - (scale * mBounds.get(1)))
}

fun RemoteCanvasDrawScope.remoteDrawTweePath(
    path1: Path,
    path2: Path,
    tween: Number,
    start: Number,
    stop: Number,
    color: Color,
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = RemoteDrawScope.DefaultBlendMode,
) {
    val canvas = drawContext.canvas.nativeCanvas
    if (canvas is RecordingCanvas) {
        canvas.drawTweenPath(
            path1,
            path2,
            tween,
            start,
            stop,
            configurePaint(color, style, alpha, colorFilter, blendMode),
        )
    } else {
        val iTween: Float =
            if (tween is RemoteFloat) tween.getFloatIdForCreationState(FallbackCreationState.state)
            else tween.toFloat()
        val path = Path()
        val it1 = path1.iterator(conicEvaluation = PathIterator.ConicEvaluation.AsQuadratics)
        val it2 = path2.iterator(conicEvaluation = PathIterator.ConicEvaluation.AsQuadratics)
        while (it1.hasNext() && it2.hasNext()) {
            val ps1 = it1.next()
            val ps2 = it2.next()
            when (ps1.type) {
                PathSegment.Type.Move ->
                    path.moveTo(
                        tween(iTween, ps1.points[0], ps2.points[0]),
                        tween(iTween, ps1.points[1], ps2.points[1]),
                    )
                PathSegment.Type.Line -> {
                    val lines = ps1.points.size / 2 - 1
                    for (i in 0..lines) {
                        val k = i * 2
                        path.lineTo(
                            tween(iTween, ps1.points[k], ps2.points[k]),
                            tween(iTween, ps1.points[k + 1], ps2.points[k + 1]),
                        )
                    }
                }
                PathSegment.Type.Quadratic -> {
                    val lines = ps1.points.size / 4 - 1
                    for (i in 0..lines) {
                        val k = i * 4
                        path.quadraticTo(
                            tween(iTween, ps1.points[k], ps2.points[k]),
                            tween(iTween, ps1.points[k + 1], ps2.points[k + 1]),
                            tween(iTween, ps1.points[k + 2], ps2.points[k + 2]),
                            tween(iTween, ps1.points[k + 3], ps2.points[k + 3]),
                        )
                    }
                }
                PathSegment.Type.Conic -> println("should not have conic")
                PathSegment.Type.Cubic -> {
                    val lines = ps1.points.size / 6 - 1
                    for (i in 0..lines) {
                        val k = i * 6
                        path.cubicTo(
                            tween(iTween, ps1.points[k], ps2.points[k]),
                            tween(iTween, ps1.points[k + 1], ps2.points[k + 1]),
                            tween(iTween, ps1.points[k + 2], ps2.points[k + 2]),
                            tween(iTween, ps1.points[k + 3], ps2.points[k + 3]),
                            tween(iTween, ps1.points[k + 4], ps2.points[k + 4]),
                            tween(iTween, ps1.points[k + 5], ps2.points[k + 5]),
                        )
                    }
                }
                PathSegment.Type.Close -> {
                    path.close()
                }
                PathSegment.Type.Done -> println("done")
            }
        }

        drawScope.drawPath(path, color, alpha, style, colorFilter, blendMode)
    }
}

fun tween(t: Float, a1: Float, a2: Float): Float {
    return a1 + t * (a2 - a1)
}

private fun configurePaint(
    size: Size,
    brush: Brush?,
    style: DrawStyle,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
): Paint {
    val paint = Paint()
    if (brush != null) {
        brush.applyTo(size, paint, alpha)
    } else {
        if (paint.shader != null) paint.shader = null
        if (paint.color != Color.Black) paint.color = Color.Black
        if (paint.alpha != alpha) paint.alpha = alpha
    }
    if (paint.colorFilter != colorFilter) paint.colorFilter = colorFilter
    if (paint.blendMode != blendMode) paint.blendMode = blendMode
    if (paint.filterQuality != filterQuality) paint.filterQuality = filterQuality
    return paint
}

private fun configurePaint(
    color: Color,
    style: DrawStyle,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
    filterQuality: FilterQuality = FilterQuality.Low,
): Paint {
    val paint = Paint()

    val targetColor =
        if (alpha == 1.0f) color
        else
            Color(
                red = color.red,
                green = color.green,
                blue = color.blue,
                alpha = alpha,
                colorSpace = color.colorSpace,
            )
    if (paint.color != targetColor) paint.color = targetColor
    if (paint.shader != null) paint.shader = null
    if (paint.colorFilter != colorFilter) paint.colorFilter = colorFilter
    if (paint.blendMode != blendMode) paint.blendMode = blendMode
    if (paint.filterQuality != filterQuality) paint.filterQuality = filterQuality

    if (style is Stroke) {
        paint.strokeWidth = (style).width
        paint.strokeCap = (style).cap
        paint.strokeJoin = (style).join
        paint.strokeMiterLimit = (style).miter
        paint.style = PaintingStyle.Stroke
    } else {
        paint.style = PaintingStyle.Fill
    }

    return paint
}

fun DrawScope.remoteDrawRect(
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    drawIntoCanvas {
        val nc = it.nativeCanvas
        val paint = configurePaint(color, style, alpha, colorFilter, blendMode)
        if (nc is RecordingCanvas) {
            val rc = nc as RecordingCanvas
            rc.drawRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                paint = paint.asFrameworkPaint(),
            )
        } else {
            it.drawRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                paint = toPaint(color, style, alpha, colorFilter, blendMode),
            )
        }
    }
}

fun DrawScope.remoteDrawPath(
    path: Path,
    color: Color,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {
    drawIntoCanvas {
        val nc = it.nativeCanvas
        val paint = configurePaint(color, style, alpha, colorFilter, blendMode)
        if (nc is RecordingCanvas) {
            if (path is RemoteComposePath) {
                nc.drawRPath(path.remote, paint.asFrameworkPaint())
            } else {
                nc.drawPath(path.asAndroidPath(), paint.asFrameworkPaint())
            }
        } else {
            if (path is RemoteComposePath) {
                // TODO
            } else {
                it.drawPath(path, paint)
            }
        }
    }
}

fun DrawScope.remoteDrawRect(
    brush: RemoteBrush,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    drawIntoCanvas {
        val paint = toPaint(brush, style, alpha, colorFilter, blendMode, size = drawContext.size)
        val nc = it.nativeCanvas
        if (nc is RecordingCanvas) {
            val rc = nc as RecordingCanvas
            rc.drawRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                paint = paint.asFrameworkPaint(),
            )
        } else {
            it.drawRect(left = left, top = top, right = right, bottom = bottom, paint = paint)
        }
    }
}

fun DrawScope.remoteDrawRoundRect(
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: CornerRadius,
    style: DrawStyle,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {
    drawIntoCanvas {
        it.drawRoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radiusX = cornerRadius.x,
            radiusY = cornerRadius.y,
            paint = toPaint(color, style, alpha, colorFilter, blendMode),
        )
    }
}

fun DrawScope.remoteDrawRoundRect(
    brush: RemoteBrush,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: CornerRadius,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {

    drawIntoCanvas {
        val paint = toPaint(brush, style, alpha, colorFilter, blendMode, size = drawContext.size)
        it.drawRoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radiusX = cornerRadius.x,
            radiusY = cornerRadius.y,
            paint = paint,
        )
    }
}

fun DrawScope.remoteDrawOval(
    color: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {
    drawIntoCanvas {
        it.drawOval(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            paint = toPaint(color, style, alpha, colorFilter, blendMode),
        )
    }
}

fun DrawScope.remoteDrawScaledBitmap(
    image: Bitmap,
    srcLeft: Float,
    srcTop: Float,
    srcRight: Float,
    srcBottom: Float,
    dstLeft: Float,
    dstTop: Float,
    dstRight: Float,
    dstBottom: Float,
    scaleType: Int,
    scaleFactor: Float,
    contentDescription: String?,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DefaultBlendMode,
    filterQuality: FilterQuality = DefaultFilterQuality,
) {
    drawIntoCanvas {
        val nc = it.nativeCanvas
        if (nc is RecordingCanvas) {
            val paint = Paint()
            paint.filterQuality = filterQuality
            paint.blendMode = blendMode
            paint.colorFilter = colorFilter
            paint.alpha = alpha
            nc.usePaint(paint.asFrameworkPaint())
            nc.drawScaledBitmap(
                image,
                srcLeft,
                srcTop,
                srcRight,
                srcBottom,
                dstLeft,
                dstTop,
                dstRight,
                dstBottom,
                scaleType,
                scaleFactor,
                contentDescription,
            )
        } else {
            val paint = Paint()
            paint.filterQuality = filterQuality
            paint.blendMode = blendMode
            paint.colorFilter = colorFilter
            paint.alpha = alpha
            val scaling =
                ImageScaling(
                    srcLeft,
                    srcTop,
                    srcRight,
                    srcBottom,
                    dstLeft,
                    dstTop,
                    dstRight,
                    dstBottom,
                    scaleType,
                    scaleFactor,
                )

            it.drawImageRect(
                image.asImageBitmap(),
                IntOffset(srcLeft.toInt(), srcTop.toInt()),
                IntSize((srcRight - srcLeft).toInt(), (srcBottom - srcTop).toInt()),
                IntOffset(scaling.mFinalDstLeft.toInt(), scaling.mFinalDstTop.toInt()),
                IntSize(
                    (scaling.mFinalDstRight - scaling.mFinalDstLeft).toInt(),
                    (scaling.mFinalDstBottom - scaling.mFinalDstTop).toInt(),
                ),
                paint = paint,
            )
        }
    }
}

fun DrawScope.remoteDrawOval(
    brush: RemoteBrush,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {
    drawIntoCanvas {
        val paint = toPaint(brush, style, alpha, colorFilter, blendMode, size = drawContext.size)
        it.drawOval(left = left, top = top, right = right, bottom = bottom, paint = paint)
    }
}

fun DrawScope.remoteDrawArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {
    drawIntoCanvas {
        it.drawArc(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = useCenter,
            paint = toPaint(color, style, alpha, colorFilter, blendMode),
        )
    }
}

/** Converts parameters to androidx.compose.ui.graphics.Paint */
internal fun toPaint(
    color: Color,
    drawStyle: DrawStyle,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
): Paint =
    Paint().apply {
        when (drawStyle) {
            is Fill -> {
                this.style = PaintingStyle.Fill
            }
            is Stroke -> {
                this.style = PaintingStyle.Stroke
                this.strokeCap = drawStyle.cap
                this.strokeWidth = drawStyle.width
                this.strokeMiterLimit = drawStyle.miter
                this.strokeJoin = drawStyle.join
                this.pathEffect = drawStyle.pathEffect
            }
        }
        this.alpha = alpha
        this.color = color
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
    }

internal fun toPaint(
    brush: RemoteBrush,
    drawStyle: DrawStyle,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    size: Size,
): Paint =
    Paint().apply {
        when (drawStyle) {
            is Fill -> {
                this.style = PaintingStyle.Fill
            }
            is Stroke -> {
                this.style = PaintingStyle.Stroke
                this.strokeCap = drawStyle.cap
                this.strokeWidth = drawStyle.width
                this.strokeMiterLimit = drawStyle.miter
                this.strokeJoin = drawStyle.join
                this.pathEffect = drawStyle.pathEffect
            }
        }
        val shader =
            if (brush.hasShader) {
                brush.createShader(size = size)
            } else {
                null
            }
        if (this.shader != shader) this.shader = shader
        this.alpha = alpha
        this.color = Color.Red
        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
    }

typealias ROffset = Offset

typealias RSize = Size

fun ROffset(x: Number, y: Number): ROffset {
    val ix: Float =
        if (x is RemoteFloat) x.getFloatIdForCreationState(FallbackCreationState.state)
        else x.toFloat()
    val iy: Float =
        if (y is RemoteFloat) y.getFloatIdForCreationState(FallbackCreationState.state)
        else y.toFloat()

    return Offset(ix, iy)
}

fun RSize(w: Number, h: Number): RSize {
    val iw: Float =
        if (w is RemoteFloat) w.getFloatIdForCreationState(FallbackCreationState.state)
        else w.toFloat()
    val ih: Float =
        if (h is RemoteFloat) h.getFloatIdForCreationState(FallbackCreationState.state)
        else h.toFloat()

    return Size(iw, ih)
}
