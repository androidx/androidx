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

package androidx.camera.camera2.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SynchronizedCaptureSessionStateCallbackTest {
    private CameraCaptureSession.StateCallback mMockCameraCaptureSessionStateCallback;
    private SynchronizedCaptureSession.StateCallback mMockStateCallback;
    private SynchronizedCaptureSession.StateCallback mStateCallback;
    private ScheduledExecutorService mScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private SynchronizedCaptureSessionBaseImpl mCaptureSessionCompatBaseImpl;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        mMockCameraCaptureSessionStateCallback = mock(CameraCaptureSession.StateCallback.class);
        mMockStateCallback = mock(SynchronizedCaptureSession.StateCallback.class);

        mCaptureSessionCompatBaseImpl = new SynchronizedCaptureSessionBaseImpl(
                mock(CaptureSessionRepository.class), android.os.AsyncTask.THREAD_POOL_EXECUTOR,
                mScheduledExecutorService, mock(Handler.class));
        mCaptureSessionCompatBaseImpl.createCaptureSessionCompat(mock(CameraCaptureSession.class));

        mStateCallback = SynchronizedCaptureSessionStateCallbacks.createComboCallback(
                mMockStateCallback,
                new SynchronizedCaptureSessionStateCallbacks.Adapter(
                        mMockCameraCaptureSessionStateCallback));
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    public void forwardCallbacks_afterO() {
        mStateCallback.onCaptureQueueEmpty(mCaptureSessionCompatBaseImpl);
        verify(mMockStateCallback).onCaptureQueueEmpty(any());
        verify(mMockCameraCaptureSessionStateCallback).onCaptureQueueEmpty(any());
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.M)
    public void forwardCallbacks_afterM() {

        mStateCallback.onSurfacePrepared(mCaptureSessionCompatBaseImpl, mock(Surface.class));
        verify(mMockStateCallback).onSurfacePrepared(any(), any(Surface.class));
        verify(mMockCameraCaptureSessionStateCallback).onSurfacePrepared(any(), any(Surface.class));
    }

    @Test
    public void forwardCallbacks_onActive() {
        mStateCallback.onActive(mCaptureSessionCompatBaseImpl);
        verify(mMockStateCallback).onActive(any());
        verify(mMockCameraCaptureSessionStateCallback).onActive(any());
    }

    @Test
    public void forwardCallbacks_onReady() {
        mStateCallback.onReady(mCaptureSessionCompatBaseImpl);
        verify(mMockStateCallback).onReady(any());
        verify(mMockCameraCaptureSessionStateCallback).onReady(any());
    }

    @Test
    public void forwardCallbacks_onConfigureFailed() {
        mStateCallback.onConfigureFailed(mCaptureSessionCompatBaseImpl);
        verify(mMockStateCallback).onConfigureFailed(any());
        verify(mMockCameraCaptureSessionStateCallback).onConfigureFailed(any());
    }

    @Test
    public void forwardCallbacks_onConfigured() {
        mStateCallback.onConfigured(mCaptureSessionCompatBaseImpl);
        verify(mMockStateCallback).onConfigured(any());
        verify(mMockCameraCaptureSessionStateCallback).onConfigured(any());
    }

    @Test
    public void forwardCallbacks_onClosed() {
        mStateCallback.onClosed(mCaptureSessionCompatBaseImpl);
        verify(mMockStateCallback).onClosed(any());
        verify(mMockCameraCaptureSessionStateCallback).onClosed(any());
    }
}
