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

import android.view.View
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.addWaitForOpenLatch
import androidx.slidingpanelayout.widget.helpers.openPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests [SlidingPaneLayout.PanelSlideListener]
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
public class PanelSlideListenerTest {

    @After
    public fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    public fun testAddPanelSlideListener() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            val slidingPaneLayout = withActivity {
                findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            }
            var onPanelSlide = false
            var onPanelOpened = false
            var onPanelClosed = false
            val panelClosedCountDownLatch = CountDownLatch(1)
            slidingPaneLayout.addPanelSlideListener(
                object : SlidingPaneLayout.PanelSlideListener {
                    override fun onPanelSlide(panel: View, slideOffset: Float) {
                        onPanelSlide = true
                    }
                    override fun onPanelOpened(panel: View) {
                        onPanelOpened = true
                    }

                    override fun onPanelClosed(panel: View) {
                        onPanelClosed = true
                        panelClosedCountDownLatch.countDown()
                    }
                })
            val latch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()

            assertWithMessage("onPanelSlide should be triggered when the pane is opened")
                .that(onPanelSlide)
                .isTrue()

            assertWithMessage("onPanelOpened should be triggered when the pane is opened")
                .that(onPanelOpened)
                .isTrue()

            onPanelSlide = false

            // Now close the SlidingPaneLayout
            slidingPaneLayout.closePane()
            assertThat(panelClosedCountDownLatch.await(2, TimeUnit.SECONDS)).isTrue()

            assertWithMessage("onPanelSlide should be triggered when the pane is closed")
                .that(onPanelSlide)
                .isTrue()

            assertWithMessage("onPanelClosed should be triggered when the pane is closed")
                .that(onPanelClosed)
                .isTrue()
        }
    }

    @Test
    public fun testRemovePanelSlideListener() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            val slidingPaneLayout = withActivity {
                findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            }
            var onPanelOpened = false
            val listener = object : SlidingPaneLayout.SimplePanelSlideListener() {
                override fun onPanelOpened(panel: View) {
                    onPanelOpened = true
                }
            }
            // Add the listener
            slidingPaneLayout.addPanelSlideListener(listener)
            // Now remove the listener before we open the pane
            slidingPaneLayout.removePanelSlideListener(listener)

            val latch = addWaitForOpenLatch(R.id.sliding_pane_layout)
            onView(withId(R.id.sliding_pane_layout)).perform(openPane())
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()

            assertWithMessage("onPanelOpened shouldn't be triggered on a removed listener")
                .that(onPanelOpened)
                .isFalse()
        }
    }
}