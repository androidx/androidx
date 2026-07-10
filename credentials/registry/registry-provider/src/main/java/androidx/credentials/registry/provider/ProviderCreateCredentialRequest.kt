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

@file:JvmName("ProviderCreateCredentialRequest")

package androidx.credentials.registry.provider

import androidx.credentials.provider.ProviderCreateCredentialRequest

/**
 * Returns the id of the entry selected by the user.
 *
 * A null return means that entry ID isn't supported for the given type of the use case at all.
 *
 * For how to handle a user selection and extract the [ProviderCreateCredentialRequest] containing
 * the selection information, see [RegistryManager.ACTION_CREATE_CREDENTIAL].
 */
@get:JvmName("getSelectedEntryId")
public val ProviderCreateCredentialRequest.selectedEntryId: String?
    get() = this.sourceBundle?.getString(EXTRA_CREDENTIAL_ID)
