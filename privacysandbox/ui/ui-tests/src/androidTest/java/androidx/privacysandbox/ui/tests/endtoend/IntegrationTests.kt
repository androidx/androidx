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

package androidx.privacysandbox.ui.tests.endtoend

import android.app.Activity
import android.app.Instrumentation
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.tests.util.TestSessionManager
import androidx.privacysandbox.ui.tests.util.TestSessionManager.Companion.TIMEOUT
import androidx.privacysandbox.ui.tests.util.TestSessionManager.TestSandboxedUiAdapter
import androidx.privacysandbox.ui.tests.util.TestSessionManager.TestSessionClient
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@MediumTest
class IntegrationTests(private val invokeBackwardsCompatFlow: Boolean) {

    @get:Rule var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    companion object {
        const val INITIAL_HEIGHT = 100
        const val INITIAL_WIDTH = 100

        @JvmStatic
        @Parameterized.Parameters(name = "invokeBackwardsCompatFlow={0}")
        fun data(): Array<Any> =
            arrayOf(
                arrayOf(true),
                arrayOf(false),
            )
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var view: SandboxedSdkView
    private lateinit var stateChangeListener: TestStateChangeListener
    private lateinit var activity: Activity
    private lateinit var errorLatch: CountDownLatch
    private lateinit var linearLayout: LinearLayout
    private lateinit var mInstrumentation: Instrumentation
    private lateinit var sessionManager: TestSessionManager

    @Before
    fun setup() {
        if (!invokeBackwardsCompatFlow) {
            // Device needs to support remote provider to invoke non-backward-compat flow.
            assumeTrue(BackwardCompatUtil.canProviderBeRemote())
        }

        mInstrumentation = InstrumentationRegistry.getInstrumentation()
        sessionManager = TestSessionManager(context, invokeBackwardsCompatFlow)

        activity = activityScenarioRule.withActivity { this }
        activityScenarioRule.withActivity {
            view = SandboxedSdkView(context)
            errorLatch = CountDownLatch(1)
            stateChangeListener = TestStateChangeListener(errorLatch)
            view.addStateChangedListener(stateChangeListener)
            linearLayout = LinearLayout(context)
            linearLayout.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            linearLayout.setBackgroundColor(Color.RED)
            setContentView(linearLayout)
            view.layoutParams = LinearLayout.LayoutParams(INITIAL_WIDTH, INITIAL_HEIGHT)
            linearLayout.addView(view)
        }
    }

    @Ignore // b/271299184
    @Test
    fun testChangingSandboxedSdkViewLayoutChangesChildLayout() {
        sessionManager.createAdapterAndEstablishSession(viewForSession = view)

        val layoutChangeLatch = CountDownLatch(1)
        val childAddedLatch = CountDownLatch(1)

        val hierarchyChangeListener =
            object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View, child: View) {
                    childAddedLatch.countDown()
                }

                override fun onChildViewRemoved(p0: View?, p1: View?) {}
            }
        view.setOnHierarchyChangeListener(hierarchyChangeListener)

