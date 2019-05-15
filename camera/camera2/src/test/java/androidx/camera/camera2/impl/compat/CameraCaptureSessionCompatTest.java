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

package androidx.camera.camera2.impl.compat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraCaptureSessionCompatTest {

    private CameraCaptureSession mCaptureSession;

    @Before
    public void setUp() {
        mCaptureSession = mock(CameraCaptureSession.class);
    }

    @Test
    @Config(maxSdk = 27)
    @SuppressWarnings("unchecked")
    public void captureBurstRequests_callsCaptureBurst() throws CameraAccessException {
        List<CaptureRequest> captureRequests = Collections.emptyList();
        CameraCaptureSessionCompat.captureBurstRequests(mCaptureSession, captureRequests,
                mock(Executor.class), mock(CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).captureBurst(any(List.class),
                any(CameraCaptureSession.CaptureCallback.class), any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    @SuppressWarnings("unchecked")
    public void captureSingleRequest_callsCaptureBurstRequests() throws CameraAccessException {
        List<CaptureRequest> captureRequests = Collections.emptyList();
        CameraCaptureSessionCompat.captureBurstRequests(mCaptureSession, captureRequests,
                mock(Executor.class), mock(CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).captureBurstRequests(any(List.class),
                any(Executor.class), any(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    @Config(maxSdk = 27)
    public void captureSingleRequest_callsCapture() throws CameraAccessException {
        CameraCaptureSessionCompat.captureSingleRequest(mCaptureSession, mock(CaptureRequest.class),
                mock(Executor.class), mock(
                        CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).capture(any(CaptureRequest.class),
                any(CameraCaptureSession.CaptureCallback.class), any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    public void captureSingleRequest_callsCaptureSingleRequest() throws CameraAccessException {
        CameraCaptureSessionCompat.captureSingleRequest(mCaptureSession, mock(CaptureRequest.class),
                mock(Executor.class), mock(
                        CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).captureSingleRequest(any(CaptureRequest.class),
                any(Executor.class), any(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    @Config(maxSdk = 27)
    @SuppressWarnings("unchecked")
    public void setRepeatingBurstRequests_callsSetRepeatingBurst() throws CameraAccessException {
        List<CaptureRequest> captureRequests = Collections.emptyList();
        CameraCaptureSessionCompat.setRepeatingBurstRequests(mCaptureSession, captureRequests,
                mock(Executor.class), mock(CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).setRepeatingBurst(any(List.class),
                any(CameraCaptureSession.CaptureCallback.class), any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    @SuppressWarnings("unchecked")
    public void setRepeatingBurstRequests_callsSetRepeatingBurstRequests()
            throws CameraAccessException {
        List<CaptureRequest> captureRequests = Collections.emptyList();
        CameraCaptureSessionCompat.setRepeatingBurstRequests(mCaptureSession, captureRequests,
                mock(Executor.class), mock(CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).setRepeatingBurstRequests(any(List.class),
                any(Executor.class), any(CameraCaptureSession.CaptureCallback.class));
    }

    @Test
    @Config(maxSdk = 27)
    public void setSingleRepeatingRequest_callsSetRepeatingRequest() throws CameraAccessException {
        CameraCaptureSessionCompat.setSingleRepeatingRequest(mCaptureSession,
                mock(CaptureRequest.class),
                mock(Executor.class), mock(
                        CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).setRepeatingRequest(any(CaptureRequest.class), any(
                CameraCaptureSession.CaptureCallback.class), any(Handler.class));
    }

    @Test
    @Config(minSdk = 28)
    public void captureSingleRequest_callsSetSingleRepeatingRequest() throws CameraAccessException {
        CameraCaptureSessionCompat.setSingleRepeatingRequest(mCaptureSession,
                mock(CaptureRequest.class),
                mock(Executor.class), mock(
                        CameraCaptureSession.CaptureCallback.class));

        verify(mCaptureSession, times(1)).setSingleRepeatingRequest(any(CaptureRequest.class),
                any(Executor.class), any(CameraCaptureSession.CaptureCallback.class));
    }
}
