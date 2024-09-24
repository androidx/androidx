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

package androidx.credentials.provider;

import static androidx.credentials.provider.BiometricPromptData.BUNDLE_HINT_ALLOWED_AUTHENTICATORS;
import static androidx.credentials.provider.BiometricPromptData.BUNDLE_HINT_CRYPTO_OP_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.credentials.provider.utils.BiometricTestUtils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 35)
public class BiometricPromptDataJavaTest {

    private static final BiometricPrompt.CryptoObject TEST_CRYPTO_OBJECT = BiometricTestUtils
            .INSTANCE.createCryptoObject$credentials_releaseAndroidTest();

    private static final long DEFAULT_BUNDLE_LONG_FOR_CRYPTO_ID = 0L;

    private static final  int TEST_ALLOWED_AUTHENTICATOR = BiometricManager.Authenticators
            .BIOMETRIC_STRONG;

    @Test
    public void construct_cryptoObjectStrongAllowedAuthenticator_success() {
        BiometricPromptData biometricPromptData = new BiometricPromptData(
                /*cryptoObject=*/TEST_CRYPTO_OBJECT,
                /*allowedAuthenticators=*/TEST_ALLOWED_AUTHENTICATOR
        );

        assertThat(biometricPromptData.getAllowedAuthenticators())
                .isEqualTo(TEST_ALLOWED_AUTHENTICATOR);
        assertThat(biometricPromptData.getCryptoObject()).isEqualTo(TEST_CRYPTO_OBJECT);
    }

    @Test
    public void construct_cryptoObjectNullAuthenticatorNotProvided_successWithWeakAuthenticator() {
        int expectedAuthenticator = BiometricManager.Authenticators.BIOMETRIC_WEAK;

        BiometricPromptData biometricPromptData = new BiometricPromptData.Builder().build();

        assertThat(biometricPromptData.getCryptoObject()).isNull();
        assertThat(biometricPromptData.getAllowedAuthenticators()).isEqualTo(expectedAuthenticator);
    }

    @Test
    public void construct_cryptoObjectExistsAuthenticatorNotProvided__defaultThrowsIAE() {
        assertThrows("Expected cryptoObject without strong authenticator to throw "
                        + "IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new BiometricPromptData.Builder()
                        .setCryptoObject(TEST_CRYPTO_OBJECT).build()
        );
    }

    @Test
    public void construct_cryptoObjectNullAuthenticatorNonNull_successPassedInAuthenticator() {
        BiometricPromptData biometricPromptData = new BiometricPromptData(
                /*cryptoObject=*/null,
                /*allowedAuthenticator=*/TEST_ALLOWED_AUTHENTICATOR
        );

        assertThat(biometricPromptData.getCryptoObject()).isNull();
        assertThat(biometricPromptData.getAllowedAuthenticators()).isEqualTo(
                TEST_ALLOWED_AUTHENTICATOR);
    }

