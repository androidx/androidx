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

import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.RESULT_UNDEFINED;
import static androidx.camera.featurecombinationquery.CameraDeviceSetupCompat.SupportQueryResult.SOURCE_UNDEFINED;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.params.SessionConfiguration;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A {@link CameraDeviceSetupCompat} implementation that combines multiple
 * {@link CameraDeviceSetupCompat}.
 *
 * <p>This class checks if a {@link SessionConfiguration} is supported in the order of the
 * provided implementation list, and returns the first non-undefined result. If all results are
 * undefined, it will return a undefined result.
 */
final class AggregatedCameraDeviceSetupCompat implements CameraDeviceSetupCompat {

    private final List<CameraDeviceSetupCompat> mCameraDeviceSetupImpls;

    AggregatedCameraDeviceSetupCompat(List<CameraDeviceSetupCompat> cameraDeviceSetupImpls) {
        mCameraDeviceSetupImpls = cameraDeviceSetupImpls;
    }

    @Override
    public @NonNull SupportQueryResult isSessionConfigurationSupported(
            @NonNull SessionConfiguration sessionConfig)
            throws CameraAccessException {
        for (CameraDeviceSetupCompat impl : mCameraDeviceSetupImpls) {
            SupportQueryResult result = impl.isSessionConfigurationSupported(sessionConfig);
            if (result.getSupported() != RESULT_UNDEFINED) {
                return result;
            }
        }
        return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_UNDEFINED, 0);
    }

    @Override
    public @NonNull SupportQueryResult isSessionConfigurationSupportedLegacy(
            @NonNull SessionConfigurationLegacy sessionConfig) {
        // Ensure that IllegalStateException is thrown if Camera2's CameraDeviceSetup
        // is supported.
        for (CameraDeviceSetupCompat impl: mCameraDeviceSetupImpls) {
            if (impl instanceof Camera2CameraDeviceSetupCompat) {
                throw new IllegalStateException(
                        "This device supports CameraDeviceSetup. Please use Camera2 "
                                + "SessionConfiguration for querying instead.");
            }
        }

        for (CameraDeviceSetupCompat impl : mCameraDeviceSetupImpls) {
            SupportQueryResult result = impl.isSessionConfigurationSupportedLegacy(sessionConfig);
            if (result.getSupported() != RESULT_UNDEFINED) {
                return result;
            }
        }
        return new SupportQueryResult(RESULT_UNDEFINED, SOURCE_UNDEFINED, 0);
    }
}
