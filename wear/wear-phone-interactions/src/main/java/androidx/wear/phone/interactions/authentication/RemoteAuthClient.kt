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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.wearable.authentication.IAuthenticationRequestCallback
import android.support.wearable.authentication.IAuthenticationRequestService
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executor

/**
 * Provides a client for supporting remote authentication on Wear. The authentication session
 * will be opened on the user's paired phone.
 *
 * * The following example triggers an authorization session to open on the phone.
 * ```
 * // PKCE (Proof Key for Code Exchange) is required for the auth
 * private var codeVerifier: CodeVerifier
 * private var authClient: RemoteAuthClient
 *
 * override public fun onCreate(b: Bundle) {
 *   super.onCreate(b);
 *   authClient = RemoteAuthClient.create(this);
 *   ...
 * }
 *
 * override public fun onDestroy() {
 *   authClient.close();
 *   super.onDestroy();
 * }
 *
 * public fun startAuthFlow() {
 *    // PKCE (Proof Key for Code Exchange) is required, store this code verifier here .
 *    // To access the resource later, both the auth token ans code verifier are needed.
 *    codeVerifier = CodeVerifier()
 *
 *   // Construct your auth request.
 *   authClient.sendAuthorizationRequest(
 *      OAuthRequest.Builder(this.applicationContext.packageName)
 *          .setAuthProviderUrl(Uri.parse("https://...."))
 *          .setCodeChallenge(CodeChallenge(codeVerifier))
 *          .build(),
 *      new MyAuthCallback()
 *   );
 * }
 *
 * private  class MyAuthCallback: RemoteAuthClient.Callback {
 *   override public fun onAuthorizationResponse(
 *      request: OAuthRequest,
 *      response: OAuthResponse
 *    ) {
 *     // Parse the result token out of the response and store it, e.g. in SharedPreferences,
 *     // so you can use it later (Note, use together with code verifier from version R)
 *     // You'll also want to display a success UI.
 *     ...
 *   }
 *
 *   override public fun onAuthorizationError(errorCode: int) {
 *     // Compare against codes available in RemoteAuthClient.ErrorCode
 *     // You'll also want to display an error UI.
 *     ...
 *   }
 * }
 * ```
 */
