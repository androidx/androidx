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

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.testapp.R
import androidx.pdf.util.Preconditions
import androidx.pdf.viewer.fragment.PdfStylingOptions
import androidx.pdf.viewer.fragment.R as PdfR
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlin.math.round
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class StylingOptionsTests {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>
    private val themeResId =
        com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar

    @Before
    fun setup() {
        val pdfStylingOptions = PdfStylingOptions(R.style.PdfViewCustomization)
        val styledFragment = TestPdfViewerFragment(pdfStylingOptions)

        scenario =
            launchFragmentInContainer(
                fragmentArgs = styledFragment.arguments,
                themeResId = themeResId,
                initialState = Lifecycle.State.INITIALIZED,
            ) {
                styledFragment
            }

        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanUp() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    @Test
    fun pdfViewerFragment_withCustomStyle_rendersFastScrollerWithCorrectDimensionsAndMargins() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Assert loading view is visible during load
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        swipeAndAssertFastScrollerStyle()

        // change orientation to landscape
        scenario.onFragment {
            it.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        swipeAndAssertFastScrollerStyle()
    }

    private fun swipeAndAssertFastScrollerStyle() {
        // Swipe actions
        onView(withId(PdfR.id.pdfView)).perform(swipeUp())
        onView(withId(PdfR.id.pdfView)).perform(swipeDown())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }

        // Espresso will wait on the idling resource on the next action performed hence adding a
        // click which is essentially a no-op
        onView(withId(PdfR.id.pdfView)).perform(click())

        scenario.onFragment { fragment ->
            val fastScrollDrawer = fragment.getPdfViewInstance().fastScroller?.fastScrollDrawer

            // assert size of view is equivalent to what specified for drawable
            assertNotNull(fastScrollDrawer)

            assertEquals(
                round(THUMB_DRAWABLE_HEIGHT.dpToPx(context)).toInt(),
                fastScrollDrawer?.thumbDrawable?.intrinsicHeight,
            )
            assertEquals(
                round(THUMB_DRAWABLE_WIDTH.dpToPx(context)).toInt(),
                fastScrollDrawer?.thumbDrawable?.intrinsicWidth,
            )
            assertEquals(
                round(THUMB_END_MARGIN.dpToPx(context)).toInt(),
                fastScrollDrawer?.thumbMarginEnd,
            )

            assertEquals(
                round(PAGE_INDICATOR_DRAWABLE_HEIGHT.dpToPx(context)).toInt(),
                fastScrollDrawer?.pageIndicatorBackground?.intrinsicHeight,
            )
            assertEquals(
                round(PAGE_INDICATOR_END_MARGIN.dpToPx(context)).toInt(),
                fastScrollDrawer?.pageIndicatorMarginEnd,
            )
        }
    }

    private fun Int.dpToPx(context: Context): Float =
        (this * context.resources.displayMetrics.density)

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val THUMB_DRAWABLE_WIDTH = 6
        private const val THUMB_DRAWABLE_HEIGHT = 64
        private const val THUMB_END_MARGIN = 8
        private const val PAGE_INDICATOR_DRAWABLE_HEIGHT = 24
        private const val PAGE_INDICATOR_END_MARGIN = 24
    }
}
