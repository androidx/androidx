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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.actions.SelectionViewActions
import androidx.pdf.testapp.ui.BasicPdfFragment
import androidx.pdf.testapp.ui.scenarios.TabsViewPdfFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class TabsViewPdfFragmentTestSuite {
    private lateinit var scenario: FragmentScenario<TabsViewPdfFragment>

    @Before
    fun setup() {
        // Launch the fragment in a container with the specified theme and initial state.
        scenario =
            launchFragmentInContainer<TabsViewPdfFragment>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED,
            )
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    private fun scenarioLoadDocument(
        filename: String,
        nextState: Lifecycle.State,
        orientation: Int,
    ): FragmentScenario<TabsViewPdfFragment> {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        scenario.moveToState(nextState)
        scenario.onFragment { it.requireActivity().requestedOrientation = orientation }

        // Load the document into the fragment.
        scenario.onFragment { fragment ->
            val uri = TestUtils.saveStream(inputStream, fragment.requireContext())
            val viewPager =
                fragment.view?.findViewById<ViewPager2>(androidx.pdf.testapp.R.id.viewpager)
            val myFragment =
                fragment.childFragmentManager.findFragmentByTag("f" + viewPager?.currentItem)

            (myFragment as BasicPdfFragment).setDocumentUri(uri)
        }

        return scenario
    }

    @Test
    fun testTabsViewPdfFragment_openPdf() {
        scenarioLoadDocument(
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        // Perform swipe actions on the PDF container.
        onView(withId(R.id.parent_pdf_container)).perform(swipeUp())
        onView(withId(R.id.parent_pdf_container)).perform(swipeDown())

        // Perform selection actions on the PDF container.
        val selectionViewActions = SelectionViewActions()
        onView(isRoot()).perform(longClick())
        onView(withId(R.id.start_drag_handle)).check(matches(isDisplayed()))
        onView(withId(R.id.stop_drag_handle)).check(matches(isDisplayed()))

        onView(withId(R.id.parent_pdf_container))
            .perform(selectionViewActions.longClickAndDragRight())
        onView(withId(R.id.parent_pdf_container)).check(selectionViewActions.stopHandleMoved())
    }

    @Test
    fun testTabsViewPdfFragment_independentZoomAndScrollAcrossTabs() {
        // Load the document in the first tab.
        scenarioLoadDocument(
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        // Zoom and scroll in the first tab.
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(swipeUp())
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(doubleClick())
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(swipeLeft())

        // Capture the bitmap of the first tab's current state.
        val firstTabInitialBitmap: Bitmap? = getCurrentBitmap()

        // Navigate to the third tab.
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(swipeLeft())
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(swipeLeft())

        // Load the same document in the third tab.
        scenarioLoadDocument(
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        // Capture the bitmap of the third tab's initial state.
        val thirdTabInitialBitmap: Bitmap? = getCurrentBitmap()

        // Return to the first tab.
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(swipeRight())
        onView(withId(androidx.pdf.testapp.R.id.viewpager)).perform(swipeRight())

        // Capture the bitmap of the first tab after returning.
        val firstTabFinalBitmap: Bitmap? = getCurrentBitmap()

        // Assert that the first tab's state is persisted after switching tabs.
        assertTrue(firstTabInitialBitmap?.sameAs(firstTabFinalBitmap) == true)

        // Assert that the third tab has a different state from the first tab for same pdf.
        assertTrue(firstTabInitialBitmap?.sameAs(thirdTabInitialBitmap) == false)
    }

    // Captures the bitmap of the currently displayed fragment's view.
    private fun getCurrentBitmap(): Bitmap? {
        var bitmap: Bitmap? = null
        scenario.onFragment { fragment ->
            val viewPager =
                fragment.view?.findViewById<ViewPager2>(androidx.pdf.testapp.R.id.viewpager)
            val currentFragment =
                fragment.childFragmentManager.findFragmentByTag("f" + viewPager?.currentItem)
            val view = currentFragment?.view

            if (view != null) {
                // Create a bitmap with the same dimensions as the view.
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap!!)
                // Draw the view's content onto the canvas.
                view.draw(canvas)
            }
        }
        return bitmap
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
    }
}
