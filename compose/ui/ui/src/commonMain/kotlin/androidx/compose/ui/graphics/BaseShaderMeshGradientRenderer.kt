/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Platform-independent [MeshGradientRenderer] that renders a mesh gradient with a GPU mesh, where
 * the bicubic Bezier surface evaluation and per-pixel color interpolation are performed by a
 * backend mesh shader.
 *
 * All of the platform-neutral work lives here: adaptive subdivision, the uv/index geometry, the
 * OkLab color conversion (with a per-point cache), and assembling each patch's shader uniforms. The
 * backend supplies only the mesh creation and draw calls through [onGeometryChanged],
 * [onMeshInstancesChanged] and [drawPatch].
 *
 * Subclasses are stateful and reuse their mesh objects and buffers across frames; this base reuses
 * the per-patch uniform buffers to avoid per-frame allocations.
 */
internal abstract class BaseShaderMeshGradientRenderer : MeshGradientRenderer {

    private var lastSubdivisionU: Int = -1
    private var lastSubdivisionV: Int = -1

    private var uvBuffArray: FloatArray? = null
    private var indicesArray: ShortArray? = null

    private var lastMeshSize: Size? = null
    private var lastPatchCount: Int = -1

    private var baseMeshColorsOkLab = FloatArray(0)

    // Cached colors array from the last draw call to compare against and avoid color conversions if
    // a point's color is unchanged.
    private var cachedBaseMeshColors: IntArray? = null

    // Reusable set of float arrays to hold the uniform data for each patch.
    private val patchPointsLocation = FloatArray(8)
    private val patchAndNeighborsColors = FloatArray(64)
    private val patchPointsLBO = FloatArray(8)
    private val patchPointsRBO = FloatArray(8)
    private val patchPointsTBO = FloatArray(8)
    private val patchPointsBBO = FloatArray(8)

    /**
     * Gives the backend a chance to draw the gradient itself (e.g. a software-canvas fallback) and
     * skip the mesh pipeline. Returns true if the [config] was fully handled; false to proceed with
     * the mesh path. Defaults to always using the mesh path.
     */
    protected open fun DrawScope.drawFallbackIfNeeded(config: MeshGradientConfig): Boolean = false

    /**
     * Notifies the backend that the mesh geometry (the uv/index buffers) changed because the
     * subdivisions changed, so it should rebuild any buffers derived from them. Not called on a
     * pure size or patch-count change, so the buffers can be reused across resizes.
     *
     * @param uvBuffer Per-vertex uv coordinates, `subdivisionsU * subdivisionsV * 2` floats.
     * @param indexBuffer Triangle indices into the vertex arrays.
     * @param subdivisionsU The number of horizontal subdivisions.
     * @param subdivisionsV The number of vertical subdivisions.
     */
    protected abstract fun onGeometryChanged(
        uvBuffer: FloatArray,
        indexBuffer: ShortArray,
        subdivisionsU: Int,
        subdivisionsV: Int,
    )

    /**
     * Notifies the backend that its cached mesh instances must be (re)built because the
     * subdivisions, [patchCount] or [bounds] changed. When the subdivisions changed,
     * [onGeometryChanged] is called first, so the latest geometry buffers are available.
     *
     * @param subdivisionsU The number of horizontal subdivisions.
     * @param subdivisionsV The number of vertical subdivisions.
     * @param patchCount The number of patches (`rows * columns`).
     * @param bounds The size of the area the gradient is drawn into.
     */
    protected abstract fun onMeshInstancesChanged(
        subdivisionsU: Int,
        subdivisionsV: Int,
        patchCount: Int,
        bounds: Size,
    )

    /**
     * Sets the per-patch uniforms on the backend's cached mesh for [patchIndex] and draws it. All
     * offsets are scaled to [bounds][onMeshInstancesChanged] and colors are already in OkLab.
     *
     * @param canvas The canvas to draw into.
     * @param patchIndex The index of the patch to draw.
     * @param hasBicubicColor Whether to use bicubic (Catmull-Rom) rather than bilinear color.
     * @param pointLocations The 4 corner positions of the patch (8 floats).
     * @param pointColors The 4x4 OkLab colors around the patch (64 floats).
     * @param leftBezierOffsets The 4 corner left Bezier offsets (8 floats).
     * @param rightBezierOffsets The 4 corner right Bezier offsets (8 floats).
     * @param topBezierOffsets The 4 corner top Bezier offsets (8 floats).
     * @param bottomBezierOffsets The 4 corner bottom Bezier offsets (8 floats).
     */
    protected abstract fun drawPatch(
        canvas: Canvas,
        patchIndex: Int,
        hasBicubicColor: Boolean,
        pointLocations: FloatArray,
        pointColors: FloatArray,
        leftBezierOffsets: FloatArray,
        rightBezierOffsets: FloatArray,
        topBezierOffsets: FloatArray,
        bottomBezierOffsets: FloatArray,
    )

