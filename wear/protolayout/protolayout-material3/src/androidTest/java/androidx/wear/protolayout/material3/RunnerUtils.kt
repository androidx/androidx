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

package androidx.wear.protolayout.material3

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.material3.test.GoldenTestActivity
import java.util.stream.Collectors

object RunnerUtils {
    // This isn't totally ideal right now. The screenshot tests run on a phone, so emulate some
    // watch dimensions here.
    const val SCREEN_SIZE_SMALL: Int = 525 // ~199dp

    @SuppressLint("BanThreadSleep")
    // TODO: b/355417923 - Avoid calling sleep.
    fun runSingleScreenshotTest(
        rule: AndroidXScreenshotTestRule,
        layout: LayoutElementBuilders.Layout,
        expected: String,
        isRtlDirection: Boolean
    ) {
        val layoutPayload = layout.toByteArray()

        val startIntent =
            Intent(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .targetContext,
                GoldenTestActivity::class.java
            )
        startIntent.putExtra("layout", layoutPayload)
        startIntent.putExtra(GoldenTestActivity.USE_RTL_DIRECTION, isRtlDirection)

        ActivityScenario.launch<GoldenTestActivity>(startIntent).use {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync()
            try {
                // Wait 1s after launching the activity. This allows for the old white layout in the
                // bootstrap activity to fully go away before proceeding.
                Thread.sleep(100)
            } catch (ex: Exception) {
                if (ex is InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                Log.e("MaterialGoldenTest", "Error sleeping", ex)
            }

            val displayMetrics: DisplayMetrics =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getResources()
                    .getDisplayMetrics()

            // RTL will put the View on the right side.
            val screenWidthStart =
                if (isRtlDirection) displayMetrics.widthPixels - SCREEN_SIZE_SMALL else 0

            val bitmap =
                Bitmap.createBitmap(
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getUiAutomation()
                        .takeScreenshot(),
                    screenWidthStart,
                    0,
                    SCREEN_SIZE_SMALL,
                    SCREEN_SIZE_SMALL
                )
            // Increase the threshold of Structural Similarity Index for image comparison to 0.995,
            // so that we do not miss the image differences.
            rule.assertBitmapAgainstGolden(bitmap, expected, MSSIMMatcher(threshold = 0.995))
        }
    }

    @SuppressLint("BanThreadSleep")
    // TODO: b/355417923 - Avoid calling sleep.
    fun waitForNotificationToDisappears() {
        try {
            // Wait for the initial notification to disappear.
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            Log.e("MaterialGoldenTest", "Error sleeping", e)
        }
    }

    fun convertToTestParameters(
        testCases: Map<String, LayoutElementBuilders.Layout>,
        isForRtr: Boolean,
        isForLtr: Boolean
    ): List<Array<Any>> {
        return testCases.entries
            .stream()
            .map { test: Map.Entry<String, LayoutElementBuilders.Layout> ->
                arrayOf(test.key, TestCase(test.value, isForRtr, isForLtr))
            }
            .collect(Collectors.toList())
    }

    /** Holds testcase parameters. */
    class TestCase(
        val layout: LayoutElementBuilders.Layout,
        val isForRtl: Boolean,
        val isForLtr: Boolean
    )
}
