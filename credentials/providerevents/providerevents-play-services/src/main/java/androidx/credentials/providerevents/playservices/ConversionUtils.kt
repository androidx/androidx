/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.playservices

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderSignalCredentialStateRequest
import androidx.credentials.providerevents.transfer.CredentialTransferCapabilitiesRequest
import androidx.credentials.providerevents.transfer.ExportCredentialsRequest
import androidx.credentials.providerevents.transfer.ImportCredentialsRequest
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupRequest
import com.google.android.gms.identitycredentials.GetCredentialTransferCapabilitiesRequest
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest

/** TODO(b/416798373): add unit test */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ConversionUtils {
    public companion object {
        private const val EXTRA_CREDENTIAL_CALLING_APP_INFO =
            "androidx.credentials.providerevents.extra.CALLING_APP_INFO"
        private const val TAG = "ConversionUtils"

        public fun convertToGmsResponse(
            response: CreateCredentialResponse?
        ): com.google.android.gms.identitycredentials.CreateCredentialResponse? {
            if (response != null) {
                return com.google.android.gms.identitycredentials.CreateCredentialResponse(
                    response.type,
                    response.data,
                )
            }
            return response
        }

        @RequiresApi(Build.VERSION_CODES.P)
        public fun convertToJetpackRequest(
            request: CreateCredentialRequest
        ): ProviderCreateCredentialRequest? {
            val callingAppInfo = constructCallingAppInfo(request)
            if (callingAppInfo == null) {
                Log.e(TAG, "Failed to construct calling app info from request")
                return null
            }
            return ProviderCreateCredentialRequest(
                androidx.credentials.CreateCredentialRequest.createFrom(
                    request.type,
                    request.credentialData,
                    request.candidateQueryData,
                    false,
                ),
                callingAppInfo = callingAppInfo,
            )
        }

        public fun convertToJetpackRequest(
            request: ExportCredentialsToDeviceSetupRequest,
            context: Context,
        ): ExportCredentialsRequest {
            val credentialsJson = UriUtils.readFromUri(request.uri, context)
            return ExportCredentialsRequest(credentialsJson)
        }

        public fun convertToJetpackRequest(
            request: ImportCredentialsForDeviceSetupRequest
        ): ImportCredentialsRequest {
            return ImportCredentialsRequest(request.requestJson)
        }

        public fun convertToJetpackRequest(
            request: GetCredentialTransferCapabilitiesRequest
        ): CredentialTransferCapabilitiesRequest {
            return CredentialTransferCapabilitiesRequest()
        }

        @Suppress("RestrictedApiAndroidX")
        public fun convertToJetpackRequest(
            request: SignalCredentialStateRequest
        ): ProviderSignalCredentialStateRequest? {
            val callingAppInfo = constructCallingAppInfo(request)
            if (callingAppInfo == null) {
                return null
            }
            val signalRequest =
                androidx.credentials.SignalCredentialStateRequest.createFrom(
                    request.type,
                    request.requestData,
                    request.origin,
                )
            return ProviderSignalCredentialStateRequest(signalRequest, callingAppInfo)
        }

        @Suppress("RestrictedApiAndroidX")
        private fun constructCallingAppInfo(request: CreateCredentialRequest): CallingAppInfo? {
            val callingAppInfoBundle =
                request.candidateQueryData.getBundle(EXTRA_CREDENTIAL_CALLING_APP_INFO)
            return callingAppInfoBundle?.let { CallingAppInfo.extractCallingAppInfo(it) }
        }

        @Suppress("RestrictedApiAndroidX")
        private fun constructCallingAppInfo(
            request: SignalCredentialStateRequest
        ): CallingAppInfo? {
            val callingAppInfoBundle =
                request.requestData.getBundle(EXTRA_CREDENTIAL_CALLING_APP_INFO)
            return callingAppInfoBundle?.let { CallingAppInfo.extractCallingAppInfo(it) }
        }
    }
}
