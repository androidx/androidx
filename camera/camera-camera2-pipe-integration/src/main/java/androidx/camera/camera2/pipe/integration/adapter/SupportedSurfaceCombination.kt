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
import android.content.pm.PackageManager.FEATURE_CAMERA_CONCURRENT
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.hardware.display.DisplayManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.ExtraSupportedSurfaceCombinationsContainer
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.ResolutionCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.TargetAspectRatio
import androidx.camera.camera2.pipe.integration.impl.DisplayInfoManager
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
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
@RequiresApi(21) // TODO(b/243963130): Remove and replace with annotation on package-info.java
// TODO(b/200306659): Remove and replace with annotation on package-info.java
class SupportedSurfaceCombination(
    context: Context,
    private val cameraMetadata: CameraMetadata,
    private val encoderProfilesProviderAdapter: EncoderProfilesProviderAdapter
) {
    private val cameraId = cameraMetadata.camera.value
    private val hardwareLevel =
        cameraMetadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    private val concurrentSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val surfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val ultraHighSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
    private val cameraModeToSupportedCombinationsMap: MutableMap<Int, List<SurfaceCombination>> =
        mutableMapOf()
    private var isRawSupported = false
    private var isBurstCaptureSupported = false
    private var isConcurrentCameraModeSupported = false
    private var isUltraHighResolutionSensorSupported = false
    internal lateinit var surfaceSizeDefinition: SurfaceSizeDefinition
    private val surfaceSizeDefinitionFormats = mutableListOf<Int>()
    private val displayManager: DisplayManager =
        (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
    private val streamConfigurationMapCompat = getStreamConfigurationMapCompat()
    private val extraSupportedSurfaceCombinationsContainer =
        ExtraSupportedSurfaceCombinationsContainer()
    private val displayInfoManager = DisplayInfoManager(context)
    private val resolutionCorrector = ResolutionCorrector()
    private val targetAspectRatio: TargetAspectRatio = TargetAspectRatio()

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
        generateSurfaceSizeDefinition()
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param cameraMode        the working camera mode.
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    fun checkSupported(
        cameraMode: Int,
        surfaceConfigList: List<SurfaceConfig>
    ): Boolean {
        // TODO(b/262772650): camera-pipe support for concurrent camera
        val targetSurfaceCombinations = getSurfaceCombinationsByCameraMode(cameraMode)
        for (surfaceCombination in targetSurfaceCombinations) {
            if (surfaceCombination.isSupported(surfaceConfigList)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the supported surface combinations according to the specified camera mode.
     */
    private fun getSurfaceCombinationsByCameraMode(
        @CameraMode.Mode cameraMode: Int
    ): List<SurfaceCombination> {
        if (cameraModeToSupportedCombinationsMap.containsKey(cameraMode)) {
            return cameraModeToSupportedCombinationsMap[cameraMode]!!
        }
        var supportedSurfaceCombinations: MutableList<SurfaceCombination> = mutableListOf()
        when (cameraMode) {
            CameraMode.CONCURRENT_CAMERA -> supportedSurfaceCombinations =
                concurrentSurfaceCombinations

            CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA -> {
                supportedSurfaceCombinations.addAll(ultraHighSurfaceCombinations)
                supportedSurfaceCombinations.addAll(surfaceCombinations)
            }

            else -> supportedSurfaceCombinations.addAll(surfaceCombinations)
        }
        cameraModeToSupportedCombinationsMap[cameraMode] = supportedSurfaceCombinations
        return supportedSurfaceCombinations
    }

    /**
     * Transform to a SurfaceConfig object with image format and size info
     *
     * @param cameraMode  the working camera mode.
     * @param imageFormat the image format info for the surface configuration object
     * @param size        the size info for the surface configuration object
     * @return new [SurfaceConfig] object
     */
    fun transformSurfaceConfig(
        cameraMode: Int,
        imageFormat: Int,
        size: Size
    ): SurfaceConfig {
        return SurfaceConfig.transformSurfaceConfig(
            cameraMode,
            imageFormat, size, getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        )
    }

    /**
     * Finds the suggested stream specification of the newly added UseCaseConfig.
     *
     * @param cameraMode        the working camera mode.
     * @param existingSurfaces  the existing surfaces.
     * @param newUseCaseConfigsSupportedSizeMap newly added UseCaseConfig to supported output sizes
     * map.
     * @return the suggested stream specs, which is a mapping from UseCaseConfig to the suggested
     * stream specification.
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     * found. This may be due to no available output size or no available surface combination.
     */
    fun getSuggestedStreamSpecifications(
        cameraMode: Int,
        existingSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>
    ): Map<UseCaseConfig<*>, StreamSpec> {
        refreshPreviewSize()
        val surfaceConfigs: MutableList<SurfaceConfig> = ArrayList()
        for (scc in existingSurfaces) {
            surfaceConfigs.add(scc.surfaceConfig)
        }
        val newUseCaseConfigs = newUseCaseConfigsSupportedSizeMap.keys.toList()
        // Use the small size (640x480) for new use cases to check whether there is any possible
        // supported combination first
        for (useCaseConfig in newUseCaseConfigs) {
            surfaceConfigs.add(
                SurfaceConfig.transformSurfaceConfig(
                    cameraMode,
                    useCaseConfig.inputFormat,
                    RESOLUTION_VGA,
                    getUpdatedSurfaceSizeDefinitionByFormat(useCaseConfig.inputFormat)
                )
            )
        }

        if (!checkSupported(cameraMode, surfaceConfigs)) {
            throw java.lang.IllegalArgumentException(
                "No supported surface combination is found for camera device - Id : " + cameraId +
                    ".  May be attempting to bind too many use cases. " + "Existing surfaces: " +
                    existingSurfaces + " New configs: " + newUseCaseConfigs
            )
        }
        // Get the index order list by the use case priority for finding stream configuration
        val useCasesPriorityOrder: List<Int> = getUseCasesPriorityOrder(
            newUseCaseConfigs
        )
        val supportedOutputSizesList: MutableList<List<Size>> = ArrayList()

        // Collect supported output sizes for all use cases
        for (index in useCasesPriorityOrder) {
            var supportedOutputSizes: List<Size> =
                newUseCaseConfigsSupportedSizeMap[newUseCaseConfigs[index]]!!
            supportedOutputSizes = applyResolutionSelectionOrderRelatedWorkarounds(
                supportedOutputSizes,
                newUseCaseConfigs[index].inputFormat
            )
            supportedOutputSizesList.add(supportedOutputSizes)
        }
        // Get all possible size arrangements
        val allPossibleSizeArrangements: List<List<Size>> = getAllPossibleSizeArrangements(
            supportedOutputSizesList
        )

        var suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec>? = null
        // Transform use cases to SurfaceConfig list and find the first (best) workable combination
        for (possibleSizeList in allPossibleSizeArrangements) {
            // Attach SurfaceConfig of original use cases since it will impact the new use cases
            val surfaceConfigList: MutableList<SurfaceConfig> = ArrayList()
            for (sc in existingSurfaces) {
                surfaceConfigList.add(sc.surfaceConfig)
            }

            // Attach SurfaceConfig of new use cases
            for (i in possibleSizeList.indices) {
                val size = possibleSizeList[i]
                val newUseCase = newUseCaseConfigs[useCasesPriorityOrder[i]]
                surfaceConfigList.add(
                    SurfaceConfig.transformSurfaceConfig(
                        cameraMode,
                        newUseCase.inputFormat,
                        size,
                        getUpdatedSurfaceSizeDefinitionByFormat(newUseCase.inputFormat)
                    )
                )
            }

            // Check whether the SurfaceConfig combination can be supported
            if (checkSupported(cameraMode, surfaceConfigList)) {
                suggestedStreamSpecMap = HashMap()
                for (useCaseConfig in newUseCaseConfigs) {
                    suggestedStreamSpecMap.put(
                        useCaseConfig,
                        StreamSpec.builder(
                            possibleSizeList[useCasesPriorityOrder.indexOf(
                                newUseCaseConfigs.indexOf(useCaseConfig)
                            )]
                        ).build()
                    )
                }
                break
            }
        }
        if (suggestedStreamSpecMap == null) {
            throw java.lang.IllegalArgumentException(
                "No supported surface combination is found for camera device - Id : " +
                    cameraId + " and Hardware level: " + hardwareLevel +
                    ". May be the specified resolution is too large and not supported." +
                    " Existing surfaces: " + existingSurfaces +
                    " New configs: " + newUseCaseConfigs
            )
        }
        return suggestedStreamSpecMap
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
    fun applyResolutionSelectionOrderRelatedWorkarounds(
        sizeList: List<Size>,
        imageFormat: Int
    ): List<Size> {
        // Applies TargetAspectRatio workaround
        var ratio: Rational? =
            when (targetAspectRatio[cameraMetadata, streamConfigurationMapCompat]) {
                TargetAspectRatio.RATIO_4_3 ->
                    AspectRatioUtil.ASPECT_RATIO_4_3

                TargetAspectRatio.RATIO_16_9 ->
                    AspectRatioUtil.ASPECT_RATIO_16_9

                TargetAspectRatio.RATIO_MAX_JPEG -> {
                    val maxJpegSize = getUpdatedSurfaceSizeDefinitionByFormat(
                        ImageFormat.JPEG
                    ).getMaximumSize(ImageFormat.JPEG)
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

    /**
     * Refresh Preview Size based on current display configurations.
     */
    private fun refreshPreviewSize() {
        displayInfoManager.refresh()
        if (!::surfaceSizeDefinition.isInitialized) {
            generateSurfaceSizeDefinition()
        } else {
            val previewSize: Size = displayInfoManager.getPreviewSize()
            surfaceSizeDefinition = SurfaceSizeDefinition.create(
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

    /**
     * Check the device's available capabilities.
     */
    private fun checkCapabilities() {
        val availableCapabilities: IntArray? =
            cameraMetadata.get<IntArray>(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        availableCapabilities?.apply {
            isRawSupported = contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
            isBurstCaptureSupported =
                contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
            isUltraHighResolutionSensorSupported = contains(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
            )
        }
    }

    /**
     * Generate the supported combination list from guaranteed configurations tables.
     */
    private fun generateSupportedCombinationList() {
        surfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.generateSupportedCombinationList(
                hardwareLevel,
                isRawSupported, isBurstCaptureSupported
            )
        )
        surfaceCombinations.addAll(
            extraSupportedSurfaceCombinationsContainer[cameraId, hardwareLevel]
        )
    }

    private fun generateUltraHighResolutionSupportedCombinationList() {
        ultraHighSurfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList()
        )
    }

    private fun generateConcurrentSupportedCombinationList() {
        concurrentSurfaceCombinations.addAll(
            GuaranteedConfigurationsUtil.generateConcurrentSupportedCombinationList()
        )
    }

    /**
     * Generation the size definition for VGA, s720p, PREVIEW, s1440p, RECORD, MAXIMUM and
     * ULTRA_MAXIMUM.
     */
    private fun generateSurfaceSizeDefinition() {
        val previewSize: Size = displayInfoManager.getPreviewSize()
        val recordSize: Size = getRecordSize()
        surfaceSizeDefinition = SurfaceSizeDefinition.create(
            RESOLUTION_VGA,
            mutableMapOf(), // s720pSizeMap
            previewSize,
            mutableMapOf(), // s1440pSizeMap
            recordSize,
            mutableMapOf(), // maximumSizeMap
            mutableMapOf() // ultraMaximumSizeMap
        )
    }

    /**
     * Updates the surface size definition for the specified format then return it.
     */
    @VisibleForTesting
    fun getUpdatedSurfaceSizeDefinitionByFormat(format: Int): SurfaceSizeDefinition {
        if (!surfaceSizeDefinitionFormats.contains(format)) {
            updateS720pOrS1440pSizeByFormat(
                surfaceSizeDefinition.s720pSizeMap,
                RESOLUTION_720P, format
            )
            updateS720pOrS1440pSizeByFormat(
                surfaceSizeDefinition.s1440pSizeMap,
                RESOLUTION_1440P, format
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
        sizeMap[format] = if (maxOutputSize == null) {
            targetSize
        } else {
            Collections.min(
                listOf(
                    targetSize,
                    maxOutputSize
                ), CompareSizesByArea()
            )
        }
    }

    /**
     * Updates the maximum size to the map for the specified format.
     */
    private fun updateMaximumSizeByFormat(
        sizeMap: MutableMap<Int, Size>,
        format: Int
    ) {
        val originalMap = streamConfigurationMapCompat.toStreamConfigurationMap()
        getMaxOutputSizeByFormat(originalMap, format, true)?.let {
            sizeMap[format] = it
        }
    }

    /**
     * Updates the ultra maximum size to the map for the specified format.
     */
    private fun updateUltraMaximumSizeByFormat(
        sizeMap: MutableMap<Int, Size>,
        format: Int
    ) {
        // Maximum resolution mode is supported since API level 31
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !isUltraHighResolutionSensorSupported
        ) {
            return
        }
        val maximumResolutionMap =
            cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION]
                ?: return
        getMaxOutputSizeByFormat(maximumResolutionMap, format, true)?.let {
            sizeMap[format] = it
        }
    }

    /**
     * RECORD refers to the camera device's maximum supported recording resolution, as determined by
     * CamcorderProfile.
     */
    private fun getRecordSize(): Size {
        val cameraId: Int = try {
            this.cameraId.toInt()
        } catch (e: NumberFormatException) {
            // The camera Id is not an integer because the camera may be a removable device. Use
            // StreamConfigurationMap to determine the RECORD size.
            return getRecordSizeFromStreamConfigurationMapCompat()
        }
        var profiles: EncoderProfilesProxy? = null
        if (encoderProfilesProviderAdapter.hasProfile(cameraId)) {
            profiles = encoderProfilesProviderAdapter.getAll(cameraId)
        }
        return if (profiles != null && profiles.videoProfiles.isNotEmpty()) {
            Size(profiles.videoProfiles[0].width, profiles.videoProfiles[0].height)
        } else getRecordSizeByHasProfile()
    }

    /**
     * Obtains the stream configuration map from camera meta data.
     */
    private fun getStreamConfigurationMapCompat(): StreamConfigurationMapCompat {
        val map = cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            ?: throw IllegalArgumentException("Cannot retrieve SCALER_STREAM_CONFIGURATION_MAP")
        return StreamConfigurationMapCompat(map, OutputSizesCorrector(cameraMetadata, map))
    }

    /**
     * Return the maximum supported video size for cameras using data from the stream
     * configuration map.
     *
     * @return Maximum supported video size.
     */
    private fun getRecordSizeFromStreamConfigurationMapCompat(): Size {
        val map = streamConfigurationMapCompat.toStreamConfigurationMap()
        val videoSizeArr = map?.getOutputSizes(
            MediaRecorder::class.java
        ) ?: return RESOLUTION_480P
        Arrays.sort(videoSizeArr, CompareSizesByArea(true))
        for (size in videoSizeArr) {
            // Returns the largest supported size under 1080P
            if (size.width <= RESOLUTION_1080P.width &&
                size.height <= RESOLUTION_1080P.height
            ) {
                return size
            }
        }
        return RESOLUTION_480P
    }

    /**
     * Return the maximum supported video size for cameras by
     * [CamcorderProfile.hasProfile].
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
     * Retrieves the display which has the max size among all displays.
     */
    private fun getMaxSizeDisplay(): Display {
        val displays: Array<Display> = displayManager.displays
        if (displays.size == 1) {
            return displays[0]
        }
        var maxDisplay: Display? = null
        var maxDisplaySize = -1
        for (display: Display in displays) {
            if (display.state != Display.STATE_OFF) {
                val displaySize = Point()
                display.getRealSize(displaySize)
                if (displaySize.x * displaySize.y > maxDisplaySize) {
                    maxDisplaySize = displaySize.x * displaySize.y
                    maxDisplay = display
                }
            }
        }
        if (maxDisplay == null) {
            throw IllegalArgumentException(
                "No display can be found from the input display manager!"
            )
        }
        return maxDisplay
    }

    /**
     * Once the stream resource is occupied by one use case, it will impact the other use cases.
     * Therefore, we need to define the priority for stream resource usage. For the use cases
     * with the higher priority, we will try to find the best one for them in priority as
     * possible.
     */
    private fun getUseCasesPriorityOrder(newUseCaseConfigs: List<UseCaseConfig<*>>): List<Int> {
        val priorityOrder: MutableList<Int> = ArrayList()
        val priorityValueList: MutableList<Int> = ArrayList()
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
            if (highResolutionOutputSizes != null && highResolutionOutputSizes.isNotEmpty()) {
                maxHighResolutionSize =
                    Collections.max(highResolutionOutputSizes.asList(), compareSizesByArea)
            }
        }

        return Collections.max(Arrays.asList(maxSize, maxHighResolutionSize), compareSizesByArea)
    }

    /**
     * Given all supported output sizes, lists out all possible size arrangements.
     */
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
        val allPossibleSizeArrangements: MutableList<MutableList<Size>> = ArrayList()

        // Initialize allPossibleSizeArrangements for the following operations
        for (i in 0 until totalArrangementsCount) {
            val sizeList: MutableList<Size> = ArrayList()
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
                surfaceConfigList.add(
                    supportedOutputSizes[i % currentRunCount / nextRunCount]
                )
            }
            if (currentIndex < supportedOutputSizesList.size - 1) {
                currentRunCount = nextRunCount
                nextRunCount = currentRunCount / supportedOutputSizesList[currentIndex + 1].size
            }
        }
        return allPossibleSizeArrangements
    }

    companion object {
        private const val TAG = "SupportedSurfaceCombination"
    }
}