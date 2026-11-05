/*
 * Copyright 2026 The Android Open Source Project
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
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.config.CameraScope
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.ConfigQueryResult
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraXConfig
import androidx.camera.core.MirrorMode
import androidx.camera.core.featuregroup.impl.UseCaseType.Companion.getFeatureGroupUseCaseType
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.HardwareConfigQueryResult
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.compat.quirk.AeFpsRangeQuirk
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.core.streamsharing.StreamSharingConfig
import com.google.common.util.concurrent.ListenableFuture
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Queries the camera device whether it supports the capture session of a
 * [androidx.camera.core.SessionConfig] configured for dynamic UseCase switching, i.e. a session
 * with the streams of all bound UseCases, by using [CameraPipe.isConfigSupported] (backed by
 * `CameraDevice.CameraDeviceSetup`).
 *
 * Placeholder surfaces are created for the streams, similar to [FeatureCombinationQueryImpl], so no
 * real output surface is required.
 */
@CameraScope
public class FastSwitchConfigQuery
@Inject
constructor(
    private val cameraPipe: CameraPipe,
    private val cameraConfig: CameraConfig,
    private val cameraProperties: CameraProperties,
    private val cameraQuirks: CameraQuirks,
    private val cameraXConfig: CameraXConfig? = null,
) {
    /**
     * Returns whether the camera device supports a session with the given [streams].
     *
     * This blocks the calling thread while the camera device is queried. It returns
     * [HardwareConfigQueryResult.UNKNOWN] below API 35, where the query is not available, or if
     * [streams] is empty, and [HardwareConfigQueryResult.UNSUPPORTED] if the streams have
     * conflicting or invalid session configurations.
     */
    public fun isConfigSupported(
        config: androidx.camera.core.SessionConfig,
        streams: List<ResolvedStreamInfo>,
    ): HardwareConfigQueryResult {
        if (streams.isEmpty()) {
            return HardwareConfigQueryResult.UNKNOWN
        }
        val sessionConfigs = mutableListOf<SessionConfig>()
        try {
            streams.mapTo(sessionConfigs) { it.createSessionConfig(config) }
            val validatingBuilder = SessionConfig.ValidatingBuilder()
            try {
                sessionConfigs.forEach { validatingBuilder.add(it) }
                if (validatingBuilder.isValid) {
                    SessionConfigAdapter.applySessionInteropConfig(
                        validatingBuilder,
                        config.interopConfig,
                    )
                }
            } catch (e: IllegalArgumentException) {
                Camera2Logger.warn(e) {
                    "FastSwitchConfigQuery: Conflicting options in $streams"
                }
                return HardwareConfigQueryResult.UNSUPPORTED
            }
            if (!validatingBuilder.isValid) {
                Camera2Logger.warn {
                    "FastSwitchConfigQuery: Can't combine $streams due to " +
                        validatingBuilder.invalidReason
                }
                return HardwareConfigQueryResult.UNSUPPORTED
            }
            val combinedSessionConfig = validatingBuilder.build()
            val effectiveSessionType =
                Camera2ImplConfig(combinedSessionConfig.implementationOptions)
                    .getSessionType(combinedSessionConfig.sessionType)
                    ?: combinedSessionConfig.sessionType
            if (effectiveSessionType != SessionConfig.SESSION_TYPE_REGULAR) {
                return HardwareConfigQueryResult.UNSUPPORTED
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                return HardwareConfigQueryResult.UNKNOWN
            }

            val surfaceConfigOptionsMap =
                SessionConfigAdapter.buildSurfaceConfigOptionsMap(
                    sessionConfigs,
                    streams.map { it.useCaseConfig },
                    combinedSessionConfig.templateType,
                    allowEmptySurfaceStreamUseCase = true,
                )

            val graphConfig =
                CameraGraphConfigProvider(
                        callbackMap = CameraCallbackMap(),
                        requestListener = ComboRequestListener(),
                        cameraConfig = cameraConfig,
                        cameraQuirks = cameraQuirks,
                        zslControl = ZslControlNoOpImpl(),
                        templateParamsOverride = TemplateParamsQuirkOverride(cameraQuirks.quirks),
                        cameraMetadata = cameraProperties.metadata,
                        cameraXConfig = cameraXConfig,
                    )
                    .create(
                        operatingMode = CameraGraph.OperatingMode.NORMAL,
                        sessionConfig = combinedSessionConfig,
                        setOutputType = true,
                        surfaceConfigOptionsMap = surfaceConfigOptionsMap,
                    )
                    .graphConfig

            val result = runBlocking { cameraPipe.isConfigSupported(graphConfig) }
            Camera2Logger.debug {
                "FastSwitchConfigQuery: result = $result for streams = " +
                    graphConfig.streams.flatMap { stream ->
                        stream.outputs.map {
                            "size=${it.size}, format=${it.format}, " +
                                "dynamicRangeProfile=${it.dynamicRangeProfile}, " +
                                "streamUseCase=${it.streamUseCase}"
                        }
                    } +
                    " and sessionParameters = ${graphConfig.sessionParameters}"
            }
            return when (result) {
                ConfigQueryResult.SUPPORTED -> HardwareConfigQueryResult.SUPPORTED
                ConfigQueryResult.UNSUPPORTED -> HardwareConfigQueryResult.UNSUPPORTED
                else -> HardwareConfigQueryResult.UNKNOWN
            }
        } finally {
            sessionConfigs.forEach { sessionConfig ->
                sessionConfig.surfaces.forEach { it.close() }
            }
        }
    }

    private fun ResolvedStreamInfo.createSessionConfig(
        config: androidx.camera.core.SessionConfig
    ): SessionConfig {
        val useCaseConfig = this.useCaseConfig
        val primarySurface =
            object : DeferrableSurface(streamSpec.resolution, useCaseConfig.inputFormat) {
                override fun provideSurface(): ListenableFuture<Surface?> =
                    Futures.immediateFuture(null)
            }
        useCaseConfig.getFeatureGroupUseCaseType().surfaceClass?.let {
            primarySurface.setContainerClass(it)
        }
        val mirrorMode =
            if (
                useCaseConfig is PreviewConfig &&
                    config.effects.none { (it.targets and CameraEffect.PREVIEW) != 0 }
            ) {
                useCaseConfig.getMirrorMode(MirrorMode.MIRROR_MODE_UNSPECIFIED)
            } else {
                MirrorMode.MIRROR_MODE_UNSPECIFIED
            }

        return SessionConfig.Builder.createFrom(useCaseConfig, streamSpec.resolution)
            .apply {
                addSurface(
                    primarySurface,
                    streamSpec.dynamicRange,
                    this@createSessionConfig.physicalCameraId,
                    mirrorMode,
                )
                for (secondaryFormat in useCaseConfig.inputFormats.drop(1)) {
                    val secondarySurface =
                        object : DeferrableSurface(streamSpec.resolution, secondaryFormat) {
                            override fun provideSurface(): ListenableFuture<Surface?> =
                                Futures.immediateFuture(null)
                        }
                    addNonRepeatingSurface(
                        secondarySurface,
                        streamSpec.dynamicRange,
                        this@createSessionConfig.physicalCameraId,
                    )
                }
                if (
                    !useCaseConfig.containsOption(Camera2ImplConfig.TEMPLATE_TYPE_OPTION) &&
                        useCaseConfig.getDefaultSessionConfig(null) == null &&
                        (useCaseConfig.captureType == CaptureType.METERING_REPEATING ||
                            useCaseConfig.captureType == CaptureType.STREAM_SHARING)
                ) {
                    setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                }
                if (useCaseConfig is StreamSharingConfig) {
                    StreamSharing.propagateChildrenConfigsToSessionConfig(
                        useCaseConfig.childrenConfigs,
                        streamSpec.resolution,
                        this,
                    )
                }
                val frameRateRange =
                    streamSpec.expectedFrameRateRange.takeIf { it != FRAME_RATE_RANGE_UNSPECIFIED }
                        ?: config.frameRateRange.takeIf { it != FRAME_RATE_RANGE_UNSPECIFIED }
                        ?: if (useCaseConfig.captureType != CaptureType.METERING_REPEATING) {
                            cameraQuirks.quirks
                                .getAll(AeFpsRangeQuirk::class.java)
                                .firstOrNull()
                                ?.targetAeFpsRange ?: FRAME_RATE_RANGE_UNSPECIFIED
                        } else {
                            FRAME_RATE_RANGE_UNSPECIFIED
                        }
                if (frameRateRange != FRAME_RATE_RANGE_UNSPECIFIED) {
                    setExpectedFrameRateRange(frameRateRange)
                }
                // Carries e.g. the stream use case chosen when resolving the stream spec.
                streamSpec.implementationOptions?.let { addImplementationOptions(it) }
            }
            .build()
            .also { sessionConfig ->
                if (useCaseConfig.captureType == CaptureType.METERING_REPEATING) {
                    sessionConfig.surfaces.forEach {
                        it.setContainerClass(SurfaceTexture::class.java)
                    }
                }
            }
    }
}
