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

package androidx.compose.ui.graphics.benchmark

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the CPU cost of recording each [DrawScope] primitive into a [GraphicsLayer] display
 * list.
 *
 * Unlike [GraphicsLayerRecordBenchmark], which establishes the reference baseline floor (0 ops) and
 * scaling curves (1, 10, 100 ops) across varying operation volumes, this benchmark isolates and
 * compares the relative recording of draw scope primitives at a fixed [OP_COUNT].
 *
 * Note: This benchmark isolates the display list recording phase and does not measure RenderThread
 * rasterization or HWUI playback.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DrawScopePrimitivesBenchmark {

    companion object {
        private const val OP_COUNT = 10

        private const val CANVAS_WIDTH = 300
        private const val CANVAS_HEIGHT = 300
        private val SIZE = IntSize(CANVAS_WIDTH, CANVAS_HEIGHT)
        private val DENSITY = Density(2f)
        private val LAYOUT_DIRECTION = LayoutDirection.Ltr

        // Step increments to distribute points across the canvas.
        private const val POINT_STEP_X = 15f
        private const val POINT_STEP_Y = 15f
    }

    @get:Rule val benchmarkRule = BenchmarkRule()

    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private var graphicsContext: GraphicsContext? = null
    private var layer: GraphicsLayer? = null

    // Resources pre-built once in setup so the timed region records only, never allocates them.
    private val path =
        Path().apply {
            moveTo(0f, 0f)
            lineTo(CANVAS_WIDTH.toFloat(), 0f)
            lineTo(CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat())
            lineTo(0f, CANVAS_HEIGHT.toFloat())
            close()
        }

    private val clipPath =
        Path().apply {
            moveTo(0f, 0f)
            lineTo(CANVAS_WIDTH.toFloat(), 0f)
            lineTo(CANVAS_WIDTH / 2f, CANVAS_HEIGHT.toFloat())
            close()
        }

    private val image = ImageBitmap(24, 24)

    private val points =
        List(20) { i ->
            Offset((i * POINT_STEP_X) % CANVAS_WIDTH, (i * POINT_STEP_Y) % CANVAS_HEIGHT)
        }

    private val color = Color.Red
    private val cornerRadius = CornerRadius(8f, 8f)
    private val lineStart = Offset(0f, 0f)
    private val lineEnd = Offset(CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat())

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            // Disable launch animation
            @Suppress("Deprecation") activity.overridePendingTransition(0, 0)
            val container = activity.findViewById<ViewGroup>(android.R.id.content)
            graphicsContext = GraphicsContext(container)
            layer = graphicsContext?.createGraphicsLayer()
        }
    }

    @After
    fun teardown() {
        scenario.onActivity {
            layer?.let { graphicsContext?.releaseGraphicsLayer(it) }
            layer = null
            graphicsContext = null
        }
        scenario.close()
    }

    @Test
    fun drawRect() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawRect(color) }
            }
        }
    }

    @Test
    fun drawRoundRect() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawRoundRect(color, cornerRadius = cornerRadius) }
            }
        }
    }

    @Test
    fun drawCircle() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawCircle(color) }
            }
        }
    }

    @Test
    fun drawLine() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawLine(color, lineStart, lineEnd, strokeWidth = 2f) }
            }
        }
    }

    @Test
    fun drawPath() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawPath(path, color) }
            }
        }
    }

    @Test
    fun drawImage() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawImage(image) }
            }
        }
    }

    @Test
    fun drawPoints() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawPoints(points, PointMode.Points, color, strokeWidth = 4f) }
            }
        }
    }

    @Test
    fun drawArc() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) {
                    drawArc(color, startAngle = 0f, sweepAngle = 120f, useCenter = false)
                }
            }
        }
    }

    @Test
    fun clipRectDrawRect() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { clipRect(0f, 0f, 150f, 150f) { drawRect(color) } }
            }
        }
    }

    @Test
    fun clipPathDrawRect() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { clipPath(clipPath) { drawRect(color) } }
            }
        }
    }

    @Test
    fun transformDrawRect() {
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) {
                    translate(10f, 10f) { scale(1.1f) { rotate(5f) { drawRect(color) } } }
                }
            }
        }
    }

    @Test
    fun drawText() {
        var textLayoutResult: TextLayoutResult? = null
        scenario.onActivity { activity ->
            val fontFamilyResolver = createFontFamilyResolver(activity)
            val localeList = LocaleList(activity.resources.configuration.locales.toLanguageTags())
            val textMeasurer =
                TextMeasurer(fontFamilyResolver, localeList, DENSITY, LAYOUT_DIRECTION)
            textLayoutResult =
                textMeasurer.measure(AnnotatedString("Hello, Compose graphics benchmark!"))
        }
        val textLayout = requireNotNull(textLayoutResult)
        val targetLayer = requireNotNull(layer)
        benchmarkRule.measureRepeatedOnMainThread {
            targetLayer.record(DENSITY, LAYOUT_DIRECTION, SIZE) {
                repeat(OP_COUNT) { drawText(textLayout, color = Color.Black) }
            }
        }
    }
}
