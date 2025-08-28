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

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_DAY_OF_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_WEEK_DAY
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.frontend.capture.RemoteDrawScope
import androidx.compose.remote.frontend.capture.shaders.RemoteBrush
import androidx.compose.remote.frontend.state.AnimatedRemoteFloat
import androidx.compose.remote.frontend.state.MutableRemoteFloat
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteString
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.DefaultTintBlendMode
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

typealias Remotable = Any

typealias RemotableFloat = Number

open class RemoteCanvasDrawScope(
    val remoteComposeCreationState: RemoteComposeCreationState,
    val drawScope: DrawScope,
    override val density: Float = drawScope.density,
    override val fontScale: Float = drawScope.fontScale,
    override val drawContext: DrawContext = drawScope.drawContext,
    override val layoutDirection: LayoutDirection = drawScope.layoutDirection,
) : RemoteDrawScope {

    fun drawCircle(
        color: Color,
        radius: RemoteFloat = remote.component.height / 2f,
        center: RemoteOffset =
            RemoteOffset(remote.component.width / 2f, remote.component.height / 2f),
        alpha: Number = 1f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultTintBlendMode,
    ) {
        drawScope.drawCircle(
            color,
            radius.internalAsFloat(),
            center.asOffset(),
            alpha.toFloat(),
            style,
            colorFilter,
        )
    }

    fun drawCircle(
        color: Color,
        radius: RemotableFloat = remote.component.height / 2f,
        center: RemoteOffset =
            RemoteOffset(remote.component.width / 2f, remote.component.height / 2f),
        alpha: RemotableFloat = RemoteFloat(1f),
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultTintBlendMode,
    ) {
        val r =
            when (radius) {
                is Float -> {
                    radius
                }
                is RemoteFloat -> {
                    radius.internalAsFloat()
                }
                else -> {
                    radius.toFloat()
                }
            }
        val a =
            when (alpha) {
                is Float -> {
                    alpha
                }
                is RemoteFloat -> {
                    alpha.internalAsFloat()
                }
                else -> {
                    alpha.toFloat()
                }
            }
        drawScope.drawCircle(color, r, center.asOffset(), a, style, colorFilter)
    }

    class RemoteAccess(
        val remoteDrawScope: RemoteCanvasDrawScope,
        val drawScope: DrawScope,
        val remoteComposeCreationState: RemoteComposeCreationState,
    ) {
        val component = RemoteComponent(drawScope, remoteComposeCreationState)
        val time = RemoteTime(drawScope, remoteComposeCreationState)

        class RemoteComponent(
            val drawScope: DrawScope,
            val remoteComposeCreationState: RemoteComposeCreationState,
        ) {
            private fun pickValue(default: Float, value: () -> RemoteFloat): RemoteFloat {
                if (
                    drawScope.drawContext.canvas.nativeCanvas is RecordingCanvas &&
                        remoteComposeCreationState !is NoRemoteCompose
                ) {
                    return value()
                }
                return RemoteFloat(default)
            }

            val width: RemoteFloat
                get() =
                    pickValue(drawScope.size.width) {
                        remoteComponentWidth(remoteComposeCreationState)
                    }

            val height: RemoteFloat
                get() =
                    pickValue(drawScope.size.height) {
                        remoteComponentHeight(remoteComposeCreationState)
                    }

            val centerX: RemoteFloat
                get() =
                    pickValue(drawScope.center.x) {
                        remoteComponentCenterX(remoteComposeCreationState)
                    }

            val centerY: RemoteFloat
                get() =
                    pickValue(drawScope.center.y) {
                        remoteComponentCenterY(remoteComposeCreationState)
                    }
        }

        class RemoteTime(val drawScope: DrawScope, val state: RemoteComposeCreationState) {
            fun Hour(): RemoteFloat {
                return RemoteFloat(FLOAT_TIME_IN_HR)
            }

            fun Minutes(): RemoteFloat {
                return RemoteFloat(FLOAT_TIME_IN_MIN)
            }

            fun Seconds(): RemoteFloat {
                if (state is NoRemoteCompose) { // in Compose local
                    state.time.value
                }
                return RemoteFloat(FLOAT_TIME_IN_SEC)
            }

            val time: Long
                get() = if (state is NoRemoteCompose) state.time.value else 0L

            fun ContinuousSec(): RemoteFloat {
                if (state is NoRemoteCompose) {
                    state.time.value
                }
                return RemoteFloat(FLOAT_CONTINUOUS_SEC)
            }

            fun UtcOffset(): RemoteFloat {
                return RemoteFloat(FLOAT_OFFSET_TO_UTC)
            }

            fun DayOfWeek(): RemoteFloat {
                return RemoteFloat(FLOAT_WEEK_DAY)
            }

            fun DayOfMonth(): RemoteFloat {
                return RemoteFloat(FLOAT_DAY_OF_MONTH)
            }
        }

        fun value(v: Float): RemoteFloat {
            return RemoteFloat(v)
        }

        fun animateFloat(
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

        fun animateFloat(
            duration: Float = 1f,
            type: Int = 1,
            spec: FloatArray? = null,
            initialValue: Float = Float.NaN,
            wrap: Float = Float.NaN,
            content: () -> RemoteFloat,
        ): RemoteFloat {
            return animateFloat(content(), duration, type, spec, initialValue, wrap)
        }

        fun loop(
            until: Int,
            from: Int = 0,
            step: Int = 1,
            content: RemoteCanvasDrawScope.(RemoteFloat) -> Unit,
        ) {
            loop(until.toFloat(), from.toFloat(), step.toFloat(), content)
        }

        fun loop(
            until: Float,
            from: Float = 0f,
            step: Float = 1f,
            content: RemoteCanvasDrawScope.(RemoteFloat) -> Unit,
        ) {
            if (drawScope.drawContext.canvas.nativeCanvas is RecordingCanvas) {
                val loopIndex = remoteComposeCreationState.document.addFloatConstant(0f)
                remoteComposeCreationState.document.startLoop(
                    Utils.idFromNan(loopIndex),
                    from,
                    step,
                    until,
                )
                content.invoke(remoteDrawScope, RemoteFloat(loopIndex))
                remoteComposeCreationState.document.endLoop()
            } else {
                var loopIndex = from
                while (loopIndex < until) {
                    content.invoke(remoteDrawScope, RemoteFloat(loopIndex))
                    loopIndex += step
                }
            }
        }
    }

    val remote = RemoteAccess(this, drawScope, remoteComposeCreationState)

    override fun drawLine(
        brush: Brush,
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawLine(
            brush,
            start,
            end,
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
        start: Offset,
        end: Offset,
        strokeWidth: Float,
        cap: StrokeCap,
        pathEffect: PathEffect?,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawLine(
            color,
            start,
            end,
            strokeWidth,
            cap,
            pathEffect,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    fun drawLine(
        color: Color,
        start: RemoteOffset,
        end: RemoteOffset,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: PathEffect? = null,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultTintBlendMode,
    ) {
        drawScope.drawLine(
            color,
            start.asOffset(),
            end.asOffset(),
            strokeWidth,
            cap,
            pathEffect,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    fun drawLine(
        color: Color,
        start: Remotable,
        end: Remotable,
        strokeWidth: Float = Stroke.HairlineWidth,
        cap: StrokeCap = Stroke.DefaultCap,
        pathEffect: PathEffect? = null,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultTintBlendMode,
    ) {
        val s =
            if (start is Offset) {
                start
            } else if (start is RemoteOffset) {
                start.asOffset()
            } else {
                null
            }
        val e =
            if (end is Offset) {
                end
            } else if (end is RemoteOffset) {
                end.asOffset()
            } else {
                null
            }
        drawScope.drawLine(
            color,
            s!!,
            e!!,
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
        topLeft: ROffset,
        size: RSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)
        drawScope.remoteDrawRect(
            RemoteBrush.fromComposeUi(brush),
            topLeft.x,
            topLeft.y,
            right,
            bottom,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    fun drawRect(
        brush: RemoteBrush,
        topLeft: ROffset = Offset.Zero,
        size: RSize = RSize(remote.component.width, remote.component.height),
        alpha: Float = 1f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)
        drawScope.remoteDrawRect(
            brush,
            topLeft.x,
            topLeft.y,
            right,
            bottom,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    fun drawRect(
        brush: RemoteBrush,
        topLeft: ROffset = Offset.Zero,
        size: RSize = RSize(remote.component.width, remote.component.height),
        alpha: RemotableFloat,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        val alphaValue =
            when (alpha) {
                is Float -> {
                    alpha
                }
                is RemoteFloat -> {
                    alpha.internalAsFloat()
                }
                else -> {
                    alpha.toFloat()
                }
            }

        drawScope.remoteDrawRect(
            brush,
            topLeft.x,
            topLeft.y,
            right,
            bottom,
            alphaValue,
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

    override fun drawRect(
        color: Color,
        topLeft: ROffset,
        size: RSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)
        drawScope.remoteDrawRect(
            color,
            topLeft.x,
            topLeft.y,
            right,
            bottom,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    fun drawScaledImage(
        image: ImageBitmap,
        srcOffset: ROffset = Offset.Zero,
        srcSize: RSize = RSize(image.width.toFloat(), image.height.toFloat()),
        dstOffset: ROffset = Offset.Zero,
        dstSize: RSize = srcSize,
        scaleType: Int,
        scaleFactor: Number = 1f,
        description: String? = null,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
        filterQuality: FilterQuality = DefaultFilterQuality,
    ) {
        if (drawContext.canvas.nativeCanvas is RecordingCanvas) {
            val srcR = ofAdd(srcOffset.x, srcSize.width)
            val srcB = ofAdd(srcOffset.y, srcSize.height)
            val dstR = ofAdd(dstOffset.x, dstSize.width)
            val dstB = ofAdd(dstOffset.y, dstSize.height)

            val iScaleFactor =
                if (scaleFactor is RemoteFloat) scaleFactor.internalAsFloat()
                else scaleFactor.toFloat()
            drawScope.remoteDrawScaledBitmap(
                image.asAndroidBitmap(),
                srcOffset.x,
                srcOffset.y,
                srcR,
                srcB,
                dstOffset.x,
                dstOffset.y,
                dstR,
                dstB,
                scaleType,
                iScaleFactor,
                description,
            )
        } else {
            val srcR = ofAdd(srcOffset.x, srcSize.width)
            val srcB = ofAdd(srcOffset.y, srcSize.height)
            val dstR = ofAdd(dstOffset.x, dstSize.width)
            val dstB = ofAdd(dstOffset.y, dstSize.height)
            val iScaleFactor =
                if (scaleFactor is RemoteFloat) scaleFactor.internalAsFloat()
                else scaleFactor.toFloat()
            val scale =
                ImageScaling(
                    srcOffset.x,
                    srcOffset.y,
                    srcR,
                    srcB,
                    dstOffset.x,
                    dstOffset.y,
                    dstR,
                    dstB,
                    scaleType,
                    iScaleFactor,
                )
            val size =
                IntSize(
                    (scale.mFinalDstRight - scale.mFinalDstLeft).toInt(),
                    (scale.mFinalDstBottom - scale.mFinalDstTop).toInt(),
                )

            clipRect(dstOffset.x, dstOffset.y, dstR, dstB) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset(srcOffset.x.toInt(), srcOffset.y.toInt()),
                    srcSize = IntSize(srcSize.width.toInt(), srcSize.height.toInt()),
                    dstOffset = IntOffset(scale.mFinalDstLeft.toInt(), scale.mFinalDstTop.toInt()),
                    dstSize = size,
                    colorFilter = colorFilter,
                    blendMode = blendMode,
                    filterQuality = filterQuality,
                )
            }
        }
    }

    override fun drawImage(
        image: ImageBitmap,
        topLeft: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawImage(image, topLeft, alpha, style, colorFilter, blendMode)
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

    fun drawRoundRect(
        brush: RemoteBrush,
        topLeft: ROffset,
        size: RSize,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (drawContext.canvas.nativeCanvas is RecordingCanvas) {
            val right = ofAdd(topLeft.x, size.width)
            val bottom = ofAdd(topLeft.y, size.height)

            drawScope.remoteDrawRoundRect(
                brush,
                topLeft.x,
                topLeft.y,
                right,
                bottom,
                cornerRadius,
                alpha,
                style,
                colorFilter,
                blendMode,
            )
        } else {
            drawScope.drawRoundRect(
                brush.toComposeUi(),
                topLeft,
                size,
                cornerRadius,
                alpha,
                style,
                colorFilter,
                blendMode,
            )
        }
    }

    fun drawRoundRect(
        brush: RemoteBrush,
        topLeft: ROffset = Offset.Zero,
        size: RSize = RSize(remote.component.width, remote.component.height),
        cornerRadius: CornerRadius = CornerRadius(0f, 0f),
        alpha: RemotableFloat = 1f,
        style: DrawStyle = Fill,
        colorFilter: ColorFilter? = null,
        blendMode: BlendMode = DefaultBlendMode,
    ) {
        val alphaValue =
            when (alpha) {
                is Float -> {
                    alpha
                }
                is RemoteFloat -> {
                    alpha.internalAsFloat()
                }
                else -> {
                    alpha.toFloat()
                }
            }

        if (drawContext.canvas.nativeCanvas is RecordingCanvas) {
            val right = ofAdd(topLeft.x, size.width)
            val bottom = ofAdd(topLeft.y, size.height)

            drawScope.remoteDrawRoundRect(
                brush,
                topLeft.x,
                topLeft.y,
                right,
                bottom,
                cornerRadius,
                alphaValue,
                style,
                colorFilter,
                blendMode,
            )
        } else {
            drawScope.drawRoundRect(
                brush.toComposeUi(),
                topLeft,
                size,
                cornerRadius,
                alphaValue,
                style,
                colorFilter,
                blendMode,
            )
        }
    }

    override fun drawRoundRect(
        brush: Brush,
        topLeft: ROffset,
        size: RSize,
        cornerRadius: CornerRadius,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (drawContext.canvas.nativeCanvas is RecordingCanvas) {
            val right = ofAdd(topLeft.x, size.width)
            val bottom = ofAdd(topLeft.y, size.height)

            drawScope.remoteDrawRoundRect(
                RemoteBrush.fromComposeUi(brush),
                topLeft.x,
                topLeft.y,
                right,
                bottom,
                cornerRadius,
                alpha,
                style,
                colorFilter,
                blendMode,
            )
        } else {
            drawScope.drawRoundRect(
                brush,
                topLeft,
                size,
                cornerRadius,
                alpha,
                style,
                colorFilter,
                blendMode,
            )
        }
    }

    override fun drawText(
        textLayoutResult: TextLayoutResult,
        color: Color,
        topLeft: Offset,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    ) {
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            color = color,
            topLeft = topLeft,
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
        topLeft: Offset,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode,
    ) {
        drawScope.drawText(
            textLayoutResult = textLayoutResult,
            brush = brush,
            topLeft = topLeft,
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
        topLeft: Offset,
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
            topLeft = topLeft,
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
        topLeft: Offset,
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
            topLeft = topLeft,
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
        topLeft: Offset,
        size: Size,
        cornerRadius: CornerRadius,
        style: DrawStyle,
        alpha: Float,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        drawScope.remoteDrawRoundRect(
            color,
            topLeft.x,
            topLeft.y,
            right,
            bottom,
            cornerRadius,
            style,
            alpha,
            colorFilter,
            blendMode,
        )
    }

    override fun drawCircle(
        brush: Brush,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawCircle(brush, radius, center, alpha, style, colorFilter, blendMode)
    }

    override fun drawCircle(
        color: Color,
        radius: Float,
        center: Offset,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        drawScope.drawCircle(color, radius, center, alpha, style, colorFilter, blendMode)
    }

    override fun drawOval(
        brush: Brush,
        topLeft: ROffset,
        size: RSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        drawScope.remoteDrawOval(
            RemoteBrush.fromComposeUi(brush),
            topLeft.x,
            topLeft.y,
            right,
            bottom,
            alpha,
            style,
            colorFilter,
            blendMode,
        )
    }

    override fun drawOval(
        color: Color,
        topLeft: ROffset,
        size: RSize,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        val right = ofAdd(topLeft.x, size.width)
        val bottom = ofAdd(topLeft.y, size.height)

        drawScope.remoteDrawOval(
            color,
            topLeft.x,
            topLeft.y,
            right,
            bottom,
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
        topLeft: Offset,
        size: Size,
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
            topLeft,
            size,
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
        topLeft: Offset,
        size: Size,
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
            topLeft,
            size,
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
        drawScope.remoteDrawPath(path, color, alpha, style, colorFilter, blendMode)
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
        tween: Number,
        color: Color,
        start: Number,
        stop: Number,
        alpha: Float,
        style: DrawStyle,
        colorFilter: ColorFilter?,
        blendMode: BlendMode,
    ) {
        if (remoteComposeCreationState is NoRemoteCompose) {
            // In Compose preview, let's at least draw one of the paths
            // TODO: support tween path in compose preview
            if (tween.toFloat() == 0f || (tween is MutableRemoteFloat && tween.value == 0f)) {
                drawPath(path1, color, alpha, style, colorFilter, blendMode)
            } else {
                drawPath(path2, color, alpha, style, colorFilter, blendMode)
            }
        } else {
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
    }

    override fun drawAnchoredText(
        text: CharSequence,
        brush: RemoteBrush,
        anchor: Offset,
        panx: Number,
        pany: Number,
        alpha: Number,
        drawStyle: DrawStyle,
        typeface: android.graphics.Typeface?,
        textSize: Number,
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
        anchor: Offset,
        panx: Number,
        pany: Number,
        alpha: Number,
        drawStyle: DrawStyle,
        typeface: android.graphics.Typeface?,
        textSize: Number,
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
        anchor: Offset,
        panx: Number,
        pany: Number,
        alpha: Number,
        drawStyle: DrawStyle,
        typeface: android.graphics.Typeface?,
        textSize: Number,
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
        anchor: Offset,
        panx: Number,
        pany: Number,
        alpha: Number,
        drawStyle: DrawStyle,
        typeface: android.graphics.Typeface?,
        textSize: Number,
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
