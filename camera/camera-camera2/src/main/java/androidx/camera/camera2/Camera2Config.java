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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.camera2.internal.Camera2CameraFactory;
import androidx.camera.camera2.internal.Camera2DeviceSurfaceManager;
import androidx.camera.camera2.internal.ImageAnalysisConfigProvider;
import androidx.camera.camera2.internal.ImageCaptureConfigProvider;
import androidx.camera.camera2.internal.PreviewConfigProvider;
import androidx.camera.camera2.internal.VideoCaptureConfigProvider;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.InitializationException;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.ExtendableUseCaseConfigFactory;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.VideoCaptureConfig;

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
    public static CameraXConfig defaultConfig() {

        // Create the camera factory for creating Camera2 camera objects
        CameraFactory.Provider cameraFactoryProvider = Camera2CameraFactory::new;

        // Create the DeviceSurfaceManager for Camera2
        CameraDeviceSurfaceManager.Provider surfaceManagerProvider = context -> {
            try {
                return new Camera2DeviceSurfaceManager(context);
            } catch (CameraUnavailableException e) {
                throw new InitializationException(e);
            }
        };

        // Create default configuration factory
        UseCaseConfigFactory.Provider configFactoryProvider = context -> {
            ExtendableUseCaseConfigFactory factory = new ExtendableUseCaseConfigFactory();
            factory.installDefaultProvider(
                    ImageAnalysisConfig.class, new ImageAnalysisConfigProvider(context));
            factory.installDefaultProvider(
                    ImageCaptureConfig.class, new ImageCaptureConfigProvider(context));
            factory.installDefaultProvider(
                    VideoCaptureConfig.class, new VideoCaptureConfigProvider(context));
            factory.installDefaultProvider(
                    PreviewConfig.class, new PreviewConfigProvider(context));
            return factory;
        };

        CameraXConfig.Builder appConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactoryProvider(cameraFactoryProvider)
                        .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
                        .setUseCaseConfigFactoryProvider(configFactoryProvider);

        return appConfigBuilder.build();
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final class DefaultProvider implements CameraXConfig.Provider {

        @NonNull
        @Override
        public CameraXConfig getCameraXConfig() {
            return defaultConfig();
        }
    }


}
