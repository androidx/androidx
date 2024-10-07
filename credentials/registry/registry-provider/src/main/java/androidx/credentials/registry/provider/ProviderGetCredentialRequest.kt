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

@file:JvmName("ProviderGetCredentialRequest")

package androidx.credentials.registry.provider

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialEntry

@RestrictTo(RestrictTo.Scope.LIBRARY)
@VisibleForTesting
public const val EXTRA_CREDENTIAL_ID: String =
    "androidx.credentials.registry.provider.extra.CREDENTIAL_ID"

/**
 * Returns the id of the entry selected by the user. For example, for a digital credential entry,
 * this maps to the corresponding entry's [DigitalCredentialEntry.id].
 *
 * A null return means that entry ID isn't supported for the given type of the use case at all. For
 * example, a [androidx.credentials.provider.PasswordCredentialEntry] does not have an id property
 * and so this getter will return null if the selected entry was a password credential.
 */
@get:JvmName("getSelectedEntryId")
public val ProviderGetCredentialRequest.selectedEntryId: String?
    get() = this.sourceBundle?.getString(EXTRA_CREDENTIAL_ID)
