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
import androidx.compose.ui.util.fastMap
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
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
        private val cornerRound15 = CornerRounding(radius = .15f)
        private val cornerRound20 = CornerRounding(radius = .2f)
        private val cornerRound30 = CornerRounding(radius = .3f)
        private val cornerRound50 = CornerRounding(radius = .5f)
        private val cornerRound100 = CornerRounding(radius = 1f)

        private val rotateNeg45 = Matrix().apply { rotateZ(-45f) }
        private val rotateNeg90 = Matrix().apply { rotateZ(-90f) }
        private val rotateNeg135 = Matrix().apply { rotateZ(-135f) }

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
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.926f, 0.970f), CornerRounding(0.189f, 0.811f)),
                    PointNRound(Offset(-0.021f, 0.967f), CornerRounding(0.187f, 0.057f))
                ),
                2
            )
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
            return customPolygon(
                listOf(
                    PointNRound(Offset(1.004f, 1.000f), CornerRounding(0.148f, 0.417f)),
                    PointNRound(Offset(0.000f, 1.000f), CornerRounding(0.151f)),
                    PointNRound(Offset(0.000f, -0.003f), CornerRounding(0.148f)),
                    PointNRound(Offset(0.978f, 0.020f), CornerRounding(0.803f))
                ),
                1
            )
        }

        internal fun arrow(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 0.892f), CornerRounding(0.313f)),
                    PointNRound(Offset(-0.216f, 1.050f), CornerRounding(0.207f)),
                    PointNRound(Offset(0.499f, -0.160f), CornerRounding(0.215f, 1.000f)),
                    PointNRound(Offset(1.225f, 1.060f), CornerRounding(0.211f))
                ),
                1
            )
        }

        internal fun semiCircle(): RoundedPolygon {
            return RoundedPolygon.rectangle(
                width = 1.6f,
                height = 1f,
                perVertexRounding =
                    listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100)
            )
        }

        internal fun oval(): RoundedPolygon {
            val m = Matrix().apply { scale(1f, 0.64f) }
            return RoundedPolygon.circle().transformed(m).transformed(rotateNeg45)
        }

        internal fun pill(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.961f, 0.039f), CornerRounding(0.426f)),
                    PointNRound(Offset(1.001f, 0.428f)),
                    PointNRound(Offset(1.000f, 0.609f), CornerRounding(1.000f))
                ),
                reps = 2,
                mirroring = true
            )
        }

        internal fun triangle(): RoundedPolygon {
            return RoundedPolygon(numVertices = 3, rounding = cornerRound20)
                .transformed(rotateNeg90)
        }

        internal fun diamond(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 1.096f), CornerRounding(0.151f, 0.524f)),
                    PointNRound(Offset(0.040f, 0.500f), CornerRounding(0.159f))
                ),
                2
            )
        }

        internal fun clamShell(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.171f, 0.841f), CornerRounding(0.159f)),
                    PointNRound(Offset(-0.020f, 0.500f), CornerRounding(0.140f)),
                    PointNRound(Offset(0.170f, 0.159f), CornerRounding(0.159f))
                ),
                2
            )
        }

        internal fun pentagon(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
                    PointNRound(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
                    PointNRound(Offset(0.828f, 0.970f), CornerRounding(0.169f))
                ),
                reps = 1,
                mirroring = true
            )
        }

        internal fun gem(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.499f, 1.023f), CornerRounding(0.241f, 0.778f)),
                    PointNRound(Offset(-0.005f, 0.792f), CornerRounding(0.208f)),
                    PointNRound(Offset(0.073f, 0.258f), CornerRounding(0.228f)),
                    PointNRound(Offset(0.433f, -0.000f), CornerRounding(0.491f))
                ),
                1,
                mirroring = true
            )
        }

        internal fun sunny(): RoundedPolygon {
            return RoundedPolygon.star(
                numVerticesPerRadius = 8,
                innerRadius = .8f,
                rounding = cornerRound15
            )
        }

        internal fun verySunny(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 1.080f), CornerRounding(0.085f)),
                    PointNRound(Offset(0.358f, 0.843f), CornerRounding(0.085f))
                ),
                8
            )
        }

        internal fun cookie4(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(1.237f, 1.236f), CornerRounding(0.258f)),
                    PointNRound(Offset(0.500f, 0.918f), CornerRounding(0.233f))
                ),
                4
            )
        }

        internal fun cookie6(): RoundedPolygon {
            // 6-point cookie
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.723f, 0.884f), CornerRounding(0.394f)),
                    PointNRound(Offset(0.500f, 1.099f), CornerRounding(0.398f))
                ),
                6
            )
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
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 0f), CornerRounding(1.000f)),
                    PointNRound(Offset(1f, 0f), CornerRounding(1.000f)),
                    PointNRound(Offset(1f, 1.140f), CornerRounding(0.254f, 0.106f)),
                    PointNRound(Offset(0.575f, 0.906f), CornerRounding(0.253f))
                ),
                reps = 1,
                mirroring = true
            )
        }

        internal fun clover4(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 0.074f)),
                    PointNRound(Offset(0.725f, -0.099f), CornerRounding(0.476f))
                ),
                reps = 4,
                mirroring = true
            )
        }

        internal fun clover8(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 0.036f)),
                    PointNRound(Offset(0.758f, -0.101f), CornerRounding(0.209f))
                ),
                reps = 8
            )
        }

        internal fun burst(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, -0.006f), CornerRounding(0.006f)),
                    PointNRound(Offset(0.592f, 0.158f), CornerRounding(0.006f))
                ),
                reps = 12
            )
        }

        internal fun softBurst(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
                    PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f))
                ),
                reps = 10
            )
        }

        internal fun boom(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.457f, 0.296f), CornerRounding(0.007f)),
                    PointNRound(Offset(0.500f, -0.051f), CornerRounding(0.007f))
                ),
                reps = 15
            )
        }

        internal fun softBoom(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.733f, 0.454f)),
                    PointNRound(Offset(0.839f, 0.437f), CornerRounding(0.532f)),
                    PointNRound(Offset(0.949f, 0.449f), CornerRounding(0.439f, 1.000f)),
                    PointNRound(Offset(0.998f, 0.478f), CornerRounding(0.174f))
                ),
                reps = 16,
                mirroring = true
            )
        }

        internal fun flower(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.370f, 0.187f)),
                    PointNRound(Offset(0.416f, 0.049f), CornerRounding(0.381f)),
                    PointNRound(Offset(0.479f, 0.001f), CornerRounding(0.095f))
                ),
                reps = 8,
                mirroring = true
            )
        }

        internal fun puffy(): RoundedPolygon {
            val m = Matrix().apply { scale(1f, 0.742f) }
            return customPolygon(
                    listOf(
                        PointNRound(Offset(0.500f, 0.053f)),
                        PointNRound(Offset(0.545f, -0.040f), CornerRounding(0.405f)),
                        PointNRound(Offset(0.670f, -0.035f), CornerRounding(0.426f)),
                        PointNRound(Offset(0.717f, 0.066f), CornerRounding(0.574f)),
                        PointNRound(Offset(0.722f, 0.128f)),
                        PointNRound(Offset(0.777f, 0.002f), CornerRounding(0.360f)),
                        PointNRound(Offset(0.914f, 0.149f), CornerRounding(0.660f)),
                        PointNRound(Offset(0.926f, 0.289f), CornerRounding(0.660f)),
                        PointNRound(Offset(0.881f, 0.346f)),
                        PointNRound(Offset(0.940f, 0.344f), CornerRounding(0.126f)),
                        PointNRound(Offset(1.003f, 0.437f), CornerRounding(0.255f)),
                    ),
                    reps = 2,
                    mirroring = true
                )
                .transformed(m)
        }

        internal fun puffyDiamond(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.870f, 0.130f), CornerRounding(0.146f)),
                    PointNRound(Offset(0.818f, 0.357f)),
                    PointNRound(Offset(1.000f, 0.332f), CornerRounding(0.853f))
                ),
                reps = 4,
                mirroring = true
            )
        }

        @Suppress("ListIterator", "PrimitiveInCollection")
        internal fun pixelCircle(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 0.000f)),
                    PointNRound(Offset(0.704f, 0.000f)),
                    PointNRound(Offset(0.704f, 0.065f)),
                    PointNRound(Offset(0.843f, 0.065f)),
                    PointNRound(Offset(0.843f, 0.148f)),
                    PointNRound(Offset(0.926f, 0.148f)),
                    PointNRound(Offset(0.926f, 0.296f)),
                    PointNRound(Offset(1.000f, 0.296f))
                ),
                reps = 2,
                mirroring = true
            )
        }

        @Suppress("ListIterator", "PrimitiveInCollection")
        internal fun pixelTriangle(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.110f, 0.500f)),
                    PointNRound(Offset(0.113f, 0.000f)),
                    PointNRound(Offset(0.287f, 0.000f)),
                    PointNRound(Offset(0.287f, 0.087f)),
                    PointNRound(Offset(0.421f, 0.087f)),
                    PointNRound(Offset(0.421f, 0.170f)),
                    PointNRound(Offset(0.560f, 0.170f)),
                    PointNRound(Offset(0.560f, 0.265f)),
                    PointNRound(Offset(0.674f, 0.265f)),
                    PointNRound(Offset(0.675f, 0.344f)),
                    PointNRound(Offset(0.789f, 0.344f)),
                    PointNRound(Offset(0.789f, 0.439f)),
                    PointNRound(Offset(0.888f, 0.439f))
                ),
                reps = 1,
                mirroring = true
            )
        }

        internal fun bun(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.796f, 0.500f)),
                    PointNRound(Offset(0.853f, 0.518f), CornerRounding(1f)),
                    PointNRound(Offset(0.992f, 0.631f), CornerRounding(1f)),
                    PointNRound(Offset(0.968f, 1.000f), CornerRounding(1f))
                ),
                reps = 2,
                mirroring = true
            )
        }

        internal fun heart(): RoundedPolygon {
            return customPolygon(
                listOf(
                    PointNRound(Offset(0.500f, 0.268f), CornerRounding(0.016f)),
                    PointNRound(Offset(0.792f, -0.066f), CornerRounding(0.958f)),
                    PointNRound(Offset(1.064f, 0.276f), CornerRounding(1.000f)),
                    PointNRound(Offset(0.501f, 0.946f), CornerRounding(0.129f))
                ),
                reps = 1,
                mirroring = true
            )
        }

        private data class PointNRound(
            val o: Offset,
            val r: CornerRounding = CornerRounding.Unrounded
        )

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
                    val actualReps = reps * 2
                    val sectionAngle = 360f / actualReps
                    repeat(actualReps) {
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

        private fun customPolygon(
            pnr: List<PointNRound>,
            reps: Int,
            center: Offset = Offset(0.5f, 0.5f),
            mirroring: Boolean = false
        ): RoundedPolygon {
            val actualPoints = doRepeat(pnr, reps, center, mirroring)
            return RoundedPolygon(
                vertices =
                    FloatArray(actualPoints.size * 2) { ix ->
                        actualPoints[ix / 2].o.let { if (ix % 2 == 0) it.x else it.y }
                    },
                perVertexRounding = buildList { for (p in actualPoints) add(p.r) },
                centerX = center.x,
                centerY = center.y
            )
        }
    }
}
