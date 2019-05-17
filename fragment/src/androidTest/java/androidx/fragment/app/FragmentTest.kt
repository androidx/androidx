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
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Miscellaneous tests for fragments that aren't big enough to belong to their own classes.
 */
@RunWith(AndroidJUnit4::class)
class FragmentTest {
    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private lateinit var activity: FragmentTestActivity
    private lateinit var instrumentation: Instrumentation

    @Before
    fun setup() {
        activity = activityRule.activity
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    @SmallTest
    @UiThreadTest
    @Test
    fun testRequireView() {
        val fragment1 = StrictViewFragment()
        activity.supportFragmentManager.beginTransaction().add(R.id.content, fragment1).commitNow()
        assertThat(fragment1.requireView()).isNotNull()
    }

    @SmallTest
    @UiThreadTest
    @Test(expected = IllegalStateException::class)
    fun testRequireViewWithoutView() {
        val fragment1 = StrictFragment()
        activity.supportFragmentManager.beginTransaction().add(fragment1, "fragment").commitNow()
        fragment1.requireView()
    }

    @SmallTest
    @UiThreadTest
    @Test
    fun testOnCreateOrder() {
        val fragment1 = OrderFragment()
        val fragment2 = OrderFragment()
        activity.supportFragmentManager.beginTransaction()
            .add(R.id.content, fragment1)
            .add(R.id.content, fragment2)
            .commitNow()
        assertThat(fragment1.createOrder).isEqualTo(0)
        assertThat(fragment2.createOrder).isEqualTo(1)
    }

    @LargeTest
    @Test
    @SdkSuppress(minSdkVersion = 16) // waitForHalfFadeIn requires API 16
    fun testChildFragmentManagerGone() {
        val fragmentA = FragmentA()
        val fragmentB = FragmentB()
        activityRule.runOnUiThread {
            activity.supportFragmentManager.beginTransaction()
                .add(R.id.content, fragmentA)
                .commitNow()
        }
        instrumentation.waitForIdleSync()
        activityRule.runOnUiThread {
            activity.supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.long_fade_in, R.anim.long_fade_out,
                    R.anim.long_fade_in, R.anim.long_fade_out
                )
                .replace(R.id.content, fragmentB)
                .addToBackStack(null)
                .commit()
        }
        // Wait for the middle of the animation
        waitForHalfFadeIn(fragmentB)

        activityRule.runOnUiThread {
            activity.supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.long_fade_in, R.anim.long_fade_out,
                    R.anim.long_fade_in, R.anim.long_fade_out
                )
                .replace(R.id.content, fragmentA)
                .addToBackStack(null)
                .commit()
        }
        // Wait for the middle of the animation
        waitForHalfFadeIn(fragmentA)
        activityRule.popBackStackImmediate()

        // Wait for the middle of the animation
        waitForHalfFadeIn(fragmentB)
        activityRule.popBackStackImmediate()
    }

    @SmallTest
    @Test
    fun testChildFragmentManagerNotAttached() {
        val fragment = Fragment()
        try {
            fragment.childFragmentManager
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment has not been attached yet.")
        }
    }

    @RequiresApi(16) // ViewTreeObserver.OnDrawListener was added in API 16
    private fun waitForHalfFadeIn(fragment: Fragment) {
        if (fragment.view == null) {
            activityRule.waitForExecution()
        }
        val view = fragment.requireView()
        val animation = view.animation
        if (animation == null || animation.hasEnded()) {
            // animation has already completed
            return
        }

        val startTime = animation.startTime
        if (view.drawingTime > animation.startTime) {
            return // We've already done at least one frame
        }
        val latch = CountDownLatch(1)
        val viewTreeObserver = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnDrawListener {
            override fun onDraw() {
                if (animation.hasEnded() || view.drawingTime > startTime) {
                    val onDrawListener = this
                    latch.countDown()
                    view.post { viewTreeObserver.removeOnDrawListener(onDrawListener) }
                }
            }
        }
        viewTreeObserver.addOnDrawListener(listener)
        latch.await(5, TimeUnit.SECONDS)
    }

    @MediumTest
    @UiThreadTest
    @Test
    fun testViewOrder() {
        val fragmentA = FragmentA()
        val fragmentB = FragmentB()
        val fragmentC = FragmentC()
        activity.supportFragmentManager
            .beginTransaction()
            .add(R.id.content, fragmentA)
            .add(R.id.content, fragmentB)
            .add(R.id.content, fragmentC)
            .commitNow()
        val content = activity.findViewById(R.id.content) as ViewGroup
        assertThat(content.childCount.toLong()).isEqualTo(3)
        assertThat(content.getChildAt(0).findViewById<View>(R.id.textA)).isNotNull()
        assertThat(content.getChildAt(1).findViewById<View>(R.id.textB)).isNotNull()
        assertThat(content.getChildAt(2).findViewById<View>(R.id.textC)).isNotNull()
    }

    @SmallTest
    @UiThreadTest
    @Test
    fun testRequireParentFragment() {
        val parentFragment = StrictFragment()
        activity.supportFragmentManager.beginTransaction().add(parentFragment, "parent").commitNow()

        val childFragmentManager = parentFragment.childFragmentManager
        val childFragment = StrictFragment()
        childFragmentManager.beginTransaction().add(childFragment, "child").commitNow()
        assertThat(childFragment.requireParentFragment()).isSameInstanceAs(parentFragment)
    }

    @SmallTest
    @Test
    fun requireMethodsThrowsWhenNotAttached() {
        val fragment = Fragment()
        try {
            fragment.requireContext()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment not attached to a context.")
        }

        try {
            fragment.requireActivity()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment not attached to an activity.")
        }

        try {
            fragment.requireHost()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment not attached to a host.")
        }

        try {
            fragment.requireParentFragment()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment is not attached to any Fragment or host")
        }

        try {
            fragment.requireView()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains(
                    "Fragment $fragment did not return a View from onCreateView() or this was " +
                            "called before onCreateView()."
                )
        }

        try {
            fragment.requireFragmentManager()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment not associated with a fragment manager.")
        }
    }

    @SmallTest
    @Test
    fun requireArguments() {
        val fragment = Fragment()
        try {
            fragment.requireArguments()
            fail()
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Fragment $fragment does not have any arguments.")
        }

        val arguments = Bundle()
        fragment.arguments = arguments
        assertWithMessage("requireArguments should return the arguments")
            .that(fragment.requireArguments())
            .isSameInstanceAs(arguments)
    }

    class OrderFragment : Fragment() {
        var createOrder = -1

        override fun onCreate(savedInstanceState: Bundle?) {
            createOrder = order.getAndIncrement()
            super.onCreate(savedInstanceState)
        }

        companion object {
            private val order = AtomicInteger()
        }
    }

    class FragmentA : Fragment(R.layout.fragment_a)

    class FragmentB : Fragment(R.layout.fragment_b)

    class FragmentC : Fragment(R.layout.fragment_c)
}
