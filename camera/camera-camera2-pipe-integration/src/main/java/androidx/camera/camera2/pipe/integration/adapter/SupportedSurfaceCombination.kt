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

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.util.Pair
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.ExtraSupportedSurfaceCombinationsContainer
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.ResolutionCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.TargetAspectRatio
import androidx.camera.camera2.pipe.integration.impl.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.internal.DynamicRangeResolver
import androidx.camera.camera2.pipe.integration.internal.StreamUseCaseUtil
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceSizeDefinition
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1440P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import java.util.Arrays
import java.util.Collections
import kotlin.math.floor
import kotlin.math.min

/**
 * Camera device supported surface configuration combinations
 *
 * <p>{@link android.hardware.camera2.CameraDevice#createCaptureSession} defines the default
 * guaranteed stream combinations for different hardware level devices. It defines what combination
 * of surface configuration type and size pairs can be supported for different hardware level camera
 * devices. This structure is used to store a list of surface combinations that are guaranteed to
 * support for this camera device.
 */
@Suppress("DEPRECATION")
// TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SupportedSurfaceCombination(
    context: Context,
    private val cameraMetadata: CameraMetadata,
    private val encoderProfilesProviderAdapter: EncoderProfilesProviderAdapter
) {
    private val cameraId = cameraMetadata.camera.value
    private val hardwareLevel =
        cameraMetadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    private val availableStabilizationMode =
        cameraMetadata[CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES]
            ?: CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
    private val concurrentSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinationsStreamUseCase: MutableList<SurfaceCombination> = mutableListOf()
    private val ultraHighSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val previewStabilizationSurfaceCombinations: MutableList<SurfaceCombination> =
        mutableListOf()
    private val featureSettingsToSupportedCombinationsMap:
        MutableMap<FeatureSettings, List<SurfaceCombination>> =
        mutableMapOf()
    private val surfaceCombinations10Bit: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinationsUltraHdr: MutableList<SurfaceCombination> = mutableListOf()
    private var isRawSupported = false
    private var isBurstCaptureSupported = false
    private var isConcurrentCameraModeSupported = false
    private var isStreamUseCaseSupported = false
    private var isUltraHighResolutionSensorSupported = false
    private var isPreviewStabilizationSupported = false
    internal lateinit var surfaceSizeDefinition: SurfaceSizeDefinition
    private val surfaceSizeDefinitionFormats = mutableListOf<Int>()
    private val streamConfigurationMapCompat = getStreamConfigurationMapCompat()
    private val extraSupportedSurfaceCombinationsContainer =
        ExtraSupportedSurfaceCombinationsContainer()
    private val displayInfoManager = DisplayInfoManager(context)
    private val resolutionCorrector = ResolutionCorrector()
    private val targetAspectRatio: TargetAspectRatio = TargetAspectRatio()
    private val dynamicRangeResolver: DynamicRangeResolver = DynamicRangeResolver(cameraMetadata)

    init {
        checkCapabilities()
        generateSupportedCombinationList()
        if (isUltraHighResolutionSensorSupported) {
            generateUltraHighResolutionSupportedCombinationList()
        }
        isConcurrentCameraModeSupported =
            context.packageManager.hasSystemFeature(FEATURE_CAMERA_CONCURRENT)
        if (isConcurrentCameraModeSupported) {
            generateConcurrentSupportedCombinationList()
        }

        if (dynamicRangeResolver.is10BitDynamicRangeSupported()) {
            generate10BitSupportedCombinationList()

            if (isUltraHdrSupported()) {
                generateUltraHdrSupportedCombinationList()
            }
        }

        if (isPreviewStabilizationSupported) {
            generatePreviewStabilizationSupportedCombinationList()
        }

        isStreamUseCaseSupported = StreamUseCaseUtil.isStreamUseCaseSupported(cameraMetadata)
        if (isStreamUseCaseSupported) {
            generateStreamUseCaseSupportedCombinationList()
        }

        generateSurfaceSizeDefinition()
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param featureSettings the settings for the camera's features/capabilities.
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    public fun checkSupported(
        featureSettings: FeatureSettings,
        surfaceConfigList: List<SurfaceConfig>
    ): Boolean {
        return getSurfaceCombinationsByFeatureSettings(featureSettings).any {
            it.getOrderedSupportedSurfaceConfigList(surfaceConfigList) != null
        }
    }

    private fun isUltraHdrSupported(): Boolean {
        return getStreamConfigurationMapCompat().getOutputFormats()?.contains(ImageFormat.JPEG_R)
            ?: false
    }

    private fun getOrderedSupportedStreamUseCaseSurfaceConfigList(
        featureSettings: FeatureSettings,
        surfaceConfigList: List<SurfaceConfig?>?
    ): List<SurfaceConfig>? {
        if (!StreamUseCaseUtil.shouldUseStreamUseCase(featureSettings)) {
            return null
        }
        for (surfaceCombination in surfaceCombinationsStreamUseCase) {
            val orderedSurfaceConfigList =
                surfaceCombination.getOrderedSupportedSurfaceConfigList(surfaceConfigList!!)
            if (orderedSurfaceConfigList != null) {
                return orderedSurfaceConfigList
            }
        }
        return null
    }

    /** Returns the supported surface combinations according to the specified feature settings. */
    private fun getSurfaceCombinationsByFeatureSettings(
        featureSettings: FeatureSettings
    ): List<SurfaceCombination> {
        if (featureSettingsToSupportedCombinationsMap.containsKey(featureSettings)) {
            return featureSettingsToSupportedCombinationsMap[featureSettings]!!
        }
        var supportedSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
        if (featureSettings.requiredMaxBitDepth == DynamicRange.BIT_DEPTH_8_BIT) {
            when (featureSettings.cameraMode) {
                CameraMode.CONCURRENT_CAMERA ->
                    supportedSurfaceCombinations = concurrentSurfaceCombinations
                CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA -> {
                    supportedSurfaceCombinations.addAll(ultraHighSurfaceCombinations)
                    supportedSurfaceCombinations.addAll(surfaceCombinations)
                }
                else -> {
                    supportedSurfaceCombinations.addAll(
                        if (featureSettings.isPreviewStabilizationOn)
                            previewStabilizationSurfaceCombinations
                        else surfaceCombinations
                    )
                }
            }
        } else if (featureSettings.requiredMaxBitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
            // For 10-bit outputs, only the default camera mode is currently supported.
            if (featureSettings.cameraMode == CameraMode.DEFAULT) {
                if (featureSettings.isUltraHdrOn) {
                    supportedSurfaceCombinations.addAll(surfaceCombinationsUltraHdr)
                } else {
                    supportedSurfaceCombinations.addAll(surfaceCombinations10Bit)
                }
            }
        }
        featureSettingsToSupportedCombinationsMap[featureSettings] = supportedSurfaceCombinations
        return supportedSurfaceCombinations
    }

    /**
     * Transform to a SurfaceConfig object with image format and size info
     *
     * @param cameraMode the working camera mode.
     * @param imageFormat the image format info for the surface configuration object
     * @param size the size info for the surface configuration object
     * @return new [SurfaceConfig] object
     */
    public fun transformSurfaceConfig(
        cameraMode: Int,
        imageFormat: Int,
        size: Size
    ): SurfaceConfig {
        return SurfaceConfig.transformSurfaceConfig(
            cameraMode,
            imageFormat,
            size,
            getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        )
    }

    /**
     * Finds the suggested stream specification of the newly added UseCaseConfig.
     *
     * @param cameraMode the working camera mode.
     * @param attachedSurfaces the existing surfaces.
     * @param newUseCaseConfigsSupportedSizeMap newly added UseCaseConfig to supported output sizes
     *   map.
     * @param isPreviewStabilizationOn whether the preview stabilization is enabled.
     * @param hasVideoCapture whether the use cases has video capture.
     * @return the suggested stream specs, which is a mapping from UseCaseConfig to the suggested
     *   stream specification.
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *   found. This may be due to no available output size or no available surface combination.
     */
    public fun getSuggestedStreamSpecifications(
        cameraMode: Int,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        isPreviewStabilizationOn: Boolean = false,
        hasVideoCapture: Boolean = false
    ): Pair<Map<UseCaseConfig<*>, StreamSpec>, Map<AttachedSurfaceInfo, StreamSpec>> {
        // Refresh Preview Size based on current display configurations.
        refreshPreviewSize()

        val newUseCaseConfigs = newUseCaseConfigsSupportedSizeMap.keys.toList()

        // Get the index order list by the use case priority for finding stream configuration
        val useCasesPriorityOrder = getUseCasesPriorityOrder(newUseCaseConfigs)
        val resolvedDynamicRanges =
            dynamicRangeResolver.resolveAndValidateDynamicRanges(
                attachedSurfaces,
                newUseCaseConfigs,
                useCasesPriorityOrder
            )
        val isUltraHdrOn = isUltraHdrOn(attachedSurfaces, newUseCaseConfigsSupportedSizeMap)
        val featureSettings =
            createFeatureSettings(
                cameraMode,
                resolvedDynamicRanges,
                isPreviewStabilizationOn,
                isUltraHdrOn
            )
        val isSurfaceCombinationSupported =
            isUseCasesCombinationSupported(
                featureSettings,
                attachedSurfaces,
                newUseCaseConfigsSupportedSizeMap
            )
        require(isSurfaceCombinationSupported) {
            "No supported surface combination is found for camera device - Id : $cameraId. " +
                "May be attempting to bind too many use cases. Existing surfaces: " +
                "$attachedSurfaces. New configs: $newUseCaseConfigs."
        }

        // Calculates the target FPS range
        val targetFpsRange =
            getTargetFpsRange(attachedSurfaces, newUseCaseConfigs, useCasesPriorityOrder)
        // Filters the unnecessary output sizes for performance improvement. This will
        // significantly reduce the number of all possible size arrangements below.
        val useCaseConfigToFilteredSupportedSizesMap =
            filterSupportedSizes(newUseCaseConfigsSupportedSizeMap, featureSettings, targetFpsRange)
        // The two maps are used to keep track of the attachedSurfaceInfo or useCaseConfigs the
        // surfaceConfigs are made from. They are populated in getSurfaceConfigListAndFpsCeiling().
        // The keys are the position of their corresponding surfaceConfigs in the list. We can
        // them map streamUseCases in orderedSurfaceConfigListForStreamUseCase, which is in the
        // same order as surfaceConfigs list, to the original useCases to determine the
        // captureTypes are correct.
        val surfaceConfigIndexAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo> =
            mutableMapOf()
        val surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = mutableMapOf()
        val allPossibleSizeArrangements =
            getAllPossibleSizeArrangements(
                getSupportedOutputSizesList(
                    useCaseConfigToFilteredSupportedSizesMap,
                    newUseCaseConfigs,
                    useCasesPriorityOrder
                )
            )

        val containsZsl: Boolean =
            StreamUseCaseUtil.containsZslUseCase(attachedSurfaces, newUseCaseConfigs)
        var orderedSurfaceConfigListForStreamUseCase: List<SurfaceConfig>? = null
        // Only checks the stream use case combination support when ZSL is not required.
        if (isStreamUseCaseSupported && !containsZsl) {
            orderedSurfaceConfigListForStreamUseCase =
                getOrderedSurfaceConfigListForStreamUseCase(
                    allPossibleSizeArrangements,
                    attachedSurfaces,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    featureSettings,
                    isSurfaceCombinationSupported,
                    surfaceConfigIndexAttachedSurfaceInfoMap,
                    surfaceConfigIndexUseCaseConfigMap
                )
        }

        val maxSupportedFps = getMaxSupportedFpsFromAttachedSurfaces(attachedSurfaces)
        val bestSizesAndFps =
            findBestSizesAndFps(
                allPossibleSizeArrangements,
                attachedSurfaces,
                newUseCaseConfigs,
                maxSupportedFps,
                useCasesPriorityOrder,
                targetFpsRange,
                featureSettings,
                orderedSurfaceConfigListForStreamUseCase
            )

        val suggestedStreamSpecMap =
            generateSuggestedStreamSpecMap(
                bestSizesAndFps,
                targetFpsRange,
                newUseCaseConfigs,
                useCasesPriorityOrder,
                resolvedDynamicRanges,
                hasVideoCapture
            )
        val attachedSurfaceStreamSpecMap = mutableMapOf<AttachedSurfaceInfo, StreamSpec>()

        populateStreamUseCaseIfSameSavedSizes(
            bestSizesAndFps,
            orderedSurfaceConfigListForStreamUseCase,
            attachedSurfaces,
            attachedSurfaceStreamSpecMap,
            suggestedStreamSpecMap,
            surfaceConfigIndexAttachedSurfaceInfoMap,
            surfaceConfigIndexUseCaseConfigMap
        )

        return Pair.create(suggestedStreamSpecMap, attachedSurfaceStreamSpecMap)
    }

    /**
     * Creates the feature settings from the related info.
     *
     * @param cameraMode the working camera mode.
     * @param resolvedDynamicRanges the resolved dynamic range list of the newly added UseCases
     * @param isPreviewStabilizationOn whether the preview stabilization is enabled.
     * @param isUltraHdrOn whether the Ultra HDR image capture is enabled.
     */
    private fun createFeatureSettings(
        @CameraMode.Mode cameraMode: Int,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        isPreviewStabilizationOn: Boolean,
        isUltraHdrOn: Boolean
    ): FeatureSettings {
        val requiredMaxBitDepth = getRequiredMaxBitDepth(resolvedDynamicRanges)
        require(
            !(cameraMode != CameraMode.DEFAULT &&
                requiredMaxBitDepth == DynamicRange.BIT_DEPTH_10_BIT)
        ) {
            "Camera device Id is $cameraId. 10 bit dynamic range is not " +
                "currently supported in ${CameraMode.toLabelString(cameraMode)} camera mode."
        }
        return FeatureSettings(
            cameraMode,
            requiredMaxBitDepth,
            isPreviewStabilizationOn,
            isUltraHdrOn
        )
    }

    /**
     * Checks whether at least a surfaces combination can be supported for the UseCases combination.
     *
     * This function collects the selected surfaces from the existing UseCases and the surfaces of
     * the smallest available supported sizes from all the new UseCases. Using this set of surfaces,
     * this function can quickly determine whether at least one surface combination can be supported
     * for the target UseCases combination.
     *
     * This function disregards the stream use case, frame rate, and ZSL factors since they are not
     * mandatory requirements if no surface combination can satisfy them. The current algorithm only
     * attempts to identify the optimal surface combination for the given conditions.
     *
     * @param featureSettings the feature settings which can affect the surface config
     *   transformation or the guaranteed supported configurations.
     * @param attachedSurfaces the existing surfaces.
     * @param newUseCaseConfigsSupportedSizeMap newly added UseCaseConfig to supported output sizes
     *   map.
     * @return `true` if at least a surface combination can be supported for the UseCases
     *   combination. Otherwise, returns `false`.
     */
    private fun isUseCasesCombinationSupported(
        featureSettings: FeatureSettings,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>
    ): Boolean {
        val surfaceConfigs = mutableListOf<SurfaceConfig>()

        // Collects the surfaces of the attached UseCases
        for (attachedSurface: AttachedSurfaceInfo in attachedSurfaces) {
            surfaceConfigs.add(attachedSurface.getSurfaceConfig())
        }

        // Collects the surfaces with the smallest available sizes of the newly attached UseCases
        // to do the quick check that whether at least a surface combination can be supported.
        val compareSizesByArea = CompareSizesByArea()
        for (useCaseConfig: UseCaseConfig<*> in newUseCaseConfigsSupportedSizeMap.keys) {
            val outputSizes = newUseCaseConfigsSupportedSizeMap[useCaseConfig]
            require(!outputSizes.isNullOrEmpty()) {
                "No available output size is found for $useCaseConfig."
            }
            val minSize = Collections.min(outputSizes, compareSizesByArea)
            val imageFormat = useCaseConfig.inputFormat
            surfaceConfigs.add(
                SurfaceConfig.transformSurfaceConfig(
                    featureSettings.cameraMode,
                    imageFormat,
                    minSize,
                    getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
                )
            )
        }
        return checkSupported(featureSettings, surfaceConfigs)
    }

    /**
     * Iterate through all possible size arrangement and returns a surfaceConfig list for stream use
     * case. This list is ordered and the indices of its items are stored into
     * surfaceConfigIndexAttachedSurfaceInfoMap and surfaceConfigIndexUseCaseConfigMap.
     */
    private fun getOrderedSurfaceConfigListForStreamUseCase(
        allPossibleSizeArrangements: List<MutableList<Size>>,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        featureSettings: FeatureSettings,
        isSurfaceCombinationSupported: Boolean,
        surfaceConfigIndexAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo>,
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>
    ): List<SurfaceConfig>? {
        var orderedSurfaceConfigListForStreamUseCase: List<SurfaceConfig>? = null
        // Check if any possible size arrangement is supported for stream use case.
        for (possibleSizeList in allPossibleSizeArrangements) {
            val surfaceConfigs =
                getSurfaceConfigList(
                    featureSettings.cameraMode,
                    attachedSurfaces,
                    possibleSizeList,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    surfaceConfigIndexAttachedSurfaceInfoMap,
                    surfaceConfigIndexUseCaseConfigMap
                )
            orderedSurfaceConfigListForStreamUseCase =
                getOrderedSupportedStreamUseCaseSurfaceConfigList(featureSettings, surfaceConfigs)
            if (
                orderedSurfaceConfigListForStreamUseCase != null &&
                    !StreamUseCaseUtil.areCaptureTypesEligible(
                        surfaceConfigIndexAttachedSurfaceInfoMap,
                        surfaceConfigIndexUseCaseConfigMap,
                        orderedSurfaceConfigListForStreamUseCase
                    )
            ) {
                orderedSurfaceConfigListForStreamUseCase = null
            }
            if (orderedSurfaceConfigListForStreamUseCase != null) {
                orderedSurfaceConfigListForStreamUseCase =
                    if (
                        StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                            cameraMetadata,
                            orderedSurfaceConfigListForStreamUseCase
                        )
                    ) {
                        break
                    } else {
                        null
                    }
            }
            surfaceConfigIndexAttachedSurfaceInfoMap.clear()
            surfaceConfigIndexUseCaseConfigMap.clear()
        }

        // We can terminate early if surface combination is not supported and none of the
        // possible size arrangement supports stream use case either.
        require(
            !(orderedSurfaceConfigListForStreamUseCase == null && !isSurfaceCombinationSupported)
        ) {
            "No supported surface combination is found for camera device - Id : $cameraId. " +
                "May be attempting to bind too many use cases. Existing surfaces: " +
                "$attachedSurfaces. New configs: $newUseCaseConfigs."
        }
        return orderedSurfaceConfigListForStreamUseCase
    }

    /**
     * If the saved max FPS and sizes are the same for non-streamUseCase and streamUseCase, populate
     * stream use case values into the output streamSpecs in attachedSurfaceStreamSpecMap and
     * suggestedStreamSpecMap.
     */
    private fun populateStreamUseCaseIfSameSavedSizes(
        bestSizesAndMaxFps: BestSizesAndMaxFpsForConfigs,
        orderedSurfaceConfigListForStreamUseCase: List<SurfaceConfig>?,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec>,
        suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec>,
        surfaceConfigIndexAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo>,
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>
    ) {
        // Only perform stream use case operations if the saved max FPS and sizes are the same
        if (
            orderedSurfaceConfigListForStreamUseCase != null &&
                bestSizesAndMaxFps.maxFps == bestSizesAndMaxFps.maxFpsForStreamUseCase &&
                bestSizesAndMaxFps.bestSizes.size ==
                    bestSizesAndMaxFps.bestSizesForStreamUseCase!!.size
        ) {
            var hasDifferentSavedSizes =
                bestSizesAndMaxFps.bestSizes.zip(bestSizesAndMaxFps.bestSizesForStreamUseCase).any {
                    it.first != it.second
                }
            if (!hasDifferentSavedSizes) {
                val hasStreamUseCaseOverride: Boolean =
                    StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                        cameraMetadata,
                        attachedSurfaces,
                        suggestedStreamSpecMap,
                        attachedSurfaceStreamSpecMap
                    )
                if (!hasStreamUseCaseOverride) {
                    StreamUseCaseUtil
                        .populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
                            suggestedStreamSpecMap,
                            attachedSurfaceStreamSpecMap,
                            surfaceConfigIndexAttachedSurfaceInfoMap,
                            surfaceConfigIndexUseCaseConfigMap,
                            orderedSurfaceConfigListForStreamUseCase
                        )
                }
            }
        }
    }

    private fun getSupportedOutputSizesList(
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
    ): List<List<Size>> {
        val supportedOutputSizesList: MutableList<List<Size>> = mutableListOf()

        // Collect supported output sizes for all use cases
        for (index in useCasesPriorityOrder) {
            var supportedOutputSizes = newUseCaseConfigsSupportedSizeMap[newUseCaseConfigs[index]]!!
            supportedOutputSizes =
                applyResolutionSelectionOrderRelatedWorkarounds(
                    supportedOutputSizes,
                    newUseCaseConfigs[index].inputFormat
                )
            supportedOutputSizesList.add(supportedOutputSizes)
        }
        return supportedOutputSizesList
    }

    private fun getTargetFpsRange(
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>
    ): Range<Int>? {
        var targetFrameRateForConfig: Range<Int>? = null
        for (attachedSurfaceInfo in attachedSurfaces) {
            // init target fps range for new configs from existing surfaces
            targetFrameRateForConfig =
                getUpdatedTargetFrameRate(
                    attachedSurfaceInfo.targetFrameRate,
                    targetFrameRateForConfig
                )
        }
        // update target fps for new configs using new use cases' priority order
        for (index in useCasesPriorityOrder) {
            targetFrameRateForConfig =
                getUpdatedTargetFrameRate(
                    newUseCaseConfigs[index].getTargetFrameRate(null),
                    targetFrameRateForConfig
                )
        }
        return targetFrameRateForConfig
    }

    private fun getMaxSupportedFpsFromAttachedSurfaces(
        attachedSurfaces: List<AttachedSurfaceInfo>,
    ): Int {
        var existingSurfaceFrameRateCeiling = Int.MAX_VALUE
        for (attachedSurfaceInfo in attachedSurfaces) {
            // get the fps ceiling for existing surfaces
            existingSurfaceFrameRateCeiling =
                getUpdatedMaximumFps(
                    existingSurfaceFrameRateCeiling,
                    attachedSurfaceInfo.imageFormat,
                    attachedSurfaceInfo.size
                )
        }
        return existingSurfaceFrameRateCeiling
    }

    /**
     * Filters the supported sizes for each use case to keep only one item for each unique config
     * size and frame rate combination.
     *
     * @return the new use case config to the supported sizes map, with the unnecessary sizes
     *   filtered out.
     */
    private fun filterSupportedSizes(
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        featureSettings: FeatureSettings,
        targetFpsRange: Range<Int>?
    ): Map<UseCaseConfig<*>, List<Size>> {
        val filteredUseCaseConfigToSupportedSizesMap = mutableMapOf<UseCaseConfig<*>, List<Size>>()
        for (useCaseConfig in newUseCaseConfigsSupportedSizeMap.keys) {
            val reducedSizeList = mutableListOf<Size>()
            val configSizeUniqueMaxFpsMap = mutableMapOf<ConfigSize, MutableSet<Int>>()
            for (size in newUseCaseConfigsSupportedSizeMap[useCaseConfig]!!) {
                val imageFormat = useCaseConfig.inputFormat
                val configSize =
                    SurfaceConfig.transformSurfaceConfig(
                            featureSettings.cameraMode,
                            imageFormat,
                            size,
                            getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
                        )
                        .configSize
                // Filters the sizes with frame rate only if there is target FPS setting
                val maxFrameRate =
                    if (targetFpsRange != null) {
                        getMaxFrameRate(imageFormat, size)
                    } else {
                        Int.MAX_VALUE
                    }

                var uniqueMaxFrameRates = configSizeUniqueMaxFpsMap[configSize]
                // Creates an empty FPS list for the config size when it doesn't exist.
                if (uniqueMaxFrameRates == null) {
                    uniqueMaxFrameRates = mutableSetOf()
                    configSizeUniqueMaxFpsMap[configSize] = uniqueMaxFrameRates
                }
                // Adds the size to the result list when there is still no entry for the config
                // size and frame rate combination.
                //
                // An example to explain the filter logic.
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
                    reducedSizeList.add(size)
                    uniqueMaxFrameRates.add(maxFrameRate)
                }
            }
            filteredUseCaseConfigToSupportedSizesMap[useCaseConfig] = reducedSizeList
        }
        return filteredUseCaseConfigToSupportedSizesMap
    }

    private fun findBestSizesAndFps(
        allPossibleSizeArrangements: List<MutableList<Size>>,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        existingSurfaceFrameRateCeiling: Int,
        useCasesPriorityOrder: List<Int>,
        targetFrameRateForConfig: Range<Int>?,
        featureSettings: FeatureSettings,
        orderedSurfaceConfigListForStreamUseCase: List<SurfaceConfig>?
    ): BestSizesAndMaxFpsForConfigs {
        var bestSizes: List<Size>? = null
        var maxFps = Int.MAX_VALUE
        var bestSizesForStreamUseCase: List<Size>? = null
        var maxFpsForStreamUseCase = Int.MAX_VALUE
        var supportedSizesFound = false
        var supportedSizesForStreamUseCaseFound = false

        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (possibleSizeList in allPossibleSizeArrangements) {
            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            val surfaceConfigList =
                getSurfaceConfigList(
                    featureSettings.cameraMode,
                    attachedSurfaces,
                    possibleSizeList,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    null,
                    null
                )
            val currentConfigFrameRateCeiling =
                getCurrentConfigFrameRateCeiling(
                    possibleSizeList,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    existingSurfaceFrameRateCeiling
                )
            var isConfigFrameRateAcceptable = true
            if (targetFrameRateForConfig != null) {
                if (
                    existingSurfaceFrameRateCeiling > currentConfigFrameRateCeiling &&
                        currentConfigFrameRateCeiling < targetFrameRateForConfig.lower
                ) {
                    // if the max fps before adding new use cases supports our target fps range
                    // BUT the max fps of the new configuration is below
                    // our target fps range, we'll want to check the next configuration until we
                    // get one that supports our target FPS
                    isConfigFrameRateAcceptable = false
                }
            }

            // Find the same possible size arrangement that is supported by stream use case again
            // if we found one earlier.

            // only change the saved config if you get another that has a better max fps
            if (!supportedSizesFound && checkSupported(featureSettings, surfaceConfigList)) {
                // if the config is supported by the device but doesn't meet the target frame rate,
                // save the config
                if (maxFps == Int.MAX_VALUE) {
                    maxFps = currentConfigFrameRateCeiling
                    bestSizes = possibleSizeList
                } else if (maxFps < currentConfigFrameRateCeiling) {
                    // only change the saved config if the max fps is better
                    maxFps = currentConfigFrameRateCeiling
                    bestSizes = possibleSizeList
                }

                // if we have a configuration where the max fps is acceptable for our target, break
                if (isConfigFrameRateAcceptable) {
                    maxFps = currentConfigFrameRateCeiling
                    bestSizes = possibleSizeList
                    supportedSizesFound = true
                    if (supportedSizesForStreamUseCaseFound) {
                        break
                    }
                }
            }
            // If we already know that there is a supported surface combination from the stream
            // use case table, keep an independent tracking on the saved sizes and max FPS. Only
            // use stream use case if the save sizes for the normal case and for stream use case
            // are the same.
            if (
                orderedSurfaceConfigListForStreamUseCase != null &&
                    !supportedSizesForStreamUseCaseFound &&
                    getOrderedSupportedStreamUseCaseSurfaceConfigList(
                        featureSettings,
                        surfaceConfigList
                    ) != null
            ) {
                if (maxFpsForStreamUseCase == Int.MAX_VALUE) {
                    maxFpsForStreamUseCase = currentConfigFrameRateCeiling
                    bestSizesForStreamUseCase = possibleSizeList
                } else if (maxFpsForStreamUseCase < currentConfigFrameRateCeiling) {
                    maxFpsForStreamUseCase = currentConfigFrameRateCeiling
                    bestSizesForStreamUseCase = possibleSizeList
                }
                if (isConfigFrameRateAcceptable) {
                    maxFpsForStreamUseCase = currentConfigFrameRateCeiling
                    bestSizesForStreamUseCase = possibleSizeList
                    supportedSizesForStreamUseCaseFound = true
                    if (supportedSizesFound) {
                        break
                    }
                }
            }
        }
        require(bestSizes != null) {
            "No supported surface combination is found for camera device - Id : $cameraId " +
                "and Hardware level: $hardwareLevel. " +
                "May be the specified resolution is too large and not supported. " +
                "Existing surfaces: $attachedSurfaces. New configs: $newUseCaseConfigs."
        }
        return BestSizesAndMaxFpsForConfigs(
            bestSizes,
            bestSizesForStreamUseCase,
            maxFps,
            maxFpsForStreamUseCase
        )
    }

    private fun generateSuggestedStreamSpecMap(
        bestSizesAndMaxFps: BestSizesAndMaxFpsForConfigs,
        targetFpsRange: Range<Int>?,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        hasVideoCapture: Boolean
    ): MutableMap<UseCaseConfig<*>, StreamSpec> {
        val suggestedStreamSpecMap = mutableMapOf<UseCaseConfig<*>, StreamSpec>()
        var targetFrameRateForDevice: Range<Int>? = null
        if (targetFpsRange != null) {
            targetFrameRateForDevice =
                getClosestSupportedDeviceFrameRate(targetFpsRange, bestSizesAndMaxFps.maxFps)
        }
        for ((index, useCaseConfig) in newUseCaseConfigs.withIndex()) {
            val resolutionForUseCase =
                bestSizesAndMaxFps.bestSizes[useCasesPriorityOrder.indexOf(index)]
            val streamSpecBuilder =
                StreamSpec.builder(resolutionForUseCase)
                    .setDynamicRange(checkNotNull(resolvedDynamicRanges[useCaseConfig]))
                    .setImplementationOptions(
                        StreamUseCaseUtil.getStreamSpecImplementationOptions(useCaseConfig)
                    )
                    .setZslDisabled(hasVideoCapture)

            if (targetFrameRateForDevice != null) {
                streamSpecBuilder.setExpectedFrameRateRange(targetFrameRateForDevice)
            }
            suggestedStreamSpecMap[useCaseConfig] = streamSpecBuilder.build()
        }
        return suggestedStreamSpecMap
    }

    private fun getRequiredMaxBitDepth(
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>
    ): Int {
        for (dynamicRange in resolvedDynamicRanges.values) {
            if (dynamicRange.bitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
                return DynamicRange.BIT_DEPTH_10_BIT
            }
        }
        return DynamicRange.BIT_DEPTH_8_BIT
    }

    private fun getSurfaceConfigList(
        @CameraMode.Mode cameraMode: Int,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        possibleSizeList: List<Size>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        surfaceConfigIndexAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo>?,
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>?
    ): MutableList<SurfaceConfig> {
        val surfaceConfigList: MutableList<SurfaceConfig> = mutableListOf()
        for (attachedSurfaceInfo in attachedSurfaces) {
            surfaceConfigList.add(attachedSurfaceInfo.surfaceConfig)
            if (surfaceConfigIndexAttachedSurfaceInfoMap != null) {
                surfaceConfigIndexAttachedSurfaceInfoMap[surfaceConfigList.size - 1] =
                    attachedSurfaceInfo
            }
        }

        // Attach SurfaceConfig of new use cases
        for ((i, size) in possibleSizeList.withIndex()) {
            val newUseCase = newUseCaseConfigs[useCasesPriorityOrder[i]]
            val imageFormat = newUseCase.inputFormat
            // add new use case/size config to list of surfaces
            val surfaceConfig =
                SurfaceConfig.transformSurfaceConfig(
                    cameraMode,
                    imageFormat,
                    size,
                    getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
                )
            surfaceConfigList.add(surfaceConfig)
            if (surfaceConfigIndexUseCaseConfigMap != null) {
                surfaceConfigIndexUseCaseConfigMap[surfaceConfigList.size - 1] = newUseCase
            }
        }
        return surfaceConfigList
    }

    private fun getCurrentConfigFrameRateCeiling(
        possibleSizeList: List<Size>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        currentConfigFrameRateCeiling: Int,
    ): Int {
        var newConfigFrameRateCeiling: Int = currentConfigFrameRateCeiling
        // Attach SurfaceConfig of new use cases
        for ((i, size) in possibleSizeList.withIndex()) {
            val newUseCase = newUseCaseConfigs[useCasesPriorityOrder[i]]
            // get the maximum fps of the new surface and update the maximum fps of the
            // proposed configuration
            newConfigFrameRateCeiling =
                getUpdatedMaximumFps(newConfigFrameRateCeiling, newUseCase.inputFormat, size)
        }
        return newConfigFrameRateCeiling
    }

    private fun getMaxFrameRate(imageFormat: Int, size: Size?): Int {
        var maxFrameRate = 0
        try {
            val minFrameDuration =
                getStreamConfigurationMapCompat().getOutputMinFrameDuration(imageFormat, size)
                    ?: return 0
            maxFrameRate = floor(1_000_000_000.0 / minFrameDuration + 0.05).toInt()
        } catch (e1: IllegalArgumentException) {
            // TODO: this try catch is in place for the rare that a surface config has a size
            //  incompatible for getOutputMinFrameDuration...  put into a Quirk
        }
        return maxFrameRate
    }

    /**
     * @param range
     * @return the length of the range
     */
    private fun getRangeLength(range: Range<Int>): Int {
        return range.upper - range.lower + 1
    }

    /** @return the distance between the nearest limits of two non-intersecting ranges */
    private fun getRangeDistance(firstRange: Range<Int>, secondRange: Range<Int>): Int {
        require(
            !firstRange.contains(secondRange.upper) && !firstRange.contains(secondRange.lower)
        ) {
            "Ranges must not intersect"
        }
        return if (firstRange.lower > secondRange.upper) {
            firstRange.lower - secondRange.upper
        } else {
            secondRange.lower - firstRange.upper
        }
    }

    /**
     * @param targetFps the target frame rate range used while comparing to device-supported ranges
     * @param storedRange the device-supported range that is currently saved and intersects with
     *   targetFps
     * @param newRange a new potential device-supported range that intersects with targetFps
     * @return the device-supported range that better matches the target fps
     */
    private fun compareIntersectingRanges(
        targetFps: Range<Int>,
        storedRange: Range<Int>,
        newRange: Range<Int>
    ): Range<Int> {
        // TODO(b/272075984): some ranges may may have a larger intersection but may also have an
        //  excessively large portion that is non-intersecting. Will want to do further
        //  investigation to find a more optimized way to decide when a potential range has too
        //  much non-intersecting value and discard it
        val storedIntersectionsize = getRangeLength(storedRange.intersect(targetFps)).toDouble()
        val newIntersectionSize = getRangeLength(newRange.intersect(targetFps)).toDouble()
        val newRangeRatio = newIntersectionSize / getRangeLength(newRange)
        val storedRangeRatio = storedIntersectionsize / getRangeLength(storedRange)
        if (newIntersectionSize > storedIntersectionsize) {
            // if new, the new range must have at least 50% of its range intersecting, OR has a
            // larger percentage of intersection than the previous stored range
            if (newRangeRatio >= .5 || newRangeRatio >= storedRangeRatio) {
                return newRange
            }
        } else if (newIntersectionSize == storedIntersectionsize) {
            // if intersecting ranges have same length... pick the one that has the higher
            // intersection ratio
            if (newRangeRatio > storedRangeRatio) {
                return newRange
            } else if (newRangeRatio == storedRangeRatio && newRange.lower > storedRange.lower) {
                // if equal intersection size AND ratios pick the higher range
                return newRange
            }
        } else if (storedRangeRatio < .5 && newRangeRatio > storedRangeRatio) {
            // if the new one has a smaller range... only change if existing has an intersection
            // ratio < 50% and the new one has an intersection ratio > than the existing one
            return newRange
        }
        return storedRange
    }

    /**
     * Finds a frame rate range supported by the device that is closest to the target frame rate
     *
     * @param targetFrameRate the Target Frame Rate resolved from all current existing surfaces and
     *   incoming new use cases
     * @return a frame rate range supported by the device that is closest to targetFrameRate
     */
    private fun getClosestSupportedDeviceFrameRate(
        targetFrameRate: Range<Int>,
        maxFps: Int
    ): Range<Int> {
        var newTargetFrameRate = targetFrameRate
        // get all fps ranges supported by device
        val availableFpsRanges =
            cameraMetadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
                ?: return StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
        // if  whole target frame rate range > maxFps of configuration, the target for this
        // calculation will be [max,max].

        // if the range is partially larger than  maxFps, the target for this calculation will be
        // [target.lower, max] for the sake of this calculation
        newTargetFrameRate =
            Range(min(newTargetFrameRate.lower, maxFps), min(newTargetFrameRate.upper, maxFps))
        var bestRange = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
        var currentIntersectSize = 0
        for (potentialRange in availableFpsRanges) {
            // ignore ranges completely larger than configuration's maximum fps
            if (maxFps < potentialRange.lower) {
                continue
            }
            if (bestRange == StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED) {
                bestRange = potentialRange
            }
            // take if range is a perfect match
            if (potentialRange == newTargetFrameRate) {
                bestRange = potentialRange
                break
            }
            try {
                // bias towards a range that intersects on the upper end
                val newIntersection = potentialRange.intersect(newTargetFrameRate)
                val newIntersectSize: Int = getRangeLength(newIntersection)
                // if this range intersects our target + no other range was already
                if (currentIntersectSize == 0) {
                    bestRange = potentialRange
                    currentIntersectSize = newIntersectSize
                } else if (newIntersectSize >= currentIntersectSize) {
                    // if the currently stored range + new range both intersect, check to see
                    // which one should be picked over the other
                    bestRange =
                        compareIntersectingRanges(newTargetFrameRate, bestRange, potentialRange)
                    currentIntersectSize = getRangeLength(newTargetFrameRate.intersect(bestRange))
                }
            } catch (e: IllegalArgumentException) {
                if (currentIntersectSize != 0) {
                    continue
                }

                // if no intersection is present, pick the range that is closer to our target
                if (
                    getRangeDistance(potentialRange, newTargetFrameRate) <
                        getRangeDistance(bestRange, newTargetFrameRate)
                ) {
                    bestRange = potentialRange
                } else if (
                    getRangeDistance(potentialRange, newTargetFrameRate) ==
                        getRangeDistance(bestRange, newTargetFrameRate)
                ) {
                    if (potentialRange.lower > bestRange.upper) {
                        // if they both have the same distance, pick the higher range
                        bestRange = potentialRange
                    } else if (getRangeLength(potentialRange) < getRangeLength(bestRange)) {
                        // if one isn't higher than the other, pick the range with the
                        // shorter length
                        bestRange = potentialRange
                    }
                }
            }
        }
        return bestRange
    }

    /**
     * @param newTargetFrameRate an incoming frame rate range
     * @param storedTargetFrameRate a stored frame rate range to be modified
     * @return adjusted target frame rate
     *
     * If the two ranges are both nonnull and disjoint of each other, then the range that was
     * already stored will be used
     */
    private fun getUpdatedTargetFrameRate(
        newTargetFrameRate: Range<Int>?,
        storedTargetFrameRate: Range<Int>?
    ): Range<Int>? {
        var updatedTarget = storedTargetFrameRate
        if (storedTargetFrameRate == null) {
            // if stored value was null before, set it to the new value
            updatedTarget = newTargetFrameRate
        } else if (newTargetFrameRate != null) {
            updatedTarget =
                try {
                    // get intersection of existing target fps
                    storedTargetFrameRate.intersect(newTargetFrameRate)
                } catch (e: java.lang.IllegalArgumentException) {
                    // no intersection, keep the previously stored value
                    storedTargetFrameRate
                }
        }
        return updatedTarget
    }

    /**
     * @param currentMaxFps the previously stored Max FPS
     * @param imageFormat the image format of the incoming surface
     * @param size the size of the incoming surface
     */
    private fun getUpdatedMaximumFps(currentMaxFps: Int, imageFormat: Int, size: Size): Int {
        return min(currentMaxFps, getMaxFrameRate(imageFormat, size))
    }

    /**
     * Applies resolution selection order related workarounds.
     *
     * TargetAspectRatio workaround makes CameraX select sizes of specific aspect ratio in priority
     * to avoid the preview image stretch issue.
     *
     * ResolutionCorrector workaround makes CameraX select specific sizes for different capture
     * types to avoid the preview image stretch issue.
     *
     * @see TargetAspectRatio
     * @see ResolutionCorrector
     */
    @VisibleForTesting
    public fun applyResolutionSelectionOrderRelatedWorkarounds(
        sizeList: List<Size>,
        imageFormat: Int
    ): List<Size> {
        // Applies TargetAspectRatio workaround
        val ratio: Rational? =
            when (targetAspectRatio[cameraMetadata, streamConfigurationMapCompat]) {
                TargetAspectRatio.RATIO_4_3 -> AspectRatioUtil.ASPECT_RATIO_4_3
                TargetAspectRatio.RATIO_16_9 -> AspectRatioUtil.ASPECT_RATIO_16_9
                TargetAspectRatio.RATIO_MAX_JPEG -> {
                    val maxJpegSize =
                        getUpdatedSurfaceSizeDefinitionByFormat(ImageFormat.JPEG)
                            .getMaximumSize(ImageFormat.JPEG)
                    Rational(maxJpegSize.width, maxJpegSize.height)
                }
                else -> null
            }
        val resultList: MutableList<Size>
        if (ratio == null) {
            resultList = sizeList.toMutableList()
        } else {
            val aspectRatioMatchedSizeList: MutableList<Size> = mutableListOf()
            resultList = mutableListOf()
            for (size in sizeList) {
                if (AspectRatioUtil.hasMatchingAspectRatio(size, ratio)) {
                    aspectRatioMatchedSizeList.add(size)
                } else {
                    resultList.add(size)
                }
            }
            resultList.addAll(0, aspectRatioMatchedSizeList)
        }

        // Applies ResolutionCorrector workaround and return the result list.
        return resolutionCorrector.insertOrPrioritize(
            SurfaceConfig.getConfigType(imageFormat),
            resultList
        )
    }

    // Utility classes and methods:
    // *********************************************************************************************

    /** Refresh Preview Size based on current display configurations. */
    private fun refreshPreviewSize() {
        displayInfoManager.refresh()
        if (!::surfaceSizeDefinition.isInitialized) {
            generateSurfaceSizeDefinition()
        } else {
            val previewSize: Size = displayInfoManager.getPreviewSize()
            surfaceSizeDefinition =
                SurfaceSizeDefinition.create(
                    surfaceSizeDefinition.analysisSize,
                    surfaceSizeDefinition.s720pSizeMap,
                    previewSize,
                    surfaceSizeDefinition.s1440pSizeMap,
                    surfaceSizeDefinition.recordSize,
                    surfaceSizeDefinition.maximumSizeMap,
                    surfaceSizeDefinition.ultraMaximumSizeMap
                )
        }
    }

    /** Check the device's available capabilities. */
    private fun checkCapabilities() {
        val availableCapabilities: IntArray? =
            cameraMetadata.get<IntArray>(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        availableCapabilities?.apply {
            isRawSupported = contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
            isBurstCaptureSupported =
                contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
            isUltraHighResolutionSensorSupported =
                contains(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                )
        }

        // Preview Stabilization
        val availablePreviewStabilizationModes: IntArray? =
            cameraMetadata.get<IntArray>(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
            )

        availablePreviewStabilizationModes?.apply {
            isPreviewStabilizationSupported =
                contains(
                    CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                )
        }
    }

    /** Generate the supported combination list from guaranteed configurations tables. */
    private fun generateSupportedCombinationList() {
        surfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.generateSupportedCombinationList(
                hardwareLevel,
                isRawSupported,
                isBurstCaptureSupported
            )
        )
        surfaceCombinations.addAll(extraSupportedSurfaceCombinationsContainer[cameraId])
    }

    private fun generateUltraHighResolutionSupportedCombinationList() {
        ultraHighSurfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList()
        )
    }

    private fun generateConcurrentSupportedCombinationList() {
        concurrentSurfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.getConcurrentSupportedCombinationList()
        )
    }

    private fun generatePreviewStabilizationSupportedCombinationList() {
        previewStabilizationSurfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.getPreviewStabilizationSupportedCombinationList()
        )
    }

    private fun generate10BitSupportedCombinationList() {
        surfaceCombinations10Bit.addAll(
            GuaranteedConfigurationsUtil.get10BitSupportedCombinationList()
        )
    }

    private fun generateUltraHdrSupportedCombinationList() {
        surfaceCombinationsUltraHdr.addAll(
            GuaranteedConfigurationsUtil.getUltraHdrSupportedCombinationList()
        )
    }

    private fun generateStreamUseCaseSupportedCombinationList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            surfaceCombinationsStreamUseCase.addAll(
                GuaranteedConfigurationsUtil.getStreamUseCaseSupportedCombinationList()
            )
        }
    }

    /**
     * Generation the size definition for VGA, s720p, PREVIEW, s1440p, RECORD, MAXIMUM and
     * ULTRA_MAXIMUM.
     */
    private fun generateSurfaceSizeDefinition() {
        val previewSize: Size = displayInfoManager.getPreviewSize()
        val recordSize: Size = getRecordSize()
        surfaceSizeDefinition =
            SurfaceSizeDefinition.create(
                RESOLUTION_VGA,
                mutableMapOf(), // s720pSizeMap
                previewSize,
                mutableMapOf(), // s1440pSizeMap
                recordSize,
                mutableMapOf(), // maximumSizeMap
                mutableMapOf() // ultraMaximumSizeMap
            )
    }

    /** Updates the surface size definition for the specified format then return it. */
    @VisibleForTesting
    public fun getUpdatedSurfaceSizeDefinitionByFormat(format: Int): SurfaceSizeDefinition {
        if (!surfaceSizeDefinitionFormats.contains(format)) {
            updateS720pOrS1440pSizeByFormat(
                surfaceSizeDefinition.s720pSizeMap,
                RESOLUTION_720P,
                format
            )
            updateS720pOrS1440pSizeByFormat(
                surfaceSizeDefinition.s1440pSizeMap,
                RESOLUTION_1440P,
                format
            )
            updateMaximumSizeByFormat(surfaceSizeDefinition.maximumSizeMap, format)
            updateUltraMaximumSizeByFormat(surfaceSizeDefinition.ultraMaximumSizeMap, format)
            surfaceSizeDefinitionFormats.add(format)
        }
        return surfaceSizeDefinition
    }

    /**
     * Updates the s720p or s720p size to the map for the specified format.
     *
     * <p>s720p refers to the 720p (1280 x 720) or the maximum supported resolution for the
     * particular format returned by {@link StreamConfigurationMap#getOutputSizes(int)}, whichever
     * is smaller.
     *
     * <p>s1440p refers to the 1440p (1920 x 1440) or the maximum supported resolution for the
     * particular format returned by {@link StreamConfigurationMap#getOutputSizes(int)}, whichever
     * is smaller.
     *
     * @param targetSize the target size to create the map.
     * @return the format to s720p or s720p size map.
     */
    private fun updateS720pOrS1440pSizeByFormat(
        sizeMap: MutableMap<Int, Size>,
        targetSize: Size,
        format: Int
    ) {
        if (!isConcurrentCameraModeSupported) {
            return
        }

        val originalMap = streamConfigurationMapCompat.toStreamConfigurationMap()
        val maxOutputSize = getMaxOutputSizeByFormat(originalMap, format, false)
        sizeMap[format] =
            if (maxOutputSize == null) {
                targetSize
            } else {
                Collections.min(listOf(targetSize, maxOutputSize), CompareSizesByArea())
            }
    }

    /** Updates the maximum size to the map for the specified format. */
    private fun updateMaximumSizeByFormat(sizeMap: MutableMap<Int, Size>, format: Int) {
        val originalMap = streamConfigurationMapCompat.toStreamConfigurationMap()
        getMaxOutputSizeByFormat(originalMap, format, true)?.let { sizeMap[format] = it }
    }

    /** Updates the ultra maximum size to the map for the specified format. */
    private fun updateUltraMaximumSizeByFormat(sizeMap: MutableMap<Int, Size>, format: Int) {
        // Maximum resolution mode is supported since API level 31
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !isUltraHighResolutionSensorSupported
        ) {
            return
        }
        val maximumResolutionMap =
            cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION]
                ?: return
        getMaxOutputSizeByFormat(maximumResolutionMap, format, true)?.let { sizeMap[format] = it }
    }

    /**
     * RECORD refers to the camera device's maximum supported recording resolution, as determined by
     * CamcorderProfile.
     */
    private fun getRecordSize(): Size {
        try {
            this.cameraId.toInt()
        } catch (e: NumberFormatException) {
            // The camera Id is not an integer because the camera may be a removable device. Use
            // StreamConfigurationMap to determine the RECORD size.
            return getRecordSizeFromStreamConfigurationMapCompat()
        }
        var profiles: EncoderProfilesProxy? = null
        if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_HIGH)
        }
        return if (profiles != null && profiles.videoProfiles.isNotEmpty()) {
            Size(profiles.videoProfiles[0].width, profiles.videoProfiles[0].height)
        } else getRecordSizeByHasProfile()
    }

    /** Obtains the stream configuration map from camera meta data. */
    private fun getStreamConfigurationMapCompat(): StreamConfigurationMapCompat {
        val map =
            cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: throw IllegalArgumentException("Cannot retrieve SCALER_STREAM_CONFIGURATION_MAP")
        return StreamConfigurationMapCompat(map, OutputSizesCorrector(cameraMetadata, map))
    }

    /**
     * Return the maximum supported video size for cameras using data from the stream configuration
     * map.
     *
     * @return Maximum supported video size.
     */
    private fun getRecordSizeFromStreamConfigurationMapCompat(): Size {
        val map = streamConfigurationMapCompat.toStreamConfigurationMap()
        val videoSizeArr = map?.getOutputSizes(MediaRecorder::class.java) ?: return RESOLUTION_480P
        Arrays.sort(videoSizeArr, CompareSizesByArea(true))
        for (size in videoSizeArr) {
            // Returns the largest supported size under 1080P
            if (size.width <= RESOLUTION_1080P.width && size.height <= RESOLUTION_1080P.height) {
                return size
            }
        }
        return RESOLUTION_480P
    }

    /**
     * Return the maximum supported video size for cameras by [CamcorderProfile.hasProfile].
     *
     * @return Maximum supported video size.
     */
    private fun getRecordSizeByHasProfile(): Size {
        var recordSize: Size = RESOLUTION_480P
        var profiles: EncoderProfilesProxy? = null

        // Check whether 4KDCI, 2160P, 2K, 1080P, 720P, 480P (sorted by size) are supported by
        // EncoderProfiles
        if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_4KDCI)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_4KDCI)
        } else if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_2160P)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_2160P)
        } else if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_2K)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_2K)
        } else if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_1080P)
        } else if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_720P)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_720P)
        } else if (encoderProfilesProviderAdapter.hasProfile(CamcorderProfile.QUALITY_480P)) {
            profiles = encoderProfilesProviderAdapter.getAll(CamcorderProfile.QUALITY_480P)
        }
        if (profiles != null && profiles.videoProfiles.isNotEmpty()) {
            recordSize = Size(profiles.videoProfiles[0].width, profiles.videoProfiles[0].height)
        }
        return recordSize
    }

    /**
     * Once the stream resource is occupied by one use case, it will impact the other use cases.
     * Therefore, we need to define the priority for stream resource usage. For the use cases with
     * the higher priority, we will try to find the best one for them in priority as possible.
     */
    private fun getUseCasesPriorityOrder(newUseCaseConfigs: List<UseCaseConfig<*>>): List<Int> {
        val priorityOrder: MutableList<Int> = mutableListOf()
        val priorityValueList: MutableList<Int> = mutableListOf()
        for (config in newUseCaseConfigs) {
            val priority = config.getSurfaceOccupancyPriority(0)
            if (!priorityValueList.contains(priority)) {
                priorityValueList.add(priority)
            }
        }
        priorityValueList.sort()
        // Reverse the priority value list in descending order since larger value means higher
        // priority
        priorityValueList.reverse()
        for (priorityValue in priorityValueList) {
            for (config in newUseCaseConfigs) {
                if (priorityValue == config.getSurfaceOccupancyPriority(0)) {
                    priorityOrder.add(newUseCaseConfigs.indexOf(config))
                }
            }
        }
        return priorityOrder
    }

    /**
     * Get max supported output size for specific image format
     *
     * @param map the original stream configuration map without quirks applied.
     * @param imageFormat the image format info
     * @param highResolutionIncluded whether high resolution output sizes are included
     * @return the max supported output size for the image format
     */
    @SuppressLint("ClassVerificationFailure")
    internal fun getMaxOutputSizeByFormat(
        map: StreamConfigurationMap?,
        imageFormat: Int,
        highResolutionIncluded: Boolean
    ): Size? {
        val outputSizes: Array<Size>? =
            if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                // This is a little tricky that 0x22 that is internal defined in
                // StreamConfigurationMap.java to be equal to ImageFormat.PRIVATE that is public
                // after Android level 23 but not public in Android L. Use {@link SurfaceTexture}
                // or {@link MediaCodec} will finally mapped to 0x22 in StreamConfigurationMap to
                // retrieve the output sizes information.
                map?.getOutputSizes(SurfaceTexture::class.java)
            } else {
                map?.getOutputSizes(imageFormat)
            }
        if (outputSizes.isNullOrEmpty()) {
            return null
        }
        val compareSizesByArea = CompareSizesByArea()
        val maxSize = Collections.max(outputSizes.asList(), compareSizesByArea)
        var maxHighResolutionSize = SizeUtil.RESOLUTION_ZERO

        if (Build.VERSION.SDK_INT >= 23 && highResolutionIncluded) {
            val highResolutionOutputSizes = map?.getHighResolutionOutputSizes(imageFormat)
            if (!highResolutionOutputSizes.isNullOrEmpty()) {
                maxHighResolutionSize =
                    Collections.max(highResolutionOutputSizes.asList(), compareSizesByArea)
            }
        }

        return Collections.max(listOf(maxSize, maxHighResolutionSize), compareSizesByArea)
    }

    /** Given all supported output sizes, lists out all possible size arrangements. */
    private fun getAllPossibleSizeArrangements(
        supportedOutputSizesList: List<List<Size>>
    ): List<MutableList<Size>> {
        var totalArrangementsCount = 1
        for (supportedOutputSizes in supportedOutputSizesList) {
            totalArrangementsCount *= supportedOutputSizes.size
        }

        // If totalArrangementsCount is 0 means that there may some problem to get
        // supportedOutputSizes
        // for some use case
        require(totalArrangementsCount != 0) { "Failed to find supported resolutions." }
        val allPossibleSizeArrangements: MutableList<MutableList<Size>> = mutableListOf()

        // Initialize allPossibleSizeArrangements for the following operations
        for (i in 0 until totalArrangementsCount) {
            val sizeList: MutableList<Size> = mutableListOf()
            allPossibleSizeArrangements.add(sizeList)
        }

        /*
         * Try to list out all possible arrangements by attaching all possible size of each column
         * in sequence. We have generated supportedOutputSizesList by the priority order for
         * different use cases. And the supported outputs sizes for each use case are also arranged
         * from large to small. Therefore, the earlier size arrangement in the result list will be
         * the better one to choose if finally it won't exceed the camera device's stream
         * combination capability.
         */
        var currentRunCount = totalArrangementsCount
        var nextRunCount = currentRunCount / supportedOutputSizesList[0].size
        for (currentIndex in supportedOutputSizesList.indices) {
            val supportedOutputSizes = supportedOutputSizesList[currentIndex]
            for (i in 0 until totalArrangementsCount) {
                val surfaceConfigList = allPossibleSizeArrangements[i]
                surfaceConfigList.add(supportedOutputSizes[i % currentRunCount / nextRunCount])
            }
            if (currentIndex < supportedOutputSizesList.size - 1) {
                currentRunCount = nextRunCount
                nextRunCount = currentRunCount / supportedOutputSizesList[currentIndex + 1].size
            }
        }
        return allPossibleSizeArrangements
    }

    /**
     * A collection of feature settings related to the Camera2 capabilities exposed by
     * [CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] and device features exposed by
     * [PackageManager.hasSystemFeature].
     *
     * @param cameraMode The camera mode. This involves the following mapping of mode to features:
     *   [CameraMode.CONCURRENT_CAMERA] -> [PackageManager.FEATURE_CAMERA_CONCURRENT]
     *   [CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA] ->
     *   [CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR]
     * @param requiredMaxBitDepth The required maximum bit depth for any non-RAW stream attached to
     *   the camera. A value of [DynamicRange.BIT_DEPTH_10_BIT] corresponds to the camera capability
     *   [CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT].
     * @param isPreviewStabilizationOn Whether the preview stabilization is enabled.
     */
    public data class FeatureSettings(
        @CameraMode.Mode val cameraMode: Int,
        val requiredMaxBitDepth: Int,
        val isPreviewStabilizationOn: Boolean = false,
        val isUltraHdrOn: Boolean = false
    )

    public data class BestSizesAndMaxFpsForConfigs(
        val bestSizes: List<Size>,
        val bestSizesForStreamUseCase: List<Size>?,
        val maxFps: Int,
        val maxFpsForStreamUseCase: Int
    )

    public companion object {
        private fun isUltraHdrOn(
            attachedSurfaces: List<AttachedSurfaceInfo>,
            newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>
        ): Boolean {
            for (surfaceInfo in attachedSurfaces) {
                if (surfaceInfo.imageFormat == ImageFormat.JPEG_R) {
                    return true
                }
            }

            for (useCaseConfig in newUseCaseConfigsSupportedSizeMap.keys) {
                if (useCaseConfig.inputFormat == ImageFormat.JPEG_R) {
                    return true
                }
            }

            return false
        }
    }
}
