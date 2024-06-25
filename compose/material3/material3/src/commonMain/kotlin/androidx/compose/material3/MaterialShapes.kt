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

package androidx.compose.material3

import androidx.compose.material3.internal.toPath
import androidx.compose.material3.internal.transformed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFlatMap
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Returns a normalized [Path] for this [RoundedPolygon].
 *
 * @param path a [Path] object which, if supplied, will avoid the function having to create a new
 *   [Path] object
 * @param startAngle an angle to rotate the Material shape's path to start drawing from. The
 *   rotation pivot is set to be the shape's centerX and centerY coordinates.
 * @see RoundedPolygon.normalized
 */
@ExperimentalMaterial3ExpressiveApi
fun RoundedPolygon.toPath(path: Path = Path(), startAngle: Int = 0): Path {
    return this.toPath(path = path, startAngle = startAngle, repeatPath = false, closePath = true)
}

/**
 * Returns a [Shape] for this [RoundedPolygon].
 *
 * @param startAngle an angle to rotate the Material shape's path to start drawing from. The
 *   rotation pivot is always set to be the shape's centerX and centerY coordinates.
 */
@ExperimentalMaterial3ExpressiveApi
fun RoundedPolygon.toShape(startAngle: Int = 0): Shape {
    return object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = toPath(startAngle = startAngle)
            val scaleMatrix = Matrix().apply { scale(x = size.width, y = size.height) }
            // Scale and translate the path to align its center with the available size center.
            path.transform(scaleMatrix)
            path.translate(size.center - path.getBounds().center)
            return Outline.Generic(path)
        }
    }
}

// TODO: Document all shapes and possible add screenshots.
/**
 * Holds predefined Material Design shapes as [RoundedPolygon]s that can be used at various
 * components as they are, or as part of a [Morph].
 *
 * Note that each [RoundedPolygon] in this class is normalized.
 *
 * @see RoundedPolygon.normalized
 */
@ExperimentalMaterial3ExpressiveApi
sealed class MaterialShapes {

    companion object {

        // Cache various roundings for use below
        private val cornerRound10 = CornerRounding(radius = .1f)
        private val cornerRound15 = CornerRounding(radius = .15f)
        private val cornerRound20 = CornerRounding(radius = .2f)
        private val cornerRound30 = CornerRounding(radius = .3f)
        private val cornerRound40 = CornerRounding(radius = .4f)
        private val cornerRound50 = CornerRounding(radius = .5f)
        private val cornerRound100 = CornerRounding(radius = 1f)

        private val rotateNeg45 = Matrix().apply { rotateZ(-45f) }
        private val rotate45 = Matrix().apply { rotateZ(45f) }
        private val rotateNeg90 = Matrix().apply { rotateZ(-90f) }
        private val rotate90 = Matrix().apply { rotateZ(90f) }
        private val rotateNeg135 = Matrix().apply { rotateZ(-135f) }
        private val unrounded = CornerRounding.Unrounded

        val Circle: RoundedPolygon by lazy { circle().normalized() }

        val Square: RoundedPolygon by lazy { square().normalized() }

        val Slanted: RoundedPolygon by lazy { slanted().normalized() }

        val Arch: RoundedPolygon by lazy { arch().normalized() }

        val Fan: RoundedPolygon by lazy { fan().normalized() }

        val Arrow: RoundedPolygon by lazy { arrow().normalized() }

        val SemiCircle: RoundedPolygon by lazy { semiCircle().normalized() }

        val Oval: RoundedPolygon by lazy { oval().normalized() }

        val Pill: RoundedPolygon by lazy { pill().normalized() }

        val Triangle: RoundedPolygon by lazy { triangle().normalized() }

        val Diamond: RoundedPolygon by lazy { diamond().normalized() }

        val ClamShell: RoundedPolygon by lazy { clamShell().normalized() }

        val Pentagon: RoundedPolygon by lazy { pentagon().normalized() }

        val Gem: RoundedPolygon by lazy { gem().normalized() }

        val VerySunny: RoundedPolygon by lazy { verySunny().normalized() }

        val Sunny: RoundedPolygon by lazy { sunny().normalized() }

        val Cookie4Sided: RoundedPolygon by lazy { cookie4().normalized() }

        val Cookie6Sided: RoundedPolygon by lazy { cookie6().normalized() }

        val Cookie7Sided: RoundedPolygon by lazy { cookie7().normalized() }

        val Cookie9Sided: RoundedPolygon by lazy { cookie9().normalized() }

        val Cookie12Sided: RoundedPolygon by lazy { cookie12().normalized() }

        val Ghostish: RoundedPolygon by lazy { ghostish().normalized() }

        val Clover4Leaf: RoundedPolygon by lazy { clover4().normalized() }

        val Clover8Leaf: RoundedPolygon by lazy { clover8().normalized() }

        val Burst: RoundedPolygon by lazy { burst().normalized() }

        val SoftBurst: RoundedPolygon by lazy { softBurst().normalized() }

        val Boom: RoundedPolygon by lazy { boom().normalized() }

        val SoftBoom: RoundedPolygon by lazy { softBoom().normalized() }

        val Flower: RoundedPolygon by lazy { flower().normalized() }

        val Puffy: RoundedPolygon by lazy { puffy().normalized() }

        val PuffyDiamond: RoundedPolygon by lazy { puffyDiamond().normalized() }

        val PixelCircle: RoundedPolygon by lazy { pixelCircle().normalized() }

        val PixelTriangle: RoundedPolygon by lazy { pixelTriangle().normalized() }

        val Bun: RoundedPolygon by lazy { bun().normalized() }

        val Heart: RoundedPolygon by lazy { heart().normalized() }

        internal fun circle(numVertices: Int = 10): RoundedPolygon {
            return RoundedPolygon.circle(numVertices = numVertices)
        }

        internal fun square(): RoundedPolygon {
            return RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = cornerRound30)
        }

