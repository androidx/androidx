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

import ANNOTATION_TOOLBAR
import ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE
import ANNOTATION_TOOLBAR_WITH_PEN_SELECTED
import ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE
import SCREENSHOT_GOLDEN_DIRECTORY
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.R
import androidx.pdf.ink.view.brush.BrushSizeSelectorView
import androidx.pdf.ink.view.brush.model.BrushSizes.penBrushSizes
import androidx.pdf.ink.view.colorpalette.ColorPaletteView
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.pdf.ink.view.tool.AnnotationToolView
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
class AnnotationToolbarUiTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    @Test
    fun test_annotation_toolbar() {
        setupAnnotationToolbar { _ -> }

        assertScreenshot(ANNOTATION_TOOLBAR_VIEW_ID, screenshotRule, ANNOTATION_TOOLBAR)
    }

    @Test
    fun test_annotation_toolbar_with_pen_selected() {
        setupAnnotationToolbar { annotationToolbar ->
            val penTool = annotationToolbar.findViewById<AnnotationToolView>(R.id.pen_button)
            // Assert state drawable of pen tool when selected.
            penTool.isSelected = true
        }

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_WITH_PEN_SELECTED,
        )
    }

    @Test
    fun test_annotation_toolbar_with_slider_visible() {
        setupAnnotationToolbar { annotationToolbar ->
            val penTool = annotationToolbar.findViewById<AnnotationToolView>(R.id.pen_button)
            val brushSlider =
                annotationToolbar.findViewById<BrushSizeSelectorView>(R.id.brush_size_selector)

            penTool.isSelected = true

            // set brush slider to 3rd(index = 2) step
            brushSlider.brushSizeSlider.value = 2f
            // convert the predetermined size for pen to px and set on brush preview
            val brushSizeInPx = penBrushSizes[2].toPx(annotationToolbar.context)
            brushSlider.brushPreviewView.brushSize = brushSizeInPx
            // show brush slider view on toolbar
            brushSlider.visibility = View.VISIBLE
        }

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE,
        )
    }

    @Test
    fun test_annotation_toolbar_with_color_palette_visible() {
        setupAnnotationToolbar { annotationToolbar ->
            val penTool = annotationToolbar.findViewById<AnnotationToolView>(R.id.pen_button)
            val colorPaletteView =
                annotationToolbar.findViewById<ColorPaletteView>(R.id.color_palette)

            penTool.isSelected = true
            // sets the pen color palette items
            val penColorPalette = getPenPaletteItems(annotationToolbar.context)
            colorPaletteView.updatePaletteItems(
                paletteItems = penColorPalette,
                currentSelectedIndex = 9,
            )
            // show color palette view on toolbar
            colorPaletteView.visibility = View.VISIBLE
        }

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE,
        )
    }

    private fun setupAnnotationToolbar(callback: (AnnotationToolbar) -> Unit) {
        activityRule.scenario.onActivity { activity ->
            val annotationToolbar =
                AnnotationToolbar(activity).apply {
                    id = ANNOTATION_TOOLBAR_VIEW_ID
                    elevation = context.resources.getDimension(R.dimen.annotation_toolbar_elevation)
                    val defaultPadding =
                        context.resources.getDimensionPixelSize(R.dimen.padding_8dp)
                    setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
                }
            activity.container.addView(
                annotationToolbar,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )
            // allow caller to do additional setup
            callback(annotationToolbar)
        }
    }

    private fun Int.toPx(context: Context): Float =
        this.toFloat() * context.resources.displayMetrics.density

    companion object {
        private val ANNOTATION_TOOLBAR_VIEW_ID = View.generateViewId()
    }
}
