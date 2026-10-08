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

package androidx.credentials.providerevents

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.core.content.IntentCompat
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.internal.UriUtils.Companion.readFromUri
import androidx.credentials.providerevents.internal.UriUtils.Companion.writeToUri
import androidx.credentials.providerevents.service.BatchCreateCredentialResponse
import androidx.credentials.providerevents.service.CreateCredentialResult
import androidx.credentials.providerevents.service.ProviderBatchCreateCredentialRequest
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse

/**
 * IntentHandler to be used by credential providers to extract requests from a given intent, or to
 * set back a response or an exception to a given intent while dealing with activities invoked by
 * intents from the credential import flow or the batch credential creation fulfillment flow.
 *
 * For the credential import flow, the Provider Selector UI Activity will display a list of
 * [ExportEntry] and create a launch intent that corresponds to the provider's activity. More info
 * on how the intent is constructed for the provider activity can be found in the documentation of
 * [ProviderEventsManager.registerExport]. When the user selects one of the [ExportEntry], the
 * credential provider's corresponding activity is invoked.
 *
 * For the batch credential creation flow, when the user selects a
 * [androidx.credentials.provider.CreateEntry] returned from
 * [androidx.credentials.providerevents.service.CredentialProviderEventsService.onBatchCreateCredentialCandidatesRequest],
 * the credential provider's fulfillment activity is invoked with a
 * [ProviderBatchCreateCredentialRequest] that can be extracted via
 * [retrieveProviderBatchCreateCredentialRequest].
 *
 * When user interaction is complete, credential providers must set the activity result by calling
 * [android.app.Activity.setResult] with [Activity.RESULT_OK] and a result [Intent] populated using
 * the response or exception setter methods in this class.
 */
