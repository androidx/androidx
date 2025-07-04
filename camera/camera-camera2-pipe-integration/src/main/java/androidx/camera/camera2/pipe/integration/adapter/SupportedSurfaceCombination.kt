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
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsPreviewStabilization
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination.CheckingMethod.WITHOUT_FEATURE_COMBO
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination.CheckingMethod.WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination.CheckingMethod.WITH_FEATURE_COMBO
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.ExtraSupportedSurfaceCombinationsContainer
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.ResolutionCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.TargetAspectRatio
import androidx.camera.camera2.pipe.integration.impl.DisplayInfoManager
import androidx.camera.camera2.pipe.integration.internal.DynamicRangeResolver
import androidx.camera.camera2.pipe.integration.internal.HighSpeedResolver
import androidx.camera.camera2.pipe.integration.internal.StreamUseCaseUtil
import androidx.camera.core.DynamicRange
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery.Companion.createSessionConfigBuilder
import androidx.camera.core.featuregroup.impl.feature.FpsRangeFeature
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.StreamUseCase
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigSource.CAPTURE_SESSION_TABLES
import androidx.camera.core.impl.SurfaceConfig.ConfigSource.FEATURE_COMBINATION_TABLE
import androidx.camera.core.impl.SurfaceSizeDefinition
import androidx.camera.core.impl.SurfaceStreamSpecQueryResult
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1440P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.core.util.Preconditions
import java.util.Arrays
import java.util.Collections
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
    private val encoderProfilesProvider: EncoderProfilesProvider,
    private val featureCombinationQuery: FeatureCombinationQuery,
) {
    private val cameraId = cameraMetadata.camera.value
    private val hardwareLevel =
        cameraMetadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    private val concurrentSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinationsStreamUseCase: MutableList<SurfaceCombination> = mutableListOf()
    private val ultraHighSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val previewStabilizationSurfaceCombinations: MutableList<SurfaceCombination> =
        mutableListOf()
    private val highSpeedSurfaceCombinations = mutableListOf<SurfaceCombination>()
    private val featureSettingsToSupportedCombinationsMap:
        MutableMap<FeatureSettings, List<SurfaceCombination>> =
        mutableMapOf()
    private val surfaceCombinations10Bit: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinationsUltraHdr: MutableList<SurfaceCombination> = mutableListOf()
    private var isRawSupported = false
    private var isBurstCaptureSupported = false
    private val isConcurrentCameraModeSupported: Boolean
    private val isStreamUseCaseSupported: Boolean
    private var isUltraHighResolutionSensorSupported = false
    private var isPreviewStabilizationSupported = cameraMetadata.supportsPreviewStabilization
    private var isManualSensorSupported = false
    internal lateinit var surfaceSizeDefinition: SurfaceSizeDefinition
    private val surfaceSizeDefinitionFormats = mutableListOf<Int>()
    private val streamConfigurationMapCompat = getStreamConfigurationMapCompat()
    private val extraSupportedSurfaceCombinationsContainer =
        ExtraSupportedSurfaceCombinationsContainer()
    private val displayInfoManager = DisplayInfoManager(context)
    private val resolutionCorrector = ResolutionCorrector()
    private val targetAspectRatio: TargetAspectRatio = TargetAspectRatio()
    private val dynamicRangeResolver: DynamicRangeResolver = DynamicRangeResolver(cameraMetadata)
    private val highSpeedResolver: HighSpeedResolver = HighSpeedResolver(cameraMetadata)

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
     * @param surfaceConfigList the surface configuration list to be compared.
     * @return the check result that whether it could be supported
     */
    public fun checkSupported(
        featureSettings: FeatureSettings,
        surfaceConfigList: List<SurfaceConfig>,
        dynamicRangesBySurfaceConfig: Map<SurfaceConfig, DynamicRange> = emptyMap(),
        newUseCaseConfigs: List<UseCaseConfig<*>> = emptyList(),
        useCasesPriorityOrder: List<Int> = emptyList(),
    ): Boolean {
        val isSupported =
            getSurfaceCombinationsByFeatureSettings(featureSettings).any {
                it.getOrderedSupportedSurfaceConfigList(surfaceConfigList) != null
            }

        if (isSupported && featureSettings.requiresFeatureComboQuery) {
            val sessionConfig =
                createFeatureComboSessionConfig(
                    featureSettings,
                    surfaceConfigList,
                    dynamicRangesBySurfaceConfig,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                )

            return featureCombinationQuery.isSupported(sessionConfig).also {
                // Clean up all the surfaces created for this query.
                sessionConfig.surfaces.forEach { it.close() }
            }
        }

        return isSupported
    }

    private fun createFeatureComboSessionConfig(
        featureSettings: FeatureSettings,
        surfaceConfigList: List<SurfaceConfig>,
        dynamicRangesBySurfaceConfig: Map<SurfaceConfig, DynamicRange>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasePriorityOrder: List<Int>,
    ): SessionConfig {
        val validatingBuilder = SessionConfig.ValidatingBuilder()

        surfaceConfigList.forEachIndexed { i, surfaceConfig ->
            val resolution =
                surfaceConfig.getResolution(
                    getUpdatedSurfaceSizeDefinitionByFormat(surfaceConfig.imageFormat)
                )

            // Since the high-level API for feature combination always unbinds implicitly, there
            // will only be new use cases
            val useCaseConfig = newUseCaseConfigs[useCasePriorityOrder[i]]

            val sessionConfigBuilder =
                useCaseConfig
                    .createSessionConfigBuilder(
                        resolution,
                        requireNotNull(dynamicRangesBySurfaceConfig[surfaceConfig]),
                    )
                    .apply {
                        setExpectedFrameRateRange(
                            featureSettings.targetFpsRange.takeIf {
                                it != FRAME_RATE_RANGE_UNSPECIFIED
                            } ?: FpsRangeFeature.DEFAULT_FPS_RANGE
                        )

                        if (featureSettings.isPreviewStabilizationOn) {
                            setPreviewStabilization(StabilizationMode.ON)
                        }
                    }

            validatingBuilder.add(sessionConfigBuilder.build())

            Preconditions.checkState(
                validatingBuilder.isValid,
                "Cannot create a combined SessionConfig for feature combo after adding " +
                    "$useCaseConfig with $surfaceConfig" +
                    " due to [${validatingBuilder.invalidReason}]" +
                    "; surfaceConfigList = $surfaceConfigList, featureSettings = $featureSettings" +
                    ", newUseCaseConfigs = $newUseCaseConfigs",
            )
        }

        return validatingBuilder.build()
    }

    private fun getOrderedSupportedStreamUseCaseSurfaceConfigList(
        featureSettings: FeatureSettings,
        surfaceConfigList: List<SurfaceConfig?>?,
        surfaceConfigIndexAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo>,
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>,
    ): List<SurfaceConfig>? {
        if (!StreamUseCaseUtil.shouldUseStreamUseCase(featureSettings)) {
            return null
        }
        for (surfaceCombination in surfaceCombinationsStreamUseCase) {
            val orderedSurfaceConfigList =
                surfaceCombination.getOrderedSupportedSurfaceConfigList(surfaceConfigList!!)
            if (orderedSurfaceConfigList != null) {
                val captureTypesEligible =
                    StreamUseCaseUtil.areCaptureTypesEligible(
                        surfaceConfigIndexAttachedSurfaceInfoMap,
                        surfaceConfigIndexUseCaseConfigMap,
                        orderedSurfaceConfigList,
                    )
                val streamUseCasesAvailableForSurfaceConfigs = lazy {
                    StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                        cameraMetadata,
                        orderedSurfaceConfigList,
                    )
                }
                if (captureTypesEligible && streamUseCasesAvailableForSurfaceConfigs.value) {
                    return orderedSurfaceConfigList
                }
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
        if (featureSettings.requiresFeatureComboQuery) {
            supportedSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.QUERYABLE_FCQ_COMBINATIONS
            )
        } else if (featureSettings.isUltraHdrOn) {
            if (surfaceCombinationsUltraHdr.isEmpty()) {
                generateUltraHdrSupportedCombinationList()
            }
            // For Ultra HDR output, only the default camera mode is currently supported.
            if (featureSettings.cameraMode == CameraMode.DEFAULT) {
                supportedSurfaceCombinations.addAll(surfaceCombinationsUltraHdr)
            }
        } else if (featureSettings.isHighSpeedOn) {
            if (highSpeedSurfaceCombinations.isEmpty()) {
                generateHighSpeedSupportedCombinationList()
            }
            supportedSurfaceCombinations.addAll(highSpeedSurfaceCombinations)
        } else if (featureSettings.requiredMaxBitDepth == DynamicRange.BIT_DEPTH_8_BIT) {
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
                supportedSurfaceCombinations.addAll(surfaceCombinations10Bit)
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
        size: Size,
        streamUseCase: StreamUseCase,
    ): SurfaceConfig {
        return SurfaceConfig.transformSurfaceConfig(
            imageFormat = imageFormat,
            size = size,
            surfaceSizeDefinition = getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
            cameraMode = cameraMode,
            // FEATURE_COMBINATION_TABLE N/A for the code flows leading to this call
            configSource = CAPTURE_SESSION_TABLES,
            streamUseCase = streamUseCase,
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
     * @param isFeatureComboInvocation whether the code flow involves CameraX feature combo API
     *   (e.g. [androidx.camera.core.SessionConfig.requiredFeatureGroup]).
     * @param findMaxSupportedFrameRate whether to find the max supported frame rate. If this is
     *   true, the target frame rate settings will be ignored. If false, the returned value of
     *   [SurfaceStreamSpecQueryResult.maxSupportedFrameRate] is undetermined.
     * @return a [SurfaceStreamSpecQueryResult].
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *   found. This may be due to no available output size or no available surface combination.
     */
    public fun getSuggestedStreamSpecifications(
        cameraMode: Int,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        isPreviewStabilizationOn: Boolean = false,
        hasVideoCapture: Boolean = false,
        isFeatureComboInvocation: Boolean,
        findMaxSupportedFrameRate: Boolean,
    ): SurfaceStreamSpecQueryResult {
        // Refresh Preview Size based on current display configurations.
        refreshPreviewSize()

        val isHighSpeedOn =
            HighSpeedResolver.isHighSpeedOn(
                attachedSurfaces,
                newUseCaseConfigsSupportedSizeMap.keys,
            )
        // Filter out unsupported sizes for high-speed at the beginning to ensure correct
        // resolution selection later. High-speed session requires all surface sizes to be the same.
        val filteredNewUseCaseConfigsSupportedSizeMap =
            if (isHighSpeedOn) {
                highSpeedResolver.filterCommonSupportedSizes(newUseCaseConfigsSupportedSizeMap)
            } else {
                newUseCaseConfigsSupportedSizeMap
            }

        val newUseCaseConfigs = filteredNewUseCaseConfigsSupportedSizeMap.keys.toList()

        // Get the index order list by the use case priority for finding stream configuration
        val useCasesPriorityOrder = getUseCasesPriorityOrder(newUseCaseConfigs)
        val resolvedDynamicRanges =
            dynamicRangeResolver.resolveAndValidateDynamicRanges(
                attachedSurfaces,
                newUseCaseConfigs,
                useCasesPriorityOrder,
            )

        debug { "resolvedDynamicRanges = $resolvedDynamicRanges" }

        val isUltraHdrOn = isUltraHdrOn(attachedSurfaces, filteredNewUseCaseConfigsSupportedSizeMap)

        // Calculates the target FPS range
        val (isStrictFpsRequired, targetFpsRange) =
            if (findMaxSupportedFrameRate) {
                // In finding maxFps mode, ignore targetFpsRange and isStrictFpsRequired so that the
                // calculations won't be interrupted by any frame rate checks.
                false to FRAME_RATE_RANGE_UNSPECIFIED
            } else {
                val isStrictFpsRequired = isStrictFpsRequired(attachedSurfaces, newUseCaseConfigs)
                val targetFpsRange =
                    getTargetFpsRange(
                        attachedSurfaces,
                        newUseCaseConfigs,
                        useCasesPriorityOrder,
                        isStrictFpsRequired,
                    )
                isStrictFpsRequired to targetFpsRange
            }

        // Ensure preview stabilization is supported by the camera.
        if (isPreviewStabilizationOn && !isPreviewStabilizationSupported) {
            // TODO: b/422055796 - Handle this for non-feature-combo code flows, probably better to
            //  silently fall back to non-preview-stabilization mode in such case.
            require(!isFeatureComboInvocation) {
                "Preview stabilization is not supported by the camera."
            }
        }

        val featureSettings =
            createFeatureSettings(
                cameraMode = cameraMode,
                hasVideoCapture = hasVideoCapture,
                resolvedDynamicRanges = resolvedDynamicRanges,
                isPreviewStabilizationOn = isPreviewStabilizationOn,
                isUltraHdrOn = isUltraHdrOn,
                isHighSpeedOn = isHighSpeedOn,
                isFeatureComboInvocation = isFeatureComboInvocation,
                requiresFeatureComboQuery = false,
                targetFpsRange = targetFpsRange,
                isStrictFpsRequired = isStrictFpsRequired,
            )

        val checkingMethod: CheckingMethod =
            getCheckingMethod(
                resolvedDynamicRanges.values,
                targetFpsRange,
                isPreviewStabilizationOn,
                isUltraHdrOn,
                isFeatureComboInvocation,
            )

        return resolveSpecsByCheckingMethod(
            checkingMethod,
            featureSettings,
            attachedSurfaces,
            filteredNewUseCaseConfigsSupportedSizeMap,
            newUseCaseConfigs,
            useCasesPriorityOrder,
            resolvedDynamicRanges,
            findMaxSupportedFrameRate,
        )
    }

    /**
     * Resolves the suggested stream specifications of the newly added UseCaseConfig according to
     * the provided [CheckingMethod].
     *
     * | **CheckingMethod**                             | **Description**                         |
     * |------------------------------------------------|-----------------------------------------|
     * | [WITH_FEATURE_COMBO]                           | Resolves stream specs using only        |
     * |                                                | [FEATURE_COMBINATION_TABLE].            |
     * | [WITHOUT_FEATURE_COMBO]                        | Resolves stream specs using only        |
     * |                                                | [CAPTURE_SESSION_TABLES].               |
     * | [WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT] | Tries to resolve stream specs using     |
     * |                                                | only [CAPTURE_SESSION_TABLES] first. If |
     * |                                                | fails, retries with                     |
     * |                                                | [FEATURE_COMBINATION_TABLE] next.       |
     *
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *   found. This may be due to no available output size, no available surface combination,
     *   unsupported combinations of {@link DynamicRange}, or requiring an unsupported combination
     *   of camera features.
     */
    private fun resolveSpecsByCheckingMethod(
        checkingMethod: CheckingMethod,
        featureSettings: FeatureSettings,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        filteredNewUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        findMaxSupportedFrameRate: Boolean,
    ): SurfaceStreamSpecQueryResult {
        debug { "resolveSpecsByCheckingMethod: checkingMethod = $checkingMethod" }

        return when (checkingMethod) {
            WITHOUT_FEATURE_COMBO ->
                resolveSpecsBySettings(
                    featureSettings.copy(requiresFeatureComboQuery = false).validateSelf(),
                    attachedSurfaces,
                    filteredNewUseCaseConfigsSupportedSizeMap,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    resolvedDynamicRanges,
                    findMaxSupportedFrameRate,
                )
            WITH_FEATURE_COMBO -> {
                // Use FpsRangeFeature.DEFAULT_FPS_RANGE when Camera2 FCQ checking is required
                val targetFpsRange =
                    if (
                        featureSettings.isFeatureComboInvocation &&
                            featureSettings.targetFpsRange === FRAME_RATE_RANGE_UNSPECIFIED
                    ) {
                        if (featureSettings.requiresFeatureComboQuery) {
                            FpsRangeFeature.DEFAULT_FPS_RANGE
                        } else {
                            featureSettings.targetFpsRange
                        }
                    } else {
                        featureSettings.targetFpsRange
                    }

                resolveSpecsBySettings(
                    featureSettings
                        .copy(requiresFeatureComboQuery = true, targetFpsRange = targetFpsRange)
                        .validateSelf(),
                    attachedSurfaces,
                    filteredNewUseCaseConfigsSupportedSizeMap,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    resolvedDynamicRanges,
                    findMaxSupportedFrameRate,
                )
            }
            WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT -> {
                try {
                    resolveSpecsBySettings(
                        featureSettings.copy(requiresFeatureComboQuery = false).validateSelf(),
                        attachedSurfaces,
                        filteredNewUseCaseConfigsSupportedSizeMap,
                        newUseCaseConfigs,
                        useCasesPriorityOrder,
                        resolvedDynamicRanges,
                        findMaxSupportedFrameRate,
                    )
                } catch (e: IllegalArgumentException) {
                    debug(e) {
                        "Failed to find a supported combination without feature combo" +
                            ", trying again with feature combo"
                    }

                    resolveSpecsBySettings(
                        featureSettings.copy(requiresFeatureComboQuery = true).validateSelf(),
                        attachedSurfaces,
                        filteredNewUseCaseConfigsSupportedSizeMap,
                        newUseCaseConfigs,
                        useCasesPriorityOrder,
                        resolvedDynamicRanges,
                        findMaxSupportedFrameRate,
                    )
                }
            }
        }
    }

    /**
     * Finds the suggested stream specifications of the newly added UseCaseConfig according to the
     * provided {@link ConfigSource}.
     *
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     *   found. This may be due to no available output size, no available surface combination,
     *   unsupported combinations of {@link DynamicRange}, or requiring an unsupported combination
     *   of camera features.
     */
    private fun resolveSpecsBySettings(
        featureSettings: FeatureSettings,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        filteredNewUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        findMaxSupportedFrameRate: Boolean,
    ): SurfaceStreamSpecQueryResult {
        debug { "resolveSpecsBySettings: featureSettings = $featureSettings" }

        // TODO: b/414489781 - Return early even with feature combo source for possible
        //  cases (e.g. the number of streams is higher than what FCQ can ever support)
        if (!featureSettings.isFeatureComboInvocation) {
            require(
                isUseCasesCombinationSupported(
                    featureSettings,
                    attachedSurfaces,
                    filteredNewUseCaseConfigsSupportedSizeMap,
                )
            ) {
                "No supported surface combination is found for camera device - Id : $cameraId. " +
                    "May be attempting to bind too many use cases. Existing surfaces: " +
                    "$attachedSurfaces. New configs: $newUseCaseConfigs. GroupableFeature settings: " +
                    "$featureSettings."
            }
        }

        // Filters the unnecessary output sizes for performance improvement. This will
        // significantly reduce the number of all possible size arrangements below.
        val useCaseConfigToFilteredSupportedSizesMap =
            filterSupportedSizes(
                filteredNewUseCaseConfigsSupportedSizeMap,
                featureSettings,
                /*forceUniqueMaxFpsFiltering=*/ findMaxSupportedFrameRate,
            )
        val supportedOutputSizesList =
            getSupportedOutputSizesList(
                useCaseConfigToFilteredSupportedSizesMap,
                newUseCaseConfigs,
                useCasesPriorityOrder,
            )
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
            if (featureSettings.isHighSpeedOn)
                highSpeedResolver.getSizeArrangements(supportedOutputSizesList)
            else getAllPossibleSizeArrangements(supportedOutputSizesList)
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
                    surfaceConfigIndexAttachedSurfaceInfoMap,
                    surfaceConfigIndexUseCaseConfigMap,
                )
            debug {
                "orderedSurfaceConfigListForStreamUseCase = $orderedSurfaceConfigListForStreamUseCase"
            }
        }

        val maxSupportedFps =
            getMaxSupportedFpsFromAttachedSurfaces(attachedSurfaces, featureSettings.isHighSpeedOn)

        val bestSizesAndFps: BestSizesAndMaxFpsForConfigs? =
            findBestSizesAndFps(
                allPossibleSizeArrangements,
                attachedSurfaces,
                newUseCaseConfigs,
                maxSupportedFps,
                useCasesPriorityOrder,
                featureSettings,
                orderedSurfaceConfigListForStreamUseCase,
                resolvedDynamicRanges,
                findMaxSupportedFrameRate,
            )

        require(bestSizesAndFps != null) {
            "No supported surface combination is found for camera device - Id : $cameraId " +
                "and Hardware level: $hardwareLevel. " +
                "May be the specified resolution is too large and not supported. " +
                "Existing surfaces: $attachedSurfaces. New configs: $newUseCaseConfigs."
        }

        debug { "resolveSpecsBySettings: bestSizesAndFps = $bestSizesAndFps" }

        val suggestedStreamSpecMap =
            generateSuggestedStreamSpecMap(
                bestSizesAndFps,
                newUseCaseConfigs,
                useCasesPriorityOrder,
                resolvedDynamicRanges,
                featureSettings,
            )
        val attachedSurfaceStreamSpecMap = mutableMapOf<AttachedSurfaceInfo, StreamSpec>()

        populateStreamUseCaseIfSameSavedSizes(
            bestSizesAndFps,
            orderedSurfaceConfigListForStreamUseCase,
            attachedSurfaces,
            attachedSurfaceStreamSpecMap,
            suggestedStreamSpecMap,
            surfaceConfigIndexAttachedSurfaceInfoMap,
            surfaceConfigIndexUseCaseConfigMap,
        )

        return SurfaceStreamSpecQueryResult(
            useCaseStreamSpecs = suggestedStreamSpecMap,
            attachedSurfaceStreamSpecs = attachedSurfaceStreamSpecMap,
            maxSupportedFrameRate = bestSizesAndFps.maxFpsForAllSizes,
        )
    }

    private fun getCheckingMethod(
        dynamicRanges: Collection<DynamicRange>,
        fps: Range<Int>?,
        isPreviewStabilizationOn: Boolean,
        isUltraHdrOn: Boolean,
        isFeatureComboInvocation: Boolean,
    ): CheckingMethod {
        if (!isFeatureComboInvocation) {
            return WITHOUT_FEATURE_COMBO
        }

        // TODO: Enforce all supported features are handled by going through some exhaustive list
        //  of supported features.
        var count = 0

        if (dynamicRanges.contains(DynamicRange.HLG_10_BIT)) {
            count++
        }
        if (fps?.getUpper() == 60) {
            count++
        }
        if (isPreviewStabilizationOn) {
            count++
        }
        if (isUltraHdrOn) {
            count++
        }

        return if (count > 1) {
            WITH_FEATURE_COMBO
        } else if (count == 1) {
            WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT
        } else {
            WITHOUT_FEATURE_COMBO
        }
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
        hasVideoCapture: Boolean,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        isPreviewStabilizationOn: Boolean,
        isUltraHdrOn: Boolean,
        isHighSpeedOn: Boolean,
        isFeatureComboInvocation: Boolean,
        requiresFeatureComboQuery: Boolean,
        targetFpsRange: Range<Int>,
        isStrictFpsRequired: Boolean,
    ): FeatureSettings {
        val requiredMaxBitDepth = getRequiredMaxBitDepth(resolvedDynamicRanges)

        return FeatureSettings(
                cameraMode,
                requiredMaxBitDepth,
                hasVideoCapture,
                isPreviewStabilizationOn,
                isUltraHdrOn,
                isHighSpeedOn,
                isFeatureComboInvocation = isFeatureComboInvocation,
                requiresFeatureComboQuery = requiresFeatureComboQuery,
                targetFpsRange = targetFpsRange,
                isStrictFpsRequired = isStrictFpsRequired,
            )
            .validateSelf()
    }

    private fun FeatureSettings.validateSelf(): FeatureSettings {
        require(!(cameraMode != CameraMode.DEFAULT && isUltraHdrOn)) {
            "Camera device Id is $cameraId. Ultra HDR is not " +
                "currently supported in ${CameraMode.toLabelString(cameraMode)} camera mode."
        }

        require(
            !(cameraMode != CameraMode.DEFAULT &&
                requiredMaxBitDepth == DynamicRange.BIT_DEPTH_10_BIT)
        ) {
            "Camera device Id is $cameraId. 10 bit dynamic range is not " +
                "currently supported in ${CameraMode.toLabelString(cameraMode)} camera mode."
        }

        require(!(cameraMode != CameraMode.DEFAULT && isFeatureComboInvocation)) {
            "Camera device Id is $cameraId. feature combination is not " +
                "currently supported in ${CameraMode.toLabelString(cameraMode)} camera mode."
        }

        require(!(isHighSpeedOn && isFeatureComboInvocation)) {
            "High-speed session is not supported with feature combination"
        }

        require(!(isHighSpeedOn && !highSpeedResolver.isHighSpeedSupported)) {
            "High-speed session is not supported on this device."
        }

        return this
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
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
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
            val streamUseCase = useCaseConfig.streamUseCase
            surfaceConfigs.add(
                SurfaceConfig.transformSurfaceConfig(
                    imageFormat = imageFormat,
                    size = minSize,
                    surfaceSizeDefinition = getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                    cameraMode = featureSettings.cameraMode,
                    // FEATURE_COMBINATION_TABLE not needed for the code flows leading to this call
                    configSource = CAPTURE_SESSION_TABLES,
                    streamUseCase = streamUseCase,
                )
            )
        }

        // This method doesn't use feature combo resolutions since feature combo API doesn't
        // guarantee that a lower resolution will always be supported if higher resolution is
        // supported with same set of features
        return checkSupported(featureSettings, surfaceConfigs)
    }

    /**
     * Iterate through all possible size arrangement and returns a surfaceConfig list for stream use
     * case. This list is ordered and the indices of its items are stored into
     * surfaceConfigIndexAttachedSurfaceInfoMap and surfaceConfigIndexUseCaseConfigMap.
     */
    private fun getOrderedSurfaceConfigListForStreamUseCase(
        allPossibleSizeArrangements: List<List<Size>>,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        featureSettings: FeatureSettings,
        surfaceConfigIndexAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo>,
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>,
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
                    surfaceConfigIndexUseCaseConfigMap,
                    false,
                )
            orderedSurfaceConfigListForStreamUseCase =
                getOrderedSupportedStreamUseCaseSurfaceConfigList(
                    featureSettings,
                    surfaceConfigs,
                    surfaceConfigIndexAttachedSurfaceInfoMap,
                    surfaceConfigIndexUseCaseConfigMap,
                )
            if (orderedSurfaceConfigListForStreamUseCase != null) {
                break
            }
            surfaceConfigIndexAttachedSurfaceInfoMap.clear()
            surfaceConfigIndexUseCaseConfigMap.clear()
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
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>,
    ) {
        // Only perform stream use case operations if the saved max FPS and sizes are the same
        if (
            orderedSurfaceConfigListForStreamUseCase != null &&
                bestSizesAndMaxFps.maxFpsForBestSizes ==
                    bestSizesAndMaxFps.maxFpsForStreamUseCase &&
                bestSizesAndMaxFps.bestSizes.size ==
                    bestSizesAndMaxFps.bestSizesForStreamUseCase!!.size
        ) {
            val hasDifferentSavedSizes =
                bestSizesAndMaxFps.bestSizes.zip(bestSizesAndMaxFps.bestSizesForStreamUseCase).any {
                    it.first != it.second
                }
            if (!hasDifferentSavedSizes) {
                val hasStreamUseCaseOverride: Boolean =
                    StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                        cameraMetadata,
                        attachedSurfaces,
                        suggestedStreamSpecMap,
                        attachedSurfaceStreamSpecMap,
                    )
                if (!hasStreamUseCaseOverride) {
                    StreamUseCaseUtil
                        .populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
                            suggestedStreamSpecMap,
                            attachedSurfaceStreamSpecMap,
                            surfaceConfigIndexAttachedSurfaceInfoMap,
                            surfaceConfigIndexUseCaseConfigMap,
                            orderedSurfaceConfigListForStreamUseCase,
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
                    newUseCaseConfigs[index].inputFormat,
                )
            supportedOutputSizesList.add(supportedOutputSizes)
        }
        return supportedOutputSizesList
    }

    private fun getTargetFpsRange(
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        isStrictFpsRequired: Boolean,
    ): Range<Int> {
        var targetFrameRateForConfig: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED
        for (attachedSurfaceInfo in attachedSurfaces) {
            // init target fps range for new configs from existing surfaces
            targetFrameRateForConfig =
                getUpdatedTargetFrameRate(
                    attachedSurfaceInfo.targetFrameRate,
                    targetFrameRateForConfig,
                    isStrictFpsRequired,
                )
        }
        // update target fps for new configs using new use cases' priority order
        for (index in useCasesPriorityOrder) {
            targetFrameRateForConfig =
                getUpdatedTargetFrameRate(
                    newUseCaseConfigs[index].getTargetFrameRate(FRAME_RATE_RANGE_UNSPECIFIED)!!,
                    targetFrameRateForConfig,
                    isStrictFpsRequired,
                )
        }
        return targetFrameRateForConfig
    }

    private fun isStrictFpsRequired(
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
    ): Boolean {
        var isStrictFpsRequired: Boolean? = null
        for (attachedSurfaceInfo in attachedSurfaces) {
            isStrictFpsRequired =
                getAndValidateIsStrictFpsRequired(
                    attachedSurfaceInfo.isStrictFrameRateRequired,
                    isStrictFpsRequired,
                )
        }

        for (newUseCaseConfigs in newUseCaseConfigs) {
            isStrictFpsRequired =
                getAndValidateIsStrictFpsRequired(
                    newUseCaseConfigs.isStrictFrameRateRequired,
                    isStrictFpsRequired,
                )
        }
        return isStrictFpsRequired ?: false
    }

    private fun getMaxSupportedFpsFromAttachedSurfaces(
        attachedSurfaces: List<AttachedSurfaceInfo>,
        isHighSpeedOn: Boolean,
    ): Int {
        var existingSurfaceFrameRateCeiling = FRAME_RATE_UNLIMITED
        for (attachedSurfaceInfo in attachedSurfaces) {
            // get the fps ceiling for existing surfaces
            existingSurfaceFrameRateCeiling =
                getUpdatedMaximumFps(
                    existingSurfaceFrameRateCeiling,
                    attachedSurfaceInfo.imageFormat,
                    attachedSurfaceInfo.size,
                    isHighSpeedOn,
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
    @VisibleForTesting
    internal fun filterSupportedSizes(
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        featureSettings: FeatureSettings,
        forceUniqueMaxFpsFiltering: Boolean = false,
    ): Map<UseCaseConfig<*>, List<Size>> {
        val filteredUseCaseConfigToSupportedSizesMap = mutableMapOf<UseCaseConfig<*>, List<Size>>()
        for (useCaseConfig in newUseCaseConfigsSupportedSizeMap.keys) {
            val reducedSizeList = mutableListOf<Size>()
            val configSizeUniqueMaxFpsMap = mutableMapOf<ConfigSize, MutableSet<Int>>()
            for (size in newUseCaseConfigsSupportedSizeMap[useCaseConfig]!!) {
                val imageFormat = useCaseConfig.inputFormat
                val streamUseCase = useCaseConfig.streamUseCase
                populateReducedSizeListAndUniqueMaxFpsMap(
                    featureSettings,
                    size,
                    imageFormat,
                    streamUseCase,
                    forceUniqueMaxFpsFiltering,
                    configSizeUniqueMaxFpsMap,
                    reducedSizeList,
                )
            }
            filteredUseCaseConfigToSupportedSizesMap[useCaseConfig] = reducedSizeList
        }
        return filteredUseCaseConfigToSupportedSizesMap
    }

    private fun populateReducedSizeListAndUniqueMaxFpsMap(
        featureSettings: FeatureSettings,
        size: Size,
        imageFormat: Int,
        streamUseCase: StreamUseCase,
        forceUniqueMaxFpsFiltering: Boolean,
        configSizeUniqueMaxFpsMap: MutableMap<ConfigSize, MutableSet<Int>>,
        reducedSizeList: MutableList<Size>,
    ) {
        val configSize =
            SurfaceConfig.transformSurfaceConfig(
                    imageFormat = imageFormat,
                    size = size,
                    surfaceSizeDefinition = getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                    cameraMode = featureSettings.cameraMode,
                    configSource =
                        if (featureSettings.requiresFeatureComboQuery) {
                            FEATURE_COMBINATION_TABLE
                        } else {
                            CAPTURE_SESSION_TABLES
                        },
                    streamUseCase = streamUseCase,
                )
                .configSize

        // Filters the sizes with frame rate only if there is target FPS setting or force enabled.
        val maxFrameRate =
            if (
                featureSettings.targetFpsRange != FRAME_RATE_RANGE_UNSPECIFIED ||
                    forceUniqueMaxFpsFiltering
            ) {
                getMaxFrameRate(imageFormat, size, featureSettings.isHighSpeedOn)
            } else {
                FRAME_RATE_UNLIMITED
            }

        // For feature combination, target FPS range must be strictly supported, so we can filter
        // out unsupported sizes earlier. Feature combination may also have some output
        // sizes
        // mapping to ConfigSize.NOT_SUPPORT, those can be filtered out earlier as well.
        if (
            featureSettings.isFeatureComboInvocation &&
                (configSize == ConfigSize.NOT_SUPPORT ||
                    (featureSettings.targetFpsRange != FRAME_RATE_RANGE_UNSPECIFIED &&
                        maxFrameRate < featureSettings.targetFpsRange.getUpper()))
        ) {
            return
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

    private fun findBestSizesAndFps(
        allPossibleSizeArrangements: List<List<Size>>,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        existingSurfaceFrameRateCeiling: Int,
        useCasesPriorityOrder: List<Int>,
        featureSettings: FeatureSettings,
        orderedSurfaceConfigListForStreamUseCase: List<SurfaceConfig>?,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        findMaxFpsForAllSizes: Boolean,
    ): BestSizesAndMaxFpsForConfigs? {
        var bestSizes: List<Size>? = null
        var maxFpsForBestSizes = FRAME_RATE_UNLIMITED
        var bestSizesForStreamUseCase: List<Size>? = null
        var maxFpsForStreamUseCase = FRAME_RATE_UNLIMITED
        var supportedSizesFound = false
        var supportedSizesForStreamUseCaseFound = false
        var maxFpsForAllSizes = Int.MAX_VALUE

        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (possibleSizeList in allPossibleSizeArrangements) {
            val surfaceConfigIndexToAttachedSurfaceInfoMap =
                mutableMapOf<Int, AttachedSurfaceInfo>()
            val surfaceConfigIndexToUseCaseConfigMap = mutableMapOf<Int, UseCaseConfig<*>>()

            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            val surfaceConfigList =
                getSurfaceConfigList(
                    featureSettings.cameraMode,
                    attachedSurfaces,
                    possibleSizeList,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    surfaceConfigIndexToAttachedSurfaceInfoMap,
                    surfaceConfigIndexToUseCaseConfigMap,
                    featureSettings.requiresFeatureComboQuery,
                )
            val currentConfigFrameRateCeiling =
                getCurrentConfigFrameRateCeiling(
                    possibleSizeList,
                    newUseCaseConfigs,
                    useCasesPriorityOrder,
                    existingSurfaceFrameRateCeiling,
                    featureSettings.isHighSpeedOn,
                )
            val isConfigFrameRateAcceptable =
                isConfigFrameRateAcceptable(
                    existingSurfaceFrameRateCeiling,
                    featureSettings.targetFpsRange,
                    currentConfigFrameRateCeiling,
                )

            val dynamicRangesBySurfaceConfig = mutableMapOf<SurfaceConfig, DynamicRange>()
            surfaceConfigList.forEachIndexed { index, surfaceConfig ->
                val dynamicRange =
                    surfaceConfigIndexToAttachedSurfaceInfoMap[index]?.dynamicRange
                        ?: requireNotNull(
                            resolvedDynamicRanges[surfaceConfigIndexToUseCaseConfigMap[index]]
                        )
                dynamicRangesBySurfaceConfig[surfaceConfig] = dynamicRange
            }

            val isSupported by
                lazy(mode = LazyThreadSafetyMode.NONE) {
                    checkSupported(
                        featureSettings,
                        surfaceConfigList,
                        dynamicRangesBySurfaceConfig,
                        newUseCaseConfigs,
                        useCasesPriorityOrder,
                    )
                }

            if (findMaxFpsForAllSizes && isSupported) {
                if (maxFpsForAllSizes == Int.MAX_VALUE) {
                    maxFpsForAllSizes = currentConfigFrameRateCeiling
                } else if (maxFpsForAllSizes < currentConfigFrameRateCeiling) {
                    maxFpsForAllSizes = currentConfigFrameRateCeiling
                }
            }

            // Find the same possible size arrangement that is supported by stream use case again
            // if we found one earlier.

            // only change the saved config if you get another that has a better max fps
            if (!supportedSizesFound && isSupported) {
                // if the config is supported by the device but doesn't meet the target frame rate,
                // save the config
                if (maxFpsForBestSizes == FRAME_RATE_UNLIMITED) {
                    maxFpsForBestSizes = currentConfigFrameRateCeiling
                    bestSizes = possibleSizeList
                } else if (maxFpsForBestSizes < currentConfigFrameRateCeiling) {
                    // only change the saved config if the max fps is better
                    maxFpsForBestSizes = currentConfigFrameRateCeiling
                    bestSizes = possibleSizeList
                }

                if (isConfigFrameRateAcceptable) {
                    maxFpsForBestSizes = currentConfigFrameRateCeiling
                    bestSizes = possibleSizeList
                    supportedSizesFound = true

                    // if we have a configuration where the max fps is acceptable for our target,
                    // break. But never break when findMaxFpsForAllSizes flag is set.
                    if (supportedSizesForStreamUseCaseFound && !findMaxFpsForAllSizes) {
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
                        surfaceConfigList,
                        surfaceConfigIndexToAttachedSurfaceInfoMap,
                        surfaceConfigIndexToUseCaseConfigMap,
                    ) != null
            ) {
                if (maxFpsForStreamUseCase == FRAME_RATE_UNLIMITED) {
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
                    // Never break when findMaxFpsForAllSizes flag is set.
                    if (supportedSizesFound && !findMaxFpsForAllSizes) {
                        break
                    }
                }
            }
        }

        if (bestSizes == null) {
            return null
        }

        // When using the combinations guaranteed via feature combination APIs, targetFpsRange must
        // be strictly maintained rather than just choosing the combination with highest max FPS.
        if (
            featureSettings.isFeatureComboInvocation &&
                featureSettings.targetFpsRange != FRAME_RATE_RANGE_UNSPECIFIED &&
                (maxFpsForBestSizes == FRAME_RATE_UNLIMITED ||
                    maxFpsForBestSizes < featureSettings.targetFpsRange.getUpper())
        ) {
            return null
        }

        return BestSizesAndMaxFpsForConfigs(
            bestSizes,
            bestSizesForStreamUseCase,
            maxFpsForBestSizes,
            maxFpsForStreamUseCase,
            maxFpsForAllSizes,
        )
    }

    private fun isConfigFrameRateAcceptable(
        existingSurfaceFrameRateCeiling: Int,
        targetFpsRange: Range<Int>,
        currentConfigFrameRateCeiling: Int,
    ): Boolean {
        var isConfigFrameRateAcceptable = true
        if (targetFpsRange != FRAME_RATE_RANGE_UNSPECIFIED) {
            // TODO: b/402372530 - currentConfigFrameRateCeiling < targetFpsRange.getUpper() to
            //  return false means that there should still be other better choice because
            //  currentConfigFrameRateCeiling is still smaller than both maxSupportedFps and
            //  targetFpsRange.getUpper(). However, for feature combo cases, we should strictly
            //  maintain the target FPS range being fully supported. It doesn't need to be handled
            //  right now though since feature combo API supports lower == upper case (i.e. FPS_60)
            //  only right now.
            if (
                currentConfigFrameRateCeiling < existingSurfaceFrameRateCeiling &&
                    currentConfigFrameRateCeiling < targetFpsRange.upper
            ) {
                // if the max fps before adding new use cases supports our target fps range
                // BUT the max fps of the new configuration is below
                // our target fps range, we'll want to check the next configuration until we
                // get one that supports our target FPS
                isConfigFrameRateAcceptable = false
            }
        }

        return isConfigFrameRateAcceptable
    }

    private fun generateSuggestedStreamSpecMap(
        bestSizesAndMaxFps: BestSizesAndMaxFpsForConfigs,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasesPriorityOrder: List<Int>,
        resolvedDynamicRanges: Map<UseCaseConfig<*>, DynamicRange>,
        featureSettings: FeatureSettings,
    ): MutableMap<UseCaseConfig<*>, StreamSpec> {
        val suggestedStreamSpecMap = mutableMapOf<UseCaseConfig<*>, StreamSpec>()
        var targetFrameRateForDevice = FRAME_RATE_RANGE_UNSPECIFIED
        if (featureSettings.targetFpsRange != FRAME_RATE_RANGE_UNSPECIFIED) {
            // get all fps ranges supported by device
            val availableFpsRanges =
                if (featureSettings.isHighSpeedOn) {
                    highSpeedResolver.getFrameRateRangesFor(bestSizesAndMaxFps.bestSizes)
                } else {
                    cameraMetadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
                }
            targetFrameRateForDevice =
                getClosestSupportedDeviceFrameRate(
                    featureSettings.targetFpsRange,
                    bestSizesAndMaxFps.maxFpsForBestSizes,
                    availableFpsRanges,
                )

            if (featureSettings.isFeatureComboInvocation || featureSettings.isStrictFpsRequired) {
                require(targetFrameRateForDevice == featureSettings.targetFpsRange) {
                    "Target FPS range ${featureSettings.targetFpsRange} is not supported." +
                        " Max FPS supported by the calculated best combination:" +
                        " ${bestSizesAndMaxFps.maxFpsForBestSizes}. Calculated best FPS range for" +
                        " device: $targetFrameRateForDevice. Device supported FPS ranges:" +
                        " ${availableFpsRanges.contentToString()}."
                }
            }
        }
        for ((index, useCaseConfig) in newUseCaseConfigs.withIndex()) {
            val resolutionForUseCase =
                bestSizesAndMaxFps.bestSizes[useCasesPriorityOrder.indexOf(index)]
            val streamSpecBuilder =
                StreamSpec.builder(resolutionForUseCase)
                    .setSessionType(
                        if (featureSettings.isHighSpeedOn) SESSION_TYPE_HIGH_SPEED
                        else SESSION_TYPE_REGULAR
                    )
                    .setDynamicRange(checkNotNull(resolvedDynamicRanges[useCaseConfig]))
                    .setImplementationOptions(
                        StreamUseCaseUtil.getStreamSpecImplementationOptions(useCaseConfig)
                    )
                    .setZslDisabled(featureSettings.hasVideoCapture)

            if (targetFrameRateForDevice != FRAME_RATE_RANGE_UNSPECIFIED) {
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
        surfaceConfigIndexUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>>?,
        checkViaFeatureComboQuery: Boolean,
    ): List<SurfaceConfig> {
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
            val streamUseCase = newUseCase.streamUseCase
            // add new use case/size config to list of surfaces
            val surfaceConfig =
                SurfaceConfig.transformSurfaceConfig(
                    imageFormat = imageFormat,
                    size = size,
                    surfaceSizeDefinition = getUpdatedSurfaceSizeDefinitionByFormat(imageFormat),
                    cameraMode = cameraMode,
                    configSource =
                        if (checkViaFeatureComboQuery) {
                            FEATURE_COMBINATION_TABLE
                        } else {
                            CAPTURE_SESSION_TABLES
                        },
                    streamUseCase = streamUseCase,
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
        isHighSpeedOn: Boolean,
    ): Int {
        var newConfigFrameRateCeiling: Int = currentConfigFrameRateCeiling
        // Attach SurfaceConfig of new use cases
        for ((i, size) in possibleSizeList.withIndex()) {
            val newUseCase = newUseCaseConfigs[useCasesPriorityOrder[i]]
            // get the maximum fps of the new surface and update the maximum fps of the
            // proposed configuration
            newConfigFrameRateCeiling =
                getUpdatedMaximumFps(
                    newConfigFrameRateCeiling,
                    newUseCase.inputFormat,
                    size,
                    isHighSpeedOn,
                )
        }
        return newConfigFrameRateCeiling
    }

    private fun getMaxFrameRate(imageFormat: Int, size: Size, isHighSpeedOn: Boolean): Int {
        return if (isHighSpeedOn) {
            check(imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)
            highSpeedResolver.getMaxFrameRate(size)
        } else {
            getMaxFrameRate(imageFormat, size)
        }
    }

    private fun getMaxFrameRate(imageFormat: Int, size: Size): Int {
        val minFrameDuration =
            getStreamConfigurationMapCompat().getOutputMinFrameDuration(imageFormat, size)
        if (minFrameDuration <= 0L) {
            if (isManualSensorSupported) {
                warn {
                    "minFrameDuration: $minFrameDuration is invalid for imageFormat = $imageFormat, size = $size"
                }
                return 0
            } else {
                // According to the doc, getOutputMinFrameDuration may return 0 if device doesn't
                // support manual sensor. Return MAX_VALUE indicates no limit.
                return FRAME_RATE_UNLIMITED
            }
        }
        return (1_000_000_000.0 / minFrameDuration).toInt()
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
        newRange: Range<Int>,
    ): Range<Int> {
        // TODO(b/272075984): some ranges may may have a larger intersection but may also have an
        //  excessively large portion that is non-intersecting. Will want to do further
        //  investigation to find a more optimized way to decide when a potential range has too
        //  much non-intersecting value and discard it
        val storedIntersectionSize = getRangeLength(storedRange.intersect(targetFps)).toDouble()
        val newIntersectionSize = getRangeLength(newRange.intersect(targetFps)).toDouble()
        val newRangeRatio = newIntersectionSize / getRangeLength(newRange)
        val storedRangeRatio = storedIntersectionSize / getRangeLength(storedRange)
        if (newIntersectionSize > storedIntersectionSize) {
            // if new, the new range must have at least 50% of its range intersecting, OR has a
            // larger percentage of intersection than the previous stored range
            if (newRangeRatio >= .5 || newRangeRatio >= storedRangeRatio) {
                return newRange
            }
        } else if (newIntersectionSize == storedIntersectionSize) {
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
     * Finds a frame rate range supported by the device that is closest to the target frame rate.
     *
     * This function first adjusts the `targetFrameRate` to ensure it does not exceed `maxFps`, i.e.
     * the target frame rate is capped by `maxFps` before comparison. For example, if target is
     * [30,60] and `maxFps` is 50, the effective target for comparison becomes [30,50].
     *
     * Then, the function iterates through `availableFpsRanges` to find the best match.
     *
     * The selection prioritizes ranges that:
     * 1. Exactly match the target frame rate.
     * 2. Intersect with the target frame rate. Among intersecting ranges, the one with the largest
     *    intersection is chosen. If multiple ranges have the same largest intersection, further
     *    tie-breaking rules are applied (see [compareIntersectingRanges]).
     * 3. Do not intersect with the target frame rate. Among non-intersecting ranges, the one with
     *    the smallest distance to the target frame rate is chosen. If multiple ranges have the same
     *    smallest distance, the higher range is preferred. If they are still tied (e.g., one range
     *    is above and one is below with the same distance), the range with the shorter length is
     *    chosen.
     *
     * @param targetFrameRate The Target Frame Rate resolved from all current existing surfaces and
     *   incoming new use cases.
     * @param maxFps The maximum FPS allowed by the current configuration.
     * @param availableFpsRanges A nullable array of frame rate ranges supported by the device.
     * @return A frame rate range supported by the device that is closest to the `targetFrameRate`,
     *   or [StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED] if no suitable range is found or inputs are
     *   invalid.
     */
    private fun getClosestSupportedDeviceFrameRate(
        targetFrameRate: Range<Int>,
        maxFps: Int,
        availableFpsRanges: Array<out Range<Int>>?,
    ): Range<Int> {
        if (targetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED) {
            return FRAME_RATE_RANGE_UNSPECIFIED
        }

        availableFpsRanges ?: return FRAME_RATE_RANGE_UNSPECIFIED

        var newTargetFrameRate = targetFrameRate
        // if  whole target frame rate range > maxFps of configuration, the target for this
        // calculation will be [max,max].

        // if the range is partially larger than  maxFps, the target for this calculation will be
        // [target.lower, max] for the sake of this calculation
        newTargetFrameRate =
            Range(min(newTargetFrameRate.lower, maxFps), min(newTargetFrameRate.upper, maxFps))
        var bestRange = FRAME_RATE_RANGE_UNSPECIFIED
        var currentIntersectSize = 0
        for (potentialRange in availableFpsRanges) {
            // ignore ranges completely larger than configuration's maximum fps
            if (maxFps < potentialRange.lower) {
                continue
            }
            if (bestRange == FRAME_RATE_RANGE_UNSPECIFIED) {
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
            } catch (_: IllegalArgumentException) {
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
     * Calculates the updated target frame rate based on a new target frame rate and a previously
     * stored target frame rate.
     *
     * If strict fps is required and both new and stored frame rates are not unspecified, they must
     * be the same or an `IllegalStateException` will be thrown.
     *
     * If strict fps is not required and both new and stored target frame rate are not unspecified,
     * the intersection of ranges will be adopted. If the ranges are disjoint, the stored frame rate
     * will be used.
     *
     * @param newTargetFrameRate an incoming frame rate range
     * @param storedTargetFrameRate a stored frame rate range to be modified
     * @param isStrictFpsRequired whether strict fps is required
     * @return adjusted target frame rate
     */
    private fun getUpdatedTargetFrameRate(
        newTargetFrameRate: Range<Int>,
        storedTargetFrameRate: Range<Int>,
        isStrictFpsRequired: Boolean,
    ): Range<Int> {
        if (
            storedTargetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED &&
                newTargetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED
        ) {
            return FRAME_RATE_RANGE_UNSPECIFIED
        } else if (storedTargetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED) {
            return newTargetFrameRate
        } else if (newTargetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED) {
            return storedTargetFrameRate
        } else {
            if (isStrictFpsRequired) {
                // An IllegalStateException is thrown here because this is an implementation error
                // rather than an unsupported combination. Currently isStrictFpsRequired is true
                // only when SessionConfig frame rate API is used.
                Preconditions.checkState(
                    newTargetFrameRate == storedTargetFrameRate,
                    "All targetFrameRate should be the same if strict fps is required",
                )
                return newTargetFrameRate
            } else {
                return try {
                    // get intersection of existing target fps
                    storedTargetFrameRate.intersect(newTargetFrameRate)
                } catch (_: IllegalArgumentException) {
                    // no intersection, keep the previously stored value
                    storedTargetFrameRate
                }
            }
        }
    }

    private fun getAndValidateIsStrictFpsRequired(
        newIsStrictFpsRequired: Boolean,
        storedIsStrictFpsRequired: Boolean?,
    ): Boolean {
        if (
            storedIsStrictFpsRequired != null && storedIsStrictFpsRequired != newIsStrictFpsRequired
        ) {
            // An IllegalStateException is thrown here because this is an implementation error
            // rather than an unsupported combination. Currently isStrictFpsRequired is true
            // only when SessionConfig frame rate API is used.
            throw IllegalStateException("All isStrictFpsRequired should be the same")
        }
        return newIsStrictFpsRequired
    }

    /**
     * @param currentMaxFps the previously stored Max FPS
     * @param imageFormat the image format of the incoming surface
     * @param size the size of the incoming surface
     * @param isHighSpeedOn whether high-speed session is enabled
     */
    private fun getUpdatedMaximumFps(
        currentMaxFps: Int,
        imageFormat: Int,
        size: Size,
        isHighSpeedOn: Boolean,
    ): Int {
        return min(currentMaxFps, getMaxFrameRate(imageFormat, size, isHighSpeedOn))
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
        imageFormat: Int,
    ): List<Size> {
        // Applies TargetAspectRatio workaround
        val ratio: Rational? =
            when (targetAspectRatio[cameraMetadata, streamConfigurationMapCompat]) {
                TargetAspectRatio.RATIO_4_3 -> AspectRatioUtil.ASPECT_RATIO_4_3
                TargetAspectRatio.RATIO_16_9 -> AspectRatioUtil.ASPECT_RATIO_16_9
                TargetAspectRatio.RATIO_MAX_JPEG ->
                    getUpdatedSurfaceSizeDefinitionByFormat(ImageFormat.JPEG)
                        .getMaximumSize(ImageFormat.JPEG)
                        ?.let { maxJpegSize -> Rational(maxJpegSize.width, maxJpegSize.height) }
                TargetAspectRatio.RATIO_ORIGINAL -> null
                else -> throw AssertionError("Undefined targetAspectRatio: $targetAspectRatio")
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
            resultList,
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
                    surfaceSizeDefinition.maximum4x3SizeMap,
                    surfaceSizeDefinition.maximum16x9SizeMap,
                    surfaceSizeDefinition.ultraMaximumSizeMap,
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
            isManualSensorSupported =
                contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        }
    }

    /** Generate the supported combination list from guaranteed configurations tables. */
    private fun generateSupportedCombinationList() {
        surfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.generateSupportedCombinationList(
                hardwareLevel,
                isRawSupported,
                isBurstCaptureSupported,
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

    private fun generateHighSpeedSupportedCombinationList() {
        if (!highSpeedResolver.isHighSpeedSupported) {
            return
        }
        highSpeedSurfaceCombinations.clear()
        // Find maximum supported size.
        highSpeedResolver.maxSize?.let { maxSize ->
            highSpeedSurfaceCombinations.addAll(
                GuaranteedConfigurationsUtil.generateHighSpeedSupportedCombinationList(
                    maxSize,
                    getUpdatedSurfaceSizeDefinitionByFormat(
                        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    ),
                )
            )
        }
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
                mutableMapOf(), // maximum4x3SizeMap
                mutableMapOf(), // maximum16x9SizeMap
                mutableMapOf(), // ultraMaximumSizeMap
            )
    }

    /** Updates the surface size definition for the specified format then return it. */
    @VisibleForTesting
    public fun getUpdatedSurfaceSizeDefinitionByFormat(format: Int): SurfaceSizeDefinition {
        if (!surfaceSizeDefinitionFormats.contains(format)) {
            updateS720pOrS1440pSizeByFormat(
                surfaceSizeDefinition.s720pSizeMap,
                RESOLUTION_720P,
                format,
            )
            updateS720pOrS1440pSizeByFormat(
                surfaceSizeDefinition.s1440pSizeMap,
                RESOLUTION_1440P,
                format,
            )
            updateMaximumSizeByFormat(surfaceSizeDefinition.maximumSizeMap, format)
            updateMaximumSizeByFormat(
                surfaceSizeDefinition.maximum4x3SizeMap,
                format,
                AspectRatioUtil.ASPECT_RATIO_4_3,
            )
            updateMaximumSizeByFormat(
                surfaceSizeDefinition.maximum16x9SizeMap,
                format,
                AspectRatioUtil.ASPECT_RATIO_16_9,
            )
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
        format: Int,
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
    private fun updateMaximumSizeByFormat(
        sizeMap: MutableMap<Int, Size>,
        format: Int,
        aspectRatio: Rational? = null,
    ) {
        val originalMap = streamConfigurationMapCompat.toStreamConfigurationMap()
        getMaxOutputSizeByFormat(originalMap, format, true, aspectRatio)?.let {
            sizeMap[format] = it
        }
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

            val recordSize = getRecordSizeFromCamcorderProfile()
            if (recordSize != null) {
                return recordSize
            }
        } catch (_: NumberFormatException) {
            // The camera Id is not an integer. The camera may be a removable device.
        }
        // Use StreamConfigurationMap to determine the RECORD size.
        val recordSize = getRecordSizeFromStreamConfigurationMapCompat()
        if (recordSize != null) {
            return recordSize
        }

        return RESOLUTION_480P
    }

    /** Obtains the stream configuration map from camera meta data. */
    private fun getStreamConfigurationMapCompat(): StreamConfigurationMapCompat {
        val map =
            cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: throw IllegalArgumentException("Cannot retrieve SCALER_STREAM_CONFIGURATION_MAP")
        return StreamConfigurationMapCompat(map, OutputSizesCorrector(cameraMetadata, map))
    }

    /**
     * Returns the maximum supported video size for cameras using data from the stream configuration
     * map.
     *
     * @return Maximum supported video size or null if none are found.
     */
    private fun getRecordSizeFromStreamConfigurationMapCompat(): Size? {
        val map = streamConfigurationMapCompat.toStreamConfigurationMap()
        val videoSizeArr =
            runCatching {
                    // b/378508360: try-catch to workaround the exception when using
                    // StreamConfigurationMap provided by Robolectric.
                    map?.getOutputSizes(MediaRecorder::class.java)
                }
                .getOrNull() ?: return null
        Arrays.sort(videoSizeArr, CompareSizesByArea(true))
        for (size in videoSizeArr) {
            if (size.width <= RESOLUTION_1080P.width && size.height <= RESOLUTION_1080P.height) {
                return size
            }
        }
        return null
    }

    /**
     * Returns the maximum supported video size for cameras by [CamcorderProfile.hasProfile].
     *
     * @return Maximum supported video size or null if none are found.
     */
    private fun getRecordSizeFromCamcorderProfile(): Size? {
        val qualities =
            listOf(
                CamcorderProfile.QUALITY_HIGH,
                CamcorderProfile.QUALITY_8KUHD,
                CamcorderProfile.QUALITY_4KDCI,
                CamcorderProfile.QUALITY_2160P,
                CamcorderProfile.QUALITY_2K,
                CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_480P,
            )

        for (quality in qualities) {
            if (encoderProfilesProvider.hasProfile(quality)) {
                val profiles = encoderProfilesProvider.getAll(quality)
                if (profiles != null && profiles.videoProfiles.isNotEmpty()) {
                    return profiles.videoProfiles[0]!!.resolution
                }
            }
        }

        return null
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
    internal fun getMaxOutputSizeByFormat(
        map: StreamConfigurationMap?,
        imageFormat: Int,
        highResolutionIncluded: Boolean,
        aspectRatio: Rational? = null,
    ): Size? {
        val outputSizes: Array<Size>? = getOutputSizes(map, imageFormat, aspectRatio)
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

    private fun getOutputSizes(
        map: StreamConfigurationMap?,
        imageFormat: Int,
        aspectRatio: Rational? = null,
    ): Array<Size>? {
        return runCatching {
                // b/378508360: try-catch to workaround the exception when using
                // StreamConfigurationMap provided by Robolectric.
                if (imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
                    // This is a little tricky that 0x22 that is internal defined in
                    // StreamConfigurationMap.java to be equal to ImageFormat.PRIVATE that is
                    // public
                    // after Android level 23 but not public in Android L. Use {@link
                    // SurfaceTexture}
                    // or {@link MediaCodec} will finally mapped to 0x22 in
                    // StreamConfigurationMap to
                    // retrieve the output sizes information.
                    map?.getOutputSizes(SurfaceTexture::class.java)
                } else {
                    map?.getOutputSizes(imageFormat)
                }
            }
            .getOrNull()
            ?.run {
                if (aspectRatio != null) {
                    filter { AspectRatioUtil.hasMatchingAspectRatio(it, aspectRatio) }
                        .toTypedArray()
                } else {
                    this
                }
            }
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
        repeat(totalArrangementsCount) {
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
        val hasVideoCapture: Boolean = false,
        val isPreviewStabilizationOn: Boolean = false,
        val isUltraHdrOn: Boolean = false,
        val isHighSpeedOn: Boolean = false,
        val isFeatureComboInvocation: Boolean = false,
        val requiresFeatureComboQuery: Boolean = false,
        val targetFpsRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
        val isStrictFpsRequired: Boolean = false,
    )

    public data class BestSizesAndMaxFpsForConfigs(
        val bestSizes: List<Size>,
        val bestSizesForStreamUseCase: List<Size>?,
        val maxFpsForBestSizes: Int,
        val maxFpsForStreamUseCase: Int,
        val maxFpsForAllSizes: Int,
    )

    internal enum class CheckingMethod {
        WITHOUT_FEATURE_COMBO,
        WITH_FEATURE_COMBO,
        WITHOUT_FEATURE_COMBO_FIRST_AND_THEN_WITH_IT,
    }

    public companion object {
        private const val FRAME_RATE_UNLIMITED = Int.MAX_VALUE

        private fun isUltraHdrOn(
            attachedSurfaces: List<AttachedSurfaceInfo>,
            newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
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
