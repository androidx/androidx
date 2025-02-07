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
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.TestUtils.waitFor
import androidx.pdf.view.PdfView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
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

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragmentV2>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragmentV2>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED
            )

        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)

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
        // TODO: Spacing between current page and total pages is locale specific. Needs to be
        //  uniform
        // onView(withId(R.id.matchStatusTextView)).check(matches(withText("1 / 24")))

        // Start selection on PdfView
        onView(isRoot()).perform(longClick())
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
        onView(isRoot()).perform(longClick())
        assertNotNull(pdfView?.currentSelection)

        // Enable search on document
        scenario.onFragment { fragment -> fragment.isTextSearchActive = true }

        // assert selection cleared on pdfView
        assertNull(pdfView?.currentSelection)

        // Check if search is functional
        onView(withId(R.id.searchQueryBox)).perform(typeText(SEARCH_QUERY))
        onView(isRoot()).perform(waitFor(50))
        onView(withId(R.id.matchStatusTextView)).check(matches(isDisplayed()))
        // TODO: Spacing between current page and total pages is locale specific. Needs to be
        //  uniform
        // onView(withId(R.id.matchStatusTextView)).check(matches(withText("1 / 24")))
    }

    companion object {
        private const val SEARCH_QUERY = "ipsum"
    }
}