public class IntentHandler {
    public companion object {
        private const val EXTRA_REQUEST_JSON =
            "androidx.credentials.providerevents.extra.IMPORT_CREDENTIALS_REQUEST_JSON"
        private const val EXTRA_PACKAGE_NAME =
            "androidx.credentials.providerevents.extra.CALLING_PACKAGE_NAME"
        private const val EXTRA_SIGNING_INFO =
            "androidx.credentials.providerevents.extra.SIGNING_INFO"
        private const val EXTRA_CRED_ID = "androidx.credentials.providerevents.extra.CREDENTIAL_ID"
        private const val EXTRA_IMPORT_CREDENTIALS_EXCEPTION =
            "androidx.credentials.providerevents.extra.EXTRA_IMPORT_CREDENTIALS_EXCEPTION"
        private const val EXTRA_SIGNATURE_COUNT =
            "androidx.credentials.providerevents.extra.SIGNATURE_COUNT"
        private const val EXTRA_SIGNATURE_PREFIX =
            "androidx.credentials.providerevents.extra.SIGNATURE_"
        private const val EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS =
            "androidx.credentials.providerevents.extra.BATCH_CREATE_CREDENTIAL_REQUESTS"
        private const val EXTRA_BATCH_CREATE_CREDENTIAL_RESULTS =
            "androidx.credentials.providerevents.extra.BATCH_CREATE_CREDENTIAL_RESULTS"
        private const val EXTRA_BATCH_CREATE_CREDENTIAL_EXCEPTION =
            "androidx.credentials.providerevents.extra.BATCH_CREATE_CREDENTIAL_EXCEPTION"
        private const val EXTRA_RESULT_PACKAGE_NAME =
            "androidx.credentials.providerevents.extra.RESULT_PACKAGE_NAME"
        private const val EXTRA_RESULT_RESPONSE_TYPE =
            "androidx.credentials.providerevents.extra.RESULT_RESPONSE_TYPE"
        private const val EXTRA_RESULT_RESPONSE_DATA =
            "androidx.credentials.providerevents.extra.RESULT_RESPONSE_DATA"
        private const val EXTRA_RESULT_EXCEPTION =
            "androidx.credentials.providerevents.extra.RESULT_EXCEPTION"
        private const val TAG = "IntentHandler"

        /**
         * Extracts the [ProviderImportCredentialsRequest] from the [Intent] that started the
         * provider's exporting [Activity].
         *
         * This should be called in your activity's `onCreate` method to retrieve the details of the
         * import request, including the calling app's information and the [Uri] for writing the
         * response back.
         *
         * @param intent the `Intent` received by the provider's exporting `Activity`.
         * @return the parsed [ProviderImportCredentialsRequest], or `null` if the intent is missing
         *   required data.
         */
        @Suppress("RestrictedApiAndroidX")
        @JvmStatic
        public fun retrieveProviderImportCredentialsRequest(
            intent: Intent
        ): ProviderImportCredentialsRequest? {
            val extras = intent.extras ?: return null
            val reqJson = extras.getString(EXTRA_REQUEST_JSON) ?: return null
            val credId = intent.getStringExtra(EXTRA_CRED_ID)
            if (credId.isNullOrEmpty()) {
                return null
            }
            val uri = intent.data ?: return null
            val callingAppInfo = extractCallingAppInfo(intent, extras) ?: return null
            val importCredentialsRequest =
                ImportCredentialsRequest.createFrom(reqJson) ?: return null
            return ProviderImportCredentialsRequest(
                importCredentialsRequest,
                callingAppInfo,
                uri,
                credId,
            )
        }

        @Suppress("RestrictedApiAndroidX")
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun retrieveProviderImportCredentialsResponse(
            context: Context,
            intent: Intent,
            uri: Uri,
        ): ProviderImportCredentialsResponse? {
            val extras = intent.extras ?: return null
            val callingAppInfo = extractCallingAppInfo(intent, extras) ?: return null
            val credentialsJson = readFromUri(uri, context)
            return ProviderImportCredentialsResponse(
                ImportCredentialsResponse(credentialsJson),
                callingAppInfo,
            )
        }

        /**
         * Writes the successful [ImportCredentialsResponse] to the content `Uri` provided by the
         * importing framework. The 'responseJson' of the successful [ImportCredentialsResponse]
         * will be written to the content 'Uri' to bypass the binder transaction limit. For any
         * additional parameters of the [ImportCredentialsResponse] will be written to the intent
         * that is passed in. This intent and [Activity.RESULT_OK] should be set as the result of
         * the activity that was invoked for credential transfer.
         *
         * @param context the context
         * @param uri the uri that was provided by the importer
         * @param intent the intent to be set on the result of the [Activity]
         * @param response the response to be passed to the importer
         */
        @JvmStatic
        public fun setImportCredentialsResponse(
            context: Context,
            uri: Uri,
            intent: Intent,
            response: ImportCredentialsResponse,
        ) {
            writeToUri(uri, response.responseJson, context)
        }

        /**
         * Sets the [androidx.credentials.providerevents.exception.ImportCredentialsException] if an
         * error is encountered when the provider application is invoked to fulfill the credential
         * import request.
         *
         * <p><b>Note:</b> After populating the intent with an exception, the provider must still
         * use [Activity.RESULT_OK] when calling [Activity.setResult]. The system will inspect the
         * `Intent` data to determine if an error occurred and return the exception back to the
         * caller. If both a valid response and an exception are found, then the exception will be
         * returned to the caller.
         *
         * @param intent the result `Intent` to which the exception will be added.
         * @param exception the exception to be returned to the importer.
         */
        @JvmStatic
        public fun setImportCredentialsException(
            intent: Intent,
            exception: ImportCredentialsException,
        ) {
            intent.putExtra(
                EXTRA_IMPORT_CREDENTIALS_EXCEPTION,
                ImportCredentialsException.toBundle(exception),
            )
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun retrieveImportCredentialsException(intent: Intent): ImportCredentialsException? {
            return ImportCredentialsException.fromBundle(
                intent.getBundleExtra(EXTRA_IMPORT_CREDENTIALS_EXCEPTION) ?: return null
            )
        }

        /**
         * Extracts the [ProviderBatchCreateCredentialRequest] from the [Intent] that started the
         * provider's batch fulfillment [Activity].
         *
         * The fulfillment activity is launched only through the [android.app.PendingIntent] of a
         * [androidx.credentials.provider.CreateEntry] the provider returned from
         * [androidx.credentials.providerevents.service.CredentialProviderEventsService.onBatchCreateCredentialCandidatesRequest].
         * Once the user confirms the batch, the system sends that `PendingIntent` with a fill-in
         * [Intent] carrying the batch.
         *
         * This method never throws. If the extras are missing or malformed it returns `null`; the
         * activity should then fail the batch with [setBatchCreateCredentialException] and finish
         * with [Activity.RESULT_OK].
         *
         * @param intent the `Intent` received by the provider's batch fulfillment `Activity`
         * @return the batch, or `null` if it is missing or malformed
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun retrieveProviderBatchCreateCredentialRequest(
            intent: Intent
        ): ProviderBatchCreateCredentialRequest? =
            try {
                val bundles =
                    IntentCompat.getParcelableArrayListExtra(
                        intent,
                        EXTRA_BATCH_CREATE_CREDENTIAL_REQUESTS,
                        Bundle::class.java,
                    )
                if (bundles.isNullOrEmpty()) {
                    null
                } else {
                    ProviderBatchCreateCredentialRequest(
                        bundles.map { ProviderCreateCredentialRequest.fromBundle(it) }
                    )
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to retrieve the batch credential creation request", e)
                null
            }

        /**
         * Sets the per-relying-party results of a batch on the result [Intent] of the provider's
         * batch fulfillment [Activity], clearing any exception set by
         * [setBatchCreateCredentialException].
         *
         * Return exactly one [CreateCredentialResult] per request in the
         * [ProviderBatchCreateCredentialRequest]. The system ignores results for package names it
         * did not request, and reports each request without a result to its relying party as a
         * failure. Finish with [Activity.RESULT_OK].
         *
         * @param intent the result `Intent` to be passed to [Activity.setResult]
         * @param response the per-relying-party results
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setBatchCreateCredentialResponse(
            intent: Intent,
            response: BatchCreateCredentialResponse,
        ) {
            intent.removeExtra(EXTRA_BATCH_CREATE_CREDENTIAL_EXCEPTION)
            intent.putParcelableArrayListExtra(
                EXTRA_BATCH_CREATE_CREDENTIAL_RESULTS,
                ArrayList(response.results.map { resultToBundle(it) }),
            )
        }

        /**
         * Fails the whole batch on the result [Intent] of the provider's batch fulfillment
         * [Activity], clearing any response set by [setBatchCreateCredentialResponse].
         *
         * Still finish with [Activity.RESULT_OK]. Use [Activity.RESULT_CANCELED] only when the user
         * backs out; the system then returns to its consent screen and no relying party is
         * notified.
         *
         * @param intent the result `Intent` to be passed to [Activity.setResult]
         * @param exception why the batch failed
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun setBatchCreateCredentialException(
            intent: Intent,
            exception: CreateCredentialException,
        ) {
            intent.removeExtra(EXTRA_BATCH_CREATE_CREDENTIAL_RESULTS)
            intent.putExtra(
                EXTRA_BATCH_CREATE_CREDENTIAL_EXCEPTION,
                CreateCredentialException.asBundle(exception),
            )
        }

        /**
         * Retrieves the [BatchCreateCredentialResponse] set by [setBatchCreateCredentialResponse],
         * or `null` if none was set or it is malformed. Never throws.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun retrieveBatchCreateCredentialResponse(
            intent: Intent
        ): BatchCreateCredentialResponse? =
            try {
                IntentCompat.getParcelableArrayListExtra(
                        intent,
                        EXTRA_BATCH_CREATE_CREDENTIAL_RESULTS,
                        Bundle::class.java,
                    )
                    ?.let { bundles ->
                        BatchCreateCredentialResponse(bundles.map { resultFromBundle(it) })
                    }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to retrieve the batch credential creation response", e)
                null
            }

        /**
         * Retrieves the [CreateCredentialException] set by [setBatchCreateCredentialException], or
         * `null` if none was set or it is malformed. Never throws.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun retrieveBatchCreateCredentialException(
            intent: Intent
        ): CreateCredentialException? =
            try {
                intent.getBundleExtra(EXTRA_BATCH_CREATE_CREDENTIAL_EXCEPTION)?.let {
                    CreateCredentialException.fromBundle(it)
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Failed to retrieve the batch credential creation exception", e)
                null
            }

        private fun resultToBundle(result: CreateCredentialResult): Bundle =
            Bundle().apply {
                putString(EXTRA_RESULT_PACKAGE_NAME, result.packageName)
                result.response?.let {
                    putString(EXTRA_RESULT_RESPONSE_TYPE, it.type)
                    putBundle(EXTRA_RESULT_RESPONSE_DATA, it.data)
                }
                result.exception?.let {
                    putBundle(EXTRA_RESULT_EXCEPTION, CreateCredentialException.asBundle(it))
                }
            }

        /** Throws [IllegalArgumentException] if [bundle] is not a result from [resultToBundle]. */
        private fun resultFromBundle(bundle: Bundle): CreateCredentialResult {
            val packageName =
                requireNotNull(bundle.getString(EXTRA_RESULT_PACKAGE_NAME)) {
                    "Batch result is missing its package name"
                }
            bundle.getBundle(EXTRA_RESULT_EXCEPTION)?.let {
                return CreateCredentialResult(
                    packageName,
                    CreateCredentialException.fromBundle(it),
                )
            }
            val type =
                requireNotNull(bundle.getString(EXTRA_RESULT_RESPONSE_TYPE)) {
                    "Batch result for $packageName has neither a response nor an exception"
                }
            val data =
                requireNotNull(bundle.getBundle(EXTRA_RESULT_RESPONSE_DATA)) {
                    "Batch result for $packageName is missing its response data"
                }
            return CreateCredentialResult(
                packageName,
                CreateCredentialResponse.createFrom(type, data),
            )
        }

        @Suppress("RestrictedApiAndroidX")
        private fun extractCallingAppInfo(intent: Intent, extras: Bundle): CallingAppInfo? {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val signingInfo: SigningInfo? = extras.getParcelable(EXTRA_SIGNING_INFO)
                if (signingInfo == null) {
                    return null
                }
                return CallingAppInfo.create(packageName, signingInfo, null)
            }
            val signatureCount = intent.getIntExtra(EXTRA_SIGNATURE_COUNT, /* defaultValue= */ 0)
            if (signatureCount == 0) {
                return null
            }
            val signatures = mutableListOf<Signature>()
            for (i in 0 until signatureCount) {
                val signature = intent.getByteArrayExtra("${EXTRA_SIGNATURE_PREFIX}$i")
                if (signature == null) {
                    // cannot find expected signature at count i
                    return null
                }
                signatures.add(Signature(signature))
            }
            return CallingAppInfo.create(packageName, signatures, null)
        }
    }
}
