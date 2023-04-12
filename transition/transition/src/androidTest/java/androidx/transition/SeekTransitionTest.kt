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
import androidx.core.os.BuildCompat
import androidx.core.util.Consumer
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.AnimationDurationScaleRule.Companion.createForAllTests
import androidx.transition.test.R
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.timeout
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@MediumTest
class SeekTransitionTest : BaseTest() {
    @get:Rule
    val animationDurationScaleRule = createForAllTests(1f)

    lateinit var view: View
    lateinit var root: LinearLayout
    lateinit var transition: Transition

    @UiThreadTest
    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        root = rule.activity.findViewById<View>(R.id.root) as LinearLayout
        transition = Fade().also {
            it.interpolator = LinearInterpolator()
        }
        view = View(root.context)
        view.setBackgroundColor(Color.BLUE)
        root.addView(view, ViewGroup.LayoutParams(100, 100))
    }

    @Test(expected = IllegalArgumentException::class)
    @UiThreadTest
    fun onlySeekingTransitions() {
        if (!BuildCompat.isAtLeastU()) throw IllegalArgumentException()
        transition = object : Visibility() {}
        TransitionManager.controlDelayedTransition(root, transition)
        fail("Expected IllegalArgumentException")
    }

    @Test
    fun waitForReady() {
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
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

            assertThat(view.transitionAlpha).isEqualTo(1f)

            seekController.currentPlayTimeMillis = 150
            assertThat(view.transitionAlpha).isEqualTo(0.5f)
            seekController.currentPlayTimeMillis = 299
            assertThat(view.transitionAlpha).isWithin(0.001f).of(1f / 300f)
            seekController.currentPlayTimeMillis = 300

            verify(listener, times(1)).onTransitionEnd(any())

            assertThat(view.transitionAlpha).isEqualTo(1f)
            assertThat(view.visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun animationDoesNotTakeOverSeek() {
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
        lateinit var seekController1: TransitionSeekController

        val stateListener1 = spy(TransitionListenerAdapter())
        transition.addListener(stateListener1)
        transition.duration = 3000
        rule.runOnUiThread {
            seekController1 = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            assertThat(seekController1.isReady).isTrue()
        }

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
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
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
        if (!BuildCompat.isAtLeastU()) return
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToStart()
        }

        verify(listener, timeout(3000)).onTransitionEnd(any())
        val listener2 = spy(TransitionListenerAdapter())
        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)

            // Now set it back to the original state with a fast transition
            transition.removeListener(listener)
            transition.addListener(listener2)
            transition.duration = 0
            TransitionManager.beginDelayedTransition(root, transition)
            view.visibility = View.VISIBLE
            root.invalidate()
        }
        verify(listener2, timeout(3000)).onTransitionStart(any())
        rule.runOnUiThread {
            verify(listener2, times(1)).onTransitionEnd(any())
            // All transitions should be ended
            val runningTransitions = TransitionManager.getRunningTransitions()
            assertThat(runningTransitions[root]).isEmpty()
        }
    }

    @Test
    fun animateToStartAfterAnimateToEnd() {
        if (!BuildCompat.isAtLeastU()) return
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

        rule.runOnUiThread {
            seekController.animateToStart()
        }

        verify(listener, timeout(3000)).onTransitionEnd(any())

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.VISIBLE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
        }
    }

    @Test
    fun animateToEndAfterAnimateToStart() {
        if (!BuildCompat.isAtLeastU()) return
        lateinit var seekController: TransitionSeekController

        val listener = spy(TransitionListenerAdapter())
        transition.addListener(listener)

        rule.runOnUiThread {
            seekController = TransitionManager.controlDelayedTransition(root, transition)!!
            view.visibility = View.GONE
        }

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 150
            seekController.animateToStart()
        }

        rule.runOnUiThread {
            seekController.animateToEnd()
        }

        verify(listener, timeout(3000)).onTransitionEnd(any())

        rule.runOnUiThread {
            assertThat(view.visibility).isEqualTo(View.GONE)
            assertThat(view.transitionAlpha).isEqualTo(1f)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun seekAfterAnimate() {
        if (!BuildCompat.isAtLeastU()) throw IllegalStateException("Not supported before U")
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

        rule.runOnUiThread {
            seekController.currentPlayTimeMillis = 120
        }
    }

    @Test
    fun seekTransitionSet() {
        if (!BuildCompat.isAtLeastU()) return
        transition = TransitionSet().also {
            it.addTransition(Fade(Fade.MODE_OUT))
                .addTransition(Fade(Fade.MODE_IN))
                .ordering = TransitionSet.ORDERING_SEQUENTIAL
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
        if (!BuildCompat.isAtLeastU()) return
        transition = TransitionSet().also {
            it.addTransition(Fade(Fade.MODE_OUT))
                .addTransition(Fade(Fade.MODE_IN))
                .ordering = TransitionSet.ORDERING_SEQUENTIAL
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
        if (!BuildCompat.isAtLeastU()) return
        transition = TransitionSet().also {
            it.addTransition(Fade(Fade.MODE_OUT))
                .addTransition(Fade(Fade.MODE_IN))
                .ordering = TransitionSet.ORDERING_SEQUENTIAL
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
            // seek to the end of the fade out
            seekController.currentPlayTimeMillis = 300

            seekController.animateToStart()
        }
        verify(listener, timeout(3000)).onTransitionEnd(any())

        val transition2 = TransitionSet().also {
            it.addTransition(Fade(Fade.MODE_OUT))
                .addTransition(Fade(Fade.MODE_IN))
                .ordering = TransitionSet.ORDERING_SEQUENTIAL
            it.duration = 0
        }

        val listener2 = spy(TransitionListenerAdapter())
        transition2.addListener(listener2)
        rule.runOnUiThread {
            TransitionManager.beginDelayedTransition(root, transition2)
            view.visibility = View.VISIBLE
            view2.visibility = View.GONE
        }
        verify(listener2, timeout(3000)).onTransitionStart(any())

        rule.runOnUiThread {
            verify(listener, times(1)).onTransitionEnd(any())
            verify(listener2, times(1)).onTransitionEnd(any())
            val runningTransitions = TransitionManager.getRunningTransitions()
            assertThat(runningTransitions[root]).isEmpty()
        }
    }

    @Test
    fun cancelPartOfTransitionSet() {
        if (!BuildCompat.isAtLeastU()) return
        transition = TransitionSet().also {
            it.addTransition(Fade(Fade.MODE_OUT))
                .addTransition(Fade(Fade.MODE_IN))
                .ordering = TransitionSet.ORDERING_SEQUENTIAL
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

        val transition2 = TransitionSet().also {
            it.addTransition(Fade(Fade.MODE_OUT))
                .addTransition(Fade(Fade.MODE_IN))
                .ordering = TransitionSet.ORDERING_SEQUENTIAL
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
    fun pauseResumeOnSeek() {
        if (!BuildCompat.isAtLeastU()) return
        var pauseCount = 0
        var resumeCount = 0
        var setPauseCount = 0
        var setResumeCount = 0
        val set = TransitionSet().also {
            it.addTransition(Fade().apply {
                addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionPause(transition: Transition) {
                        pauseCount++
                    }

                    override fun onTransitionResume(transition: Transition) {
                        resumeCount++
                    }
                })
            })
            it.addListener(object : TransitionListenerAdapter() {
                override fun onTransitionPause(transition: Transition) {
                    setPauseCount++
                }

                override fun onTransitionResume(transition: Transition) {
                    setResumeCount++
                }
            })
        }
        transition = TransitionSet().also {
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
}