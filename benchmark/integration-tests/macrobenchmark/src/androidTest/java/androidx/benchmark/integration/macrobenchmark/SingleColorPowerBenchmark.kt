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

package androidx.benchmark.integration.macrobenchmark

import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import androidx.benchmark.macro.BatteryCharge
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.PowerCategory
import androidx.benchmark.macro.PowerCategoryDisplayLevel
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.PowerRail
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
@OptIn(ExperimentalMetricApi::class)
class SingleColorPowerBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun validateDeviceState() {
        assumeTrue(PowerRail.hasMetrics())
        assumeTrue(BatteryCharge.hasMinimumCharge())
    }

    fun measureScreenColorPower(color: Int) {
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                PowerMetric(
                    PowerMetric.Power(
                        mapOf(PowerCategory.DISPLAY to PowerCategoryDisplayLevel.BREAKDOWN)
                    )
                ),
            ),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                val intent = Intent()
                val bundle = Bundle()
                bundle.putInt("bg_color", color)
                intent.action = ACTION
                intent.putExtras(bundle)
                startActivityAndWait(intent)
            }
        ) {
            Thread.sleep(DURATION_MS.toLong())
        }
    }

    @Test
    fun measureDarkScreenPower() {
        measureScreenColorPower(Color.BLACK)
    }

    @Test
    fun measureLightScreenPower() {
        measureScreenColorPower(Color.WHITE)
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION = "$PACKAGE_NAME.SINGLE_COLOR_ACTIVITY"
        private const val DURATION_MS = 5000
    }
}