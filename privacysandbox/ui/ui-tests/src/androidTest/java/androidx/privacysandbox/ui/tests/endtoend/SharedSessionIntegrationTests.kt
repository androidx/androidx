/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Bundle
import androidx.privacysandbox.ui.client.SharedUiAdapterFactory
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// TODO(b/263460954): in the following CLs with AppOwnedUiContainer implementation:
//  1) Add tests for cases when the adapter is set on an app-owned container;
//  2) Add state change listener checks to the tests;
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
@RunWith(Parameterized::class)
@MediumTest
class SharedSessionIntegrationTests(private val invokeBackwardsCompatFlow: Boolean) {

    companion object {
        const val TIMEOUT = 1000L
        const val TEST_ONLY_USE_REMOTE_ADAPTER = "testOnlyUseRemoteAdapter"

        @JvmStatic
        @Parameterized.Parameters(name = "invokeBackwardsCompatFlow={0}")
        fun data(): Array<Any> =
            arrayOf(
                arrayOf(true),
                arrayOf(false),
            )
    }

    @get:Rule var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        if (!invokeBackwardsCompatFlow) {
            // Device needs to support remote provider to invoke non-backward-compat flow.
            assumeTrue(BackwardCompatUtil.canProviderBeRemote())
        }
    }

    @Test
    fun testOpenSession_fromAdapter() {
        val client = TestSessionClient()

        val adapter = createAdapterAndEstablishSession(client)

        assertThat(adapter.session).isNotNull()
        assertThat(client.isSessionOpened).isTrue()
    }

    @Test
    fun testSessionError() {
        val client = TestSessionClient()

        createAdapterAndEstablishSession(client, isFailingSession = true)

        assertThat(client.isSessionErrorCalled).isTrue()
    }

    @Test
    fun testCloseSession() {
        val client = TestSessionClient()
        val adapter = createAdapterAndEstablishSession(client)

        client.closeClient()

        assertThat(client.isClientClosed).isTrue()
        assertWithMessage("close is called on Session").that(adapter.isCloseSessionCalled).isTrue()
    }

    @Test
    fun testSessionClientProxy_methodsOnObjectClass() {
        // Only makes sense when a dynamic proxy is involved in the flow
        assumeTrue(invokeBackwardsCompatFlow)
        val testSessionClient = TestSessionClient()

        val sdkAdapter =
            createAdapterAndEstablishSession(testSharedSessionClient = testSessionClient)
        // Verify toString, hashCode and equals have been implemented for dynamic proxy
        val testSession = sdkAdapter.session as TestSharedUiAdapter.TestSession
        val client = testSession.sessionClient

        assertThat(client.toString()).isEqualTo(testSessionClient.toString())
        assertThat(client.equals(client)).isTrue()
        assertThat(client).isNotEqualTo(testSessionClient)
        assertThat(client.hashCode()).isEqualTo(client.hashCode())
    }

    // TODO (b/263460954): add app-owned container as a parameter once it's implemented
    private fun createAdapterAndEstablishSession(
        testSharedSessionClient: TestSessionClient = TestSessionClient(),
        isFailingSession: Boolean = false
    ): TestSharedUiAdapter {
        val adapter = TestSharedUiAdapter(isFailingSession)
        val adapterFromCoreLibInfo =
            SharedUiAdapterFactory.createFromCoreLibInfo(getCoreLibInfoFromAdapter(adapter))
        adapterFromCoreLibInfo.openSession(Runnable::run, testSharedSessionClient)

        assertWithMessage("openSession is called on adapter")
            .that(adapter.isOpenSessionCalled)
            .isTrue()
        return adapter
    }

    private fun getCoreLibInfoFromAdapter(sdkAdapter: SharedUiAdapter): Bundle {
        val bundle = sdkAdapter.toCoreLibInfo()
        bundle.putBoolean(TEST_ONLY_USE_REMOTE_ADAPTER, !invokeBackwardsCompatFlow)
        return bundle
    }

    inner class TestSharedUiAdapter(private val isFailingSession: Boolean = false) :
        SharedUiAdapter {
        private val openSessionLatch: CountDownLatch = CountDownLatch(1)
        private val closeSessionLatch: CountDownLatch = CountDownLatch(1)

        val isOpenSessionCalled: Boolean
            get() = openSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        val isCloseSessionCalled: Boolean
            get() = closeSessionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        lateinit var session: SharedUiAdapter.Session

        override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
            session =
                if (isFailingSession) FailingTestSession(client, clientExecutor)
                else TestSession(client)
            client.onSessionOpened(session)
            openSessionLatch.countDown()
        }

        inner class TestSession(val sessionClient: SharedUiAdapter.SessionClient) :
            SharedUiAdapter.Session {
            override fun close() {
                closeSessionLatch.countDown()
            }
        }

        inner class FailingTestSession(
            val sessionClient: SharedUiAdapter.SessionClient,
            clientExecutor: Executor
        ) : SharedUiAdapter.Session {
            init {
                clientExecutor.execute {
                    sessionClient.onSessionError(Throwable("Test Session Exception"))
                }
            }

            override fun close() {
                closeSessionLatch.countDown()
            }
        }
    }

    inner class TestSessionClient : SharedUiAdapter.SessionClient {
        private val sessionOpenedLatch = CountDownLatch(1)
        private val sessionErrorLatch = CountDownLatch(1)
        private val closeClientLatch = CountDownLatch(1)

        private var session: SharedUiAdapter.Session? = null
            get() {
                sessionOpenedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)
                return field
            }

        val isSessionOpened: Boolean
            get() = sessionOpenedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        val isSessionErrorCalled: Boolean
            get() = sessionErrorLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        val isClientClosed: Boolean
            get() = closeClientLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)

        fun closeClient() {
            val localSession = session
            if (localSession != null) {
                localSession.close()
                closeClientLatch.countDown()
            }
        }

        override fun onSessionOpened(session: SharedUiAdapter.Session) {
            this.session = session
            sessionOpenedLatch.countDown()
        }

        override fun onSessionError(throwable: Throwable) {
            sessionErrorLatch.countDown()
        }
    }
}
