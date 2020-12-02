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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraDevice;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class CameraDeviceStateCallbacksTest {

    @Test
    public void comboCallbackInvokesConstituentCallbacks() {
        CameraDevice.StateCallback callback0 = Mockito.mock(CameraDevice.StateCallback.class);
        CameraDevice.StateCallback callback1 = Mockito.mock(CameraDevice.StateCallback.class);
        CameraDevice.StateCallback comboCallback =
                CameraDeviceStateCallbacks.createComboCallback(callback0, callback1);
        CameraDevice device = Mockito.mock(CameraDevice.class);

        comboCallback.onOpened(device);
        verify(callback0, times(1)).onOpened(device);
        verify(callback1, times(1)).onOpened(device);

        comboCallback.onClosed(device);
        verify(callback0, times(1)).onClosed(device);
        verify(callback1, times(1)).onClosed(device);

        comboCallback.onDisconnected(device);
        verify(callback0, times(1)).onDisconnected(device);
        verify(callback1, times(1)).onDisconnected(device);

        final int error = 1;
        comboCallback.onError(device, error);
        verify(callback0, times(1)).onError(device, error);
        verify(callback1, times(1)).onError(device, error);
    }

    @Test
    public void comboCallbackOnSingle_returnsSingle() {
        CameraDevice.StateCallback callback = mock(CameraDevice.StateCallback.class);

        CameraDevice.StateCallback returnCallback =
                CameraDeviceStateCallbacks.createComboCallback(callback);

        assertThat(returnCallback).isEqualTo(callback);
    }

    @Test
    public void comboCallbackOnEmpty_returnsNoOp() {
        CameraDevice.StateCallback callback = CameraDeviceStateCallbacks.createComboCallback();

        assertThat(callback).isInstanceOf(CameraDeviceStateCallbacks.NoOpDeviceStateCallback.class);
    }
}
