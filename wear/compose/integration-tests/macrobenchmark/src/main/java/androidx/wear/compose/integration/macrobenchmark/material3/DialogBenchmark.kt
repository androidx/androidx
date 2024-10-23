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

package androidx.wear.compose.integration.macrobenchmark.material3

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.testutils.createCompilationParams
import androidx.wear.compose.integration.macrobenchmark.disableChargingExperience
import androidx.wear.compose.integration.macrobenchmark.enableChargingExperience
import java.lang.Thread.sleep
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMetricApi::class)
@LargeTest
@RunWith(Parameterized::class)
class DialogBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        disableChargingExperience()
    }

    @After
    fun destroy() {
        enableChargingExperience()
    }

    @Test
    fun start() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(FrameTimingGfxInfoMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Last)),
            compilationMode = compilationMode,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                intent.action = DIALOG_ACTIVITY
                startActivityAndWait(intent)
            }
        ) {
            val buttonOpenAlertDialog = device.findObject(By.desc(OPEN_ALERT_DIALOG))
            val buttonOpenConfirmDialog = device.findObject(By.desc(OPEN_CONFIRM_DIALOG))
            val buttonOpenSuccessDialog = device.findObject(By.desc(OPEN_SUCCESS_DIALOG))
            val buttonOpenFailureDialog = device.findObject(By.desc(OPEN_FAILURE_DIALOG))

            requireNotNull(buttonOpenAlertDialog).click()
            sleep(1000)
            requireNotNull(device.findObject(By.desc(DIALOG_CONFIRM))).click()

            requireNotNull(buttonOpenConfirmDialog).click()
            sleep(3000)

            requireNotNull(buttonOpenSuccessDialog).click()
            sleep(3000)

            requireNotNull(buttonOpenFailureDialog).click()
            sleep(3000)
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val DIALOG_ACTIVITY = "${PACKAGE_NAME}.material3.DIALOG_ACTIVITY"

        private const val OPEN_ALERT_DIALOG = "OPEN_ALERT_DIALOG"
        private const val OPEN_CONFIRM_DIALOG = "OPEN_CONFIRM_DIALOG"
        private const val OPEN_SUCCESS_DIALOG = "OPEN_SUCCESS_DIALOG"
        private const val OPEN_FAILURE_DIALOG = "OPEN_FAILURE_DIALOG"

        private const val DIALOG_CONFIRM = "DIALOG_CONFIRM"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
