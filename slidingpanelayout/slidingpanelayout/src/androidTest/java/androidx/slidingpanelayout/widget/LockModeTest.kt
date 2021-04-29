/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.SlidingPaneLayout.LOCK_MODE_LOCKED_CLOSED
import androidx.slidingpanelayout.widget.SlidingPaneLayout.LOCK_MODE_LOCKED_OPEN
import androidx.slidingpanelayout.widget.SlidingPaneLayout.LOCK_MODE_LOCKED
import androidx.slidingpanelayout.widget.SlidingPaneLayout.LOCK_MODE_UNLOCKED
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.addWaitForCloseLatch
import androidx.slidingpanelayout.widget.helpers.addWaitForOpenLatch
import androidx.slidingpanelayout.widget.helpers.addWaitForSlideLatch
import androidx.slidingpanelayout.widget.helpers.openPane
import androidx.slidingpanelayout.widget.helpers.slideClose
import androidx.slidingpanelayout.widget.helpers.slideOpen
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Tests [SlidingPaneLayout.setLockMode] and [SlidingPaneLayout.getLockMode]
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
public class LockModeTest {

    @After
    public fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    public fun testLayoutInflation() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            onView(withId(R.id.sliding_pane_layout)).check(ViewAssertions.matches(isDisplayed()))
            onView(withId(R.id.list_pane)).check(ViewAssertions.matches(isDisplayed()))
            onView(withId(R.id.detail_pane)).check(
                ViewAssertions.matches(
                    ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
                )
            )
            val slidingPaneLayout =
                withActivity { findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout) }
            assertThat(slidingPaneLayout.isOpen).isFalse()
            assertThat(slidingPaneLayout.isSlideable).isTrue()
        }
    }

    /**
     * Test users can swipe right between list and detail panes when lock mode set to
     * LOCK_MODE_UNLOCKED.
     */
    @SdkSuppress(maxSdkVersion = 28) // TODO: Fix flaky test issues on API 30 Cuttlefish devices.
    @Test
    public fun testCanSlideRightWhenLockModeUnlocked() {
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
            val panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideClose())
            // wait for detail pane sliding
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    /**
     * Test users can swipe left between list and detail panes when lock mode set to
     * LOCK_MODE_UNLOCKED.
     */
    @SdkSuppress(maxSdkVersion = 28) // TODO: Fix flaky test issues on API 30 Cuttlefish devices.
    @Test
    public fun testCanSlideLeftWhenLockModeUnlocked() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideOpen())
            // wait for detail pane sliding
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    /**
     * Test users can swipe to open detail pane in lock mode LOCK_MODE_LOCKED_OPEN when
     * detail view is in closed state. Otherwise, users cannot swipe it.
     */
    @SdkSuppress(maxSdkVersion = 28) // TODO: Fix flaky test issues on API 30 Cuttlefish devices.
    @Test
    public fun testSwipeWhenLockModeLockedOpen() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_LOCKED_OPEN
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            var panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideOpen())
            // can slide to open
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()
            panelSlideCountDownLatch = addWaitForCloseLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideClose())
            // cannot slide to close
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isFalse()
        }
    }

    /**
     * Test users can swipe to close the detail pane in lock mode LOCK_MODE_LOCKED_CLOSED when
     * detail view is in open state. Otherwise, users cannot swipe it.
     */
    @SdkSuppress(maxSdkVersion = 28) // TODO: Fix flaky test issues on API 30 Cuttlefish devices.
    @Test
    public fun testSwipeWhenLockModeLockedClosed() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_LOCKED_CLOSED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            var panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideOpen())
            // cannot slide to open
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isFalse()
            val latch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideClose())
            // can slide to close
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()
        }
    }

    /**
     * Test users cannot swipe between list and detail panes when lock mode set to
     * LOCK_MODE_LOCKED
     */
    @SdkSuppress(maxSdkVersion = 28) // TODO: Fix flaky test issues on API 30 Cuttlefish devices.
    @Test
    public fun testSwipeWhenLockModeLocked() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_LOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            var panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideOpen())
            // cannot slide to open
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isFalse()
            val latch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            panelSlideCountDownLatch = addWaitForSlideLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(slideClose())
            // cannot slide to close
            assertThat(panelSlideCountDownLatch.await(2, TimeUnit.SECONDS)).isFalse()
        }
    }
}