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
package androidx.pdf.viewer.fragment

import android.content.pm.ActivityInfo
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.util.Preconditions
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.FragmentUtils.scenarioLoadDocument
import androidx.pdf.viewer.TestPdfViewerFragment
import androidx.pdf.viewer.fragment.R as PdfR
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.EspressoKey
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for external hardware input events on [PdfViewerFragment]. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerFragmentExternalInputTest {
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
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .register(fragment.pdfSearchViewVisibleIdlingResource.countingIdlingResource)
        }
        PdfFeatureFlags.isExternalHardwareInteractionEnabled = true
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfSearchViewVisibleIdlingResource.countingIdlingResource)
        }
        scenario.close()
        PdfFeatureFlags.isExternalHardwareInteractionEnabled = false
    }

    @Test
    fun testCtrlF_opensSearchView() {
        // Load a document into the fragment.
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        // Verify the initial state once the document has finished loading.
        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // Assert that the search view is not visible initially.
        onView(withId(PdfR.id.pdfSearchView))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // Perform a Ctrl+F key press on the PdfView.
        onView(withId(androidx.pdf.R.id.pdfView))
            .check { view, _ ->
                val pdfView = view as PdfView
                pdfView.requestFocus()
            }
            .perform(
                ViewActions.pressKey(
                    EspressoKey.Builder()
                        .withKeyCode(KeyEvent.KEYCODE_F)
                        .withCtrlPressed(true)
                        .build()
                )
            )

        // Assert that the search view is now displayed.
        onView(withId(PdfR.id.pdfSearchView)).check(matches(isDisplayed()))
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
    }
}
