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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;

/**
 * Convenience class for generating a fake {@link CameraXConfig}.
 *
 * <p>This {@link CameraXConfig} contains all fake CameraX implementation components.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class FakeAppConfig {
    private FakeAppConfig() {
    }

    private static final String CAMERA_ID_0 = "0";
    private static final String CAMERA_ID_1 = "1";

    /** Generates a fake {@link CameraXConfig}. */
    @NonNull
    public static CameraXConfig create() {
        return create(null);
    }

    /**
     * Generates a fake {@link CameraXConfig} with the provided {@linkplain CameraSelector
     * available cameras limiter}.
     */
    @NonNull
    public static CameraXConfig create(@Nullable CameraSelector availableCamerasSelector) {
        final CameraFactory.Provider cameraFactoryProvider = (ignored1, ignored2, ignored3) -> {
            final FakeCameraFactory cameraFactory = new FakeCameraFactory(availableCamerasSelector);
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

        final CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                (ignored1, ignored2, ignored3) -> new FakeCameraDeviceSurfaceManager();

        final CameraXConfig.Builder appConfigBuilder = new CameraXConfig.Builder()
                .setCameraFactoryProvider(cameraFactoryProvider)
                .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
                .setUseCaseConfigFactoryProvider(ignored -> new FakeUseCaseConfigFactory());

        if (availableCamerasSelector != null) {
            appConfigBuilder.setAvailableCamerasLimiter(availableCamerasSelector);
        }

        return appConfigBuilder.build();
    }

    /** @hide */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class DefaultProvider implements CameraXConfig.Provider {

        @NonNull
        @Override
        public CameraXConfig getCameraXConfig() {
            return create();
        }
    }
}
