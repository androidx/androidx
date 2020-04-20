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

package androidx.camera.core;

import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * A Robolectric shadow of {@link CameraX}.
 */
@Implements(CameraX.class)
public class ShadowCameraX {
    public static final String DEFAULT_CAMERA_ID = "0";

    private static final UseCaseConfig<ImageAnalysis> DEFAULT_IMAGE_ANALYSIS_CONFIG =
            new ImageAnalysis.Builder().setSessionOptionUnpacker(
                    (config, builder) -> {
                    }).getUseCaseConfig();

    private static final UseCaseConfig<Preview> DEFAULT_PREVIEW_CONFIG =
            new Preview.Builder().setSessionOptionUnpacker(
                    (config, builder) -> {
                    }).getUseCaseConfig();

    private static final UseCaseConfig<ImageCapture> DEFAULT_IMAGE_CAPTURE_CONFIG =
            new ImageCapture.Builder().setSessionOptionUnpacker(
                    (config, builder) -> {
                    }).getUseCaseConfig();

    private static final CameraInfo DEFAULT_CAMERA_INFO = new FakeCameraInfoInternal();

    private static final CameraDeviceSurfaceManager DEFAULT_DEVICE_SURFACE_MANAGER =
            new FakeCameraDeviceSurfaceManager();

    /**
     * Shadow of {@link CameraX#getCameraInfo(String)}.
     */
    @Implementation
    public static CameraInfo getCameraInfo(String cameraId) {
        return DEFAULT_CAMERA_INFO;
    }

    /**
     * Shadow of {@link CameraX#getDefaultUseCaseConfig(Class, CameraInfo)}.
     */
    @SuppressWarnings("unchecked")
    @Implementation
    public static <C extends UseCaseConfig<?>> C getDefaultUseCaseConfig(Class<C> configType,
            @Nullable CameraInfo cameraInfo) {
        if (configType.equals(PreviewConfig.class)) {
            return (C) DEFAULT_PREVIEW_CONFIG;
        } else if (configType.equals(ImageAnalysisConfig.class)) {
            return (C) DEFAULT_IMAGE_ANALYSIS_CONFIG;
        } else if (configType.equals(ImageCaptureConfig.class)) {
            return (C) DEFAULT_IMAGE_CAPTURE_CONFIG;
        }
        throw new UnsupportedOperationException(
                "Shadow UseCase config not implemented: " + configType);
    }

    /**
     * Shadow of {@link CameraX#getSurfaceManager()}.
     */
    @Implementation
    public static CameraDeviceSurfaceManager getSurfaceManager() {
        return DEFAULT_DEVICE_SURFACE_MANAGER;
    }
}
