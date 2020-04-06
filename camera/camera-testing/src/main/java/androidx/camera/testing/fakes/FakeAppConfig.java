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

package androidx.camera.testing.fakes;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.ExtendableUseCaseConfigFactory;

/**
 * Convenience class for generating a fake {@link CameraXConfig}.
 *
 * <p>This {@link CameraXConfig} contains all fake CameraX implementation components.
 */
public final class FakeAppConfig {
    private FakeAppConfig() {
    }

    private static final String CAMERA_ID_0 = "0";
    private static final String CAMERA_ID_1 = "1";

    /** Generates a fake {@link CameraXConfig}. */
    @NonNull
    public static CameraXConfig create() {
        CameraFactory.Provider cameraFactoryProvider = (ignored1, ignored2) -> {
            FakeCameraFactory cameraFactory = new FakeCameraFactory();
            cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_0,
                    () -> new FakeCamera(CAMERA_ID_0, null,
                            new FakeCameraInfoInternal(CAMERA_ID_0, 0,
                                    CameraSelector.LENS_FACING_BACK)));
            cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_1,
                    () -> new FakeCamera(CAMERA_ID_1, null,
                            new FakeCameraInfoInternal(CAMERA_ID_1, 0,
                                    CameraSelector.LENS_FACING_FRONT)));
            return cameraFactory;
        };

        CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                ignored -> new FakeCameraDeviceSurfaceManager();

        CameraXConfig.Builder appConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactoryProvider(cameraFactoryProvider)
                        .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
                        .setUseCaseConfigFactoryProvider(ignored ->
                                new ExtendableUseCaseConfigFactory());

        return appConfigBuilder.build();
    }
}
