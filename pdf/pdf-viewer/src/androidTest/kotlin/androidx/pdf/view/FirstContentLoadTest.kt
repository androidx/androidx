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

import android.graphics.Point
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class FirstContentLoadTest {

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

    @Test
    fun testPdfView_firstContentLoadEvent_bitmapAvailable() {

        var callbackCount = 0
        pdfView.addOnFirstContentLoadListener { callbackCount++ }

        pdfView.isAnyBitmapAvailable = true
        pdfView.notifyFirstContentLoad = true
        assertEquals(0, callbackCount)

        // force invalidation to trigger onDraw
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // check event fired once and shouldNotify set to false
        assertEquals(1, callbackCount)
        assertFalse(pdfView.notifyFirstContentLoad)
        assertTrue(pdfView.isAnyBitmapAvailable)

        // recheck event should not fire as shouldNotifyContentLoad is false
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(1, callbackCount)
        assertFalse(pdfView.notifyFirstContentLoad)
        assertTrue(pdfView.isAnyBitmapAvailable)

        // check event should fire on reset
        pdfView.notifyFirstContentLoad = true
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(2, callbackCount)
        assertFalse(pdfView.notifyFirstContentLoad)
        assertTrue(pdfView.isAnyBitmapAvailable)
    }

    @Test
    fun testPdfView_firstContentLoadEvent_bitmapNotAvailable() {

        var callbackCount = 0
        pdfView.addOnFirstContentLoadListener { callbackCount++ }

        // set bitmap is not available
        pdfView.isAnyBitmapAvailable = false
        pdfView.notifyFirstContentLoad = true
        assertEquals(0, callbackCount)

        // force invalidation to trigger onDraw
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // check event should not fire if bitmap is not available
        assertEquals(0, callbackCount)
        assertTrue(pdfView.notifyFirstContentLoad)

        // check event should not fire if we change shouldNotifyContentLoad
        pdfView.notifyFirstContentLoad = false
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertFalse(pdfView.notifyFirstContentLoad)
        assertFalse(pdfView.isAnyBitmapAvailable)
        assertEquals(0, callbackCount)
    }

    @Test
    fun testPdfView_firstContentLoadEvent_removeListener() {
        var callbackCount = 0
        val listener = PdfView.OnFirstContentLoadListener { callbackCount++ }

        // add the listener
        pdfView.addOnFirstContentLoadListener(listener)

        // reset the listener configuration
        pdfView.isAnyBitmapAvailable = true
        pdfView.notifyFirstContentLoad = true

        // verify the listener is working
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(1, callbackCount)

        // remove the listener
        pdfView.removeOnFirstContentLoadListener(listener)

        // reset the listener configuration
        pdfView.isAnyBitmapAvailable = true
        pdfView.notifyFirstContentLoad = true

        // verify the listener was actually removed and won't be called
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals(1, callbackCount)
    }

    @Test
    fun testPdfView_firstContentLoadEvent_AddMultipleListenerAndRemoveOneListener() {

        var firstListenerCount = 0
        var secondListenerCount = 0
        val firstListener = PdfView.OnFirstContentLoadListener { firstListenerCount += 1 }
        val secondListener = PdfView.OnFirstContentLoadListener { secondListenerCount += 1 }

        // add multiple onFirstContent load listeners to pdfView
        pdfView.addOnFirstContentLoadListener(firstListener)
        pdfView.addOnFirstContentLoadListener(secondListener)

        // reset bitmap is not available
        pdfView.isAnyBitmapAvailable = true
        pdfView.notifyFirstContentLoad = true

        // force invalidation to trigger onDraw
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // check if both listeners are called
        assertEquals(1, firstListenerCount)
        assertEquals(1, secondListenerCount)
        assertEquals(true, pdfView.isAnyBitmapAvailable)
        assertEquals(false, pdfView.notifyFirstContentLoad)

        // reset firstContentLoad event configuration
        pdfView.isAnyBitmapAvailable = true
        pdfView.notifyFirstContentLoad = true

        // remove one listener
        pdfView.removeOnFirstContentLoadListener(secondListener)

        // force invalidation to trigger onDraw
        pdfView.invalidate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // check only one listener should be called
        assertEquals(2, firstListenerCount)
        assertEquals(1, secondListenerCount)
        assertEquals(true, pdfView.isAnyBitmapAvailable)
        assertEquals(false, pdfView.notifyFirstContentLoad)
    }

    companion object {
        const val PDF_VIEW_ID = 123456789
    }
}
