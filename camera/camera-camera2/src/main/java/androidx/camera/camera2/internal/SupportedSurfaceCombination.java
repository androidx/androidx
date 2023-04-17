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

import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P;
import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA;

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
import android.util.Range;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat;
import androidx.camera.camera2.internal.compat.workaround.ExtraSupportedSurfaceCombinationsContainer;
import androidx.camera.camera2.internal.compat.workaround.ResolutionCorrector;
import androidx.camera.camera2.internal.compat.workaround.TargetAspectRatio;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceSizeDefinition;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camera device supported surface configuration combinations
 *
 * <p>{@link CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store a list of surface combinations that are guaranteed to
 * support for this camera device.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SupportedSurfaceCombination {
    private static final String TAG = "SupportedSurfaceCombination";
    private final List<SurfaceCombination> mSurfaceCombinations = new ArrayList<>();
    private final List<SurfaceCombination> mUltraHighSurfaceCombinations = new ArrayList<>();
    private final List<SurfaceCombination> mConcurrentSurfaceCombinations = new ArrayList<>();
    private final Map<FeatureSettings, List<SurfaceCombination>>
            mFeatureSettingsToSupportedCombinationsMap = new HashMap<>();
    private final List<SurfaceCombination> mSurfaceCombinations10Bit = new ArrayList<>();
    private final String mCameraId;
    private final CamcorderProfileHelper mCamcorderProfileHelper;
    private final CameraCharacteristicsCompat mCharacteristics;
    private final ExtraSupportedSurfaceCombinationsContainer
            mExtraSupportedSurfaceCombinationsContainer;
    private final int mHardwareLevel;
    private boolean mIsRawSupported = false;
    private boolean mIsBurstCaptureSupported = false;
    private boolean mIsConcurrentCameraModeSupported = false;
    private boolean mIsUltraHighResolutionSensorSupported = false;
    @VisibleForTesting
    SurfaceSizeDefinition mSurfaceSizeDefinition;
    List<Integer> mSurfaceSizeDefinitionFormats = new ArrayList<>();
    @NonNull
    private final DisplayInfoManager mDisplayInfoManager;

    private final TargetAspectRatio mTargetAspectRatio = new TargetAspectRatio();
    private final ResolutionCorrector mResolutionCorrector = new ResolutionCorrector();
    private final DynamicRangeResolver mDynamicRangeResolver;

    @IntDef({DynamicRange.BIT_DEPTH_8_BIT, DynamicRange.BIT_DEPTH_10_BIT})
    @Retention(RetentionPolicy.SOURCE)
    @interface RequiredMaxBitDepth {}

    SupportedSurfaceCombination(@NonNull Context context, @NonNull String cameraId,
            @NonNull CameraManagerCompat cameraManagerCompat,
            @NonNull CamcorderProfileHelper camcorderProfileHelper)
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
                }
            }
        }

        mDynamicRangeResolver = new DynamicRangeResolver(mCharacteristics);
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

        generateSurfaceSizeDefinition();
        checkCustomization();
    }

    String getCameraId() {
        return mCameraId;
    }

    boolean isRawSupported() {
        return mIsRawSupported;
    }

    boolean isBurstCaptureSupported() {
        return mIsBurstCaptureSupported;
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param featureSettings  the settings for the camera's features/capabilities.
     * @param surfaceConfigList the surface configuration list to be compared
     *
     * @return the check result that whether it could be supported
     */
    boolean checkSupported(
            @NonNull FeatureSettings featureSettings,
            List<SurfaceConfig> surfaceConfigList) {
        boolean isSupported = false;

        for (SurfaceCombination surfaceCombination : getSurfaceCombinationsByFeatureSettings(
                featureSettings)) {
            isSupported = surfaceCombination.isSupported(surfaceConfigList);

            if (isSupported) {
                break;
            }
        }

        return isSupported;
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

        if (featureSettings.getRequiredMaxBitDepth() == DynamicRange.BIT_DEPTH_8_BIT) {
            switch (featureSettings.getCameraMode()) {
                case CameraMode.CONCURRENT_CAMERA:
                    supportedSurfaceCombinations = mConcurrentSurfaceCombinations;
                    break;
                case CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA:
                    supportedSurfaceCombinations.addAll(mUltraHighSurfaceCombinations);
                    supportedSurfaceCombinations.addAll(mSurfaceCombinations);
                    break;
                default:
                    supportedSurfaceCombinations.addAll(mSurfaceCombinations);
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
     * @return new {@link SurfaceConfig} object
     */
    SurfaceConfig transformSurfaceConfig(
            @CameraMode.Mode int cameraMode,
            int imageFormat,
            Size size) {
        return SurfaceConfig.transformSurfaceConfig(
                cameraMode,
                imageFormat,
                size,
                getUpdatedSurfaceSizeDefinitionByFormat(imageFormat));
    }

    static int getMaxFramerate(CameraCharacteristicsCompat characteristics, int imageFormat,
            Size size) {
        int maxFramerate = 0;
        try {
            maxFramerate = (int) (1000000000.0
                    / characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputMinFrameDuration(imageFormat,
                            size));
        } catch (Exception e) {
            //TODO
            //this try catch is in place for the rare that a surface config has a size
            // incompatible for getOutputMinFrameDuration...  put into a Quirk
        }
        return maxFramerate;
    }

    /**
     *
     * @param range
     * @return the length of the range
     */
    private static int getRangeLength(Range<Integer> range) {
        return (range.getUpper() - range.getLower()) + 1;
    }

    /**
     * @return the distance between the nearest limits of two non-intersecting ranges
     */
    private static int getRangeDistance(Range<Integer> firstRange, Range<Integer> secondRange) {
        Preconditions.checkState(
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

        double storedIntersectionsize = getRangeLength(storedRange.intersect(targetFps));
        double newIntersectionSize = getRangeLength(newRange.intersect(targetFps));

        double newRangeRatio = newIntersectionSize / getRangeLength(newRange);
        double storedRangeRatio = storedIntersectionsize / getRangeLength(storedRange);

        if (newIntersectionSize > storedIntersectionsize) {
            // if new, the new range must have at least 50% of its range intersecting, OR has a
            // larger percentage of intersection than the previous stored range
            if (newRangeRatio >= .5 || newRangeRatio >= storedRangeRatio) {
                return newRange;
            }
        } else if (newIntersectionSize == storedIntersectionsize) {
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
     * Finds a frame rate range supported by the device that is closest to the target framerate
     *
     * @param targetFrameRate the Target Frame Rate resolved from all current existing surfaces
     *                        and incoming new use cases
     * @return a frame rate range supported by the device that is closest to targetFrameRate
     */

    @NonNull
    private Range<Integer> getClosestSupportedDeviceFrameRate(Range<Integer> targetFrameRate,
            int maxFps) {
        if (targetFrameRate == null) {
            return StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
        }

        // get all fps ranges supported by device
        Range<Integer>[] availableFpsRanges =
                mCharacteristics.get(CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        if (availableFpsRanges == null) {
            return StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
        }
        // if  whole target framerate range > maxFps of configuration, the target for this
        // calculation will be [max,max].

        // if the range is partially larger than  maxFps, the target for this calculation will be
        // [target.lower, max] for the sake of this calculation
        targetFrameRate = new Range<>(
                Math.min(targetFrameRate.getLower(), maxFps),
                Math.min(targetFrameRate.getUpper(), maxFps)
        );

        Range<Integer> bestRange = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
        int currentIntersectSize = 0;


        for (Range<Integer> potentialRange : availableFpsRanges) {
            // ignore ranges completely larger than configuration's maximum fps
            if (maxFps >= potentialRange.getLower()) {
                if (bestRange.equals(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)) {
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
     * @param newTargetFramerate    an incoming framerate range
     * @param storedTargetFramerate a stored framerate range to be modified
     * @return adjusted target frame rate
     *
     * If the two ranges are both nonnull and disjoint of each other, then the range that was
     * already stored will be used
     */
    private Range<Integer> getUpdatedTargetFramerate(Range<Integer> newTargetFramerate,
            Range<Integer> storedTargetFramerate) {
        Range<Integer> updatedTarget = storedTargetFramerate;

        if (storedTargetFramerate == null) {
            // if stored value was null before, set it to the new value
            updatedTarget = newTargetFramerate;
        } else if (newTargetFramerate != null) {
            try {
                // get intersection of existing target fps
                updatedTarget =
                        storedTargetFramerate
                                .intersect(newTargetFramerate);
            } catch (IllegalArgumentException e) {
                // no intersection, keep the previously stored value
                updatedTarget = storedTargetFramerate;
            }
        }
        return updatedTarget;
    }

    /**
     * @param currentMaxFps the previously stored Max FPS
     * @param imageFormat   the image format of the incoming surface
     * @param size          the size of the incoming surface
     */
    private int getUpdatedMaximumFps(int currentMaxFps, int imageFormat, Size size) {
        return Math.min(currentMaxFps, getMaxFramerate(mCharacteristics, imageFormat, size));
    }

    /**
     * Finds the suggested stream specifications of the newly added UseCaseConfig.
     *
     * @param cameraMode                        the working camera mode.
     * @param attachedSurfaces                  the existing surfaces.
     * @param newUseCaseConfigsSupportedSizeMap newly added UseCaseConfig to supported output
     *                                          sizes map.
     * @return the suggested stream specifications, which is a mapping from UseCaseConfig to the
     * suggested stream specification.
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *                                  found. This may be due to no available output size, no
     *                                  available surface combination, unsupported combinations
     *                                  of {@link DynamicRange}, or requiring an
     *                                  unsupported combination of camera features.
     */
    @NonNull
    Map<UseCaseConfig<?>, StreamSpec> getSuggestedStreamSpecifications(
            @CameraMode.Mode int cameraMode,
            @NonNull List<AttachedSurfaceInfo> attachedSurfaces,
            @NonNull Map<UseCaseConfig<?>, List<Size>> newUseCaseConfigsSupportedSizeMap) {
        // Refresh Preview Size based on current display configurations.
        refreshPreviewSize();
        List<SurfaceConfig> surfaceConfigs = new ArrayList<>();
        for (AttachedSurfaceInfo attachedSurface : attachedSurfaces) {
            surfaceConfigs.add(attachedSurface.getSurfaceConfig());
        }

        List<UseCaseConfig<?>> newUseCaseConfigs = new ArrayList<>(
                newUseCaseConfigsSupportedSizeMap.keySet());

        // Get the index order list by the use case priority for finding stream configuration
        List<Integer> useCasesPriorityOrder = getUseCasesPriorityOrder(newUseCaseConfigs);
        Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges =
                mDynamicRangeResolver.resolveAndValidateDynamicRanges(attachedSurfaces,
                        newUseCaseConfigs, useCasesPriorityOrder);
        int requiredMaxBitDepth = getRequiredMaxBitDepth(resolvedDynamicRanges);
        FeatureSettings featureSettings = FeatureSettings.of(cameraMode, requiredMaxBitDepth);
        if (cameraMode != CameraMode.DEFAULT
                && requiredMaxBitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
            throw new IllegalArgumentException(String.format("No supported surface combination is "
                            + "found for camera device - Id : %s. Ten bit dynamic range is not "
                            + "currently supported in %s camera mode.", mCameraId,
                    CameraMode.toLabelString(cameraMode)));
        }

        // Use the small size (640x480) for new use cases to check whether there is any possible
        // supported combination first
        for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
            int imageFormat = useCaseConfig.getInputFormat();
            surfaceConfigs.add(
                    SurfaceConfig.transformSurfaceConfig(
                            cameraMode,
                            imageFormat,
                            new Size(640, 480),
                            getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)));
        }

        if (!checkSupported(featureSettings, surfaceConfigs)) {
            throw new IllegalArgumentException(
                    "No supported surface combination is found for camera device - Id : "
                            + mCameraId + ".  May be attempting to bind too many use cases. "
                            + "Existing surfaces: " + attachedSurfaces + " New configs: "
                            + newUseCaseConfigs);
        }

        Range<Integer> targetFramerateForConfig = null;
        int existingSurfaceFrameRateCeiling = Integer.MAX_VALUE;

        for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
            // init target fps range for new configs from existing surfaces
            targetFramerateForConfig = getUpdatedTargetFramerate(
                    attachedSurfaceInfo.getTargetFrameRate(),
                    targetFramerateForConfig);
            //get the fps ceiling for existing surfaces
            existingSurfaceFrameRateCeiling = getUpdatedMaximumFps(
                    existingSurfaceFrameRateCeiling,
                    attachedSurfaceInfo.getImageFormat(), attachedSurfaceInfo.getSize());
        }

        List<List<Size>> supportedOutputSizesList = new ArrayList<>();

        // Collect supported output sizes for all use cases
        for (Integer index : useCasesPriorityOrder) {
            UseCaseConfig<?> useCaseConfig = newUseCaseConfigs.get(index);
            List<Size> supportedOutputSizes = newUseCaseConfigsSupportedSizeMap.get(useCaseConfig);
            supportedOutputSizes = applyResolutionSelectionOrderRelatedWorkarounds(
                    supportedOutputSizes, useCaseConfig.getInputFormat());
            supportedOutputSizesList.add(supportedOutputSizes);
        }

        // Get all possible size arrangements
        List<List<Size>> allPossibleSizeArrangements =
                getAllPossibleSizeArrangements(
                        supportedOutputSizesList);

        // update target fps for new configs using new use cases' priority order
        for (Integer index : useCasesPriorityOrder) {
            targetFramerateForConfig =
                    getUpdatedTargetFramerate(
                            newUseCaseConfigs.get(index).getTargetFrameRate(null),
                            targetFramerateForConfig);
        }

        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap;
        List<Size> savedSizes = null;
        int savedConfigMaxFps = Integer.MAX_VALUE;

        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (List<Size> possibleSizeList : allPossibleSizeArrangements) {
            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            List<SurfaceConfig> surfaceConfigList = new ArrayList<>();
            int currentConfigFramerateCeiling = existingSurfaceFrameRateCeiling;
            boolean isConfigFrameRateAcceptable = true;

            for (AttachedSurfaceInfo attachedSurfaceInfo : attachedSurfaces) {
                surfaceConfigList.add(attachedSurfaceInfo.getSurfaceConfig());
            }

            // Attach SurfaceConfig of new use cases
            for (int i = 0; i < possibleSizeList.size(); i++) {
                Size size = possibleSizeList.get(i);
                UseCaseConfig<?> newUseCase =
                        newUseCaseConfigs.get(useCasesPriorityOrder.get(i));
                int imageFormat = newUseCase.getInputFormat();
                // add new use case/size config to list of surfaces
                surfaceConfigList.add(
                        SurfaceConfig.transformSurfaceConfig(
                                cameraMode,
                                imageFormat,
                                size,
                                getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)));

                // get the maximum fps of the new surface and update the maximum fps of the
                // proposed configuration
                currentConfigFramerateCeiling = getUpdatedMaximumFps(
                        currentConfigFramerateCeiling,
                        newUseCase.getInputFormat(),
                        size);
            }
            if (targetFramerateForConfig != null) {
                if (existingSurfaceFrameRateCeiling > currentConfigFramerateCeiling
                        && currentConfigFramerateCeiling < targetFramerateForConfig.getLower()) {
                    // if the max fps before adding new use cases supports our target fps range
                    // BUT the max fps of the new configuration is below
                    // our target fps range, we'll want to check the next configuration until we
                    // get one that supports our target FPS
                    isConfigFrameRateAcceptable = false;
                }
            }

            // only change the saved config if you get another that has a better max fps
            if (checkSupported(featureSettings, surfaceConfigList)) {
                // if the config is supported by the device but doesn't meet the target framerate,
                // save the config
                if (savedConfigMaxFps == Integer.MAX_VALUE) {
                    savedConfigMaxFps = currentConfigFramerateCeiling;
                    savedSizes = possibleSizeList;
                } else if (savedConfigMaxFps < currentConfigFramerateCeiling) {
                    // only change the saved config if the max fps is better
                    savedConfigMaxFps = currentConfigFramerateCeiling;
                    savedSizes = possibleSizeList;
                }

                // if we have a configuration where the max fps is acceptable for our target, break
                if (isConfigFrameRateAcceptable) {
                    savedConfigMaxFps = currentConfigFramerateCeiling;
                    savedSizes = possibleSizeList;
                    break;
                }
            }
        }

        // Map the saved supported SurfaceConfig combination
        if (savedSizes != null) {
            Range<Integer> targetFramerateForDevice = null;
            if (targetFramerateForConfig != null) {
                targetFramerateForDevice =
                        getClosestSupportedDeviceFrameRate(targetFramerateForConfig,
                                savedConfigMaxFps);
            }
            suggestedStreamSpecMap = new HashMap<>();
            for (UseCaseConfig<?> useCaseConfig : newUseCaseConfigs) {
                Size resolutionForUseCase = savedSizes.get(
                        useCasesPriorityOrder.indexOf(newUseCaseConfigs.indexOf(useCaseConfig)));
                StreamSpec.Builder streamSpecBuilder = StreamSpec.builder(resolutionForUseCase)
                        .setDynamicRange(Preconditions.checkNotNull(
                                resolvedDynamicRanges.get(useCaseConfig)));
                if (targetFramerateForDevice != null) {
                    streamSpecBuilder.setExpectedFrameRateRange(targetFramerateForDevice);
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
        return suggestedStreamSpecMap;
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
    @NonNull
    List<Size> applyResolutionSelectionOrderRelatedWorkarounds(@NonNull List<Size> sizeList,
            int imageFormat) {
        // Applies TargetAspectRatio workaround
        int targetAspectRatio = mTargetAspectRatio.get(mCameraId, mCharacteristics);
        Rational ratio = null;

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
                ratio = new Rational(maxJpegSize.getWidth(), maxJpegSize.getHeight());
                break;
            case TargetAspectRatio.RATIO_ORIGINAL:
                ratio = null;
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

    private List<Integer> getUseCasesPriorityOrder(List<UseCaseConfig<?>> newUseCaseConfigs) {
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
    private Size getMaxOutputSizeByFormat(StreamConfigurationMap map, int imageFormat,
            boolean highResolutionIncluded) {
        Size[] outputSizes;
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

    private void generateSupportedCombinationList() {
        mSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.generateSupportedCombinationList(mHardwareLevel,
                        mIsRawSupported, mIsBurstCaptureSupported));

        mSurfaceCombinations.addAll(
                mExtraSupportedSurfaceCombinationsContainer.get(mCameraId, mHardwareLevel));
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
                new HashMap<>()); // ultraMaximumSizeMap
    }

    /**
     * Updates the surface size definition for the specified format then return it.
     */
    @VisibleForTesting
    @NonNull
    SurfaceSizeDefinition getUpdatedSurfaceSizeDefinitionByFormat(int format) {
        if (!mSurfaceSizeDefinitionFormats.contains(format)) {
            updateS720pOrS1440pSizeByFormat(mSurfaceSizeDefinition.getS720pSizeMap(),
                    SizeUtil.RESOLUTION_720P, format);
            updateS720pOrS1440pSizeByFormat(mSurfaceSizeDefinition.getS1440pSizeMap(),
                    SizeUtil.RESOLUTION_1440P, format);
            updateMaximumSizeByFormat(mSurfaceSizeDefinition.getMaximumSizeMap(), format);
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
     * @return the format to s720p or s720p size map.
     */
    private void updateS720pOrS1440pSizeByFormat(@NonNull Map<Integer, Size> sizeMap,
            @NonNull Size targetSize, int format) {
        if (!mIsConcurrentCameraModeSupported) {
            return;
        }

        StreamConfigurationMap originalMap =
                mCharacteristics.getStreamConfigurationMapCompat().toStreamConfigurationMap();
        Size maxOutputSize = getMaxOutputSizeByFormat(originalMap, format, false);
        sizeMap.put(format, maxOutputSize == null ? targetSize
                : Collections.min(Arrays.asList(targetSize, maxOutputSize),
                        new CompareSizesByArea()));
    }

    /**
     * Updates the maximum size to the map for the specified format.
     */
    private void updateMaximumSizeByFormat(@NonNull Map<Integer, Size> sizeMap, int format) {
        StreamConfigurationMap originalMap =
                mCharacteristics.getStreamConfigurationMapCompat().toStreamConfigurationMap();
        Size maxOutputSize = getMaxOutputSizeByFormat(originalMap, format, true);
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

        sizeMap.put(format, getMaxOutputSizeByFormat(maximumResolutionMap, format, true));
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
                    mSurfaceSizeDefinition.getUltraMaximumSizeMap());
        }
    }

    /**
     * RECORD refers to the camera device's maximum supported recording resolution, as determined by
     * CamcorderProfile.
     */
    @NonNull
    private Size getRecordSize() {
        int cameraId;

        try {
            cameraId = Integer.parseInt(mCameraId);
        } catch (NumberFormatException e) {
            // The camera Id is not an integer because the camera may be a removable device. Use
            // StreamConfigurationMap to determine the RECORD size.
            return getRecordSizeFromStreamConfigurationMap();
        }

        CamcorderProfile profile = null;

        if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        }

        if (profile != null) {
            return new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        return getRecordSizeByHasProfile(cameraId);
    }

    /**
     * Return the maximum supported video size for cameras using data from the stream
     * configuration map.
     *
     * @return Maximum supported video size.
     */
    @NonNull
    private Size getRecordSizeFromStreamConfigurationMap() {
        // Determining the record size needs to retrieve the output size from the original stream
        // configuration map without quirks applied.
        StreamConfigurationMapCompat mapCompat = mCharacteristics.getStreamConfigurationMapCompat();
        Size[] videoSizeArr = mapCompat.toStreamConfigurationMap().getOutputSizes(
                MediaRecorder.class);

        if (videoSizeArr == null) {
            return RESOLUTION_480P;
        }

        Arrays.sort(videoSizeArr, new CompareSizesByArea(true));

        for (Size size : videoSizeArr) {
            // Returns the largest supported size under 1080P
            if (size.getWidth() <= RESOLUTION_1080P.getWidth()
                    && size.getHeight() <= RESOLUTION_1080P.getHeight()) {
                return size;
            }
        }

        return RESOLUTION_480P;
    }

    /**
     * Return the maximum supported video size for cameras by
     * {@link CamcorderProfile#hasProfile(int, int)}.
     *
     * @return Maximum supported video size.
     */
    @NonNull
    private Size getRecordSizeByHasProfile(int cameraId) {
        Size recordSize = RESOLUTION_480P;
        CamcorderProfile profile = null;

        // Check whether 4KDCI, 2160P, 2K, 1080P, 720P, 480P (sorted by size) are supported by
        // CamcorderProfile
        if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_4KDCI)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_4KDCI);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_2160P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_2160P);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_2K)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_2K);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_1080P);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_720P);
        } else if (mCamcorderProfileHelper.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            profile = mCamcorderProfileHelper.get(cameraId, CamcorderProfile.QUALITY_480P);
        }

        if (profile != null) {
            recordSize = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        }

        return recordSize;
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Size[] getHighResolutionOutputSizes(StreamConfigurationMap streamConfigurationMap,
                int format) {
            return streamConfigurationMap.getHighResolutionOutputSizes(format);
        }

    }

    /**
     * A collection of feature settings related to the Camera2 capabilities exposed by
     * {@link CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES} and device features exposed
     * by {@link PackageManager#hasSystemFeature(String)}.
     */
    @AutoValue
    abstract static class FeatureSettings {
        @NonNull
        static FeatureSettings of(@CameraMode.Mode int cameraMode,
                @RequiredMaxBitDepth int requiredMaxBitDepth) {
            return new AutoValue_SupportedSurfaceCombination_FeatureSettings(
                    cameraMode, requiredMaxBitDepth);
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
         * The required maximum bit depth for any non-RAW stream attached to the camera.
         *
         * <p>A value of {@link androidx.camera.core.DynamicRange#BIT_DEPTH_10_BIT} corresponds
         * to the camera capability
         * {@link CameraCharacteristics#REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT}.
         */
        abstract @RequiredMaxBitDepth int getRequiredMaxBitDepth();
    }
}
