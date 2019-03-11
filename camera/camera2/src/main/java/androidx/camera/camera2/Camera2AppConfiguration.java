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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.AppConfiguration;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.core.ImageAnalysisUseCaseConfiguration;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.VideoCaptureUseCaseConfiguration;
import androidx.camera.core.ViewFinderUseCaseConfiguration;

/**
 * Convenience class for generating a pre-populated Camera2 {@link AppConfiguration}.
 *
 * @hide Until CameraX.init() is made public
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class Camera2AppConfiguration {

    private Camera2AppConfiguration() {
    }

    /**
     * Creates the {@link AppConfiguration} containing the Camera2 implementation pieces for
     * CameraX.
     */
    public static AppConfiguration create(Context context) {
        // Create the camera factory for creating Camera2 camera objects
        CameraFactory cameraFactory = new Camera2CameraFactory(context);

        // Create the DeviceSurfaceManager for Camera2
        CameraDeviceSurfaceManager surfaceManager = new Camera2DeviceSurfaceManager(context);

        // Create default configuration factory
        ExtendableUseCaseConfigFactory configFactory = new ExtendableUseCaseConfigFactory();
        configFactory.installDefaultProvider(
                ImageAnalysisUseCaseConfiguration.class,
                new DefaultImageAnalysisConfigurationProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                ImageCaptureUseCaseConfiguration.class,
                new DefaultImageCaptureConfigurationProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                VideoCaptureUseCaseConfiguration.class,
                new DefaultVideoCaptureConfigurationProvider(cameraFactory, context));
        configFactory.installDefaultProvider(
                ViewFinderUseCaseConfiguration.class,
                new DefaultViewFinderConfigurationProvider(cameraFactory, context));

        AppConfiguration.Builder appConfigBuilder =
                new AppConfiguration.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(configFactory);

        return appConfigBuilder.build();
    }
}
