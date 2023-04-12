/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.playservices.controllers

import android.app.Activity
import android.os.Bundle
import android.os.CancellationSignal
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import java.util.concurrent.Executor

/**
 * Extensible abstract class for credential controllers. Please implement this class per every
 * request/response credential type. Unique logic is left to the use case of the implementation.
 * If you are building your own version as an OEM, the below can be mimicked to your own
 * credential provider equivalent and whatever internal service you invoke.
 *
 * @param T1 the credential request type from credential manager
 * @param T2 the credential request type converted to play services
 * @param R2 the credential response type from play services
 * @param R1 the credential response type converted back to that used by credential manager
 * @param E1 the credential error type to throw
 *
 * @hide
 */
@Suppress("deprecation")
abstract class CredentialProviderController<T1 : Any, T2 : Any, R2 : Any, R1 : Any,
    E1 : Any>(private val activity: Activity) : CredentialProviderBaseController(activity) {

    companion object {

        /**
         * This handles result code exception reporting across all create flows.
         *
         * @return a boolean indicating if the create flow contains a result code exception
         */
        @JvmStatic
        protected fun maybeReportErrorResultCodeCreate(
            resultCode: Int,
            type: String,
            cancelOnError: (
                CancellationSignal?,
                    () -> Unit
            ) -> Unit,
            onError: (CreateCredentialException) -> Unit,
            cancellationSignal: CancellationSignal?
        ): Boolean {
            if (resultCode != Activity.RESULT_OK) {
                var exception: CreateCredentialException = CreateCredentialUnknownException(
                    generateErrorStringUnknown(type, resultCode)
                )
                if (resultCode == Activity.RESULT_CANCELED) {
                    exception = CreateCredentialCancellationException(
                        generateErrorStringCanceled(type)
                    )
                }
                cancelOnError(cancellationSignal) { onError(exception) }
                return true
            }
            return false
        }

        internal fun generateErrorStringUnknown(type: String, resultCode: Int): String {
            return "$type activity with result code: $resultCode indicating not RESULT_OK"
        }

        internal fun generateErrorStringCanceled(type: String): String {
            return "$type activity is cancelled by the user."
        }

        /**
         * This allows catching result code errors from the get flow if they exist.
         *
         * @return a boolean indicating if the get flow had an error
         */
        @JvmStatic
        protected fun maybeReportErrorResultCodeGet(
            resultCode: Int,
            type: String,
            cancelOnError: (
                CancellationSignal?,
                    () -> Unit
            ) -> Unit,
            onError: (GetCredentialException) -> Unit,
            cancellationSignal: CancellationSignal?
        ): Boolean {
            if (resultCode != Activity.RESULT_OK) {
                var exception: GetCredentialException = GetCredentialUnknownException(
                    generateErrorStringUnknown(type, resultCode)
                )
                if (resultCode == Activity.RESULT_CANCELED) {
                    exception = GetCredentialCancellationException(
                        generateErrorStringCanceled(type)
                    )
                }
                cancelOnError(cancellationSignal) { onError(exception) }
                return true
            }
            return false
        }

        /**
         * This will check for cancellation, and will otherwise set a result to the callback, or an
         * exception.
         */
        @JvmStatic
        protected fun cancelOrCallbackExceptionOrResult(
            cancellationSignal: CancellationSignal?,
            onResultOrException: () -> Unit
        ) {
            if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
                return
            }
            onResultOrException()
        }
    }

    /**
     * To avoid redundant logic across all controllers for exceptions parceled back from the
     * hidden activity, this can be generally implemented.
     *
     * @return a boolean indicating if an error was reported or not by the result receiver
     */
    protected fun maybeReportErrorFromResultReceiver(
        resultData: Bundle,
        conversionFn: (String?, String?) -> E1,
        executor: Executor,
        callback: CredentialManagerCallback<R1, E1>,
        cancellationSignal: CancellationSignal?
    ): Boolean {
        val isError = resultData.getBoolean(FAILURE_RESPONSE_TAG)
        if (!isError) {
            return false
        }
        val errType = resultData.getString(EXCEPTION_TYPE_TAG)
        val errMsg = resultData.getString(EXCEPTION_MESSAGE_TAG)
        val exception = conversionFn(errType, errMsg)
        cancelOrCallbackExceptionOrResult(cancellationSignal = cancellationSignal,
            onResultOrException = {
            executor.execute { callback.onError(exception) }
        })
        return true
    }

    /**
     * Invokes the flow that starts retrieving credential data. In this use case, we invoke
     * play service modules.
     *
     * @param request a credential provider request
     * @param callback a credential manager callback with a credential provider response
     * @param executor to be used in any multi-threaded operation calls, such as listenable futures
     */
    abstract fun invokePlayServices(
        request: T1,
        callback: CredentialManagerCallback<R1, E1>,
        executor: Executor,
        cancellationSignal: CancellationSignal?
    )

    /**
     * Allows converting from a credential provider request to a play service request.
     *
     * @param request a credential provider request
     * @return a play service request
     */
    protected abstract fun convertRequestToPlayServices(request: T1): T2

    /**
     * Allows converting from a play service response to a credential provider response.
     *
     * @param response a play service response
     * @return a credential provider response
     */
    protected abstract fun convertResponseToCredentialManager(response: R2): R1
}