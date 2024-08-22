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

package androidx.constraintlayout.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test

private const val PACKAGE_NAME =
    "androidx.constraintlayout.compose.integration.macrobenchmark.target"
private const val ACTION =
    "androidx.constraintlayout.compose.integration.macrobenchmark.target.MOTION_LAYOUT_ACTIVITY"

/**
 * Run locally using `./gradlew
 * :constraintlayout:constraintlayout-compose:integration-tests:macrobenchmark:connectedCheck`
 */
@LargeTest
class MotionLayoutBenchmark {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    /**
     * Transitions the Layout through its three different ConstraintSets using the MotionScene DSL.
     */
    @Test fun messageDsl() = benchmarkRule.testNewMessage(NewMessageMode.Dsl)

    @Test fun messageOptimizedDsl() = benchmarkRule.testNewMessage(NewMessageMode.OptimizedDsl)

    /**
     * Transitions the Layout through its three different ConstraintSets using the MotionScene JSON.
     */
    @Test fun messageJson() = benchmarkRule.testNewMessage(NewMessageMode.Json)

    @Test
    fun collapsibleToolbar() =
        benchmarkRule.motionBenchmark("CollapsibleToolbar") {
            val column = device.findObject(By.res("LazyColumn"))
            val bounds = column.visibleBounds

            // Margin to reduce the amount of pixels scrolled and avoid the navigation pill
            val vMargin = (bounds.height() * 0.2f).roundToInt()
            val x = (bounds.width() * 0.5f).roundToInt()
            val y1 = bounds.bottom - vMargin
            val y2 = bounds.top + vMargin

            // Scroll down
            device.swipe(x, y1, x, y2, 50)
            device.waitForIdle()

            // Scroll up
            device.swipe(x, y2, x, y1, 50)
            device.waitForIdle()
        }

    /**
     * LazyList based layout, where every item is a MotionLayout Composable and are animated as they
     * are revealed.
     */
    @Test
    fun dynamicGraphs() =
        benchmarkRule.motionBenchmark("DynamicGraphs") {
            val column = device.findObject(By.res("LazyColumn"))
            val bounds = column.visibleBounds

            // Margin to avoid swiping the navigation pill
            val vMargin = (bounds.height() * 0.1f).roundToInt()
            val x = (bounds.width() * 0.5f).roundToInt()
            val y1 = bounds.bottom - vMargin
            val y2 = bounds.top + vMargin

            repeat(5) {
                // Fast swipe upwards, to scroll down through multiple animated items at a time
                device.swipe(x, y1, x, y2, 6)
                device.waitForComposeIdle()
            }
        }

    /**
     * The base method to benchmark FrameTimings of a Composable from the macrobenchmark-target
     * module.
     *
     * [composableName] should be a registered Composable in **MotionLayoutBenchmarkActivity**
     *
     * The [setupBlock] is run after the activity starts with the given [composableName]. You may
     * use this as a chance to set the UI in the way you wish it to be measured.
     *
     * The [measureBlock] is called after the setup. [FrameTimingMetric] measures UI performance
     * during this block.
     */
    private fun MacrobenchmarkRule.motionBenchmark(
        composableName: String,
        setupBlock: MacrobenchmarkScope.() -> Unit = {},
        measureBlock: MacrobenchmarkScope.() -> Unit
    ) {
        measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 8,
            // HOT causes issues with the measure block logic where multiple click actions are
            // triggered at once
            startupMode = StartupMode.WARM,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                intent.putExtra("ComposableName", composableName)
                startActivityAndWait(intent)
                device.waitForIdle()
                device.waitForComposeIdle()
                setupBlock()
            },
            measureBlock = measureBlock
        )
    }

    private fun MacrobenchmarkRule.testNewMessage(mode: NewMessageMode) {
        motionBenchmark(mode.composableName) {
            val toFab = device.findObject(By.res("Fab"))
            val toFull = device.findObject(By.res("Full"))
            val toMini = device.findObject(By.res("Mini"))

            toMini.click()
            device.waitForIdle()

            toFab.click()
            device.waitForIdle()

            toFull.click()
            device.waitForIdle()
        }
    }

    internal enum class NewMessageMode(val composableName: String) {
        Json("NewMessageJson"),
        Dsl("NewMessageDsl"),
        OptimizedDsl("OptimizedNewMessageDsl")
    }

    private fun UiDevice.waitForComposeIdle(timeoutMs: Long = 3000) {
        wait(Until.findObject(By.desc("COMPOSE-IDLE")), timeoutMs)
    }
}
