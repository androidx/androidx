/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.phone.interactions.authentication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.wear.phone.interactions.WearPhoneInteractionsTestRunner
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [RemoteAuthClient].  */
@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument // Needed because it is defined in the "android" package.
@Config(minSdk = 26)
@RequiresApi(Build.VERSION_CODES.O)
public class RemoteAuthTest {

    @Config(minSdk = 26)
    @RequiresApi(Build.VERSION_CODES.O)
    internal companion object {
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        private val DIRECT_EXECUTOR = Executor { command -> command.run() }
        private const val authProviderUrlA = "http://myrequesturl/a?client_id=iamtheclient"
        private const val authProviderUrlB = "http://myrequesturl/b?client_id=iamtheclient"
        private val responseUrl = Uri.parse("http://myresponseurl")
        private val appPackageName = context.packageName

        private var requestA: OAuthRequest
        private var requestB: OAuthRequest

        init {
            setSystemFeatureChina(false)

            requestA =
                OAuthRequest.Builder(context)
                    .setAuthProviderUrl(Uri.parse(authProviderUrlA))
                    .setCodeChallenge(CodeChallenge(CodeVerifier()))
                    .build()

            requestB =
                OAuthRequest.Builder(context)
                    .setAuthProviderUrl(Uri.parse(authProviderUrlB))
                    .setCodeChallenge(CodeChallenge(CodeVerifier()))
                    .build()
        }

        private val response = OAuthResponse.Builder().setResponseUrl(responseUrl).build()

        // Note: This can't be static as Robolectric isn't set up at class init time.
        private val mServiceName = ComponentName(
            "com.google.android.wearable.app", "auth_lib_shouldnt_care_about_this"
        )
        private val mockCallback: RemoteAuthClient.Callback =
            Mockito.mock(RemoteAuthClient.Callback::class.java)

        private fun setSystemFeatureChina(value: Boolean) {
            shadowPackageManager.setSystemFeature("cn.google", value)
        }
    }

    private var fakeServiceBinder: FakeServiceBinder = FakeServiceBinder()
    private var fakeService: FakeClockworkHomeAuthService = FakeClockworkHomeAuthService()
    private val remoteInteractionsManager: IRemoteInteractionsManager = mock()
    private var clientUnderTest: RemoteAuthClient =
        RemoteAuthClient(
            remoteInteractionsManager,
            fakeServiceBinder,
            DIRECT_EXECUTOR,
            appPackageName)
    private val executor: Executor = SyncExecutor()

    @Test
    public fun doesntConnectUntilARequestIsMade() {
        // WHEN the client is created
        // THEN the Auth library should not yet connect to Clockwork Home
        assertThat(fakeServiceBinder.state).isEqualTo(ConnectionState.DISCONNECTED)
    }

