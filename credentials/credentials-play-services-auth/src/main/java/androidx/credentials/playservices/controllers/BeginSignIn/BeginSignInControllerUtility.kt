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

package androidx.credentials.playservices.controllers.BeginSignIn

import android.content.Context
import android.content.pm.PackageManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility.Companion.convertToPlayAuthPasskeyJsonRequest
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.PublicKeyCredentialControllerUtility.Companion.convertToPlayAuthPasskeyRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

/**
 * A utility class to handle logic for the begin sign in controller.
 */
@Suppress("deprecation")
internal class BeginSignInControllerUtility {

    companion object {

        private const val TAG = "BeginSignInUtility"
        private const val AUTH_MIN_VERSION_JSON_PARSING: Long = 231815000
        internal fun constructBeginSignInRequest(request: GetCredentialRequest, context: Context):
            BeginSignInRequest {
            var isPublicKeyCredReqFound = false
            val requestBuilder = BeginSignInRequest.Builder()
            var autoSelect = false
            for (option in request.credentialOptions) {
                if (option is GetPasswordOption) {
                    requestBuilder.setPasswordRequestOptions(
                        BeginSignInRequest.PasswordRequestOptions.Builder()
                            .setSupported(true)
                            .build()
                    )
                    autoSelect = autoSelect || option.isAutoSelectAllowed
                } else if (option is GetPublicKeyCredentialOption && !isPublicKeyCredReqFound) {
                    val curAuthVersion = determineDeviceGMSVersionCode(context)
                    if (needsBackwardsCompatibleRequest(curAuthVersion)) {
                        requestBuilder.setPasskeysSignInRequestOptions(
                            convertToPlayAuthPasskeyRequest(option)
                        )
                    } else {
                        requestBuilder.setPasskeyJsonSignInRequestOptions(
                            convertToPlayAuthPasskeyJsonRequest(option)
                        )
                    }
                    isPublicKeyCredReqFound = true
                } else if (option is GetGoogleIdOption) {
                    requestBuilder.setGoogleIdTokenRequestOptions(
                        convertToGoogleIdTokenOption(option)
                    )
                    autoSelect = autoSelect || option.autoSelectEnabled
                }
            }
            return requestBuilder
                .setAutoSelectEnabled(autoSelect)
                .build()
        }

        /**
         * Recovers the current GMS version code *running on the device*. This is needed because
         * even if a dependency knows the methods and functions of a newer code, the device may
         * only contain the older module, which can cause exceptions due to the discrepancy.
         */
        private fun determineDeviceGMSVersionCode(context: Context): Long {
            val packageManager: PackageManager = context.packageManager
            val packageName = GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE
            return packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        }

        /**
         * Determines if curAuthVersion needs the backwards compatible GIS json parsing flow or
         * not. If curAuthVersion >= minVersion for the new flow, this returns false.
         * Otherwise, it's < than the minVersion for the new flow, so this is true.
         */
        private fun needsBackwardsCompatibleRequest(curAuthVersion: Long): Boolean {
            if (curAuthVersion >= AUTH_MIN_VERSION_JSON_PARSING) {
                return false
            }
            return true
        }

        private fun convertToGoogleIdTokenOption(option: GetGoogleIdOption):
            GoogleIdTokenRequestOptions {
            var idTokenOption =
                GoogleIdTokenRequestOptions.builder()
                    .setFilterByAuthorizedAccounts(option.filterByAuthorizedAccounts)
                    .setNonce(option.nonce)
                    .setRequestVerifiedPhoneNumber(option.requestVerifiedPhoneNumber)
                    .setServerClientId(option.serverClientId)
                    .setSupported(true)
            if (option.linkedServiceId != null) {
                idTokenOption.associateLinkedAccounts(
                    option.linkedServiceId!!,
                    option.idTokenDepositionScopes
                )
            }
            return idTokenOption.build()
        }
    }
}
