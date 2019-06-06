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
package androidx.fragment.app.test

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.executePendingTransactions
import androidx.fragment.app.popBackStackImmediate
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests usage of the [FragmentTransaction] class.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentTransactionTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var activity: FragmentTestActivity
    private var onBackStackChangedTimes: Int = 0
    private lateinit var onBackStackChangedListener: FragmentManager.OnBackStackChangedListener

    @Before
    fun setUp() {
        activity = activityRule.activity
        onBackStackChangedTimes = 0
        onBackStackChangedListener =
                FragmentManager.OnBackStackChangedListener { onBackStackChangedTimes++ }
        activity.supportFragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)
    }

    @After
    fun tearDown() {
        activity.supportFragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
    }

    @Test
    @UiThreadTest
    fun testAddTransactionWithValidFragment() {
        val fragment = CorrectFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(R.id.content, fragment)
            .addToBackStack(null)
            .commit()
        activity.supportFragmentManager.executePendingTransactions()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        assertThat(fragment.isAdded).isTrue()
    }

    @Test
    @UiThreadTest
    fun testAddTransactionWithPrivateFragment() {
        val fragment = PrivateFragment()
        try {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            activity.supportFragmentManager.executePendingTransactions()
            assertThat(onBackStackChangedTimes).isEqualTo(1)
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains("Fragment " + fragment.javaClass.canonicalName +
                        " must be a public static class to be  properly recreated from instance " +
                        "state.")
        } finally {
            assertWithMessage("Fragment shouldn't be added").that(fragment.isAdded).isFalse()
        }
    }

    @Test
    @UiThreadTest
    fun testAddTransactionWithPackagePrivateFragment() {
        val fragment = OuterPackagePrivateFragment.PackagePrivateFragment()
        try {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            activity.supportFragmentManager.executePendingTransactions()
            assertThat(onBackStackChangedTimes).isEqualTo(1)
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("Fragment " + fragment.javaClass.canonicalName +
                    " must be a public static class to be  properly recreated from instance " +
                    "state.")
        } finally {
            assertWithMessage("Fragment shouldn't be added").that(fragment.isAdded).isFalse()
        }
    }

    @Test
    @UiThreadTest
    fun testAddTransactionWithAnonymousFragment() {
        val fragment = object : Fragment() {}
        try {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            activity.supportFragmentManager.executePendingTransactions()
            assertThat(onBackStackChangedTimes).isEqualTo(1)
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("Fragment " + fragment.javaClass.canonicalName +
                    " must be a public static class to be  properly recreated from instance state.")
        } finally {
            assertWithMessage("Fragment shouldn't be added").that(fragment.isAdded).isFalse()
        }
    }

    @Test
    @UiThreadTest
    fun testGetLayoutInflater() {
        val fragment1 = OnGetLayoutInflaterFragment()
        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(0)
        activity.supportFragmentManager.beginTransaction()
            .add(R.id.content, fragment1)
            .addToBackStack(null)
            .commit()
        activity.supportFragmentManager.executePendingTransactions()
        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(1)
        assertThat(fragment1.layoutInflater).isEqualTo(fragment1.baseLayoutInflater)
        // getBaseLayoutInflater() didn't force onGetLayoutInflater()
        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(1)

        var layoutInflater = fragment1.baseLayoutInflater
        // Replacing fragment1 won't detach it, so the value won't be cleared
        val fragment2 = OnGetLayoutInflaterFragment()
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.content, fragment2)
            .addToBackStack(null)
            .commit()
        activity.supportFragmentManager.executePendingTransactions()

        assertThat(fragment1.layoutInflater).isSameInstanceAs(layoutInflater)
        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(1)

        // Popping it should cause onCreateView again, so a new LayoutInflater...
        activity.supportFragmentManager.popBackStackImmediate()
        assertThat(fragment1.layoutInflater).isNotSameInstanceAs(layoutInflater)
        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(2)
        layoutInflater = fragment1.baseLayoutInflater
        assertThat(fragment1.layoutInflater).isSameInstanceAs(layoutInflater)

        // Popping it should detach it, clearing the cached value again
        activity.supportFragmentManager.popBackStackImmediate()

        // once it is detached, the getBaseLayoutInflater() will default to throw
        // an exception, but we've made it return null instead.
        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(2)
        try {
            fragment1.layoutInflater
            fail("getLayoutInflater should throw when the Fragment is detached")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("onGetLayoutInflater() cannot be executed " +
                    "until the Fragment is attached to the FragmentManager.")
        }

        assertThat(fragment1.onGetLayoutInflaterCalls).isEqualTo(3)
    }

    @Test
    @UiThreadTest
    fun testAddTransactionWithNonStaticFragment() {
        val fragment = NonStaticFragment()
        try {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            activity.supportFragmentManager.executePendingTransactions()
            assertThat(onBackStackChangedTimes).isEqualTo(1)
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("Fragment " + fragment.javaClass.canonicalName +
                    " must be a public static class to be  properly recreated from instance state.")
        } finally {
            assertWithMessage("Fragment shouldn't be added").that(fragment.isAdded).isFalse()
        }
    }

    @Test
    @UiThreadTest
    fun testPostOnCommit() {
        var ran = false
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction().runOnCommit { ran = true }.commit()
        fm.executePendingTransactions()

        assertWithMessage("runOnCommit runnable never ran").that(ran).isTrue()

        ran = false

        try {
            fm.beginTransaction().runOnCommit { ran = true }.addToBackStack(null).commit()
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains("This FragmentTransaction is not allowed to" +
                    " be added to the back stack.")
        }

        fm.executePendingTransactions()

        assertWithMessage("runOnCommit runnable for back stack transaction was run")
            .that(ran)
            .isFalse()
    }

    // Ensure that getFragments() works during transactions, even if it is run off thread
    @Test
    fun getFragmentsOffThread() {
        val fm = activity.supportFragmentManager

        // Make sure that adding a fragment works
        val fragment = CorrectFragment()
        fm.beginTransaction()
            .add(R.id.content, fragment)
            .addToBackStack(null)
            .commit()

        activityRule.executePendingTransactions()
        var fragments: Collection<Fragment> = fm.fragments
        assertThat(fragments.size).isEqualTo(1)
        assertThat(fragments.contains(fragment)).isTrue()

        // Removed fragments shouldn't show
        fm.beginTransaction()
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()
        assertThat(fm.fragments.isEmpty()).isTrue()

        // Now try detached fragments
        activityRule.popBackStackImmediate()
        fm.beginTransaction()
            .detach(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()
        assertThat(fm.fragments.isEmpty()).isTrue()

        // Now try hidden fragments
        activityRule.popBackStackImmediate()
        fm.beginTransaction()
            .hide(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()
        fragments = fm.fragments
        assertThat(fragments.size).isEqualTo(1)
        assertThat(fragments.contains(fragment)).isTrue()

        // And showing it again shouldn't change anything:
        activityRule.popBackStackImmediate()
        fragments = fm.fragments
        assertThat(fragments.size).isEqualTo(1)
        assertThat(fragments.contains(fragment)).isTrue()

        // Now pop back to the start state
        activityRule.popBackStackImmediate()

        // We can't force concurrency, but we can do it lots of times and hope that
        // we hit it.
        // Reset count here to verify afterwards

        // Wait until we receive a OnBackStackChange callback for the total number of times
        // specified by transactionCount times 2 (1 for adding, 1 for removal)
        val transactionCount = 100
        val backStackLatch = CountDownLatch(transactionCount * 2)
        val countDownListener =
            FragmentManager.OnBackStackChangedListener { backStackLatch.countDown() }

        fm.addOnBackStackChangedListener(countDownListener)

        for (i in 0 until transactionCount) {
            val fragment2 = CorrectFragment()
            fm.beginTransaction()
                .add(R.id.content, fragment2)
                .addToBackStack(null)
                .commit()
            getFragmentsUntilSize(1)

            fm.popBackStack()
            getFragmentsUntilSize(0)
        }

        backStackLatch.await()

        fm.removeOnBackStackChangedListener(countDownListener)
    }

    /**
     * When a FragmentManager is detached, it should allow commitAllowingStateLoss()
     * and commitNowAllowingStateLoss() by just dropping the transaction.
     */
    @Test
    fun commitAllowStateLossDetached() {
        val fragment1 = CorrectFragment()
        activity.supportFragmentManager
            .beginTransaction()
            .add(fragment1, "1")
            .commit()
        activityRule.executePendingTransactions()
        val fm = fragment1.childFragmentManager
        activity.supportFragmentManager
            .beginTransaction()
            .remove(fragment1)
            .commit()
        activityRule.executePendingTransactions()
        assertThat(activity.supportFragmentManager.fragments.size).isEqualTo(0)
        assertThat(fm.fragments.size).isEqualTo(0)

        // Now the fragment1's fragment manager should allow commitAllowingStateLoss
        // by doing nothing since it has been detached.
        val fragment2 = CorrectFragment()
        fm.beginTransaction()
            .add(fragment2, "2")
            .commitAllowingStateLoss()
        activityRule.executePendingTransactions()
        assertThat(fm.fragments.size).isEqualTo(0)

        // It should also allow commitNowAllowingStateLoss by doing nothing
        activityRule.runOnUiThread {
            val fragment3 = CorrectFragment()
            fm.beginTransaction()
                .add(fragment3, "3")
                .commitNowAllowingStateLoss()
            assertThat(fm.fragments.size).isEqualTo(0)
        }
    }

    /**
     * onNewIntent() should note that the state is not saved so that child fragment
     * managers can execute transactions.
     */
    @Test
    fun newIntentUnlocks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent1 = Intent(activity, NewIntentActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val newIntentActivity = instrumentation.startActivitySync(intent1) as NewIntentActivity
        activityRule.waitForExecution()

        val intent2 = Intent(activity, FragmentTestActivity::class.java)
        intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        instrumentation.startActivitySync(intent2)
        activityRule.waitForExecution()

        val intent3 = Intent(activity, NewIntentActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent3)
        assertThat(newIntentActivity.newIntent.await(1, TimeUnit.SECONDS)).isTrue()
        activityRule.waitForExecution()

        for (fragment in newIntentActivity.supportFragmentManager.fragments) {
            // There really should only be one fragment in newIntentActivity.
            assertThat(fragment.childFragmentManager.fragments.size).isEqualTo(1)
        }
    }

    private fun getFragmentsUntilSize(expectedSize: Int) {
        val endTime = SystemClock.uptimeMillis() + 3000

        do {
            assertThat(SystemClock.uptimeMillis() < endTime).isTrue()
        } while (activity.supportFragmentManager.fragments.size != expectedSize)
    }

    class CorrectFragment : Fragment()

    private class PrivateFragment : Fragment()

    private inner class NonStaticFragment : Fragment()

    class OnGetLayoutInflaterFragment : Fragment(R.layout.fragment_a) {
        var onGetLayoutInflaterCalls = 0
        lateinit var baseLayoutInflater: LayoutInflater

        override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
            onGetLayoutInflaterCalls++
            baseLayoutInflater = super.onGetLayoutInflater(savedInstanceState)
            return baseLayoutInflater
        }
    }
}
