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

package androidx.ink.brush

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.ink.geometry.AngleRadiansFloat
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.Suppress
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic

/**
 * Parameters that control stroke mesh rendering. Note: This contains only a subset of the
 * parameters as support is added for them.
 *
 * The core of each paint consists of one or more texture layers. The output of each layer is
 * blended together in sequence, then the combined texture is blended with the output from the brush
 * color.
 * - Starting with the first [TextureLayer], the combined texture for layers 0 to i (source) is
 *   blended with layer i+1 (destination) using the blend mode for layer i.
 * - The final combined texture (source) is blended with the (possibly adjusted per-vertex) brush
 *   color (destination) according to the blend mode of the last texture layer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@ExperimentalInkCustomBrushApi
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushPaint(
    // The [textureLayers] val below is a defensive copy of this parameter.
    textureLayers: List<TextureLayer> = emptyList()
) {
    /** The textures to apply to the stroke. */
    public val textureLayers: List<TextureLayer> = unmodifiableList(textureLayers.toList())

    /** A handle to the underlying native [BrushPaint] object. */
    internal val nativePointer: Long = nativeCreateBrushPaint(textureLayers.size)

    init {
        for (layer in textureLayers) {
            nativeAppendTextureLayer(nativePointer, layer.nativePointer)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BrushPaint) return false
        return textureLayers == other.textureLayers
    }

    override fun toString(): String = "BrushPaint(textureLayers=$textureLayers)"

    override fun hashCode(): Int {
        return textureLayers.hashCode()
    }

    /** Delete native BrushPaint memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrushPaint(nativePointer)
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative private external fun nativeCreateBrushPaint(textureLayersCount: Int): Long

    /**
     * Appends a texture layer to a *mutable* C++ BrushPaint object as referenced by
     * [nativePointer]. Only call during `init{}` so to keep this BrushPaint object immutable after
     * construction and equivalent across Kotlin and C++.
     */
    @UsedByNative
    private external fun nativeAppendTextureLayer(nativePointer: Long, textureLayerPointer: Long)

    /** Release the underlying memory allocated in [nativeCreateBrushPaint]. */
    @UsedByNative private external fun nativeFreeBrushPaint(nativePointer: Long)

    /** Specification of how the texture should apply to the stroke. */
    public class TextureMapping private constructor(@JvmField internal val value: Int) {
        override fun toString(): String =
            when (this) {
                TILING -> "BrushPaint.TextureMapping.TILING"
                WINDING -> "BrushPaint.TextureMapping.WINDING"
                else -> "BrushPaint.TextureMapping.INVALID($value)"
            }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is TextureMapping) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /**
             * The texture will repeat according to a 2D affine transformation of vertex positions.
             * Each copy of the texture will have the same size and shape modulo reflections.
             */
            @JvmField public val TILING: TextureMapping = TextureMapping(0)
            /**
             * The texture will morph to "wind along the path of the stroke." The horizontal axis of
             * texture space will lie along the width of the stroke and the vertical axis will lie
             * along the direction of travel of the stroke at each point.
             */
            @JvmField public val WINDING: TextureMapping = TextureMapping(1)
        }
    }

    /** Specification of the origin point to use for the texture. */
    public class TextureOrigin private constructor(@JvmField internal val value: Int) {
        override fun toString(): String =
            when (this) {
                STROKE_SPACE_ORIGIN -> "BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN"
                FIRST_STROKE_INPUT -> "BrushPaint.TextureOrigin.FIRST_STROKE_INPUT"
                LAST_STROKE_INPUT -> "BrushPaint.TextureOrigin.LAST_STROKE_INPUT"
                else -> "BrushPaint.TextureOrigin.INVALID($value)"
            }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is TextureOrigin) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /**
             * The texture origin is the origin of stroke space, however that happens to be defined
             * for a given stroke.
             */
            @JvmField public val STROKE_SPACE_ORIGIN: TextureOrigin = TextureOrigin(0)
            /** The texture origin is the first input position for the stroke. */
            @JvmField public val FIRST_STROKE_INPUT: TextureOrigin = TextureOrigin(1)
            /**
             * The texture origin is the last input position (including predicted inputs) for the
             * stroke. Note that this means that the texture origin for an in-progress stroke will
             * move as more inputs are added.
             */
            @JvmField public val LAST_STROKE_INPUT: TextureOrigin = TextureOrigin(2)
        }
    }

    /** Units for specifying [TextureLayer.sizeX] and [TextureLayer.sizeY]. */
    public class TextureSizeUnit private constructor(@JvmField internal val value: Int) {
        override fun toString(): String =
            when (this) {
                BRUSH_SIZE -> "BrushPaint.TextureSizeUnit.BRUSH_SIZE"
                STROKE_SIZE -> "BrushPaint.TextureSizeUnit.STROKE_SIZE"
                STROKE_COORDINATES -> "BrushPaint.TextureSizeUnit.STROKE_COORDINATES"
                else -> "BrushPaint.TextureSizeUnit.INVALID($value)"
            }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is TextureSizeUnit) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /** As multiples of brush size. */
            @JvmField public val BRUSH_SIZE: TextureSizeUnit = TextureSizeUnit(0)
            /**
             * As multiples of the stroke "size". This has different meanings depending on the value
             * of [TextureMapping] for the given texture. For [TextureMapping.TILING] textures, the
             * stroke size is equal to the dimensions of the XY bounding rectangle of the mesh. For
             * [TextureMapping.WINDING] textures, the stroke size components are given by x: stroke
             * width, which may change over the course of the stroke if behaviors affect the tip
             * geometry. y: the total distance traveled by the stroke.
             */
            @JvmField public val STROKE_SIZE: TextureSizeUnit = TextureSizeUnit(1)
            /** In the same units as the stroke's input positions and stored geometry. */
            @JvmField public val STROKE_COORDINATES: TextureSizeUnit = TextureSizeUnit(2)
        }
    }

    /**
     * The method by which the combined texture layers (index <= i) are blended with the next layer.
     * The blend mode on the final layer controls how the combined texture is blended with the brush
     * color, and should typically be a mode whose output alpha is proportional to the destination
     * alpha, so that it can be adjusted by anti-aliasing.
     */
    public class BlendMode private constructor(@JvmField internal val value: Int) {
        override fun toString(): String =
            when (this) {
                MODULATE -> "BrushPaint.BlendMode.MODULATE"
                DST_IN -> "BrushPaint.BlendMode.DST_IN"
                DST_OUT -> "BrushPaint.BlendMode.DST_OUT"
                SRC_ATOP -> "BrushPaint.BlendMode.SRC_ATOP"
                SRC_IN -> "BrushPaint.BlendMode.SRC_IN"
                SRC_OVER -> "BrushPaint.BlendMode.SRC_OVER"
                DST_OVER -> "BrushPaint.BlendMode.DST_OVER"
                SRC -> "BrushPaint.BlendMode.SRC"
                DST -> "BrushPaint.BlendMode.DST"
                SRC_OUT -> "BrushPaint.BlendMode.SRC_OUT"
                DST_ATOP -> "BrushPaint.BlendMode.DST_ATOP"
                XOR -> "BrushPaint.BlendMode.XOR"
                else -> "BrushPaint.BlendMode.INVALID($value)"
            }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            return other is BlendMode && this.value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {

            /**
             * Source and destination are component-wise multiplied, including opacity.
             *
             * ```
             * Alpha = Alpha_src * Alpha_dst
             * Color = Color_src * Color_dst
             * ```
             */
            @JvmField public val MODULATE: BlendMode = BlendMode(0)
            /**
             * Keeps destination pixels that cover source pixels. Discards remaining source and
             * destination pixels.
             *
             * ```
             * Alpha = Alpha_src * Alpha_dst
             * Color = Alpha_src * Color_dst
             * ```
             */
            @JvmField public val DST_IN: BlendMode = BlendMode(1)
            /**
             * Keeps the destination pixels not covered by source pixels. Discards destination
             * pixels that are covered by source pixels and all source pixels.
             *
             * ```
             * Alpha = (1 - Alpha_src) * Alpha_dst
             * Color = (1 - Alpha_src) * Color_dst
             * ```
             */
            @JvmField public val DST_OUT: BlendMode = BlendMode(2)
            /**
             * Discards source pixels that do not cover destination pixels. Draws remaining pixels
             * over destination pixels.
             *
             * ```
             * Alpha = Alpha_dst
             * Color = Alpha_dst * Color_src + (1 - Alpha_src) * Color_dst
             * ```
             */
            @JvmField public val SRC_ATOP: BlendMode = BlendMode(3)
            /**
             * Keeps the source pixels that cover destination pixels. Discards remaining source and
             * destination pixels.
             *
             * ```
             * Alpha = Alpha_src * Alpha_dst
             * Color = Color_src * Alpha_dst
             * ```
             */
            @JvmField public val SRC_IN: BlendMode = BlendMode(4)

            /*
             * The following modes can't be used for the last TextureLayer, which defines the mode for
             * blending the combined texture with the (possibly adjusted per-vertex) brush color. That blend
             * mode needs the output Alpha to be a multiple of Alpha_dst so that per-vertex adjustment for
             * anti-aliasing is preserved correctly.
             */

            /**
             * The source pixels are drawn over the destination pixels.
             *
             * ```
             * Alpha = Alpha_src + (1 - Alpha_src) * Alpha_dst
             * Color = Color_src + (1 - Alpha_src) * Color_dst
             * ```
             *
             * This mode shouldn't normally be used for the final [TextureLayer], since its output
             * alpha is not proportional to the destination alpha (so it wouldn't preserve alpha
             * adjustments from anti-aliasing).
             */
            @JvmField public val SRC_OVER: BlendMode = BlendMode(5)
            /**
             * The source pixels are drawn behind the destination pixels.
             *
             * ```
             * Alpha = Alpha_dst + (1 - Alpha_dst) * Alpha_src
             * Color = Color_dst + (1 - Alpha_dst) * Color_src
             * ```
             *
             * This mode shouldn't normally be used for the final [TextureLayer], since its output
             * alpha is not proportional to the destination alpha (so it wouldn't preserve alpha
             * adjustments from anti-aliasing).
             */
            @JvmField public val DST_OVER: BlendMode = BlendMode(6)
            /**
             * Keeps the source pixels and discards the destination pixels.
             *
             * ```
             * Alpha = Alpha_src
             * Color = Color_src
             * ```
             *
             * This mode shouldn't normally be used for the final [TextureLayer], since its output
             * alpha is not proportional to the destination alpha (so it wouldn't preserve alpha
             * adjustments from anti-aliasing).
             */
            @JvmField public val SRC: BlendMode = BlendMode(7)
            /**
             * Keeps the destination pixels and discards the source pixels.
             *
             * ```
             * Alpha = Alpha_dst
             * Color = Color_dst
             * ```
             *
             * This mode is unlikely to be useful, since it effectively causes the renderer to just
             * ignore this [TextureLayer] and all layers before it, but it is included for
             * completeness.
             */
            @JvmField public val DST: BlendMode = BlendMode(8)
            /**
             * Keeps the source pixels that do not cover destination pixels. Discards destination
             * pixels and all source pixels that cover destination pixels.
             *
             * ```
             * Alpha = (1 - Alpha_dst) * Alpha_src
             * Color = (1 - Alpha_dst) * Color_src
             * ```
             *
             * This mode shouldn't normally be used for the final [TextureLayer], since its output
             * alpha is not proportional to the destination alpha (so it wouldn't preserve alpha
             * adjustments from anti-aliasing).
             */
            @JvmField public val SRC_OUT: BlendMode = BlendMode(9)
            /**
             * Discards destination pixels that aren't covered by source pixels. Remaining
             * destination pixels are drawn over source pixels.
             *
             * ```
             * Alpha = Alpha_src
             * Color = Alpha_src * Color_dst + (1 - Alpha_dst) * Color_src
             * ```
             *
             * This mode shouldn't normally be used for the final [TextureLayer], since its output
             * alpha is not proportional to the destination alpha (so it wouldn't preserve alpha
             * adjustments from anti-aliasing).
             */
            @JvmField public val DST_ATOP: BlendMode = BlendMode(10)
            /**
             * Discards source and destination pixels that intersect; keeps source and destination
             * pixels that do not intersect.
             *
             * ```
             * Alpha = (1 - Alpha_dst) * Alpha_src + (1 - Alpha_src) * Alpha_dst
             * Color = (1 - Alpha_dst) * Color_src + (1 - Alpha_src) * Color_dst
             * ```
             *
             * This mode shouldn't normally be used for the final [TextureLayer], since its output
             * alpha is not proportional to the destination alpha (so it wouldn't preserve alpha
             * adjustments from anti-aliasing).
             */
            @JvmField public val XOR: BlendMode = BlendMode(11)
        }
    }

    /**
     * An explicit layer defined by an image.
     *
     * @param colorTextureUri The URI of an image that provides the color for a particular pixel for
     *   this layer. The coordinates within this image that will be used are determined by the other
     *   parameters.
     * @param sizeX The X size in [TextureSizeUnit] of the image specified by [colorTextureUri].
     * @param sizeY The Y size in [TextureSizeUnit] of the image specified by [colorTextureUri].
     * @param offsetX An offset into the texture, specified as fractions of the texture [sizeX] in
     *   the range [0,1].
     * @param offsetY An offset into the texture, specified as fractions of the texture [sizeY] in
     *   the range [0,1].
     * @param rotation Angle in radians specifying the rotation of the texture. The rotation is
     *   carried out about the center of the texture's first repetition along both axes.
     * @param opacity Overall layer opacity in the range [0,1], where 0 is transparent and 1 is
     *   opaque.
     * @param sizeUnit The units used to specify [sizeX] and [sizeY].
     * @param mapping The method by which the coordinates of the [colorTextureUri] image will apply
     *   to the stroke.
     * @param blendMode The method by which the texture layers up to this one (index <= i) are
     *   combined with the subsequent texture layer (index == i+1). For the last texture layer, this
     *   defines the method by which the texture layer is combined with the brush color (possibly
     *   after that color gets per-vertex adjustments).
     */
    @Suppress("NotCloseable") // Finalize is only used to free the native peer.
    public class TextureLayer(
        public val colorTextureUri: String,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        public val sizeX: Float,
        @FloatRange(
            from = 0.0,
            fromInclusive = false,
            to = Double.POSITIVE_INFINITY,
            toInclusive = false,
        )
        public val sizeY: Float,
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
        public val offsetX: Float = 0f,
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
        public val offsetY: Float = 0f,
        @AngleRadiansFloat public val rotation: Float = 0F,
        @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
        public val opacity: Float = 1f,
        public val sizeUnit: TextureSizeUnit = TextureSizeUnit.STROKE_COORDINATES,
        public val origin: TextureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
        public val mapping: TextureMapping = TextureMapping.TILING,
        public val blendMode: BlendMode = BlendMode.MODULATE,
    ) {
        internal val nativePointer: Long =
            nativeCreateTextureLayer(
                colorTextureUri,
                sizeX,
                sizeY,
                offsetX,
                offsetY,
                rotation,
                opacity,
                sizeUnit.value,
                origin.value,
                mapping.value,
                blendMode.value,
            )

        /**
         * Creates a copy of `this` and allows named properties to be altered while keeping the rest
         * unchanged.
         */
        @JvmSynthetic
        public fun copy(
            colorTextureUri: String = this.colorTextureUri,
            sizeX: Float = this.sizeX,
            sizeY: Float = this.sizeY,
            offsetX: Float = this.offsetX,
            offsetY: Float = this.offsetY,
            @AngleRadiansFloat rotation: Float = this.rotation,
            opacity: Float = this.opacity,
            sizeUnit: TextureSizeUnit = this.sizeUnit,
            origin: TextureOrigin = this.origin,
            mapping: TextureMapping = this.mapping,
            blendMode: BlendMode = this.blendMode,
        ): TextureLayer {
            if (
                colorTextureUri == this.colorTextureUri &&
                    sizeX == this.sizeX &&
                    sizeY == this.sizeY &&
                    offsetX == this.offsetX &&
                    offsetY == this.offsetY &&
                    rotation == this.rotation &&
                    opacity == this.opacity &&
                    sizeUnit == this.sizeUnit &&
                    origin == this.origin &&
                    mapping == this.mapping &&
                    blendMode == this.blendMode
            ) {
                return this
            }
            return TextureLayer(
                colorTextureUri,
                sizeX,
                sizeY,
                offsetX,
                offsetY,
                rotation,
                opacity,
                sizeUnit,
                origin,
                mapping,
                blendMode,
            )
        }

        /**
         * Returns a [Builder] with values set equivalent to `this`. Java developers, use the
         * returned builder to build a copy of a TextureLayer. Kotlin developers, see [copy] method.
         */
        public fun toBuilder(): Builder =
            Builder(
                colorTextureUri = this.colorTextureUri,
                sizeX = this.sizeX,
                sizeY = this.sizeY,
                offsetX = this.offsetX,
                offsetY = this.offsetY,
                rotation = this.rotation,
                opacity = this.opacity,
                sizeUnit = this.sizeUnit,
                origin = this.origin,
                mapping = this.mapping,
                blendMode = this.blendMode,
            )

        override fun equals(other: Any?): Boolean {
            if (other !is TextureLayer) return false
            return colorTextureUri == other.colorTextureUri &&
                sizeX == other.sizeX &&
                sizeY == other.sizeY &&
                offsetX == other.offsetX &&
                offsetY == other.offsetY &&
                rotation == other.rotation &&
                opacity == other.opacity &&
                sizeUnit == other.sizeUnit &&
                origin == other.origin &&
                mapping == other.mapping &&
                blendMode == other.blendMode
        }

        override fun toString(): String =
            "BrushPaint.TextureLayer(colorTextureUri=$colorTextureUri, sizeX=$sizeX, sizeY=$sizeY, " +
                "offset=[$offsetX, $offsetY], rotation=$rotation, opacity=$opacity sizeUnit=$sizeUnit, origin=$origin, mapping=$mapping, " +
                "blendMode=$blendMode)"

        override fun hashCode(): Int {
            var result = colorTextureUri.hashCode()
            result = 31 * result + sizeX.hashCode()
            result = 31 * result + sizeY.hashCode()
            result = 31 * result + offsetX.hashCode()
            result = 31 * result + offsetY.hashCode()
            result = 31 * result + rotation.hashCode()
            result = 31 * result + opacity.hashCode()
            result = 31 * result + sizeUnit.hashCode()
            result = 31 * result + origin.hashCode()
            result = 31 * result + mapping.hashCode()
            result = 31 * result + blendMode.hashCode()
            return result
        }

        /** Delete native TextureLayer memory. */
        protected fun finalize() {
            // NOMUTANTS -- Not tested post garbage collection.
            nativeFreeTextureLayer(nativePointer)
        }

        /**
         * Builder for [TextureLayer].
         *
         * Construct from TextureLayer.toBuilder().
         */
        @Suppress(
            "ScopeReceiverThis"
        ) // Builder pattern supported for Java clients, despite being an anti-pattern in Kotlin.
        public class Builder
        internal constructor(
            private var colorTextureUri: String,
            @FloatRange(
                from = 0.0,
                fromInclusive = false,
                to = Double.POSITIVE_INFINITY,
                toInclusive = false,
            )
            private var sizeX: Float,
            @FloatRange(
                from = 0.0,
                fromInclusive = false,
                to = Double.POSITIVE_INFINITY,
                toInclusive = false,
            )
            private var sizeY: Float,
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
            private var offsetX: Float = 0f,
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
            private var offsetY: Float = 0f,
            @AngleRadiansFloat private var rotation: Float = 0F,
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = true, toInclusive = true)
            private var opacity: Float = 1f,
            private var sizeUnit: TextureSizeUnit = TextureSizeUnit.STROKE_COORDINATES,
            private var origin: TextureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
            private var mapping: TextureMapping = TextureMapping.TILING,
            private var blendMode: BlendMode = BlendMode.MODULATE,
        ) {
            public fun setColorTextureUri(colorTextureUri: String): Builder = apply {
                this.colorTextureUri = colorTextureUri
            }

            public fun setSizeX(sizeX: Float): Builder = apply { this.sizeX = sizeX }

            public fun setSizeY(sizeY: Float): Builder = apply { this.sizeY = sizeY }

            public fun setOffsetX(offsetX: Float): Builder = apply { this.offsetX = offsetX }

            public fun setOffsetY(offsetY: Float): Builder = apply { this.offsetY = offsetY }

            public fun setRotation(rotation: Float): Builder = apply { this.rotation = rotation }

            public fun setOpacity(opacity: Float): Builder = apply { this.opacity = opacity }

            public fun setSizeUnit(sizeUnit: TextureSizeUnit): Builder = apply {
                this.sizeUnit = sizeUnit
            }

            public fun setOrigin(origin: TextureOrigin): Builder = apply { this.origin = origin }

            public fun setMapping(mapping: TextureMapping): Builder = apply {
                this.mapping = mapping
            }

            public fun setBlendMode(blendMode: BlendMode): Builder = apply {
                this.blendMode = blendMode
            }

            public fun build(): TextureLayer =
                TextureLayer(
                    colorTextureUri,
                    sizeX,
                    sizeY,
                    offsetX,
                    offsetY,
                    rotation,
                    opacity,
                    sizeUnit,
                    origin,
                    mapping,
                    blendMode,
                )
        }

        @UsedByNative
        private external fun nativeCreateTextureLayer(
            colorTextureUri: String,
            sizeX: Float,
            sizeY: Float,
            offsetX: Float,
            offsetY: Float,
            rotation: Float,
            opacity: Float,
            sizeUnit: Int,
            origin: Int,
            mapping: Int,
            blendMode: Int,
        ): Long

        /** Release the underlying memory allocated in [nativeCreateTextureLayer]. */
        @UsedByNative private external fun nativeFreeTextureLayer(nativePointer: Long)

        // To be extended by extension methods.
        public companion object
    }

    // To be extended by extension methods.
    public companion object {
        init {
            NativeLoader.load()
        }
    }
}
