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

package androidx.compose.remote.creation.compose.state

import android.graphics.BlendModeColorFilter as AndroidBlendModeColorFilter
import android.graphics.ColorFilter as AndroidColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.toAndroidBlendMode
import androidx.compose.remote.creation.compose.layout.toAndroidCap
import androidx.compose.remote.creation.compose.layout.toAndroidJoin
import androidx.compose.remote.creation.compose.layout.toAndroidStyle
import androidx.compose.remote.creation.compose.layout.toComposeBlendMode
import androidx.compose.remote.creation.compose.layout.toPaintingStyle
import androidx.compose.remote.creation.compose.layout.toStrokeCap
import androidx.compose.remote.creation.compose.layout.toStrokeJoin
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.shaders.RemoteSolidColor
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asAndroidPathEffect
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposePathEffect

/**
 * A paint object used for remote drawing operations.
 *
 * [RemotePaint] bridges standard platform paint properties with remote-first types like
 * [RemoteFloat] and [RemoteColor], allowing properties to be associated with remote IDs for
 * efficient serialization and dynamic expressions.
 *
 * This interface can be implemented by classes that wrap standard [android.graphics.Paint] or
 * [androidx.compose.ui.graphics.Paint], or by a pure data implementation like
 * [StandardRemotePaint].
 */
public sealed interface RemotePaint {

    /** The [BlendMode] to use when drawing with this paint. */
    public var blendMode: BlendMode

    /** The [PaintingStyle] to use (e.g., Fill, Stroke). */
    public var style: PaintingStyle

    /** The width of the stroke when [style] is set to Stroke. */
    public var strokeWidth: RemoteFloat

    /** The [StrokeCap] to use for the ends of lines and paths. */
    public var strokeCap: StrokeCap

    /** The [StrokeJoin] to use for the joints of lines and paths. */
    public var strokeJoin: StrokeJoin

    /** The color to use for drawing. */
    public var color: RemoteColor

    /** The size of the text to draw. */
    public var textSize: RemoteFloat

    /** Whether anti-aliasing is enabled when drawing with this paint. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var isAntiAlias: Boolean

    /** The [FilterQuality] to use when scaling bitmaps. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var filterQuality: FilterQuality

    /** The [Shader] to use for drawing gradients or other patterns. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var shader: Shader?

    /** The [PathEffect] to apply to the stroke. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var pathEffect: PathEffect?

    /** The color filter to apply to the drawn content. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var colorFilter: RemoteColorFilter?

    /** The [Typeface] to use for drawing text. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var typeface: Typeface?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a new [RemotePaint] instance using [StandardRemotePaint].
         *
         * @param init An optional initialization block to configure the paint.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public operator fun invoke(init: StandardRemotePaint.() -> Unit = {}): RemotePaint =
            StandardRemotePaint().apply(init)
    }
}

/** A default implementation of [RemotePaint] that stores properties as fields. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StandardRemotePaint() : RemotePaint {

    public override var isAntiAlias: Boolean = true
    public override var blendMode: BlendMode = BlendMode.SrcOver
    public override var style: PaintingStyle = PaintingStyle.Fill
    public override var strokeWidth: RemoteFloat = 0.0f.rf
    public override var strokeCap: StrokeCap = StrokeCap.Butt
    public override var strokeJoin: StrokeJoin = StrokeJoin.Miter
    public override var filterQuality: FilterQuality = FilterQuality.Low
    public override var shader: Shader? = null
    public override var pathEffect: PathEffect? = null
    public override var textSize: RemoteFloat = 12f.rf
    public override var typeface: Typeface? = Typeface.DEFAULT
    public override var color: RemoteColor = Color.Black.rc
    public override var colorFilter: RemoteColorFilter? = null

    /**
     * Creates a [StandardRemotePaint] by copying properties from another [RemotePaint].
     *
     * @param other The paint to copy properties from.
     */
    public constructor(other: RemotePaint) : this() {
        this.isAntiAlias = other.isAntiAlias
        this.blendMode = other.blendMode
        this.style = other.style
        this.strokeWidth = other.strokeWidth
        this.strokeCap = other.strokeCap
        this.strokeJoin = other.strokeJoin
        this.filterQuality = other.filterQuality
        this.shader = other.shader
        this.pathEffect = other.pathEffect
        this.colorFilter = other.colorFilter
        this.textSize = other.textSize
        this.typeface = other.typeface
        this.color = other.color
    }

