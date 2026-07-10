/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.impl

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.media.MediaCodec
import android.os.Build
import android.view.SurfaceHolder
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.ZslControl
import androidx.camera.camera2.compat.DynamicRangeProfilesCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.quirk.CaptureSessionStuckQuirk
import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.camera2.compat.quirk.DisableAbortCapturesOnStopQuirk
import androidx.camera.camera2.compat.quirk.DisableAbortCapturesOnStopWithSessionProcessorQuirk
import androidx.camera.camera2.compat.quirk.FinalizeSessionOnCloseQuirk
import androidx.camera.camera2.compat.quirk.QuickSuccessiveImageCaptureFailsRepeatingRequestQuirk
import androidx.camera.camera2.compat.workaround.CloseCameraOnCameraGraphClose
import androidx.camera.camera2.compat.workaround.TemplateParamsOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.internal.DynamicRangeConversions.dynamicRangeToFirstSupportedProfile
import androidx.camera.camera2.interop.configureWithUnchecked
import androidx.camera.camera2.interop.getCamera2CaptureRequestConfigurator
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode
import androidx.camera.camera2.pipe.CameraGraph.RepeatingRequestRequirementsBeforeCapture.CompletionBehavior.AT_LEAST
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.InputStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.OutputStream.DynamicRangeProfile
import androidx.camera.camera2.pipe.OutputStream.OutputType
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.compat.CameraPipeKeys
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.MirrorMode
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.OutputConfig.SURFACE_GROUP_ID_NONE
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.stabilization.StabilizationMode
import javax.inject.Inject

/**
 * A provider class responsible for creating [CameraGraph.Config] instances.
 *
 * This class encapsulates the complex logic of building a CameraGraph configuration. It is scoped
 * to a specific camera and dependencies are provided via injection, simplifying its usage within
 * UseCaseManager.
 */
