/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.app

import android.animation.LayoutTransition
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentContainerViewTest {
    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.fragment_container_view)
    }

    @Test
    fun setLayoutTransitionUnsupported() {
        val activity = activityRule.activity
        val layout = FragmentContainerView(activity.applicationContext)

        try {
            layout.layoutTransition = LayoutTransition()
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .contains("FragmentContainerView does not support Layout Transitions or " +
                        "animateLayoutChanges=\"true\".")
        }
    }

    // If view sets animateLayoutChanges to true, throw UnsupportedOperationException
    @Test
    fun animateLayoutChangesTrueUnsupported() {
        try {
            StrictViewFragment(R.layout.fragment_container_view_unsupported_operation)
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .contains("FragmentContainerView does not support Layout Transitions or " +
                        "animateLayoutChanges=\"true\".")
        }
    }

    @Test
    fun createFragmentWithFragmentContainerView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = StrictViewFragment(R.layout.fragment_container_view)
        fm.beginTransaction()
            .add(R.id.fragment_container_view, fragment)
            .commit()
        activityRule.runOnUiThread { fm.executePendingTransactions() }

        assertWithMessage("Fragment View should be a FragmentContainerView")
            .that(fragment.view)
            .isInstanceOf(FragmentContainerView::class.java)
    }
}