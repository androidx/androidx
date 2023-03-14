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

package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/268014171): Remove API requirements once S- support is added
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RunWith(AndroidJUnit4::class)
@LargeTest
class SandboxedSdkViewTest {

    private lateinit var context: Context

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

    class TestErrorConsumer(private val latch: CountDownLatch?) : Consumer<Throwable> {
        var isErrorConsumed = false
        override fun accept(throwable: Throwable) {
            isErrorConsumed = true
            latch?.countDown()
        }
    }

    class FailingTestSandboxedUiAdapter : SandboxedUiAdapter {
        override fun openSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            client.onSessionError(Exception("Error in openSession()"))
        }
    }

    class TestSandboxedUiAdapter(private val latch: CountDownLatch?) : SandboxedUiAdapter {

        var isSessionOpened = false
        var internalClient: SandboxedUiAdapter.SessionClient? = null

        override fun openSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            internalClient = client
            val view = View(context)
            view.layoutParams = LinearLayout.LayoutParams(initialWidth, initialHeight)
            client.onSessionOpened(TestSession(context, initialWidth, initialHeight))
            isSessionOpened = true
            latch?.countDown()
        }

        class TestSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int
        ) : SandboxedUiAdapter.Session {

            override val view: View = View(context)

            init {
                view.layoutParams = LinearLayout.LayoutParams(initialWidth, initialHeight)
            }

            override fun close() {
                TODO("Not yet implemented")
            }
        }
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    }

    @Test
    fun onAttachedToWindowTest() {
        val activity = activityScenarioRule.withActivity { this }
        val view = SandboxedSdkView(context)
        var latch = CountDownLatch(1)
        val testSandboxedUiAdapter = TestSandboxedUiAdapter(latch)
        val layoutParams = LinearLayout.LayoutParams(100, 100)
        view.layoutParams = layoutParams

        view.setAdapter(testSandboxedUiAdapter)

        activity.runOnUiThread(Runnable {
            activity.findViewById<LinearLayout>(
                R.id.mainlayout
            ).addView(view)
        })

        latch.await(1000, TimeUnit.MILLISECONDS)

        assertTrue(testSandboxedUiAdapter.isSessionOpened)
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)
    }

    @Test
    fun errorConsumerTest() {
        val activity = activityScenarioRule.withActivity { this }
        val view = SandboxedSdkView(context)
        var latch = CountDownLatch(1)
        val testSandboxedUiAdapter = FailingTestSandboxedUiAdapter()
        val testErrorConsumer = TestErrorConsumer(latch)
        view.setSdkErrorConsumer(testErrorConsumer)
        view.setAdapter(testSandboxedUiAdapter)

        activity.runOnUiThread(Runnable {
            activity.findViewById<LinearLayout>(
                R.id.mainlayout
            ).addView(view)
        })

        latch.await(2000, TimeUnit.MILLISECONDS)
        assertTrue(testErrorConsumer.isErrorConsumed)
        assertTrue(latch.count == 0.toLong())
    }

    @Test
    fun childViewRemovedOnErrorTest() {
        val activity = activityScenarioRule.withActivity { this }
        val view = SandboxedSdkView(context)
        var latch = CountDownLatch(1)
        val testSandboxedUiAdapter = TestSandboxedUiAdapter(latch)

        view.setSdkErrorConsumer(TestErrorConsumer(latch))
        val layoutParams = LinearLayout.LayoutParams(100, 100)
        view.layoutParams = layoutParams
        view.setAdapter(testSandboxedUiAdapter)

        assertTrue(view.childCount == 0)

        activity.runOnUiThread(Runnable {
            activity.findViewById<LinearLayout>(
                R.id.mainlayout
            ).addView(view)
        })

        latch.await(1000, TimeUnit.MILLISECONDS)
        assertTrue(latch.count == 0.toLong())
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)

        activity.runOnUiThread(Runnable {
            testSandboxedUiAdapter.internalClient!!.onSessionError(Exception())
            assertTrue(view.childCount == 0)
        })
    }

    @Test
    fun sandboxedSdkViewIsTransitionGroup() {
        val view = SandboxedSdkView(context)
        assertTrue("SandboxedSdkView isTransitionGroup by default", view.isTransitionGroup)
    }

    @Test
    fun sandboxedSdkViewInflatesTransitionGroup() {
        val activity = activityScenarioRule.withActivity { this }
        val view = activity.layoutInflater.inflate(
            R.layout.sandboxedsdkview_transition_group_false,
            null
        ) as ViewGroup
        assertFalse(
            "XML overrides SandboxedSdkView.isTransitionGroup", view.isTransitionGroup
        )
    }
}