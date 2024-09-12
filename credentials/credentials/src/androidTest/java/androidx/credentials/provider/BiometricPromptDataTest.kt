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

package androidx.credentials.provider

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.credentials.provider.BiometricPromptData.Companion.BUNDLE_HINT_ALLOWED_AUTHENTICATORS
import androidx.credentials.provider.BiometricPromptData.Companion.BUNDLE_HINT_CRYPTO_OP_ID
import androidx.credentials.provider.utils.BiometricTestUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@SmallTest
class BiometricPromptDataTest {
    @Test
    fun construct_cryptoObjectStrongAllowedAuthenticator_success() {
        val biometricPromptData =
            BiometricPromptData(TEST_CRYPTO_OBJECT, TEST_ALLOWED_AUTHENTICATOR)

        assertThat(biometricPromptData.allowedAuthenticators).isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
        assertThat(biometricPromptData.cryptoObject).isEqualTo(TEST_CRYPTO_OBJECT)
    }

    @Test
    fun construct_cryptoObjectNullAuthenticatorNotProvided_successWithWeakAuthenticator() {
        val expectedAuthenticator = BiometricManager.Authenticators.BIOMETRIC_WEAK

        val biometricPromptData = BiometricPromptData()

        assertThat(biometricPromptData.cryptoObject).isNull()
        assertThat(biometricPromptData.allowedAuthenticators).isEqualTo(expectedAuthenticator)
    }

    @Test
    fun construct_cryptoObjectExistsAuthenticatorNotProvided_defaultWeakAuthenticatorThrowsIAE() {
        assertThrows(
            "Expected invalid allowed authenticator with cryptoObject to throw " +
                "IllegalArgumentException",
            java.lang.IllegalArgumentException::class.java
        ) {
            BiometricPromptData(TEST_CRYPTO_OBJECT)
        }
    }

    @Test
    fun construct_cryptoObjectNullAuthenticatorNonNull_successPassedInAuthenticator() {
        val expectedAuthenticator = BiometricManager.Authenticators.BIOMETRIC_STRONG

        val biometricPromptData = BiometricPromptData(cryptoObject = null, expectedAuthenticator)

        assertThat(biometricPromptData.cryptoObject).isNull()
        assertThat(biometricPromptData.allowedAuthenticators).isEqualTo(expectedAuthenticator)
    }

    @Test
    fun construct_authenticatorNotAccepted_throwsIAE() {
        assertThrows(
            "Expected invalid allowed authenticator IllegalArgumentException",
            java.lang.IllegalArgumentException::class.java
        ) {
            BiometricPromptData(null, allowedAuthenticators = Int.MIN_VALUE)
        }
    }

    @Test
    fun build_requiredParamsOnly_success() {
        val expectedAllowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK

        val actualBiometricPromptData = BiometricPromptData.Builder().build()

        assertThat(actualBiometricPromptData.allowedAuthenticators)
            .isEqualTo(expectedAllowedAuthenticators)
        assertThat(actualBiometricPromptData.cryptoObject).isNull()
    }

    @Test
    fun build_setCryptoObjectWithStrongAuthenticatorOnly_success() {
        val actualBiometricPromptData =
            BiometricPromptData.Builder()
                .setCryptoObject(TEST_CRYPTO_OBJECT)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

        assertThat(actualBiometricPromptData.cryptoObject).isEqualTo(TEST_CRYPTO_OBJECT)
        assertThat(actualBiometricPromptData.allowedAuthenticators)
            .isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
    }

    @Test
    fun build_setAllowedAuthenticator_success() {
        val actualBiometricPromptData =
            BiometricPromptData.Builder()
                .setAllowedAuthenticators(TEST_ALLOWED_AUTHENTICATOR)
                .build()

        assertThat(actualBiometricPromptData.allowedAuthenticators)
            .isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
    }

    @Test
    fun build_setInvalidAllowedAuthenticator_success() {
        assertThrows(
            "Expected builder invalid allowed authenticator to throw " + "IllegalArgumentException",
            java.lang.IllegalArgumentException::class.java
        ) {
            BiometricPromptData.Builder().setAllowedAuthenticators(-10000).build()
        }
    }

