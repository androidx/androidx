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
import androidx.slidingpanelayout.widget.SlidingPaneLayout.LOCK_MODE_OPEN_ONLY
import androidx.slidingpanelayout.widget.SlidingPaneLayout.LOCK_MODE_UNLOCKED
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.addWaitForOpenLatch
import androidx.slidingpanelayout.widget.helpers.dragLeft
import androidx.slidingpanelayout.widget.helpers.dragRight
import androidx.slidingpanelayout.widget.helpers.findViewX
import androidx.slidingpanelayout.widget.helpers.openPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests [SlidingPaneLayout.setLockMode] and [SlidingPaneLayout.getLockMode]
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
public class LockModeTest {

    @Test
    public fun testLayoutInflation() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            onView(withId(R.id.sliding_pane_layout)).check(ViewAssertions.matches(isDisplayed()))
            onView(withId(R.id.list_pane)).check(ViewAssertions.matches(isDisplayed()))
            onView(withId(R.id.detail_pane)).check(ViewAssertions.matches(isDisplayed()))
        }
    }

    /**
     * Test users can freely swipe right between list and detail panes when lock mode set to
     * LOCK_MODE_UNLOCKED.
     */
    @SdkSuppress(maxSdkVersion = 29) // TODO: Fix flaky test issues on API 30 Cuddlefish devices.
    @Test
    public fun testCanSlideRightWhenLockModeUnlocked() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val listPaneCloseX = findViewX(R.id.detail_pane)
            onView(withId(R.id.sliding_pane_layout)).perform(dragRight())
            assertThat(findViewX(R.id.detail_pane)).isGreaterThan(listPaneCloseX)
        }
    }

    /**
     * Test users can freely swipe left between list and detail panes when lock mode set to
     * LOCK_MODE_UNLOCKED.
     */
    @SdkSuppress(maxSdkVersion = 29) // TODO: Fix flaky test issues on API 30 Cuddlefish devices.
    @Test
    public fun testCanSlideLeftWhenLockModeUnlocked() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_UNLOCKED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val latch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            latch.await(2, TimeUnit.SECONDS)
            var listPaneOpenX = findViewX(R.id.detail_pane)
            onView(withId(R.id.sliding_pane_layout)).perform(dragLeft())
            assertThat(findViewX(R.id.detail_pane)).isLessThan(listPaneOpenX)
        }
    }

    /**
     * Test users cannot swipe from list to detail, but can swipe from detail to list when lock
     * mode set to LOCK_MODE_LOCKED_OPEN
     */
    @SdkSuppress(maxSdkVersion = 29) // TODO: Fix flaky test issues on API 30 Cuddlefish devices.
    @Test
    public fun testCanSlideListToDetailWhenLockModeLockedOpen() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_LOCKED_OPEN
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            var latch: CountDownLatch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            val listPaneOpenX = findViewX(R.id.detail_pane)
            onView(withId(R.id.sliding_pane_layout)).perform(dragRight())
            assertThat(findViewX(R.id.detail_pane)).isEqualTo(listPaneOpenX)
            onView(withId(R.id.sliding_pane_layout)).perform(dragLeft())
            assertThat(findViewX(R.id.detail_pane)).isLessThan(listPaneOpenX)
        }
    }

    /**
     * Test users cannot swipe from detail to list, but can swipe from list to detail when lock
     * mode set to LOCK_MODE_LOCKED_CLOSED
     */
    @SdkSuppress(maxSdkVersion = 29) // TODO: Fix flaky test issues on API 30 Cuddlefish devices.
    @Test
    public fun testCanSlideDetailToListWhenLockModeClosed() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_LOCKED_CLOSED
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val listPaneClosedX = findViewX(R.id.detail_pane)
            onView(withId(R.id.sliding_pane_layout)).perform(dragRight())
            val listPaneOpenX = findViewX(R.id.detail_pane)
            assertThat(listPaneOpenX).isGreaterThan(listPaneClosedX)
            onView(withId(R.id.sliding_pane_layout)).perform(dragLeft())
            assertThat(findViewX(R.id.detail_pane)).isEqualTo(listPaneOpenX)
        }
    }

    /**
     * Test users cannot swipe between list and detail panes when lock mode set to
     * LOCK_MODE_OPEN_ONLY
     */
    @SdkSuppress(maxSdkVersion = 29) // TODO: Fix flaky test issues on API 30 Cuddlefish devices.
    @Test
    public fun testCannotSlideWhenLockModeOpenOnly() {
        TestActivity.onActivityCreated = { activity ->
            val slidingPaneLayout =
                activity.findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.lockMode = LOCK_MODE_OPEN_ONLY
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val listPaneClosedX = findViewX(R.id.detail_pane)
            onView(withId(R.id.sliding_pane_layout)).perform(dragRight())
            assertThat(findViewX(R.id.detail_pane)).isEqualTo(listPaneClosedX)
            val latch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            val listPaneOpenX = findViewX(R.id.detail_pane)
            onView(withId(R.id.sliding_pane_layout)).perform(dragLeft())
            assertThat(findViewX(R.id.detail_pane)).isEqualTo(listPaneOpenX)
        }
    }
}