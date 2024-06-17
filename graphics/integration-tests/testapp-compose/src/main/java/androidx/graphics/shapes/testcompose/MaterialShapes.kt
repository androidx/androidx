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
import androidx.graphics.shapes.RoundedPolygon
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
            shapeId = ShapeParameters.ShapeId.Polygon
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
                        listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20)
                )
                .rotated(-135f)
        },
        CustomShapeParameters("Fan") {
            RoundedPolygon(
                    4,
                    perVertexRounding =
                        listOf(cornerRound100, cornerRound20, cornerRound20, cornerRound20)
                )
                .rotated(-45f)
        },
        ShapeParameters(
            "Arrow",
            innerRadius = 0.1f,
            roundness = 0.22f,
            shapeId = ShapeParameters.ShapeId.Triangle
        ),
        CustomShapeParameters("Semicircle") {
            snapshotFlow {}
            RoundedPolygon.rectangle(
                width = 1.8f,
                height = 1f,
                perVertexRounding =
                    listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100)
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
            shapeId = ShapeParameters.ShapeId.Polygon
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
            var points = FloatArray(numVertices * 2)
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
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "Sunny",
            sides = 8,
            innerRadius = 0.83f,
            roundness = 0.15f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "4-Sided Cookie",
            sides = 4,
            innerRadius = 0.5f,
            roundness = 0.3f,
            rotation = -45f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "6-Sided Cookie",
            sides = 6,
            innerRadius = 0.75f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "7-Sided Cookie",
            sides = 7,
            innerRadius = 0.75f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "9-Sided Cookie",
            sides = 9,
            innerRadius = 0.75f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "12-Sided Cookie",
            sides = 12,
            innerRadius = 0.8f,
            roundness = 0.5f,
            rotation = -90f,
            shapeId = ShapeParameters.ShapeId.Star
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
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "8-Leaf clover",
            sides = 8,
            innerRadius = 0.65f,
            roundness = 0.3f,
            innerRoundness = 0f,
            rotation = 360f / 16,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "Burst",
            sides = 12,
            innerRadius = 0.7f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "Soft burst",
            sides = 12,
            innerRadius = 0.7f,
            roundness = 0.085f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        ShapeParameters(
            "Boom",
            sides = 15,
            innerRadius = 0.42f,
            shapeId = ShapeParameters.ShapeId.Star
        ),
        CustomShapeParameters("Soft Bloom") {
            val points =
                arrayOf(
                    Offset(0.456f, 0.224f),
                    Offset(0.460f, 0.170f),
                    Offset(0.500f, 0.100f),
                    Offset(0.540f, 0.170f),
                    Offset(0.544f, 0.224f),
                    Offset(0.538f, 0.308f)
                )
            val actualPoints = doRepeat(points, 16, center = Offset(0.5f, 0.5f))
            val roundings =
                listOf(
                        CornerRounding(0.020f, 0.000f),
                        CornerRounding(0.143f, 0.000f),
                        CornerRounding(0.025f, 0.000f),
                        CornerRounding(0.143f, 0.000f),
                        CornerRounding(0.190f, 0.000f),
                        CornerRounding(0.000f, 0.000f)
                    )
                    .let { l -> (0 until 16).flatMap { l } }
            RoundedPolygon(
                actualPoints,
                perVertexRounding = roundings,
                centerX = 0.5f,
                centerY = 0.5f
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
            shapeId = ShapeParameters.ShapeId.Star
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
                centerY = 0.5f
            )
        },
        CustomShapeParameters("Puffy Diamond") {
            val points =
                arrayOf(
                    Offset(0.390f, 0.260f),
                    Offset(0.390f, 0.130f),
                    Offset(0.610f, 0.130f),
                    Offset(0.610f, 0.260f),
                    Offset(0.740f, 0.260f)
                )
            val actualPoints = doRepeat(points, 4, center = Offset(0.5f, 0.5f))
            val roundings =
                listOf(
                        CornerRounding(0.000f, 0.000f),
                        CornerRounding(0.104f, 0.000f),
                        CornerRounding(0.104f, 0.000f),
                        CornerRounding(0.000f, 0.000f),
                        CornerRounding(0.104f, 0.000f)
                    )
                    .let { l -> (0 until 4).flatMap { l } }
            RoundedPolygon(
                actualPoints,
                perVertexRounding = roundings,
                centerX = 0.5f,
                centerY = 0.5f
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
                    -2 * pixelSize
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
            var inset = .4f
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
                    0f
                )
            var pvRounding =
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
                    cornerRound100
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
                    -.5f
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
                    unrounded
                )
            RoundedPolygon(points, perVertexRounding = pvRounding).transformed { x, y ->
                TransformResult(-y, x)
            }
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
