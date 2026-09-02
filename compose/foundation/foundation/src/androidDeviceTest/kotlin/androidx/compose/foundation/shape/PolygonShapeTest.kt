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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding as GraphicsCornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PolygonShapeTest {

    private val density = Density(2f)
    private val unitDensity = Density(1f)
    private val size = Size(200.0f, 200.0f)
    private val sizePx = 200f
    private val tightTolerance = 2f

    @Test
    fun regularPolygon_matchesDirectRoundedPolygon() {
        // 8.dp at density 2 is 16px; default radius is minDimension / 2 = 100, centered.
        val polygonShape =
            PolygonShape.regularPolygon(
                numVertices = 5,
                rounding = CornerRounding(radius = 8.dp, smoothing = 0.5f),
            )
        val reference =
            RoundedPolygon(
                numVertices = 5,
                radius = 100f,
                centerX = 100f,
                centerY = 100f,
                rounding = GraphicsCornerRounding(16f, smoothing = 0.5f),
            )
        assertPathsMatch(
            "pentagon top-level vs direct RoundedPolygon",
            reference.asComposePath(),
            polygonShape.outlinePath(density),
            tightTolerance,
        )
    }

    @Test
    fun dpRounding_matchesEquivalentPxRounding() {
        // A raw float radius is pixels irrespective of density, so 12.dp at density 2 must equal
        // 24f at the same density.
        val viaDp = PolygonShape { polygon(5, rounding = CornerRounding(radius = 12.dp)) }
        val viaPx = PolygonShape { polygon(5, rounding = CornerRounding(radius = 24f)) }
        assertPathsMatch(
            "12.dp at density 2 vs 24px",
            viaPx.outlinePath(density),
            viaDp.outlinePath(density),
            tightTolerance,
        )
        assertNotEquals(
            "dp and px shapes at different resolved radii must differ",
            sampleOutline(viaDp.outlinePath(unitDensity)).toList(),
            sampleOutline(viaDp.outlinePath(density)).toList(),
        )
    }

    @Test
    fun percentRounding_isFractionOfGeneratingRadius() {
        // 10% of the default radius (100px) is a 10px rounding.
        val polygonShape = PolygonShape { polygon(5, rounding = CornerRounding(percent = 10)) }
        val reference =
            RoundedPolygon(
                numVertices = 5,
                radius = 100f,
                centerX = 100f,
                centerY = 100f,
                rounding = GraphicsCornerRounding(10f),
            )
        assertPathsMatch(
            "10 percent vs 10px at radius 100",
            reference.asComposePath(),
            polygonShape.outlinePath(unitDensity),
            tightTolerance,
        )
    }

    @Test
    fun perVertexPercentRounding_isFractionOfGeneratingRadius() {
        // Per-vertex percent in the count factory resolves against the generating radius too:
        // 20% of radius 100 is a 20px rounding at every vertex.
        val percentShape = PolygonShape {
            polygon(4, perVertexRounding = List(4) { CornerRounding(percent = 20) }, radius = 100f)
        }
        val pxShape = PolygonShape {
            polygon(4, perVertexRounding = List(4) { CornerRounding(radius = 20f) }, radius = 100f)
        }
        assertPathsMatch(
            "per-vertex 20 percent vs 20px at radius 100",
            pxShape.outlinePath(unitDensity),
            percentShape.outlinePath(unitDensity),
            tightTolerance,
        )
    }

    @Test
    fun customVertices_matchDirectRoundedPolygon() {
        val vertices = listOf(Offset(100f, 0f), Offset(200f, 200f), Offset(0f, 200f))
        val polygonShape = PolygonShape { polygon(vertices) }
        val reference =
            RoundedPolygon(
                vertices = floatArrayOf(100f, 0f, 200f, 200f, 0f, 200f),
                centerX = Float.MIN_VALUE,
                centerY = Float.MIN_VALUE,
            )
        assertPathsMatch(
            "custom triangle vs direct RoundedPolygon",
            reference.asComposePath(),
            polygonShape.outlinePath(unitDensity),
            tightTolerance,
        )
    }

    @Test
    fun perVertexRounding_appliesAtMatchingVertices() {
        val vertices = listOf(Offset(100f, 0f), Offset(200f, 200f), Offset(0f, 200f))
        val perVertex =
            listOf(
                CornerRounding(40f),
                PolygonShapeGeometry.CornerRounding.Unrounded,
                CornerRounding(10f),
            )
        val shape = PolygonShape { polygon(vertices, perVertexRounding = perVertex) }
        val reference =
            RoundedPolygon(
                vertices = floatArrayOf(100f, 0f, 200f, 200f, 0f, 200f),
                perVertexRounding =
                    listOf(
                        GraphicsCornerRounding(40f),
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(10f),
                    ),
                centerX = Float.MIN_VALUE,
                centerY = Float.MIN_VALUE,
            )
        assertPathsMatch(
            "per-vertex rounding placement",
            reference.asComposePath(),
            shape.outlinePath(unitDensity),
            tightTolerance,
        )
    }

    @Test
    fun star_matchesDirectRoundedPolygon() {
        val shape =
            PolygonShape.star(
                numPoints = 8,
                innerRadiusRatio = 0.5f,
                outerRounding = CornerRounding(radius = 8.dp),
                innerRounding = CornerRounding(radius = 4.dp),
            )
        val reference =
            RoundedPolygon.star(
                numVerticesPerRadius = 8,
                radius = 100f,
                innerRadius = 50f,
                rounding = GraphicsCornerRounding(16f),
                innerRounding = GraphicsCornerRounding(8f),
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "star vs direct RoundedPolygon.star",
            reference.asComposePath(),
            shape.outlinePath(density),
            tightTolerance,
        )
    }

    @Test
    fun pill_nonSquareContainer_matchesDirectRoundedPolygon() {
        val container = Size(300f, 150f)
        val shape = PolygonShape.pill(smoothing = 0.5f)
        val reference =
            RoundedPolygon.pill(
                width = 300f,
                height = 150f,
                smoothing = 0.5f,
                centerX = 150f,
                centerY = 75f,
            )
        assertPathsMatch(
            "pill in 300x150",
            reference.asComposePath(),
            shape.outlinePath(unitDensity, container),
            tightTolerance,
        )
    }

    @Test
    fun pillStar_nonSquareContainer_matchesDirectRoundedPolygon() {
        // The container is inset by half its min dimension, so a 300x100 container builds a
        // RoundedPolygon.pillStar of 250x50 centered in the container.
        val container = Size(300f, 100f)
        val shape =
            PolygonShape.pillStar(numPoints = 8, outerRounding = CornerRounding(radius = 12f))
        val reference =
            RoundedPolygon.pillStar(
                width = 250f,
                height = 50f,
                numVerticesPerRadius = 8,
                rounding = GraphicsCornerRounding(12f),
                centerX = 150f,
                centerY = 50f,
            )
        assertPathsMatch(
            "pillStar in 300x100",
            reference.asComposePath(),
            shape.outlinePath(unitDensity, container),
            tightTolerance,
        )
    }

    @Test
    fun pillStar_percentRounding_isFractionOfOuterRadius() {
        // In a 300x100 container the star's outer radius is min(250, 50) = 50, so 40 percent is
        // a 20px rounding — matching the star factory's percent convention.
        val container = Size(300f, 100f)
        val shape =
            PolygonShape.pillStar(numPoints = 8, outerRounding = CornerRounding(percent = 40))
        val reference =
            RoundedPolygon.pillStar(
                width = 250f,
                height = 50f,
                numVerticesPerRadius = 8,
                rounding = GraphicsCornerRounding(20f),
                centerX = 150f,
                centerY = 50f,
            )
        assertPathsMatch(
            "pillStar percent rounding reference",
            reference.asComposePath(),
            shape.outlinePath(unitDensity, container),
            tightTolerance,
        )
    }

    @Test
    fun circle_matchesDirectRoundedPolygon() {
        val shape = PolygonShape.circle(numVertices = 12)
        val reference =
            RoundedPolygon.circle(numVertices = 12, radius = 100f, centerX = 100f, centerY = 100f)
        assertPathsMatch(
            "circle vs direct RoundedPolygon.circle",
            reference.asComposePath(),
            shape.outlinePath(unitDensity),
            tightTolerance,
        )
    }

    @Test
    fun rectangle_matchesDirectRoundedPolygon() {
        val unrounded = PolygonShape.rectangle()
        val unroundedReference =
            RoundedPolygon.rectangle(width = 200f, height = 200f, centerX = 100f, centerY = 100f)
        assertPathsMatch(
            "unrounded rectangle",
            unroundedReference.asComposePath(),
            unrounded.outlinePath(unitDensity),
            tightTolerance,
        )

        // 10.dp at density 2 is 20px.
        val rounded = PolygonShape.rectangle(rounding = CornerRounding(radius = 10.dp))
        val roundedReference =
            RoundedPolygon.rectangle(
                width = 200f,
                height = 200f,
                rounding = GraphicsCornerRounding(20f),
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "rounded rectangle",
            roundedReference.asComposePath(),
            rounded.outlinePath(density),
            tightTolerance,
        )
    }

    @Test
    fun rectangle_perCornerRounding_appliesAtMatchingCorners() {
        val shape =
            PolygonShape.rectangle(
                topStartRounding = CornerRounding(radius = 40f),
                bottomEndRounding = CornerRounding(radius = 12f),
            )
        val reference =
            RoundedPolygon.rectangle(
                width = 200f,
                height = 200f,
                // RoundedPolygon.rectangle vertex order: bottom right, bottom left, top left,
                // top right.
                perVertexRounding =
                    listOf(
                        GraphicsCornerRounding(12f),
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(40f),
                        GraphicsCornerRounding.Unrounded,
                    ),
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "per-corner rectangle rounding placement in LTR",
            reference.asComposePath(),
            shape.outlinePath(unitDensity, layoutDirection = LayoutDirection.Ltr),
            tightTolerance,
        )
    }

    @Test
    fun rectangle_perCornerRounding_mirrorsInRtl() {
        val shape =
            PolygonShape.rectangle(
                topStartRounding = CornerRounding(radius = 40f),
                bottomEndRounding = CornerRounding(radius = 12f),
            )
        // In RTL, topStart is top-right and bottomEnd is bottom-left.
        val rtlReference =
            RoundedPolygon.rectangle(
                width = 200f,
                height = 200f,
                // RoundedPolygon.rectangle vertex order: bottom right, bottom left, top left,
                // top right.
                perVertexRounding =
                    listOf(
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(12f),
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(40f),
                    ),
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "per-corner rectangle rounding placement in RTL",
            rtlReference.asComposePath(),
            shape.outlinePath(unitDensity, layoutDirection = LayoutDirection.Rtl),
            tightTolerance,
        )
    }

    @Test
    fun absoluteRectangle_doesNotMirrorInRtl() {
        val shape =
            PolygonShape.absoluteRectangle(
                topLeftRounding = CornerRounding(radius = 40f),
                bottomRightRounding = CornerRounding(radius = 12f),
            )
        // In absoluteRectangle, topLeft remains topLeft and bottomRight remains bottomRight even in
        // RTL.
        val reference =
            RoundedPolygon.rectangle(
                width = 200f,
                height = 200f,
                perVertexRounding =
                    listOf(
                        GraphicsCornerRounding(12f),
                        GraphicsCornerRounding.Unrounded,
                        GraphicsCornerRounding(40f),
                        GraphicsCornerRounding.Unrounded,
                    ),
                centerX = 100f,
                centerY = 100f,
            )
        assertPathsMatch(
            "absolute rectangle in RTL",
            reference.asComposePath(),
            shape.outlinePath(unitDensity, layoutDirection = LayoutDirection.Rtl),
            tightTolerance,
        )
    }

    @Test
    fun scopePolygonFactory_defaults() {
        // The count-based factory defaults: radius = minDimension / 2, center = container center.
        val polygon = PolygonShape {
            polygon(6)
        }
            .buildPolygon(size, LayoutDirection.Ltr, unitDensity)
        assertEquals(sizePx / 2f, polygon.centerX, 1e-2f)
        assertEquals(sizePx / 2f, polygon.centerY, 1e-2f)
        val bounds = polygon.calculateBounds(FloatArray(4), false)
        assertEquals(sizePx, bounds[2] - bounds[0], 1f)
    }

    // Unit-space geometry

    @Test
    fun roundingPercent_scalesWithGeometry() {
        // The same percent at different authoring radii produces the same shape once fit into
        // equal bounds, because the rounding scales with the geometry.
        fun pentagonAt(radius: Float): PolygonShape {
            val vertices =
                List(5) { i ->
                    val angle = PI.toFloat() / 5 * 2 * i
                    Offset(radius * cos(angle), radius * sin(angle))
                }
            return PolygonShape(
                PolygonShapeGeometry(vertices, rounding = CornerRounding(percent = 20))
            )
        }
        assertPathsMatch(
            "percent rounding at radius 50 vs 400",
            pentagonAt(50f).outlinePath(unitDensity),
            pentagonAt(400f).outlinePath(unitDensity),
            tightTolerance,
        )
    }

    @Test
    fun unitSpaceShape_isFitCenteredByDefault() {
        val shape =
            PolygonShape(
                PolygonShapeGeometry(
                    vertices =
                        listOf(
                            Offset(0.5f, 0f),
                            Offset(1f, 0.5f),
                            Offset(0.5f, 1f),
                            Offset(0f, 0.5f),
                        )
                )
            )
        val polygon = shape.buildPolygon(size, LayoutDirection.Ltr, unitDensity)
        val bounds = polygon.calculateBounds(FloatArray(4), false)
        assertEquals(0f, bounds[0], 1e-2f)
        assertEquals(0f, bounds[1], 1e-2f)
        assertEquals(sizePx, bounds[2], 1e-2f)
        assertEquals(sizePx, bounds[3], 1e-2f)
    }

    @Test
    fun unitSpaceShape_fitPreservesAspectRatio() {
        // A 2:1 author-space rectangle in a square container must not stretch.
        val shape =
            PolygonShape(
                PolygonShapeGeometry(
                    vertices =
                        listOf(Offset(0f, 0f), Offset(2f, 0f), Offset(2f, 1f), Offset(0f, 1f))
                )
            )
        val polygon = shape.buildPolygon(size, LayoutDirection.Ltr, unitDensity)
        val bounds = polygon.calculateBounds(FloatArray(4), false)
        assertEquals(sizePx, bounds[2] - bounds[0], 1e-2f)
        assertEquals(sizePx / 2f, bounds[3] - bounds[1], 1e-2f)
        assertEquals(sizePx / 2f, (bounds[1] + bounds[3]) / 2f, 1e-2f)
    }

    @Test
    fun createOutline_sameInputs_returnsCachedInstance() {
        val shape = PolygonShape.star(8, outerRounding = CornerRounding(percent = 20))
        val first = shape.createOutline(size, LayoutDirection.Ltr, unitDensity)
        val second = shape.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertThat(second).isSameInstanceAs(first)

        val resized = shape.createOutline(Size(100f, 100f), LayoutDirection.Ltr, unitDensity)
        assertThat(resized).isNotSameInstanceAs(first)
    }

    @Test
    fun builderShape_recomputesWhenCapturedStateChanges() {
        // The builder lambda reads captured mutable state while the shape instance (and the
        // resolution inputs) stay the same; the outline must track the state, not go stale.
        var radius = 40f
        val shape = PolygonShape { polygon(4, radius = radius) }

        val first = shape.createOutline(size, LayoutDirection.Ltr, unitDensity)
        val firstBounds = (first as Outline.Generic).path.getBounds()
        assertEquals(80f, firstBounds.width, 1f)

        radius = 90f
        val second = shape.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertNotSame(first, second)
        val secondBounds = (second as Outline.Generic).path.getBounds()
        assertEquals(180f, secondBounds.width, 1f)

        // Unchanged capture keeps returning the cached outline instance.
        val third = shape.createOutline(size, LayoutDirection.Ltr, unitDensity)
        assertSame(second, third)
    }

    @Test
    fun cornerRounding_isValueEqual() {
        assertEquals(CornerRounding(percent = 20), CornerRounding(percent = 20))
        assertEquals(
            CornerRounding(percent = 20).hashCode(),
            CornerRounding(percent = 20).hashCode(),
        )
        assertNotEquals(CornerRounding(percent = 20), CornerRounding(percent = 30))
        assertNotEquals(CornerRounding(radius = 8f), CornerRounding(radius = 8.dp))
    }

    @Test
    fun polygonGeometry_isValueEqual() {
        fun make(percent: Int = 20) =
            PolygonShapeGeometry(
                vertices = listOf(Offset(0.5f, 0f), Offset(1f, 1f), Offset(0f, 1f)),
                rounding = CornerRounding(percent = percent),
                center = Offset(0.5f, 0.5f),
            )
        assertEquals(make(), make())
        assertEquals(make().hashCode(), make().hashCode())
        assertNotEquals(make(percent = 20), make(percent = 30))

        fun makePerVertex() =
            PolygonShapeGeometry(
                vertices = listOf(Offset(0.5f, 0f), Offset(1f, 1f), Offset(0f, 1f)),
                perVertexRounding =
                    listOf(
                        CornerRounding(0.1f),
                        CornerRounding(0.2f),
                        PolygonShapeGeometry.CornerRounding.Unrounded,
                    ),
            )
        assertEquals(makePerVertex(), makePerVertex())
        assertEquals(makePerVertex().hashCode(), makePerVertex().hashCode())
        assertNotEquals(make(), makePerVertex())
    }

    @Test
    fun namedFactories_areValueEqual() {
        assertEquals(
            PolygonShape.star(8, 0.6f, CornerRounding(percent = 15)),
            PolygonShape.star(8, 0.6f, CornerRounding(percent = 15)),
        )
        assertNotEquals(
            PolygonShape.star(8, 0.6f, CornerRounding(percent = 15)),
            PolygonShape.star(8, 0.6f, CornerRounding(percent = 20)),
        )
        assertEquals(
            PolygonShape.regularPolygon(5, CornerRounding(percent = 15)),
            PolygonShape.regularPolygon(5, CornerRounding(percent = 15)),
        )
        assertNotEquals(
            PolygonShape.regularPolygon(5, CornerRounding(percent = 15)),
            PolygonShape.regularPolygon(6, CornerRounding(percent = 15)),
        )
        assertNotEquals(
            PolygonShape.regularPolygon(5, CornerRounding(percent = 15)),
            PolygonShape.regularPolygon(5, CornerRounding(percent = 20)),
        )
        assertEquals(PolygonShape.circle(10), PolygonShape.circle(10))
        assertNotEquals(PolygonShape.circle(10), PolygonShape.circle(12))
        assertEquals(PolygonShape.pill(0.3f), PolygonShape.pill(0.3f))
        assertEquals(
            PolygonShape.pillStar(8, 0.5f, outerRounding = CornerRounding(percent = 10)),
            PolygonShape.pillStar(8, 0.5f, outerRounding = CornerRounding(percent = 10)),
        )
        assertEquals(
            PolygonShape.rectangle(CornerRounding(percent = 30)),
            PolygonShape.rectangle(CornerRounding(percent = 30)),
        )
        assertEquals(
            PolygonShape.absoluteRectangle(topLeftRounding = CornerRounding(percent = 30)),
            PolygonShape.absoluteRectangle(topLeftRounding = CornerRounding(percent = 30)),
        )
        assertNotEquals(
            PolygonShape.rectangle(topStartRounding = CornerRounding(percent = 30)),
            PolygonShape.absoluteRectangle(topLeftRounding = CornerRounding(percent = 30)),
        )
    }

    @Test
    fun geometryShape_isValueEqual() {
        fun make() =
            PolygonShape(
                PolygonShapeGeometry(
                    vertices = listOf(Offset(0.5f, 0f), Offset(1f, 1f), Offset(0f, 1f)),
                    rounding = CornerRounding(0.2f, smoothing = 0.5f),
                    center = Offset(0.5f, 0.5f),
                )
            )
        assertEquals(make(), make())
        assertEquals(make().hashCode(), make().hashCode())
    }

    @Test
    fun builderShape_equalOnSameLambdaInstance() {
        val builder: PolygonShapeScope.() -> PolygonShapeGeometry = { polygon(6) }
        assertEquals(PolygonShape(builder), PolygonShape(builder))
        assertEquals(PolygonShape(builder).hashCode(), PolygonShape(builder).hashCode())
        assertNotEquals(PolygonShape(builder), PolygonShape { polygon(6) })
    }

    @Test
    fun scopeFactory_mismatchedPerVertexRounding_throwsAtResolution() {
        val invalidShape = PolygonShape {
            polygon(
                5,
                perVertexRounding = List(3) { PolygonShapeGeometry.CornerRounding.Unrounded },
            )
        }
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                invalidShape.createOutline(size, LayoutDirection.Ltr, density)
            }
        assertThat(e).hasMessageThat().contains("perVertexRounding has 3")
    }

    @Test
    fun polygonGeometry_validatesInputs() {
        val tooFew =
            assertThrows(IllegalArgumentException::class.java) {
                PolygonShapeGeometry(vertices = listOf(Offset(0f, 0f), Offset(1f, 0f)))
            }
        assertThat(tooFew).hasMessageThat().contains("at least 3 vertices")

        val mismatched =
            assertThrows(IllegalArgumentException::class.java) {
                PolygonShapeGeometry(
                    vertices = listOf(Offset(0.5f, 0f), Offset(1f, 1f), Offset(0f, 1f)),
                    perVertexRounding = List(2) { PolygonShapeGeometry.CornerRounding.Unrounded },
                )
            }
        assertThat(mismatched).hasMessageThat().contains("perVertexRounding has 2")
    }

    @Test
    fun cornerRounding_validatesRanges() {
        assertThat(
                assertThrows(IllegalArgumentException::class.java) { CornerRounding(percent = 101) }
            )
            .hasMessageThat()
            .contains("percent must be in the range 0..100")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) { CornerRounding(radius = -1f) }
            )
            .hasMessageThat()
            .contains("radius must be non-negative")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    CornerRounding(radius = (-1).dp)
                }
            )
            .hasMessageThat()
            .contains("radius must be non-negative")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    CornerRounding(radius = 0.2f, smoothing = 1.5f)
                }
            )
            .hasMessageThat()
            .contains("smoothing must be in the range 0..1")
    }

    @Test
    fun factories_validateRanges() {
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    PolygonShape.regularPolygon(2)
                }
            )
            .hasMessageThat()
            .contains("at least 3 vertices")
        assertThat(assertThrows(IllegalArgumentException::class.java) { PolygonShape.star(1) })
            .hasMessageThat()
            .contains("at least 2 points")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    PolygonShape.star(8, innerRadiusRatio = 0f)
                }
            )
            .hasMessageThat()
            .contains("innerRadiusRatio")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    PolygonShape.pill(smoothing = -0.5f)
                }
            )
            .hasMessageThat()
            .contains("smoothing must be in the range 0..1")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    PolygonShape.pillStar(8, vertexSpacing = 1.5f)
                }
            )
            .hasMessageThat()
            .contains("vertexSpacing must be in the range 0..1")
        assertThat(
                assertThrows(IllegalArgumentException::class.java) {
                    PolygonShape.pillStar(8, startLocation = -0.1f)
                }
            )
            .hasMessageThat()
            .contains("startLocation must be in the range 0..1")
    }

    @Test
    fun circle_delegatesVertexCountValidation() {
        // graphics-shapes validates the count; confirm the failure surfaces at resolution.
        val circle = PolygonShape.circle(numVertices = 2)
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                circle.createOutline(size, LayoutDirection.Ltr, density)
            }
        assertThat(e).hasMessageThat().contains("at least three vertices")
    }

    private fun Shape.outlinePath(
        density: Density,
        container: Size = size,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    ): Path = (createOutline(container, layoutDirection, density) as Outline.Generic).path

    private fun PolygonShape.buildPolygon(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): RoundedPolygon = (this as CachingPolygonShape).buildPolygon(size, layoutDirection, density)

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