    @Test
    public void construct_authenticatorNotAccepted_throwsIAE() {
        assertThrows("Expected invalid allowed authenticator to throw "
                        + "IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new BiometricPromptData(
                        /*cryptoObject=*/null,
                        /*allowedAuthenticator=*/Integer.MIN_VALUE
                )
        );
    }

    @Test
    public void build_requiredParamsOnly_success() {
        int expectedAllowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK;

        BiometricPromptData actualBiometricPromptData = new BiometricPromptData.Builder().build();

        assertThat(actualBiometricPromptData.getAllowedAuthenticators()).isEqualTo(
                expectedAllowedAuthenticators);
        assertThat(actualBiometricPromptData.getCryptoObject()).isNull();
    }

    @Test
    public void build_setCryptoObjectWithStrongAuthenticator_success() {
        BiometricPromptData actualBiometricPromptData = new BiometricPromptData.Builder()
                .setCryptoObject(TEST_CRYPTO_OBJECT)
                .setAllowedAuthenticators(TEST_ALLOWED_AUTHENTICATOR).build();

        assertThat(actualBiometricPromptData.getCryptoObject()).isEqualTo(TEST_CRYPTO_OBJECT);
        assertThat(actualBiometricPromptData.getAllowedAuthenticators())
                .isEqualTo(TEST_ALLOWED_AUTHENTICATOR);
    }

    @Test
    public void build_setAllowedAuthenticator_success() {
        BiometricPromptData actualBiometricPromptData = new BiometricPromptData.Builder()
                .setAllowedAuthenticators(TEST_ALLOWED_AUTHENTICATOR).build();

        assertThat(actualBiometricPromptData.getAllowedAuthenticators())
                .isEqualTo(TEST_ALLOWED_AUTHENTICATOR);
    }

    @Test
    public void fromBundle_validAllowedAuthenticator_success() {
        Bundle inputBundle = new Bundle();
        inputBundle.putInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS, TEST_ALLOWED_AUTHENTICATOR);

        BiometricPromptData actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle);

        assertThat(actualBiometricPromptData).isNotNull();
        assertThat(actualBiometricPromptData.getAllowedAuthenticators()).isEqualTo(
                TEST_ALLOWED_AUTHENTICATOR);
        assertThat(actualBiometricPromptData.getCryptoObject()).isNull();
    }

    @Test
    public void fromBundle_validAllowedAuthenticatorAboveApi35_success() {
        long expectedOpId = BiometricTestUtils.INSTANCE
                .getTestCryptoObjectOpId$credentials_releaseAndroidTest(TEST_CRYPTO_OBJECT);
        Bundle inputBundle = new Bundle();
        inputBundle.putInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS, TEST_ALLOWED_AUTHENTICATOR);
        inputBundle.putLong(BUNDLE_HINT_CRYPTO_OP_ID, expectedOpId);

        BiometricPromptData actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle);

        assertThat(actualBiometricPromptData).isNotNull();
        assertThat(actualBiometricPromptData.getAllowedAuthenticators()).isEqualTo(
                TEST_ALLOWED_AUTHENTICATOR);
        assertThat(actualBiometricPromptData.getCryptoObject()).isNull();
        // TODO(b/368395001) : Add CryptoObject test back when library dependency updates
    }

    @Test
    public void fromBundle_unrecognizedAllowedAuthenticator_success() {
        Bundle inputBundle = new Bundle();
        int unrecognizedAuthenticator = Integer.MAX_VALUE;
        inputBundle.putInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS, unrecognizedAuthenticator);

        BiometricPromptData actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle);

        assertThat(actualBiometricPromptData).isNotNull();
        assertThat(actualBiometricPromptData.getAllowedAuthenticators())
                .isEqualTo(unrecognizedAuthenticator);
    }

    @Test
    public void fromBundle_invalidBundleKey_nullBiometricPromptData() {
        int expectedOpId = Integer.MIN_VALUE;
        Bundle inputBundle = new Bundle();
        int unrecognizedAuthenticator = Integer.MAX_VALUE;
        inputBundle.putInt("invalidKey", unrecognizedAuthenticator);
        inputBundle.putInt(BUNDLE_HINT_CRYPTO_OP_ID, expectedOpId);

        BiometricPromptData actualBiometricPromptData = BiometricPromptData.fromBundle(inputBundle);

        assertThat(actualBiometricPromptData).isNull();
    }

    @Test
    public void toBundle_success() {
        BiometricPromptData testBiometricPromptData = new BiometricPromptData(/*cryptoObject=*/null,
                TEST_ALLOWED_AUTHENTICATOR);

        Bundle actualBundle = BiometricPromptData.toBundle(
                testBiometricPromptData);

        assertThat(actualBundle).isNotNull();
        assertThat(actualBundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS)).isEqualTo(
                TEST_ALLOWED_AUTHENTICATOR
        );
        assertThat(actualBundle.getInt(BUNDLE_HINT_CRYPTO_OP_ID)).isEqualTo(
                DEFAULT_BUNDLE_LONG_FOR_CRYPTO_ID);
    }

    @Test
    public void toBundle_api35AndAboveWithOpId_success() {
        BiometricPromptData testBiometricPromptData = new BiometricPromptData(TEST_CRYPTO_OBJECT,
                TEST_ALLOWED_AUTHENTICATOR);
        long expectedOpId = BiometricTestUtils.INSTANCE
                .getTestCryptoObjectOpId$credentials_releaseAndroidTest(TEST_CRYPTO_OBJECT);

        Bundle actualBundle = BiometricPromptData.toBundle(
                testBiometricPromptData);

        assertThat(actualBundle).isNotNull();
        assertThat(actualBundle.getInt(BUNDLE_HINT_ALLOWED_AUTHENTICATORS)).isEqualTo(
                TEST_ALLOWED_AUTHENTICATOR
        );
        assertThat(actualBundle.getLong(BUNDLE_HINT_CRYPTO_OP_ID)).isEqualTo(expectedOpId);
    }

    @Test
    public void build_setInvalidAllowedAuthenticator_throwsIAE() {
        assertThrows("Expected invalid allowed authenticator to throw "
                        + "IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new BiometricPromptData.Builder().setAllowedAuthenticators(-10000).build()
        );
    }
}