    override fun DrawScope.draw(config: MeshGradientConfig) {
        if (drawFallbackIfNeeded(config)) return

        val (subdivisionsU, subdivisionsV) =
            calculateMeshGradientSubdivisions(config.rows, config.columns, config.positions, size)

        val subdivisionsChanged =
            lastSubdivisionU != subdivisionsU || lastSubdivisionV != subdivisionsV
        if (uvBuffArray == null || indicesArray == null || subdivisionsChanged) {
            buildVertexAndIndexBuffer(subdivisionsU, subdivisionsV)
            onGeometryChanged(uvBuffArray!!, indicesArray!!, subdivisionsU, subdivisionsV)
            lastSubdivisionU = subdivisionsU
            lastSubdivisionV = subdivisionsV
        }

        val colorsArraySize = (config.rows + 1) * (config.columns + 1) * 4
        if (baseMeshColorsOkLab.size != colorsArraySize) {
            baseMeshColorsOkLab = FloatArray(colorsArraySize)
            cachedBaseMeshColors = null
        }
        convertBaseMeshColorsToOkLab(config.colors, baseMeshColorsOkLab)

        val numberOfPatches = config.rows * config.columns
        if (subdivisionsChanged || size != lastMeshSize || lastPatchCount != numberOfPatches) {
            onMeshInstancesChanged(subdivisionsU, subdivisionsV, numberOfPatches, size)
            lastMeshSize = size
            lastPatchCount = numberOfPatches
        }

        for (patchIdx in 0 until numberOfPatches) {
            readLocationDataOfPatchPoints(
                patchIdx,
                config.columns,
                config.positions,
                size,
                patchPointsLocation,
            )
            readColorDataOfPatchPointsAndNeighbors(
                patchIdx,
                config.rows,
                config.columns,
                baseMeshColorsOkLab,
                patchAndNeighborsColors,
            )
            readLocationDataOfPatchPoints(
                patchIdx,
                config.columns,
                config.leftBezierOffsets,
                size,
                patchPointsLBO,
            )
            readLocationDataOfPatchPoints(
                patchIdx,
                config.columns,
                config.rightBezierOffsets,
                size,
                patchPointsRBO,
            )
            readLocationDataOfPatchPoints(
                patchIdx,
                config.columns,
                config.topBezierOffsets,
                size,
                patchPointsTBO,
            )
            readLocationDataOfPatchPoints(
                patchIdx,
                config.columns,
                config.bottomBezierOffsets,
                size,
                patchPointsBBO,
            )

            drawPatch(
                drawContext.canvas,
                patchIdx,
                config.hasBicubicColor,
                patchPointsLocation,
                patchAndNeighborsColors,
                patchPointsLBO,
                patchPointsRBO,
                patchPointsTBO,
                patchPointsBBO,
            )
        }
    }

    /** Builds the per-vertex uv coordinates and the triangle index buffer for the subdivisions. */
    private fun buildVertexAndIndexBuffer(tesselationFactorU: Int, tesselationFactorV: Int) {
        val vertexCount = tesselationFactorU * tesselationFactorV
        val uvBuffArray = FloatArray(vertexCount * 2)
        val indicesArray = ShortArray((tesselationFactorU - 1) * (tesselationFactorV - 1) * 6)

        var indicesWriteIndex = 0
        for (u in 0..<tesselationFactorU) {
            for (v in 0..<tesselationFactorV) {
                val uvWriteIndex = (u * (tesselationFactorV) + v) * 2
                uvBuffArray[uvWriteIndex] = u.toFloat() / (tesselationFactorU - 1).toFloat()
                uvBuffArray[uvWriteIndex + 1] = v.toFloat() / (tesselationFactorV - 1).toFloat()

                if (u < tesselationFactorU - 1 && v < tesselationFactorV - 1) {
                    indicesArray[indicesWriteIndex] = (u * (tesselationFactorV) + v).toShort()
                    indicesArray[indicesWriteIndex + 1] =
                        ((u + 1) * (tesselationFactorV) + v).toShort()
                    indicesArray[indicesWriteIndex + 2] =
                        (u * (tesselationFactorV) + (v + 1)).toShort()

                    indicesArray[indicesWriteIndex + 3] =
                        (u * (tesselationFactorV) + (v + 1)).toShort()
                    indicesArray[indicesWriteIndex + 4] =
                        ((u + 1) * (tesselationFactorV) + v).toShort()
                    indicesArray[indicesWriteIndex + 5] =
                        ((u + 1) * (tesselationFactorV) + (v + 1)).toShort()
                    indicesWriteIndex += 6
                }
            }
        }

        this.uvBuffArray = uvBuffArray
        this.indicesArray = indicesArray
    }

