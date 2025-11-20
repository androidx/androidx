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
import android.os.StrictMode
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class StrictModeTestSuite {

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>
    private lateinit var originalPolicy: StrictMode.VmPolicy

    @Before
    fun setup() {
        // Launch the fragment in a container with the specified theme and initial state.
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
        }

        originalPolicy = StrictMode.getVmPolicy()
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build()
        )
    }

    @After
    fun cleanup() {
        StrictMode.setVmPolicy(originalPolicy)

        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    @Test
    fun test_loadsTwoValidPdfs_shouldNotTriggerStrictModeViolations() {
        scenarioLoadDocument(
            scenario,
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        onView(isRoot()).perform(click())

        scenarioLoadDocument(
            scenario,
            TEST_LINK_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )
        onView(isRoot()).perform(click())

        System.gc()
        System.runFinalization()
    }

    @Test
    fun test_loadsValidPdfThenCorruptedPdf_shouldNotTriggerStrictModeViolations() {
        scenarioLoadDocument(
            scenario,
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )
        onView(isRoot()).perform(click())

        scenarioLoadDocument(
            scenario,
            TEST_CORRUPTED_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )
        onView(isRoot()).perform(click())

        System.gc()
        System.runFinalization()
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val TEST_LINK_DOCUMENT_FILE = "sample_links.pdf"
        private const val TEST_CORRUPTED_DOCUMENT_FILE = "corrupted.pdf"
    }
}
