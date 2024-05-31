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

package androidx.compose.material3.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@Ignore("Sample first macro benchmark, we don't actually want to track this.")
@RunWith(AndroidJUnit4::class)
class TooltipBenchmark {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun animationTest() {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Full(),
            iterations = 10,
            startupMode = StartupMode.WARM,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                startActivityAndWait(intent)
            }
        ) {
            val anchorIconButton = device.findObject(By.desc("tooltipAnchor"))
            anchorIconButton.longClick()
            device.wait(Until.findObject(By.desc("tooltipAnchor").longClickable(true)), 1500)
        }
    }

    companion object {
        private const val PACKAGE_NAME =
            "androidx.compose.material3.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.compose.material3.integration.macrobenchmark.target.TOOLTIP_ACTIVITY"
    }
}
