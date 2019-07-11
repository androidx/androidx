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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.SessionConfig;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class Camera2SessionOptionUnpackerTest {

    private Camera2SessionOptionUnpacker mUnpacker;

    @Before
    public void setUp() {
        mUnpacker = Camera2SessionOptionUnpacker.INSTANCE;
    }

    @Test
    public void unpackerExtractsInteropCallbacks() {
        ImageCaptureConfig.Builder imageCaptureConfigBuilder = new ImageCaptureConfig.Builder();
        CaptureCallback captureCallback = mock(CaptureCallback.class);
        CameraDevice.StateCallback deviceCallback = mock(CameraDevice.StateCallback.class);
        CameraCaptureSession.StateCallback sessionStateCallback =
                mock(CameraCaptureSession.StateCallback.class);
        CameraEventCallbacks cameraEventCallbacks = mock(CameraEventCallbacks.class);
        when(cameraEventCallbacks.clone()).thenReturn(cameraEventCallbacks);

        new Camera2Config.Extender(imageCaptureConfigBuilder)
                .setSessionCaptureCallback(captureCallback)
                .setDeviceStateCallback(deviceCallback)
                .setSessionStateCallback(sessionStateCallback)
                .setCameraEventCallback(cameraEventCallbacks);

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(imageCaptureConfigBuilder.build(), sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        CameraCaptureCallback interopCallback =
                sessionConfig.getSingleCameraCaptureCallbacks().get(0);
        assertThat(((CaptureCallbackContainer) interopCallback).getCaptureCallback())
                .isEqualTo(captureCallback);
        assertThat(sessionConfig.getSingleCameraCaptureCallbacks())
                .containsExactly(interopCallback);
        assertThat(sessionConfig.getRepeatingCameraCaptureCallbacks())
                .containsExactly(interopCallback);
        assertThat(sessionConfig.getDeviceStateCallbacks()).containsExactly(deviceCallback);
        assertThat(sessionConfig.getSessionStateCallbacks())
                .containsExactly(sessionStateCallback);
        assertThat(
                new Camera2Config(sessionConfig.getImplementationOptions()).getCameraEventCallback(
                        null)).isEqualTo(cameraEventCallbacks);
    }

    @Test
    public void unpackerExtractsOptions() {
        ImageCaptureConfig.Builder imageCaptureConfigBuilder = new ImageCaptureConfig.Builder();

        // Add 2 options to ensure that multiple options can be unpacked.
        new Camera2Config.Extender(imageCaptureConfigBuilder)
                .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                .setCaptureRequestOption(
                        CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(imageCaptureConfigBuilder.build(), sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        Camera2Config config = new Camera2Config(sessionConfig.getImplementationOptions());

        assertThat(config.getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO);
        assertThat(config.getCaptureRequestOption(
                CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF))
                .isEqualTo(CaptureRequest.FLASH_MODE_TORCH);
    }
}
