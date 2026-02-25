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

package androidx.pdf.annotation

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.DeadObjectException
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.pdf.annotation.highlights.InProgressHighlightsView
import androidx.pdf.annotation.highlights.models.InProgressHighlightId
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.view.FakePdfDocument
import androidx.pdf.view.PdfViewTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class InProgressHighlightViewTest {

    private val highlightIdlingResource = CountingIdlingResource(HIGHLIGHT_RESOURCE_NAME)
    private lateinit var highlightView: InProgressHighlightsView
    private lateinit var testHighlightListener: FakeInProgressTextHighlightsListener
    private lateinit var testId: InProgressHighlightId

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(highlightIdlingResource)
        testHighlightListener = FakeInProgressTextHighlightsListener(highlightIdlingResource)
        testId = InProgressHighlightId.create()

        setupActivity()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(highlightIdlingResource)
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun startTextHighlight_onText_invokesSuccessCallback() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()

            scenario.onActivity {
                val point = PointF(50f, 50f)
                highlightView.startTextHighlight(
                    id = testId,
                    pageNum = 0,
                    startPdfPoint = point,
                    startViewPoint = point,
                    pageToViewTransform = Matrix(),
                )
            }

            Espresso.onIdle()

            assertThat(testHighlightListener.isStarted).isTrue()
            assertThat(testHighlightListener.startedId).isEqualTo(testId)
        }
    }

    @Test
    fun startTextHighlight_noText_invokesFailureCallback() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()

            scenario.onActivity {
                // (5,5) is outside text bounds (10,10 - 100,100)
                val point = PointF(5f, 5f)
                highlightView.startTextHighlight(
                    id = testId,
                    pageNum = 0,
                    startPdfPoint = point,
                    startViewPoint = point,
                    pageToViewTransform = Matrix(),
                )
            }

            Espresso.onIdle()

            assertThat(testHighlightListener.isFailed).isTrue()
        }
    }

    @Test
    fun finishTextHighlight_validSelection_createsStampAnnotation() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()

            // 1. Start the highlight
            scenario.onActivity {
                val point = PointF(50f, 50f)
                highlightView.startTextHighlight(
                    id = testId,
                    pageNum = 0,
                    startPdfPoint = point,
                    startViewPoint = point,
                    pageToViewTransform = Matrix(),
                )
            }
            Espresso.onIdle()
            assertThat(testHighlightListener.isStarted).isTrue()

            // 2. Add to the highlight (simulate dragging)
            scenario.onActivity { highlightView.addToTextHighlight(testId, PointF(70f, 70f)) }
            Espresso.onIdle()

            // 3. Finish the highlight
            highlightIdlingResource.increment()
            scenario.onActivity { highlightView.finishTextHighlight(testId, PointF(90f, 90f)) }
            Espresso.onIdle()

            // 4. Verify
            val createdAnnotation = testHighlightListener.finishedAnnotations[testId]
            assertThat(createdAnnotation).isInstanceOf(StampAnnotation::class.java)

            with(createdAnnotation as StampAnnotation) {
                assertThat(pageNum).isEqualTo(0)
                // FakePdfDocument returns a single rect covering start to end
                assertThat(bounds).isEqualTo(RectF(50f, 50f, 90f, 90f))
            }
        }
    }

    @Test
    fun cancelHighlight_afterStart_producesNoAnnotationOnFinish() {
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()

            scenario.onActivity {
                val point = PointF(50f, 50f)
                highlightView.startTextHighlight(
                    id = testId,
                    pageNum = 0,
                    startPdfPoint = point,
                    startViewPoint = point,
                    pageToViewTransform = Matrix(),
                )
            }
            Espresso.onIdle()
            assertThat(testHighlightListener.isStarted).isTrue()

            scenario.onActivity {
                highlightView.cancelTextHighlight(testId)
                highlightView.finishTextHighlight(testId, PointF(80f, 80f))
            }

            assertThat(testHighlightListener.finishedAnnotations[testId]).isNull()
        }
    }

    @Test
    fun textHighlight_withError_invokesErrorCallback() {
        val exception = DeadObjectException()
        val pageText =
            PdfPageTextContent(bounds = listOf(RectF(10f, 10f, 100f, 100f)), text = "Sample Text")
        val fakePdfDocument =
            FakePdfDocument(
                pages = listOf(Point(500, 500)),
                textContents = listOf(pageText),
                exceptionToThrow = exception,
            )

        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()
            scenario.onActivity {
                highlightView.pdfDocument = fakePdfDocument
                val point = PointF(50f, 50f)
                highlightView.startTextHighlight(
                    id = testId,
                    pageNum = 0,
                    startPdfPoint = point,
                    startViewPoint = point,
                    pageToViewTransform = Matrix(),
                )
            }

            Espresso.onIdle()
            assertThat(testHighlightListener.isStarted).isFalse()
            assertThat(testHighlightListener.isFailed).isFalse()
            assertThat(testHighlightListener.exceptionThrown).isNotNull()
            assertThat(testHighlightListener.exceptionThrown?.throwable).isEqualTo(exception)
        }
    }

    @Test
    fun touchEvents_dragAndUp_createsAnnotation() {
        setupActivity()
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()

            scenario.onActivity {
                val downEvent = obtainMotionEvent(50f, 50f, MotionEvent.ACTION_DOWN)
                highlightView.onTouchEvent(downEvent)
                downEvent.recycle()
            }
            Espresso.onIdle()

            scenario.onActivity {
                val moveEvent = obtainMotionEvent(70f, 70f, MotionEvent.ACTION_MOVE)
                highlightView.onTouchEvent(moveEvent)
                moveEvent.recycle()
            }

            highlightIdlingResource.increment()
            scenario.onActivity {
                val upEvent = obtainMotionEvent(90f, 90f, MotionEvent.ACTION_UP)
                highlightView.onTouchEvent(upEvent)
                upEvent.recycle()
            }

            Espresso.onIdle()

            assertThat(testHighlightListener.isStarted).isTrue()
            val createdAnnotation =
                testHighlightListener.finishedAnnotations[testHighlightListener.startedId]
            assertThat(createdAnnotation).isNotNull()
            assertThat(createdAnnotation).isInstanceOf(StampAnnotation::class.java)
            with(createdAnnotation as StampAnnotation) {
                assertThat(pageNum).isEqualTo(0)
                assertThat(bounds).isEqualTo(RectF(50f, 50f, 90f, 90f))
            }
        }
    }

    @Test
    fun touchEvents_cancelEvent_abortsHighlight() {
        setupActivity()
        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            highlightIdlingResource.increment()

            scenario.onActivity {
                val downEvent = obtainMotionEvent(50f, 50f, MotionEvent.ACTION_DOWN)
                highlightView.onTouchEvent(downEvent)
                downEvent.recycle()
            }
            Espresso.onIdle()

            assertThat(testHighlightListener.isStarted).isTrue()

            scenario.onActivity {
                val moveEvent = obtainMotionEvent(70f, 70f, MotionEvent.ACTION_MOVE)
                highlightView.onTouchEvent(moveEvent)
                moveEvent.recycle()

                val cancelEvent = obtainMotionEvent(90f, 90f, MotionEvent.ACTION_CANCEL)
                highlightView.onTouchEvent(cancelEvent)
                cancelEvent.recycle()
            }

            Espresso.onIdle()

            assertThat(testHighlightListener.finishedAnnotations).isEmpty()
        }
    }

    @Test
    fun touchEvents_upOutsidePage_usesLastValidPdfPointToFinalize() {
        val pageBounds = RectF(0f, 0f, 150f, 150f)
        setupActivity(pageInfoProvider = FakePageInfoProvider(pageBounds))

        ActivityScenario.launch(PdfViewTestActivity::class.java).use { scenario ->
            val startViewPoint = PointF(50f, 50f)
            val moveViewPoint = PointF(70f, 70f)
            val endViewPoint = PointF(200f, 200f)

            highlightIdlingResource.increment()
            scenario.onActivity {
                val downEvent =
                    obtainMotionEvent(startViewPoint.x, startViewPoint.y, MotionEvent.ACTION_DOWN)
                highlightView.onTouchEvent(downEvent)
                downEvent.recycle()
            }
            Espresso.onIdle()
            assertThat(testHighlightListener.isStarted).isTrue()

            scenario.onActivity {
                val moveEvent =
                    obtainMotionEvent(moveViewPoint.x, moveViewPoint.y, MotionEvent.ACTION_MOVE)
                highlightView.onTouchEvent(moveEvent)
                moveEvent.recycle()
            }

            highlightIdlingResource.increment()
            scenario.onActivity {
                val upEvent =
                    obtainMotionEvent(endViewPoint.x, endViewPoint.y, MotionEvent.ACTION_UP)
                highlightView.onTouchEvent(upEvent)
                upEvent.recycle()
            }
            Espresso.onIdle()

            // Verify the highlight was finished using the last valid point
            val createdAnnotation =
                testHighlightListener.finishedAnnotations[testHighlightListener.startedId]
            assertThat(createdAnnotation).isNotNull()
            assertThat(createdAnnotation).isInstanceOf(StampAnnotation::class.java)

            with(createdAnnotation as StampAnnotation) {
                assertThat(pageNum).isEqualTo(0)
                assertThat(bounds)
                    .isEqualTo(
                        RectF(startViewPoint.x, startViewPoint.y, moveViewPoint.x, moveViewPoint.y)
                    )
            }
        }
    }

    private fun setupActivity(pageInfoProvider: PageInfoProvider = FakePageInfoProvider()) {
        val pageText =
            PdfPageTextContent(bounds = listOf(RectF(10f, 10f, 100f, 100f)), text = "Sample Text")
        val fakePdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = listOf(pageText))

        PdfViewTestActivity.onCreateCallback = { activity ->
            highlightView =
                InProgressHighlightsView(activity).apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    pdfDocument = fakePdfDocument
                    addInProgressTextHighlightsListener(testHighlightListener)
                    this.pageInfoProvider = pageInfoProvider
                }
            activity.container.addView(highlightView)
        }
    }

    private fun obtainMotionEvent(x: Float, y: Float, action: Int): MotionEvent {
        val now = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, action, x, y, 0)
    }

    companion object {
        private val HIGHLIGHT_RESOURCE_NAME = "TextHighlight-${UUID.randomUUID()}"
    }
}
