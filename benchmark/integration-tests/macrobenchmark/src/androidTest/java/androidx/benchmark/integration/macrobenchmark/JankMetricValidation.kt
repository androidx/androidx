/*
 * Copyright 2020 The Android Open Source Project
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

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.JankMetric
import androidx.benchmark.macro.MacrobenchmarkConfig
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@SdkSuppress(minSdkVersion = 29)
@RunWith(Parameterized::class)
class JankMetricValidation(
    private val compilationMode: CompilationMode,
    private val killProcess: Boolean
) {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun start() {
        val config = MacrobenchmarkConfig(
            packageName = PACKAGE_NAME,
            metrics = listOf(JankMetric()),
            compilationMode = compilationMode,
            killProcessEachIteration = killProcess,
            iterations = 10
        )

        benchmarkRule.measureRepeated(
            config,
            setupBlock = {
                val intent = Intent()
                intent.action = ACTION
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                launchIntentAndWait(intent)
            }
        ) {
            val recycler = device.findObject(By.res(PACKAGE_NAME, RESOURCE_ID))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            recycler.setGestureMargin(device.displayWidth / 5)
            recycler.fling(Direction.DOWN)
            device.waitForIdle()
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.benchmark.integration.macrobenchmark.target"
        private const val ACTION =
            "androidx.benchmark.integration.macrobenchmark.target.RECYCLER_VIEW"
        private const val RESOURCE_ID = "recycler"

        @Parameterized.Parameters(name = "compilation_mode={0}, kill_process={1}")
        @JvmStatic
        fun jankParameters(): List<Array<Any>> {
            val compilationModes = listOf(
                CompilationMode.None,
                CompilationMode.SpeedProfile(warmupIterations = 3)
            )
            val processKillOptions = listOf(false, false)
            return compilationModes.zip(processKillOptions).map {
                arrayOf(it.first, it.second)
            }
        }
    }
}
