/*
 * Copyright 2019 The Android Open Source Project
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

import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@LargeTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class BiometricFragmentTest {
    private static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };

    @Mock
    public BiometricPrompt.AuthenticationCallback mAuthenticationCallback;

    @Captor
    public ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    private BiometricFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = BiometricFragment.newInstance();
    }

    @Test
    public void testCancel_DoesNotCrash_WhenNotAssociatedWithFragmentManager() {
        mFragment.cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
    }

    @Test
    public void testOnBiometricSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        mFragment.setClientCallback(EXECUTOR, mAuthenticationCallback);

        mFragment.mCallbackProvider.getBiometricCallback()
                .onAuthenticationSucceeded(null /* result */);

        verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
    }

    @Test
    public void testOnFingerprintSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        mFragment.setClientCallback(EXECUTOR, mAuthenticationCallback);

        mFragment.mCallbackProvider.getFingerprintCallback()
                .onAuthenticationSucceeded(null /* result */);

        verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
    }

    @Test
    public void testOnFingerprintError_DoesShowErrorAndDismissDialog_WhenHardwareUnavailable() {
        mFragment.setClientCallback(EXECUTOR, mAuthenticationCallback);

        final int errMsgId = BiometricConstants.ERROR_HW_UNAVAILABLE;
        final String errString = "lorem ipsum";
        mFragment.mCallbackProvider.getFingerprintCallback()
                .onAuthenticationError(errMsgId, errString);

        verify(mAuthenticationCallback).onAuthenticationError(errMsgId, errString);
        assertThat(mFragment.isVisible()).isFalse();
    }
}
