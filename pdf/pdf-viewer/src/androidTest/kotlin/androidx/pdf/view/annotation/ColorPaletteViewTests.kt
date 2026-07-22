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

package androidx.pdf.view.annotation

import PALETTE_VIEW_AFTER_CLICK_COLOR
import PALETTE_VIEW_WITH_HIGHLIGHT_ITEMS
import PALETTE_VIEW_WITH_PEN_ITEMS
import SCREENSHOT_GOLDEN_DIRECTORY
import android.view.View
import android.view.ViewGroup
import androidx.pdf.PdfTestActivity
import androidx.pdf.R
import androidx.pdf.assertScreenshot
import androidx.pdf.util.clickItemAt
import androidx.pdf.view.annotation.colorpalette.ColorPaletteAdapter
import androidx.pdf.view.annotation.colorpalette.ColorPaletteView
import androidx.pdf.view.annotation.colorpalette.model.PaletteItem
import androidx.pdf.view.annotation.colorpalette.model.getHighlightPaletteItems
import androidx.pdf.view.annotation.colorpalette.model.getPenPaletteItems
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ColorPaletteViewTests {
    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun testColorPaletteView_colorPaletteItems() {
        setupColorPaletteView { view -> view.updatePaletteItems(getPenPaletteItems(view.context)) }

        assertScreenshot(PALETTE_VIEW_ID, screenshotRule, PALETTE_VIEW_WITH_PEN_ITEMS)
    }

    @Test
    fun testColorPaletteView_highlightPaletteItems() {
        setupColorPaletteView { view ->
            view.updatePaletteItems(getHighlightPaletteItems(view.context))
        }

        assertScreenshot(PALETTE_VIEW_ID, screenshotRule, PALETTE_VIEW_WITH_HIGHLIGHT_ITEMS)
    }

    @Test
    fun testColorPaletteView_itemSelection_color() {
        setupColorPaletteView { view -> view.updatePaletteItems(getPenPaletteItems(view.context)) }

        // Click on the 4th item (index 3) using RecyclerViewActions
        clickItemAt<ColorPaletteAdapter.PaletteItemViewHolder>(COLOR_ITEM_INDEX)

        assertScreenshot(PALETTE_VIEW_ID, screenshotRule, PALETTE_VIEW_AFTER_CLICK_COLOR)
    }

    @Test
    fun testColorPaletteView_itemSelection_noPriorSelection() {
        var colorToAssert: PaletteItem? = null
        var selectedItem: PaletteItem? = null

        setupColorPaletteView { view ->
            val penItems = getPenPaletteItems(view.context)
            view.updatePaletteItems(penItems)
            colorToAssert = penItems[COLOR_ITEM_INDEX]

            view.setPaletteItemSelectedListener(
                getPaletteItemSelectedListener { _, item -> selectedItem = item }
            )
        }

        clickItemAt<ColorPaletteAdapter.PaletteItemViewHolder>(COLOR_ITEM_INDEX)

        // Verify that the listener was called with the correct item
        assertThat(selectedItem).isEqualTo(colorToAssert)
    }

    @Test
    fun testColorPaletteView_itemSelection_sameItem() {
        var colorToAssert: PaletteItem? = null
        var selectedItem: PaletteItem? = null
        var callbackTriggered = 0

        setupColorPaletteView { view ->
            val penItems = getPenPaletteItems(view.context)
            view.updatePaletteItems(penItems)
            colorToAssert = penItems[COLOR_ITEM_INDEX]

            view.setPaletteItemSelectedListener(
                getPaletteItemSelectedListener { _, item ->
                    callbackTriggered++
                    selectedItem = item
                }
            )
        }

        clickItemAt<ColorPaletteAdapter.PaletteItemViewHolder>(COLOR_ITEM_INDEX)
        // Select the same item again
        clickItemAt<ColorPaletteAdapter.PaletteItemViewHolder>(COLOR_ITEM_INDEX)

        // Verify that the listener was called with the correct item
        assertThat(selectedItem).isEqualTo(colorToAssert)
        // Verify that the listener was called no. of times item clicked
        assertThat(callbackTriggered).isEqualTo(2)
    }

    private fun getPaletteItemSelectedListener(
        action: (Int, PaletteItem) -> Unit
    ): ColorPaletteView.PaletteItemSelectedListener {
        return object : ColorPaletteView.PaletteItemSelectedListener {
            override fun onItemSelected(index: Int, paletteItem: PaletteItem) {
                action(index, paletteItem)
            }
        }
    }

    private fun setupColorPaletteView(callback: (ColorPaletteView) -> Unit) {
        activityRule.scenario.onActivity { activity ->
            val paletteView =
                ColorPaletteView(activity).apply {
                    id = PALETTE_VIEW_ID
                    areAnimationsEnabled = false
                }

            val resources = paletteView.context.resources
            val defaultPadding = resources.getDimensionPixelSize(R.dimen.padding_8dp)
            paletteView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)

            activity.container.addView(
                paletteView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            callback(paletteView)
        }
    }

    companion object {
        private val PALETTE_VIEW_ID = View.generateViewId()
        private const val COLOR_ITEM_INDEX = 3
    }
}
