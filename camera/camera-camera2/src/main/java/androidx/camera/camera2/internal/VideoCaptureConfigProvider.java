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

import static androidx.camera.core.impl.ImageOutputConfig.DEFAULT_ASPECT_RATIO_LANDSCAPE;
import static androidx.camera.core.impl.ImageOutputConfig.DEFAULT_ASPECT_RATIO_PORTRAIT;

import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.VideoCaptureConfig;

/**
 * Provides defaults for {@link VideoCaptureConfig} in the Camera2 implementation.
 */
public final class VideoCaptureConfigProvider implements ConfigProvider<VideoCaptureConfig> {
    private static final String TAG = "VideoCaptureProvider";

    private final WindowManager mWindowManager;

    public VideoCaptureConfigProvider(@NonNull Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    @NonNull
    public VideoCaptureConfig getConfig(@Nullable CameraInfo cameraInfo) {
        VideoCaptureConfig.Builder builder =
                VideoCaptureConfig.Builder.fromConfig(
                        VideoCapture.DEFAULT_CONFIG.getConfig(cameraInfo));

        // SessionConfig containing all intrinsic properties needed for VideoCapture
        SessionConfig.Builder sessionBuilder = new SessionConfig.Builder();
        // TODO(b/114762170): Must set to preview here until we allow for multiple template types
        sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        // Add options to UseCaseConfig
        builder.setDefaultSessionConfig(sessionBuilder.build());
        builder.setSessionOptionUnpacker(Camera2SessionOptionUnpacker.INSTANCE);

        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        captureBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder.setDefaultCaptureConfig(captureBuilder.build());
        builder.setCaptureOptionUnpacker(Camera2CaptureOptionUnpacker.INSTANCE);

        int targetRotation = mWindowManager.getDefaultDisplay().getRotation();
        builder.setTargetRotation(targetRotation);

        // Add options that requires camera info to UseCaseConfig
        if (cameraInfo != null) {
            int rotationDegrees = cameraInfo.getSensorRotationDegrees(targetRotation);
            boolean isRotateNeeded = (rotationDegrees == 90 || rotationDegrees == 270);
            builder.setTargetAspectRatioCustom(isRotateNeeded ? DEFAULT_ASPECT_RATIO_PORTRAIT
                    : DEFAULT_ASPECT_RATIO_LANDSCAPE);
        }

        return builder.getUseCaseConfig();
    }
}
