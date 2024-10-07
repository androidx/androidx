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

package androidx.privacysandbox.ui.provider.impl

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.util.Consumer
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 23)
class DeferredSessionClientTest {

    @Test
    fun onSessionOpened_whenDemandObjectSuccess_delegates() {
        val stubClient = StubClient()
        val deferredClient = createDeferredClient(stubClient)

        val session = StubSession()
        deferredClient.onSessionOpened(session)

        assertThat(stubClient.sessions).containsExactly(session)
        assertThat(session.closed).isFalse()
    }

    @Test
    fun onSessionOpened_whenDemandObjectFailed_callErrorHandlerAndCloseSession() {
        val exception = RuntimeException("Something went wrong")
        val stubClient = StubClient(exceptionOnInit = exception)
        val fail = AtomicReference<Throwable>()
        val deferredClient = createDeferredClient(stubClient, errorHandler = fail::set)

        val session = StubSession()
        deferredClient.onSessionOpened(session)

        assertThat(fail.get()).isEqualTo(exception)
        assertThat(stubClient.sessions).isEmpty()
        assertThat(session.closed).isTrue()
    }

    @Test
    fun onSessionError_whenDemandObjectSuccess_delegates() {
        val stubClient = StubClient()
        val deferredClient = createDeferredClient(stubClient)

        val sessionError = RuntimeException("Session opening error")
        deferredClient.onSessionError(sessionError)

        assertThat(stubClient.errors).containsExactly(sessionError)
    }

    @Test
    fun onSessionError_whenDemandObjectFailed_callErrorHandler() {
        val exception = RuntimeException("Something went wrong")
        val stubClient = StubClient(exceptionOnInit = exception)
        val fail = AtomicReference<Throwable>()
        val deferredClient = createDeferredClient(stubClient, errorHandler = fail::set)

        val sessionError = RuntimeException("Session opening error")
        deferredClient.onSessionError(sessionError)

        assertThat(fail.get()).isEqualTo(exception)
        assertThat(stubClient.errors).isEmpty()
    }

    @Test
    fun onResizeRequested_whenDemandObjectSuccess_delegates() {
        val stubClient = StubClient()
        val deferredClient = createDeferredClient(stubClient)

        deferredClient.onResizeRequested(1, 2)

        assertThat(stubClient.resizes).containsExactly(Pair(1, 2))
    }

    @Test
    fun onResizeRequested_whenDemandObjectFailed_callErrorHandler() {
        val exception = RuntimeException("Something went wrong")
        val stubClient = StubClient(exceptionOnInit = exception)
        val fail = AtomicReference<Throwable>()
        val deferredClient = createDeferredClient(stubClient, errorHandler = fail::set)

        deferredClient.onResizeRequested(1, 2)

        assertThat(fail.get()).isEqualTo(exception)
        assertThat(stubClient.resizes).isEmpty()
    }

    private fun createDeferredClient(
        stubClient: StubClient,
        errorHandler: Consumer<Throwable> = Consumer {
            Assert.fail("Unexpected fail " + it.message)
        }
    ): DeferredSessionClient {
        return DeferredSessionClient.create(
            clientFactory = { stubClient },
            clientInit = StubClient::initialize,
            errorHandler = errorHandler
        )
    }

    private class StubClient(val exceptionOnInit: Throwable? = null) :
        SandboxedUiAdapter.SessionClient {

        val sessions: MutableList<SandboxedUiAdapter.Session> = mutableListOf()
        val errors: MutableList<Throwable> = mutableListOf()
        val resizes: MutableList<Pair<Int, Int>> = mutableListOf()

        fun initialize() {
            if (exceptionOnInit != null) {
                throw exceptionOnInit
            }
        }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            sessions.add(session)
        }

        override fun onSessionError(throwable: Throwable) {
            errors.add(throwable)
        }

        override fun onResizeRequested(width: Int, height: Int) {
            resizes.add(Pair(width, height))
        }
    }

    private class StubSession : SandboxedUiAdapter.Session {

        var closed: Boolean = false

        override val view: View
            get() = throw UnsupportedOperationException("Not implemented")

        override val signalOptions: Set<String>
            get() = throw UnsupportedOperationException("Not implemented")

        override fun notifyResized(width: Int, height: Int) {}

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

        override fun notifyConfigurationChanged(configuration: Configuration) {}

        override fun notifyUiChanged(uiContainerInfo: Bundle) {}

        override fun close() {
            closed = true
        }
    }
}
