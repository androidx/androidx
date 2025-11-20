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

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.Resources
import android.os.Build
import android.view.Choreographer
import android.view.View
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.annotation.AnimatorRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentAnimatorTest {

    @Suppress("DEPRECATION")
    val activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.simple_container)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators
    @Test
    fun addAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertEnterPopExit(fragment)
    }

    // Ensure Fragments using default transits make it to resumed
    @Test
    fun defaultTransitionAddReorderedTrue() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment.resumeLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(fragment.mView.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.mView.alpha).isEqualTo(1f)
    }

    // Ensure Fragments using default transits make it to resumed
    @Test
    fun defaultTransitionAddReorderedFalse() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(false)
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment.resumeLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(fragment.mView.visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.mView.alpha).isEqualTo(1f)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators
    @Test
    fun removeAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertExitPopEnter(fragment)
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    // This tests reordered transactions
    @Test
    fun showAnimatorsReordered() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit()
        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .show(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        }

        assertEnterPopExit(fragment)

        val layoutCountDownLatch = CountDownLatch(1)

        activityRule.runOnUiThread {
            Choreographer.getInstance().postFrameCallback { layoutCountDownLatch.countDown() }
        }

        assertThat(layoutCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    // This tests ordered transactions
    @Test
    fun showAnimatorsOrdered() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .hide(fragment)
            .setReorderingAllowed(false)
            .commit()
        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .show(fragment)
            .setReorderingAllowed(false)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        }

        assertEnterPopExit(fragment)
        val postFrameCountDownLatch = CountDownLatch(1)

        activityRule.runOnUiThread {
            Choreographer.getInstance().postFrameCallback { postFrameCountDownLatch.countDown() }
        }

        assertThat(postFrameCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            assertThat(fragment.requireView().visibility).isEqualTo(View.GONE)
        }
    }

    // Ensure that hiding and popping a Fragment uses the exit and popEnter animators
    @Test
    fun hideAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .hide(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertExitPopEnter(fragment)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun hideAnimatorsPredictiveBackCancelRepeat() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = AnimatorFragment(R.layout.scene1)
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "1").commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment(R.layout.scene1)
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment2, "2")
            .hide(fragment1)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }

        if (FragmentManager.USE_PREDICTIVE_BACK) {
            assertThat(fragment1.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment2.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment1.inProgress).isTrue()
            assertThat(fragment2.inProgress).isTrue()
        } else {
            assertThat(fragment1.inProgress).isFalse()
            assertThat(fragment1.inProgress).isFalse()
        }

        activityRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }

        if (FragmentManager.USE_PREDICTIVE_BACK) {
            assertThat(fragment1.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment2.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment1.inProgress).isTrue()
            assertThat(fragment2.inProgress).isTrue()
        } else {
            assertThat(fragment1.inProgress).isFalse()
            assertThat(fragment1.inProgress).isFalse()
        }

        activityRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
    }

    // Ensure a hide animation is canceled if fragment is shown before it happens
    @Test
    fun hideAndShowNoAnimator() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = AnimatorFragment()
        val fragment2 = AnimatorFragment()

        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .hide(fragment2)
            .commit()
        activityRule.executePendingTransactions(fm)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT)
            .show(fragment2)
            .hide(fragment1)
            .commit()
        activityRule.executePendingTransactions(fm)

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT)
            .show(fragment1)
            .hide(fragment2)
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.isVisible).isTrue()
        assertFragmentAnimation(fragment2, 2, false, EXIT)
        assertThat(fragment2.isVisible).isFalse()
    }

    // Ensure that attaching and popping a Fragment uses the enter and popExit animators
    @Test
    fun attachAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment)
            .detach(fragment)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .attach(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertEnterPopExit(fragment)
    }

    // Ensure that detaching and popping a Fragment uses the exit and popEnter animators
    @Test
    fun detachAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .detach(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertExitPopEnter(fragment)
    }

    // Replace should exit the existing fragments and enter the added fragment, then
    // popping should popExit the removed fragment and popEnter the added fragments
    @Test
    fun replaceAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        // One fragment with a view
        val fragment1 = AnimatorFragment()
        val fragment2 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fragment3 = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment1, 1, false, EXIT)
        assertFragmentAnimation(fragment2, 1, false, EXIT)
        assertFragmentAnimation(fragment3, 1, true, ENTER)

        fm.popBackStack()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment3, 2, false, POP_EXIT)
        val replacement1 = fm.findFragmentByTag("1") as AnimatorFragment
        val replacement2 = fm.findFragmentByTag("1") as AnimatorFragment
        val expectedAnimations = if (replacement1 === fragment1) 2 else 1
        assertFragmentAnimation(replacement1, expectedAnimations, true, POP_ENTER)
        assertFragmentAnimation(replacement2, expectedAnimations, true, POP_ENTER)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedAddAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fragment.postponeEnterTransition()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertPostponed(fragment, 0)
        fragment.startPostponedEnterTransition()

        activityRule.waitForExecution()
        assertEnterPopExit(fragment)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedRemoveAnimators() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        assertExitPostponedPopEnter(fragment)
    }

    // Ensure that adding and popping a Fragment is postponed in both directions
    // when the fragments have been marked for postponing.
    @Test
    fun postponedAddRemove() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponed(fragment2, 0)
        assertThat(fragment1.requireView()).isNotNull()
        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment1.requireView().alpha).isWithin(0f).of(1f)
        assertThat(fragment1.requireView().isAttachedToWindow()).isTrue()

        fragment2.startPostponedEnterTransition()
        activityRule.waitForExecution()

        assertExitPostponedPopEnter(fragment1)
    }

    // Popping a postponed transaction should result in no animators
    @Test
    fun popPostponed() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        assertThat(fragment1.numStartedAnimators).isEqualTo(0)

        val fragment2 = AnimatorFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponed(fragment2, 0)

        // Now pop the postponed transaction
        activityRule.popBackStackImmediate()

        assertThat(fragment1.view).isNotNull()
        assertThat(fragment1.requireView().alpha).isWithin(0f).of(1f)
        assertThat(fragment1.requireView().isAttachedToWindow()).isTrue()
        assertThat(fragment1.isAdded).isTrue()

        assertThat(fragment2.view).isNull()
        assertThat(fragment2.isAdded).isFalse()

        assertThat(fragment1.numStartedAnimators).isEqualTo(0)
        assertThat(fragment2.numStartedAnimators).isEqualTo(0)
    }

    // Make sure that if the state was saved while a Fragment was animating that its
    // state is proper after restoring.
    @Test
    fun saveWhileAnimatingAway() {
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimatorFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.animator.slow_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.executePendingTransactions(fm1)

        fm1.popBackStack()

        activityRule.executePendingTransactions(fm1)
        // ensure the animation was started
        assertThat(fragment2.wasStarted).isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null) // fragmentManager does not know about animating fragment
        // Only do this check if the animator is still going.
        if (fragment2.endLatch.count == 1L) {
            assertThat(fragment2.parentFragmentManager)
                .isEqualTo(fm1) // but the animating fragment knows the fragmentManager
        }

        val fc2 = fc1.restart(activityRule, viewModelStore)

        val fm2 = fc2.supportFragmentManager
        val fragment2restored = fm2.findFragmentByTag("2")
        assertThat(fragment2restored).isNull()

        val fragment1restored = fm2.findFragmentByTag("1")!!
        assertThat(fragment1restored).isNotNull()
        assertThat(fragment1restored.view).isNotNull()
    }

    // Test to ensure animators going when the FragmentManager is destroyed are cancelled
    @Test
    @UiThreadTest
    fun cancelAllAnimatorsWhenFragmentManagerDestroyed() {
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimatorFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.animator.slow_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.executePendingTransactions(fm1)

        fm1.popBackStack()

        activityRule.executePendingTransactions(fm1)
        // ensure the animation was started
        assertThat(fragment2.wasStarted).isTrue()

        fc1.shutdown(viewModelStore)

        assertThat(fragment2.endLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    // Ensures that when a Fragment that is animating away gets readded the state is properly
    // updated
    @Test
    @UiThreadTest // Needed in order to add a fragment during the animation, otherwise the
    // animation ends before the transaction is executed.
    fun reAddAnimatingAwayAnimatorFragment() {
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimatorFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.animator.slow_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.executePendingTransactions(fm1)

        fm1.popBackStack()

        activityRule.executePendingTransactions(fm1)
        // ensure the animation was started
        assertThat(fragment2.wasStarted).isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null) // fragmentManager does not know about animating fragment
        assertThat(fragment2.parentFragmentManager)
            .isEqualTo(fm1) // but the animating fragment knows the fragmentManager

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.animator.slow_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.isAdded).isTrue()
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun replaceOperationWithAnimatorsThenSystemBack() {
        val fm1 = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment(R.layout.scene1)
        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment()

        fm1.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out,
                android.R.animator.fade_in,
                android.R.animator.fade_out,
            )
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.wasStarted).isTrue()
        // We need to wait for the exit animation to end
        assertThat(fragment1.endLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }

        if (FragmentManager.USE_PREDICTIVE_BACK) {
            assertThat(fragment1.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment2.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment1.inProgress).isTrue()
            assertThat(fragment2.inProgress).isTrue()
        } else {
            assertThat(fragment1.inProgress).isFalse()
            assertThat(fragment1.inProgress).isFalse()
        }

        activityRule.runOnUiThread { dispatcher.onBackPressed() }

        assertThat(fragment2.wasStarted).isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null) // fragmentManager does not know about animating fragment

        // We need to wait for the exit animation to end
        assertThat(fragment2.endLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment1.requireView().parent).isNotNull()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun replaceOperationWithAnimatorsThenCancel() {
        val fm1 = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment(R.layout.scene1)
        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimatorFragment()

        fm1.beginTransaction()
            .setCustomAnimations(
                android.R.animator.fade_in,
                android.R.animator.fade_out,
                android.R.animator.fade_in,
                android.R.animator.fade_out,
            )
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.wasStarted).isTrue()
        // We need to wait for the exit animation to end
        assertThat(fragment1.endLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }

        fragment2.resumeLatch = CountDownLatch(1)

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }

        if (FragmentManager.USE_PREDICTIVE_BACK) {
            assertThat(fragment1.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment2.startLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(fragment1.inProgress).isTrue()
            assertThat(fragment2.inProgress).isTrue()
        } else {
            assertThat(fragment1.inProgress).isFalse()
            assertThat(fragment1.inProgress).isFalse()
        }

        activityRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }

        assertThat(fragment2.wasStarted).isTrue()
        // Now fragment1 should be animating away
        assertThat(fragment1.isAdded).isFalse()

        // Now fragment2 should be animating back in
        assertThat(fragment2.isAdded).isTrue()
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)

        assertThat(fragment2.resumeLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment2.view?.parent).isNotNull()

        assertThat(fragment1.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
        assertThat(fragment1.view).isNull()
    }

    private fun assertEnterPopExit(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, true, ENTER)

        val fm = activityRule.activity.supportFragmentManager
        fm.popBackStack()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment, 2, false, POP_EXIT)
    }

    private fun assertExitPopEnter(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        val fm = activityRule.activity.supportFragmentManager
        fm.popBackStack()
        activityRule.waitForExecution()

        val replacement = fm.findFragmentByTag("1") as AnimatorFragment

        val isSameFragment = replacement === fragment
        val expectedAnimators = if (isSameFragment) 2 else 1
        assertFragmentAnimation(replacement, expectedAnimators, true, POP_ENTER)
    }

    private fun assertExitPostponedPopEnter(fragment: AnimatorFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        fragment.postponeEnterTransition()
        activityRule.popBackStackImmediate()

        assertPostponed(fragment, 1)

        fragment.startPostponedEnterTransition()
        activityRule.waitForExecution()
        assertFragmentAnimation(fragment, 2, true, POP_ENTER)
    }

    private fun assertFragmentAnimation(
        fragment: AnimatorFragment,
        numAnimators: Int,
        isEnter: Boolean,
        animatorResourceId: Int,
    ) {
        assertThat(fragment.numStartedAnimators).isEqualTo(numAnimators)
        assertThat(fragment.baseEnter).isEqualTo(isEnter)
        assertThat(fragment.resourceId).isEqualTo(animatorResourceId)
        assertThat(fragment.baseAnimator).isNotNull()
        assertThat(fragment.wasStarted).isTrue()
        assertThat(fragment.endLatch.await(200, TimeUnit.MILLISECONDS)).isTrue()
    }

    private fun assertPostponed(fragment: AnimatorFragment, expectedAnimators: Int) {
        assertThat(fragment.onCreateViewCalled).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.requireView().alpha).isWithin(0f).of(0f)
        assertThat(fragment.numStartedAnimators).isEqualTo(expectedAnimators)
    }

    class AnimatorFragment(@LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment) :
        StrictViewFragment(contentLayoutId) {
        var numStartedAnimators: Int = 0
        lateinit var baseAnimator: Animator
        var baseEnter: Boolean = false
        var resourceId: Int = 0
        var wasStarted: Boolean = false
        lateinit var startLatch: CountDownLatch
        lateinit var endLatch: CountDownLatch
        var resumeLatch = CountDownLatch(1)
        var initialized: Boolean = false
        var inProgress = false

        override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
            if (nextAnim == 0) {
                return null
            }

            var animator: Animator? = null
            try {
                animator = AnimatorInflater.loadAnimator(context, nextAnim)
            } catch (e: Resources.NotFoundException) {}

            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1f).setDuration(1)
            }

            return animator?.apply {
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            wasStarted = true
                            inProgress = true
                            numStartedAnimators++
                            startLatch.countDown()
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            endLatch.countDown()
                            inProgress = false
                        }
                    }
                )
                wasStarted = false
                startLatch = CountDownLatch(1)
                endLatch = CountDownLatch(1)
                resourceId = nextAnim
                baseEnter = enter
                baseAnimator = this
                initialized = true
            }
        }

        override fun onResume() {
            super.onResume()
            resumeLatch.countDown()
        }
    }

    companion object {
        // These are pretend resource IDs for animators. We don't need real ones since we
        // load them by overriding onCreateAnimator
        @AnimatorRes private val ENTER = 1
        @AnimatorRes private val EXIT = 2
        @AnimatorRes private val POP_ENTER = 3
        @AnimatorRes private val POP_EXIT = 4
    }
}
