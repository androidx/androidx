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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CrossfadeBenchmark(private val compilationMode: CompilationMode) {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun crossfadeBenchmarkInitialComposition() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent().apply { action = Action }
                startActivityAndWait(intent)
            }
        ) {
            repeat(2) {
                device.findObject(By.desc(ToggleCrossfadeDescription)).click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    @Test
    fun crossfadeBenchmarkTargetStateChange() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent().apply { action = Action }
                startActivityAndWait(intent)
            }
        ) {
            repeat(2) {
                device.findObject(By.desc(ToggleTargetStateDescription)).click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.CROSSFADE_ACTIVITY"
        const val ToggleCrossfadeDescription = "toggle-crossfade"
        const val ToggleTargetStateDescription = "toggle-target"
        const val ComposeIdle = "COMPOSE-IDLE"

        @Parameterized.Parameters(name = "compilationMode={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
