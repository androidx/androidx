/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import static androidx.camera.camera2.impl.Camera2ImplConfig.STREAM_USE_CASE_OPTION;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.SessionConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class that contains utility methods for stream use case.
 */
public final class StreamUseCaseUtil {

    private StreamUseCaseUtil() {

    }

    private static Map<Class<?>, Long> sUseCaseToStreamUseCaseMapping;

    /**
     * Populates the mapping between surfaces of a capture session and the Stream Use Case of their
     * associated stream.
     *
     * @param sessionConfigs   collection of all session configs for this capture session
     * @param streamUseCaseMap the mapping between surfaces and Stream Use Case flag
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public static void populateSurfaceToStreamUseCaseMapping(
            @NonNull Collection<SessionConfig> sessionConfigs,
            @NonNull Map<DeferrableSurface, Long> streamUseCaseMap,
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat,
            boolean shouldSetStreamUseCaseByDefault) {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }

        if (cameraCharacteristicsCompat.get(
                CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES) == null) {
            return;
        }

        Set<Long> supportedStreamUseCases = new HashSet<>();
        for (long useCase : cameraCharacteristicsCompat.get(
                CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES)) {
            supportedStreamUseCases.add(useCase);
        }

        for (SessionConfig sessionConfig : sessionConfigs) {
            if (sessionConfig.getTemplateType()
                    == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
            ) {
                // If is ZSL, do not populate anything.
                streamUseCaseMap.clear();
                return;
            }
            for (DeferrableSurface surface : sessionConfig.getSurfaces()) {
                if (sessionConfig.getImplementationOptions().containsOption(
                        STREAM_USE_CASE_OPTION) && putStreamUseCaseToMappingIfAvailable(
                        streamUseCaseMap,
                        surface,
                        sessionConfig.getImplementationOptions().retrieveOption(
                                STREAM_USE_CASE_OPTION),
                        supportedStreamUseCases)) {
                    continue;
                }

                if (shouldSetStreamUseCaseByDefault) {
                    // TODO(b/266879290) This is currently gated out because of camera device
                    // crashing due to unsupported stream useCase combinations.
                    Long streamUseCase = getUseCaseToStreamUseCaseMapping()
                            .get(surface.getContainerClass());
                    putStreamUseCaseToMappingIfAvailable(streamUseCaseMap,
                            surface,
                            streamUseCase,
                            supportedStreamUseCases);
                }
            }
        }
    }

    private static boolean putStreamUseCaseToMappingIfAvailable(
            Map<DeferrableSurface, Long> streamUseCaseMap,
            DeferrableSurface surface,
            @Nullable Long streamUseCase,
            Set<Long> availableStreamUseCases) {
        if (streamUseCase == null) {
            return false;
        }

        if (!availableStreamUseCases.contains(streamUseCase)) {
            return false;
        }

        streamUseCaseMap.put(surface, streamUseCase);
        return true;
    }

    /**
     * Returns the mapping between the container class of a surface and the StreamUseCase
     * associated with that class. Refer to {@link UseCase} for the potential UseCase as the
     * container class for a given surface.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private static Map<Class<?>, Long> getUseCaseToStreamUseCaseMapping() {
        if (sUseCaseToStreamUseCaseMapping == null) {
            sUseCaseToStreamUseCaseMapping = new HashMap<>();
            sUseCaseToStreamUseCaseMapping.put(ImageAnalysis.class,
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
            sUseCaseToStreamUseCaseMapping.put(Preview.class,
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
            sUseCaseToStreamUseCaseMapping.put(ImageCapture.class,
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
            sUseCaseToStreamUseCaseMapping.put(MediaCodec.class,
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        }
        return sUseCaseToStreamUseCaseMapping;
    }
}
