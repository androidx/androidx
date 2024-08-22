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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
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
 * Returns a [Path] for this [Morph].
 *
 * @param progress the [Morph]'s progress
 * @param path a [Path] to rewind and set with the new path data. In case provided, this Path would
 *   be the returned one.
 * @param startAngle an angle to rotate the [Path] to start drawing from
 */
@ExperimentalMaterial3ExpressiveApi
fun Morph.toPath(progress: Float, path: Path = Path(), startAngle: Int = 0): Path {
    return this.toPath(path = path, progress = progress, startAngle = startAngle)
}

/**
 * Returns a [Path] that is remembered across compositions for this [RoundedPolygon].
 *
 * @param startAngle an angle to rotate the Material shape's path to start drawing from. The
 *   rotation pivot is set to be the shape's centerX and centerY coordinates.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun RoundedPolygon.toPath(startAngle: Int = 0): Path {
    val path = remember { Path() }
    return remember(this, startAngle) {
        this.toPath(path = path, startAngle = startAngle, repeatPath = false, closePath = true)
    }
}

/**
 * Returns a [Shape] that is remembered across compositions for this [RoundedPolygon].
 *
 * @param startAngle an angle to rotate the Material shape's path to start drawing from. The
 *   rotation pivot is always set to be the shape's centerX and centerY coordinates.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun RoundedPolygon.toShape(startAngle: Int = 0): Shape {
    return remember(this, startAngle) {
        object : Shape {
            // Store the Path we convert from the RoundedPolygon here. The path we will be
            // manipulating and using on the createOutline would be a copy of this to ensure we
            // don't mutate the original.
            private val shapePath: Path = toPath(startAngle = startAngle)
            private val workPath: Path = Path()

            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                workPath.rewind()
                workPath.addPath(shapePath)
                val scaleMatrix = Matrix().apply { scale(x = size.width, y = size.height) }
                // Scale and translate the path to align its center with the available size
                // center.
                workPath.transform(scaleMatrix)
                workPath.translate(size.center - workPath.getBounds().center)
                return Outline.Generic(workPath)
            }
        }
    }
}

/**
 * Holds predefined Material Design shapes as [RoundedPolygon]s that can be used at various
 * components as they are, or as part of a [Morph].
 *
 * ![Shapes
 * image](https://developer.android.com/images/reference/androidx/compose/material3/shapes.png)
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

        private var _circle: RoundedPolygon? = null
        private var _square: RoundedPolygon? = null
        private var _slanted: RoundedPolygon? = null
        private var _arch: RoundedPolygon? = null
        private var _fan: RoundedPolygon? = null
        private var _arrow: RoundedPolygon? = null
        private var _semiCircle: RoundedPolygon? = null
        private var _oval: RoundedPolygon? = null
        private var _pill: RoundedPolygon? = null
        private var _triangle: RoundedPolygon? = null
        private var _diamond: RoundedPolygon? = null
        private var _clamShell: RoundedPolygon? = null
        private var _pentagon: RoundedPolygon? = null
        private var _gem: RoundedPolygon? = null
        private var _verySunny: RoundedPolygon? = null
        private var _sunny: RoundedPolygon? = null
        private var _cookie4Sided: RoundedPolygon? = null
        private var _cookie6Sided: RoundedPolygon? = null
        private var _cookie7Sided: RoundedPolygon? = null
        private var _cookie9Sided: RoundedPolygon? = null
        private var _cookie12Sided: RoundedPolygon? = null
        private var _ghostish: RoundedPolygon? = null
        private var _clover4Leaf: RoundedPolygon? = null
        private var _clover8Leaf: RoundedPolygon? = null
        private var _burst: RoundedPolygon? = null
        private var _softBurst: RoundedPolygon? = null
        private var _boom: RoundedPolygon? = null
        private var _softBoom: RoundedPolygon? = null
        private var _flower: RoundedPolygon? = null
        private var _puffy: RoundedPolygon? = null
        private var _puffyDiamond: RoundedPolygon? = null
        private var _pixelCircle: RoundedPolygon? = null
        private var _pixelTriangle: RoundedPolygon? = null
        private var _bun: RoundedPolygon? = null
        private var _heart: RoundedPolygon? = null

        /** A circle shape. */
        val Circle
            get() = _circle ?: circle().normalized().also { _circle = it }

        /** A rounded square shape. */
        val Square
            get() = _square ?: square().normalized().also { _square = it }

        /** A slanted square shape. */
        val Slanted
            get() = _slanted ?: slanted().normalized().also { _slanted = it }

        /** An arch shape. */
        val Arch
            get() = _arch ?: arch().normalized().also { _arch = it }

        /** A fan shape. */
        val Fan
            get() = _fan ?: fan().normalized().also { _fan = it }

        /** An arrow shape. */
        val Arrow
            get() = _arrow ?: arrow().normalized().also { _arrow = it }

        /** A semi-circle shape. */
        val SemiCircle
            get() = _semiCircle ?: semiCircle().normalized().also { _semiCircle = it }

        /** An oval shape. */
        val Oval
            get() = _oval ?: oval().normalized().also { _oval = it }

        /** A pill shape. */
        val Pill
            get() = _pill ?: pill().normalized().also { _pill = it }

        /** A rounded triangle shape. */
        val Triangle
            get() = _triangle ?: triangle().normalized().also { _triangle = it }

        /** A diamond shape. */
        val Diamond
            get() = _diamond ?: diamond().normalized().also { _diamond = it }

        /** A clam-shell shape. */
        val ClamShell
            get() = _clamShell ?: clamShell().normalized().also { _clamShell = it }

        /** A pentagon shape. */
        val Pentagon
            get() = _pentagon ?: pentagon().normalized().also { _pentagon = it }

        /** A gem shape. */
        val Gem
            get() = _gem ?: gem().normalized().also { _gem = it }

        /** A sunny shape. */
        val Sunny
            get() = _sunny ?: sunny().normalized().also { _sunny = it }

        /** A very-sunny shape. */
        val VerySunny
            get() = _verySunny ?: verySunny().normalized().also { _verySunny = it }

        /** A 4-sided cookie shape. */
        val Cookie4Sided
            get() = _cookie4Sided ?: cookie4().normalized().also { _cookie4Sided = it }

        /** A 6-sided cookie shape. */
        val Cookie6Sided
            get() = _cookie6Sided ?: cookie6().normalized().also { _cookie6Sided = it }

        /** A 7-sided cookie shape. */
        val Cookie7Sided
            get() = _cookie7Sided ?: cookie7().normalized().also { _cookie7Sided = it }

        /** A 9-sided cookie shape. */
        val Cookie9Sided
            get() = _cookie9Sided ?: cookie9().normalized().also { _cookie9Sided = it }

        /** A 12-sided cookie shape. */
        val Cookie12Sided
            get() = _cookie12Sided ?: cookie12().normalized().also { _cookie12Sided = it }

        /** A ghost-ish shape. */
        val Ghostish
            get() = _ghostish ?: ghostish().normalized().also { _ghostish = it }

        /** A 4-leaf clover shape. */
        val Clover4Leaf
            get() = _clover4Leaf ?: clover4().normalized().also { _clover4Leaf = it }

        /** An 8-leaf clover shape. */
        val Clover8Leaf
            get() = _clover8Leaf ?: clover8().normalized().also { _clover8Leaf = it }

        /** A burst shape. */
        val Burst
            get() = _burst ?: burst().normalized().also { _burst = it }

        /** A soft-burst shape. */
        val SoftBurst
            get() = _softBurst ?: softBurst().normalized().also { _softBurst = it }

        /** A boom shape. */
        val Boom
            get() = _boom ?: boom().normalized().also { _boom = it }

        /** A soft-boom shape. */
        val SoftBoom
            get() = _softBoom ?: softBoom().normalized().also { _softBoom = it }

        /** A flower shape. */
        val Flower
            get() = _flower ?: flower().normalized().also { _flower = it }

        /** A puffy shape. */
        val Puffy
            get() = _puffy ?: puffy().normalized().also { _puffy = it }

        /** A puffy-diamond shape. */
        val PuffyDiamond
            get() = _puffyDiamond ?: puffyDiamond().normalized().also { _puffyDiamond = it }

        /** A pixel-circle shape. */
        val PixelCircle
            get() = _pixelCircle ?: pixelCircle().normalized().also { _pixelCircle = it }

        /** A pixel-triangle shape. */
        val PixelTriangle
            get() = _pixelTriangle ?: pixelTriangle().normalized().also { _pixelTriangle = it }

        /** A bun shape. */
        val Bun
            get() = _bun ?: bun().normalized().also { _bun = it }

        /** A heart shape. */
        val Heart
            get() = _heart ?: heart().normalized().also { _heart = it }

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
            return triangleChip(
                innerRadius = .3375f,
                rounding = CornerRounding(radius = .25f, smoothing = .48f)
            )
        }

        internal fun triangleChip(innerRadius: Float, rounding: CornerRounding): RoundedPolygon {
            val topR = 0.888f
            val points =
                floatArrayOf(
                    radialToCartesian(radius = topR, 270f.toRadians()).x,
                    radialToCartesian(radius = topR, 270f.toRadians()).y,
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
            return RoundedPolygon.pill(width = width, height = height).transformed(rotateNeg45)
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

        internal fun sunny(): RoundedPolygon {
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = .8f,
                rounding = cornerRound15
            )
        }

        internal fun verySunny(): RoundedPolygon {
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = .65f,
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
            val inset = .46f
            val h = 1.2f
            val points = floatArrayOf(-1f, -h, 1f, -h, 1f, h, 0f, inset, -1f, h)
            val pvRounding =
                listOf(cornerRound100, cornerRound100, cornerRound50, cornerRound100, cornerRound50)
            return RoundedPolygon(points, perVertexRounding = pvRounding)
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
                .transformed(Matrix().apply { rotateZ(360f / 20) })
        }

        internal fun boom(): RoundedPolygon {
            return RoundedPolygon.star(numVerticesPerRadius = 15, innerRadius = .42f)
                .transformed(Matrix().apply { rotateZ(360f / 60) })
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
            val smoothRound = CornerRounding(radius = .12f, smoothing = .48f)
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = 1f,
                innerRadius = .588f,
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

        @Suppress("ListIterator", "PrimitiveInCollection")
        internal fun pixelCircle(): RoundedPolygon {
            val main = 0.4f
            val holes = listOf(Offset(0.28f, 0.14f), Offset(0.16f, 0.16f), Offset(0.16f, 0.3f))
            var p = Offset(main, -1f)
            val corner = buildList {
                add(p)
                holes.fastForEach { delta ->
                    p += Offset(0f, delta.y)
                    add(p)
                    p += Offset(delta.x, 0f)
                    add(p)
                }
            }
            val half = corner + corner.fastMap { Offset(it.x, -it.y) }.reversed()
            val points = half + half.fastMap { Offset(-it.x, it.y) }.reversed()
            return RoundedPolygon(
                points.fastFlatMap { listOf(it.x, it.y) }.toFloatArray(),
                unrounded
            )
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
