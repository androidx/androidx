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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2CameraFactory;
import androidx.camera.camera2.impl.Camera2DeviceSurfaceManager;
import androidx.camera.camera2.impl.ImageAnalysisConfigProvider;
import androidx.camera.camera2.impl.ImageCaptureConfigProvider;
import androidx.camera.camera2.impl.PreviewConfigProvider;
import androidx.camera.camera2.impl.VideoCaptureConfigProvider;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCaptureConfig;
import androidx.core.util.Preconditions;

/**
 * Convenience class for generating a pre-populated Camera2 {@link CameraXConfig}.
 */
public final class Camera2Config {

    private Camera2Config() {
    }

    /**
     * Creates a {@link CameraXConfig} containing the default Camera2 implementation for CameraX.
     */
    @NonNull
    public static CameraXConfig defaultConfig(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        // Create the camera factory for creating Camera2 camera objects
        CameraFactory cameraFactory = new Camera2CameraFactory(context);

        // Create the DeviceSurfaceManager for Camera2
        CameraDeviceSurfaceManager surfaceManager = new Camera2DeviceSurfaceManager(context);

        // Create default configuration factory
        ExtendableUseCaseConfigFactory configFactory = new ExtendableUseCaseConfigFactory();
        configFactory.installDefaultProvider(
                ImageAnalysisConfig.class, new ImageAnalysisConfigProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                ImageCaptureConfig.class, new ImageCaptureConfigProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                VideoCaptureConfig.class, new VideoCaptureConfigProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                PreviewConfig.class, new PreviewConfigProvider(cameraFactory, context));

        CameraXConfig.Builder appConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(configFactory);

        return appConfigBuilder.build();
    }
}
