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
package androidx.credentials.provider

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.service.credentials.BeginCreateCredentialResponse
import android.service.credentials.CreateCredentialRequest
import android.service.credentials.CredentialEntry
import android.service.credentials.CredentialProviderService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.utils.BeginGetCredentialUtil
import java.util.stream.Collectors

/**
 * PendingIntentHandler to be used by credential providers to extract requests from a given intent,
 * or to set back a response or an exception to a given intent while dealing with activities invoked
 * by pending intents set on a [CreateEntry] for the create flow, or on a [CredentialEntry],
 * [AuthenticationAction], [Action], or a [RemoteEntry] set for a get flow.
 *
 * When user selects one of the entries mentioned above, the credential provider's corresponding
 * activity is invoked. The intent associated with this activity must be extracted and passed into
 * the utils in this class to extract the required requests.
 *
 * When user interaction is complete, credential providers must set the activity result by calling
 * [android.app.Activity.setResult] by setting an appropriate result code and data of type [Intent].
 * This data should also be prepared by using the utils in this class to populate the required
 * response/exception.
 *
 * See extension functions for [Intent] in IntentHandlerConverters.kt to help test intents that are
 * set on pending intents in different entry classes.
 */
@RequiresApi(34)
class PendingIntentHandler {
    companion object {
        private const val TAG = "PendingIntentHandler"

        /**
         * Extracts the [ProviderCreateCredentialRequest] from the provider's [PendingIntent]
         * invoked by the Android system.
         *
         * @param intent the intent associated with the [Activity] invoked through the
         *   [PendingIntent]
         * @throws NullPointerException If [intent] is null
         */
        @JvmStatic
        fun retrieveProviderCreateCredentialRequest(
            intent: Intent
        ): ProviderCreateCredentialRequest? {
            val frameworkReq: CreateCredentialRequest? =
                intent.getParcelableExtra(
                    CredentialProviderService.EXTRA_CREATE_CREDENTIAL_REQUEST,
                    CreateCredentialRequest::class.java
                )
            if (frameworkReq == null) {
                Log.i(TAG, "Request not found in pendingIntent")
                return frameworkReq
            }
            var biometricPromptResult = retrieveBiometricPromptResult(intent)
            if (biometricPromptResult == null) {
                biometricPromptResult = retrieveBiometricPromptResultFallback(intent)
            }
            return try {
                ProviderCreateCredentialRequest(
                    callingRequest =
                        androidx.credentials.CreateCredentialRequest.createFrom(
                            frameworkReq.type,
                            frameworkReq.data,
                            frameworkReq.data,
                            requireSystemProvider = false,
                            frameworkReq.callingAppInfo.origin
                        ),
                    callingAppInfo =
                        CallingAppInfo.create(
                            frameworkReq.callingAppInfo.packageName,
                            frameworkReq.callingAppInfo.signingInfo,
                            frameworkReq.callingAppInfo.origin
                        ),
                    biometricPromptResult = biometricPromptResult
                )
            } catch (e: IllegalArgumentException) {
                return null
            }
        }

        private fun retrieveBiometricPromptResult(
            intent: Intent,
            resultKey: String? = AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE,
            errorKey: String? = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR,
            errorMessageKey: String? = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE
        ): BiometricPromptResult? {
            if (intent.extras == null) {
                return null
            }
            if (intent.extras!!.containsKey(resultKey)) {
                val authResultType = intent.extras!!.getInt(resultKey)
                return BiometricPromptResult(
                    authenticationResult = AuthenticationResult(authResultType)
                )
            } else if (intent.extras!!.containsKey(errorKey)) {
                val authResultError = intent.extras!!.getInt(errorKey)
                return BiometricPromptResult(
                    authenticationError =
                        AuthenticationError(
                            authResultError,
                            intent.extras?.getCharSequence(errorMessageKey)
                        )
                )
            }
            return null
        }

        private fun retrieveBiometricPromptResultFallback(intent: Intent): BiometricPromptResult? {
            // TODO(b/353798766) : Remove fallback keys once beta users have finalized testing
            val fallbackResultKey = AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE_FALLBACK
            val fallbackErrorKey = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_FALLBACK
            if (
                intent.extras != null &&
                    (intent.extras!!.containsKey(fallbackResultKey) ||
                        intent.extras!!.containsKey(fallbackErrorKey))
            ) {
                return retrieveBiometricPromptResult(
                    intent,
                    resultKey = AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE_FALLBACK,
                    errorKey = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_FALLBACK,
                    errorMessageKey =
                        AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE_FALLBACK
                )
            }
            return null
        }

        /**
         * Extracts the [BeginGetCredentialRequest] from the provider's [PendingIntent] invoked by
         * the Android system when the user selects an [AuthenticationAction].
         *
         * @param intent the intent associated with the [Activity] invoked through the
         *   [PendingIntent]
         * @throws NullPointerException If [intent] is null
         */
        @JvmStatic
        fun retrieveBeginGetCredentialRequest(intent: Intent): BeginGetCredentialRequest? {
            val request =
                intent.getParcelableExtra(
                    "android.service.credentials.extra.BEGIN_GET_CREDENTIAL_REQUEST",
                    android.service.credentials.BeginGetCredentialRequest::class.java
                )
            return request?.let { BeginGetCredentialUtil.convertToJetpackRequest(it) }
        }

        /**
         * Sets the [CreateCredentialResponse] on the intent passed in. This intent is then set as
         * the data associated with the result of the activity invoked by the [PendingIntent] set on
         * a [CreateEntry]. The intent is set using the [Activity.setResult] method that takes in
         * the intent, as well as a result code.
         *
         * A credential provider must set the result code to [Activity.RESULT_OK] if a valid
         * response, or a valid exception is being set as the data to the result. However, if the
         * credential provider is unable to resolve to a valid response or exception, the result
         * code must be set to [Activity.RESULT_CANCELED]. Note that setting the result code to
         * [Activity.RESULT_CANCELED] will re-surface the account selection bottom sheet that
         * displayed the original [CredentialEntry], hence allowing the user to re-select.
         *
         * @param intent the intent to be set on the result of the [Activity] invoked through the
         *   [PendingIntent]
         * @param response the response to be set as an extra on the [intent]
         * @throws NullPointerException If [intent], or [response] is null
         */
        @JvmStatic
        fun setCreateCredentialResponse(intent: Intent, response: CreateCredentialResponse) {
            intent.putExtra(
                CredentialProviderService.EXTRA_CREATE_CREDENTIAL_RESPONSE,
                android.credentials.CreateCredentialResponse(response.data)
            )
        }

        /**
         * Extracts the [ProviderGetCredentialRequest] from the provider's [PendingIntent] invoked
         * by the Android system, when the user selects a [CredentialEntry].
         *
         * @param intent the intent associated with the [Activity] invoked through the
         *   [PendingIntent]
         * @throws NullPointerException If [intent] is null
         */
        @JvmStatic
        fun retrieveProviderGetCredentialRequest(intent: Intent): ProviderGetCredentialRequest? {
            val frameworkReq =
                intent.getParcelableExtra(
                    CredentialProviderService.EXTRA_GET_CREDENTIAL_REQUEST,
                    android.service.credentials.GetCredentialRequest::class.java
                )
            if (frameworkReq == null) {
                Log.i(TAG, "Get request from framework is null")
                return null
            }
            var biometricPromptResult = retrieveBiometricPromptResult(intent)
            if (biometricPromptResult == null) {
                biometricPromptResult = retrieveBiometricPromptResultFallback(intent)
            }
            return ProviderGetCredentialRequest.createFrom(
                frameworkReq.credentialOptions
                    .stream()
                    .map { option ->
                        CredentialOption.createFrom(
                            option.type,
                            option.credentialRetrievalData,
                            option.candidateQueryData,
                            option.isSystemProviderRequired,
                            option.allowedProviders,
                        )
                    }
                    .collect(Collectors.toList()),
                CallingAppInfo.create(
                    frameworkReq.callingAppInfo.packageName,
                    frameworkReq.callingAppInfo.signingInfo,
                    frameworkReq.callingAppInfo.origin
                ),
                biometricPromptResult
            )
        }

        /**
         * Sets the [android.credentials.GetCredentialResponse] on the intent passed in. This intent
         * is then set as the data associated with the result of the activity invoked by the
         * [PendingIntent], set on a [CredentialEntry]. The intent is set using the
         * [Activity.setResult] method that takes in the intent, as well as a result code.
         *
         * A credential provider must set the result code to [Activity.RESULT_OK] if a valid
         * credential, or a valid exception is being set as the data to the result. However, if the
         * credential provider is unable to resolve to a valid response or exception, the result
         * code must be set to [Activity.RESULT_CANCELED]. Note that setting the result code to
         * [Activity.RESULT_CANCELED] will re-surface the account selection bottom sheet that
         * displayed the original [CredentialEntry], hence allowing the user to re-select.
         *
         * @param intent the intent to be set on the result of the [Activity] invoked through the
         *   [PendingIntent]
         * @param response the response to be set as an extra on the [intent]
         * @throws NullPointerException If [intent], or [response] is null
         */
        @JvmStatic
        fun setGetCredentialResponse(intent: Intent, response: GetCredentialResponse) {
            intent.putExtra(
                CredentialProviderService.EXTRA_GET_CREDENTIAL_RESPONSE,
                android.credentials.GetCredentialResponse(
                    android.credentials.Credential(
                        response.credential.type,
                        response.credential.data
                    )
                )
            )
        }

        /**
         * Sets the [android.service.credentials.BeginGetCredentialResponse] on the intent passed
         * in. This intent is then set as the data associated with the result of the activity
         * invoked by the [PendingIntent], set on an [AuthenticationAction]. The intent is set using
         * the [Activity.setResult] method that takes in the intent, as well as a result code.
         *
         * A credential provider must set the result code to [Activity.RESULT_OK] if a valid
         * response, or a valid exception is being set as part of the data to the result. However,
         * if the credential provider is unable to resolve to a valid response or exception, the
         * result code must be set to [Activity.RESULT_CANCELED]. Note that setting the result code
         * to [Activity.RESULT_CANCELED] will re-surface the account selection bottom sheet that
         * displayed the original [CredentialEntry], hence allowing the user to re-select.
         *
         * @param intent the intent to be set on the result of the [Activity] invoked through the
         *   [PendingIntent]
         * @param response the response to be set as an extra on the [intent]
         * @throws NullPointerException If [intent], or [response] is null
         */
        @JvmStatic
        fun setBeginGetCredentialResponse(intent: Intent, response: BeginGetCredentialResponse) {
            intent.putExtra(
                CredentialProviderService.EXTRA_BEGIN_GET_CREDENTIAL_RESPONSE,
                BeginGetCredentialUtil.convertToFrameworkResponse(response)
            )
        }

        /**
         * Sets the [androidx.credentials.exceptions.GetCredentialException] if an error is
         * encountered during the final phase of the get credential flow.
         *
         * A credential provider service returns a list of [CredentialEntry] as part of the
         * [BeginGetCredentialResponse] to the query phase of the get-credential flow. If the user
         * selects one of these entries, the corresponding [PendingIntent] is fired and the
         * provider's activity is invoked. If there is an error encountered during the lifetime of
         * that activity, the provider must use this API to set an exception on the given intent
         * before finishing the activity in question.
         *
         * The intent is set using the [Activity.setResult] method that takes in the intent, as well
         * as a result code. A credential provider must set the result code to [Activity.RESULT_OK]
         * if a valid credential, or a valid exception is being set as the data to the result.
         * However, if the credential provider is unable to resolve to a valid response or
         * exception, the result code must be set to [Activity.RESULT_CANCELED].
         *
         * Note that setting the result code to [Activity.RESULT_CANCELED] will re-surface the
         * account selection bottom sheet that displayed the original [CredentialEntry], hence
         * allowing the user to re-select.
         *
         * @param intent the intent to be set on the result of the [Activity] invoked through the
         *   [PendingIntent]
         * @param exception the exception to be set as an extra to the [intent]
         * @throws NullPointerException If [intent], or [exception] is null
         */
        @JvmStatic
        fun setGetCredentialException(intent: Intent, exception: GetCredentialException) {
            intent.putExtra(
                CredentialProviderService.EXTRA_GET_CREDENTIAL_EXCEPTION,
                android.credentials.GetCredentialException(exception.type, exception.message)
            )
        }

        /**
         * Sets the [androidx.credentials.exceptions.CreateCredentialException] if an error is
         * encountered during the final phase of the create credential flow.
         *
         * A credential provider service returns a list of [CreateEntry] as part of the
         * [BeginCreateCredentialResponse] to the query phase of the get-credential flow.
         *
         * If the user selects one of these entries, the corresponding [PendingIntent] is fired and
         * the provider's activity is invoked. If there is an error encountered during the lifetime
         * of that activity, the provider must use this API to set an exception before finishing the
         * activity.
         *
         * The intent is set using the [Activity.setResult] method that takes in the intent, as well
         * as a result code. A credential provider must set the result code to [Activity.RESULT_OK]
         * if a valid credential, or a valid exception is being set as the data to the result.
         * However, if the credential provider is unable to resolve to a valid response or
         * exception, the result code must be set to [Activity.RESULT_CANCELED].
         *
         * Note that setting the result code to [Activity.RESULT_CANCELED] will re-surface the
         * account selection bottom sheet that displayed the original [CreateEntry], hence allowing
         * the user to re-select.
         *
         * @param intent the intent to be set on the result of the [Activity] invoked through the
         *   [PendingIntent]
         * @param exception the exception to be set as an extra to the [intent]
         * @throws NullPointerException If [intent], or [exception] is null
         */
        @JvmStatic
        fun setCreateCredentialException(intent: Intent, exception: CreateCredentialException) {
            intent.putExtra(
                CredentialProviderService.EXTRA_CREATE_CREDENTIAL_EXCEPTION,
                android.credentials.CreateCredentialException(exception.type, exception.message)
            )
        }
    }
}
