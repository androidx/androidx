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

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.isTwoPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SlidingPaneLayoutTest {

    @After
    fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    fun testLayoutRoot() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun testLayoutWidthSpecExact() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutWidthSpecAtMost() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }
}