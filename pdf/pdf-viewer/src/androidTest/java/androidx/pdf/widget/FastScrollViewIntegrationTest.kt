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

package androidx.pdf.widget

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.pdf.TestActivity
import androidx.pdf.models.Dimensions
import androidx.pdf.viewer.PaginatedView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration tests for [FastScrollView] */
@RunWith(AndroidJUnit4::class)
@MediumTest
class FastScrollViewIntegrationTest {
    @get:Rule
    val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private lateinit var fastScrollView: FastScrollView
    private lateinit var zoomView: ZoomView
    private lateinit var paginatedView: PaginatedView

    private val dragHandle: View
        get() = fastScrollView.dragHandle

    private val pageIndicator: TextView
        get() = fastScrollView.pageIndicator

    private fun configureViews() {
        activityScenario.scenario.onActivity { activity ->
            // Build layout from the bottom up
            // Start by adding a PaginatedView with 10 50x50 pages
            paginatedView =
                PaginatedView(activity).apply {
                    model.apply {
                        initialize(10)
                        for (i in 0..9) {
                            addPage(i, Dimensions(50, 50))
                        }
                    }
                }
            // Add a ZoomView to host the PaginatedView
            zoomView =
                ZoomView(activity).apply {
                    layoutParams = ViewGroup.LayoutParams(100, 400)
                    addView(paginatedView)
                }
            // Add a FastScrollView to host the ZoomView
            fastScrollView =
                FastScrollView(activity).apply {
                    layoutParams = ViewGroup.LayoutParams(100, 400)
                    setPaginationModel(paginatedView.model)
                    addView(zoomView)
                }
            activity.setContentView(fastScrollView)
            // FastScrollView expects to be inflated from XML, so simulate this
            fastScrollView.onFinishInflate()
        }
    }

    @Test
    fun pageIndicatorUpdatesOnScroll() {
        configureViews()
        activityScenario.scenario.onActivity {
            // Indicator is hidden
            assertThat(pageIndicator.visibility).isEqualTo(View.GONE)

            // Overscroll the bottom
            zoomView.scrollTo(0, 2000, true)
            assertThat(pageIndicator.visibility).isEqualTo(View.VISIBLE)
            assertThat(pageIndicator.text).isEqualTo("9-10 / 10")

            // Overscroll the top
            zoomView.scrollTo(0, -50, true)
            assertThat(pageIndicator.visibility).isEqualTo(View.VISIBLE)
            assertThat(pageIndicator.text).isEqualTo("1-3 / 10")
        }
    }

    @Test
    fun contentScrollsWithDragHandle() {
        configureViews()
        activityScenario.scenario.onActivity {
            // Setup: reveal drag handle by scrolling up 50 pixels
            zoomView.scrollTo(0, 50, true)
            assertThat(dragHandle.alpha).isEqualTo(1.0f)
            val contentPosInit = zoomView.scrollY

            // Drag the handle to the top
            fastScrollView.onTouchEvent(
                downEvent(dragHandle.x + dragHandle.width / 2, dragHandle.y + dragHandle.height / 2)
            )
            fastScrollView.onTouchEvent(moveEvent(dragHandle.x + dragHandle.height / 2, 0f))

            assertThat(zoomView.scrollY).isLessThan(contentPosInit)
        }
    }

    @Test
    fun dragHandleScrollsWithContent() {
        configureViews()
        activityScenario.scenario.onActivity {
            val handlePosInit = dragHandle.translationY
            // Drag handle is initially hidden
            assertThat(dragHandle.alpha).isEqualTo(0F)

            zoomView.scrollTo(0, 50, true)

            // Drag handle is revealed and has translated downwards
            assertThat(dragHandle.alpha).isEqualTo(1.0f)
            assertThat(dragHandle.translationY).isGreaterThan(handlePosInit)
        }
    }

    private fun motionEvent(
        action: Int,
        x: Float,
        y: Float,
    ) = MotionEvent.obtain(0L, 0L, action, x, y, 0)

    private fun downEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_DOWN, x, y)

    private fun moveEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_MOVE, x, y)
}
