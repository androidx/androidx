/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas.internal

import android.os.Build
import androidx.ink.rendering.android.canvas.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator-based screenshot test of [CanvasMeshRenderer]. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CanvasMeshRendererScreenshotTest {

    @get:Rule
    val activityScenarioRule =
        ActivityScenarioRule(CanvasMeshRendererScreenshotTestActivity::class.java)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun onView_showsSimpleStroke() {
        assertScreenshot("app_steady_state")
    }

    private fun assertScreenshot(filename: String) {
        onView(isRoot())
            .perform(
                captureToBitmap() {
                    it.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$filename")
                }
            )
    }
}
