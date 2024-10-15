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

package androidx.compose.integration.hero.jetsnack.macrobenchmark

import android.content.Intent
import android.view.KeyEvent.KEYCODE_TAB
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.jetsnack.ITERATIONS
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class JetsnackFocusBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun focusHome() =
        benchmarkFocus(
            action = "$JETSNACK_TARGET_PACKAGE_NAME.JETSNACK_ACTIVITY",
            metrics =
                listOf(
                    TraceSectionMetric(
                        "FocusOwnerImpl:dispatchKeyEvent",
                        mode = TraceSectionMetric.Mode.Average
                    ),
                    TraceSectionMetric(
                        "FocusTransactions:requestFocus",
                        mode = TraceSectionMetric.Mode.Average
                    )
                ),
            setupBlock = {
                val searchCondition = Until.hasObject(By.res("snack_collection"))
                // Wait until a snack collection item within the list is rendered
                device.wait(searchCondition, 3_000)
            }
        )

    @Test
    fun focusViewsHome() =
        benchmarkFocus(
            action = "$JETSNACK_TARGET_PACKAGE_NAME.JETSNACK_VIEWS_ACTIVITY",
            setupBlock = {
                val resPkg = JETSNACK_TARGET_PACKAGE_NAME
                val searchCondition = Until.hasObject(By.res(resPkg, "snackImageView"))
                // Wait until a snack collection item within the list is rendered
                device.wait(searchCondition, 3_000)
            }
        )

    private fun benchmarkFocus(
        action: String,
        metrics: List<Metric> = listOf(),
        setupBlock: MacrobenchmarkScope.() -> Unit
    ) =
        benchmarkRule.measureRepeated(
            packageName = JETSNACK_TARGET_PACKAGE_NAME,
            metrics =
                buildList {
                    add(FrameTimingMetric())
                    addAll(metrics)
                },
            compilationMode = compilationMode,
            iterations = ITERATIONS,
            setupBlock = {
                // Ensure item animation consistency between Views and Compose
                device.executeShellCommand("settings put global animator_duration_scale 1.0")
                device.pressBack()
                val intent = Intent()
                intent.action = action
                startActivityAndWait(intent)
                setupBlock()
            },
            measureBlock = { repeat(30) { device.pressKeyCode(KEYCODE_TAB) } }
        )

    companion object {
        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
