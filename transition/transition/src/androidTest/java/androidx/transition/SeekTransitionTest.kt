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

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.core.util.Consumer
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.AnimationDurationScaleRule.Companion.createForAllTests
import androidx.testutils.PollingCheck
import androidx.transition.Transition.TransitionListener
import androidx.transition.test.R
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.timeout
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@MediumTest
class SeekTransitionTest : BaseTest() {
    @get:Rule val animationDurationScaleRule = createForAllTests(1f)

    lateinit var view: View
    lateinit var root: LinearLayout
    lateinit var transition: Transition

    @UiThreadTest
    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        root = rule.activity.findViewById<View>(R.id.root) as LinearLayout
        transition = Fade().also { it.interpolator = LinearInterpolator() }
        view = View(root.context)
        view.setBackgroundColor(Color.BLUE)
        root.addView(view, ViewGroup.LayoutParams(100, 100))
    }

    @Test(expected = IllegalArgumentException::class)
    @UiThreadTest
    fun onlySeekingTransitions() {
        transition = object : Visibility() {}
        TransitionManager.controlDelayedTransition(root, transition)
        fail("Expected IllegalArgumentException")
    }

    @Test
    fun waitForReady() {
        lateinit var seekController: TransitionSeekController

        @Suppress("UNCHECKED_CAST")
        val readyCall: Consumer<TransitionSeekController> =
            mock(Consumer::class.java) as Consumer<TransitionSeekController>

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            assertThat(seekController.isReady).isFalse()
            seekController.addOnReadyListener(readyCall)
            view.visibility = View.GONE
        }

        verify(readyCall, timeout(3000)).accept(seekController)
        assertThat(seekController.isReady).isTrue()
    }

    @Test
    fun waitForReadyNoChange() {
        lateinit var seekController: TransitionSeekController

        @Suppress("UNCHECKED_CAST")
        val readyCall: Consumer<TransitionSeekController> =
            mock(Consumer::class.java) as Consumer<TransitionSeekController>

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            assertThat(seekController.isReady).isFalse()
            seekController.addOnReadyListener(readyCall)
            view.requestLayout()
        }

        verify(readyCall, timeout(3000)).accept(seekController)
    }

    @Test
    fun addListenerAfterReady() {
        lateinit var seekController: TransitionSeekController

        @Suppress("UNCHECKED_CAST")
        val readyCall: Consumer<TransitionSeekController> =
            mock(Consumer::class.java) as Consumer<TransitionSeekController>

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            assertThat(seekController.isReady).isFalse()
            seekController.addOnReadyListener(readyCall)
            view.visibility = View.GONE
        }

        verify(readyCall, timeout(3000)).accept(seekController)

        @Suppress("UNCHECKED_CAST")
        val readyCall2: Consumer<TransitionSeekController> =
            mock(Consumer::class.java) as Consumer<TransitionSeekController>

        seekController.addOnReadyListener(readyCall2)
        verify(readyCall, times(1)).accept(seekController)
    }

    @Test
    fun seekTransition() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            assertThat(seekController.isReady).isFalse()
            view.visibility = View.GONE
        }

        verify(listener, timeout(1000)).onTransitionStart(any())
        verify(listener, times(0)).onTransitionEnd(any())

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)

            assertThat(seekController.durationMillis).isEqualTo(300)
            assertThat(seekController.currentPlayTimeMillis).isEqualTo(0)
            assertThat(seekController.currentFraction).isEqualTo(0f)

            assertThat(view.transitionAlpha).isEqualTo(1f)

            seekController.currentPlayTimeMillis = 150
            assertThat(seekController.currentFraction).isEqualTo(0.5f)
            assertThat(view.transitionAlpha).isEqualTo(0.5f)
            seekController.currentPlayTimeMillis = 299
            assertThat(seekController.currentFraction).isWithin(0.001f).of(299f / 300f)
            assertThat(view.transitionAlpha).isWithin(0.001f).of(1f / 300f)
            seekController.currentPlayTimeMillis = 300
            assertThat(seekController.currentFraction).isEqualTo(1f)
            verify(listener, times(1)).onTransitionEnd(any())

            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun seekTransitionWithFraction() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            assertThat(seekController.isReady).isFalse()
            view.visibility = View.GONE
        }

        verify(listener, timeout(1000)).onTransitionStart(any())
        verify(listener, times(0)).onTransitionEnd(any())

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)

            assertThat(seekController.durationMillis).isEqualTo(300)
            assertThat(seekController.currentPlayTimeMillis).isEqualTo(0)
            assertThat(seekController.currentFraction).isEqualTo(0f)

            assertThat(view.transitionAlpha).isEqualTo(1f)

            seekController.currentFraction = 0.5f
            assertThat(seekController.currentPlayTimeMillis).isEqualTo(150)
            assertThat(view.transitionAlpha).isEqualTo(0.5f)
            seekController.currentFraction = 299f / 300f
            assertThat(seekController.currentPlayTimeMillis).isEqualTo(299)
            assertThat(view.transitionAlpha).isWithin(0.001f).of(1f / 300f)
            seekController.currentFraction = 1f
            assertThat(seekController.currentPlayTimeMillis).isEqualTo(300)
            verify(listener, times(1)).onTransitionEnd(any())

            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun animationDoesNotTakeOverSeek() {
        lateinit var seekController: TransitionSeekController

        val stateListener1 = spy(TransitionListenerAdapter())
        transition.addListener(stateListener1)
        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            assertThat(seekController.isReady).isFalse()
            view.visibility = View.GONE
        }

        verify(stateListener1, timeout(3000)).onTransitionStart(any())

        val stateListener2 = spy(TransitionListenerAdapter())
        val transition2 = Fade()
        transition2.addListener(stateListener2)

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            TransitionManager.beginDelayedTransition(root, transition2)
            view.visibility = View.GONE
        }

        verify(stateListener2, timeout(3000)).onTransitionStart(any())
        verify(stateListener2, timeout(3000)).onTransitionEnd(any())
        verify(stateListener1, times(0)).onTransitionEnd(any())
        verify(stateListener1, times(0)).onTransitionCancel(any())

        rule.runOnUiThread {
            // Seek is still controlling the visibility
            assertThat(view.transitionAlpha).isEqualTo(0.5f)
            assertThat(view.visibility).isEqualTo(View.VISIBLE)

            seekController.currentPlayTimeMillis = 300
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun seekCannotTakeOverAnimation() {
        lateinit var seekController: TransitionSeekController

        val stateListener1 = spy(TransitionListenerAdapter())
        transition.addListener(stateListener1)
        transition.duration = 1000
        transition.addListener(TransitionListenerAdapter())
        rule.runOnUiThread {
            TransitionManager.beginDelayedTransition(root, transition)
            view.visibility = View.GONE
        }

        verify(stateListener1, timeout(3000)).onTransitionStart(any())

        val stateListener2 = spy(TransitionListenerAdapter())
        val transition2 = Fade()
        transition2.duration = 3000
        transition2.addListener(stateListener2)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition2)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            // The second transition doesn't have any animations in it because it didn't take over.
            assertThat(seekController.isReady).isTrue()
            assertThat(seekController.durationMillis).isEqualTo(0)
        }

        // The first animation should continue
        verify(stateListener1, timeout(3000)).onTransitionEnd(any())

        // The animation is ended
        assertThat(view.transitionAlpha).isEqualTo(1f)
        assertThat(view.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun seekCannotTakeOverSeek() {
        lateinit var seekController1: TransitionSeekController

        val stateListener1 = spy(TransitionListenerAdapter())
        transition.addListener(stateListener1)
        transition.duration = 3000
        rule.runOnUiThread {
            seekController1 = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread { assertThat(seekController1.isReady).isTrue() }

        val stateListener2 = spy(TransitionListenerAdapter())
        val transition2 = Fade()
        transition2.duration = 3000
        transition2.addListener(stateListener2)

        rule.runOnUiThread {
            seekController1.currentPlayTimeMillis = 1500
            // First transition should be started now
            verify(stateListener1, times(1)).onTransitionStart(any())
            TransitionManager.controlDelayedTransition(root, transition2)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            // second transition should just start/end immediately
            verify(stateListener2, times(1)).onTransitionStart(any())
            verify(stateListener2, times(1)).onTransitionEnd(any())

            // first transition should still be controllable
            verify(stateListener1, times(0)).onTransitionEnd(any())

            // second transition should be ready and taking over the previous animation
            assertThat(view.transitionAlpha).isEqualTo(0.5f)
            seekController1.currentPlayTimeMillis = 3000
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun seekReplacesSeek() {
        lateinit var seekController1: TransitionSeekController

        val stateListener1 = spy(TransitionListenerAdapter())
        transition.addListener(stateListener1)
        transition.duration = 3000
        rule.runOnUiThread {
            seekController1 = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        verify(stateListener1, timeout(3000)).onTransitionStart(any())

        val stateListener2 = spy(TransitionListenerAdapter())
        val transition2 = Fade()
        transition2.duration = 3000
        transition2.addListener(stateListener2)

        lateinit var seekController2: TransitionSeekController
        rule.runOnUiThread {
            seekController1.currentPlayTimeMillis = 1500
            seekController2 = TransitionManager.controlDelayedTransition(root, transition2)!!
            view.visibility = View.VISIBLE
        }

        verify(stateListener2, timeout(3000)).onTransitionStart(any())
        rule.runOnUiThread {}
        verify(stateListener1, times(1)).onTransitionEnd(any())
        assertThat(seekController2.isReady).isTrue()

        rule.runOnUiThread {
            verify(stateListener2, never()).onTransitionEnd(any())
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(0.5f)
            seekController2.currentPlayTimeMillis = 3000
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            verify(stateListener2, times(1)).onTransitionEnd(any())
        }
    }

    @Test
    fun animateToEnd() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToEnd()
        }

        verify(listener, timeout(3000)).onTransitionEnd(any())
        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
            val runningTransitions = TransitionManager.getRunningTransitions()
            assertThat(runningTransitions[root]).isEmpty()
        }
    }

    @Test
    fun animateToStart() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToStart { view.visibility = View.VISIBLE }
        }

        verify(listener, timeout(3000)).onTransitionEnd(any())
        val listener2 = spy(TransitionListenerAdapter())
        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)

            // Now set it back to the original state -- no transition should happen
            val fade = Fade().also { it.addListener(listener2) }
            TransitionManager.beginDelayedTransition(root, fade)
            view.visibility = View.VISIBLE
            root.invalidate()
        }

        rule.runOnUiThread {
            verify(listener2, times(1)).onTransitionStart(any(), eq(false))
            verify(listener2, times(1)).onTransitionEnd(any(), eq(false))
        }
    }

    @Test
    fun animateToStartNoReset() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToStart {}
        }

        verify(listener, timeout(3000)).onTransitionEnd(any())
        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
        }
    }

    @Test
    fun animateToStartAfterAnimateToEnd() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToEnd()
        }

        rule.runOnUiThread { seekController.animateToStart { view.visibility = View.VISIBLE } }

        verify(listener, timeout(3000)).onTransitionEnd(any(), eq(true))

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
        }
    }

    @Test
    fun animateToEndAfterAnimateToStart() {
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToStart { view.visibility = View.VISIBLE }
        }

        rule.runOnUiThread { seekController.animateToEnd() }

        verify(listener, timeout(3000)).onTransitionEnd(any())

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun seekAfterAnimate() {
        lateinit var seekController: TransitionSeekController
        transition.duration = 5000

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToEnd()
        }

        rule.runOnUiThread { seekController.currentPlayTimeMillis = 120 }
    }

    @Test(expected = IllegalStateException::class)
    fun seekFractionAfterAnimate() {
        lateinit var seekController: TransitionSeekController
        transition.duration = 5000

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentFraction = 0.5f
            seekController.animateToEnd()
        }

        rule.runOnUiThread { seekController.currentFraction = 0.2f }
    }

    @Test
    fun seekTransitionSet() {
        transition =
            TransitionSet().also {
                it.addTransition(Fade(Fade.MODE_OUT)).addTransition(Fade(Fade.MODE_IN)).ordering =
                    TransitionSet.ORDERING_SEQUENTIAL
            }
        val view2 = View(root.context)
        view2.setBackgroundColor(Color.GREEN)

        val view3 = View(root.context)
        view3.setBackgroundColor(Color.RED)

        rule.runOnUiThread {
            root.addView(view2, ViewGroup.LayoutParams(100, 100))
            root.addView(view3, ViewGroup.LayoutParams(100, 100))
            view2.visibility = View.GONE
        }

        lateinit var seekController: TransitionSeekController

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view2.visibility = View.VISIBLE
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            assertThat(seekController.durationMillis).isEqualTo(600)
            assertThat(seekController.currentPlayTimeMillis).isEqualTo(0)
            seekController.currentPlayTimeMillis = 0

            // We should be at the start
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(0f)

            // seek to the end of the fade out
            seekController.currentPlayTimeMillis = 300

            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(0f)

            // seek to the end of transition
            seekController.currentPlayTimeMillis = 600
            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(1f)

            // seek back to the middle
            seekController.currentPlayTimeMillis = 300

            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(0f)

            // back to the beginning
            seekController.currentPlayTimeMillis = 0

            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(0f)
        }
    }

    @Test
    fun animateToEndTransitionSet() {
        transition =
            TransitionSet().also {
                it.addTransition(Fade(Fade.MODE_OUT)).addTransition(Fade(Fade.MODE_IN)).ordering =
                    TransitionSet.ORDERING_SEQUENTIAL
            }
        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        val view2 = View(root.context)
        view2.setBackgroundColor(Color.GREEN)

        val view3 = View(root.context)
        view3.setBackgroundColor(Color.RED)

        rule.runOnUiThread {
            root.addView(view2, ViewGroup.LayoutParams(100, 100))
            root.addView(view3, ViewGroup.LayoutParams(100, 100))
            view2.visibility = View.GONE
        }

        lateinit var seekController: TransitionSeekController

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view2.visibility = View.VISIBLE
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            verify(listener, times(0)).onTransitionEnd(any())
            // seek to the end of the fade out
            seekController.currentPlayTimeMillis = 300
            verify(listener, times(0)).onTransitionEnd(any())

            seekController.animateToEnd()
        }
        verify(listener, timeout(3000)).onTransitionEnd(any())

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(1f)
            val runningTransitions = TransitionManager.getRunningTransitions()
            assertThat(runningTransitions[root]).isEmpty()
        }
    }

    @Test
    fun animateToStartTransitionSet() {
        transition =
            TransitionSet().also {
                it.addTransition(Fade(Fade.MODE_OUT)).addTransition(Fade(Fade.MODE_IN)).ordering =
                    TransitionSet.ORDERING_SEQUENTIAL
            }
        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        val view2 = View(root.context)
        view2.setBackgroundColor(Color.GREEN)

        val view3 = View(root.context)
        view3.setBackgroundColor(Color.RED)

        rule.runOnUiThread {
            root.addView(view2, ViewGroup.LayoutParams(100, 100))
            root.addView(view3, ViewGroup.LayoutParams(100, 100))
            view2.visibility = View.GONE
        }

        lateinit var seekController: TransitionSeekController

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view2.visibility = View.VISIBLE
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            // seek to near the end of the fade out
            seekController.currentPlayTimeMillis = 299

            seekController.animateToStart {
                view.visibility = View.VISIBLE
                view2.visibility = View.GONE
            }
        }
        verify(listener, timeout(3000)).onTransitionEnd(any(), eq(true))
        verify(listener, never()).onTransitionEnd(any(), eq(false))
        verify(listener, times(1)).onTransitionEnd(any(), eq(true))

        val transition2 =
            TransitionSet().also {
                it.addTransition(Fade(Fade.MODE_OUT)).addTransition(Fade(Fade.MODE_IN)).ordering =
                    TransitionSet.ORDERING_SEQUENTIAL
                it.duration = 0
            }

        val listener2 = spy(TransitionListenerAdapter())
        transition2.addListener(listener2)
        rule.runOnUiThread {
            TransitionManager.beginDelayedTransition(root, transition2)
            view.visibility = View.VISIBLE
            view2.visibility = View.GONE
            root.invalidate()
        }

        rule.runOnUiThread {
            // It should start and end in the same frame
            verify(listener2, times(1)).onTransitionStart(any(), eq(false))
            verify(listener2, times(1)).onTransitionEnd(any(), eq(false))
            val runningTransitions = TransitionManager.getRunningTransitions()
            assertThat(runningTransitions[root]).isEmpty()
        }
    }

    @Test
    fun cancelPartOfTransitionSet() {
        transition =
            TransitionSet().also {
                it.addTransition(Fade(Fade.MODE_OUT)).addTransition(Fade(Fade.MODE_IN)).ordering =
                    TransitionSet.ORDERING_SEQUENTIAL
            }
        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        val view2 = View(root.context)
        view2.setBackgroundColor(Color.GREEN)

        val view3 = View(root.context)
        view3.setBackgroundColor(Color.RED)

        rule.runOnUiThread {
            root.addView(view2, ViewGroup.LayoutParams(100, 100))
            root.addView(view3, ViewGroup.LayoutParams(100, 100))
            view2.visibility = View.GONE
        }

        lateinit var seekController: TransitionSeekController

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view2.visibility = View.VISIBLE
            view.visibility = View.GONE
        }

        val transition2 =
            TransitionSet().also {
                it.addTransition(Fade(Fade.MODE_OUT)).addTransition(Fade(Fade.MODE_IN)).ordering =
                    TransitionSet.ORDERING_SEQUENTIAL
            }

        val listener2 = spy(TransitionListenerAdapter())
        transition2.addListener(listener2)

        rule.runOnUiThread {
            // seek to the end of the fade out
            seekController.currentPlayTimeMillis = 300
            TransitionManager.beginDelayedTransition(root, transition2)
            // Undo making the view visible
            view2.visibility = View.GONE
        }
        verify(listener2, timeout(3000)).onTransitionStart(any())
        verify(listener2, timeout(3000)).onTransitionEnd(any())

        // The first transition shouldn't end. You can still control it.
        verify(listener, times(0)).onTransitionEnd(any())

        rule.runOnUiThread {
            // view2 should now be gone
            assertThat(view2.visibility).isEqualTo(View.GONE)
            assertThat(view2.transitionAlpha).isEqualTo(1f)

            // Try to seek further. It should not affect view2 because that transition should be
            // canceled.
            seekController.currentPlayTimeMillis = 600
            assertThat(view2.visibility).isEqualTo(View.GONE)
            assertThat(view2.transitionAlpha).isEqualTo(1f)

            verify(listener, times(1)).onTransitionEnd(any())
        }
    }

    @Test
    fun onTransitionCallsForwardAndReversed() {
        val listener = spy(TransitionListenerAdapter())
        transition = Fade()
        transition.addListener(listener)

        lateinit var seekController: TransitionSeekController
        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }
        rule.runOnUiThread {
            verifyCallCounts(listener, startForward = 1)
            seekController.currentPlayTimeMillis = 300
            verifyCallCounts(listener, startForward = 1, endForward = 1)
            seekController.currentPlayTimeMillis = 150
            verifyCallCounts(listener, startForward = 1, endForward = 1, startReverse = 1)
            seekController.currentPlayTimeMillis = 0
            verifyCallCounts(
                listener,
                startForward = 1,
                endForward = 1,
                startReverse = 1,
                endReverse = 1
            )
        }
    }

    @Test
    fun onTransitionCallsForwardAndReversedTransitionSet() {
        val fadeOut = Fade(Fade.MODE_OUT)
        val outListener = spy(TransitionListenerAdapter())
        fadeOut.addListener(outListener)
        val fadeIn = Fade(Fade.MODE_IN)
        val inListener = spy(TransitionListenerAdapter())
        fadeIn.addListener(inListener)
        val set = TransitionSet()
        set.addTransition(fadeOut)
        set.addTransition(fadeIn)
        set.setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
        val setListener = spy(TransitionListenerAdapter())
        set.addListener(setListener)

        val view2 = View(view.context)

        lateinit var seekController: TransitionSeekController
        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, set)!!
            view.visibility = View.GONE
            root.addView(view2, ViewGroup.LayoutParams(100, 100))
        }

        rule.runOnUiThread {
            verifyCallCounts(setListener, startForward = 1)
            verifyCallCounts(outListener, startForward = 1)
            verifyCallCounts(inListener)
            seekController.currentPlayTimeMillis = 301
            verifyCallCounts(setListener, startForward = 1)
            verifyCallCounts(outListener, startForward = 1, endForward = 1)
            verifyCallCounts(inListener, startForward = 1)
            seekController.currentPlayTimeMillis = 600
            verifyCallCounts(setListener, startForward = 1, endForward = 1)
            verifyCallCounts(outListener, startForward = 1, endForward = 1)
            verifyCallCounts(inListener, startForward = 1, endForward = 1)
            seekController.currentPlayTimeMillis = 301
            verifyCallCounts(setListener, startForward = 1, endForward = 1, startReverse = 1)
            verifyCallCounts(outListener, startForward = 1, endForward = 1)
            verifyCallCounts(inListener, startForward = 1, endForward = 1, startReverse = 1)
            seekController.currentPlayTimeMillis = 299
            verifyCallCounts(setListener, startForward = 1, endForward = 1, startReverse = 1)
            verifyCallCounts(outListener, startForward = 1, endForward = 1, startReverse = 1)
            verifyCallCounts(
                inListener,
                startForward = 1,
                endForward = 1,
                startReverse = 1,
                endReverse = 1
            )
            seekController.currentPlayTimeMillis = 0
            verifyCallCounts(
                setListener,
                startForward = 1,
                endForward = 1,
                startReverse = 1,
                endReverse = 1
            )
            verifyCallCounts(
                outListener,
                startForward = 1,
                endForward = 1,
                startReverse = 1,
                endReverse = 1
            )
            verifyCallCounts(
                inListener,
                startForward = 1,
                endForward = 1,
                startReverse = 1,
                endReverse = 1
            )
        }
    }

    private fun verifyCallCounts(
        listener: TransitionListener,
        startForward: Int = 0,
        endForward: Int = 0,
        startReverse: Int = 0,
        endReverse: Int = 0
    ) {
        verify(listener, times(startForward)).onTransitionStart(any(), eq(false))
        verify(listener, times(endForward)).onTransitionEnd(any(), eq(false))
        verify(listener, times(startReverse)).onTransitionStart(any(), eq(true))
        verify(listener, times(endReverse)).onTransitionEnd(any(), eq(true))
    }

    @Test
    fun pauseResumeOnSeek() {
        var pauseCount = 0
        var resumeCount = 0
        var setPauseCount = 0
        var setResumeCount = 0
        val set =
            TransitionSet().also {
                it.addTransition(
                    Fade().apply {
                        addListener(
                            object : TransitionListenerAdapter() {
                                override fun onTransitionPause(transition: Transition) {
                                    pauseCount++
                                }

                                override fun onTransitionResume(transition: Transition) {
                                    resumeCount++
                                }
                            }
                        )
                    }
                )
                it.addListener(
                    object : TransitionListenerAdapter() {
                        override fun onTransitionPause(transition: Transition) {
                            setPauseCount++
                        }

                        override fun onTransitionResume(transition: Transition) {
                            setResumeCount++
                        }
                    }
                )
            }
        transition =
            TransitionSet().also {
                it.addTransition(AlwaysTransition("before"))
                it.addTransition(set)
                it.setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
            }

        lateinit var seekController: TransitionSeekController
        lateinit var view: View

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view = View(rule.activity)
            root.addView(view, ViewGroup.LayoutParams(100, 100))
        }

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(0f)

            // Move it to the end and then back to the beginning:
            seekController.currentPlayTimeMillis = 600
            seekController.currentPlayTimeMillis = 0

            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            assertThat(pauseCount).isEqualTo(1)
            assertThat(resumeCount).isEqualTo(1)
            assertThat(setPauseCount).isEqualTo(1)
            assertThat(setResumeCount).isEqualTo(1)
        }
    }

    @Test
    fun animationListener() {
        lateinit var seekController: TransitionSeekController
        var animatedFraction = -1f
        var animatedMillis = -1L
        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, Fade())!!
            view.visibility = View.GONE

            seekController.addOnProgressChangedListener {
                animatedFraction = it.currentFraction
                animatedMillis = it.currentPlayTimeMillis
            }
        }

        rule.runOnUiThread {
            assertThat(animatedFraction).isEqualTo(0f)
            assertThat(animatedMillis).isEqualTo(0)
            seekController.currentFraction = 0.25f
            assertThat(animatedFraction).isEqualTo(0.25f)
            assertThat(animatedMillis).isEqualTo(75)
            seekController.animateToEnd()
        }

        PollingCheck.waitFor { animatedFraction == 1f }
    }

    @Test
    fun animationListenerRemoval() {
        lateinit var seekController: TransitionSeekController
        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, Fade())!!
            view.visibility = View.GONE
        }

        var animatedFraction = -1f
        var animatedMillis = -1L
        val removeListener =
            object : Consumer<TransitionSeekController> {
                override fun accept(value: TransitionSeekController) {
                    seekController.removeOnProgressChangedListener(this)
                }
            }
        seekController.addOnProgressChangedListener(removeListener)
        val changeListener =
            Consumer<TransitionSeekController> {
                animatedFraction = it.currentFraction
                animatedMillis = it.currentPlayTimeMillis
            }
        seekController.addOnProgressChangedListener(changeListener)

        rule.runOnUiThread {
            assertThat(animatedFraction).isEqualTo(0f)
            assertThat(animatedMillis).isEqualTo(0)
            seekController.removeOnProgressChangedListener(changeListener)
            seekController.currentFraction = 0.25f
            assertThat(animatedFraction).isEqualTo(0)
            assertThat(animatedMillis).isEqualTo(0)
        }
    }

    @Test
    fun seekToScene() {
        lateinit var seekController: TransitionSeekController
        val scene1 = Scene(root, view)
        val view2 = View(view.context)
        val scene2 = Scene(root, view2)
        rule.runOnUiThread { TransitionManager.go(scene1) }

        rule.runOnUiThread {
            val controller = TransitionManager.createSeekController(scene2, Fade())
            assertThat(controller).isNotNull()
            seekController = controller!!
        }

        rule.runOnUiThread {
            assertThat(seekController.currentFraction).isEqualTo(0f)
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.isAttachedToWindow).isTrue()
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(0f)
            assertThat(view2.isAttachedToWindow).isTrue()
            seekController.currentFraction = 1f
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.isAttachedToWindow).isFalse()
            assertThat(view2.visibility).isEqualTo(View.VISIBLE)
            assertThat(view2.transitionAlpha).isEqualTo(1f)
            assertThat(view2.isAttachedToWindow).isTrue()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun seekToScene_notSupportedTransition() {
        class NoSeekingTransition : Fade() {
            override fun isSeekingSupported(): Boolean = false
        }
        val scene1 = Scene(root, view)
        val view2 = View(view.context)
        val scene2 = Scene(root, view2)
        rule.runOnUiThread { TransitionManager.go(scene1) }

        rule.runOnUiThread { TransitionManager.createSeekController(scene2, NoSeekingTransition()) }
    }

    @Test
    fun seekToScene_alreadyRunningTransition() {
        val scene1 = Scene(root, view)
        val view2 = View(view.context)
        val scene2 = Scene(root, view2)
        rule.runOnUiThread { TransitionManager.go(scene1) }

        rule.runOnUiThread {
            TransitionManager.go(scene2, Fade())
            assertThat(TransitionManager.createSeekController(scene1, Fade())).isNull()
        }
    }

    // onTransitionEnd() listeners should be called after the animateToStart() lambda has
    // executed.
    @Test
    fun animateToStartTransitionEndListener() {
        lateinit var seekController: TransitionSeekController
        val callOrder = mutableListOf<String>()
        val latch = CountDownLatch(1)

        transition.addListener(
            object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition, isReverse: Boolean) {
                    callOrder += "onTransitionEnd($isReverse)"
                    super.onTransitionEnd(transition, isReverse)
                }

                override fun onTransitionEnd(transition: Transition) {
                    callOrder += "onTransitionEnd()"
                    super.onTransitionEnd(transition)
                }
            }
        )

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            seekController = controller!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread { seekController.currentFraction = 0.5f }

        rule.runOnUiThread {
            seekController.animateToStart {
                view.visibility = View.VISIBLE
                callOrder += "animateToStartLambda"
                latch.countDown()
            }
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        rule.runOnUiThread {
            assertThat(callOrder).hasSize(3)
            assertThat(callOrder)
                .isEqualTo(
                    mutableListOf(
                        "animateToStartLambda",
                        "onTransitionEnd(true)",
                        "onTransitionEnd()"
                    )
                )
        }
    }

    // The animateToEnd() should run after the transition is ready, even if called before
    // the transition is ready.
    @Test
    fun animateToEndAfterReady() {
        val latch = CountDownLatch(1)

        transition.addListener(
            object : TransitionListenerAdapter() {
                override fun onTransitionEnd(transition: Transition, isReverse: Boolean) {
                    super.onTransitionEnd(transition, isReverse)
                    latch.countDown()
                }
            }
        )

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            view.visibility = View.GONE
            controller!!.animateToEnd()
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    // The animateToStart() should run after the transition is ready, even if called before
    // the transition is ready.
    @Test
    fun animateToStartAfterReady() {
        val latch = CountDownLatch(1)

        rule.runOnUiThread {
            val controller = TransitionManager.controlDelayedTransition(root, transition)
            assertThat(controller).isNotNull()
            view.visibility = View.GONE
            controller!!.animateToStart(latch::countDown)
        }

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun interruptTransitionSet() {
        val transition = AutoTransition()
        lateinit var controller1: TransitionSeekController
        val controller1Latch = CountDownLatch(1)

        rule.runOnUiThread {
            controller1 = TransitionManager.controlDelayedTransition(root, transition)!!
            root.removeView(view)
            controller1.addOnProgressChangedListener {
                if (it.currentFraction == 1f) {
                    controller1Latch.countDown()
                }
            }
        }

        rule.runOnUiThread { controller1.currentFraction = 0.9f }

        lateinit var controller2: TransitionSeekController
        rule.runOnUiThread {
            controller2 = TransitionManager.controlDelayedTransition(root, transition)!!
            // Using the same View as was removed will cancel the first transition, but the
            // controller is still active.
            root.addView(view)
        }

        rule.runOnUiThread {
            controller1.animateToEnd()
            controller2.currentFraction = 0.9f
        }

        assertThat(controller1Latch.await(1, TimeUnit.SECONDS)).isTrue()
    }
}
