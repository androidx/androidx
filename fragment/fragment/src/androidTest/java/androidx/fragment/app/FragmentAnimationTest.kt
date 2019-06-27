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
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import androidx.annotation.AnimRes
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentAnimationTest {

    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var instrumentation: Instrumentation

    @Before
    fun setupContainer() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        activityRule.setContentView(R.layout.simple_container)
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators
    @Test
    fun addAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertEnterPopExit(fragment)
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators
    @Test
    fun removeAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
        fm.beginTransaction().add(R.id.fragmentContainer, fragment, "1").commit()
        activityRule.waitForExecution()

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
    fun showAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
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
    fun hideAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
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
    fun attachAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
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
    fun detachAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
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
    fun replaceAnimators() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        val fragment2 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .add(R.id.fragmentContainer, fragment2, "2")
            .commit()
        activityRule.waitForExecution()

        val fragment3 = AnimatorFragment()
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, fragment3)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment1, 1, false, EXIT)
        assertFragmentAnimation(fragment2, 1, false, EXIT)
        assertFragmentAnimation(fragment3, 1, true, ENTER)

        fm.popBackStack()
        activityRule.waitForExecution()

        assertFragmentAnimation(fragment3, 2, false, POP_EXIT)
        val replacement1 = fm.findFragmentByTag("1") as AnimatorFragment?
        val replacement2 = fm.findFragmentByTag("1") as AnimatorFragment?
        val expectedAnimations = if (replacement1 === fragment1) 2 else 1
        assertFragmentAnimation(replacement1!!, expectedAnimations, true, POP_ENTER)
        assertFragmentAnimation(replacement2!!, expectedAnimations, true, POP_ENTER)
    }

    // Ensure child view is not removed before parent view animates out.
    @Test
    fun removeParentWithAnimation() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val parent = AnimatorFragment(R.layout.simple_container)
        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .add(R.id.fragmentContainer, parent, "parent")
            .commit()
        activityRule.executePendingTransactions()

        val child = AnimatorFragment()
        parent.childFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, child, "child")
            .commit()
        activityRule.executePendingTransactions(parent.childFragmentManager)

        val childContainer = child.mContainer
        val childView = child.mView

        fm.beginTransaction()
            .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
            .replace(R.id.fragmentContainer, AnimatorFragment(), "other")
            .commit()
        activityRule.executePendingTransactions()

        assertFragmentAnimation(parent, 2, false, EXIT)
        assertThat(childContainer.findViewById<View>(childView.id)).isNotNull()
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    fun postponedAddAnimators() {
        waitForAnimationReady()
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
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment = AnimatorFragment()
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
        assertThat(fragment1.view).isNotNull()
        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment1.requireView().alpha).isWithin(0f).of(1f)
        assertThat(ViewCompat.isAttachedToWindow(fragment1.requireView())).isTrue()

        fragment2.startPostponedEnterTransition()
        activityRule.waitForExecution()

        assertExitPostponedPopEnter(fragment1)
    }

    // Popping a postponed transaction should result in no animators
    @Test
    fun popPostponed() {
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = AnimatorFragment()
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        assertThat(fragment1.numAnimators).isEqualTo(0)

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
        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment1.requireView().alpha).isWithin(0f).of(1f)
        assertThat(ViewCompat.isAttachedToWindow(fragment1.requireView())).isTrue()
        assertThat(fragment1.isAdded).isTrue()

        assertThat(fragment2.view).isNull()
        assertThat(fragment2.isAdded).isFalse()

        assertThat(fragment1.numAnimators).isEqualTo(0)
        assertThat(fragment2.numAnimators).isEqualTo(0)
        assertThat(fragment1.animation).isNull()
        assertThat(fragment2.animation).isNull()
    }

    // Make sure that if the state was saved while a Fragment was animating that its
    // state is proper after restoring.
    @Test
    fun saveWhileAnimatingAway() {
        waitForAnimationReady()
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)

        val fm1 = fc1.supportFragmentManager

        val fragment1 = StrictViewFragment(R.layout.scene1)
        fm1.beginTransaction()
            .add(R.id.fragmentContainer, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val fragment2 = StrictViewFragment()

        fm1.beginTransaction()
            .setCustomAnimations(0, 0, 0, R.anim.long_fade_out)
            .replace(R.id.fragmentContainer, fragment2, "2")
            .addToBackStack(null)
            .commit()
        instrumentation.runOnMainSync { fm1.executePendingTransactions() }
        activityRule.waitForExecution()

        fm1.popBackStack()

        instrumentation.runOnMainSync { fm1.executePendingTransactions() }
        activityRule.waitForExecution()
        // Now fragment2 should be animating away
        assertThat(fragment2.isAdded).isFalse()
        // still exists because it is animating
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)

        val fc2 = fc1.restart(activityRule, viewModelStore)

        val fm2 = fc2.supportFragmentManager
        val fragment2restored = fm2.findFragmentByTag("2")
        assertThat(fragment2restored).isNull()

        val fragment1restored = fm2.findFragmentByTag("1")
        assertThat(fragment1restored).isNotNull()
        assertThat(fragment1restored!!.view).isNotNull()
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
        waitForAnimationReady()
        val fm = activityRule.activity.supportFragmentManager

        // Add first fragment
        val fragment1 = AnimationListenerFragment()
        fragment1.forceRunOnHwLayer = false
        fragment1.repeat = true
        fm.beginTransaction()
            .add(R.id.fragmentContainer, fragment1)
            .commit()
        activityRule.waitForExecution()

        // Replace first fragment with second fragment with a fade in/out animation
        val fragment2 = AnimationListenerFragment()
        fragment2.forceRunOnHwLayer = true
        fragment2.repeat = false
        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        // Wait for animation to finish
        assertThat(fragment1.exitLatch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment2.enterLatch.await(2, TimeUnit.SECONDS)).isTrue()

        // Check if all animation listener callbacks have been called
        activityRule.runOnUiThread {
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
        }
        fragment1.resetCounts()
        fragment2.resetCounts()

        // Now pop the transaction
        activityRule.popBackStackImmediate()

        assertThat(fragment2.exitLatch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(fragment1.enterLatch.await(2, TimeUnit.SECONDS)).isTrue()

        activityRule.runOnUiThread {
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

        val replacement = fm.findFragmentByTag("1") as AnimatorFragment?

        val isSameFragment = replacement === fragment
        val expectedAnimators = if (isSameFragment) 2 else 1
        assertFragmentAnimation(replacement!!, expectedAnimators, true, POP_ENTER)
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
        animatorResourceId: Int
    ) {
        assertThat(fragment.numAnimators).isEqualTo(numAnimators)
        assertThat(fragment.enter).isEqualTo(isEnter)
        assertThat(fragment.resourceId).isEqualTo(animatorResourceId)
        assertThat(fragment.animation).isNotNull()
        assertThat(fragment.animation!!.waitForEnd(1000)).isTrue()
        assertThat(fragment.animation?.hasStarted()!!).isTrue()
        assertThat(fragment.nextAnim).isEqualTo(0)
    }

    private fun assertPostponed(fragment: AnimatorFragment, expectedAnimators: Int) {
        assertThat(fragment.onCreateViewCalled).isTrue()
        assertThat(fragment.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(fragment.requireView().alpha).isWithin(0f).of(0f)
        assertThat(fragment.numAnimators).isEqualTo(expectedAnimators)
    }

    // On Lollipop and earlier, animations are not allowed during window transitions
    private fun waitForAnimationReady() {
        val view = arrayOfNulls<View>(1)
        val activity = activityRule.activity
        // Add a view to the hierarchy
        activityRule.runOnUiThread {
            view[0] = spy(View(activity))
            val content = activity.findViewById<ViewGroup>(R.id.fragmentContainer)
            content.addView(view[0])
        }

        // Wait for its draw method to be called so we know that drawing can happen after
        // the first frame (API 21 didn't allow it during Window transitions)
        verify(view[0], within(1000))?.draw(ArgumentMatchers.any() as Canvas?)

        // Remove the view that we just added
        activityRule.runOnUiThread {
            val content = activity.findViewById<ViewGroup>(R.id.fragmentContainer)
            content.removeView(view[0])
        }
    }

    class AnimatorFragment(@LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment) :
        StrictViewFragment(contentLayoutId) {
        var numAnimators: Int = 0
        var animation: Animation? = null
        var enter: Boolean = false
        var resourceId: Int = 0

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0) {
                return null
            }
            numAnimators++
            animation = TranslateAnimation(-10f, 0f, 0f, 0f)
            (animation as TranslateAnimation).duration = 1
            resourceId = nextAnim
            this.enter = enter
            return animation
        }
    }

    class AnimationListenerFragment : StrictViewFragment() {
        lateinit var createdView: View
        var forceRunOnHwLayer: Boolean = false
        var repeat: Boolean = false
        var enterStartCount = 0
        var enterRepeatCount = 0
        var enterEndCount = 0
        var exitStartCount = 0
        var exitRepeatCount = 0
        var exitEndCount = 0
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
                // return true, which in turn makes FragmentManagerImpl.shouldRunOnHWLayer() return
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
                    }

                    override fun onAnimationEnd(animation: Animation) {
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
