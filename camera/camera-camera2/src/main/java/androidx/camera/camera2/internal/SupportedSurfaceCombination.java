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

import static android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT;
import static android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES;

import static androidx.camera.camera2.internal.GuaranteedConfigurationsUtil.generateQueryableFcqCombinations;
import static androidx.camera.camera2.internal.SupportedSurfaceCombination.CheckingMethod.WITHOUT_FEATURE_COMBO;
import static androidx.camera.camera2.internal.SupportedSurfaceCombination.CheckingMethod.WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT;
import static androidx.camera.camera2.internal.SupportedSurfaceCombination.CheckingMethod.WITH_FEATURE_COMBO;
import static androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED;
import static androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR;
import static androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
import static androidx.camera.core.impl.SurfaceConfig.ConfigSource.CAPTURE_SESSION_TABLES;
import static androidx.camera.core.impl.SurfaceConfig.ConfigSource.FEATURE_COMBINATION_TABLE;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat;
import androidx.camera.camera2.internal.compat.workaround.ExtraSupportedSurfaceCombinationsContainer;
import androidx.camera.camera2.internal.compat.workaround.ResolutionCorrector;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalSessionConfig;
import androidx.camera.core.Logger;
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery;
import androidx.camera.core.featuregroup.impl.feature.FpsRangeFeature;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.StreamUseCase;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceConfig.ConfigSize;
import androidx.camera.core.impl.SurfaceConfig.ConfigSource;
import androidx.camera.core.impl.SurfaceSizeDefinition;
import androidx.camera.core.impl.SurfaceStreamSpecQueryResult;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.stabilization.StabilizationMode;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import kotlin.Lazy;
import kotlin.UnsafeLazyImpl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Camera device supported surface configuration combinations
 *
 * <p>{@link CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store a list of surface combinations that are guaranteed to
 * support for this camera device.
 */
@OptIn(markerClass = ExperimentalCamera2Interop.class)
public final class SupportedSurfaceCombination {
    private static final String TAG = "SupportedSurfaceCombination";
    private static final int FRAME_RATE_UNLIMITED = Integer.MAX_VALUE;
    private final List<SurfaceCombination> mSurfaceCombinations = new ArrayList<>();
    private final List<SurfaceCombination> mUltraHighSurfaceCombinations = new ArrayList<>();
    private final List<SurfaceCombination> mConcurrentSurfaceCombinations = new ArrayList<>();
    private final List<SurfaceCombination> mPreviewStabilizationSurfaceCombinations =
            new ArrayList<>();
    private final List<SurfaceCombination> mHighSpeedSurfaceCombinations = new ArrayList<>();
    private final List<SurfaceCombination> mFcqSurfaceCombinations = new ArrayList<>();
    private final Map<FeatureSettings, List<SurfaceCombination>>
            mFeatureSettingsToSupportedCombinationsMap = new HashMap<>();
    private final List<SurfaceCombination> mSurfaceCombinations10Bit = new ArrayList<>();
    private final List<SurfaceCombination> mSurfaceCombinationsUltraHdr = new ArrayList<>();
    private final List<SurfaceCombination> mSurfaceCombinationsStreamUseCase = new ArrayList<>();
    private final String mCameraId;
    private final CamcorderProfileHelper mCamcorderProfileHelper;
    private final CameraCharacteristicsCompat mCharacteristics;
    private final ExtraSupportedSurfaceCombinationsContainer
            mExtraSupportedSurfaceCombinationsContainer;
    private final int mHardwareLevel;
    private boolean mIsRawSupported = false;
    private boolean mIsBurstCaptureSupported = false;
    private final boolean mIsConcurrentCameraModeSupported;
    private final boolean mIsStreamUseCaseSupported;
    private boolean mIsUltraHighResolutionSensorSupported = false;
    private boolean mIsManualSensorSupported = false;
    private final boolean mIsPreviewStabilizationSupported;
    @VisibleForTesting
    SurfaceSizeDefinition mSurfaceSizeDefinition;
    List<Integer> mSurfaceSizeDefinitionFormats = new ArrayList<>();
    private final @NonNull DisplayInfoManager mDisplayInfoManager;

    private final TargetAspectRatio mTargetAspectRatio = new TargetAspectRatio();
    private final ResolutionCorrector mResolutionCorrector = new ResolutionCorrector();
    private final DynamicRangeResolver mDynamicRangeResolver;
    private final HighSpeedResolver mHighSpeedResolver;

    private final FeatureCombinationQuery mFeatureCombinationQuery;

    @IntDef({DynamicRange.BIT_DEPTH_8_BIT, DynamicRange.BIT_DEPTH_10_BIT})
    @Retention(RetentionPolicy.SOURCE)
    @interface RequiredMaxBitDepth {}

    SupportedSurfaceCombination(@NonNull Context context, @NonNull String cameraId,
            @NonNull CameraManagerCompat cameraManagerCompat,
            @NonNull CamcorderProfileHelper camcorderProfileHelper,
            @NonNull FeatureCombinationQuery featureCombinationQuery)
            throws CameraUnavailableException {
        mCameraId = Preconditions.checkNotNull(cameraId);
        mCamcorderProfileHelper = Preconditions.checkNotNull(camcorderProfileHelper);
        mExtraSupportedSurfaceCombinationsContainer =
                new ExtraSupportedSurfaceCombinationsContainer();
        mDisplayInfoManager = DisplayInfoManager.getInstance(context);

        try {
            mCharacteristics = cameraManagerCompat.getCameraCharacteristicsCompat(mCameraId);
            Integer keyValue = mCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            mHardwareLevel = keyValue != null ? keyValue
                    : CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
        } catch (CameraAccessExceptionCompat e) {
            throw CameraUnavailableExceptionHelper.createFrom(e);
        }

        int[] availableCapabilities =
                mCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

        if (availableCapabilities != null) {
            for (int capability : availableCapabilities) {
                if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                    mIsRawSupported = true;
                } else if (capability
                        == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE) {
                    mIsBurstCaptureSupported = true;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && capability
                        == CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR) {
                    mIsUltraHighResolutionSensorSupported = true;
                } else if (capability
                        == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) {
                    mIsManualSensorSupported = true;
                }
            }
        }

        mDynamicRangeResolver = new DynamicRangeResolver(mCharacteristics);
        mHighSpeedResolver = new HighSpeedResolver(mCharacteristics);
        generateSupportedCombinationList();

        if (mIsUltraHighResolutionSensorSupported) {
            generateUltraHighSupportedCombinationList();
        }

        mIsConcurrentCameraModeSupported =
                context.getPackageManager().hasSystemFeature(FEATURE_CAMERA_CONCURRENT);
        if (mIsConcurrentCameraModeSupported) {
            generateConcurrentSupportedCombinationList();
        }

        if (mDynamicRangeResolver.is10BitDynamicRangeSupported()) {
            generate10BitSupportedCombinationList();
        }

        mIsStreamUseCaseSupported = StreamUseCaseUtil.isStreamUseCaseSupported(mCharacteristics);
        if (mIsStreamUseCaseSupported) {
            generateStreamUseCaseSupportedCombinationList();
        }

        mIsPreviewStabilizationSupported = VideoStabilizationUtil.isPreviewStabilizationSupported(
                mCharacteristics);
        if (mIsPreviewStabilizationSupported) {
            generatePreviewStabilizationSupportedCombinationList();
        }

        generateSurfaceSizeDefinition();
        checkCustomization();

        mFeatureCombinationQuery = featureCombinationQuery;
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param featureSettings              the settings for the camera's features/capabilities.
     * @param surfaceConfigList            the surface configuration list to be compared
     * @param dynamicRangesBySurfaceConfig the mapping of surface config to dynamic range, required
     *                                     for feature combination query.
     * @return the check result that whether it could be supported
     */
    boolean checkSupported(
            @NonNull FeatureSettings featureSettings,
            List<SurfaceConfig> surfaceConfigList,
            @NonNull Map<@NonNull SurfaceConfig, @NonNull DynamicRange>
                    dynamicRangesBySurfaceConfig,
            @NonNull List<@NonNull UseCaseConfig<?>> newUseCaseConfigs,
            @NonNull List<@NonNull Integer> useCasePriorityOrder) {
        boolean isSupported = false;

        for (SurfaceCombination surfaceCombination : getSurfaceCombinationsByFeatureSettings(
                featureSettings)) {
            isSupported = surfaceCombination.getOrderedSupportedSurfaceConfigList(surfaceConfigList)
                    != null;

            if (isSupported) {
                break;
            }
        }

        if (isSupported && featureSettings.requiresFeatureComboQuery()) {
            SessionConfig sessionConfig = createFeatureComboSessionConfig(featureSettings,
                    surfaceConfigList, dynamicRangesBySurfaceConfig, newUseCaseConfigs,
                    useCasePriorityOrder);
            isSupported = mFeatureCombinationQuery.isSupported(sessionConfig);

            // Clean up all the surfaces created for this query.
            for (DeferrableSurface surface : sessionConfig.getSurfaces()) {
                surface.close();
            }
        }

        return isSupported;
    }

    private SessionConfig createFeatureComboSessionConfig(
            FeatureSettings featureSettings,
            List<SurfaceConfig> surfaceConfigList,
            @NonNull Map<@NonNull SurfaceConfig, @NonNull DynamicRange>
                    dynamicRangesBySurfaceConfig,
            @NonNull List<@NonNull UseCaseConfig<?>> newUseCaseConfigs,
            @NonNull List<@NonNull Integer> useCasePriorityOrder) {
        Range<Integer> fpsRange = featureSettings.getTargetFpsRange();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        for (int i = 0; i < surfaceConfigList.size(); i++) {
            SurfaceConfig surfaceConfig = surfaceConfigList.get(i);
            Size resolution = surfaceConfig.getResolution(
                    getUpdatedSurfaceSizeDefinitionByFormat(surfaceConfig.getImageFormat()));

            // Since the high-level API for feature combination always unbinds implicitly, there
            // will only be new use cases
            UseCaseConfig<?> useCaseConfig = newUseCaseConfigs.get(useCasePriorityOrder.get(i));

            SessionConfig.Builder sessionConfigBuilder =
                    FeatureCombinationQuery.createSessionConfigBuilder(useCaseConfig, resolution,
                            requireNonNull(dynamicRangesBySurfaceConfig.get(surfaceConfig)));

            sessionConfigBuilder.setExpectedFrameRateRange(
                    FRAME_RATE_RANGE_UNSPECIFIED.equals(fpsRange)
                            ? FpsRangeFeature.DEFAULT_FPS_RANGE : fpsRange);

            if (featureSettings.isPreviewStabilizationOn()) {
                sessionConfigBuilder.setPreviewStabilization(StabilizationMode.ON);
            }

            validatingBuilder.add(sessionConfigBuilder.build());

            checkState(validatingBuilder.isValid(),
                    "Cannot create a combined SessionConfig for feature combo after adding "
                            + useCaseConfig + " with " + surfaceConfig + " due to ["
                            + validatingBuilder.getInvalidReason() + "]; surfaceConfigList = "
                            + surfaceConfigList + ", featureSettings = " + featureSettings
                            + ", newUseCaseConfigs = " + newUseCaseConfigs);
        }

        return validatingBuilder.build();
    }

    @Nullable
    List<SurfaceConfig> getOrderedSupportedStreamUseCaseSurfaceConfigList(
            @NonNull FeatureSettings featureSettings,
            @NonNull List<SurfaceConfig> surfaceConfigList,
            @NonNull Map<Integer, AttachedSurfaceInfo> surfaceConfigIndexAttachedSurfaceInfoMap,
            @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigIndexUseCaseConfigMap) {
        if (!StreamUseCaseUtil.shouldUseStreamUseCase(featureSettings)) {
            return null;
        }

        for (SurfaceCombination surfaceCombination : mSurfaceCombinationsStreamUseCase) {
            List<SurfaceConfig> orderedSurfaceConfigList =
                    surfaceCombination.getOrderedSupportedSurfaceConfigList(surfaceConfigList);
            if (orderedSurfaceConfigList != null) {
                boolean captureTypesEligible = StreamUseCaseUtil.areCaptureTypesEligible(
                        surfaceConfigIndexAttachedSurfaceInfoMap,
                        surfaceConfigIndexUseCaseConfigMap, orderedSurfaceConfigList);
                Lazy<Boolean> streamUseCasesAvailableForSurfaceConfigs = new UnsafeLazyImpl<>(
                        () -> StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                                mCharacteristics, orderedSurfaceConfigList));

                if (captureTypesEligible && streamUseCasesAvailableForSurfaceConfigs.getValue()) {

                    return orderedSurfaceConfigList;
                }
            }
        }
        return null;
    }

    /**
     * Returns the supported surface combinations according to the specified feature
     * settings.
     */
    private List<SurfaceCombination> getSurfaceCombinationsByFeatureSettings(
            @NonNull FeatureSettings featureSettings) {
        if (mFeatureSettingsToSupportedCombinationsMap.containsKey(featureSettings)) {
            return mFeatureSettingsToSupportedCombinationsMap.get(featureSettings);
        }

        List<SurfaceCombination> supportedSurfaceCombinations = new ArrayList<>();

        if (featureSettings.requiresFeatureComboQuery()) {
            if (mFcqSurfaceCombinations.isEmpty()) {
                generateFcqSurfaceCombinations();
            }
            supportedSurfaceCombinations.addAll(mFcqSurfaceCombinations);
        } else if (featureSettings.isUltraHdrOn()) {
            // Creates Ultra Hdr list only when it is needed.
            if (mSurfaceCombinationsUltraHdr.isEmpty()) {
                generateUltraHdrSupportedCombinationList();
            }
            // For Ultra HDR output, only the default camera mode is currently supported.
            if (featureSettings.getCameraMode() == CameraMode.DEFAULT) {
                supportedSurfaceCombinations.addAll(mSurfaceCombinationsUltraHdr);
            }
        } else if (featureSettings.isHighSpeedOn()) {
            if (mHighSpeedSurfaceCombinations.isEmpty()) {
                generateHighSpeedSupportedCombinationList();
            }
            supportedSurfaceCombinations.addAll(mHighSpeedSurfaceCombinations);
        } else if (featureSettings.getRequiredMaxBitDepth() == DynamicRange.BIT_DEPTH_8_BIT) {
            switch (featureSettings.getCameraMode()) {
                case CameraMode.CONCURRENT_CAMERA:
                    supportedSurfaceCombinations = mConcurrentSurfaceCombinations;
                    break;
                case CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA:
                    supportedSurfaceCombinations.addAll(mUltraHighSurfaceCombinations);
                    supportedSurfaceCombinations.addAll(mSurfaceCombinations);
                    break;
                default:
                    supportedSurfaceCombinations.addAll(featureSettings.isPreviewStabilizationOn()
                            ? mPreviewStabilizationSurfaceCombinations : mSurfaceCombinations);
                    break;
            }
        } else if (featureSettings.getRequiredMaxBitDepth() == DynamicRange.BIT_DEPTH_10_BIT) {
            // For 10-bit outputs, only the default camera mode is currently supported.
            if (featureSettings.getCameraMode() == CameraMode.DEFAULT) {
                supportedSurfaceCombinations.addAll(mSurfaceCombinations10Bit);
            }
        }

        mFeatureSettingsToSupportedCombinationsMap.put(featureSettings,
                supportedSurfaceCombinations);

        return supportedSurfaceCombinations;
    }

    /**
     * Transform to a SurfaceConfig object with image format and size info
     *
     * @param cameraMode  the working camera mode.
     * @param imageFormat the image format info for the surface configuration object
     * @param size        the size info for the surface configuration object
     * @param streamUseCase the stream use case for the surface configuration object
     * @return new {@link SurfaceConfig} object
     */
    SurfaceConfig transformSurfaceConfig(
            @CameraMode.Mode int cameraMode,
            int imageFormat,
            @NonNull Size size,
            @NonNull StreamUseCase streamUseCase) {
        return SurfaceConfig.transformSurfaceConfig(
                imageFormat,
                size,
                getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                cameraMode,
                // FEATURE_COMBINATION_TABLE N/A for the code flows leading to this call
                CAPTURE_SESSION_TABLES,
                streamUseCase);
    }

    private int getMaxFrameRate(int imageFormat, @NonNull Size size, boolean isHighSpeedOn) {
        checkState(!isHighSpeedOn
                || imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
        return isHighSpeedOn ? mHighSpeedResolver.getMaxFrameRate(size)
                : getMaxFrameRate(mCharacteristics, imageFormat, size);
    }

    private int getMaxFrameRate(@NonNull CameraCharacteristicsCompat characteristics,
            int imageFormat, Size size) {
        long minFrameDuration = requireNonNull(
                characteristics.getStreamConfigurationMapCompat())
                .getOutputMinFrameDuration(imageFormat, size);
        if (minFrameDuration <= 0L) {
            if (mIsManualSensorSupported) {
                Logger.w(TAG, "minFrameDuration: " + minFrameDuration + " is invalid for "
                        + "imageFormat = " + imageFormat + ", size = " + size);
                return 0;
            } else {
                // According to the doc, getOutputMinFrameDuration may return 0 if device doesn't
                // support manual sensor. Return MAX_VALUE indicates no limit.
                return FRAME_RATE_UNLIMITED;
            }
        }
        return (int) (1000000000.0 / minFrameDuration);
    }

    private static int getRangeLength(@NonNull Range<Integer> range) {
        return (range.getUpper() - range.getLower()) + 1;
    }

    /**
     * @return the distance between the nearest limits of two non-intersecting ranges
     */
    private static int getRangeDistance(@NonNull Range<Integer> firstRange,
            @NonNull Range<Integer> secondRange) {
        checkState(
                !firstRange.contains(secondRange.getUpper())
                        && !firstRange.contains(secondRange.getLower()),
                "Ranges must not intersect");
        if (firstRange.getLower() > secondRange.getUpper()) {
            return firstRange.getLower() - secondRange.getUpper();
        } else {
            return secondRange.getLower() - firstRange.getUpper();
        }
    }

    /**
     * @param targetFps the target frame rate range used while comparing to device-supported ranges
     * @param storedRange the device-supported range that is currently saved and intersects with
     *                    targetFps
     * @param newRange a new potential device-supported range that intersects with targetFps
     * @return the device-supported range that better matches the target fps
     */
    private static Range<Integer> compareIntersectingRanges(Range<Integer> targetFps,
            Range<Integer> storedRange, Range<Integer> newRange) {
        // TODO(b/272075984): some ranges may may have a larger intersection but may also have an
        //  excessively large portion that is non-intersecting. Will want to do further
        //  investigation to find a more optimized way to decide when a potential range has too
        //  much non-intersecting value and discard it

        double storedIntersectionSize = getRangeLength(storedRange.intersect(targetFps));
        double newIntersectionSize = getRangeLength(newRange.intersect(targetFps));

        double newRangeRatio = newIntersectionSize / getRangeLength(newRange);
        double storedRangeRatio = storedIntersectionSize / getRangeLength(storedRange);

        if (newIntersectionSize > storedIntersectionSize) {
            // if new, the new range must have at least 50% of its range intersecting, OR has a
            // larger percentage of intersection than the previous stored range
            if (newRangeRatio >= .5 || newRangeRatio >= storedRangeRatio) {
                return newRange;
            }
        } else if (newIntersectionSize == storedIntersectionSize) {
            // if intersecting ranges have same length... pick the one that has the higher
            // intersection ratio
            if (newRangeRatio > storedRangeRatio) {
                return newRange;
            } else if (newRangeRatio == storedRangeRatio
                    && newRange.getLower() > storedRange.getLower()) {
                // if equal intersection size AND ratios pick the higher range
                return newRange;
            }

        } else if (storedRangeRatio < .5
                && newRangeRatio > storedRangeRatio) {
            // if the new one has a smaller range... only change if existing has an intersection
            // ratio < 50% and the new one has an intersection ratio > than the existing one
            return newRange;
        }
        return storedRange;
    }

    /**
     * Finds a frame rate range supported by the device that is closest to the target frame rate.
     *
     * <p>This method first adjusts the {@code targetFrameRate} parameter to ensure it does not
     * exceed {@code maxFps}, i.e. the target frame rate is capped by {@code maxFps} before
     * comparison. For example, if target is [30,60] and {@code maxFps} is 50, the effective target
     * for comparison becomes [30,50].
     *
     * <p>Then, the method iterates through `availableFpsRanges` to find the best match.
     *
     * <p>The selection prioritizes ranges that:
     * <ol>
     *     <li>Exactly match the target frame rate.</li>
     *     <li>Intersect with the target frame rate. Among intersecting ranges, the one with the
     *         largest intersection is chosen. If multiple ranges have the same largest
     *         intersection, further tie-breaking rules are applied (see
     *         {@code compareIntersectingRanges}).</li>
     *     <li>Do not intersect with the target frame rate. Among non-intersecting ranges, the
     *         one with the smallest distance to the target frame rate is chosen. If multiple
     *         ranges have the same smallest distance, the higher range is preferred. If they
     *         are still tied (e.g., one range is above and one is below with the same distance),
     *         the range with the shorter length is chosen.</li>
     * </ol>
     *
     * <p>If the target frame rate is {@link StreamSpec#FRAME_RATE_RANGE_UNSPECIFIED} or
     * {@code availableFpsRanges} is null, {@link StreamSpec#FRAME_RATE_RANGE_UNSPECIFIED} is
     * returned.
     *
     * @param targetFrameRate    The Target Frame Rate resolved from all current existing surfaces
     *                           and incoming new use cases
     * @param maxFps             The maximum FPS allowed by the current configuration.
     * @param availableFpsRanges A nullable array of frame rate ranges available on the device.
     * @return A frame rate range supported by the device that is closest to the
     *         {@code targetFrameRate}, or {@link StreamSpec#FRAME_RATE_RANGE_UNSPECIFIED} if no
     *         suitable range is found or inputs are invalid.
     */
    private @NonNull Range<Integer> getClosestSupportedDeviceFrameRate(
            @NonNull Range<Integer> targetFrameRate, int maxFps,
            @Nullable Range<Integer>[] availableFpsRanges) {
        if (FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFrameRate)) {
            return FRAME_RATE_RANGE_UNSPECIFIED;
        }

        if (availableFpsRanges == null) {
            return FRAME_RATE_RANGE_UNSPECIFIED;
        }
        // if  whole target frame rate range > maxFps of configuration, the target for this
        // calculation will be [max,max].

        // if the range is partially larger than  maxFps, the target for this calculation will be
        // [target.lower, max] for the sake of this calculation
        targetFrameRate = new Range<>(
                Math.min(targetFrameRate.getLower(), maxFps),
                Math.min(targetFrameRate.getUpper(), maxFps)
        );

        Range<Integer> bestRange = FRAME_RATE_RANGE_UNSPECIFIED;
        int currentIntersectSize = 0;


        for (Range<Integer> potentialRange : availableFpsRanges) {
            // ignore ranges completely larger than configuration's maximum fps
            if (maxFps >= requireNonNull(potentialRange).getLower()) {
                if (bestRange.equals(FRAME_RATE_RANGE_UNSPECIFIED)) {
                    bestRange = potentialRange;
                }
                // take if range is a perfect match
                if (potentialRange.equals(targetFrameRate)) {
                    bestRange = potentialRange;
                    break;
                }

                try {
                    // bias towards a range that intersects on the upper end
                    Range<Integer> newIntersection = potentialRange.intersect(targetFrameRate);
                    int newIntersectSize = getRangeLength(newIntersection);
                    // if this range intersects our target + no other range was already
                    if (currentIntersectSize == 0) {
                        bestRange = potentialRange;
                        currentIntersectSize = newIntersectSize;
                    } else if (newIntersectSize >= currentIntersectSize) {
                        // if the currently stored range + new range both intersect, check to see
                        // which one should be picked over the other
                        bestRange = compareIntersectingRanges(targetFrameRate, bestRange,
                                potentialRange);
                        currentIntersectSize = getRangeLength(targetFrameRate.intersect(bestRange));
                    }
                } catch (IllegalArgumentException e) {
                    // if no intersection is present, pick the range that is closer to our target
                    if (currentIntersectSize == 0) {
                        if (getRangeDistance(potentialRange, targetFrameRate)
                                < getRangeDistance(bestRange, targetFrameRate)) {
                            bestRange = potentialRange;
                        } else if (getRangeDistance(potentialRange, targetFrameRate)
                                == getRangeDistance(bestRange, targetFrameRate)) {
                            if (potentialRange.getLower() > bestRange.getUpper()) {
                                // if they both have the same distance, pick the higher range
                                bestRange = potentialRange;
                            } else if (getRangeLength(potentialRange) < getRangeLength(bestRange)) {
                                // if one isn't higher than the other, pick the range with the
                                // shorter length
                                bestRange = potentialRange;
                            }
                        }
                    }
                }
            }

        }
        return bestRange;
    }

    /**
     * Calculates the updated target frame rate based on a new target frame rate and a
     * previously stored target frame rate.
     *
     * <p>If strict fps is required and both new and stored frame rates are not unspecified, they
     * must be the same or an `IllegalStateException` will be thrown.
     *
     * <p>If strict fps is not required and both new and stored target frame rate are not
     * unspecified, the intersection of ranges will be adopted. If the ranges are disjoint, the
     * stored frame rate will be used.
     *
     * @param newTargetFrameRate    an incoming frame rate range
     * @param storedTargetFrameRate a stored frame rate range to be modified
     * @param isStrictFpsRequired   whether strict fps is required
     * @return adjusted target frame rate
     */
    @NonNull
    private Range<Integer> getUpdatedTargetFrameRate(@NonNull Range<Integer> newTargetFrameRate,
            @NonNull Range<Integer> storedTargetFrameRate, boolean isStrictFpsRequired) {
        if (FRAME_RATE_RANGE_UNSPECIFIED.equals(storedTargetFrameRate)
                && FRAME_RATE_RANGE_UNSPECIFIED.equals(newTargetFrameRate)) {
            return FRAME_RATE_RANGE_UNSPECIFIED;
        } else if (FRAME_RATE_RANGE_UNSPECIFIED.equals(storedTargetFrameRate)) {
            return newTargetFrameRate;
        } else if (FRAME_RATE_RANGE_UNSPECIFIED.equals(newTargetFrameRate)) {
            return storedTargetFrameRate;
        } else {
            if (isStrictFpsRequired) {
                // An IllegalStateException is thrown here because this is an implementation error
                // rather than an unsupported combination. Currently isStrictFpsRequired is true
                // only when SessionConfig frame rate API is used.
                Preconditions.checkState(
                        newTargetFrameRate == storedTargetFrameRate,
                        "All targetFrameRate should be the same if strict fps is required"
                );
                return newTargetFrameRate;
            } else {
                try {
                    // get intersection of existing target fps
                    return storedTargetFrameRate.intersect(newTargetFrameRate);
                } catch (IllegalArgumentException e) {
                    // no intersection, keep the previously stored value
                    return storedTargetFrameRate;
                }
            }
        }
    }

    private boolean getAndValidateIsStrictFpsRequired(boolean newIsStrictFpsRequired,
            @Nullable Boolean storedIsStrictFpsRequired) {
        if (storedIsStrictFpsRequired != null
                && storedIsStrictFpsRequired != newIsStrictFpsRequired) {
            // An IllegalStateException is thrown here because this is an implementation error
            // rather than an unsupported combination. Currently isStrictFpsRequired is true
            // only when SessionConfig frame rate API is used.
            throw new IllegalStateException("All isStrictFpsRequired should be the same");
        }
        return newIsStrictFpsRequired;
    }

    /**
     * @param currentMaxFps the previously stored Max FPS
     * @param imageFormat   the image format of the incoming surface
     * @param size          the size of the incoming surface
     * @param isHighSpeedOn whether high-speed session is enabled
     */
    private int getUpdatedMaximumFps(int currentMaxFps, int imageFormat, Size size,
            boolean isHighSpeedOn) {
        return Math.min(currentMaxFps, getMaxFrameRate(imageFormat, size, isHighSpeedOn));
    }

    /**
     * Finds the suggested stream specifications of the newly added UseCaseConfig.
     *
     * @param cameraMode                        the working camera mode.
     * @param attachedSurfaces                  the existing surfaces.
     * @param newUseCaseConfigsSupportedSizeMap newly added UseCaseConfig to supported output
     *                                          sizes map.
     * @param isPreviewStabilizationOn          whether the preview stabilization is enabled.
     * @param hasVideoCapture                   whether the use cases has video capture.
     * @param isFeatureComboInvocation whether the code flow involves CameraX feature combo
     *                                 API (e.g. {@link
     *                                 androidx.camera.core.SessionConfig#requiredFeatureGroup}).
     * @param findMaxSupportedFrameRate          whether to find the max supported frame rate. If
     *                                           this is true, the target frame rate settings
     *                                           will be ignored when calculating the stream spec.
     *                                           If false, the returned value of
     *                                    {@link SurfaceStreamSpecQueryResult#maxSupportedFrameRate}
     *                                           is undetermined.
     *
     * @return a {@link SurfaceStreamSpecQueryResult}.
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *                                  found. This may be due to no available output size, no
     *                                  available surface combination, unsupported combinations
     *                                  of {@link DynamicRange}, or requiring an
     *                                  unsupported combination of camera features.
     */
    @NonNull
    SurfaceStreamSpecQueryResult getSuggestedStreamSpecifications(
            @CameraMode.Mode int cameraMode,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap,
            boolean isPreviewStabilizationOn,
            boolean hasVideoCapture,
            boolean isFeatureComboInvocation,
            boolean findMaxSupportedFrameRate) {
        // Refresh Preview Size based on current display configurations.
        refreshPreviewSize();

        boolean isHighSpeedOn = HighSpeedResolver.isHighSpeedOn(attachedSurfaces,
                newUseCaseConfigsSupportedSizeMap.keySet());
        // Filter out unsupported sizes for high-speed at the beginning to ensure correct
        // resolution selection later. High-speed session requires all surface sizes to be the same.
        if (isHighSpeedOn) {
            newUseCaseConfigsSupportedSizeMap = mHighSpeedResolver.filterCommonSupportedSizes(
                    newUseCaseConfigsSupportedSizeMap);
        }

        List<UseCaseConfig<?>> newUseCaseConfigs = new ArrayList<>(
                newUseCaseConfigsSupportedSizeMap.keySet());

        // Get the index order list by the use case priority for finding stream configuration
        List<Integer> useCasesPriorityOrder = getUseCasesPriorityOrder(newUseCaseConfigs);
        Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges =
                mDynamicRangeResolver.resolveAndValidateDynamicRanges(attachedSurfaces,
                        newUseCaseConfigs, useCasesPriorityOrder);

        Logger.d(TAG, "resolvedDynamicRanges = " + resolvedDynamicRanges);

        boolean isUltraHdrOn = isUltraHdrOn(attachedSurfaces, newUseCaseConfigsSupportedSizeMap);

        // Calculates the target FPS range
        Range<Integer> targetFpsRange;
        boolean isStrictFpsRequired;
        if (findMaxSupportedFrameRate) {
            // In finding maxFps mode, ignore targetFpsRange and isStrictFpsRequired so that the
            // calculations won't be interrupted by any frame rate checks.
            isStrictFpsRequired = false;
            targetFpsRange = FRAME_RATE_RANGE_UNSPECIFIED;
        } else {
            isStrictFpsRequired = isStrictFpsRequired(attachedSurfaces, newUseCaseConfigs);
            targetFpsRange = getTargetFpsRange(attachedSurfaces, newUseCaseConfigs,
                    useCasesPriorityOrder, isStrictFpsRequired);
        }

        // Ensure preview stabilization is supported by the camera.
        if (isPreviewStabilizationOn && !mIsPreviewStabilizationSupported) {
            // TODO: b/422055796 - Handle this for non-feature-combo code flows, probably better to
            //  silently fall back to non-preview-stabilization mode in such case.

            if (isFeatureComboInvocation) {
                throw new IllegalArgumentException(
                        "Preview stabilization is not supported by the camera.");
            }
        }

        FeatureSettings featureSettings = createFeatureSettings(cameraMode, hasVideoCapture,
                resolvedDynamicRanges, isPreviewStabilizationOn, isUltraHdrOn, isHighSpeedOn,
                isFeatureComboInvocation, /* requiresFeatureComboQuery = */  false, targetFpsRange,
                isStrictFpsRequired);

        CheckingMethod checkingMethod = getCheckingMethod(
                resolvedDynamicRanges.values(), targetFpsRange, isPreviewStabilizationOn,
                isUltraHdrOn, isFeatureComboInvocation);

        return resolveSpecsByCheckingMethod(checkingMethod, featureSettings,
                attachedSurfaces, newUseCaseConfigsSupportedSizeMap, newUseCaseConfigs,
                useCasesPriorityOrder, resolvedDynamicRanges, findMaxSupportedFrameRate);
    }

    /**
     * Resolves the suggested stream specifications of the newly added UseCaseConfig according to
     * the provided {@link CheckingMethod}.
     *
     * <table>
     *  <tr>
     *      <th>CheckingMethod</th>
     *      <th>Description</th>
     *  </tr>
     *  <tr>
     *     <td>WITH_FEATURE_COMBO</td>
     *     <td>Resolves stream specs using only {@link ConfigSource#FEATURE_COMBINATION_TABLE}.</td>
     *  </tr>
     *  <tr>
     *     <td>WITHOUT_FEATURE_COMBO</td>
     *     <td>Resolves stream specs using only {@link ConfigSource#CAPTURE_SESSION_TABLES}.</td>
     *  </tr>
     *  <tr>
     *     <td>WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT</td>
     *     <td>Tries to resolve stream specs using only {@link ConfigSource#CAPTURE_SESSION_TABLES}
     *     first. If fails, retries with {@link ConfigSource#FEATURE_COMBINATION_TABLE} next.</td>
     *  </tr>
     * </table>
     *
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *                                  found. This may be due to no available output size, no
     *                                  available surface combination, unsupported combinations
     *                                  of {@link DynamicRange}, or requiring an
     *                                  unsupported combination of camera features.
     */
    @OptIn(markerClass = ExperimentalSessionConfig.class)
    @NonNull
    private SurfaceStreamSpecQueryResult resolveSpecsByCheckingMethod(
            @NonNull CheckingMethod checkingMethod,
            @NonNull FeatureSettings featureSettings,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap,
            List<UseCaseConfig<?>> newUseCaseConfigs,
            List<Integer> useCasesPriorityOrder,
            Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges,
            boolean findMaxSupportedFrameRate) {
        Logger.d(TAG, "resolveSpecsByCheckingMethod: checkingMethod = " + checkingMethod);

        switch (checkingMethod) {
            case WITH_FEATURE_COMBO: {
                FeatureSettings fcqFeatureSettings = createFeatureSettings(
                        featureSettings.getCameraMode(), featureSettings.hasVideoCapture(),
                        resolvedDynamicRanges, featureSettings.isPreviewStabilizationOn(),
                        featureSettings.isUltraHdrOn(), featureSettings.isHighSpeedOn(),
                        featureSettings.isFeatureComboInvocation(),
                        /* requiresFeatureComboQuery = */ true,
                        featureSettings.getTargetFpsRange(),
                        featureSettings.isStrictFpsRequired());

                return resolveSpecsBySettings(fcqFeatureSettings,
                        attachedSurfaces, newUseCaseConfigsSupportedSizeMap,
                        newUseCaseConfigs, useCasesPriorityOrder,
                        resolvedDynamicRanges, findMaxSupportedFrameRate);
            }
            case WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT: {
                try {
                    return resolveSpecsBySettings(featureSettings,
                            attachedSurfaces, newUseCaseConfigsSupportedSizeMap,
                            newUseCaseConfigs, useCasesPriorityOrder,
                            resolvedDynamicRanges, findMaxSupportedFrameRate);
                } catch (IllegalArgumentException e) {
                    Logger.d(TAG, "Failed to find a supported combination without feature"
                            + " combo, trying again with feature combo", e);

                    FeatureSettings fcqFeatureSettings = createFeatureSettings(
                            featureSettings.getCameraMode(), featureSettings.hasVideoCapture(),
                            resolvedDynamicRanges, featureSettings.isPreviewStabilizationOn(),
                            featureSettings.isUltraHdrOn(), featureSettings.isHighSpeedOn(),
                            featureSettings.isFeatureComboInvocation(),
                            /* requiresFeatureComboQuery = */ true,
                            featureSettings.getTargetFpsRange(),
                            featureSettings.isStrictFpsRequired());

                    return resolveSpecsBySettings(fcqFeatureSettings,
                            attachedSurfaces, newUseCaseConfigsSupportedSizeMap,
                            newUseCaseConfigs, useCasesPriorityOrder,
                            resolvedDynamicRanges, findMaxSupportedFrameRate);
                }
            }
            default:
                return resolveSpecsBySettings(featureSettings,
                        attachedSurfaces, newUseCaseConfigsSupportedSizeMap,
                        newUseCaseConfigs, useCasesPriorityOrder,
                        resolvedDynamicRanges, findMaxSupportedFrameRate);
        }
    }

    /**
     * Resolves the suggested stream specifications of the newly added UseCaseConfig according to
     * the provided {@link FeatureSettings}.
     *
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *                                  found. This may be due to no available output size, no
     *                                  available surface combination, unsupported combinations
     *                                  of {@link DynamicRange}, or requiring an
     *                                  unsupported combination of camera features.
     */
    private @NonNull SurfaceStreamSpecQueryResult resolveSpecsBySettings(
            FeatureSettings featureSettings,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap,
            List<UseCaseConfig<?>> newUseCaseConfigs,
            List<Integer> useCasesPriorityOrder,
            Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges,
            boolean findMaxSupportedFrameRate) {
        Logger.d(TAG, "resolveSpecsBySettings: featureSettings = " + featureSettings);

        // TODO: b/414489781 - Return early even with feature combo source for possible cases
        //  (e.g. the number of streams is higher than what FCQ can ever support)
        if (!featureSettings.requiresFeatureComboQuery() && !isUseCasesCombinationSupported(
                featureSettings, attachedSurfaces, newUseCaseConfigsSupportedSizeMap)) {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + mCameraId + ".  May be attempting to bind too many use cases. "
                            + "Existing surfaces: " + attachedSurfaces + ". New configs: "
                            + newUseCaseConfigs + ". GroupableFeature settings: "
                            + featureSettings);
        }

        // Filters the unnecessary output sizes for performance improvement. This will
        // significantly reduce the number of all possible size arrangements below.
        Map<UseCaseConfig<?>, List<Size>> useCaseConfigToFilteredSupportedSizesMap =
                filterSupportedSizes(newUseCaseConfigsSupportedSizeMap, featureSettings,
                        /*forceUniqueMaxFpsFiltering=*/findMaxSupportedFrameRate);

        List<List<Size>> supportedOutputSizesList = new ArrayList<>();

        // Collect supported output sizes for all use cases
        for (Integer index : useCasesPriorityOrder) {
            UseCaseConfig<?> useCaseConfig = newUseCaseConfigs.get(index);
            List<Size> supportedOutputSizes = useCaseConfigToFilteredSupportedSizesMap.get(
                    useCaseConfig);
            if (supportedOutputSizes == null) {
                supportedOutputSizes = Collections.emptyList();
            }
            supportedOutputSizes = applyResolutionSelectionOrderRelatedWorkarounds(
                    supportedOutputSizes, useCaseConfig.getInputFormat());
            supportedOutputSizesList.add(supportedOutputSizes);
        }

        // Get all possible size arrangements
        List<List<Size>> allPossibleSizeArrangements =
                featureSettings.isHighSpeedOn() ? mHighSpeedResolver.getSizeArrangements(
                        supportedOutputSizesList) : getAllPossibleSizeArrangements(
                        supportedOutputSizesList);

        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        // The two maps are used to keep track of the attachedSurfaceInfo or useCaseConfigs the
        // surfaceConfigs are made from. They are populated in getSurfaceConfigListAndFpsCeiling ().
        // The keys are the position of their corresponding surfaceConfigs in the list. We can
        // them map streamUseCases in orderedSurfaceConfigListForStreamUseCase, which is in the
        // same order as surfaceConfigs list, to the original useCases to determine the
        // captureTypes are correct.
        Map<Integer, AttachedSurfaceInfo> surfaceConfigIndexAttachedSurfaceInfoMap =
                new HashMap<>();
        Map<Integer, UseCaseConfig<?>> surfaceConfigIndexUseCaseConfigMap =
                new HashMap<>();

        boolean containsZsl = StreamUseCaseUtil.containsZslUseCase(attachedSurfaces,
                newUseCaseConfigs);
        List<SurfaceConfig> orderedSurfaceConfigListForStreamUseCase = null;
        int maxSupportedFps = getMaxSupportedFpsFromAttachedSurfaces(attachedSurfaces,
                featureSettings.isHighSpeedOn());
        // Only checks the stream use case combination support when ZSL is not required.
        if (mIsStreamUseCaseSupported && !containsZsl) {
            // Check if any possible size arrangement is supported for stream use case.
            for (List<Size> possibleSizeList : allPossibleSizeArrangements) {
                List<SurfaceConfig> surfaceConfigs = getSurfaceConfigListAndFpsCeiling(
                        featureSettings,
                        attachedSurfaces, possibleSizeList, newUseCaseConfigs,
                        useCasesPriorityOrder, maxSupportedFps,
                        surfaceConfigIndexAttachedSurfaceInfoMap,
                        surfaceConfigIndexUseCaseConfigMap).first;
                orderedSurfaceConfigListForStreamUseCase =
                        getOrderedSupportedStreamUseCaseSurfaceConfigList(featureSettings,
                                surfaceConfigs,
                                surfaceConfigIndexAttachedSurfaceInfoMap,
                                surfaceConfigIndexUseCaseConfigMap);
                if (orderedSurfaceConfigListForStreamUseCase != null) {
                    break;
                }
                surfaceConfigIndexAttachedSurfaceInfoMap.clear();
                surfaceConfigIndexUseCaseConfigMap.clear();
            }
            Logger.d(TAG, "orderedSurfaceConfigListForStreamUseCase = "
                    + orderedSurfaceConfigListForStreamUseCase);
        }

        BestSizesAndMaxFpsForConfigs bestSizesAndFps = findBestSizesAndFps(
                featureSettings, attachedSurfaces, newUseCaseConfigs, useCasesPriorityOrder,
                allPossibleSizeArrangements, orderedSurfaceConfigListForStreamUseCase,
                resolvedDynamicRanges, maxSupportedFps,
                findMaxSupportedFrameRate);

        Logger.d(TAG, "resolveSpecsBySettings: bestSizesAndFps = " + bestSizesAndFps);

        List<Size> savedSizes = bestSizesAndFps.getBestSizes();
        int savedConfigMaxFps = bestSizesAndFps.getMaxFpsForBestSizes();
        List<Size> savedSizesForStreamUseCase = bestSizesAndFps.getBestSizesForStreamUseCase();
        int savedConfigMaxFpsForStreamUseCase = bestSizesAndFps.getMaxFpsForStreamUseCase();
        int savedMaxFpsForAllSizes = bestSizesAndFps.getMaxFpsForAllSizes();

        // Map the saved supported SurfaceConfig combination
        if (savedSizes != null) {
            Range<Integer> targetFrameRateForDevice = FRAME_RATE_RANGE_UNSPECIFIED;
            if (!FRAME_RATE_RANGE_UNSPECIFIED.equals(featureSettings.getTargetFpsRange())) {
                Range<Integer>[] availableFpsRanges = featureSettings.isHighSpeedOn()
                        ? mHighSpeedResolver.getFrameRateRangesFor(savedSizes)
                        : mCharacteristics.get(CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                targetFrameRateForDevice = getClosestSupportedDeviceFrameRate(
                        featureSettings.getTargetFpsRange(), savedConfigMaxFps,
                        availableFpsRanges);

                if (featureSettings.isFeatureComboInvocation()
                        || featureSettings.isStrictFpsRequired()) {
                    checkArgument(
                            targetFrameRateForDevice.equals(featureSettings.getTargetFpsRange()),
                            "Target FPS range " + featureSettings.getTargetFpsRange()
                                    + " is not supported. Max FPS supported by the calculated best"
                                    + " combination: " + savedConfigMaxFps + ". Calculated best FPS"
                                    + " range for device: " + targetFrameRateForDevice
                                    + ". Device supported FPS ranges: "
                                    + Arrays.toString(availableFpsRanges));
                }
            }
            for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
                Size resolutionForUseCase = savedSizes.get(
                        useCasesPriorityOrder.indexOf(newUseCaseConfigs.indexOf(useCaseConfig)));
                StreamSpec.Builder streamSpecBuilder = StreamSpec.builder(resolutionForUseCase)
                        .setSessionType(
                                featureSettings.isHighSpeedOn() ? SESSION_TYPE_HIGH_SPEED
                                        : SESSION_TYPE_REGULAR)
                        .setDynamicRange(Preconditions.checkNotNull(
                                resolvedDynamicRanges.get(useCaseConfig)))
                        .setImplementationOptions(
                                StreamUseCaseUtil.getStreamSpecImplementationOptions(useCaseConfig)
                        )
                        .setZslDisabled(featureSettings.hasVideoCapture());
                if (!FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFrameRateForDevice)) {
                    streamSpecBuilder.setExpectedFrameRateRange(targetFrameRateForDevice);
                }
                suggestedStreamSpecMap.put(useCaseConfig, streamSpecBuilder.build());
            }
        } else {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + mCameraId + " and Hardware level: " + mHardwareLevel
                            + ". May be the specified resolution is too large and not supported."
                            + " Existing surfaces: " + attachedSurfaces
                            + " New configs: " + newUseCaseConfigs);
        }

        // Only perform stream use case operations if the saved max FPS and sizes are the same
        if (orderedSurfaceConfigListForStreamUseCase != null
                && savedConfigMaxFps == savedConfigMaxFpsForStreamUseCase
                && savedSizes.size() == savedSizesForStreamUseCase.size()) {
            boolean hasDifferenceSavedSizes = false;
            for (int i = 0; i < savedSizes.size(); i++) {
                if (!savedSizes.get(i).equals(savedSizesForStreamUseCase.get(i))) {
                    hasDifferenceSavedSizes = true;
                    break;
                }
            }
            if (!hasDifferenceSavedSizes) {
                boolean hasStreamUseCaseOverride =
                        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                                mCharacteristics, attachedSurfaces, suggestedStreamSpecMap,
                                attachedSurfaceStreamSpecMap);
                if (!hasStreamUseCaseOverride) {
                    StreamUseCaseUtil
                            .populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
                                    suggestedStreamSpecMap, attachedSurfaceStreamSpecMap,
                                    surfaceConfigIndexAttachedSurfaceInfoMap,
                                    surfaceConfigIndexUseCaseConfigMap,
                                    orderedSurfaceConfigListForStreamUseCase);
                }
            }
        }
        return new SurfaceStreamSpecQueryResult(suggestedStreamSpecMap,
                attachedSurfaceStreamSpecMap, savedMaxFpsForAllSizes);
    }

    private CheckingMethod getCheckingMethod(
            @NonNull Collection<DynamicRange> dynamicRanges, @Nullable Range<Integer> fps,
            boolean isPreviewStabilizationOn, boolean isUltraHdrOn,
            boolean isFeatureComboInvocation) {
        if (!isFeatureComboInvocation) {
            return WITHOUT_FEATURE_COMBO;
        }

        // TODO: Enforce all supported features are handled by going through some exhaustive list
        //  of supported features.
        int count = 0;

        if (dynamicRanges.contains(DynamicRange.HLG_10_BIT)) {
            count++;
        }
        if (fps != null && fps.getUpper() == 60) {
            count++;
        }
        if (isPreviewStabilizationOn) {
            count++;
        }
        if (isUltraHdrOn) {
            count++;
        }

        if (count > 1) {
            return WITH_FEATURE_COMBO;
        } else if (count == 1) {
            return WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT;
        } else {
            return WITHOUT_FEATURE_COMBO;
        }
    }

    private BestSizesAndMaxFpsForConfigs findBestSizesAndFps(
            FeatureSettings featureSettings,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs,
            List<Integer> useCasesPriorityOrder,
            List<List<Size>> allPossibleSizeArrangements,
            List<SurfaceConfig> orderedSurfaceConfigListForStreamUseCase,
            Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges,
            int maxSupportedFps,
            boolean findMaxFpsForAllSizes) {
        Range<Integer> targetFpsRange = featureSettings.getTargetFpsRange();

        List<Size> savedSizes = null;
        int savedConfigMaxFps = FRAME_RATE_UNLIMITED;
        List<Size> savedSizesForStreamUseCase = null;
        int savedConfigMaxFpsForStreamUseCase = FRAME_RATE_UNLIMITED;
        int maxFpsForAllSizes = Integer.MAX_VALUE;

        boolean supportedSizesFound = false;
        boolean supportedSizesForStreamUseCaseFound = false;

        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (List<Size> possibleSizeList : allPossibleSizeArrangements) {
            Map<Integer, AttachedSurfaceInfo> surfaceConfigIndexToAttachedSurfaceInfoMap =
                    new HashMap<>();
            Map<Integer, UseCaseConfig<?>> surfaceConfigIndexToUseCaseConfigMap =
                    new HashMap<>();

            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            Pair<List<SurfaceConfig>, Integer> resultPair =
                    getSurfaceConfigListAndFpsCeiling(featureSettings,
                            attachedSurfaces, possibleSizeList, newUseCaseConfigs,
                            useCasesPriorityOrder, maxSupportedFps,
                            surfaceConfigIndexToAttachedSurfaceInfoMap,
                            surfaceConfigIndexToUseCaseConfigMap);
            List<SurfaceConfig> surfaceConfigList = resultPair.first;
            int currentConfigFrameRateCeiling = resultPair.second;
            boolean isConfigFrameRateAcceptable = isConfigFrameRateAcceptable(maxSupportedFps,
                    targetFpsRange, currentConfigFrameRateCeiling);

            Map<SurfaceConfig, DynamicRange> dynamicRangesBySurfaceConfig = new HashMap<>();
            for (int index = 0; index < surfaceConfigList.size(); index++) {
                SurfaceConfig surfaceConfig = surfaceConfigList.get(index);
                DynamicRange dynamicRange = DynamicRange.UNSPECIFIED;

                if (surfaceConfigIndexToAttachedSurfaceInfoMap.containsKey(index)) {
                    dynamicRange = requireNonNull(
                            surfaceConfigIndexToAttachedSurfaceInfoMap.get(
                                    index)).getDynamicRange();
                } else if (surfaceConfigIndexToUseCaseConfigMap.containsKey(index)) {
                    dynamicRange = resolvedDynamicRanges.get(requireNonNull(
                            surfaceConfigIndexToUseCaseConfigMap.get(index)));
                }

                dynamicRangesBySurfaceConfig.put(surfaceConfig, dynamicRange);
            }

            Lazy<Boolean> isSupported = new UnsafeLazyImpl<>(
                    () -> checkSupported(featureSettings, surfaceConfigList,
                            dynamicRangesBySurfaceConfig, newUseCaseConfigs,
                            useCasesPriorityOrder));

            if (findMaxFpsForAllSizes && isSupported.getValue()) {
                if (maxFpsForAllSizes == Integer.MAX_VALUE) {
                    maxFpsForAllSizes = currentConfigFrameRateCeiling;
                } else if (maxFpsForAllSizes < currentConfigFrameRateCeiling) {
                    maxFpsForAllSizes = currentConfigFrameRateCeiling;
                }
            }

            // Find the same possible size arrangement that is supported by stream use case again
            // if we found one earlier.

            // only change the saved config if you get another that has a better max fps
            if (!supportedSizesFound && isSupported.getValue()) {
                // if the config is supported by the device but doesn't meet the target frame rate,
                // save the config
                if (savedConfigMaxFps == FRAME_RATE_UNLIMITED) {
                    savedConfigMaxFps = currentConfigFrameRateCeiling;
                    savedSizes = possibleSizeList;
                } else if (savedConfigMaxFps < currentConfigFrameRateCeiling) {
                    // only change the saved config if the max fps is better
                    savedConfigMaxFps = currentConfigFrameRateCeiling;
                    savedSizes = possibleSizeList;
                }

                if (isConfigFrameRateAcceptable) {
                    savedConfigMaxFps = currentConfigFrameRateCeiling;
                    savedSizes = possibleSizeList;
                    supportedSizesFound = true;
                    // if we have a configuration where the max fps is acceptable for our target,
                    // break. But never break when findMaxFpsForAllSizes flag is set.
                    if (supportedSizesForStreamUseCaseFound && !findMaxFpsForAllSizes) {
                        break;
                    }
                }
            }

            // If we already know that there is a supported surface combination from the stream
            // use case table, keep an independent tracking on the saved sizes and max FPS. Only
            // use stream use case if the save sizes for the normal case and for stream use case
            // are the same.
            if (orderedSurfaceConfigListForStreamUseCase != null
                    && !supportedSizesForStreamUseCaseFound
                    && getOrderedSupportedStreamUseCaseSurfaceConfigList(
                    featureSettings, surfaceConfigList, surfaceConfigIndexToAttachedSurfaceInfoMap,
                    surfaceConfigIndexToUseCaseConfigMap) != null) {
                if (savedConfigMaxFpsForStreamUseCase == FRAME_RATE_UNLIMITED) {
                    savedConfigMaxFpsForStreamUseCase = currentConfigFrameRateCeiling;
                    savedSizesForStreamUseCase = possibleSizeList;
                } else if (savedConfigMaxFpsForStreamUseCase < currentConfigFrameRateCeiling) {
                    savedConfigMaxFpsForStreamUseCase = currentConfigFrameRateCeiling;
                    savedSizesForStreamUseCase = possibleSizeList;
                }

                if (isConfigFrameRateAcceptable) {
                    savedConfigMaxFpsForStreamUseCase = currentConfigFrameRateCeiling;
                    savedSizesForStreamUseCase = possibleSizeList;
                    supportedSizesForStreamUseCaseFound = true;
                    // Never break when findMaxFpsForAllSizes flag is set.
                    if (supportedSizesFound && !findMaxFpsForAllSizes) {
                        break;
                    }
                }
            }
        }

        // When using the combinations guaranteed via feature combination APIs, targetFpsRange must
        // be strictly maintained rather than just choosing the combination with highest max FPS.
        if (featureSettings.isFeatureComboInvocation()
                && !FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFpsRange)
                && (savedConfigMaxFps == FRAME_RATE_UNLIMITED
                || savedConfigMaxFps < targetFpsRange.getUpper())) {
            return BestSizesAndMaxFpsForConfigs.of(null, null, FRAME_RATE_UNLIMITED,
                    FRAME_RATE_UNLIMITED, FRAME_RATE_UNLIMITED);
        }

        return BestSizesAndMaxFpsForConfigs.of(savedSizes, savedSizesForStreamUseCase,
                savedConfigMaxFps, savedConfigMaxFpsForStreamUseCase, maxFpsForAllSizes);
    }

    private static boolean isConfigFrameRateAcceptable(int maxSupportedFps,
            @NonNull Range<Integer> targetFpsRange, int currentConfigFrameRateCeiling) {
        boolean isConfigFrameRateAcceptable = true;
        if (!FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFpsRange)) {
            // currentConfigFrameRateCeiling < targetFpsRange.getUpper() to return false means that
            // there should still be other better choice because currentConfigFrameRateCeiling is
            // still smaller than both maxSupportedFps and targetFpsRange.getUpper().
            // For feature combo cases, fps ranges need to be fully supported, but sizes not
            // supporting target FPS range fully are already filtered out in
            // filterSupportedSizes API.
            if (currentConfigFrameRateCeiling < maxSupportedFps
                    && currentConfigFrameRateCeiling < targetFpsRange.getUpper()) {
                // if the max fps before adding new use cases supports our target fps range
                // BUT the max fps of the new configuration is below
                // our target fps range, we'll want to check the next configuration until we
                // get one that supports our target FPS
                isConfigFrameRateAcceptable = false;
            }
        }
        return isConfigFrameRateAcceptable;
    }

    private static boolean isUltraHdrOn(@NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap) {
        for (AttachedSurfaceInfo surfaceInfo : attachedSurfaces) {
            if (surfaceInfo.getImageFormat() == ImageFormat.JPEG_R) {
                return true;
            }
        }

        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigsSupportedSizeMap.keySet()) {
            if (useCaseConfig.getInputFormat() == ImageFormat.JPEG_R) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates the feature settings from the related info.
     *
     * @param cameraMode                the working camera mode.
     * @param resolvedDynamicRanges     the resolved dynamic range list of the newly added UseCases
     * @param isPreviewStabilizationOn  whether the preview stabilization is enabled.
     * @param isUltraHdrOn              whether the Ultra HDR image capture is enabled.
     * @param requiresFeatureComboQuery whether feature combination query is required.
     */
    private @NonNull FeatureSettings createFeatureSettings(
            @CameraMode.Mode int cameraMode, boolean hasVideoCapture,
            @NonNull Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges,
            boolean isPreviewStabilizationOn, boolean isUltraHdrOn, boolean isHighSpeedOn,
            boolean isFeatureComboInvocation, boolean requiresFeatureComboQuery,
            @NonNull Range<Integer> targetFpsRange,
            boolean isStrictFrameRateRequired) {
        int requiredMaxBitDepth = getRequiredMaxBitDepth(resolvedDynamicRanges);

        if (cameraMode != CameraMode.DEFAULT && isUltraHdrOn) {
            throw new IllegalArgumentException(String.format("Camera device id is %s. Ultra HDR "
                            + "is not currently supported in %s camera mode.",
                    mCameraId,
                    CameraMode.toLabelString(cameraMode)));
        }

        if (cameraMode != CameraMode.DEFAULT
                && requiredMaxBitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
            throw new IllegalArgumentException(String.format("Camera device id is %s. 10 bit "
                            + "dynamic range is not currently supported in %s camera mode.",
                    mCameraId,
                    CameraMode.toLabelString(cameraMode)));
        }

        if (cameraMode != CameraMode.DEFAULT && isFeatureComboInvocation) {
            throw new IllegalArgumentException(String.format("Camera device id is %s. Feature "
                            + "combination query is not currently supported in %s camera mode.",
                    mCameraId,
                    CameraMode.toLabelString(cameraMode)));
        }

        if (isHighSpeedOn && isFeatureComboInvocation) {
            throw new IllegalArgumentException("High-speed session is not supported with feature"
                    + " combination");
        }

        if (isHighSpeedOn && !mHighSpeedResolver.isHighSpeedSupported()) {
            throw new IllegalArgumentException(
                    "High-speed session is not supported on this device.");
        }

        // Use FpsRangeFeature.DEFAULT_FPS_RANGE when Camera2 FCQ checking is required
        if (isFeatureComboInvocation && targetFpsRange == FRAME_RATE_RANGE_UNSPECIFIED) {
            if (requiresFeatureComboQuery) {
                targetFpsRange = FpsRangeFeature.DEFAULT_FPS_RANGE;
            }
        }

        return FeatureSettings.of(cameraMode, hasVideoCapture, requiredMaxBitDepth,
                isPreviewStabilizationOn, isUltraHdrOn, isHighSpeedOn, isFeatureComboInvocation,
                requiresFeatureComboQuery, targetFpsRange, isStrictFrameRateRequired);
    }

    /**
     * Checks whether at least a surfaces combination can be supported for the UseCases
     * combination.
     *
     * <p>This function collects the selected surfaces from the existing UseCases and the
     * surfaces of the smallest available supported sizes from all the new UseCases. Using this
     * set of surfaces, this function can quickly determine whether at least one surface
     * combination can be supported for the target UseCases combination.
     *
     * <p>This function disregards the stream use case, frame rate, and ZSL factors since they
     * are not mandatory requirements if no surface combination can satisfy them. The current
     * algorithm only attempts to identify the optimal surface combination for the given conditions.
     *
     * @param featureSettings                   the feature settings which can affect the surface
     *                                          config transformation or the guaranteed supported
     *                                          configurations.
     * @param attachedSurfaces                  the existing surfaces.
     * @param newUseCaseConfigsSupportedSizeMap newly added UseCaseConfig to supported output
     *                                          sizes map.
     * @return {@code true} if at least a surface combination can be supported for the UseCases
     * combination. Otherwise, returns {@code false}.
     */
    private boolean isUseCasesCombinationSupported(
            @NonNull FeatureSettings featureSettings,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap) {
        List<SurfaceConfig> surfaceConfigs = new ArrayList<>();

        // Collects the surfaces of the attached UseCases
        for (AttachedSurfaceInfo attachedSurface : attachedSurfaces) {
            surfaceConfigs.add(attachedSurface.getSurfaceConfig());
        }

        // Collects the surfaces with the smallest available sizes of the newly attached UseCases
        // to do the quick check that whether at least a surface combination can be supported.
        CompareSizesByArea compareSizesByArea = new CompareSizesByArea();
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigsSupportedSizeMap.keySet()) {
            List<Size> supportedSizes = newUseCaseConfigsSupportedSizeMap.get(useCaseConfig);
            Preconditions.checkArgument(supportedSizes != null && !supportedSizes.isEmpty(), "No "
                    + "available output size is found for " + useCaseConfig + ".");
            Size minSize = Collections.min(supportedSizes, compareSizesByArea);
            int imageFormat = useCaseConfig.getInputFormat();
            surfaceConfigs.add(
                    SurfaceConfig.transformSurfaceConfig(
                            imageFormat,
                            minSize,
                            getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                            featureSettings.getCameraMode(),
                            // Feature combo src not needed for the code flows leading to this call
                            CAPTURE_SESSION_TABLES,
                            useCaseConfig.getStreamUseCase()));
        }

        // This method doesn't use feature combo resolutions since feature combo API doesn't
        // guarantee that a lower resolution will always be supported if higher resolution is
        // supported with same set of features
        return checkSupported(featureSettings, surfaceConfigs, Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList());
    }

    private @NonNull Range<Integer> getTargetFpsRange(
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs,
            @NonNull List<Integer> useCasesPriorityOrder,
            boolean isStrictFpsRequired) {
        Range<Integer> targetFrameRateForConfig = FRAME_RATE_RANGE_UNSPECIFIED;

        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            // init target fps range for new configs from existing surfaces
            targetFrameRateForConfig = getUpdatedTargetFrameRate(
                    attachedSurfaceInfo.getTargetFrameRate(),
                    targetFrameRateForConfig, isStrictFpsRequired);
        }

        // update target fps for new configs using new use cases' priority order
        for (Integer index : useCasesPriorityOrder) {
            Range<Integer> newTargetFrameRate = requireNonNull(newUseCaseConfigs.get(index)
                    .getTargetFrameRate(FRAME_RATE_RANGE_UNSPECIFIED));
            targetFrameRateForConfig = getUpdatedTargetFrameRate(newTargetFrameRate,
                    targetFrameRateForConfig, isStrictFpsRequired);
        }

        return targetFrameRateForConfig;
    }

    private boolean isStrictFpsRequired(@NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs) {
        Boolean isStrictFpsRequired = null;
        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            isStrictFpsRequired = getAndValidateIsStrictFpsRequired(
                    attachedSurfaceInfo.isStrictFrameRateRequired(), isStrictFpsRequired);
        }

        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            isStrictFpsRequired = getAndValidateIsStrictFpsRequired(
                    useCaseConfig.isStrictFrameRateRequired(), isStrictFpsRequired);
        }
        return isStrictFpsRequired != null ? isStrictFpsRequired : false;
    }

    private int getMaxSupportedFpsFromAttachedSurfaces(
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces, boolean isHighSpeedOn) {
        int existingSurfaceFrameRateCeiling = FRAME_RATE_UNLIMITED;

        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            //get the fps ceiling for existing surfaces
            existingSurfaceFrameRateCeiling = getUpdatedMaximumFps(
                    existingSurfaceFrameRateCeiling,
                    attachedSurfaceInfo.getImageFormat(), attachedSurfaceInfo.getSize(),
                    isHighSpeedOn);
        }

        return existingSurfaceFrameRateCeiling;
    }

    /**
     * Filters the supported sizes for each use case to keep only one item for each unique config
     * size and frame rate combination.
     *
     * @return the new use case config to the supported sizes map, with the unnecessary sizes
     * filtered out.
     */
    @VisibleForTesting
    @NonNull Map<UseCaseConfig<?>, List<Size>> filterSupportedSizes(
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap,
            @NonNull FeatureSettings featureSettings,
            boolean forceUniqueMaxFpsFiltering) {
        Map<UseCaseConfig<?>, List<Size>> filteredUseCaseConfigToSupportedSizesMap =
                new HashMap<>();
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigsSupportedSizeMap.keySet()) {
            List<Size> reducedSizeList = new ArrayList<>();
            Map<ConfigSize, Set<Integer>> configSizeUniqueMaxFpsMap =
                    new HashMap<>();
            for (Size size : requireNonNull(
                    newUseCaseConfigsSupportedSizeMap.get(useCaseConfig))) {
                int imageFormat = useCaseConfig.getInputFormat();
                StreamUseCase streamUseCase = useCaseConfig.getStreamUseCase();
                populateReducedSizeListAndUniqueMaxFpsMap(featureSettings,
                        featureSettings.getTargetFpsRange(), size, imageFormat, streamUseCase,
                        forceUniqueMaxFpsFiltering, configSizeUniqueMaxFpsMap, reducedSizeList);
            }
            filteredUseCaseConfigToSupportedSizesMap.put(useCaseConfig, reducedSizeList);
        }
        return filteredUseCaseConfigToSupportedSizesMap;
    }

    private void populateReducedSizeListAndUniqueMaxFpsMap(@NonNull FeatureSettings featureSettings,
            @NonNull Range<Integer> targetFpsRange, @NonNull Size size, int imageFormat,
            @NonNull StreamUseCase streamUseCase, boolean forceUniqueMaxFpsFiltering,
            @NonNull Map<ConfigSize, Set<Integer>> configSizeToUniqueMaxFpsMap,
            @NonNull List<Size> reducedSizeList) {
        ConfigSize configSize = SurfaceConfig.transformSurfaceConfig(
                imageFormat, size, getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                featureSettings.getCameraMode(),
                featureSettings.requiresFeatureComboQuery() ? FEATURE_COMBINATION_TABLE
                        : CAPTURE_SESSION_TABLES,
                streamUseCase).getConfigSize();

        int maxFrameRate = FRAME_RATE_UNLIMITED;
        // Filters the sizes with frame rate only if there is target FPS setting or force enabled.
        if (!FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFpsRange) || forceUniqueMaxFpsFiltering) {
            maxFrameRate = getMaxFrameRate(imageFormat, size, featureSettings.isHighSpeedOn());
        }

        // For feature combination, target FPS range must be strictly supported, so we can filter
        // out unsupported sizes earlier. Feature combination may also have some output sizes
        // mapping to ConfigSize.NOT_SUPPORT, those can be filtered out earlier as well.
        if (featureSettings.isFeatureComboInvocation()
                && (configSize == ConfigSize.NOT_SUPPORT
                || (!FRAME_RATE_RANGE_UNSPECIFIED.equals(targetFpsRange)
                && maxFrameRate < targetFpsRange.getUpper()))
        ) {
            return;
        }

        Set<Integer> uniqueMaxFrameRates = configSizeToUniqueMaxFpsMap.get(configSize);
        // Creates an empty FPS list for the config size when it doesn't exist.
        if (uniqueMaxFrameRates == null) {
            uniqueMaxFrameRates = new HashSet<>();
            configSizeToUniqueMaxFpsMap.put(configSize, uniqueMaxFrameRates);
        }

        // Adds the size to the result list when there is still no entry for the config
        // size and frame rate combination.
        //
        // An example to explain the filter logic (with only ConfigSource.CAPTURE_SESSION_TABLES
        // for simplicity).
        //
        // If a UseCase's sorted supported sizes are in the following sequence, the
        // corresponding config size type and the supported max frame rate are as the
        // following:
        //
        //    4032x3024 => MAXIMUM size, 30 fps
        //    3840x2160 => RECORD size, 30 fps
        //    2560x1440 => RECORD size, 30 fps -> can be filtered out
        //    1920x1080 => PREVIEW size, 60 fps
        //    1280x720 => PREVIEW size, 60 fps -> can be filtered out
        //
        // If 3840x2160 can be used, then it will have higher priority than 2560x1440 to
        // be used. Therefore, 2560x1440 can be filtered out because they belong to the
        // same config size type and also have the same max supported frame rate. The same
        // logic also works for 1920x1080 and 1280x720.
        //
        // If there are three UseCases have the same sorted supported sizes list, the
        // number of possible arrangements can be reduced from 125 (5x5x5) to 27 (3x3x3).
        // On real devices, more than 20 output sizes might be supported. This filtering
        // step can possibly reduce the number of possible arrangements from 8000 to less
        // than 100. Therefore, we can improve the bindToLifecycle function performance
        // because we can skip a large amount of unnecessary checks.
        if (!uniqueMaxFrameRates.contains(maxFrameRate)) {
            reducedSizeList.add(size);
            uniqueMaxFrameRates.add(maxFrameRate);
        }
    }

    private Pair<List<SurfaceConfig>, Integer> getSurfaceConfigListAndFpsCeiling(
            FeatureSettings featureSettings,
            List<AttachedSurfaceInfo> attachedSurfaces,
            List<Size> possibleSizeList, List<UseCaseConfig<?>> newUseCaseConfigs,
            List<Integer> useCasesPriorityOrder,
            int currentConfigFrameRateCeiling,
            @Nullable Map<Integer, AttachedSurfaceInfo> surfaceConfigIndexAttachedSurfaceInfoMap,
            @Nullable Map<Integer, UseCaseConfig<?>> surfaceConfigIndexUseCaseConfigMap) {
        List<SurfaceConfig> surfaceConfigList = new ArrayList<>();
        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            surfaceConfigList.add(attachedSurfaceInfo.getSurfaceConfig());
            if (surfaceConfigIndexAttachedSurfaceInfoMap != null) {
                surfaceConfigIndexAttachedSurfaceInfoMap.put(surfaceConfigList.size() - 1,
                        attachedSurfaceInfo);
            }
        }

        // Attach SurfaceConfig of new use cases
        for (int i = 0; i < possibleSizeList.size(); i++) {
            Size size = possibleSizeList.get(i);
            UseCaseConfig<?> newUseCase =
                    newUseCaseConfigs.get(useCasesPriorityOrder.get(i));
            int imageFormat = newUseCase.getInputFormat();
            StreamUseCase streamUseCase = newUseCase.getStreamUseCase();
            // add new use case/size config to list of surfaces
            ConfigSource configSource =
                    featureSettings.requiresFeatureComboQuery() ? FEATURE_COMBINATION_TABLE
                            : CAPTURE_SESSION_TABLES;
            SurfaceConfig surfaceConfig = SurfaceConfig.transformSurfaceConfig(
                    imageFormat,
                    size,
                    getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                    featureSettings.getCameraMode(),
                    configSource,
                    streamUseCase);
            surfaceConfigList.add(surfaceConfig);
            if (surfaceConfigIndexUseCaseConfigMap != null) {
                surfaceConfigIndexUseCaseConfigMap.put(surfaceConfigList.size() - 1, newUseCase);
            }
            // get the maximum fps of the new surface and update the maximum fps of the
            // proposed configuration
            currentConfigFrameRateCeiling = getUpdatedMaximumFps(
                    currentConfigFrameRateCeiling,
                    newUseCase.getInputFormat(),
                    size, featureSettings.isHighSpeedOn());
        }
        return new Pair<>(surfaceConfigList, currentConfigFrameRateCeiling);
    }

    /**
     * Applies resolution selection order related workarounds.
     *
     * <p>{@link TargetAspectRatio} workaround makes CameraX select sizes of specific aspect
     * ratio in priority to avoid the preview image stretch issue.
     *
     * <p>{@link ResolutionCorrector} workaround makes CameraX select specific sizes for
     * different capture types to avoid the preview image stretch issue.
     *
     * @see TargetAspectRatio
     * @see ResolutionCorrector
     */
    @VisibleForTesting
    @NonNull List<Size> applyResolutionSelectionOrderRelatedWorkarounds(
            @NonNull List<Size> sizeList, int imageFormat) {
        // Applies TargetAspectRatio workaround
        int targetAspectRatio = mTargetAspectRatio.get(mCameraId, mCharacteristics);
        Rational ratio;

        switch (targetAspectRatio) {
            case TargetAspectRatio.RATIO_4_3:
                ratio = AspectRatioUtil.ASPECT_RATIO_4_3;
                break;
            case TargetAspectRatio.RATIO_16_9:
                ratio = AspectRatioUtil.ASPECT_RATIO_16_9;
                break;
            case TargetAspectRatio.RATIO_MAX_JPEG:
                Size maxJpegSize = getUpdatedSurfaceSizeDefinitionByFormat(
                        ImageFormat.JPEG).getMaximumSize(ImageFormat.JPEG);
                ratio = maxJpegSize == null ? null : new Rational(maxJpegSize.getWidth(),
                        maxJpegSize.getHeight());
                break;
            case TargetAspectRatio.RATIO_ORIGINAL:
                ratio = null;
                break;
            default:
                throw new AssertionError("Undefined targetAspectRatio: " + targetAspectRatio);
        }

        List<Size> resultList;

        if (ratio == null) {
            resultList = sizeList;
        } else {
            List<Size> aspectRatioMatchedSizeList = new ArrayList<>();
            resultList = new ArrayList<>();

            for (Size size : sizeList) {
                if (AspectRatioUtil.hasMatchingAspectRatio(size, ratio)) {
                    aspectRatioMatchedSizeList.add(size);
                } else {
                    resultList.add(size);
                }
            }
            resultList.addAll(0, aspectRatioMatchedSizeList);
        }

        // Applies ResolutionCorrector workaround and return the result list.
        return mResolutionCorrector.insertOrPrioritize(
                SurfaceConfig.getConfigType(imageFormat),
                resultList);
    }

    @RequiredMaxBitDepth
    private static int getRequiredMaxBitDepth(
            @NonNull Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges) {
        for (DynamicRange dynamicRange : resolvedDynamicRanges.values()) {
            if (dynamicRange.getBitDepth() == DynamicRange.BIT_DEPTH_10_BIT) {
                return DynamicRange.BIT_DEPTH_10_BIT;
            }
        }

        return DynamicRange.BIT_DEPTH_8_BIT;
    }

    private static List<Integer> getUseCasesPriorityOrder(
            List<UseCaseConfig<?>> newUseCaseConfigs) {
        List<Integer> priorityOrder = new ArrayList<>();

        /*
         * Once the stream resource is occupied by one use case, it will impact the other use cases.
         * Therefore, we need to define the priority for stream resource usage. For the use cases
         * with the higher priority, we will try to find the best one for them in priority as
         * possible.
         */
        List<Integer> priorityValueList = new ArrayList<>();

        for (UseCaseConfig<?> config : newUseCaseConfigs) {
            int priority = config.getSurfaceOccupancyPriority(0);
            if (!priorityValueList.contains(priority)) {
                priorityValueList.add(priority);
            }
        }

        Collections.sort(priorityValueList);
        // Reverse the priority value list in descending order since larger value means higher
        // priority
        Collections.reverse(priorityValueList);

        for (int priorityValue : priorityValueList) {
            for (UseCaseConfig<?> config : newUseCaseConfigs) {
                if (priorityValue == config.getSurfaceOccupancyPriority(0)) {
                    priorityOrder.add(newUseCaseConfigs.indexOf(config));
                }
            }
        }

        return priorityOrder;
    }

    private List<List<Size>> getAllPossibleSizeArrangements(
            List<List<Size>> supportedOutputSizesList) {
        int totalArrangementsCount = 1;

        for (List<Size> supportedOutputSizes : supportedOutputSizesList) {
            totalArrangementsCount *= supportedOutputSizes.size();
        }

        // If totalArrangementsCount is 0 means that there may some problem to get
        // supportedOutputSizes
        // for some use case
        if (totalArrangementsCount == 0) {
            throw new IllegalArgumentException("Failed to find supported resolutions.");
        }

        List<List<Size>> allPossibleSizeArrangements = new ArrayList<>();

        // Initialize allPossibleSizeArrangements for the following operations
        for (int i = 0; i < totalArrangementsCount; i++) {
            List<Size> sizeList = new ArrayList<>();
            allPossibleSizeArrangements.add(sizeList);
        }

        /*
         * Try to list out all possible arrangements by attaching all possible size of each column
         * in sequence. We have generated supportedOutputSizesList by the priority order for
         * different use cases. And the supported outputs sizes for each use case are also arranged
         * from large to small. Therefore, the earlier size arrangement in the result list will be
         * the better one to choose if finally it won't exceed the camera device's stream
         * combination capability.
         */
        int currentRunCount = totalArrangementsCount;
        int nextRunCount = currentRunCount / supportedOutputSizesList.get(0).size();

        for (int currentIndex = 0; currentIndex < supportedOutputSizesList.size(); currentIndex++) {
            List<Size> supportedOutputSizes = supportedOutputSizesList.get(currentIndex);
            for (int i = 0; i < totalArrangementsCount; i++) {
                List<Size> surfaceConfigList = allPossibleSizeArrangements.get(i);

                surfaceConfigList.add(
                        supportedOutputSizes.get((i % currentRunCount) / nextRunCount));
            }

            if (currentIndex < supportedOutputSizesList.size() - 1) {
                currentRunCount = nextRunCount;
                nextRunCount =
                        currentRunCount / supportedOutputSizesList.get(currentIndex + 1).size();
            }
        }

        return allPossibleSizeArrangements;
    }

    /**
     * Get max supported output size for specific image format
     *
     * @param map the original stream configuration map without quirks applied.
     * @param imageFormat the image format info
     * @param highResolutionIncluded whether high resolution output sizes are included
     * @return the max supported output size for the image format
     */
    private Size getMaxOutputSizeByFormat(@NonNull StreamConfigurationMap map, int imageFormat,
            boolean highResolutionIncluded, @Nullable Rational aspectRatio) {
        Size[] outputSizes = getOutputSizes(map, imageFormat, aspectRatio);

        if (outputSizes == null || outputSizes.length == 0) {
            return null;
        }

        CompareSizesByArea compareSizesByArea = new CompareSizesByArea();
        Size maxSize = Collections.max(Arrays.asList(outputSizes), compareSizesByArea);

        // Checks high resolution output sizes
        Size maxHighResolutionSize = SizeUtil.RESOLUTION_ZERO;
        if (Build.VERSION.SDK_INT >= 23 && highResolutionIncluded) {
            Size[] highResolutionOutputSizes = Api23Impl.getHighResolutionOutputSizes(map,
                    imageFormat);

            if (highResolutionOutputSizes != null && highResolutionOutputSizes.length > 0) {
                maxHighResolutionSize = Collections.max(Arrays.asList(highResolutionOutputSizes),
                        compareSizesByArea);
            }
        }

        return Collections.max(Arrays.asList(maxSize, maxHighResolutionSize), compareSizesByArea);
    }

    @Nullable
    private static Size @Nullable [] getOutputSizes(@NonNull StreamConfigurationMap map,
            int imageFormat, @Nullable Rational aspectRatio) {
        Size[] outputSizes = null;
        try {
            // b/378508360: try-catch to workaround the exception when using
            // StreamConfigurationMap provided by Robolectric.
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                // This is a little tricky that 0x22 that is internal defined in
                // StreamConfigurationMap.java to be equal to ImageFormat.PRIVATE that is public
                // after Android level 23 but not public in Android L. Use {@link SurfaceTexture}
                // or {@link MediaCodec} will finally mapped to 0x22 in StreamConfigurationMap to
                // retrieve the output sizes information.
                outputSizes = map.getOutputSizes(SurfaceTexture.class);
            } else {
                outputSizes = map.getOutputSizes(imageFormat);
            }
        } catch (Throwable t) {
            // No-Op.
        }

        if (outputSizes == null || outputSizes.length == 0) {
            return null;
        }

        if (aspectRatio != null) {
            List<Size> filteredSizes = new ArrayList<>();
            for (Size size : outputSizes) {
                if (AspectRatioUtil.hasMatchingAspectRatio(size, aspectRatio)) {
                    filteredSizes.add(size);
                }
            }

            if (filteredSizes.isEmpty()) {
                return null;
            }

            outputSizes = filteredSizes.toArray(new Size[0]);
        }

        return outputSizes;
    }

    private void generateSupportedCombinationList() {
        mSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.generateSupportedCombinationList(mHardwareLevel,
                        mIsRawSupported, mIsBurstCaptureSupported));

        mSurfaceCombinations.addAll(mExtraSupportedSurfaceCombinationsContainer.get(mCameraId));
    }

    private void generateUltraHighSupportedCombinationList() {
        mUltraHighSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList());
    }

    private void generateConcurrentSupportedCombinationList() {
        mConcurrentSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.getConcurrentSupportedCombinationList());
    }

    private void generate10BitSupportedCombinationList() {
        mSurfaceCombinations10Bit.addAll(
                GuaranteedConfigurationsUtil.get10BitSupportedCombinationList());
    }

    private void generateUltraHdrSupportedCombinationList() {
        mSurfaceCombinationsUltraHdr.addAll(
                GuaranteedConfigurationsUtil.getUltraHdrSupportedCombinationList());
    }

    private void generateStreamUseCaseSupportedCombinationList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mSurfaceCombinationsStreamUseCase.addAll(
                    GuaranteedConfigurationsUtil.getStreamUseCaseSupportedCombinationList());
        }
    }

    private void generatePreviewStabilizationSupportedCombinationList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mPreviewStabilizationSurfaceCombinations.addAll(
                    GuaranteedConfigurationsUtil.getPreviewStabilizationSupportedCombinationList());
        }
    }

    private void generateHighSpeedSupportedCombinationList() {
        if (!mHighSpeedResolver.isHighSpeedSupported()) {
            return;
        }
        mHighSpeedSurfaceCombinations.clear();
        // Find maximum supported size.
        Size maxSize = mHighSpeedResolver.getMaxSize();
        if (maxSize != null) {
            mHighSpeedSurfaceCombinations.addAll(
                    GuaranteedConfigurationsUtil.generateHighSpeedSupportedCombinationList(maxSize,
                            getUpdatedSurfaceSizeDefinitionByFormat(
                                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)));
        }
    }

    private void generateFcqSurfaceCombinations() {
        mFcqSurfaceCombinations.addAll(generateQueryableFcqCombinations());
    }

    private void checkCustomization() {
        // TODO(b/119466260): Integrate found feasible stream combinations into supported list
    }

    // Utility classes and methods:
    // *********************************************************************************************

    private void generateSurfaceSizeDefinition() {
        Size previewSize = mDisplayInfoManager.getPreviewSize();
        Size recordSize = getRecordSize();
        mSurfaceSizeDefinition = SurfaceSizeDefinition.create(RESOLUTION_VGA,
                new HashMap<>(), // s720pSizeMap
                previewSize,
                new HashMap<>(),
                recordSize, // s1440pSizeMap
                new HashMap<>(), // maximumSizeMap
                new HashMap<>(), // maximum4x3SizeMap
                new HashMap<>(), // maximum16x9SizeMap
                new HashMap<>()); // ultraMaximumSizeMap
    }

    /**
     * Updates the surface size definition for the specified format then return it.
     */
    @VisibleForTesting
    @NonNull SurfaceSizeDefinition getUpdatedSurfaceSizeDefinitionByFormat(int format) {
        if (!mSurfaceSizeDefinitionFormats.contains(format)) {
            updateS720pOrS1440pSizeByFormat(mSurfaceSizeDefinition.getS720pSizeMap(),
                    SizeUtil.RESOLUTION_720P, format);
            updateS720pOrS1440pSizeByFormat(mSurfaceSizeDefinition.getS1440pSizeMap(),
                    SizeUtil.RESOLUTION_1440P, format);
            updateMaximumSizeByFormat(mSurfaceSizeDefinition.getMaximumSizeMap(), format, null);
            updateMaximumSizeByFormat(mSurfaceSizeDefinition.getMaximum4x3SizeMap(), format,
                    AspectRatioUtil.ASPECT_RATIO_4_3);
            updateMaximumSizeByFormat(mSurfaceSizeDefinition.getMaximum16x9SizeMap(), format,
                    AspectRatioUtil.ASPECT_RATIO_16_9);
            updateUltraMaximumSizeByFormat(mSurfaceSizeDefinition.getUltraMaximumSizeMap(), format);
            mSurfaceSizeDefinitionFormats.add(format);
        }
        return mSurfaceSizeDefinition;
    }

    /**
     * Updates the s720p or s720p size to the map for the specified format.
     *
     * <p>s720p refers to the 720p (1280 x 720) or the maximum supported resolution for the
     * particular format returned by {@link StreamConfigurationMap#getOutputSizes(int)},
     * whichever is smaller.
     *
     * <p>s1440p refers to the 1440p (1920 x 1440) or the maximum supported resolution for the
     * particular format returned by {@link StreamConfigurationMap#getOutputSizes(int)},
     * whichever is smaller.
     *
     * @param targetSize the target size to create the map.
     */
    private void updateS720pOrS1440pSizeByFormat(@NonNull Map<Integer, Size> sizeMap,
            @NonNull Size targetSize, int format) {
        if (!mIsConcurrentCameraModeSupported) {
            return;
        }

        StreamConfigurationMap originalMap =
                mCharacteristics.getStreamConfigurationMapCompat().toStreamConfigurationMap();
        Size maxOutputSize = getMaxOutputSizeByFormat(originalMap, format, false, null);
        sizeMap.put(format, maxOutputSize == null ? targetSize
                : Collections.min(Arrays.asList(targetSize, maxOutputSize),
                        new CompareSizesByArea()));
    }

    /**
     * Updates the maximum size to the map for the specified format.
     */
    private void updateMaximumSizeByFormat(@NonNull Map<Integer, Size> sizeMap, int format,
            @Nullable Rational aspectRatio) {
        StreamConfigurationMap originalMap =
                mCharacteristics.getStreamConfigurationMapCompat().toStreamConfigurationMap();
        Size maxOutputSize = getMaxOutputSizeByFormat(originalMap, format, true, aspectRatio);
        if (maxOutputSize != null) {
            sizeMap.put(format, maxOutputSize);
        }
    }

    /**
     * Updates the ultra maximum size to the map for the specified format.
     */
    private void updateUltraMaximumSizeByFormat(@NonNull Map<Integer, Size> sizeMap, int format) {
        // Maximum resolution mode is supported since API level 31
        if (Build.VERSION.SDK_INT < 31 || !mIsUltraHighResolutionSensorSupported) {
            return;
        }

        StreamConfigurationMap maximumResolutionMap = mCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION);

        if (maximumResolutionMap == null) {
            return;
        }

        sizeMap.put(format, getMaxOutputSizeByFormat(maximumResolutionMap, format, true, null));
    }

    private void refreshPreviewSize() {
        mDisplayInfoManager.refresh();
        if (mSurfaceSizeDefinition == null) {
            generateSurfaceSizeDefinition();
        } else {
            Size previewSize = mDisplayInfoManager.getPreviewSize();
            mSurfaceSizeDefinition = SurfaceSizeDefinition.create(
                    mSurfaceSizeDefinition.getAnalysisSize(),
                    mSurfaceSizeDefinition.getS720pSizeMap(),
                    previewSize,
                    mSurfaceSizeDefinition.getS1440pSizeMap(),
                    mSurfaceSizeDefinition.getRecordSize(),
                    mSurfaceSizeDefinition.getMaximumSizeMap(),
                    mSurfaceSizeDefinition.getMaximum4x3SizeMap(),
                    mSurfaceSizeDefinition.getMaximum16x9SizeMap(),
                    mSurfaceSizeDefinition.getUltraMaximumSizeMap());
        }
    }

    /**
     * RECORD refers to the camera device's maximum supported recording resolution, as determined by
     * CamcorderProfile.
     */
    private @NonNull Size getRecordSize() {
        try {
            int cameraId = Integer.parseInt(mCameraId);
            Size recordSize = getRecordSizeFromCamcorderProfile(cameraId);
            if (recordSize != null) {
                return recordSize;
            }
        } catch (NumberFormatException e) {
            // The camera Id is not an integer. The camera may be a removable device.
        }
        // Use StreamConfigurationMap to determine the RECORD size.
        Size recordSize = getRecordSizeFromStreamConfigurationMap();
        if (recordSize != null) {
            return recordSize;
        }

        return RESOLUTION_480P;
    }

    /**
     * Returns the maximum supported video size for cameras using data from the stream
     * configuration map.
     *
     * @return Maximum supported video size or null if none are found.
     */
    private @Nullable Size getRecordSizeFromStreamConfigurationMap() {
        // Determining the record size needs to retrieve the output size from the original stream
        // configuration map without quirks applied.
        StreamConfigurationMapCompat mapCompat = mCharacteristics.getStreamConfigurationMapCompat();
        Size[] videoSizeArr = null;
        try {
            // b/378508360: try-catch to workaround the exception when using
            // StreamConfigurationMap provided by Robolectric.
            videoSizeArr = mapCompat.toStreamConfigurationMap().getOutputSizes(MediaRecorder.class);
        } catch (Throwable t) {
            // No-Op
        }

        if (videoSizeArr == null) {
            return null;
        }

        Arrays.sort(videoSizeArr, new CompareSizesByArea(true));

        for (Size size : videoSizeArr) {
            if (size.getWidth() <= RESOLUTION_1080P.getWidth()
                    && size.getHeight() <= RESOLUTION_1080P.getHeight()) {
                return size;
            }
        }

        return null;
    }

    /**
     * Returns the maximum supported video size for cameras by {@link CamcorderProfile}.
     *
     * @return Maximum supported video size or null if none are found.
     */
    private @Nullable Size getRecordSizeFromCamcorderProfile(int cameraId) {
        int[] qualities = {
                CamcorderProfile.QUALITY_HIGH,
                CamcorderProfile.QUALITY_8KUHD,
                CamcorderProfile.QUALITY_4KDCI,
                CamcorderProfile.QUALITY_2160P,
                CamcorderProfile.QUALITY_2K,
                CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_480P
        };

        for (int quality : qualities) {
            if (mCamcorderProfileHelper.hasProfile(cameraId, quality)) {
                CamcorderProfile profile = mCamcorderProfileHelper.get(cameraId, quality);
                if (profile != null) {
                    return new Size(profile.videoFrameWidth, profile.videoFrameHeight);
                }
            }
        }

        return null;
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        static Size[] getHighResolutionOutputSizes(StreamConfigurationMap streamConfigurationMap,
                int format) {
            return streamConfigurationMap.getHighResolutionOutputSizes(format);
        }

    }

    @AutoValue
    abstract static class BestSizesAndMaxFpsForConfigs {
        static @NonNull BestSizesAndMaxFpsForConfigs of(@Nullable List<Size> bestSizes,
                @Nullable List<Size> bestSizesForStreamUseCase, int maxFpsForBestSizes,
                int maxFpsForStreamUseCase, int maxFpsForAllSizes) {
            return new AutoValue_SupportedSurfaceCombination_BestSizesAndMaxFpsForConfigs(
                    bestSizes, bestSizesForStreamUseCase, maxFpsForBestSizes,
                    maxFpsForStreamUseCase, maxFpsForAllSizes);
        }

        @Nullable abstract List<Size> getBestSizes();

        @Nullable abstract List<Size> getBestSizesForStreamUseCase();

        abstract int getMaxFpsForBestSizes();

        abstract int getMaxFpsForStreamUseCase();

        abstract int getMaxFpsForAllSizes();
    }

    enum CheckingMethod {
        WITHOUT_FEATURE_COMBO,
        WITH_FEATURE_COMBO,
        WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT
    }

    /**
     * A collection of feature settings related to the Camera2 capabilities exposed by
     * {@link CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES} and device features exposed
     * by {@link PackageManager#hasSystemFeature(String)}.
     */
    @AutoValue
    public abstract static class FeatureSettings {
        static @NonNull FeatureSettings of(@CameraMode.Mode int cameraMode,
                boolean hasVideoCapture, @RequiredMaxBitDepth int requiredMaxBitDepth,
                boolean isPreviewStabilizationOn, boolean isUltraHdrOn, boolean isHighSpeedOn,
                boolean isFeatureComboInvocation, boolean requiresFeatureComboQuery,
                @NonNull Range<Integer> targetFpsRange, boolean isStrictFpsRequired) {
            return new AutoValue_SupportedSurfaceCombination_FeatureSettings(cameraMode,
                    hasVideoCapture, requiredMaxBitDepth, isPreviewStabilizationOn, isUltraHdrOn,
                    isHighSpeedOn, isFeatureComboInvocation, requiresFeatureComboQuery,
                    targetFpsRange, isStrictFpsRequired);
        }

        /**
         * The camera mode.
         *
         * <p>This involves the following mapping of mode to feature:
         * <ul>
         *     <li>{@link CameraMode#CONCURRENT_CAMERA} ->
         *         {@link PackageManager#FEATURE_CAMERA_CONCURRENT}
         *     <li>{@link CameraMode#ULTRA_HIGH_RESOLUTION_CAMERA} ->
         *         {@link CameraCharacteristics
         *         #REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR}
         * </ul>
         *
         * <p>A value of {@link CameraMode#DEFAULT} represents the camera operating in its regular
         * capture mode.
         */
        abstract @CameraMode.Mode int getCameraMode();

        /**
         * Whether video capture is added.
         */
        abstract boolean hasVideoCapture();

        /**
         * The required maximum bit depth for any non-RAW stream attached to the camera.
         *
         * <p>A value of {@link DynamicRange#BIT_DEPTH_10_BIT} corresponds
         * to the camera capability
         * {@link CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT}.
         */
        abstract @RequiredMaxBitDepth int getRequiredMaxBitDepth();

        /**
         * Whether the preview stabilization is enabled.
         */
        abstract boolean isPreviewStabilizationOn();

        /**
         * Whether the Ultra HDR image capture is enabled.
         */
        abstract boolean isUltraHdrOn();

        /**
         * Whether the high-speed capture is enabled.
         */
        abstract boolean isHighSpeedOn();

        /**
         * Whether the code invocation is started through CameraX feature combination APIs.
         *
         * @see androidx.camera.core.CameraInfo#isFeatureGroupSupported
         * @see androidx.camera.core.SessionConfig#requiredFeatureGroup
         * @see androidx.camera.core.SessionConfig#preferredFeatureGroup
         */
        abstract boolean isFeatureComboInvocation();

        /**
         * Whether feature combination query (i.e. the Camera2/Jetpack FCQ APIs) is required for
         * checking if config combinations are supported.
         */
        abstract boolean requiresFeatureComboQuery();

        /** Gets the target FPS range, null if none. */
        abstract @NonNull Range<Integer> getTargetFpsRange();

        /** Whether strict frame rate is required. */
        abstract boolean isStrictFpsRequired();
    }
}
