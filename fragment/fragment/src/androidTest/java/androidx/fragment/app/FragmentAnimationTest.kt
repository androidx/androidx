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

import android.app.Instrumentation
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.annotation.AnimRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.waitForExecution
import androidx.testutils.withActivity
import androidx.testutils.withUse
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
class FragmentAnimationTest {

    @Suppress("DEPRECATION")
    val activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    // Detect leaks BEFORE and AFTER activity is destroyed
    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(DetectLeaksAfterTestSuccess())
        .around(activityRule)

    private lateinit var instrumentation: Instrumentation

    @Before
    fun setupContainer() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        activityRule.setContentView(R.layout.simple_container)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators
    @Test
    fun addAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertEnterPopExit(fragment)
        assertThat(fragment.onResumeCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators
    @Test
    fun removeAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        activityRule.waitForExecution()
        assertThat(fragment.onResumeCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertExitPopEnter(fragment)
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    @Test
    fun showAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .show(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertEnterPopExit(fragment)
    }

    // Ensure that hiding and popping a Fragment uses the exit and popEnter animators
    @Test
    fun hideAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .hide(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertExitPopEnter(fragment)
    }

    // Ensure that attaching and popping a Fragment uses the enter and popExit animators
    @Test
    fun attachAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).detach(fragment).commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .attach(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertEnterPopExit(fragment)
    }

    // Ensure that detaching and popping a Fragment uses the exit and popEnter animators
    @Test
    fun detachAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .detach(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertExitPopEnter(fragment)
    }

    // Replace should exit the existing fragments and enter the added fragment, then
    // popping should popExit the removed fragment and popEnter the added fragments
    @Test
    fun replaceAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimationFragment()
        val fragment2 = AnimationFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .commit()
        activityRule.waitForExecution()
        assertThat(fragment1.onResumeCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(fragment2.onResumeCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        val fragment3 = AnimationFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(fragment3.onResumeCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        assertFragmentAnimation(fragment1, 1, false, EXIT)
        assertFragmentAnimation(fragment2, 1, false, EXIT)
        assertFragmentAnimation(fragment3, 1, true, ENTER)

        fm.popBackStack()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment3, 2, false, POP_EXIT)
        val replacement1 = fm.findFragmentByTag("1") as AnimationFragment?
        val replacement2 = fm.findFragmentByTag("1") as AnimationFragment?
        val expectedAnimations = if (replacement1 === fragment1) 2 else 1
        assertFragmentAnimation(replacement1!!, expectedAnimations, true, POP_ENTER)
        assertFragmentAnimation(replacement2!!, expectedAnimations, true, POP_ENTER)
    }

    // Ensure child view is not removed before parent view animates out.
    @Test
    fun removeParentWithAnimation() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val parent = AnimationFragment(R.layout.simple_container)
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        assertThat(
            parent.animationStartedCountDownLatch.await(1000, TimeUnit.MILLISECONDS)
        ).isTrue()

        assertFragmentAnimation(parent, 1, true, ENTER)

        val child = AnimationFragment()
        parent.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, child, "child")
            .commit()
        activityRule.executePendingTransactions(parent.childFragmentManager)

        val childContainer = child.mContainer
        val childView = child.requireView()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, AnimationFragment(), "other")
            .commit()
        activityRule.executePendingTransactions()

        assertThat(childContainer.findViewById<View>(childView.id)).isNotNull()
        assertThat(
            parent.animationStartedCountDownLatch.await(1000, TimeUnit.MILLISECONDS)
        ).isTrue()

