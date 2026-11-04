/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.compose.annotation.toolbar

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.R
import androidx.pdf.annotation.toolbar.AnnotationToolbarDockState.DOCK_STATE_END
import androidx.pdf.annotation.toolbar.AnnotationToolbarDockState.DOCK_STATE_START
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import com.google.android.material.slider.Slider
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPdfApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class AnnotationToolbarScubaTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun test_annotationToolbar() {
        setupAnnotationToolbar()

        assertScreenshot(ANNOTATION_TOOLBAR)
    }

    @Test
    fun test_annotationToolbar_withPenSelected() {
        setupAnnotationToolbar { state ->
            val penTool = checkNotNull(state.toolbarView).findViewById<View>(R.id.pen_button)
            // Assert state drawable of pen tool when selected.
            penTool.isSelected = true
        }

        assertScreenshot(ANNOTATION_TOOLBAR_WITH_PEN_SELECTED)
    }

    @Test
    fun test_annotationToolbar_withSliderVisible() {
        setupAnnotationToolbar()

        onView(withId(R.id.pen_button)).perform(click())
        composeTestRule.waitForIdle()

        onView(
                allOf(
                    isAssignableFrom(Slider::class.java),
                    isDescendantOfA(withId(R.id.brush_size_selector)),
                )
            )
            .perform(setSliderValue(2.0f))
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE)
    }

    @Test
    fun test_annotationToolbar_withColorPaletteVisible() {
        setupAnnotationToolbar()

        onView(withId(R.id.color_palette_button)).perform(click())
        composeTestRule.waitForIdle()

        clickPaletteItemAt(9)
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE)
    }

    @Test
    fun test_annotationToolbar_inLightMode() {
        setupAnnotationToolbar(isDarkMode = false)

        // Select white color in light mode to test the color palette icon
        onView(withId(R.id.color_palette_button)).perform(click())
        composeTestRule.waitForIdle()

        clickPaletteItemAt(21)
        composeTestRule.waitForIdle()

        // Hide color palette
        onView(withId(R.id.color_palette_button)).perform(click())
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_IN_LIGHT_MODE)
    }

    @Test
    fun test_annotationToolbar_inDarkMode() {
        setupAnnotationToolbar(isDarkMode = true)

        assertScreenshot(ANNOTATION_TOOLBAR_IN_DARK_MODE)
    }

    @Test
    fun test_annotationToolbar_expanded_after_collapsed() {
        val state = setupAnnotationToolbar()

        onView(withId(R.id.color_palette_button)).perform(click())
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { state.isExpanded = false }
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_COLLAPSED)
    }

    @Test
    fun test_annotationToolbar_dockedStart_withBrushSlider() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_START }

        onView(withId(R.id.pen_button)).perform(click())
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_DOCKED_START_WITH_BRUSH_SIZE)
    }

    @Test
    fun test_annotationToolbar_dockedStart_withColorPalette() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_START }

        onView(withId(R.id.color_palette_button)).perform(click())
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_DOCKED_START_WITH_COLOR_PALETTE)
    }

    @Test
    fun test_annotationToolbar_dockedEnd_withBrushSlider() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_END }

        onView(withId(R.id.pen_button)).perform(click())
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_DOCKED_END_WITH_BRUSH_SIZE)
    }

    @Test
    fun test_annotationToolbar_dockedEnd_withColorPalette() {
        setupAnnotationToolbar { it.dockState = DOCK_STATE_END }

        onView(withId(R.id.color_palette_button)).perform(click())
        composeTestRule.waitForIdle()

        assertScreenshot(ANNOTATION_TOOLBAR_DOCKED_END_WITH_COLOR_PALETTE)
    }

    @Test
    fun test_annotationToolbar_withCustomTool() {
        setupAnnotationToolbar(
            content = {
                tool(key = "custom_tool") {
                    BasicText(
                        text = "Custom",
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        )

        assertScreenshot(ANNOTATION_TOOLBAR_WITH_CUSTOM_TOOL)
    }

    private fun setupAnnotationToolbar(
        isDarkMode: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(8.dp),
        content: AnnotationToolbarScope.() -> Unit = {},
        callback: (AnnotationToolbarState) -> Unit = {},
    ): AnnotationToolbarState {
        lateinit var state: AnnotationToolbarState
        composeTestRule.setContent {
            val baseContext = LocalContext.current
            val context =
                remember(baseContext, isDarkMode) {
                    if (isDarkMode) {
                        createDarkModeContext(baseContext)
                    } else {
                        baseContext
                    }
                }
            CompositionLocalProvider(LocalContext provides context) {
                state = rememberAnnotationToolbarState()
                Box(modifier = Modifier.fillMaxSize()) {
                    AnnotationToolbar(
                        state = state,
                        modifier = Modifier.testTag(ANNOTATION_TOOLBAR_TAG),
                        contentPadding = contentPadding,
                        content = content,
                    )
                }
            }
        }

        composeTestRule.runOnIdle {
            checkNotNull(state.toolbarView).areAnimationsEnabled = false
            callback(state)
        }
        composeTestRule.waitForIdle()
        return state
    }

    private fun assertScreenshot(goldenName: String) {
        composeTestRule
            .onNodeWithTag(ANNOTATION_TOOLBAR_TAG)
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(
                screenshotRule,
                goldenName,
                MSSIMMatcher(MATCHING_SIMILARITY_THRESHOLD),
            )
    }

    private fun clickPaletteItemAt(position: Int) {
        onView(withId(R.id.color_palette))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    position,
                    click(),
                )
            )
    }

    private fun setSliderValue(value: Float): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(Slider::class.java)
            }

            override fun getDescription(): String {
                return "Set slider value to $value"
            }

            override fun perform(uiController: UiController, view: View) {
                val slider = view as Slider
                if (value >= slider.valueFrom && value <= slider.valueTo) {
                    slider.value = value
                }
            }
        }
    }

    private fun createDarkModeContext(baseContext: Context): Context {
        val configuration =
            Configuration(baseContext.resources.configuration).apply {
                uiMode =
                    (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        Configuration.UI_MODE_NIGHT_YES
            }

        return baseContext.createConfigurationContext(configuration)
    }

    companion object {
        internal const val SCREENSHOT_GOLDEN_DIRECTORY = "pdf/pdf-compose"
        private const val ANNOTATION_TOOLBAR_TAG = "annotation_toolbar"
        private const val MATCHING_SIMILARITY_THRESHOLD = 0.99

        private const val ANNOTATION_TOOLBAR = "annotation_toolbar"
        private const val ANNOTATION_TOOLBAR_WITH_PEN_SELECTED =
            "annotation_toolbar_with_pen_selected"
        private const val ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE =
            "annotation_toolbar_with_slider_visible"
        private const val ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE =
            "annotation_toolbar_with_color_palette_visible"
        private const val ANNOTATION_TOOLBAR_IN_LIGHT_MODE = "annotation_toolbar_in_light_mode"
        private const val ANNOTATION_TOOLBAR_IN_DARK_MODE = "annotation_toolbar_in_dark_mode"
        private const val ANNOTATION_TOOLBAR_COLLAPSED = "annotation_toolbar_collapsed"
        private const val ANNOTATION_TOOLBAR_DOCKED_START_WITH_BRUSH_SIZE =
            "annotation_toolbar_docked_start_with_brush_size"
        private const val ANNOTATION_TOOLBAR_DOCKED_START_WITH_COLOR_PALETTE =
            "annotation_toolbar_docked_start_with_color_palette"
        private const val ANNOTATION_TOOLBAR_DOCKED_END_WITH_BRUSH_SIZE =
            "annotation_toolbar_docked_end_with_brush_size"
        private const val ANNOTATION_TOOLBAR_DOCKED_END_WITH_COLOR_PALETTE =
            "annotation_toolbar_docked_end_with_color_palette"
        private const val ANNOTATION_TOOLBAR_WITH_CUSTOM_TOOL =
            "annotation_toolbar_with_custom_tool"
    }
}
