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

package androidx.pdf.view

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Build
import android.os.ext.SdkExtensions
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ImageSelectionTest {

    private lateinit var scenario: ActivityScenario<PdfViewTestActivity>
    private lateinit var pdfView: PdfView

    @Before
    fun setup() {
        val fakePdfDocument = FakePdfDocument(List(1) { Point(100, 200) })

        // No setup required
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                        // Default isAnyBitmapAvailable should be false
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }

        scenario = ActivityScenario.launch(PdfViewTestActivity::class.java)

        scenario.onActivity { activity -> pdfView = activity.findViewById(PDF_VIEW_ID) }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @SuppressLint("NewApi")
    @Test
    fun enablingImageSelection_onTestDeviceWithSdkExt_smallerThan19() = runTest {
        kotlin.test.assertFalse(pdfView.isImageSelectionEnabled)

        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) < 19) {
            pdfView.isImageSelectionEnabled = true
        }

        assertFalse(pdfView.isImageSelectionEnabled)
    }

    @Test
    fun enablingImageSelection_onTestDeviceWithSdkExt_greaterThanOrEqualTo19() = runTest {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 19) {
            kotlin.test.assertFalse(pdfView.isImageSelectionEnabled)
            pdfView.isImageSelectionEnabled = true
            assertTrue(pdfView.isImageSelectionEnabled)
        }
    }

    companion object {
        const val PDF_VIEW_ID = 123456789
    }
}
