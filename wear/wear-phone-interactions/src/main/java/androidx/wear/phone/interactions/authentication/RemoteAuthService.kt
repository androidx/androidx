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
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.wearable.authentication.IAuthenticationRequestCallback
import android.support.wearable.authentication.IAuthenticationRequestService
import androidx.wear.phone.interactions.authentication.RemoteAuthClient.Companion.KEY_ERROR_CODE
import androidx.wear.phone.interactions.authentication.RemoteAuthClient.Companion.KEY_PACKAGE_NAME
import androidx.wear.phone.interactions.authentication.RemoteAuthClient.Companion.KEY_RESPONSE_URL
import java.security.SecureRandom

/**
 * Interface for specifying how the service handles the remote auth requests.
 */
public interface RemoteAuthRequestHandler {

    /**
     * Whether the auth service is enabled, return false would give an early out by sending the
     * 3p app a response with error code of ERROR_UNSUPPORTED
     */
    public fun isAuthSupported(): Boolean

    /**
     * Handle the auth request by sending it to the phone.
     * Typically, if the paired phone is not connected, send a response with error code of
     * ERROR_PHONE_UNAVAILABLE; otherwise listening for the response from the phone and send it
     * back to the 3p app.
     *
     * [RemoteAuthService.sendResponseToCallback] is provided for sending response back to the
     * callback provided by the 3p app.
     *
     */
    public fun sendAuthRequest(
        request: OAuthRequest,
        packageNameAndRequestId: Pair<String, Int>
    )
}

/*
 * Extend this service class to trigger the handling of the remote auth requests, the
 * RemoteAuthRequestHandler is specified when the service is bound, typically:
 *
 *  class AuthenticationService : RemoteAuthService {
 *      override fun onBind(intent: Intent): IBinder {
 *          return onBind(
 *              intent,
 *              object : RemoteAuthRequestHandler {
 *                  override fun isAuthSupported(): Boolean {...}
 *                  override fun sendAuthRequest(...) {
 *                    ...
 *                    sendResponseToCallback(...)
 *                  }
 *          })
 *      }
 *  }
 */
public abstract class RemoteAuthService : Service() {

    public companion object {
        @JvmStatic
        private val callbacksByPackageNameAndRequestID:
            MutableMap<Pair<String, Int>, IAuthenticationRequestCallback> = HashMap()

        /**
         * To be called by the child class to invoke the callback with Response
         */
        @SuppressLint("DocumentExceptions")
        @JvmStatic
        public fun sendResponseToCallback(
            response: OAuthResponse,
            packageNameAndRequestId: Pair<String, Int>
        ) {
            try {
                callbacksByPackageNameAndRequestID[packageNameAndRequestId]?.onResult(
                    buildBundleFromResponse(response, packageNameAndRequestId.first)
                )
                callbacksByPackageNameAndRequestID.remove(packageNameAndRequestId)
            } catch (e: RemoteException) {
                throw e.cause!!
            }
        }

        internal fun getCallback(packageNameAndRequestId: Pair<String, Int>):
            IAuthenticationRequestCallback? =
                callbacksByPackageNameAndRequestID[packageNameAndRequestId]

        internal fun buildBundleFromResponse(response: OAuthResponse, packageName: String): Bundle =
            Bundle().apply {
                putParcelable(KEY_RESPONSE_URL, response.getResponseUrl())
                putInt(KEY_ERROR_CODE, response.getErrorCode())
                putString(KEY_PACKAGE_NAME, packageName)
            }
    }

    private val secureRandom: SecureRandom = SecureRandom()

    /**
     * To be called by child class when implementing the [Service.onBind], provide the
     * RemoteAuthRequestHandler and return the IBinder.
     */
    protected fun onBind(
        @Suppress("UNUSED_PARAMETER") intent: Intent,
        remoteAuthRequestHandler: RemoteAuthRequestHandler
    ): IBinder = RemoteAuthServiceBinder(this, remoteAuthRequestHandler)

    /**
     * Implementation of [Service.onUnbind]
     */
    public override fun onUnbind(intent: Intent): Boolean {
        callbacksByPackageNameAndRequestID.clear()
        return super.onUnbind(intent)
    }

    /**
     * Allow the child class to override the default behavior of the package name verification.
     *
     * By default, we check the request's package name belongs to the requester's UID.
     */
    protected open fun verifyPackageName(context: Context, requestPackageName: String?): Boolean {
        val packagesForUID: Array<String>? =
            context.packageManager.getPackagesForUid(Binder.getCallingUid())
        return !(
            requestPackageName.isNullOrEmpty() ||
                packagesForUID.isNullOrEmpty() ||
                !(packagesForUID.contains(requestPackageName))
            )
    }

    internal inner class RemoteAuthServiceBinder(
        private val context: Context,
        private val remoteAuthRequestHandler: RemoteAuthRequestHandler
    ) : IAuthenticationRequestService.Stub() {

        override fun getApiVersion(): Int = IAuthenticationRequestService.API_VERSION

        /**
         * @throws SecurityException
         */
        override fun openUrl(
            request: Bundle,
            authenticationRequestCallback: IAuthenticationRequestCallback
        ) {
            val packageName = request.getString(RemoteAuthClient.KEY_PACKAGE_NAME)
            if (remoteAuthRequestHandler.isAuthSupported()) {
                if (!verifyPackageName(context, packageName)) {
                    throw SecurityException("Failed to verify the Requester's package name")
                }

                val packageNameAndRequestId = Pair(packageName!!, secureRandom.nextInt())
                callbacksByPackageNameAndRequestID[packageNameAndRequestId] =
                    authenticationRequestCallback

                val requestUrl: Uri? = request.getParcelable(RemoteAuthClient.KEY_REQUEST_URL)
                remoteAuthRequestHandler.sendAuthRequest(
                    OAuthRequest(packageName, requestUrl!!),
                    packageNameAndRequestId
                )
            } else {
                authenticationRequestCallback.onResult(
                    Bundle().apply { putInt(KEY_ERROR_CODE, RemoteAuthClient.ERROR_UNSUPPORTED) }
                )
            }
        }
    }
}