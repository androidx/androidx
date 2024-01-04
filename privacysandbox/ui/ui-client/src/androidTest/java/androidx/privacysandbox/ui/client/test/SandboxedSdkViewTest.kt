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
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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
        // Longer timeout used for expensive operations like device rotation.
        const val UI_INTENSIVE_TIMEOUT = 2000.toLong()
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
            windowInputToken: IBinder,
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
        var isZOrderOnTop = true
        var inputToken: IBinder? = null

        // When set to true, the onSessionOpened callback will only be invoked when specified
        // by the test. This is to test race conditions when the session is being loaded.
        var delayOpenSessionCallback = false

        override fun openSession(
            context: Context,
            windowInputToken: IBinder,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            internalClient = client
            testSession =
                TestSession(context, initialWidth, initialHeight, resizeLatch, configChangedLatch)
            if (!delayOpenSessionCallback) {
                client.onSessionOpened(testSession!!)
            }
            isSessionOpened = true
            this.isZOrderOnTop = isZOrderOnTop
            this.inputToken = windowInputToken
            openSessionLatch?.countDown()
        }

        internal fun sendOnSessionOpened() {
            internalClient?.onSessionOpened(testSession!!)
        }

        inner class TestSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
            private var resizeLatch: CountDownLatch?,
            private var configChangedLatch: CountDownLatch?
        ) : SandboxedUiAdapter.Session {

            var zOrderChangedLatch: CountDownLatch = CountDownLatch(1)

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
                this@TestSandboxedUiAdapter.isZOrderOnTop = isZOrderOnTop
                zOrderChangedLatch.countDown()
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
        view = SandboxedSdkView(activity)
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
        addViewToLayout()
        openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(testSandboxedUiAdapter.isSessionOpened)
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)
    }

    @Test
    fun childViewRemovedOnErrorTest() {
        assertTrue(view.childCount == 0)
        addViewToLayout()

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
        addViewToLayout()

        // When session is opened, the provider should not receive a Z-order notification.
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        val session = testSandboxedUiAdapter.testSession!!
        val adapter = testSandboxedUiAdapter
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(adapter.isZOrderOnTop).isTrue()

        // When state changes to false, the provider should be notified.
        view.orderProviderUiAboveClientUi(false)
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(adapter.isZOrderOnTop).isFalse()

        // When state changes back to true, the provider should be notified.
        session.zOrderChangedLatch = CountDownLatch(1)
        view.orderProviderUiAboveClientUi(true)
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(adapter.isZOrderOnTop).isTrue()
    }

    @Test
    fun onZOrderUnchangedTest() {
        addViewToLayout()

        // When session is opened, the provider should not receive a Z-order notification.
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        val session = testSandboxedUiAdapter.testSession!!
        val adapter = testSandboxedUiAdapter
        assertThat(adapter.isZOrderOnTop).isTrue()

        // When Z-order state is unchanged, the provider should not be notified.
        session.zOrderChangedLatch = CountDownLatch(1)
        view.orderProviderUiAboveClientUi(true)
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(adapter.isZOrderOnTop).isTrue()
    }

    @Test
    fun setZOrderNotOnTopBeforeOpeningSession() {
        view.orderProviderUiAboveClientUi(false)
        addViewToLayout()
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        val session = testSandboxedUiAdapter.testSession!!

        // The initial Z-order state is passed to the session, but notifyZOrderChanged is not called
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun setZOrderNotOnTopWhileSessionLoading() {
        testSandboxedUiAdapter.delayOpenSessionCallback = true
        addViewToLayout()
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        view.orderProviderUiAboveClientUi(false)
        val session = testSandboxedUiAdapter.testSession!!
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        activity.runOnUiThread {
            testSandboxedUiAdapter.sendOnSessionOpened()
        }

        // After session has opened, the pending Z order changed made while loading is notified
        // th the session.
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun onConfigurationChangedTest() {
        addViewToLayout()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        // newWindow() will be triggered by a window state change, even if the activity handles
        // orientation changes without recreating the activity.
        device.performActionAndWait({
            device.setOrientationLeft()
        }, Until.newWindow(), UI_INTENSIVE_TIMEOUT)
        assertThat(configChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        device.performActionAndWait({
            device.setOrientationNatural()
        }, Until.newWindow(), UI_INTENSIVE_TIMEOUT)
    }

    @Test
    fun onConfigurationChangedTestSameConfiguration() {
        addViewToLayout()
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        activity.runOnUiThread {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        assertThat(configChangedLatch.await(UI_INTENSIVE_TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun onSizeChangedTest() {
        addViewToLayout()
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        activity.runOnUiThread {
            view.layoutParams = LinearLayout.LayoutParams(100, 200)
        }
        assertThat(resizeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
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

    /**
     * Ensures that the input token passed when opening a session is non-null and is the same host
     * token as another [SurfaceView] in the same activity.
     */
    @Test
    fun inputTokenIsCorrect() {
        lateinit var layout: LinearLayout
        val surfaceView = SurfaceView(context)
        val surfaceViewLatch = CountDownLatch(1)

        // Attach SurfaceView
        activity.runOnUiThread {
            layout = activity.findViewById(
                R.id.mainlayout
            )
            layout.addView(surfaceView)
        }
        var token: IBinder? = null
        surfaceView.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View) {
                    token = surfaceView.hostToken
                    surfaceViewLatch.countDown()
                }

                override fun onViewDetachedFromWindow(p0: View) {
                }
            }
        )

        // Verify SurfaceView has a non-null token when attached.
        assertThat(surfaceViewLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(surfaceView.hostToken).isNotNull()
        activity.runOnUiThread {
            layout.removeView(surfaceView)
        }

        // Verify that the UI adapter receives the same host token object when opening a session.
        addViewToLayout()
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(testSandboxedUiAdapter.inputToken).isEqualTo(token)
    }

    @Test
    fun getBoundingParent_withoutScrollParent() {
        addViewToLayout()
        onView(withId(R.id.mainlayout)).check(matches(isDisplayed()))
        val boundingRect = Rect()
        assertThat(view.getBoundingParent(boundingRect)).isTrue()
        val rootView: ViewGroup = activity.findViewById(android.R.id.content)
        val rootRect = Rect()
        rootView.getGlobalVisibleRect(rootRect)
        assertThat(boundingRect).isEqualTo(rootRect)
    }

    @Test
    fun getBoundingParent_withScrollParent() {
        val scrollViewRect = Rect()
        val scrollView = activity.findViewById<ScrollView>(R.id.scroll_view)
        activity.runOnUiThread {
            scrollView.visibility = View.VISIBLE
            scrollView.addView(view)
        }
        onView(withId(R.id.scroll_view)).check(matches(isDisplayed()))
        assertThat(scrollView.getGlobalVisibleRect(scrollViewRect)).isTrue()
        val boundingRect = Rect()
        assertThat(view.getBoundingParent(boundingRect)).isTrue()
        assertThat(scrollViewRect).isEqualTo(boundingRect)
    }

    /**
     * Ensures that ACTIVE will only be sent to registered state change listeners after the next
     * frame commit.
     */
    @Test
    fun activeStateOnlySentAfterNextFrameCommitted() {
        addViewToLayout()
        var latch = CountDownLatch(1)
        view.addStateChangedListener {
            if (it == SandboxedSdkUiSessionState.Active) {
                latch.countDown()
            }
        }
        assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()

        // Manually set state to IDLE.
        // Subsequent frame commits should not flip the state back to ACTIVE.
        view.stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Idle
        latch = CountDownLatch(1)
        assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    private fun addViewToLayout() {
        activity.runOnUiThread {
            activity.findViewById<LinearLayout>(
                R.id.mainlayout
            ).addView(view)
        }
    }
}
