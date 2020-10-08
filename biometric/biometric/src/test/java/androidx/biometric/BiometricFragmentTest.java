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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.os.CancellationSignal;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
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

    @Mock private BiometricPrompt.AuthenticationCallback mAuthenticationCallback;
    @Mock private Handler mHandler;
    @Mock private androidx.core.hardware.fingerprint.FingerprintManagerCompat
            mFingerprintManagerCompat;
    @Mock private Context mContext;

    @Captor private ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    private BiometricFragment mFragment;
    private BiometricViewModel mViewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        prepareMockHandler(mHandler);
        mFragment = BiometricFragment.newInstance();
        mViewModel = new BiometricViewModel();
        mFragment.mHandler = mHandler;
        mFragment.mViewModel = mViewModel;
    }

    @Test
    public void testCancel_DoesNotCrash_WhenNotAssociatedWithFragmentManager() {
        mFragment.cancelAuthentication(BiometricFragment.CANCELED_FROM_INTERNAL);
    }

    @Test
    public void testOnAuthenticationSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        mViewModel.setClientExecutor(EXECUTOR);
        mViewModel.setClientCallback(mAuthenticationCallback);
        mViewModel.setAwaitingResult(true);

        mFragment.onAuthenticationSucceeded(
                new BiometricPrompt.AuthenticationResult(
                        null /* crypto */, BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC));

        verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M, maxSdk = Build.VERSION_CODES.O_MR1)
    public void testOnFingerprintError_DoesShowErrorAndDismiss_WhenHardwareUnavailable() {
        final int errMsgId = BiometricPrompt.ERROR_HW_UNAVAILABLE;
        final String errString = "lorem ipsum";

        mViewModel.setClientExecutor(EXECUTOR);
        mViewModel.setClientCallback(mAuthenticationCallback);
        mViewModel.setPromptShowing(true);
        mViewModel.setAwaitingResult(true);
        mViewModel.setFingerprintDialogDismissedInstantly(false);

        mFragment.onAuthenticationError(errMsgId, errString);

        assertThat(mViewModel.getFingerprintDialogState().getValue())
                .isEqualTo(FingerprintDialogFragment.STATE_FINGERPRINT_ERROR);
        assertThat(mViewModel.getFingerprintDialogHelpMessage().getValue()).isEqualTo(errString);
        assertThat(mViewModel.isPromptShowing()).isFalse();
        verify(mAuthenticationCallback).onAuthenticationError(errMsgId, errString);
    }

    @Test
    public void testAuthenticate_ReturnsWithoutError_WhenDetached() {
        mFragment.authenticate(
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Title")
                        .setNegativeButtonText("Cancel")
                        .build(),
                null /* crypto */);
    }

    @Test
    public void testAuthenticateWithFingerprint_DoesShowErrorAndDismiss_WhenNPEThrown() {
        mViewModel.setClientExecutor(EXECUTOR);
        mViewModel.setClientCallback(mAuthenticationCallback);
        mViewModel.setAwaitingResult(true);

        final int errMsgId = BiometricPrompt.ERROR_HW_UNAVAILABLE;
        final String errString = "test string";

        doThrow(NullPointerException.class).when(mFingerprintManagerCompat).authenticate(
                nullable(androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        .CryptoObject.class),
                anyInt(),
                any(CancellationSignal.class),
                any(androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        .AuthenticationCallback.class),
                nullable(Handler.class)
        );

        // Configure mock context to return test string
        when(mContext.getString(anyInt())).thenReturn(errString);

        // Have authentication via BiometricPrompt run, and have it invoke the mFingerprintCompat
        // authenticate call
        mFragment.authenticateWithFingerprint(mFingerprintManagerCompat, mContext);

        // Verify that authentication should fail and we should receive onError
        verify(mAuthenticationCallback).onAuthenticationError(eq(errMsgId), anyString());
    }

    private static void prepareMockHandler(Handler mockHandler) {
        // Immediately invoke any scheduled callbacks.
        when(mockHandler.postDelayed(any(Runnable.class), anyLong()))
                .thenAnswer(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) {
                        final Runnable runnable = invocation.getArgument(0);
                        if (runnable != null) {
                            runnable.run();
                        }
                        return true;
                    }
                });
    }
}
