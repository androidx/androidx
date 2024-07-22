/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.featurecombinationquery;

import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_SUPPORTED;
import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_UNSUPPORTED;
import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.SOURCE_ANDROID_FRAMEWORK;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.SessionConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A Android framework based {@link CameraDeviceSetupCompat} implementation.
 */
@RequiresApi(api = 35)
class Camera2CameraDeviceSetupCompat implements CameraDeviceSetupCompat {

    private final CameraDevice.CameraDeviceSetup mCameraDeviceSetup;

    Camera2CameraDeviceSetupCompat(@NonNull CameraManager cameraManager, @NonNull String cameraId)
            throws CameraAccessException {
        mCameraDeviceSetup = cameraManager.getCameraDeviceSetup(cameraId);
    }

    @NonNull
    @Override
    public SupportQueryResult isSessionConfigurationSupported(
            @NonNull SessionConfiguration sessionConfig)
            throws CameraAccessException {
        return new SupportQueryResult(
                mCameraDeviceSetup.isSessionConfigurationSupported(sessionConfig) ? RESULT_SUPPORTED
                        : RESULT_UNSUPPORTED,
                SOURCE_ANDROID_FRAMEWORK,
                getBuildTimeEpochMillis());
    }

    public static long getBuildTimeEpochMillis() {
        String value = System.getProperty("ro.build.date.utc");
        if (value != null) {
            try {
                return Long.parseLong(value) * 1000;
            } catch (NumberFormatException e) {
                // Fall through
            }
        }
        return 0;
    }
}
