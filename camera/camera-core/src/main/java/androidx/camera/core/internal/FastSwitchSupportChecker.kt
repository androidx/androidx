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

package androidx.camera.core.internal

import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.HardwareConfigQueryResult
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.UseCaseAdditionSimulator
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfig.OPTION_CAPTURE_TYPE
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.internal.compat.quirk.DeviceQuirks
import androidx.camera.core.internal.compat.quirk.FastSessionSwitchDisablerQuirk
import androidx.camera.core.streamsharing.StreamSharing

/**
 * Checks whether a [SessionConfig] that specifies a subset of
 * [SessionConfig.initialEnabledUseCases] is supported for dynamic UseCase switching on a camera,
 * see [CameraInfoInternal.isSessionConfigSupported].
 */
public object FastSwitchSupportChecker {
    private const val TAG = "FastSwitchSupportCheck"
    private const val CAMERA2_SESSION_TYPE_OPTION_ID = "camera2.cameraCaptureSession.sessionType"

    /**
     * Checks the preconditions for dynamic UseCase switching that do not require stream spec
     * simulation.
     */
    @JvmStatic
    public fun isFastSwitchPreconditionsMet(
        cameraInfo: CameraInfoInternal,
        sessionConfig: SessionConfig,
    ): Boolean {
        if (DeviceQuirks.get(FastSessionSwitchDisablerQuirk::class.java) != null) {
            return false
        }
        if (cameraInfo is AdapterCameraInfo && cameraInfo.sessionProcessor != null) {
            return false
        }
        if (sessionConfig.cameraFilter != null) {
            return false
        }
        if (!isRegularSession(sessionConfig)) {
            return false
        }
        if (
            sessionConfig.requiredFeatureGroup.isNotEmpty() ||
                sessionConfig.preferredFeatureGroup.isNotEmpty()
        ) {
            return false
        }
        if (sessionConfig.useCases.any { isZeroShutterLagEnabled(it) }) {
            return false
        }
        return true
    }

    /**
     * Returns whether [sessionConfig] and its [UseCase]s only configure a regular capture session
     * ([SESSION_TYPE_REGULAR]), including any session type set via Camera2 interop options.
     */
    @JvmStatic
    public fun isRegularSession(sessionConfig: SessionConfig): Boolean {
        if (sessionConfig.sessionType != SESSION_TYPE_REGULAR) {
            return false
        }
        if (!hasOnlyRegularCamera2SessionType(sessionConfig.interopConfig)) {
            return false
        }
        return sessionConfig.useCases.all { useCase ->
            hasOnlyRegularCamera2SessionType(useCase.currentConfig) &&
                hasOnlyRegularCamera2SessionType(useCase.appConfig)
        }
    }

