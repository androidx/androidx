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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.strokes.StrokeInput
import java.util.WeakHashMap

/**
 * Helper class for obtaining [Paint] from [BrushPaint].
 *
 * @param paintFlags Used to set [Paint.flags] for all [Paint] objects it creates.
 * @param applyColorFilterToTexture If true, the [BrushPaint] and the provided color are used to
 *   configure [Paint.colorFilter] to apply a color to the paint's shader. This should generally be
 *   set when using an API that expects a color to be uniformly applied by the Paint, instead of
 *   providing per-vertex-modified colors to the draw call.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
internal class BrushPaintCache(
    val textureStore: TextureBitmapStore,
    val additionalPaintFlags: Int = 0,
    val applyColorFilterToTexture: Boolean = false,
) {

    /** Holds onto the [Paint] for each [BrushPaint] for efficiency. */
    private val paintCache = WeakHashMap<BrushPaint, PaintCacheData>()

    /** Used to construct and update a shader, holding on to data that's needed for later update. */
    private inner class ShaderHelper(
        private val textureLayers: List<BrushPaint.TextureLayer>,
        private val bitmaps: List<Bitmap?>,
        private val bitmapShaders: List<Shader?>,
    ) {
        private val scratchMatrix = Matrix()

        private val bitmapShaderLocalMatrices: List<Matrix?>? =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // `Shader.setLocalMatrix` saves the `Matrix` instance rather than copying its data
                // to an
                // internal instance before API 26, so allocate dedicated `Matrix` instances for
                // those
                // shaders to avoid accidentally clobbering data. After API 26, `scratchMatrix` can
                // be used
                // for all layers to save allocations.
                bitmapShaders.map { if (it == null) null else Matrix() }
            } else {
                null
            }

        init {
            require(
                bitmaps.size == textureLayers.size && bitmapShaders.size == textureLayers.size
            ) {
                "textureLayers, bitmaps, and bitmapShaders should be parallel lists."
            }
            for (i in 0 until textureLayers.size) {
                require(bitmapShaders[i] == null || bitmaps[i] != null) {
                    "bitmap[$i] should be non-null if bitmapShaders[$i] is non-null."
                }
            }
        }

        fun updateInternalToStrokeTransform(
            @FloatRange(from = 0.0) brushSize: Float,
            firstInput: StrokeInput,
            lastInput: StrokeInput,
            internalToStrokeTransform: Matrix?,
        ) {
            for (i in 0 until textureLayers.size) {
                val bitmapShader = bitmapShaders[i] ?: continue
                val textureLayer = textureLayers[i]
                val bitmap =
                    checkNotNull(bitmaps[i]) {
                        "bitmap[$i] should be non-null if bitmapShaders[$i] is non-null."
                    }
                val scratchShaderLocalMatrix =
                    if (bitmapShaderLocalMatrices != null) {
                        checkNotNull(bitmapShaderLocalMatrices[i]) {
                            "bitmapShaderLocalMatrices[$i] shouldbe non-null if bitmapShader[$i] is non-null."
                        }
                    } else {
                        scratchMatrix
                    }
                // The texture coordinates being drawn are in the mesh's "internal" coordinate space
                // (which
                // for legacy strokes may be different than the publicly facing stroke coordinate
                // space). However, [BitmapShader] assumes we're working with texel coordinates, so
                // we need
                // to compute the combined chain of transforms from that coordinate space to
                // "internal" mesh
                // space.
                val texelToInternalTransform =
                    scratchShaderLocalMatrix.also {
                        // At the end of this chain of transforms, we'll need to go from stroke
                        // space to
                        // "internal" mesh space. Start by computing that, then we'll work
                        // backwards.
                        //
                        // Compute (stroke -> internal) = (internal -> stroke)^-1
                        //
                        // Note that internalToStrokeTransform is nullable; if null, we treat it as
                        // an identity
                        // matrix, but skip the needless call to [invert].
                        it.reset()
                        internalToStrokeTransform?.invert(it)

                        // While we're in stroke space, shift the origin to the position specified
                        // by the
                        // [TextureLayer].
                        when (textureLayer.origin) {
                            BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN -> {}
                            BrushPaint.TextureOrigin.FIRST_STROKE_INPUT -> {
                                it.preTranslate(firstInput.x, firstInput.y)
                            }
                            BrushPaint.TextureOrigin.LAST_STROKE_INPUT -> {
                                it.preTranslate(lastInput.x, lastInput.y)
                            }
                        }

                        // To get to stroke space, we first need to scale from the coordinate space
                        // where
                        // distance is measured in the chosen SizeUnit for this particular texture
                        // layer.
                        //
                        // Compute (SizeUnit -> internal) = (stroke -> internal) * (SizeUnit ->
                        // stroke)
                        when (textureLayer.sizeUnit) {
                            BrushPaint.TextureSizeUnit.BRUSH_SIZE ->
                                it.preScale(brushSize, brushSize)
                            BrushPaint.TextureSizeUnit.STROKE_SIZE -> {
                                // TODO: b/336835642 - Implement BrushPaintCache support for
                                // TextureSizeUnit.STROKE_SIZE.
                            }
                            BrushPaint.TextureSizeUnit.STROKE_COORDINATES -> {
                                // Nothing to do, since stroke space and SizeUnit space are
                                // identical.
                            }
                        }

                        // To get to SizeUnit space, we first need to scale from the texture UV
                        // coordinate
                        // space; that is, the coordinate space where the texture image is a unit
                        // square.
                        //
                        // Compute (UV -> internal) = (SizeUnit -> internal) * (UV -> SizeUnit)
                        it.preScale(textureLayer.sizeX, textureLayer.sizeY)

                        // The texture offset is specified as fractions of the texture size; in
                        // other words, it
                        // should be applied within texture UV space.
                        it.preTranslate(textureLayer.offsetX, textureLayer.offsetY)

                        // To get to texture UV space, we first need to scale from the coordinate
                        // space where
                        // distance is measured in texels; that is, where each texel is a unit
                        // square.
                        //
                        // Compute (texel -> internal) = (UV -> internal) * (texel -> UV)
                        it.preScale(1f / bitmap.width, 1f / bitmap.height)
                    }
                // Do not use Matrix.isIdentity - it returns false for the identity matrix on
                // earlier API
                // levels.
                val localMatrix =
                    if (texelToInternalTransform == IDENTITY_MATRIX) {
                        null
                    } else {
                        texelToInternalTransform
                    }
                bitmapShader.setLocalMatrix(localMatrix)
            }
        }
    }

    private class ColorFilterHelper {
        private @ColorInt var colorFilterColor: Int = 0

        fun updateColorFilterColor(
            paint: Paint,
            brushPaint: BrushPaint,
            @ColorInt paintColor: Int
        ) {
            if (paint.colorFilter != null && colorFilterColor == paintColor) return
            val lastTextureLayer =
                requireNotNull(brushPaint.textureLayers.lastOrNull()) {
                    "Paint.colorFilter should only be used when Paint.shader is set, which should only " +
                        "happen when there is at least one item in BrushPaint.textureLayers."
                }
            // In [CanvasMeshRenderer], when we call [Canvas.drawMesh] with the last texture layer's
            // blend
            // mode, that method treats the mesh color as the DST, and the shader texture as the SRC
            // (which matches how we've specified the meaning of [BrushPaint.BlendMode]). Here, we
            // are
            // using a color filter to emulate that behavior for the sake of [CanvasPathRenderer],
            // but the
            // color filter treats [paintColor] as the SRC, and the path texture as the DST.  So we
            // need
            // to use [toReversePorterDuffMode] here so as to swap SRC and DST from what the
            // [BrushPaint.BlendMode] says.
            val colorBlendMode = lastTextureLayer.blendMode.toReversePorterDuffMode()
            paint.colorFilter = PorterDuffColorFilter(paintColor, colorBlendMode)
            colorFilterColor = paintColor
        }
    }

    private fun createCacheData(brushPaint: BrushPaint): PaintCacheData {
        val paint =
            Paint(additionalPaintFlags).apply {
                // This sets Paint.FILTER_BITMAP_FLAG for consistency. For Android versions <= O,
                // bilinear
                // sampling is always used on scaled bitmaps when hardware acceleration is available
                // and
                // the behavior depends on this flag otherwise. Starting at Android Q, this flag is
                // set by
                // default. So setting it results in consistent behavior for Android P and for <= O
                // when
                // hardware acceleration is not available.
                setFilterBitmap(true)
            }
        val textureLayers = brushPaint.textureLayers
        if (textureLayers.isEmpty()) {
            // Early exit for efficiency.
            return PaintCacheData(paint)
        }
        val bitmaps = textureLayers.map { textureStore.get(it.colorTextureUri) }
        val bitmapShaders =
            bitmaps.map { bitmap ->
                if (bitmap == null) return@map null
                BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }
        // Each layer is combined with the result of combining all of the previous layers, using the
        // immediately previous layer's blend mode. (Effectively, ComposeShader acts as the non-leaf
        // nodes in a binary tree; more like a linked-list in this case because the destination side
        // is
        // always a leaf.) No layers reduce to null, a single layer reduces to the single
        // BitmapShader.
        paint.shader =
            bitmapShaders.reduceIndexedOrNull<Shader?, Shader?> { i, acc, shader ->
                when {
                    // TextureLayers that fail to resolve to a texture Bitmap are ignored. This
                    // seems like
                    // clearer behavior than refusing to apply the whole texture, and a more gentle
                    // fallback
                    // than crashing. It also allows textures to be disabled with a
                    // TextureBitmapStore whose
                    // load method returns null.
                    acc == null -> shader
                    shader == null -> acc
                    // The constructor arguments are destination, source, blend mode.
                    else ->
                        ComposeShader(
                            shader,
                            acc,
                            textureLayers[i - 1].blendMode.toPorterDuffMode()
                        )
                }
            }
        return PaintCacheData(
            paint,
            // Only construct the ShaderHelper if we actually loaded some texture bitmaps and
            // generated
            // a shader.
            if (paint.shader != null) ShaderHelper(textureLayers, bitmaps, bitmapShaders) else null,
            if (applyColorFilterToTexture && paint.shader != null) ColorFilterHelper() else null,
        )
    }

    private fun PaintCacheData.update(
        brushPaint: BrushPaint,
        @ColorInt paintColor: Int,
        @FloatRange(from = 0.0) brushSize: Float,
        firstInput: StrokeInput,
        lastInput: StrokeInput,
        internalToStrokeTransform: Matrix?,
    ) {
        shaderHelper?.updateInternalToStrokeTransform(
            brushSize,
            firstInput,
            lastInput,
            internalToStrokeTransform,
        )
        if (colorFilterHelper != null) {
            colorFilterHelper.updateColorFilterColor(paint, brushPaint, paintColor)
            paint.color = Color.WHITE
        } else {
            paint.color = paintColor
        }
    }

    /**
     * Obtains a [Paint] for the [BrushPaint] from the cache, creating it if necessary and updating
     * it with the current [internalToStrokeTransform]. If [BrushPaint.TextureLayer.colorTextureUri]
     * can't be resolved to a bitmap for any layer, that layer is ignored.
     *
     * @param brushPaint Used to configure [Paint.shader].
     * @param paintColor Used to set [Paint.color].
     * @param brushSize Used for supporting [BrushPaint.TextureSizeUnit.BRUSH_SIZE].
     * @param firstInput Used for supporting [BrushPaint.TextureOrigin.FIRST_STROKE_INPUT].
     * @param lastInput Used for supporting [BrushPaint.TextureOrigin.LAST_STROKE_INPUT].
     * @param internalToStrokeTransform Used to update the local matrix of [Paint.shader] if
     *   applicable. Defaults to null, which is treated equivalently to the identity matrix.
     */
    fun obtain(
        brushPaint: BrushPaint,
        @ColorInt paintColor: Int,
        @FloatRange(from = 0.0) brushSize: Float,
        firstInput: StrokeInput,
        lastInput: StrokeInput,
        internalToStrokeTransform: Matrix? = null,
    ): Paint {
        val cached = paintCache.getOrPut(brushPaint) { createCacheData(brushPaint) }
        cached.update(
            brushPaint,
            paintColor,
            brushSize,
            firstInput,
            lastInput,
            internalToStrokeTransform,
        )
        return cached.paint
    }

    private class PaintCacheData(
        val paint: Paint,
        val shaderHelper: ShaderHelper? = null,
        val colorFilterHelper: ColorFilterHelper? = null,
    )

    private companion object {
        // Would be better to use the immutable [Matrix.IDENTITY_MATRIX], but that's in API version
        // 31.
        val IDENTITY_MATRIX = Matrix()
    }
}
