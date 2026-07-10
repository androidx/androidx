/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.datastore.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.BaselineProfileMode.Require
import androidx.benchmark.macro.CompilationMode.Partial
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreReadWriteBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun noDataStore() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = benchmarkMetrics,
            compilationMode = Partial(Require),
            iterations = 10,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() },
        ) {
            // Measure Startup.
            startActivityAndWait(Intent(NO_DATA_STORE_ACTION))

            // Measure Updates.
            device.updateValues()
        }
    }

    @Test
    fun preferencesDataStore() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = benchmarkMetrics,
            compilationMode = Partial(Require),
            iterations = 10,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // Start the setup activity to copy the file and wait for it to finish.
                startActivityAndWait(
                    Intent().apply {
                        action = DATA_STORE_SETUP_ACTION
                        putExtra("copy datastore file", "preferences")
                    }
                )
                pressHome()
            },
        ) {
            // Measure Startup (Data Store Read).
            startActivityAndWait(Intent(PREFERENCES_DATA_STORE_ACTION))

            // Measure Updates (Data Store Writes).
            device.updateValues()
        }
    }

    @Test
    fun protoDataStore() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = benchmarkMetrics,
            compilationMode = Partial(Require),
            iterations = 10,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // Start the setup activity to copy the file and wait for it to finish.
                startActivityAndWait(
                    Intent().apply {
                        action = DATA_STORE_SETUP_ACTION
                        putExtra("copy datastore file", "proto")
                    }
                )
                pressHome()
            },
        ) {
            // Measure Startup (Data Store Read).
            startActivityAndWait(Intent(PROTO_DATA_STORE_ACTION))

            // Measure Updates (Data Store Writes).
            device.updateValues()
        }
    }

    @Test
    fun jsonDataStore() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = benchmarkMetrics,
            compilationMode = Partial(Require),
            iterations = 10,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // Start the setup activity to copy the file and wait for it to finish.
                startActivityAndWait(
                    Intent().apply {
                        action = DATA_STORE_SETUP_ACTION
                        putExtra("copy datastore file", "json")
                    }
                )
                pressHome()
            },
        ) {
            // Measure Startup (Data Store Read).
            startActivityAndWait(Intent(JSON_DATA_STORE_ACTION))

            device.updateValues()
        }
    }

    private fun UiDevice.updateValues() {
        delay()
        findObject(By.res("UserTextField")).setText("Jane Doe")
        delay()
        findObject(By.res("DisplayModeDropdownMenu")).click()
        delay()
        findObject(By.res("DisplayModeOption2")).click()
        delay()
        findObject(By.res("CameraDropdownMenu")).click()
        delay()
        findObject(By.res("CameraOption2")).click()
        delay()
        findObject(By.res("VolumeSlider")).swipe(Direction.RIGHT, 0.8f)
        delay()
        findObject(By.res("BrightnessSlider")).swipe(Direction.RIGHT, 0.8f)
        delay()
        findObject(By.res("DarkModeSwitch")).click()
        delay()
    }

    private fun UiDevice.delay() {
        Thread.sleep(300L)
        waitForIdle()
    }

    companion object {
        private const val TARGET_PACKAGE = "androidx.datastore.macrobenchmark.target"
        private const val DATA_STORE_SETUP_ACTION = "$TARGET_PACKAGE.DataStoreSetup"
        private const val NO_DATA_STORE_ACTION = "$TARGET_PACKAGE.NoDataStore"
        private const val PREFERENCES_DATA_STORE_ACTION = "$TARGET_PACKAGE.PreferencesDataStore"
        private const val PROTO_DATA_STORE_ACTION = "$TARGET_PACKAGE.ProtoDataStore"
        private const val JSON_DATA_STORE_ACTION = "$TARGET_PACKAGE.JsonDataStore"

        @OptIn(ExperimentalMetricApi::class)
        private val benchmarkMetrics =
            listOf(
                StartupTimingMetric(),
                TraceSectionMetric("userUpdate"),
                TraceSectionMetric("displayModeUpdate"),
                TraceSectionMetric("cameraUpdate"),
                TraceSectionMetric("volumeUpdate"),
                TraceSectionMetric("brightnessUpdate"),
                TraceSectionMetric("darkModeUpdate"),
            )
    }
}
