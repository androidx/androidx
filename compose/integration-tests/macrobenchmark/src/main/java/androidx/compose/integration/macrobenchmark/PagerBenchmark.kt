/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalMetricApi::class)

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PagerBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun pager_of_grids_gesture_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.Grid)
                startActivityAndWait(intent)
            }
        ) {
            val pager = device.findObject(By.desc(ContentDescription))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            pager.setGestureMargin(device.displayWidth / 5)
            for (i in 1..EventRepeatCount) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                pager.swipe(Direction.LEFT, 1.0f)
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_grids_animated_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.Grid)
                intent.putExtra(BenchmarkType.Tab, true)
                startActivityAndWait(intent)
            }
        ) {
            val nextButton = device.findObject(By.desc(NextDescription))
            repeat(EventRepeatCount) {
                nextButton.click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_lists_gesture_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.List)
                startActivityAndWait(intent)
            }
        ) {
            val pager = device.findObject(By.desc(ContentDescription))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            pager.setGestureMargin(device.displayWidth / 5)
            for (i in 1..EventRepeatCount) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                pager.swipe(Direction.LEFT, 1.0f)
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_lists_animated_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.List)
                intent.putExtra(BenchmarkType.Tab, true)
                startActivityAndWait(intent)
            }
        ) {
            val nextButton = device.findObject(By.desc(NextDescription))
            repeat(EventRepeatCount) {
                nextButton.click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_webviews_gesture_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.WebView)
                startActivityAndWait(intent)
            }
        ) {
            val pager = device.findObject(By.desc(ContentDescription))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            pager.setGestureMargin(device.displayWidth / 5)
            for (i in 1..EventRepeatCount) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                pager.swipe(Direction.LEFT, 1.0f)
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_webviews_animated_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.WebView)
                intent.putExtra(BenchmarkType.Tab, true)
                startActivityAndWait(intent)
            }
        ) {
            val nextButton = device.findObject(By.desc(NextDescription))
            repeat(EventRepeatCount) {
                nextButton.click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_images_full_page_gesture_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.putExtra(BenchmarkType.Key, BenchmarkType.FullScreenImage)
                intent.action = Action
                startActivityAndWait(intent)
            }
        ) {
            val pager = device.findObject(By.desc(ContentDescription))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            pager.setGestureMargin(device.displayWidth / 5)
            for (i in 1..EventRepeatCount) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                pager.swipe(Direction.LEFT, 1.0f)
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_images_full_page_animated_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.putExtra(BenchmarkType.Key, BenchmarkType.FullScreenImage)
                intent.putExtra(BenchmarkType.Tab, true)
                intent.action = Action
                startActivityAndWait(intent)
            }
        ) {
            val nextButton = device.findObject(By.desc(NextDescription))
            repeat(EventRepeatCount) {
                nextButton.click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_images_fixed_size_page_gesture_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric(), FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.FixedSizeImage)
                startActivityAndWait(intent)
            }
        ) {
            val pager = device.findObject(By.desc(ContentDescription))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            pager.setGestureMargin(device.displayWidth / 5)
            for (i in 1..EventRepeatCount) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                pager.swipe(Direction.LEFT, 1.0f)
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun pager_of_images_fixed_size_page_animated_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.FixedSizeImage)
                intent.putExtra(BenchmarkType.Tab, true)
                startActivityAndWait(intent)
            }
        ) {
            val nextButton = device.findObject(By.desc(NextDescription))
            repeat(EventRepeatCount) {
                nextButton.click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun list_of_pagers_gesture_scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingGfxInfoMetric()),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                intent.putExtra(BenchmarkType.Key, BenchmarkType.ListOfPager)
                startActivityAndWait(intent)
            }
        ) {
            val pager = device.findObject(By.desc("List"))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            pager.setGestureMargin(device.displayHeight / 5)
            for (i in 1..EventRepeatCount) {
                // From center we scroll 2/3 of it which is 1/3 of the screen.
                pager.swipe(Direction.UP, 1.0f)
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.LAZY_PAGER_ACTIVITY"
        private const val ContentDescription = "Pager"
        private const val NextDescription = "Next"
        private const val ComposeIdle = "COMPOSE-IDLE"
        private const val EventRepeatCount = 10

        object BenchmarkType {
            val Key = "BenchmarkType"
            val Tab = "EnableTab"
            val Grid = "Pager of Grids"
            val List = "Pager of List"
            val WebView = "Pager of WebViews"
            val FullScreenImage = "Pager of Full Screen Images"
            val FixedSizeImage = "Pager of Fixed Size Images"
            val ListOfPager = "Pager Inside A List"
        }

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = listOf(arrayOf(CompilationMode.Full()))
    }
}
