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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executor;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class FingerprintHelperFragmentTest {
    private static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };

    private static final FingerprintHelperFragment.MessageRouter MESSAGE_ROUTER =
            new FingerprintHelperFragment.MessageRouter(null /* handler */) {
                @Override
                void sendMessage(int what) {
                }

                @Override
                void sendMessage(int what, Object obj) {
                }

                @Override
                void sendMessage(int what, int arg1, int arg2, Object obj) {
                }
            };

    @Test
    @UiThreadTest
    public void testOnAuthenticationSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        final BiometricPrompt.AuthenticationCallback callback =
                mock(BiometricPrompt.AuthenticationCallback.class);
        helperFragment.setCallback(EXECUTOR, callback);
        helperFragment.setMessageRouter(MESSAGE_ROUTER);

        helperFragment.mAuthenticationCallback.onAuthenticationSucceeded(null /* result */);

        final ArgumentCaptor<BiometricPrompt.AuthenticationResult> resultCaptor =
                ArgumentCaptor.forClass(BiometricPrompt.AuthenticationResult.class);
        verify(callback).onAuthenticationSucceeded(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getCryptoObject()).isNull();
    }
}
