/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import javax.crypto.Cipher;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AuthenticationCallbackProviderTest {
    @Mock private AuthenticationCallbackProvider.Listener mListener;
    @Mock private Cipher mCipher;

    @Captor private ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    private AuthenticationCallbackProvider mAuthenticationCallbackProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAuthenticationCallbackProvider = new AuthenticationCallbackProvider(mListener);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testBiometricCallback_IsCached() {
        final android.hardware.biometrics.BiometricPrompt.AuthenticationCallback callback =
                mAuthenticationCallbackProvider.getBiometricCallback();
        assertThat(mAuthenticationCallbackProvider.getBiometricCallback()).isEqualTo(callback);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testBiometricCallback_HandlesSuccess() {
        final android.hardware.biometrics.BiometricPrompt.AuthenticationResult result =
                mock(android.hardware.biometrics.BiometricPrompt.AuthenticationResult.class);
        final android.hardware.biometrics.BiometricPrompt.CryptoObject crypto =
                new android.hardware.biometrics.BiometricPrompt.CryptoObject(mCipher);
        when(result.getCryptoObject()).thenReturn(crypto);

        mAuthenticationCallbackProvider.getBiometricCallback().onAuthenticationSucceeded(result);

        verify(mListener).onSuccess(mResultCaptor.capture());
        verifyNoMoreInteractions(mListener);
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNotNull();
        assertThat(mResultCaptor.getValue().getCryptoObject().getCipher()).isEqualTo(mCipher);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testBiometricCallback_HandlesError() {
        final int errorCode = BiometricPrompt.ERROR_HW_NOT_PRESENT;
        final String errorMessage = "Lorem ipsum";

        mAuthenticationCallbackProvider.getBiometricCallback()
                .onAuthenticationError(errorCode, errorMessage);

        verify(mListener).onError(errorCode, errorMessage);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testBiometricCallback_IgnoresHelp() {
        final int helpCode = BiometricPrompt.ERROR_UNABLE_TO_PROCESS;
        final String helpMessage = "Dolor sit amet";

        mAuthenticationCallbackProvider.getBiometricCallback()
                .onAuthenticationHelp(helpCode, helpMessage);

        verifyZeroInteractions(mListener);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testBiometricCallback_HandlesFailure() {
        mAuthenticationCallbackProvider.getBiometricCallback().onAuthenticationFailed();

        verify(mListener).onFailure();
        verifyNoMoreInteractions(mListener);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFingerprintCallback_IsCached() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback
                callback = mAuthenticationCallbackProvider.getFingerprintCallback();
        assertThat(mAuthenticationCallbackProvider.getFingerprintCallback()).isEqualTo(callback);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFingerprintCallback_HandlesSuccess() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject crypto =
                new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                        mCipher);
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationResult
                result = new androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        .AuthenticationResult(crypto);

        mAuthenticationCallbackProvider.getFingerprintCallback().onAuthenticationSucceeded(result);

        verify(mListener).onSuccess(mResultCaptor.capture());
        verifyNoMoreInteractions(mListener);
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNotNull();
        assertThat(mResultCaptor.getValue().getCryptoObject().getCipher()).isEqualTo(mCipher);
    }

    @Test
    public void testFingerprintCallback_HandlesError() {
        final int errorCode = BiometricPrompt.ERROR_CANCELED;
        final String errorMessage = "Foo bar";

        mAuthenticationCallbackProvider.getFingerprintCallback()
                .onAuthenticationError(errorCode, errorMessage);

        verify(mListener).onError(errorCode, errorMessage);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void testFingerprintCallback_HandlesHelp() {
        final int helpCode = BiometricPrompt.ERROR_TIMEOUT;
        final String helpMessage = "Baz qux";

        mAuthenticationCallbackProvider.getFingerprintCallback()
                .onAuthenticationHelp(helpCode, helpMessage);

        verify(mListener).onHelp(helpMessage);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void testFingerprintCallback_HandlesFailure() {
        mAuthenticationCallbackProvider.getFingerprintCallback().onAuthenticationFailed();

        verify(mListener).onFailure();
        verifyNoMoreInteractions(mListener);
    }
}