    @Test
    fun fromBundle_validAllowedAuthenticator_success() {
        val inputBundle = Bundle()
        inputBundle.putInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS, TEST_ALLOWED_AUTHENTICATOR)

        val actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle)

        assertThat(actualBiometricPromptData).isNotNull()
        assertThat(actualBiometricPromptData!!.allowedAuthenticators)
            .isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
        assertThat(actualBiometricPromptData.cryptoObject).isNull()
    }

    @Test
    fun fromBundle_validAllowedAuthenticatorAboveApi35_success() {
        val expectedOpId = getTestCryptoObjectOpId()
        val inputBundle = Bundle()
        inputBundle.putInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS, TEST_ALLOWED_AUTHENTICATOR)
        inputBundle.putLong(BUNDLE_HINT_CRYPTO_OP_ID, expectedOpId)

        val actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle)

        assertThat(actualBiometricPromptData).isNotNull()
        assertThat(actualBiometricPromptData!!.allowedAuthenticators)
            .isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
        assertThat(actualBiometricPromptData.cryptoObject).isNull()
        // TODO(b/368395001) : Add CryptoObject test back when library dependency updates
    }

    private fun getTestCryptoObjectOpId(cryptoObject: CryptoObject = TEST_CRYPTO_OBJECT) =
        BiometricTestUtils.getTestCryptoObjectOpId(cryptoObject)

    @Test
    fun fromBundle_unrecognizedAllowedAuthenticator_success() {
        val inputBundle = Bundle()
        val unrecognizedAuthenticator = Integer.MAX_VALUE
        inputBundle.putInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS, unrecognizedAuthenticator)

        val actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle)

        assertThat(actualBiometricPromptData).isNotNull()
        assertThat(actualBiometricPromptData!!.allowedAuthenticators)
            .isEqualTo(unrecognizedAuthenticator)
    }

    @Test
    fun fromBundle_invalidBundleKey_nullBiometricPromptData() {
        val expectedOpId = Integer.MIN_VALUE
        val inputBundle = Bundle()
        val unrecognizedAuthenticator = Integer.MAX_VALUE
        inputBundle.putInt("invalid key", unrecognizedAuthenticator)
        inputBundle.putInt(BUNDLE_HINT_CRYPTO_OP_ID, expectedOpId)

        val actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle)

        assertThat(actualBiometricPromptData).isNull()
    }

    @Test
    fun toBundle_success() {
        val testBiometricPromptData =
            BiometricPromptData(TEST_CRYPTO_OBJECT, TEST_ALLOWED_AUTHENTICATOR)

        val actualBundle = BiometricPromptData.toBundle(testBiometricPromptData)

        assertThat(actualBundle).isNotNull()
        assertThat(actualBundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS))
            .isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
        assertThat(actualBundle.getInt(BUNDLE_HINT_CRYPTO_OP_ID))
            .isEqualTo(DEFAULT_BUNDLE_LONG_FOR_CRYPTO_ID)
    }

    @Test
    fun toBundle_api35AndAboveWithOpId_success() {
        val testBiometricPromptData =
            BiometricPromptData(TEST_CRYPTO_OBJECT, TEST_ALLOWED_AUTHENTICATOR)
        val expectedOpId = getTestCryptoObjectOpId()

        val actualBundle = BiometricPromptData.toBundle(testBiometricPromptData)

        assertThat(actualBundle).isNotNull()
        assertThat(actualBundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS))
            .isEqualTo(TEST_ALLOWED_AUTHENTICATOR)
        assertThat(actualBundle.getLong(BUNDLE_HINT_CRYPTO_OP_ID)).isEqualTo(expectedOpId)
    }

    private companion object {
        private val TEST_CRYPTO_OBJECT = BiometricTestUtils.createCryptoObject()

        private const val DEFAULT_BUNDLE_LONG_FOR_CRYPTO_ID = 0L

        private const val TEST_ALLOWED_AUTHENTICATOR =
            BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
}
