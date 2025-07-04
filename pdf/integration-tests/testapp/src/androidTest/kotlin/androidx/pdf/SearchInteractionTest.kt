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

package androidx.pdf

import android.content.pm.ActivityInfo
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowInsets
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.TestUtils.waitFor
import androidx.pdf.view.PdfView
import androidx.pdf.view.search.PdfSearchView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
internal class SearchInteractionTest {

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragment>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED,
            )

        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .register(fragment.pdfSearchFocusIdlingResource.countingIdlingResource)

            scenarioLoadDocument(
                scenario = scenario,
                nextState = Lifecycle.State.STARTED,
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            )
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfSearchFocusIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    @Test
    fun test_searchClosed_upon_textSelection() {
        onView(withId(androidx.pdf.viewer.fragment.R.id.pdfView)).check(matches(isDisplayed()))

        var pdfView: PdfView? = null
        scenario.onFragment { fragment ->
            pdfView =
                fragment.view?.findViewById<PdfView>(androidx.pdf.viewer.fragment.R.id.pdfView)
            fragment.isTextSearchActive = true
        }

        onView(withId(R.id.searchQueryBox)).perform(typeText(SEARCH_QUERY))
        onView(isRoot()).perform(waitFor(50))
        onView(withId(R.id.matchStatusTextView)).check(matches(isDisplayed()))
        val expectedText = pdfView?.context?.getString(R.string.message_match_status, 1, 24)
        onView(withId(R.id.matchStatusTextView)).check(matches(withText(expectedText)))

        // Start selection on PdfView
        longClickAtCenter()
        assertNotNull(pdfView?.currentSelection)

        // assert search is not displayed
        onView(withId(androidx.pdf.viewer.fragment.R.id.pdfSearchView))
            .check(matches(not(isDisplayed())))
        scenario.onFragment { fragment -> assertFalse(fragment.isTextSearchActive) }
    }

    @Test
    fun test_selection_cleared_upon_search() {
        onView(withId(androidx.pdf.viewer.fragment.R.id.pdfView)).check(matches(isDisplayed()))

        var pdfView: PdfView? = null
        scenario.onFragment { fragment ->
            pdfView =
                fragment.view?.findViewById<PdfView>(androidx.pdf.viewer.fragment.R.id.pdfView)
        }

        // Start selection on PdfView
        longClickAtCenter()
        assertNotNull(pdfView?.currentSelection)

        // Enable search on document
        scenario.onFragment { fragment -> fragment.isTextSearchActive = true }

        // assert selection cleared on pdfView
        assertNull(pdfView?.currentSelection)

        // Check if search is functional
        onView(withId(R.id.searchQueryBox)).perform(typeText(SEARCH_QUERY))
        onView(isRoot()).perform(waitFor(50))
        onView(withId(R.id.matchStatusTextView)).check(matches(isDisplayed()))
        val expectedText = pdfView?.context?.getString(R.string.message_match_status, 1, 24)
        onView(withId(R.id.matchStatusTextView)).check(matches(withText(expectedText)))
    }

    @Test
    fun test_pdfViewerFragment_searchFocusCleared_onSingleTap() {
        onView(withId(androidx.pdf.viewer.fragment.R.id.pdfView)).check(matches(isDisplayed()))
        var pdfSearchView: PdfSearchView? = null

        scenario.onFragment { fragment ->
            pdfSearchView =
                fragment.view?.findViewById<PdfSearchView>(
                    androidx.pdf.viewer.fragment.R.id.pdfSearchView
                )
            fragment.isTextSearchActive = true
        }

        pdfSearchView?.let {
            // assert search view is focused when user starts searching
            assertTrue(it.hasFocus())

            // Single tap on PdfView(anywhere on the content)
            onView(isRoot()).perform(click())

            scenario.onFragment { it.pdfSearchFocusIdlingResource.increment() }
            // Espresso will wait on the idling resource on the next action performed hence adding a
            // click which is essentially a no-op
            onView(isRoot()).perform(click())

            // search focus on search is cleared
            assertFalse(it.hasFocus())
            // assert soft input mode is also hidden
            assertFalse(it.rootWindowInsets.isVisible(WindowInsets.Type.ime()))
        }
    }

    @Test
    fun test_pdfViewerFragment_searchFocused_onResume() {
        onView(withId(androidx.pdf.viewer.fragment.R.id.pdfView)).check(matches(isDisplayed()))
        var pdfSearchView: PdfSearchView? = null

        scenario.onFragment { fragment ->
            pdfSearchView =
                fragment.view?.findViewById<PdfSearchView>(
                    androidx.pdf.viewer.fragment.R.id.pdfSearchView
                )
            fragment.isTextSearchActive = true
        }

        pdfSearchView?.let {
            // Assert that the search view is focused when the user initiates a search.
            assertTrue(it.hasFocus())

            // Single tap on PdfView(anywhere on the content)
            onView(isRoot()).perform(click())

            scenario.onFragment { it.pdfSearchFocusIdlingResource.increment() }
            // Espresso will wait on the idling resource on the next action performed hence adding a
            // click which is essentially a no-op
            onView(isRoot()).perform(click())

            // search focus on search is cleared
            assertFalse(it.hasFocus())
            // assert soft input mode is also hidden
            assertFalse(it.rootWindowInsets.isVisible(WindowInsets.Type.ime()))

            // Simulate the user switching away and then returning back.
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            // Assert that the search view gains focus when the user returns to the PDF view.
            assertTrue(it.hasFocus())
        }
    }

    private fun longClickAtCenter() {
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
                    MotionEvent.BUTTON_PRIMARY,
                )
            )
    }

    companion object {
        private const val SEARCH_QUERY = "ipsum"
    }
}
