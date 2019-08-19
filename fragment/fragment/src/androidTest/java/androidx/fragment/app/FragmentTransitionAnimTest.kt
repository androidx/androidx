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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.annotation.AnimRes
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class FragmentTransitionAnimTest(private val reorderingAllowed: Boolean) {

    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var fragmentManager: FragmentManager
    private var onBackStackChangedTimes: Int = 0
    private val onBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { onBackStackChangedTimes++ }

    @Before
    fun setup() {
        activityRule.setContentView(R.layout.simple_container)
        onBackStackChangedTimes = 0
        fragmentManager = activityRule.activity.supportFragmentManager
        fragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)
    }

    @After
    fun teardown() {
        fragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
    }

    // Ensure when transition duration is longer than animation duration, we will get both end
    // callbacks
    @Test
    fun transitionShorterThanAnimation() {
        val fragment = TransitionAnimationFragment()
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        fragment.waitForTransition()
        verifyAndClearTransition(fragment.enterTransition, null, activityRule.findBlue(),
            activityRule.findGreen())
        verifyNoOtherTransitions(fragment)

        val changeBoundsExitTransition = ChangeBounds().apply {
            duration = 100
        }
        fragment.setExitTransition(changeBoundsExitTransition)
        changeBoundsExitTransition.addListener(fragment.listener)

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment.waitForTransition()
        assertThat(fragment.exitAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(onBackStackChangedTimes).isEqualTo(2)
    }

    // Ensure when transition duration is shorter than animation duration, we will get both end
    // callbacks
    @Test
    fun transitionLongerThanAnimation() {
        val fragment = TransitionAnimationFragment()
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)

        fragment.waitForTransition()
        verifyAndClearTransition(fragment.enterTransition, null, activityRule.findBlue(),
            activityRule.findGreen())
        verifyNoOtherTransitions(fragment)

        val changeBoundsExitTransition = ChangeBounds().apply {
            duration = 1000
        }
        fragment.setExitTransition(changeBoundsExitTransition)
        changeBoundsExitTransition.addListener(fragment.listener)

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment.waitForTransition()
        assertThat(fragment.exitAnimationLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(onBackStackChangedTimes).isEqualTo(2)
    }

    // Ensure when transition duration is shorter than animator duration, we will get both end
    // callbacks
    @Test
    fun transitionShorterThanAnimator() {
        val fragment = TransitionAnimatorFragment()
        fragment.exitTransition.duration = 100
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        fragment.waitForTransition()
        verifyAndClearTransition(fragment.enterTransition, null, activityRule.findBlue(),
            activityRule.findGreen())
        verifyNoOtherTransitions(fragment)

        val changeBoundsExitTransition = ChangeBounds().apply {
            duration = 100
        }
        fragment.setExitTransition(changeBoundsExitTransition)
        changeBoundsExitTransition.addListener(fragment.listener)

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment.waitForTransition()
        assertThat(fragment.exitAnimatorLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(onBackStackChangedTimes).isEqualTo(2)
    }

    // Ensure when transition duration is longer than animator duration, we will get both end
    // callbacks
    @Test
    fun transitionLongerThanAnimator() {
        val fragment = TransitionAnimatorFragment()
        fragment.exitTransition.duration = 1000
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        fragment.waitForTransition()
        verifyAndClearTransition(fragment.enterTransition, null, activityRule.findBlue(),
            activityRule.findGreen())
        verifyNoOtherTransitions(fragment)

        val changeBoundsExitTransition = ChangeBounds().apply {
            duration = 1000
        }
        fragment.setExitTransition(changeBoundsExitTransition)
        changeBoundsExitTransition.addListener(fragment.listener)

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .setCustomAnimations(ENTER, EXIT)
            .remove(fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment.waitForTransition()
        assertThat(fragment.exitAnimatorLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(onBackStackChangedTimes).isEqualTo(2)
    }

    class TransitionAnimationFragment : TransitionFragment(R.layout.scene1) {
        lateinit var createdView: View
        val exitAnimationLatch = CountDownLatch(1)

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState)?.apply {
            createdView = this
        }

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0) {
                return null
            }

            return TranslateAnimation(-10f, 0f, 0f, 0f).apply {
                duration = 300
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) { }

                    override fun onAnimationEnd(animation: Animation) {
                        if (viewLifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED
                        ) {
                            if (!enter) {
                                // When exiting, the view is detached after onAnimationEnd,
                                // so wait one frame to count down the latch
                                createdView.post { exitAnimationLatch.countDown() }
                            }
                        }
                    }
                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
        }
    }

    class TransitionAnimatorFragment : TransitionFragment(R.layout.scene1) {
        lateinit var exitAnimatorLatch: CountDownLatch

        override fun onCreateAnimator(
            transit: Int,
            enter: Boolean,
            nextAnim: Int
        ) = ValueAnimator.ofFloat(0f, 1f).setDuration(300)?.apply {
            if (nextAnim == 0) {
                return null
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!enter) {
                        exitAnimatorLatch.countDown()
                    }
                }
            })
            exitAnimatorLatch = CountDownLatch(1)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Boolean> {
            return arrayOf(false, true)
        }

        @AnimRes
        private val ENTER = 1
        @AnimRes
        private val EXIT = 2
    }
}