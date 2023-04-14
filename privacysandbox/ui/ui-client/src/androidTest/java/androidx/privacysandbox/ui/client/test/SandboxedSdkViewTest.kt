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

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO(b/268014171): Remove API requirements once S- support is added
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RunWith(AndroidJUnit4::class)
@LargeTest
class SandboxedSdkViewTest {

    companion object {
        const val TIMEOUT = 1000.toLong()
    }

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var view: SandboxedSdkView
    private lateinit var layoutParams: ViewGroup.LayoutParams
    private lateinit var testSandboxedUiAdapter: TestSandboxedUiAdapter
    private lateinit var openSessionLatch: CountDownLatch
    private lateinit var resizeLatch: CountDownLatch
    private lateinit var configChangedLatch: CountDownLatch
    private lateinit var stateChangedListener: StateChangedListener

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

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

    // TODO(b/270965314) Use OnLayoutChangeListener to track resizes and config changes
    //  in SandboxedSdkViewTest.
    class TestSandboxedUiAdapter(
        private val openSessionLatch: CountDownLatch?,
        private val resizeLatch: CountDownLatch?,
        private val configChangedLatch: CountDownLatch?
    ) : SandboxedUiAdapter {

        var isSessionOpened = false
        var internalClient: SandboxedUiAdapter.SessionClient? = null
        var testSession: TestSession? = null

        override fun openSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            internalClient = client
            testSession =
                TestSession(context, initialWidth, initialHeight, resizeLatch, configChangedLatch)
            client.onSessionOpened(testSession!!)
            isSessionOpened = true
            openSessionLatch?.countDown()
        }

