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

import android.app.PendingIntent
import android.service.credentials.GetCredentialsResponse
import androidx.annotation.RequiresApi
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.credentials.provider.GetCredentialsProviderResponse.Companion.createWithAuthentication
import androidx.credentials.provider.GetCredentialsProviderResponse.Companion.createWithCredentialsResponseContent

/**
 * Response for [GetCredentialsProviderRequest] from a credential provider.
 *
 * If the provider is locked and cannot return any credentials, the [createWithAuthentication]
 * method should be used to create the response with an [AuthenticationAction]. When the user
 * selects this [AuthenticationAction], the corresponding [PendingIntent] will be fired that can
 * bring up the provider's unlock activity.
 *
 * If the provider is not locked and can return credential, the
 * [createWithCredentialsResponseContent] method should be used to create the response with
 * a list of [CredentialEntry], and a list of [Action].
 *
 * @hide
 */
@RequiresApi(34)
class GetCredentialsProviderResponse internal constructor(
    val credentialsResponseContent: CredentialsResponseContent?,
    val authenticationAction: AuthenticationAction?
    ) {

    init {
        require(!(credentialsResponseContent == null && authenticationAction == null)) {
            "Both authenticationAction and credentialsDisplayProviderContent must not be null"
        }
    }
    companion object {
        @JvmStatic
        fun createWithAuthentication(
            authenticationAction: AuthenticationAction
        ): GetCredentialsProviderResponse {
            return GetCredentialsProviderResponse(
                /*credentialsResponseContent=*/null, authenticationAction)
        }

        @JvmStatic
        fun createWithCredentialsResponseContent(
            credentialsResponseContent: CredentialsResponseContent
        ): GetCredentialsProviderResponse {
            return GetCredentialsProviderResponse(
                credentialsResponseContent, /*authenticationAction=*/null)
        }

        @JvmStatic
        internal fun toFrameworkClass(response: GetCredentialsProviderResponse):
            GetCredentialsResponse {
            return if (response.credentialsResponseContent != null) {
                GetCredentialsResponse.createWithResponseContent(
                    CredentialsResponseContent.toFrameworkClass(
                        response.credentialsResponseContent)
                )
            } else if (response.authenticationAction != null) {
                GetCredentialsResponse.createWithAuthentication(
                    AuthenticationAction.toFrameworkClass(response.authenticationAction)
                )
            } else {
                throw FrameworkClassParsingException()
            }
        }
    }
}