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

import androidx.biometric.AuthenticationRequest.Biometric
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationRequestTest {

    @Test
    fun biometricRequest_withEmptyFallbacks_doesNotThrow() {
        val request =
            AuthenticationRequest.biometricRequest(
                title = "Title",
                authFallbacks = emptyArray<Biometric.Fallback>(),
            ) {}
        assertThat(request.authFallbacks).isEmpty()
    }

    @Test
    fun biometricRequest_withMultipleDeviceCredentials_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationRequest.biometricRequest(
                title = "Title",
                Biometric.Fallback.DeviceCredential,
                Biometric.Fallback.DeviceCredential,
            ) {}
        }
    }

    @Test
    fun biometricRequest_withTooManyFallbacks_throwsIllegalArgumentException() {
        val maxFallbacks = Biometric.getMaxFallbackOptions()
        val fallbacks = Array(maxFallbacks + 1) { Biometric.Fallback.CustomOption("Option $it") }
        assertThrows(IllegalArgumentException::class.java) {
            AuthenticationRequest.biometricRequest(title = "Title", authFallbacks = fallbacks) {}
        }
    }

    @Test
    fun biometricRequest_withValidFallbacks_doesNotThrow() {
        val request =
            AuthenticationRequest.biometricRequest(
                title = "Title",
                Biometric.Fallback.CustomOption("Cancel"),
            ) {
                setSubtitle("Subtitle")
            }
        assertThat(request.title).isEqualTo("Title")
        assertThat(request.subtitle).isEqualTo("Subtitle")
        assertThat(request.authFallbacks).hasSize(1)
        assertThat((request.authFallbacks[0] as Biometric.Fallback.CustomOption).text)
            .isEqualTo("Cancel")
    }

    @Test
    fun biometricRequest_withMultipleCustomOptions_storesAllFallbacks() {
        val request =
            AuthenticationRequest.biometricRequest(
                title = "Title",
                Biometric.Fallback.CustomOption("Option 1"),
                Biometric.Fallback.CustomOption("Option 2"),
            ) {}
        assertThat(request.authFallbacks).hasSize(2)
        assertThat((request.authFallbacks[0] as Biometric.Fallback.CustomOption).text)
            .isEqualTo("Option 1")
        assertThat((request.authFallbacks[1] as Biometric.Fallback.CustomOption).text)
            .isEqualTo("Option 2")
    }

    @Test
    fun biometricRequest_withCustomOptionAndDeviceCredential_storesBoth() {
        val request =
            AuthenticationRequest.biometricRequest(
                title = "Title",
                Biometric.Fallback.CustomOption("Cancel"),
                Biometric.Fallback.DeviceCredential,
            ) {}
        assertThat(request.authFallbacks).hasSize(2)
        assertThat(request.authFallbacks[0])
            .isInstanceOf(Biometric.Fallback.CustomOption::class.java)
        assertThat(request.authFallbacks[1]).isEqualTo(Biometric.Fallback.DeviceCredential)
    }

    @Test
    fun biometricRequest_withMaxFallbacks_storesAll() {
        val maxFallbacks = Biometric.getMaxFallbackOptions()
        val fallbacks = Array(maxFallbacks) { Biometric.Fallback.CustomOption("Option $it") }
        val request =
            AuthenticationRequest.biometricRequest(title = "Title", authFallbacks = fallbacks) {}
        assertThat(request.authFallbacks).hasSize(maxFallbacks)
    }

    @Test
    fun biometricRequest_usingBuilder_storesOptionsCorrectly() {
        val fallback = Biometric.Fallback.CustomOption("Option")
        val request =
            Biometric.Builder("Title", fallback)
                .setSubtitle("Subtitle")
                .setContent(AuthenticationRequest.BodyContent.PlainText("Content"))
                .setIsConfirmationRequired(false)
                .build()

        assertThat(request.title).isEqualTo("Title")
        assertThat(request.subtitle).isEqualTo("Subtitle")
        assertThat((request.content as AuthenticationRequest.BodyContent.PlainText).description)
            .isEqualTo("Content")
        assertThat(request.isConfirmationRequired).isFalse()
        assertThat(request.authFallbacks).containsExactly(fallback)
    }

    @Test
    fun biometricRequest_withMultipleValidFallbacks_storesAllFallbacks() {
        val request =
            AuthenticationRequest.biometricRequest(
                title = "Title",
                Biometric.Fallback.CustomOption("Option 1", BiometricPrompt.ICON_TYPE_PASSWORD),
                Biometric.Fallback.CustomOption("Option 2", BiometricPrompt.ICON_TYPE_ACCOUNT),
                Biometric.Fallback.DeviceCredential,
            ) {}
        assertThat(request.authFallbacks).hasSize(3)
        assertThat((request.authFallbacks[0] as Biometric.Fallback.CustomOption).text)
            .isEqualTo("Option 1")
        assertThat((request.authFallbacks[0] as Biometric.Fallback.CustomOption).iconType)
            .isEqualTo(BiometricPrompt.ICON_TYPE_PASSWORD)
        assertThat((request.authFallbacks[1] as Biometric.Fallback.CustomOption).text)
            .isEqualTo("Option 2")
        assertThat((request.authFallbacks[1] as Biometric.Fallback.CustomOption).iconType)
            .isEqualTo(BiometricPrompt.ICON_TYPE_ACCOUNT)
        assertThat(request.authFallbacks[2]).isEqualTo(Biometric.Fallback.DeviceCredential)
    }
}
