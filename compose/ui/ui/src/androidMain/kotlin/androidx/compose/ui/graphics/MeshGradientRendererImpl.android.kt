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

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * [MeshGradientRenderer] that uses [Canvas.drawVertices] to draw the gradient, which is hardware
 * accelerated from API 29 and above.
 */
@Suppress("PrimitiveInCollection")
internal class MeshGradientRendererImpl : MeshGradientRenderer {

    private val paint = android.graphics.Paint()

    private var indexBuffer: ShortArray? = null
    private var lastSubdivisionU: Int = -1
    private var lastSubdivisionV: Int = -1

    private var vBernsteinBasis: FloatArray? = null
    private var vCatmullRomBasis: FloatArray? = null
    private var forwardDifferenceRowResultsX: FloatArray? = null
    private var forwardDifferenceRowResultsY: FloatArray? = null
    private var colorForwardDifferenceRowResults: FloatArray? = null

    private var positionsBuffer: FloatArray? = null
    private var colorsBuffer: IntArray? = null

    private val patchPositions = FloatArray(8)
    private val patchLeftBezierOffsets = FloatArray(8)
    private val patchRightBezierOffsets = FloatArray(8)
    private val patchTopBezierOffsets = FloatArray(8)
    private val patchBottomBezierOffsets = FloatArray(8)
    private val patchColors = IntArray(16)
    private val okLabPatchColors = FloatArray(64)
    private val controlPoints = FloatArray(32)

    override fun DrawScope.draw(config: MeshGradientConfig) {
        val rows = config.rows
        val columns = config.columns
        val positions = config.positions
        val colors = config.colors
        val leftBezierOffsets = config.leftBezierOffsets
        val topBezierOffsets = config.topBezierOffsets
        val rightBezierOffsets = config.rightBezierOffsets
        val bottomBezierOffsets = config.bottomBezierOffsets
        val hasBicubicColor = config.hasBicubicColor

        val (subdivisionsU, subdivisionsV) = calculateSubdivisions(rows, columns, positions, size)
        val vertexCount = subdivisionsU * subdivisionsV

        if (
            indexBuffer == null ||
                lastSubdivisionU != subdivisionsU ||
                lastSubdivisionV != subdivisionsV
        ) {
            indexBuffer = ShortArray((subdivisionsU - 1) * (subdivisionsV - 1) * 6)
            forwardDifferenceRowResultsX = FloatArray(4 * subdivisionsU)
            forwardDifferenceRowResultsY = FloatArray(4 * subdivisionsU)
            colorForwardDifferenceRowResults = FloatArray(4 * subdivisionsU * 4)
            positionsBuffer = FloatArray(vertexCount * 2)
            colorsBuffer =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) IntArray(vertexCount)
                else IntArray(vertexCount * 2)
            buildIndexBuffer(subdivisionsU, subdivisionsV)
            precomputeBasisArrays(subdivisionsV)
            lastSubdivisionU = subdivisionsU
            lastSubdivisionV = subdivisionsV
        }

        val indices = indexBuffer!!
        // Holds the bezier surface vertex position data
        val surfacePositions = positionsBuffer!!
        // Holds the bezier surface vertex color data
        val surfaceColors = colorsBuffer!!

