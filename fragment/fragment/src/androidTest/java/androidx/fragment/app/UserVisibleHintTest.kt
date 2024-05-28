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

import androidx.fragment.app.test.FragmentTestActivity
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.runOnUiThreadRethrow
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
@MediumTest
class UserVisibleHintTest {

    @Suppress("DEPRECATION")
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    @UiThreadTest
    @Test
    fun startOrdering() {
        var firstStartedFragment: Fragment? = null
        val callback: (fragment: Fragment) -> Unit = {
            if (firstStartedFragment == null) {
                firstStartedFragment = it
            }
        }
        val deferredFragment = ReportStartFragment(callback).apply { userVisibleHint = false }
        val fragment = ReportStartFragment(callback)
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction().add(deferredFragment, "deferred").commit()
        fm.beginTransaction().add(fragment, "fragment").commit()
        activityRule.executePendingTransactions()

        assertWithMessage("userVisibleHint=false Fragments should start after other Fragments")
            .that(firstStartedFragment)
            .isSameInstanceAs(fragment)
    }

    @UiThreadTest
    @Test
    fun startOrderingAfterSave() {
        var firstStartedFragment: Fragment? = null
        val callback: (fragment: Fragment) -> Unit = {
            if (firstStartedFragment == null) {
                firstStartedFragment = it
            }
        }
        val fm = activityRule.activity.supportFragmentManager

        // Add the fragment, save its state, then remove it.
        var deferredFragment = ReportStartFragment().apply { userVisibleHint = false }
        fm.beginTransaction().add(deferredFragment, "tag").commit()
        activityRule.executePendingTransactions()
        var state: Fragment.SavedState? = null
        activityRule.runOnUiThreadRethrow { state = fm.saveFragmentInstanceState(deferredFragment) }
        fm.beginTransaction().remove(deferredFragment).commit()
        activityRule.executePendingTransactions()

        // Create a new instance, calling setInitialSavedState
        deferredFragment = ReportStartFragment(callback)
        deferredFragment.setInitialSavedState(state)

        val fragment = ReportStartFragment(callback)
        fm.beginTransaction().add(deferredFragment, "deferred").commit()
        fm.beginTransaction().add(fragment, "fragment").commit()
        activityRule.executePendingTransactions()

        assertWithMessage("userVisibleHint=false Fragments should start after other Fragments")
            .that(firstStartedFragment)
            .isSameInstanceAs(fragment)
    }

    class ReportStartFragment(val callback: ((fragment: Fragment) -> Unit) = {}) :
        StrictFragment() {
        override fun onStart() {
            super.onStart()
            callback.invoke(this)
        }
    }
}
