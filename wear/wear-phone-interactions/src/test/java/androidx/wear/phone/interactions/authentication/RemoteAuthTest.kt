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
import android.os.IBinder
import android.os.RemoteException
import android.util.Pair
import androidx.wear.phone.interactions.WearPhoneInteractionsTestRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.ArrayList
import java.util.concurrent.Executor

/** Unit tests for [RemoteAuthClient].  */
@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument // Needed because it is defined in the "android" package.
public class RemoteAuthTest {

    internal companion object {
        private val DIRECT_EXECUTOR = Executor { command -> command.run() }
        private const val authProviderUrlA = "http://myrequesturl/a?client_id=iamtheclient"
        private const val authProviderUrlB = "http://myrequesturl/b?client_id=iamtheclient"
        private val responseUrl = Uri.parse("http://myresponseurl")
        private const val appPackageName = "com.friendlyapp"
        private val requestA =
            OAuthRequest.Builder(appPackageName)
                .setAuthProviderUrl(Uri.parse(authProviderUrlA))
                .setCodeChallenge(CodeChallenge(CodeVerifier()))
                .build()
        private val requestB =
            OAuthRequest.Builder(appPackageName)
                .setAuthProviderUrl(Uri.parse(authProviderUrlB))
                .setCodeChallenge(CodeChallenge(CodeVerifier()))
                .build()
        private val response = OAuthResponse.Builder().setResponseUrl(responseUrl).build()

        // Note: This can't be static as Robolectric isn't set up at class init time.
        private val mServiceName = ComponentName(
            "com.google.android.wearable.app", "auth_lib_shouldnt_care_about_this"
        )
        private val mockCallback: RemoteAuthClient.Callback =
            Mockito.mock(RemoteAuthClient.Callback::class.java)
    }

    private var fakeServiceBinder: FakeServiceBinder = FakeServiceBinder()
    private var fakeService: FakeClockworkHomeAuthService = FakeClockworkHomeAuthService()
    private var clientUnderTest: RemoteAuthClient =
        RemoteAuthClient(fakeServiceBinder, DIRECT_EXECUTOR, appPackageName)
    private val executor: Executor = SyncExecutor()

    @Test
    public fun doesntConnectUntilARequestIsMade() {
        // WHEN the client is created
        // THEN the Auth library should not yet connect to Clockwork Home
        Assert.assertEquals(ConnectionState.DISCONNECTED, fakeServiceBinder.state)
    }

    @Test
    public fun sendAuthorizationRequestShouldMakeConnectionToClockworkHome() {
        val requestUri = "http://myrequesturl?client_id=xxx"
        // WHEN an authorization request is sent
        clientUnderTest.sendAuthorizationRequest(
            OAuthRequest.Builder(appPackageName)
                .setAuthProviderUrl(Uri.parse(requestUri))
                .setCodeChallenge(CodeChallenge(CodeVerifier()))
                .build(),
            executor,
            mockCallback
        )
        // THEN a connection is made to Clockwork Home's Auth service
        Assert.assertEquals(ConnectionState.CONNECTING, fakeServiceBinder.state)
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
        Assert.assertEquals(
            requestA.requestUrl,
            requestReceived.requestUrl
        )
        Assert.assertEquals(
            requestReceived.requestUrl.toString().indexOf(authProviderUrlA),
            0
        )
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
        Assert.assertEquals(2, fakeService.requests.size.toLong())
        // THEN the request url is set correctly for both (A then B)
        Assert.assertEquals(
            requestA.requestUrl,
            requestAReceived.requestUrl
        )
        Assert.assertEquals(
            requestB.requestUrl,
            requestBReceived.requestUrl
        )
        Assert.assertEquals(
            requestAReceived.requestUrl.toString().indexOf(authProviderUrlA),
            0
        )
        Assert.assertEquals(
            requestBReceived.requestUrl.toString().indexOf(authProviderUrlB),
            0
        )
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
        Assert.assertEquals(ConnectionState.CONNECTED, fakeServiceBinder.state)
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
        Assert.assertEquals(ConnectionState.DISCONNECTED, fakeServiceBinder.state)
    }

    private enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    /** Fakes binding to Clockwork Home.  */
    private inner class FakeServiceBinder : RemoteAuthClient.ServiceBinder {
        var state = ConnectionState.DISCONNECTED
        private var serviceConnection: ServiceConnection? = null
        override fun bindService(
            intent: Intent?,
            connection: ServiceConnection?,
            flags: Int
        ): Boolean {
            if (intent!!.getPackage() != RemoteAuthClient.WEARABLE_PACKAGE_NAME) {
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
            Assert.assertTrue(state == ConnectionState.CONNECTING)
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