        val onLayoutChangeListener: OnLayoutChangeListener =
            object : OnLayoutChangeListener {
                override fun onLayoutChange(
                    view: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    assertTrue(left == 10 && top == 10 && right == 10 && bottom == 10)
                    layoutChangeLatch.countDown()
                    view?.removeOnLayoutChangeListener(this)
                }
            }
        childAddedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(childAddedLatch.count == 0.toLong())
        view.getChildAt(0).addOnLayoutChangeListener(onLayoutChangeListener)
        view.layout(10, 10, 10, 10)
        layoutChangeLatch.await(2000, TimeUnit.MILLISECONDS)
        assertTrue(layoutChangeLatch.count == 0.toLong())
        assertTrue(stateChangeListener.currentState == SandboxedSdkUiSessionState.Active)
    }

    @Test
    fun testOpenSession_onSetAdapter() {
        val adapter = sessionManager.createAdapterAndEstablishSession(viewForSession = view)
        assertThat(adapter.session).isNotNull()
    }

    @Test
    fun testOpenSession_fromAdapter() {
        val adapter = sessionManager.createAdapterAndEstablishSession(viewForSession = null)
        assertThat(adapter.session).isNotNull()
    }

    @Test
    fun testConfigurationChanged() {
        val sdkAdapter = sessionManager.createAdapterAndEstablishSession(viewForSession = view)

        activityScenarioRule.withActivity {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        val testSession = sdkAdapter.session as TestSandboxedUiAdapter.TestSession
        assertWithMessage("Configuration changed")
            .that(testSession.config?.orientation)
            .isEqualTo(Configuration.ORIENTATION_LANDSCAPE)
    }

    /** Tests that the provider receives Z-order change updates. */
    @Test
    @Ignore("b/302090927")
    fun testZOrderChanged() {
        val adapter = sessionManager.createAdapterAndEstablishSession(viewForSession = view)

        view.orderProviderUiAboveClientUi(!adapter.initialZOrderOnTop)
        val testSession = adapter.session as TestSandboxedUiAdapter.TestSession
        assertThat(testSession.zOrderChanged).isTrue()
    }

    /** Tests that the provider does not receive Z-order updates if the Z-order is unchanged. */
    @Test
    fun testZOrderUnchanged() {
        val adapter = sessionManager.createAdapterAndEstablishSession(viewForSession = view)

        view.orderProviderUiAboveClientUi(adapter.initialZOrderOnTop)
        val testSession = adapter.session as TestSandboxedUiAdapter.TestSession
        assertThat(testSession.zOrderChanged).isFalse()
    }

    @Test
    fun testHostCanSetZOrderAboveBeforeOpeningSession() {
        // TODO(b/301976432): Stop skipping this for backwards compat flow
        assumeTrue(!invokeBackwardsCompatFlow)

        val adapter =
            sessionManager.createAdapterAndWaitToBeActive(
                viewForSession = view,
                initialZOrder = true
            )
        injectInputEventOnView()
        // the injected touch should be handled by the provider in Z-above mode
        assertThat(adapter.touchedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    @Ignore("b/302006586")
    fun testHostCanSetZOrderBelowBeforeOpeningSession() {
        // TODO(b/300396631): Skip for backward compat
        assumeTrue(!invokeBackwardsCompatFlow)

        val adapter =
            sessionManager.createAdapterAndWaitToBeActive(
                viewForSession = view,
                initialZOrder = false
            )
        injectInputEventOnView()
        // the injected touch should not reach the provider in Z-below mode
        assertThat(adapter.touchedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun testSessionError() {
        sessionManager.createAdapterAndEstablishSession(
            viewForSession = view,
            hasFailingTestSession = true
        )

        assertThat(errorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(stateChangeListener.error?.message).isEqualTo("Test Session Exception")
    }

    /**
     * Tests that a provider-initiated resize is accepted if the view's parent does not impose exact
     * restrictions on the view's size.
     */
    @Test
    fun testResizeRequested_requestedAccepted_atMostMeasureSpec() {
        view.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        val sdkAdapter = sessionManager.createAdapterAndWaitToBeActive(viewForSession = view)

        val testSession = sdkAdapter.session as TestSandboxedUiAdapter.TestSession
        val newWidth = INITIAL_WIDTH - 10
        val newHeight = INITIAL_HEIGHT - 10

        activityScenarioRule.withActivity {
            testSession.sessionClient.onResizeRequested(newWidth, newHeight)
        }
        assertWithMessage("Resized width").that(testSession.resizedWidth).isEqualTo(newWidth)
        assertWithMessage("Resized height").that(testSession.resizedHeight).isEqualTo(newHeight)
        testSession.assertResizeOccurred(
            /* expectedWidth=*/ newWidth,
            /* expectedHeight=*/ newHeight
        )
    }

    /**
     * Tests that a provider-initiated resize is ignored if the view's parent provides exact
     * measurements.
     */
    @Test
    fun testResizeRequested_requestIgnored_exactlyMeasureSpec() {
        view.layoutParams = LinearLayout.LayoutParams(INITIAL_WIDTH, INITIAL_HEIGHT)
        val sdkAdapter = sessionManager.createAdapterAndWaitToBeActive(viewForSession = view)
        val testSession = sdkAdapter.session as TestSandboxedUiAdapter.TestSession

        activityScenarioRule.withActivity {
            testSession.sessionClient.onResizeRequested(INITIAL_WIDTH - 10, INITIAL_HEIGHT - 10)
        }
        testSession.assertResizeDidNotOccur()
    }

    @Test
    fun testResize_ClientInitiated() {
        val sdkAdapter = sessionManager.createAdapterAndWaitToBeActive(viewForSession = view)
        val newWidth = INITIAL_WIDTH - 10
        val newHeight = INITIAL_HEIGHT - 10
        activityScenarioRule.withActivity {
            view.layoutParams = LinearLayout.LayoutParams(newWidth, newHeight)
        }

        val testSession = sdkAdapter.session as TestSandboxedUiAdapter.TestSession
        assertWithMessage("Resized width").that(testSession.resizedWidth).isEqualTo(newWidth)
        assertWithMessage("Resized height").that(testSession.resizedHeight).isEqualTo(newHeight)
        testSession.assertResizeOccurred(
            /* expectedWidth=*/ newWidth,
            /* expectedHeight=*/ newHeight
        )
    }

    @Test
    fun testSessionClientProxy_methodsOnObjectClass() {
        // Only makes sense when a dynamic proxy is involved in the flow
        assumeTrue(invokeBackwardsCompatFlow)

        val testSessionClient = TestSessionClient()
        val sdkAdapter =
            sessionManager.createAdapterAndEstablishSession(
                viewForSession = null,
                testSessionClient = testSessionClient
            )

        // Verify toString, hashCode and equals have been implemented for dynamic proxy
        val testSession = sdkAdapter.session as TestSandboxedUiAdapter.TestSession
        val client = testSession.sessionClient

        // TODO(b/329468679): We cannot assert this as we wrap the client on the provider side.
        // assertThat(client.toString()).isEqualTo(testSessionClient.toString())

        assertThat(client.equals(client)).isTrue()
        assertThat(client).isNotEqualTo(testSessionClient)
        assertThat(client.hashCode()).isEqualTo(client.hashCode())
    }

    /**
     * Verifies that when the [View] returned as part of a [SandboxedUiAdapter.Session] is a
     * [ViewGroup], that the child view is measured and laid out by its parent.
     */
    @Test
    fun testViewGroup_ChildViewIsLaidOut() {
        val adapter =
            sessionManager.createAdapterAndWaitToBeActive(
                viewForSession = view,
                placeViewInsideFrameLayout = true
            )
        val session = adapter.session as TestSandboxedUiAdapter.TestSession

        // Force a layout pass by changing the size of the view
        activityScenarioRule.withActivity {
            session.sessionClient.onResizeRequested(INITIAL_WIDTH - 10, INITIAL_HEIGHT - 10)
        }
        session.assertViewWasLaidOut()
    }

    @Test
    fun testAddSessionObserverFactory_ObserverIsCreated() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        sessionManager.createAdapterAndWaitToBeActive(
            viewForSession = view,
            sessionObserverFactories = listOf(factory)
        )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
    }

    @Test
    fun testAddSessionObserverFactory_OnSessionOpenedIsSent() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        sessionManager.createAdapterAndWaitToBeActive(
            viewForSession = view,
            sessionObserverFactories = listOf(factory)
        )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
        val sessionObserver = factory.sessionObservers[0]
        sessionObserver.assertSessionOpened()
    }

    @Test
    fun testAddSessionObserverFactory_NoObserverCreatedForAlreadyOpenSession() {
        val adapter = sessionManager.createAdapterAndWaitToBeActive(viewForSession = view)
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        adapter.addObserverFactory(factory)
        factory.assertNoSessionsAreCreated()
    }

    @Test
    fun testAddSessionObserverFactory_MultipleFactories() {
        val factory1 = TestSessionManager.SessionObserverFactoryImpl()
        val factory2 = TestSessionManager.SessionObserverFactoryImpl()
        sessionManager.createAdapterAndWaitToBeActive(
            viewForSession = view,
            sessionObserverFactories = listOf(factory1, factory2)
        )
        assertThat(factory1.sessionObservers.size).isEqualTo(1)
        assertThat(factory2.sessionObservers.size).isEqualTo(1)
    }

    @Test
    fun testAddSessionObserverFactory_SessionObserverContextIsCorrect() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        val adapter =
            sessionManager.createAdapterAndWaitToBeActive(
                viewForSession = view,
                sessionObserverFactories = listOf(factory)
            )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
        val sessionObserver = factory.sessionObservers[0]
        sessionObserver.assertSessionOpened()
        assertThat(sessionObserver.sessionObserverContext).isNotNull()
        assertThat(sessionObserver.sessionObserverContext?.view).isEqualTo(adapter.session.view)
    }

    @Test
    fun testRegisterSessionObserverFactory_OnUiContainerChangedSentWhenSessionOpened() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        sessionManager.createAdapterAndWaitToBeActive(
            viewForSession = view,
            sessionObserverFactories = listOf(factory)
        )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
        val sessionObserver = factory.sessionObservers[0]
        sessionObserver.assertOnUiContainerChangedSent()
    }

    @Test
    fun testRemoveSessionObserverFactory_DoesNotImpactExistingObservers() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        val adapter =
            sessionManager.createAdapterAndWaitToBeActive(
                viewForSession = view,
                sessionObserverFactories = listOf(factory)
            )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
        adapter.removeObserverFactory(factory)
        val sessionObserver = factory.sessionObservers[0]
        // Setting a new adapter on the SandboxedSdKView will cause the current session to close.
        activityScenarioRule.withActivity { view.setAdapter(TestSandboxedUiAdapter()) }
        // onSessionClosed is still sent for the observer
        sessionObserver.assertSessionClosed()
    }

    @Test
    fun testRemoveSessionObserverFactory_DoesNotCreateObserverForNewSession() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        val adapter =
            sessionManager.createAdapterAndWaitToBeActive(
                viewForSession = view,
                sessionObserverFactories = listOf(factory)
            )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
        adapter.removeObserverFactory(factory)
        val sandboxedSdkView2 = SandboxedSdkView(context)
        activityScenarioRule.withActivity { linearLayout.addView(sandboxedSdkView2) }
        // create a new session and wait to be active
        sandboxedSdkView2.setAdapter(adapter)

        val activeLatch = CountDownLatch(1)
        sandboxedSdkView2.addStateChangedListener { state ->
            if (state is SandboxedSdkUiSessionState.Active) {
                activeLatch.countDown()
            }
        }
        assertThat(activeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        // The session observers size should remain 1, showing that no new observers have been
        // created for the new session.
        assertThat(factory.sessionObservers.size).isEqualTo(1)
    }

    @Test
    fun testSessionObserver_OnClosedSentWhenSessionClosed() {
        val factory = TestSessionManager.SessionObserverFactoryImpl()
        sessionManager.createAdapterAndWaitToBeActive(
            viewForSession = view,
            sessionObserverFactories = listOf(factory)
        )
        assertThat(factory.sessionObservers.size).isEqualTo(1)
        val sessionObserver = factory.sessionObservers[0]
        // Setting a new adapter on the SandboxedSdKView will cause the current session to close.
        activityScenarioRule.withActivity { view.setAdapter(TestSandboxedUiAdapter()) }
        sessionObserver.assertSessionClosed()
    }

    private fun injectInputEventOnView() {
        activityScenarioRule.withActivity {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .injectInputEvent(
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN,
                        (location[0] + 1).toFloat(),
                        (location[1] + 1).toFloat(),
                        0
                    ),
                    false
                )
        }
    }

    class TestStateChangeListener(private val errorLatch: CountDownLatch) :
        SandboxedSdkUiSessionStateChangedListener {
        var currentState: SandboxedSdkUiSessionState? = null
        var error: Throwable? = null

        override fun onStateChanged(state: SandboxedSdkUiSessionState) {
            currentState = state
            if (state is SandboxedSdkUiSessionState.Error) {
                error = state.throwable
                errorLatch.countDown()
            }
        }
    }
}
