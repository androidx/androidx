/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.ComponentCallbacks2
import android.graphics.Point
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewTrimMemoryTest {

    @Before
    fun setup() {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                val pdfView = PdfView(activity).apply { id = PDF_VIEW_ID }
                container.addView(pdfView, ViewGroup.LayoutParams(500, 1000))
            }
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun trimBackground_releasesBitmaps_andFetchesThemAgainWhenShown() = runTest {
        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).pdfDocument = fakePdfDocument
            }
            fakePdfDocument.waitForRender(untilPage = 4)
            assertThat(fakePdfDocument.bitmapRequests.size).isEqualTo(5)

            hideUiAndTrim(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            fakePdfDocument.clearBitmapRequests()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(fakePdfDocument.bitmapRequests).isEmpty()

            moveToState(Lifecycle.State.RESUMED)

            // Bitmaps were released, so they are fetched again for the visible pages
            fakePdfDocument.waitForRender(untilPage = 4)
            assertThat(fakePdfDocument.bitmapRequests.size).isEqualTo(5)
            close()
        }
    }

    @Test
    fun trimUiHidden_retainsBitmaps() = runTest {
        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).pdfDocument = fakePdfDocument
            }
            fakePdfDocument.waitForRender(untilPage = 4)
            assertThat(fakePdfDocument.bitmapRequests.size).isEqualTo(5)

            hideUiAndTrim(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            fakePdfDocument.clearBitmapRequests()
            moveToState(Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // Bitmaps were retained, so none should be requested again
            assertThat(fakePdfDocument.bitmapRequests).isEmpty()
            close()
        }
    }

    /** Stops the activity, hiding its window, and dispatches an app-wide trim signal. */
    private fun ActivityScenario<PdfViewTestActivity>.hideUiAndTrim(level: Int) {
        moveToState(Lifecycle.State.CREATED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onActivity { activity -> activity.application.onTrimMemory(level) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private companion object {
        const val PDF_VIEW_ID = 123456789
    }
}
