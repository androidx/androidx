/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("ProviderCreateCredentialRequest")

package androidx.credentials.registry.digitalcredentials.openid4vci

import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.registry.provider.selectedEntryMetadata

/**
 * Returns the parsed [OpenId4VciSelectionMetadata] associated with the selected entry.
 *
 * A null return means that metadata isn't supported, wasn't provided, or failed to parse.
 */
public val ProviderCreateCredentialRequest.selectedOpenId4VciMetadata: OpenId4VciSelectionMetadata?
    get() {
        val metadataString = this.selectedEntryMetadata ?: return null
        return try {
            OpenId4VciSelectionMetadata.parse(metadataString)
        } catch (e: Exception) {
            null
        }
    }
