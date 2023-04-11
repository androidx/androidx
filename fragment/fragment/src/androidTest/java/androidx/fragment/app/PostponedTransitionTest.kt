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

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.runOnUiThreadRethrow
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.rules.RuleChain

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class PostponedTransitionTest() {

    @Suppress("DEPRECATION")
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess())
        .around(activityRule)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun setupContainer(beginningFragment: TransitionFragment = PostponedFragment1()) {
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

        assertWithMessage("Timed out waiting for OnBackStackChangedListener callback")
            .that(backStackLatch.await(1, TimeUnit.SECONDS))
            .isTrue()

        beginningFragment.startPostponedEnterTransition()
        beginningFragment.waitForTransition()
        val blueSquare = activityRule.findBlue()
        val greenSquare = activityRule.findGreen()
        beginningFragment.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(blueSquare, greenSquare)
        }
        verifyNoOtherTransitions(beginningFragment)
    }

    // Ensure that replacing with a fragment that has a postponed transition
    // will properly postpone it, both adding and popping.
    @Test
    fun replaceTransition() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

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
        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment
        )

        val startBlue2 = activityRule.findBlue()
        val startGreen2 = activityRule.findGreen()
        val startBlueBounds2 = startBlue2.boundsOnScreen

        activityRule.popBackStackImmediate()

        // should be postponed going back, too
        assertPostponedTransition(fragment, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(
            startBlue2, startBlueBounds2, startGreen2,
            fragment, beginningFragment
        )
    }

    @Test
    fun changePostponedFragmentVisibility() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = PostponedFragment1()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            fragment.requireView().visibility = View.INVISIBLE
        }

        activityRule.waitForExecution(1)

        // should be postponed now
        assertPostponedTransition(beginningFragment, fragment, toFragmentVisible = false)

        // should be invisible
        assertThat(fragment.requireView().visibility).isEqualTo(View.INVISIBLE)

        // start the postponed transition
        fragment.startPostponedEnterTransition()

        // nothing should run since the fragment is INVISIBLE
        verifyNoOtherTransitions(beginningFragment)
        verifyNoOtherTransitions(fragment)
    }

    // Ensure that replacing a fragment doesn't cause problems with the back stack nesting level
    @Test
    fun backStackNestingLevel() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

        val fragment1 = TransitionFragment(R.layout.scene2)
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        // make sure transition ran
        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment1
        )

        val endBlue = activityRule.findBlue()
        val endGreen = activityRule.findGreen()
        val endBlueBounds = endBlue.boundsOnScreen

        activityRule.popBackStackImmediate()

        // should be postponed going back
        assertPostponedTransition(fragment1, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(endBlue, endBlueBounds, endGreen, fragment1, beginningFragment)

        val startBlue2 = activityRule.findBlue()
        val startGreen2 = activityRule.findGreen()
        val startBlueBounds2 = startBlue2.boundsOnScreen

        val fragment2 = TransitionFragment(R.layout.scene2)
        fm.beginTransaction()
            .addSharedElement(startBlue2, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        // make sure transition ran
        assertForwardTransition(
            startBlue2, startBlueBounds2, startGreen2,
            beginningFragment, fragment2
        )

        val endBlue2 = activityRule.findBlue()
        val endGreen2 = activityRule.findGreen()
        val endBlueBounds2 = endBlue2.boundsOnScreen

        activityRule.popBackStackImmediate()

        // should be postponed going back
        assertPostponedTransition(fragment2, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(endBlue2, endBlueBounds2, endGreen2, fragment2, beginningFragment)
    }

    // Ensure that postponed transition is forced after another has been committed.
    // This tests when the transactions are executed together
    @Test
    fun forcedTransition1() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

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

        // fragment2 should have been put on the back stack without any transitions
        verifyNoOtherTransitions(fragment2)

        // fragment3 should be postponed
        assertPostponedTransition(beginningFragment, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment3
        )

        val startBlue3 = fragment3.requireView().findViewById<View>(R.id.blueSquare)
        val startGreen3 = fragment3.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds3 = startBlue3.boundsOnScreen

        activityRule.popBackStackImmediate(commit, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // The transition back to beginningFragment should be postponed
        // and fragment2 should be removed without any transitions
        assertPostponedTransition(fragment3, beginningFragment, fragment2)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(
            startBlue3, startBlueBounds3, startGreen3,
            fragment3, beginningFragment
        )
    }

    // Ensure that postponed transition is forced after another has been committed.
    // This tests when the transactions are processed separately.
    @Test
    fun forcedTransition2() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

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

        // fragment2 should have been put on the back stack without any transitions
        verifyNoOtherTransitions(fragment2)

        // fragment3 should be postponed
        assertPostponedTransition(beginningFragment, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment3
        )

        val startBlue3 = fragment3.requireView().findViewById<View>(R.id.blueSquare)
        val startGreen3 = fragment3.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds3 = startBlue3.boundsOnScreen

        // Pop back to fragment2, but it should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment3, fragment2)

        // Pop to beginningFragment -- should cancel the fragment2 transition and
        // start the beginningFragment transaction postponed
        activityRule.popBackStackImmediate()

        // The transition back to beginningFragment should be postponed
        // and no transitions should be done on fragment2
        assertPostponedTransition(fragment3, beginningFragment)
        verifyNoOtherTransitions(fragment2)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(
            startBlue3, startBlueBounds3, startGreen3,
            fragment3, beginningFragment
        )
        verifyNoOtherTransitions(fragment2)
    }

    // Do a bunch of things to one fragment in a transaction and see if it can screw things up.
    @Test
    fun extensiveTransition() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

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
        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment2
        )

        val startBlue2 = activityRule.findBlue()
        val startGreen2 = activityRule.findGreen()
        val startBlueBounds2 = startBlue2.boundsOnScreen

        // Pop back to fragment2, but it should be postponed
        activityRule.popBackStackImmediate()

        assertPostponedTransition(fragment2, beginningFragment)

        // start the postponed transition
        beginningFragment.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(
            startBlue2, startBlueBounds2, startGreen2,
            fragment2, beginningFragment
        )
    }

    // Execute transactions on different containers and ensure that they don't conflict
    @Test
    fun differentContainers() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

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
        val startGreen1 = fragment1.requireView().findViewById<View>(R.id.greenSquare)
        val startGreen2 = fragment2.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds1 = startBlue1.boundsOnScreen
        val startBlueBounds2 = startBlue2.boundsOnScreen

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
        assertForwardTransition(startBlue1, startBlueBounds1, startGreen1, fragment1, fragment3)
        assertPostponedTransition(fragment2, fragment4)

        // start the postponed transition
        fragment4.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(startBlue2, startBlueBounds2, startGreen2, fragment2, fragment4)

        val startBlue3 = fragment3.requireView().findViewById<View>(R.id.blueSquare)
        val startBlue4 = fragment4.requireView().findViewById<View>(R.id.blueSquare)
        val startGreen3 = fragment3.requireView().findViewById<View>(R.id.greenSquare)
        val startGreen4 = fragment4.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds3 = startBlue3.boundsOnScreen
        val startBlueBounds4 = startBlue4.boundsOnScreen

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
        assertBackTransition(startBlue4, startBlueBounds4, startGreen4, fragment4, fragment2)

        // but not the postponed one
        assertPostponedTransition(fragment3, fragment1)

        // start the postponed transition
        fragment1.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(startBlue3, startBlueBounds3, startGreen3, fragment3, fragment1)
    }

    // Execute transactions on different containers and ensure that they don't conflict.
    // The postponement can be started out-of-order
    @Test
    fun outOfOrderContainers() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

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
        val startGreen1 = fragment1.requireView().findViewById<View>(R.id.greenSquare)
        val startGreen2 = fragment2.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds1 = startBlue1.boundsOnScreen
        val startBlueBounds2 = startBlue2.boundsOnScreen

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
        assertForwardTransition(startBlue2, startBlueBounds2, startGreen2, fragment2, fragment4)
        assertPostponedTransition(fragment1, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(startBlue1, startBlueBounds1, startGreen1, fragment1, fragment3)

        val startBlue3 = fragment3.requireView().findViewById<View>(R.id.blueSquare)
        val startBlue4 = fragment4.requireView().findViewById<View>(R.id.blueSquare)
        val startGreen3 = fragment3.requireView().findViewById<View>(R.id.greenSquare)
        val startGreen4 = fragment4.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds3 = startBlue3.boundsOnScreen
        val startBlueBounds4 = startBlue4.boundsOnScreen

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
        assertBackTransition(startBlue3, startBlueBounds3, startGreen3, fragment3, fragment1)

        // but not the postponed one
        assertPostponedTransition(fragment4, fragment2)

        // start the postponed transition
        fragment2.startPostponedEnterTransition()

        // make sure it ran
        assertBackTransition(startBlue4, startBlueBounds4, startGreen4, fragment4, fragment2)
    }

    // Make sure that commitNow for a transaction on a different fragment container doesn't
    // affect the postponed transaction
    @Test
    fun commitNowNoEffect() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

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
        val startGreen1 = fragment1.requireView().findViewById<View>(R.id.greenSquare)
        val startGreen2 = fragment2.requireView().findViewById<View>(R.id.greenSquare)
        val startBlueBounds1 = startBlue1.boundsOnScreen
        val startBlueBounds2 = startBlue2.boundsOnScreen

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
        assertForwardTransition(startBlue2, startBlueBounds2, startGreen2, fragment2, fragment4)
        assertPostponedTransition(fragment1, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        // make sure it ran
        assertForwardTransition(startBlue1, startBlueBounds1, startGreen1, fragment1, fragment3)
    }

    // Make sure that commitNow for a transaction affecting a postponed fragment in the same
    // container forces the postponed transition to start.
    @Test
    fun commitNowStartsPostponed() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

        val fragment2 = PostponedFragment2()
        val fragment3 = PostponedFragment1()

        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val startBlue2 = fragment2.requireView().findViewById<View>(R.id.blueSquare)

        instrumentation.runOnMainSync {
            fm.beginTransaction()
                .addSharedElement(startBlue2, "blueSquare")
                .replace(R.id.fragmentContainer, fragment3)
                .setReorderingAllowed(true)
                .commitNow()
        }

        // fragment2 should have been put on the back stack without any transitions
        verifyNoOtherTransitions(fragment2)

        // fragment3 should be postponed
        assertPostponedTransition(beginningFragment, fragment3)

        // start the postponed transition
        fragment3.startPostponedEnterTransition()

        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment3
        )
    }

    // Make sure that when a transaction that removes a view is postponed that
    // another transaction doesn't accidentally remove the view early.
    @Test
    fun noAccidentalRemoval() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

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
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

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

        verifyNoOtherTransitions(fragment)
        verifyNoOtherTransitions(beginningFragment)

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
        setupContainer(PostponedFragment1())

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
        activityRule.runOnUiThread {
            assertThat(fragment2.view).isNull()
        }
    }

    // Ensure that the postponed fragment transactions don't allow reentrancy in fragment manager
    @Test
    fun postponeDoesNotAllowReentrancy() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

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
            // This should succeed since onResume() is called outside of the
            // transaction completing
            fm.executePendingTransactions()
        }
    }

    // Ensure startPostponedEnterTransaction is called after the timeout expires
    @Test
    fun testTimedPostpone() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = PostponedFragment3(1000)
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

    @Test
    fun testTimedPostponeNoLeak() {
        val beginningFragment = PostponedFragment3(100000)
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = TransitionFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()
    }

    @Test
    fun testTimedPostponeBeforeAttachedNoLeak() {
        val beginningFragment = PostponedConstructorFragment(100000)
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = TransitionFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()
    }

    @Test
    fun testTimedPostponeTwiceBeforeAttachedNoLeak() {
        val beginningFragment = PostponedConstructorFragment(100000)
        beginningFragment.postponeEnterTransition(1, TimeUnit.HOURS)
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = TransitionFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()
    }

    @Test
    fun testTimedPostponeBeforeAttachedAndAfterAttachedNoLeak() {
        val beginningFragment = PostponedConstructorAndAttachFragment(100000)
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = TransitionFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()
    }

    @Test
    fun testTimedPostponeStartOnTestThreadNoLeak() {
        val beginningFragment = PostponedFragment3(100000)
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = TransitionFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        beginningFragment.startPostponedEnterTransition()
    }

    @Test
    fun testTimedPostponeStartOnMainThreadNoLeak() {
        val beginningFragment = PostponedFragment3(100000)
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = TransitionFragment()
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            beginningFragment.startPostponedEnterTransition()
        }
    }

    // Ensure that if startPostponedEnterTransaction is called before the timeout, there is no crash
    @Test
    fun testTimedPostponeStartPostponedCalledTwice() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

        val fragment = PostponedFragment3(1000)
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

        assertForwardTransition(
            startBlue, startBlueBounds, startGreen,
            beginningFragment, fragment
        )

        assertWithMessage("After startPostponed is called the transition should not be postponed")
            .that(fragment.isPostponed).isFalse()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(0)
    }

    // Ensure that if postponedEnterTransition is called twice the first one is removed.
    @Test
    fun testTimedPostponeEnterPostponedCalledTwice() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = PostponedFragment3(1000)
        fragment.startPostponedCountDownLatch = CountDownLatch(2)
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        fragment.postponeEnterTransition(1000, TimeUnit.MILLISECONDS)

        activityRule.waitForExecution()

        assertWithMessage("Fragment should be postponed")
            .that(fragment.isPostponed).isTrue()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(2)

        assertPostponedTransition(beginningFragment, fragment)

        fragment.waitForTransition()

        assertWithMessage(
            "After startPostponed is called the transition should not be postponed"
        )
            .that(fragment.isPostponed).isFalse()

        assertThat(fragment.startPostponedCountDownLatch.count).isEqualTo(1)
    }

    // Ensure that if postponedEnterTransition with a duration of 0 it waits one frame.
    @Test
    fun testTimedPostponeEnterPostponedCalledWithZero() {
        setupContainer(PostponedFragment1())

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

        val fragment = PostponedFragment3(0)
        fm.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        assertThat(
            fragment.onCreateViewCountLatch.await(250, TimeUnit.MILLISECONDS)
        ).isTrue()

        activityRule.waitForExecution(1)

        assertWithMessage(
            "After startPostponed is called the transition should not be postponed"
        ).that(fragment.isPostponed).isFalse()

        fragment.waitForTransition()
    }

    // Ensure postponedEnterTransaction(long, TimeUnit) works even if called in constructor
    @Test
    fun testTimedPostponeCalledInConstructor() {
        val beginningFragment = PostponedFragment1()
        setupContainer(beginningFragment)

        val fm = activityRule.activity.supportFragmentManager
        val startBlue = activityRule.findBlue()

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
        removedFragment: TransitionFragment? = null,
        toFragmentVisible: Boolean = true
    ) {
        if (removedFragment != null) {
            assertThat(removedFragment.view).isNull()
            verifyNoOtherTransitions(removedFragment)
        }

        toFragment.waitForNoTransition()
        assertThat(fromFragment.view).isNotNull()
        assertThat(toFragment.view).isNotNull()
        assertThat(fromFragment.requireView().isAttachedToWindow).isTrue()
        assertThat(toFragment.requireView().isAttachedToWindow).isTrue()
        assertThat(fromFragment.requireView().visibility).isEqualTo(View.VISIBLE)
        if (toFragmentVisible) {
            assertThat(toFragment.requireView().visibility).isEqualTo(View.VISIBLE)
        } else {
            assertThat(toFragment.requireView().visibility).isEqualTo(View.INVISIBLE)
        }
        assertThat(toFragment.requireView().alpha).isWithin(0f).of(0f)

        verifyNoOtherTransitions(fromFragment)
        verifyNoOtherTransitions(toFragment)
        assertThat(fromFragment.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(toFragment.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
    }

    private fun clearTargets(fragment: TransitionFragment) {
        fragment.enterTransition.clearTargets()
        fragment.reenterTransition.clearTargets()
        fragment.exitTransition.clearTargets()
        fragment.returnTransition.clearTargets()
        fragment.sharedElementEnter.clearTargets()
        fragment.sharedElementReturn.clearTargets()
    }

    private fun assertForwardTransition(
        sharedElement: View,
        sharedElementBounds: Rect,
        transitioningView: View,
        start: TransitionFragment,
        end: TransitionFragment
    ) {
        start.waitForTransition()
        end.waitForTransition()

        val endSharedElement = end.requireView().findViewById<View>(R.id.blueSquare)
        val endTransitioningView = end.requireView().findViewById<View>(R.id.greenSquare)
        val endSharedElementBounds = endSharedElement.boundsOnScreen

        end.enterTransition.verifyAndClearTransition {
            epicenter = endSharedElementBounds
            enteringViews += endTransitioningView
        }

        start.exitTransition.verifyAndClearTransition {
            epicenter = sharedElementBounds
            exitingViews += transitioningView
        }

        end.sharedElementEnter.verifyAndClearTransition {
            epicenter = sharedElementBounds
            exitingViews += sharedElement
            enteringViews += endSharedElement
        }

        // This checks need to be done on the mainThread to ensure that the shared element draw is
        // complete. If these checks are not on the mainThread, there is a chance that this gets
        // checked during OnShotPreDrawListener.onPreDraw, causing the name assert to fail.
        activityRule.runOnUiThread {
            assertThat(sharedElement.transitionName).isEqualTo("blueSquare")
            assertThat(endSharedElement.transitionName).isEqualTo("blueSquare")
        }

        verifyNoOtherTransitions(start)
        verifyNoOtherTransitions(end)

        assertNoTargets(start)
        assertNoTargets(end)
    }

    private fun assertBackTransition(
        sharedElement: View,
        sharedElementBounds: Rect,
        transitionView: View,
        start: TransitionFragment,
        end: TransitionFragment
    ) {
        start.waitForTransition()
        end.waitForTransition()

        val endSharedElement = end.requireView().findViewById<View>(R.id.blueSquare)
        val endTransitioningView = end.requireView().findViewById<View>(R.id.greenSquare)
        val endSharedElementBounds = endSharedElement.boundsOnScreen

        end.reenterTransition.verifyAndClearTransition {
            epicenter = endSharedElementBounds
            enteringViews += endTransitioningView
        }

        start.returnTransition.verifyAndClearTransition {
            epicenter = sharedElementBounds
            exitingViews += transitionView
        }

        start.sharedElementReturn.verifyAndClearTransition {
            epicenter = sharedElementBounds
            exitingViews += sharedElement
            enteringViews += endSharedElement
        }

        // This checks need to be done on the mainThread to ensure that the shared element draw is
        // complete. If these checks are not on the mainThread, there is a chance that this gets
        // checked during OnShotPreDrawListener.onPreDraw, causing the name assert to fail.
        activityRule.runOnUiThreadRethrow {
            assertThat(sharedElement.transitionName).isEqualTo("blueSquare")
            assertThat(endSharedElement.transitionName).isEqualTo("blueSquare")
        }

        verifyNoOtherTransitions(start)
        verifyNoOtherTransitions(end)

        assertNoTargets(end)
        assertNoTargets(start)
    }

    private fun assertNoTargets(fragment: TransitionFragment) {
        assertThat(fragment.enterTransition.targets).isEmpty()
        assertThat(fragment.reenterTransition.targets).isEmpty()
        assertThat(fragment.exitTransition.targets).isEmpty()
        assertThat(fragment.returnTransition.targets).isEmpty()
        assertThat(fragment.sharedElementEnter.targets).isEmpty()
        assertThat(fragment.sharedElementReturn.targets).isEmpty()
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

    class PostponedFragment3(val duration: Long) : TransitionFragment(R.layout.scene2) {
        var startPostponedCountDownLatch = CountDownLatch(1)
        val onCreateViewCountLatch = CountDownLatch(1)
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition(duration, TimeUnit.MILLISECONDS)
            onCreateViewCountLatch.countDown()
        }

        override fun startPostponedEnterTransition() {
            super.startPostponedEnterTransition()
            startPostponedCountDownLatch.countDown()
        }
    }

    class PostponedConstructorFragment(duration: Long = 1000) :
        TransitionFragment(R.layout.scene2) {

        init {
            postponeEnterTransition(duration, TimeUnit.MILLISECONDS)
        }

        val startPostponedCountDownLatch = CountDownLatch(1)

        override fun startPostponedEnterTransition() {
            super.startPostponedEnterTransition()
            startPostponedCountDownLatch.countDown()
        }
    }

    class PostponedConstructorAndAttachFragment(private val duration: Long = 1000) :
        TransitionFragment(R.layout.scene2) {

        init {
            postponeEnterTransition(duration, TimeUnit.MILLISECONDS)
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            postponeEnterTransition(duration, TimeUnit.MILLISECONDS)
        }
    }

    class CommitNowFragment : PostponedFragment1() {
        override fun onResume() {
            super.onResume()
            // This should throw because this happens during the execution
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, PostponedFragment1())
                .commitNow()
        }
    }
}
