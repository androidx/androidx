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
import ANNOTATION_TOOLBAR_COLLAPSED
import ANNOTATION_TOOLBAR_DOCKED_END_WITH_BRUSH_SIZE
import ANNOTATION_TOOLBAR_DOCKED_END_WITH_COLOR_PALETTE
import ANNOTATION_TOOLBAR_DOCKED_START_WITH_BRUSH_SIZE
import ANNOTATION_TOOLBAR_DOCKED_START_WITH_COLOR_PALETTE
import ANNOTATION_TOOLBAR_IN_DARK_MODE
import ANNOTATION_TOOLBAR_IN_LIGHT_MODE
import ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE
import ANNOTATION_TOOLBAR_WITH_PEN_SELECTED
import ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE
import SCREENSHOT_GOLDEN_DIRECTORY
import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.R
import androidx.pdf.ink.util.clickItemAt
import androidx.pdf.ink.util.setSliderValue
import androidx.pdf.ink.view.colorpalette.ColorPaletteAdapter
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import androidx.pdf.ink.view.tool.AnnotationToolView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import assertScreenshot
import com.google.android.material.slider.Slider
import org.hamcrest.CoreMatchers.allOf
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
        setupAnnotationToolbar()

        onView(withId(R.id.pen_button)).perform(click())
        onView(
                allOf(
                    isAssignableFrom(Slider::class.java),
                    isDescendantOfA(withId(R.id.brush_size_selector)),
                )
            )
            .perform(setSliderValue(2.0f))

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE,
        )
    }

    @Test
    fun test_annotation_toolbar_with_color_palette_visible() {
        setupAnnotationToolbar()

        onView(withId(R.id.color_palette_button)).perform(click())
        clickItemAt<ColorPaletteAdapter.PaletteItemViewHolder>(9)

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE,
        )
    }

    @Test
    fun test_annotation_toolbar_in_light_mode() {
        setupAnnotationToolbar(isDarkMode = false)
        // select white color in light mode to test the color palette icon
        onView(withId(R.id.color_palette_button)).perform(click())
        clickItemAt<ColorPaletteAdapter.PaletteItemViewHolder>(21)
        // Hide color palette
        onView(withId(R.id.color_palette_button)).perform(click())

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_IN_LIGHT_MODE,
        )
    }

    @Test
    fun test_annotation_toolbar_in_dark_mode() {
        setupAnnotationToolbar(isDarkMode = true)

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_IN_DARK_MODE,
        )
    }

    @Test
    fun test_annotationToolbar_expanded_after_collapsed() {
        var annotationToolbar: AnnotationToolbar? = null
        setupAnnotationToolbar { annotationToolbar = it }

        if (annotationToolbar == null) return

        onView(withId(R.id.color_palette_button)).perform(click())

        annotationToolbar.collapseToolbar()

        assertScreenshot(ANNOTATION_TOOLBAR_VIEW_ID, screenshotRule, ANNOTATION_TOOLBAR_COLLAPSED)
    }

    @Test
    fun test_annotationToolbar_dockedStart_withBrushSlider() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_START }
        // Open brush slider
        onView(withId(R.id.pen_button)).perform(click())

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_DOCKED_START_WITH_BRUSH_SIZE,
        )
    }

    @Test
    fun test_annotationToolbar_dockedStart_withColorPalette() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_START }
        // Open color palette
        onView(withId(R.id.color_palette_button)).perform(click())

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_DOCKED_START_WITH_COLOR_PALETTE,
        )
    }

    @Test
    fun test_annotationToolbar_dockedEnd_withBrushSlider() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_END }
        // Open brush slider
        onView(withId(R.id.pen_button)).perform(click())

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_DOCKED_END_WITH_BRUSH_SIZE,
        )
    }

    @Test
    fun test_annotationToolbar_dockedEnd_withColorPalette() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_END }
        // Open color palette
        onView(withId(R.id.color_palette_button)).perform(click())

        assertScreenshot(
            ANNOTATION_TOOLBAR_VIEW_ID,
            screenshotRule,
            ANNOTATION_TOOLBAR_DOCKED_END_WITH_COLOR_PALETTE,
        )
    }

    private fun setupAnnotationToolbar(
        isDarkMode: Boolean = false,
        callback: (AnnotationToolbar) -> Unit = {},
    ) {
        activityRule.scenario.onActivity { activity ->
            // 1. Create a context that matches the desired mode
            val themedContext =
                if (isDarkMode) {
                    createDarkModeContext(activity)
                } else activity

            // 2. Instantiate and configure the view
            val annotationToolbar =
                AnnotationToolbar(themedContext).apply {
                    id = ANNOTATION_TOOLBAR_VIEW_ID
                    applyDefaultStyling()
                    areAnimationsEnabled = false
                }

            // 3. Add to hierarchy
            activity.container.addView(
                annotationToolbar,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )

            callback(annotationToolbar)
        }

        onIdle()
    }

    /** Creates a context wrapper that forces Dark Mode and applies Material3 styling. */
    private fun createDarkModeContext(baseContext: Context): Context {
        val configuration =
            Configuration(baseContext.resources.configuration).apply {
                uiMode =
                    (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        Configuration.UI_MODE_NIGHT_YES
            }

        val darkModeContext = baseContext.createConfigurationContext(configuration)
        return ContextThemeWrapper(
            darkModeContext,
            com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight,
        )
    }

    /** Extension function to keep the setup logic clean. */
    private fun AnnotationToolbar.applyDefaultStyling() {
        val padding = context.resources.getDimensionPixelSize(R.dimen.padding_8dp)
        setPadding(padding, padding, padding, padding)
        elevation = context.resources.getDimension(R.dimen.annotation_toolbar_elevation)
    }

    private fun Int.toPx(context: Context): Float =
        this.toFloat() * context.resources.displayMetrics.density

    companion object {
        private val ANNOTATION_TOOLBAR_VIEW_ID = View.generateViewId()
    }
}
