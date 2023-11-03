/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.transition

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.waitForExecution
import androidx.transition.test.R
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class FragmentTransitionSeekingTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(
        FragmentTransitionTestActivity::class.java
    )

    @Test
    fun replaceOperationWithTransitionsThenGestureBack() {
        val fm1 = activityRule.activity.supportFragmentManager

        var startedEnter = false
        val fragment1 = TransitionFragment(R.layout.scene1)
        fragment1.setReenterTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    startedEnter = true
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val startedExitCountDownLatch = CountDownLatch(1)
        val fragment2 = TransitionFragment()
        fragment2.setReturnTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    startedExitCountDownLatch.countDown()
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2, "2")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(startedEnter).isTrue()
        assertThat(startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            dispatcher.onBackPressed()
        }
        activityRule.executePendingTransactions(fm1)

        fragment1.waitForTransition()

        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2"))
            .isEqualTo(null)

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment1.requireView().parent).isNotNull()
    }

    @Test
    fun replaceOperationWithTransitionsThenBackCancelled() {
        val fm1 = activityRule.activity.supportFragmentManager

        var startedEnter = false
        val fragment1 = TransitionFragment(R.layout.scene1)
        fragment1.setReenterTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    startedEnter = true
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val startedExitCountDownLatch = CountDownLatch(1)
        val fragment2 = TransitionFragment()
        fragment2.setReturnTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    startedExitCountDownLatch.countDown()
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2, "2")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(startedEnter).isTrue()
        assertThat(startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackCancelled()
        }
        activityRule.executePendingTransactions(fm1)

        fragment1.waitForTransition()

        assertThat(fragment2.isAdded).isTrue()
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment2.requireView()).isNotNull()
    }

    @Test
    fun replaceOperationWithTransitionsThenGestureBackTwice() {
        val fm1 = activityRule.activity.supportFragmentManager

        var startedEnter = false
        val fragment1 = TransitionFragment(R.layout.scene1)
        fragment1.setReenterTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    startedEnter = true
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val fragment2startedExitCountDownLatch = CountDownLatch(1)
        val fragment2 = TransitionFragment()
        fragment2.setReenterTransition(Fade().apply { duration = 300 })
        fragment2.setReturnTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    fragment2startedExitCountDownLatch.countDown()
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2, "2")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val fragment3startedExitCountDownLatch = CountDownLatch(1)
        val fragment3 = TransitionFragment()
        fragment3.setReturnTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    fragment3startedExitCountDownLatch.countDown()
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment3, "3")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        assertThat(fragment3.startTransitionCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()
        // We need to wait for the exit animation to end
        assertThat(fragment2.endTransitionCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment3startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            dispatcher.onBackPressed()
        }
        activityRule.executePendingTransactions(fm1)

        fragment2.waitForTransition()
        fragment3.waitForTransition()

        assertThat(fragment3.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("3")).isEqualTo(null)

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment2.requireView().parent).isNotNull()

        val fragment2ResumedLatch = CountDownLatch(1)
        activityRule.runOnUiThread {
            fragment2.lifecycle.addObserver(
                object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event.targetState == Lifecycle.State.RESUMED) {
                            fragment2ResumedLatch.countDown()
                        }
                    }
                }
            )
        }

        assertThat(fragment2ResumedLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
            )
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(startedEnter).isTrue()
        assertThat(fragment2startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        activityRule.runOnUiThread {
            dispatcher.onBackPressed()
        }
        activityRule.executePendingTransactions(fm1)

        fragment1.waitForTransition()

        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment1.requireView().parent).isNotNull()
    }

    @Ignore // b/300694860
    @Test
    fun replaceOperationWithTransitionsThenOnBackPressedTwice() {
        val fm1 = activityRule.activity.supportFragmentManager

        var startedEnter = false
        val fragment1 = TransitionFragment(R.layout.scene1)
        fragment1.setReenterTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    startedEnter = true
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment1, "1")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val fragment2startedExitCountDownLatch = CountDownLatch(1)
        val fragment2 = TransitionFragment()
        fragment2.setReturnTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    fragment2startedExitCountDownLatch.countDown()
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment2, "2")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val fragment3startedExitCountDownLatch = CountDownLatch(1)
        val fragment3 = TransitionFragment()
        fragment3.setReturnTransition(Fade().apply {
            duration = 300
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionStart(transition: Transition) {
                    fragment3startedExitCountDownLatch.countDown()
                }
            })
        })

        fm1.beginTransaction()
            .replace(R.id.fragmentContainer, fragment3, "3")
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        assertThat(fragment3.startTransitionCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()
        // We need to wait for the exit animation to end
        assertThat(fragment2.endTransitionCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
            .isTrue()

        val dispatcher = activityRule.activity.onBackPressedDispatcher
        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        activityRule.runOnUiThread {
            dispatcher.onBackPressed()
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(fragment3startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        fragment2.waitForTransition()
        fragment3.waitForTransition()

        assertThat(fragment3.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("3")).isEqualTo(null)

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment2.requireView().parent).isNotNull()

        activityRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT))
        }
        activityRule.executePendingTransactions(fm1)

        activityRule.runOnUiThread {
            dispatcher.onBackPressed()
        }
        activityRule.executePendingTransactions(fm1)

        assertThat(startedEnter).isTrue()
        assertThat(fragment2startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

        fragment1.waitForTransition()

        assertThat(fragment2.isAdded).isFalse()
        assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

        // Make sure the original fragment was correctly readded to the container
        assertThat(fragment1.requireView().parent).isNotNull()
    }
}
