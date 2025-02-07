/*
 * Copyright 2022 The Android Open Source Project
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;

import com.google.common.util.concurrent.MoreExecutors;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@SuppressWarnings("deprecation")
public class BiometricFragmentTest {
    private static final Executor EXECUTOR = MoreExecutors.directExecutor();

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback;
    @Mock
    private Context mContext;
    @Mock
    private Handler mHandler;
    @Mock
    private androidx.core.hardware.fingerprint.FingerprintManagerCompat mFingerprintManager;

    @Captor
    private ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    private final BiometricViewModel mViewModel = new BiometricViewModel();
    private BiometricFragmentFactory mFragmentFactory;

    @Before
    public void setUp() {
        prepareMockHandler(mHandler);
        mViewModel.setClientExecutor(EXECUTOR);
        mViewModel.setClientCallback(mAuthenticationCallback);
        mViewModel.setAwaitingResult(true);
        mViewModel.setPromptInfo(new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .setNegativeButtonText("Cancel")
                .build());
        mFragmentFactory = new BiometricFragmentFactory(mHandler, mViewModel, true, true, true,
                true);
    }

    @Test
    public void testCancel_DoesNotCrash_WhenNotAssociatedWithFragmentManager() {
        try (FragmentScenario<BiometricFragment> scenario = FragmentScenario.launchInContainer(
                BiometricFragment.class, null, mFragmentFactory)) {
            scenario.onFragment(fragment -> {
                fragment.cancelAuthentication(
                        BiometricFragment.CANCELED_FROM_INTERNAL);

            });
        }
    }

    @Test
    public void testOnAuthenticationSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        try (FragmentScenario<BiometricFragment> scenario = FragmentScenario.launchInContainer(
                BiometricFragment.class, null, mFragmentFactory)) {
            scenario.onFragment(fragment -> {
                fragment.onAuthenticationSucceeded(
                        new BiometricPrompt.AuthenticationResult(
                                null /* crypto */,
                                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC));

                verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
                assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
            });
        }

    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M, maxSdk = Build.VERSION_CODES.O_MR1)
    public void testOnFingerprintError_DoesShowErrorAndDismiss_WhenHardwareUnavailable() {
        final int errMsgId = BiometricPrompt.ERROR_HW_UNAVAILABLE;
        final String errString = "lorem ipsum";

        mViewModel.setPromptShowing(true);
        mViewModel.setFingerprintDialogDismissedInstantly(false);
        mFragmentFactory = new BiometricFragmentFactory(mHandler, mViewModel, true, true, true,
                true);

        try (FragmentScenario<BiometricFragment> scenario = FragmentScenario.launchInContainer(
                BiometricFragment.class, null, mFragmentFactory)) {
            scenario.onFragment(fragment -> {
                fragment.onAuthenticationError(errMsgId, errString);

                assertThat(mViewModel.getFingerprintDialogState().getValue())
                        .isEqualTo(FingerprintDialogFragment.STATE_FINGERPRINT_ERROR);
                assertThat(mViewModel.getFingerprintDialogHelpMessage().getValue()).isEqualTo(
                        errString);
                assertThat(mViewModel.isPromptShowing()).isFalse();
                verify(mAuthenticationCallback).onAuthenticationError(errMsgId, errString);
            });
        }


    }

    @Test
    public void testAuthenticate_ReturnsWithoutError_WhenDetached() {
        try (FragmentScenario<BiometricFragment> scenario = FragmentScenario.launchInContainer(
                BiometricFragment.class, null, mFragmentFactory)) {
            scenario.onFragment(fragment -> fragment.authenticate(
                    new BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Title")
                            .setNegativeButtonText("Cancel")
                            .build(),
                    null /* crypto */));
        }
    }

    @Test
    public void testAuthenticateWithFingerprint_DoesShowErrorAndDismiss_WhenNPEThrown() {
        try (FragmentScenario<BiometricFragment> scenario = FragmentScenario.launchInContainer(
                BiometricFragment.class, null, mFragmentFactory)) {
            scenario.onFragment(fragment -> {
                final int errMsgId = BiometricPrompt.ERROR_HW_UNAVAILABLE;
                final String errString = "test string";
                doThrow(NullPointerException.class).when(mFingerprintManager).authenticate(
                        nullable(androidx.core.hardware.fingerprint.FingerprintManagerCompat
                                .CryptoObject.class),
                        anyInt(),
                        any(androidx.core.os.CancellationSignal.class),
                        any(androidx.core.hardware.fingerprint.FingerprintManagerCompat
                                .AuthenticationCallback.class),
                        nullable(Handler.class));
                when(mContext.getString(anyInt())).thenReturn(errString);

                fragment.authenticateWithFingerprint(mFingerprintManager, mContext);

                verify(mAuthenticationCallback).onAuthenticationError(eq(errMsgId), anyString());

            });
        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    @RequiresApi(Build.VERSION_CODES.P)
    public void testAuthenticateWithBiometricPrompt_DoesShowErrorAndDismiss_WhenNPEThrown() {
        final int errMsgId = BiometricPrompt.ERROR_HW_UNAVAILABLE;
        final String errString = "test string";

        try (FragmentScenario<BiometricFragment> scenario = FragmentScenario.launchInContainer(
                BiometricFragment.class, null, mFragmentFactory)) {
            scenario.onFragment(fragment -> {
                final android.hardware.biometrics.BiometricPrompt biometricPrompt =
                        mock(android.hardware.biometrics.BiometricPrompt.class);
                doThrow(NullPointerException.class).when(biometricPrompt).authenticate(
                        any(android.os.CancellationSignal.class),
                        any(Executor.class),
                        any(android.hardware.biometrics.BiometricPrompt.AuthenticationCallback.class));
                when(mContext.getString(anyInt())).thenReturn(errString);

                fragment.authenticateWithBiometricPrompt(biometricPrompt, mContext);

                verify(mAuthenticationCallback).onAuthenticationError(eq(errMsgId), anyString());

            });
        }
    }

    private static void prepareMockHandler(Handler mockHandler) {
        // Immediately invoke any scheduled callbacks.
        when(mockHandler.postDelayed(any(Runnable.class), anyLong()))
                .thenAnswer((Answer<Boolean>) invocation -> {
                    final Runnable runnable = invocation.getArgument(0);
                    if (runnable != null) {
                        runnable.run();
                    }
                    return true;
                });
    }


    private static class BiometricFragmentFactory extends FragmentFactory {
        private final Handler mHandler;
        private final BiometricViewModel mViewModel;
        private final boolean mHostedInActivity;
        private final boolean mHasFingerprint;
        private final boolean mHasFace;
        private final boolean mHasIris;

        BiometricFragmentFactory(@NonNull Handler handler,
                @NonNull BiometricViewModel viewModel,
                boolean hostedInActivity, boolean hasFingerprint, boolean hasFace,
                boolean hasIris) {
            mHandler = handler;
            mViewModel = viewModel;
            mHostedInActivity = hostedInActivity;
            mHasFace = hasFace;
            mHasFingerprint = hasFingerprint;
            mHasIris = hasIris;
        }

        @Override
        @NonNull
        public Fragment instantiate(@NonNull ClassLoader classLoader, String className) {
            if (className.equals(BiometricFragment.class.getName())) {
                return BiometricFragment.newInstance(mHandler, mViewModel, mHostedInActivity,
                        mHasFingerprint, mHasFace, mHasIris);
            }
            return super.instantiate(classLoader, className);
        }
    }
}