public class RemoteAuthClient internal constructor(
    private val serviceBinder: ServiceBinder,
    private val uiThreadExecutor: Executor,
    private val packageName: String
) : AutoCloseable {
    public companion object {
        /**
         * The URL to be opened in a web browser on the companion.
         * Value type: Uri
         */
        internal const val KEY_REQUEST_URL: String = "requestUrl"

        /**
         * The package name obtained from calling getPackageName() on the context passed into
         * [create].
         * Value type: String
         */
        internal const val KEY_PACKAGE_NAME: String = "packageName"

        /**
         * The URL that the web browser is directed to that triggered the companion to open.
         * Value type: Uri
         */
        internal const val KEY_RESPONSE_URL: String = "responseUrl"

        /**
         * The error code explaining why the request failed.
         * Value type: [ErrorCode]
         */
        internal const val KEY_ERROR_CODE: String = "errorCode"

        /**
         * Package name for the service provider on Wearable.
         * Home app for Wear 2, and Wear Core Service for wear 3
         */
        internal const val WEARABLE_PACKAGE_NAME: String = "com.google.android.wearable.app"

        /**
         * Triggering a service that will prompt a user for authorization credential on the phone
         * For backwards compatibility, leave this action name as "OAUTH", so 3p app using this new
         * androidx class can still send request to the service in clockwork home with WSL.
         */
        internal const val ACTION_AUTH: String =
            "android.support.wearable.authentication.action.OAUTH"

        /** Indicates 3p authentication is finished without error  */
        public const val NO_ERROR: Int = -1

        /** Indicates 3p authentication isn't supported by Wear OS  */
        public const val ERROR_UNSUPPORTED: Int = 0

        /** Indicates no phone is connected, or the phone connected doesn't support 3p auth */
        public const val ERROR_PHONE_UNAVAILABLE: Int = 1

        /** Errors returned in [Callback.onAuthorizationError].  */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(NO_ERROR, ERROR_UNSUPPORTED, ERROR_PHONE_UNAVAILABLE)
        internal annotation class ErrorCode

        /** service connection status */
        private const val STATE_DISCONNECTED: Int = 0
        private const val STATE_CONNECTING: Int = 1
        private const val STATE_CONNECTED: Int = 2

        /** Return a client that can be used to make async remote authorization requests */
        @JvmStatic
        public fun create(context: Context): RemoteAuthClient {
            val appContext: Context = context.applicationContext
            return RemoteAuthClient(
                object : ServiceBinder {
                    override fun bindService(
                        intent: Intent?,
                        connection: ServiceConnection?,
                        flags: Int
                    ): Boolean {
                        return appContext.bindService(intent, connection!!, flags)
                    }

                    override fun unbindService(connection: ServiceConnection?) {
                        appContext.unbindService(connection!!)
                    }
                },
                { command -> Handler(appContext.mainLooper).post(command) },
                context.packageName
            )
        }
    }

    private var allocationSite: Throwable? =
        Throwable("Explicit termination method 'close' not called")
    private var connectionState: Int = STATE_DISCONNECTED
    private var service: IAuthenticationRequestService? = null
    private val outstandingRequests: MutableSet<RequestCallback> = HashSet()
    private val queuedRunnables: Queue<Runnable> = ArrayDeque()
    private val connection: ServiceConnection = RemoteAuthConnection()

    /**
     * This callback is notified when an async remote authentication request completes.
     *
     * Typically, your app should update its UI to let the user aware of the success or failure.
     */
    public abstract class Callback {

        /**
         * Called when an async remote authentication request completes successfully.
         *
         * see [sendAuthorizationRequest]
         */
        @UiThread
        public abstract fun onAuthorizationResponse(request: OAuthRequest, response: OAuthResponse)

        /**
         * Called when an async remote authentication request fails.
         *
         * see [sendAuthorizationRequest]
         */
        @UiThread
        public abstract fun onAuthorizationError(@ErrorCode errorCode: Int)
    }

    /**
     * Send a remote auth request. This will cause an authorization UI to be presented on
     * the user's phone.
     * This request is asynchronous; the callback provided will be be notified when the request
     * completes.
     *
     * @param request Request that will be sent to the phone. The auth response should redirect
     * to the Wear OS companion. See [WEAR_REDIRECT_URL_PREFIX]
     *
     * @Throws RuntimeException if the service has error to open the request
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun sendAuthorizationRequest(request: OAuthRequest, clientCallback: Callback) {
        require(packageName == request.getPackageName()) {
            "The request's package name is different from the auth client's package name."
        }

        if (connectionState == STATE_DISCONNECTED) {
            connect()
        }
        whenConnected(
            Runnable {
                val callback = RequestCallback(request, clientCallback)
                outstandingRequests.add(callback)
                try {
                    service!!.openUrl(request.toBundle(), callback)
                } catch (e: Exception) {
                    removePendingCallback(callback)
                    throw RuntimeException(e)
                }
            }
        )
    }

    /**
     * Check that the explicit termination method 'close' is called
     *
     *  @Throws RuntimeException if the 'close' method was not called
     */
    protected fun finalize() {
        if (allocationSite != null) {
            throw RuntimeException(
                "A RemoteAuthClient was acquired at the attached stack trace but never released" +
                    " Call RemoteAuthClient.close()"
            )
        }
    }

    /**
     * Frees any resources used by the client, dropping any outstanding requests. The client
     * cannot be used to make requests thereafter.
     */
    @UiThread
    override fun close() {
        allocationSite = null
        queuedRunnables.clear()
        outstandingRequests.clear()
        disconnect()
    }

    internal interface ServiceBinder {
        /** See [Context.bindService].  */
        fun bindService(intent: Intent?, connection: ServiceConnection?, flags: Int): Boolean

        /** See [Context.unbindService].  */
        fun unbindService(connection: ServiceConnection?)
    }

    /**
     * Runs the given runnable immediately if already connected, or queues it for later if a
     * connection has not yet been fully established.
     */
    private fun whenConnected(runnable: Runnable) {
        if (connectionState == STATE_CONNECTED) {
            runnable.run()
        } else {
            queuedRunnables.add(runnable)
        }
    }

    private fun removePendingCallback(requestCallback: RequestCallback) {
        outstandingRequests.remove(requestCallback)
        if (outstandingRequests.isEmpty() && service != null) {
            disconnect()
        }
    }

    private fun connect() {
        check(connectionState == STATE_DISCONNECTED) { "State is $connectionState" }
        val intent =
            Intent(ACTION_AUTH).setPackage(WEARABLE_PACKAGE_NAME)
        val success: Boolean =
            serviceBinder.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        if (success) {
            connectionState = STATE_CONNECTING
        } else {
            throw RuntimeException("Failed to bind to Auth service")
        }
    }

    private fun disconnect() {
        if (connectionState != STATE_DISCONNECTED) {
            serviceBinder.unbindService(connection)
            service = null
            connectionState = STATE_DISCONNECTED
        }
    }

    /** Receives results of async requests to the remote auth service.  */
    internal inner class RequestCallback internal constructor(
        private val request: OAuthRequest,
        private val clientCallback: Callback
    ) : IAuthenticationRequestCallback.Stub() {

        override fun getApiVersion(): Int = IAuthenticationRequestCallback.API_VERSION

        /**
         * Called when an aync remote authentication request is completed.
         *
         * Bundle contents:
         * <ul><li>"responseUrl": the response URL from the Auth request (Uri)
         * <ul><li>"error": an error code explaining why the request failed (int)
         */
        override fun onResult(result: Bundle) {
            val errorCode = result.getInt(KEY_ERROR_CODE, NO_ERROR)
            val responseUrl: Uri? = result.getParcelable(KEY_RESPONSE_URL)
            onResult(OAuthResponse(errorCode, responseUrl))
        }

        @SuppressLint("SyntheticAccessor")
        private fun onResult(response: OAuthResponse) {
            @ErrorCode val error = response.getErrorCode()
            uiThreadExecutor.execute(
                Runnable {
                    removePendingCallback(this@RequestCallback)
                    if (error == NO_ERROR) {
                        clientCallback.onAuthorizationResponse(request, response)
                    } else {
                        clientCallback.onAuthorizationError(response.getErrorCode())
                    }
                }
            )
        }
    }

    /** Manages the connection with Wearable Auth service.  */
    private inner class RemoteAuthConnection : ServiceConnection {
        @UiThread
        override fun onServiceConnected(name: ComponentName, boundService: IBinder) {
            service = IAuthenticationRequestService.Stub.asInterface(boundService)
            connectionState = STATE_CONNECTED
            // Run all queued runnables
            while (!queuedRunnables.isEmpty()) {
                queuedRunnables.poll()!!.run()
            }
        }

        @UiThread
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }
}