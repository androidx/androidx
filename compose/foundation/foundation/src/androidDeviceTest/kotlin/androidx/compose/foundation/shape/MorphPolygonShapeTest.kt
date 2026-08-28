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
import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding as GraphicsCornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.max
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MorphPolygonShapeTest {

    private val density = Density(2f)
    private val unitDensity = Density(1f)
    private val size = Size(200.0f, 200.0f)
    private val tightTolerance = 2f

    @Test
    fun morphPolygonShape_progressOutsideRange_resolvesToEndpoints() {
        val start = PolygonShape { polygon(3, rounding = CornerRounding(percent = 10)) }
        val end =
            PolygonShape.star(
                numPoints = 8,
                innerRadiusRatio = 0.5f,
                outerRounding = CornerRounding(percent = 10),
            )

        val below = MorphPolygonShape(start, end) { -0.5f }
        assertPathsMatch(
            "morph below 0 vs start",
            start.outlinePath(),
            below.outlinePath(),
            tightTolerance,
        )

        val above = MorphPolygonShape(start, end) { 1.5f }
        assertPathsMatch(
            "morph above 1 vs end",
            end.outlinePath(),
            above.outlinePath(),
            tightTolerance,
        )
    }

    @Test
    fun morphPolygonShape_boundaryProgress_matchesRestingShapes() {
        val start = PolygonShape { polygon(3, rounding = CornerRounding(percent = 10)) }
        val end =
            PolygonShape.star(
                numPoints = 8,
                innerRadiusRatio = 0.5f,
                outerRounding = CornerRounding(percent = 10),
            )

        val atStart = MorphPolygonShape(start, end) { 0f }
        assertPathsMatch(
            "morph at 0 vs start",
            start.outlinePath(),
            atStart.outlinePath(),
            tightTolerance,
        )

        val atEnd = MorphPolygonShape(start, end) { 1f }
        assertPathsMatch(
            "morph at 1 vs end",
            end.outlinePath(),
            atEnd.outlinePath(),
            tightTolerance,
        )
    }

    @Test
    fun morphPolygonShape_sameProgress_returnsCachedOutlineInstance() {
        var progress = 0.37f
        val morph =
            MorphPolygonShape(PolygonShape { polygon(3) }, PolygonShape { polygon(4) }) { progress }
        val first = morph.createOutline(size, LayoutDirection.Ltr, unitDensity)
        val second = morph.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertSame(first, second)

        progress = 0.62f
        val third = morph.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertNotSame(first, third)
    }

    @Test
    fun morphPolygonShape_sizeChange_rebuildsOutline() {
        val morph =
            MorphPolygonShape(PolygonShape { polygon(3) }, PolygonShape { polygon(5) }) { 0.5f }
        val big = morph.createOutline(size, LayoutDirection.Ltr, unitDensity) as Outline.Generic
        assertTrue(big.path.getBounds().right > 100f)

        val small = morph.createOutline(Size(100f, 100f), LayoutDirection.Ltr, unitDensity)
        val smallBounds = (small as Outline.Generic).path.getBounds()
        assertTrue(
            "outline did not shrink with the container: $smallBounds",
            smallBounds.right <= 100.5f,
        )
        assertTrue(smallBounds.bottom <= 100.5f)
    }

    @Test
    fun morphPolygonShape_isValueEqual() {
        val start = PolygonShape.circle(10)
        val end = PolygonShape.star(8, outerRounding = CornerRounding(percent = 20))
        val progress: () -> Float = { 0.5f }
        assertEquals(
            MorphPolygonShape(start, end, progress),
            MorphPolygonShape(start, end, progress),
        )
        assertEquals(
            MorphPolygonShape(start, end, progress).hashCode(),
            MorphPolygonShape(start, end, progress).hashCode(),
        )
        assertNotEquals(
            MorphPolygonShape(start, end, progress),
            MorphPolygonShape(start, end) { 0.5f },
        )
        assertNotEquals(
            MorphPolygonShape(start, end, progress),
            MorphPolygonShape(end, start, progress),
        )
    }

    @Test
    fun morphPolygonShape_tracksBuilderEndpointStateChange() {
        // A builder-backed endpoint reads captured state; the morph must rebuild when the
        // endpoint's geometry changes, not serve the stale Morph.
        var radius = 40f
        val start = PolygonShape { polygon(4, radius = radius) }
        val end = PolygonShape.circle()
        val morph = MorphPolygonShape(start, end) { 0f }

        val first = morph.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertEquals(80f, (first as Outline.Generic).path.getBounds().width, 1f)

        radius = 90f
        val second = morph.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertEquals(180f, (second as Outline.Generic).path.getBounds().width, 1f)
    }

    @Test
    fun roundedCornerConversion_matchesDirectRoundedPolygon() {
        // 16.dp at density 2 is 32px on every corner. Vertices are ordered bottom-right,
        // bottom-left, top-left, top-right.
        val ours = RoundedCornerShape(16.dp).toPolygonShape()
        val reference =
            RoundedPolygon(
                vertices = floatArrayOf(200f, 200f, 0f, 200f, 0f, 0f, 200f, 0f),
                perVertexRounding = List(4) { GraphicsCornerRounding(32f) },
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "rounded corner conversion",
            reference.asComposePath(),
            ours.outlinePath(density),
            tightTolerance,
        )
    }

    @Test
    fun cutCornerConversion_matchesDirectRoundedPolygon() {
        // 20.dp at density 2 is 40px: a beveled octagon with sharp vertices.
        val cut = 40f
        val ours = CutCornerShape(20.dp).toPolygonShape()
        val reference =
            RoundedPolygon(
                vertices =
                    floatArrayOf(
                        cut,
                        0f,
                        200f - cut,
                        0f,
                        200f,
                        cut,
                        200f,
                        200f - cut,
                        200f - cut,
                        200f,
                        cut,
                        200f,
                        0f,
                        200f - cut,
                        0f,
                        cut,
                    ),
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "cut corner conversion",
            reference.asComposePath(),
            ours.outlinePath(density),
            tightTolerance,
        )
    }

    @Test
    fun conversion_rtl_swapsCornersUnlessAbsolute() {
        // topStart lands on the top-right in RTL; the reference pins that corner directly.
        val topRightRounded =
            RoundedPolygon(
                vertices = floatArrayOf(200f, 200f, 0f, 200f, 0f, 0f, 200f, 0f),
                perVertexRounding =
                    listOf(
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(80f),
                    ),
                centerX = 100f,
                centerY = 100f,
            )
        val regular = RoundedCornerShape(topStart = 40.dp).toPolygonShape()
        assertPathsMatch(
            "topStart in RTL is the top-right corner",
            topRightRounded.asComposePath(),
            regular.outlinePath(density, direction = LayoutDirection.Rtl),
            tightTolerance,
        )

        // The absolute variant ignores the layout direction: topLeft stays top-left.
        val topLeftRounded =
            RoundedPolygon(
                vertices = floatArrayOf(200f, 200f, 0f, 200f, 0f, 0f, 200f, 0f),
                perVertexRounding =
                    listOf(
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(80f),
                        GraphicsCornerRounding.Unrounded,
                    ),
                centerX = 100f,
                centerY = 100f,
            )
        val absolute = AbsoluteRoundedCornerShape(topLeft = 40.dp).toPolygonShape()
        assertPathsMatch(
            "absolute conversion does not swap in RTL",
            topLeftRounded.asComposePath(),
            absolute.outlinePath(density, direction = LayoutDirection.Rtl),
            tightTolerance,
        )
    }

    @Test
    fun roundedCornerConversion_oversizedCorners_matchesSourceNormalization() {
        // An 80px start pair on a 100px minimum dimension exceeds it (160 > 100); the source
        // shape scales the pair to 50px per corner, and the conversion must match that outline.
        val tall = Size(100f, 300f)
        val source = RoundedCornerShape(topStart = 80.dp, bottomStart = 80.dp)
        assertPathsMatch(
            "rounded conversion with oversized corners",
            source.outlinePathAt(tall),
            source.toPolygonShape().outlinePathAt(tall),
            tightTolerance,
        )
    }

    @Test
    fun cutCornerConversion_oversizedCorners_matchesSourceNormalization() {
        val tall = Size(100f, 300f)
        val source = CutCornerShape(topStart = 80.dp, bottomStart = 80.dp)
        assertPathsMatch(
            "cut conversion with oversized corners",
            source.outlinePathAt(tall),
            source.toPolygonShape().outlinePathAt(tall),
            tightTolerance,
        )
    }

    @Test
    fun cutCornerConversion_zeroSizedCorners_matchesSource() {
        // A zero cut collapses a bevel into a plain vertex; the conversion must not emit the
        // degenerate zero-length edge.
        val source = CutCornerShape(topStart = 20.dp)
        assertPathsMatch(
            "cut conversion with zero-sized corners",
            source.outlinePathAt(size),
            source.toPolygonShape().outlinePathAt(size),
            tightTolerance,
        )
    }

    @Test
    fun roundedCornerConversion_oversizedAsymmetricCorners_rtl_matchesSource() {
        // In RTL the oversized start pair lands on the right side and still scales like the
        // source; the smaller end pair stays unscaled on the left.
        val tall = Size(100f, 300f)
        val source =
            RoundedCornerShape(
                topStart = 80.dp,
                topEnd = 20.dp,
                bottomEnd = 20.dp,
                bottomStart = 80.dp,
            )
        assertPathsMatch(
            "rtl rounded conversion with oversized corners",
            source.outlinePathAt(tall, LayoutDirection.Rtl),
            source.toPolygonShape().outlinePathAt(tall, LayoutDirection.Rtl),
            tightTolerance,
        )
    }

    @Test
    fun cutCornerConversion_isDistinctFromRounded() {
        val cut = AbsoluteCutCornerShape(15.dp).toPolygonShape()
        val outline = cut.toOutline()
        assertTrue(outline is Outline.Generic)
        assertNotEquals(cut, AbsoluteRoundedCornerShape(15.dp).toPolygonShape())
    }

    @Test
    fun cornerShapeConversion_isValueEqual() {
        val a = RoundedCornerShape(8.dp).toPolygonShape()
        val b = RoundedCornerShape(8.dp).toPolygonShape()
        // Two conversions of equal source shapes are value-equal, so outline caching is stable.
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(
            RoundedCornerShape(8.dp).toPolygonShape(),
            CutCornerShape(8.dp).toPolygonShape(),
        )
    }

    private fun Shape.toOutline(direction: LayoutDirection = LayoutDirection.Ltr) =
        createOutline(size, direction, density)

    private fun Shape.outlinePath(
        density: Density = unitDensity,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ): Path = (createOutline(size, direction, density) as Outline.Generic).path

    /** Resolves the outline at an arbitrary [size] and returns it as a [Path]. */
    private fun Shape.outlinePathAt(
        size: Size,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ): Path {
        val outline = createOutline(size, direction, unitDensity)
        return Path().apply { addOutline(outline) }
    }

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
