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

package androidx.camera.core.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.camera.core.impl.CameraCaptureCallbacks.NoOpCameraCaptureCallback;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class CameraCaptureCallbacksTest {

    @Test
    public void comboCallbackInvokesConstituentCallbacks() {
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback comboCallback =
                CameraCaptureCallbacks.createComboCallback(callback0, callback1);
        CameraCaptureResult result = mock(CameraCaptureResult.class);
        CameraCaptureFailure failure = new CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR);

        int captureConfigId = 10;
        comboCallback.onCaptureCompleted(captureConfigId, result);
        verify(callback0, times(1)).onCaptureCompleted(captureConfigId, result);
        verify(callback1, times(1)).onCaptureCompleted(captureConfigId, result);

        comboCallback.onCaptureFailed(captureConfigId, failure);
        verify(callback0, times(1)).onCaptureFailed(captureConfigId, failure);
        verify(callback1, times(1)).onCaptureFailed(captureConfigId, failure);

        comboCallback.onCaptureCancelled(captureConfigId);
        verify(callback0, times(1)).onCaptureCancelled(captureConfigId);
        verify(callback1, times(1)).onCaptureCancelled(captureConfigId);

        comboCallback.onCaptureStarted(captureConfigId);
        verify(callback0, times(1)).onCaptureStarted(captureConfigId);
        verify(callback1, times(1)).onCaptureStarted(captureConfigId);

        comboCallback.onCaptureProcessProgressed(captureConfigId, 100);
        verify(callback0, times(1)).onCaptureProcessProgressed(captureConfigId, 100);
        verify(callback1, times(1)).onCaptureProcessProgressed(captureConfigId, 100);
    }

    @Test
    public void comboCallbackOnSingle_returnsSingle() {
        CameraCaptureCallback callback = mock(CameraCaptureCallback.class);

        CameraCaptureCallback returnCallback = CameraCaptureCallbacks.createComboCallback(callback);

        assertThat(returnCallback).isEqualTo(callback);
    }

    @Test
    public void comboCallbackOnEmpty_returnsNoOp() {
        CameraCaptureCallback callback = CameraCaptureCallbacks.createComboCallback();

        assertThat(callback).isInstanceOf(NoOpCameraCaptureCallback.class);
    }
}
