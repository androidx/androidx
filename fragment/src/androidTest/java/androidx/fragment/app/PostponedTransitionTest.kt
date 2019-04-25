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

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class PostponedTransitionTest {
    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val beginningFragment = PostponedFragment1()

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.simple_container)
        val fm = activityRule.activity.supportFragmentManager

        val backStackLatch = CountDownLatch(1)
        val backStackListener = object : FragmentManager.OnBackStackChangedListener {
            override fun onBackStackChanged() {
                backStackLatch.countDown()
                fm.removeOnBackStackChangedListener(this)
            }
        }
        fm.addOnBackStackChangedListener(backStackListener)
        fm.beginTransaction()
            .add(R.id.fragmentContainer, beginningFragment)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()

        backStackLatch.await()

        beginningFragment.startPostponedEnterTransition()
        beginningFragment.waitForTransition()
        clearTargets(beginningFragment)
    }

    // Ensure that replacing with a fragment that has a postponed transition
    // will properly postpone it, both adding and popping.
    @Test
    fun replaceTransition() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment = PostponedFragment2()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        // should be postponed now
        assertPostponedTransition(beginningFragment, fragment)

        // start the postponed transition
        fragment.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(beginningFragment, fragment)

        activityRule.popBackStackImmediate()

        // should be postponed going back, too
        assertPostponedTransition(fragment, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment, beginningFragment)
    }

    // Ensure that replacing a fragment doesn't cause problems with the back stack nesting level
    @Test
    fun backStackNestingLevel() {
        val fm = activityRule.activity.supportFragmentManager
        var startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment1 = TransitionFragment(R.layout.scene2)
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        // make sure transition ran
        assertForwardTransition(beginningFragment, fragment1)

        activityRule.popBackStackImmediate()

        // should be postponed going back
        assertPostponedTransition(fragment1, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment1, beginningFragment)

        startBlue = activityRule.activity.findViewById(R.id.blueSquare)

        val fragment2 = TransitionFragment(R.layout.scene2)
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        // make sure transition ran
        assertForwardTransition(beginningFragment, fragment2)

        activityRule.popBackStackImmediate()

        // should be postponed going back
        assertPostponedTransition(fragment2, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment2, beginningFragment)
    }

    // Ensure that postponed transition is forced after another has been committed.
    // This tests when the transactions are executed together
    @Test
    fun forcedTransition1() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment2 = PostponedFragment2()
        val fragment3 = PostponedFragment1()

        var commit = 0
        // Need to run this on the UI thread so that the transaction doesn't start
        // between the two
        instrumentation.runOnMainSync {
            commit = fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()

            fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit()
        }
        activityRule.waitForExecution()

        // transition to fragment2 should be started
        assertForwardTransition(beginningFragment, fragment2)

        // fragment3 should be postponed, but fragment2 should be executed with no transition.
        assertPostponedTransition(fragment2, fragment3, beginningFragment)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(fragment2, fragment3)

        activityRule.popBackStackImmediate(commit, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        assertBackTransition(fragment3, fragment2)

        assertPostponedTransition(fragment2, beginningFragment, fragment3)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment2, beginningFragment)
    }

    // Ensure that postponed transition is forced after another has been committed.
    // This tests when the transactions are processed separately.
    @Test
    fun forcedTransition2() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment2 = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(beginningFragment, fragment2)

        val fragment3 = PostponedFragment1()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        // This should cancel the beginningFragment -> fragment2 transition
        // and start fragment2 -> fragment3 transition postponed
        activityRule.waitForExecution()

        // fragment3 should be postponed, but fragment2 should be executed with no transition.
        assertPostponedTransition(fragment2, fragment3, beginningFragment)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(fragment2, fragment3)

        // Pop back to fragment2, but it should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment3, fragment2)

        // Pop to beginningFragment -- should cancel the fragment2 transition and
        // start the beginningFragment transaction postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment2, beginningFragment, fragment3)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment2, beginningFragment)
    }

    // Do a bunch of things to one fragment in a transaction and see if it can screw things up.
    @Test
    fun crazyTransition() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment2 = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .hide(beginningFragment)
            .replace(R.id.fragmentContainer, fragment2)
            .hide(fragment2)
            .detach(fragment2)
            .attach(fragment2)
            .show(fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(beginningFragment, fragment2)

        // start the postponed transition
        fragment2.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(beginningFragment, fragment2)

        // Pop back to fragment2, but it should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment2, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment2, beginningFragment)
    }

    // Execute transactions on different containers and ensure that they don't conflict
    @Test
    fun differentContainers() {
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .remove(beginningFragment)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        activityRule.setContentView(R.layout.double_container)

        val fragment1 = PostponedFragment1()
        val fragment2 = PostponedFragment1()

        fm.beginTransaction()
            .add(R.id.fragmentContainer1, fragment1)
            .add(R.id.fragmentContainer2, fragment2)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        fragment1.startPostponedEnterTransition()
        fragment2.startPostponedEnterTransition()
        fragment1.waitForTransition()
        fragment2.waitForTransition()
        clearTargets(fragment1)
        clearTargets(fragment2)

        val startBlue1 = fragment1.requireView().findViewById<View>(R.id.blueSquare)
        val startBlue2 = fragment2.requireView().findViewById<View>(R.id.blueSquare)

        val fragment3 = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue1, "blueSquare")
            .replace(R.id.fragmentContainer1, fragment3)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment3)

        val fragment4 = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue2, "blueSquare")
            .replace(R.id.fragmentContainer2, fragment4)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment3)
        assertPostponedTransition(fragment2, fragment4)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure only one ran
        assertForwardTransition(fragment1, fragment3)
        assertPostponedTransition(fragment2, fragment4)

        // start the postponed transition
        fragment4.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(fragment2, fragment4)

        // Pop back to fragment2 -- should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment4, fragment2)

        // Pop back to fragment1 -- also should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment4, fragment2)
        assertPostponedTransition(fragment3, fragment1)

        // start the postponed transition
        fragment2.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment4, fragment2)

        // but not the postponed one
        assertPostponedTransition(fragment3, fragment1)

        // start the postponed transition
        fragment1.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment3, fragment1)
    }

    // Execute transactions on different containers and ensure that they don't conflict.
    // The postponement can be started out-of-order
    @Test
    fun outOfOrderContainers() {
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .remove(beginningFragment)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        activityRule.setContentView(R.layout.double_container)

        val fragment1 = PostponedFragment1()
        val fragment2 = PostponedFragment1()

        fm.beginTransaction()
            .add(R.id.fragmentContainer1, fragment1)
            .add(R.id.fragmentContainer2, fragment2)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        fragment1.startPostponedEnterTransition()
        fragment2.startPostponedEnterTransition()
        fragment1.waitForTransition()
        fragment2.waitForTransition()
        clearTargets(fragment1)
        clearTargets(fragment2)

        val startBlue1 = fragment1.requireView().findViewById<View>(R.id.blueSquare)
        val startBlue2 = fragment2.requireView().findViewById<View>(R.id.blueSquare)

        val fragment3 = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue1, "blueSquare")
            .replace(R.id.fragmentContainer1, fragment3)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment3)

        val fragment4 = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue2, "blueSquare")
            .replace(R.id.fragmentContainer2, fragment4)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment3)
        assertPostponedTransition(fragment2, fragment4)

        // start the postponed transition
        fragment4.startPostponedEnterTransition()

        // make sure only one ran
        assertForwardTransition(fragment2, fragment4)
        assertPostponedTransition(fragment1, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(fragment1, fragment3)

        // Pop back to fragment2 -- should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment4, fragment2)

        // Pop back to fragment1 -- also should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment4, fragment2)
        assertPostponedTransition(fragment3, fragment1)

        // start the postponed transition
        fragment1.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment3, fragment1)

        // but not the postponed one
        assertPostponedTransition(fragment4, fragment2)

        // start the postponed transition
        fragment2.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(fragment4, fragment2)
    }

    // Make sure that commitNow for a transaction on a different fragment container doesn't
    // affect the postponed transaction
    @Test
    fun commitNowNoEffect() {
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .remove(beginningFragment)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        activityRule.setContentView(R.layout.double_container)

        val fragment1 = PostponedFragment1()
        val fragment2 = PostponedFragment1()

        fm.beginTransaction()
            .add(R.id.fragmentContainer1, fragment1)
            .add(R.id.fragmentContainer2, fragment2)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        fragment1.startPostponedEnterTransition()
        fragment2.startPostponedEnterTransition()
        fragment1.waitForTransition()
        fragment2.waitForTransition()
        clearTargets(fragment1)
        clearTargets(fragment2)

        val startBlue1 = fragment1.requireView().findViewById<View>(R.id.blueSquare)
        val startBlue2 = fragment2.requireView().findViewById<View>(R.id.blueSquare)

        val fragment3 = PostponedFragment2()
        val strictFragment1 = StrictFragment()

        fm.beginTransaction()
            .addSharedElement(startBlue1, "blueSquare")
            .replace(R.id.fragmentContainer1, fragment3)
            .add(strictFragment1, "1")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment3)

        val fragment4 = PostponedFragment2()
        val strictFragment2 = StrictFragment()

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .addSharedElement(startBlue2, "blueSquare")
                .replace(R.id.fragmentContainer2, fragment4)
                .remove(strictFragment1)
                .add(strictFragment2, "2")
                .setReorderingAllowed(true)
                .commitNow()
        }

        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment3)
        assertPostponedTransition(fragment2, fragment4)

        // start the postponed transition
        fragment4.startPostponedEnterTransition()

        // make sure only one ran
        assertForwardTransition(fragment2, fragment4)
        assertPostponedTransition(fragment1, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(fragment1, fragment3)
    }

    // Make sure that commitNow for a transaction affecting a postponed fragment in the same
    // container forces the postponed transition to start.
    @Test
    fun commitNowStartsPostponed() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue1 = beginningFragment.requireView().findViewById<View>(R.id.blueSquare)

        val fragment2 = PostponedFragment2()
        val fragment1 = PostponedFragment1()

        fm.beginTransaction()
            .addSharedElement(startBlue1, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val startBlue2 = fragment2.requireView().findViewById<View>(R.id.blueSquare)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .addSharedElement(startBlue2, "blueSquare")
                .replace(R.id.fragmentContainer, fragment1)
                .setReorderingAllowed(true)
                .commitNow()
        }

        assertPostponedTransition(fragment2, fragment1, beginningFragment)

        // start the postponed transition
        fragment1.startPostponedEnterTransition()

        assertForwardTransition(fragment2, fragment1)
    }

    // Make sure that when a transaction that removes a view is postponed that
    // another transaction doesn't accidentally remove the view early.
    @Test
    fun noAccidentalRemoval() {
        val fm = activityRule.activity.supportFragmentManager
        fm.beginTransaction()
            .remove(beginningFragment)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        activityRule.setContentView(R.layout.double_container)

        val fragment1 = PostponedFragment1()

        fm.beginTransaction()
            .add(R.id.fragmentContainer1, fragment1)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        fragment1.startPostponedEnterTransition()
        fragment1.waitForTransition()
        clearTargets(fragment1)

        val fragment2 = PostponedFragment2()
        // Create a postponed transaction that removes a view
        fm.beginTransaction()
            .replace(R.id.fragmentContainer1, fragment2)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        assertPostponedTransition(fragment1, fragment2)

        val fragment3 = PostponedFragment1()
        // Create a transaction that doesn't interfere with the previously postponed one
        fm.beginTransaction()
            .replace(R.id.fragmentContainer2, fragment3)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertPostponedTransition(fragment1, fragment2)

        fragment3.startPostponedEnterTransition()
        fragment3.waitForTransition()
        clearTargets(fragment3)

        assertPostponedTransition(fragment1, fragment2)
    }

    // Ensure that a postponed transaction that is popped runs immediately and that
    // the transaction results in the original state with no transition.
    @Test
    fun popPostponedTransaction() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = beginningFragment.requireView().findViewById<View>(R.id.blueSquare)

        val fragment = PostponedFragment2()

        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertPostponedTransition(beginningFragment, fragment)

        activityRule.popBackStackImmediate()

        fragment.waitForNoTransition()
        beginningFragment.waitForNoTransition()

        assureNoTransition(fragment)
        assureNoTransition(beginningFragment)

        assertThat(fragment.isAdded).isFalse()
        assertThat(fragment.view).isNull()
        assertThat(beginningFragment.view).isNotNull()
        assertThat(beginningFragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(beginningFragment.requireView().alpha).isWithin(0f).of(1f)
        assertThat(beginningFragment.requireView().isAttachedToWindow).isTrue()
    }

    // Make sure that when saving the state during a postponed transaction that it saves
    // the state as if it wasn't postponed.
    @Test
    fun saveWhilePostponed() {
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = PostponedFragment1()
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fc2 = fc1.restart(activityRule, viewModelStore)

        val fm2 = fc2.supportFragmentManager
        val fragment2 = fm2.findFragmentByTag("1")!!
        assertThat(fragment2).isNotNull()
        assertThat(fragment2.requireView()).isNotNull()
        assertThat(fragment2.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment2.requireView().alpha).isWithin(0f).of(1f)
        assertThat(fragment2.isResumed).isTrue()
        assertThat(fragment2.isAdded).isTrue()
        assertThat(fragment2.requireView().isAttachedToWindow).isTrue()

        instrumentation.runOnMainSync { assertThat(fm2.popBackStackImmediate()).isTrue() }

        assertThat(fragment2.isResumed).isFalse()
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fragment2.view).isNull()
    }

    // Ensure that the postponed fragment transactions don't allow reentrancy in fragment manager
    @Test
    fun postponeDoesNotAllowReentrancy() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment = CommitNowFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()

        // should be postponed now
        assertPostponedTransition(beginningFragment, fragment)

        activityRule.runOnUiThread {
            // start the postponed transition
            fragment.startPostponedEnterTransition()

            try {
                // This should trigger an IllegalStateException
                fm.executePendingTransactions()
                fail("commitNow() while executing a transaction should cause an" +
                        " IllegalStateException")
            } catch (e: IllegalStateException) {
                assertThat(e)
                    .hasMessageThat().contains("FragmentManager is already executing transactions")
            }
        }
    }

    // Ensure startPostponedEnterTransaction is called after the timeout expires
    @Test
    fun testTimedPostpone() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment = PostponedFragment3()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertWithMessage("Fragment should be postponed")
            .that(fragment.isPostponed).isTrue()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(1)

        assertPostponedTransition(beginningFragment, fragment)

        fragment.waitForTransition()

        assertWithMessage("After startPostponed is called the transition should not be postponed")
            .that(fragment.isPostponed).isFalse()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(0)
    }

    // Ensure that if startPostponedEnterTransaction is called before the timeout, there is no crash
    @Test
    fun testTimedPostponeStartPostponedCalledTwice() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment = PostponedFragment3()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertWithMessage("Fragment should be postponed")
            .that(fragment.isPostponed).isTrue()

        fragment.startPostponedEnterTransition()

        assertForwardTransition(beginningFragment, fragment)

        assertWithMessage("After startPostponed is called the transition should not be postponed")
            .that(fragment.isPostponed).isFalse()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(0)
    }

    // Ensure postponedEnterTransaction(long, TimeUnit) works even if called in constructor
    @Test
    fun testTimedPostponeCalledInConstructor() {
        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.activity.findViewById<View>(R.id.blueSquare)

        val fragment = PostponedConstructorFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertWithMessage("Fragment should be postponed")
            .that(fragment.isPostponed).isTrue()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(1)

        assertPostponedTransition(beginningFragment, fragment)

        fragment.waitForTransition()

        assertWithMessage("After startPostponed is called the transition should not be postponed")
            .that(fragment.isPostponed).isFalse()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(0)
    }

    private fun assertPostponedTransition(
        fromFragment: TransitionFragment,
        toFragment: TransitionFragment,
        removedFragment: TransitionFragment? = null
    ) {
        if (removedFragment != null) {
            assertThat(removedFragment.view).isNull()
            assureNoTransition(removedFragment)
        }

        toFragment.waitForNoTransition()
        assertThat(fromFragment.view).isNotNull()
        assertThat(toFragment.view).isNotNull()
        assertThat(fromFragment.requireView().isAttachedToWindow).isTrue()
        assertThat(toFragment.requireView().isAttachedToWindow).isTrue()
        assertThat(fromFragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(toFragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(toFragment.requireView().alpha).isWithin(0f).of(0f)
        assureNoTransition(fromFragment)
        assureNoTransition(toFragment)
        assertThat(fromFragment.isResumed).isTrue()
        assertThat(toFragment.isResumed).isFalse()
    }

    private fun clearTargets(fragment: TransitionFragment) {
        fragment.enterTransition.targets.clear()
        fragment.reenterTransition.targets.clear()
        fragment.exitTransition.targets.clear()
        fragment.returnTransition.targets.clear()
        fragment.sharedElementEnter.targets.clear()
        fragment.sharedElementReturn.targets.clear()
    }

    private fun assureNoTransition(fragment: TransitionFragment) {
        assertThat(fragment.enterTransition.targets.size).isEqualTo(0)
        assertThat(fragment.reenterTransition.targets.size).isEqualTo(0)
        assertThat(fragment.enterTransition.targets.size).isEqualTo(0)
        assertThat(fragment.returnTransition.targets.size).isEqualTo(0)
        assertThat(fragment.sharedElementEnter.targets.size).isEqualTo(0)
        assertThat(fragment.sharedElementReturn.targets.size).isEqualTo(0)
    }

    private fun assertForwardTransition(start: TransitionFragment, end: TransitionFragment) {
        start.waitForTransition()
        end.waitForTransition()
        assertThat(start.enterTransition.targets.size).isEqualTo(0)
        assertThat(end.enterTransition.targets.size).isEqualTo(1)

        assertThat(start.reenterTransition.targets.size).isEqualTo(0)
        assertThat(end.reenterTransition.targets.size).isEqualTo(0)

        assertThat(start.returnTransition.targets.size).isEqualTo(0)
        assertThat(end.returnTransition.targets.size).isEqualTo(0)

        assertThat(start.exitTransition.targets.size).isEqualTo(1)
        assertThat(end.exitTransition.targets.size).isEqualTo(0)

        assertThat(start.sharedElementEnter.targets.size).isEqualTo(0)
        assertThat(end.sharedElementEnter.targets.size).isEqualTo(2)

        assertThat(start.sharedElementReturn.targets.size).isEqualTo(0)
        assertThat(end.sharedElementReturn.targets.size).isEqualTo(0)

        val blue = end.requireView().findViewById<View>(R.id.blueSquare)
        assertThat(end.sharedElementEnter.targets.contains(blue)).isTrue()
        assertThat(end.sharedElementEnter.targets[0].transitionName).isEqualTo("blueSquare")
        assertThat(end.sharedElementEnter.targets[1].transitionName).isEqualTo("blueSquare")

        assertNoTargets(start)
        assertNoTargets(end)

        clearTargets(start)
        clearTargets(end)
    }

    private fun assertBackTransition(start: TransitionFragment, end: TransitionFragment) {
        start.waitForTransition()
        end.waitForTransition()
        assertThat(end.reenterTransition.targets.size).isEqualTo(1)
        assertThat(start.reenterTransition.targets.size).isEqualTo(0)

        assertThat(end.returnTransition.targets.size).isEqualTo(0)
        assertThat(start.returnTransition.targets.size).isEqualTo(1)

        assertThat(start.enterTransition.targets.size).isEqualTo(0)
        assertThat(end.enterTransition.targets.size).isEqualTo(0)

        assertThat(start.exitTransition.targets.size).isEqualTo(0)
        assertThat(end.exitTransition.targets.size).isEqualTo(0)

        assertThat(start.sharedElementEnter.targets.size).isEqualTo(0)
        assertThat(end.sharedElementEnter.targets.size).isEqualTo(0)

        assertThat(start.sharedElementReturn.targets.size).isEqualTo(2)
        assertThat(end.sharedElementReturn.targets.size).isEqualTo(0)

        val blue = end.requireView().findViewById<View>(R.id.blueSquare)
        assertThat(start.sharedElementReturn.targets.contains(blue)).isTrue()
        assertThat(start.sharedElementReturn.targets[0].transitionName).isEqualTo("blueSquare")
        assertThat(start.sharedElementReturn.targets[1].transitionName).isEqualTo("blueSquare")

        assertNoTargets(end)
        assertNoTargets(start)

        clearTargets(start)
        clearTargets(end)
    }

    private fun assertNoTargets(fragment: TransitionFragment) {
        assertThat(fragment.enterTransition.getTargets().isEmpty()).isTrue()
        assertThat(fragment.reenterTransition.getTargets().isEmpty()).isTrue()
        assertThat(fragment.exitTransition.getTargets().isEmpty()).isTrue()
        assertThat(fragment.returnTransition.getTargets().isEmpty()).isTrue()
        assertThat(fragment.sharedElementEnter.getTargets().isEmpty()).isTrue()
        assertThat(fragment.sharedElementReturn.getTargets().isEmpty()).isTrue()
    }

    open class PostponedFragment1 : TransitionFragment(R.layout.scene1) {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition()
        }
    }

    class PostponedFragment2 : TransitionFragment(R.layout.scene2) {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition()
        }
    }

    class PostponedFragment3 : TransitionFragment(R.layout.scene2) {
        val startPostponedCountDownLatch = CountDownLatch(1)
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition(1000, TimeUnit.MILLISECONDS)
        }

        override fun startPostponedEnterTransition() {
            super.startPostponedEnterTransition()
            startPostponedCountDownLatch.countDown()
        }
    }

    class PostponedConstructorFragment : TransitionFragment(R.layout.scene2) {

        init {
            postponeEnterTransition(1000, TimeUnit.MILLISECONDS)
        }

        val startPostponedCountDownLatch = CountDownLatch(1)

        override fun startPostponedEnterTransition() {
            super.startPostponedEnterTransition()
            startPostponedCountDownLatch.countDown()
        }
    }

    class CommitNowFragment : PostponedFragment1() {
        override fun onResume() {
            super.onResume()
            // This should throw because this happens during the execution
            fragmentManager!!.beginTransaction()
                .add(R.id.fragmentContainer, PostponedFragment1())
                .commitNow()
        }
    }
}
