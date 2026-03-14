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

package androidx.compose.remote.creation.compose.layout

import android.graphics.Typeface
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_DAY_OF_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_WEEK_DAY
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.capture.RecordingCanvas
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.RemoteDrawScope0
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.state.AnimatedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemoteCanvasDrawScope0(
    public val remoteComposeCreationState: RemoteComposeCreationState,
    public val drawScope: DrawScope,
    override val density: Float = drawScope.density,
    override val fontScale: Float = drawScope.fontScale,
    override val drawContext: DrawContext = drawScope.drawContext,
    override val layoutDirection: LayoutDirection = drawScope.layoutDirection,
) : RemoteDrawScope0, RemoteStateScope {

    override val parentScope: RemoteComposeCreationState
        get() = remoteComposeCreationState

    override val remoteDensity: RemoteDensity
        get() = parentScope.remoteDensity

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class RemoteAccess(
        public val remoteDrawScope: RemoteCanvasDrawScope0,
        public val drawScope: DrawScope,
        public val remoteComposeCreationState: RemoteComposeCreationState,
    ) {
        public val component: RemoteComponent =
            RemoteComponent(drawScope, remoteComposeCreationState)
        public val time: RemoteTime = RemoteTime(drawScope, remoteComposeCreationState)

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public class RemoteComponent(
            public val drawScope: DrawScope,
            public val remoteComposeCreationState: RemoteComposeCreationState,
        ) {
            private val context = RemoteFloatContext(remoteComposeCreationState)

            public val width: RemoteFloat
                get() = context.componentWidth()

            public val height: RemoteFloat
                get() = context.componentHeight()

            public val centerX: RemoteFloat
                get() = context.componentCenterX()

            public val centerY: RemoteFloat
                get() = context.componentCenterY()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public class RemoteTime(
            public val drawScope: DrawScope,
            public val state: RemoteComposeCreationState,
        ) {
            public fun Hour(): RemoteFloat {
                return RemoteFloat(FLOAT_TIME_IN_HR)
            }

            public fun Minutes(): RemoteFloat {
                return RemoteFloat(FLOAT_TIME_IN_MIN)
            }

            public fun Seconds(): RemoteFloat {
                if (false) { // in Compose local
                    state.time.value
                }
                return RemoteFloat(FLOAT_TIME_IN_SEC)
            }

            public val time: Long
                get() = if (false) state.time.value else 0L

            public fun ContinuousSec(): RemoteFloat {
                if (false) {
                    state.time.value
                }
                return RemoteFloat(FLOAT_CONTINUOUS_SEC)
            }

            public fun UtcOffset(): RemoteFloat {
                return RemoteFloat(FLOAT_OFFSET_TO_UTC)
            }

            public fun DayOfWeek(): RemoteFloat {
                return RemoteFloat(FLOAT_WEEK_DAY)
            }

            public fun DayOfMonth(): RemoteFloat {
                return RemoteFloat(FLOAT_DAY_OF_MONTH)
            }
        }

        public fun value(v: Float): RemoteFloat {
            return RemoteFloat(v)
        }

        public fun animateFloat(
            rf: RemoteFloat,
            duration: Float = 1f,
            type: Int = 1,
            spec: FloatArray? = null,
            initialValue: Float = Float.NaN,
            wrap: Float = Float.NaN,
        ): RemoteFloat {
            remoteComposeCreationState.time.value
            val anim = RemoteComposeBuffer.packAnimation(duration, type, spec, initialValue, wrap)
            return AnimatedRemoteFloat(rf, anim)
        }

        public fun animateFloat(
            duration: Float = 1f,
            type: Int = 1,
            spec: FloatArray? = null,
            initialValue: Float = Float.NaN,
            wrap: Float = Float.NaN,
            content: () -> RemoteFloat,
        ): RemoteFloat {
            return animateFloat(content(), duration, type, spec, initialValue, wrap)
        }

        public fun loop(
            until: Int,
            from: Int = 0,
            step: Int = 1,
            content: RemoteCanvasDrawScope0.(RemoteFloat) -> Unit,
        ) {
            loop(until.toFloat(), from.toFloat(), step.toFloat(), content)
        }

        public fun loop(
            until: Float,
            from: Float = 0f,
            step: Float = 1f,
            content: RemoteCanvasDrawScope0.(RemoteFloat) -> Unit,
        ) {
            val loopIndex = remoteComposeCreationState.document.addFloatConstant(0f)
            remoteComposeCreationState.document.startLoop(
                Utils.idFromNan(loopIndex),
                from,
                step,
                until,
            )
            content.invoke(remoteDrawScope, RemoteFloat(loopIndex))
            remoteComposeCreationState.document.endLoop()
        }
    }

    public override val remote: RemoteAccess =
        RemoteAccess(this, drawScope, remoteComposeCreationState)

    override fun drawLine(
        brush: Brush,
        start: RemoteOffset,
        end: RemoteOffset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawLine(
            brush,
            start.asOffset(this),
            end.asOffset(this),
            strokeWidth,
            cap,
            pathEffect,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    override fun drawLine(
        color: Color,
        start: RemoteOffset,
        end: RemoteOffset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawLine(
            color,
            start.asOffset(this),
            end.asOffset(this),
            strokeWidth,
            cap,
            pathEffect,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    override fun drawRect(
        brush: Brush,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)
        remoteDrawRect(
            RemoteBrush.fromComposeUi(brush),
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawRect(
        brush: RemoteBrush,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: RemoteFloat,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        remoteDrawRect(
            brush,
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            alpha.floatId,
            style,
            colorFilter,
            blendMode,
        )
    }

    private fun ofAdd(pos: Float, size: Float): Float {
        return (RemoteFloat(pos) + RemoteFloat(size)).getFloatIdForCreationState(
            remoteComposeCreationState
        )
    }

    private fun ofAdd(pos: RemoteFloat, size: RemoteFloat): RemoteFloat {
        return pos + size
    }

    override fun drawRect(
        color: Color,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)
        remoteDrawRect(
            color,
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    public fun drawScaledImage(
        image: ImageBitmap,
        srcOffset: RemoteOffset = RemoteOffset.Zero,
        srcSize: RemoteSize = RemoteSize(image.width.rf, image.height.rf),
        dstOffset: RemoteOffset = RemoteOffset.Zero,
        dstSize: RemoteSize = srcSize,
        scaleType: Int,
        scaleFactor: RemoteFloat = 1f.rf,
        description: String? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality,
    ) {
        val srcR = ofAdd(srcOffset.x, srcSize.width)
        val srcB = ofAdd(srcOffset.y, srcSize.height)
        val dstR = ofAdd(dstOffset.x, dstSize.width)
        val dstB = ofAdd(dstOffset.y, dstSize.height)

        remoteDrawScaledBitmap(
            image,
            srcOffset.x.floatId,
            srcOffset.y.floatId,
            srcR.floatId,
            srcB.floatId,
            dstOffset.x.floatId,
            dstOffset.y.floatId,
            dstR.floatId,
            dstB.floatId,
            scaleType,
            scaleFactor.floatId,
            description,
        )
    }

    override fun drawImage(
        image: ImageBitmap,
        topLeft: RemoteOffset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawImage(image, topLeft.asOffset(this), alpha, style, colorFilter, blendMode)
    }

    @Deprecated(
        "Prefer usage of drawImage that consumes an optional FilterQuality parameter",
        replaceWith =
            ReplaceWith(
                "drawImage(image, srcOffset, srcSize, dstOffset, dstSize, alpha, style, colorFilter, blendMode, FilterQuality.Low)",
                "androidx.compose.ui.graphics.MyDrawScope",
                "androidx.compose.ui.graphics.FilterQuality",
            ),
        level = DeprecationLevel.HIDDEN,
    )
    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawImage(
            image,
            srcOffset,
            srcSize,
            dstOffset,
            dstSize,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawImage(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
        filterQuality: FilterQuality,
    ) {
        drawScope.drawImage(
            image,
            srcOffset,
            srcSize,
            dstOffset,
            dstSize,
            alpha,
            style,
            colorFilter,
            blendMode,
            filterQuality,
        )
    }

    public fun drawRoundRect(
        brush: RemoteBrush,
        topLeft: RemoteOffset,
        size: RemoteSize,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        remoteDrawRoundRect(
            brush,
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            cornerRadius,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    public fun drawRoundRect(
        brush: RemoteBrush,
        topLeft: RemoteOffset = RemoteOffset.Zero,
        size: RemoteSize = RemoteSize(remote.component.width, remote.component.height),
        cornerRadius: CornerRadius = CornerRadius(0f, 0f),
        alpha: RemoteFloat = 1f.rf,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) {
        if (drawContext.canvas.nativeCanvas is RecordingCanvas) {
            val right = ofAdd(topLeft.x, size.width)
            val bottom = ofAdd(topLeft.y, size.height)

            remoteDrawRoundRect(
                brush,
                topLeft.x.floatId,
                topLeft.y.floatId,
                right.floatId,
                bottom.floatId,
                cornerRadius,
                alpha.floatId,
                style,
                colorFilter,
                blendMode,
            )
        }
    }

    override fun drawRoundRect(
        brush: Brush,
        topLeft: RemoteOffset,
        size: RemoteSize,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        remoteDrawRoundRect(
            RemoteBrush.fromComposeUi(brush),
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            cornerRadius,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawText(
        textLayoutResult: TextLayoutResult,
        color: Color,
        topLeft: RemoteOffset,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    ) {
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            color = color,
            topLeft = topLeft.asOffset(this),
            alpha = alpha,
            shadow = shadow,
            textDecoration = textDecoration,
            drawStyle = drawStyle,
            blendMode = blendMode,
        )
    }

    override fun drawText(
        textLayoutResult: TextLayoutResult,
        brush: Brush,
        topLeft: RemoteOffset,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    ) {
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            brush = brush,
            topLeft = topLeft.asOffset(this),
            alpha = alpha,
            shadow = shadow,
            textDecoration = textDecoration,
            drawStyle = drawStyle,
            blendMode = blendMode,
        )
    }

    override fun drawText(
        textMeasurer: TextMeasurer,
        text: String,
        topLeft: RemoteOffset,
        style: TextStyle,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
        size: Size,
        blendMode: BlendMode,
    ) {
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = topLeft.asOffset(this),
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            size = size,
            blendMode = blendMode,
        )
    }

    override fun drawText(
        textMeasurer: TextMeasurer,
        text: AnnotatedString,
        topLeft: RemoteOffset,
        style: TextStyle,
        overflow: TextOverflow,
        softWrap: Boolean,
        maxLines: Int,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        size: Size,
        blendMode: BlendMode,
    ) {
        drawScope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = topLeft.asOffset(this),
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            placeholders = placeholders,
            size = size,
            blendMode = blendMode,
        )
    }

    override fun drawRoundRect(
        color: Color,
        topLeft: RemoteOffset,
        size: RemoteSize,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        remoteDrawRoundRect(
            color,
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            cornerRadius,
            style,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    override fun drawCircle(
        color: Color,
        radius: RemoteFloat,
        center: RemoteOffset,
        alpha: RemoteFloat,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        canvas.drawCircle(
            center.x.floatId,
            center.y.floatId,
            radius.floatId,
            toPaint(color, style, alpha.floatId, colorFilter, blendMode).nativePaint,
        )
    }

    override fun drawOval(
        brush: Brush,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        remoteDrawOval(
            RemoteBrush.fromComposeUi(brush),
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawOval(
        color: Color,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        remoteDrawOval(
            color,
            topLeft.x.floatId,
            topLeft.y.floatId,
            right.floatId,
            bottom.floatId,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawArc(
        brush: Brush,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawArc(
            brush,
            startAngle,
            sweepAngle,
            useCenter,
            topLeft.asOffset(this),
            size.asSize(this),
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawArc(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        topLeft: RemoteOffset,
        size: RemoteSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawArc(
            color,
            startAngle,
            sweepAngle,
            useCenter,
            topLeft.asOffset(this),
            size.asSize(this),
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawPath(
        path: Path,
        color: Color,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        remoteDrawPath(path, color, alpha, style, colorFilter, blendMode)
    }

    override fun drawPath(
        path: Path,
        brush: Brush,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawPath(path, brush, alpha, style, colorFilter, blendMode)
    }

    override fun drawTweenPath(
        path1: Path,
        path2: Path,
        tween: RemoteFloat,
        color: Color,
        start: RemoteFloat,
        stop: RemoteFloat,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        remoteDrawTweePath(
            path1,
            path2,
            tween,
            start,
            stop,
            color,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawAnchoredText(
        text: CharSequence,
        brush: RemoteBrush,
        anchor: RemoteOffset,
        panx: RemoteFloat,
        pany: RemoteFloat,
        alpha: RemoteFloat,
        drawStyle: DrawStyle,
        typeface: Typeface?,
        textSize: RemoteFloat,
    ) {
        remoteDrawAnchoredText(
            text,
            brush,
            anchor,
            panx,
            pany,
            alpha,
            drawStyle,
            typeface,
            textSize,
        )
    }

    override fun drawAnchoredText(
        text: RemoteString,
        brush: RemoteBrush,
        anchor: RemoteOffset,
        panx: RemoteFloat,
        pany: RemoteFloat,
        alpha: RemoteFloat,
        drawStyle: DrawStyle,
        typeface: Typeface?,
        textSize: RemoteFloat,
    ) {
        remoteDrawAnchoredText(
            text,
            brush,
            anchor,
            panx,
            pany,
            alpha,
            drawStyle,
            typeface,
            textSize,
        )
    }

    override fun drawAnchoredText(
        text: CharSequence,
        color: Color,
        anchor: RemoteOffset,
        panx: RemoteFloat,
        pany: RemoteFloat,
        alpha: RemoteFloat,
        drawStyle: DrawStyle,
        typeface: Typeface?,
        textSize: RemoteFloat,
    ) {
        remoteDrawAnchoredText(
            text,
            color,
            anchor,
            panx,
            pany,
            alpha,
            drawStyle,
            typeface,
            textSize,
        )
    }

    override fun drawAnchoredText(
        text: RemoteString,
        color: Color,
        anchor: RemoteOffset,
        panx: RemoteFloat,
        pany: RemoteFloat,
        alpha: RemoteFloat,
        drawStyle: DrawStyle,
        typeface: Typeface?,
        textSize: RemoteFloat,
    ) {
        remoteDrawAnchoredText(
            text,
            color,
            anchor,
            panx,
            pany,
            alpha,
            drawStyle,
            typeface,
            textSize,
        )
    }

    @Suppress("PrimitiveInCollection")
    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        color: Color,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawPoints(
            points,
            pointMode,
            color,
            strokeWidth,
            cap,
            pathEffect,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    @Suppress("PrimitiveInCollection")
    override fun drawPoints(
        points: List<Offset>,
        pointMode: PointMode,
        brush: Brush,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawPoints(
            points,
            pointMode,
            brush,
            strokeWidth,
            cap,
            pathEffect,
            alpha,
            colorFilter,
            blendMode,
        )
    }
}
