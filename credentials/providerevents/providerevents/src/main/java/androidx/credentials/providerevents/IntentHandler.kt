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
import androidx.annotation.RestrictTo
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.internal.UriUtils.Companion.readFromUri
import androidx.credentials.providerevents.internal.UriUtils.Companion.writeToUri
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsRequest
import androidx.credentials.providerevents.transfer.ProviderImportCredentialsResponse

/**
 * IntentHandler to be used by credential providers to extract requests from a given intent, or to
 * set back a response or an exception to a given intent while dealing with activities invoked by
 * intents from the import flow.
 *
 * The Provider Selector UI Activity will display a list of [ExportEntry] and create a launch intent
 * that corresponds to the provider's activity. More info on how the intent is constructed for the
 * provider activity can be found in the documentation of [ProviderEventsManager.registerExport].
 *
 * When user selects one of the [ExportEntry], the credential provider's corresponding activity is
 * invoked. The intent associated with this activity must be extracted and passed into the utils in
 * this class to extract the required requests.
 *
 * When user interaction is complete, credential providers must set the activity result by calling
 * [android.app.Activity.setResult] by setting an appropriate result code and data of type [Intent].
 * This data should also be prepared by using the utils in this class to populate the required
 * response/exception.
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
            return ProviderImportCredentialsRequest(
                ImportCredentialsRequest(reqJson),
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
                ImportCredentialsException.asBundle(exception),
            )
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun retrieveImportCredentialsException(intent: Intent): ImportCredentialsException? {
            return ImportCredentialsException.fromBundle(
                intent.getBundleExtra(EXTRA_IMPORT_CREDENTIALS_EXCEPTION) ?: return null
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
