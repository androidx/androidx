/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;

import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CameraCaptureCallbacks;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CaptureCallbackConverterAndroidTest {

    @Test
    public void toCaptureCallback() {
        CameraCaptureCallback cameraCallback = Mockito.mock(CameraCaptureCallback.class);
        CaptureCallback callback = CaptureCallbackConverter.toCaptureCallback(cameraCallback);
        callback.onCaptureCompleted(null, null, null);
        verify(cameraCallback, times(1)).onCaptureCompleted(any());
    }

    @Test
    public void toCaptureCallback_withNullArgument() {
        CaptureCallback callback = CaptureCallbackConverter.toCaptureCallback(null);
        assertThat(callback).isNull();
    }

    @Test
    public void toCaptureCallback_withCaptureCallbackContainer() {
        CaptureCallback actualCallback = Mockito.mock(CaptureCallback.class);
        CaptureCallbackContainer callbackContainer =
                CaptureCallbackContainer.create(actualCallback);
        CaptureCallback callback = CaptureCallbackConverter.toCaptureCallback(callbackContainer);
        callback.onCaptureCompleted(null, null, null);
        verify(actualCallback, times(1)).onCaptureCompleted(any(), any(), any());
    }

    @Test
    public void toCaptureCallback_withComboCameraCallback() {
        CameraCaptureCallback cameraCallback1 = Mockito.mock(CameraCaptureCallback.class);
        CameraCaptureCallback cameraCallback2 = Mockito.mock(CameraCaptureCallback.class);
        CaptureCallback cameraCallback3 = Mockito.mock(CaptureCallback.class);

        CaptureCallback callback =
                CaptureCallbackConverter.toCaptureCallback(
                        CameraCaptureCallbacks.createComboCallback(
                                cameraCallback1,
                                CameraCaptureCallbacks.createComboCallback(
                                        cameraCallback2,
                                        CaptureCallbackContainer.create(cameraCallback3))));

        callback.onCaptureCompleted(null, null, null);
        verify(cameraCallback1, times(1)).onCaptureCompleted(any());
        verify(cameraCallback2, times(1)).onCaptureCompleted(any());
        verify(cameraCallback3, times(1)).onCaptureCompleted(any(), any(), any());
    }
}
