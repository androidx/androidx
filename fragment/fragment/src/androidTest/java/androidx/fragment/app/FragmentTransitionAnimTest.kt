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
import android.transition.ChangeBounds
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.annotation.AnimRes
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FragmentTransitionAnimTest(private val reorderingAllowed: ReorderingAllowed) {
    private var onBackStackChangedTimes: Int = 0

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Before
    fun setup() {
        onBackStackChangedTimes = 0
    }

    // Ensure when transition duration is shorter than animation duration, we will get both end
    // callbacks
    @Test
    fun transitionShorterThanAnimation() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimationFragment()
            fragment.exitTransition.duration = 100

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(onBackStackChangedTimes).isEqualTo(1)
            fragment.waitForTransition()

            val blue = withActivity { findViewById<View>(R.id.blueSquare) }
            val green = withActivity { findViewById<View>(R.id.greenSquare) }

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(blue, green)
            }
            verifyNoOtherTransitions(fragment)

            val changeBoundsExitTransition = ChangeBounds().apply { duration = 100 }
            fragment.setExitTransition(changeBoundsExitTransition)
            changeBoundsExitTransition.addListener(fragment.listener)

            // exit transition
            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .remove(fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val startAnimationRan =
                fragment.startAnimationLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
            assertThat(startAnimationRan).isFalse()
            fragment.waitForTransition()
            val exitAnimationRan = fragment.exitAnimationLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
            assertThat(exitAnimationRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    // Ensure when transition duration is longer than animation duration, we will get both end
    // callbacks
    @Test
    fun transitionLongerThanAnimation() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimationFragment()
            fragment.exitTransition.duration = 1000

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(onBackStackChangedTimes).isEqualTo(1)
            fragment.waitForTransition()

            val blue = withActivity { findViewById<View>(R.id.blueSquare) }
            val green = withActivity { findViewById<View>(R.id.greenSquare) }

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(blue, green)
            }
            verifyNoOtherTransitions(fragment)

            val changeBoundsExitTransition = ChangeBounds().apply { duration = 1000 }
            fragment.setExitTransition(changeBoundsExitTransition)
            changeBoundsExitTransition.addListener(fragment.listener)

            // exit transition
            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .remove(fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val startAnimationRan =
                fragment.startAnimationLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
            assertThat(startAnimationRan).isFalse()
            fragment.waitForTransition()
            val exitAnimationRan = fragment.exitAnimationLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
            assertThat(exitAnimationRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    // Ensure when transition duration is shorter than animator duration, we will get both end
    // callbacks
    @Test
    fun transitionShorterThanAnimator() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimatorFragment()
            fragment.exitTransition.duration = 100

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(ENTER, EXIT)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(onBackStackChangedTimes).isEqualTo(1)
            fragment.waitForTransition()

            val blue = withActivity { findViewById<View>(R.id.blueSquare) }
            val green = withActivity { findViewById<View>(R.id.greenSquare) }

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(blue, green)
            }
            verifyNoOtherTransitions(fragment)

            val changeBoundsExitTransition = ChangeBounds().apply { duration = 100 }
            fragment.setExitTransition(changeBoundsExitTransition)
            changeBoundsExitTransition.addListener(fragment.listener)

            // exit transition
            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(ENTER, EXIT)
                .remove(fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            fragment.waitForTransition()
            val exitAnimatorRan = fragment.exitAnimatorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
            assertThat(exitAnimatorRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    // Ensure when transition duration is longer than animator duration, we will get both end
    // callbacks
    @Test
    fun transitionLongerThanAnimator() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimatorFragment()
            fragment.exitTransition.duration = 1000

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(ENTER, EXIT)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            assertThat(onBackStackChangedTimes).isEqualTo(1)
            fragment.waitForTransition()

            val blue = withActivity { findViewById<View>(R.id.blueSquare) }
            val green = withActivity { findViewById<View>(R.id.greenSquare) }

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(blue, green)
            }
            verifyNoOtherTransitions(fragment)

            val changeBoundsExitTransition = ChangeBounds().apply { duration = 1000 }
            fragment.setExitTransition(changeBoundsExitTransition)
            changeBoundsExitTransition.addListener(fragment.listener)

            // exit transition
            fragmentManager
                .beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(ENTER, EXIT)
                .remove(fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            fragment.waitForTransition()
            val exitAnimatorRan = fragment.exitAnimatorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
            assertThat(exitAnimatorRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun replaceOperationWithTransitionsAndAnimatorsThenSystemBack() {
        withUse(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = TransitionAnimatorFragment()
            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val fragment2 = TransitionAnimatorFragment()

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
            executePendingTransactions()

            // We need to wait for the exit transitions to end
            assertThat(fragment2.endTransitionCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()
            assertThat(fragment1.endTransitionCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            val dispatcher = withActivity { onBackPressedDispatcher }
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
            executePendingTransactions()

            // We should not start any transitions when we get the started callback
            fragment1.waitForNoTransition()

            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
            withActivity { dispatcher.onBackPressed() }
            executePendingTransactions()

            fragment1.waitForTransition()
            fragment2.waitForTransition()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    class TransitionAnimationFragment : TransitionFragment(R.layout.scene1) {
        val startAnimationLatch = CountDownLatch(1)
        val exitAnimationLatch = CountDownLatch(1)

        override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
            if (nextAnim == 0) {
                return null
            }

            return AnimationUtils.loadAnimation(activity, nextAnim).apply {
                setAnimationListener(
                    object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {
                            startAnimationLatch.countDown()
                        }

                        override fun onAnimationEnd(animation: Animation) {
                            if (!enter) {
                                exitAnimationLatch.countDown()
                            }
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    }
                )
            }
        }
    }

    class TransitionAnimatorFragment : TransitionFragment(R.layout.scene1) {
        val exitAnimatorLatch = CountDownLatch(1)

        override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int) =
            ValueAnimator.ofFloat(0f, 1f).setDuration(300)?.apply {
                if (nextAnim == 0) {
                    return null
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (!enter) {
                                exitAnimatorLatch.countDown()
                            }
                        }
                    }
                )
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "ordering={0}")
        fun data() = arrayOf(Ordered, Reordered)

        @AnimRes private val ENTER = 1
        @AnimRes private val EXIT = 2

        private const val TIMEOUT = 1000L
    }
}
