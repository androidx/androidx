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

import androidx.credentials.providerevents.internal.CREDENTIAL_TYPES_JSON_KEY
import androidx.credentials.providerevents.internal.KNOWN_EXTENSIONS_JSON_KEY
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ImportCredentialsRequestTest {

    @Test
    fun testRequestJson_generatesCorrectJsonString() {
        // Arrange
        val types =
            setOf(
                CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
            )
        val extensions = setOf(KnownExtensions.KNOWN_EXTENSION_SHARED)
        val request = ImportCredentialsRequest(types, extensions)

        // Act
        val jsonString = request.requestJson

        // Assert
        val jsonObject = JSONObject(jsonString)
        val typesArray = jsonObject.getJSONArray(CREDENTIAL_TYPES_JSON_KEY)
        val extensionArray = jsonObject.getJSONArray(KNOWN_EXTENSIONS_JSON_KEY)

        assertThat(typesArray.length()).isEqualTo(2)
        assertThat(types).contains(typesArray[0])
        assertThat(types).contains(typesArray[1])
        assertThat(extensionArray.length()).isEqualTo(1)
        assertThat(extensions).contains(extensionArray[0])
    }

    @Test
    fun testEmptyCredentialTypes_throwsIAE() {
        // Arrange
        val types: Set<String> = setOf()
        val extensions = setOf(KnownExtensions.KNOWN_EXTENSION_SHARED)

        try {
            // Act
            val request = ImportCredentialsRequest(types, extensions)
            Assert.fail()
        } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun testRoundTrip_json() {
        // Arrange
        val original =
            ImportCredentialsRequest(
                setOf(
                    CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                    CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
                ),
                setOf(KnownExtensions.KNOWN_EXTENSION_SHARED),
            )

        // Act
        val jsonString = original.requestJson
        val reconstructed = ImportCredentialsRequest.createFrom(jsonString)

        // Assert
        assertThat(reconstructed!!.credentialTypes).isEqualTo(original.credentialTypes)
        assertThat(reconstructed.knownExtensions).isEqualTo(original.knownExtensions)
        assertThat(reconstructed.requestJson).isEqualTo(original.requestJson)
    }

    @Test
    fun testRoundTrip_json_emptyKnownExtensions() {
        // Arrange
        val original =
            ImportCredentialsRequest(
                setOf(
                    CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                    CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
                ),
                setOf(),
            )

        // Act
        val jsonString = original.requestJson
        val reconstructed = ImportCredentialsRequest.createFrom(jsonString)

        // Assert
        assertThat(reconstructed!!.credentialTypes).isEqualTo(original.credentialTypes)
        assertThat(reconstructed.knownExtensions).isEqualTo(original.knownExtensions)
        assertThat(reconstructed.requestJson).isEqualTo(original.requestJson)
    }

    @Test
    fun testRoundTrip_bundle() {
        // Arrange
        val original =
            ImportCredentialsRequest(
                setOf(
                    CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                    CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
                ),
                setOf(KnownExtensions.KNOWN_EXTENSION_SHARED),
            )

        // Act
        val bundle = ImportCredentialsRequest.toBundle(original)
        val reconstructed = ImportCredentialsRequest.createFrom(bundle)

        // Assert
        assertThat(reconstructed.credentialTypes).isEqualTo(original.credentialTypes)
        assertThat(reconstructed.knownExtensions).isEqualTo(original.knownExtensions)
        assertThat(reconstructed.requestJson).isEqualTo(original.requestJson)
    }
}
