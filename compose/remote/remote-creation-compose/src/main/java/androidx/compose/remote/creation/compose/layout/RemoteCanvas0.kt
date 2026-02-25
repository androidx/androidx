/*
 * Copyright 2025 The Android Open Source Project
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
@file:Suppress("USELESS_IS_CHECK")

package androidx.compose.remote.creation.compose.layout

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Spacer
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteComposePath
import androidx.compose.remote.creation.compose.capture.RemoteDrawScope0.Companion.DefaultBlendMode
import androidx.compose.remote.creation.compose.capture.RemoteDrawScope0.Companion.DefaultFilterQuality
import androidx.compose.remote.creation.compose.capture.withTransform
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteSolidColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb

/**
 * RemoteCanvas implements a Canvas layout, delegating to the foundation Canvas layout as needed.
 * This allows RemoteCanvas to both work as a normal Column when called within a normal Compose
 * tree, and capture the layout information when called within a capture pass for RemoteCompose.
 */
@RemoteComposable
@Composable
public fun RemoteCanvas0(
    modifier: RemoteModifier = RemoteModifier,
    content: RemoteCanvasDrawScope0.() -> Unit,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    Spacer(
        modifier =
            RemoteComposeCanvasModifier(captureMode.toRecordingModifier(modifier))
                .drawBehind { RemoteCanvasDrawScope0(captureMode, drawScope = this).content() }
                .then(modifier.toComposeUiLayout())
    )
}

public fun RemoteCanvasDrawScope0.rotate(
    angle: RemoteFloat,
    pivotX: RemoteFloat,
    pivotY: RemoteFloat,
    function: RemoteCanvasDrawScope0.() -> Unit,
) {
    canvas.save()
    canvas.rotate(angle, pivotX, pivotY)
    this@rotate.function()
    canvas.restore()
}

public fun RemoteCanvasDrawScope0.clipRect(
    left: RemoteFloat,
    top: RemoteFloat,
    right: RemoteFloat,
    bottom: RemoteFloat,
    clipOp: ClipOp = ClipOp.Intersect,
    block: RemoteCanvasDrawScope0.() -> Unit,
) {
    withTransform({
        with(this@clipRect) {
            this@withTransform.clipRect(
                left.floatId,
                top.floatId,
                right.floatId,
                bottom.floatId,
                clipOp,
            )
        }
    }) {
        this@clipRect.block()
    }
}

public fun RemoteStateScope.translate(transform: DrawTransform, x: RemoteFloat, y: RemoteFloat) {
    transform.translate(x.floatId, y.floatId)
}

