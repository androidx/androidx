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

package androidx.credentials.providerevents.transfer

import android.graphics.Bitmap

/**
 * Each export entry corresponds to an entry in the provider selector UI that the user can choose
 * from.
 *
 * ExportEntry serves two purposes:
 * 1. Contain display data to be shown on the selector UI.
 * 2. Contain fields that can be used to match an incoming [ImportCredentialsRequest]. By default,
 *    the `supportedCredentialTypes` field will be used to match against the request.
 *
 * @param id the secret id that is used to identify the export entry. This should be randomly
 *   generated and stored. When the provider's credential transfer activity gets launched, the
 *   provider should verify that the [ProviderImportCredentialsRequest] contains the pre-registered
 *   id.
 * @param accountDisplayName the account display name of the entry
 * @param userDisplayName the user display name of the entry
 * @param icon the icon to display for this entry; this icon should be 32x32 and if not will be
 *   rescaled into a 32x32 pixel PGN for display
 * @param supportedCredentialTypes the credential types that this entry supports. By default, this
 *   field will be used to filter whether the entry will be displayed for an incoming import
 *   request. The values include, but not limited to, the constants in [CredentialTypes]
 * @throws IllegalArgumentException if [supportedCredentialTypes] is empty
 */
public class ExportEntry(
    public val id: String,
    public val accountDisplayName: CharSequence?,
    public val userDisplayName: CharSequence,
    public val icon: Bitmap,
    public val supportedCredentialTypes: Set<String>,
) {
    init {
        require(supportedCredentialTypes.isNotEmpty()) {
            "supportedCredentialTypes must not be empty"
        }
    }
}
