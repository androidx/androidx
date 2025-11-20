/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.view

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.test.R
import androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
import androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test

@LargeTest
public class WindowInsetsAnimationCompatActivityTest {

    private lateinit var scenario: ActivityScenario<WindowInsetsCompatActivity>

    private var barsShown = true

    @Before
    public fun setup() {
        scenario = ActivityScenario.launch(WindowInsetsCompatActivity::class.java)
        onIdle()
        // Close the IME if it's open, so we start from a known scenario
        onView(withId(R.id.edittext)).perform(ViewActions.closeSoftKeyboard())
        scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
            WindowCompat.getInsetsController(it.window, it.window.decorView).show(systemBars())
            barsShown = true
        }
        onIdle()
    }

    @Test
    public fun add_both_listener() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }
        var applyInsetsCalled = false
        var insetsAnimationCallbackCalled = false
        val insetsLatch = CountDownLatch(1)
        val animationLatch = CountDownLatch(1)
        val animationCallback =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    animationLatch.countDown()
                }
            )
        val insetListener: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled = true
                insetsLatch.countDown()
                insetsCompat
            }

        // Check that both ApplyWindowInsets and the Animation Callback are called
        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener)
        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback)
        triggerInsetAnimation(container)
        animationLatch.await(4, TimeUnit.SECONDS)
        insetsLatch.await(4, TimeUnit.SECONDS)
        assertTrue(
            "The WindowInsetsAnimationCallback has not been called",
            insetsAnimationCallbackCalled,
        )
        assertTrue("onApplyWindowInsetsListener has not been called", applyInsetsCalled)
    }

    @Test
    public fun remove_insets_listener() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }
        var applyInsetsCalled = false
        var insetsAnimationCallbackCalled = false
        val insetsLatch = CountDownLatch(1)
        val animationLatch = CountDownLatch(1)
        val animationCallback =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    animationLatch.countDown()
                }
            )
        val insetListener: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled = true
                insetsLatch.countDown()
                insetsCompat
            }

        // Check that both ApplyWindowInsets and the Animation Callback are called
        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener)
        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback)
        ViewCompat.setOnApplyWindowInsetsListener(container, null)
        triggerInsetAnimation(container)
        animationLatch.await(4, TimeUnit.SECONDS)
        assertFalse("onApplyWindowInsetsListener should NOT have been called", applyInsetsCalled)
        assertTrue(
            "The WindowInsetsAnimationCallback has not been called",
            insetsAnimationCallbackCalled,
        )
    }

    @Test
    public fun remove_animation_listener() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }
        var applyInsetsCalled = false
        var insetsAnimationCallbackCalled = false
        val insetsLatch = CountDownLatch(1)
        val animationLatch = CountDownLatch(1)
        val animationCallback =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    animationLatch.countDown()
                }
            )
        val insetListener: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled = true
                insetsLatch.countDown()
                insetsCompat
            }

        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener)
        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback)
        ViewCompat.setWindowInsetsAnimationCallback(container, null)
        triggerInsetAnimation(container)
        insetsLatch.await(4, TimeUnit.SECONDS)
        assertTrue("onApplyWindowInsetsListener has not been called", applyInsetsCalled)
        assertFalse(
            "The WindowInsetsAnimationCallback should NOT have been called",
            insetsAnimationCallbackCalled,
        )
    }

    @Test
    public fun all_callbacks_called() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }

        val res = mutableSetOf<String>()
        val progress = mutableListOf<Float>()
        val latch = CountDownLatch(3)
        val callback =
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    res.add("prepare")
                    latch.countDown()
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    res.add("progress")
                    progress.add(runningAnimations[0].fraction)
                    return insets
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat,
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    res.add("start")
                    latch.countDown()
                    return bounds
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    res.add("end")
                    latch.countDown()
                }
            }
        ViewCompat.setWindowInsetsAnimationCallback(container, callback)
        triggerInsetAnimation(container)
        latch.await(5, TimeUnit.SECONDS)
        Truth.assertThat(res).containsExactly("prepare", "start", "progress", "end").inOrder()
        Truth.assertThat(progress).contains(1.0f)
        Truth.assertThat(progress).isInOrder()
    }

    private fun triggerInsetAnimation(container: View) {
        scenario.onActivity {
            if (barsShown) {
                WindowCompat.getInsetsController(it.window, container).hide(statusBars())
                barsShown = false
            } else {
                WindowCompat.getInsetsController(it.window, container).show(statusBars())
                barsShown = true
            }
        }
    }

    @Test
    public fun update_apply_listener() {
        val container = scenario.withActivity { findViewById(R.id.container) }
        var applyInsetsCalled1 = false
        var applyInsetsCalled2 = false
        var applyInsetsCalled3 = false
        var insetsAnimationCallbackCalled1 = false
        var insetsAnimationCallbackCalled2 = false
        val insetsLatch = CountDownLatch(1)
        val animationLatch = CountDownLatch(1)
        val animationCallback1 =
            createCallback(onPrepare = { insetsAnimationCallbackCalled1 = true })
        val animationCallback2 =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled2 = true
                    animationLatch.countDown()
                }
            )
        val insetListener1: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled1 = true
                insetsCompat
            }

        val insetListener2: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled2 = true
                insetsCompat
            }

        val insetListener3: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled3 = true
                insetsLatch.countDown()
                insetsCompat
            }

        // Check that both ApplyWindowInsets and the Animation Callback are called
        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener1)
        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback1)
        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener2)
        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback2)
        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener3)
        triggerInsetAnimation(container)
        animationLatch.await(5, TimeUnit.SECONDS)
        insetsLatch.await(5, TimeUnit.SECONDS)
        assertFalse(
            "The WindowInsetsAnimationCallback #1 should have not been called",
            insetsAnimationCallbackCalled1,
        )
        assertFalse(
            "The onApplyWindowInsetsListener #1 should have not been called",
            applyInsetsCalled1,
        )
        assertTrue(
            "The WindowInsetsAnimationCallback #2 has not been called",
            insetsAnimationCallbackCalled2,
        )
        assertFalse(
            "onApplyWindowInsetsListener #2 should not have been called",
            applyInsetsCalled2,
        )
        assertTrue("onApplyWindowInsetsListener #3 has not been called", applyInsetsCalled3)
    }

    @FlakyTest(bugId = 249124990)
    @Test
    public fun add_animation_listener_first() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }
        var applyInsetsCalled = false
        var insetsAnimationCallbackCalled = false
        val insetsLatch = CountDownLatch(1)
        val animationLatch = CountDownLatch(1)
        val animationCallback =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    animationLatch.countDown()
                }
            )
        val insetListener: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { _, insetsCompat ->
                applyInsetsCalled = true
                insetsLatch.countDown()
                insetsCompat
            }

        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback)
        ViewCompat.setOnApplyWindowInsetsListener(container, insetListener)
        triggerInsetAnimation(container)
        animationLatch.await(4, TimeUnit.SECONDS)
        insetsLatch.await(4, TimeUnit.SECONDS)
        assertTrue(
            "The WindowInsetsAnimationCallback has not been called",
            insetsAnimationCallbackCalled,
        )
        assertTrue("onApplyWindowInsetsListener has not been called", applyInsetsCalled)
    }

    @Test
    public fun child_callback_called_when_consumed() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }
        val child = scenario.withActivity { findViewById(R.id.view) }
        var parentListenerCalled = false
        var insetsAnimationCallbackCalled = false
        var childListenerCalledCount = 0
        var savedInsets: WindowInsetsCompat? = null
        var savedView: View? = null
        val applyInsetsLatch = CountDownLatch(2) // Insets will be dispatched 3 times
        val onPrepareLatch = CountDownLatch(2)
        val childLatch = CountDownLatch(1)
        val animationCallback =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    onPrepareLatch.countDown()
                },
                onEnd = { ViewCompat.dispatchApplyWindowInsets(savedView!!, savedInsets!!) },
            )

        val childCallback =
            createCallback(
                onEnd = {
                    ++childListenerCalledCount
                    childLatch.countDown()
                }
            )

        // First we check that when the parent consume the insets, the child listener is not called
        val consumingListener: (v: View, insets: WindowInsetsCompat) -> WindowInsetsCompat =
            { v, insetsCompat ->
                parentListenerCalled = true
                savedInsets = insetsCompat
                savedView = v
                applyInsetsLatch.countDown()
                WindowInsetsCompat.CONSUMED
            }

        ViewCompat.setWindowInsetsAnimationCallback(container, animationCallback)
        ViewCompat.setWindowInsetsAnimationCallback(child, childCallback)
        ViewCompat.setOnApplyWindowInsetsListener(container, consumingListener)
        triggerInsetAnimation(container)
        applyInsetsLatch.await(4, TimeUnit.SECONDS)
        childLatch.await(2, TimeUnit.SECONDS)
        assertTrue(
            "The WindowInsetsAnimationCallback has not been called",
            insetsAnimationCallbackCalled,
        )
        assertTrue("parent listener has not been called", parentListenerCalled)
        // Parent consumed the insets, child listener won't be called
        assertEquals("child listener should not have been called", 1, childListenerCalledCount)
    }

    @Test
    public fun check_view_on_apply_called() {
        val container = scenario.withActivity { findViewById(R.id.container) }
        val onApplyLatch = CountDownLatch(1)
        val customView =
            object : View(scenario.withActivity { this }) {
                override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                    onApplyLatch.countDown()
                    return insets
                }
            }
        scenario.onActivity { (container as ViewGroup).addView(customView) }
        val stopCallback = createCallback()
        ViewCompat.setWindowInsetsAnimationCallback(customView, stopCallback)
        triggerInsetAnimation(container)
        assertTrue(
            "The View.onApplyWindowInsets has not been called",
            onApplyLatch.await(2, TimeUnit.SECONDS),
        )
    }

    private fun createCallback(
        dispatchMode: Int = DISPATCH_MODE_CONTINUE_ON_SUBTREE,
        onPrepare: ((WindowInsetsAnimationCompat) -> Unit)? = null,
        onEnd: ((WindowInsetsAnimationCompat) -> Unit)? = null,
    ): WindowInsetsAnimationCompat.Callback {
        return object : WindowInsetsAnimationCompat.Callback(dispatchMode) {

            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                onPrepare?.invoke(animation)
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat = insets

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                onEnd?.invoke(animation)
            }
        }
    }

    @Test
    @FlakyTest(bugId = 196917541)
    public fun child_callback_not_called_when_dispatch_stop() {
        assumeNotCuttlefish()

        val container = scenario.withActivity { findViewById(R.id.container) }
        val child = scenario.withActivity { findViewById(R.id.view) }
        var insetsAnimationCallbackCalled = false
        var childListenerCalledCount = 0
        var onPrepareLatch = CountDownLatch(1)
        val childLatch = CountDownLatch(1)
        val stopCallback =
            createCallback(
                DISPATCH_MODE_STOP,
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    onPrepareLatch.countDown()
                },
            )

        ViewCompat.setWindowInsetsAnimationCallback(container, stopCallback)
        ViewCompat.setWindowInsetsAnimationCallback(
            child,
            createCallback(onEnd = { ++childListenerCalledCount }),
        )
        triggerInsetAnimation(container)
        onPrepareLatch.await(2, TimeUnit.SECONDS)
        assertTrue(
            "The WindowInsetsAnimationCallback has not been called",
            insetsAnimationCallbackCalled,
        )
        // Parent consumed the insets, child listener won't be called
        assertEquals(
            "child listener should have not been called. Call count: ",
            0,
            childListenerCalledCount,
        )

        onPrepareLatch = CountDownLatch(1)
        val dispatchCallback =
            createCallback(
                onPrepare = {
                    insetsAnimationCallbackCalled = true
                    onPrepareLatch.countDown()
                }
            )
        ViewCompat.setWindowInsetsAnimationCallback(container, dispatchCallback)
        ViewCompat.setWindowInsetsAnimationCallback(
            child,
            createCallback(
                onEnd = {
                    ++childListenerCalledCount
                    childLatch.countDown()
                }
            ),
        )
        triggerInsetAnimation(container)
        childLatch.await(4, TimeUnit.SECONDS)
        assertTrue(
            "The WindowInsetsAnimationCallback has not been called",
            insetsAnimationCallbackCalled,
        )
        assertEquals(
            "child listener should have been called 1 time but was called " +
                "$childListenerCalledCount times",
            1,
            childListenerCalledCount,
        )
    }

    private fun assumeNotCuttlefish() {
        // TODO: remove this if b/159103848 is resolved
        Assume.assumeFalse(
            "Unable to test: Cuttlefish devices default to the virtual keyboard being disabled.",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
    }
}
