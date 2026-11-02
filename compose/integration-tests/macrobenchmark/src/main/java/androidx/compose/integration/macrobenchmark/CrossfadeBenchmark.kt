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
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.testutils.createCompilationParams
import androidx.testutils.defaultComposeScrollingMetrics
import androidx.testutils.defaultMemoryMetrics
import androidx.testutils.findObjectOrThrow
import androidx.testutils.waitForComposeIdleOrThrow
import androidx.tracing.trace
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
            metrics = defaultComposeScrollingMetrics() + defaultMemoryMetrics(),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent().apply { action = Action }
                startActivityAndWait(intent)
                device.waitForComposeIdleOrThrow()
            },
        ) {
            repeat(2) {
                trace("Crossfade#initialComposition_$it") {
                    device.findObjectOrThrow(By.desc(ToggleCrossfadeDescription)).click()
                    device.waitForComposeIdleOrThrow()
                }
            }
        }
    }

    @Test
    fun crossfadeBenchmarkTargetStateChange() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics = defaultComposeScrollingMetrics() + defaultMemoryMetrics(),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent().apply { action = Action }
                startActivityAndWait(intent)
                device.waitForComposeIdleOrThrow()
            },
        ) {
            repeat(2) {
                trace("Crossfade#targetStateChange_$it") {
                    device.findObjectOrThrow(By.desc(ToggleTargetStateDescription)).click()
                    device.waitForComposeIdleOrThrow()
                }
            }
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.CROSSFADE_ACTIVITY"
        const val ToggleCrossfadeDescription = "toggle-crossfade"
        const val ToggleTargetStateDescription = "toggle-target"

        @Parameterized.Parameters(name = "compilationMode={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
