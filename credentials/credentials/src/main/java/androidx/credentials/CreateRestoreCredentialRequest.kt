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

package androidx.credentials

import android.os.Bundle
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import org.json.JSONObject

/**
 * A request to create a restore credential used for app restore purpose.
 * [App restore feature](https://www.android.com/transfer-data-android-to-android/) is a feature
 * that allows users to copy an app and its data to a new Android device.
 *
 * If the [isCloudBackupEnabled] is true, the restore credential service will periodically backup
 * the restore credential to cloud. In order to use this setting, the user device must enable backup
 * and have end-to-end-encryption enabled, such as screen lock. More about cloud backup can be found
 * [here](https://developer.android.com/identity/data/backup). Cloud backup is supported on Android
 * 2.2 and above. If the cloud backup is not enabled, catch the [E2eeUnavailableException] and retry
 * without cloud backup.
 *
 * @param requestJson the request in JSON format in the
 *   [standard webauthn web json](https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson).
 * @param isCloudBackupEnabled whether the credential should be backed up to cloud.
 * @throws E2eeUnavailableException if [isCloudBackupEnabled] was requested but the user device did
 *   not enable backup or e2ee (screen lock).
 * @throws CreateRestoreCredentialDomException if the requestJson is an invalid Json that does not
 *   follow the standard webauthn web json format
 * @throws IllegalArgumentException If [requestJson] is empty, or if it is not a valid JSON, or if
 *   it doesn't have a valid `user.id` defined according to the [webauthn spec]
 *   (https://w3c.github.io/webauthn/#dictdef-publickeycredentialcreationoptionsjson)
 */
class CreateRestoreCredentialRequest
@JvmOverloads
constructor(
    val requestJson: String,
    val isCloudBackupEnabled: Boolean = true,
) :
    CreateCredentialRequest(
        type = RestoreCredential.TYPE_RESTORE_CREDENTIAL,
        credentialData = toCredentialDataBundle(requestJson, isCloudBackupEnabled),
        isSystemProviderRequired = false,
        displayInfo = getDisplayInfoFromJson(requestJson),
        origin = null,
        preferImmediatelyAvailableCredentials = false,
        isAutoSelectAllowed = false,
        candidateQueryData = Bundle()
    ) {
    companion object {
        private const val BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_REQUEST =
            "androidx.credentials.BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_REQUEST"
        private const val BUNDLE_KEY_SHOULD_BACKUP_TO_CLOUD =
            "androidx.credentials.BUNDLE_KEY_SHOULD_BACKUP_TO_CLOUD"

        private fun getDisplayInfoFromJson(requestJson: String): DisplayInfo {
            try {
                val json = JSONObject(requestJson)
                val userJson = json.getJSONObject("user")
                return DisplayInfo(userJson.getString("id"))
            } catch (e: Exception) {
                throw IllegalArgumentException("user.id must be defined in requestJson")
            }
        }

        private fun toCredentialDataBundle(
            requestJson: String,
            isCloudBackupEnabled: Boolean
        ): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_CREATE_RESTORE_CREDENTIAL_REQUEST, requestJson)
            bundle.putBoolean(BUNDLE_KEY_SHOULD_BACKUP_TO_CLOUD, isCloudBackupEnabled)
            return bundle
        }
    }
}
