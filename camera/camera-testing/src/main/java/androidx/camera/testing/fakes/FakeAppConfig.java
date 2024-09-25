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
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator;
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.impl.fakes.FakeCameraFactory;
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience class for generating a fake {@link CameraXConfig}.
 *
 * <p>This {@link CameraXConfig} contains all fake CameraX implementation components.
 */
public final class FakeAppConfig {
    private FakeAppConfig() {
    }

    private static final String DEFAULT_BACK_CAMERA_ID = "0";
    private static final String DEFAULT_FRONT_CAMERA_ID = "1";

    @Nullable
    private static FakeCamera sBackCamera = null;

    @Nullable
    private static FakeCamera sFrontCamera = null;

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
        FakeCameraFactory cameraFactory = createCameraFactory(availableCamerasSelector);

        final CameraFactory.Provider cameraFactoryProvider =
                (ignored1, ignored2, ignored3, ignore4) -> cameraFactory;

        final CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                (ignored1, ignored2, ignored3) -> new FakeCameraDeviceSurfaceManager();

        final CameraXConfig.Builder appConfigBuilder = new CameraXConfig.Builder()
                .setCameraFactoryProvider(cameraFactoryProvider)
                .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
                .setUseCaseConfigFactoryProvider(ignored -> {
                    List<FakeCamera> fakeCameras = new ArrayList<>();
                    for (String cameraId : cameraFactory.getAvailableCameraIds()) {
                        fakeCameras.add((FakeCamera) cameraFactory.getCamera(cameraId));
                    }

                    return new FakeUseCaseConfigFactory(fakeCameras);
                });

        if (availableCamerasSelector != null) {
            appConfigBuilder.setAvailableCamerasLimiter(availableCamerasSelector);
        }

        return appConfigBuilder.build();
    }

    private static FakeCameraFactory createCameraFactory(
            @Nullable CameraSelector availableCamerasSelector) {
        FakeCameraFactory cameraFactory = new FakeCameraFactory(availableCamerasSelector);
        cameraFactory.insertCamera(
                CameraSelector.LENS_FACING_BACK,
                DEFAULT_BACK_CAMERA_ID,
                FakeAppConfig::getBackCamera);
        cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT,
                DEFAULT_FRONT_CAMERA_ID,
                FakeAppConfig::getFrontCamera);
        final CameraCoordinator cameraCoordinator = new FakeCameraCoordinator();
        cameraFactory.setCameraCoordinator(cameraCoordinator);
        return cameraFactory;
    }

    /**
     * Returns the default fake back camera that is used internally by CameraX.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static FakeCamera getBackCamera() {
        if (sBackCamera == null || sBackCamera.isReleased()) {
            sBackCamera = new FakeCamera(DEFAULT_BACK_CAMERA_ID, null,
                    new FakeCameraInfoInternal(DEFAULT_BACK_CAMERA_ID, 0,
                            CameraSelector.LENS_FACING_BACK));
        }
        return sBackCamera;
    }

    /**
     * Returns the default fake front camera that is used internally by CameraX.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static FakeCamera getFrontCamera() {
        if (sFrontCamera == null || sFrontCamera.isReleased()) {
            sFrontCamera = new FakeCamera(DEFAULT_FRONT_CAMERA_ID, null,
                    new FakeCameraInfoInternal(DEFAULT_FRONT_CAMERA_ID, 0,
                            CameraSelector.LENS_FACING_FRONT));
        }
        return sFrontCamera;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class DefaultProvider implements CameraXConfig.Provider {

        @NonNull
        @Override
        public CameraXConfig getCameraXConfig() {
            return create();
        }
    }
}
