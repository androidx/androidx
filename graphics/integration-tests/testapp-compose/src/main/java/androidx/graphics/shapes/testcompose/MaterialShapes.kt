/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import android.annotation.SuppressLint
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.FeatureSerializer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.SvgPathParser
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.rectangle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Cache various roundings for use below
private val cornerRound20 = CornerRounding(.2f)
private val cornerRound50 = CornerRounding(.5f)
private val cornerRound100 = CornerRounding(1f)

private fun RoundedPolygon.rotated(angle: Float) = transformed(Matrix().apply { rotateZ(angle) })

private val unrounded = CornerRounding.Unrounded

@SuppressLint("PrimitiveInCollection")
fun materialShapes() =
    listOf(
        /**
         * ************************************************************************************
         * Line1
         * *************************************************************************************
         */
        ShapeParameters(
            "Circle",
            sides = 8,
            roundness = 1f,
            shapeId = ShapeParameters.ShapeId.Circle,
        ),
        ShapeParameters(
            "Square",
            sides = 4,
            roundness = 0.3f,
            rotation = 45f,
            shapeId = ShapeParameters.ShapeId.Polygon,
        ),
        CustomShapeParameters("Slanted") {
            RoundedPolygon(4, rounding = CornerRounding(0.2f, 0.5f)).rotated(45f).transformed { x, y
                ->
                TransformResult(x - 0.15f * y, y) // Compose's matrix doesn't support skew!?
            }
        },
        CustomShapeParameters("Dome") {
            RoundedPolygon(
                    4,
                    perVertexRounding =
                        listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20),
                )
                .rotated(-135f)
        },
        CustomShapeParameters("Fan") {
            RoundedPolygon(
                    4,
                    perVertexRounding =
                        listOf(cornerRound100, cornerRound20, cornerRound20, cornerRound20),
                )
                .rotated(-45f)
        },
        ShapeParameters(
            "Arrow",
            innerRadius = 0.1f,
            roundness = 0.22f,
            shapeId = ShapeParameters.ShapeId.Triangle,
        ),
        CustomShapeParameters("Semicircle") {
            snapshotFlow {}
            RoundedPolygon.rectangle(
                width = 1.8f,
                height = 1f,
                perVertexRounding =
                    listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100),
            )
        },

        /**
         * ************************************************************************************
         * Line2
         * *************************************************************************************
         */
        ShapeParameters(
            "Oval",
            sides = 8,
            roundness = 1f,
            width = 1.8f,
            rotation = -45f,
            shapeId = ShapeParameters.ShapeId.Circle,
        ),
        ShapeParameters(
            "Pill",
            width = 1f,
            height = 1.25f,
            rotation = 45f,
            shapeId = ShapeParameters.ShapeId.Pill,
        ),
        ShapeParameters(
            "Triangle",
            sides = 3,
            roundness = .2f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Polygon,
        ),
        CustomShapeParameters("Diamond") {
            RoundedPolygon(4, rounding = CornerRounding(0.3f))
                .transformed(Matrix().apply { scale(y = 1.2f) })
        },
        CustomShapeParameters("Hexagon") {
            val cornerInset = .6f
            val edgeInset = .4f
            val height = .65f
            val hexPoints =
                floatArrayOf(
                    1f,
                    0f,
                    cornerInset,
                    height,
                    edgeInset,
                    height,
                    -edgeInset,
                    height,
                    -cornerInset,
                    height,
                    -1f,
                    0f,
                    -cornerInset,
                    -height,
                    -edgeInset,
                    -height,
                    edgeInset,
                    -height,
                    cornerInset,
                    -height,
                )
            val pvRounding =
                listOf(
                    cornerRound50,
                    cornerRound50,
                    unrounded,
                    unrounded,
                    cornerRound50,
                    cornerRound50,
                    cornerRound50,
                    unrounded,
                    unrounded,
                    cornerRound50,
                )
            RoundedPolygon(hexPoints, perVertexRounding = pvRounding)
        },
        ShapeParameters("Pentagon", sides = 5, roundness = 0.5f, rotation = -360f / 20),
        CustomShapeParameters("Gem") {
            // irregular hexagon (right narrower than left, then rotated)
            // First, generate a standard hexagon
            val numVertices = 6
            val radius = 1f
            val points = FloatArray(numVertices * 2)
            var index = 0
            for (i in 0 until numVertices) {
                val vertex = radialToCartesian(radius, (PI.toFloat() / numVertices * 2 * i))
                points[index++] = vertex.x
                points[index++] = vertex.y
            }
            // Now adjust-in the points at the top (next-to-last and second vertices, post rotation)
            points[2] -= .1f
            points[3] -= .1f
            points[10] -= .1f
            points[11] += .1f
            RoundedPolygon(points, cornerRound50).rotated(-90f)
        },

        /**
         * ************************************************************************************
         * Line3
         * *************************************************************************************
         */
        ShapeParameters(
            "Very Sunny",
            sides = 8,
            innerRadius = 0.65f,
            roundness = 0.15f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "Sunny",
            sides = 8,
            innerRadius = 0.83f,
            roundness = 0.15f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "4-Sided Cookie",
            sides = 4,
            innerRadius = 0.5f,
            roundness = 0.3f,
            rotation = -45f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "6-Sided Cookie",
            sides = 6,
            innerRadius = 0.75f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "7-Sided Cookie",
            sides = 7,
            innerRadius = 0.75f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "9-Sided Cookie",
            sides = 9,
            innerRadius = 0.75f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "12-Sided Cookie",
            sides = 12,
            innerRadius = 0.8f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),

        /**
         * ************************************************************************************
         * Line4
         * *************************************************************************************
         */
        CustomShapeParameters("Ghost-ish") {
            val w = .88f
            val points = floatArrayOf(1f, w, -1f, w, -0.5f, 0f, -1f, -w, 1f, -w)
            val pvRounding =
                listOf(cornerRound100, cornerRound50, cornerRound100, cornerRound50, cornerRound100)
            RoundedPolygon(points, perVertexRounding = pvRounding).rotated(-90f)
        },
        ShapeParameters(
            "4-Leaf clover",
            sides = 4,
            innerRadius = 0.2f,
            roundness = 0.4f,
            innerRoundness = 0f,
            rotation = -45f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "8-Leaf clover",
            sides = 8,
            innerRadius = 0.65f,
            roundness = 0.3f,
            innerRoundness = 0f,
            rotation = 360f / 16,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "Burst",
            sides = 12,
            innerRadius = 0.7f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "Soft burst",
            sides = 12,
            innerRadius = 0.7f,
            roundness = 0.085f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        ShapeParameters(
            "Boom",
            sides = 15,
            innerRadius = 0.42f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        CustomShapeParameters("Soft Bloom") {
            val points =
                arrayOf(
                    Offset(0.456f, 0.224f),
                    Offset(0.460f, 0.170f),
                    Offset(0.500f, 0.100f),
                    Offset(0.540f, 0.170f),
                    Offset(0.544f, 0.224f),
                    Offset(0.538f, 0.308f),
                )
            val actualPoints = doRepeat(points, 16, center = Offset(0.5f, 0.5f))
            val roundings =
                listOf(
                        CornerRounding(0.020f, 0.000f),
                        CornerRounding(0.143f, 0.000f),
                        CornerRounding(0.025f, 0.000f),
                        CornerRounding(0.143f, 0.000f),
                        CornerRounding(0.190f, 0.000f),
                        CornerRounding(0.000f, 0.000f),
                    )
                    .let { l -> (0 until 16).flatMap { l } }
            RoundedPolygon(
                actualPoints,
                perVertexRounding = roundings,
                centerX = 0.5f,
                centerY = 0.5f,
            )
        },

        /**
         * ************************************************************************************
         * Line5
         * *************************************************************************************
         */
        ShapeParameters(
            "Flower",
            sides = 8,
            innerRadius = 0.575f,
            roundness = 0.13f,
            smooth = 0.95f,
            innerRoundness = 0f,
            shapeId = ShapeParameters.ShapeId.Star,
        ),
        CustomShapeParameters("Puffy") {
            val pnr =
                listOf(
                    PointNRound(Offset(0.500f, 0.260f), CornerRounding.Unrounded),
                    PointNRound(Offset(0.526f, 0.188f), CornerRounding(0.095f)),
                    PointNRound(Offset(0.676f, 0.226f), CornerRounding(0.095f)),
                    PointNRound(Offset(0.660f, 0.300f), CornerRounding.Unrounded),
                    PointNRound(Offset(0.734f, 0.230f), CornerRounding(0.095f)),
                    PointNRound(Offset(0.838f, 0.350f), CornerRounding(0.095f)),
                    PointNRound(Offset(0.782f, 0.418f), CornerRounding.Unrounded),
                    PointNRound(Offset(0.874f, 0.414f), CornerRounding(0.095f)),
                )
            val actualPoints = doRepeat(pnr, 4, center = Offset(0.5f, 0.5f), mirroring = true)
            RoundedPolygon(
                actualPoints.flatMap { listOf(it.o.x, it.o.y) }.toFloatArray(),
                perVertexRounding = actualPoints.map { it.r },
                centerX = 0.5f,
                centerY = 0.5f,
            )
        },
        CustomShapeParameters("Puffy Diamond") {
            val points =
                arrayOf(
                    Offset(0.390f, 0.260f),
                    Offset(0.390f, 0.130f),
                    Offset(0.610f, 0.130f),
                    Offset(0.610f, 0.260f),
                    Offset(0.740f, 0.260f),
                )
            val actualPoints = doRepeat(points, 4, center = Offset(0.5f, 0.5f))
            val roundings =
                listOf(
                        CornerRounding(0.000f, 0.000f),
                        CornerRounding(0.104f, 0.000f),
                        CornerRounding(0.104f, 0.000f),
                        CornerRounding(0.000f, 0.000f),
                        CornerRounding(0.104f, 0.000f),
                    )
                    .let { l -> (0 until 4).flatMap { l } }
            RoundedPolygon(
                actualPoints,
                perVertexRounding = roundings,
                centerX = 0.5f,
                centerY = 0.5f,
            )
        },
        CustomShapeParameters("Pixel circle") {
            val pixelSize = .1f
            val points =
                floatArrayOf(
                    // BR quadrant
                    6 * pixelSize,
                    0 * pixelSize,
                    6 * pixelSize,
                    2 * pixelSize,
                    5 * pixelSize,
                    2 * pixelSize,
                    5 * pixelSize,
                    4 * pixelSize,
                    4 * pixelSize,
                    4 * pixelSize,
                    4 * pixelSize,
                    5 * pixelSize,
                    2 * pixelSize,
                    5 * pixelSize,
                    2 * pixelSize,
                    6 * pixelSize,

                    // BL quadrant+
                    -2 * pixelSize,
                    6 * pixelSize,
                    -2 * pixelSize,
                    5 * pixelSize,
                    -4 * pixelSize,
                    5 * pixelSize,
                    -4 * pixelSize,
                    4 * pixelSize,
                    -5 * pixelSize,
                    4 * pixelSize,
                    -5 * pixelSize,
                    2 * pixelSize,
                    -6 * pixelSize,
                    2 * pixelSize,
                    -6 * pixelSize,
                    0 * pixelSize,

                    // TL quadrant
                    -6 * pixelSize,
                    -2 * pixelSize,
                    -5 * pixelSize,
                    -2 * pixelSize,
                    -5 * pixelSize,
                    -4 * pixelSize,
                    -4 * pixelSize,
                    -4 * pixelSize,
                    -4 * pixelSize,
                    -5 * pixelSize,
                    -2 * pixelSize,
                    -5 * pixelSize,
                    -2 * pixelSize,
                    -6 * pixelSize,

                    // TR quadrant
                    2 * pixelSize,
                    -6 * pixelSize,
                    2 * pixelSize,
                    -5 * pixelSize,
                    4 * pixelSize,
                    -5 * pixelSize,
                    4 * pixelSize,
                    -4 * pixelSize,
                    5 * pixelSize,
                    -4 * pixelSize,
                    5 * pixelSize,
                    -2 * pixelSize,
                    6 * pixelSize,
                    -2 * pixelSize,
                )
            RoundedPolygon(points)
        },
        CustomShapeParameters("Pixel triangle") {
            var point = Offset(0f, 0f)
            val points = mutableListOf<Offset>()
            points.add(point)
            val sizes = listOf(56f, 28f, 44f, 26f, 44f, 32f, 38f, 26f, 38f, 32f)
            sizes.chunked(2).forEach { (dx, dy) ->
                point += Offset(dx, 0f)
                points.add(point)
                point += Offset(0f, dy)
                points.add(point)
            }
            point += Offset(32f, 0f)
            points.add(point)
            point += Offset(0f, 38f)
            points.add(point)
            point += Offset(-32f, 0f)
            points.add(point)
            sizes.reversed().chunked(2).forEach { (dy, dx) ->
                point += Offset(0f, dy)
                points.add(point)
                point += Offset(-dx, 0f)
                points.add(point)
            }
            val centerX = points.maxOf { it.x } / 2
            val centerY = points.maxOf { it.y } / 2
            RoundedPolygon(
                    points.flatMap { listOf(it.x, it.y) }.toFloatArray(),
                    centerX = centerX,
                    centerY = centerY,
                )
                .normalized()
        },
        CustomShapeParameters("DoublePill") {
            // Sandwich cookie - basically, two pills stacked on each other
            val inset = .4f
            val sandwichPoints =
                floatArrayOf(
                    1f,
                    1f,
                    inset,
                    1f,
                    -inset,
                    1f,
                    -1f,
                    1f,
                    -1f,
                    0f,
                    -inset,
                    0f,
                    -1f,
                    0f,
                    -1f,
                    -1f,
                    -inset,
                    -1f,
                    inset,
                    -1f,
                    1f,
                    -1f,
                    1f,
                    0f,
                    inset,
                    0f,
                    1f,
                    0f,
                )
            val pvRounding =
                listOf(
                    cornerRound100,
                    unrounded,
                    unrounded,
                    cornerRound100,
                    cornerRound100,
                    unrounded,
                    cornerRound100,
                    cornerRound100,
                    unrounded,
                    unrounded,
                    cornerRound100,
                    cornerRound100,
                    unrounded,
                    cornerRound100,
                )
            RoundedPolygon(sandwichPoints, perVertexRounding = pvRounding)
        },
        CustomShapeParameters("Heart") {
            // Heart
            val points =
                floatArrayOf(
                    .2f,
                    0f,
                    -.4f,
                    .5f,
                    -1f,
                    1f,
                    -1.5f,
                    .5f,
                    -1f,
                    0f,
                    -1.5f,
                    -.5f,
                    -1f,
                    -1f,
                    -.4f,
                    -.5f,
                )
            val pvRounding =
                listOf(
                    unrounded,
                    unrounded,
                    cornerRound100,
                    cornerRound100,
                    unrounded,
                    cornerRound100,
                    cornerRound100,
                    unrounded,
                )
            RoundedPolygon(points, perVertexRounding = pvRounding).transformed { x, y ->
                TransformResult(-y, x)
            }
        },
        /**
         * ************************************************************************************
         * Line6 This is a developer app and the following imports are not optimized for production
         * use. Refer to [FeatureSerializer] for proper usage of svg path import.
         * *************************************************************************************
         */
        CustomShapeParameters("Android") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 31.87 -235.46 q 9.19 -108.96 66.91 -200.61 q 57.72 -91.65 153.15 -145.6 L 176.5 -711.83 q -6 -9.24 -2.88 -19.35 q 3.12 -10.12 13.12 -15.36 q 8.48 -5.24 18.57 -2.15 q 10.09 3.1 16.15 12.39 l 75.19 130.39 q 87.44 -36.72 183.35 -36.72 t 183.35 36.72 l 75.1 -130.46 q 6.09 -9.17 16.21 -12.29 q 10.12 -3.12 18.6 2.12 q 10 5.24 13.12 15.36 q 3.12 10.11 -2.88 19.35 l -75.43 130.16 q 95.43 53.95 153.15 145.6 q 57.72 91.65 66.91 200.61 H 31.87 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
                .normalized()
        },
        CustomShapeParameters("Bolt") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 383.08 -380 H 261.39 q -20.16 0 -29.54 -17.85 q -9.39 -17.84 2.31 -34.38 L 496.23 -810.3 q 8.08 -11.31 20.62 -15.47 q 12.53 -4.15 26.07 0.7 q 13.54 4.84 19.81 16.57 t 4.65 25.27 L 537.31 -540 h 149.23 q 21.38 0 30.15 19.16 q 8.77 19.15 -5.15 35.69 L 421.77 -138.08 q -8.69 10.31 -21.23 13.35 q -12.54 3.03 -24.46 -2.43 q -11.92 -5.46 -18.5 -16.69 t -4.96 -24.77 L 383.08 -380 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Cloud") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 260.72 -151.87 q -94.35 0 -161.24 -65.15 q -66.89 -65.15 -66.89 -159.26 q 0 -80.39 47.36 -143.67 q 47.35 -63.27 125.03 -80.75 q 26.43 -92.95 102.63 -150.19 Q 383.8 -808.13 480 -808.13 q 118.2 0 201.25 81.74 t 85.68 199.69 q 69.72 10.63 115.1 63.81 q 45.38 53.17 45.38 123.61 q 0 78.11 -54.65 132.76 q -54.65 54.65 -132.76 54.65 H 260.72 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Emergency") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 402.11 -192.39 v -151.98 l -132.13 76.11 q -27.87 15.67 -58.98 7.84 q -31.11 -7.84 -47.02 -36.71 q -15.68 -27.87 -7.72 -58.98 q 7.96 -31.11 35.83 -47.02 L 324.22 -480 l -131.89 -75.87 q -27.87 -15.67 -35.95 -47.16 q -8.08 -31.49 7.6 -59.36 q 15.67 -27.87 47.02 -35.83 q 31.35 -7.95 59.22 7.72 l 131.89 75.87 v -152.98 q 0 -32.35 22.77 -55.12 Q 447.65 -845.5 480 -845.5 q 32.35 0 55.12 22.77 q 22.77 22.77 22.77 55.12 v 152.98 l 132.13 -76.11 q 27.87 -15.67 58.98 -7.72 q 31.11 7.96 46.78 35.83 q 15.92 28.11 7.58 59.6 q -8.34 31.49 -36.45 47.16 L 635.78 -480 l 132.13 76.87 q 27.87 15.67 35.83 46.9 q 7.96 31.23 -7.96 59.86 q -15.67 27.87 -46.78 35.83 q -31.11 7.95 -58.98 -7.72 l -132.13 -76.11 v 151.98 q 0 32.35 -22.77 55.12 Q 512.35 -114.5 480 -114.5 q -32.35 0 -55.12 -22.77 q -22.77 -22.77 -22.77 -55.12 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Comic Bubble") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 838.04 -121.96 q -9.43 9.44 -23.72 12.68 q -14.3 3.24 -28.97 -4.72 L 557.67 -229.96 l -85.6 85.61 q -13.68 13.68 -32.07 13.68 t -32.07 -13.68 l -87.28 -87.52 H 197.37 q -19.15 0 -32.33 -13.17 q -13.17 -13.18 -13.17 -32.33 v -123.28 l -87.52 -87.28 Q 50.67 -501.61 50.67 -520 t 13.68 -32.07 l 87.52 -87.28 v -123.28 q 0 -19.15 13.17 -32.33 q 13.18 -13.17 32.33 -13.17 h 123.28 l 87.28 -87.52 q 13.68 -13.68 32.07 -13.68 t 32.07 13.68 l 87.28 87.52 h 123.28 q 19.15 0 32.33 13.17 q 13.17 13.18 13.17 32.33 v 123.28 l 87.52 87.28 q 13.68 13.68 13.68 32.07 t -13.68 32.07 l -85.61 85.6 L 846 -174.65 q 7.96 14.67 4.72 28.97 q -3.24 14.29 -12.68 23.72 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Pet") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 380 -71.87 q -61.15 0 -104.4 -41.46 q -43.25 -41.45 -43.25 -102.6 q 0 -7.33 2.02 -11.37 q 2.02 -4.05 -0.5 -6.57 q -2.52 -2.52 -6.57 -0.5 q -4.04 2.02 -11.37 2.02 q -61.15 0 -102.6 -43.25 Q 71.87 -318.85 71.87 -380 q 0 -62.11 43.01 -105.12 q 43.01 -43.01 105.12 -43.01 q 22.04 0 40.8 5.64 q 18.77 5.64 35.77 16.69 L 454.2 -663.43 q -11.05 -17 -16.69 -35.77 q -5.64 -18.76 -5.64 -40.8 q 0 -62.11 43.01 -105.12 q 43.01 -43.01 105.12 -43.01 q 61.15 0 104.4 41.46 q 43.25 41.45 43.25 102.6 q 0 7.33 -2.02 11.37 q -2.02 4.05 0.5 6.57 q 2.52 2.52 6.57 0.5 q 4.04 -2.02 11.37 -2.02 q 61.15 0 102.6 43.25 q 41.46 43.25 41.46 104.4 q 0 62.11 -43.01 105.12 q -43.01 43.01 -105.12 43.01 q -22.04 0 -40.8 -5.64 q -18.77 -5.64 -35.77 -16.69 L 505.8 -296.57 q 11.05 17 16.69 35.77 q 5.64 18.76 5.64 40.8 q 0 62.11 -43.01 105.12 Q 442.11 -71.87 380 -71.87 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Home") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 180 -200 v -353.85 q 0 -17.17 7.68 -32.53 q 7.69 -15.37 21.24 -25.31 l 227.7 -171.54 q 18.95 -14.46 43.32 -14.46 t 43.44 14.46 l 227.7 171.54 q 13.55 9.94 21.24 25.31 q 7.68 15.36 7.68 32.53 V -200 q 0 24.54 -17.73 42.27 Q 744.54 -140 720 -140 H 592.31 q -15.37 0 -25.76 -10.4 q -10.4 -10.39 -10.4 -25.76 v -195.38 q 0 -15.36 -10.39 -25.76 q -10.39 -10.39 -25.76 -10.39 h -80 q -15.37 0 -25.76 10.39 q -10.39 10.4 -10.39 25.76 v 195.38 q 0 15.37 -10.4 25.76 q -10.39 10.4 -25.76 10.4 H 240 q -24.54 0 -42.27 -17.73 Q 180 -175.46 180 -200 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        /**
         * ************************************************************************************
         * Line7 This is a developer app and the following imports are not optimized for production
         * use. Refer to [FeatureSerializer] for proper usage of svg path import.
         * *************************************************************************************
         */
        CustomShapeParameters("Park") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 397.93 -231.87 H 190.22 q -27.35 0 -40.79 -23.87 q -13.43 -23.87 2.24 -46.74 l 113.26 -169.39 h -2.28 q -24.48 0 -36.1 -21.74 q -11.62 -21.74 2.62 -41.98 l 213.29 -304.82 q 6.95 -9.44 17.03 -14.42 q 10.08 -4.97 20.51 -4.97 q 10.43 0 20.51 4.97 q 10.08 4.98 17.03 14.42 l 213.29 304.82 q 14.24 20.24 2.62 41.98 q -11.62 21.74 -36.1 21.74 h -2.28 l 113.26 169.39 q 15.67 22.87 2.24 46.74 q -13.44 23.87 -40.79 23.87 H 562.07 v 114.5 q 0 19.15 -13.18 32.33 q -13.17 13.17 -32.32 13.17 h -73.14 q -19.15 0 -32.32 -13.17 q -13.18 -13.18 -13.18 -32.33 v -114.5 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Bedtime") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 484 -75.22 q -84.96 0 -159.29 -32.36 q -74.34 -32.35 -129.56 -87.57 q -55.22 -55.22 -87.57 -129.56 Q 75.22 -399.04 75.22 -484 q 0 -126.8 70.44 -230.57 q 70.45 -103.76 189.06 -148.86 q 26.06 -10.87 49.49 4.54 q 23.42 15.41 23.62 43.43 q -0.61 82.61 28.79 157.7 q 29.4 75.09 87.73 133.41 q 58.32 58.33 133.29 87.61 t 157.34 28.91 q 30.06 0.44 45.31 22.41 q 15.25 21.96 5.34 47.83 q -45.91 119.05 -149.77 190.71 Q 612 -75.22 484 -75.22 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Send") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 176.24 -178.7 q -22.87 9.44 -43.26 -3.85 q -20.39 -13.3 -20.39 -38.17 V -393.3 l 331 -86.7 l -331 -86.7 v -172.58 q 0 -24.87 20.39 -38.17 q 20.39 -13.29 43.26 -3.85 l 613.61 259.28 q 28.11 12.43 28.11 42.02 q 0 29.59 -28.11 42.02 L 176.24 -178.7 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Humidity") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 480 -91.87 q -136.11 0 -232.12 -94.15 q -96.01 -94.15 -96.01 -229.5 q 0 -64.91 25.1 -124.09 q 25.1 -59.17 71.53 -104.37 l 167.85 -164.85 q 13.67 -12.67 30.08 -19.39 q 16.42 -6.71 33.57 -6.71 t 33.57 6.71 q 16.41 6.72 30.08 19.39 L 711.5 -643.98 q 46.43 45.2 71.53 104.37 q 25.1 59.18 25.1 124.09 q 0 135.35 -96.01 229.5 T 480 -91.87 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Pan") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 402 -32.11 q -30.72 0 -57.55 -13.86 q -26.84 -13.86 -44.08 -39.57 L 65.8 -437.04 q -8 -12.48 -6.64 -26.84 q 1.36 -14.36 12.84 -24.36 q 20.2 -19 47.27 -22.12 q 27.08 -3.12 51.43 13.79 l 106.19 73.59 v -381.56 q 0 -18.2 12.46 -30.66 q 12.45 -12.45 30.65 -12.45 t 30.65 12.45 q 12.46 12.46 12.46 30.66 v 327.65 h 75.46 v -409.57 q 0 -18.19 12.45 -30.65 q 12.46 -12.46 30.65 -12.46 q 18.2 0 30.66 12.46 q 12.45 12.46 12.45 30.65 v 409.57 h 75.46 v -368.13 q 0 -18.2 12.46 -30.65 q 12.45 -12.46 30.65 -12.46 q 18.19 0 30.65 12.46 q 12.46 12.45 12.46 30.65 v 368.13 h 75.45 v -288.13 q 0 -18.2 12.46 -30.65 q 12.46 -12.46 30.65 -12.46 q 18.2 0 30.65 12.46 q 12.46 12.45 12.46 30.65 v 569.8 q 0 67.44 -47.84 115.27 q -47.83 47.84 -115.27 47.84 H 402 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Eco") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 451.2 -72.35 q -33.48 0 -67.22 -7.26 t -68.26 -21.02 q 9.85 -123.15 68.08 -230.42 Q 442.04 -438.33 534 -520.48 q -111.43 56.96 -192.05 151.23 q -80.62 94.27 -115.02 213.95 q -4.23 -3.48 -8.21 -7.22 q -3.98 -3.74 -8.22 -7.98 q -47.96 -47.96 -73.29 -107.75 q -25.34 -59.79 -25.34 -124.47 q 0 -69.43 27.6 -132.39 q 27.6 -62.96 76.55 -111.91 Q 286.5 -717.5 390 -746.34 q 103.5 -28.83 276.39 -22.03 q 29.59 1 54.7 12.7 q 25.11 11.69 44.5 31.08 t 30.7 45 q 11.32 25.61 13.32 55.2 q 5.28 175.46 -22.68 277.05 q -27.95 101.6 -97.43 171.32 q -49.72 49.95 -111.29 76.81 q -61.58 26.86 -127.01 26.86 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
        CustomShapeParameters("Flight") {
            val svgPathNotSuitableForProductionCodeImport =
                "M 394.26 -400.83 L 145.09 -301.5 q -26.39 10.96 -49.81 -4.98 q -23.41 -15.93 -23.41 -44.32 v -23.92 q 0 -13.19 5.98 -25.03 q 5.98 -11.84 16.93 -19.55 l 299.48 -209 v -174.09 q 0 -35.39 25.17 -60.57 q 25.18 -25.17 60.57 -25.17 t 60.57 25.17 q 25.17 25.18 25.17 60.57 v 174.09 l 299.48 209 q 10.95 7.71 16.93 19.55 t 5.98 25.03 v 23.92 q 0 28.39 -23.41 44.32 q -23.42 15.94 -49.81 4.98 l -249.17 -99.33 v 134.44 l 101.8 70.8 q 8.96 6.48 14.06 16.18 q 5.1 9.69 5.1 20.65 v 26.15 q 0 22.15 -18.18 35.85 q -18.17 13.69 -40.32 7.22 L 480 -133.78 L 331.8 -89.54 q -22.15 6.47 -40.32 -7.22 q -18.18 -13.7 -18.18 -35.85 v -26.15 q 0 -10.96 5.1 -20.65 q 5.1 -9.7 14.06 -16.18 l 101.8 -70.8 v -134.44 Z"
            RoundedPolygon(SvgPathParser.parseFeatures(svgPathNotSuitableForProductionCodeImport))
                .normalized()
        },
    )