public fun RemoteCanvasDrawScope0.remoteDrawAnchoredText(
    text: CharSequence,
    brush: RemoteBrush,
    anchor: RemoteOffset,
    panx: RemoteFloat,
    pany: RemoteFloat,
    alpha: RemoteFloat,
    drawStyle: DrawStyle = Fill,
    typeface: Typeface? = null,
    textSize: RemoteFloat = 18f.rf,
) {
    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = DefaultBlendMode

    val size = RemoteSize(remote.component.width, remote.component.height)
    val paint = toPaint(brush, drawStyle, alpha, colorFilter, blendMode, size = size)

    val ap = paint.nativePaint

    if (typeface != null) {
        ap.setTypeface(typeface)
    } else {
        ap.setTypeface(Typeface.DEFAULT)
    }
    ap.textSize = textSize.floatId
    canvas.drawAnchoredText(
        text.toString(),
        anchorX = anchor.x,
        anchorY = anchor.y,
        panx = panx,
        pany = pany,
        flags = 0,
        paint = ap,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawAnchoredText(
    text: RemoteString,
    brush: RemoteBrush,
    anchor: RemoteOffset,
    panx: RemoteFloat,
    pany: RemoteFloat,
    alpha: RemoteFloat,
    drawStyle: DrawStyle = Fill,
    typeface: Typeface? = null,
    textSize: RemoteFloat = 18f.rf,
) {
    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = DefaultBlendMode

    val size = RemoteSize(remote.component.width, remote.component.height)
    val paint = toPaint(brush, drawStyle, alpha, colorFilter, blendMode, size = size)

    val ap = paint.nativePaint

    if (typeface != null) {
        ap.setTypeface(typeface)
    } else {
        ap.setTypeface(Typeface.DEFAULT)
    }
    ap.textSize = textSize.floatId
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

public fun RemoteCanvasDrawScope0.remoteDrawAnchoredText(
    text: CharSequence,
    color: Color,
    anchor: RemoteOffset,
    panx: RemoteFloat,
    pany: RemoteFloat,
    alpha: RemoteFloat,
    drawStyle: DrawStyle = Fill,
    typeface: Typeface? = null,
    textSize: RemoteFloat = 18f.rf,
) {
    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = DefaultBlendMode

    val paint = configurePaint(color, drawStyle, alpha.floatId, colorFilter, blendMode)
    val ap = paint.nativePaint

    if (typeface != null) {
        ap.setTypeface(typeface)
    } else {
        ap.setTypeface(Typeface.DEFAULT)
    }
    ap.textSize = textSize.floatId
    canvas.drawAnchoredText(
        text.toString(),
        anchorX = anchor.x,
        anchorY = anchor.y,
        panx = panx,
        pany = pany,
        flags = 0,
        paint = ap,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawAnchoredText(
    text: RemoteString,
    color: Color,
    anchor: RemoteOffset,
    panx: RemoteFloat,
    pany: RemoteFloat,
    alpha: RemoteFloat,
    drawStyle: DrawStyle = Fill,
    typeface: Typeface? = null,
    textSize: RemoteFloat = 18f.rf,
) {
    val colorFilter: ColorFilter? = null
    val blendMode: BlendMode = DefaultBlendMode

    val paint = configurePaint(color, drawStyle, alpha.floatId, colorFilter, blendMode)
    val ap = paint.nativePaint

    if (typeface != null) {
        ap.setTypeface(typeface)
    } else {
        ap.setTypeface(Typeface.DEFAULT)
    }
    ap.textSize = textSize.floatId
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

public fun RemoteCanvasDrawScope0.remoteDrawTweePath(
    path1: Path,
    path2: Path,
    tween: RemoteFloat,
    start: RemoteFloat,
    stop: RemoteFloat,
    color: Color,
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DefaultBlendMode,
) {
    canvas.drawTweenPath(
        path1,
        path2,
        tween,
        start,
        stop,
        configurePaint(color, style, alpha, colorFilter, blendMode),
    )
}

public fun tween(t: Float, a1: Float, a2: Float): Float {
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

public fun RemoteCanvasDrawScope0.remoteDrawRect(
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
    val paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    canvas.drawRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        paint = paint.nativePaint,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawPath(
    path: Path,
    color: Color,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
) {
    val paint = configurePaint(color, style, alpha, colorFilter, blendMode)
    if (path is RemoteComposePath) {
        canvas.drawRPath(path.remote, paint.nativePaint)
    } else {
        canvas.drawPath(path.asAndroidPath(), paint.nativePaint)
    }
}

public fun RemoteCanvasDrawScope0.remoteDrawRect(
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
    val size = RemoteSize(remote.component.width, remote.component.height)
    val paint = toPaint(brush, style, alpha.rf, colorFilter, blendMode, size = size)
    canvas.drawRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        paint = paint.nativePaint,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawRoundRect(
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
    canvas.drawRoundRect(
        left,
        top,
        right,
        bottom,
        cornerRadius.x,
        cornerRadius.y,
        toPaint(color, style, alpha, colorFilter, blendMode).nativePaint,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawRoundRect(
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
    val size = RemoteSize(remote.component.width, remote.component.height)
    val paint = toPaint(brush, style, alpha.rf, colorFilter, blendMode, size = size)
    canvas.drawRoundRect(
        left,
        top,
        right,
        bottom,
        cornerRadius.x,
        cornerRadius.y,
        paint.nativePaint,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawOval(
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
    canvas.drawOval(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        paint = toPaint(color, style, alpha, colorFilter, blendMode).nativePaint,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawScaledBitmap(
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
    val paint = Paint()
    paint.filterQuality = filterQuality
    paint.blendMode = blendMode
    paint.colorFilter = colorFilter
    paint.alpha = alpha
    canvas.usePaint(paint.nativePaint)
    canvas.drawScaledBitmap(
        image,
        srcLeft.rf,
        srcTop.rf,
        srcRight.rf,
        srcBottom.rf,
        dstLeft.rf,
        dstTop.rf,
        dstRight.rf,
        dstBottom.rf,
        scaleType,
        scaleFactor.rf,
        contentDescription,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawOval(
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
    val size = RemoteSize(remote.component.width, remote.component.height)
    val paint = toPaint(brush, style, alpha.rf, colorFilter, blendMode, size = size)
    canvas.drawOval(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        paint = paint.nativePaint,
    )
}

public fun RemoteCanvasDrawScope0.remoteDrawArc(
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
    canvas.drawArc(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = useCenter,
        paint = toPaint(color, style, alpha, colorFilter, blendMode).nativePaint,
    )
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

internal fun RemoteStateScope.toPaint(
    brush: RemoteBrush,
    drawStyle: DrawStyle,
    alpha: RemoteFloat,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    size: RemoteSize,
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
                with(brush) { this@toPaint.createShader(size = size) }
            } else {
                null
            }
        if (this.shader != shader) this.shader = shader
        this.alpha = alpha.floatId
        when (brush) {
            is RemoteSolidColor -> {
                val constantValue = brush.color.constantValueOrNull
                color =
                    if (constantValue != null) {
                        Color(constantValue.toArgb())
                    } else {
                        // If the remote color isn't a constant value then we don't have a way of
                        // accurately setting it via setColor, so set it to a known value.
                        Color.Transparent
                    }
            }
            else -> {
                // if brush is gradient
            }
        }

        if (this.colorFilter != colorFilter) this.colorFilter = colorFilter
        if (this.blendMode != blendMode) this.blendMode = blendMode
        if (this.filterQuality != filterQuality) this.filterQuality = filterQuality
    }
