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

package androidx.privacysandbox.ui.tests.util

import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SharedUiAdapter
import androidx.privacysandbox.ui.tests.endtoend.SharedSessionIntegrationTests
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

// TODO(b/340471014): remove global latches once shared UI event listener is implemented.
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class TestSharedUiAdapter(
    private val isFailingSession: Boolean = false,
    private val globalOpenSessionLatch: CountDownLatch? = null,
    private val globalCloseSessionLatch: CountDownLatch? = null,
) : SharedUiAdapter {
    private val openSessionLatch: CountDownLatch = CountDownLatch(1)
    private val closeSessionLatch: CountDownLatch = CountDownLatch(1)

    val isOpenSessionCalled: Boolean
        get() =
            openSessionLatch.await(
                SharedSessionIntegrationTests.Companion.TIMEOUT,
                TimeUnit.MILLISECONDS,
            )

    val isCloseSessionCalled: Boolean
        get() =
            closeSessionLatch.await(
                SharedSessionIntegrationTests.Companion.TIMEOUT,
                TimeUnit.MILLISECONDS,
            )

    lateinit var session: SharedUiAdapter.Session
    lateinit var client: SharedUiAdapter.SessionClient

    fun triggerOnSessionError() {
        client.onSessionError(Throwable("Test Exception"))
    }

    override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
        session =
            if (isFailingSession) FailingTestSession(client, clientExecutor)
            else TestSession(client)
        client.onSessionOpened(session)
        openSessionLatch.countDown()
        globalOpenSessionLatch?.countDown()
    }

    inner class TestSession(val sessionClient: SharedUiAdapter.SessionClient) :
        SharedUiAdapter.Session {
        override fun close() {
            closeSessionLatch.countDown()
            globalCloseSessionLatch?.countDown()
        }
    }

    inner class FailingTestSession(
        val sessionClient: SharedUiAdapter.SessionClient,
        clientExecutor: Executor,
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

@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class TestSharedUiSessionClient : SharedUiAdapter.SessionClient {
    private val sessionOpenedLatch = CountDownLatch(1)
    private val sessionErrorLatch = CountDownLatch(1)
    private val closeClientLatch = CountDownLatch(1)

    private var session: SharedUiAdapter.Session? = null
        get() {
            sessionOpenedLatch.await(
                SharedSessionIntegrationTests.Companion.TIMEOUT,
                TimeUnit.MILLISECONDS,
            )
            return field
        }

    val isSessionOpened: Boolean
        get() =
            sessionOpenedLatch.await(
                SharedSessionIntegrationTests.Companion.TIMEOUT,
                TimeUnit.MILLISECONDS,
            )

    val isSessionErrorCalled: Boolean
        get() =
            sessionErrorLatch.await(
                SharedSessionIntegrationTests.Companion.TIMEOUT,
                TimeUnit.MILLISECONDS,
            )

    val isClientClosed: Boolean
        get() =
            closeClientLatch.await(
                SharedSessionIntegrationTests.Companion.TIMEOUT,
                TimeUnit.MILLISECONDS,
            )

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
