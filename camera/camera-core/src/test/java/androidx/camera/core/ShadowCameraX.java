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
            new ImageAnalysisConfig.Builder().setSessionOptionUnpacker(
                    new SessionConfig.OptionUnpacker() {
                        @Override
                        public void unpack(UseCaseConfig<?> config, SessionConfig.Builder builder) {
                            // no op.
                        }
                    }).build();

    private static final CameraInfo DEFAULT_CAMERA_INFO = new CameraInfoInternal() {
        MutableLiveData<Boolean> mFlashAvailability = new MutableLiveData<>(Boolean.TRUE);
        @Override
        public CameraX.LensFacing getLensFacing() {
            return CameraX.LensFacing.BACK;
        }

        @Override
        public int getSensorRotationDegrees() {
            return 0;
        }

        @Override
        public int getSensorRotationDegrees(int relativeRotation) {
            return 0;
        }

        @NonNull
        @Override
        public LiveData<Boolean> isFlashAvailable() {
            return mFlashAvailability;
        }
    };

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
    public static CameraInfo getCameraInfo(String cameraId) throws CameraInfoUnavailableException {
        return DEFAULT_CAMERA_INFO;
    }

    /**
     * Shadow of {@link CameraX#getDefaultUseCaseConfig(Class, CameraX.LensFacing)}.
     */
    @SuppressWarnings("unchecked")
    @Implementation
    public static <C extends UseCaseConfig<?>> C getDefaultUseCaseConfig(
            Class<C> configType, CameraX.LensFacing lensFacing) {
        return (C) DEFAULT_IMAGE_ANALYSIS_CONFIG;
    }

}
