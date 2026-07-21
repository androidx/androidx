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

import androidx.activity.ComponentActivity
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeatedOnMainThread
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Baseline and scaling curve for the recording half of the graphics pipeline. Times
 * [GraphicsLayer.record] building a display list.
 *
 * At `operations=1` and `operations=0`, this acts as the reference baseline floor for minimal
 * display-list construction cost, while higher operation counts quantify how recording overhead
 * scales linearly with command volume.
 */
@LargeTest
@RunWith(Parameterized::class)
class GraphicsLayerRecordBenchmark(private val numberOfOperations: Int) {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private var graphicsContext: GraphicsContext? = null
    private var layer: GraphicsLayer? = null
    private lateinit var density: Density
    private val path: Path =
        Path().apply {
            moveTo(0f, 0f)
            lineTo(100f, 100f)
            lineTo(0f, 100f)
            close()
        }
    private val image: ImageBitmap = ImageBitmap(100, 100)

    private val size = IntSize(WIDTH, HEIGHT)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "operations={0}")
        fun parameters() = listOf(1, 10, 100)

        private const val WIDTH = 512
        private const val HEIGHT = 512
        // 8f spacing staggered modulo 32 gives an offset span of 0..248f, fitting within 512x512
        // and matching standard 8dp Material grid spacing.
        private const val RECT_SIZE = 8f
    }

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        // onActivity blocks and runs the block on the UI thread, mirroring how Compose sets up its
        // layer container.
        scenario.onActivity { activity ->
            // disable launch animation
            @Suppress("Deprecation") activity.overridePendingTransition(0, 0)
            val container = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            graphicsContext = GraphicsContext(container)
            layer = graphicsContext?.createGraphicsLayer()
            density = Density(activity)
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
    fun recordRects() {
        val rectSize = Size(RECT_SIZE, RECT_SIZE)
        benchmarkRule.measureRepeatedOnMainThread {
            layer?.record(density, LayoutDirection.Ltr, size) {
                var i = 0
                while (i < numberOfOperations) {
                    // Stagger rects across the canvas grid so operations are recorded with varied
                    // coordinates.
                    val offset = (i % 32) * RECT_SIZE
                    drawRect(Color.Red, topLeft = Offset(offset, offset), size = rectSize)
                    i++
                }
            }
        }
    }

    @Test
    fun recordLines() {
        benchmarkRule.measureRepeatedOnMainThread {
            layer?.record(density, LayoutDirection.Ltr, size) {
                var i = 0
                while (i < numberOfOperations) {
                    drawLine(Color.Red, Offset.Zero, Offset(10f, 10f))
                    i++
                }
            }
        }
    }

    @Test
    fun recordPaths() {
        benchmarkRule.measureRepeatedOnMainThread {
            layer?.record(density, LayoutDirection.Ltr, size) {
                var i = 0
                while (i < numberOfOperations) {
                    drawPath(path, Color.Red)
                    i++
                }
            }
        }
    }

    @Test
    fun recordImages() {
        benchmarkRule.measureRepeatedOnMainThread {
            layer?.record(density, LayoutDirection.Ltr, size) {
                var i = 0
                while (i < numberOfOperations) {
                    drawImage(image)
                    i++
                }
            }
        }
    }

    @Test
    fun recordMixedCommands() {
        benchmarkRule.measureRepeatedOnMainThread {
            layer?.record(density, LayoutDirection.Ltr, size) {
                var i = 0
                while (i < numberOfOperations) {
                    drawRect(Color.Red)
                    drawLine(Color.Blue, Offset.Zero, Offset(10f, 10f))
                    drawPath(path, Color.Green)
                    drawImage(image)
                    i++
                }
            }
        }
    }

    @Test
    @Ignore("Manual baseline benchmark to isolate zero-op recording floor.")
    fun recordNoOps() {
        benchmarkRule.measureRepeatedOnMainThread {
            layer?.record(density, LayoutDirection.Ltr, size) {}
        }
    }
}
