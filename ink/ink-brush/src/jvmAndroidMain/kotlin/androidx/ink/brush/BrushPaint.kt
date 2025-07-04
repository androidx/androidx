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
import androidx.annotation.IntRange
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@ExperimentalInkCustomBrushApi
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushPaint
private constructor(
    /** A handle to the underlying native [BrushPaint] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    // The [textureLayers] val below is a defensive copy of this parameter.
    textureLayers: List<TextureLayer>,
) {
    /** The textures to apply to the stroke. */
    public val textureLayers: List<TextureLayer> = unmodifiableList(textureLayers.toList())

    /**
     * Creates a [BrushPaint] with the given [textureLayers].
     *
     * @param textureLayers The textures to apply to the stroke.
     */
    @JvmOverloads
    public constructor(
        textureLayers: List<TextureLayer> = emptyList()
    ) : this(
        BrushPaintNative.create(textureLayers.map { it.nativePointer }.toLongArray()),
        textureLayers,
    )

    override fun equals(other: Any?): Boolean {
        if (other !is BrushPaint) return false
        return textureLayers == other.textureLayers
    }

    override fun toString(): String = "BrushPaint(textureLayers=$textureLayers)"

    override fun hashCode(): Int {
        return textureLayers.hashCode()
    }

    /** Delete native BrushPaint memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // TODO: b/423019041 - Investigate why this is failing in native code with nativePointer=0
        if (nativePointer != 0L) {
            BrushPaintNative.free(nativePointer)
        }
    }

    /** Specification of how the texture should apply to the stroke. */
    public class TextureMapping internal constructor(v: Int) {

        // Not declared in the constructor to avoid conflicting compile/lint errors/warnings.
        @JvmField
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        public val value: Int = v

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
    public class TextureOrigin internal constructor(@JvmField internal val value: Int) {
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
    public class TextureSizeUnit internal constructor(@JvmField internal val value: Int) {
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

    /** Wrap modes for specifying [TextureLayer.wrapX] and [TextureLayer.wrapY]. */
    public class TextureWrap internal constructor(@JvmField internal val value: Int) {
        override fun toString(): String =
            when (this) {
                REPEAT -> "BrushPaint.TextureWrap.REPEAT"
                MIRROR -> "BrushPaint.TextureWrap.MIRROR"
                CLAMP -> "BrushPaint.TextureWrap.CLAMP"
                else -> "BrushPaint.TextureWrap.INVALID($value)"
            }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is TextureWrap) return false
            return value == other.value
        }

        override fun hashCode(): Int = value.hashCode()

        public companion object {
            /** Repeats texture image horizontally/vertically. */
            @JvmField public val REPEAT: TextureWrap = TextureWrap(0)
            /**
             * Repeats texture image horizontally/vertically, alternating mirror images so that
             * adjacent edges always match.
             */
            @JvmField public val MIRROR: TextureWrap = TextureWrap(1)
            /**
             * Points outside of the texture have the color of the nearest texture edge point. This
             * mode is typically most useful when the edge pixels of the texture image are all the
             * same, e.g. either transparent or a single solid color.
             */
            @JvmField public val CLAMP: TextureWrap = TextureWrap(2)
        }
    }

    /**
     * The method by which the combined texture layers (index <= i) are blended with the next layer.
     * The blend mode on the final layer controls how the combined texture is blended with the brush
     * color, and should typically be a mode whose output alpha is proportional to the destination
     * alpha, so that it can be adjusted by anti-aliasing.
     */
    public class BlendMode internal constructor(@JvmField internal val value: Int) {
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

    /** An explicit layer defined by an image. */
    @Suppress("NotCloseable") // Finalize is only used to free the native peer.
    public class TextureLayer private constructor(internal val nativePointer: Long) {

        /**
         * Creates a new [TextureLayer] with the specified parameters.
         *
         * @param clientTextureId A string identifier of an image that provides the color for a
         *   particular pixel for this layer. The coordinates within this image that will be used
         *   are determined by the other parameters.
         * @param sizeX The X size in [TextureSizeUnit] of (one animation frame of) the image
         *   specified by [clientTextureId].
         * @param sizeY The Y size in [TextureSizeUnit] of (one animation frame of) the image
         *   specified by [clientTextureId].
         * @param offsetX An offset into the texture, specified as fractions of the texture [sizeX].
         * @param offsetY An offset into the texture, specified as fractions of the texture [sizeY].
         * @param rotation Angle in radians specifying the rotation of the texture. The rotation is
         *   carried out about the center of the texture's first repetition along both axes.
         * @param opacity Overall layer opacity in the range [0,1], where 0 is transparent and 1 is
         *   opaque.
         * @param animationFrames The number of animation frames in this texture, or 1 for no
         *   animation. If greater than 1, then the texture image is treated as a grid of animation
         *   frame images, with dimensions of [animationRows] by [animationColumns] frames. The
         *   frames will be indexed in row-major order, where row=0 and column=0 is frame index 0,
         *   then row=0 and column=1 is frame index 1, and so on. [animationFrames] must be at most
         *   the product of [animationRows] and [animationColumns], and there may be unused frame
         *   cells in the final row.
         * @param animationRows The number of frame rows in this texture. See [animationFrames] for
         *   more detail. When constructing an animation texture image, avoid making it too large in
         *   any one dimension by choosing values for [animationRows] and [animationColumns] that
         *   are close to each other, but just large enough such that [animationFrames] <=
         *   [animationRows] * [animationColumns].
         * @param animationColumns Like [animationRows], but for columns.
         * @param sizeUnit The units used to specify [sizeX] and [sizeY].
         * @param origin The origin point to be used for texture space.
         * @param mapping The method by which the coordinates of the [clientTextureId] image will
         *   apply to the stroke.
         * @param wrapX The wrap mode along the horizontal texture axis.
         * @param wrapY The wrap mode along the vertical texture axis.
         * @param blendMode The method by which the texture layers up to this one (index <= i) are
         *   combined with the subsequent texture layer (index == i+1). For the last texture layer,
         *   this defines the method by which the texture layer is combined with the brush color
         *   (possibly after that color gets per-vertex adjustments).
         */
        public constructor(
            clientTextureId: String,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeX: Float,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeY: Float,
            offsetX: Float = 0f,
            offsetY: Float = 0f,
            @AngleRadiansFloat rotation: Float = 0F,
            @FloatRange(from = 0.0, to = 1.0) opacity: Float = 1f,
            @IntRange(from = 1, to = 1 shl 24) animationFrames: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) animationRows: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) animationColumns: Int = 1,
            sizeUnit: TextureSizeUnit = TextureSizeUnit.STROKE_COORDINATES,
            origin: TextureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
            mapping: TextureMapping = TextureMapping.TILING,
            wrapX: TextureWrap = TextureWrap.REPEAT,
            wrapY: TextureWrap = TextureWrap.REPEAT,
            blendMode: BlendMode = BlendMode.MODULATE,
        ) : this(
            TextureLayerNative.create(
                clientTextureId,
                sizeX,
                sizeY,
                offsetX,
                offsetY,
                rotation,
                opacity,
                animationFrames,
                animationRows,
                animationColumns,
                sizeUnit.value,
                origin.value,
                mapping.value,
                wrapX.value,
                wrapY.value,
                blendMode.value,
            )
        )

        public val clientTextureId: String = TextureLayerNative.getClientTextureId(nativePointer)

        // Caching the native accessors here even for primitive fields because these are accessed
        // mostly
        // in Kotlin.

        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public val sizeX: Float = TextureLayerNative.getSizeX(nativePointer)

        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public val sizeY: Float = TextureLayerNative.getSizeY(nativePointer)

        public val offsetX: Float = TextureLayerNative.getOffsetX(nativePointer)

        public val offsetY: Float = TextureLayerNative.getOffsetY(nativePointer)

        @AngleRadiansFloat
        public val rotation: Float = TextureLayerNative.getRotationInRadians(nativePointer)

        @FloatRange(from = 0.0, to = 1.0)
        public val opacity: Float = TextureLayerNative.getOpacity(nativePointer)

        @IntRange(from = 1, to = 1 shl 24)
        public val animationFrames: Int = TextureLayerNative.getAnimationFrames(nativePointer)

        @IntRange(from = 1, to = 1 shl 12)
        public val animationRows: Int = TextureLayerNative.getAnimationRows(nativePointer)

        @IntRange(from = 1, to = 1 shl 12)
        public val animationColumns: Int = TextureLayerNative.getAnimationColumns(nativePointer)

        public val sizeUnit: TextureSizeUnit = TextureLayerNative.getSizeUnit(nativePointer)

        public val origin: TextureOrigin = TextureLayerNative.getOrigin(nativePointer)

        public val mapping: TextureMapping = TextureLayerNative.getMapping(nativePointer)

        public val wrapX: TextureWrap = TextureLayerNative.getWrapX(nativePointer)

        public val wrapY: TextureWrap = TextureLayerNative.getWrapY(nativePointer)

        public val blendMode: BlendMode = TextureLayerNative.getBlendMode(nativePointer)

        init {
            require(animationFrames <= animationRows * animationColumns) {
                "$animationFrames frames cannot fit into a grid with $animationRows and " +
                    "$animationColumns (up to ${animationRows * animationColumns} frames)"
            }
        }

        /**
         * Creates a copy of `this` and allows named properties to be altered while keeping the rest
         * unchanged.
         */
        @JvmSynthetic
        public fun copy(
            clientTextureId: String = this.clientTextureId,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            sizeX: Float = this.sizeX,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            sizeY: Float = this.sizeY,
            offsetX: Float = this.offsetX,
            offsetY: Float = this.offsetY,
            @AngleRadiansFloat rotation: Float = this.rotation,
            @FloatRange(from = 0.0, to = 1.0) opacity: Float = this.opacity,
            @IntRange(from = 1, to = 1 shl 24) animationFrames: Int = this.animationFrames,
            @IntRange(from = 1, to = 1 shl 12) animationRows: Int = this.animationRows,
            @IntRange(from = 1, to = 1 shl 12) animationColumns: Int = this.animationColumns,
            sizeUnit: TextureSizeUnit = this.sizeUnit,
            origin: TextureOrigin = this.origin,
            mapping: TextureMapping = this.mapping,
            wrapX: TextureWrap = this.wrapX,
            wrapY: TextureWrap = this.wrapY,
            blendMode: BlendMode = this.blendMode,
        ): TextureLayer {
            if (
                clientTextureId == this.clientTextureId &&
                    sizeX == this.sizeX &&
                    sizeY == this.sizeY &&
                    offsetX == this.offsetX &&
                    offsetY == this.offsetY &&
                    rotation == this.rotation &&
                    opacity == this.opacity &&
                    animationFrames == this.animationFrames &&
                    animationRows == this.animationRows &&
                    animationColumns == this.animationColumns &&
                    sizeUnit == this.sizeUnit &&
                    origin == this.origin &&
                    mapping == this.mapping &&
                    wrapX == this.wrapX &&
                    wrapY == this.wrapY &&
                    blendMode == this.blendMode
            ) {
                return this
            }
            return TextureLayer(
                clientTextureId,
                sizeX,
                sizeY,
                offsetX,
                offsetY,
                rotation,
                opacity,
                animationFrames,
                animationRows,
                animationColumns,
                sizeUnit,
                origin,
                mapping,
                wrapX,
                wrapY,
                blendMode,
            )
        }

        /**
         * Returns a [Builder] with values set equivalent to `this`. Java developers, use the
         * returned builder to build a copy of a TextureLayer. Kotlin developers, see [copy] method.
         */
        public fun toBuilder(): Builder =
            Builder(
                clientTextureId = this.clientTextureId,
                sizeX = this.sizeX,
                sizeY = this.sizeY,
                offsetX = this.offsetX,
                offsetY = this.offsetY,
                rotation = this.rotation,
                opacity = this.opacity,
                animationFrames = this.animationFrames,
                animationRows = this.animationRows,
                animationColumns = this.animationColumns,
                sizeUnit = this.sizeUnit,
                origin = this.origin,
                mapping = this.mapping,
                wrapX = this.wrapX,
                wrapY = this.wrapY,
                blendMode = this.blendMode,
            )

        override fun equals(other: Any?): Boolean {
            if (other !is TextureLayer) return false
            return clientTextureId == other.clientTextureId &&
                sizeX == other.sizeX &&
                sizeY == other.sizeY &&
                offsetX == other.offsetX &&
                offsetY == other.offsetY &&
                rotation == other.rotation &&
                opacity == other.opacity &&
                animationFrames == other.animationFrames &&
                animationRows == other.animationRows &&
                animationColumns == other.animationColumns &&
                sizeUnit == other.sizeUnit &&
                origin == other.origin &&
                mapping == other.mapping &&
                wrapX == other.wrapX &&
                wrapY == other.wrapY &&
                blendMode == other.blendMode
        }

        override fun toString(): String =
            "BrushPaint.TextureLayer(clientTextureId=$clientTextureId, sizeX=$sizeX, " +
                "sizeY=$sizeY, offset=[$offsetX, $offsetY], rotation=$rotation, opacity=$opacity, " +
                "animationFrames=$animationFrames, animationRows=$animationRows, " +
                "animationColumns=$animationColumns, sizeUnit=$sizeUnit, origin=$origin, " +
                "mapping=$mapping, wrapX=$wrapX, wrapY=$wrapY, blendMode=$blendMode)"

        override fun hashCode(): Int {
            var result = clientTextureId.hashCode()
            result = 31 * result + sizeX.hashCode()
            result = 31 * result + sizeY.hashCode()
            result = 31 * result + offsetX.hashCode()
            result = 31 * result + offsetY.hashCode()
            result = 31 * result + rotation.hashCode()
            result = 31 * result + opacity.hashCode()
            result = 31 * result + animationFrames.hashCode()
            result = 31 * result + animationRows.hashCode()
            result = 31 * result + animationColumns.hashCode()
            result = 31 * result + sizeUnit.hashCode()
            result = 31 * result + origin.hashCode()
            result = 31 * result + mapping.hashCode()
            result = 31 * result + wrapX.hashCode()
            result = 31 * result + wrapY.hashCode()
            result = 31 * result + blendMode.hashCode()
            return result
        }

        /** Delete native TextureLayer memory. */
        // NOMUTANTS -- Not tested post garbage collection.
        protected fun finalize() {
            // TODO: b/423019041 - Investigate why this is failing in native code with
            // nativePointer=0
            if (nativePointer != 0L) {
                TextureLayerNative.free(nativePointer)
            }
        }

        /**
         * Builder for [TextureLayer].
         *
         * Construct from `textureLayer.toBuilder()` or `TextureLayer.builder()`.
         */
        @Suppress(
            "ScopeReceiverThis"
        ) // Builder pattern supported for Java clients, despite being an anti-pattern in Kotlin.
        public class Builder
        internal constructor(
            private var clientTextureId: String,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            private var sizeX: Float,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            private var sizeY: Float,
            private var offsetX: Float = 0f,
            private var offsetY: Float = 0f,
            @AngleRadiansFloat private var rotation: Float = 0F,
            @FloatRange(from = 0.0, to = 1.0) private var opacity: Float = 1f,
            @IntRange(from = 1, to = 1 shl 24) private var animationFrames: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) private var animationRows: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) private var animationColumns: Int = 1,
            private var sizeUnit: TextureSizeUnit = TextureSizeUnit.STROKE_COORDINATES,
            private var origin: TextureOrigin = TextureOrigin.STROKE_SPACE_ORIGIN,
            private var mapping: TextureMapping = TextureMapping.TILING,
            private var wrapX: TextureWrap = TextureWrap.REPEAT,
            private var wrapY: TextureWrap = TextureWrap.REPEAT,
            private var blendMode: BlendMode = BlendMode.MODULATE,
        ) {
            public fun setClientTextureId(clientTextureId: String): Builder = apply {
                this.clientTextureId = clientTextureId
            }

            public fun setSizeX(
                @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeX: Float
            ): Builder = apply { this.sizeX = sizeX }

            public fun setSizeY(
                @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeY: Float
            ): Builder = apply { this.sizeY = sizeY }

            public fun setOffsetX(offsetX: Float): Builder = apply { this.offsetX = offsetX }

            public fun setOffsetY(offsetY: Float): Builder = apply { this.offsetY = offsetY }

            public fun setRotation(@AngleRadiansFloat rotation: Float): Builder = apply {
                this.rotation = rotation
            }

            public fun setOpacity(@FloatRange(from = 0.0, to = 1.0) opacity: Float): Builder =
                apply {
                    this.opacity = opacity
                }

            public fun setAnimationFrames(
                @IntRange(from = 1, to = 1 shl 24) animationFrames: Int
            ): Builder = apply { this.animationFrames = animationFrames }

            public fun setAnimationRows(
                @IntRange(from = 1, to = 1 shl 12) animationRows: Int
            ): Builder = apply { this.animationRows = animationRows }

            public fun setAnimationColumns(
                @IntRange(from = 1, to = 1 shl 12) animationColumns: Int
            ): Builder = apply { this.animationColumns = animationColumns }

            public fun setSizeUnit(sizeUnit: TextureSizeUnit): Builder = apply {
                this.sizeUnit = sizeUnit
            }

            public fun setOrigin(origin: TextureOrigin): Builder = apply { this.origin = origin }

            public fun setMapping(mapping: TextureMapping): Builder = apply {
                this.mapping = mapping
            }

            public fun setWrapX(wrapX: TextureWrap): Builder = apply { this.wrapX = wrapX }

            public fun setWrapY(wrapY: TextureWrap): Builder = apply { this.wrapY = wrapY }

            public fun setBlendMode(blendMode: BlendMode): Builder = apply {
                this.blendMode = blendMode
            }

            public fun build(): TextureLayer =
                TextureLayer(
                    clientTextureId,
                    sizeX,
                    sizeY,
                    offsetX,
                    offsetY,
                    rotation,
                    opacity,
                    animationFrames,
                    animationRows,
                    animationColumns,
                    sizeUnit,
                    origin,
                    mapping,
                    wrapX,
                    wrapY,
                    blendMode,
                )
        }

        // To be extended by extension methods.
        public companion object {
            /**
             * Returns a [Builder] with the required fields set.
             *
             * @param clientTextureId The texture ID of the texture to be used for the texture
             *   layer.
             * @param sizeX The size of the texture in the x-direction.
             * @param sizeY The size of the texture in the y-direction.
             */
            @JvmStatic
            public fun builder(
                clientTextureId: String,
                @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeX: Float,
                @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeY: Float,
            ): Builder = Builder(clientTextureId, sizeX, sizeY)

            /**
             * Construct a [TextureLayer] from an unowned heap-allocated native pointer to a C++
             * `BrushPaint::TextureLayer`.
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public fun wrapNative(unownedNativePointer: Long): TextureLayer =
                TextureLayer(unownedNativePointer)
        }
    }

    // To be extended by extension methods.
    public companion object {
        /**
         * Construct a [BrushPaint] from an unowned heap-allocated native pointer to a C++
         * `BrushPaint`. Kotlin wrapper objects nested under the [BrushPaint] are initialized
         * similarly using their own [wrapNative] methods, passing those pointers to newly
         * copy-constructed heap-allocated objects. That avoids the need to call Kotlin constructors
         * for those objects from C++.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): BrushPaint =
            BrushPaint(
                unownedNativePointer,
                List(BrushPaintNative.getTextureLayerCount(unownedNativePointer)) { index ->
                    TextureLayer.wrapNative(
                        BrushPaintNative.newCopyOfTextureLayer(unownedNativePointer, index)
                    )
                },
            )
    }
}

/** Singleton wrapper around BrushPaint native JNI calls. */
@UsedByNative
private object BrushPaintNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative external fun create(textureLayerNativePointers: LongArray): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getTextureLayerCount(nativePointer: Long): Int

    /**
     * Returns a new, unowned native pointer to a copy of the texture layer at the given index on
     * the pointed-at native `BrushPaint`.
     */
    @UsedByNative external fun newCopyOfTextureLayer(nativePointer: Long, index: Int): Long
}

