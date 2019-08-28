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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.testutils.runOnUiThreadRethrow
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@LargeTest
class PrimaryNavFragmentTest {
    @get:Rule
    val activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    fun delegateBackToPrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment = spy(StrictFragment())

        fm.beginTransaction()
            .add(strictFragment, null)
            .setPrimaryNavigationFragment(strictFragment)
            .commit()
        executePendingTransactions(fm)

        verify(strictFragment).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment)

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
        val strictFragment1 = spy(StrictFragment())
        val strictFragment2 = spy(StrictFragment())
        val inOrder = inOrder(strictFragment1, strictFragment2)

        fm.beginTransaction()
            .add(strictFragment1, null)
            .setPrimaryNavigationFragment(strictFragment1)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)

        fm.beginTransaction()
            .remove(strictFragment1)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(false)
        assertWithMessage("primary nav fragment is not null after remove")
            .that(fm.primaryNavigationFragment)
            .isNull()

        activityRule.onBackPressed()

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment was not restored on pop")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)

        fm.beginTransaction()
            .remove(strictFragment1)
            .add(strictFragment2, null)
            .setPrimaryNavigationFragment(strictFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(strictFragment2).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not updated to new fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment2)

        activityRule.onBackPressed()

        inOrder.verify(strictFragment2).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not restored on pop")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)

        fm.beginTransaction()
            .setPrimaryNavigationFragment(strictFragment1)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not retained when set again in new transaction")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)
        activityRule.onBackPressed()

        assertWithMessage(
            "same primary nav fragment not retained when set primary nav transaction popped")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)
    }

    @Test
    fun replacePrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment1 = spy(StrictFragment())
        val strictFragment2 = spy(StrictFragment())
        val inOrder = inOrder(strictFragment1, strictFragment2)

        fm.beginTransaction()
            .add(android.R.id.content, strictFragment1)
            .setPrimaryNavigationFragment(strictFragment1)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)

        fm.beginTransaction()
            .replace(android.R.id.content, strictFragment2)
            .addToBackStack(null)
            .commit()

        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(false)
        assertWithMessage("primary nav fragment not null after replace")
            .that(fm.primaryNavigationFragment)
            .isNull()

        activityRule.onBackPressed()

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not restored after popping replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)

        fm.beginTransaction()
            .setPrimaryNavigationFragment(null)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(false)
        assertWithMessage("primary nav fragment not null after explicit set to null")
            .that(fm.primaryNavigationFragment)
            .isNull()

        fm.beginTransaction()
            .replace(android.R.id.content, strictFragment2)
            .setPrimaryNavigationFragment(strictFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment2).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment2)

        activityRule.onBackPressed()

        inOrder.verify(strictFragment2).onPrimaryNavigationFragmentChanged(false)
        assertWithMessage("primary nav fragment not null after popping replace")
            .that(fm.primaryNavigationFragment)
            .isNull()
    }

    @Test
    fun replacePrimaryNavAfterSetPrimary() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment1 = spy(StrictFragment())
        val strictFragment2 = spy(StrictFragment())
        val inOrder = inOrder(strictFragment1, strictFragment2)

        fm.beginTransaction()
            .add(android.R.id.content, strictFragment1)
            .setPrimaryNavigationFragment(strictFragment1)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)

        // Note that we specifically call setPrimaryNavigationFragment() before the replace() call
        fm.beginTransaction()
            .setPrimaryNavigationFragment(strictFragment2)
            .replace(android.R.id.content, strictFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(strictFragment2).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment2)

        activityRule.onBackPressed()

        // Note that strictFragment2 does not get a callback since the
        // pop of the replace happens before the pop of the setPrimaryFragment
        inOrder.verify(strictFragment1).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment is restored after popping replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment1)
    }

    @Test
    fun replacePostponedFragment() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment = spy(StrictViewFragment())
        val postponedFragment = spy(PostponedFragment())
        val replacementFragment = spy(StrictFragment())
        val inOrder = inOrder(strictFragment, postponedFragment, replacementFragment)

        fm.beginTransaction()
            .add(android.R.id.content, strictFragment)
            .setPrimaryNavigationFragment(strictFragment)
            .setReorderingAllowed(true)
            .commit()
        executePendingTransactions(fm)

        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment)

        fm.beginTransaction()
            .replace(android.R.id.content, postponedFragment)
            .setPrimaryNavigationFragment(postponedFragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(postponedFragment).onPrimaryNavigationFragmentChanged(true)
        inOrder.verify(postponedFragment).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment)

        // Now pop the back stack and also add a replacement Fragment
        fm.popBackStack()
        fm.beginTransaction()
            .replace(android.R.id.content, replacementFragment)
            .setPrimaryNavigationFragment(replacementFragment)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(postponedFragment).onPrimaryNavigationFragmentChanged(true)
        inOrder.verify(postponedFragment).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(true)
        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(replacementFragment).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Now go back to the first Fragment
        activityRule.onBackPressed()

        inOrder.verify(replacementFragment).onPrimaryNavigationFragmentChanged(false)
        inOrder.verify(strictFragment).onPrimaryNavigationFragmentChanged(true)
        assertWithMessage("primary nav fragment is restored after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(strictFragment)
        assertWithMessage("Only the first Fragment should exist on the FragmentManager")
            .that(fm.fragments)
            .containsExactly(strictFragment)
    }

    private fun executePendingTransactions(fm: FragmentManager) {
        activityRule.runOnUiThread { fm.executePendingTransactions() }
    }

    private fun ActivityTestRule<out Activity>.onBackPressed() = runOnUiThreadRethrow {
        activity.onBackPressed()
    }

    open class PostponedFragment : StrictViewFragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition()
        }
    }
}