internal fun doRepeat(points: Array<Offset>, reps: Int, center: Offset) =
    points.size.let { np ->
        (0 until np * reps)
            .flatMap {
                val point = points[it % np].rotateDegrees((it / np) * 360f / reps, center)
                listOf(point.x, point.y)
            }
            .toFloatArray()
    }

internal fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero) =
    (angle.toRadians()).let { a ->
        val off = this - center
        Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
    }

internal fun Offset.angleDegrees() = atan2(y, x) * 180f / PI.toFloat()

internal data class PointNRound(val o: Offset, val r: CornerRounding)

@SuppressLint("PrimitiveInCollection")
internal fun doRepeat(points: List<PointNRound>, reps: Int, center: Offset, mirroring: Boolean) =
    if (mirroring) {
        buildList {
            val angles = points.map { (it.o - center).angleDegrees() }
            val distances = points.map { (it.o - center).getDistance() }
            val sectionAngle = 360f / reps
            repeat(reps) {
                points.indices.forEach { index ->
                    val i = if (it % 2 == 0) index else points.lastIndex - index
                    if (i > 0 || it % 2 == 0) {
                        val a =
                            (sectionAngle * it +
                                    if (it % 2 == 0) angles[i]
                                    else sectionAngle - angles[i] + 2 * angles[0])
                                .toRadians()
                        val finalPoint = Offset(cos(a), sin(a)) * distances[i] + center
                        add(PointNRound(finalPoint, points[i].r))
                    }
                }
            }
        }
    } else {
        points.size.let { np ->
            (0 until np * reps).map {
                val point = points[it % np].o.rotateDegrees((it / np) * 360f / reps, center)
                PointNRound(point, points[it % np].r)
            }
        }
    }
