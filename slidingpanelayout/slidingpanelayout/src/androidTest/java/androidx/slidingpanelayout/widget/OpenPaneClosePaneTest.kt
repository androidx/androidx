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

package androidx.slidingpanelayout.widget

import android.view.animation.Interpolator
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.SlidingPaneLayout.Companion.LOCK_MODE_UNLOCKED
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.addWaitForCloseLatch
import androidx.slidingpanelayout.widget.helpers.addWaitForOpenLatch
import androidx.slidingpanelayout.widget.helpers.closePane
import androidx.slidingpanelayout.widget.helpers.openPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [SlidingPaneLayout.openPane] */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OpenPaneClosePaneTest {

    @After
    fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    fun testOpenPaneAndClosePane_doOpenAndClosePane() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelOpenCountDownLatch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            // wait for detail pane open
            assertThat(panelOpenCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()

            val panelCloseCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(closePane())
            // wait for detail pane close
            assertThat(panelCloseCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun testOpenPaneInOpenState_doNothing() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelOpenCountDownLatch1 = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            // wait for detail pane open
            assertThat(panelOpenCountDownLatch1.await(2000, TimeUnit.MILLISECONDS)).isTrue()

            val panelOpenCountDownLatch2 = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            // It won't trigger onPaneOpen again
            assertThat(panelOpenCountDownLatch2.await(2000, TimeUnit.MILLISECONDS)).isFalse()

            onView(withId(R.id.sliding_pane_layout)).perform(openPane(500, testInterpolator))
            // It won't trigger onPaneOpen again
            assertThat(panelOpenCountDownLatch2.await(2000, TimeUnit.MILLISECONDS)).isFalse()
        }
    }

    @Test
    fun testClosePaneInCloseState_doNothing() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelCloseCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(closePane())
            // wait for detail pane open
            assertThat(panelCloseCountDownLatch.await(2000, TimeUnit.MILLISECONDS)).isFalse()

            val panelOpenCountDownLatch2 = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(closePane(500, testInterpolator))
            // wait for detail pane close
            assertThat(panelOpenCountDownLatch2.await(2000, TimeUnit.MILLISECONDS)).isFalse()
        }
    }

    @Test
    fun testOpenPaneAndClosePaneWithInterpolatorAndDuration_doOpenAndClosePane() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelOpenCountDownLatch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane(500, testInterpolator))
            // wait for detail pane open
            assertThat(panelOpenCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            val panelCloseCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(closePane(500, testInterpolator))
            // wait for detail pane close
            assertThat(panelCloseCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun testOpenPaneAndClosePaneWithInterpolatorAndDuration_specifiedDuration() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelOpenCountDownLatch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane(1000, testInterpolator))
            // the pane is not open yet after 500ms.
            assertThat(panelOpenCountDownLatch.await(500, TimeUnit.MILLISECONDS)).isFalse()
            // the pane is open after 1500ms.
            assertThat(panelOpenCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            val panelCloseCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(closePane(1000, testInterpolator))
            // the pane is not closed yet after 500ms.
            assertThat(panelCloseCountDownLatch.await(500, TimeUnit.MILLISECONDS)).isFalse()
            // the pane is closed after 1500ms.
            assertThat(panelCloseCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    private fun testClosePaneInTheMiddleOfOpenPane(
        useDurationForOpen: Boolean,
        useDurationForClose: Boolean
    ) {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelOpenCountDownLatch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            val panelCloseCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            if (useDurationForOpen) {
                onView(withId(R.id.sliding_pane_layout)).perform(openPane(500, testInterpolator))
            } else {
                onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            }
            // the pane is not open yet after 50ms.
            assertThat(panelOpenCountDownLatch.await(50, TimeUnit.MILLISECONDS)).isFalse()

            if (useDurationForClose) {
                onView(withId(R.id.sliding_pane_layout)).perform(closePane(500, testInterpolator))
            } else {
                onView(withId(R.id.sliding_pane_layout)).perform(closePane())
            }
            // The pane is closed eventually.
            assertThat(panelCloseCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun testClosePaneInTheMiddleOfOpenPane() {
        testClosePaneInTheMiddleOfOpenPane(useDurationForOpen = false, useDurationForClose = false)
    }

    @Test
    fun testClosePaneInTheMiddleOfOpenPane_openWithInterpolatorAndDuration() {
        testClosePaneInTheMiddleOfOpenPane(useDurationForOpen = true, useDurationForClose = false)
    }

    @Test
    fun testClosePaneInTheMiddleOfOpenPane_closeWithInterpolatorAndDuration() {
        testClosePaneInTheMiddleOfOpenPane(useDurationForOpen = false, useDurationForClose = true)
    }

    @Test
    fun testClosePaneInTheMiddleOfOpenPane_bothWithInterpolatorAndDuration() {
        testClosePaneInTheMiddleOfOpenPane(useDurationForOpen = true, useDurationForClose = true)
    }

    private fun testOpenPaneInTheMiddleOfClosePane(
        useDurationForOpen: Boolean,
        useDurationForClose: Boolean
    ) {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelOpenCountDownLatch1 = addWaitForOpenLatch(R.id.sliding_pane_layout)
            val panelCloseCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            // Make sure the pane is open first.
            assertThat(panelOpenCountDownLatch1.await(2, TimeUnit.SECONDS)).isTrue()

            val panelOpenCountDownLatch2 = addWaitForOpenLatch(R.id.sliding_pane_layout)
            if (useDurationForClose) {
                onView(withId(R.id.sliding_pane_layout)).perform(closePane(500, testInterpolator))
            } else {
                onView(withId(R.id.sliding_pane_layout)).perform(closePane())
            }
            // the pane is not closed yet after 50ms.
            assertThat(panelCloseCountDownLatch.await(50, TimeUnit.MILLISECONDS)).isFalse()

            // open pane in the middle of close pane.
            if (useDurationForOpen) {
                onView(withId(R.id.sliding_pane_layout)).perform(openPane(500, testInterpolator))
            } else {
                onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            }
            // the pane is open eventually.
            assertThat(panelOpenCountDownLatch2.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    @Test
    fun testOpenPaneInTheMiddleOfClosePane() {
        testOpenPaneInTheMiddleOfClosePane(useDurationForOpen = false, useDurationForClose = false)
    }

    @Test
    fun testOpenPaneInTheMiddleOfClosePane_openWithInterpolatorAndDuration() {
        testOpenPaneInTheMiddleOfClosePane(useDurationForOpen = true, useDurationForClose = false)
    }

    @Test
    fun testOpenPaneInTheMiddleOfClosePane_closeWithInterpolatorAndDuration() {
        testOpenPaneInTheMiddleOfClosePane(useDurationForOpen = false, useDurationForClose = false)
    }

    @Test
    fun testOpenPaneInTheMiddleOfClosePane_bothWithInterpolatorAndDuration() {
        testOpenPaneInTheMiddleOfClosePane(useDurationForOpen = true, useDurationForClose = true)
    }

    private val testInterpolator =
        object : Interpolator {
            override fun getInterpolation(input: Float): Float {
                val t = input - 1f
                return t * t * t * t * t + 1f
            }
        }
}
