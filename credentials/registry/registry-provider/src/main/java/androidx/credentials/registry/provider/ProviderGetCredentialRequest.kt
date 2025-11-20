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
 *
 * For how to handle a user selection and extract the [ProviderGetCredentialRequest] containing the
 * selection information, see [RegistryManager.ACTION_GET_CREDENTIAL].
 */
@get:JvmName("getSelectedEntryId")
public val ProviderGetCredentialRequest.selectedEntryId: String?
    get() = this.sourceBundle?.getString(EXTRA_CREDENTIAL_ID)

/**
 * Contains information about the user selection result on the Credential Manager UI.
 *
 * During the [androidx.credentials.CredentialManager.getCredential] invocation, the user will be
 * presented a Credential Manager selector & permission UI that displays the available credentials
 * to choose from. The selector may display credential options in two dimensions. First, it may
 * display a set of options; then each credential set can contain a list of credentials that must be
 * all returned at once.
 *
 * For example, the user could be presented options of `{ {cred1, cred2, cred3}, {cred1, cred4},
 * {cred5} }` and may choose to use the {cred1, cred4} credential set. In that case, the selected
 * provider will be invoked and be given a [SelectedCredentialSet] containing the corresponding
 * `setId` and `credentialId`s for each individual credential in the set.
 *
 * @param credentialSetId the id of the selected credential set; the `credentialSetId` is generated
 *   by the provider's matcher (see [RegisterCredentialsRequest.matcher]) and therefore its format
 *   is specific to the matcher
 * @param credentials the non-empty list of credentials in this selected credential set; the
 *   provider must return all credentials to the caller
 * @throws IllegalArgumentException if `credentials` is empty
 */
public class SelectedCredentialSet(
    public val credentialSetId: String,
    public val credentials: List<SelectedCredential>,
) {
    init {
        require(credentials.isNotEmpty()) { "credentials cannot be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedCredentialSet) return false
        return this.credentialSetId == other.credentialSetId &&
            this.credentials == other.credentials
    }

    override fun hashCode(): Int {
        var result = credentialSetId.hashCode()
        result = 31 * result + credentials.hashCode()
        return result
    }
}

/**
 * A credential selected within a [SelectedCredentialSet].
 *
 * @param credentialId the id of the selected credential. For example for a digital credential
 *   entry, this maps to the corresponding entry's [DigitalCredentialEntry.id]
 * @param metadata optional metadata associated with the selected credential; this can be generated
 *   by the provider matcher (see [RegisterCredentialsRequest.matcher])
 */
public class SelectedCredential(public val credentialId: String, public val metadata: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedCredential) return false
        return this.credentialId == other.credentialId && this.metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = credentialId.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        return result
    }
}

private const val EXTRA_CREDENTIAL_SET_ID =
    "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ID"
private const val EXTRA_CREDENTIAL_SET_ELEMENT_LENGTH =
    "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_LENGTH"
private const val EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX =
    "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_ID_"
private const val EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX =
    "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_METADATA_"

/**
 * Returns credential set selected by the user.
 *
 * A null return means that the credential user case isn't registry based. In other words, it means
 * the credentials weren't registered through the [RegistryManager.registerCredentials] API.
 *
 * For how to handle a user selection and extract the [ProviderGetCredentialRequest] containing the
 * selection information, see [RegistryManager.ACTION_GET_CREDENTIAL].
 */
@get:JvmName("getSelectedCredentialSet")
public val ProviderGetCredentialRequest.selectedCredentialSet: SelectedCredentialSet?
    get() =
        this.sourceBundle?.let {
            try {
                val setId = it.getString(EXTRA_CREDENTIAL_SET_ID) ?: return null
                val credentials = mutableListOf<SelectedCredential>()
                val setLength = it.getInt(EXTRA_CREDENTIAL_SET_ELEMENT_LENGTH, 0)
                for (i in 0 until setLength) {
                    val credId =
                        it.getString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}$i") ?: return null
                    val metadata = it.getString("${EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX}$i")
                    credentials.add(SelectedCredential(credId, metadata))
                }
                SelectedCredentialSet(setId, credentials)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
