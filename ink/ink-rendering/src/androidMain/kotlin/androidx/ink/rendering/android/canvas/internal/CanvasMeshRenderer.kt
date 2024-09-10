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

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorSpace as AndroidColorSpace
import android.graphics.Matrix
import android.graphics.Mesh as AndroidMesh
import android.graphics.MeshSpecification
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.Size
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableObjectLongMap
import androidx.core.os.BuildCompat
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.colorspace.ColorSpaces as ComposeColorSpaces
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.Mesh as InkMesh
import androidx.ink.geometry.MeshAttributeUnpackingParams
import androidx.ink.geometry.MeshFormat
import androidx.ink.geometry.populateMatrix
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import java.util.WeakHashMap

/**
 * Renders Ink objects using [Canvas.drawMesh]. This is the most fully-featured and performant
 * [Canvas] Ink renderer.
 *
 * This is not thread safe, so if it must be used from multiple threads, the caller is responsible
 * for synchronizing access. If it is being used in two very different contexts where there are
 * unlikely to be cached mesh data in common, the easiest solution to thread safety is to have two
 * different instances of this object.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class CanvasMeshRenderer(
    textureStore: TextureBitmapStore = TextureBitmapStore { null },
    /** Monotonic time with a non-epoch zero time. */
    private val getDurationTimeMillis: () -> Long = SystemClock::elapsedRealtime,
) : CanvasStrokeRenderer {

    /** Caches [Paint] objects so these can be reused between strokes with the same [BrushPaint]. */
    private val paintCache = BrushPaintCache(textureStore)

    /**
     * Caches [android.graphics.Mesh] instances so that they can be reused between calls to
     * [Canvas.drawMesh], greatly improving performance.
     *
     * On Android U, a bug in [android.graphics.Mesh] uniform handling causes a mesh rendered twice
     * with different uniform values to overwrite the first draw's uniform values with the second
     * draw's values. Therefore, [MeshData] tracks the most recent uniform data that a mesh has been
     * drawn with, and if the next draw differs from the previous draw, a new
     * [android.graphics.Mesh] will be created to satisfy it. This allows the typical use case,
     * where a stroke is drawn the same way frame to frame, to remain fast and reuse cached
     * [android.graphics.Mesh] instances. But less typical use cases, like animations that change
     * the color or scale/rotation of strokes, will still work but will be a little slower.
     *
     * On Android V+, this bug has been fixed, so the same [android.graphics.Mesh] can be reused
     * with different uniform values even within the same frame. Therefore, the extra data in
     * [MeshData] is ignored, and it will just contain the values that were first used when
     * rendering the associated [InkMesh].
     */
    private val inkMeshToAndroidMesh = WeakHashMap<InkMesh, MeshData>()

    /**
     * On Android U, this holds strong references to [android.graphics.Mesh] instances that were
     * recently used in [draw], so that they aren't garbage collected too soon causing their
     * underlying memory to be freed before the render thread can use it. Otherwise, the render
     * thread may use the memory after it is freed, leading to undefined behavior (typically a
     * crash). The values of this map are the last time (according to [getDurationTimeMillis]) that
     * the corresponding mesh has been drawn, so that its contents can be periodically evicted to
     * keep memory usage under control.
     *
     * On Android V+, this bug has been fixed, so this map will remain empty.
     */
    private val recentlyDrawnMeshesToLastDrawTimeMillis = MutableObjectLongMap<AndroidMesh>()

    /**
     * The last time that [recentlyDrawnMeshesToLastDrawTimeMillis] was checked for old meshes that
     * can be cleaned up.
     */
    private var recentlyDrawnMeshesLastCleanupTimeMillis = Long.MIN_VALUE

    /**
     * Cached [android.graphics.Mesh]es so that they can be reused between calls to
     * [Canvas.drawMesh], greatly improving performance. Each [InProgressStroke] maps to a list of
     * [InProgressMeshData] objects, one for each brush coat. Because [InProgressStroke] is mutable,
     * this cache is based not just on the existence of data, but whether that data's version number
     * matches that of the [InProgressStroke].
     */
    private val inProgressStrokeToAndroidMeshes =
        WeakHashMap<InProgressStroke, List<InProgressMeshData>>()

    /**
     * Caches [ShaderMetadata]s so that when two [MeshFormat] objects have equivalent packed
     * representations (see [MeshFormat.isPackedEquivalent]), the same [ShaderMetadata] object can
     * be used instead of reconstructed. This is a list instead of a map because:
     * 1. There should never be more than ~9 unique values of [MeshFormat], so a linear scan is not
     *    an undue cost when constructing a new [android.graphics.Mesh].
     * 2. [MeshFormat] does not implement `hashCode` and `equals` in a way that would be relevant
     *    here, since we only care about the packed representation for this use case. This could be
     *    worked around with a wrapper type of [MeshFormat] that is specific to the packed
     *    representation, but it didn't seem worth the extra effort.
     *
     * @See [obtainShaderMetadata] for the management of this cache.
     */
    private val meshFormatToPackedShaderMetadata = ArrayList<Pair<MeshFormat, ShaderMetadata>>()

    /**
     * Holds a mapping from [MeshFormat] to [ShaderMetadata], so that when two [MeshFormat] objects
     * are equivalent when it comes to their unpacked representation (see
     * [MeshFormat.isUnpackedEquivalent]), then the same [MeshSpecification] object can be used
     * instead of reconstructed. This is a list instead of a map because
     * 1. There should never be more than ~9 unique values of [MeshFormat], so a linear scan is not
     *    an undue cost when constructing a new [android.graphics.Mesh].
     * 2. [MeshFormat] does not implement `hashCode` and `equals` in a way that would be relevant
     *    here, since we only care about the unpacked representation for this use case.
     *
     * @See [obtainShaderMetadata] for the management of this cache.
     */
    private val meshFormatToUnpackedShaderMetadata = ArrayList<Pair<MeshFormat, ShaderMetadata>>()

    /** Scratch [Matrix] used for draw calls taking an [AffineTransform]. */
    private val scratchMatrix = Matrix()

    /** Scratch space used as the argument to [Matrix.getValues]. */
    private val matrixValuesScratchArray = FloatArray(9)

    /** Scratch space used to hold the scale/skew components of a [Matrix] in column-major order. */
    @Size(4) private val objectToCanvasLinearComponentScratch = FloatArray(4)

    /** Allocated once and reused for performance, passed to [AndroidMesh.setFloatUniform]. */
    private val colorRgbaScratchArray = FloatArray(4)

    // First and last inputs for the stroke being rendered, reused so that we don't need to allocate
    // new ones for every stroke.
    private val scratchFirstInput = StrokeInput()
    private val scratchLastInput = StrokeInput()

    override fun draw(canvas: Canvas, stroke: Stroke, strokeToCanvasTransform: AffineTransform) {
        strokeToCanvasTransform.populateMatrix(scratchMatrix)
        draw(canvas, stroke, scratchMatrix)
    }

    /**
     * Draw a [Stroke] to the [Canvas].
     *
     * @param canvas The [Canvas] to draw to.
     * @param stroke The [Stroke] to draw.
     * @param strokeToCanvasTransform The transform [Matrix] to convert from [Stroke] actual
     *   coordinates to the coordinates of [canvas]. It is important to pass this here to be applied
     *   internally rather than applying it to [canvas] in calling code, to ensure anti-aliasing has
     *   the information it needs to render properly. In addition, any transforms previously applied
     *   to [canvas] must only be translations, or rotations in multiples of 90 degrees. If you are
     *   not transforming [canvas] yourself then this will be correct, as the [android.view.View]
     *   hierarchy applies only translations by default. If you are rendering in a
     *   [android.view.View] where it (or one of its ancestors) is rotated or scaled within its
     *   parent, or if you are applying rotation or scaling transforms to [canvas] yourself, then
     *   care must be taken to undo those transforms before calling this method, and calling this
     *   method with a full stroke-to-screen (modulo translation or multi-90 degree rotation)
     *   transform. Without this, anti-aliasing at the edge of strokes will not render properly.
     */
    override fun draw(canvas: Canvas, stroke: Stroke, strokeToCanvasTransform: Matrix) {
        require(strokeToCanvasTransform.isAffine) { "strokeToCanvasTransform must be affine" }
        if (stroke.inputs.isEmpty()) return // nothing to draw
        stroke.inputs.populate(0, scratchFirstInput)
        stroke.inputs.populate(stroke.inputs.size - 1, scratchLastInput)
        for (coatIndex in 0 until stroke.brush.family.coats.size) {
            val meshes = stroke.shape.renderGroupMeshes(coatIndex)
            if (meshes.isEmpty()) continue
            val brushPaint = stroke.brush.family.coats[coatIndex].paint
            val blendMode = finalBlendMode(brushPaint)
            // A white paint color ensures that the paint color doesn't affect how the paint texture
            // is blended with the mesh coloring.
            val androidPaint =
                paintCache.obtain(
                    brushPaint,
                    AndroidColor.WHITE,
                    stroke.brush.size,
                    scratchFirstInput,
                    scratchLastInput,
                )
            for (mesh in meshes) {
                drawFromStroke(
                    canvas,
                    mesh,
                    strokeToCanvasTransform,
                    stroke.brush.composeColor,
                    blendMode,
                    androidPaint,
                )
            }
        }
    }

    /** Draw an [InkMesh] as if it is part of a stroke. */
    private fun drawFromStroke(
        canvas: Canvas,
        inkMesh: InkMesh,
        meshToCanvasTransform: Matrix,
        brushColor: ComposeColor,
        blendMode: BlendMode,
        paint: Paint,
    ) {
        fillObjectToCanvasLinearComponent(
            meshToCanvasTransform,
            objectToCanvasLinearComponentScratch
        )
        val cachedMeshData = inkMeshToAndroidMesh[inkMesh]
        @OptIn(BuildCompat.PrereleaseSdkCheck::class) val uniformBugFixed = BuildCompat.isAtLeastV()
        val androidMesh =
            if (
                cachedMeshData == null ||
                    (!uniformBugFixed &&
                        !cachedMeshData.areUniformsEquivalent(
                            brushColor,
                            objectToCanvasLinearComponentScratch
                        ))
            ) {
                val newMesh =
                    createAndroidMesh(inkMesh) ?: return // Nothing to draw if the mesh is empty.
                updateAndroidMesh(
                    newMesh,
                    inkMesh.format,
                    objectToCanvasLinearComponentScratch,
                    brushColor,
                    inkMesh.vertexAttributeUnpackingParams,
                )
                inkMeshToAndroidMesh[inkMesh] =
                    MeshData.create(newMesh, brushColor, objectToCanvasLinearComponentScratch)
                newMesh
            } else {
                if (uniformBugFixed) {
                    // Update the uniform values unconditionally because it's inexpensive after the
                    // bug fix.
                    // Before the bug fix, there's no need to update the uniforms since changed
                    // uniform values
                    // could have caused the mesh to be recreated above.
                    updateAndroidMesh(
                        cachedMeshData.androidMesh,
                        inkMesh.format,
                        objectToCanvasLinearComponentScratch,
                        brushColor,
                        inkMesh.vertexAttributeUnpackingParams,
                    )
                }
                cachedMeshData.androidMesh
            }

        canvas.save()
        try {
            canvas.concat(meshToCanvasTransform)
            canvas.drawMesh(androidMesh, blendMode, paint)
        } finally {
            // If any exceptions occur while drawing, restore the canvas so that restore is always
            // called
            // after canvas.save().
            canvas.restore()
        }

        if (!uniformBugFixed) {
            val currentTimeMillis = getDurationTimeMillis()
            // Before the `androidMesh` variable goes out of scope, save it as a hard reference
            // (temporarily) as a workaround for the Android U bug where drawMesh would not hand off
            // or
            // share ownership of the mesh data properly so data could be used by the render thread
            // after
            // being freed and cause a crash.
            saveRecentlyDrawnAndroidMesh(androidMesh, currentTimeMillis)
            // Clean up meshes that were previously saved as hard references, but shouldn't be saved
            // forever otherwise we'll run out of memory. Anything purged by this will only be kept
            // around
            // if its associated InkMesh is still referenced, due to their presence in
            // `inkMeshToAndroidMesh`.
            cleanUpRecentlyDrawnAndroidMeshes(currentTimeMillis)
        }
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToCanvasTransform: AffineTransform,
    ) {
        strokeToCanvasTransform.populateMatrix(scratchMatrix)
        draw(canvas, inProgressStroke, scratchMatrix)
    }

    override fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToCanvasTransform: Matrix,
    ) {
        val brush =
            checkNotNull(inProgressStroke.brush) {
                "Attempting to draw an InProgressStroke that has not been started."
            }
        require(strokeToCanvasTransform.isAffine) { "strokeToCanvasTransform must be affine" }
        val inputCount = inProgressStroke.getInputCount()
        if (inputCount == 0) return // nothing to draw
        inProgressStroke.populateInput(scratchFirstInput, 0)
        inProgressStroke.populateInput(scratchLastInput, inputCount - 1)
        fillObjectToCanvasLinearComponent(
            strokeToCanvasTransform,
            objectToCanvasLinearComponentScratch
        )
        val brushCoatCount = inProgressStroke.getBrushCoatCount()
        canvas.save()
        try {
            canvas.concat(strokeToCanvasTransform)
            for (coatIndex in 0 until brushCoatCount) {
                val brushPaint = brush.family.coats[coatIndex].paint
                val blendMode = finalBlendMode(brushPaint)
                val androidPaint =
                    paintCache.obtain(
                        brushPaint,
                        AndroidColor.WHITE,
                        brush.size,
                        scratchFirstInput,
                        scratchLastInput,
                    )
                val inProgressMeshData = obtainInProgressMeshData(inProgressStroke, coatIndex)
                for (meshIndex in 0 until inProgressMeshData.androidMeshes.size) {
                    val androidMesh = inProgressMeshData.androidMeshes[meshIndex] ?: continue
                    updateAndroidMesh(
                        androidMesh,
                        inProgressStroke.getMeshFormat(coatIndex, meshIndex),
                        objectToCanvasLinearComponentScratch,
                        brush.composeColor,
                        attributeUnpackingParams = null,
                    )
                    canvas.drawMesh(androidMesh, blendMode, androidPaint)
                }
            }
        } finally {
            // If any exceptions occur while drawing, restore the canvas so that restore is always
            // called
            // after canvas.save().
            canvas.restore()
        }
    }

    /** Create a new [AndroidMesh] for the given [InkMesh]. */
    private fun createAndroidMesh(inkMesh: InkMesh): AndroidMesh? {
        val bounds = inkMesh.bounds ?: return null // Nothing to render with an empty mesh.
        val meshSpec = obtainShaderMetadata(inkMesh.format, isPacked = true).meshSpecification
        return AndroidMesh(
            meshSpec,
            AndroidMesh.TRIANGLES,
            inkMesh.rawVertexData,
            inkMesh.vertexCount,
            inkMesh.rawTriangleIndexData,
            RectF(bounds.xMin, bounds.yMin, bounds.xMax, bounds.yMax),
        )
    }

    /**
     * Update [androidMesh] with the information that might have changed since the previous call to
     * [drawFromStroke] with the [InkMesh]. This is intended to be so low cost that it can be called
     * on every draw call.
     */
    private fun updateAndroidMesh(
        androidMesh: AndroidMesh,
        meshFormat: MeshFormat,
        @Size(min = 4) meshToCanvasLinearComponent: FloatArray,
        brushColor: ComposeColor,
        attributeUnpackingParams: List<MeshAttributeUnpackingParams>?,
    ) {
        val isPacked = attributeUnpackingParams != null
        var colorUniformName = INVALID_NAME
        var positionUnpackingParamsUniformName = INVALID_NAME
        var positionAttributeIndex = INVALID_ATTRIBUTE_INDEX
        var sideDerivativeUnpackingParamsUniformName = INVALID_NAME
        var sideDerivativeAttributeIndex = INVALID_ATTRIBUTE_INDEX
        var forwardDerivativeUnpackingParamsUniformName = INVALID_NAME
        var forwardDerivativeAttributeIndex = INVALID_ATTRIBUTE_INDEX
        var objectToCanvasLinearComponentUniformName = INVALID_NAME

        for ((id, _, name, unpackingIndex) in
            obtainShaderMetadata(meshFormat, isPacked).uniformMetadata) {
            when (id) {
                UniformId.OBJECT_TO_CANVAS_LINEAR_COMPONENT ->
                    objectToCanvasLinearComponentUniformName = name
                UniformId.BRUSH_COLOR -> colorUniformName = name
                UniformId.POSITION_UNPACKING_TRANSFORM -> {
                    check(isPacked) {
                        "Unpacking transform uniform is only supported for packed meshes."
                    }
                    positionUnpackingParamsUniformName = name
                    positionAttributeIndex = unpackingIndex
                }
                UniformId.SIDE_DERIVATIVE_UNPACKING_TRANSFORM -> {
                    check(isPacked) {
                        "Unpacking transform uniform is only supported for packed meshes."
                    }
                    sideDerivativeUnpackingParamsUniformName = name
                    sideDerivativeAttributeIndex = unpackingIndex
                }
                UniformId.FORWARD_DERIVATIVE_UNPACKING_TRANSFORM -> {
                    check(isPacked) {
                        "Unpacking transform uniform is only supported for packed meshes."
                    }
                    forwardDerivativeUnpackingParamsUniformName = name
                    forwardDerivativeAttributeIndex = unpackingIndex
                }
            }
        }
        // Color and object-to-canvas uniforms are required for all meshes.
        check(objectToCanvasLinearComponentUniformName != INVALID_NAME)
        check(colorUniformName != INVALID_NAME)
        // Unpacking transform uniforms are required for and only for packed meshes.
        check(
            !isPacked ||
                (positionUnpackingParamsUniformName != INVALID_NAME &&
                    sideDerivativeUnpackingParamsUniformName != INVALID_NAME &&
                    forwardDerivativeUnpackingParamsUniformName != INVALID_NAME)
        )

        androidMesh.setFloatUniform(
            objectToCanvasLinearComponentUniformName,
            meshToCanvasLinearComponent[0],
            meshToCanvasLinearComponent[1],
            meshToCanvasLinearComponent[2],
            meshToCanvasLinearComponent[3],
        )

        // Don't use setColorUniform because it does some color space conversion that we don't want.
        // Instead, set the uniform as an array of 4 floats, but ensure that the color is in the
        // same
        // color space that the MeshSpecification is configured to operate in. In
        // LinearExtendedSrgb,
        // "linear" refers to the format, "extended" means that the channel values are not clamped
        // to
        // [0, 1], and "sRGB" is the color space itself.
        androidMesh.setFloatUniform(
            colorUniformName,
            colorRgbaScratchArray.also {
                brushColor
                    .convert(ComposeColorSpaces.LinearExtendedSrgb)
                    .fillFloatArray(colorRgbaScratchArray)
            },
        )

        if (!isPacked) return

        attributeUnpackingParams!!.let {
            val positionParams = it[positionAttributeIndex]
            androidMesh.setFloatUniform(
                positionUnpackingParamsUniformName,
                positionParams.xOffset,
                positionParams.xScale,
                positionParams.yOffset,
                positionParams.yScale,
            )

            val sideDerivativeParams = it[sideDerivativeAttributeIndex]
            androidMesh.setFloatUniform(
                sideDerivativeUnpackingParamsUniformName,
                sideDerivativeParams.xOffset,
                sideDerivativeParams.xScale,
                sideDerivativeParams.yOffset,
                sideDerivativeParams.yScale,
            )

            val forwardDerivativeParams = it[forwardDerivativeAttributeIndex]
            androidMesh.setFloatUniform(
                forwardDerivativeUnpackingParamsUniformName,
                forwardDerivativeParams.xOffset,
                forwardDerivativeParams.xScale,
                forwardDerivativeParams.yOffset,
                forwardDerivativeParams.yScale,
            )
        }
    }

    private fun fillObjectToCanvasLinearComponent(
        objectToCanvasTransform: Matrix,
        @Size(min = 4) objectToCanvasLinearComponent: FloatArray,
    ) {
        require(objectToCanvasTransform.isAffine) { "objectToCanvasTransform must be affine" }
        objectToCanvasTransform.getValues(matrixValuesScratchArray)
        objectToCanvasLinearComponent.let {
            it[0] = matrixValuesScratchArray[Matrix.MSCALE_X]
            it[1] = matrixValuesScratchArray[Matrix.MSKEW_Y]
            it[2] = matrixValuesScratchArray[Matrix.MSKEW_X]
            it[3] = matrixValuesScratchArray[Matrix.MSCALE_Y]
        }
    }

    private fun obtainInProgressMeshData(
        inProgressStroke: InProgressStroke,
        coatIndex: Int,
    ): InProgressMeshData {
        val cachedMeshDatas = inProgressStrokeToAndroidMeshes[inProgressStroke]
        if (
            cachedMeshDatas != null && cachedMeshDatas.size == inProgressStroke.getBrushCoatCount()
        ) {
            val inProgressMeshData = cachedMeshDatas[coatIndex]
            if (inProgressMeshData.version == inProgressStroke.version) {
                return inProgressMeshData
            }
        }
        val inProgressMeshDatas = computeInProgressMeshDatas(inProgressStroke)
        inProgressStrokeToAndroidMeshes[inProgressStroke] = inProgressMeshDatas
        return inProgressMeshDatas[coatIndex]
    }

    private fun computeInProgressMeshDatas(
        inProgressStroke: InProgressStroke
    ): List<InProgressMeshData> =
        buildList() {
            for (coatIndex in 0 until inProgressStroke.getBrushCoatCount()) {
                val androidMeshes =
                    buildList() {
                        for (meshIndex in
                            0 until inProgressStroke.getMeshPartitionCount(coatIndex)) {
                            add(createAndroidMesh(inProgressStroke, coatIndex, meshIndex))
                        }
                    }
                add(InProgressMeshData(inProgressStroke.version, androidMeshes))
            }
        }

    /**
     * Create a new [AndroidMesh] for the unpacked mesh at [meshIndex] in brush coat [coatIndex] of
     * the given [inProgressStroke].
     */
    @VisibleForTesting
    internal fun createAndroidMesh(
        inProgressStroke: InProgressStroke,
        coatIndex: Int,
        meshIndex: Int,
    ): AndroidMesh? {
        val vertexCount = inProgressStroke.getVertexCount(coatIndex, meshIndex)
        if (vertexCount < 3) {
            // Fail gracefully when mesh doesn't contain enough vertices for a full triangle.
            return null
        }
        val bounds = BoxAccumulator().apply { inProgressStroke.populateMeshBounds(coatIndex, this) }
        if (bounds.isEmpty()) return null // Empty mesh; nothing to render.
        return AndroidMesh(
            obtainShaderMetadata(
                    inProgressStroke.getMeshFormat(coatIndex, meshIndex),
                    isPacked = false
                )
                .meshSpecification,
            AndroidMesh.TRIANGLES,
            inProgressStroke.getRawVertexBuffer(coatIndex, meshIndex),
            vertexCount,
            inProgressStroke.getRawTriangleIndexBuffer(coatIndex, meshIndex),
            bounds.box?.let { RectF(it.xMin, it.yMin, it.xMax, it.yMax) } ?: return null,
        )
    }

    /**
     * Returns a [ShaderMetadata] compatible with the [isPacked] state of the given [MeshFormat]'s
     * vertex format. This may be newly created, or an internally cached value.
     *
     * This method manages read and write access to both [meshFormatToPackedShaderMetadata] and
     * [meshFormatToUnpackedShaderMetadata]
     */
    @VisibleForTesting
    internal fun obtainShaderMetadata(meshFormat: MeshFormat, isPacked: Boolean): ShaderMetadata {
        val meshFromatToShaderMetaData =
            if (isPacked) meshFormatToPackedShaderMetadata else meshFormatToUnpackedShaderMetadata
        // Check the cache first.
        return getCachedValue(meshFormat, meshFromatToShaderMetaData, isPacked)
            ?: createShaderMetadata(meshFormat, isPacked).also {
                // Populate the cache before returning the newly-created ShaderMetadata.
                meshFromatToShaderMetaData.add(Pair(meshFormat, it))
            }
    }

    /**
     * Returns true when the [stroke]'s [inputs] are empty, or [MeshFormat] is compatible with the
     * native Skia `MeshSpecificationData`.
     */
    internal fun canDraw(stroke: Stroke): Boolean {
        for (groupIndex in 0 until stroke.shape.getRenderGroupCount()) {
            if (stroke.shape.renderGroupMeshes(groupIndex).isEmpty()) continue
            val format = stroke.shape.renderGroupFormat(groupIndex)
            if (!nativeIsMeshFormatRenderable(format.getNativeAddress(), isPacked = true)) {
                return false
            }
        }
        return true
    }

    private fun createShaderMetadata(meshFormat: MeshFormat, isPacked: Boolean): ShaderMetadata {
        // Fill "out" parameter arrays with invalid data, to fail fast in case anything goes wrong.
        val attributeTypesOut = IntArray(MAX_ATTRIBUTES) { Type.INVALID_NATIVE_VALUE }
        val attributeOffsetsBytesOut = IntArray(MAX_ATTRIBUTES) { INVALID_OFFSET }
        val attributeNamesOut = Array(MAX_ATTRIBUTES) { INVALID_NAME }
        val vertexStrideBytesOut = intArrayOf(INVALID_VERTEX_STRIDE)
        val varyingTypesOut = IntArray(MAX_VARYINGS) { Type.INVALID_NATIVE_VALUE }
        val varyingNamesOut = Array(MAX_VARYINGS) { INVALID_NAME }
        val uniformIdsOut = IntArray(MAX_UNIFORMS) { UniformId.INVALID_NATIVE_VALUE }
        val uniformTypesOut = IntArray(MAX_UNIFORMS) { Type.INVALID_NATIVE_VALUE }
        val uniformUnpackingIndicesOut = IntArray(MAX_UNIFORMS) { INVALID_ATTRIBUTE_INDEX }
        val uniformNamesOut = Array(MAX_UNIFORMS) { INVALID_NAME }
        val vertexShaderOut = arrayOf("unset vertex shader")
        val fragmentShaderOut = arrayOf("unset fragment shader")
        fillSkiaMeshSpecData(
            meshFormat.getNativeAddress(),
            isPacked,
            attributeTypesOut,
            attributeOffsetsBytesOut,
            attributeNamesOut,
            vertexStrideBytesOut,
            varyingTypesOut,
            varyingNamesOut,
            uniformIdsOut,
            uniformTypesOut,
            uniformUnpackingIndicesOut,
            uniformNamesOut,
            vertexShaderOut,
            fragmentShaderOut,
        )
        val attributes = mutableListOf<MeshSpecification.Attribute>()
        for (attrIndex in 0 until MAX_ATTRIBUTES) {
            val type = Type.fromNativeValue(attributeTypesOut[attrIndex]) ?: break
            val offset = attributeOffsetsBytesOut[attrIndex]
            val name = attributeNamesOut[attrIndex]
            attributes.add(MeshSpecification.Attribute(type.meshSpecValue, offset, name))
        }
        val varyings = mutableListOf<MeshSpecification.Varying>()
        for (varyingIndex in 0 until MAX_VARYINGS) {
            val type = Type.fromNativeValue(varyingTypesOut[varyingIndex]) ?: break
            val name = varyingNamesOut[varyingIndex]
            varyings.add(MeshSpecification.Varying(type.meshSpecValue, name))
        }
        val uniforms = mutableListOf<UniformMetadata>()
        for (uniformIndex in 0 until MAX_UNIFORMS) {
            val id = UniformId.fromNativeValue(uniformIdsOut[uniformIndex]) ?: break
            val type = Type.fromNativeValue(uniformTypesOut[uniformIndex]) ?: break
            val name = uniformNamesOut[uniformIndex]
            val attributeIndex = uniformUnpackingIndicesOut[uniformIndex]
            uniforms.add(UniformMetadata(id, type, name, attributeIndex))
        }

        return ShaderMetadata(
            meshSpecification =
                MeshSpecification.make(
                    attributes.toTypedArray(),
                    validVertexStrideBytes(vertexStrideBytesOut[0]),
                    varyings.toTypedArray(),
                    vertexShaderOut[0],
                    fragmentShaderOut[0],
                    // The shaders output linear, premultiplied, non-clamped sRGB colors.
                    AndroidColorSpace.get(AndroidColorSpace.Named.LINEAR_EXTENDED_SRGB),
                    MeshSpecification.ALPHA_TYPE_PREMULTIPLIED,
                ),
            uniformMetadata = uniforms,
        )
    }

    private fun validVertexStrideBytes(vertexStride: Int): Int {
        // MeshSpecification.make is documented to accept a vertex stride between 1 and 1024 bytes
        // (inclusive), but its only supported vertex attribute types are in multiples of 4 bytes,
        // so
        // its true lower bound is 4 bytes.
        require(vertexStride in 4..1024)
        return vertexStride
    }

    /**
     * Retrieves data analogous to [MeshSpecification] from native code. It makes use of "out"
     * parameters to return this data, as it is tedious (and therefore error-prone) to construct and
     * return complex objects from JNI. These "out" parameters are all arrays, as those are well
     * supported by JNI, especially primitive arrays.
     *
     * @param meshFormatNativeAddress The raw pointer address of a [MeshFormat].
     * @param isPacked Whether to fill the mesh spec with properties describing a packed format (as
     *   in ink::Mesh) or an unpacked format (as in ink::MutableMesh).
     * @param attributeTypesOut An array that can hold at least [MAX_ATTRIBUTES] values. It will
     *   contain the resulting attribute types aligning with [Type.nativeValue]. The number of
     *   attributes will be determined by the first index of this array with an invalid value, and
     *   that attribute count will determine the number of entries to look at in
     *   [attributeOffsetsBytesOut] and [attributeNamesOut]. See
     *   [MeshSpecification.Attribute.getType].
     * @param attributeOffsetsBytesOut An array that can hold at least [MAX_ATTRIBUTES] values.
     *   Specifies the layout of each vertex of the raw data for a mesh, where each vertex is a
     *   contiguous chunk of memory and each attribute is located at a particular number of bytes
     *   (offset) from the beginning of that vertex's chunk of memory.
     * @param attributeNamesOut The names of each attribute, referenced in the shader code.
     * @param vertexStrideBytesOut In the raw data of the mesh vertices, the number of bytes between
     *   the start of each vertex. See [attributeOffsetsBytesOut] for how each attribute is laid
     *   out.
     * @param varyingTypesOut An array that can hold at least [MAX_VARYINGS] values. It will contain
     *   the resulting varying types aligning with [Type.nativeValue]. The number of varyings will
     *   be determined by the first index of this array with an invalid value, and that varying
     *   count will determine the number of entries to look at in [varyingNamesOut]. See
     *   [MeshSpecification.Varying.getType].
     * @param varyingNamesOut The names of each varying, referenced in the shader code.
     * @param vertexShaderOut An array with at least one element that will be filled in by the
     *   string vertex shader code.
     * @param fragmentShaderOut An array with at least one element that will be filled in by the
     *   string fragment shader code.
     * @throws IllegalArgumentException If an unrecognized format was passed in, i.e. when
     *   [nativeIsMeshFormatRenderable] returns false.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun fillSkiaMeshSpecData(
        meshFormatNativeAddress: Long,
        isPacked: Boolean,
        attributeTypesOut: IntArray,
        attributeOffsetsBytesOut: IntArray,
        attributeNamesOut: Array<String>,
        vertexStrideBytesOut: IntArray,
        varyingTypesOut: IntArray,
        varyingNamesOut: Array<String>,
        uniformIdsOut: IntArray,
        uniformTypesOut: IntArray,
        uniformUnpackingIndicesOut: IntArray,
        uniformNamesOut: Array<String>,
        vertexShaderOut: Array<String>,
        fragmentShaderOut: Array<String>,
    )

    /**
     * Constructs native [MeshFormat] from [meshFormatNativeAddress] and checks whether it is
     * compatible with the native Skia `MeshSpecificationData`.
     *
     * @param isPacked checks whether [meshFormat] describes a packed format (as in native
     *   ink::Mesh) or an unpacked format (as in native ink::MutableMesh).
     *
     * [fillSkiaMeshSpecData] throws IllegalArgumentException when this method returns false.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun nativeIsMeshFormatRenderable(
        meshFormatNativeAddress: Long,
        isPacked: Boolean,
    ): Boolean

    private fun saveRecentlyDrawnAndroidMesh(androidMesh: AndroidMesh, currentTimeMillis: Long) {
        recentlyDrawnMeshesToLastDrawTimeMillis[androidMesh] = currentTimeMillis
    }

    private fun cleanUpRecentlyDrawnAndroidMeshes(currentTimeMillis: Long) {
        if (
            recentlyDrawnMeshesLastCleanupTimeMillis + EVICTION_SCAN_PERIOD_MS > currentTimeMillis
        ) {
            return
        }
        recentlyDrawnMeshesToLastDrawTimeMillis.removeIf { _, lastDrawTimeMillis ->
            lastDrawTimeMillis + MESH_STRONG_REFERENCE_DURATION_MS < currentTimeMillis
        }
        recentlyDrawnMeshesLastCleanupTimeMillis = currentTimeMillis
    }

    @VisibleForTesting
    internal fun getRecentlyDrawnAndroidMeshesCount(): Int {
        return recentlyDrawnMeshesToLastDrawTimeMillis.size
    }

    private fun ComposeColor.fillFloatArray(@Size(min = 4) outRgba: FloatArray) {
        outRgba[0] = this.red
        outRgba[1] = this.green
        outRgba[2] = this.blue
        outRgba[3] = this.alpha
    }

    private class MeshData
    private constructor(
        val androidMesh: AndroidMesh,
        val brushColor: ComposeColor,
        /** Do not modify! */
        @Size(4) val objectToCanvasLinearComponent: FloatArray,
    ) {

        fun areUniformsEquivalent(
            otherBrushColor: ComposeColor,
            @Size(4) otherObjectToCanvasLinearComponent: FloatArray,
        ): Boolean =
            otherBrushColor == brushColor &&
                otherObjectToCanvasLinearComponent.contentEquals(objectToCanvasLinearComponent)

        companion object {
            fun create(
                androidMesh: AndroidMesh,
                brushColor: ComposeColor,
                @Size(4) objectToCanvasLinearComponent: FloatArray,
            ): MeshData {
                val copied = FloatArray(4)
                System.arraycopy(
                    /* src = */ objectToCanvasLinearComponent,
                    /* srcPos = */ 0,
                    /* dest = */ copied,
                    /* destPos = */ 0,
                    /* length = */ 4,
                )
                return MeshData(androidMesh, brushColor, copied)
            }
        }
    }

    /**
     * Contains the [android.graphics.Mesh] data for an [InProgressStroke], along with metadata used
     * to verify if that data is still valid.
     */
    private data class InProgressMeshData(
        /** If this does not match [InProgressStroke.version], the data is invalid. */
        val version: Long,
        /**
         * At each index, the [android.graphics.Mesh] for the corresponding partition index of the
         * [InProgressStroke], or `null` if that partition is empty.
         */
        val androidMeshes: List<AndroidMesh?>,
    )

    companion object {
        init {
            NativeLoader.load()
        }

        /**
         * On Android U, how long to hold a reference to an [android.graphics.Mesh] after it has
         * been drawn with [Canvas.drawMesh]. This is an imperfect workaround for a bug in the
         * native layer where the render thread is not given ownership of the mesh data to prevent
         * it from being freed before the render thread uses it for drawing.
         */
        private const val MESH_STRONG_REFERENCE_DURATION_MS = 5000
        private const val EVICTION_SCAN_PERIOD_MS = 2000

        /** All the metadata about values sent to the shader for a given mesh. Used for caching. */
        internal data class ShaderMetadata(
            val meshSpecification: MeshSpecification,
            val uniformMetadata: List<UniformMetadata>,
        )

        internal data class UniformMetadata(
            val id: UniformId,
            val type: Type,
            val name: String,
            val unpackingAttributeIndex: Int,
        )

        internal enum class UniformId(val nativeValue: Int) {
            /**
             * The 2x2 linear component of the affine transformation from mesh / "object"
             * coordinates to the canvas. This requires that the [meshToCanvasTransform] matrix used
             * during drawing is an affine transform. Set it with [AndroidMesh.setFloatUniform]. It
             * is a `float4` with the following expected entries:
             * - `[0]`: `matrixValues[Matrix.MSCALE_X]`
             * - `[1]`: `matrixValues[Matrix.MSKEW_X]`
             * - `[2]`: `matrixValues[Matrix.MSKEW_Y]`
             * - `[3]`: `matrixValues[Matrix.MSCALE_Y]`
             */
            OBJECT_TO_CANVAS_LINEAR_COMPONENT(0),

            /**
             * The [Color] of the Stroke's brush, which will be combined with per-vertex color
             * shifts in the shaders. Set it with [AndroidMesh.setColorUniform]. Must be specified
             * for every format.
             */
            BRUSH_COLOR(1),

            /**
             * The transform parameters to convert packed [InkMesh] coordinates into actual
             * ("object") coordinates. Set it with [AndroidMesh.setFloatUniform]. Must be specified
             * for packed meshes only. It is a `float4` with the following entries:
             * - `[0]`: x offset
             * - `[1]`: x scale
             * - `[2]`: y offset
             * - `[3]`: y scale
             */
            POSITION_UNPACKING_TRANSFORM(2),

            /**
             * The transform parameters to convert packed [InkMesh] side-derivative attribute values
             * into their unpacked values. Set it with [AndroidMesh.setFloatUniform]. Must be
             * specified for packed meshes only. It is a `float4` with the following entries:
             * - `[0]`: x offset
             * - `[1]`: x scale
             * - `[2]`: y offset
             * - `[3]`: y scale
             */
            SIDE_DERIVATIVE_UNPACKING_TRANSFORM(3),

            /**
             * The transform parameters to convert packed [InkMesh] forward-derivative attribute
             * values into their unpacked values. Set it with [AndroidMesh.setFloatUniform]. Must be
             * specified for packed meshes only. It is a `float4` with the following entries:
             * - `[0]`: x offset
             * - `[1]`: x scale
             * - `[2]`: y offset
             * - `[3]`: y scale
             */
            FORWARD_DERIVATIVE_UNPACKING_TRANSFORM(4);

            companion object {
                const val INVALID_NATIVE_VALUE = -1

                fun fromNativeValue(nativeValue: Int): UniformId? {
                    for (type in UniformId.values()) {
                        if (type.nativeValue == nativeValue) return type
                    }
                    return null
                }
            }
        }

        private const val MAX_ATTRIBUTES = 8
        private const val MAX_VARYINGS = 6
        private const val MAX_UNIFORMS = 6

        private const val INVALID_OFFSET = -1
        private const val INVALID_VERTEX_STRIDE = -1
        private const val INVALID_NAME = ")"
        private const val INVALID_ATTRIBUTE_INDEX = -1

        internal enum class Type(val nativeValue: Int, val meshSpecValue: Int) {
            FLOAT(0, MeshSpecification.TYPE_FLOAT),
            FLOAT2(1, MeshSpecification.TYPE_FLOAT2),
            FLOAT3(2, MeshSpecification.TYPE_FLOAT3),
            FLOAT4(3, MeshSpecification.TYPE_FLOAT4),
            UBYTE4(4, MeshSpecification.TYPE_UBYTE4);

            companion object {
                const val INVALID_NATIVE_VALUE = -1

                fun fromNativeValue(nativeValue: Int): Type? {
                    for (type in Type.values()) {
                        if (type.nativeValue == nativeValue) return type
                    }
                    return null
                }
            }
        }

        /**
         * Returns the [T] associated with [key] in [cache]. If [isPacked] is false, keys are
         * considered equivalent if their unpacked format is the same; if true, if their packed
         * format is the same. This provides a map-like getter interface for a cache implemented as
         * a list of key-value pairs. Returns `null` if no equivalent key is found.
         */
        private fun <T> getCachedValue(
            key: MeshFormat,
            cache: ArrayList<Pair<MeshFormat, T>>,
            isPacked: Boolean,
        ): T? {
            for ((format, item) in cache) {
                if (isPacked && format.isPackedEquivalent(key)) {
                    return item
                } else if (!isPacked && format.isUnpackedEquivalent(key)) {
                    return item
                }
            }
            return null
        }

        private fun finalBlendMode(brushPaint: BrushPaint): BlendMode =
            brushPaint.textureLayers.lastOrNull()?.let { it.blendMode.toBlendMode() }
                ?: BlendMode.MODULATE

        private val MeshAttributeUnpackingParams.xOffset
            get() = components[0].offset

        private val MeshAttributeUnpackingParams.xScale
            get() = components[0].scale

        private val MeshAttributeUnpackingParams.yOffset
            get() = components[1].offset

        private val MeshAttributeUnpackingParams.yScale
            get() = components[1].scale
    }
}
