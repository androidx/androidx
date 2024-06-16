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

package androidx.wear.compose.material3.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Benchmark for Wear Compose Material 3 [Icon]. */
@LargeTest
@RunWith(Parameterized::class)
class IconBenchmark(private val iconType: IconType) {

    companion object {
        @Parameterized.Parameters(name = "{0}") @JvmStatic fun parameters() = IconType.values()
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val iconCaseFactory = { IconTestCase(iconType) }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(iconCaseFactory)
    }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(iconCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(iconCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(iconCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(iconCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(iconCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(iconCaseFactory)
    }
}

internal class IconTestCase(private val iconType: IconType) : LayeredComposeTestCase() {
    private val width = 24.dp
    private val height = 24.dp
    private val imageVector =
        ImageVector.Builder(
                defaultWidth = width,
                defaultHeight = height,
                viewportWidth = width.value,
                viewportHeight = height.value
            )
            .build()
    private val imageBitmap =
        ImageBitmap(width = width.value.toInt(), height = height.value.toInt())
    private val painter =
        object : Painter() {
            override fun DrawScope.onDraw() {
                drawRect(color = Color.Black)
            }

            override val intrinsicSize: Size
                get() = Size(width.value, height.value)
        }

    @Composable
    override fun MeasuredContent() {
        when (iconType) {
            IconType.ImageVector -> Icon(imageVector = imageVector, contentDescription = "vector")
            IconType.Bitmap -> Icon(bitmap = imageBitmap, contentDescription = "bitmap")
            IconType.Painter -> Icon(painter = painter, contentDescription = "painter")
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

enum class IconType {
    ImageVector,
    Bitmap,
    Painter
}
