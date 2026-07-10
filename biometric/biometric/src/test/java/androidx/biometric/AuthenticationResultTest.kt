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

package androidx.biometric

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationResultTest {

    @Test
    fun success_helpersReturnCorrectValues() {
        val success =
            AuthenticationResult.Success(null, BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC)

        assertThat(success.isSuccess()).isTrue()
        assertThat(success.isError()).isFalse()
        assertThat(success.isCustomFallbackSelected()).isFalse()

        assertThat(success.success()).isEqualTo(success)
        assertThat(success.error()).isNull()
        assertThat(success.customFallbackSelected()).isNull()
    }

    @Test
    fun success_withCryptoObject_returnsCorrectValues() {
        val crypto =
            BiometricPrompt.CryptoObject(javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding"))
        val success =
            AuthenticationResult.Success(
                crypto,
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
            )

        assertThat(success.crypto).isEqualTo(crypto)
        assertThat(success.authType).isEqualTo(BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC)
    }

    @Test
    fun error_helpersReturnCorrectValues() {
        val error = AuthenticationResult.Error(BiometricPrompt.ERROR_CANCELED, "Canceled")

        assertThat(error.isSuccess()).isFalse()
        assertThat(error.isError()).isTrue()
        assertThat(error.isCustomFallbackSelected()).isFalse()

        assertThat(error.error()).isEqualTo(error)
        assertThat(error.success()).isNull()
        assertThat(error.customFallbackSelected()).isNull()
    }

    @Test
    fun customFallbackSelected_helpersReturnCorrectValues() {
        val fallback = AuthenticationRequest.Biometric.Fallback.CustomOption("Test Fallback")
        val result = AuthenticationResult.CustomFallbackSelected(fallback)

        assertThat(result.isSuccess()).isFalse()
        assertThat(result.isError()).isFalse()
        assertThat(result.isCustomFallbackSelected()).isTrue()

        assertThat(result.customFallbackSelected()).isEqualTo(result)
        assertThat(result.fallback).isEqualTo(fallback)

        assertThat(result.success()).isNull()
        assertThat(result.error()).isNull()
    }
}
