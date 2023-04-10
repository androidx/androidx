/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.SlidingPaneLayout.SlideableStateListener
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SlideableStateListenerTest {

    @After
    public fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    fun testAddSlideableStateListener() {
        var isSlideableCalled = false
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val layout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                layout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            val slidingPaneLayout = container
                .findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.addSlideableStateListener { _ ->
                isSlideableCalled = true
            }

            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            assertWithMessage(
                "isSlideable should be called when measuring the layout has completed")
                .that(isSlideableCalled)
                .isTrue()
        }
    }

    @Test
    fun testRemoveSlideableStateListener() {
        var isSlideableCalled: Boolean? = null

        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val layout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                layout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            val slideableStateListener = SlideableStateListener {
                isSlideableCalled = true
            }

            val slidingPaneLayout = container
                .findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
            slidingPaneLayout.addSlideableStateListener(slideableStateListener)

            activity.setContentView(container)
            slidingPaneLayout.removeSlideableStateListener(slideableStateListener)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            assertWithMessage("isSlideable should not be called if the listener has been removed")
                .that(isSlideableCalled)
                .isNull()
        }
    }
}