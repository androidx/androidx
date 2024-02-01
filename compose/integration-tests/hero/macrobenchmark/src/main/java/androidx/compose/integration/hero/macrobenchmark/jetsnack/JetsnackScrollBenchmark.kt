/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.integration.hero.macrobenchmark.jetsnack

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.macrobenchmark.ITERATIONS
import androidx.compose.integration.hero.macrobenchmark.PACKAGE_NAME
import androidx.compose.integration.hero.macrobenchmark.waitForComposeIdle
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class JetsnackScrollBenchmark(val compilationMode: CompilationMode) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun scrollHome() {
        val ACTION = "$PACKAGE_NAME.jetsnack.JETSNACK_ACTIVITY"
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = ITERATIONS
        ) {
            val intent = Intent()
            intent.action = ACTION
            startActivityAndWait(intent)

            val searchCondition = Until.hasObject(By.res("snack_collection"))
            // Wait until a snack collection item within the list is rendered
            device.wait(searchCondition, 3_000)

            val contentList = device.findObject(By.res("snack_list"))
            scrollActions(contentList)
        }
    }

    private fun MacrobenchmarkScope.scrollActions(contentList: UiObject2) {
        // Set gesture margin to avoid triggering gesture navigation
        contentList.setGestureMargin(device.displayWidth / 5)

        contentList.fling(Direction.DOWN)
        device.waitForComposeIdle()
        contentList.fling(Direction.UP)
        device.waitForComposeIdle()
        contentList.fling(Direction.DOWN)
        device.waitForComposeIdle()
    }

    companion object {
        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