    /**
     * Applies a [RemoteBrush] to this paint.
     *
     * Depending on whether the brush is a shader or a solid color, this method updates [shader] and
     * [color] accordingly.
     *
     * @param remoteBrush The brush to apply.
     * @param size The size of the area being drawn, used for shader calculation.
     * @param matrix3x3 An optional matrix to apply to the shader.
     */
    public fun RemoteStateScope.applyRemoteBrush(
        remoteBrush: RemoteBrush,
        size: RemoteSize,
        matrix3x3: RemoteMatrix3x3? = null,
    ) {
        if (remoteBrush.hasShader) {
            shader =
                with(remoteBrush) { createShader(size).apply { this.remoteMatrix3x3 = matrix3x3 } }
            color = Color.Black.rc
        } else if (remoteBrush is RemoteSolidColor) {
            color = remoteBrush.color
            shader = null
        } else {
            throw UnsupportedOperationException("Unsupported brush type: $remoteBrush")
        }
    }

    override fun toString(): String {
        return "RemotePaint(isAntiAlias=$isAntiAlias, blendMode=$blendMode, style=$style, strokeWidth=$strokeWidth, strokeCap=$strokeCap, strokeJoin=$strokeJoin, filterQuality=$filterQuality, shader=$shader, pathEffect=$pathEffect, textSize=$textSize, typeface=$typeface, remoteColor=$color, colorFilter=$colorFilter)"
    }
}

/**
 * An implementation of [android.graphics.Paint] that supports [RemoteColor] and [RemoteShader].
 *
 * This class provides a bridge for using remote-first types with standard Android paint APIs,
 * allowing it to be easily converted to a [RemotePaint].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class CompatAndroidRemotePaint : AndroidPaint, RemotePaintConvertible {
    public constructor() : super()

    public constructor(flags: Int) : super(flags)

    public constructor(paint: AndroidPaint) : super(paint) {
        if (paint is CompatAndroidRemotePaint) {
            remoteColor = paint.remoteColor
            remoteColorFilter = paint.remoteColorFilter
            remoteShader = paint.remoteShader
        }
    }

    /**
     * The [RemoteColor] associated with this paint.
     *
     * Setting this property also updates the underlying [android.graphics.Paint.setColor] if the
     * [RemoteColor] has a constant value.
     */
    public var remoteColor: RemoteColor? = null
        set(value) {
            field = value
            if (value != null) {
                val constantValue = value.constantValueOrNull
                if (constantValue != null) {
                    super.setColor(constantValue.toArgb())
                } else {
                    super.setColor(android.graphics.Color.TRANSPARENT)
                }
            }
        }

    public override fun setColor(@ColorInt color: Int) {
        remoteColor = null
        super.setColor(color)
    }

    /**
     * The [RemoteColorFilter] associated with this paint.
     *
     * Setting this property also updates the underlying [android.graphics.Paint.setColorFilter] if
     * the [RemoteColorFilter] is a type that can be converted to a platform color filter.
     */
    public var remoteColorFilter: RemoteColorFilter? = null
        set(value) {
            field = value
            when (value) {
                is RemoteBlendModeColorFilter -> {
                    val constantValue = value.color.constantValueOrNull
                    if (constantValue != null) {
                        super.setColorFilter(
                            AndroidBlendModeColorFilter(
                                constantValue.toArgb(),
                                value.blendMode.toAndroidBlendMode(),
                            )
                        )
                    } else {
                        super.setColorFilter(null)
                    }
                }
                is ComposeRemoteColorFilter -> {
                    super.setColorFilter(value.composeColorFilter.asAndroidColorFilter())
                }
                null -> super.setColorFilter(null)
            }
        }

    public override fun setColorFilter(filter: AndroidColorFilter?): AndroidColorFilter? {
        remoteColorFilter = null
        return super.setColorFilter(filter)
    }

    /**
     * The [RemoteShader] associated with this paint.
     *
     * This is a convenience property that aliases [android.graphics.Paint.setShader].
     */
    public var remoteShader: RemoteShader?
        get() = shader as? RemoteShader
        set(value) {
            shader = value
        }

    /**
     * Applies a [RemoteBrush] to this paint.
     *
     * Depending on whether the brush is a shader or a solid color, this method updates
     * [remoteShader] and [remoteColor] accordingly.
     *
     * @param remoteBrush The brush to apply.
     * @param size The size of the area being drawn, used for shader calculation.
     * @param matrix3x3 An optional matrix to apply to the shader.
     */
    public fun RemoteStateScope.applyRemoteBrush(
        remoteBrush: RemoteBrush,
        size: RemoteSize,
        matrix3x3: RemoteMatrix3x3? = null,
    ) {
        if (remoteBrush.hasShader) {
            remoteShader =
                with(remoteBrush) { createShader(size).apply { this.remoteMatrix3x3 = matrix3x3 } }
            remoteColor = null
        } else if (remoteBrush is RemoteSolidColor) {
            remoteColor = remoteBrush.color
            remoteShader = null
        } else {
            throw UnsupportedOperationException("Unsupported brush type: $remoteBrush")
        }
    }

    /** Converts this paint to a [RemotePaint]. */
    override val remotePaint: RemotePaint
        get() = AndroidRemotePaint(this)
}

