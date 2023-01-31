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
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.AspectRatio
import androidx.camera.core.Logger
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CamcorderProfileProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceSizeDefinition
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace
import androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.core.util.Preconditions
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
    private val camcorderProfileProviderAdapter: CamcorderProfileProviderAdapter
) {
    private val cameraId = cameraMetadata.camera.value
    private val hardwareLevel =
        cameraMetadata[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    private val isSensorLandscapeResolution = isSensorLandscapeResolution(cameraMetadata)
    private val surfaceCombinations: MutableList<SurfaceCombination> = ArrayList()
    private val outputSizesCache: MutableMap<Int, Array<Size>> = HashMap()
    private var isRawSupported = false
    private var isBurstCaptureSupported = false
    internal lateinit var surfaceSizeDefinition: SurfaceSizeDefinition
    private val displayManager: DisplayManager =
        (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
    private val activeArraySize =
        cameraMetadata[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]

    init {
        checkCapabilities()
        generateSupportedCombinationList()
        generateSurfaceSizeDefinition()
    }

    /**
     * Check whether the input surface configuration list is under the capability of any combination
     * of this object.
     *
     * @param surfaceConfigList the surface configuration list to be compared
     * @return the check result that whether it could be supported
     */
    fun checkSupported(surfaceConfigList: List<SurfaceConfig>): Boolean {
        for (surfaceCombination in surfaceCombinations) {
            if (surfaceCombination.isSupported(surfaceConfigList)) {
                return true
            }
        }
        return false
    }

    /**
     * Transform to a SurfaceConfig object with image format and size info
     *
     * @param imageFormat the image format info for the surface configuration object
     * @param size        the size info for the surface configuration object
     * @return new [SurfaceConfig] object
     */
    fun transformSurfaceConfig(imageFormat: Int, size: Size): SurfaceConfig {
        return SurfaceConfig.transformSurfaceConfig(imageFormat, size, surfaceSizeDefinition)
    }

    /**
     * Finds the suggested stream specification of the newly added UseCaseConfig.
     *
     * @param existingSurfaces  the existing surfaces.
     * @param newUseCaseConfigs newly added UseCaseConfig.
     * @return the suggested stream specs, which is a mapping from UseCaseConfig to the suggested
     * stream specification.
     * @throws IllegalArgumentException if the suggested solution for newUseCaseConfigs cannot be
     * found. This may be due to no available output size or no available surface combination.
     */
    fun getSuggestedStreamSpecifications(
        existingSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>
    ): Map<UseCaseConfig<*>, StreamSpec> {
        refreshPreviewSize()
        val surfaceConfigs: MutableList<SurfaceConfig> = ArrayList()
        for (scc in existingSurfaces) {
            surfaceConfigs.add(scc.surfaceConfig)
        }
        // Use the small size (640x480) for new use cases to check whether there is any possible
        // supported combination first
        for (useCaseConfig in newUseCaseConfigs) {
            surfaceConfigs.add(
                SurfaceConfig.transformSurfaceConfig(
                    useCaseConfig.inputFormat,
                    RESOLUTION_VGA,
                    surfaceSizeDefinition
                )
            )
        }

        if (!checkSupported(surfaceConfigs)) {
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
            val supportedOutputSizes: List<Size> = getSupportedOutputSizes(
                newUseCaseConfigs[index]
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
                        newUseCase.inputFormat,
                        size,
                        surfaceSizeDefinition
                    )
                )
            }

            // Check whether the SurfaceConfig combination can be supported
            if (checkSupported(surfaceConfigList)) {
                suggestedStreamSpecMap = HashMap()
                for (useCaseConfig in newUseCaseConfigs) {
                    suggestedStreamSpecMap.put(
                        useCaseConfig,
                        StreamSpec.builder(possibleSizeList[useCasesPriorityOrder.indexOf(
                            newUseCaseConfigs.indexOf(useCaseConfig)
                        )]).build()
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

    // Utility classes and methods:
    // *********************************************************************************************

    /**
     * Refresh Preview Size based on current display configurations.
     */
    private fun refreshPreviewSize() {
        val previewSize: Size = calculatePreviewSize()
        surfaceSizeDefinition = SurfaceSizeDefinition.create(
            surfaceSizeDefinition.analysisSize,
            previewSize,
            surfaceSizeDefinition.recordSize
        )
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
        // TODO(b/246609101): ExtraSupportedSurfaceCombinationsQuirk is supposed to be here to add additional
        //  surface combinations to the list
    }

    /**
     * Generation the size definition for VGA, PREVIEW, and RECORD.
     */
    private fun generateSurfaceSizeDefinition() {
        val vgaSize = Size(640, 480)
        val previewSize: Size = calculatePreviewSize()
        val recordSize: Size = getRecordSize()
        surfaceSizeDefinition = SurfaceSizeDefinition.create(vgaSize, previewSize, recordSize)
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
            return getRecordSizeFromStreamConfigurationMap()
        }
        var profile: CamcorderProfileProxy? = null
        if (camcorderProfileProviderAdapter.hasProfile(cameraId)) {
            profile = camcorderProfileProviderAdapter.get(cameraId)
        }
        return if (profile != null) {
            Size(profile.videoFrameWidth, profile.videoFrameHeight)
        } else getRecordSizeByHasProfile()
    }

    /**
     * Obtains the stream configuration map from camera meta data.
     */
    private fun getStreamConfigurationMap(): StreamConfigurationMap {
        return cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            ?: throw IllegalArgumentException("Cannot retrieve SCALER_STREAM_CONFIGURATION_MAP")
    }

    /**
     * Return the maximum supported video size for cameras using data from the stream
     * configuration map.
     *
     * @return Maximum supported video size.
     */
    private fun getRecordSizeFromStreamConfigurationMap(): Size {
        val map: StreamConfigurationMap = getStreamConfigurationMap()
        val videoSizeArr = map.getOutputSizes(
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
        var profile: CamcorderProfileProxy? = null

        // Check whether 4KDCI, 2160P, 2K, 1080P, 720P, 480P (sorted by size) are supported by
        // CamcorderProfile
        if (camcorderProfileProviderAdapter.hasProfile(CamcorderProfile.QUALITY_4KDCI)) {
            profile = camcorderProfileProviderAdapter.get(CamcorderProfile.QUALITY_4KDCI)
        } else if (camcorderProfileProviderAdapter.hasProfile(CamcorderProfile.QUALITY_2160P)) {
            profile = camcorderProfileProviderAdapter.get(CamcorderProfile.QUALITY_2160P)
        } else if (camcorderProfileProviderAdapter.hasProfile(CamcorderProfile.QUALITY_2K)) {
            profile = camcorderProfileProviderAdapter.get(CamcorderProfile.QUALITY_2K)
        } else if (camcorderProfileProviderAdapter.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            profile = camcorderProfileProviderAdapter.get(CamcorderProfile.QUALITY_1080P)
        } else if (camcorderProfileProviderAdapter.hasProfile(CamcorderProfile.QUALITY_720P)) {
            profile = camcorderProfileProviderAdapter.get(CamcorderProfile.QUALITY_720P)
        } else if (camcorderProfileProviderAdapter.hasProfile(CamcorderProfile.QUALITY_480P)) {
            profile = camcorderProfileProviderAdapter.get(CamcorderProfile.QUALITY_480P)
        }
        if (profile != null) {
            recordSize = Size(profile.videoFrameWidth, profile.videoFrameHeight)
        }
        return recordSize
    }

    /**
     * Check if the size obtained from sensor info indicates landscape mode.
     */
    private fun isSensorLandscapeResolution(cameraMetadata: CameraMetadata): Boolean {
        val pixelArraySize: Size? =
            cameraMetadata.get<Size>(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

        // Make the default value is true since usually the sensor resolution is landscape.
        return if (pixelArraySize != null) pixelArraySize.width >= pixelArraySize.height else true
    }

    /**
     * Calculates the size for preview. If the max size is larger than 1080p, use 1080p.
     */
    @SuppressWarnings("deprecation")
    /* getRealSize */
    private fun calculatePreviewSize(): Size {
        val displaySize = Point()
        val display: Display = getMaxSizeDisplay()
        display.getRealSize(displaySize)
        var displayViewSize: Size
        displayViewSize = if (displaySize.x > displaySize.y) {
            Size(displaySize.x, displaySize.y)
        } else {
            Size(displaySize.y, displaySize.x)
        }
        if (displayViewSize.width * displayViewSize.height
            > RESOLUTION_1080P.width * RESOLUTION_1080P.height
        ) {
            displayViewSize = RESOLUTION_1080P
        }
        // TODO(b/245619094): Use ExtraCroppingQuirk to potentially override this with select
        //  resolution
        return displayViewSize
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
     * @param imageFormat the image format info
     * @return the max supported output size for the image format
     */
    internal fun getMaxOutputSizeByFormat(imageFormat: Int): Size {
        val outputSizes = getAllOutputSizesByFormat(imageFormat)
        return Collections.max(listOf(*outputSizes), CompareSizesByArea())
    }

    /**
     * Get all output sizes for a given image format.
     */
    private fun doGetAllOutputSizesByFormat(imageFormat: Int): Array<Size> {
        val map: StreamConfigurationMap = getStreamConfigurationMap()
        val outputSizes = if (Build.VERSION.SDK_INT < 23 &&
            imageFormat == ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
        ) {
            // This is a little tricky that 0x22 that is internal defined in
            // StreamConfigurationMap.java to be equal to ImageFormat.PRIVATE that is public
            // after Android level 23 but not public in Android L. Use {@link SurfaceTexture}
            // or {@link MediaCodec} will finally mapped to 0x22 in StreamConfigurationMap to
            // retrieve the output sizes information.
            map.getOutputSizes(SurfaceTexture::class.java)
        } else {
            map.getOutputSizes(imageFormat)
        }
        // TODO(b/244477758): Exclude problematic sizes

        // Sort the output sizes. The Comparator result must be reversed to have a descending order
        // result.
        Arrays.sort(outputSizes, CompareSizesByArea(true))
        return outputSizes
    }

    /**
     * Retrieves the output size associated with the given format.
     */
    private fun getAllOutputSizesByFormat(imageFormat: Int): Array<Size> {
        var outputs: Array<Size>? = outputSizesCache[imageFormat]
        if (outputs == null) {
            outputs = doGetAllOutputSizesByFormat(imageFormat)
            outputSizesCache[imageFormat] = outputs
        }
        return outputs
    }

    /**
     * Retrieves the sorted customized supported resolutions from the given config
     */
    private fun getCustomizedSupportSizesFromConfig(
        imageFormat: Int,
        config: ImageOutputConfig
    ): Array<Size>? {
        var outputSizes: Array<Size>? = null

        // Try to retrieve customized supported resolutions from config.
        val formatResolutionsPairList = config.getSupportedResolutions(null)
        if (formatResolutionsPairList != null) {
            for (formatResolutionPair in formatResolutionsPairList) {
                if (formatResolutionPair.first == imageFormat) {
                    outputSizes = formatResolutionPair.second
                    break
                }
            }
        }
        if (outputSizes != null) {
            // TODO(b/244477758): Exclude problematic sizes

            // Sort the output sizes. The Comparator result must be reversed to have a descending
            // order result.
            Arrays.sort(outputSizes, CompareSizesByArea(true))
        }
        return outputSizes
    }

    /**
     * Flips the size if rotation is needed.
     */
    private fun flipSizeByRotation(size: Size?, targetRotation: Int): Size? {
        var outputSize = size
        // Calibrates the size with the display and sensor rotation degrees values.
        if (size != null && isRotationNeeded(targetRotation)) {
            outputSize = Size(/* width= */size.height, /* height= */size.width)
        }
        return outputSize
    }

    /**
     * Determines whether rotation needs to be done on target rotation.
     */
    private fun isRotationNeeded(targetRotation: Int): Boolean {
        val sensorOrientation: Int? =
            cameraMetadata[CameraCharacteristics.SENSOR_ORIENTATION]
        Preconditions.checkNotNull(
            sensorOrientation, "Camera HAL in bad state, unable to " +
                "retrieve the SENSOR_ORIENTATION"
        )
        val relativeRotationDegrees = CameraOrientationUtil.surfaceRotationToDegrees(targetRotation)

        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        val lensFacing: Int? = cameraMetadata[CameraCharacteristics.LENS_FACING]
        Preconditions.checkNotNull(
            lensFacing, "Camera HAL in bad state, unable to retrieve the " +
                "LENS_FACING"
        )
        val isOppositeFacingScreen = CameraCharacteristics.LENS_FACING_BACK == lensFacing
        val sensorRotationDegrees = CameraOrientationUtil.getRelativeImageRotation(
            relativeRotationDegrees,
            sensorOrientation!!,
            isOppositeFacingScreen
        )
        return sensorRotationDegrees == 90 || sensorRotationDegrees == 270
    }

    /**
     * Obtains the target size from ImageOutputConfig.
     */
    private fun getTargetSize(imageOutputConfig: ImageOutputConfig): Size? {
        val targetRotation = imageOutputConfig.getTargetRotation(Surface.ROTATION_0)
        // Calibrate targetSize by the target rotation value.
        var targetSize = imageOutputConfig.getTargetResolution(null)
        targetSize = flipSizeByRotation(targetSize, targetRotation)
        return targetSize
    }

    /**
     * Returns the aspect ratio group key of the target size when grouping the input resolution
     * candidate list.
     *
     * The resolution candidate list will be grouped with mod 16 consideration. Therefore, we
     * also need to consider the mod 16 factor to find which aspect ratio of group the target size
     * might be put in. So that sizes of the group will be selected to use in the highest priority.
     */
    private fun getAspectRatioGroupKeyOfTargetSize(
        targetSize: Size?,
        resolutionCandidateList: List<Size>
    ): Rational? {
        if (targetSize == null) {
            return null
        }

        val aspectRatios = getResolutionListGroupingAspectRatioKeys(
            resolutionCandidateList
        )
        aspectRatios.forEach {
            if (hasMatchingAspectRatio(targetSize, it)) {
                return it
            }
        }
        return Rational(targetSize.width, targetSize.height)
    }

    /**
     * Returns the grouping aspect ratio keys of the input resolution list.
     *
     * Some sizes might be mod16 case. When grouping, those sizes will be grouped into an
     * existing aspect ratio group if the aspect ratio can match by the mod16 rule.
     */
    private fun getResolutionListGroupingAspectRatioKeys(
        resolutionCandidateList: List<Size>
    ): List<Rational> {
        val aspectRatios: MutableList<Rational> = mutableListOf()

        // Adds the default 4:3 and 16:9 items first to avoid their mod16 sizes to create
        // additional items.
        aspectRatios.add(AspectRatioUtil.ASPECT_RATIO_4_3)
        aspectRatios.add(AspectRatioUtil.ASPECT_RATIO_16_9)

        // Tries to find the aspect ratio which the target size belongs to.
        resolutionCandidateList.forEach { size ->
            val newRatio = Rational(size.width, size.height)
            var aspectRatioFound = aspectRatios.contains(newRatio)

            // The checking size might be a mod16 size which can be mapped to an existing aspect
            // ratio group.
            if (!aspectRatioFound) {
                var hasMatchingAspectRatio = false
                aspectRatios.forEach loop@{ aspectRatio ->
                    if (hasMatchingAspectRatio(size, aspectRatio)) {
                        hasMatchingAspectRatio = true
                        return@loop
                    }
                }
                if (!hasMatchingAspectRatio) {
                    aspectRatios.add(newRatio)
                }
            }
        }
        return aspectRatios
    }

    /**
     * Returns the target aspect ratio value corrected by quirks.
     *
     * The final aspect ratio is determined by the following order:
     * 1. The aspect ratio returned by TargetAspectRatio quirk (not implemented yet).
     * 2. The use case's original aspect ratio if TargetAspectRatio quirk returns RATIO_ORIGINAL
     * and the use case has target aspect ratio setting.
     * 3. The aspect ratio of use case's target size setting if TargetAspectRatio quirk returns
     * RATIO_ORIGINAL and the use case has no target aspect ratio but has target size setting.
     *
     * @param imageOutputConfig       the image output config of the use case.
     * @param resolutionCandidateList the resolution candidate list which will be used to
     *                                determine the aspect ratio by target size when target
     *                                aspect ratio setting is not set.
     */
    private fun getTargetAspectRatio(
        imageOutputConfig: ImageOutputConfig,
        resolutionCandidateList: List<Size>
    ): Rational? {
        var outputRatio: Rational? = null
        // TODO(b/245622117) Get the corrected aspect ratio from quirks instead of always using
        //  TargetAspectRatio.RATIO_ORIGINAL
        if (imageOutputConfig.hasTargetAspectRatio()) {
            when (@AspectRatio.Ratio val aspectRatio = imageOutputConfig.targetAspectRatio) {
                AspectRatio.RATIO_4_3 -> outputRatio =
                    if (isSensorLandscapeResolution) AspectRatioUtil.ASPECT_RATIO_4_3
                    else AspectRatioUtil.ASPECT_RATIO_3_4
                AspectRatio.RATIO_16_9 -> outputRatio =
                    if (isSensorLandscapeResolution) AspectRatioUtil.ASPECT_RATIO_16_9
                    else AspectRatioUtil.ASPECT_RATIO_9_16
                AspectRatio.RATIO_DEFAULT -> Unit
                else -> Logger.e(
                    TAG,
                    "Undefined target aspect ratio: $aspectRatio"
                )
            }
        } else {
            // The legacy resolution API will use the aspect ratio of the target size to
            // be the fallback target aspect ratio value when the use case has no target
            // aspect ratio setting.
            val targetSize = getTargetSize(imageOutputConfig)
            if (targetSize != null) {
                outputRatio = getAspectRatioGroupKeyOfTargetSize(
                    targetSize,
                    resolutionCandidateList
                )
            }
        }
        return outputRatio
    }

    /**
     * Removes unnecessary sizes by target size.
     *
     *
     * If the target resolution is set, a size that is equal to or closest to the target
     * resolution will be selected. If the list includes more than one size equal to or larger
     * than the target resolution, only one closest size needs to be kept. The other larger sizes
     * can be removed so that they won't be selected to use.
     *
     * @param supportedSizesList The list should have been sorted in descending order.
     * @param targetSize         The target size used to remove unnecessary sizes.
     */
    private fun removeSupportedSizesByTargetSize(
        supportedSizesList: MutableList<Size>?,
        targetSize: Size
    ) {
        if (supportedSizesList == null || supportedSizesList.isEmpty()) {
            return
        }
        var indexBigEnough = -1
        val removeSizes: MutableList<Size> = ArrayList()

        // Get the index of the item that is equal to or closest to the target size.
        for (i in supportedSizesList.indices) {
            val outputSize = supportedSizesList[i]
            if (outputSize.width >= targetSize.width && outputSize.height >= targetSize.height) {
                // New big enough item closer to the target size is found. Adding the previous
                // one into the sizes list that will be removed.
                if (indexBigEnough >= 0) {
                    removeSizes.add(supportedSizesList[indexBigEnough])
                }
                indexBigEnough = i
            } else {
                break
            }
        }
        // Remove the unnecessary items that are larger than the item closest to the target size.
        supportedSizesList.removeAll(removeSizes)
    }

    /**
     * Groups sizes together according to their aspect ratios.
     */
    private fun groupSizesByAspectRatio(sizes: List<Size>): Map<Rational, MutableList<Size>> {
        val aspectRatioSizeListMap: MutableMap<Rational, MutableList<Size>> = mutableMapOf()

        val aspectRatioKeys = getResolutionListGroupingAspectRatioKeys(sizes)

        aspectRatioKeys.forEach {
            aspectRatioSizeListMap[it] = mutableListOf()
        }

        sizes.forEach { size ->
            aspectRatioSizeListMap.keys.forEach { aspectRatio ->
                // Put the size into all groups that is matched in mod16 condition since a size
                // may match multiple aspect ratio in mod16 algorithm.
                if (hasMatchingAspectRatio(size, aspectRatio)) {
                    aspectRatioSizeListMap[aspectRatio]?.add(size)
                }
            }
        }
        return aspectRatioSizeListMap
    }

    /**
     * Obtains the supported sizes for a given user case.
     */
    internal fun getSupportedOutputSizes(config: UseCaseConfig<*>): List<Size> {
        val imageFormat = config.inputFormat
        val imageOutputConfig = config as ImageOutputConfig
        val customOrderedResolutions = imageOutputConfig.getCustomOrderedResolutions(null)
        if (customOrderedResolutions != null) {
            return customOrderedResolutions
        }
        var outputSizes: Array<Size>? =
            getCustomizedSupportSizesFromConfig(imageFormat, imageOutputConfig)
        if (outputSizes == null) {
            outputSizes = getAllOutputSizesByFormat(imageFormat)
        }
        val outputSizeCandidates: MutableList<Size> = ArrayList()
        var maxSize = imageOutputConfig.getMaxResolution(null)
        val maxOutputSizeByFormat: Size = getMaxOutputSizeByFormat(imageFormat)

        // Set maxSize as the max resolution setting or the max supported output size for the
        // image format, whichever is smaller.
        if (maxSize == null ||
            SizeUtil.getArea(maxOutputSizeByFormat) < SizeUtil.getArea(maxSize)
        ) {
            maxSize = maxOutputSizeByFormat
        }

        // Sort the output sizes. The Comparator result must be reversed to have a descending order
        // result.
        Arrays.sort(outputSizes, CompareSizesByArea(true))
        var targetSize: Size? = getTargetSize(imageOutputConfig)
        var minSize = RESOLUTION_VGA
        val defaultSizeArea = SizeUtil.getArea(RESOLUTION_VGA)
        val maxSizeArea = SizeUtil.getArea(maxSize)
        // When maxSize is smaller than 640x480, set minSize as 0x0. It means the min size bound
        // will be ignored. Otherwise, set the minimal size according to min(DEFAULT_SIZE,
        // TARGET_RESOLUTION).
        if (maxSizeArea < defaultSizeArea) {
            minSize = SizeUtil.RESOLUTION_ZERO
        } else if (targetSize != null && SizeUtil.getArea(targetSize) < defaultSizeArea) {
            minSize = targetSize
        }

        // Filter out the ones that exceed the maximum size and the minimum size. The output
        // sizes candidates list won't have duplicated items.
        for (outputSize: Size in outputSizes) {
            if (SizeUtil.getArea(outputSize) <= SizeUtil.getArea(maxSize) &&
                SizeUtil.getArea(outputSize) >= SizeUtil.getArea(minSize!!) &&
                !outputSizeCandidates.contains(outputSize)
            ) {
                outputSizeCandidates.add(outputSize)
            }
        }
        if (outputSizeCandidates.isEmpty()) {
            throw java.lang.IllegalArgumentException(
                "Can not get supported output size under supported maximum for the format: " +
                    imageFormat
            )
        }

        val aspectRatio: Rational? = getTargetAspectRatio(imageOutputConfig, outputSizeCandidates)

        // Check the default resolution if the target resolution is not set
        targetSize = targetSize ?: imageOutputConfig.getDefaultResolution(null)
        var supportedResolutions: MutableList<Size> = ArrayList()
        var aspectRatioSizeListMap: Map<Rational, MutableList<Size>>
        if (aspectRatio == null) {
            // If no target aspect ratio is set, all sizes can be added to the result list
            // directly. No need to sort again since the source list has been sorted previously.
            supportedResolutions.addAll(outputSizeCandidates)

            // If the target resolution is set, use it to remove unnecessary larger sizes.
            targetSize?.let { removeSupportedSizesByTargetSize(supportedResolutions, it) }
        } else {
            // Rearrange the supported size to put the ones with the same aspect ratio in the front
            // of the list and put others in the end from large to small. Some low end devices may
            // not able to get an supported resolution that match the preferred aspect ratio.

            // Group output sizes by aspect ratio.
            aspectRatioSizeListMap = groupSizesByAspectRatio(outputSizeCandidates)

            // If the target resolution is set, use it to remove unnecessary larger sizes.
            if (targetSize != null) {
                // Remove unnecessary larger sizes from each aspect ratio size list
                for (key: Rational? in aspectRatioSizeListMap.keys) {
                    removeSupportedSizesByTargetSize(aspectRatioSizeListMap[key], targetSize)
                }
            }

            // Sort the aspect ratio key set by the target aspect ratio.
            val aspectRatios: List<Rational?> = ArrayList(aspectRatioSizeListMap.keys)
            val fullFovRatio = if (activeArraySize != null) {
                Rational(activeArraySize.width(), activeArraySize.height())
            } else {
                null
            }
            Collections.sort(
                aspectRatios,
                CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                    aspectRatio,
                    fullFovRatio
                )
            )

            // Put available sizes into final result list by aspect ratio distance to target ratio.
            for (rational: Rational? in aspectRatios) {
                for (size: Size in aspectRatioSizeListMap[rational]!!) {
                    // A size may exist in multiple groups in mod16 condition. Keep only one in
                    // the final list.
                    if (!supportedResolutions.contains(size)) {
                        supportedResolutions.add(size)
                    }
                }
            }
        }

        // TODO(b/245619094): Use ExtraCroppingQuirk to insert selected resolutions

        return supportedResolutions
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