        for (patchIdx in 0 until rows * columns) {
            drawPatch(
                drawContext.canvas,
                patchIdx,
                rows,
                columns,
                hasBicubicColor,
                positions,
                colors,
                leftBezierOffsets,
                topBezierOffsets,
                rightBezierOffsets,
                bottomBezierOffsets,
                size,
                subdivisionsU,
                subdivisionsV,
                surfacePositions,
                surfaceColors,
                indices,
            )
        }
    }

    private fun drawPatch(
        canvas: Canvas,
        patchIdx: Int,
        rows: Int,
        columns: Int,
        hasBicubicColor: Boolean,
        positions: FloatArray,
        colors: IntArray,
        leftBezierOffsets: FloatArray?,
        topBezierOffsets: FloatArray?,
        rightBezierOffsets: FloatArray?,
        bottomBezierOffsets: FloatArray?,
        size: Size,
        subdivisionsU: Int,
        subdivisionsV: Int,
        surfacePositions: FloatArray,
        surfaceColors: IntArray,
        indices: ShortArray,
    ) {
        readPatchPositions(patchIdx, columns, positions, size, patchPositions)
        readPatchPositions(patchIdx, columns, leftBezierOffsets, size, patchLeftBezierOffsets)
        readPatchPositions(patchIdx, columns, rightBezierOffsets, size, patchRightBezierOffsets)
        readPatchPositions(patchIdx, columns, topBezierOffsets, size, patchTopBezierOffsets)
        readPatchPositions(patchIdx, columns, bottomBezierOffsets, size, patchBottomBezierOffsets)
        readPatchColors(patchIdx, rows, columns, colors, patchColors)

        buildControlPointMatrix(
            patchPositions,
            patchLeftBezierOffsets,
            patchRightBezierOffsets,
            patchTopBezierOffsets,
            patchBottomBezierOffsets,
            controlPoints,
        )
        computeBezierSurfacePoints(controlPoints, subdivisionsU, subdivisionsV, surfacePositions)

        if (hasBicubicColor) {
            computeCatmullRomSurfaceColors(patchColors, subdivisionsU, subdivisionsV, surfaceColors)
        } else {
            computeBilinearSurfaceColors(patchColors, subdivisionsU, subdivisionsV, surfaceColors)
        }

        canvas.nativeCanvas.drawVertices(
            android.graphics.Canvas.VertexMode.TRIANGLES,
            surfacePositions.size,
            surfacePositions,
            0,
            null,
            0,
            surfaceColors,
            0,
            indices,
            0,
            indices.size,
            paint,
        )
    }

    private fun buildControlPointMatrix(
        patchPositions: FloatArray,
        leftBezierOffsets: FloatArray,
        rightBezierOffsets: FloatArray,
        topBezierOffsets: FloatArray,
        bottomBezierOffsets: FloatArray,
        out: FloatArray,
    ) {
        // Helper to map 2D (row, col) to 1D index in the 4x4x2 controlPoints array
        fun idx(row: Int, col: Int, component: Int): Int = (row * 4 + col) * 2 + component

        // Corners
        out[idx(0, 0, 0)] = patchPositions[0]
        out[idx(0, 0, 1)] = patchPositions[1]
        out[idx(0, 3, 0)] = patchPositions[2]
        out[idx(0, 3, 1)] = patchPositions[3]
        out[idx(3, 0, 0)] = patchPositions[4]
        out[idx(3, 0, 1)] = patchPositions[5]
        out[idx(3, 3, 0)] = patchPositions[6]
        out[idx(3, 3, 1)] = patchPositions[7]

        // Horizontal Bezier Offsets
        out[idx(0, 1, 0)] = out[idx(0, 0, 0)] + rightBezierOffsets[0]
        out[idx(0, 1, 1)] = out[idx(0, 0, 1)] + rightBezierOffsets[1]
        out[idx(0, 2, 0)] = out[idx(0, 3, 0)] + leftBezierOffsets[2]
        out[idx(0, 2, 1)] = out[idx(0, 3, 1)] + leftBezierOffsets[3]
        out[idx(3, 1, 0)] = out[idx(3, 0, 0)] + rightBezierOffsets[4]
        out[idx(3, 1, 1)] = out[idx(3, 0, 1)] + rightBezierOffsets[5]
        out[idx(3, 2, 0)] = out[idx(3, 3, 0)] + leftBezierOffsets[6]
        out[idx(3, 2, 1)] = out[idx(3, 3, 1)] + leftBezierOffsets[7]

        // Vertical Bezier Offsets
        out[idx(1, 0, 0)] = out[idx(0, 0, 0)] + bottomBezierOffsets[0]
        out[idx(1, 0, 1)] = out[idx(0, 0, 1)] + bottomBezierOffsets[1]
        out[idx(2, 0, 0)] = out[idx(3, 0, 0)] + topBezierOffsets[4]
        out[idx(2, 0, 1)] = out[idx(3, 0, 1)] + topBezierOffsets[5]
        out[idx(1, 3, 0)] = out[idx(0, 3, 0)] + bottomBezierOffsets[2]
        out[idx(1, 3, 1)] = out[idx(0, 3, 1)] + bottomBezierOffsets[3]
        out[idx(2, 3, 0)] = out[idx(3, 3, 0)] + topBezierOffsets[6]
        out[idx(2, 3, 1)] = out[idx(3, 3, 1)] + topBezierOffsets[7]

        // Interior points with zero twist vectors
        out[idx(1, 1, 0)] = out[idx(0, 1, 0)] + out[idx(1, 0, 0)] - out[idx(0, 0, 0)]
        out[idx(1, 1, 1)] = out[idx(0, 1, 1)] + out[idx(1, 0, 1)] - out[idx(0, 0, 1)]
        out[idx(1, 2, 0)] = out[idx(0, 2, 0)] + out[idx(1, 3, 0)] - out[idx(0, 3, 0)]
        out[idx(1, 2, 1)] = out[idx(0, 2, 1)] + out[idx(1, 3, 1)] - out[idx(0, 3, 1)]
        out[idx(2, 1, 0)] = out[idx(2, 0, 0)] + out[idx(3, 1, 0)] - out[idx(3, 0, 0)]
        out[idx(2, 1, 1)] = out[idx(2, 0, 1)] + out[idx(3, 1, 1)] - out[idx(3, 0, 1)]
        out[idx(2, 2, 0)] = out[idx(2, 3, 0)] + out[idx(3, 2, 0)] - out[idx(3, 3, 0)]
        out[idx(2, 2, 1)] = out[idx(2, 3, 1)] + out[idx(3, 2, 1)] - out[idx(3, 3, 1)]
    }

    /**
     * Computes the vertex positions for a bicubic Bezier surface patch.
     *
     * This implementation uses the forward differencing technique to efficiently evaluate the cubic
     * polynomials.
     *
     * @param controlPoints The 4x4 grid of control points (32 floats: x, y for each).
     * @param subdivisionsU The number of horizontal subdivisions.
     * @param subdivisionsV The number of vertical subdivisions.
     * @param outPositions The output list to store the calculated [Offset] for each vertex.
     */
    private fun computeBezierSurfacePoints(
        controlPoints: FloatArray,
        subdivisionsU: Int,
        subdivisionsV: Int,
        outPositions: FloatArray,
    ) {
        val forwardDiffX = forwardDifferenceRowResultsX!!
        val forwardDiffY = forwardDifferenceRowResultsY!!
        val stepSize = 1f / (subdivisionsU - 1).toFloat()
        val stepSize2 = stepSize * stepSize
        val stepSize3 = stepSize2 * stepSize

        for (row in 0 until 4) {
            val base = row * 8

            val cubicTermX =
                (-controlPoints[base] + 3f * controlPoints[base + 2] -
                    3f * controlPoints[base + 4] + controlPoints[base + 6]) * stepSize3
            val quadraticTermX =
                (3f * controlPoints[base] - 6f * controlPoints[base + 2] +
                    3f * controlPoints[base + 4]) * stepSize2

            var forwardDiff1x =
                cubicTermX +
                    quadraticTermX +
                    (-3f * controlPoints[base] + 3f * controlPoints[base + 2]) * stepSize
            var forwardDiff2x = 6f * cubicTermX + 2f * quadraticTermX
            val forwardDiff3x = 6f * cubicTermX

            val cubicTermY =
                (-controlPoints[base + 1] + 3f * controlPoints[base + 3] -
                    3f * controlPoints[base + 5] + controlPoints[base + 7]) * stepSize3
            val quadraticTermY =
                (3f * controlPoints[base + 1] - 6f * controlPoints[base + 3] +
                    3f * controlPoints[base + 5]) * stepSize2

            var forwardDiff1y =
                cubicTermY +
                    quadraticTermY +
                    (-3f * controlPoints[base + 1] + 3f * controlPoints[base + 3]) * stepSize
            var forwardDiff2y = 6f * cubicTermY + 2f * quadraticTermY
            val forwardDiff3y = 6f * cubicTermY

            var currentX = controlPoints[base]
            var currentY = controlPoints[base + 1]
            val rowOffset = row * subdivisionsU
            forwardDiffX[rowOffset] = currentX
            forwardDiffY[rowOffset] = currentY

            for (uIndex in 1 until subdivisionsU) {
                currentX += forwardDiff1x
                forwardDiff1x += forwardDiff2x
                forwardDiff2x += forwardDiff3x
                currentY += forwardDiff1y
                forwardDiff1y += forwardDiff2y
                forwardDiff2y += forwardDiff3y
                forwardDiffX[rowOffset + uIndex] = currentX
                forwardDiffY[rowOffset + uIndex] = currentY
            }
        }

        val bernsteinBasis = vBernsteinBasis!!
        for (vIndex in 0 until subdivisionsV) {
            val vBase = vIndex * 4

            for (uIndex in 0 until subdivisionsU) {
                val outIdx = (uIndex * subdivisionsV + vIndex) * 2
                outPositions[outIdx] =
                    bernsteinBasis[vBase] * forwardDiffX[uIndex] +
                        bernsteinBasis[vBase + 1] * forwardDiffX[subdivisionsU + uIndex] +
                        bernsteinBasis[vBase + 2] * forwardDiffX[2 * subdivisionsU + uIndex] +
                        bernsteinBasis[vBase + 3] * forwardDiffX[3 * subdivisionsU + uIndex]
                outPositions[outIdx + 1] =
                    bernsteinBasis[vBase] * forwardDiffY[uIndex] +
                        bernsteinBasis[vBase + 1] * forwardDiffY[subdivisionsU + uIndex] +
                        bernsteinBasis[vBase + 2] * forwardDiffY[2 * subdivisionsU + uIndex] +
                        bernsteinBasis[vBase + 3] * forwardDiffY[3 * subdivisionsU + uIndex]
            }
        }
    }

    /**
     * Computes the colors for a patch using bicubic Catmull-Rom interpolation. This is used when
     * hasBicubicColor is true to provide smoother color transitions.
     *
     * This implementation uses the forward differencing algorithm to efficiently evaluate the
     * Catmull-Rom spline across the surface subdivisions.
     *
     * @param patchColors The 4x4 grid of colors surrounding and including the patch.
     * @param subdivisionsU The number of horizontal subdivisions.
     * @param subdivisionsV The number of vertical subdivisions.
     * @param outColors The output list to store the interpolated colors for each vertex.
     */
    private fun computeCatmullRomSurfaceColors(
        patchColors: IntArray,
        subdivisionsU: Int,
        subdivisionsV: Int,
        outColors: IntArray,
    ) {
        for (i in 0 until 16) {
            val color = Color(patchColors[i]).convert(ColorSpaces.Oklab)
            okLabPatchColors[i * 4] = color.red
            okLabPatchColors[i * 4 + 1] = color.green
            okLabPatchColors[i * 4 + 2] = color.blue
            okLabPatchColors[i * 4 + 3] = color.alpha
        }

        val forwardDiffColor = colorForwardDifferenceRowResults!!
        val stepSize = 1f / (subdivisionsU - 1).toFloat()
        val stepSize2 = stepSize * stepSize
        val stepSize3 = stepSize2 * stepSize

        for (row in 0 until 4) {
            val rowBase = row * 16
            for (channel in 0 until 4) {
                val cubicTerm =
                    0.5f *
                        (-okLabPatchColors[rowBase + channel] +
                            3f * okLabPatchColors[rowBase + 4 + channel] -
                            3f * okLabPatchColors[rowBase + 8 + channel] +
                            okLabPatchColors[rowBase + 12 + channel]) *
                        stepSize3
                val quadraticTerm =
                    0.5f *
                        (2f * okLabPatchColors[rowBase + channel] -
                            5f * okLabPatchColors[rowBase + 4 + channel] +
                            4f * okLabPatchColors[rowBase + 8 + channel] -
                            okLabPatchColors[rowBase + 12 + channel]) *
                        stepSize2

                var forwardDiff1Color =
                    cubicTerm +
                        quadraticTerm +
                        0.5f *
                            (-okLabPatchColors[rowBase + channel] +
                                okLabPatchColors[rowBase + 8 + channel]) *
                            stepSize
                var forwardDiff2Color = 6f * cubicTerm + 2f * quadraticTerm
                val forwardDiff3Color = 6f * cubicTerm

                var currentColorValue = okLabPatchColors[rowBase + 4 + channel]
                val rowOffset = row * subdivisionsU * 4

                val minValue = if (channel < 3) ColorSpaces.Oklab.getMinValue(channel) else 0f
                val maxValue = if (channel < 3) ColorSpaces.Oklab.getMaxValue(channel) else 1f

                forwardDiffColor[rowOffset + channel] =
                    currentColorValue.coerceIn(minValue, maxValue)

                for (uIndex in 1 until subdivisionsU) {
                    currentColorValue += forwardDiff1Color
                    forwardDiff1Color += forwardDiff2Color
                    forwardDiff2Color += forwardDiff3Color
                    forwardDiffColor[rowOffset + uIndex * 4 + channel] =
                        currentColorValue.coerceIn(minValue, maxValue)
                }
            }
        }

        val catmullRomBasis = vCatmullRomBasis!!
        for (uIndex in 0 until subdivisionsU) {
            val uBase0 = uIndex * 4
            val uBase1 = subdivisionsU * 4 + uIndex * 4
            val uBase2 = 2 * subdivisionsU * 4 + uIndex * 4
            val uBase3 = 3 * subdivisionsU * 4 + uIndex * 4

            for (vIndex in 0 until subdivisionsV) {
                val vBasisOffset = vIndex * 4

                val l =
                    (catmullRomBasis[vBasisOffset] * forwardDiffColor[uBase0] +
                        catmullRomBasis[vBasisOffset + 1] * forwardDiffColor[uBase1] +
                        catmullRomBasis[vBasisOffset + 2] * forwardDiffColor[uBase2] +
                        catmullRomBasis[vBasisOffset + 3] * forwardDiffColor[uBase3])
                val a =
                    (catmullRomBasis[vBasisOffset] * forwardDiffColor[uBase0 + 1] +
                        catmullRomBasis[vBasisOffset + 1] * forwardDiffColor[uBase1 + 1] +
                        catmullRomBasis[vBasisOffset + 2] * forwardDiffColor[uBase2 + 1] +
                        catmullRomBasis[vBasisOffset + 3] * forwardDiffColor[uBase3 + 1])
                val b =
                    (catmullRomBasis[vBasisOffset] * forwardDiffColor[uBase0 + 2] +
                        catmullRomBasis[vBasisOffset + 1] * forwardDiffColor[uBase1 + 2] +
                        catmullRomBasis[vBasisOffset + 2] * forwardDiffColor[uBase2 + 2] +
                        catmullRomBasis[vBasisOffset + 3] * forwardDiffColor[uBase3 + 2])
                val alpha =
                    (catmullRomBasis[vBasisOffset] * forwardDiffColor[uBase0 + 3] +
                        catmullRomBasis[vBasisOffset + 1] * forwardDiffColor[uBase1 + 3] +
                        catmullRomBasis[vBasisOffset + 2] * forwardDiffColor[uBase2 + 3] +
                        catmullRomBasis[vBasisOffset + 3] * forwardDiffColor[uBase3 + 3])

                outColors[uIndex * subdivisionsV + vIndex] =
                    Color(
                            red = l,
                            green = a,
                            blue = b,
                            alpha = alpha,
                            colorSpace = ColorSpaces.Oklab,
                        )
                        .convert(ColorSpaces.Srgb)
                        .toArgb()
            }
        }
    }

    /**
     * Computes the colors for a patch using bilinear interpolation. This is used when
     * hasBicubicColor is false.
     *
     * @param patchColors The 4x4 grid of colors forming the patch.
     * @param subdivisionsU The number of horizontal subdivisions.
     * @param subdivisionsV The number of vertical subdivisions.
     * @param outColors The output list to store the interpolated colors for each vertex.
     */
    private fun computeBilinearSurfaceColors(
        patchColors: IntArray,
        subdivisionsU: Int,
        subdivisionsV: Int,
        outColors: IntArray,
    ) {
        val subdivisionsUMinus1 = (subdivisionsU - 1).toFloat()
        val subdivisionsVMinus1 = (subdivisionsV - 1).toFloat()

        fun colorIdx(row: Int, col: Int): Int = (row * 4 + col)

        // Offsets for the 4 corners of the current patch inside the 4x4 RGBA matrix.
        // Reading them as Color type to perceptually interpolate between them by utilizing
        // Color.lerp api which converts these sRGB colors to OkLab space before interpolating.
        val topLeft = Color(patchColors[colorIdx(1, 1)])
        val topRight = Color(patchColors[colorIdx(1, 2)])
        val bottomLeft = Color(patchColors[colorIdx(2, 1)])
        val bottomRight = Color(patchColors[colorIdx(2, 2)])

        for (uIndex in 0 until subdivisionsU) {
            val u = uIndex / subdivisionsUMinus1
            val topLR = lerp(topLeft, topRight, u)
            val bottomLR = lerp(bottomLeft, bottomRight, u)
            for (vIndex in 0 until subdivisionsV) {
                val v = vIndex / subdivisionsVMinus1
                outColors[uIndex * subdivisionsV + vIndex] = lerp(topLR, bottomLR, v).toArgb()
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

    /**
     * Extracts the four corner positions of a specific patch from the global [inArray] and scales
     * them by the provided [size].
     *
     * @param patchIdx The index of the patch to read.
     * @param columns The number of columns in the mesh.
     * @param inArray The source array containing normalized (0-1) vertex positions.
     * @param size The dimensions to scale the normalized positions by.
     * @param out The output FloatArray to store the 8 coordinates (4 * 2).
     */
    private fun readPatchPositions(
        patchIdx: Int,
        columns: Int,
        inArray: FloatArray?,
        size: Size,
        out: FloatArray,
    ) {
        if (inArray == null) {
            for (i in out.indices) {
                out[i] = 0f
            }
            return
        }
        val patchRow = patchIdx / columns
        val patchColumn = patchIdx % columns
        val topLeft = getPointIndex(patchRow, patchColumn, columns) * 2
        val topRight = getPointIndex(patchRow, patchColumn + 1, columns) * 2
        val bottomLeft = getPointIndex(patchRow + 1, patchColumn, columns) * 2
        val bottomRight = getPointIndex(patchRow + 1, patchColumn + 1, columns) * 2
        out[0] = inArray[topLeft] * size.width
        out[1] = inArray[topLeft + 1] * size.height
        out[2] = inArray[topRight] * size.width
        out[3] = inArray[topRight + 1] * size.height
        out[4] = inArray[bottomLeft] * size.width
        out[5] = inArray[bottomLeft + 1] * size.height
        out[6] = inArray[bottomRight] * size.width
        out[7] = inArray[bottomRight + 1] * size.height
    }

    /**
     * Extracts a 4x4 grid of colors centered around a specific patch for bicubic interpolation.
     *
     * @param patchIdx The index of the patch to read.
     * @param rows The number of rows in the mesh.
     * @param columns The number of columns in the mesh.
     * @param colors The source array containing RGBA color components for each vertex.
     * @param out The output FloatArray to store the 64 color components (16 vertices * 4 channels).
     */
    private fun readPatchColors(
        patchIdx: Int,
        rows: Int,
        columns: Int,
        colors: IntArray,
        out: IntArray,
    ) {
        val patchRow = patchIdx / columns
        val patchColumn = patchIdx % columns
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val row = (patchRow - 1 + r).coerceIn(0, rows)
                val col = (patchColumn - 1 + c).coerceIn(0, columns)
                val writeIdx = (r * 4 + c)
                val readIdx = getPointIndex(row, col, columns)
                out[writeIdx] = colors[readIdx]
            }
        }
    }

    /** Builds the index buffer for a grid of triangles based on the number of subdivisions. */
    private fun buildIndexBuffer(subdivisionsU: Int, subdivisionsV: Int) {
        val indices = indexBuffer!!
        var idx = 0
        for (u in 0 until subdivisionsU - 1) {
            for (v in 0 until subdivisionsV - 1) {
                val topLeft = (u * subdivisionsV + v).toShort()
                val bottomLeft = (u * subdivisionsV + v + 1).toShort()
                val topRight = ((u + 1) * subdivisionsV + v).toShort()
                val bottomRight = ((u + 1) * subdivisionsV + v + 1).toShort()
                indices[idx++] = topLeft
                indices[idx++] = topRight
                indices[idx++] = bottomRight
                indices[idx++] = topLeft
                indices[idx++] = bottomRight
                indices[idx++] = bottomLeft
            }
        }
    }

    /**
     * Precomputes the Bernstein and Catmull-Rom basis matrices for the given number of
     * [subdivisionsV]. These arrays are used during surface interpolation to avoid redundant power
     * and multiplication operations for every vertex in every patch.
     */
    private fun precomputeBasisArrays(subdivisionsV: Int) {
        if (vBernsteinBasis == null || vBernsteinBasis!!.size != subdivisionsV * 4) {
            vBernsteinBasis = FloatArray(subdivisionsV * 4)
        }
        val bernsteinBasis = vBernsteinBasis!!
        val subdivisionsVMinus1 = (subdivisionsV - 1).toFloat()
        for (vIndex in 0 until subdivisionsV) {
            val v = vIndex / subdivisionsVMinus1
            val v2 = v * v
            val v3 = v2 * v
            val base = vIndex * 4
            bernsteinBasis[base] = -v3 + 3f * v2 - 3f * v + 1f
            bernsteinBasis[base + 1] = 3f * v3 - 6f * v2 + 3f * v
            bernsteinBasis[base + 2] = -3f * v3 + 3f * v2
            bernsteinBasis[base + 3] = v3
        }

        if (vCatmullRomBasis == null || vCatmullRomBasis!!.size != subdivisionsV * 4) {
            vCatmullRomBasis = FloatArray(subdivisionsV * 4)
        }
        val vCatmullRom = vCatmullRomBasis!!
        for (vIndex in 0 until subdivisionsV) {
            val v = vIndex / subdivisionsVMinus1
            val v2 = v * v
            val v3 = v2 * v
            val base = vIndex * 4
            vCatmullRom[base] = 0.5f * (-v3 + 2f * v2 - v)
            vCatmullRom[base + 1] = 0.5f * (3f * v3 - 5f * v2 + 2f)
            vCatmullRom[base + 2] = 0.5f * (-3f * v3 + 4f * v2 + v)
            vCatmullRom[base + 3] = 0.5f * (v3 - v2)
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
