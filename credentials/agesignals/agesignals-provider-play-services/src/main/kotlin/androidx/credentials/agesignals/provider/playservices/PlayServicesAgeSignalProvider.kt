/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals.provider.playservices

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.AgeSignalProvider
import androidx.credentials.agesignals.AgeSignalResultHandler
import androidx.credentials.agesignals.GetAgeRangeRequest
import androidx.credentials.agesignals.GetAgeRangeResponse
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.credentials.agesignals.exceptions.GetAgeRangeInterruptedException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnavailableException
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.android.gms.identitycredentials.GetAgeRangeRequest as GmsGetAgeRangeRequest
import com.google.android.gms.identitycredentials.IdentityCredentialClient
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import java.util.concurrent.Executor

/** Entry point of all age signals requests to the Google Play Services module. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PlayServicesAgeSignalProvider(context: Context) : AgeSignalProvider {

    private val appContext: Context = context.applicationContext ?: context

    @VisibleForTesting
    internal var googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()

    @VisibleForTesting internal var credentialClient: IdentityCredentialClient? = null

    override fun onGetAgeRange(
        context: Context,
        request: GetAgeRangeRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        if (cancellationReviewer(cancellationSignal)) {
            return
        }
        if (!isAvailable()) {
            dispatchError(
                GetAgeRangeProviderConfigurationException(
                    "The minimum required version of Google Play Services is not available on this device."
                ),
                cancellationSignal,
                executor,
                callback,
            )
            return
        }

        val client =
            try {
                getClient(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get IdentityCredentialClient", e)
                dispatchError(fromGmsException(e), cancellationSignal, executor, callback)
                return
            }

        val gmsRequest = GmsGetAgeRangeRequest()
        client
            .getAgeRange(gmsRequest)
            .addOnSuccessListener { handle ->
                if (cancellationReviewer(cancellationSignal)) {
                    return@addOnSuccessListener
                }
                val resultData = handle?.resultData
                val pendingIntent = handle?.pendingIntent
                when {
                    resultData != null && pendingIntent == null -> {
                        handleSilentResult(
                            resultData = resultData,
                            cancellationSignal = cancellationSignal,
                            executor = executor,
                            callback = callback,
                        )
                    }
                    pendingIntent != null && resultData == null -> {
                        // TODO: Launch interactive consent UI flow.
                        dispatchError(
                            GetAgeRangeUnknownException(
                                "Interactive age signal flow is not yet implemented."
                            ),
                            cancellationSignal,
                            executor,
                            callback,
                        )
                    }
                    else -> {
                        dispatchError(
                            GetAgeRangeUnknownException(
                                "Invalid handle from Google Play Services: expected " +
                                    "exactly one of resultData or pendingIntent."
                            ),
                            cancellationSignal,
                            executor,
                            callback,
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                dispatchError(fromGmsException(e), cancellationSignal, executor, callback)
            }
            .addOnCanceledListener {
                dispatchError(
                    GetAgeRangeInterruptedException("The age signal request was canceled."),
                    cancellationSignal,
                    executor,
                    callback,
                )
            }
    }

    private fun getClient(context: Context): IdentityCredentialClient {
        return credentialClient ?: IdentityCredentialManager.getClient(context)
    }

    @VisibleForTesting
    internal fun handleSilentResult(
        resultData: Bundle,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        handleResultBundle(resultData, cancellationSignal, executor, callback) {
            GetAgeRangeUnknownException("Failed to parse silent age range response.")
        }
    }

    private fun handleResultBundle(
        bundle: Bundle?,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
        fallbackException: () -> GetAgeRangeException,
    ) {
        if (cancellationReviewer(cancellationSignal)) {
            return
        }
        if (bundle != null) {
            val response = AgeSignalResultHandler.retrieveGetAgeRangeResponse(bundle)
            if (response != null) {
                dispatchResult(response, cancellationSignal, executor, callback)
                return
            }
            val exception = AgeSignalResultHandler.retrieveGetAgeRangeException(bundle)
            if (exception != null) {
                dispatchError(exception, cancellationSignal, executor, callback)
                return
            }
        }
        dispatchError(fallbackException(), cancellationSignal, executor, callback)
    }

    private fun dispatchResult(
        response: GetAgeRangeResponse,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        cancelOrCallbackExceptionOrResult(cancellationSignal) {
            executor.execute {
                if (cancellationReviewer(cancellationSignal)) {
                    return@execute
                }
                callback.onResult(response)
            }
        }
    }

    private fun dispatchError(
        exception: GetAgeRangeException,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        cancelOrCallbackExceptionOrResult(cancellationSignal) {
            executor.execute {
                if (cancellationReviewer(cancellationSignal)) {
                    return@execute
                }
                callback.onError(exception)
            }
        }
    }

    @VisibleForTesting
    internal fun fromGmsException(e: Throwable): GetAgeRangeException {
        when (e) {
            is ApiException -> {
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        return GetAgeRangeUnavailableException(e.message)
                    }
                    CommonStatusCodes.API_NOT_CONNECTED -> {
                        return GetAgeRangeUnavailableException("API is not available: ${e.message}")
                    }
                    in retryables -> {
                        return GetAgeRangeInterruptedException(e.message)
                    }
                    else -> {
                        return GetAgeRangeUnknownException(
                            "Google Play Services age signal request failed with " +
                                "code: ${e.statusCode}, message: ${e.message}"
                        )
                    }
                }
            }
            is UnsupportedApiCallException -> {
                return GetAgeRangeUnavailableException("API is unsupported: ${e.message}")
            }
            else -> {
                return GetAgeRangeUnknownException(
                    "Google Play Services age signal request failed: ${e.message}"
                )
            }
        }
    }

    override fun isAvailable(): Boolean {
        return isAvailableOnDevice(MIN_GMS_APK_VERSION)
    }

    // https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
    // There is one error code that supports retry API_DISABLED_FOR_CONNECTION but it would not
    // be useful to retry that one because our connection to GMSCore is a static variable
    // (see GoogleApiAvailability.getInstance()) so we cannot recreate the connection to retry.
    private fun isGooglePlayServicesAvailable(context: Context, minApkVersion: Int): Int {
        return googleApiAvailability.isGooglePlayServicesAvailable(
            context,
            /*minApkVersion=*/ minApkVersion,
        )
    }

    private fun isAvailableOnDevice(minApkVersion: Int): Boolean {
        val resultCode = isGooglePlayServicesAvailable(appContext, minApkVersion)
        val isSuccessful = resultCode == ConnectionResult.SUCCESS
        if (!isSuccessful) {
            val connectionResult = ConnectionResult(resultCode)
            Log.w(
                TAG,
                "Connection with Google Play Services was not successful. " +
                    "Connection result is: $connectionResult",
            )
        }
        return isSuccessful
    }

    internal companion object {
        private const val TAG = "GmsAgeSignalProvider"

        @VisibleForTesting internal const val MIN_GMS_APK_VERSION = 264400000

        private val retryables: Set<Int> =
            setOf(
                CommonStatusCodes.NETWORK_ERROR,
                CommonStatusCodes.CONNECTION_SUSPENDED_DURING_CALL,
                CommonStatusCodes.INTERRUPTED,
                CommonStatusCodes.TIMEOUT,
            )

        internal fun cancellationReviewer(cancellationSignal: CancellationSignal?): Boolean {
            if (cancellationSignal != null && cancellationSignal.isCanceled) {
                Log.i(TAG, "The age signal flow has been canceled")
                return true
            }
            return false
        }

        internal fun cancelOrCallbackExceptionOrResult(
            cancellationSignal: CancellationSignal?,
            onResultOrException: () -> Unit,
        ) {
            if (cancellationReviewer(cancellationSignal)) {
                return
            }
            onResultOrException()
        }
    }
}
