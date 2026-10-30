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
package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.BlackArgb
import androidx.compose.remote.creation.compose.layout.toComposeBlendMode
import androidx.compose.remote.creation.compose.layout.toComposePaintingStyle
import androidx.compose.remote.creation.compose.layout.toComposeStrokeCap
import androidx.compose.remote.creation.compose.layout.toComposeStrokeJoin
import androidx.compose.remote.creation.compose.layout.toRemoteBlendMode
import androidx.compose.remote.creation.compose.layout.toRemotePaintingStyle
import androidx.compose.remote.creation.compose.layout.toRemoteStrokeCap
import androidx.compose.remote.creation.compose.layout.toRemoteStrokeJoin
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.compose.text.toRemoteFontVariationSettings
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontVariation

/**
 * A paint object used for remote drawing operations.
 *
 * [RemotePaint] bridges standard platform paint properties with remote-first types like
 * [RemoteFloat] and [RemoteColor], allowing properties to be associated with remote IDs for
 * efficient serialization and dynamic expressions.
 *
 * This interface can be instantiated via the [RemotePaint] factory function, or converted from
 * platform paints using platform-specific extension functions.
 */
public sealed interface RemotePaint {

    /** The [BlendMode] to use when drawing with this paint. */
    public var blendMode: BlendMode

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var remoteBlendMode: RemoteBlendMode
        get() = blendMode.toRemoteBlendMode()
        set(value) {
            blendMode = value.toComposeBlendMode()
        }

    /** The [PaintingStyle] to use (e.g., Fill, Stroke). */
    public var style: PaintingStyle

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var remoteStyle: RemotePaintingStyle
        get() = style.toRemotePaintingStyle()
        set(value) {
            style = value.toComposePaintingStyle()
        }

    /** The width of the stroke when [style] is set to Stroke. */
    public var strokeWidth: RemoteFloat

    /** The [StrokeCap] to use for the ends of lines and paths. */
    public var strokeCap: StrokeCap

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var remoteStrokeCap: RemoteStrokeCap
        get() = strokeCap.toRemoteStrokeCap()
        set(value) {
            strokeCap = value.toComposeStrokeCap()
        }

    /** The [StrokeJoin] to use for the joints of lines and paths. */
    public var strokeJoin: StrokeJoin

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var remoteStrokeJoin: RemoteStrokeJoin
        get() = strokeJoin.toRemoteStrokeJoin()
        set(value) {
            strokeJoin = value.toComposeStrokeJoin()
        }

    /** The color to use for drawing. */
    public var color: RemoteColor

    /** The size of the text to draw. */
    public var textSize: RemoteFloat

    /** Whether anti-aliasing is enabled when drawing with this paint. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var isAntiAlias: Boolean

    /** The [RemoteFilterQuality] to use when scaling bitmaps. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var filterQuality: RemoteFilterQuality

    /** The [RemoteShader] to use for drawing gradients or other patterns. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var shader: RemoteShader?

    /** The color filter to apply to the drawn content. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var colorFilter: RemoteColorFilter?

    /** The [RemoteTypeface] to use for drawing text. */
    public var typeface: RemoteTypeface?

    /** The CSS-style font feature settings string (e.g., "tnum", "zero"). */
    public var fontFeatureSettings: String?

    /** The [FontVariation.Settings] to use for drawing text. */
    public var fontVariationSettings: FontVariation.Settings?

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val remoteFontVariationSettings: RemoteFontVariation.Settings?
        get() = fontVariationSettings?.toRemoteFontVariationSettings()

    public companion object {
        /**
         * Creates a new [RemotePaint] instance using [StandardRemotePaint].
         *
         * @param init An optional initialization block to configure the paint.
         */
        public operator fun invoke(init: RemotePaint.() -> Unit = {}): RemotePaint =
            StandardRemotePaint().apply(init)
    }
}

/** A default implementation of [RemotePaint] that stores properties as fields. */
internal class StandardRemotePaint() : RemotePaint {

    public override var isAntiAlias: Boolean = true
    public override var blendMode: BlendMode = BlendMode.SrcOver
    public override var style: PaintingStyle = PaintingStyle.Fill
    public override var strokeWidth: RemoteFloat = 0.0f.rf
    public override var strokeCap: StrokeCap = StrokeCap.Butt
    public override var strokeJoin: StrokeJoin = StrokeJoin.Miter
    public override var filterQuality: RemoteFilterQuality = RemoteFilterQuality.Low
    public override var shader: RemoteShader? = null
    public override var textSize: RemoteFloat = 12f.rf
    public override var typeface: RemoteTypeface? = RemoteTypeface.Default
    public override var color: RemoteColor = RemoteColor(BlackArgb)
    public override var colorFilter: RemoteColorFilter? = null
    public override var fontFeatureSettings: String? = null
    public override var fontVariationSettings: FontVariation.Settings? = null

    /**
     * Creates a [StandardRemotePaint] by copying properties from another [RemotePaint].
     *
     * @param other The paint to copy properties from.
     */
    internal constructor(other: RemotePaint) : this() {
        this.isAntiAlias = other.isAntiAlias
        this.blendMode = other.blendMode
        this.style = other.style
        this.strokeWidth = other.strokeWidth
        this.strokeCap = other.strokeCap
        this.strokeJoin = other.strokeJoin
        this.filterQuality = other.filterQuality
        this.shader = other.shader
        this.colorFilter = other.colorFilter
        this.textSize = other.textSize
        this.typeface = other.typeface
        this.color = other.color
        this.fontFeatureSettings = other.fontFeatureSettings
        this.fontVariationSettings = other.fontVariationSettings
    }

    override fun toString(): String =
        "RemotePaint(isAntiAlias=$isAntiAlias, blendMode=$blendMode, style=$style, " +
            "strokeWidth=$strokeWidth, strokeCap=$strokeCap, strokeJoin=$strokeJoin, " +
            "filterQuality=$filterQuality, shader=$shader, " +
            "textSize=$textSize, typeface=$typeface, remoteColor=$color, " +
            "colorFilter=$colorFilter, fontFeatureSettings=$fontFeatureSettings, " +
            "fontVariationSettings=$fontVariationSettings)"
}
