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

package androidx.credentials.provider

import android.os.Bundle
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.getTestCallingAppInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@OptIn(ExperimentalDigitalCredentialApi::class)
class BindingTokenTest {

    @Test
    fun constructor_propertiesSet() {
        val rawToken = byteArrayOf(1, 2, 3, 4, 5)
        val holderAppId = "android:apk-key-hash:abcdef123456:com.example.holder"
        val version = BindingToken.VERSION_PREVIEW

        val token = BindingToken(rawToken, holderAppId, version)

        assertThat(token.bindingToken).isEqualTo(rawToken)
        assertThat(token.holderAppId).isEqualTo(holderAppId)
        assertThat(token.version).isEqualTo(version)
    }

    @Test
    fun constructor_defaultVersion() {
        val rawToken = byteArrayOf(1, 2, 3)
        val holderAppId = "android:apk-key-hash:abcdef123456:com.example.holder"

        val token = BindingToken(rawToken, holderAppId)

        assertThat(token.bindingToken).isEqualTo(rawToken)
        assertThat(token.holderAppId).isEqualTo(holderAppId)
        assertThat(token.version).isEqualTo(BindingToken.VERSION_PREVIEW)
    }

    @Test
    fun equals_and_hashCode() {
        val token1 = byteArrayOf(1, 2, 3)
        val token2 = byteArrayOf(1, 2, 3)
        val token3 = byteArrayOf(4, 5, 6)

        val obj1 = BindingToken(token1, "appId1", BindingToken.VERSION_PREVIEW)
        val obj2 = BindingToken(token2, "appId1", BindingToken.VERSION_PREVIEW)
        val obj3 = BindingToken(token3, "appId1", BindingToken.VERSION_PREVIEW)
        val obj4 = BindingToken(token1, "appId2", BindingToken.VERSION_PREVIEW)
        val obj5 = BindingToken(token1, "appId1", "v2")

        assertThat(obj1).isEqualTo(obj2)
        assertThat(obj1.hashCode()).isEqualTo(obj2.hashCode())

        assertThat(obj1).isNotEqualTo(obj3)
        assertThat(obj1).isNotEqualTo(obj4)
        assertThat(obj1).isNotEqualTo(obj5)
    }

    @Test
    fun providerCreateCredentialRequest_retrieveBindingToken_present() {
        val rawToken = byteArrayOf(9, 8, 7)
        val holderAppId = "android:apk-key-hash:xyz:com.holder"
        val version = BindingToken.VERSION_PREVIEW

        val sourceBundle =
            Bundle().apply {
                putByteArray(BindingToken.EXTRA_BINDING_TOKEN, rawToken)
                putString(BindingToken.EXTRA_BINDING_TOKEN_HOLDER_APP_ID, holderAppId)
                putString(BindingToken.EXTRA_BINDING_TOKEN_VERSION, version)
            }

        val request =
            ProviderCreateCredentialRequest(
                callingRequest = CreatePasswordRequest("id", "pw"),
                callingAppInfo = getTestCallingAppInfo(null),
                biometricPromptResult = null,
                sourceBundle = sourceBundle,
            )

        val bindingToken = request.retrieveBindingToken()
        assertThat(bindingToken).isNotNull()
        assertThat(bindingToken!!.bindingToken).isEqualTo(rawToken)
        assertThat(bindingToken.holderAppId).isEqualTo(holderAppId)
        assertThat(bindingToken.version).isEqualTo(version)
    }

    @Test
    fun providerCreateCredentialRequest_retrieveBindingToken_defaultVersionFallback() {
        val rawToken = byteArrayOf(9, 8, 7)
        val holderAppId = "android:apk-key-hash:xyz:com.holder"

        val sourceBundle =
            Bundle().apply {
                putByteArray(BindingToken.EXTRA_BINDING_TOKEN, rawToken)
                putString(BindingToken.EXTRA_BINDING_TOKEN_HOLDER_APP_ID, holderAppId)
            }

        val request =
            ProviderCreateCredentialRequest(
                callingRequest = CreatePasswordRequest("id", "pw"),
                callingAppInfo = getTestCallingAppInfo(null),
                biometricPromptResult = null,
                sourceBundle = sourceBundle,
            )

        val bindingToken = request.retrieveBindingToken()
        assertThat(bindingToken).isNotNull()
        assertThat(bindingToken!!.bindingToken).isEqualTo(rawToken)
        assertThat(bindingToken.holderAppId).isEqualTo(holderAppId)
        assertThat(bindingToken.version).isEqualTo(BindingToken.VERSION_PREVIEW)
    }

    @Test
    fun providerCreateCredentialRequest_retrieveBindingToken_missingToken_returnsNull() {
        val sourceBundle =
            Bundle().apply {
                putString(BindingToken.EXTRA_BINDING_TOKEN_HOLDER_APP_ID, "appId")
            }

        val request =
            ProviderCreateCredentialRequest(
                callingRequest = CreatePasswordRequest("id", "pw"),
                callingAppInfo = getTestCallingAppInfo(null),
                biometricPromptResult = null,
                sourceBundle = sourceBundle,
            )

        assertThat(request.retrieveBindingToken()).isNull()
    }

    @Test
    fun providerCreateCredentialRequest_retrieveBindingToken_missingHolderAppId_returnsNull() {
        val sourceBundle =
            Bundle().apply { putByteArray(BindingToken.EXTRA_BINDING_TOKEN, byteArrayOf(1, 2)) }

        val request =
            ProviderCreateCredentialRequest(
                callingRequest = CreatePasswordRequest("id", "pw"),
                callingAppInfo = getTestCallingAppInfo(null),
                biometricPromptResult = null,
                sourceBundle = sourceBundle,
            )

        assertThat(request.retrieveBindingToken()).isNull()
    }

    @Test
    fun providerCreateCredentialRequest_retrieveBindingToken_missingBundle_returnsNull() {
        val request =
            ProviderCreateCredentialRequest(
                callingRequest = CreatePasswordRequest("id", "pw"),
                callingAppInfo = getTestCallingAppInfo(null),
                biometricPromptResult = null,
                sourceBundle = null,
            )

        assertThat(request.retrieveBindingToken()).isNull()
    }
}
