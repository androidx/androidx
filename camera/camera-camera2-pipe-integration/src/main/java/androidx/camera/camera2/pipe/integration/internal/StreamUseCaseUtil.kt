/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.internal

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_CASE_OPTION
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.streamsharing.StreamSharingConfig
import androidx.core.util.Preconditions.checkState

public object StreamUseCaseUtil {

    @VisibleForTesting
    public val STREAM_USE_CASE_STREAM_SPEC_OPTION: Config.Option<Long> =
        Config.Option.create("camera2.streamSpec.streamUseCase", Long::class.javaPrimitiveType!!)
    private val STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP: Map<Long, Set<CaptureType>> =
        buildMap {
            if (Build.VERSION.SDK_INT >= 33) {
                put(
                    SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong(),
                    setOf(CaptureType.PREVIEW)
                )
                put(
                    SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                    setOf(CaptureType.PREVIEW, CaptureType.IMAGE_ANALYSIS)
                )
                put(
                    SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong(),
                    setOf(CaptureType.IMAGE_CAPTURE)
                )
                put(
                    SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong(),
                    setOf(CaptureType.VIDEO_CAPTURE)
                )
            }
        }

    private val STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP:
        Map<Long, Set<CaptureType>> =
        buildMap {
            if (Build.VERSION.SDK_INT >= 33) {
                put(
                    SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong(),
                    setOf(CaptureType.PREVIEW, CaptureType.IMAGE_CAPTURE, CaptureType.VIDEO_CAPTURE)
                )
                put(
                    SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong(),
                    setOf(CaptureType.PREVIEW, CaptureType.VIDEO_CAPTURE)
                )
            }
        }

    /**
     * Populates the mapping between surfaces of a capture session and the Stream Use Case of their
     * associated stream.
     *
     * @param sessionConfigs collection of all session configs for this capture session
     * @param streamUseCaseMap the mapping between surfaces and Stream Use Case flag
     */
    public fun populateSurfaceToStreamUseCaseMapping(
        sessionConfigs: Collection<SessionConfig>,
        useCaseConfigs: Collection<UseCaseConfig<*>>,
        streamUseCaseMap: MutableMap<DeferrableSurface, Long>
    ) {
        var position = 0
        var hasStreamUseCase = false
        val useCaseConfigArrayList = ArrayList(useCaseConfigs)
        for (sessionConfig: SessionConfig in sessionConfigs) {
            if (
                sessionConfig.implementationOptions.containsOption(
                    STREAM_USE_CASE_STREAM_SPEC_OPTION
                ) && sessionConfig.surfaces.size != 1
            ) {
                Log.error {
                    "StreamUseCaseUtil: SessionConfig has stream use case but also contains " +
                        "${sessionConfig.surfaces.size} surfaces, " +
                        "abort populateSurfaceToStreamUseCaseMapping()."
                }
                return
            }
            if (
                sessionConfig.implementationOptions.containsOption(
                    STREAM_USE_CASE_STREAM_SPEC_OPTION
                )
            ) {
                hasStreamUseCase = true
                break
            }
        }
        if (hasStreamUseCase) {
            for (sessionConfig: SessionConfig in sessionConfigs) {
                if (
                    (useCaseConfigArrayList[position].captureType == CaptureType.METERING_REPEATING)
                ) {
                    // MeteringRepeating is attached after the StreamUseCase population logic and
                    // therefore won't have the StreamUseCase option. It should always have
                    // SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW
                    checkState(
                        sessionConfig.surfaces.isNotEmpty(),
                        "MeteringRepeating should contain a surface"
                    )
                    streamUseCaseMap[sessionConfig.surfaces[0]] =
                        SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                } else if (
                    sessionConfig.implementationOptions.containsOption(
                        STREAM_USE_CASE_STREAM_SPEC_OPTION
                    )
                ) {
                    if (sessionConfig.surfaces.isNotEmpty()) {
                        streamUseCaseMap[sessionConfig.surfaces[0]] =
                            sessionConfig.implementationOptions.retrieveOption(
                                STREAM_USE_CASE_STREAM_SPEC_OPTION
                            )!!
                    }
                }
                position++
            }
        }
    }

