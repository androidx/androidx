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

package androidx.pdf.search

import android.content.Context
import android.content.Intent
import android.view.ViewGroup.LayoutParams
import androidx.pdf.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.pdf.SEARCH_VIEW_IN_LTR_MODE
import androidx.pdf.SEARCH_VIEW_IN_RTL_MODE
import androidx.pdf.assertScreenshot
import androidx.pdf.view.PdfViewTestActivity
import androidx.pdf.view.PdfViewTestActivity.Companion.LOCALE_COUNTRY
import androidx.pdf.view.PdfViewTestActivity.Companion.LOCALE_LANGUAGE
import androidx.pdf.view.search.PdfSearchView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class PdfSearchViewScubaTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testPdfSearchViewInLTRMode() {
        setupPdfSearchView()
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            assertScreenshot(PDF_SEARCH_VIEW_ID, screenshotRule, SEARCH_VIEW_IN_LTR_MODE)

            close()
        }
    }

    @Test
    fun testPdfSearchViewInRTLMode() {
        setupPdfSearchView()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent =
            Intent(context, PdfViewTestActivity::class.java).apply {
                // Arabic is an RTL language
                putExtra(LOCALE_LANGUAGE, "ar")
                putExtra(LOCALE_COUNTRY, "SA")
            }

        with(ActivityScenario.launch<PdfViewTestActivity>(intent)) {
            assertScreenshot(PDF_SEARCH_VIEW_ID, screenshotRule, SEARCH_VIEW_IN_RTL_MODE)

            close()
        }
    }

    private fun setupPdfSearchView() {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfSearchView(activity).apply { id = PDF_SEARCH_VIEW_ID },
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
                )
            }
        }
    }

    companion object {
        /** Arbitrary fixed ID for PdfSearchView */
        private const val PDF_SEARCH_VIEW_ID = 987654321
    }
}
