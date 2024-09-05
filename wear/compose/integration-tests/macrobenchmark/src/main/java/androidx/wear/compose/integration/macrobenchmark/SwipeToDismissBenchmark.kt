/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.testutils.createCompilationParams
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class SwipeToDismissBenchmark(private val compilationMode: CompilationMode) {
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
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = 10,
            setupBlock = {
                val intent = Intent()
                intent.action = SWIPE_TO_DISMISS_ACTIVITY
                startActivityAndWait(intent)
            }
        ) {
            val swipeToDismissBox = device.findObject(By.desc(CONTENT_DESCRIPTION))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            swipeToDismissBox.setGestureMargin(device.displayWidth / 5)
            repeat(3) {
                swipeToDismissBox.swipe(Direction.RIGHT, 1f, SWIPE_SPEED)
                // Sleeping the current thread for sometime before swiping again. This is required
                // for cuttlefish_wear emulator as swipes are not completed when performed
                // repeatedly. See b/328016250 for more details.
                // TODO(b/329837878): Remove the sleep once infra improves
                Thread.sleep(500)
                device.waitForIdle()
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "androidx.wear.compose.integration.macrobenchmark.target"
        private const val SWIPE_TO_DISMISS_ACTIVITY = "${PACKAGE_NAME}.SWIPE_TO_DISMISS_ACTIVITY"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }

    private val SWIPE_SPEED = 500
}
