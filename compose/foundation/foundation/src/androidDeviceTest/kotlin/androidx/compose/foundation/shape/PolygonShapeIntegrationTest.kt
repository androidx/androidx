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

import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.foundation.shape.PolygonShapeGeometry.CornerRounding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration coverage for the composite usage patterns exercised by the ui-demos
 * GraphicsShapesDemo (which has no test source set of its own): Material-style shape definitions
 * built on the public API, resolved across sizes and morphed across kinds.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class PolygonShapeIntegrationTest {

    private val density = Density(2f)
    private val sizes = listOf(Size(200f, 200f), Size(96f, 96f), Size(320f, 180f))

    /** Mirrors the demo's M3-style shape list: unit-space customs, stars, circle, converter. */
    private fun demoShapes(): List<PolygonShape> {
        fun repeated(points: List<Pair<Offset, CornerRounding>>, reps: Int): PolygonShapeGeometry {
            val center = Offset(0.5f, 0.5f)
            val out =
                (0 until points.size * reps).map { i ->
                    val (o, r) = points[i % points.size]
                    val angle = (i / points.size) * 2f * PI.toFloat() / reps
                    val off = o - center
                    Offset(
                        off.x * cos(angle) - off.y * sin(angle) + center.x,
                        off.x * sin(angle) + off.y * cos(angle) + center.y,
                    ) to r
                }
            return PolygonShapeGeometry(
                vertices = out.map { it.first },
                perVertexRounding = out.map { it.second },
                center = center,
            )
        }
        return listOf(
            PolygonShape(
                repeated(
                    listOf(
                        Offset(0.193f, 0.277f) to CornerRounding(0.053f),
                        Offset(0.176f, 0.055f) to CornerRounding(0.053f),
                    ),
                    reps = 10,
                )
            ),
            PolygonShape.star(9, 0.8f, outerRounding = CornerRounding(percent = 50)),
            PolygonShape(
                repeated(
                    listOf(
                        Offset(1.237f, 1.236f) to CornerRounding(0.258f),
                        Offset(0.500f, 0.918f) to CornerRounding(0.233f),
                    ),
                    reps = 4,
                )
            ),
            PolygonShape.star(8, 0.8f, outerRounding = CornerRounding(percent = 15)),
            PolygonShape.circle(10),
            RoundedCornerShape(12.dp).toPolygonShape(),
        )
    }

    @Test
    fun demoShapes_resolveAtMultipleSizes_withinBounds() {
        val tolerance = 3f
        for (shape in demoShapes()) {
            for (size in sizes) {
                val outline = shape.createOutline(size, LayoutDirection.Ltr, density)
                assertTrue(outline is Outline.Generic)
                val bounds = (outline as Outline.Generic).path.getBounds()
                assertTrue(
                    "outline escaped container $size: $bounds",
                    bounds.left >= -tolerance &&
                        bounds.top >= -tolerance &&
                        bounds.right <= size.width + tolerance &&
                        bounds.bottom <= size.height + tolerance,
                )
                assertTrue("outline is degenerate at $size", bounds.width > size.width / 4f)
            }
        }
    }

    @Test
    fun demoShapes_morphAcrossAllConsecutivePairs() {
        val shapes = demoShapes()
        for (i in shapes.indices) {
            val start = shapes[i]
            val end = shapes[(i + 1) % shapes.size]
            var progress = 0f
            val morph = MorphPolygonShape(start, end) { progress }
            for (p in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                progress = p
                val outline = morph.createOutline(sizes[0], LayoutDirection.Ltr, density)
                assertTrue("pair $i at progress $p", outline is Outline.Generic)
                val bounds = (outline as Outline.Generic).path.getBounds()
                assertTrue("morph outline degenerate for pair $i at $p", bounds.width > 0f)
            }
        }
    }

    @Test
    fun demoShapes_morphSurvivesSizeChangeMidProgress() {
        val tolerance = 3f
        val shapes = demoShapes()
        var progress = 0.5f
        val morph = MorphPolygonShape(shapes[0], shapes[1]) { progress }
        morph.createOutline(sizes[0], LayoutDirection.Ltr, density)
        progress = 0.6f
        val resized = morph.createOutline(sizes[1], LayoutDirection.Ltr, density)
        val bounds = (resized as Outline.Generic).path.getBounds()
        assertTrue(
            "morph outline did not track the new container: $bounds",
            bounds.right <= sizes[1].width + tolerance &&
                bounds.bottom <= sizes[1].height + tolerance,
        )
    }
}
