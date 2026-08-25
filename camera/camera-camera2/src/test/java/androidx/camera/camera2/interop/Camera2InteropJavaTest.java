/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.interop;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Build;

import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.test.filters.SdkSuppress;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Unit tests to verify Camera2Interop usage from Java callers and ensure no ambiguous method
 * overloads.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.N)
public class Camera2InteropJavaTest {

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void canCallSetPhysicalCameraIdFromJava() {
        Preview.Builder builder = new Preview.Builder();
        builder.setInterop(Camera2Interop.forUseCase(interop -> {
            interop.setPhysicalCameraId("0");
            interop.setSurfaceGroupId(1);
        }));

        Camera2ImplConfig config = new Camera2ImplConfig(builder.getInteropMutableConfig());
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("0");
        assertThat(config.getSurfaceGroupId(-1)).isEqualTo(1);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void canCallSensorPixelModesUsedFromJava() {
        Preview.Builder builder = new Preview.Builder();
        builder.setInterop(Camera2Interop.forUseCase(interop -> {
            interop.addSensorPixelModeUsed(1);
            interop.setSensorPixelModesUsed(java.util.Collections.singleton(2));
        }));

        Camera2ImplConfig config = new Camera2ImplConfig(builder.getInteropMutableConfig());
        assertThat(config.getSensorPixelModesUsed(null)).containsExactly(2);
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    public void canCallApi33OutputConfigurationMethodsFromJava() {
        Preview.Builder builder = new Preview.Builder();
        builder.setInterop(Camera2Interop.forUseCase(interop -> {
            interop.setStreamUseCase(3L);
            interop.setMirrorMode(OutputConfiguration.MIRROR_MODE_H);
            interop.setTimestampBase(OutputConfiguration.TIMESTAMP_BASE_SENSOR);
            interop.setDynamicRangeProfile(1L);
        }));

        Camera2ImplConfig config = new Camera2ImplConfig(builder.getInteropMutableConfig());
        assertThat(config.getStreamUseCase(-1L)).isEqualTo(3L);
        assertThat(config.getMirrorMode(-1)).isEqualTo(OutputConfiguration.MIRROR_MODE_H);
        assertThat(config.getTimestampBase(-1)).isEqualTo(
                OutputConfiguration.TIMESTAMP_BASE_SENSOR);
        assertThat(config.getDynamicRangeProfile(-1L)).isEqualTo(1L);
    }

    @Test
    public void canCallImageCaptureInteropMethodsFromJava() {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        CameraCaptureSession.CaptureCallback callback =
                new CameraCaptureSession.CaptureCallback() {
                };

        builder.setInterop(Camera2Interop.forImageCapture(interop -> {
            interop.setStillCaptureRequestTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);
            interop.setStillCaptureCallback(callback);
        }));

        Camera2ImplConfig config = new Camera2ImplConfig(builder.getUseCaseConfig());
        assertThat(config.getStillCaptureTemplateType(-1))
                .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE);
        assertThat(config.retrieveOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, null))
                .isNotNull();
    }

    @Test
    public void canCallSessionConfigInteropMethodsFromJava() {
        CameraDevice.StateCallback deviceCallback =
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                    }
                };
        CameraCaptureSession.StateCallback sessionCallback =
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                };

        Preview preview = new Preview.Builder().build();
        androidx.camera.core.SessionConfig.Builder sessionConfigBuilder =
                new androidx.camera.core.SessionConfig.Builder(preview);

        sessionConfigBuilder.setInterop(Camera2Interop.forSessionConfig(interop -> {
            interop.setRepeatingCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW);
            interop.setDeviceStateCallback(deviceCallback);
            interop.setSessionStateCallback(sessionCallback);
        }));

        androidx.camera.core.SessionConfig sessionConfig = sessionConfigBuilder.build();
        Camera2ImplConfig config = new Camera2ImplConfig(sessionConfig.getInteropConfig());
        assertThat(config.getCaptureRequestTemplate(-1)).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
        assertThat(config.getDeviceStateCallback(null)).isNotNull();
        assertThat(config.getSessionStateCallback(null)).isNotNull();
    }
}