    /**
     * Populate all implementation options needed to determine the StreamUseCase option in the
     * StreamSpec for this UseCaseConfig
     */
    public fun getStreamSpecImplementationOptions(
        useCaseConfig: UseCaseConfig<*>
    ): Camera2ImplConfig {
        val optionsBundle = MutableOptionsBundle.create()
        if (useCaseConfig.containsOption(STREAM_USE_CASE_OPTION)) {
            optionsBundle.insertOption(
                STREAM_USE_CASE_OPTION,
                useCaseConfig.retrieveOption(STREAM_USE_CASE_OPTION)
            )
        }
        if (useCaseConfig.containsOption(UseCaseConfig.OPTION_ZSL_DISABLED)) {
            optionsBundle.insertOption(
                UseCaseConfig.OPTION_ZSL_DISABLED,
                useCaseConfig.retrieveOption(UseCaseConfig.OPTION_ZSL_DISABLED)
            )
        }
        if (useCaseConfig.containsOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)) {
            optionsBundle.insertOption(
                ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE,
                useCaseConfig.retrieveOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)
            )
        }
        if (useCaseConfig.containsOption(UseCaseConfig.OPTION_INPUT_FORMAT)) {
            optionsBundle.insertOption(
                UseCaseConfig.OPTION_INPUT_FORMAT,
                useCaseConfig.retrieveOption(UseCaseConfig.OPTION_INPUT_FORMAT)
            )
        }
        return Camera2ImplConfig(optionsBundle)
    }

    /** Return true if the given camera characteristics support stream use case */
    public fun isStreamUseCaseSupported(cameraMetadata: CameraMetadata): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return false
        }
        val availableStreamUseCases: LongArray? =
            cameraMetadata[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES]
        return !(availableStreamUseCases == null || availableStreamUseCases.isEmpty())
    }

    /** Return true if the given feature settings is appropriate for stream use case usage. */
    public fun shouldUseStreamUseCase(
        featureSettings: SupportedSurfaceCombination.FeatureSettings
    ): Boolean {
        return (featureSettings.cameraMode == CameraMode.DEFAULT &&
            featureSettings.requiredMaxBitDepth == DynamicRange.BIT_DEPTH_8_BIT)
    }

    /**
     * Populate the [STREAM_USE_CASE_STREAM_SPEC_OPTION] option in StreamSpecs for both existing
     * UseCases and new UseCases to be attached. This option will be written into the session
     * configurations of the UseCases. When creating a new capture session during downstream, it
     * will be used to set the StreamUseCase flag via
     * [android.hardware.camera2.params.OutputConfiguration.setStreamUseCase]
     *
     * @param cameraMetadata the camera characteristics of the device
     * @param attachedSurfaces surface info of the already attached use cases
     * @param suggestedStreamSpecMap the UseCaseConfig-to-StreamSpec map for new use cases
     * @param attachedSurfaceStreamSpecMap the SurfaceInfo-to-StreamSpec map for attached use cases
     *   whose StreamSpecs needs to be updated
     * @return true if StreamSpec options are populated. False if not.
     */
    public fun populateStreamUseCaseStreamSpecOptionWithInteropOverride(
        cameraMetadata: CameraMetadata,
        attachedSurfaces: List<AttachedSurfaceInfo>,
        suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec>,
        attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec>
    ): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return false
        }
        val newUseCaseConfigs: List<UseCaseConfig<*>> =
            java.util.ArrayList(suggestedStreamSpecMap.keys)
        // All AttachedSurfaceInfo should have implementation options
        for (attachedSurfaceInfo in attachedSurfaces) {
            checkNotNull(attachedSurfaceInfo.implementationOptions)
        }
        // All StreamSpecs in the map should have implementation options
        for (useCaseConfig in newUseCaseConfigs) {
            checkNotNull(checkNotNull(suggestedStreamSpecMap[useCaseConfig]).implementationOptions)
        }
        val availableStreamUseCases: LongArray? =
            cameraMetadata[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES]
        if (availableStreamUseCases == null || availableStreamUseCases.isEmpty()) {
            return false
        }
        val availableStreamUseCaseSet: MutableSet<Long> = HashSet()
        for (availableStreamUseCase in availableStreamUseCases) {
            availableStreamUseCaseSet.add(availableStreamUseCase)
        }
        if (
            isValidCamera2InteropOverride(
                attachedSurfaces,
                newUseCaseConfigs,
                availableStreamUseCaseSet
            )
        ) {
            for (attachedSurfaceInfo in attachedSurfaces) {
                val oldImplementationOptions = attachedSurfaceInfo.implementationOptions
                getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                        oldImplementationOptions!!,
                        oldImplementationOptions.retrieveOption(STREAM_USE_CASE_OPTION)
                    )
                    ?.also {
                        attachedSurfaceStreamSpecMap[attachedSurfaceInfo] =
                            attachedSurfaceInfo.toStreamSpec(it)
                    }
            }
            for (newUseCaseConfig in newUseCaseConfigs) {
                val oldStreamSpec = suggestedStreamSpecMap[newUseCaseConfig]
                val oldImplementationOptions = oldStreamSpec!!.implementationOptions
                getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                        oldImplementationOptions!!,
                        oldImplementationOptions.retrieveOption(STREAM_USE_CASE_OPTION)
                    )
                    ?.also {
                        suggestedStreamSpecMap[newUseCaseConfig] =
                            oldStreamSpec.toBuilder().setImplementationOptions(it).build()
                    }
            }
            return true
        }
        return false
    }

    /**
     * Return true if the stream use cases in the given surface configurations are available for the
     * device.
     */
    public fun areStreamUseCasesAvailableForSurfaceConfigs(
        cameraMetadata: CameraMetadata,
        surfaceConfigs: List<SurfaceConfig>
    ): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return false
        }
        val availableStreamUseCases: LongArray? =
            cameraMetadata[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES]
        if (availableStreamUseCases == null || availableStreamUseCases.isEmpty()) {
            return false
        }
        val availableStreamUseCaseSet: MutableSet<Long> = java.util.HashSet()
        for (availableStreamUseCase in availableStreamUseCases) {
            availableStreamUseCaseSet.add(availableStreamUseCase)
        }
        for (surfaceConfig in surfaceConfigs) {
            if (!availableStreamUseCaseSet.contains(surfaceConfig.streamUseCase)) {
                return false
            }
        }
        return true
    }

    /**
     * Return true if the given capture type and stream use case are a eligible pair. If the given
     * captureType is STREAM_SHARING, checks the streamSharingTypes, which are the capture types of
     * the children, are eligible with the stream use case.
     */
    private fun isEligibleCaptureType(
        captureType: CaptureType,
        streamUseCase: Long,
        streamSharingTypes: List<CaptureType>
    ): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return false
        }
        return if (captureType == CaptureType.STREAM_SHARING) {
            if (
                !STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP.containsKey(
                    streamUseCase
                )
            ) {
                return false
            }
            val captureTypes: Set<CaptureType> =
                STREAM_USE_CASE_TO_ELIGIBLE_STREAM_SHARING_CHILDREN_TYPES_MAP[streamUseCase]!!
            if (streamSharingTypes.size != captureTypes.size) {
                return false
            }
            for (childType in streamSharingTypes) {
                if (!captureTypes.contains(childType)) {
                    return false
                }
            }
            true
        } else {
            STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP.containsKey(streamUseCase) &&
                STREAM_USE_CASE_TO_ELIGIBLE_CAPTURE_TYPES_MAP[streamUseCase]!!.contains(captureType)
        }
    }

    /**
     * Return true if the stream use cases contained in surfaceConfigsWithStreamUseCases all have
     * eligible capture type pairing with the use cases that these surfaceConfigs are constructed
     * from.
     *
     * @param surfaceConfigIndexAttachedSurfaceInfoMap mapping between an surfaceConfig's index in
     *   the list and the attachedSurfaceInfo it is constructed from
     * @param surfaceConfigIndexUseCaseConfigMap mapping between an surfaceConfig's index in the
     *   list and the useCaseConfig it is constructed from
     * @param surfaceConfigsWithStreamUseCase the supported surfaceConfigs that contains accurate
     *   streamUseCases
     */
    public fun areCaptureTypesEligible(
        surfaceConfigIndexAttachedSurfaceInfoMap: Map<Int, AttachedSurfaceInfo?>,
        surfaceConfigIndexUseCaseConfigMap: Map<Int, UseCaseConfig<*>>,
        surfaceConfigsWithStreamUseCase: List<SurfaceConfig>
    ): Boolean {
        for (i in surfaceConfigsWithStreamUseCase.indices) {
            // Verify that the use case has the eligible capture type the given stream use case.
            val streamUseCase = surfaceConfigsWithStreamUseCase[i].streamUseCase
            if (surfaceConfigIndexAttachedSurfaceInfoMap.containsKey(i)) {
                val attachedSurfaceInfo = surfaceConfigIndexAttachedSurfaceInfoMap[i]
                if (
                    !isEligibleCaptureType(
                        if (attachedSurfaceInfo!!.captureTypes.size == 1)
                            attachedSurfaceInfo.captureTypes[0]
                        else CaptureType.STREAM_SHARING,
                        streamUseCase,
                        attachedSurfaceInfo.captureTypes
                    )
                ) {
                    return false
                }
            } else if (surfaceConfigIndexUseCaseConfigMap.containsKey(i)) {
                val newUseCaseConfig = surfaceConfigIndexUseCaseConfigMap[i]!!
                if (
                    !isEligibleCaptureType(
                        newUseCaseConfig.captureType,
                        streamUseCase,
                        if (newUseCaseConfig.captureType == CaptureType.STREAM_SHARING)
                            (newUseCaseConfig as StreamSharingConfig).captureTypes
                        else emptyList()
                    )
                ) {
                    return false
                }
            } else {
                throw AssertionError("SurfaceConfig does not map to any use case")
            }
        }
        return true
    }

    /**
     * @param suggestedStreamSpecMap mapping between useCaseConfig and its streamSpecs
     * @param attachedSurfaceStreamSpecMap mapping between attachedSurfaceInfo and its streamSpecs
     *   that contains streamUseCases. All streamSpecs in this map has streamUseCases
     * @param surfaceConfigIndexAttachedSurfaceInfoMap mapping between an surfaceConfig's index in
     *   the list and the attachedSurfaceInfo it is constructed from
     * @param surfaceConfigIndexUseCaseConfigMap mapping between an surfaceConfig's index in the
     *   list and the useCaseConfig it is constructed from
     * @param surfaceConfigsWithStreamUseCase the supported surfaceConfigs that contains accurate
     *   streamUseCases
     */
    public fun populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
        suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec>,
        attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec>,
        surfaceConfigIndexAttachedSurfaceInfoMap: Map<Int, AttachedSurfaceInfo>,
        surfaceConfigIndexUseCaseConfigMap: Map<Int, UseCaseConfig<*>>,
        surfaceConfigsWithStreamUseCase: List<SurfaceConfig>
    ) {
        // Populate StreamSpecs with stream use cases.
        for (i in surfaceConfigsWithStreamUseCase.indices) {
            val streamUseCase = surfaceConfigsWithStreamUseCase[i].streamUseCase
            if (surfaceConfigIndexAttachedSurfaceInfoMap.containsKey(i)) {
                val attachedSurfaceInfo = surfaceConfigIndexAttachedSurfaceInfoMap[i]
                val oldImplementationOptions = attachedSurfaceInfo!!.implementationOptions
                val newImplementationOptions: Config? =
                    getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                        oldImplementationOptions!!,
                        streamUseCase
                    )
                if (newImplementationOptions != null) {
                    attachedSurfaceStreamSpecMap[attachedSurfaceInfo] =
                        attachedSurfaceInfo.toStreamSpec(newImplementationOptions)
                }
            } else if (surfaceConfigIndexUseCaseConfigMap.containsKey(i)) {
                val newUseCaseConfig: UseCaseConfig<*> = surfaceConfigIndexUseCaseConfigMap[i]!!
                val oldStreamSpec = suggestedStreamSpecMap[newUseCaseConfig]
                val oldImplementationOptions = oldStreamSpec!!.implementationOptions
                val newImplementationOptions: Config? =
                    getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
                        oldImplementationOptions!!,
                        streamUseCase
                    )
                if (newImplementationOptions != null) {
                    val newStreamSpec =
                        oldStreamSpec
                            .toBuilder()
                            .setImplementationOptions(newImplementationOptions)
                            .build()
                    suggestedStreamSpecMap[newUseCaseConfig] = newStreamSpec
                }
            } else {
                throw AssertionError("SurfaceConfig does not map to any use case")
            }
        }
    }

    /**
     * Given an old options, return a new option with stream use case stream spec option inserted
     */
    private fun getUpdatedImplementationOptionsWithUseCaseStreamSpecOption(
        oldImplementationOptions: Config,
        streamUseCase: Long?
    ): Config? {
        if (
            oldImplementationOptions.containsOption(STREAM_USE_CASE_STREAM_SPEC_OPTION) &&
                oldImplementationOptions.retrieveOption(STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                    streamUseCase
        ) {
            // The old options already has the same stream use case. No need to update
            return null
        }
        val optionsBundle = MutableOptionsBundle.from(oldImplementationOptions)
        optionsBundle.insertOption(STREAM_USE_CASE_STREAM_SPEC_OPTION, streamUseCase)
        return Camera2ImplConfig(optionsBundle)
    }

    /** Return true if any one of the existing or new UseCases is ZSL. */
    public fun containsZslUseCase(
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>
    ): Boolean {
        for (attachedSurfaceInfo: AttachedSurfaceInfo in attachedSurfaces) {
            val captureTypes = attachedSurfaceInfo.captureTypes
            val captureType = captureTypes[0]
            if (isZslUseCase(attachedSurfaceInfo.implementationOptions!!, captureType)) {
                return true
            }
        }
        for (useCaseConfig: UseCaseConfig<*> in newUseCaseConfigs) {
            if (isZslUseCase(useCaseConfig, useCaseConfig.captureType)) {
                return true
            }
        }
        return false
    }

    /** Check whether a UseCase is ZSL. */
    private fun isZslUseCase(config: Config, captureType: CaptureType): Boolean {
        if (config.retrieveOption(UseCaseConfig.OPTION_ZSL_DISABLED, false)!!) {
            return false
        }
        // Skip if capture mode doesn't exist in the options
        if (!config.containsOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)) {
            return false
        }
        @CaptureMode
        val captureMode: Int = config.retrieveOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)!!
        return (getSessionConfigTemplateType(captureType, captureMode) ==
            CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
    }

    /** Check whether the given StreamUseCases are available to the device. */
    private fun areStreamUseCasesAvailable(
        availableStreamUseCasesSet: Set<Long>,
        streamUseCases: Set<Long>
    ): Boolean {
        for (streamUseCase: Long in streamUseCases) {
            if (!availableStreamUseCasesSet.contains(streamUseCase)) {
                return false
            }
        }
        return true
    }

    private fun throwInvalidCamera2InteropOverrideException() {
        throw IllegalArgumentException(
            "Either all use cases must have non-default stream use " +
                "case assigned or none should have it"
        )
    }

    /**
     * Return true if all existing UseCases and new UseCases have Camera2Interop override and these
     * StreamUseCases are all available to the device.
     */
    private fun isValidCamera2InteropOverride(
        attachedSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        availableStreamUseCases: Set<Long>
    ): Boolean {
        val streamUseCases: MutableSet<Long> = mutableSetOf()
        var hasNonDefaultStreamUseCase = false
        var hasDefaultOrNullStreamUseCase = false
        for (attachedSurfaceInfo: AttachedSurfaceInfo in attachedSurfaces) {
            if (
                !attachedSurfaceInfo.implementationOptions!!.containsOption(STREAM_USE_CASE_OPTION)
            ) {
                hasDefaultOrNullStreamUseCase = true
                break
            }
            val streamUseCaseOverride: Long =
                (attachedSurfaceInfo.implementationOptions!!.retrieveOption(
                    STREAM_USE_CASE_OPTION
                ))!!
            if ((streamUseCaseOverride == SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong())) {
                hasDefaultOrNullStreamUseCase = true
                break
            }
            hasNonDefaultStreamUseCase = true
            break
        }
        for (useCaseConfig: UseCaseConfig<*> in newUseCaseConfigs) {
            if (!useCaseConfig.containsOption(STREAM_USE_CASE_OPTION)) {
                hasDefaultOrNullStreamUseCase = true
                if (hasNonDefaultStreamUseCase) {
                    throwInvalidCamera2InteropOverrideException()
                }
            } else {
                val streamUseCaseOverride: Long =
                    useCaseConfig.retrieveOption(STREAM_USE_CASE_OPTION)!!
                if ((streamUseCaseOverride == SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong())) {
                    hasDefaultOrNullStreamUseCase = true
                    if (hasNonDefaultStreamUseCase) {
                        throwInvalidCamera2InteropOverrideException()
                    }
                } else {
                    hasNonDefaultStreamUseCase = true
                    if (hasDefaultOrNullStreamUseCase) {
                        throwInvalidCamera2InteropOverrideException()
                    }
                    streamUseCases.add(streamUseCaseOverride)
                }
            }
        }
        return !hasDefaultOrNullStreamUseCase &&
            areStreamUseCasesAvailable(availableStreamUseCases, streamUseCases)
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalZeroShutterLag::class])
    private fun getSessionConfigTemplateType(
        captureType: CaptureType,
        @CaptureMode captureMode: Int
    ): Int {
        return when (captureType) {
            CaptureType.IMAGE_CAPTURE ->
                if (captureMode == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                    CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG
                else CameraDevice.TEMPLATE_PREVIEW
            CaptureType.VIDEO_CAPTURE,
            CaptureType.STREAM_SHARING -> CameraDevice.TEMPLATE_RECORD
            CaptureType.PREVIEW,
            CaptureType.IMAGE_ANALYSIS -> CameraDevice.TEMPLATE_PREVIEW
            else -> CameraDevice.TEMPLATE_PREVIEW
        }
    }
}
