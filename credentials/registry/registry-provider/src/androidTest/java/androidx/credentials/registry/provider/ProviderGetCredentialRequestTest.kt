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

package androidx.credentials.registry.provider

import androidx.credentials.GetPasswordOption
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ProviderGetCredentialRequestTest {
    @Test
    fun selectedEntryId_success() {
        val request =
            ProviderGetCredentialRequest(listOf(GetPasswordOption()), getTestCallingAppInfo(null))
        val requestBundle = ProviderGetCredentialRequest.asBundle(request)
        requestBundle.putString(EXTRA_CREDENTIAL_ID, "id")

        val actual = ProviderGetCredentialRequest.fromBundle(requestBundle)

        assertThat(actual.selectedEntryId).isEqualTo("id")
    }

    @Test
    fun selectedEntryId_doesNotExist_returnsNull() {
        val request =
            ProviderGetCredentialRequest(listOf(GetPasswordOption()), getTestCallingAppInfo(null))

        assertThat(request.selectedEntryId).isNull()
    }

    @Test
    fun selectedCredentialSet_setContainsSingleCredential_success() {
        val request =
            ProviderGetCredentialRequest(listOf(GetPasswordOption()), getTestCallingAppInfo(null))
        val requestBundle = ProviderGetCredentialRequest.asBundle(request)
        requestBundle.putString(EXTRA_CREDENTIAL_SET_ID, "setId")
        requestBundle.putInt(EXTRA_CREDENTIAL_SET_ELEMENT_LENGTH, 1)
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}0", "credId0")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX}0", "metadata")
        val actual = ProviderGetCredentialRequest.fromBundle(requestBundle)

        val selectedCredentialSet = actual.selectedCredentialSet!!

        assertThat(selectedCredentialSet.credentialSetId).isEqualTo("setId")
        assertThat(selectedCredentialSet.credentials)
            .isEqualTo(listOf(SelectedCredential("credId0", "metadata")))
    }

    @Test
    fun selectedCredentialSet_setContainsMultipleCredentials_success() {
        val request =
            ProviderGetCredentialRequest(listOf(GetPasswordOption()), getTestCallingAppInfo(null))
        val requestBundle = ProviderGetCredentialRequest.asBundle(request)
        requestBundle.putString(EXTRA_CREDENTIAL_SET_ID, "setId")
        requestBundle.putInt(EXTRA_CREDENTIAL_SET_ELEMENT_LENGTH, 3)
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}0", "credId0")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX}0", "metadata0")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}1", "credId1")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}2", "credId2")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX}2", "metadata2")
        val actual = ProviderGetCredentialRequest.fromBundle(requestBundle)

        val selectedCredentialSet = actual.selectedCredentialSet!!

        assertThat(selectedCredentialSet.credentialSetId).isEqualTo("setId")
        assertThat(selectedCredentialSet.credentials)
            .isEqualTo(
                listOf(
                    SelectedCredential("credId0", "metadata0"),
                    SelectedCredential("credId1", null),
                    SelectedCredential("credId2", "metadata2"),
                )
            )
    }

    @Test
    fun selectedCredentialSet_malformedData_success() {
        val request =
            ProviderGetCredentialRequest(listOf(GetPasswordOption()), getTestCallingAppInfo(null))
        val requestBundle = ProviderGetCredentialRequest.asBundle(request)
        requestBundle.putString(EXTRA_CREDENTIAL_SET_ID, "setId")
        requestBundle.putInt(EXTRA_CREDENTIAL_SET_ELEMENT_LENGTH, 3)
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}0", "credId0")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX}0", "metadata0")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX}2", "credId2")
        requestBundle.putString("${EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX}2", "metadata2")

        assertThat(request.selectedCredentialSet).isNull()
    }

    @Test
    fun selectedCredentialSet_doesNotExist_returnsNull() {
        val request =
            ProviderGetCredentialRequest(listOf(GetPasswordOption()), getTestCallingAppInfo(null))

        assertThat(request.selectedCredentialSet).isNull()
    }

    private companion object {
        const val EXTRA_CREDENTIAL_SET_ID =
            "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ID"
        const val EXTRA_CREDENTIAL_SET_ELEMENT_LENGTH =
            "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_LENGTH"
        const val EXTRA_CREDENTIAL_SET_ELEMENT_ID_PREFIX =
            "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_ID_"
        const val EXTRA_CREDENTIAL_SET_ELEMENT_METADATA_PREFIX =
            "androidx.credentials.registry.provider.extra.CREDENTIAL_SET_ELEMENT_METADATA_"
    }
}
