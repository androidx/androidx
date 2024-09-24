/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.tests.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.FrameLayout
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import androidx.privacysandbox.ui.tests.endtoend.IntegrationTests
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/** A utility class for performing session-related operations for integration testing. */
class TestSessionManager(
    private val context: Context,
    private val invokeBackwardsCompatFlow: Boolean
) {

    companion object {
        const val TEST_ONLY_USE_REMOTE_ADAPTER = "testOnlyUseRemoteAdapter"
        const val TIMEOUT = 1000.toLong()
        const val SDK_VIEW_COLOR = Color.YELLOW
    }

    /**
     * Creates a [TestSandboxedUiAdapter] and establishes a session.
     *
     * If [viewForSession] is null, then session is opened using the adapter directly. Otherwise,
     * the created adapter is set on [viewForSession] to establish the session.
     */
    fun createAdapterAndEstablishSession(
        hasFailingTestSession: Boolean = false,
        placeViewInsideFrameLayout: Boolean = false,
        viewForSession: SandboxedSdkView?,
        testSessionClient: TestSessionClient = TestSessionClient(),
        sessionObserverFactories: List<SessionObserverFactory>? = null
    ): TestSandboxedUiAdapter {

        val adapter = TestSandboxedUiAdapter(hasFailingTestSession, placeViewInsideFrameLayout)
        sessionObserverFactories?.forEach { adapter.addObserverFactory(it) }
        val adapterFromCoreLibInfo =
            SandboxedUiAdapterFactory.createFromCoreLibInfo(getCoreLibInfoFromAdapter(adapter))
        if (viewForSession != null) {
            viewForSession.setAdapter(adapterFromCoreLibInfo)
        } else {
            adapterFromCoreLibInfo.openSession(
                context,
                windowInputToken = Binder(),
                IntegrationTests.INITIAL_WIDTH,
                IntegrationTests.INITIAL_HEIGHT,
                isZOrderOnTop = true,
                clientExecutor = Runnable::run,
                testSessionClient
            )
        }

        assertWithMessage("openSession is called on adapter")
            .that(adapter.isOpenSessionCalled)
            .isTrue()
        if (viewForSession == null) {
            assertWithMessage("onSessionOpened received by SessionClient")
                .that(testSessionClient.isSessionOpened)
                .isTrue()
        }
        return adapter
    }

    fun createAdapterAndWaitToBeActive(
        initialZOrder: Boolean = true,
        viewForSession: SandboxedSdkView,
        placeViewInsideFrameLayout: Boolean = false,
        sessionObserverFactories: List<SessionObserverFactory>? = null,
    ): TestSandboxedUiAdapter {
        viewForSession.orderProviderUiAboveClientUi(initialZOrder)

        val adapter =
            createAdapterAndEstablishSession(
                placeViewInsideFrameLayout = placeViewInsideFrameLayout,
                viewForSession = viewForSession,
                sessionObserverFactories = sessionObserverFactories
            )

        val activeLatch = CountDownLatch(1)
        viewForSession.addStateChangedListener { state ->
            if (state is SandboxedSdkUiSessionState.Active) {
                activeLatch.countDown()
            }
        }
        assertThat(activeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        return adapter
    }

    /**
     * TestSandboxedUiAdapter provides content from a fake SDK to show on the host's UI.
     *
     * A [SandboxedUiAdapter] is supposed to fetch the content from SandboxedSdk, but we fake the
     * source of content in this class.
     *
     * If [hasFailingTestSession] is true, the fake server side logic returns error.
     */
    class TestSandboxedUiAdapter(
        private val hasFailingTestSession: Boolean = false,
        private val placeViewInsideFrameLayout: Boolean = false
    ) : AbstractSandboxedUiAdapter() {

        private val openSessionLatch: CountDownLatch = CountDownLatch(1)

        val isOpenSessionCalled: Boolean
            get() = openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        var initialZOrderOnTop = false
        var touchedLatch = CountDownLatch(1)

        lateinit var session: SandboxedUiAdapter.Session
        var initialHeight: Int = -1
        var initialWidth: Int = -1

        override fun openSession(
            context: Context,
            windowInputToken: IBinder,
            initialWidth: Int,
            initialHeight: Int,
            isZOrderOnTop: Boolean,
            clientExecutor: Executor,
            client: SandboxedUiAdapter.SessionClient
        ) {
            initialZOrderOnTop = isZOrderOnTop
            this.initialHeight = initialHeight
            this.initialWidth = initialWidth
            session =
                if (hasFailingTestSession) {
                    FailingTestSession(context, client, clientExecutor)
                } else {
                    TestSession(context, client, placeViewInsideFrameLayout)
                }
            clientExecutor.execute {
                client.onSessionOpened(session)
                openSessionLatch.countDown()
            }
        }

        /**
         * A failing session that always sends error notice to the client when content is requested.
         */
        inner class FailingTestSession(
            private val context: Context,
            sessionClient: SandboxedUiAdapter.SessionClient,
            private val clientExecutor: Executor,
        ) : TestSession(context, sessionClient) {
            override val view: View
                get() {
                    clientExecutor.execute {
                        sessionClient.onSessionError(Throwable("Test Session Exception"))
                    }
                    return View(context)
                }

            override fun notifyResized(width: Int, height: Int) {}

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

            override fun notifyConfigurationChanged(configuration: Configuration) {}

            override fun close() {}
        }

        open inner class TestSession(
            private val context: Context,
            val sessionClient: SandboxedUiAdapter.SessionClient,
            private val placeViewInsideFrameLayout: Boolean = false
        ) : AbstractSession() {

            private val configLatch = CountDownLatch(1)
            private val resizeLatch = CountDownLatch(1)
            private val zOrderLatch = CountDownLatch(1)
            private val sizeChangedLatch = CountDownLatch(1)
            private val layoutLatch = CountDownLatch(1)
            private var width = -1
            private var height = -1

            var config: Configuration? = null
                get() {
                    configLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
                    return field
                }

            var zOrderChanged = false
                get() {
                    zOrderLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
                    return field
                }

            var resizedWidth = 0
                get() {
                    resizeLatch.await(TIMEOUT * 2, TimeUnit.MILLISECONDS)
                    return field
                }

            var resizedHeight = 0
                get() {
                    resizeLatch.await(TIMEOUT * 2, TimeUnit.MILLISECONDS)
                    return field
                }

            inner class TestView(context: Context) : View(context) {
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    canvas.drawColor(SDK_VIEW_COLOR)
                }
            }

            private val testView: View =
                TestView(context).also {
                    it.setOnTouchListener { _, _ ->
                        touchedLatch.countDown()
                        true
                    }
                    it.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                        width = right - left
                        height = bottom - top
                        // Don't count down for the initial layout. We want to capture the
                        // layout change for a size change.
                        if (width != initialWidth || height != initialHeight) {
                            sizeChangedLatch.countDown()
                        }
                        layoutLatch.countDown()
                    }
                }

            override val view: View by lazy {
                if (placeViewInsideFrameLayout) {
                    FrameLayout(context).also { it.addView(testView) }
                } else {
                    testView
                }
            }

            override fun notifyResized(width: Int, height: Int) {
                resizedWidth = width
                resizedHeight = height
                resizeLatch.countDown()
            }

            override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
                zOrderChanged = true
                zOrderLatch.countDown()
            }

            override fun notifyConfigurationChanged(configuration: Configuration) {
                config = configuration
                configLatch.countDown()
            }

            override fun close() {}

            internal fun assertResizeOccurred(expectedWidth: Int, expectedHeight: Int) {
                assertThat(sizeChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
                assertThat(width).isEqualTo(expectedWidth)
                assertThat(height).isEqualTo(expectedHeight)
            }

            internal fun assertResizeDidNotOccur() {
                assertThat(sizeChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
            }

            internal fun assertViewWasLaidOut() {
                assertThat(layoutLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
            }
        }
    }

    class TestSessionClient : SandboxedUiAdapter.SessionClient {
        private val sessionOpenedLatch = CountDownLatch(1)
        private val resizeRequestedLatch = CountDownLatch(1)

        private var session: SandboxedUiAdapter.Session? = null
            get() {
                sessionOpenedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
                return field
            }

        val isSessionOpened: Boolean
            get() = sessionOpenedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        private var resizedWidth = 0
            get() {
                resizeRequestedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
                return field
            }

        private var resizedHeight = 0
            get() {
                resizeRequestedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
                return field
            }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            this.session = session
            sessionOpenedLatch.countDown()
        }

        override fun onSessionError(throwable: Throwable) {}

        override fun onResizeRequested(width: Int, height: Int) {
            resizedWidth = width
            resizedHeight = height
            resizeRequestedLatch.countDown()
        }
    }

    class SessionObserverFactoryImpl : SessionObserverFactory {
        val sessionObservers: MutableList<SessionObserverImpl> = mutableListOf()
        private val sessionObserverCreatedLatch = CountDownLatch(1)

        override fun create(): SessionObserver {
            sessionObserverCreatedLatch.countDown()
            val sessionObserver = SessionObserverImpl()
            sessionObservers.add(sessionObserver)
            return sessionObserver
        }

        fun assertNoSessionsAreCreated() {
            assertThat(sessionObserverCreatedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
        }
    }

    class SessionObserverImpl : SessionObserver {
        var sessionObserverContext: SessionObserverContext? = null
        private val sessionOpenedLatch = CountDownLatch(1)
        private val sessionClosedLatch = CountDownLatch(1)
        private val uiContainerChangedLatch = CountDownLatch(1)

        override fun onSessionOpened(sessionObserverContext: SessionObserverContext) {
            this.sessionObserverContext = sessionObserverContext
            sessionOpenedLatch.countDown()
        }

        override fun onUiContainerChanged(uiContainerInfo: Bundle) {
            uiContainerChangedLatch.countDown()
        }

        override fun onSessionClosed() {
            sessionClosedLatch.countDown()
        }

        fun assertSessionOpened() {
            assertThat(sessionOpenedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun assertOnUiContainerChangedSent() {
            assertThat(uiContainerChangedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun assertSessionClosed() {
            assertThat(sessionClosedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }
    }

    fun getCoreLibInfoFromAdapter(sdkAdapter: SandboxedUiAdapter): Bundle {
        val bundle = sdkAdapter.toCoreLibInfo(context)
        bundle.putBoolean(TEST_ONLY_USE_REMOTE_ADAPTER, !invokeBackwardsCompatFlow)
        return bundle
    }
}
