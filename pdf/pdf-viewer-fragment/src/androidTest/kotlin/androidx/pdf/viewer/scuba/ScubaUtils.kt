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

package androidx.pdf.viewer.scuba

import android.graphics.Bitmap
import androidx.pdf.viewer.fragment.test.R
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher

private const val MATCHING_SIMILARITY_THRESHOLD = 0.99

/**
 * Assert screenshot captured while executing test to available golden.
 *
 * @param screenshotRule Screenshot test rule.
 * @param filename Screenshot filename.
 */
internal fun assertScreenshot(screenshotRule: AndroidXScreenshotTestRule, filename: String) {
    onView(withId(R.id.test_fragment_container))
        .perform(
            captureToBitmap() {
                it.assertAgainstGolden(
                    screenshotRule,
                    filename,
                    MSSIMMatcher(MATCHING_SIMILARITY_THRESHOLD),
                )
            }
        )
}

/**
 * Assert screenshot captured while executing test to available golden.
 *
 * @param screenshotRule Screenshot test rule.
 * @param filename Screenshot filename.
 */
internal fun assertFullScreenshot(screenshotRule: AndroidXScreenshotTestRule, filename: String) {
    val bitmap = captureEntireScreenWithKeyboard()
    bitmap.assertAgainstGolden(
        screenshotRule,
        filename,
        MSSIMMatcher(MATCHING_SIMILARITY_THRESHOLD),
    )
}

fun captureEntireScreenWithKeyboard(): Bitmap {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiAutomation = instrumentation.uiAutomation

    return uiAutomation.takeScreenshot()
}