    @Test
    public fun sendAuthorizationRequestShouldMakeConnectionToClockworkHome() {
        setSystemFeatureChina(false)
        val requestUri = "http://myrequesturl?client_id=xxx"
        // WHEN an authorization request is sent
        clientUnderTest.sendAuthorizationRequest(
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(requestUri))
                .setCodeChallenge(CodeChallenge(CodeVerifier()))
                .build(),
            executor,
            mockCallback
        )
        // THEN a connection is made to Clockwork Home's Auth service
        assertThat(fakeServiceBinder.state).isEqualTo(ConnectionState.CONNECTING)
    }

    @Test
    public fun sendAuthorizationRequestShouldCallBinderMethod() {
        // WHEN an authorization request is sent
        clientUnderTest.sendAuthorizationRequest(requestA, executor, mockCallback)
        fakeServiceBinder.completeConnection()
        // THEN a request is made to Clockwork Home
        val request = fakeService.requests[0]
        val requestReceived = request.first
        // THEN the request url is set correctly
        assertThat(requestReceived.requestUrl).isEqualTo(requestA.requestUrl)
        assertThat(requestReceived.requestUrl.toString().indexOf(authProviderUrlA)).isEqualTo(0)
    }

    @Test
    public fun twoQueuedAuthorizationRequestsBeforeConnectCompletes() {
        // GIVEN two authorization requests were made before connecting to Clockwork Home completes
        clientUnderTest.sendAuthorizationRequest(requestA, executor, mockCallback)
        clientUnderTest.sendAuthorizationRequest(requestB, executor, mockCallback)
        // WHEN the connection does complete
        fakeServiceBinder.completeConnection()
        // THEN two requests are made to Clockwork Home
        val requestAReceived = fakeService.requests[0].first
        val requestBReceived = fakeService.requests[1].first
        assertThat(fakeService.requests.size.toLong()).isEqualTo(2)
        // THEN the request url is set correctly for both (A then B)
        assertThat(requestAReceived.requestUrl).isEqualTo(requestA.requestUrl)
        assertThat(requestBReceived.requestUrl).isEqualTo(requestB.requestUrl)
        assertThat(requestAReceived.requestUrl.toString().indexOf(authProviderUrlA)).isEqualTo(0)
        assertThat(requestBReceived.requestUrl.toString().indexOf(authProviderUrlB)).isEqualTo(0)
    }

    @Test
    @Throws(RemoteException::class)
    public fun requestCompletionShouldCallBackToClient() {
        // GIVEN an authorization request was sent
        clientUnderTest.sendAuthorizationRequest(requestA, executor, mockCallback)
        fakeServiceBinder.completeConnection()
        val request = fakeService.requests[0]
        // WHEN the request completes
        // callback supplied earlier is called with the correct request URL and response URL
        Mockito.verify(mockCallback).onAuthorizationResponse(request.first, response)
    }

    @Test
    @Throws(RemoteException::class)
    public fun doesntDisconnectWhenRequestStillInProgress() {
        // GIVEN 2 authorization requests were sent
        clientUnderTest.sendAuthorizationRequest(requestA, executor, mockCallback)
        // GIVEN the async binding to Clockwork Home completed after the 1st but before the 2nd
        fakeServiceBinder.completeConnection()
        clientUnderTest.sendAuthorizationRequest(requestB, executor, mockCallback)
        // WHEN the first one completes
        RemoteAuthService.sendResponseToCallback(
            response,
            fakeService.requests[0].second
        )
        // THEN the service remains connected (as there's still a request ongoing, and we won't get
        // the callback for the other request if we unbind now)
        assertThat(fakeServiceBinder.state).isEqualTo(ConnectionState.CONNECTED)
    }

    @Test
    @Throws(RemoteException::class)
    public fun disconnectsWhenAllRequestsComplete() {
        // GIVEN 2 authorization requests were sent
        clientUnderTest.sendAuthorizationRequest(requestA, executor, mockCallback)
        // GIVEN the async binding to Clockwork Home completed after the 1st but before the 2nd
        fakeServiceBinder.completeConnection()
        clientUnderTest.sendAuthorizationRequest(requestB, executor, mockCallback)
        RemoteAuthService.sendResponseToCallback(
            response,
            fakeService.requests[0].second
        )
        // WHEN the other completes
        RemoteAuthService.sendResponseToCallback(
            response,
            fakeService.requests[1].second
        )
        // THEN the OAuth library disconnects from Clockwork Home
        assertThat(fakeServiceBinder.state).isEqualTo(ConnectionState.DISCONNECTED)
    }

    @Test
    fun remoteAuthClientStatus_notSupported_unknown() {
        whenever(remoteInteractionsManager.isAvailabilityStatusApiSupported).thenReturn(false)
        val isAvailable = runBlocking {
            clientUnderTest.availabilityStatus.first()
        }

        assertThat(isAvailable).isEqualTo(RemoteAuthClient.STATUS_UNKNOWN)
        verify(remoteInteractionsManager, never())
            .registerRemoteAuthClientStatusListener(any(), any())
    }

    @Test
    fun remoteAuthClientStatus_isSupported_propagateListenerValues() {
        for (remoteStatus in listOf(
            RemoteAuthClient.STATUS_AVAILABLE,
            RemoteAuthClient.STATUS_UNAVAILABLE,
            RemoteAuthClient.STATUS_TEMPORARILY_UNAVAILABLE)) {
        whenever(remoteInteractionsManager.isAvailabilityStatusApiSupported).thenReturn(true)
            doAnswer {
                    @Suppress("UNCHECKED_CAST")
                    val consumer: Consumer<Int> = it.arguments[1] as (Consumer<Int>)
                    consumer.accept(remoteStatus)
                }
                .whenever(remoteInteractionsManager)
                .registerRemoteAuthClientStatusListener(any(), any())

            val isAvailable = runBlocking {
                clientUnderTest.availabilityStatus.first()
            }

            assertThat(isAvailable).isEqualTo(remoteStatus)
            verify(remoteInteractionsManager).registerRemoteAuthClientStatusListener(any(), any())
            verify(remoteInteractionsManager).unregisterRemoteAuthClientStatusListener(any())
            reset(remoteInteractionsManager)
        }
    }

    internal enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    /** Fakes binding to Clockwork Home.  */
    private inner class FakeServiceBinder : RemoteAuthClient.ServiceBinder {
        var state = ConnectionState.DISCONNECTED
        private var serviceConnection: ServiceConnection? = null
        override fun bindService(
            intent: Intent,
            connection: ServiceConnection,
            flags: Int
        ): Boolean {
            if (intent.getPackage() != RemoteAuthClient.WEARABLE_PACKAGE_NAME) {
                throw UnsupportedOperationException()
            }
            if (intent.action != RemoteAuthClient.ACTION_AUTH) {
                throw UnsupportedOperationException()
            }
            check(state == ConnectionState.DISCONNECTED) { "Already connected or connecting" }
            state = ConnectionState.CONNECTING
            serviceConnection = connection
            return true
        }

        fun completeConnection() {
            assertThat(ConnectionState.CONNECTING).isEqualTo(state)
            state = ConnectionState.CONNECTED
            serviceConnection!!.onServiceConnected(mServiceName, fakeService.onBind(Intent()))
        }

        override fun unbindService(connection: ServiceConnection?) {
            check(state != ConnectionState.DISCONNECTED) { "Not connected; can't disconnect" }
            state = ConnectionState.DISCONNECTED
            serviceConnection = null
        }
    }

    /**
     * Fake implementation of the OAuth service in Clockwork Home. Instead of talking to the user's
     * phone, this class just records the method calls that were made so we can handle them manually
     * in our tests.
     */
    private inner class FakeClockworkHomeAuthService : RemoteAuthService() {
        private val requestHandler: RemoteAuthRequestHandler

        val requests: MutableList<Pair<OAuthRequest, kotlin.Pair<String, Int>>> =
            ArrayList()

        init {
            requestHandler = AuthenticationRequestHandler()
        }

        override fun onBind(intent: Intent): IBinder {
            return onBind(intent, requestHandler)
        }

        override fun verifyPackageName(context: Context, requestPackageName: String?): Boolean {
            return true
        }

        private inner class AuthenticationRequestHandler : RemoteAuthRequestHandler {
            override fun isAuthSupported(): Boolean {
                return true
            }

            override fun sendAuthRequest(
                request: OAuthRequest,
                packageNameAndRequestId: kotlin.Pair<String, Int>
            ) {
                if (fakeServiceBinder.state != ConnectionState.CONNECTED) {
                    throw RemoteException("not connected")
                }
                requests.add(Pair.create(request, packageNameAndRequestId))
                mockCallback.onAuthorizationResponse(request, response)
            }
        }
    }
}

private class SyncExecutor : Executor {
    override fun execute(command: Runnable?) {
        command?.run()
    }
}
