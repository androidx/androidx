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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class Camera2CaptureCallbacksTest {

    @Test
    public void comboCallbackInvokesConstituentCallbacks() {
        CameraCaptureSession.CaptureCallback callback0 =
                Mockito.mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureSession.CaptureCallback callback1 =
                Mockito.mock(CameraCaptureSession.CaptureCallback.class);
        CameraCaptureSession.CaptureCallback comboCallback =
                Camera2CaptureCallbacks.createComboCallback(callback0, callback1);
        CameraCaptureSession session = Mockito.mock(CameraCaptureSession.class);
        CaptureResult result = Mockito.mock(CaptureResult.class);
        CaptureFailure failure = Mockito.mock(CaptureFailure.class);
        Surface surface = Mockito.mock(Surface.class);
        // CaptureRequest, TotalCaptureResult are final classes which cannot be mocked, and it is
        // difficult to create fake instances without an actual Camera2 pipeline. Use null as a
        // placeholder.
        CaptureRequest request = null;
        TotalCaptureResult totalResult = null;

        if (Build.VERSION.SDK_INT >= 24) {
            comboCallback.onCaptureBufferLost(session, request, surface, 1L);
            verify(callback0, times(1)).onCaptureBufferLost(session, request, surface, 1L);
            verify(callback1, times(1)).onCaptureBufferLost(session, request, surface, 1L);
        }

        comboCallback.onCaptureCompleted(session, request, totalResult);
        verify(callback0, times(1)).onCaptureCompleted(session, request, totalResult);
        verify(callback1, times(1)).onCaptureCompleted(session, request, totalResult);

        comboCallback.onCaptureFailed(session, request, failure);
        verify(callback0, times(1)).onCaptureFailed(session, request, failure);
        verify(callback1, times(1)).onCaptureFailed(session, request, failure);

        comboCallback.onCaptureProgressed(session, request, result);
        verify(callback0, times(1)).onCaptureProgressed(session, request, result);
        verify(callback1, times(1)).onCaptureProgressed(session, request, result);

        comboCallback.onCaptureSequenceAborted(session, 1);
        verify(callback0, times(1)).onCaptureSequenceAborted(session, 1);
        verify(callback1, times(1)).onCaptureSequenceAborted(session, 1);

        comboCallback.onCaptureSequenceCompleted(session, 1, 123L);
        verify(callback0, times(1)).onCaptureSequenceCompleted(session, 1, 123L);
        verify(callback1, times(1)).onCaptureSequenceCompleted(session, 1, 123L);

        comboCallback.onCaptureStarted(session, request, 123L, 1L);
        verify(callback0, times(1)).onCaptureStarted(session, request, 123L, 1L);
        verify(callback1, times(1)).onCaptureStarted(session, request, 123L, 1L);
    }
}
