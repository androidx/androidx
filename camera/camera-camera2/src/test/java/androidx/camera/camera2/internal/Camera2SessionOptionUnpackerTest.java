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

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Size;

import androidx.annotation.OptIn;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Config.OptionPriority;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.impl.VideoCaptureConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        instrumentedPackages = { "androidx.camera.camera2.impl" })
public final class Camera2SessionOptionUnpackerTest {

    private static final Size RESOLUTION_HD = new Size(1280, 720);
    private static final Size RESOLUTION_VGA = new Size(640, 480);

    private Camera2SessionOptionUnpacker mUnpacker;

    @Before
    public void setUp() {
        mUnpacker = Camera2SessionOptionUnpacker.INSTANCE;
    }

    @Test
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public void unpackerExtractsInteropCallbacks() {
        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder();
        CaptureCallback captureCallback = mock(CaptureCallback.class);
        CameraDevice.StateCallback deviceCallback = mock(CameraDevice.StateCallback.class);
        CameraCaptureSession.StateCallback sessionStateCallback =
                mock(CameraCaptureSession.StateCallback.class);
        new Camera2Interop.Extender<>(imageCaptureBuilder)
                .setSessionCaptureCallback(captureCallback)
                .setDeviceStateCallback(deviceCallback)
                .setSessionStateCallback(sessionStateCallback);
        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_VGA, imageCaptureBuilder.getUseCaseConfig(), sessionBuilder);
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
    }

    @Test
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public void unpackerExtractsOptions() {
        ImageCapture.Builder imageCaptureConfigBuilder = new ImageCapture.Builder();

        // Add 2 options to ensure that multiple options can be unpacked.
        new Camera2Interop.Extender<>(imageCaptureConfigBuilder)
                .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                .setCaptureRequestOption(
                        CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        ImageCaptureConfig useCaseConfig = imageCaptureConfigBuilder.getUseCaseConfig();

        OptionPriority priorityAfMode = getCaptureRequestOptionPriority(useCaseConfig,
                CaptureRequest.CONTROL_AF_MODE);
        OptionPriority priorityFlashMode = getCaptureRequestOptionPriority(useCaseConfig,
                CaptureRequest.FLASH_MODE);

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_VGA, useCaseConfig, sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        Camera2ImplConfig config = new Camera2ImplConfig(sessionConfig.getImplementationOptions());

        assertThat(config.getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO);
        assertThat(config.getCaptureRequestOption(
                CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF))
                .isEqualTo(CaptureRequest.FLASH_MODE_TORCH);

        // Make sures the priority of Camera2Interop is preserved after unpacking.
        assertThat(getCaptureRequestOptionPriority(config, CaptureRequest.CONTROL_AF_MODE))
                .isEqualTo(priorityAfMode);
        assertThat(getCaptureRequestOptionPriority(config, CaptureRequest.CONTROL_AF_MODE))
                .isEqualTo(priorityFlashMode);

        assertThat(config.getCaptureRequestOption(CaptureRequest.TONEMAP_MODE)).isNull();
    }

    @Test
    public void unpackerExtractsOptionsForPreviewResolution16x9() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Google");
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "sunfish");

        Preview.Builder previewConfigBuilder = new Preview.Builder();

        PreviewConfig useCaseConfig = previewConfigBuilder.getUseCaseConfig();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_HD, useCaseConfig, sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        Camera2ImplConfig config = new Camera2ImplConfig(sessionConfig.getImplementationOptions());

        assertThat(config.getCaptureRequestOption(CaptureRequest.TONEMAP_MODE)).isNull();
    }

    @Test
    public void unpackerExtractsOptionsForPreviewResolution4x3() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Google");
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "sunfish");

        Preview.Builder previewConfigBuilder = new Preview.Builder();

        PreviewConfig useCaseConfig = previewConfigBuilder.getUseCaseConfig();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_VGA, useCaseConfig, sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        Camera2ImplConfig config = new Camera2ImplConfig(sessionConfig.getImplementationOptions());

        assertThat(config.getCaptureRequestOption(CaptureRequest.TONEMAP_MODE))
                .isEqualTo(CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
    }

    @Test
    public void unpackerExtractsPreviewStabilizationMode() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Google");
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "sunfish");

        Preview.Builder previewConfigBuilder =
                new Preview.Builder().setPreviewStabilizationEnabled(true);

        PreviewConfig useCaseConfig = previewConfigBuilder.getUseCaseConfig();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_VGA, useCaseConfig, sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();

        assertThat(captureConfig.getVideoStabilizationMode())
                .isEqualTo(StabilizationMode.UNSPECIFIED);
        assertThat(captureConfig.getPreviewStabilizationMode())
                .isEqualTo(StabilizationMode.ON);
    }

    @Test
    public void unpackerExtractsVideoStabilizationMode() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Google");
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "sunfish");

        VideoCapture.Builder<Recorder> videoCaptureConfigBuilder =
                new VideoCapture.Builder<>(new Recorder.Builder().build())
                        .setVideoStabilizationEnabled(true);

        VideoCaptureConfig<Recorder> useCaseConfig = videoCaptureConfigBuilder.getUseCaseConfig();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_VGA, useCaseConfig, sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();

        CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();

        assertThat(captureConfig.getVideoStabilizationMode())
                .isEqualTo(StabilizationMode.ON);
        assertThat(captureConfig.getPreviewStabilizationMode())
                .isEqualTo(StabilizationMode.UNSPECIFIED);
    }

    @Test
    public void unpackerExtractsBothPreviewAndVideoStabilizationMode() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Google");
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "sunfish");

        // unpack for preview
        Preview.Builder previewConfigBuilder =
                new Preview.Builder().setPreviewStabilizationEnabled(true);

        PreviewConfig previewConfig = previewConfigBuilder.getUseCaseConfig();

        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        mUnpacker.unpack(RESOLUTION_VGA, previewConfig, sessionBuilder);

        // unpack for preview
        VideoCapture.Builder<Recorder> videoCaptureConfigBuilder =
                new VideoCapture.Builder<>(new Recorder.Builder().build())
                        .setVideoStabilizationEnabled(true);

        VideoCaptureConfig<Recorder> videoCaptureConfig =
                videoCaptureConfigBuilder.getUseCaseConfig();

        mUnpacker.unpack(RESOLUTION_VGA, videoCaptureConfig, sessionBuilder);
        SessionConfig sessionConfig = sessionBuilder.build();
        CaptureConfig captureConfig = sessionConfig.getRepeatingCaptureConfig();

        assertThat(captureConfig.getVideoStabilizationMode())
                .isEqualTo(StabilizationMode.ON);
        assertThat(captureConfig.getPreviewStabilizationMode())
                .isEqualTo(StabilizationMode.ON);
    }

    private OptionPriority getCaptureRequestOptionPriority(Config config,
            CaptureRequest.Key<?> key) {
        Config.Option<?> option = Camera2ImplConfig.createCaptureRequestOption(key);
        return config.getOptionPriority(option);
    }
}