@CameraScope
public class CameraGraphConfigProvider
@Inject
constructor(
    private val callbackMap: CameraCallbackMap,
    private val requestListener: ComboRequestListener,
    private val cameraConfig: CameraConfig,
    private val cameraQuirks: CameraQuirks,
    private val zslControl: ZslControl,
    private val templateParamsOverride: TemplateParamsOverride,
    private val cameraMetadata: CameraMetadata?,
    private val cameraXConfig: CameraXConfig? = null,
    private val cameraInteropStateCallbackRepository: CameraInteropStateCallbackRepository? = null,
) {
    private val closeCameraOnCameraGraphClose = CloseCameraOnCameraGraphClose()
    private val supportedDynamicRangeProfiles =
        if (Build.VERSION.SDK_INT >= 33) {
            cameraMetadata
                ?.let { DynamicRangeProfilesCompat.fromCameraMetaData(it) }
                ?.toDynamicRangeProfiles()
        } else {
            null
        }

    public fun create(
        operatingMode: OperatingMode,
        sessionConfig: SessionConfig?,
        setOutputType: Boolean,
        graphStateToCameraStateAdapter: GraphStateToCameraStateAdapter? = null,
        camera2ExtensionMode: Int? = null,
        surfaceToStreamUseCaseMap: Map<DeferrableSurface, Long> = emptyMap(),
        surfaceToStreamUseHintMap: Map<DeferrableSurface, Long> = emptyMap(),
    ): GraphConfigBundle {
        val isExtensions = operatingMode == OperatingMode.EXTENSION
        val enableStreamUseCase = !isExtensions // Enable StreamUseCase if not in Extension mode

        val streamGroupMap = mutableMapOf<Int, MutableList<CameraStream.Config>>()
        val inputStreams = mutableListOf<InputStream.Config>()
        var sessionTemplate = RequestTemplate(TEMPLATE_PREVIEW)
        val sessionParameters: MutableMap<Any, Any> = mutableMapOf()
        val streamConfigMap: MutableMap<CameraStream.Config, DeferrableSurface> = mutableMapOf()
        sessionConfig?.let { sessionConfig ->
            cameraInteropStateCallbackRepository?.updateCallbacks(sessionConfig)

            if (sessionConfig.templateType != CaptureConfig.TEMPLATE_TYPE_NONE) {
                sessionTemplate = RequestTemplate(sessionConfig.templateType)
            }
            sessionParameters.putAll(templateParamsOverride.getOverrideParams(sessionTemplate))
            sessionParameters.putAll(sessionConfig.implementationOptions.toParameters())
            if (operatingMode == OperatingMode.EXTENSION) {
                // camera2ExtensionMode must be non-null when operatingMode is EXTENSION
                sessionParameters[CameraPipeKeys.camera2ExtensionMode] = camera2ExtensionMode!!
            }

            val physicalCameraIdForAllStreams =
                sessionConfig.toCamera2ImplConfig().getPhysicalCameraId(null)
            var zslStream: CameraStream.Config? = null
            for (outputConfig in sessionConfig.outputConfigs) {
                val deferrableSurface = outputConfig.surface
                val physicalCameraId =
                    physicalCameraIdForAllStreams ?: outputConfig.physicalCameraId
                val dynamicRange = outputConfig.dynamicRange
                val mirrorMode = outputConfig.mirrorMode
                val outputStreamConfig =
                    OutputStream.Config.create(
                        dynamicRangeProfile = dynamicRange.toDynamicRangeProfile(),
                        size = deferrableSurface.prescribedSize,
                        format = StreamFormat(deferrableSurface.prescribedStreamFormat),
                        camera =
                            if (physicalCameraId == null) {
                                null
                            } else {
                                CameraId.fromCamera2Id(physicalCameraId)
                            },
                        // No need to map MIRROR_MODE_ON_FRONT_ONLY to MIRROR_MODE_AUTO
                        // since its default value in framework
                        mirrorMode =
                            when (mirrorMode) {
                                MirrorMode.MIRROR_MODE_OFF ->
                                    OutputStream.MirrorMode(OutputConfiguration.MIRROR_MODE_NONE)
                                MirrorMode.MIRROR_MODE_ON ->
                                    OutputStream.MirrorMode(OutputConfiguration.MIRROR_MODE_H)
                                else -> null
                            },
                        outputType =
                            if (setOutputType) {
                                when (outputConfig.surface.containerClass) {
                                    // Used for VideoCapture use case
                                    MediaCodec::class.java -> OutputType.MEDIA_CODEC

                                    // Preview may use either SurfaceView or SurfaceTexture
                                    SurfaceHolder::class.java -> OutputType.SURFACE_VIEW
                                    SurfaceTexture::class.java -> OutputType.SURFACE_TEXTURE

                                    // Using the generic SURFACE type by default, usually
                                    // ImageReader surfaces
                                    // (from ImageCapture/ImageAnalysis use cases) fall to this
                                    // case for CameraX
                                    else -> OutputType.SURFACE
                                }
                            } else {
                                // Default output type
                                OutputType.SURFACE
                            },
                        streamUseCase =
                            if (enableStreamUseCase) {
                                getStreamUseCase(
                                    deferrableSurface,
                                    surfaceToStreamUseCaseMap,
                                    cameraMetadata,
                                )
                            } else {
                                null
                            },
                        streamUseHint =
                            if (enableStreamUseCase) {
                                getStreamUseHint(deferrableSurface, surfaceToStreamUseHintMap)
                            } else {
                                null
                            },
                    )
                val surfaces = outputConfig.sharedSurfaces + deferrableSurface
                for (surface in surfaces) {
                    val stream = CameraStream.Config.create(outputStreamConfig)
                    streamConfigMap[stream] = surface
                    if (outputConfig.surfaceGroupId != SURFACE_GROUP_ID_NONE) {
                        val streamList = streamGroupMap[outputConfig.surfaceGroupId]
                        if (streamList == null) {
                            streamGroupMap[outputConfig.surfaceGroupId] = mutableListOf(stream)
                        } else {
                            streamList.add(stream)
                        }
                    }
                    if (surface != deferrableSurface) continue
                    if (zslControl.isZslSurface(surface, sessionConfig)) {
                        zslStream = stream
                    }
                }
            }
            if (sessionConfig.inputConfiguration != null) {
                zslStream?.let {
                    inputStreams.add(
                        InputStream.Config(
                            stream = it,
                            maxImages = 1,
                            streamFormat = it.outputs.single().format,
                        )
                    )
                }
            }
        }

        val combinedFlags = createCameraGraphFlags(cameraQuirks, isExtensions)

        // Set video stabilization mode to capture request
        var videoStabilizationMode: Int? = null
        if (sessionConfig != null) {
            val config = sessionConfig.repeatingCaptureConfig
            videoStabilizationMode = getVideoStabilizationModeFromCaptureConfig(config)
        }

        // Set fps range to capture request
        val targetFpsRange =
            sessionConfig?.expectedFrameRateRange.takeIf {
                it != StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
            }

        // TODO: b/427615304 - Pass the same params in both sessionParameters and
        //  defaultParameters to reduce duplicate code.
        val defaultParameters =
            buildMap<Any, Any?> {
                if (isExtensions) {
                    set(CameraPipeKeys.ignore3ARequiredParameters, true)
                }
                videoStabilizationMode?.let {
                    set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, it)
                }
                set(
                    CameraPipeKeys.camera2CaptureRequestTag,
                    "android.hardware.camera2.CaptureRequest.setTag.CX",
                )
                targetFpsRange?.let {
                    set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetFpsRange)
                }
            }

        // Explicitly set session parameters which aren't included in CameraX
        // SessionConfig.implementationOptions
        // TODO: b/427615304 - Improve the design so that these params are included in
        //  implementationOptions and don't have to be set separately which is a bit errorprone
        targetFpsRange?.let {
            sessionParameters[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] = targetFpsRange
        }
        videoStabilizationMode?.let {
            sessionParameters[CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE] =
                videoStabilizationMode
        }

        val postviewStream =
            sessionConfig?.let { config ->
                val physicalCameraIdForAllStreams =
                    config.toCamera2ImplConfig().getPhysicalCameraId(null)
                config.postviewOutputConfig?.let { postviewOutputConfig ->
                    createPostviewStream(postviewOutputConfig, physicalCameraIdForAllStreams)
                        ?.also { streamConfigMap[it] = postviewOutputConfig.surface }
                }
            }

        cameraXConfig
            ?.getCamera2CaptureRequestConfigurator()
            ?.configureWithUnchecked(sessionParameters)

        // TODO: b/327517884 - Add a quirk to not abort captures on stop for certain OEMs during
        //   extension sessions.

        // Build up a config (using TEMPLATE_PREVIEW by default)
        val graphConfig =
            CameraGraph.Config(
                camera = cameraConfig.cameraId,
                streams = streamConfigMap.keys.toList(),
                exclusiveStreamGroups = streamGroupMap.values.toList(),
                input = if (inputStreams.isEmpty()) null else inputStreams,
                postviewStream = postviewStream,
                sessionTemplate = sessionTemplate,
                sessionParameters = sessionParameters,
                sessionMode = operatingMode,
                defaultListeners = listOf(callbackMap, requestListener),
                defaultParameters = defaultParameters,
                flags = combinedFlags,
                graphStateListeners = listOfNotNull(graphStateToCameraStateAdapter),
            )

        return GraphConfigBundle(
            graphConfig = graphConfig,
            streamToSurfaceMap = streamConfigMap.toMap(),
        )
    }

    private fun createPostviewStream(
        postviewConfig: SessionConfig.OutputConfig,
        physicalCameraIdForAllStreams: String?,
    ): CameraStream.Config? {
        val deferrableSurface = postviewConfig.surface
        val physicalCameraId = physicalCameraIdForAllStreams ?: postviewConfig.physicalCameraId
        val mirrorMode = postviewConfig.mirrorMode
        val outputStreamConfig =
            OutputStream.Config.create(
                size = deferrableSurface.prescribedSize,
                format = StreamFormat(deferrableSurface.prescribedStreamFormat),
                camera =
                    if (physicalCameraId == null) {
                        null
                    } else {
                        CameraId.fromCamera2Id(physicalCameraId)
                    },
                // No need to map MIRROR_MODE_ON_FRONT_ONLY to MIRROR_MODE_AUTO
                // since its default value in framework
                mirrorMode =
                    when (mirrorMode) {
                        MirrorMode.MIRROR_MODE_OFF ->
                            OutputStream.MirrorMode(OutputConfiguration.MIRROR_MODE_NONE)
                        MirrorMode.MIRROR_MODE_ON ->
                            OutputStream.MirrorMode(OutputConfiguration.MIRROR_MODE_H)
                        else -> null
                    },
            )
        return CameraStream.Config.create(outputStreamConfig)
    }

    private fun getStreamUseCase(
        deferrableSurface: DeferrableSurface,
        mapping: Map<DeferrableSurface, Long>,
        cameraMetadata: CameraMetadata?,
    ): OutputStream.StreamUseCase? {
        val expectedStreamUseCase =
            mapping[deferrableSurface]?.let { OutputStream.StreamUseCase(it) }
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                expectedStreamUseCase != null &&
                cameraMetadata
                    ?.get(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES)
                    ?.contains(expectedStreamUseCase.value) == true
        ) {
            expectedStreamUseCase
        } else {
            Camera2Logger.warn {
                "Expected stream use case for $deferrableSurface, " +
                    "$expectedStreamUseCase cannot be set!"
            }
            null
        }
    }

    private fun getStreamUseHint(
        deferrableSurface: DeferrableSurface,
        mapping: Map<DeferrableSurface, Long>,
    ): OutputStream.StreamUseHint? {
        return mapping[deferrableSurface]?.let { OutputStream.StreamUseHint(it) }
    }

    private fun createCameraGraphFlags(
        cameraQuirks: CameraQuirks,
        isExtensions: Boolean,
    ): CameraGraph.Flags {
        if (cameraQuirks.quirks.contains(CaptureSessionStuckQuirk::class.java)) {
            Camera2Logger.debug {
                "CameraPipe should be enabling CaptureSessionStuckQuirk by default"
            }
        }
        // TODO(b/276354253): Set quirkWaitForRepeatingRequestOnDisconnect flag for overrides.

        // TODO(b/277310425): When creating a CameraGraph, this flag should be turned OFF when
        //  this behavior is not needed based on the use case interaction and the device on
        //  which the test is running.
        val shouldFinalizeSessionOnCloseBehavior = FinalizeSessionOnCloseQuirk.getBehavior()

        // SurfaceRequest API documentation stipulates that previous SurfaceRequests are guaranteed
        // to be detached when a new request is made. This means whenever we need a new session,
        // all Surfaces should be disconnected. To do this, we need to close the capture session
        // unconditionally.
        val shouldCloseCaptureSessionOnDisconnect = true

        val shouldCloseCameraDeviceOnClose =
            closeCameraOnCameraGraphClose.shouldCloseCameraDevice(isExtensions)

        val shouldAbortCapturesOnStop =
            when {
                isExtensions &&
                    DeviceQuirks[DisableAbortCapturesOnStopWithSessionProcessorQuirk::class.java] !=
                        null -> false
                DeviceQuirks[DisableAbortCapturesOnStopQuirk::class.java] != null -> false
                /** @see [CameraGraph.Flags.abortCapturesOnStop] */
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> true
                else -> false
            }

        val repeatingRequestsToCompleteBeforeNonRepeatingCapture =
            if (
                cameraQuirks.quirks.contains(
                    QuickSuccessiveImageCaptureFailsRepeatingRequestQuirk::class.java
                )
            ) {
                1u
            } else {
                0u
            }

        return CameraGraph.Flags(
            abortCapturesOnStop = shouldAbortCapturesOnStop,
            awaitRepeatingRequestBeforeCapture =
                CameraGraph.RepeatingRequestRequirementsBeforeCapture(
                    repeatingFramesToComplete =
                        repeatingRequestsToCompleteBeforeNonRepeatingCapture,
                    // TODO: b/364491700 - use CompletionBehavior.EXACT to disable CameraPipe
                    //  internal workaround when not required. See
                    //  Camera2Quirks.getRepeatingRequestFrameCountForCapture for details.
                    completionBehavior = AT_LEAST,
                ),
            closeCaptureSessionOnDisconnect = shouldCloseCaptureSessionOnDisconnect,
            closeCameraDeviceOnClose = shouldCloseCameraDeviceOnClose,
            finalizeSessionOnCloseBehavior = shouldFinalizeSessionOnCloseBehavior,
            enableRestartDelays = true,
        )
    }

    private fun DynamicRange.toDynamicRangeProfile(): DynamicRangeProfile? {
        var dynamicRangeProfile: DynamicRangeProfile? = null

        if (Build.VERSION.SDK_INT >= 33) {
            dynamicRangeProfile = DynamicRangeProfile.STANDARD

            if (supportedDynamicRangeProfiles != null) {
                val firstSupportedProfile =
                    dynamicRangeToFirstSupportedProfile(this, supportedDynamicRangeProfiles)
                if (firstSupportedProfile != null) {
                    dynamicRangeProfile = DynamicRangeProfile(firstSupportedProfile)
                } else {
                    Camera2Logger.error {
                        "Requested dynamic range is not supported. Defaulting to STANDARD" +
                            " dynamic range profile.\nRequested dynamic range:\n $this"
                    }
                }
            }
        }

        return dynamicRangeProfile
    }

    private fun SessionConfig.toCamera2ImplConfig(): Camera2ImplConfig {
        return Camera2ImplConfig(implementationOptions)
    }

    // return video stabilization mode. null indicate mode unspecified.
    private fun getVideoStabilizationModeFromCaptureConfig(captureConfig: CaptureConfig): Int? {
        val isPreviewStabilizationMode = captureConfig.previewStabilizationMode
        val isVideoStabilizationMode = captureConfig.videoStabilizationMode

        return if (
            isPreviewStabilizationMode == StabilizationMode.OFF ||
                isVideoStabilizationMode == StabilizationMode.OFF
        ) {
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        } else if (isPreviewStabilizationMode == StabilizationMode.ON) {
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
        } else if (isVideoStabilizationMode == StabilizationMode.ON) {
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
        } else {
            null
        }
    }

    override fun toString(): String = "CameraGraphConfigProvider<${cameraConfig.cameraId}>"
}

/**
 * A bundle containing the [CameraGraph.Config] and the mapping of [CameraStream.Config] to
 * [DeferrableSurface] used to bridge CameraPipe and CameraX.
 */
public data class GraphConfigBundle(
    val graphConfig: CameraGraph.Config,
    val streamToSurfaceMap: Map<CameraStream.Config, DeferrableSurface>,
)
