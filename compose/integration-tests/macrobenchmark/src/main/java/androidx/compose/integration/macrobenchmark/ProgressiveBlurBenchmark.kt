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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@RunWith(Parameterized::class)
class ProgressiveBlurBenchmark(private val compilationMode: CompilationMode) {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun progressiveBlurAnimation() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent().apply { action = Action }
                startActivityAndWait(intent)
            },
        ) {
            // Wait for the button to appear and click it to start the animation
            val toggleBtn = device.wait(Until.findObject(By.desc(ToggleAnimationDescription)), 5000)
            toggleBtn.click()
            device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)

            // Click again to animate progressive blur back to zero
            val toggleBtnBack =
                device.wait(Until.findObject(By.desc(ToggleAnimationDescription)), 5000)
            toggleBtnBack.click()
            device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.PROGRESSIVE_BLUR_ACTIVITY"
        const val ToggleAnimationDescription = "toggle-animation"
        const val ComposeIdle = "COMPOSE-IDLE"

        @Parameterized.Parameters(name = "compilationMode={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