        inner class TestSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            private var resizeLatch: CountDownLatch?,
            private var configChangedLatch: CountDownLatch?
        ) : SandboxedUiAdapter.Session {

            var isZOrderChanged = false

            override val view: View = View(context)

            init {
                view.layoutParams = LinearLayout.LayoutParams(initialWidth, initialHeight)
            }

            fun requestSizeChange(width: Int, height: Int) {
                internalClient?.onResizeRequested(width, height)
            }

            override fun notifyResized(width: Int, height: Int) {
                resizeLatch?.countDown()
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                isZOrderChanged = true
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                configChangedLatch?.countDown()
            }

            override fun close() {
            }
        }
    }

    open class StateChangedListener : SandboxedSdkUiSessionStateChangedListener {
        var currentState: SandboxedSdkUiSessionState? = null
        var latch: CountDownLatch = CountDownLatch(1)

        override fun onStateChanged(state: SandboxedSdkUiSessionState) {
            currentState = state
            latch.countDown()
        }
    }

    @Before
    fun setup() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        context = InstrumentationRegistry.getInstrumentation().targetContext
        activity = activityScenarioRule.withActivity { this }
        view = SandboxedSdkView(context)
        stateChangedListener = StateChangedListener()
        view.addStateChangedListener(stateChangedListener)

        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        view.layoutParams = layoutParams
        openSessionLatch = CountDownLatch(1)
        resizeLatch = CountDownLatch(1)
        configChangedLatch = CountDownLatch(1)
        testSandboxedUiAdapter = TestSandboxedUiAdapter(
            openSessionLatch, resizeLatch, configChangedLatch
        )
        view.setAdapter(testSandboxedUiAdapter)
    }

    @Test
    fun addAndRemoveStateChangeListenerTest() {
        // Initial state (Idle) should be sent to listener
        var stateListenerManager: SandboxedSdkView.StateListenerManager = view.stateListenerManager
        assertThat(stateChangedListener.latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(stateChangedListener.currentState).isEqualTo(SandboxedSdkUiSessionState.Idle)

        // While registered, listener should receive state change
        stateChangedListener.latch = CountDownLatch(1)
        stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Active
        assertThat(stateChangedListener.latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(stateChangedListener.currentState).isEqualTo(SandboxedSdkUiSessionState.Active)

        // While unregistered, listener should not receive state change
        stateChangedListener.latch = CountDownLatch(1)
        view.removeStateChangedListener(stateChangedListener)
        stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Loading
        assertThat(stateChangedListener.latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun reentrantDispatchTest() {
        val latch = CountDownLatch(2)
        var currentState: SandboxedSdkUiSessionState? = SandboxedSdkUiSessionState.Idle

        val listener1 = SandboxedSdkUiSessionStateChangedListener {
            if (it != currentState) {
                currentState = it
                view.stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Active
                latch.countDown()
            }
        }

        view.addStateChangedListener(listener1)
        assertThat(currentState).isEqualTo(SandboxedSdkUiSessionState.Idle)

        view.stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Loading
        assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(currentState).isEqualTo(SandboxedSdkUiSessionState.Active)
    }

    @Test
    fun onAttachedToWindowTest() {
        activity.runOnUiThread(Runnable {
            activity.findViewById<LinearLayout>(
                R.id.mainlayout
            ).addView(view)
        })
        openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(testSandboxedUiAdapter.isSessionOpened)
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)
    }

    @Test
    fun childViewRemovedOnErrorTest() {
        assertTrue(view.childCount == 0)

        activity.runOnUiThread(Runnable {
            activity.findViewById<LinearLayout>(
                R.id.mainlayout
            ).addView(view)
        })

        openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(openSessionLatch.count == 0.toLong())
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)

        activity.runOnUiThread(Runnable {
            testSandboxedUiAdapter.internalClient!!.onSessionError(Exception())
            assertTrue(view.childCount == 0)
        })
    }

    @Test
    fun onZOrderChangedTest() {
        val layout = activity.findViewById<LinearLayout>(
            R.id.mainlayout
        )

        view.setZOrderOnTopAndEnableUserInteraction(true)
        resizeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        testSandboxedUiAdapter.testSession?.let { assertFalse(it.isZOrderChanged) }

        activity.runOnUiThread(Runnable {
            layout.addView(view)
        })
        view.setZOrderOnTopAndEnableUserInteraction(false)

        resizeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        testSandboxedUiAdapter.testSession?.let { assertTrue(it.isZOrderChanged) }
        assertTrue(resizeLatch.count == 0.toLong())
    }

    @Test
    fun onConfigurationChangedTest() {
        val layout = activity.findViewById<LinearLayout>(R.id.mainlayout)

        activity.runOnUiThread(Runnable {
            layout.addView(view)
        })

        openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(openSessionLatch.count == 0.toLong())
        activity.runOnUiThread {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        configChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(configChangedLatch.count == 0.toLong())
    }

    @Test
    fun onSizeChangedTest() {
        val layout = activity.findViewById<LinearLayout>(
            R.id.mainlayout
        )

        activity.runOnUiThread(Runnable {
            layout.addView(view)
            view.layoutParams = LinearLayout.LayoutParams(100, 200)
        })
        resizeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(resizeLatch.count == 0.toLong())
        assertTrue(view.width == 100 && view.height == 200)
    }

    @Test
    fun onSdkRequestsResizeTest() {
        val globalLayoutLatch = CountDownLatch(1)
        lateinit var layout: LinearLayout
        activity.runOnUiThread(Runnable {
            layout = activity.findViewById<LinearLayout>(
                R.id.mainlayout
            )
            layout.addView(view)
        })
        openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(openSessionLatch.count == 0.toLong())
        activity.runOnUiThread(Runnable {
            testSandboxedUiAdapter.testSession?.requestSizeChange(layout.width, layout.height)
        })
        val observer = view.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (view.width == layout.width && view.height == layout.height) {
                    globalLayoutLatch.countDown()
                    observer.removeOnGlobalLayoutListener(this)
                }
            }
        })
        globalLayoutLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(view.width == layout.width && view.height == layout.height)
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
            null,
            false
        ) as ViewGroup
        assertFalse(
            "XML overrides SandboxedSdkView.isTransitionGroup", view.isTransitionGroup
        )
    }
}
