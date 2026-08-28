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

package androidx.compose.foundation.benchmark

import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.PolygonShapeGeometry
import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.LargeTest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class PolygonShapeBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val size = Size(200f, 200f)
    private val density = Density(1f)

    @Test
    fun foundationMorphFrame() {
        var progress = 0.01f
        val shape =
            MorphPolygonShape(FoundationShapes.softBurst, FoundationShapes.star9) { progress }
        benchmarkRule.measureRepeated {
            progress += 0.049f
            if (progress >= 0.99f) progress = 0.01f
            shape.createOutline(size, LayoutDirection.Ltr, density)
        }
    }

    @Test
    fun foundationStaticShapeOutline() {
        // Steady-state: PolygonShape caches its resolved outline per size, so this measures the
        // cache-hit path (the equivalent of M3's cached-unit-path fast path).
        val shape = FoundationShapes.star9
        benchmarkRule.measureRepeated { shape.createOutline(size, LayoutDirection.Ltr, density) }
    }

    @Test
    fun foundationStaticShapeOutlineResized() {
        // Alternating sizes defeat the per-size cache, measuring the cold rebuild cost that is
        // paid once per size change (geometry construction + path).
        val shape = FoundationShapes.star9
        val sizes = arrayOf(size, Size(240f, 240f))
        var i = 0
        benchmarkRule.measureRepeated {
            val s = sizes[i]
            i = i xor 1
            shape.createOutline(s, LayoutDirection.Ltr, density)
        }
    }

    @Test
    fun foundationPlainPolygonOutline() {
        val shape = PolygonShape { polygon(5, rounding = CornerRounding(percent = 20)) }
        benchmarkRule.measureRepeated { shape.createOutline(size, LayoutDirection.Ltr, density) }
    }

    @Test
    fun foundationUnitSpaceShapeOutlineResized() {
        // Unit-space geometry shape: rebuild includes the resolve + fit-center materialization.
        val shape = FoundationShapes.softBurst
        val sizes = arrayOf(size, Size(240f, 240f))
        var i = 0
        benchmarkRule.measureRepeated {
            val s = sizes[i]
            i = i xor 1
            shape.createOutline(s, LayoutDirection.Ltr, density)
        }
    }
}

/** Shapes built with M3's construction values (softBurst vertices, cookie star parameters). */
internal object FoundationShapes {
    private fun Float.toRadians() = this / 360f * 2 * PI.toFloat()

    private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero): Offset {
        val a = angle.toRadians()
        val off = this - center
        return Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
    }

    private data class PointNRound(
        val o: Offset,
        val r: PolygonShapeGeometry.CornerRounding = PolygonShapeGeometry.CornerRounding.Unrounded,
    )

    private fun repeat(points: List<PointNRound>, reps: Int, center: Offset): List<PointNRound> =
        (0 until points.size * reps).map {
            val p = points[it % points.size]
            PointNRound(p.o.rotateDegrees((it / points.size) * 360f / reps, center), p.r)
        }

    val softBurst: PolygonShape = run {
        val points =
            repeat(
                listOf(
                    PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
                    PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
                ),
                reps = 10,
                center = Offset(0.5f, 0.5f),
            )
        PolygonShape {
            polygon(
                vertices = points.map { it.o },
                perVertexRounding = points.map { it.r },
                center = Offset(0.5f, 0.5f),
            )
        }
    }

    val star9: PolygonShape =
        PolygonShape.star(
            numPoints = 9,
            innerRadiusRatio = 0.8f,
            outerRounding = CornerRounding(percent = 50),
        )
}
