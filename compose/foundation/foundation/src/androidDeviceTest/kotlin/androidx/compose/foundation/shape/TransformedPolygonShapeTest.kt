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

package androidx.compose.foundation.shape

import android.graphics.PathMeasure
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformedPolygonShapeTest {

    private val unitDensity = Density(1f)
    private val size = Size(200.0f, 200.0f)
    private val sizePx = 200f
    private val tightTolerance = 2f

    @Test
    fun polygonShape_transform_rotatesInPlace() {
        val base = PolygonShape { polygon(3) }
        val expected = Path().apply { addPath(base.outlinePath()) }
        expected
            .asAndroidPath()
            .transform(android.graphics.Matrix().apply { setRotate(90f, sizePx / 2f, sizePx / 2f) })

        base.transform { rotate(90f) }

        val outline = base.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertTrue(outline is Outline.Generic)
        assertPathsMatch(
            "rotated triangle vs rotated base path",
            expected,
            (outline as Outline.Generic).path,
            tightTolerance,
        )
    }

    @Test
    fun polygonShape_scaleToFit_mutatesInPlace() {
        val base = PolygonShape { polygon(5, radius = 1000f) } // gigantic radius

        base.transform { scaleToFit() }

        val underlying = base.resolvePolygon(size, LayoutDirection.Ltr, unitDensity)
        val bounds = underlying.calculateBounds(FloatArray(4), false)
        assertEquals(100f, (bounds[0] + bounds[2]) / 2f, 1e-2f)
        assertEquals(100f, (bounds[1] + bounds[3]) / 2f, 1e-2f)
        assertTrue(bounds[0] >= -0.5f && bounds[1] >= -0.5f)
        assertTrue(bounds[2] <= 200.5f && bounds[3] <= 200.5f)
    }

    @Test
    fun roundingFraction_isRelativeToGeneratingRadius() {
        // Geometry-relative rounding: the same fraction at different explicit radii must produce
        // the same shape once scaled to fit, because the rounding scales with the radius.
        fun pentagonAt(radius: Float) = PolygonShape {
            polygon(5, radius = radius, rounding = CornerRounding.fraction(0.2f))
        }
            .apply { transform { scaleToFit() } }
        assertPathsMatch(
            "fraction rounding at radius 50 vs 400",
            pentagonAt(50f).outlinePath(),
            pentagonAt(400f).outlinePath(),
            tightTolerance,
        )
    }

    private fun wideRectangleShape(): PolygonShape = PolygonShape {
        polygon(listOf(Offset(0f, 0f), Offset(320f, 0f), Offset(320f, 200f), Offset(0f, 200f)))
    }

    @Test
    fun scaleToFit_fitsIrregularGeometry_preservingAspectRatio() {
        // 320x200 rectangle fit into a 300x150 container: scale = min(300/320, 150/200) = 0.75,
        // so the resolved bounds must be 240x150, centered. A stretch would fill 300x150.
        val shape = wideRectangleShape().apply { transform { scaleToFit() } }
        val polygon = shape.resolvePolygon(Size(300f, 150f), LayoutDirection.Ltr, unitDensity)
        val bounds = polygon.calculateBounds(FloatArray(4), false)
        assertEquals(30f, bounds[0], 1e-2f)
        assertEquals(0f, bounds[1], 1e-2f)
        assertEquals(270f, bounds[2], 1e-2f)
        assertEquals(150f, bounds[3], 1e-2f)
    }

    @Test
    fun scaleToFit_centersSmallerAxis() {
        // The same 1.6:1 geometry in a square container: width-bound, vertically centered.
        val shape = wideRectangleShape().apply { transform { scaleToFit() } }
        val polygon = shape.resolvePolygon(size, LayoutDirection.Ltr, unitDensity)
        val bounds = polygon.calculateBounds(FloatArray(4), false)
        assertEquals(0f, bounds[0], 1e-2f)
        assertEquals(37.5f, bounds[1], 1e-2f)
        assertEquals(200f, bounds[2], 1e-2f)
        assertEquals(162.5f, bounds[3], 1e-2f)
    }

    @Test
    fun scaleToFit_fillBounds_stretchesToContainer() {
        val shape = wideRectangleShape().apply { transform { scaleToFit(ContentScale.FillBounds) } }
        val bounds =
            shape
                .resolvePolygon(Size(300f, 150f), LayoutDirection.Ltr, unitDensity)
                .calculateBounds(FloatArray(4), false)
        assertEquals(0f, bounds[0], 1e-2f)
        assertEquals(0f, bounds[1], 1e-2f)
        assertEquals(300f, bounds[2], 1e-2f)
        assertEquals(150f, bounds[3], 1e-2f)
    }

    @Test
    fun scaleToFit_crop_fillsPreservingAspectRatio() {
        // 320x200 geometry cropped into 300x150: scale = max(300/320, 150/200) = 0.9375, so the
        // scaled geometry is 300x187.5, horizontally exact and vertically overflowing, centered.
        val shape = wideRectangleShape().apply { transform { scaleToFit(ContentScale.Crop) } }
        val bounds =
            shape
                .resolvePolygon(Size(300f, 150f), LayoutDirection.Ltr, unitDensity)
                .calculateBounds(FloatArray(4), false)
        assertEquals(0f, bounds[0], 1e-2f)
        assertEquals(-18.75f, bounds[1], 1e-2f)
        assertEquals(300f, bounds[2], 1e-2f)
        assertEquals(168.75f, bounds[3], 1e-2f)
    }

    @Test
    fun scaleToFit_alignment_positionsScaledGeometry() {
        val shape =
            wideRectangleShape().apply { transform { scaleToFit(alignment = Alignment.TopStart) } }
        val bounds =
            shape
                .resolvePolygon(Size(300f, 150f), LayoutDirection.Ltr, unitDensity)
                .calculateBounds(FloatArray(4), false)
        assertEquals(0f, bounds[0], 1e-2f)
        assertEquals(0f, bounds[1], 1e-2f)
        assertEquals(240f, bounds[2], 1e-2f)
        assertEquals(150f, bounds[3], 1e-2f)
    }

    @Test
    fun transform_pivotsAroundPolygonCenter() {
        val rotated = PolygonShape { polygon(5) }.apply { transform { rotate(36f) } }
        val polygon = rotated.resolvePolygon(size, LayoutDirection.Ltr, unitDensity)
        assertEquals(sizePx / 2f, polygon.centerX, 1e-2f)
        assertEquals(sizePx / 2f, polygon.centerY, 1e-2f)
        val bounds = polygon.calculateBounds(FloatArray(4), false)
        assertTrue("rotated shape escaped its container: ${bounds.toList()}", bounds[0] >= -0.5f)
        assertTrue(bounds[1] >= -0.5f)
        assertTrue(bounds[2] <= sizePx + 0.5f)
        assertTrue(bounds[3] <= sizePx + 0.5f)
    }

    @Test
    fun transform_multipleRotations_composeAdditively() {
        val base = PolygonShape { polygon(3) }
        val chained =
            base.copy().apply {
                transform {
                    rotate(30f)
                    rotate(60f)
                }
            }
        val single = base.copy().apply { transform { rotate(90f) } }
        assertPathsMatch(
            "chained rotations",
            single.outlinePath(),
            chained.outlinePath(),
            tightTolerance,
        )
    }

    @Test
    fun transform_multipleUniformScales_composeMultiplicatively() {
        val base = PolygonShape { polygon(6) }
        val chained =
            base.copy().apply {
                transform {
                    scale(0.5f, 0.5f)
                    scale(0.5f, 0.5f)
                }
            }
        val single = base.copy().apply { transform { scale(0.25f, 0.25f) } }
        assertPathsMatch(
            "chained scales",
            single.outlinePath(),
            chained.outlinePath(),
            tightTolerance,
        )
    }

    @Test
    fun transform_callOrderMatchesComposedMatrix() {
        // Order-sensitive composition: scale then rotate. A single Matrix built as
        // rotateZ-then-scale applies the scale first (compose Matrix operations post-multiply),
        // so it must equal calling scale() then rotate() in PolygonShapeTransformScope.
        val base = PolygonShape { polygon(3) }
        val chained =
            base.copy().apply {
                transform {
                    scale(1f, 0.5f)
                    rotate(90f)
                }
            }
        val composed =
            base.copy().apply {
                transform {
                    transform(
                        Matrix().apply {
                            rotateZ(90f)
                            scale(1f, 0.5f)
                        }
                    )
                }
            }
        assertPathsMatch(
            "scale-then-rotate chain vs composed matrix",
            composed.outlinePath(),
            chained.outlinePath(),
            tightTolerance,
        )
    }

    @Test
    fun transform_scaleThenRotate_staysCentered() {
        val shape = PolygonShape {
            polygon(4)
        }
            .apply {
                transform {
                    scale(1f, 0.5f)
                    rotate(45f)
                }
            }
        val polygon = shape.resolvePolygon(size, LayoutDirection.Ltr, unitDensity)
        assertEquals(sizePx / 2f, polygon.centerX, 1e-2f)
        assertEquals(sizePx / 2f, polygon.centerY, 1e-2f)
    }

    @Test
    fun scaleToFit_afterRotation_refitsRotatedGeometryIntoContainer() {
        // A rotated wide rectangle escapes its container; scaleToFit() measures the transformed
        // geometry in the same pass and must land it exactly inscribed.
        val shape =
            wideRectangleShape().apply {
                transform {
                    rotate(90f)
                    scaleToFit()
                }
            }
        val bounds =
            shape
                .resolvePolygon(Size(300f, 150f), LayoutDirection.Ltr, unitDensity)
                .calculateBounds(FloatArray(4), false)
        // 320x200 rotated 90 degrees is 200x320; fit into 300x150 gives scale 150/320 so the
        // resolved bounds are 93.75x150, centered horizontally.
        assertEquals(103.125f, bounds[0], 1e-1f)
        assertEquals(0f, bounds[1], 1e-1f)
        assertEquals(196.875f, bounds[2], 1e-1f)
        assertEquals(150f, bounds[3], 1e-1f)
    }

    @Test
    fun scaleToFit_alignment_respectsRtl() {
        // TopStart aligns to the right edge under RTL: the horizontal bias flips.
        val shape =
            wideRectangleShape().apply { transform { scaleToFit(alignment = Alignment.TopStart) } }
        val bounds =
            shape
                .resolvePolygon(Size(300f, 150f), LayoutDirection.Rtl, unitDensity)
                .calculateBounds(FloatArray(4), false)
        assertEquals(60f, bounds[0], 1e-2f)
        assertEquals(0f, bounds[1], 1e-2f)
        assertEquals(300f, bounds[2], 1e-2f)
        assertEquals(150f, bounds[3], 1e-2f)
    }

    @Test
    fun transform_rejectsPerspectiveMatrix() {
        val matrix = Matrix()
        matrix.values[3] = 0.001f
        val shape = PolygonShape.star(5).apply { transform { transform(matrix) } }
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                shape.createOutline(size, LayoutDirection.Ltr, unitDensity)
            }
        assertThat(exception).hasMessageThat().contains("perspective")
    }

    @Test
    fun transform_overBuilderShape_reflectsCapturedStateChange() {
        // A builder lambda reading captured state must produce a fresh transformed outline when
        // that state changes.
        var radius = 40f
        val shape = PolygonShape { polygon(4, radius = radius) }.apply { transform { rotate(45f) } }
        val before = shape.outlinePath().getBounds()
        radius = 80f
        val after = shape.outlinePath().getBounds()
        assertEquals(before.width * 2f, after.width, 1f)
        assertEquals(before.height * 2f, after.height, 1f)
    }

    @Test
    fun transform_dynamicStateInTransformBlock_updatesOutlineAndMorph() {
        var degrees = 0f
        val start = PolygonShape { polygon(3) }.apply { transform { rotate(degrees) } }
        val end = PolygonShape.circle()
        val morph = MorphPolygonShape(start, end) { 0f }

        val unrotatedOutline = start.outlinePath()
        val morphAtZero =
            (morph.createOutline(size, LayoutDirection.Ltr, unitDensity) as Outline.Generic).path
        assertPathsMatch("initial morph at 0deg", unrotatedOutline, morphAtZero, tightTolerance)

        degrees = 90f
        val rotatedOutline = start.outlinePath()
        val expectedRotated = PolygonShape {
            polygon(3)
        }
            .apply { transform { rotate(90f) } }
            .outlinePath()
        assertPathsMatch(
            "dynamic rotation at 90deg",
            expectedRotated,
            rotatedOutline,
            tightTolerance,
        )

        val morphAtNinety =
            (morph.createOutline(size, LayoutDirection.Ltr, unitDensity) as Outline.Generic).path
        assertPathsMatch("updated morph at 90deg", expectedRotated, morphAtNinety, tightTolerance)
    }

    @Test
    fun transform_translate_matchesMatrixTranslation() {
        val base = PolygonShape.star(5)
        val translatedParam = base.copy().apply { transform { translate(20f, 30f) } }
        val translatedMatrix =
            base.copy().apply { transform { transform(Matrix().apply { translate(20f, 30f) }) } }
        assertPathsMatch(
            "translate vs matrix translation",
            translatedMatrix.outlinePath(),
            translatedParam.outlinePath(),
            tightTolerance,
        )
    }

    @Test
    fun copy_createsIndependentPolygonShape() {
        val base = PolygonShape { polygon(3) }
        val copy = base.copy()
        assertNotSame(base, copy)

        copy.transform { rotate(90f) }

        val expectedRotated = PolygonShape {
            polygon(3)
        }
            .apply { transform { rotate(90f) } }
            .outlinePath()
        val expectedUnrotated = PolygonShape { polygon(3) }.outlinePath()
        assertPathsMatch(
            "base stays unrotated",
            expectedUnrotated,
            base.outlinePath(),
            tightTolerance,
        )
        assertPathsMatch("copy is rotated", expectedRotated, copy.outlinePath(), tightTolerance)
    }

    private fun Shape.outlinePath(): Path =
        (createOutline(size, LayoutDirection.Ltr, unitDensity) as Outline.Generic).path

    private fun assertPathsMatch(label: String, expected: Path, actual: Path, tolerance: Float) {
        val e = sampleOutline(expected)
        val a = sampleOutline(actual)
        assertTrue("$label: failed to sample paths", e.isNotEmpty() && a.isNotEmpty())
        val divergence = max(directedHausdorff(e, a), directedHausdorff(a, e))
        assertTrue(
            "$label: outlines diverge by ${divergence}px (tolerance ${tolerance}px)",
            divergence <= tolerance,
        )
    }

    /** Samples points along every contour of the path, evenly spaced by arc length. */
    private fun sampleOutline(path: Path, samples: Int = 720): FloatArray {
        val measure = PathMeasure(path.asAndroidPath(), false)
        val points = ArrayList<Float>(samples * 2)
        val position = FloatArray(2)
        do {
            val length = measure.length
            if (length > 0f) {
                for (i in 0 until samples) {
                    measure.getPosTan(length * i / samples, position, null)
                    points.add(position[0])
                    points.add(position[1])
                }
            }
        } while (measure.nextContour())
        return points.toFloatArray()
    }

    /** Max over points in [from] of the distance to the nearest point in [to]. */
    private fun directedHausdorff(from: FloatArray, to: FloatArray): Float {
        var maxMinSquared = 0f
        var i = 0
        while (i < from.size) {
            val x = from[i]
            val y = from[i + 1]
            var bestSquared = Float.MAX_VALUE
            var j = 0
            while (j < to.size) {
                val dx = to[j] - x
                val dy = to[j + 1] - y
                val d = dx * dx + dy * dy
                if (d < bestSquared) bestSquared = d
                j += 2
            }
            if (bestSquared > maxMinSquared) maxMinSquared = bestSquared
            i += 2
        }
        return sqrt(maxMinSquared)
    }
}
