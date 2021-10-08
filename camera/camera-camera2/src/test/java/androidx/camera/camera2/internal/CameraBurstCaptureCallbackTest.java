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

package androidx.camera.camera2.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.view.Surface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class CameraBurstCaptureCallbackTest {
    private CameraCaptureSession mSession;
    private CaptureRequest mRequest0;
    private CaptureRequest mRequest1;
    private TotalCaptureResult mResult;
    private CaptureFailure mFailure;

    @Before
    public void setUp() {
        mSession = mock(CameraCaptureSession.class);
        mRequest0 = mock(CaptureRequest.class);
        mRequest1 = mock(CaptureRequest.class);
        mResult = mock(TotalCaptureResult.class);
        mFailure = mock(CaptureFailure.class);
    }

    @Test
    public void addCamera2Callbacks_withSameRequest() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks = new ArrayList<>();
        captureCallbacks.add(captureCallback0);
        captureCallbacks.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(captureCallback0).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(captureCallback1).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCamera2Callbacks_withSameRequestSeparately() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks1);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest0, mResult);

        verify(captureCallback0).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(captureCallback1).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCamera2Callbacks_withDifferentRequest_doesNotCall() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks1);

        burstCaptureCallback.onCaptureCompleted(mSession, mRequest1, mResult);

        verify(captureCallback0, never()).onCaptureCompleted(mSession, mRequest0, mResult);
        verify(captureCallback1, never()).onCaptureCompleted(mSession, mRequest0, mResult);
    }

    @Test
    public void addCamera2Callbacks_onCaptureFailedIsCalled() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest1, captureCallbacks1);

        burstCaptureCallback.onCaptureFailed(mSession, mRequest0, mFailure);

        verify(captureCallback0).onCaptureFailed(mSession, mRequest0, mFailure);
        verify(captureCallback1, never()).onCaptureFailed(mSession, mRequest0, mFailure);
    }

    @Test
    public void addCamera2Callbacks_onCaptureProgressedIsCalled() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest1, captureCallbacks1);

        burstCaptureCallback.onCaptureProgressed(mSession, mRequest0, mResult);

        verify(captureCallback0).onCaptureProgressed(mSession, mRequest0, mResult);
        verify(captureCallback1, never()).onCaptureProgressed(mSession, mRequest0, mResult);
    }

    @Test
    public void addCamera2Callbacks_onCaptureStartedIsCalled() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest1, captureCallbacks1);

        long timestamp = 0;
        long frame = 1;
        burstCaptureCallback.onCaptureStarted(mSession, mRequest0, timestamp, frame);
        verify(captureCallback0).onCaptureStarted(mSession, mRequest0, timestamp, frame);
        verify(captureCallback1, never()).onCaptureStarted(mSession, mRequest0, timestamp, frame);
    }

    @Config(minSdk = Build.VERSION_CODES.N)
    @Test
    public void addCamera2Callbacks_onCaptureBufferLostIsCalled() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest1, captureCallbacks1);

        Surface surface = mock(Surface.class);
        long frame = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            burstCaptureCallback.onCaptureBufferLost(mSession, mRequest0, surface, frame);

            verify(captureCallback0).onCaptureBufferLost(mSession, mRequest0, surface, frame);
            verify(captureCallback1, never()).onCaptureBufferLost(mSession, mRequest0, surface,
                    frame);
        }
    }

    @Test
    public void addCamera2Callbacks_onCaptureSequenceCompletedIsCalled() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest1, captureCallbacks1);

        int sequenceId = 1;
        long frame = 1;
        burstCaptureCallback.onCaptureSequenceCompleted(mSession, sequenceId, frame);
        verify(captureCallback0).onCaptureSequenceCompleted(mSession, sequenceId, frame);
        // onCaptureSequenceCompleted is called regardless of request.
        verify(captureCallback1).onCaptureSequenceCompleted(mSession, sequenceId, frame);
    }

    @Test
    public void addCamera2Callbacks_onCaptureSequenceAbortIsCalled() {
        CameraCaptureSession.CaptureCallback captureCallback0 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks0 = new ArrayList<>();
        captureCallbacks0.add(captureCallback0);
        CameraCaptureSession.CaptureCallback captureCallback1 =
                mock(CameraCaptureSession.CaptureCallback.class);
        List<CameraCaptureSession.CaptureCallback> captureCallbacks1 = new ArrayList<>();
        captureCallbacks1.add(captureCallback1);
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.addCamera2Callbacks(mRequest0, captureCallbacks0);
        burstCaptureCallback.addCamera2Callbacks(mRequest1, captureCallbacks1);

        int sequenceId = 1;
        burstCaptureCallback.onCaptureSequenceAborted(mSession, sequenceId);
        verify(captureCallback0).onCaptureSequenceAborted(mSession, sequenceId);
        // onCaptureSequenceCompleted is called regardless of request.
        verify(captureCallback1).onCaptureSequenceAborted(mSession, sequenceId);
    }

    @Test
    public void callbackWithoutListener_doesNotThrowException() {
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        burstCaptureCallback.onCaptureStarted(mSession, mRequest0, 0, 0);
        // No listener called.
    }

    @Test
    public void captureSequenceCallback_calledWhenSequenceCompleted() {
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        CameraBurstCaptureCallback.CaptureSequenceCallback sequenceCallback = mock(
                CameraBurstCaptureCallback.CaptureSequenceCallback.class);
        burstCaptureCallback.setCaptureSequenceCallback(sequenceCallback);

        burstCaptureCallback.onCaptureSequenceCompleted(mSession, 0, 0);

        verify(sequenceCallback).onCaptureSequenceCompletedOrAborted(mSession, 0, false);
    }

    @Test
    public void captureSequenceCallback_calledWhenSequenceAborted() {
        CameraBurstCaptureCallback burstCaptureCallback = new CameraBurstCaptureCallback();
        CameraBurstCaptureCallback.CaptureSequenceCallback sequenceCallback = mock(
                CameraBurstCaptureCallback.CaptureSequenceCallback.class);
        burstCaptureCallback.setCaptureSequenceCallback(sequenceCallback);

        burstCaptureCallback.onCaptureSequenceAborted(mSession, 0);

        verify(sequenceCallback).onCaptureSequenceCompletedOrAborted(mSession, 0, true);
    }
}
