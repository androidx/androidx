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

import PALETTE_COLOR_ITEM_SELECTED
import PALETTE_COLOR_ITEM_SELECTED_INVERSE_COLOR_TICK
import PALETTE_COLOR_ITEM_UNSELECTED
import SCREENSHOT_GOLDEN_DIRECTORY
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.view.colorpalette.PaletteItemView
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import assertScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ColorPaletteItemViewTest {

    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun test_palette_item_set_to_color_unselected() {
        setupPaletteItemView { item ->
            val color = getPenPaletteItems(item.context)[10]
            item.setPaletteItem(color)
        }

        assertScreenshot(PALETTE_ITEM_VIEW_ID, screenshotRule, PALETTE_COLOR_ITEM_UNSELECTED)
    }

    @Test
    fun test_palette_item_set_to_color_selected() {
        setupPaletteItemView { item ->
            val color = getPenPaletteItems(item.context)[4]
            item.setPaletteItem(color)
            item.setSelected(selected = true, animate = false)
        }

        assertScreenshot(PALETTE_ITEM_VIEW_ID, screenshotRule, PALETTE_COLOR_ITEM_SELECTED)
    }

    @Test
    fun test_palette_item_set_to_color_selected_inverseColorTick() {
        setupPaletteItemView { item ->
            val color = getPenPaletteItems(item.context)[10]
            item.setPaletteItem(color)
            item.setSelected(selected = true, animate = false)
        }

        assertScreenshot(
            PALETTE_ITEM_VIEW_ID,
            screenshotRule,
            PALETTE_COLOR_ITEM_SELECTED_INVERSE_COLOR_TICK,
        )
    }

    private fun setupPaletteItemView(callback: (PaletteItemView) -> Unit) {
        activityRule.scenario.onActivity { activity ->
            val paletteItemView = PaletteItemView(activity).apply { id = PALETTE_ITEM_VIEW_ID }
            activity.container.addView(
                paletteItemView,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )
            // allow caller to do additional setup
            callback(paletteItemView)
        }
    }

    companion object {
        private val PALETTE_ITEM_VIEW_ID = View.generateViewId()
    }
}
