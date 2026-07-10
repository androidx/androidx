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

package androidx.pdf.ink.view

import BRUSH_SIZE_IN_VERTICAL_ORIENTATION
import BRUSH_SIZE_SELECTED_ON_STEP_0
import BRUSH_SIZE_SELECTED_ON_STEP_4
import SCREENSHOT_GOLDEN_DIRECTORY
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout.HORIZONTAL
import android.widget.LinearLayout.VERTICAL
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.view.brush.BrushSizeSelectorView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import assertScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class BrushSelectorViewUiTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    @Test
    fun test_brushSizeSelector_onStep0() {
        setupBrushSizeSelectorView { _ -> }

        assertScreenshot(BRUSH_SELECTOR_VIEW_ID, screenshotRule, BRUSH_SIZE_SELECTED_ON_STEP_0)
    }

    @Test
    fun test_brushSizeSelector_onStep4() {
        setupBrushSizeSelectorView { view ->
            view.brushSizeSlider.value = 4f
            view.brushPreviewView.brushSize = 100f
        }

        assertScreenshot(BRUSH_SELECTOR_VIEW_ID, screenshotRule, BRUSH_SIZE_SELECTED_ON_STEP_4)
    }

    @Test
    fun test_brushSizeSelector_vertical() {
        setupBrushSizeSelectorView(VERTICAL) { view ->
            view.brushSizeSlider.value = 4f
            view.brushPreviewView.brushSize = 100f
        }

        assertScreenshot(BRUSH_SELECTOR_VIEW_ID, screenshotRule, BRUSH_SIZE_IN_VERTICAL_ORIENTATION)
    }

    private fun setupBrushSizeSelectorView(
        orientation: Int = HORIZONTAL,
        callback: (BrushSizeSelectorView) -> Unit,
    ) {
        activityRule.scenario.onActivity { activity ->
            val brushSizeSelectorView =
                BrushSizeSelectorView(activity).apply { id = BRUSH_SELECTOR_VIEW_ID }
            brushSizeSelectorView.orientation = orientation
            val layoutParams =
                if (orientation == VERTICAL) {
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
                } else {
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                }
            activity.container.addView(brushSizeSelectorView, layoutParams)
            // allow caller to do additional setup
            callback(brushSizeSelectorView)
        }
    }

    companion object {
        private val BRUSH_SELECTOR_VIEW_ID = View.generateViewId()
    }
}
