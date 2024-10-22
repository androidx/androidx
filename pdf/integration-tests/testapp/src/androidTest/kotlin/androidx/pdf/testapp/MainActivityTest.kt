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

package androidx.pdf.testapp

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.pdf.testapp.ui.BasicPdfFragment
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.material.button.MaterialButton
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@Suppress("UNCHECKED_CAST")
@SmallTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testActivityInitialization() {
        launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)

            Assert.assertEquals(Lifecycle.State.CREATED, scenario.state)
        }
    }

    @Test
    fun testOpenPdfButtonClickLaunchesFilePicker() {
        val mockFilePicker: ActivityResultLauncher<String> =
            mock(ActivityResultLauncher::class.java) as ActivityResultLauncher<String>

        launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                // Step 1: Click the button that launches the Fragment
                val singlePdfButton: MaterialButton = activity.findViewById(R.id.single_pdf)
                Assert.assertNotNull(singlePdfButton)
                singlePdfButton.performClick()

                // Step 2: Find the Fragment that was launched
                val fragmentManager = activity.supportFragmentManager
                val fragment = fragmentManager.findFragmentByTag("pdf_interaction_fragment_tag")
                Assert.assertNotNull(fragment)

                // Step 3: Find Single Pdf Fragment
                val singlePdfFragmentManager = fragment?.childFragmentManager
                val singlePdfFragment =
                    singlePdfFragmentManager?.findFragmentByTag("single_pdf_fragment_tag")
                Assert.assertNotNull(singlePdfFragment)

                // Step 3: Inject the mock into the Fragment
                (singlePdfFragment as BasicPdfFragment).filePicker = mockFilePicker

                // Step 4: Find OpenPdf Button and performClick
                val openPdfButton: MaterialButton? =
                    singlePdfFragment.view?.findViewById(R.id.open_pdf)
                Assert.assertNotNull(openPdfButton)
                openPdfButton?.performClick()

                verify(mockFilePicker).launch("application/pdf")
            }
        }
    }
}