    private fun hasOnlyRegularCamera2SessionType(
        config: androidx.camera.core.impl.Config
    ): Boolean {
        for (option in config.listOptions()) {
            if (option.id == CAMERA2_SESSION_TYPE_OPTION_ID) {
                val value = config.retrieveOption(option)
                if (value is Int && value != SESSION_TYPE_REGULAR) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Returns whether [useCase] is configured with [ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG].
     */
    @OptIn(ExperimentalZeroShutterLag::class)
    @JvmStatic
    public fun isZeroShutterLagEnabled(useCase: UseCase): Boolean =
        (useCase is ImageCapture &&
            useCase.captureMode == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG) ||
            useCase.currentConfig.retrieveOption(
                ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE,
                null,
            ) == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
            useCase.appConfig.retrieveOption(
                ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE,
                null,
            ) == ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG

    /**
     * Checks whether a [SessionConfig] that specifies a subset of
     * [SessionConfig.initialEnabledUseCases] is supported for dynamic UseCase switching on
     * [cameraInfo].
     */
    @JvmStatic
    public fun isSessionConfigFastSwitchSupported(
        cameraInfo: CameraInfoInternal,
        sessionConfig: SessionConfig,
        unionInfo: CalculatedUseCaseInfo,
    ): Boolean {
        if (!isFastSwitchPreconditionsMet(cameraInfo, sessionConfig)) {
            return false
        }
        val sessionStreams =
            prepareSessionStreams(cameraInfo, sessionConfig, unionInfo) ?: return false
        return queryHardwareSupport(cameraInfo, sessionConfig, sessionStreams) !=
            HardwareConfigQueryResult.UNSUPPORTED
    }

    /**
     * Filters [candidateFrameRates] to those supported for dynamic UseCase switching with
     * [sessionConfig] on [cameraInfo].
     *
     * For each candidate range, stream specifications are resolved with that target frame rate so
     * that size arrangements and internal repeating stream checks match what binding with that
     * frame rate range will select.
     */
    @JvmStatic
    public fun filterFastSwitchSupportedFrameRates(
        cameraInfo: CameraInfoInternal,
        sessionConfig: SessionConfig,
        candidateFrameRates: Set<Range<Int>>,
    ): Set<Range<Int>> {
        if (
            candidateFrameRates.isEmpty() ||
                !isFastSwitchPreconditionsMet(cameraInfo, sessionConfig)
        ) {
            return emptySet()
        }
        return candidateFrameRates.filterTo(linkedSetOf()) { frameRate ->
            val infoForFps =
                try {
                    UseCaseAdditionSimulator.simulateAddUseCases(
                        cameraInfo,
                        sessionConfig,
                        findSupportedFrameRateRanges = false,
                        targetFrameRate = frameRate,
                    )
                } catch (e: IllegalArgumentException) {
                    Logger.d(TAG, "Frame rate $frameRate not supported for $sessionConfig", e)
                    return@filterTo false
                } catch (e: CameraUseCaseAdapter.CameraException) {
                    Logger.d(TAG, "Frame rate $frameRate not supported for $sessionConfig", e)
                    return@filterTo false
                }
            val sessionStreams =
                prepareSessionStreams(cameraInfo, sessionConfig, infoForFps)
                    ?: return@filterTo false
            if (sessionStreams.isEmpty()) {
                return@filterTo true
            }
            val streamsForFps = sessionStreams.map { stream ->
                ResolvedStreamInfo(
                    stream.useCaseConfig,
                    stream.streamSpec.toBuilder().setExpectedFrameRateRange(frameRate).build(),
                    stream.physicalCameraId,
                )
            }
            queryHardwareSupport(cameraInfo, sessionConfig, streamsForFps) !=
                HardwareConfigQueryResult.UNSUPPORTED
        }
    }

    private fun prepareSessionStreams(
        cameraInfo: CameraInfoInternal,
        sessionConfig: SessionConfig,
        unionInfo: CalculatedUseCaseInfo,
    ): List<ResolvedStreamInfo>? {
        val mergedConfigs =
            try {
                unionInfo.cameraUseCases.associateWith { useCase ->
                    val configPair = unionInfo.useCaseConfigs[useCase]
                    if (configPair != null) {
                        useCase.mergeConfigs(
                            cameraInfo.implementation,
                            configPair.mExtendedConfig,
                            configPair.mCameraConfig,
                        )
                    } else {
                        useCase.currentConfig
                    }
                }
            } catch (e: RuntimeException) {
                Logger.w(TAG, "Failed to merge configs for $sessionConfig", e)
                return null
            }
        val initialMeteringRequired =
            cameraInfo.isRepeatingStreamForced &&
                isInitialMeteringRepeatingRequired(sessionConfig, unionInfo, mergedConfigs)
        val adapterPhysicalCameraId = (cameraInfo as? AdapterCameraInfo)?.physicalCameraId
        val streams =
            unionInfo.primaryStreamSpecResult.streamSpecs.map { (useCase, streamSpec) ->
                ResolvedStreamInfo(
                    mergedConfigs.getValue(useCase),
                    streamSpec,
                    useCase.physicalCameraId ?: adapterPhysicalCameraId,
                )
            }
        val repeatingStream =
            if (
                cameraInfo.isRepeatingStreamForced &&
                    willAttemptMeteringRepeating(unionInfo, mergedConfigs)
            ) {
                try {
                    cameraInfo.resolveRepeatingStream(unionInfo, streams)
                } catch (e: RuntimeException) {
                    Logger.w(TAG, "Failed to resolve repeating stream for $sessionConfig", e)
                    null
                }
            } else {
                null
            }
        if (initialMeteringRequired && repeatingStream == null) {
            return null
        }
        return if (repeatingStream != null) {
            streams + repeatingStream
        } else {
            streams
        }
    }

    private fun queryHardwareSupport(
        cameraInfo: CameraInfoInternal,
        sessionConfig: SessionConfig,
        sessionStreams: List<ResolvedStreamInfo>,
    ): HardwareConfigQueryResult =
        try {
            cameraInfo.queryFastSwitchHardwareSupport(sessionConfig, sessionStreams)
        } catch (e: RuntimeException) {
            // The hardware query is best effort, the result of the other checks is kept.
            Logger.w(TAG, "Failed to query the hardware support of $sessionConfig", e)
            HardwareConfigQueryResult.UNKNOWN
        }

    private fun willAttemptMeteringRepeating(
        unionInfo: CalculatedUseCaseInfo,
        mergedConfigs: Map<UseCase, UseCaseConfig<*>>,
    ): Boolean =
        unionInfo.cameraUseCases.any { useCase ->
            !useCase.hasRepeatingStream(mergedConfigs[useCase])
        }

    private fun isInitialMeteringRepeatingRequired(
        sessionConfig: SessionConfig,
        unionInfo: CalculatedUseCaseInfo,
        mergedConfigs: Map<UseCase, UseCaseConfig<*>>,
    ): Boolean {
        val streamSharing = unionInfo.streamSharing
        val initialCameraUseCases =
            sessionConfig.initialEnabledUseCases.mapTo(mutableSetOf()) { useCase ->
                if (streamSharing != null && streamSharing.children.contains(useCase)) {
                    streamSharing
                } else {
                    useCase
                }
            }
        if (initialCameraUseCases.isEmpty()) {
            return false
        }
        return initialCameraUseCases.none { useCase ->
            useCase.hasRepeatingStream(mergedConfigs[useCase])
        }
    }

    private fun UseCase.hasRepeatingStream(mergedConfig: UseCaseConfig<*>?): Boolean {
        val captureType =
            if (mergedConfig != null && mergedConfig.containsOption(OPTION_CAPTURE_TYPE)) {
                mergedConfig.captureType
            } else {
                null
            }
        return this is Preview ||
            this is ImageAnalysis ||
            this is StreamSharing ||
            captureType == CaptureType.PREVIEW ||
            captureType == CaptureType.IMAGE_ANALYSIS ||
            captureType == CaptureType.STREAM_SHARING
    }
}
