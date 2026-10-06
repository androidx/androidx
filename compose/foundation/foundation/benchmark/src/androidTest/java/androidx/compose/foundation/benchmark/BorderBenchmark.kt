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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.benchmark

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerRounding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmarks for [androidx.compose.foundation.border], covering every [Outline] type.
 *
 * @param shapeName only used for naming the test
 * @param shape the shape of the border
 * @param brushName only used for naming the test
 * @param brush the brush of the border
 * @param alternateBrush a different brush of the same type as [brush], used when toggling the brush
 * @param newImplementation value for [ComposeFoundationFlags.isNewBorderImplementationEnabled]
 */
@LargeTest
@RunWith(Parameterized::class)
class BorderBenchmark(
    @Suppress("unused") private val shapeName: String,
    private val shape: Shape,
    @Suppress("unused") private val brushName: String,
    private val brush: Brush,
    private val alternateBrush: Brush,
    private val newImplementation: Boolean,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "shape={0},brush={2},newImpl={5}")
        fun initParameters(): List<Array<Any>> {
            val shapes =
                listOf(
                    // Outline.Rectangle
                    "Rectangle" to RectangleShape,
                    // Outline.Rounded with equal corner radii (RoundRect.isSimple)
                    "SimpleRoundedRect" to RoundedCornerShape(16.dp),
                    // Outline.Rounded with different corner radii
                    "ComplexRoundedRect" to
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 8.dp,
                            bottomStart = 24.dp,
                        ),
                    // Simple Outline.Generic
                    "SimpleGeneric" to
                        GenericShape { size, _ ->
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        },
                    // Complex Outline.Generic
                    "ComplexGeneric" to
                        PolygonShape.star(
                            numPoints = 9,
                            innerRadiusRatio = 0.9f,
                            outerRounding = CornerRounding.fraction(0.2f),
                        ),
                )
            val brushes = buildList {
                add(arrayOf("Color", SolidColor(Color.Blue), SolidColor(Color.Red)))
                add(arrayOf("Gradient", GradientBrush, AlternateGradientBrush))
                if (Build.VERSION.SDK_INT >= 33) {
                    add(
                        arrayOf(
                            "RuntimeShader",
                            redRuntimeShaderBrush,
                            blueRuntimeShaderBrush,
                        )
                    )
                }
            }
            return shapes.flatMap { (shapeName, shape) ->
                brushes.flatMap { (brushName, brush, alternateBrush) ->
                    listOf(true, false).map { newImpl ->
                        arrayOf(shapeName, shape, brushName, brush, alternateBrush, newImpl)
                    }
                }
            }
        }
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private var previousFlagValue = ComposeFoundationFlags.isNewBorderImplementationEnabled

    @Before
    fun setup() {
        previousFlagValue = ComposeFoundationFlags.isNewBorderImplementationEnabled
        ComposeFoundationFlags.isNewBorderImplementationEnabled = newImplementation
    }

    @After
    fun teardown() {
        ComposeFoundationFlags.isNewBorderImplementationEnabled = previousFlagValue
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel { createTestCase() }
    }

    @Test
    fun firstDraw() {
        benchmarkRule.benchmarkFirstDraw { createTestCase() }
    }

    /**
     * Re-draw with no border parameter changes. Another draw modifier on the same node reads state
     * during draw, so the layer is re-recorded and the border must draw again without its width /
     * brush / shape changing. This models common cases such as a ripple or animated background /
     * content inside a bordered component (e.g. an outlined button being pressed), and measures how
     * well the border caches across repeated draws.
     */
    @Test
    fun redraw_noBorderChange() {
        benchmarkRule.toggleStateBenchmarkDraw(
            { createTestCase(hasBackgroundColor = true) { toggleBackground() } },
            toggleCausesRecompose = false,
        )
    }

    @Test
    fun toggleBrush_draw() {
        benchmarkRule.toggleStateBenchmarkDraw({ createTestCase { toggleBrush() } })
    }

    @Test
    fun toggleWidth_draw() {
        benchmarkRule.toggleStateBenchmarkDraw({ createTestCase { toggleWidth() } })
    }

    private fun createTestCase(
        hasBackgroundColor: Boolean = false,
        onToggle: BorderTestCase.() -> Unit = {},
    ): BorderTestCase {
        if (
            brushName == "RuntimeShader" &&
                (shapeName == "SimpleGeneric" || shapeName == "ComplexGeneric")
        ) {
            throw AssumptionViolatedException(
                "b/570461408 - currently unsupported for generic shapes"
            )
        }
        return BorderTestCase(shape, brush, alternateBrush, hasBackgroundColor, onToggle)
    }
}

/**
 * @param shape the shape of the border
 * @param brush the brush of the border
 * @param alternateBrush the brush used after [toggleBrush] is called
 * @param onToggle invoked by [toggleState]
 */
private class BorderTestCase(
    private val shape: Shape,
    private val brush: Brush,
    private val alternateBrush: Brush,
    private val hasBackgroundColor: Boolean,
    private val onToggle: BorderTestCase.() -> Unit,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val useAlternateBrush = mutableStateOf(false)
    private val borderWidth = mutableStateOf(2.dp)
    private val backgroundColor = mutableStateOf(Color.White)

    @Composable
    override fun MeasuredContent() {
        Box(
            Modifier.size(100.dp)
                .then(
                    if (hasBackgroundColor) {
                        // Reads state only in the draw phase, so toggling it re-records this node's
                        // layer
                        // without recomposing or changing any border parameters.
                        Modifier.drawBehind { drawRect(backgroundColor.value) }
                    } else Modifier
                )
                .border(
                    borderWidth.value,
                    if (useAlternateBrush.value) alternateBrush else brush,
                    shape,
                )
        )
    }

    override fun toggleState() = onToggle()

    fun toggleBackground() {
        check(hasBackgroundColor)
        backgroundColor.value =
            if (backgroundColor.value == Color.White) Color.LightGray else Color.White
    }

    fun toggleBrush() {
        useAlternateBrush.value = !useAlternateBrush.value
    }

    fun toggleWidth() {
        borderWidth.value = if (borderWidth.value == 2.dp) 4.dp else 2.dp
    }
}

@get:RequiresApi(33)
private val redRuntimeShaderBrush
    get() =
        ShaderBrush(RuntimeShader("half4 main(float2 coord) { return half4(1.0, 0.0, 0.0, 1.0); }"))

@get:RequiresApi(33)
private val blueRuntimeShaderBrush
    get() =
        ShaderBrush(RuntimeShader("half4 main(float2 coord) { return half4(0.0, 0.0, 1.0, 1.0); }"))

private val GradientColors =
    listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red,
    )

private val GradientBrush: Brush = Brush.sweepGradient(GradientColors)

private val AlternateGradientBrush: Brush = Brush.sweepGradient(GradientColors.reversed())
