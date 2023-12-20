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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.streamsharing.StreamSharingConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private static final String TAG = "StreamUseCaseUtil";

    public static final Config.Option<Long> STREAM_USE_CASE_STREAM_SPEC_OPTION =
            Config.Option.create("camera2.streamSpec.streamUseCase", long.class);

    private static final Map<Long, Set<UseCaseConfigFactory.CaptureType>>
            STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP = new HashMap<>();

    private static final Map<Long, Set<UseCaseConfigFactory.CaptureType>>
            STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP = new HashMap<>();

    static {
        if (Build.VERSION.SDK_INT >= 33) {
            Set<UseCaseConfigFactory.CaptureType> captureTypes = new HashSet<>();
            captureTypes.add(UseCaseConfigFactory.CaptureType.PREVIEW);
            captureTypes.add(UseCaseConfigFactory.CaptureType.METERING_REPEATING);
            STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.put(
                    Long.valueOf(
                            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL),
                    captureTypes);
            captureTypes = new HashSet<>();
            captureTypes.add(UseCaseConfigFactory.CaptureType.PREVIEW);
            captureTypes.add(UseCaseConfigFactory.CaptureType.METERING_REPEATING);
            captureTypes.add(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS);
            STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.put(
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW),
                    captureTypes);
            captureTypes = new HashSet<>();
            captureTypes.add(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE);
            STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.put(
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE),
                    captureTypes);
            captureTypes = new HashSet<>();
            captureTypes.add(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE);
            STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.put(
                    Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD),
                    captureTypes);

            captureTypes = new HashSet<>();
            captureTypes.add(UseCaseConfigFactory.CaptureType.PREVIEW);
            captureTypes.add(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE);
            captureTypes.add(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE);
            STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP.put(Long.valueOf(
                            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL),
                    captureTypes);
            captureTypes = new HashSet<>();
            captureTypes.add(UseCaseConfigFactory.CaptureType.PREVIEW);
            captureTypes.add(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE);
            STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP.put(Long.valueOf(
                            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD),
                    captureTypes);
        }
    }

    private StreamUseCaseUtil() {
    }

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
            @NonNull Collection<UseCaseConfig<?>> useCaseConfigs,
            @NonNull Map<DeferrableSurface, Long> streamUseCaseMap) {
        int position = 0;
        boolean hasStreamUseCase = false;
        ArrayList<UseCaseConfig<?>> useCaseConfigArrayList = new ArrayList<>(useCaseConfigs);
        for (SessionConfig sessionConfig : sessionConfigs) {
            if (sessionConfig.getImplementationOptions().containsOption(
                    STREAM_USE_CASE_STREAM_SPEC_OPTION)
                    && sessionConfig.getSurfaces().size() != 1) {
                Logger.e(TAG, String.format("SessionConfig has stream use case but also contains "
                                + "%d surfaces, abort populateSurfaceToStreamUseCaseMapping().",
                        sessionConfig.getSurfaces().size()));
                return;
            }
            if (sessionConfig.getImplementationOptions().containsOption(
                    STREAM_USE_CASE_STREAM_SPEC_OPTION)) {
                hasStreamUseCase = true;
                break;
            }
        }

        if (hasStreamUseCase) {
            for (SessionConfig sessionConfig : sessionConfigs) {
                if (useCaseConfigArrayList.get(position).getCaptureType()
                        == UseCaseConfigFactory.CaptureType.METERING_REPEATING) {
                    // MeteringRepeating is attached after the StreamUseCase population logic and
                    // therefore won't have the StreamUseCase option. It should always have
                    // SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW
                    streamUseCaseMap.put(sessionConfig.getSurfaces().get(0),
                            Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));

                } else if (sessionConfig.getImplementationOptions().containsOption(
                        STREAM_USE_CASE_STREAM_SPEC_OPTION)) {
                    streamUseCaseMap.put(sessionConfig.getSurfaces().get(0),
                            sessionConfig.getImplementationOptions().retrieveOption(
                                    STREAM_USE_CASE_STREAM_SPEC_OPTION));
                }
                position++;
            }
        }
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
    public static boolean shouldUseStreamUseCase(
            @NonNull SupportedSurfaceCombination.FeatureSettings featureSettings) {
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
        long[] availableStreamUseCases = characteristicsCompat.get(
                CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES);
        if (availableStreamUseCases == null || availableStreamUseCases.length == 0) {
            return false;
        }
        Set<Long> availableStreamUseCaseSet = new HashSet<>();
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
                    attachedSurfaceStreamSpecMap.put(attachedSurfaceInfo,
                            attachedSurfaceInfo.toStreamSpec(newImplementationOptions));
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
     * Return true if  the stream use cases in the given surface configurations are available for
     * the device.
     */
    public static boolean areStreamUseCasesAvailableForSurfaceConfigs(
            @NonNull CameraCharacteristicsCompat characteristicsCompat,
            @NonNull List<SurfaceConfig> surfaceConfigs) {
        if (Build.VERSION.SDK_INT < 33) {
            return false;
        }
        long[] availableStreamUseCases = characteristicsCompat.get(
                CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES);
        if (availableStreamUseCases == null || availableStreamUseCases.length == 0) {
            return false;
        }
        Set<Long> availableStreamUseCaseSet = new HashSet<>();
        for (Long availableStreamUseCase : availableStreamUseCases) {
            availableStreamUseCaseSet.add(availableStreamUseCase);
        }
        for (SurfaceConfig surfaceConfig : surfaceConfigs) {
            if (!availableStreamUseCaseSet.contains(surfaceConfig.getStreamUseCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if the given capture type and stream use case are a eligible pair. If the
     * given captureType is STREAM_SHARING, checks the streamSharingTypes, which are the capture
     * types of the children, are eligible with the stream use case.
     */
    private static boolean isEligibleCaptureType(UseCaseConfigFactory.CaptureType captureType,
            long streamUseCase, List<UseCaseConfigFactory.CaptureType> streamSharingTypes) {
        if (Build.VERSION.SDK_INT < 33) {
            return false;
        }
        if (captureType == UseCaseConfigFactory.CaptureType.STREAM_SHARING) {
            if (!STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP.containsKey(
                    streamUseCase)) {
                return false;
            }
            Set<UseCaseConfigFactory.CaptureType> captureTypes =
                    STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP.get(
                            streamUseCase);
            if (streamSharingTypes.size() != captureTypes.size()) {
                return false;
            }
            for (UseCaseConfigFactory.CaptureType childType : streamSharingTypes) {
                if (!captureTypes.contains(childType)) {
                    return false;
                }
            }
            return true;
        } else {
            return STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.containsKey(streamUseCase)
                    && STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.get(streamUseCase).contains(
                    captureType);
        }
    }

    /**
     * Return true if the stream use cases contained in surfaceConfigsWithStreamUseCases all have
     * eligible capture type pairing with the use cases that these surfaceConfigs are constructed
     * from.
     *
     * @param surfaceConfigIndexAttachedSurfaceInfoMap mapping between an surfaceConfig's index
     *                                                 in the list and the attachedSurfaceInfo it
     *                                                 is constructed from
     * @param surfaceConfigIndexUseCaseConfigMap       mapping between an surfaceConfig's index
     *      *                                          in the list and the useCaseConfig it is
     *                                                 constructed from
     * @param surfaceConfigsWithStreamUseCase          the supported surfaceConfigs that contains
     *                                                 accurate streamUseCases
     */
    public static boolean areCaptureTypesEligible(
            @NonNull Map<Integer, AttachedSurfaceInfo> surfaceConfigIndexAttachedSurfaceInfoMap,
            @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigIndexUseCaseConfigMap,
            @NonNull List<SurfaceConfig> surfaceConfigsWithStreamUseCase) {
        for (int i = 0; i < surfaceConfigsWithStreamUseCase.size(); i++) {
            // Verify that the use case has the eligible capture type the given stream use case.
            long streamUseCase = surfaceConfigsWithStreamUseCase.get(i).getStreamUseCase();
            if (surfaceConfigIndexAttachedSurfaceInfoMap.containsKey(i)) {
                AttachedSurfaceInfo attachedSurfaceInfo =
                        surfaceConfigIndexAttachedSurfaceInfoMap.get(i);
                if (!isEligibleCaptureType(attachedSurfaceInfo.getCaptureTypes().size() == 1
                                ? attachedSurfaceInfo.getCaptureTypes().get(0) :
                                UseCaseConfigFactory.CaptureType.STREAM_SHARING, streamUseCase,
                        attachedSurfaceInfo.getCaptureTypes())) {
                    return false;
                }
            } else if (surfaceConfigIndexUseCaseConfigMap.containsKey(i)) {
                UseCaseConfig<?> newUseCaseConfig =
                        surfaceConfigIndexUseCaseConfigMap.get(i);
                if (!isEligibleCaptureType(newUseCaseConfig.getCaptureType(), streamUseCase,
                        newUseCaseConfig.getCaptureType()
                                == UseCaseConfigFactory.CaptureType.STREAM_SHARING
                                ? ((StreamSharingConfig) newUseCaseConfig).getCaptureTypes()
                                : Collections.emptyList())) {
                    return false;
                }
            } else {
                throw new AssertionError("SurfaceConfig does not map to any use case");
            }
        }
        return true;
    }

    /**
     * @param suggestedStreamSpecMap                   mapping between useCaseConfig and its
     *                                                 streamSpecs
     * @param attachedSurfaceStreamSpecMap             mapping between attachedSurfaceInfo and its
     *                                                 streamSpecs that contains streamUseCases.
     *                                                 All streamSpecs in this map has
     *                                                 streamUseCases
     * @param surfaceConfigIndexAttachedSurfaceInfoMap mapping between an surfaceConfig's index
     *                                                 in the list and the
     *                                                 attachedSurfaceInfo it
     *                                                 is constructed from
     *@param surfaceConfigIndexUseCaseConfigMap        mapping between an surfaceConfig's
     *                                                 index in the list and the useCaseConfig
     *                                                 it is constructed from
     * @param surfaceConfigsWithStreamUseCase          the supported surfaceConfigs that contains
     *                                                 accurate streamUseCases
     */
    public static void populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
            @NonNull Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap,
            @NonNull Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap,
            @NonNull Map<Integer, AttachedSurfaceInfo> surfaceConfigIndexAttachedSurfaceInfoMap,
            @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigIndexUseCaseConfigMap,
            @NonNull List<SurfaceConfig> surfaceConfigsWithStreamUseCase) {
        // Populate StreamSpecs with stream use cases.
        for (int i = 0; i < surfaceConfigsWithStreamUseCase.size(); i++) {
            long streamUseCase = surfaceConfigsWithStreamUseCase.get(i).getStreamUseCase();
            if (surfaceConfigIndexAttachedSurfaceInfoMap.containsKey(i)) {
                AttachedSurfaceInfo attachedSurfaceInfo =
                        surfaceConfigIndexAttachedSurfaceInfoMap.get(i);
                Config oldImplementationOptions = attachedSurfaceInfo.getImplementationOptions();
                Config newImplementationOptions =
                        getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                                oldImplementationOptions, streamUseCase);
                if (newImplementationOptions != null) {
                    attachedSurfaceStreamSpecMap.put(attachedSurfaceInfo,
                            attachedSurfaceInfo.toStreamSpec(newImplementationOptions));
                }
            } else if (surfaceConfigIndexUseCaseConfigMap.containsKey(i)) {
                UseCaseConfig<?> newUseCaseConfig =
                        surfaceConfigIndexUseCaseConfigMap.get(i);
                StreamSpec oldStreamSpec = suggestedStreamSpecMap.get(newUseCaseConfig);
                Config oldImplementationOptions = oldStreamSpec.getImplementationOptions();
                Config newImplementationOptions =
                        getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                                oldImplementationOptions, streamUseCase);
                if (newImplementationOptions != null) {
                    StreamSpec newStreamSpec =
                            oldStreamSpec.toBuilder().setImplementationOptions(
                                    newImplementationOptions).build();
                    suggestedStreamSpecMap.put(newUseCaseConfig, newStreamSpec);
                }

            } else {
                throw new AssertionError("SurfaceConfig does not map to any use case");
            }
        }

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
