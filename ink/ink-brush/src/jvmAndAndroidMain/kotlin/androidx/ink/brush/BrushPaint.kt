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

import androidx.annotation.ColorInt
import androidx.annotation.ColorLong
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.collection.MutableIntObjectMap
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.toArgb
import androidx.ink.geometry.AngleDegreesFloat
import androidx.ink.geometry.MeshFormat
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.Suppress
import kotlin.jvm.JvmField

/**
 * Parameters that control stroke mesh rendering.
 *
 * The core of each paint consists of one or more texture layers. The output of each layer is
 * blended together in sequence, then the combined texture is blended with the output from the brush
 * color.
 * - Starting with the first [TextureLayer], the combined texture for layers 0 to i (source) is
 *   blended with layer i+1 (destination) using the blend mode for layer i.
 * - The final combined texture (source) is blended with the (possibly adjusted per-vertex) brush
 *   color (destination) according to the blend mode of the last texture layer.
 */
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushPaint
private constructor(
    /** A handle to the underlying native [BrushPaint] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    // The [textureLayers] val below is a defensive copy of this parameter.
    textureLayers: List<TextureLayer>,
    // The [colorFunctions] val below is a defensive copy of this parameter.
    colorFunctions: List<ColorFunction>,
    /** The rendering behavior to use for strokes that overlap themselves. */
    public val selfOverlap: SelfOverlap,
) {

    /** The textures to apply to the stroke. */
    public val textureLayers: List<TextureLayer> = unmodifiableList(textureLayers.toList())

    /**
     * Transformations to apply to the base brush color (in order) before drawing this coat of
     * paint. When this list is empty, the base brush color will be used unchanged.
     */
    public val colorFunctions: List<ColorFunction> = unmodifiableList(colorFunctions.toList())

    /**
     * Creates a [BrushPaint] with the given [textureLayers].
     *
     * @param textureLayers The textures to apply to the stroke.
     * @param colorFunctions The color functions to apply to the brush color.
     * @param selfOverlap The self-overlap behavior to apply to this coat of the stroke.
     */
    @JvmOverloads
    public constructor(
        textureLayers: List<TextureLayer> = emptyList(),
        colorFunctions: List<ColorFunction> = emptyList(),
        selfOverlap: SelfOverlap = SelfOverlap.ANY,
    ) : this(
        BrushPaintNative.create(
            textureLayers.map { it.nativePointer }.toLongArray(),
            colorFunctions.map { it.nativePointer }.toLongArray(),
            selfOverlap.value,
        ),
        textureLayers,
        colorFunctions,
        selfOverlap,
    )

    /** Uses this paint's color functions (if any) to transform the given brush color. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun applyColorFunctions(color: ComposeColor): ComposeColor {
        var transformedColor = color
        for (colorFunction in colorFunctions) {
            transformedColor = colorFunction.transformComposeColor(transformedColor)
        }
        return transformedColor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrushPaint) return false
        if (this === other) return true
        return textureLayers == other.textureLayers &&
            colorFunctions == other.colorFunctions &&
            selfOverlap == other.selfOverlap
    }

    override fun toString(): String =
        "BrushPaint(textureLayers=$textureLayers, colorFunctions=$colorFunctions, selfOverlap=$selfOverlap)"

    override fun hashCode(): Int {
        var result = textureLayers.hashCode()
        result = 31 * result + colorFunctions.hashCode()
        result = 31 * result + selfOverlap.hashCode()
        return result
    }

    /** Delete native BrushPaint memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushPaintNative.free(nativePointer)
    }

    /** Whether the given [MeshFormat] has sufficient attributes to render this [BrushPaint]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun isCompatibleWithMeshFormat(meshFormat: MeshFormat): Boolean {
        return BrushPaintNative.isCompatibleWithMeshFormat(nativePointer, meshFormat.nativePointer)
    }

    /** An explicit layer defined by an image. */
    @Suppress("NotCloseable") // Finalize is only used to free the native peer.
    public class TextureLayer private constructor(internal val nativePointer: Long) {

        /**
         * Creates a new [TextureLayer] with the specified parameters.
         *
         * Java callers should use the [Builder] class instead.
         *
         * @param clientTextureId A string identifier of an image that provides the color for a
         *   particular pixel for this layer. The coordinates within this image that will be used
         *   are determined by the other parameters.
         * @param sizeX The X size in [TextureLayer.SizeUnit] of the image specified by
         *   [clientTextureId].
         * @param sizeY The Y size in [TextureLayer.SizeUnit] of the image specified by
         *   [clientTextureId].
         * @param offsetX An offset into the texture, specified as fractions of the texture [sizeX].
         * @param offsetY An offset into the texture, specified as fractions of the texture [sizeY].
         * @param rotationDegrees Angle in degrees specifying the rotation of the texture. The
         *   rotation is carried out about the center of the texture's first repetition along both
         *   axes.
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
            @AngleDegreesFloat rotationDegrees: Float = 0F,
            sizeUnit: SizeUnit = SizeUnit.STROKE_COORDINATES,
            origin: Origin = Origin.STROKE_SPACE_ORIGIN,
            mapping: Mapping = Mapping.TILING,
            wrapX: Wrap = Wrap.REPEAT,
            wrapY: Wrap = Wrap.REPEAT,
            blendMode: BlendMode = BlendMode.MODULATE,
        ) : this(
            TextureLayerNative.create(
                clientTextureId = clientTextureId,
                sizeX = sizeX,
                sizeY = sizeY,
                offsetX = offsetX,
                offsetY = offsetY,
                rotationDegrees = rotationDegrees,
                animationFrames = 1,
                animationRows = 1,
                animationColumns = 1,
                animationDurationMillis = 1000L,
                sizeUnit = sizeUnit.value,
                origin = origin.value,
                mapping = mapping.value,
                wrapX = wrapX.value,
                wrapY = wrapY.value,
                blendMode = blendMode.value,
            )
        )

        /**
         * Creates a new [TextureLayer] with the specified parameters.
         *
         * Java callers should use the [Builder] class instead.
         *
         * @param clientTextureId A string identifier of an image that provides the color for a
         *   particular pixel for this layer. The coordinates within this image that will be used
         *   are determined by the other parameters.
         * @param sizeX The X size in [SizeUnit] of (one animation frame of) the image specified by
         *   [clientTextureId].
         * @param sizeY The Y size in [SizeUnit] of (one animation frame of) the image specified by
         *   [clientTextureId].
         * @param offsetX An offset into the texture, specified as fractions of the texture [sizeX].
         * @param offsetY An offset into the texture, specified as fractions of the texture [sizeY].
         * @param rotationDegrees Angle in degrees specifying the rotation of the texture. The
         *   rotation is carried out about the center of the texture's first repetition along both
         *   axes.
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
         * @param animationDurationMillis The length of time in milliseconds that it takes to loop
         *   through all of the [animationFrames] frames in the texture. This means that each frame
         *   will be displayed (on average) for [animationDurationMillis] / [animationFrames]
         *   milliseconds. Defaults to 1000 milliseconds, but ignored if [animationFrames] is 1 (its
         *   default value) because that indicates that animation is disabled.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        public constructor(
            clientTextureId: String,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeX: Float,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeY: Float,
            offsetX: Float = 0f,
            offsetY: Float = 0f,
            @AngleDegreesFloat rotationDegrees: Float = 0F,
            sizeUnit: SizeUnit = SizeUnit.STROKE_COORDINATES,
            origin: Origin = Origin.STROKE_SPACE_ORIGIN,
            mapping: Mapping = Mapping.TILING,
            wrapX: Wrap = Wrap.REPEAT,
            wrapY: Wrap = Wrap.REPEAT,
            blendMode: BlendMode = BlendMode.MODULATE,
            @IntRange(from = 1, to = 1 shl 24) animationFrames: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) animationRows: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) animationColumns: Int = 1,
            @IntRange(from = 1, to = 1 shl 24) animationDurationMillis: Long = 1000L,
        ) : this(
            TextureLayerNative.create(
                clientTextureId = clientTextureId,
                sizeX = sizeX,
                sizeY = sizeY,
                offsetX = offsetX,
                offsetY = offsetY,
                rotationDegrees = rotationDegrees,
                animationFrames = animationFrames,
                animationRows = animationRows,
                animationColumns = animationColumns,
                animationDurationMillis = animationDurationMillis,
                sizeUnit = sizeUnit.value,
                origin = origin.value,
                mapping = mapping.value,
                wrapX = wrapX.value,
                wrapY = wrapY.value,
                blendMode = blendMode.value,
            )
        )

        public val clientTextureId: String = TextureLayerNative.getClientTextureId(nativePointer)

        // Caching the native accessors here even for primitive fields because these are accessed
        // mostly
        // in Kotlin.

        /** The width of the texture, specified in `sizeUnit`s. */
        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public val sizeX: Float = TextureLayerNative.getSizeX(nativePointer)

        /** The height of the texture, specified in `sizeUnit`s. */
        @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
        public val sizeY: Float = TextureLayerNative.getSizeY(nativePointer)

        /** The horizontal offset for the texture, specified as a fraction of the texture width. */
        public val offsetX: Float = TextureLayerNative.getOffsetX(nativePointer)

        /** The vertical offset for the texture, specified as a fraction of the texture height. */
        public val offsetY: Float = TextureLayerNative.getOffsetY(nativePointer)

        /**
         * Angle specifying the rotation of the texture. The rotation is carried out about the
         * center of the texture's first repetition along both axes.
         */
        @AngleDegreesFloat
        public val rotationDegrees: Float = TextureLayerNative.getRotationDegrees(nativePointer)

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        @IntRange(from = 1, to = 1 shl 24)
        public val animationFrames: Int = TextureLayerNative.getAnimationFrames(nativePointer)

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        @IntRange(from = 1, to = 1 shl 12)
        public val animationRows: Int = TextureLayerNative.getAnimationRows(nativePointer)

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        @IntRange(from = 1, to = 1 shl 12)
        public val animationColumns: Int = TextureLayerNative.getAnimationColumns(nativePointer)

        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        @IntRange(from = 1, to = 1 shl 24)
        public val animationDurationMillis: Long =
            TextureLayerNative.getAnimationDurationMillis(nativePointer)

        /** The units in which this texture layer's width and height are measured. */
        public val sizeUnit: SizeUnit = TextureLayerNative.getSizeUnit(nativePointer)

        /** The origin that will be used for positioning this texture layer. */
        public val origin: Origin = TextureLayerNative.getOrigin(nativePointer)

        /** The mapping mode used for applying this texture layer. */
        public val mapping: Mapping = TextureLayerNative.getMapping(nativePointer)

        /** The horizontal wrapping mode for this texture layer. */
        public val wrapX: Wrap = TextureLayerNative.getWrapX(nativePointer)

        /** The vertical wrapping mode for this texture layer. */
        public val wrapY: Wrap = TextureLayerNative.getWrapY(nativePointer)

        /**
         * The rule by which the texture layers up to and including this one are combined with the
         * subsequent layer.
         *
         * I.e. `BrushPaint::texture_layers[index].blend_mode` will be used to combine "src", which
         * is the result of blending layers [0..index], with "dst", which is the layer at index + 1.
         * If index refers to the last texture layer, then the layer at "index + 1" is the brush
         * color layer.
         */
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
         *
         * Java callers should use [Builder] instead.
         */
        @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
        public fun copy(
            clientTextureId: String = this.clientTextureId,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            sizeX: Float = this.sizeX,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            sizeY: Float = this.sizeY,
            offsetX: Float = this.offsetX,
            offsetY: Float = this.offsetY,
            @AngleDegreesFloat rotationDegrees: Float = this.rotationDegrees,
            sizeUnit: SizeUnit = this.sizeUnit,
            origin: Origin = this.origin,
            mapping: Mapping = this.mapping,
            wrapX: Wrap = this.wrapX,
            wrapY: Wrap = this.wrapY,
            blendMode: BlendMode = this.blendMode,
        ): TextureLayer {
            if (
                clientTextureId == this.clientTextureId &&
                    sizeX == this.sizeX &&
                    sizeY == this.sizeY &&
                    offsetX == this.offsetX &&
                    offsetY == this.offsetY &&
                    rotationDegrees == this.rotationDegrees &&
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
                clientTextureId = clientTextureId,
                sizeX = sizeX,
                sizeY = sizeY,
                offsetX = offsetX,
                offsetY = offsetY,
                rotationDegrees = rotationDegrees,
                sizeUnit = sizeUnit,
                origin = origin,
                mapping = mapping,
                wrapX = wrapX,
                wrapY = wrapY,
                blendMode = blendMode,
                animationFrames = this.animationFrames,
                animationRows = this.animationRows,
                animationColumns = this.animationColumns,
                animationDurationMillis = this.animationDurationMillis,
            )
        }

        /**
         * Creates a copy of `this` and allows named properties to be altered while keeping the rest
         * unchanged.
         *
         * Java callers should use [Builder] instead.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
        @Suppress("MissingJvmstatic") // no @JvmOverloads; not intended for Java callers
        public fun copy(
            clientTextureId: String = this.clientTextureId,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            sizeX: Float = this.sizeX,
            @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false)
            sizeY: Float = this.sizeY,
            offsetX: Float = this.offsetX,
            offsetY: Float = this.offsetY,
            @AngleDegreesFloat rotationDegrees: Float = this.rotationDegrees,
            sizeUnit: SizeUnit = this.sizeUnit,
            origin: Origin = this.origin,
            mapping: Mapping = this.mapping,
            wrapX: Wrap = this.wrapX,
            wrapY: Wrap = this.wrapY,
            blendMode: BlendMode = this.blendMode,
            @IntRange(from = 1, to = 1 shl 24) animationFrames: Int = this.animationFrames,
            @IntRange(from = 1, to = 1 shl 12) animationRows: Int = this.animationRows,
            @IntRange(from = 1, to = 1 shl 12) animationColumns: Int = this.animationColumns,
            @IntRange(from = 1, to = 1 shl 24)
            animationDurationMillis: Long = this.animationDurationMillis,
        ): TextureLayer {
            if (
                clientTextureId == this.clientTextureId &&
                    sizeX == this.sizeX &&
                    sizeY == this.sizeY &&
                    offsetX == this.offsetX &&
                    offsetY == this.offsetY &&
                    rotationDegrees == this.rotationDegrees &&
                    sizeUnit == this.sizeUnit &&
                    origin == this.origin &&
                    mapping == this.mapping &&
                    wrapX == this.wrapX &&
                    wrapY == this.wrapY &&
                    blendMode == this.blendMode &&
                    animationFrames == this.animationFrames &&
                    animationRows == this.animationRows &&
                    animationColumns == this.animationColumns &&
                    animationDurationMillis == this.animationDurationMillis
            ) {
                return this
            }
            return TextureLayer(
                clientTextureId = clientTextureId,
                sizeX = sizeX,
                sizeY = sizeY,
                offsetX = offsetX,
                offsetY = offsetY,
                rotationDegrees = rotationDegrees,
                sizeUnit = sizeUnit,
                origin = origin,
                mapping = mapping,
                wrapX = wrapX,
                wrapY = wrapY,
                blendMode = blendMode,
                animationFrames = animationFrames,
                animationRows = animationRows,
                animationColumns = animationColumns,
                animationDurationMillis = animationDurationMillis,
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
                rotationDegrees = this.rotationDegrees,
                animationFrames = this.animationFrames,
                animationRows = this.animationRows,
                animationColumns = this.animationColumns,
                animationDurationMillis = this.animationDurationMillis,
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
                rotationDegrees == other.rotationDegrees &&
                animationFrames == other.animationFrames &&
                animationRows == other.animationRows &&
                animationColumns == other.animationColumns &&
                animationDurationMillis == other.animationDurationMillis &&
                sizeUnit == other.sizeUnit &&
                origin == other.origin &&
                mapping == other.mapping &&
                wrapX == other.wrapX &&
                wrapY == other.wrapY &&
                blendMode == other.blendMode
        }

        override fun toString(): String =
            "BrushPaint.TextureLayer(clientTextureId=$clientTextureId, sizeX=$sizeX, " +
                "sizeY=$sizeY, offset=[$offsetX, $offsetY], rotationDegrees=$rotationDegrees, " +
                "animationFrames=$animationFrames, animationRows=$animationRows, " +
                "animationColumns=$animationColumns, animationDurationMillis=$animationDurationMillis, " +
                "sizeUnit=$sizeUnit, origin=$origin, mapping=$mapping, wrapX=$wrapX, wrapY=$wrapY, " +
                "blendMode=$blendMode)"

        override fun hashCode(): Int {
            var result = clientTextureId.hashCode()
            result = 31 * result + sizeX.hashCode()
            result = 31 * result + sizeY.hashCode()
            result = 31 * result + offsetX.hashCode()
            result = 31 * result + offsetY.hashCode()
            result = 31 * result + rotationDegrees.hashCode()
            result = 31 * result + animationFrames.hashCode()
            result = 31 * result + animationRows.hashCode()
            result = 31 * result + animationColumns.hashCode()
            result = 31 * result + animationDurationMillis.hashCode()
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
            // Note that the instance becomes finalizable at the conclusion of the Object
            // constructor,
            // which in Kotlin is always before any non-default field initialization has been done
            // by a
            // derived class constructor.
            if (nativePointer == 0L) return
            TextureLayerNative.free(nativePointer)
        }

        /**
         * Builder for [TextureLayer].
         *
         * For Java developers, use `TextureLayer.Builder` to construct a [TextureLayer] with
         * default values, overriding only as needed. For example: `TextureLayer layer =
         * TextureLayer.builder().setClientTextureId(id).setSizeX(width).setSizeY(height).build();`
         */
        @Suppress(
            "ScopeReceiverThis"
        ) // Builder pattern supported for Java clients, despite being an anti-pattern in Kotlin.
        public class Builder
        internal constructor(
            private var clientTextureId: String? = null,
            private var sizeX: Float = -1f,
            private var sizeY: Float = -1f,
            private var offsetX: Float = 0f,
            private var offsetY: Float = 0f,
            @AngleDegreesFloat private var rotationDegrees: Float = 0F,
            @IntRange(from = 1, to = 1 shl 24) private var animationFrames: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) private var animationRows: Int = 1,
            @IntRange(from = 1, to = 1 shl 12) private var animationColumns: Int = 1,
            @IntRange(from = 1, to = 1 shl 24) private var animationDurationMillis: Long = 1000,
            private var sizeUnit: SizeUnit = SizeUnit.STROKE_COORDINATES,
            private var origin: Origin = Origin.STROKE_SPACE_ORIGIN,
            private var mapping: Mapping = Mapping.TILING,
            private var wrapX: Wrap = Wrap.REPEAT,
            private var wrapY: Wrap = Wrap.REPEAT,
            private var blendMode: BlendMode = BlendMode.MODULATE,
        ) {
            /** Sets the client texture ID for this texture layer. */
            public fun setClientTextureId(clientTextureId: String): Builder = apply {
                this.clientTextureId = clientTextureId
            }

            /**
             * Sets the width of this texture layer, measured in the units specified by `sizeUnit`.
             */
            public fun setSizeX(
                @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeX: Float
            ): Builder = apply { this.sizeX = sizeX }

            /**
             * Sets the height of this texture layer, measured in the units specified by `sizeUnit`.
             */
            public fun setSizeY(
                @FloatRange(from = 0.0, fromInclusive = false, toInclusive = false) sizeY: Float
            ): Builder = apply { this.sizeY = sizeY }

            /**
             * Sets the horizontal offset of this texture layer, expressed as a fraction of the
             * texture width.
             */
            public fun setOffsetX(offsetX: Float): Builder = apply { this.offsetX = offsetX }

            /**
             * Sets the vertical offset of this texture layer, expressed as a fraction of the
             * texture height.
             */
            public fun setOffsetY(offsetY: Float): Builder = apply { this.offsetY = offsetY }

            /** Sets the rotation angle of this texture layer. */
            public fun setRotationDegrees(@AngleDegreesFloat degrees: Float): Builder = apply {
                rotationDegrees = degrees
            }

            /** Sets the number of animation frames in this texture layer. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
            public fun setAnimationFrames(
                @IntRange(from = 1, to = 1 shl 24) animationFrames: Int
            ): Builder = apply { this.animationFrames = animationFrames }

            /** Sets the number of animation rows in this texture layer. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
            public fun setAnimationRows(
                @IntRange(from = 1, to = 1 shl 12) animationRows: Int
            ): Builder = apply { this.animationRows = animationRows }

            /** Sets the number of animation columns in this texture layer. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
            public fun setAnimationColumns(
                @IntRange(from = 1, to = 1 shl 12) animationColumns: Int
            ): Builder = apply { this.animationColumns = animationColumns }

            /** Sets the duration of the animation for this texture layer. */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
            public fun setAnimationDurationMillis(
                @IntRange(from = 1, to = 1 shl 24) animationDurationMillis: Long
            ): Builder = apply { this.animationDurationMillis = animationDurationMillis }

            /** Sets the units in which this texture layer's width and height are measured. */
            public fun setSizeUnit(sizeUnit: SizeUnit): Builder = apply { this.sizeUnit = sizeUnit }

            /** Sets the origin that should be used for positioning this texture layer. */
            public fun setOrigin(origin: Origin): Builder = apply { this.origin = origin }

            /** Sets the mapping mode used for applying this texture layer. */
            public fun setMapping(mapping: Mapping): Builder = apply { this.mapping = mapping }

            /** Sets the horizontal wrapping mode for this texture layer. */
            public fun setWrapX(wrapX: Wrap): Builder = apply { this.wrapX = wrapX }

            /** Sets the vertical wrapping mode for this texture layer. */
            public fun setWrapY(wrapY: Wrap): Builder = apply { this.wrapY = wrapY }

            /**
             * Sets the blend mode used for blending this and all previous texture layers with the
             * next one.
             */
            public fun setBlendMode(blendMode: BlendMode): Builder = apply {
                this.blendMode = blendMode
            }

            /**
             * Constructs a [TextureLayer] from this [Builder].
             *
             * @throws IllegalStateException if `clientTextureId`, `sizeX`, and/or `sizeY` were
             *   never set
             */
            @Suppress("Range") // we check() before passing floats
            public fun build(): TextureLayer {
                val clientTextureId =
                    checkNotNull(this.clientTextureId) {
                        "must set clientTextureId before calling build()"
                    }
                check(sizeX > 0f) { "must set sizeX before calling build()" }
                check(sizeY > 0f) { "must set sizeY before calling build()" }
                return TextureLayer(
                    clientTextureId = clientTextureId,
                    sizeX = sizeX,
                    sizeY = sizeY,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    rotationDegrees = rotationDegrees,
                    animationFrames = animationFrames,
                    animationRows = animationRows,
                    animationColumns = animationColumns,
                    animationDurationMillis = animationDurationMillis,
                    sizeUnit = sizeUnit,
                    origin = origin,
                    mapping = mapping,
                    wrapX = wrapX,
                    wrapY = wrapY,
                    blendMode = blendMode,
                )
            }
        }

        // To be extended by extension methods.
        public companion object {
            /** Returns a new [TextureLayer.Builder]. */
            @JvmStatic public fun builder(): Builder = Builder()

            /**
             * Construct a [TextureLayer] from an unowned heap-allocated native pointer to a C++
             * `BrushPaint::TextureLayer`.
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public fun wrapNative(unownedNativePointer: Long): TextureLayer =
                TextureLayer(unownedNativePointer)
        }

        /** Specification of how the texture should apply to the stroke. */
        public class Mapping
        private constructor(
            @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
            public val value: Int,
            private val name: String,
        ) {

            init {
                check(value !in VALUE_TO_INSTANCE) { "Duplicate Mapping value: $value." }
                VALUE_TO_INSTANCE[value] = this
            }

            override fun toString(): String = "TextureLayer.Mapping.$name"

            public companion object {
                private val VALUE_TO_INSTANCE = MutableIntObjectMap<Mapping>()

                internal fun fromInt(value: Int): Mapping =
                    checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Mapping value: $value" }

                /**
                 * The texture will repeat according to a 2D affine transformation of vertex
                 * positions. Each copy of the texture will have the same size and shape, modulo
                 * reflections.
                 */
                // This mode does not support texture animations, so it ignores the
                // `animationFrames`,
                // `animationRows`, `animationColumns`, and `animationDuration` fields.
                @JvmField public val TILING: Mapping = Mapping(0, "TILING")
                /**
                 * This mode is intended for use with particle brush coats (i.e. with a brush tip
                 * with a nonzero particle gap). A copy of the texture will be "stamped" onto each
                 * particle of the stroke, scaled or rotated appropriately to cover the whole
                 * particle.
                 *
                 * Since the texture is always scaled to the size of each particle and positioned
                 * atop each one, this mode ignores the `origin`, `sizeUnit`, `wrapX`, `wrapY`,
                 * `sizeX`, and `sizeY` fields.
                 */
                @JvmField public val STAMPING: Mapping = Mapping(1, "STAMPING")
            }
        }

        /** Specification of the origin point to use for the texture. */
        public class Origin private constructor(internal val value: Int, private val name: String) {
            init {
                check(value !in VALUE_TO_INSTANCE) { "Duplicate Origin value: $value." }
                VALUE_TO_INSTANCE[value] = this
            }

            override fun toString(): String = "TextureLayer.Origin.$name"

            public companion object {
                private val VALUE_TO_INSTANCE = MutableIntObjectMap<Origin>()

                internal fun fromInt(value: Int): Origin =
                    checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Origin value: $value" }

                /**
                 * The texture origin is the origin of stroke space, however that happens to be
                 * defined for a given stroke.
                 */
                @JvmField public val STROKE_SPACE_ORIGIN: Origin = Origin(0, "STROKE_SPACE_ORIGIN")
                /** The texture origin is the first input position for the stroke. */
                @JvmField public val FIRST_STROKE_INPUT: Origin = Origin(1, "FIRST_STROKE_INPUT")
                /**
                 * The texture origin is the last input position (including predicted inputs) for
                 * the stroke. Note that this means that the texture origin for an in-progress
                 * stroke will move as more inputs are added.
                 */
                @JvmField public val LAST_STROKE_INPUT: Origin = Origin(2, "LAST_STROKE_INPUT")
            }
        }

        /** Units for specifying [TextureLayer.sizeX] and [TextureLayer.sizeY]. */
        public class SizeUnit
        private constructor(internal val value: Int, private val name: String) {
            init {
                check(value !in VALUE_TO_INSTANCE) { "Duplicate SizeUnit value: $value." }
                VALUE_TO_INSTANCE[value] = this
            }

            override fun toString(): String = "TextureLayer.SizeUnit.$name"

            public companion object {
                private val VALUE_TO_INSTANCE = MutableIntObjectMap<SizeUnit>()

                internal fun fromInt(value: Int): SizeUnit =
                    checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid SizeUnit value: $value" }

                /** As multiples of brush size. */
                @JvmField public val BRUSH_SIZE: SizeUnit = SizeUnit(0, "BRUSH_SIZE")
                /** In the same units as the stroke's input positions and stored geometry. */
                @JvmField
                public val STROKE_COORDINATES: SizeUnit = SizeUnit(1, "STROKE_COORDINATES")
            }
        }

        /** Wrap modes for specifying [TextureLayer.wrapX] and [TextureLayer.wrapY]. */
        public class Wrap private constructor(internal val value: Int, private val name: String) {
            init {
                check(value !in VALUE_TO_INSTANCE) { "Duplicate Wrap value: $value." }
                VALUE_TO_INSTANCE[value] = this
            }

            override fun toString(): String = "TextureLayer.Wrap.$name"

            public companion object {
                private val VALUE_TO_INSTANCE = MutableIntObjectMap<Wrap>()

                internal fun fromInt(value: Int): Wrap =
                    checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid Wrap value: $value" }

                /** Repeats texture image horizontally/vertically. */
                @JvmField public val REPEAT: Wrap = Wrap(0, "REPEAT")
                /**
                 * Repeats texture image horizontally/vertically, alternating mirror images so that
                 * adjacent edges always match.
                 */
                @JvmField public val MIRROR: Wrap = Wrap(1, "MIRROR")
                /**
                 * Points outside of the texture have the color of the nearest texture edge point.
                 * This mode is typically most useful when the edge pixels of the texture image are
                 * all the same, e.g. either transparent or a single solid color.
                 */
                @JvmField public val CLAMP: Wrap = Wrap(2, "CLAMP")
            }
        }

        /**
         * The method by which the combined texture layers (index <= i) are blended with the next
         * layer. The blend mode on the final layer controls how the combined texture is blended
         * with the brush color, and should typically be a mode whose output alpha is proportional
         * to the destination alpha, so that it can be adjusted by anti-aliasing.
         */
        public class BlendMode
        private constructor(internal val value: Int, private val name: String) {
            init {
                check(value !in VALUE_TO_INSTANCE) { "Duplicate BlendMode value: $value." }
                VALUE_TO_INSTANCE[value] = this
            }

            override fun toString(): String = "TextureLayer.BlendMode.$name"

            public companion object {
                private val VALUE_TO_INSTANCE = MutableIntObjectMap<BlendMode>()

                internal fun fromInt(value: Int): BlendMode =
                    checkNotNull(VALUE_TO_INSTANCE.get(value)) { "Invalid BlendMode value: $value" }

                /**
                 * Source and destination are component-wise multiplied, including opacity.
                 *
                 * ```
                 * Alpha = Alpha_src * Alpha_dst
                 * Color = Color_src * Color_dst
                 * ```
                 */
                @JvmField public val MODULATE: BlendMode = BlendMode(0, "MODULATE")
                /**
                 * Keeps destination pixels that cover source pixels. Discards remaining source and
                 * destination pixels.
                 *
                 * ```
                 * Alpha = Alpha_src * Alpha_dst
                 * Color = Alpha_src * Color_dst
                 * ```
                 */
                @JvmField public val DST_IN: BlendMode = BlendMode(1, "DST_IN")
                /**
                 * Keeps the destination pixels not covered by source pixels. Discards destination
                 * pixels that are covered by source pixels and all source pixels.
                 *
                 * ```
                 * Alpha = (1 - Alpha_src) * Alpha_dst
                 * Color = (1 - Alpha_src) * Color_dst
                 * ```
                 */
                @JvmField public val DST_OUT: BlendMode = BlendMode(2, "DST_OUT")
                /**
                 * Discards source pixels that do not cover destination pixels. Draws remaining
                 * pixels over destination pixels.
                 *
                 * ```
                 * Alpha = Alpha_dst
                 * Color = Alpha_dst * Color_src + (1 - Alpha_src) * Color_dst
                 * ```
                 */
                @JvmField public val SRC_ATOP: BlendMode = BlendMode(3, "SRC_ATOP")
                /**
                 * Keeps the source pixels that cover destination pixels. Discards remaining source
                 * and destination pixels.
                 *
                 * ```
                 * Alpha = Alpha_src * Alpha_dst
                 * Color = Color_src * Alpha_dst
                 * ```
                 */
                @JvmField public val SRC_IN: BlendMode = BlendMode(4, "SRC_IN")

                /*
                 * The following modes can't be used for the last TextureLayer, which defines the mode for
                 * blending the combined texture with the (possibly adjusted per-vertex) brush color. That
                 * blend mode needs the output Alpha to be a multiple of Alpha_dst so that per-vertex
                 * adjustment for anti-aliasing is preserved correctly.
                 */

                /**
                 * The source pixels are drawn over the destination pixels.
                 *
                 * ```
                 * Alpha = Alpha_src + (1 - Alpha_src) * Alpha_dst
                 * Color = Color_src + (1 - Alpha_src) * Color_dst
                 * ```
                 *
                 * This mode shouldn't normally be used for the final [TextureLayer], since its
                 * output alpha is not proportional to the destination alpha (so it wouldn't
                 * preserve alpha adjustments from anti-aliasing).
                 */
                @JvmField public val SRC_OVER: BlendMode = BlendMode(5, "SRC_OVER")
                /**
                 * The source pixels are drawn behind the destination pixels.
                 *
                 * ```
                 * Alpha = Alpha_dst + (1 - Alpha_dst) * Alpha_src
                 * Color = Color_dst + (1 - Alpha_dst) * Color_src
                 * ```
                 *
                 * This mode shouldn't normally be used for the final [TextureLayer], since its
                 * output alpha is not proportional to the destination alpha (so it wouldn't
                 * preserve alpha adjustments from anti-aliasing).
                 */
                @JvmField public val DST_OVER: BlendMode = BlendMode(6, "DST_OVER")
                /**
                 * Keeps the source pixels and discards the destination pixels.
                 *
                 * ```
                 * Alpha = Alpha_src
                 * Color = Color_src
                 * ```
                 *
                 * This mode shouldn't normally be used for the final [TextureLayer], since its
                 * output alpha is not proportional to the destination alpha (so it wouldn't
                 * preserve alpha adjustments from anti-aliasing).
                 */
                @JvmField public val SRC: BlendMode = BlendMode(7, "SRC")
                /**
                 * Keeps the destination pixels and discards the source pixels.
                 *
                 * ```
                 * Alpha = Alpha_dst
                 * Color = Color_dst
                 * ```
                 *
                 * This mode is unlikely to be useful, since it effectively causes the renderer to
                 * just ignore this [TextureLayer] and all layers before it, but it is included for
                 * completeness.
                 */
                @JvmField public val DST: BlendMode = BlendMode(8, "DST")
                /**
                 * Keeps the source pixels that do not cover destination pixels. Discards
                 * destination pixels and all source pixels that cover destination pixels.
                 *
                 * ```
                 * Alpha = (1 - Alpha_dst) * Alpha_src
                 * Color = (1 - Alpha_dst) * Color_src
                 * ```
                 *
                 * This mode shouldn't normally be used for the final [TextureLayer], since its
                 * output alpha is not proportional to the destination alpha (so it wouldn't
                 * preserve alpha adjustments from anti-aliasing).
                 */
                @JvmField public val SRC_OUT: BlendMode = BlendMode(9, "SRC_OUT")
                /**
                 * Discards destination pixels that aren't covered by source pixels. Remaining
                 * destination pixels are drawn over source pixels.
                 *
                 * ```
                 * Alpha = Alpha_src
                 * Color = Alpha_src * Color_dst + (1 - Alpha_dst) * Color_src
                 * ```
                 *
                 * This mode shouldn't normally be used for the final [TextureLayer], since its
                 * output alpha is not proportional to the destination alpha (so it wouldn't
                 * preserve alpha adjustments from anti-aliasing).
                 */
                @JvmField public val DST_ATOP: BlendMode = BlendMode(10, "DST_ATOP")
                /**
                 * Discards source and destination pixels that intersect; keeps source and
                 * destination pixels that do not intersect.
                 *
                 * ```
                 * Alpha = (1 - Alpha_dst) * Alpha_src + (1 - Alpha_src) * Alpha_dst
                 * Color = (1 - Alpha_dst) * Color_src + (1 - Alpha_src) * Color_dst
                 * ```
                 *
                 * This mode shouldn't normally be used for the final [TextureLayer], since its
                 * output alpha is not proportional to the destination alpha (so it wouldn't
                 * preserve alpha adjustments from anti-aliasing).
                 */
                @JvmField public val XOR: BlendMode = BlendMode(11, "XOR")
            }
        }
    }

    /** A [ColorFunction] defines a mapping over colors. */
    // NotCloseable: Finalize is only used to free the native peer.
    @Suppress("NotCloseable")
    public abstract class ColorFunction private constructor(internal val nativePointer: Long) {

        /** Transforms the input color into a new color. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public abstract fun transformComposeColor(color: ComposeColor): ComposeColor

        /** Transforms the input color into a new color. */
        @ColorInt
        public fun transformColorIntArgb(@ColorInt colorIntArgb: Int): Int =
            transformComposeColor(ComposeColor(colorIntArgb)).toArgb()

        // NOMUTANTS -- Not tested post garbage collection.
        protected fun finalize() {
            // Note that the instance becomes finalizable at the conclusion of the Object
            // constructor,
            // which
            // in Kotlin is always before any non-default field initialization has been done by a
            // derived
            // class constructor.
            if (nativePointer == 0L) return
            ColorFunctionNative.free(nativePointer)
        }

        public companion object {
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public fun wrapNative(unownedNativePointer: Long): ColorFunction =
                when (ColorFunctionNative.getParametersType(unownedNativePointer)) {
                    0 -> OpacityMultiplier(unownedNativePointer)
                    1 -> ReplaceColor(unownedNativePointer)
                    else -> throw IllegalArgumentException("Invalid color function type")
                }
        }

        /** A [ColorFunction] that scales the color opacity by a specified multiplier. */
        public class OpacityMultiplier internal constructor(nativePointer: Long) :
            ColorFunction(nativePointer) {

            /** Constructs a color function that applies the specified opacity multiplier. */
            public constructor(
                @FloatRange(from = 0.0) multiplier: Float
            ) : this(ColorFunctionNative.createOpacityMultiplier(multiplier))

            /** The opacity multiplier to apply. */
            @get:FloatRange(from = 0.0)
            public val multiplier: Float
                get() = ColorFunctionNative.getOpacityMultiplier(nativePointer)

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            override fun transformComposeColor(color: ComposeColor): ComposeColor =
                color.copy(alpha = color.alpha * multiplier)

            override fun equals(other: Any?): Boolean {
                if (other == null || other !is OpacityMultiplier) {
                    return false
                }
                return multiplier == other.multiplier
            }

            override fun hashCode(): Int = multiplier.hashCode()

            override fun toString(): String = "ColorFunction.OpacityMultiplier($multiplier)"

            // Declared to make extension functions available.
            public companion object
        }

        /**
         * A [ColorFunction] that ignores the input color and replaces it with the specified color.
         */
        public class ReplaceColor internal constructor(nativePointer: Long) :
            ColorFunction(nativePointer) {

            @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @Suppress("HiddenTypeParameter") // Internal API.
            public val internalColor: ComposeColor =
                // Caching this because the native call is slow. Still doing the round-trip on
                // construction
                // to ensure this is exercised by tests and that deserialized color functions are
                // consistent
                // with newly constructed color functions.
                ComposeColor(ColorFunctionNative.computeReplaceColorLong(nativePointer).toULong())

            /** The color that will replace the input color, as a `@ColorLong`. */
            public val colorLong: Long
                @ColorLong get(): Long = internalColor.value.toLong()

            /** The color that will replace the input color, as a `@ColorInt`. */
            public val colorIntArgb: Int
                @ColorInt get(): Int = internalColor.toArgb()

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            override fun transformComposeColor(color: ComposeColor): ComposeColor =
                this.internalColor

            override fun equals(other: Any?): Boolean {
                if (other == null || other !is ReplaceColor) {
                    return false
                }
                return internalColor.equals(other.internalColor)
            }

            override fun hashCode(): Int = internalColor.hashCode()

            override fun toString(): String = "ColorFunction.ReplaceColor($internalColor)"

            public companion object {
                /**
                 * Returns a color function that will replace its input color with the given color.
                 */
                @JvmStatic
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                public fun withComposeColor(color: ComposeColor): ReplaceColor =
                    ReplaceColor(
                        color.toColorInInkSupportedColorSpace().let { convertedColor ->
                            ColorFunctionNative.createReplaceColor(
                                convertedColor.red,
                                convertedColor.green,
                                convertedColor.blue,
                                convertedColor.alpha,
                                convertedColor.colorSpace.toInkColorSpaceId(),
                            )
                        }
                    )

                /**
                 * Returns a color function that will replace its input color with the given color.
                 */
                @JvmStatic
                public fun withColorLong(@ColorLong colorLong: Long): ReplaceColor =
                    ReplaceColor.withComposeColor(ComposeColor(colorLong.toULong()))

                /**
                 * Returns a color function that will replace its input color with the given color.
                 */
                @JvmStatic
                public fun withColorIntArgb(@ColorInt colorIntArgb: Int): ReplaceColor =
                    ReplaceColor.withComposeColor(ComposeColor(colorIntArgb))
            }
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
                List(BrushPaintNative.getColorFunctionCount(unownedNativePointer)) { index ->
                    ColorFunction.wrapNative(
                        BrushPaintNative.newCopyOfColorFunction(unownedNativePointer, index)
                    )
                },
                BrushPaintNative.getSelfOverlap(unownedNativePointer),
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
    @UsedByNative
    external fun create(
        textureLayerNativePointers: LongArray,
        colorFunctionNativePointers: LongArray,
        selfOverlapInt: Int,
    ): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getTextureLayerCount(nativePointer: Long): Int

    /**
     * Returns a new, unowned native pointer to a copy of the texture layer at the given index on
     * the pointed-at native `BrushPaint`.
     */
    @UsedByNative external fun newCopyOfTextureLayer(nativePointer: Long, index: Int): Long

    @UsedByNative external fun getColorFunctionCount(nativePointer: Long): Int

    /**
     * Returns a new, unowned native pointer to a copy of the color function at the given index on
     * the pointed-at native `BrushPaint`.
     */
    @UsedByNative external fun newCopyOfColorFunction(nativePointer: Long, index: Int): Long

    fun getSelfOverlap(nativePointer: Long) = SelfOverlap.fromInt(getSelfOverlapInt(nativePointer))

    @UsedByNative external fun getSelfOverlapInt(nativePointer: Long): Int

    @UsedByNative
    external fun isCompatibleWithMeshFormat(
        nativePointer: Long,
        meshFormatNativePointer: Long,
    ): Boolean
}

/** Singleton wrapper around BrushPaint.TextureLayer native JNI calls. */
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
        @AngleDegreesFloat rotationDegrees: Float,
        animationFrames: Int,
        animationRows: Int,
        animationColumns: Int,
        animationDurationMillis: Long,
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

    @AngleDegreesFloat @UsedByNative external fun getRotationDegrees(nativePointer: Long): Float

    @UsedByNative external fun getAnimationFrames(nativePointer: Long): Int

    @UsedByNative external fun getAnimationRows(nativePointer: Long): Int

    @UsedByNative external fun getAnimationColumns(nativePointer: Long): Int

    @UsedByNative external fun getAnimationDurationMillis(nativePointer: Long): Long

    fun getSizeUnit(nativePointer: Long): BrushPaint.TextureLayer.SizeUnit =
        BrushPaint.TextureLayer.SizeUnit.fromInt(getSizeUnitInt(nativePointer))

    @UsedByNative external fun getSizeUnitInt(nativePointer: Long): Int

    fun getOrigin(nativePointer: Long): BrushPaint.TextureLayer.Origin =
        BrushPaint.TextureLayer.Origin.fromInt(getOriginInt(nativePointer))

    @UsedByNative private external fun getOriginInt(nativePointer: Long): Int

    fun getMapping(nativePointer: Long): BrushPaint.TextureLayer.Mapping =
        BrushPaint.TextureLayer.Mapping.fromInt(getMappingInt(nativePointer))

    @UsedByNative private external fun getMappingInt(nativePointer: Long): Int

    fun getWrapX(nativePointer: Long): BrushPaint.TextureLayer.Wrap =
        BrushPaint.TextureLayer.Wrap.fromInt(getWrapXInt(nativePointer))

    @UsedByNative private external fun getWrapXInt(nativePointer: Long): Int

    fun getWrapY(nativePointer: Long): BrushPaint.TextureLayer.Wrap =
        BrushPaint.TextureLayer.Wrap.fromInt(getWrapYInt(nativePointer))

    @UsedByNative private external fun getWrapYInt(nativePointer: Long): Int

    fun getBlendMode(nativePointer: Long): BrushPaint.TextureLayer.BlendMode =
        BrushPaint.TextureLayer.BlendMode.fromInt(getBlendModeInt(nativePointer))

    @UsedByNative private external fun getBlendModeInt(nativePointer: Long): Int

    @UsedByNative external fun free(nativePointer: Long)
}

@UsedByNative
private object ColorFunctionNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createOpacityMultiplier(multiplier: Float): Long

    @UsedByNative
    external fun createReplaceColor(
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float,
        colorSpace: Int,
    ): Long

    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getParametersType(nativePointer: Long): Int

    @UsedByNative external fun getOpacityMultiplier(nativePointer: Long): Float

    @UsedByNative external fun computeReplaceColorLong(nativePointer: Long): Long
}
