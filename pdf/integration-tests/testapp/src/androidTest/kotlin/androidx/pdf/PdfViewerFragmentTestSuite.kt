/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf

import android.content.pm.ActivityInfo
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.actions.SelectionViewActions
import androidx.pdf.matchers.SearchViewAssertions
import androidx.pdf.util.Preconditions
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerFragmentTestSuite {

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragment>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED
            )
        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    private fun scenarioLoadDocument(
        filename: String,
        nextState: Lifecycle.State,
        orientation: Int
    ): FragmentScenario<TestPdfViewerFragment> {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        scenario.moveToState(nextState)
        scenario.onFragment { it.requireActivity().requestedOrientation = orientation }

        // Loading view assertion
        onView(withId(R.id.loadingView)).check(matches(isDisplayed()))

        // Load the document in the fragment
        scenario.onFragment { fragment ->
            fragment.pdfLoadingIdlingResource.increment()
            fragment.documentUri = TestUtils.saveStream(inputStream, fragment.requireContext())
        }

        return scenario
    }

    @Test
    fun testPdfViewerFragment_setDocumentUri() {
        val scenario =
            scenarioLoadDocument(
                TEST_DOCUMENT_FILE,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )

        // Delay required for the PDF to load
        onView(withId(R.id.loadingView))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}"
            )
        }

        // Swipe actions
        onView(withId(R.id.parent_pdf_container)).perform(swipeUp())
        onView(withId(R.id.parent_pdf_container)).perform(swipeDown())

        // Selection
        val selectionViewActions = SelectionViewActions()
        onView(isRoot())
            .perform(
                GeneralClickAction(
                    Tap.LONG,
                    { view ->
                        GeneralLocation.CENTER.calculateCoordinates(view)
                            .map { it + 20f }
                            .toFloatArray()
                    },
                    Press.THUMB,
                    InputDevice.SOURCE_UNKNOWN,
                    MotionEvent.BUTTON_PRIMARY
                )
            )
        onView(withId(R.id.start_drag_handle)).check(matches(isDisplayed()))
        onView(withId(R.id.stop_drag_handle)).check(matches(isDisplayed()))

        onView(withId(R.id.parent_pdf_container))
            .perform(selectionViewActions.longClickAndDragRight())
        onView(withId(R.id.parent_pdf_container)).check(selectionViewActions.stopHandleMoved())
    }

    @Test
    fun testPdfViewerFragment_isTextSearchActive_toggleMenu() {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val scenario =
            scenarioLoadDocument(
                TEST_DOCUMENT_FILE,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )

        onView(withId(R.id.loadingView))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}"
            )
        }

        // Toggle search menu
        val searchViewAssertion = SearchViewAssertions()
        scenario.onFragment { it.isTextSearchActive = true }
        onView(withId(R.id.search_container)).check(matches(isDisplayed()))

        onView(withId(R.id.find_query_box)).perform(typeText(SEARCH_QUERY))
        onView(withId(R.id.match_status_textview)).check(matches(isDisplayed()))
        onView(withId(R.id.match_status_textview)).check(searchViewAssertion.extractAndMatch())

        // Prev/next search results
        onView(withId(R.id.find_prev_btn)).perform(click())
        val keyboard = uiDevice.findObject(UiSelector().descriptionContains(KEYBOARD_CONTENT_DESC))
        // Assert keyboard is dismissed on clicking prev/next
        assertFalse(keyboard.exists())
        onView(withId(R.id.match_status_textview)).check(searchViewAssertion.matchPrevious())
        onView(withId(R.id.find_next_btn)).perform(click())
        onView(withId(R.id.match_status_textview)).check(searchViewAssertion.matchNext())
        onView(withId(R.id.find_next_btn)).perform(click())
        onView(withId(R.id.match_status_textview)).check(searchViewAssertion.matchNext())

        // Assert for keyboard collapse
        onView(withId(R.id.find_query_box)).perform(click())
        onView(withId(R.id.close_btn)).perform(click())
        onView(withId(R.id.find_query_box))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    /**
     * This test verifies the behavior of the Pdf viewer in immersive mode, specifically the
     * visibility of the toolbox and the host app's search button.
     */
    @Test
    fun testPdfViewerFragment_immersiveMode_toggleMenu() {
        // Load a PDF document into the fragment
        val scenario =
            scenarioLoadDocument(
                TEST_DOCUMENT_FILE,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )

        // Check that the document is loaded successfully
        onView(withId(R.id.loadingView))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}"
            )
        }

        // Show the toolbox and check visibility of buttons
        scenario.onFragment { it.isToolboxVisible = true }
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed()))
        onView(withId(androidx.pdf.testapp.R.id.host_Search)).check(matches(isDisplayed()))

        // Hide the toolbox and check visibility of buttons
        scenario.onFragment { it.isToolboxVisible = false }
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(androidx.pdf.testapp.R.id.host_Search)).check(matches(isDisplayed()))

        // Enter immersive mode and check visibility of buttons
        scenario.onFragment { it.onRequestImmersiveMode(true) }
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(androidx.pdf.testapp.R.id.host_Search))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // Exit immersive mode and check visibility of buttons
        scenario.onFragment { it.onRequestImmersiveMode(false) }
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed()))
        onView(withId(androidx.pdf.testapp.R.id.host_Search)).check(matches(isDisplayed()))

        // Click the host app search button and check visibility of elements
        onView(withId(androidx.pdf.testapp.R.id.host_Search)).perform(click())
        onView(withId(R.id.search_container)).check(matches(isDisplayed()))
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(androidx.pdf.testapp.R.id.host_Search))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun testPdfViewerFragment_onLoadDocumentError_corruptPdf() {
        val scenario =
            scenarioLoadDocument(
                TEST_CORRUPTED_DOCUMENT_FILE,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )

        onView(withId(R.id.errorTextView)).check(matches(isDisplayed()))
        scenario.onFragment { fragment ->
            Preconditions.checkArgument(
                fragment.documentError is RuntimeException,
                "Exception is of incorrect type"
            )
            Preconditions.checkArgument(
                fragment.documentError
                    ?.message
                    .equals(fragment.resources.getString(R.string.pdf_error)),
                "Incorrect exception returned ${fragment.documentError?.message}"
            )
        }
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val TEST_PROTECTED_DOCUMENT_FILE = "sample-protected.pdf"
        private const val TEST_CORRUPTED_DOCUMENT_FILE = "corrupted.pdf"
        private const val PROTECTED_DOCUMENT_PASSWORD = "abcd1234"
        private const val SEARCH_QUERY = "ipsum"
        private const val KEYBOARD_CONTENT_DESC = "keyboard"
    }
}
