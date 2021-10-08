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
import androidx.annotation.AnimRes
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class FragmentTransitionAnimTest(
    private val reorderingAllowed: ReorderingAllowed,
) {
    private var onBackStackChangedTimes: Int = 0

    @Before
    fun setup() {
        onBackStackChangedTimes = 0
    }

    // Ensure when transition duration is shorter than animation duration, we will get both end
    // callbacks
    @Test
    fun transitionShorterThanAnimation() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimationFragment()
            fragment.exitTransition.duration = 100

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager.beginTransaction()
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

            val changeBoundsExitTransition = ChangeBounds().apply {
                duration = 100
            }
            fragment.setExitTransition(changeBoundsExitTransition)
            changeBoundsExitTransition.addListener(fragment.listener)

            // exit transition
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .remove(fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val startAnimationRan = fragment.startAnimationLatch.await(
                TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            assertThat(startAnimationRan).isFalse()
            fragment.waitForTransition()
            val exitAnimationRan = fragment.exitAnimationLatch.await(
                TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            assertThat(exitAnimationRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    // Ensure when transition duration is longer than animation duration, we will get both end
    // callbacks
    @Test
    fun transitionLongerThanAnimation() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimationFragment()
            fragment.exitTransition.duration = 1000

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager.beginTransaction()
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

            val changeBoundsExitTransition = ChangeBounds().apply {
                duration = 1000
            }
            fragment.setExitTransition(changeBoundsExitTransition)
            changeBoundsExitTransition.addListener(fragment.listener)

            // exit transition
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .remove(fragment)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            val startAnimationRan = fragment.startAnimationLatch.await(
                TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            assertThat(startAnimationRan).isFalse()
            fragment.waitForTransition()
            val exitAnimationRan = fragment.exitAnimationLatch.await(
                TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            assertThat(exitAnimationRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    // Ensure when transition duration is shorter than animator duration, we will get both end
    // callbacks
    @Test
    fun transitionShorterThanAnimator() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimatorFragment()
            fragment.exitTransition.duration = 100

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager.beginTransaction()
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
            executePendingTransactions()

            fragment.waitForTransition()
            val exitAnimatorRan = fragment.exitAnimatorLatch.await(
                TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            assertThat(exitAnimatorRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
        }
    }

    // Ensure when transition duration is longer than animator duration, we will get both end
    // callbacks
    @Test
    fun transitionLongerThanAnimator() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val fragment = TransitionAnimatorFragment()
            fragment.exitTransition.duration = 1000

            val fragmentManager = withActivity { supportFragmentManager }

            fragmentManager.addOnBackStackChangedListener { onBackStackChangedTimes++ }

            fragmentManager.beginTransaction()
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
            executePendingTransactions()

            fragment.waitForTransition()
            val exitAnimatorRan = fragment.exitAnimatorLatch.await(
                TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            assertThat(exitAnimatorRan).isFalse()
            assertThat(onBackStackChangedTimes).isEqualTo(2)
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
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        startAnimationLatch.countDown()
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        if (!enter) {
                            exitAnimationLatch.countDown()
                        }
                    }
                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
        }
    }

    class TransitionAnimatorFragment : TransitionFragment(R.layout.scene1) {
        val exitAnimatorLatch = CountDownLatch(1)

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
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "ordering={0}")
        fun data() = mutableListOf<Array<Any>>().apply {
            arrayOf(
                Ordered,
                Reordered
            )
        }

        @AnimRes
        private val ENTER = 1
        @AnimRes
        private val EXIT = 2

        private const val TIMEOUT = 1000L
    }
}