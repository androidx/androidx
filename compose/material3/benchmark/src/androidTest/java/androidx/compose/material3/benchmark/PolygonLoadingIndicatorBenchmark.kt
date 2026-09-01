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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.MorphPolygonShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.PolygonShapeGeometry
import androidx.compose.foundation.shape.PolygonShapeGeometry.Companion.CornerRounding
import androidx.compose.foundation.shape.PolygonShapeGeometry.CornerRounding
import androidx.compose.foundation.shape.scaledToFit
import androidx.compose.foundation.shape.transformed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test

/**
 * Composable-level counterpart of [LoadingIndicatorBenchmark]: the same determinate progress-driven
 * morph, but expressed as a PolygonShape used on a drawBehind modifier instead of Material3's
 * LoadingIndicator (which draws its Morph via `Morph.toPath` in a draw modifier). Uses the same
 * test harness and toggle pattern so the `changeProgress` numbers are directly comparable, with the
 * caveat that M3's component also runs its container/track logic.
 */
@MediumTest
class PolygonLoadingIndicatorBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { PolygonLoadingIndicatorTestCase() }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(testCaseFactory)
    }

    @Test
    fun changeProgress() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            caseFactory = testCaseFactory,
            requireRecomposition = false,
        )
    }
}

internal class PolygonLoadingIndicatorTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var state: MutableFloatState

    @Composable
    override fun MeasuredContent() {
        state = remember { mutableFloatStateOf(0f) }
        val morphs = remember {
            val count = IndicatorShapes.size
            List(count) { i ->
                MorphPolygonShape(IndicatorShapes[i], IndicatorShapes[(i + 1) % count]) {
                    val progress = state.floatValue.coerceIn(0f, 1f)
                    val step = 1f / count
                    ((progress - i * step) / step).coerceIn(0f, 1f)
                }
            }
        }
        val shape =
            remember(morphs) {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        val progress = state.floatValue.coerceIn(0f, 1f)
                        val count = morphs.size
                        val step = 1f / count
                        val index = (progress / step).toInt().coerceAtMost(count - 1)
                        return morphs[index].createOutline(size, layoutDirection, density)
                    }
                }
            }

        val color = MaterialTheme.colorScheme.primary
        Spacer(
            Modifier.size(48.dp).drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline = outline, color = color)
            }
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        state.floatValue = if (state.floatValue == 0f) 0.5f else 0.0f
    }
}

private data class PointNRound(val o: Offset, val r: CornerRounding = CornerRounding.Unrounded)

private val cornerRound15 = CornerRounding(percent = 15)
private val cornerRound50 = CornerRounding(percent = 50)
private val rotateNeg45 = Matrix().apply { rotateZ(-45f) }
private val rotateNeg90 = Matrix().apply { rotateZ(-90f) }

private fun Float.toRadians() = this / 360f * 2 * PI.toFloat()

private fun Offset.angleDegrees() = atan2(y, x) * 180f / PI.toFloat()

private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero) =
    (angle.toRadians()).let { a ->
        val off = this - center
        Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
    }

private fun doRepeat(
    points: List<PointNRound>,
    reps: Int,
    center: Offset,
    mirroring: Boolean,
): List<PointNRound> =
    if (mirroring) {
        buildList {
            val MathAngles = points.map { (it.o - center).angleDegrees() }
            val distances = points.map { (it.o - center).getDistance() }
            val actualReps = reps * 2
            val sectionAngle = 360f / actualReps
            repeat(actualReps) {
                points.indices.forEach { index ->
                    val i = if (it % 2 == 0) index else points.lastIndex - index
                    if (i > 0 || it % 2 == 0) {
                        val a =
                            (sectionAngle * it +
                                    if (it % 2 == 0) MathAngles[i]
                                    else sectionAngle - MathAngles[i] + 2 * MathAngles[0])
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

private fun customPolygonShape(
    pnr: List<PointNRound>,
    reps: Int,
    center: Offset = Offset(0.5f, 0.5f),
    mirroring: Boolean = false,
    startRotation: Matrix? = null,
): PolygonShape {
    val points = doRepeat(pnr, reps, center, mirroring)
    val shape =
        PolygonShape(
            PolygonShapeGeometry(
                vertices = points.map { it.o },
                perVertexRounding = points.map { it.r },
                center = center,
            )
        )
    return if (startRotation != null) {
        shape.transformed(startRotation, contentScale = ContentScale.Fit)
    } else {
        shape
    }
}

private val IndicatorShapes: List<PolygonShape> =
    listOf(
        customPolygonShape(
            listOf(
                PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
                PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
            ),
            reps = 10,
        ),
        PolygonShape.star(numPoints = 9, innerRadiusRatio = 0.8f, outerRounding = cornerRound50)
            .transformed(rotateNeg90, contentScale = ContentScale.Fit),
        customPolygonShape(
            listOf(
                PointNRound(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
                PointNRound(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
                PointNRound(Offset(0.828f, 0.970f), CornerRounding(0.169f)),
            ),
            reps = 1,
            mirroring = true,
        ),
        customPolygonShape(
            listOf(
                PointNRound(Offset(0.961f, 0.039f), CornerRounding(0.426f)),
                PointNRound(Offset(1.001f, 0.428f)),
                PointNRound(Offset(1.000f, 0.609f), CornerRounding(1f)),
            ),
            reps = 2,
            mirroring = true,
        ),
        PolygonShape.star(numPoints = 8, innerRadiusRatio = 0.8f, outerRounding = cornerRound15)
            .scaledToFit(),
        customPolygonShape(
            listOf(
                PointNRound(Offset(1.237f, 1.236f), CornerRounding(0.258f)),
                PointNRound(Offset(0.500f, 0.918f), CornerRounding(0.233f)),
            ),
            reps = 4,
        ),
        PolygonShape.circle(numVertices = 10)
            .transformed(
                matrix =
                    Matrix().apply {
                        rotateZ(-45f)
                        scale(1f, 0.64f)
                    },
                contentScale = ContentScale.Fit,
            ),
    )
