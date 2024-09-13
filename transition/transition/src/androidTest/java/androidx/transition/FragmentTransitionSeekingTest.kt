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
import android.view.View
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.waitForExecution
import androidx.testutils.withActivity
import androidx.testutils.withUse
import androidx.transition.test.R
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class FragmentTransitionSeekingTest {

    @Test
    fun replaceOperationWithTransitionsThenGestureBack() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            var startedEnter = false
            val fragment1 = TransitionFragment(R.layout.scene1)
            fragment1.setReenterTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                startedEnter = true
                            }
                        }
                    )
                }
            )

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val startedExitCountDownLatch = CountDownLatch(1)
            val fragment2 = TransitionFragment()
            fragment2.setReturnTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                startedExitCountDownLatch.countDown()
                            }
                        }
                    )
                }
            )

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment1.waitForTransition()
            fragment2.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            assertThat(startedEnter).isTrue()
            assertThat(startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            fragment1.waitForNoTransition()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @Test
    fun multipleReplaceOperationFastSystemBack() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = TransitionFragment(R.layout.scene1)
            fragment1.setReenterTransition(Fade().apply { duration = 300 })
            fragment1.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2 = TransitionFragment(R.layout.scene1)
            fragment2.setReenterTransition(Fade().apply { duration = 300 })
            fragment2.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment1.waitForTransition()
            fragment2.waitForTransition()

            val fragment3 = TransitionFragment(R.layout.scene1)
            fragment3.setReenterTransition(Fade().apply { duration = 300 })
            fragment3.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment2.waitForTransition()
            fragment3.waitForTransition()

            val fragment4 = TransitionFragment(R.layout.scene1)
            fragment4.setReenterTransition(Fade().apply { duration = 300 })
            fragment4.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment4, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment3.waitForTransition()
            fragment4.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            fragment1.waitForNoTransition()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @Test
    fun multipleReplaceOperationFastGestureBack() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = TransitionFragment(R.layout.scene1)
            fragment1.setReenterTransition(Fade().apply { duration = 300 })
            fragment1.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2 = TransitionFragment(R.layout.scene1)
            fragment2.setReenterTransition(Fade().apply { duration = 300 })
            fragment2.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment1.waitForTransition()
            fragment2.waitForTransition()

            val fragment3 = TransitionFragment(R.layout.scene1)
            fragment3.setReenterTransition(Fade().apply { duration = 300 })
            fragment3.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment2.waitForTransition()
            fragment3.waitForTransition()

            val fragment4 = TransitionFragment(R.layout.scene1)
            fragment4.setReenterTransition(Fade().apply { duration = 300 })
            fragment4.setReturnTransition(Fade().apply { duration = 300 })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment4, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment3.waitForTransition()
            fragment4.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }
            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }
            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }
            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            fragment1.waitForNoTransition()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @Test
    fun replaceOperationWithTransitionsThenBackCancelled() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }
            val startedEnterCountDownLatch = CountDownLatch(1)
            val fragment1 = StrictViewFragment(R.layout.scene1)
            val transitionEndCountDownLatch = CountDownLatch(1)
            fragment1.reenterTransition =
                (Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                startedEnterCountDownLatch.countDown()
                            }

                            override fun onTransitionCancel(transition: Transition) {
                                transitionEndCountDownLatch.countDown()
                            }
                        }
                    )
                })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val startedExitCountDownLatch = CountDownLatch(1)
            val fragment2 = StrictViewFragment()
            fragment2.returnTransition =
                (Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                startedExitCountDownLatch.countDown()
                            }
                        }
                    )
                })

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }

            assertThat(startedEnterCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
            assertThat(startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            withActivity { dispatcher.dispatchOnBackCancelled() }
            executePendingTransactions()

            assertThat(fragment2.isAdded).isTrue()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(fragment2)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment2.requireView()).isNotNull()

            assertThat(transitionEndCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    @Test
    fun replaceOperationWithTransitionsThenGestureBackTwice() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            var startedEnter = false
            val fragment1 = TransitionFragment(R.layout.scene1)
            fragment1.setReenterTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                startedEnter = true
                            }
                        }
                    )
                }
            )
            fragment1.sharedElementEnterTransition = null
            fragment1.sharedElementReturnTransition = null

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2startedExitCountDownLatch = CountDownLatch(1)
            val fragment2 = TransitionFragment()
            fragment2.setReenterTransition(Fade().apply { duration = 300 })
            fragment2.setReturnTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                fragment2startedExitCountDownLatch.countDown()
                            }
                        }
                    )
                }
            )
            fragment2.sharedElementEnterTransition = null
            fragment2.sharedElementReturnTransition = null

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment1.waitForTransition()
            fragment2.waitForTransition()

            val fragment3startedExitCountDownLatch = CountDownLatch(1)
            val fragment3 = TransitionFragment()
            fragment3.setReturnTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                fragment3startedExitCountDownLatch.countDown()
                            }
                        }
                    )
                }
            )
            fragment3.sharedElementEnterTransition = null
            fragment3.sharedElementReturnTransition = null

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            // We need to wait for the exit animation to end
            fragment2.waitForTransition()
            fragment3.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            assertThat(fragment3startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            fragment2.waitForNoTransition()
            fragment3.waitForNoTransition()

            assertThat(fragment3.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("3")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment2.requireView().parent).isNotNull()

            val fragment2ResumedLatch = CountDownLatch(1)
            withActivity {
                fragment2.lifecycle.addObserver(
                    object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            if (event.targetState == Lifecycle.State.RESUMED) {
                                fragment2ResumedLatch.countDown()
                            }
                        }
                    }
                )
            }

            assertThat(fragment2ResumedLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            withActivity {
                dispatcher.dispatchOnBackProgressed(
                    BackEventCompat(0.2F, 0.2F, 0.2F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            assertThat(startedEnter).isTrue()
            assertThat(fragment2startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            fragment1.waitForNoTransition()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @Test
    fun replaceOperationWithTransitionsThenOnBackPressedTwice() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            var startedEnter = false
            val fragment1 = TransitionFragment(R.layout.scene1)
            fragment1.setReenterTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                startedEnter = true
                            }
                        }
                    )
                }
            )

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2startedExitCountDownLatch = CountDownLatch(1)
            val fragment2 = TransitionFragment()
            fragment2.setReturnTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                fragment2startedExitCountDownLatch.countDown()
                            }
                        }
                    )
                }
            )

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment1.waitForTransition()
            fragment2.waitForTransition()

            val fragment3startedExitCountDownLatch = CountDownLatch(1)
            val fragment3 = TransitionFragment()
            fragment3.setReturnTransition(
                Fade().apply {
                    duration = 300
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                fragment3startedExitCountDownLatch.countDown()
                            }
                        }
                    )
                }
            )

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            // We need to wait for the exit animation to end
            fragment2.waitForTransition()
            fragment3.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            fragment2.waitForNoTransition()
            fragment3.waitForNoTransition()

            assertThat(fragment3startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            assertThat(fragment3.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("3")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment2.requireView().parent).isNotNull()

            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            withActivity { dispatcher.onBackPressed() }
            waitForExecution()

            assertThat(startedEnter).isTrue()
            assertThat(fragment2startedExitCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            fragment1.waitForNoTransition()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun replaceOperationWithAnimatorsInterruptCommit() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = TransitionFragment(R.layout.scene1)
            fragment1.exitTransition.apply {
                setRealTransition(true)
                duration = 1000
            }
            fragment1.reenterTransition.apply {
                setRealTransition(true)
                duration = 1000
            }
            withActivity {
                fm1.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment1, "1")
                    .addToBackStack(null)
                    .commit()
                fm1.executePendingTransactions()
            }

            val fragment2 = TransitionFragment()
            fragment2.enterTransition.apply {
                setRealTransition(true)
                duration = 1000
            }
            fragment2.returnTransition.apply {
                setRealTransition(true)
                duration = 1000
            }

            var resumedBeforeOnBackStarted = false
            var resumedAfterOnBackStarted = false

            withActivity {
                fm1.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment2, "2")
                    .addToBackStack(null)
                    .commit()
                fm1.executePendingTransactions()

                resumedBeforeOnBackStarted = fragment2.isResumed

                val dispatcher = onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
                resumedAfterOnBackStarted = fragment2.isResumed

                dispatcher.onBackPressed()
            }

            assertThat(resumedBeforeOnBackStarted).isFalse()
            assertThat(resumedAfterOnBackStarted).isTrue()
        }
    }

    @Test
    fun GestureBackWithNonSeekableSharedElement() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment(R.layout.scene1)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2 = TransitionFragment(R.layout.scene6)
            fragment2.setEnterTransition(Fade())
            fragment2.setReturnTransition(Fade())

            val greenSquare = fragment1.requireView().findViewById<View>(R.id.greenSquare)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .addSharedElement(greenSquare, "green")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment2.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            executePendingTransactions()

            withActivity { dispatcher.onBackPressed() }
            executePendingTransactions()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isEqualTo(null)

            // Make sure the original fragment was correctly readded to the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @Test
    fun GestureBackWithNonSeekableSharedElementCancelled() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment(R.layout.scene1)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2 = TransitionFragment(R.layout.scene6)
            fragment2.setEnterTransition(Fade())
            fragment2.setReturnTransition(Fade())

            val greenSquare = fragment1.requireView().findViewById<View>(R.id.greenSquare)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .addSharedElement(greenSquare, "green")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment2.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }

            withActivity { dispatcher.dispatchOnBackCancelled() }
            executePendingTransactions()

            assertThat(fragment1.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("2")).isNotNull()

            // Make sure that fragment 1 does not have a view
            assertThat(fragment1.view).isNull()
            // Make sure fragment2 is still in the container
            assertThat(fragment2.requireView().parent).isNotNull()
        }
    }

    @Test
    fun gestureBackWithNonSeekableSharedElementCancelInterruptedBack() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment(R.layout.scene1)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2 = TransitionFragment(R.layout.scene6)
            fragment2.setEnterTransition(Fade())
            fragment2.setReturnTransition(Fade())

            val greenSquare = fragment1.requireView().findViewById<View>(R.id.greenSquare)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .addSharedElement(greenSquare, "green")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            fragment2.waitForTransition()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }

            withActivity { supportFragmentManager.popBackStackImmediate() }

            withActivity { dispatcher.dispatchOnBackCancelled() }
            executePendingTransactions()

            assertThat(fragment2.isAdded).isFalse()
            assertThat(fm1.findFragmentByTag("1")).isNotNull()

            // Make sure that fragment 2 does not have a view
            assertThat(fragment2.view).isNull()
            // Make sure fragment1 is still in the container
            assertThat(fragment1.requireView().parent).isNotNull()
        }
    }

    @Test
    fun gestureBackWithNonSeekableSharedElementCancelInterruptedForward() {
        withUse(ActivityScenario.launch(FragmentTransitionTestActivity::class.java)) {
            val fm1 = withActivity { supportFragmentManager }

            val fragment1 = StrictViewFragment(R.layout.scene1)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            val fragment2 = StrictViewFragment(R.layout.scene6)
            val fragment2EnterCountDownLatch = CountDownLatch(1)
            fragment2.enterTransition =
                Fade().apply {
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionEnd(transition: Transition) {
                                fragment2EnterCountDownLatch.countDown()
                            }
                        }
                    )
                }
            fragment2.returnTransition = Fade()

            val greenSquare = fragment1.requireView().findViewById<View>(R.id.greenSquare)

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2, "2")
                .addSharedElement(greenSquare, "green")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            waitForExecution()

            assertThat(fragment2EnterCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            val dispatcher = withActivity { onBackPressedDispatcher }
            withActivity {
                dispatcher.dispatchOnBackStarted(
                    BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
                )
            }
            waitForExecution()

            val fragment3 = StrictViewFragment(R.layout.scene6)
            val fragment3EnterCountDownLatch = CountDownLatch(1)
            fragment3.enterTransition =
                Fade().apply {
                    addListener(
                        object : TransitionListenerAdapter() {
                            override fun onTransitionEnd(transition: Transition) {
                                fragment3EnterCountDownLatch.countDown()
                            }
                        }
                    )
                }

            fm1.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3, "3")
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
            executePendingTransactions()

            withActivity { dispatcher.dispatchOnBackCancelled() }
            executePendingTransactions()

            assertThat(fragment3.isAdded).isTrue()
            assertThat(fm1.findFragmentByTag("3")).isNotNull()

            // we verify the state of fragment2 here as this is a transitioning, non-seekable case
            // so we wouldn't actually have run the animation until the back press was completed.
            assertThat(fragment2.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
            assertThat(fragment3EnterCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()

            // Make sure that fragment 2 does not have a view
            assertThat(fragment2.view).isNull()
            // Make sure that fragment 2 does not have a view
            assertThat(fragment3.view).isNotNull()
            // Make sure fragment3 is still in the container
            assertThat(fragment3.requireView().parent).isNotNull()
        }
    }
}
