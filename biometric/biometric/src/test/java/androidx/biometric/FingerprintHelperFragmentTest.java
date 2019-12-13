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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
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
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executor;

@LargeTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class FingerprintHelperFragmentTest {
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
    @Mock
    private Message mMessage;

    @Captor
    private ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        prepareMockHandler(mHandler, mMessage);
    }

    @Test
    public void testOnAuthenticationSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        helperFragment.setCallback(EXECUTOR, mAuthenticationCallback);
        helperFragment.setHandler(mHandler);

        helperFragment.mAuthenticationCallback.onAuthenticationSucceeded(null /* result */);

        verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
    }

    @Test
    public void testOnAuthenticationError_DoesShowErrorAndDismissDialog_WhenHardwareUnavailable() {
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        helperFragment.setCallback(EXECUTOR, mAuthenticationCallback);
        helperFragment.setHandler(mHandler);

        final int errMsgId = BiometricConstants.ERROR_HW_UNAVAILABLE;
        final String errString = "lorem ipsum";
        helperFragment.mAuthenticationCallback.onAuthenticationError(errMsgId, errString);

        verify(mHandler).obtainMessage(eq(FingerprintDialogFragment.MSG_SHOW_ERROR),
                eq(errMsgId), anyInt(), eq(errString));
        verify(mHandler).obtainMessage(FingerprintDialogFragment.MSG_DISMISS_DIALOG_ERROR);
        verify(mMessage, times(2)).sendToTarget();
        verify(mAuthenticationCallback).onAuthenticationError(errMsgId, errString);
    }

    private static void prepareMockHandler(Handler handler, Message message) {
        when(handler.obtainMessage(anyInt())).thenReturn(message);
        when(handler.obtainMessage(anyInt(), any())).thenReturn(message);
        when(handler.obtainMessage(anyInt(), anyInt(), anyInt())).thenReturn(message);
        when(handler.obtainMessage(anyInt(), anyInt(), anyInt(), any())).thenReturn(message);
        when(handler.post(any(Runnable.class))).thenAnswer(RUN_IMMEDIATELY);
        when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(RUN_IMMEDIATELY);
    }
}
