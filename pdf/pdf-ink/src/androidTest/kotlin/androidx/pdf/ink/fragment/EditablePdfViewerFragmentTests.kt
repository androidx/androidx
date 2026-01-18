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

package androidx.pdf.ink.fragment

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.ext.SdkExtensions
import android.widget.LinearLayout
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.R as PdfR
import androidx.pdf.ink.R
import androidx.pdf.ink.view.AnnotationToolbar
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import androidx.pdf.util.FragmentTestUtils.scenarioLoadDocument
import androidx.pdf.util.ToolbarMatchers.matchesToolbarMask
import androidx.pdf.util.ToolbarMatchers.withDockState
import androidx.pdf.util.ToolbarViewActions
import androidx.pdf.util.ToolbarViewActions.performDragAndDrop
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfViewerFragmentTests {
    private lateinit var scenario: FragmentScenario<TestEditablePdfViewerFragment>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestEditablePdfViewerFragment>(
                    themeResId =
                        com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                    initialState = Lifecycle.State.INITIALIZED,
                )
                .onFragment { fragment ->
                    IdlingRegistry.getInstance()
                        .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
                }
    }

    @After
    fun cleanup() {
        if (!::scenario.isInitialized) return

        scenario.onFragment { fragment ->
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    private fun loadDocumentAndSetupFragment() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.RESUMED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )
        onIdle() // Wait for document to load

        scenario.onFragment { fragment ->
            fragment.setIsAnnotationIntentResolvable(true)
            fragment.isToolboxVisible = true
        }
    }

    private fun enterEditMode() {
        onView(withId(PdfR.id.edit_fab)).apply {
            check(matches(isDisplayed()))
            perform(click())
            check(matches(not(isDisplayed())))
        }
        onIdle()
    }

    @Test
    fun test_annotationToolbar_dockedAtBottom_andCanDragToStart() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        onView(withId(R.id.annotationToolbar))
            .check(matches(isDisplayed()))
            .check(matches(withDockState(DOCK_STATE_BOTTOM)))

        // Initiate drag event
        onView(withId(R.id.annotationToolbar)).perform(ViewActions.longClick())
        onIdle()
        // Verify toolbar collapses on long press
        onView(withId(R.id.collapsed_tool)).check(matches(isDisplayed()))

        // Drag to the left side of the screen
        performDragAndDrop(
            toolbarId = R.id.annotationToolbar,
            to = ToolbarViewActions.DragTarget.LEFT,
        )
        onIdle()

        // Verify toolbar docked to the left side
        onView(withId(R.id.annotationToolbar)).check(matches(withDockState(DOCK_STATE_START)))

        // Verify tool tray orientation is vertical
        scenario.onFragment { fragment ->
            val toolTray = fragment.view?.findViewById<LinearLayout>(R.id.tool_tray)
            assertThat(toolTray?.orientation).isEqualTo(LinearLayout.VERTICAL)
        }
    }

    @Test
    fun test_annotationToolbar_persistsDockStateThroughRotation() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // Move toolbar to the END (Right) side
        onView(withId(R.id.annotationToolbar)).perform(ViewActions.longClick())
        performDragAndDrop(
            toolbarId = R.id.annotationToolbar,
            to = ToolbarViewActions.DragTarget.RIGHT,
        )
        onIdle()

        // Rotate the device to Landscape
        scenario.onFragment { fragment ->
            fragment.activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onIdle()

        // Verify it remains on the END side after rotation
        onView(withId(R.id.annotationToolbar)).check(matches(withDockState(DOCK_STATE_END)))
    }

    @Test
    fun test_toolbarMovement_updatesWetStrokesMaskPath() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        var toolbar: AnnotationToolbar? = null
        scenario.onFragment { fragment ->
            toolbar = fragment.view?.findViewById(R.id.annotationToolbar)
        }

        // Initial check: Toolbar is at bottom, mask should be at bottom
        onView(withId(R.id.pdf_wet_strokes_view)).check(matches(matchesToolbarMask(toolbar!!)))

        // Move the toolbar to the START (Left) side
        onView(withId(R.id.annotationToolbar)).perform(ViewActions.longClick())
        performDragAndDrop(
            R.id.annotationToolbar,
            to = ToolbarViewActions.DragTarget.LEFT,
        ) // Using the helper from previous step
        onIdle()

        // Verify the mask path updated to the new location (START side)
        onView(withId(R.id.pdf_wet_strokes_view)).check(matches(matchesToolbarMask(toolbar!!)))
    }

    @Test
    fun test_annotationToolbar_reExpands_onLongPressWithoutMove() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // Perform Long Press but do NOT call performDragAndDrop
        onView(withId(R.id.annotationToolbar)).perform(ViewActions.longClick())
        onIdle()

        // Simulate releasing the touch (Action Up)
        onView(withId(R.id.annotationToolbar)).perform(click())
        // Since we didn't move, it should re-expand at same dock position
        onView(withId(R.id.tool_tray)).check(matches(isDisplayed()))
        onView(withId(R.id.collapsed_tool)).check(matches(not(isDisplayed())))
        onView(withId(R.id.annotationToolbar)).check(matches(withDockState(DOCK_STATE_BOTTOM)))
    }

    @Test
    fun test_editablePdfFragment_restoresViewportChanged_onForceReload() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        scenario.onFragment { fragment ->
            val pdfView = fragment.getPdfViewInstance()
            assertNotNull(pdfView)
            // scroll to a different page
            pdfView.scrollToPage(2)

            onIdle()

            val expectedFirstVisiblePage = 1

            // Exit edit mode to perform a force refresh
            fragment.isEditModeEnabled = false

            // Let force reload complete interaction
            onIdle()
            // After reload, assert we move to the 1st page
            assertThat(pdfView.firstVisiblePage).isEqualTo(expectedFirstVisiblePage)
        }
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val REQUIRED_EXTENSION_VERSION = 18

        fun isRequiredSdkExtensionAvailable(): Boolean {
            // Get the device's version for the specified SDK extension
            val deviceExtensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)
            return deviceExtensionVersion >= REQUIRED_EXTENSION_VERSION
        }
    }
}