        internal fun slanted(): RoundedPolygon {
            return RoundedPolygon(
                    numVertices = 4,
                    rounding = CornerRounding(radius = 0.3f, smoothing = 0.5f)
                )
                .transformed(rotateNeg45)
                .transformed { x, y ->
                    TransformResult(x - 0.1f * y, y) // Compose's matrix doesn't support skew!?
                }
        }

        internal fun arch(): RoundedPolygon {
            return RoundedPolygon(
                    numVertices = 4,
                    perVertexRounding =
                        listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20)
                )
                .transformed(rotateNeg135)
        }

        internal fun fan(): RoundedPolygon {
            return RoundedPolygon(
                    numVertices = 4,
                    perVertexRounding =
                        listOf(cornerRound100, cornerRound20, cornerRound20, cornerRound20)
                )
                .transformed(rotateNeg45)
        }

        internal fun arrow(): RoundedPolygon {
            return triangleChip(innerRadius = .2f, CornerRounding(radius = .22f))
        }

        internal fun triangleChip(innerRadius: Float, rounding: CornerRounding): RoundedPolygon {
            val points =
                floatArrayOf(
                    radialToCartesian(radius = 1f, 270f.toRadians()).x,
                    radialToCartesian(radius = 1f, 270f.toRadians()).y,
                    radialToCartesian(radius = 1f, 30f.toRadians()).x,
                    radialToCartesian(radius = 1f, 30f.toRadians()).y,
                    radialToCartesian(radius = innerRadius, 90f.toRadians()).x,
                    radialToCartesian(radius = innerRadius, 90f.toRadians()).y,
                    radialToCartesian(radius = 1f, 150f.toRadians()).x,
                    radialToCartesian(radius = 1f, 150f.toRadians()).y
                )
            return RoundedPolygon(points, rounding)
        }

        internal fun semiCircle(): RoundedPolygon {
            return RoundedPolygon.rectangle(
                width = 1.6f,
                height = 1f,
                perVertexRounding =
                    listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100)
            )
        }

        internal fun oval(scaleX: Float = 1f, scaleY: Float = .7f): RoundedPolygon {
            val m = Matrix().apply { scale(x = scaleX, y = scaleY) }
            return RoundedPolygon.circle().transformed(m).transformed(rotateNeg45)
        }

        internal fun pill(width: Float = 1.25f, height: Float = 1f): RoundedPolygon {
            return RoundedPolygon.pill(width = width, height = height)
        }

        internal fun triangle(): RoundedPolygon {
            return RoundedPolygon(numVertices = 3, rounding = cornerRound20)
                .transformed(rotateNeg90)
        }

        internal fun diamond(scaleX: Float = 1f, scaleY: Float = 1.2f): RoundedPolygon {
            return RoundedPolygon(numVertices = 4, rounding = cornerRound30)
                .transformed(Matrix().apply { scale(x = scaleX, y = scaleY) })
        }

        internal fun clamShell(): RoundedPolygon {
            val cornerInset = .6f
            val edgeInset = .4f
            val height = .7f
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
                    cornerRound30,
                    cornerRound30,
                    unrounded,
                    unrounded,
                    cornerRound30,
                    cornerRound30,
                    cornerRound30,
                    unrounded,
                    unrounded,
                    cornerRound30,
                )
            return RoundedPolygon(hexPoints, perVertexRounding = pvRounding)
        }

        internal fun pentagon(): RoundedPolygon {
            return RoundedPolygon(numVertices = 5, rounding = cornerRound30)
                .transformed(Matrix().apply { rotateZ(-360f / 20f) })
        }

        internal fun gem(): RoundedPolygon {
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
            return RoundedPolygon(points, cornerRound40).transformed(rotateNeg90)
        }

        internal fun verySunny(): RoundedPolygon {
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = .65f,
                rounding = cornerRound15
            )
        }

        internal fun sunny(): RoundedPolygon {
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = .83f,
                rounding = cornerRound15
            )
        }

        internal fun cookie4(): RoundedPolygon {
            return RoundedPolygon.star(
                    numVerticesPerRadius = 4,
                    innerRadius = .5f,
                    rounding = cornerRound30
                )
                .transformed(rotateNeg45)
        }

        internal fun cookie6(): RoundedPolygon {
            // 6-point cookie
            return RoundedPolygon.star(
                    numVerticesPerRadius = 6,
                    innerRadius = .75f,
                    rounding = cornerRound50
                )
                .transformed(rotateNeg90)
        }

        internal fun cookie7(): RoundedPolygon {
            // 7-point cookie
            return RoundedPolygon.star(
                    numVerticesPerRadius = 7,
                    innerRadius = .75f,
                    rounding = cornerRound50
                )
                .transformed(rotateNeg90)
        }

        internal fun cookie9(): RoundedPolygon {
            return RoundedPolygon.star(
                    numVerticesPerRadius = 9,
                    innerRadius = .8f,
                    rounding = cornerRound50
                )
                .transformed(rotateNeg90)
        }

        internal fun cookie12(): RoundedPolygon {
            return RoundedPolygon.star(
                    numVerticesPerRadius = 12,
                    innerRadius = .8f,
                    rounding = cornerRound50
                )
                .transformed(rotateNeg90)
        }

        internal fun ghostish(): RoundedPolygon {
            val inset = .5f
            val w = .88f
            val points = floatArrayOf(1f, w, -1f, w, -inset, 0f, -1f, -w, 1f, -w)
            val pvRounding =
                listOf(cornerRound100, cornerRound50, cornerRound100, cornerRound50, cornerRound100)
            return RoundedPolygon(points, perVertexRounding = pvRounding).transformed(rotateNeg90)
        }

        internal fun clover4(): RoundedPolygon {
            // (no inner rounding)
            return RoundedPolygon.star(
                    numVerticesPerRadius = 4,
                    innerRadius = .2f,
                    rounding = cornerRound40,
                    innerRounding = unrounded
                )
                .transformed(rotate45)
        }

        internal fun clover8(): RoundedPolygon {
            // (no inner rounding)
            return RoundedPolygon.star(
                    numVerticesPerRadius = 8,
                    innerRadius = .65f,
                    rounding = cornerRound30,
                    innerRounding = unrounded
                )
                .transformed(Matrix().apply { rotateZ(360f / 16) })
        }

        internal fun burst(): RoundedPolygon {
            return RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = .7f)
        }

        internal fun softBurst(): RoundedPolygon {
            return RoundedPolygon.star(
                radius = 1f,
                numVerticesPerRadius = 10,
                innerRadius = .65f,
                rounding = cornerRound10,
                innerRounding = cornerRound10
            )
        }

        internal fun boom(): RoundedPolygon {
            return RoundedPolygon.star(numVerticesPerRadius = 15, innerRadius = .42f)
        }

        internal fun softBoom(): RoundedPolygon {
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
                        CornerRounding(radius = 0.020f),
                        CornerRounding(radius = 0.143f),
                        CornerRounding(radius = 0.025f),
                        CornerRounding(radius = 0.143f),
                        CornerRounding(radius = 0.190f),
                        CornerRounding(radius = 0f)
                    )
                    .let { l -> (0 until 16).flatMap { l } }

            return RoundedPolygon(
                actualPoints,
                perVertexRounding = roundings,
                centerX = 0.5f,
                centerY = 0.5f
            )
        }

        internal fun flower(): RoundedPolygon {
            val smoothRound = CornerRounding(radius = .13f, smoothing = .95f)
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = 1f,
                innerRadius = .575f,
                rounding = smoothRound,
                innerRounding = unrounded
            )
        }

        internal fun puffy(): RoundedPolygon {
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
            val actualPoints =
                doRepeat(pnr, reps = 4, center = Offset(0.5f, 0.5f), mirroring = true)

            return RoundedPolygon(
                actualPoints.fastFlatMap { listOf(it.o.x, it.o.y) }.toFloatArray(),
                perVertexRounding = actualPoints.fastMap { it.r },
                centerX = 0.5f,
                centerY = 0.5f
            )
        }

        internal fun puffyDiamond(): RoundedPolygon {
            val points =
                arrayOf(
                    Offset(0.390f, 0.260f),
                    Offset(0.390f, 0.130f),
                    Offset(0.610f, 0.130f),
                    Offset(0.610f, 0.260f),
                    Offset(0.740f, 0.260f)
                )
            val actualPoints = doRepeat(points, reps = 4, center = Offset(0.5f, 0.5f))
            val roundings =
                listOf(
                        CornerRounding(radius = 0.000f),
                        CornerRounding(radius = 0.104f),
                        CornerRounding(radius = 0.104f),
                        CornerRounding(radius = 0.000f),
                        CornerRounding(radius = 0.104f)
                    )
                    .let { l -> (0 until 4).flatMap { l } }

            return RoundedPolygon(
                actualPoints,
                perVertexRounding = roundings,
                centerX = 0.5f,
                centerY = 0.5f
            )
        }

        internal fun pixelCircle(): RoundedPolygon {
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

                    // BL quadrant
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
            return RoundedPolygon(points, unrounded)
        }

        @Suppress("ListIterator", "PrimitiveInCollection")
        internal fun pixelTriangle(): RoundedPolygon {
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
            val centerX = points.fastMaxBy { it.x }!!.x / 2
            val centerY = points.fastMaxBy { it.y }!!.y / 2

            return RoundedPolygon(
                points.fastFlatMap { listOf(it.x, it.y) }.toFloatArray(),
                centerX = centerX,
                centerY = centerY,
            )
        }

        internal fun bun(): RoundedPolygon {
            // Basically, two pills stacked on each other
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
                    0f
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
                    cornerRound100
                )
            return RoundedPolygon(sandwichPoints, perVertexRounding = pvRounding)
        }

        internal fun heart(): RoundedPolygon {
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
            return RoundedPolygon(points, perVertexRounding = pvRounding).transformed(rotate90)
        }

        private data class PointNRound(val o: Offset, val r: CornerRounding)

        private fun doRepeat(points: Array<Offset>, reps: Int, center: Offset) =
            points.size.let { np ->
                (0 until np * reps)
                    .flatMap {
                        val point = points[it % np].rotateDegrees((it / np) * 360f / reps, center)
                        listOf(point.x, point.y)
                    }
                    .toFloatArray()
            }

        @Suppress("PrimitiveInCollection")
        private fun doRepeat(
            points: List<PointNRound>,
            reps: Int,
            center: Offset,
            mirroring: Boolean
        ) =
            if (mirroring) {
                buildList {
                    val angles = points.fastMap { (it.o - center).angleDegrees() }
                    val distances = points.fastMap { (it.o - center).getDistance() }
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

        private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero) =
            (angle.toRadians()).let { a ->
                val off = this - center
                Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
            }

        private fun Float.toRadians(): Float {
            return this / 360f * 2 * PI.toFloat()
        }

        private fun Offset.angleDegrees() = atan2(y, x) * 180f / PI.toFloat()

        private fun directionVector(angleRadians: Float) =
            Offset(cos(angleRadians), sin(angleRadians))

        private fun radialToCartesian(
            radius: Float,
            angleRadians: Float,
            center: Offset = Offset.Zero
        ) = directionVector(angleRadians) * radius + center
    }
}
