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
import androidx.camera.core.impl.CameraDeviceConfig;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

    private static final CameraInfo DEFAULT_CAMERA_INFO = new CameraInfoInternal() {
        final boolean mHasFlashUnit = true;
        MutableLiveData<Integer> mTorchState = new MutableLiveData<>(TorchState.OFF);
        MutableLiveData<Float> mZoomRatio = new MutableLiveData<>(1.0f);
        MutableLiveData<Float> mMaxZoomRatio = new MutableLiveData<>(4.0f);
        MutableLiveData<Float> mMinZoomRatio = new MutableLiveData<>(1.0f);
        MutableLiveData<Float> mLinearZoom = new MutableLiveData<>(0f);

        @Override
        public Integer getLensFacing() {
            return CameraSelector.LENS_FACING_BACK;
        }

        @NonNull
        @Override
        public String getCameraId() {
            return DEFAULT_CAMERA_ID;
        }

        @Override
        public int getSensorRotationDegrees() {
            return 0;
        }

        @Override
        public int getSensorRotationDegrees(int relativeRotation) {
            return 0;
        }

        @Override
        public boolean hasFlashUnit() {
            return mHasFlashUnit;
        }

        @NonNull
        @Override
        @TorchState.State
        public LiveData<Integer> getTorchState() {
            return mTorchState;
        }

        @NonNull
        @Override
        public LiveData<Float> getZoomRatio() {
            return mZoomRatio;
        }

        @NonNull
        @Override
        public LiveData<Float> getMaxZoomRatio() {
            return mMaxZoomRatio;
        }

        @NonNull
        @Override
        public LiveData<Float> getMinZoomRatio() {
            return mMinZoomRatio;
        }

        @NonNull
        @Override
        public LiveData<Float> getLinearZoom() {
            return mLinearZoom;
        }
    };

    private static final CameraDeviceSurfaceManager DEFAULT_DEVICE_SURFACE_MANAGER =
            new FakeCameraDeviceSurfaceManager();

    /**
     * Shadow of {@link ShadowCameraX#getCameraWithCameraDeviceConfig(CameraDeviceConfig)}.
     */
    @Implementation
    public static String getCameraWithCameraDeviceConfig(CameraDeviceConfig config) {
        return DEFAULT_CAMERA_ID;
    }

    /**
     * Shadow of {@link CameraX#getCameraInfo(String)}.
     */
    @Implementation
    public static CameraInfo getCameraInfo(String cameraId) {
        return DEFAULT_CAMERA_INFO;
    }

    /**
     * Shadow of {@link CameraX#getDefaultUseCaseConfig(Class, Integer)}.
     */
    @SuppressWarnings("unchecked")
    @Implementation
    public static <C extends UseCaseConfig<?>> C getDefaultUseCaseConfig(
            Class<C> configType, @Nullable Integer lensFacing) {
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
