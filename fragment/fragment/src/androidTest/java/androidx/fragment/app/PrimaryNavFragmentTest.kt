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
import androidx.testutils.runOnUiThreadRethrow
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PrimaryNavFragmentTest {
    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    fun delegateBackToPrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val navigations = mutableListOf<Pair<Fragment, Boolean>>()
        val trackingFragment = TrackingFragment(navigations)

        fm.beginTransaction()
            .add(trackingFragment, null)
            .setPrimaryNavigationFragment(trackingFragment)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment to true))
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment)

        val child = StrictFragment()
        val cfm = trackingFragment.childFragmentManager
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
        val navigations = mutableListOf<Pair<Fragment, Boolean>>()
        val trackingFragment1 = TrackingFragment(navigations)
        val trackingFragment2 = TrackingFragment(navigations)

        fm.beginTransaction()
            .add(trackingFragment1, null)
            .setPrimaryNavigationFragment(trackingFragment1)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to true))
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)

        fm.beginTransaction()
            .remove(trackingFragment1)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to false))
        assertWithMessage("primary nav fragment is not null after remove")
            .that(fm.primaryNavigationFragment)
            .isNull()

        activityRule.onBackPressed()

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to true))
        assertWithMessage("primary nav fragment was not restored on pop")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)

        fm.beginTransaction()
            .remove(trackingFragment1)
            .add(trackingFragment2, null)
            .setPrimaryNavigationFragment(trackingFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(
            listOf(trackingFragment1 to false, trackingFragment2 to true)
        )

        assertWithMessage("primary nav fragment not updated to new fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment2)

        activityRule.onBackPressed()

        assertThat(navigations.drain()).isEqualTo(
            listOf(trackingFragment2 to false, trackingFragment1 to true)
        )
        assertWithMessage("primary nav fragment not restored on pop")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)

        fm.beginTransaction()
            .setPrimaryNavigationFragment(trackingFragment1)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertWithMessage("primary nav fragment not retained when set again in new transaction")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)
        activityRule.onBackPressed()

        assertWithMessage(
            "same primary nav fragment not retained when set primary nav transaction popped"
        )
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)
    }

    @Test
    fun replacePrimaryNav() {
        val fm = activityRule.activity.supportFragmentManager
        val navigations = mutableListOf<Pair<Fragment, Boolean>>()

        val trackingFragment1 = TrackingFragment(navigations)
        val trackingFragment2 = TrackingFragment(navigations)

        fm.beginTransaction()
            .add(android.R.id.content, trackingFragment1)
            .setPrimaryNavigationFragment(trackingFragment1)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to true))
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)

        fm.beginTransaction()
            .replace(android.R.id.content, trackingFragment2)
            .addToBackStack(null)
            .commit()

        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to false))
        navigations.clear()
        assertWithMessage("primary nav fragment not null after replace")
            .that(fm.primaryNavigationFragment)
            .isNull()

        activityRule.onBackPressed()

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to true))
        assertWithMessage("primary nav fragment not restored after popping replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)

        fm.beginTransaction()
            .setPrimaryNavigationFragment(null)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment1 to false))
        assertWithMessage("primary nav fragment not null after explicit set to null")
            .that(fm.primaryNavigationFragment)
            .isNull()

        fm.beginTransaction()
            .replace(android.R.id.content, trackingFragment2)
            .setPrimaryNavigationFragment(trackingFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment2 to true))
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment2)

        activityRule.onBackPressed()

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment2 to false))
        assertWithMessage("primary nav fragment not null after popping replace")
            .that(fm.primaryNavigationFragment)
            .isNull()
    }

    @Test
    fun replacePrimaryNavAfterSetPrimary() {
        val fm = activityRule.activity.supportFragmentManager
        val navigations = mutableListOf<Pair<Fragment, Boolean>>()

        val trackingFragment1 = TrackingFragment(navigations)
        val trackingFragment2 = TrackingFragment(navigations)

        fm.beginTransaction()
            .add(android.R.id.content, trackingFragment1)
            .setPrimaryNavigationFragment(trackingFragment1)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).containsExactly(trackingFragment1 to true).inOrder()
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)

        // Note that we specifically call setPrimaryNavigationFragment() before the replace() call
        fm.beginTransaction()
            .setPrimaryNavigationFragment(trackingFragment2)
            .replace(android.R.id.content, trackingFragment2)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).containsExactly(
            trackingFragment1 to false, trackingFragment2 to true
        ).inOrder()
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment2)

        activityRule.onBackPressed()

        // Note that strictFragment2 does not get a callback since the
        // pop of the replace happens before the pop of the setPrimaryFragment
        assertThat(navigations.drain()).containsExactly(
            trackingFragment2 to false, trackingFragment1 to true
        ).inOrder()
        assertWithMessage("primary nav fragment is restored after popping replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment1)
    }

    // When USE_STATE_MANAGER is false, postponed transactions are temporarily popped.
    // This test verifies that the dispatched primary navigation fragment
    // matches the current state of the FragmentManager itself.
    @Test
    fun replacePostponedFragment() {
        val fm = activityRule.activity.supportFragmentManager
        val navigations = mutableListOf<Pair<Fragment, Boolean>>()
        val trackingFragment = TrackingViewFragment(navigations)
        val postponedFragment = PostponedFragment(navigations)
        val replacementFragment = TrackingFragment(navigations)

        fm.beginTransaction()
            .add(android.R.id.content, trackingFragment)
            .setPrimaryNavigationFragment(trackingFragment)
            .setReorderingAllowed(true)
            .commit()
        executePendingTransactions(fm)

        assertThat(navigations.drain()).isEqualTo(listOf(trackingFragment to true))
        assertWithMessage("new fragment is not primary nav fragment")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment)

        fm.beginTransaction()
            .replace(android.R.id.content, postponedFragment)
            .setPrimaryNavigationFragment(postponedFragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertThat(navigations.drain()).isEqualTo(
            listOf(trackingFragment to false, postponedFragment to true)
        )
        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(postponedFragment)

        // Now pop the back stack and also add a replacement Fragment
        fm.popBackStack()
        fm.beginTransaction()
            .replace(android.R.id.content, replacementFragment)
            .setPrimaryNavigationFragment(replacementFragment)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertThat(navigations.drain()).isEqualTo(
            listOf(
                postponedFragment to false,
                trackingFragment to true,
                trackingFragment to false,
                replacementFragment to true
            )
        )

        assertWithMessage("primary nav fragment not set correctly after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(replacementFragment)

        // Now go back to the first Fragment
        activityRule.onBackPressed()

        assertThat(navigations.drain()).isEqualTo(
            listOf(replacementFragment to false, trackingFragment to true)
        )

        assertWithMessage("primary nav fragment is restored after replace")
            .that(fm.primaryNavigationFragment)
            .isSameInstanceAs(trackingFragment)
        assertWithMessage("Only the first Fragment should exist on the FragmentManager")
            .that(fm.fragments)
            .containsExactly(trackingFragment)
    }

    private fun executePendingTransactions(fm: FragmentManager) {
        activityRule.runOnUiThread { fm.executePendingTransactions() }
    }

    @Suppress("DEPRECATION")
    private fun androidx.test.rule.ActivityTestRule<out Activity>.onBackPressed() =
        runOnUiThreadRethrow {
            activity.onBackPressed()
        }

    private fun <T> MutableList<T>.drain(): List<T> {
        val result = ArrayList<T>(this)
        this.clear()
        return result
    }

    class PostponedFragment(
        tracking: MutableList<Pair<Fragment, Boolean>>
    ) : TrackingViewFragment(tracking) {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition()
        }
    }

    class TrackingFragment(val tracker: MutableList<Pair<Fragment, Boolean>>) : StrictFragment() {
        override fun onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment: Boolean) {
            tracker.add(this to isPrimaryNavigationFragment)
        }
    }

    open class TrackingViewFragment(
        val tracker: MutableList<Pair<Fragment, Boolean>>
    ) : StrictViewFragment() {
        override fun onPrimaryNavigationFragmentChanged(isPrimaryNavigationFragment: Boolean) {
            tracker.add(this to isPrimaryNavigationFragment)
        }
    }
}
