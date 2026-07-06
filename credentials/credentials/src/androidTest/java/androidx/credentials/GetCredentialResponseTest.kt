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

package androidx.credentials

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@OptIn(ExperimentalDigitalCredentialApi::class)
class GetCredentialResponseTest {

    @Test
    fun constructor_emptyCredentialsList_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) { GetCredentialResponse(emptyList()) }
    }

    @Test
    fun constructor_multipleCredentials_success() {
        val credential1 =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val1\"}}")
        val credential2 =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val2\"}}")
        val response = GetCredentialResponse(listOf(credential1, credential2))

        assertThat(response.credentials).hasSize(2)
        assertThat(response.credentials).containsExactly(credential1, credential2).inOrder()
        assertThat(response.credential).isEqualTo(response.credentials.first())
    }

    @Test
    fun bundleConversion_singleCredential_success() {
        val credential = CustomCredential("type_single", Bundle().apply { putString("k", "v") })
        val response = GetCredentialResponse(credential)

        val bundle = GetCredentialResponse.asBundle(response)
        val unpacked = GetCredentialResponse.fromBundle(bundle)

        assertThat(unpacked).isNotNull()
        assertThat(unpacked!!.credentials).hasSize(1)
        assertThat(unpacked.credential.type).isEqualTo("type_single")
        assertThat(unpacked.credential.data.getString("k")).isEqualTo("v")
    }

    @Test
    fun bundleConversion_multipleCredentials_success() {
        val credential1 =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val1\"}}")
        val credential2 =
            DigitalCredential("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val2\"}}")
        val response = GetCredentialResponse(listOf(credential1, credential2))

        val bundle = GetCredentialResponse.asBundle(response)
        val unpacked = GetCredentialResponse.fromBundle(bundle)

        assertThat(unpacked).isNotNull()
        assertThat(unpacked!!.credentials).hasSize(2)
        val unpackedCred1 = unpacked.credentials[0] as DigitalCredential
        assertThat(unpackedCred1.credentialJson)
            .isEqualTo("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val1\"}}")
        val unpackedCred2 = unpacked.credentials[1] as DigitalCredential
        assertThat(unpackedCred2.credentialJson)
            .isEqualTo("{\"protocol\":\"openid4vp\",\"data\":{\"token\":\"val2\"}}")
        assertThat(unpacked.credential).isEqualTo(unpacked.credentials.first())
    }

    @Test
    fun fromBundle_missingTypeForListElement_returnsNull() {
        val bundle = Bundle()
        bundle.putInt("androidx.credentials.provider.extra.CREDENTIAL_LIST_SIZE", 1)
        bundle.putBundle("androidx.credentials.provider.extra.CREDENTIAL_DATA_0", Bundle())

        assertThat(GetCredentialResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_missingDataForListElement_returnsNull() {
        val bundle = Bundle()
        bundle.putInt("androidx.credentials.provider.extra.CREDENTIAL_LIST_SIZE", 1)
        bundle.putString("androidx.credentials.provider.extra.CREDENTIAL_TYPE_0", "type1")

        assertThat(GetCredentialResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_missingTypeForSingleCredential_returnsNull() {
        val bundle = Bundle()
        bundle.putBundle("androidx.credentials.provider.extra.CREDENTIAL_DATA", Bundle())

        assertThat(GetCredentialResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_missingDataForSingleCredential_returnsNull() {
        val bundle = Bundle()
        bundle.putString("androidx.credentials.provider.extra.CREDENTIAL_TYPE", "type1")

        assertThat(GetCredentialResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_emptyBundle_returnsNull() {
        assertThat(GetCredentialResponse.fromBundle(Bundle())).isNull()
    }
}
