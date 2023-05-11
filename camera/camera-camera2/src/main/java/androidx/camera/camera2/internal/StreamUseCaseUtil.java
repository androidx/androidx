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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.media.MediaCodec;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.streamsharing.StreamSharing;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class that contains utility methods for stream use case.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class StreamUseCaseUtil {

    public static final Config.Option<Long> STREAM_USE_CASE_STREAM_SPEC_OPTION =
            Config.Option.create("camera2.streamSpec.streamUseCase", long.class);

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
                        Camera2ImplConfig.STREAM_USE_CASE_OPTION)
                        && putStreamUseCaseToMappingIfAvailable(
                        streamUseCaseMap,
                        surface,
                        sessionConfig.getImplementationOptions().retrieveOption(
                                Camera2ImplConfig.STREAM_USE_CASE_OPTION),
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
            sUseCaseToStreamUseCaseMapping.put(StreamSharing.class,
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        }
        return sUseCaseToStreamUseCaseMapping;
    }

    /**
     * Populate all implementation options needed to determine the StreamUseCase option in the
     * StreamSpec for this UseCaseConfig
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @NonNull
    public static Camera2ImplConfig getStreamSpecImplementationOptions(
            @NonNull UseCaseConfig<?> useCaseConfig
    ) {
        MutableOptionsBundle optionsBundle = MutableOptionsBundle.create();
        if (useCaseConfig.containsOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION)) {
            optionsBundle.insertOption(
                    Camera2ImplConfig.STREAM_USE_CASE_OPTION,
                    useCaseConfig.retrieveOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION)
            );
        }
        if (useCaseConfig.containsOption(UseCaseConfig.OPTION_ZSL_DISABLED)) {
            optionsBundle.insertOption(
                    UseCaseConfig.OPTION_ZSL_DISABLED,
                    useCaseConfig.retrieveOption(UseCaseConfig.OPTION_ZSL_DISABLED)
            );
        }
        if (useCaseConfig.containsOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)) {
            optionsBundle.insertOption(
                    ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE,
                    useCaseConfig
                            .retrieveOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)
            );
        }
        if (useCaseConfig.containsOption(UseCaseConfig.OPTION_INPUT_FORMAT)) {
            optionsBundle.insertOption(
                    UseCaseConfig.OPTION_INPUT_FORMAT,
                    useCaseConfig
                            .retrieveOption(UseCaseConfig.OPTION_INPUT_FORMAT)
            );
        }
        return new Camera2ImplConfig(optionsBundle);
    }

    /**
     * Return true if the given camera characteristics support stream use case
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public static boolean isStreamUseCaseSupported(
            @NonNull CameraCharacteristicsCompat characteristicsCompat) {
        if (Build.VERSION.SDK_INT < 33) {
            return false;
        }
        long[] availableStreamUseCases = characteristicsCompat.get(
                CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES);
        if (availableStreamUseCases == null || availableStreamUseCases.length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Return true if the given feature settings is appropriate for stream use case usage.
     */
    public static boolean shouldUseStreamUseCase(@NonNull
            SupportedSurfaceCombination.FeatureSettings featureSettings) {
        return featureSettings.getCameraMode() == CameraMode.DEFAULT
                && featureSettings.getRequiredMaxBitDepth() == DynamicRange.BIT_DEPTH_8_BIT;
    }

    /**
     * Populate the {@link STREAM_USE_CASE_STREAM_SPEC_OPTION} option in StreamSpecs for both
     * existing UseCases and new UseCases to be attached. This option will be written into the
     * session configurations of the UseCases. When creating a new capture session during
     * downstream, it will be used to set the StreamUseCase flag via
     * {@link android.hardware.camera2.params.OutputConfiguration#setStreamUseCase(long)}
     *
     * @param characteristicsCompat        the camera characteristics of the device
     * @param attachedSurfaces             surface info of the already attached use cases
     * @param suggestedStreamSpecMap       the UseCaseConfig-to-StreamSpec map for new use cases
     * @param attachedSurfaceStreamSpecMap the SurfaceInfo-to-StreamSpec map for attached use cases
     *                                     whose StreamSpecs needs to be updated
     * @return true if StreamSpec options are populated. False if not.
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public static boolean populateStreamUseCaseStreamSpecOptionWithInteropOverride(
            @NonNull CameraCharacteristicsCompat characteristicsCompat,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap,
            @NonNull Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap) {
        if (Build.VERSION.SDK_INT < 33) {
            return false;
        }
        List<UseCaseConfig<?>> newUseCaseConfigs = new ArrayList<>(suggestedStreamSpecMap.keySet());
        // All AttachedSurfaceInfo should have implementation options
        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            Preconditions.checkNotNull(attachedSurfaceInfo.getImplementationOptions());
        }
        // All StreamSpecs in the map should have implementation options
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            Preconditions.checkNotNull(Preconditions.checkNotNull(
                    suggestedStreamSpecMap.get(useCaseConfig)).getImplementationOptions());
        }
        Set<Long> availableStreamUseCaseSet = new HashSet<>();
        long[] availableStreamUseCases = characteristicsCompat.get(
                CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES);
        for (Long availableStreamUseCase : availableStreamUseCases) {
            availableStreamUseCaseSet.add(availableStreamUseCase);
        }
        if (isValidCamera2InteropOverride(attachedSurfaces, newUseCaseConfigs,
                availableStreamUseCaseSet)) {
            for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
                Config oldImplementationOptions = attachedSurfaceInfo.getImplementationOptions();
                Config newImplementationOptions =
                        getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                                oldImplementationOptions,
                                oldImplementationOptions.retrieveOption(
                                        Camera2ImplConfig.STREAM_USE_CASE_OPTION));
                if (newImplementationOptions != null) {
                    StreamSpec.Builder newStreamSpecBuilder =
                            StreamSpec.builder(attachedSurfaceInfo.getSize())
                                    .setDynamicRange(attachedSurfaceInfo.getDynamicRange())
                                    .setImplementationOptions(newImplementationOptions);
                    if (attachedSurfaceInfo.getTargetFrameRate() != null) {
                        newStreamSpecBuilder.setExpectedFrameRateRange(
                                attachedSurfaceInfo.getTargetFrameRate());
                    }
                    attachedSurfaceStreamSpecMap.put(attachedSurfaceInfo,
                            newStreamSpecBuilder.build());
                }
            }
            for (UseCaseConfig<?> newUseCaseConfig : newUseCaseConfigs) {
                StreamSpec oldStreamSpec = suggestedStreamSpecMap.get(newUseCaseConfig);
                Config oldImplementationOptions = oldStreamSpec.getImplementationOptions();
                Config newImplementationOptions =
                        getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                                oldImplementationOptions, oldImplementationOptions.retrieveOption(
                                        Camera2ImplConfig.STREAM_USE_CASE_OPTION));
                if (newImplementationOptions != null) {
                    StreamSpec newStreamSpec =
                            oldStreamSpec.toBuilder().setImplementationOptions(
                                    newImplementationOptions).build();
                    suggestedStreamSpecMap.put(newUseCaseConfig, newStreamSpec);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Given an old options, return a new option with stream use case stream spec option inserted
     */
    @Nullable
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static Config getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
            Config oldImplementationOptions,
            long streamUseCase
    ) {
        if (oldImplementationOptions.containsOption(STREAM_USE_CASE_STREAM_SPEC_OPTION)
                && oldImplementationOptions.retrieveOption(STREAM_USE_CASE_STREAM_SPEC_OPTION)
                == streamUseCase) {
            // The old options already has the same stream use case. No need to update
            return null;
        }
        MutableOptionsBundle optionsBundle =
                MutableOptionsBundle.from(oldImplementationOptions);
        optionsBundle.insertOption(STREAM_USE_CASE_STREAM_SPEC_OPTION, streamUseCase);
        return new Camera2ImplConfig(optionsBundle);
    }

    /**
     * Return true if any one of the existing or new UseCases is ZSL.
     */
    public static boolean containsZslUseCase(
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs) {
        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            List<UseCaseConfigFactory.CaptureType> captureTypes =
                    attachedSurfaceInfo.getCaptureTypes();
            UseCaseConfigFactory.CaptureType captureType = captureTypes.get(0);
            if (isZslUseCase(
                    attachedSurfaceInfo.getImplementationOptions(),
                    captureType)) {
                return true;
            }
        }
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            if (isZslUseCase(useCaseConfig, useCaseConfig.getCaptureType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a UseCase is ZSL.
     */
    private static boolean isZslUseCase(Config config,
            UseCaseConfigFactory.CaptureType captureType) {
        if (config.retrieveOption(UseCaseConfig.OPTION_ZSL_DISABLED, false)) {
            return false;
        }
        // Skip if capture mode doesn't exist in the options
        if (!config.containsOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)) {
            return false;
        }

        @ImageCapture.CaptureMode int captureMode =
                config.retrieveOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE);
        return TemplateTypeUtil.getSessionConfigTemplateType(captureType, captureMode)
                == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG;
    }

    /**
     * Check whether the given StreamUseCases are available to the device.
     */
    private static boolean areStreamUseCasesAvailable(Set<Long> availableStreamUseCasesSet,
            Set<Long> streamUseCases) {
        for (Long streamUseCase : streamUseCases) {
            if (!availableStreamUseCasesSet.contains(streamUseCase)) {
                return false;
            }
        }
        return true;
    }

    private static void throwInvalidCamera2InteropOverrideException() {
        throw new IllegalArgumentException("Either all use cases must have non-default stream use "
                + "case assigned or none should have it");
    }

    /**
     * Return true if all existing UseCases and new UseCases have Camera2Interop override and
     * these StreamUseCases are all available to the device.
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static boolean isValidCamera2InteropOverride(
            List<AttachedSurfaceInfo> attachedSurfaces,
            List<UseCaseConfig<?>> newUseCaseConfigs,
            Set<Long> availableStreamUseCases) {
        Set<Long> streamUseCases = new HashSet<>();
        boolean hasNonDefaultStreamUseCase = false;
        boolean hasDefaultOrNullStreamUseCase = false;
        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            if (!attachedSurfaceInfo.getImplementationOptions().containsOption(
                    Camera2ImplConfig.STREAM_USE_CASE_OPTION)) {
                hasDefaultOrNullStreamUseCase = true;
                break;
            }
            long streamUseCaseOverride =
                    attachedSurfaceInfo.getImplementationOptions()
                            .retrieveOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION);
            if (streamUseCaseOverride
                    == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT) {
                hasDefaultOrNullStreamUseCase = true;
                break;
            }
            hasNonDefaultStreamUseCase = true;
            break;
        }
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            if (!useCaseConfig.containsOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION)) {
                hasDefaultOrNullStreamUseCase = true;
                if (hasNonDefaultStreamUseCase) {
                    throwInvalidCamera2InteropOverrideException();
                }
            } else {
                long streamUseCaseOverride =
                        useCaseConfig.retrieveOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION);
                if (streamUseCaseOverride
                        == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT) {
                    hasDefaultOrNullStreamUseCase = true;
                    if (hasNonDefaultStreamUseCase) {
                        throwInvalidCamera2InteropOverrideException();
                    }
                } else {
                    hasNonDefaultStreamUseCase = true;
                    if (hasDefaultOrNullStreamUseCase) {
                        throwInvalidCamera2InteropOverrideException();
                    }
                    streamUseCases.add(streamUseCaseOverride);
                }
            }

        }
        return !hasDefaultOrNullStreamUseCase && areStreamUseCasesAvailable(availableStreamUseCases,
                streamUseCases);
    }
}
