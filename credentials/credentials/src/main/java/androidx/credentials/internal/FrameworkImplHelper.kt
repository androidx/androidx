/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.internal

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.Credential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse

@RequiresApi(34)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class FrameworkImplHelper {
    companion object {
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        fun convertGetResponseToJetpackClass(
            response: android.credentials.GetCredentialResponse
        ): GetCredentialResponse {
            val credential = response.credential
            return GetCredentialResponse(Credential.createFrom(credential.type, credential.data))
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun convertGetRequestToFrameworkClass(
            request: GetCredentialRequest
        ): android.credentials.GetCredentialRequest {
            val builder =
                android.credentials.GetCredentialRequest.Builder(
                    GetCredentialRequest.toRequestDataBundle(request)
                )
            request.credentialOptions.forEach {
                builder.addCredentialOption(
                    android.credentials.CredentialOption.Builder(
                            it.type,
                            it.requestData,
                            it.candidateQueryData
                        )
                        .setIsSystemProviderRequired(it.isSystemProviderRequired)
                        .setAllowedProviders(it.allowedProviders)
                        .build()
                )
            }
            setOriginForGetRequest(request, builder)
            return builder.build()
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @SuppressLint("MissingPermission")
        @VisibleForTesting
        @JvmStatic
        fun setOriginForGetRequest(
            request: GetCredentialRequest,
            builder: android.credentials.GetCredentialRequest.Builder
        ) {
            if (request.origin != null) {
                builder.setOrigin(request.origin)
            }
        }
    }
}