        assertFragmentAnimation(parent, 2, false, EXIT)
    }

    // Ensure grandChild view is not removed before parent view animates out.
    @Test
    fun removeGrandParentWithAnimation() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val parent = AnimationFragment(R.layout.simple_container)
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        assertThat(
            parent.animationStartedCountDownLatch.await(1000, TimeUnit.MILLISECONDS)
        ).isTrue()

        assertFragmentAnimation(parent, 1, true, ENTER)

        val child = AnimationFragment(R.layout.simple_container)
        parent.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, child, "child")
            .commit()
        activityRule.executePendingTransactions(parent.childFragmentManager)

        val grandChild = AnimationFragment()
        child.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, grandChild, "grandChild")
            .commit()
        activityRule.executePendingTransactions(child.childFragmentManager)

        val childContainer = child.mContainer
        val childView = child.requireView()

        val grandChildContainer = grandChild.mContainer
        val grandChildView = grandChild.requireView()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, AnimationFragment(), "other")
            .commit()
        activityRule.executePendingTransactions()

        assertThat(childContainer.findViewById<View>(childView.id)).isNotNull()
        assertThat(grandChildContainer.findViewById<View>(grandChildView.id)).isNotNull()
        assertThat(
            parent.animationStartedCountDownLatch.await(1000, TimeUnit.MILLISECONDS)
        ).isTrue()

        assertFragmentAnimation(parent, 2, false, EXIT)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedAddAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
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
    fun postponedRemoveAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimationFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
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
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimationFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationFragment()
        fragment2.postponeEnterTransition()

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()

        activityRule.waitForExecution()

        assertPostponed(fragment2, 0)
        assertThat(fragment1.view).isNotNull()
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
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimationFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        assertThat(fragment1.numStartedAnimators).isEqualTo(0)

        val fragment2 = AnimationFragment()
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
        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
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
        waitForAnimationReady()
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimationListenerFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationListenerFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.anim.long_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        fm1.popBackStack()
        activityRule.executePendingTransactions(fm1)
        // ensure the animation was started
        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        // fragmentManager does not know about animating fragment
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)
        // but the animating fragment knows the fragmentManager
        assertThat(fragment2.parentFragmentManager).isEqualTo(fm1)

        val fc2 = fc1.restart(activityRule, viewModelStore)

        val fm2 = fc2.supportFragmentManager
        val fragment2restored = fm2.findFragmentByTag("2")
        assertThat(fragment2restored).isNull()

        val fragment1restored = fm2.findFragmentByTag("1")
        assertThat(fragment1restored).isNotNull()
        assertThat(fragment1restored!!.view).isNotNull()
    }

    // Test to ensure animations going when the FragmentManager is destroyed are cancelled
    @Test
    fun cancelAllAnimationsWhenFragmentManagerDestroyed() {
        waitForAnimationReady()
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimationListenerFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationListenerFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.anim.long_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        fm1.popBackStack()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        fc1.restart(activityRule, viewModelStore)

        // We need to wait the entire duration of the animation to see if onAnimationEnd is ever
        // called because if the activity gets destroyed no callback will ever be received.
        // (Animators do not suffer from this problem and onAnimationEnd is always called.
        assertThat(fragment2.exitLatch.await(5000, TimeUnit.MILLISECONDS)).isFalse()
    }

    // Ensures that when a Fragment that is animating away gets readded the state is properly
    // updated
    @Test
    fun reAddAnimatingAwayAnimationFragment() {
        waitForAnimationReady()
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimationListenerFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationListenerFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.anim.long_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        fm1.popBackStack()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null) // fragmentManager does not know about animating fragment
        assertThat(fragment2.parentFragmentManager)
            .isEqualTo(fm1) // but the animating fragment knows the fragmentManager

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.anim.long_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.isAdded).isTrue()
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)
    }

    @Test
    fun popReplaceOperationWithAnimations() {
        waitForAnimationReady()
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = AnimationListenerFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationListenerFragment()

        fm1.beginTransaction()
            .setCustomAnimations(
                R.anim.long_fade_in,
                R.anim.long_fade_out,
                R.anim.long_fade_in,
                R.anim.long_fade_out
            )
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        fm1.popBackStack()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null) // fragmentManager does not know about animating fragment
        assertThat(fragment2.parentFragmentManager)
            .isEqualTo(fm1) // but the animating fragment knows the fragmentManager

        // We need to wait for the exit animation to end
        assertThat(fragment2.exitLatch.await(6000, TimeUnit.MILLISECONDS)).isTrue()

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment1.requireView().parent).isNotNull()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun replaceOperationWithAnimationsThenSystemBack() {
        waitForAnimationReady()
        val fm1 = activityRule.activity.supportFragmentManager

        val fragment1 = AnimationListenerFragment(R.layout.scene1)
        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationListenerFragment()

        fm1.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // We need to wait for the exit animation to end
        assertThat(fragment1.exitLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        // We should not start any animations when we get the started callback
        assertThat(fragment1.enterStartCount).isEqualTo(0)

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
            dispatcher.onBackPressed()
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment2.startAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null) // fragmentManager does not know about animating fragment
        assertThat(fragment2.parentFragmentManager)
            .isEqualTo(fm1) // but the animating fragment knows the fragmentManager

        // We need to wait for the exit animation to end
        assertThat(fragment2.exitLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment1.requireView().parent).isNotNull()
    }

    // When an animation is running on a Fragment's View, the view shouldn't be
    // prevented from being removed. There's no way to directly test this, so we have to
    // test to see if the animation is still running.
    @Test
    fun clearAnimations() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val fragmentView = fragment1.requireView()

        val xAnimation = TranslateAnimation(0f, 1000f, 0f, 0f)
        activityRule.runOnUiThread { fragmentView.startAnimation(xAnimation) }

        activityRule.waitForExecution()
        activityRule.popBackStackImmediate()
        activityRule.runOnUiThread { assertThat(fragmentView.animation).isNull() }
    }

    // When a view is animated out, is parent should be null after the animation completes
    @Test
    fun parentNullAfterAnimation() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimationListenerFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationListenerFragment()

        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()

        assertThat(fragment1.exitLatch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment2.enterLatch.await(1, TimeUnit.SECONDS)).isTrue()

        activityRule.runOnUiThread {
            assertThat(fragment1.createdView).isNotNull()
            assertThat(fragment2.createdView).isNotNull()
            assertThat(fragment1.createdView.parent).isNull()
        }

        // Now pop the transaction
        activityRule.popBackStackImmediate()

        assertThat(fragment2.exitLatch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment1.enterLatch.await(1, TimeUnit.SECONDS)).isTrue()

        activityRule.runOnUiThread { assertThat(fragment2.createdView.parent).isNull() }
    }

    @Test
    fun animationListenersAreCalled() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fm = withActivity {
                setContentView(R.layout.simple_container)
                supportFragmentManager
            }

            // Add first fragment
            val fragment1 = AnimationListenerFragment()
            fragment1.forceRunOnHwLayer = false
            fragment1.repeat = true
            withActivity {
                fm.beginTransaction()
                    .add(R.id.fragmentContainer, fragment1)
                    .commit()
                fm.executePendingTransactions()
            }

            // Replace first fragment with second fragment with a fade in/out animation
            val fragment2 = AnimationListenerFragment()
            fragment2.forceRunOnHwLayer = true
            fragment2.repeat = false
            withActivity {
                fm.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out
                    )
                    .replace(R.id.fragmentContainer, fragment2)
                    .addToBackStack(null)
                    .commit()
                fm.executePendingTransactions()
            }

            // Wait for animation to finish
            assertThat(fragment1.exitLatch.await(2, TimeUnit.SECONDS)).isTrue()
            assertThat(fragment2.enterLatch.await(2, TimeUnit.SECONDS)).isTrue()

            // Check if all animation listener callbacks have been called
            assertThat(fragment1.exitStartCount).isEqualTo(1)
            assertThat(fragment1.exitRepeatCount).isEqualTo(1)
            assertThat(fragment1.exitEndCount).isEqualTo(1)
            assertThat(fragment2.enterStartCount).isEqualTo(1)
            assertThat(fragment2.enterRepeatCount).isEqualTo(0)
            assertThat(fragment2.enterEndCount).isEqualTo(1)

            // fragment1 exited, so its enter animation should not have been called
            assertThat(fragment1.enterStartCount).isEqualTo(0)
            assertThat(fragment1.enterRepeatCount).isEqualTo(0)
            assertThat(fragment1.enterEndCount).isEqualTo(0)
            // fragment2 entered, so its exit animation should not have been called
            assertThat(fragment2.exitStartCount).isEqualTo(0)
            assertThat(fragment2.exitRepeatCount).isEqualTo(0)
            assertThat(fragment2.exitEndCount).isEqualTo(0)

            fragment1.resetCounts()
            fragment2.resetCounts()

            // Now pop the transaction
            withActivity { fm.popBackStackImmediate() }

            assertThat(fragment2.exitLatch.await(2, TimeUnit.SECONDS)).isTrue()
            assertThat(fragment1.enterLatch.await(2, TimeUnit.SECONDS)).isTrue()

            assertThat(fragment2.exitStartCount).isEqualTo(1)
            assertThat(fragment2.exitRepeatCount).isEqualTo(0)
            assertThat(fragment2.exitEndCount).isEqualTo(1)
            assertThat(fragment1.enterStartCount).isEqualTo(1)
            assertThat(fragment1.enterRepeatCount).isEqualTo(1)
            assertThat(fragment1.enterEndCount).isEqualTo(1)

            // fragment1 entered, so its exit animation should not have been called
            assertThat(fragment1.exitStartCount).isEqualTo(0)
            assertThat(fragment1.exitRepeatCount).isEqualTo(0)
            assertThat(fragment1.exitEndCount).isEqualTo(0)
            // fragment2 exited, so its enter animation should not have been called
            assertThat(fragment2.enterStartCount).isEqualTo(0)
            assertThat(fragment2.enterRepeatCount).isEqualTo(0)
            assertThat(fragment2.enterEndCount).isEqualTo(0)
        }
    }

    @Test
    fun removingFragmentAnimationChange() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimationFragment()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.fragmentContainer, fragment1, "fragment1")
            .addToBackStack("fragment1")
            .commit()
        activityRule.waitForExecution()

        val fragment2 = AnimationFragment()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2, "fragment2")
            .addToBackStack("fragment2")
            .commit()

        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            fm.popBackStack("fragment1", 0)

            val fragment3 = AnimationFragment()

            fm.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .replace(R.id.fragmentContainer, fragment3, "fragment3")
                .addToBackStack("fragment3")
                .commit()
        }

        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            assertThat(fragment2.loadedAnimation).isEqualTo(EXIT)
        }
    }

    @Test
    fun removePopExitAnimation() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = AnimationFragment()
        val fragment2 = AnimationFragment()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment1, "fragment1")
            .addToBackStack("fragment1")
            .commit()
        activityRule.waitForExecution()

        activityRule.runOnUiThread {
            fm.popBackStack()
            fm.beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .replace(R.id.fragmentContainer, fragment2, "fragment2")
                .addToBackStack("fragment2")
                .commit()
        }
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(EXIT)
    }

    @Test
    fun removePopExitAnimationWithSetPrimaryNavigation() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = AnimationFragment()
        val fragment2 = AnimationFragment()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment1, "fragment1")
            .setPrimaryNavigationFragment(fragment1)
            .addToBackStack("fragment1")
            .commit()
        activityRule.waitForExecution()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment2, "fragment2")
            .setPrimaryNavigationFragment(fragment2)
            .addToBackStack("fragment2")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(EXIT)
        assertThat(fragment2.loadedAnimation).isEqualTo(ENTER)

        fm.popBackStack()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(POP_ENTER)
        assertThat(fragment2.loadedAnimation).isEqualTo(POP_EXIT)
    }

    @Test
    fun ensureProperAnimationOnPopUpAndReplace() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = AnimationFragment()
        val fragment2 = AnimationFragment()
        val fragment3 = AnimationFragment()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER_OTHER, EXIT_OTHER)
            .add(R.id.fragmentContainer, fragment1, "fragment1")
            .setPrimaryNavigationFragment(fragment1)
            .addToBackStack("fragment1")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(ENTER_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(0)
        assertThat(fragment3.loadedAnimation).isEqualTo(0)

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER_OTHER, EXIT_OTHER)
            .replace(R.id.fragmentContainer, fragment2, "fragment2")
            .setPrimaryNavigationFragment(fragment2)
            .addToBackStack("fragment2")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(EXIT_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(ENTER_OTHER)
        assertThat(fragment3.loadedAnimation).isEqualTo(0)

        fm.popBackStack()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER, EXIT)
            .replace(R.id.fragmentContainer, fragment3, "fragment3")
            .setPrimaryNavigationFragment(fragment3)
            .addToBackStack("fragment3")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(EXIT_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(EXIT)
        assertThat(fragment3.loadedAnimation).isEqualTo(ENTER)
    }

    @Test
    fun ensureProperAnimationOnDoublePop() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager
        val fragment1 = AnimationFragment()
        val fragment2 = AnimationFragment()
        val fragment3 = AnimationFragment()

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER_OTHER, EXIT_OTHER, ENTER_OTHER, EXIT_OTHER)
            .add(R.id.fragmentContainer, fragment1, "fragment1")
            .setPrimaryNavigationFragment(fragment1)
            .addToBackStack("fragment1")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(ENTER_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(0)
        assertThat(fragment3.loadedAnimation).isEqualTo(0)

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER_OTHER, EXIT_OTHER, ENTER_OTHER, EXIT_OTHER)
            .replace(R.id.fragmentContainer, fragment2, "fragment2")
            .setPrimaryNavigationFragment(fragment2)
            .addToBackStack("fragment2")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(EXIT_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(ENTER_OTHER)
        assertThat(fragment3.loadedAnimation).isEqualTo(0)

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(ENTER, EXIT, ENTER, EXIT)
            .replace(R.id.fragmentContainer, fragment3, "fragment3")
            .setPrimaryNavigationFragment(fragment3)
            .addToBackStack("fragment3")
            .commit()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(EXIT_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(EXIT)
        assertThat(fragment3.loadedAnimation).isEqualTo(ENTER)

        fm.popBackStack()
        fm.popBackStack()
        activityRule.waitForExecution()

        assertThat(fragment1.loadedAnimation).isEqualTo(ENTER_OTHER)
        assertThat(fragment2.loadedAnimation).isEqualTo(EXIT)
        assertThat(fragment3.loadedAnimation).isEqualTo(EXIT_OTHER)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun predictiveBackNoAnimation() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity { setContentView(R.layout.simple_container) }
            val fragment1 = StrictViewFragment()
            val fragment2 = StrictViewFragment()

            val fm = withActivity { supportFragmentManager }

            withActivity {
                fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainer, fragment1, "fragment1")
                    .addToBackStack("fragment1")
                    .commit()
            }
            waitForExecution()

            withActivity {
                fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, fragment2, "fragment2")
                    .addToBackStack("fragment2")
                    .commit()
            }
            waitForExecution()

            fragment1.mContainer = null
            fragment2.mContainer = null

            val dispatcher = activityRule.activity.onBackPressedDispatcher
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            executePendingTransactions()

            withActivity {
                dispatcher.onBackPressed()
            }
            executePendingTransactions()

            assertThat(fragment2.calledOnDestroy).isTrue()
        }
    }

    private fun assertEnterPopExit(fragment: AnimationFragment) {
        assertFragmentAnimation(fragment, 1, true, ENTER)

        val fm = activityRule.activity.supportFragmentManager
        fm.popBackStack()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment, 2, false, POP_EXIT)
    }

    private fun assertExitPopEnter(fragment: AnimationFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        val fm = activityRule.activity.supportFragmentManager
        fm.popBackStack()
        activityRule.waitForExecution()

        val replacement = fm.findFragmentByTag("1") as AnimationFragment?

        val isSameFragment = replacement === fragment
        val expectedAnimators = if (isSameFragment) 2 else 1
        assertFragmentAnimation(replacement!!, expectedAnimators, true, POP_ENTER)
    }

    private fun assertExitPostponedPopEnter(fragment: AnimationFragment) {
        assertFragmentAnimation(fragment, 1, false, EXIT)

        fragment.postponeEnterTransition()
        activityRule.popBackStackImmediate()

        assertPostponed(fragment, 1)

        fragment.startPostponedEnterTransition()
        activityRule.waitForExecution()
        assertFragmentAnimation(fragment, 2, true, POP_ENTER)
    }

    private fun assertFragmentAnimation(
        fragment: AnimationFragment,
        numAnimators: Int,
        isEnter: Boolean,
        animatorResourceId: Int
    ) {
        assertThat(fragment.numStartedAnimators).isEqualTo(numAnimators)
        assertThat(fragment.enter).isEqualTo(isEnter)
        assertThat(fragment.resourceId).isEqualTo(animatorResourceId)
        assertThat(fragment.animation).isNotNull()
        assertThat(fragment.animation!!.waitForEnd(1000)).isTrue()
        assertThat(fragment.animation?.hasStarted()!!).isTrue()
        assertThat(fragment.enterAnim).isEqualTo(0)
        assertThat(fragment.exitAnim).isEqualTo(0)
        assertThat(fragment.popEnterAnim).isEqualTo(0)
        assertThat(fragment.popExitAnim).isEqualTo(0)
    }

    private fun assertPostponed(fragment: AnimationFragment, expectedAnimators: Int) {
        assertThat(fragment.onCreateViewCalled).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.requireView().alpha).isWithin(0f).of(0f)
        assertThat(fragment.numStartedAnimators).isEqualTo(expectedAnimators)
    }

    // On Lollipop and earlier, animations are not allowed during window transitions
    private fun waitForAnimationReady() {
        val activity = activityRule.activity
        lateinit var drawView: DrawView
        // Add view to the hierarchy
        activityRule.runOnUiThread {
            drawView = DrawView(activity)
            val content = activity.findViewById<ViewGroup>(R.id.fragmentContainer)
            content.addView(drawView)
        }

        // Wait for its draw method to be called so we know that drawing can happen after
        // the first frame (API 21 didn't allow it during Window transitions)
        assertThat(drawView.onDrawCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Remove the view that we just added
        activityRule.runOnUiThread {
            val content = activity.findViewById<ViewGroup>(R.id.fragmentContainer)
            content.removeView(drawView)
        }
    }

    class DrawView(context: android.content.Context) : View(context) {
        val onDrawCountDownLatch = CountDownLatch(1)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            onDrawCountDownLatch.countDown()
        }
    }

    class AnimationFragment(@LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment) :
        StrictViewFragment(contentLayoutId) {
        var numStartedAnimators: Int = 0
        var animationStartedCountDownLatch = CountDownLatch(1)
        var animation: Animation? = null
        var enter: Boolean = false
        var resourceId: Int = 0
        var loadedAnimation = 0
        val onResumeCountDownLatch = CountDownLatch(1)

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0 ||
                viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED
            ) {
                return null
            }
            loadedAnimation = nextAnim
            animation = TranslateAnimation(-10f, 0f, 0f, 0f)
            (animation as TranslateAnimation).duration = 1
            animationStartedCountDownLatch = CountDownLatch(1)
            (animation as TranslateAnimation).setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    numStartedAnimators++
                    animationStartedCountDownLatch.countDown()
                }

                override fun onAnimationEnd(p0: Animation?) { }

                override fun onAnimationRepeat(p0: Animation?) { }
            })
            resourceId = nextAnim
            this.enter = enter
            return animation
        }

        override fun onResume() {
            super.onResume()
            onResumeCountDownLatch.countDown()
        }
    }

    class AnimationListenerFragment(
        @LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment
    ) : StrictViewFragment(contentLayoutId) {
        lateinit var createdView: View
        var forceRunOnHwLayer: Boolean = false
        var repeat: Boolean = false
        var enterStartCount = 0
        var enterRepeatCount = 0
        var enterEndCount = 0
        var exitStartCount = 0
        var exitRepeatCount = 0
        var exitEndCount = 0
        lateinit var startAnimationLatch: CountDownLatch
        val enterLatch = CountDownLatch(1)
        val exitLatch = CountDownLatch(1)

        fun resetCounts() {
            enterEndCount = 0
            enterRepeatCount = enterEndCount
            enterStartCount = enterRepeatCount
            exitEndCount = 0
            exitRepeatCount = exitEndCount
            exitStartCount = exitRepeatCount
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState)?.apply {
            if (forceRunOnHwLayer) {
                // Set any background color on the TextView, so view.hasOverlappingRendering() will
                // return true, which in turn makes FragmentManager.shouldRunOnHWLayer() return
                // true.
                setBackgroundColor(-0x1)
            }
            createdView = this
        }

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0) {
                return null
            }
            val anim = AnimationUtils.loadAnimation(activity, nextAnim)
            startAnimationLatch = CountDownLatch(1)
            if (anim != null) {
                if (repeat) {
                    anim.repeatCount = 1
                }
                anim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        if (enter) {
                            enterStartCount++
                        } else {
                            exitStartCount++
                        }
                        startAnimationLatch.countDown()
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        if (viewLifecycleOwner.lifecycle.currentState
                            != Lifecycle.State.DESTROYED
                        ) {
                            if (enter) {
                                enterEndCount++
                                enterLatch.countDown()
                            } else {
                                exitEndCount++
                                // When exiting, the view is detached after onAnimationEnd,
                                // so wait one frame to count down the latch
                                createdView.post { exitLatch.countDown() }
                            }
                        }
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                        if (enter) {
                            enterRepeatCount++
                        } else {
                            exitRepeatCount++
                        }
                    }
                })
            }
            return anim
        }
    }

    companion object {
        // These are pretend resource IDs for animators. We don't need real ones since we
        // load them by overriding onCreateAnimator
        @AnimRes
        private val ENTER = 1
        @AnimRes
        private val EXIT = 2
        @AnimRes
        private val POP_ENTER = 3
        @AnimRes
        private val POP_EXIT = 4
        @AnimRes
        private val ENTER_OTHER = 5
        @AnimRes
        private val EXIT_OTHER = 6
    }
}

private fun Animation.waitForEnd(timeout: Long): Boolean {
    val endTime = SystemClock.uptimeMillis() + timeout
    var hasEnded = false
    val check = Runnable { hasEnded = hasEnded() }
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    do {
        SystemClock.sleep(10)
        instrumentation.runOnMainSync(check)
    } while (!hasEnded && SystemClock.uptimeMillis() < endTime)
    return hasEnded
}
