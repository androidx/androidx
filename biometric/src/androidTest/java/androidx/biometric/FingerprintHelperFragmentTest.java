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
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
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

import java.util.ArrayList;
import java.util.List;
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

    /**
     * A version of {@link androidx.biometric.FingerprintHelperFragment.MessageRouter} that doesn't
     * forward any messages to its associated {@link Handler}.
     */
    private static class NoOpMessageRouter extends FingerprintHelperFragment.MessageRouter {
        NoOpMessageRouter(Handler handler) {
            super(handler);
        }

        @Override
        void sendMessage(int what) {
            // No-op
        }

        @Override
        void sendMessage(int what, Object obj) {
            // No-op
        }

        @Override
        void sendMessage(int what, int arg1, int arg2, Object obj) {
            // No-op
        }
    }

    /**
     * A testable version of {@link androidx.biometric.FingerprintHelperFragment.MessageRouter} that
     * keeps track of message IDs as they're sent through to the associated {@link Handler}.
     */
    private static class TestableMessageRouter extends FingerprintHelperFragment.MessageRouter {
        private List<Integer> mReceivedMessages;

        TestableMessageRouter(Handler handler) {
            super(handler);
            mReceivedMessages = new ArrayList<>();
        }

        @Override
        void sendMessage(int what) {
            super.sendMessage(what);
            mReceivedMessages.add(what);
        }

        @Override
        void sendMessage(int what, Object obj) {
            super.sendMessage(what, obj);
            mReceivedMessages.add(what);
        }

        @Override
        void sendMessage(int what, int arg1, int arg2, Object obj) {
            super.sendMessage(what, arg1, arg2, obj);
            mReceivedMessages.add(what);
        }

        List<Integer> getReceivedMessages() {
            return mReceivedMessages;
        }
    }

    @Mock
    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback;
    @Mock
    private Handler mHandler;

    @Captor
    private ArgumentCaptor<BiometricPrompt.AuthenticationResult> mResultCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Force Handler#postDelayed(Runnable, long) callbacks to run immediately.
        when(mHandler.sendMessageAtTime(any(Message.class), anyLong())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                final Message msg = invocation.getArgument(0);
                final Runnable callback = msg.getCallback();
                if (callback != null) {
                    callback.run();
                }
                return null;
            }
        });
    }

    @Test
    @UiThreadTest
    public void testOnAuthenticationSucceeded_TriggersCallbackWithNullCrypto_WhenGivenNullResult() {
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        helperFragment.setCallback(EXECUTOR, mAuthenticationCallback);
        helperFragment.setHandler(mHandler);
        helperFragment.setMessageRouter(new NoOpMessageRouter(mHandler));

        helperFragment.mAuthenticationCallback.onAuthenticationSucceeded(null /* result */);

        verify(mAuthenticationCallback).onAuthenticationSucceeded(mResultCaptor.capture());
        assertThat(mResultCaptor.getValue().getCryptoObject()).isNull();
    }

    @Test
    @UiThreadTest
    public void testOnAuthenticationError_DoesShowErrorAndDismissDialog_WhenHardwareUnavailable() {
        final FingerprintHelperFragment helperFragment = FingerprintHelperFragment.newInstance();
        final TestableMessageRouter messageRouter = new TestableMessageRouter(mHandler);
        helperFragment.setCallback(EXECUTOR, mAuthenticationCallback);
        helperFragment.setHandler(mHandler);
        helperFragment.setMessageRouter(messageRouter);

        final int errMsgId = BiometricConstants.ERROR_HW_UNAVAILABLE;
        final String errString = "lorem ipsum";
        helperFragment.mAuthenticationCallback.onAuthenticationError(errMsgId, errString);

        assertThat(messageRouter.getReceivedMessages()).containsExactly(
                FingerprintDialogFragment.MSG_SHOW_ERROR,
                FingerprintDialogFragment.MSG_DISMISS_DIALOG_ERROR);
        verify(mAuthenticationCallback).onAuthenticationError(errMsgId, errString);
    }
}