    private fun convertBaseMeshColorsToOkLab(inputArray: IntArray, outArray: FloatArray) {
        val cache = cachedBaseMeshColors
        if (cache == null) {
            // Since we do not have any cached color, convert all colors without any comparison.
            // This will usually be the case when drawing the gradient for the first time.
            val newCache = IntArray(inputArray.size)
            for (i in inputArray.indices) {
                val color = Color(inputArray[i]).convert(ColorSpaces.Oklab)
                outArray[i * 4 + 0] = color.red // L
                outArray[i * 4 + 1] = color.green // a
                outArray[i * 4 + 2] = color.blue // b
                outArray[i * 4 + 3] = color.alpha
                newCache[i] = inputArray[i]
            }
            cachedBaseMeshColors = newCache
        } else {
            for (i in inputArray.indices) {
                if (cache[i] == inputArray[i]) continue
                val color = Color(inputArray[i]).convert(ColorSpaces.Oklab)
                outArray[i * 4 + 0] = color.red // L
                outArray[i * 4 + 1] = color.green // a
                outArray[i * 4 + 2] = color.blue // b
                outArray[i * 4 + 3] = color.alpha
                cache[i] = inputArray[i]
            }
        }
    }

    /**
     * Reads location/offset data for the four corner points of a specific patch from a given input
     * array. The data is scaled by the provided [size] and stored in the [outArray].
     *
     * @param patchIdx The index of the current patch.
     * @param columns The total number of columns in the mesh.
     * @param inArray The input [FloatArray] containing the data (e.g., positions or bezier
     *   offsets).
     * @param size The [Size] object used for scaling the data.
     * @param outArray The output [FloatArray] where the scaled data for the patch's corner points
     *   will be stored. It is expected to have a size of 8 (4 points * 2 components).
     */
    private fun readLocationDataOfPatchPoints(
        patchIdx: Int,
        columns: Int,
        inArray: FloatArray,
        size: Size,
        outArray: FloatArray,
    ) {
        val patchRow = patchIdx / columns
        val patchCol = patchIdx % columns
        val strideWidth = columns + 1

        val topLeftIdx = (patchRow * strideWidth + patchCol) * 2
        val topRightIdx = (patchRow * strideWidth + patchCol + 1) * 2
        val bottomLeftIdx = ((patchRow + 1) * strideWidth + patchCol) * 2
        val bottomRightIdx = ((patchRow + 1) * strideWidth + patchCol + 1) * 2

        outArray[0] = inArray[topLeftIdx] * size.width
        outArray[1] = inArray[topLeftIdx + 1] * size.height
        outArray[2] = inArray[topRightIdx] * size.width
        outArray[3] = inArray[topRightIdx + 1] * size.height
        outArray[4] = inArray[bottomLeftIdx] * size.width
        outArray[5] = inArray[bottomLeftIdx + 1] * size.height
        outArray[6] = inArray[bottomRightIdx] * size.width
        outArray[7] = inArray[bottomRightIdx + 1] * size.height
    }

    /**
     * Reads color data for a 4x4 grid of points centered around a specific patch, including its
     * neighbors. The color data is read from the input [colors] list and stored in the [outArray].
     *
     * The 4x4 grid includes the patch itself, its immediate neighbors, and the diagonal neighbors.
     *
     * @param patchIdx The index of the current patch.
     * @param rows The total number of rows in the mesh.
     * @param columns The total number of columns in the mesh.
     * @param colors The input [FloatArray] containing the color data (RGBA, 4 components per
     *   point).
     * @param outArray The output [FloatArray] where the color data for the 4x4 grid will be stored.
     *   It is expected to have a size of at least 64 (16 points * 4 components).
     */
    private fun readColorDataOfPatchPointsAndNeighbors(
        patchIdx: Int,
        rows: Int,
        columns: Int,
        colors: FloatArray,
        outArray: FloatArray,
    ) {
        val patchRow = patchIdx / columns
        val patchCol = patchIdx % columns

        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val row = (patchRow - 1 + r).coerceIn(0, rows)
                val col = (patchCol - 1 + c).coerceIn(0, columns)
                val writeIndex = (r * 4 + c) * 4
                val readIndex = (row * (columns + 1) + col) * 4
                outArray[writeIndex] = colors[readIndex]
                outArray[writeIndex + 1] = colors[readIndex + 1]
                outArray[writeIndex + 2] = colors[readIndex + 2]
                outArray[writeIndex + 3] = colors[readIndex + 3]
            }
        }
    }
}