/** Singleton wrapper around BrushPaint.TextureLayer native JNI calls. */
@OptIn(ExperimentalInkCustomBrushApi::class)
@UsedByNative
private object TextureLayerNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun create(
        clientTextureId: String,
        sizeX: Float,
        sizeY: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float,
        opacity: Float,
        animationFrames: Int,
        animationRows: Int,
        animationColumns: Int,
        sizeUnit: Int,
        origin: Int,
        mapping: Int,
        wrapX: Int,
        wrapY: Int,
        blendMode: Int,
    ): Long

    @UsedByNative external fun getClientTextureId(nativePointer: Long): String

    @UsedByNative external fun getSizeX(nativePointer: Long): Float

    @UsedByNative external fun getSizeY(nativePointer: Long): Float

    @UsedByNative external fun getOffsetX(nativePointer: Long): Float

    @UsedByNative external fun getOffsetY(nativePointer: Long): Float

    @UsedByNative external fun getRotationInRadians(nativePointer: Long): Float

    @UsedByNative external fun getOpacity(nativePointer: Long): Float

    @UsedByNative external fun getAnimationFrames(nativePointer: Long): Int

    @UsedByNative external fun getAnimationRows(nativePointer: Long): Int

    @UsedByNative external fun getAnimationColumns(nativePointer: Long): Int

    fun getSizeUnit(nativePointer: Long): BrushPaint.TextureSizeUnit =
        BrushPaint.TextureSizeUnit(getSizeUnitInt(nativePointer))

    @UsedByNative external fun getSizeUnitInt(nativePointer: Long): Int

    fun getOrigin(nativePointer: Long): BrushPaint.TextureOrigin =
        BrushPaint.TextureOrigin(getOriginInt(nativePointer))

    @UsedByNative private external fun getOriginInt(nativePointer: Long): Int

    fun getMapping(nativePointer: Long): BrushPaint.TextureMapping =
        BrushPaint.TextureMapping(getMappingInt(nativePointer))

    @UsedByNative private external fun getMappingInt(nativePointer: Long): Int

    fun getWrapX(nativePointer: Long): BrushPaint.TextureWrap =
        BrushPaint.TextureWrap(getWrapXInt(nativePointer))

    @UsedByNative private external fun getWrapXInt(nativePointer: Long): Int

    fun getWrapY(nativePointer: Long): BrushPaint.TextureWrap =
        BrushPaint.TextureWrap(getWrapYInt(nativePointer))

    @UsedByNative private external fun getWrapYInt(nativePointer: Long): Int

    fun getBlendMode(nativePointer: Long): BrushPaint.BlendMode =
        BrushPaint.BlendMode(getBlendModeInt(nativePointer))

    @UsedByNative private external fun getBlendModeInt(nativePointer: Long): Int

    @UsedByNative external fun free(nativePointer: Long)
}
