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

import android.graphics.BlendMode
import android.graphics.Mesh
import android.graphics.MeshSpecification
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.ceil
import kotlin.math.sqrt
import org.intellij.lang.annotations.Language

/**
 * API 34+ implementation of a [MeshGradientRenderer] that renders a mesh gradient using Android's
 * `Mesh` API (available from API 34).
 *
 * This API leverages Android's `Mesh` API to render a bicubic Bezier patch mesh. Each patch is
 * defined by 4 corner points of a cell in the grid, 8 bezier control points (2 per side), and 16
 * color points (4x4 grid around the patch).
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class MeshGradientRendererV34Impl : MeshGradientRenderer {
    private val paint = Paint()

    private val attributes =
        arrayOf(MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT2, 0, "uv"))

    private val stride = 8

    private val varyings = arrayOf(MeshSpecification.Varying(MeshSpecification.TYPE_FLOAT2, "uv"))

    private val meshSpec =
        MeshSpecification.make(
            attributes,
            stride,
            varyings,
            vertexShaderSource,
            fragmentShaderSource,
            ColorSpaces.LinearSrgb.toAndroidColorSpace(),
            MeshSpecification.ALPHA_TYPE_PREMULTIPLIED,
        )

    // Mesh instances once created for a patch are stored here for reuse in subsequent draw calls.
    private var meshObjects: Array<Mesh>? = null
    private var lastMeshSize: Size? = null

    // In case the canvas is not hardware-accelerated, we use this renderer instance that uses
    // `drawVertices` to draw the mesh.
    private var meshGradientFallbackRendererImpl: MeshGradientRenderer? = null

    // More subdivisions make the mesh more detailed (high poly) but also affect the performance.
    // Since the mesh is a tessellation of a bicubic Bezier patch, fewer subdivisions can make the
    // curved edges look not smooth enough.
    // Additionally, note that `android.graphics.Mesh`'s index buffer is a ShortBuffer,
    // which means that if the total number of vertices in a patch is more than the range of
    // unsigned Short, the indexing will be incorrect because of overflow.
    // It is also worth noting that since we are doing per pixel shading, this subdivisionFactor has
    // no
    // effect on color interpolation whatsoever.
    // The subdivisions are dynamically calculated based on the size of the gradient.
    private var lastSubdivisionU: Int = -1
    private var lastSubdivisionV: Int = -1

    private var uvBuffArray: FloatArray? = null
    private var indicesArray: ShortArray? = null
    private var vertexDataBuffer: ByteBuffer? = null
    private var indexBuffer: ShortBuffer? = null

    // Reusable set of float arrays to hold data for each patch
    private val patchPointsLBO = FloatArray(8)
    private val patchPointsRBO = FloatArray(8)
    private val patchPointsTBO = FloatArray(8)
    private val patchPointsBBO = FloatArray(8)
    private val patchPointsLocation = FloatArray(8)
    private val patchAndNeighborsColors = FloatArray(64)

    private var baseMeshColorsOkLab = FloatArray(0)

    // Cached colors array from the last draw call to compare against and avoid color conversions if
    // a point's color is unchanged
    private var cachedBaseMeshColors: IntArray? = null

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

        vertexDataBuffer =
            ByteBuffer.allocateDirect(uvBuffArray.size * 4).apply {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().put(uvBuffArray)
                position(0)
            }
        indexBuffer =
            ByteBuffer.allocateDirect(indicesArray.size * 2)
                .apply {
                    order(ByteOrder.nativeOrder())
                    asShortBuffer().put(indicesArray)
                    position(0)
                }
                .asShortBuffer()

        this.uvBuffArray = uvBuffArray
        this.indicesArray = indicesArray
    }

    private fun drawInternal(
        canvas: android.graphics.Canvas,
        config: MeshGradientConfig,
        size: Size,
    ) {

        val (subdivisionsU, subdivisionsV) =
            calculateSubdivisions(config.rows, config.columns, config.positions, size)

        val subdivisionsHaveChanged =
            (lastSubdivisionU != subdivisionsU || lastSubdivisionV != subdivisionsV)

        if (uvBuffArray == null || indicesArray == null || subdivisionsHaveChanged) {
            buildVertexAndIndexBuffer(subdivisionsU, subdivisionsV)
            lastSubdivisionU = subdivisionsU
            lastSubdivisionV = subdivisionsV
        }

        val currentVertexDataBuffer = this.vertexDataBuffer ?: return
        val currentIndexBuffer = this.indexBuffer ?: return

        val colorsArraySize = (config.rows + 1) * (config.columns + 1) * 4
        if (baseMeshColorsOkLab.size != colorsArraySize) {
            baseMeshColorsOkLab = FloatArray(colorsArraySize)
            cachedBaseMeshColors = null
        }
        convertBaseMeshColorsToOkLab(config.colors, baseMeshColorsOkLab)

        val numberOfPatches = config.rows * config.columns
        var meshes = meshObjects

        if (
            meshes == null ||
                meshes.size != numberOfPatches ||
                size != lastMeshSize ||
                subdivisionsHaveChanged
        ) {
            meshes =
                Array(numberOfPatches) {
                    Mesh(
                        meshSpec,
                        Mesh.TRIANGLES,
                        currentVertexDataBuffer,
                        subdivisionsU * subdivisionsV,
                        currentIndexBuffer,
                        RectF(0f, 0f, size.width, size.height),
                    )
                }
            meshObjects = meshes
            lastMeshSize = size
        }

        for (patchIdx in 0..<config.rows * config.columns) {
            val mesh = meshObjects!![patchIdx]

            mesh.setIntUniform("useBicubicColorInterpolation", if (config.hasBicubicColor) 1 else 0)

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

            mesh.setFloatUniform("baseMeshPointLocations", patchPointsLocation)
            mesh.setFloatUniform("baseMeshPointColors", patchAndNeighborsColors)
            mesh.setFloatUniform("baseMeshPointLeftBezierOffsets", patchPointsLBO)
            mesh.setFloatUniform("baseMeshPointRightBezierOffsets", patchPointsRBO)
            mesh.setFloatUniform("baseMeshPointTopBezierOffsets", patchPointsTBO)
            mesh.setFloatUniform("baseMeshPointBottomBezierOffsets", patchPointsBBO)

            // Using BlendMode.DST since this argument dictates blending with mesh primitives as the
            // destination color and paint's color/shader as the source color.
            canvas.drawMesh(mesh, BlendMode.DST, paint)
        }
    }

    override fun DrawScope.draw(config: MeshGradientConfig) {
        if (this.drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
            drawInternal(this.drawContext.canvas.nativeCanvas, config, size)
        } else {
            // Fallback to `drawVertices` since it is supported on a software backed canvas.
            val fallback =
                meshGradientFallbackRendererImpl
                    ?: MeshGradientRendererImpl().also { meshGradientFallbackRendererImpl = it }
            with(fallback) { draw(config) }
        }
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
     * Dynamically calculates the number of subdivisions (segments) for the mesh grid based on the
     * physical size of the largest patch. This is to avoid over tessellations when a higher LOD is
     * not necessarily required.
     *
     * @param rows The number of rows in the mesh.
     * @param columns The number of columns in the mesh.
     * @param positions The array of mesh positions.
     * @param size The total size of the area where the gradient is being drawn.
     */
    private fun calculateSubdivisions(
        rows: Int,
        columns: Int,
        positions: FloatArray,
        size: Size,
    ): IntSize {
        var maxW = 0f
        var maxH = 0f
        for (patchIdx in 0 until rows * columns) {
            val patchRow = patchIdx / columns
            val patchColumn = patchIdx % columns
            val topLeft = getPointIndex(patchRow, patchColumn, columns) * 2
            val topRight = getPointIndex(patchRow, patchColumn + 1, columns) * 2
            val bottomLeft = getPointIndex(patchRow + 1, patchColumn, columns) * 2
            val bottomRight = getPointIndex(patchRow + 1, patchColumn + 1, columns) * 2

            val patchWidth =
                (dist(
                    positions[topLeft] * size.width,
                    positions[topLeft + 1] * size.height,
                    positions[topRight] * size.width,
                    positions[topRight + 1] * size.height,
                ) +
                    dist(
                        positions[bottomLeft] * size.width,
                        positions[bottomLeft + 1] * size.height,
                        positions[bottomRight] * size.width,
                        positions[bottomRight + 1] * size.height,
                    )) * 0.5f
            val patchHeight =
                (dist(
                    positions[topLeft] * size.width,
                    positions[topLeft + 1] * size.height,
                    positions[bottomLeft] * size.width,
                    positions[bottomLeft + 1] * size.height,
                ) +
                    dist(
                        positions[topRight] * size.width,
                        positions[topRight + 1] * size.height,
                        positions[bottomRight] * size.width,
                        positions[bottomRight + 1] * size.height,
                    )) * 0.5f

            maxW = maxOf(maxW, patchWidth)
            maxH = maxOf(maxH, patchHeight)
        }

        val subdivisionsU =
            ceil(maxW / TargetPxPerSegment).toInt().coerceIn(MinSubdivision, MaxSubdivision)
        val subdivisionsV =
            ceil(maxH / TargetPxPerSegment).toInt().coerceIn(MinSubdivision, MaxSubdivision)
        return IntSize(subdivisionsU, subdivisionsV)
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

    /**
     * Calculates the flat index into a vertex-based array (like positions or colors) based on the
     * [row] and [col] in a grid with a specific number of [columns].
     *
     * Since a mesh with N columns has N+1 vertices horizontally, the stride used is (columns + 1).
     */
    private fun getPointIndex(row: Int, col: Int, columns: Int): Int {
        return row * (columns + 1) + col
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    companion object {
        private const val MinSubdivision = 4
        private const val MaxSubdivision = 64
        private const val TargetPxPerSegment = 8f
    }
}

@Language("AGSL")
private val vertexShaderSource =
    """
    uniform int useBicubicColorInterpolation;
    uniform float2 baseMeshPointLocations[4];
    uniform float4 baseMeshPointColors[16];
    uniform float2 baseMeshPointLeftBezierOffsets[4];
    uniform float2 baseMeshPointRightBezierOffsets[4];
    uniform float2 baseMeshPointTopBezierOffsets[4];
    uniform float2 baseMeshPointBottomBezierOffsets[4];
    
    // Basis matrix
    const float4x4 bM = float4x4(
        vec4(-1.0, 3.0, -3, 1),
        vec4(3.0, -6.0, 3.0, 0),
        vec4(-3.0, 3.0, 0.0, 0),
        vec4(1.0, 0.0, 0.0, 0)
    );

    Varyings main(const Attributes attributes) {

        float2 uv = attributes.uv;

        int patchRow = 0;
        int patchColumn = 0;

        float u = uv.x;
        float v = uv.y;

        float2 pointAPos = baseMeshPointLocations[0];
        float2 pointBPos = baseMeshPointLocations[1];
        float2 pointCPos = baseMeshPointLocations[2];
        float2 pointDPos = baseMeshPointLocations[3];

        float2 pointALBO = baseMeshPointLeftBezierOffsets[0];
        float2 pointBLBO = baseMeshPointLeftBezierOffsets[1];
        float2 pointCLBO = baseMeshPointLeftBezierOffsets[2];
        float2 pointDLBO = baseMeshPointLeftBezierOffsets[3];

        float2 pointARBO = baseMeshPointRightBezierOffsets[0];
        float2 pointBRBO = baseMeshPointRightBezierOffsets[1];
        float2 pointCRBO = baseMeshPointRightBezierOffsets[2];
        float2 pointDRBO = baseMeshPointRightBezierOffsets[3];

        float2 pointATBO = baseMeshPointTopBezierOffsets[0];
        float2 pointBTBO = baseMeshPointTopBezierOffsets[1];
        float2 pointCTBO = baseMeshPointTopBezierOffsets[2];
        float2 pointDTBO = baseMeshPointTopBezierOffsets[3];

        float2 pointABBO = baseMeshPointBottomBezierOffsets[0];
        float2 pointBBBO = baseMeshPointBottomBezierOffsets[1];
        float2 pointCBBO = baseMeshPointBottomBezierOffsets[2];
        float2 pointDBBO = baseMeshPointBottomBezierOffsets[3];


        vec2 controlPointMatrix[4 * 4];

        controlPointMatrix[0 * 4 + 0] = pointAPos;
        controlPointMatrix[0 * 4 + 3] = pointBPos;
        controlPointMatrix[3 * 4 + 0] = pointCPos;
        controlPointMatrix[3 * 4 + 3] = pointDPos;

        controlPointMatrix[0 * 4 + 1] = pointAPos + pointARBO;
        controlPointMatrix[0 * 4 + 2] = pointBPos + pointBLBO;
        controlPointMatrix[3 * 4 + 1] = pointCPos + pointCRBO;
        controlPointMatrix[3 * 4 + 2] = pointDPos + pointDLBO;
        controlPointMatrix[1 * 4 + 0] = pointAPos + pointABBO;
        controlPointMatrix[2 * 4 + 0] = pointCPos + pointCTBO;
        controlPointMatrix[1 * 4 + 3] = pointBPos + pointBBBO;
        controlPointMatrix[2 * 4 + 3] = pointDPos + pointDTBO;

        controlPointMatrix[1 * 4 + 1] = controlPointMatrix[0 * 4 + 1] + controlPointMatrix[1 * 4 + 0] - controlPointMatrix[0 * 4 + 0];
        controlPointMatrix[1 * 4 + 2] = controlPointMatrix[0 * 4 + 2] + controlPointMatrix[1 * 4 + 3] - controlPointMatrix[0 * 4 + 3];
        controlPointMatrix[2 * 4 + 1] = controlPointMatrix[2 * 4 + 0] + controlPointMatrix[3 * 4 + 1] - controlPointMatrix[3 * 4 + 0];
        controlPointMatrix[2 * 4 + 2] = controlPointMatrix[2 * 4 + 3] + controlPointMatrix[3 * 4 + 2] - controlPointMatrix[3 * 4 + 3];

        
        float4 Tu = vec4(u*u*u, u*u, u, 1);
        float4 Tv = vec4(v*v*v, v*v, v, 1);
        vec4 TubM = Tu * bM;
        vec4 TvbM = Tv * bM;

        vec2 pT[4];
        for(int rowIdx = 0; rowIdx < 4; rowIdx++) {
            vec2 p0 = controlPointMatrix[rowIdx * 4 + 0];
            vec2 p1 = controlPointMatrix[rowIdx * 4 + 1];
            vec2 p2 = controlPointMatrix[rowIdx * 4 + 2];
            vec2 p3 = controlPointMatrix[rowIdx * 4 + 3];
            float4x4 G = float4x4(
                vec4(p0.x, p1.x, p2.x, p3.x),
                vec4(p0.y, p1.y, p2.y, p3.y),
                vec4(1.0, 1.0, 1.0, 1.0),
                vec4(1.0, 1.0, 1.0, 1.0)
            );
            pT[rowIdx] = (TubM * G).xy;
        }
        float4x4 G = float4x4(
            vec4(pT[0].x, pT[1].x, pT[2].x, pT[3].x),
            vec4(pT[0].y, pT[1].y, pT[2].y, pT[3].y),
            vec4(1.0, 1.0, 1.0, 1.0),
            vec4(1.0, 1.0, 1.0, 1.0)
        );

        vec2 finalPos = (TvbM * G).xy;

        Varyings varyings;
        varyings.position = finalPos;
        varyings.uv = uv;
        return varyings;
    }
    """

@Language("AGSL")
private val fragmentShaderSource =
    """
    uniform int useBicubicColorInterpolation;
    uniform float2 baseMeshPointLocations[4];
    uniform float4 baseMeshPointColors[16];
    uniform float2 baseMeshPointLeftBezierOffsets[4];
    uniform float2 baseMeshPointRightBezierOffsets[4];
    uniform float2 baseMeshPointTopBezierOffsets[4];
    uniform float2 baseMeshPointBottomBezierOffsets[4];
    
    const float4x4 bM = float4x4(
        vec4(0, -1, 2, -1),
        vec4(2, 0, -5, 3),
        vec4(0, 1, 4, -3),
        vec4(0, 0, -1, 1)
    );

    /** 
     *  From Bjorn's original blog introducing OkLab
     *  https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
     */
    vec3 oklab_to_linear_srgb(vec3 c) 
    {
        float l_ = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;
        float m_ = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;
        float s_ = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        return vec3(
        	    +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        	    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        	    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
            );
    }

    float2 main(const Varyings varyings, out float4 color) {
        float u = varyings.uv.x;
        float v = varyings.uv.y;

        if (useBicubicColorInterpolation == 0) {
            float4 pA = baseMeshPointColors[5];
            float4 pB = baseMeshPointColors[6];
            float4 pC = baseMeshPointColors[9];
            float4 pD = baseMeshPointColors[10];

            vec4 left = mix(pA, pC, v);
            vec4 right = mix(pB, pD, v);
            vec4 mixed = mix(left, right, u);
            vec3 linearSrgbColor = oklab_to_linear_srgb(mixed.xyz);
            linearSrgbColor = clamp(linearSrgbColor, 0.0, 1.0);
            color = vec4(linearSrgbColor * mixed.a, mixed.a);
        } else {
            vec4 Tu = vec4(1.0, u, u * u, u * u * u);
            vec4 Tv = vec4(1.0, v, v * v, v * v * v);
                
            vec4 TubM = 0.5 * Tu * bM;
            vec4 TvbM = 0.5 * Tv * bM;

            vec4 uInterp[4];
            for (int r = 0; r < 4; r++) {
                vec4 p0 = baseMeshPointColors[r * 4 + 0];
                vec4 p1 = baseMeshPointColors[r * 4 + 1];
                vec4 p2 = baseMeshPointColors[r * 4 + 2];
                vec4 p3 = baseMeshPointColors[r * 4 + 3];

                float4x4 G = float4x4(
                    vec4(p0.r, p1.r, p2.r, p3.r),
                    vec4(p0.g, p1.g, p2.g, p3.g),
                    vec4(p0.b, p1.b, p2.b, p3.b),
                    vec4(p0.a, p1.a, p2.a, p3.a)
                );
                uInterp[r] = TubM * G;
            }
            float4x4 G = float4x4(
                vec4(uInterp[0].r, uInterp[1].r, uInterp[2].r, uInterp[3].r),
                vec4(uInterp[0].g, uInterp[1].g, uInterp[2].g, uInterp[3].g),
                vec4(uInterp[0].b, uInterp[1].b, uInterp[2].b, uInterp[3].b),
                vec4(uInterp[0].a, uInterp[1].a, uInterp[2].a, uInterp[3].a)
            );
            vec4 interpolatedColor = TvbM * G;
            vec3 linearSrgbColor = oklab_to_linear_srgb(interpolatedColor.xyz);
            linearSrgbColor = clamp(linearSrgbColor, 0.0, 1.0);
            float alpha = clamp(interpolatedColor.a, 0.0, 1.0);
            color = vec4(linearSrgbColor * alpha, alpha);
        }
        return varyings.position;
    }
    """
