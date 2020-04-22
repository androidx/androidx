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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.test.filters.MediumTest;

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
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@MediumTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class FingerprintDialogFragmentTest {
    private static final Answer<Object> RUN_IMMEDIATELY = new Answer<Object>() {
        @Override
        public Object answer(InvocationOnMock invocation) {
            final Runnable runnable = invocation.getArgument(0);
            if (runnable != null) {
                runnable.run();
            }
            return null;
        }
    };

    private static final Executor EXECUTOR = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };

    @Mock
    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback;
    @Mock
    private Handler mHandler;

    @Captor
    private ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    private FingerprintDialogFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        prepareMockHandler(mHandler);
        mFragment = FingerprintDialogFragment.newInstance();
        mFragment.setHandler(mHandler);
    }

    @Test
    public void testCancel_DoesNotCrash_WhenNotAssociatedWithFragmentManager() {
        mFragment.cancel(FingerprintDialogFragment.USER_CANCELED_FROM_NONE);
    }

    @Test
    public void testOnAuthenticationSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        mFragment.setCallback(EXECUTOR, mAuthenticationCallback);

        mFragment.mCallbackProvider.getFingerprintCallback()
                .onAuthenticationSucceeded(null /* result */);

        verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
    }

    @Test
    public void testOnAuthenticationError_DoesShowErrorAndDismissDialog_WhenHardwareUnavailable() {
        mFragment.setCallback(EXECUTOR, mAuthenticationCallback);

        final int errMsgId = BiometricConstants.ERROR_HW_UNAVAILABLE;
        final String errString = "lorem ipsum";
        mFragment.mCallbackProvider.getFingerprintCallback()
                .onAuthenticationError(errMsgId, errString);

        verify(mAuthenticationCallback).onAuthenticationError(errMsgId, errString);
        assertThat(mFragment.isVisible()).isFalse();
        assertThat(mFragment.isShowing()).isFalse();
    }

    private static void prepareMockHandler(Handler handler) {
        when(handler.post(any(Runnable.class))).thenAnswer(RUN_IMMEDIATELY);
        when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(RUN_IMMEDIATELY);
    }
}
