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

package androidx.credentials.internal

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.R
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialCustomException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialException

/** Take the create request's `credentialData` and add SDK specific values to it. */
@RequiresApi(Build.VERSION_CODES.M)
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun getFinalCreateCredentialData(request: CreateCredentialRequest, context: Context): Bundle {
    val createCredentialData = request.credentialData
    val displayInfoBundle = request.displayInfo.toBundle()
    displayInfoBundle.putParcelable(
        CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_CREDENTIAL_TYPE_ICON,
        Icon.createWithResource(
            context,
            when (request) {
                is CreatePasswordRequest -> R.drawable.adx_ic_password
                is CreatePublicKeyCredentialRequest -> R.drawable.adx_ic_passkey
                else -> R.drawable.adx_ic_other_sign_in
            },
        ),
    )
    createCredentialData.putBundle(
        CreateCredentialRequest.DisplayInfo.BUNDLE_KEY_REQUEST_DISPLAY_INFO,
        displayInfoBundle,
    )
    return createCredentialData
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun toJetpackGetException(errorType: String, errorMsg: CharSequence?): GetCredentialException {

    return when (errorType) {
        android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL ->
            NoCredentialException(errorMsg)
        android.credentials.GetCredentialException.TYPE_USER_CANCELED ->
            GetCredentialCancellationException(errorMsg)
        android.credentials.GetCredentialException.TYPE_INTERRUPTED ->
            GetCredentialInterruptedException(errorMsg)
        android.credentials.GetCredentialException.TYPE_UNKNOWN ->
            GetCredentialUnknownException(errorMsg)
        GetCredentialProviderConfigurationException
            .TYPE_GET_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION ->
            GetCredentialProviderConfigurationException(errorMsg)
        GetCredentialUnsupportedException.TYPE_GET_CREDENTIAL_UNSUPPORTED_EXCEPTION ->
            GetCredentialUnsupportedException(errorMsg)
        else -> {
            if (
                errorType.startsWith(
                    GetPublicKeyCredentialDomException.TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION
                )
            ) {
                GetPublicKeyCredentialException.createFrom(errorType, errorMsg?.toString())
            } else {
                GetCredentialCustomException(errorType, errorMsg)
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun toJetpackCreateException(
    errorType: String,
    errorMsg: CharSequence? = null,
): CreateCredentialException {
    return when (errorType) {
        android.credentials.CreateCredentialException.TYPE_NO_CREATE_OPTIONS ->
            CreateCredentialNoCreateOptionException(errorMsg)
        android.credentials.CreateCredentialException.TYPE_USER_CANCELED ->
            CreateCredentialCancellationException(errorMsg)
        android.credentials.CreateCredentialException.TYPE_INTERRUPTED ->
            CreateCredentialInterruptedException(errorMsg)
        android.credentials.CreateCredentialException.TYPE_UNKNOWN ->
            CreateCredentialUnknownException(errorMsg)
        CreateCredentialProviderConfigurationException
            .TYPE_CREATE_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION ->
            CreateCredentialProviderConfigurationException(errorMsg)
        CreateCredentialUnsupportedException.TYPE_CREATE_CREDENTIAL_UNSUPPORTED_EXCEPTION ->
            CreateCredentialUnsupportedException(errorMsg)
        else -> {
            if (
                errorType.startsWith(
                    CreatePublicKeyCredentialDomException
                        .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION
                )
            ) {
                CreatePublicKeyCredentialException.createFrom(errorType, errorMsg?.toString())
            } else {
                CreateCredentialCustomException(errorType, errorMsg)
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
fun isValidBase64Url(s: String): Boolean {
    return try {
        Base64.decode(s, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}
