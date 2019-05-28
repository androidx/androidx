/*
 * Copyright 2018 The Android Open Source Project
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

import android.app.Activity
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PrimaryNavFragmentTest {
    @get:Rule
    val activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    fun delegateBackToPrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment = StrictFragment()

        fm.beginTransaction()
            .add(strictFragment, null)
            .setPrimaryNavigationFragment(strictFragment)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment)

        val child = StrictFragment()
        val cfm = strictFragment.childFragmentManager
        cfm.beginTransaction()
            .add(child, null)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(cfm)

        assertWithMessage("child transaction not on back stack")
            .that(cfm.backStackEntryCount)
            .isEqualTo(1)

        activityRule.onBackPressed()

        assertWithMessage("child transaction still on back stack")
            .that(cfm.backStackEntryCount)
            .isEqualTo(0)
    }

    @Test
    fun popPrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment1 = StrictFragment()

        fm.beginTransaction()
            .add(strictFragment1, null)
            .setPrimaryNavigationFragment(strictFragment1)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)

        fm.beginTransaction()
            .remove(strictFragment1)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment is not null after remove")
            .that(fm.primaryNavigationFragment)
            .isNull()

        activityRule.onBackPressed()

        assertWithMessage("primary nav fragment was not restored on pop")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)

        val strictFragment2 = StrictFragment()
        fm.beginTransaction()
            .remove(strictFragment1)
            .add(strictFragment2, null)
            .setPrimaryNavigationFragment(strictFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not updated to new fragment")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment2)

        activityRule.onBackPressed()

        assertWithMessage("primary nav fragment not restored on pop")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)

        fm.beginTransaction()
            .setPrimaryNavigationFragment(strictFragment1)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not retained when set again in new transaction")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)
        activityRule.onBackPressed()

        assertWithMessage(
            "same primary nav fragment not retained when set primary nav transaction popped")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)
    }

    @Test
    fun replacePrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment1 = StrictFragment()

        fm.beginTransaction()
            .add(android.R.id.content, strictFragment1)
            .setPrimaryNavigationFragment(strictFragment1)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)

        val strictFragment2 = StrictFragment()
        fm.beginTransaction()
            .replace(android.R.id.content, strictFragment2)
            .addToBackStack(null)
            .commit()

        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not null after replace")
            .that(fm.primaryNavigationFragment)
            .isNull()

        activityRule.onBackPressed()

        assertWithMessage("primary nav fragment not restored after popping replace")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment1)

        fm.beginTransaction()
            .setPrimaryNavigationFragment(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not null after explicit set to null")
            .that(fm.primaryNavigationFragment)
            .isNull()

        fm.beginTransaction()
            .replace(android.R.id.content, strictFragment2)
            .setPrimaryNavigationFragment(strictFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameAs(strictFragment2)

        activityRule.onBackPressed()

        assertWithMessage("primary nav fragment not null after popping replace")
            .that(fm.primaryNavigationFragment)
            .isNull()
    }

    private fun executePendingTransactions(fm: FragmentManager) {
        activityRule.runOnUiThread { fm.executePendingTransactions() }
    }

    private fun ActivityTestRule<out Activity>.onBackPressed() = runOnUiThread {
        activity.onBackPressed()
    }
}