/** An implementation of [RemotePaint] that wraps an [android.graphics.Paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AndroidRemotePaint(internal val frameworkPaint: android.graphics.Paint) : RemotePaint {

    override var isAntiAlias: Boolean
        get() = frameworkPaint.isAntiAlias
        set(value) {
            frameworkPaint.isAntiAlias = value
        }

    override var blendMode: BlendMode
        get() = frameworkPaint.blendMode?.toComposeBlendMode() ?: BlendMode.SrcOver
        set(value) {
            frameworkPaint.blendMode = value.toAndroidBlendMode()
        }

    override var style: PaintingStyle
        get() = frameworkPaint.style.toPaintingStyle()
        set(value) {
            frameworkPaint.style = value.toAndroidStyle()
        }

    override var strokeWidth: RemoteFloat
        get() = frameworkPaint.strokeWidth.rf
        set(value) {
            // Can fail on non constant values
            frameworkPaint.strokeWidth = value.constantValue
        }

    override var strokeCap: StrokeCap
        get() = frameworkPaint.strokeCap.toStrokeCap()
        set(value) {
            frameworkPaint.strokeCap = value.toAndroidCap()
        }

    override var strokeJoin: StrokeJoin
        get() = frameworkPaint.strokeJoin.toStrokeJoin()
        set(value) {
            frameworkPaint.strokeJoin = value.toAndroidJoin()
        }

    override var filterQuality: FilterQuality
        get() =
            frameworkPaint.let {
                if (it.isFilterBitmap) {
                    FilterQuality.None
                } else {
                    FilterQuality.Low
                }
            }
        set(value) {
            frameworkPaint.isFilterBitmap = value != FilterQuality.None
        }

    override var shader: Shader?
        get() = frameworkPaint.shader
        set(value) {
            frameworkPaint.shader = value
        }

    override var pathEffect: PathEffect?
        get() = frameworkPaint.pathEffect.toComposePathEffect()
        set(value) {
            frameworkPaint.pathEffect = value?.asAndroidPathEffect()
        }

    override var color: RemoteColor
        get() = RemoteColor(frameworkPaint.color)
        set(value) {
            // Can fail on non constant values
            frameworkPaint.color = value.constantValue.toArgb()
        }

    override var colorFilter: RemoteColorFilter?
        get() =
            (frameworkPaint.colorFilter as? AndroidBlendModeColorFilter)?.let {
                RemoteBlendModeColorFilter(Color(it.color).rc, it.mode.toComposeBlendMode())
            }
        set(value) {
            frameworkPaint.colorFilter =
                when (value) {
                    is RemoteBlendModeColorFilter ->
                        AndroidBlendModeColorFilter(
                            value.color.constantValue.toArgb(),
                            value.blendMode.toAndroidBlendMode(),
                        )
                    is ComposeRemoteColorFilter -> value.composeColorFilter.asAndroidColorFilter()
                    null -> null
                }
        }

    override var textSize: RemoteFloat
        get() = frameworkPaint.textSize.rf
        set(value) {
            // Can fail on non constant values
            frameworkPaint.textSize = value.constantValue
        }

    override var typeface: Typeface?
        get() = frameworkPaint.typeface
        set(value) {
            frameworkPaint.typeface = value
        }

    override fun toString(): String {
        return "AndroidRemotePaint(frameworkPaint=$frameworkPaint)"
    }
}

/** An implementation of [RemotePaint] that wraps a [androidx.compose.ui.graphics.Paint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeRemotePaint(internal val composePaint: Paint) : RemotePaint {
    override var isAntiAlias: Boolean
        get() = composePaint.isAntiAlias
        set(value) {
            composePaint.isAntiAlias = value
        }

    override var blendMode: BlendMode
        get() = composePaint.blendMode
        set(value) {
            composePaint.blendMode = value
        }

    override var style: PaintingStyle
        get() = composePaint.style
        set(value) {
            composePaint.style = value
        }

    override var strokeWidth: RemoteFloat
        get() = composePaint.strokeWidth.rf
        set(value) {
            // Can fail on non constant values
            composePaint.strokeWidth = value.constantValue
        }

    override var strokeCap: StrokeCap
        get() = composePaint.strokeCap
        set(value) {
            composePaint.strokeCap = value
        }

    override var strokeJoin: StrokeJoin
        get() = composePaint.strokeJoin
        set(value) {
            composePaint.strokeJoin = value
        }

    override var filterQuality: FilterQuality
        get() = composePaint.filterQuality
        set(value) {
            composePaint.filterQuality = value
        }

    override var shader: Shader?
        get() = composePaint.shader
        set(value) {
            composePaint.shader = value
        }

    override var pathEffect: PathEffect?
        get() = composePaint.pathEffect
        set(value) {
            composePaint.pathEffect = value
        }

    override var color: RemoteColor
        get() = composePaint.color.rc
        set(value) {
            // Can fail on non constant values
            composePaint.color = value.constantValue
        }

    override var colorFilter: RemoteColorFilter?
        get() = composePaint.colorFilter?.let { ComposeRemoteColorFilter(it) }
        set(value) {
            composePaint.colorFilter = (value as ComposeRemoteColorFilter).composeColorFilter
        }

    override var textSize: RemoteFloat
        get() = composePaint.nativePaint.textSize.rf
        set(value) {
            // Can fail on non constant values
            composePaint.nativePaint.textSize = value.constantValue
        }

    override var typeface: Typeface?
        get() = composePaint.nativePaint.typeface
        set(value) {
            composePaint.nativePaint.typeface = value
        }

    override fun toString(): String {
        return "ComposeRemotePaint(composePaint=$composePaint)"
    }
}

/** Converts a [androidx.compose.ui.graphics.Paint] to a [RemotePaint]. */
public fun Paint.asRemotePaint(): RemotePaint = ComposeRemotePaint(this)

/** Converts an [android.graphics.Paint] to a [RemotePaint]. */
public fun android.graphics.Paint.asRemotePaint(): RemotePaint =
    if (this is RemotePaintConvertible) {
        remotePaint
    } else {
        AndroidRemotePaint(this)
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemotePaintConvertible {
    public val remotePaint: RemotePaint
}
