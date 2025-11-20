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

import PALETTE_VIEW_AFTER_CLICK_COLOR
import PALETTE_VIEW_AFTER_CLICK_EMOJI
import PALETTE_VIEW_WITH_HIGHLIGHT_ITEMS
import PALETTE_VIEW_WITH_PEN_ITEMS
import SCREENSHOT_GOLDEN_DIRECTORY
import android.view.View
import android.view.ViewGroup
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.test.R
import androidx.pdf.ink.view.colorpalette.ColorPaletteAdapter
import androidx.pdf.ink.view.colorpalette.ColorPaletteView
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.pdf.ink.view.colorpalette.model.getHighlightPaletteItems
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import assertScreenshot
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
        clickOnPaletteItemAt(COLOR_ITEM_INDEX)

        assertScreenshot(PALETTE_VIEW_ID, screenshotRule, PALETTE_VIEW_AFTER_CLICK_COLOR)
    }

    @Test
    fun testColorPaletteView_itemSelection_emoji() {
        setupColorPaletteView { view ->
            view.updatePaletteItems(getHighlightPaletteItems(view.context))
        }

        // Select a color first
        clickOnPaletteItemAt(COLOR_ITEM_INDEX)
        // Now select an emoji
        clickOnPaletteItemAt(EMOJI_ITEM_INDEX)

        assertScreenshot(PALETTE_VIEW_ID, screenshotRule, PALETTE_VIEW_AFTER_CLICK_EMOJI)
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

        clickOnPaletteItemAt(COLOR_ITEM_INDEX)

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

        clickOnPaletteItemAt(COLOR_ITEM_INDEX)
        // Select the same item again
        clickOnPaletteItemAt(COLOR_ITEM_INDEX)

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
            val defaultPadding =
                resources.getDimensionPixelSize(androidx.pdf.ink.R.dimen.padding_8dp)
            paletteView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)

            activity.container.addView(
                paletteView,
                ViewGroup.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.color_palette_width),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            callback(paletteView)
        }
    }

    /** Helper function to perform a click on a RecyclerView item at a specific position. */
    private fun clickOnPaletteItemAt(position: Int) {
        onView(isAssignableFrom(RecyclerView::class.java))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<
                    ColorPaletteAdapter.PaletteItemViewHolder
                >(
                    position,
                    click(),
                )
            )
    }

    companion object {
        private val PALETTE_VIEW_ID = View.generateViewId()
        private const val COLOR_ITEM_INDEX = 3
        private const val EMOJI_ITEM_INDEX = 23
    }
}
