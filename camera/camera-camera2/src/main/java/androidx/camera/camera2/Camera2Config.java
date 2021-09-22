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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.camera2.internal.Camera2CameraFactory;
import androidx.camera.camera2.internal.Camera2DeviceSurfaceManager;
import androidx.camera.camera2.internal.Camera2UseCaseConfigFactory;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.InitializationException;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.UseCaseConfigFactory;

/**
 * Convenience class for generating a pre-populated Camera2 {@link CameraXConfig}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
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
        CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                (context, cameraManager, availableCameraIds) -> {
                    try {
                        return new Camera2DeviceSurfaceManager(context, cameraManager,
                                availableCameraIds);
                    } catch (CameraUnavailableException e) {
                        throw new InitializationException(e);
                    }
                };

        // Create default configuration factory
        UseCaseConfigFactory.Provider configFactoryProvider =
                context -> new Camera2UseCaseConfigFactory(context);

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
