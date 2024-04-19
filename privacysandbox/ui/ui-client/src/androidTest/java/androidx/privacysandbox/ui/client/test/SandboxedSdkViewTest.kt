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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.IBinder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.BackwardCompatUtil
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
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SandboxedSdkViewTest {

    companion object {
        const val TIMEOUT = 1000.toLong()
        // Longer timeout used for expensive operations like device rotation.
        const val UI_INTENSIVE_TIMEOUT = 2000.toLong()
    }

    private lateinit var context: Context
    private lateinit var view: SandboxedSdkView
    private lateinit var layoutParams: LayoutParams
    private lateinit var testSandboxedUiAdapter: TestSandboxedUiAdapter
    private lateinit var stateChangedListener: StateChangedListener
    private var mainLayoutWidth = -1
    private var mainLayoutHeight = -1

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

    class TestSandboxedUiAdapter : SandboxedUiAdapter {

        var isSessionOpened = false
        var internalClient: SandboxedUiAdapter.SessionClient? = null
        var testSession: TestSession? = null
        var isZOrderOnTop = true
        var inputToken: IBinder? = null

        // When set to true, the onSessionOpened callback will only be invoked when specified
        // by the test. This is to test race conditions when the session is being loaded.
        var delayOpenSessionCallback = false

        private val openSessionLatch = CountDownLatch(1)
        private val resizeLatch = CountDownLatch(1)
        private val configChangedLatch = CountDownLatch(1)

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
                TestSession(context, initialWidth, initialHeight)
            if (!delayOpenSessionCallback) {
                client.onSessionOpened(testSession!!)
            }
            isSessionOpened = true
            this.isZOrderOnTop = isZOrderOnTop
            this.inputToken = windowInputToken
            openSessionLatch.countDown()
        }

        internal fun sendOnSessionOpened() {
            internalClient?.onSessionOpened(testSession!!)
        }

        internal fun assertSessionOpened() {
            assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        internal fun wasNotifyResizedCalled(): Boolean {
            return resizeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        }

        internal fun wasOnConfigChangedCalled(): Boolean {
            return configChangedLatch.await(UI_INTENSIVE_TIMEOUT, TimeUnit.MILLISECONDS)
        }

        inner class TestSession(
            context: Context,
            initialWidth: Int,
            initialHeight: Int,
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
                resizeLatch.countDown()
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                this@TestSandboxedUiAdapter.isZOrderOnTop = isZOrderOnTop
                zOrderChangedLatch.countDown()
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                configChangedLatch.countDown()
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
        context = InstrumentationRegistry.getInstrumentation().targetContext
        activityScenarioRule.withActivity {
            view = SandboxedSdkView(this)
            stateChangedListener = StateChangedListener()
            view.addStateChangedListener(stateChangedListener)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            testSandboxedUiAdapter = TestSandboxedUiAdapter()
            view.setAdapter(testSandboxedUiAdapter)
        }
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
        testSandboxedUiAdapter.assertSessionOpened()
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)
    }

    @Test
    fun childViewRemovedOnErrorTest() {
        assertTrue(view.childCount == 0)
        addViewToLayout()

        testSandboxedUiAdapter.assertSessionOpened()
        assertTrue(view.childCount == 1)
        assertTrue(view.layoutParams == layoutParams)

        activityScenarioRule.withActivity {
            testSandboxedUiAdapter.internalClient!!.onSessionError(Exception())
            assertTrue(view.childCount == 0)
        }
    }

    @Test
    fun onZOrderChangedTest() {
        addViewToLayout()

        // When session is opened, the provider should not receive a Z-order notification.
        testSandboxedUiAdapter.assertSessionOpened()
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
        testSandboxedUiAdapter.assertSessionOpened()
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
        testSandboxedUiAdapter.assertSessionOpened()
        val session = testSandboxedUiAdapter.testSession!!

        // The initial Z-order state is passed to the session, but notifyZOrderChanged is not called
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(testSandboxedUiAdapter.isZOrderOnTop).isFalse()
    }

    @Test
    fun setZOrderNotOnTopWhileSessionLoading() {
        testSandboxedUiAdapter.delayOpenSessionCallback = true
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        view.orderProviderUiAboveClientUi(false)
        val session = testSandboxedUiAdapter.testSession!!
        assertThat(session.zOrderChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        activityScenarioRule.withActivity {
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
        testSandboxedUiAdapter.assertSessionOpened()
        // newWindow() will be triggered by a window state change, even if the activity handles
        // orientation changes without recreating the activity.
        device.performActionAndWait({
            device.setOrientationLeft()
        }, Until.newWindow(), UI_INTENSIVE_TIMEOUT)
        testSandboxedUiAdapter.assertSessionOpened()
        device.performActionAndWait({
            device.setOrientationNatural()
        }, Until.newWindow(), UI_INTENSIVE_TIMEOUT)
    }

    @Test
    fun onConfigurationChangedTestSameConfiguration() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        assertThat(testSandboxedUiAdapter.wasOnConfigChangedCalled()).isFalse()
    }

    @Test
    fun onLayoutTestWithSizeChange() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity {
            view.layoutParams = LinearLayout.LayoutParams(100, 200)
        }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isTrue()
        assertTrue(view.width == 100 && view.height == 200)
    }

    @Test
    fun onLayoutTestNoSizeChange() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        activityScenarioRule.withActivity {
            view.layout(view.left, view.top, view.right, view.bottom)
        }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isFalse()
    }

    @Test
    fun onLayoutTestViewShiftWithoutSizeChange() {
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        val rightShift = 10
        val upperShift = 30
        activityScenarioRule.withActivity {
            view.layout(view.left + rightShift, view.top - upperShift,
                view.right + rightShift, view.bottom - upperShift)
        }
        assertThat(testSandboxedUiAdapter.wasNotifyResizedCalled()).isFalse()
    }

    @Test
    fun onSdkRequestsResizeTest() {
        val globalLayoutLatch = CountDownLatch(1)
        lateinit var layout: LinearLayout
        activityScenarioRule.withActivity {
            layout = findViewById<LinearLayout>(
                R.id.mainlayout
            )
            layout.addView(view)
        }
        testSandboxedUiAdapter.assertSessionOpened()
        testSandboxedUiAdapter.testSession?.requestSizeChange(layout.width, layout.height)
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
        activityScenarioRule.withActivity {
            val view = SandboxedSdkView(context)
            assertTrue("SandboxedSdkView isTransitionGroup by default", view.isTransitionGroup)
        }
    }

    @Test
    fun sandboxedSdkViewInflatesTransitionGroup() {
        activityScenarioRule.withActivity {
            val view = layoutInflater.inflate(
                R.layout.sandboxedsdkview_transition_group_false,
                null,
                false
            ) as ViewGroup
            assertFalse(
                "XML overrides SandboxedSdkView.isTransitionGroup", view.isTransitionGroup
            )
        }
    }

    /**
     * Ensures that the input token passed when opening a session is non-null and is the same host
     * token as another [SurfaceView] in the same activity.
     */
    @SuppressLint("NewApi") // Test runs on U+ devices
    @Test
    fun inputTokenIsCorrect() {
        // Input token is only needed when provider can be located on a separate process.
        assumeTrue(BackwardCompatUtil.canProviderBeRemote())

        lateinit var layout: LinearLayout
        val surfaceView = SurfaceView(context)
        val surfaceViewLatch = CountDownLatch(1)

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

        // Attach SurfaceView
        activityScenarioRule.withActivity {
            layout = findViewById(
                R.id.mainlayout
            )
            layout.addView(surfaceView)
            layout.removeView(surfaceView)
        }

        // Verify SurfaceView has a non-null token when attached.
        assertThat(surfaceViewLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(token).isNotNull()

        // Verify that the UI adapter receives the same host token object when opening a session.
        addViewToLayout()
        testSandboxedUiAdapter.assertSessionOpened()
        assertThat(testSandboxedUiAdapter.inputToken).isEqualTo(token)
    }

    @Test
    fun getBoundingParent_withoutScrollParent() {
        addViewToLayout()
        onView(withId(R.id.mainlayout)).check(matches(isDisplayed()))
        activityScenarioRule.withActivity {
            val boundingRect = Rect()
            assertThat(view.maybeUpdateClippingBounds(boundingRect)).isTrue()
            val rootView: ViewGroup = findViewById(android.R.id.content)
            val rootRect = Rect()
            rootView.getGlobalVisibleRect(rootRect)
            assertThat(boundingRect).isEqualTo(rootRect)
        }
    }

    @Test
    fun getBoundingParent_withScrollParent() {
        lateinit var scrollView: ScrollView
        activityScenarioRule.withActivity {
            scrollView = findViewById<ScrollView>(R.id.scroll_view)
            scrollView.visibility = View.VISIBLE
            scrollView.addView(view)
        }
        onView(withId(R.id.scroll_view)).check(matches(isDisplayed()))

        val scrollViewRect = Rect()
        assertThat(scrollView.getGlobalVisibleRect(scrollViewRect)).isTrue()
        val boundingRect = Rect()
        assertThat(view.maybeUpdateClippingBounds(boundingRect)).isTrue()
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

    @Ignore("b/307829956")
    @Test
    fun requestSizeWithMeasureSpecAtMost_withinParentBounds() {
        view.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addViewToLayoutAndWaitToBeActive()
        requestSizeAndVerifyLayout(
            /* requestedWidth=*/ mainLayoutWidth - 100,
            /* requestedHeight=*/ mainLayoutHeight - 100,
            /* expectedWidth=*/ mainLayoutWidth - 100,
            /* expectedHeight=*/ mainLayoutHeight - 100)
    }

    @Test
    fun requestSizeWithMeasureSpecAtMost_exceedsParentBounds() {
        view.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        addViewToLayoutAndWaitToBeActive()
        // the resize is constrained by the parent's size
        requestSizeAndVerifyLayout(
            /* requestedWidth=*/ mainLayoutWidth + 100,
            /* requestedHeight=*/ mainLayoutHeight + 100,
            /* expectedWidth=*/ mainLayoutWidth,
            /* expectedHeight=*/ mainLayoutHeight)
    }

    @Test
    fun requestSizeWithMeasureSpecExactly() {
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addViewToLayoutAndWaitToBeActive()
        val currentWidth = view.width
        val currentHeight = view.height
        // the request is a no-op when the MeasureSpec is EXACTLY
        requestSizeAndVerifyLayout(
            /* requestedWidth=*/ currentWidth - 100,
            /* requestedHeight=*/ currentHeight - 100,
            /* expectedWidth=*/ currentWidth,
            /* expectedHeight=*/ currentHeight)
    }

    private fun addViewToLayout(waitToBeActive: Boolean = false) {
        activityScenarioRule.withActivity {
            val mainLayout: LinearLayout = findViewById(R.id.mainlayout)
            mainLayoutWidth = mainLayout.width
            mainLayoutHeight = mainLayout.height
            mainLayout.addView(view)
        }
        if (waitToBeActive) {
            val latch = CountDownLatch(1)
            view.addStateChangedListener {
                if (it == SandboxedSdkUiSessionState.Active) {
                    latch.countDown()
                }
            }
            assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    private fun addViewToLayoutAndWaitToBeActive() {
        addViewToLayout(true)
    }

    private fun requestSizeAndVerifyLayout(
        requestedWidth: Int,
        requestedHeight: Int,
        expectedWidth: Int,
        expectedHeight: Int
    ) {
        val layoutLatch = CountDownLatch(1)
        var width = -1
        var height = -1
        view.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            width = right - left
            height = bottom - top
            layoutLatch.countDown()
        }
        activityScenarioRule.withActivity {
            view.requestSize(requestedWidth, requestedHeight)
        }
        assertThat(layoutLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(width).isEqualTo(expectedWidth)
        assertThat(height).isEqualTo(expectedHeight)
    }
}
