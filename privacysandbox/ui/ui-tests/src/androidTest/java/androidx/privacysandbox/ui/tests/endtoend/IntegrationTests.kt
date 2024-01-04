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

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RunWith(AndroidJUnit4::class)
@MediumTest
class IntegrationTests {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    companion object {
        const val TIMEOUT = 1000.toLong()
    }

    private lateinit var context: Context
    private lateinit var activity: AppCompatActivity
    private lateinit var view: SandboxedSdkView
    private lateinit var stateChangeListener: TestStateChangeListener
    private lateinit var errorLatch: CountDownLatch

    @Before
    fun setup() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        context = InstrumentationRegistry.getInstrumentation().context
        activity = activityScenarioRule.withActivity { this }
        view = SandboxedSdkView(context)
        errorLatch = CountDownLatch(1)
        stateChangeListener = TestStateChangeListener(errorLatch)
        view.addStateChangedListener(stateChangeListener)
        activity.runOnUiThread {
            val linearLayout = LinearLayout(context)
            linearLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            activity.setContentView(linearLayout)
            view.layoutParams = LinearLayout.LayoutParams(100, 100)
            linearLayout.addView(view)
        }
    }

    @Ignore // b/271299184
    @Test
    fun testChangingSandboxedSdkViewLayoutChangesChildLayout() {
        val adapter = TestSandboxedUiAdapter(
            null,
            null,
            false /* hasFailiningTestSession */
        )
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val userRemoteAdapter = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.setAdapter(userRemoteAdapter)

        val layoutChangeLatch = CountDownLatch(1)
        val childAddedLatch = CountDownLatch(1)

        val hierarchyChangeListener = object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                childAddedLatch.countDown()
            }

            override fun onChildViewRemoved(p0: View?, p1: View?) {
            }
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
                    old_left: Int,
                    old_top: Int,
                    old_right: Int,
                    old_bottom: Int
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
    fun testSessionOpen() {
        val openSessionLatch = CountDownLatch(1)
        val adapter = TestSandboxedUiAdapter(openSessionLatch, null, false)
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val userRemoteAdapter = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.setAdapter(userRemoteAdapter)

        openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(adapter.isOpenSessionCalled)
        var isSessionInitialised = try {
            adapter.session
            true
        } catch (e: UninitializedPropertyAccessException) {
            false
        }
        assertTrue(isSessionInitialised)
    }

    @Test
    fun testOpenSessionFromAdapter() {
        val openSessionLatch = CountDownLatch(1)
        val adapter = TestSandboxedUiAdapter(openSessionLatch, null, false)
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val adapterFromCoreLibInfo = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        val testSessionClient = TestSandboxedUiAdapter.TestSessionClient()

        adapterFromCoreLibInfo.openSession(
            context,
            Binder(),
            10 /* initialWidth */,
            10 /* initialHeight */,
            true,
            Runnable::run,
            testSessionClient
        )

        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(adapter.isOpenSessionCalled).isTrue()
        assertThat(testSessionClient.isSessionOpened).isTrue()
    }

    @Test
    @Ignore("b/272324246")
    fun testConfigurationChanged() {
        val configChangedLatch = CountDownLatch(1)
        val adapter = TestSandboxedUiAdapter(
            null,
            configChangedLatch,
            false
        )
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val adapterFromCoreLibInfo = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.setAdapter(adapterFromCoreLibInfo)
        activity.runOnUiThread {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        configChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(configChangedLatch.count == 0.toLong())
    }

    /**
     * Tests that the provider receives Z-order change updates.
     */
    @Test
    fun testZOrderChanged() {
        val openSessionLatch = CountDownLatch(1)
        val adapter = TestSandboxedUiAdapter(
            openSessionLatch,
            null,
            /* hasFailingTestSession=*/false
        )
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val adapterFromCoreLibInfo = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.setAdapter(adapterFromCoreLibInfo)
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        view.orderProviderUiAboveClientUi(!adapter.initialZOrderOnTop)
        assertThat(adapter.zOrderLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    /**
     * Tests that the provider does not receive Z-order updates if the Z-order is unchanged.
     */
    @Test
    fun testZOrderUnchanged() {
        val openSessionLatch = CountDownLatch(1)
        val adapter = TestSandboxedUiAdapter(
            openSessionLatch,
            null,
            /* hasFailingTestSession=*/false
        )
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val adapterFromCoreLibInfo = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.setAdapter(adapterFromCoreLibInfo)
        assertThat(openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        view.orderProviderUiAboveClientUi(adapter.initialZOrderOnTop)
        assertThat(adapter.zOrderLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun testHostCanSetZOrderAboveBeforeOpeningSession() {
        val adapter = openSessionAndWaitToBeActive(true)
        injectInputEventOnView()
        // the injected touch should be handled by the provider in Z-above mode
        assertThat(adapter.touchedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun testHostCanSetZOrderBelowBeforeOpeningSession() {
        val adapter = openSessionAndWaitToBeActive(false)
        injectInputEventOnView()
        // the injected touch should not reach the provider in Z-below mode
        assertThat(adapter.touchedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun testSessionError() {
        val adapter = TestSandboxedUiAdapter(
            null, null, true
        )
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val adapterThatFailsToCreateUi =
            SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.setAdapter(adapterThatFailsToCreateUi)
        errorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
        assertTrue(stateChangeListener.currentState is SandboxedSdkUiSessionState.Error)
        val errorMessage = (stateChangeListener.currentState as
            SandboxedSdkUiSessionState.Error).throwable.message
        assertTrue(errorMessage == "Test Session Exception")
    }

    private fun openSessionAndWaitToBeActive(initialZOrder: Boolean): TestSandboxedUiAdapter {
        val adapter = TestSandboxedUiAdapter(
            null,
            null,
            /* hasFailingTestSession=*/false
        )
        val coreLibInfo = adapter.toCoreLibInfo(context)
        val adapterFromCoreLibInfo = SandboxedUiAdapterFactory.createFromCoreLibInfo(coreLibInfo)
        view.orderProviderUiAboveClientUi(initialZOrder)
        view.setAdapter(adapterFromCoreLibInfo)
        val activeLatch = CountDownLatch(1)
        view.addStateChangedListener { state ->
            if (state is SandboxedSdkUiSessionState.Active) {
                activeLatch.countDown()
            }
        }
        assertThat(activeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        return adapter
    }

    private fun injectInputEventOnView() {
        activity.runOnUiThread {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN,
                    (location[0] + 1).toFloat(),
                    (location[1] + 1).toFloat(), 0), false)
        }
    }

    class TestStateChangeListener(private val errorLatch: CountDownLatch) :
        SandboxedSdkUiSessionStateChangedListener {
        var currentState: SandboxedSdkUiSessionState? = null
        var error: Throwable? = null

        override fun onStateChanged(state: SandboxedSdkUiSessionState) {
            currentState = state
            if (state is SandboxedSdkUiSessionState.Error) {
                errorLatch.countDown()
                error = state.throwable
            }
        }
    }

    class TestSandboxedUiAdapter(
        val openSessionLatch: CountDownLatch?,
        val configChangedLatch: CountDownLatch?,
        val hasFailingTestSession: Boolean
    ) : SandboxedUiAdapter {

        var isOpenSessionCalled = false
        var initialZOrderOnTop = false
        var zOrderLatch = CountDownLatch(1)
        var touchedLatch = CountDownLatch(1)
        lateinit var session: SandboxedUiAdapter.Session
        lateinit var internalClient: SandboxedUiAdapter.SessionClient

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
            isOpenSessionCalled = true
            initialZOrderOnTop = isZOrderOnTop
            session = if (hasFailingTestSession) {
                FailingTestSession(context)
            } else {
                TestSession(context)
            }
            client.onSessionOpened(session)
            openSessionLatch?.countDown()
        }

        inner class FailingTestSession(
            private val context: Context
        ) : SandboxedUiAdapter.Session {
            override val view: View
                get() {
                    internalClient.onSessionError(Throwable("Test Session Exception"))
                    return View(context)
                }

            override fun notifyResized(width: Int, height: Int) {
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
            }

            override fun close() {
            }
        }

        inner class TestSession(
            private val context: Context
        ) : SandboxedUiAdapter.Session {
            override val view: View
                get() {
                    return View(context).also {
                        it.setOnTouchListener { _, _ ->
                            touchedLatch.countDown()
                            true
                        }
                    }
                }

            init {
                internalClient.onSessionOpened(this)
            }

            override fun notifyResized(width: Int, height: Int) {
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                zOrderLatch.countDown()
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                configChangedLatch?.countDown()
            }

            override fun close() {
            }
        }

        class TestSessionClient : SandboxedUiAdapter.SessionClient {
            private val latch = CountDownLatch(1)
            val isSessionOpened: Boolean
                get() = latch.await(TIMEOUT, TimeUnit.MILLISECONDS)

            override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
                latch.countDown()
            }

            override fun onSessionError(throwable: Throwable) {
            }

            override fun onResizeRequested(width: Int, height: Int) {
            }
        }
    }
}
