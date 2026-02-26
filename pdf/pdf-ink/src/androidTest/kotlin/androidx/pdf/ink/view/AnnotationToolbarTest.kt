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

import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.R
import androidx.pdf.ink.util.setSliderValue
import androidx.pdf.ink.util.withSliderValue
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import androidx.pdf.ink.view.tool.AnnotationToolView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.material.slider.Slider
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class AnnotationToolbarTest {

    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    @After
    fun tearDown() {
        // Reset the on create callback
        PdfTestActivity.onCreateCallback = {}
    }

    @Test
    fun testPenButtonClicked_whenPenToolIsSelected() {
        var penTool: AnnotationToolView? = null
        setupAnnotationToolbar { penTool = it.findViewById(R.id.pen_button) }

        assertNotNull(penTool)
        // Assert pen is selected by default
        assertTrue(penTool.isSelected)

        onView(withId(R.id.pen_button)).perform(click())
        // Assert brush slider is visible
        onView(withId(R.id.brush_size_selector)).check(matches(isDisplayed()))

        // Clicking again should hide the brush slider view
        onView(withId(R.id.pen_button)).perform(click())
        onView(withId(R.id.brush_size_selector)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testPenButtonClicked_whenPenToolIsNotSelected() {
        var penTool: AnnotationToolView? = null
        setupAnnotationToolbar {
            penTool = it.findViewById(R.id.pen_button)
            // Clear any tool selection
            it.clearToolSelection()
        }

        assertNotNull(penTool)
        assertFalse(penTool.isSelected)
        // click on the pen tool
        onView(withId(R.id.pen_button)).perform(click())

        assertTrue(penTool.isSelected)
    }

    @Test
    fun testColorPalette_whenPenToolIsSelected() {
        setupAnnotationToolbar()
        assertColorPaletteChecks()
    }

    @Test
    fun testColorPalette_whenHighlighterIsSelected() {
        setupAnnotationToolbar()
        onView(withId(R.id.highlighter_button)).perform(click())
        assertColorPaletteChecks()
    }

    @Test
    fun test_colorPalette_brushSlider_mutuallyExclusive_whenPenToolSelected() {
        setupAnnotationToolbar()
        assertBrushSliderAndColorPaletteMutualExclusion(R.id.pen_button)
    }

    @Test
    fun test_colorPalette_brushSlider_mutuallyExclusive_whenHighlighterToolSelected() {
        setupAnnotationToolbar()
        onView(withId(R.id.highlighter_button)).perform(click())
        assertBrushSliderAndColorPaletteMutualExclusion(R.id.highlighter_button)
    }

    @Test
    fun testEraserButtonClicked_selectsEraserAndDisablesColorPalette() {
        var eraserTool: AnnotationToolView? = null
        setupAnnotationToolbar { eraserTool = it.findViewById(R.id.eraser_button) }

        assertNotNull(eraserTool)
        // Initially eraser is not selected
        assertFalse(eraserTool.isSelected)

        // Click on the eraser tool
        onView(withId(R.id.eraser_button)).perform(click())

        // Assert eraser is now selected
        assertTrue(eraserTool.isSelected)

        // Assert color palette button is disabled and brush slider is not shown
        onView(withId(R.id.color_palette_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.brush_size_selector)).check(matches(not(isDisplayed())))

        // Clicking eraser again should do nothing to the brush slider
        onView(withId(R.id.eraser_button)).perform(click())
        onView(withId(R.id.brush_size_selector)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testUndoRedoButtons_initialStateAndEnabling() {
        setupAnnotationToolbar()

        // Initially, undo and redo are disabled
        onView(withId(R.id.undo_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.redo_button)).check(matches(not(isEnabled())))

        // Enable canUndo and canRedo
        activityRule.scenario.onActivity { activity ->
            val toolbar = activity.findViewById<AnnotationToolbar>(ANNOTATION_TOOLBAR_VIEW_ID)
            toolbar.canUndo = true
            toolbar.canRedo = true
        }

        // Assert they are now enabled
        onView(withId(R.id.undo_button)).check(matches(isEnabled()))
        onView(withId(R.id.redo_button)).check(matches(isEnabled()))
    }

    @Test
    fun testUndoRedoButtons_clickListeners() {
        var undoClicked = false
        var redoClicked = false
        setupAnnotationToolbar {
            it.setAnnotationToolbarListener(
                object : AnnotationToolbar.AnnotationToolbarListener {
                    override fun onToolChanged(
                        toolInfo: androidx.pdf.ink.view.tool.AnnotationToolInfo
                    ) {}

                    override fun onUndo() {
                        undoClicked = true
                    }

                    override fun onRedo() {
                        redoClicked = true
                    }

                    override fun onAnnotationVisibilityChanged(isVisible: Boolean) {}
                }
            )
            // Enable the buttons for the test
            it.canUndo = true
            it.canRedo = true
        }

        onView(withId(R.id.undo_button)).perform(click())
        assertTrue(undoClicked)

        onView(withId(R.id.redo_button)).perform(click())
        assertTrue(redoClicked)
    }

    @Test
    fun testToggleAnnotationVisibility_disablesOtherTools() {
        var toggleButton: AnnotationToolView? = null
        setupAnnotationToolbar {
            toggleButton = it.findViewById(R.id.toggle_annotation_button)
            // Pre-enable undo/redo to ensure the toggle disables them
            it.canUndo = true
            it.canRedo = true
        }

        assertNotNull(toggleButton)
        // Initially not selected (annotations are visible)
        assertFalse(toggleButton.isSelected)

        // Click to hide annotations
        onView(withId(R.id.toggle_annotation_button)).perform(click())

        // Assert it is now selected
        assertTrue(toggleButton.isSelected)

        // Assert other tools are disabled
        onView(withId(R.id.pen_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.highlighter_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.eraser_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.color_palette_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.undo_button)).check(matches(not(isEnabled())))
        onView(withId(R.id.redo_button)).check(matches(not(isEnabled())))

        // Click again to show annotations
        onView(withId(R.id.toggle_annotation_button)).perform(click())

        // Assert it is no longer selected
        assertFalse(toggleButton.isSelected)

        // Assert other tools are enabled again
        onView(withId(R.id.pen_button)).check(matches(isEnabled()))
        onView(withId(R.id.highlighter_button)).check(matches(isEnabled()))
        onView(withId(R.id.eraser_button)).check(matches(isEnabled()))
        onView(withId(R.id.color_palette_button)).check(matches(isEnabled()))
        onView(withId(R.id.undo_button)).check(matches(isEnabled()))
        onView(withId(R.id.redo_button)).check(matches(isEnabled()))
    }

    @Test
    fun testAnnotationToolbar_restoresStateOnConfigChange() {
        PdfTestActivity.onCreateCallback = { activity ->
            activity.container.addView(
                createToolbar(activity),
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )
        }

        with(ActivityScenario.launch(PdfTestActivity::class.java)) {
            // select highlighter tool
            onView(withId(R.id.highlighter_button)).perform(click())
            onView(withId(R.id.highlighter_button)).check(matches(isSelected()))
            // Click again to show the brush slider
            onView(withId(R.id.highlighter_button)).perform(click())
            // Verify the container (BrushSizeSelectorView) is displayed
            onView(withId(R.id.brush_size_selector)).check(matches(isDisplayed()))

            // Slide the brush slider to a new value (e.g., 3.0f)
            onView(
                    allOf(
                        isAssignableFrom(Slider::class.java),
                        isDescendantOfA(withId(R.id.brush_size_selector)),
                    )
                )
                .perform(setSliderValue(3.0f))

            // recreate the activity
            recreate()

            // Assert toolbar restored
            onView(withId(R.id.highlighter_button)).check(matches(isSelected()))
            onView(withId(R.id.brush_size_selector)).check(matches(isDisplayed()))
            onView(
                    allOf(
                        isAssignableFrom(Slider::class.java),
                        isDescendantOfA(withId(R.id.brush_size_selector)),
                    )
                )
                .check(matches(withSliderValue(3.0f)))
        }
    }

    @Test
    fun testAnnotationToolbar_isConfigPopupVisible() {
        var annotationToolbar: AnnotationToolbar? = null
        setupAnnotationToolbar { annotationToolbar = it }

        assertNotNull(annotationToolbar)
        assertFalse(annotationToolbar.isConfigPopupVisible)

        onView(withId(R.id.pen_button)).perform(click())
        assertTrue(annotationToolbar.isConfigPopupVisible)
        annotationToolbar.dismissPopups()
        assertFalse(annotationToolbar.isConfigPopupVisible)

        onView(withId(R.id.color_palette_button)).perform(click())
        assertTrue(annotationToolbar.isConfigPopupVisible)
        annotationToolbar.dismissPopups()
        assertFalse(annotationToolbar.isConfigPopupVisible)
    }

    @Test
    fun testSetDockState_updatesOrientationAndConstraints() {
        var toolbar: AnnotationToolbar? = null
        setupAnnotationToolbar { toolbar = it }

        assertNotNull(toolbar)

        // Change dock state to START (Vertical)
        activityRule.scenario.onActivity { toolbar.dockState = DOCK_STATE_START }
        onIdle()

        // Verify tool tray orientation updated to Vertical
        val toolTray = toolbar.findViewById<LinearLayout>(R.id.tool_tray)
        assertThat(toolTray.orientation).isEqualTo(LinearLayout.VERTICAL)

        // Change dock state back to BOTTOM (Horizontal)
        activityRule.scenario.onActivity { toolbar.dockState = DOCK_STATE_BOTTOM }
        onIdle()

        // Verify tool tray orientation reverted to Horizontal
        assertThat(toolTray?.orientation).isEqualTo(LinearLayout.HORIZONTAL)
    }

    @Test
    fun testDockState_restoresOnConfigChange() {
        // Prepare activity with a toolbar
        PdfTestActivity.onCreateCallback = { activity ->
            activity.container.addView(
                createToolbar(activity),
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )
        }

        with(ActivityScenario.launch(PdfTestActivity::class.java)) {
            // Set dock state to END
            onActivity { activity ->
                val toolbar = activity.findViewById<AnnotationToolbar>(ANNOTATION_TOOLBAR_VIEW_ID)
                toolbar.dockState = DOCK_STATE_END
            }
            onIdle()

            // Recreate activity
            recreate()

            // Assert dock state is restored
            onActivity { activity ->
                val toolbar = activity.findViewById<AnnotationToolbar>(ANNOTATION_TOOLBAR_VIEW_ID)
                assertThat(toolbar.dockState).isEqualTo(DOCK_STATE_END)

                val toolTray = toolbar.findViewById<LinearLayout>(R.id.tool_tray)
                assertThat(toolTray.orientation).isEqualTo(LinearLayout.VERTICAL)
            }
        }
    }

    @Test
    fun testToolbarScrollable_whenDockedAtBottom() {
        var toolbar: AnnotationToolbar? = null
        setupAnnotationToolbar { toolbar = it }

        activityRule.scenario.onActivity {
            repeat(DUMMY_TOOLS_COUNT) {
                toolbar?.toolTray?.addView(createAnnotationTool(toolbar.context))
            }
        }

        // Assert pen tool, (i.e. 1st option in tool tray) is visible
        onView(withId(R.id.pen_button)).check(matches(isDisplayed()))

        onView(withId(R.id.scrollable_tool_tray_container)).perform(swipeLeft())

        // Assert after scroll pen tool is no longer visible
        onView(withId(R.id.pen_button)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testToolbarScrollable_whenDockedAtStart() {
        var toolbar: AnnotationToolbar? = null
        setupAnnotationToolbar {
            toolbar = it
            toolbar.dockState = DOCK_STATE_START
        }

        activityRule.scenario.onActivity {
            repeat(DUMMY_TOOLS_COUNT) {
                toolbar?.toolTray?.addView(createAnnotationTool(toolbar.context))
            }
        }

        // Assert pen tool, (i.e. 1st option in tool tray) is visible
        onView(withId(R.id.pen_button)).check(matches(isDisplayed()))

        onView(withId(R.id.scrollable_tool_tray_container)).perform(swipeUp())

        // Assert after scroll pen tool is no longer visible
        onView(withId(R.id.pen_button)).check(matches(not(isDisplayed())))
    }

    private fun assertColorPaletteChecks() {
        onView(withId(R.id.color_palette_button)).check(matches(isEnabled()))
        // assert initially color palette is not visible
        onView(withId(R.id.color_palette)).check(matches(not(isDisplayed())))

        // click on the color palette
        onView(withId(R.id.color_palette_button)).perform(click())
        onView(withId(R.id.color_palette)).check(matches(isDisplayed()))

        // clicking again should toggle color palette
        onView(withId(R.id.color_palette_button)).perform(click())
        onView(withId(R.id.color_palette)).check(matches(not(isDisplayed())))
    }

    private fun assertBrushSliderAndColorPaletteMutualExclusion(viewId: Int) {
        onView(withId(R.id.color_palette_button)).check(matches(isEnabled()))

        // click on the color palette
        onView(withId(R.id.color_palette_button)).perform(click())
        onView(withId(R.id.color_palette)).check(matches(isDisplayed()))
        onView(withId(R.id.brush_size_selector)).check(matches(not(isDisplayed())))

        // click on the pen tool
        onView(withId(viewId)).perform(click())

        // assert color palette and brush slider are mutually visible
        onView(withId(R.id.color_palette)).check(matches(not(isDisplayed())))
        onView(withId(R.id.brush_size_selector)).check(matches(isDisplayed()))

        onView(withId(R.id.color_palette_button)).perform(click())

        // assert color palette and brush slider are mutually visible
        onView(withId(R.id.brush_size_selector)).check(matches(not(isDisplayed())))
        onView(withId(R.id.color_palette)).check(matches(isDisplayed()))
    }

    private fun setupAnnotationToolbar(callback: ((AnnotationToolbar) -> Unit) = {}) {
        activityRule.scenario.onActivity { activity ->
            val annotationToolbar = createToolbar(activity)
            activity.container.addView(
                annotationToolbar,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )
            // allow caller to do additional setup
            callback(annotationToolbar)

            // wait for the view to become visible
            onIdle()
        }
    }

    private fun createAnnotationTool(context: Context): AnnotationToolView {
        return AnnotationToolView(context).apply {
            layoutParams =
                LayoutParams(
                    LayoutParams(
                        context.resources.getDimensionPixelSize(R.dimen.annotation_tool_width),
                        context.resources.getDimensionPixelSize(R.dimen.annotation_tool_height),
                    )
                )
            icon = context.getDrawable(R.drawable.pen_state_drawable)
            backgroundTintList =
                context.getColorStateList(R.color.annotation_tool_background_color_state)
        }
    }

    private fun createToolbar(context: Context): AnnotationToolbar =
        AnnotationToolbar(context).apply {
            id = ANNOTATION_TOOLBAR_VIEW_ID
            areAnimationsEnabled = false
        }

    companion object {
        private const val ANNOTATION_TOOLBAR_VIEW_ID = 4091995
        private const val DUMMY_TOOLS_COUNT = 20
    }
}
