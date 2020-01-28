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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.SessionConfig;
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
    public static final String DEFAULT_CAMERA_ID = "DEFAULT_CAMERA_ID";

    private static final UseCaseConfig<ImageAnalysis> DEFAULT_IMAGE_ANALYSIS_CONFIG =
            new ImageAnalysis.Builder().setSessionOptionUnpacker(
                    new SessionConfig.OptionUnpacker() {
                        @Override
                        public void unpack(@NonNull UseCaseConfig<?> config,
                                @NonNull SessionConfig.Builder builder) {
                            // no op.
                        }
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
        return (C) DEFAULT_IMAGE_ANALYSIS_CONFIG;
    }

    /**
     * Shadow of {@link CameraX#getSurfaceManager()}.
     */
    @Implementation
    public static CameraDeviceSurfaceManager getSurfaceManager() {
        return DEFAULT_DEVICE_SURFACE_MANAGER;
    }
}
