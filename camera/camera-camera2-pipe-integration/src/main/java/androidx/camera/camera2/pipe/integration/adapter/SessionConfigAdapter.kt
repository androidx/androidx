/*
 * Copyright 2021 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CameraDevice
import android.media.MediaCodec
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_CASE_OPTION
import androidx.camera.camera2.pipe.integration.impl.STREAM_USE_HINT_OPTION
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.streamsharing.StreamSharing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Aggregate the SessionConfig from a List of [UseCase]s, and provide a validated SessionConfig for
 * operation.
 */
class SessionConfigAdapter(
    private val useCases: Collection<UseCase>,
) {
    val surfaceToStreamUseCaseMap: Map<DeferrableSurface, Long> by lazy {
        val sessionConfigs = mutableListOf<SessionConfig>()
        for (useCase in useCases) {
            sessionConfigs.add(useCase.sessionConfig)
        }
        getSurfaceToStreamUseCaseMapping(sessionConfigs, shouldSetStreamUseCaseByDefault = false)
    }
    val surfaceToStreamUseHintMap: Map<DeferrableSurface, Long> by lazy {
        val sessionConfigs = useCases.map { it.sessionConfig }
        getSurfaceToStreamUseHintMapping(sessionConfigs)
    }
    private val validatingBuilder: SessionConfig.ValidatingBuilder by lazy {
        val validatingBuilder = SessionConfig.ValidatingBuilder()

        for (useCase in useCases) {
            validatingBuilder.add(useCase.sessionConfig)
        }

        validatingBuilder
    }

    private val sessionConfig: SessionConfig by lazy {
        check(validatingBuilder.isValid)

        validatingBuilder.build()
    }

    val deferrableSurfaces: List<DeferrableSurface> by lazy {
        check(validatingBuilder.isValid)

        sessionConfig.surfaces
    }

    fun getValidSessionConfigOrNull(): SessionConfig? {
        return if (isSessionConfigValid()) sessionConfig else null
    }

    fun isSessionConfigValid(): Boolean {
        return validatingBuilder.isValid
    }

    fun reportSurfaceInvalid(deferrableSurface: DeferrableSurface) {
        debug { "Unavailable $deferrableSurface, notify SessionConfig invalid" }

        // Only report error to one SessionConfig, CameraInternal#onUseCaseReset()
        // will handle the other failed Surfaces if there are any.
        val sessionConfig = useCases.firstOrNull { useCase ->
            useCase.sessionConfig.surfaces.contains(deferrableSurface)
        }?.sessionConfig

        CoroutineScope(Dispatchers.Main.immediate).launch {
            // The error listener is used to notify the UseCase to recreate the pipeline,
            // and the create pipeline task would be executed on the main thread.
            sessionConfig?.errorListeners?.forEach {
                it.onError(
                    sessionConfig,
                    SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
                )
            }
        }
    }

    /**
     * Populates the mapping between surfaces of a capture session and the Stream Use Case of their
     * associated stream.
     *
     * @param sessionConfigs collection of all session configs for this capture session
     * @return the mapping between surfaces and Stream Use Case flag
     */
    @VisibleForTesting
    fun getSurfaceToStreamUseCaseMapping(
        sessionConfigs: Collection<SessionConfig>,
        shouldSetStreamUseCaseByDefault: Boolean
    ): Map<DeferrableSurface, Long> {
        if (sessionConfigs.any { it.templateType == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG }) {
            // If is ZSL, do not populate anything.
            Log.error { "ZSL in populateSurfaceToStreamUseCaseMapping()" }
            return emptyMap()
        }

        val mapping = mutableMapOf<DeferrableSurface, Long>()
        for (sessionConfig in sessionConfigs) {
            if (sessionConfig.templateType == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG) {
                // If is ZSL, stream use case is ignored.
                Log.info { "ZSL in populateSurfaceToStreamUseCaseMapping()" }
                return emptyMap()
            }

            for (surface in sessionConfig.surfaces) {
                if (sessionConfig.implementationOptions.containsOption(STREAM_USE_CASE_OPTION) &&
                    sessionConfig.implementationOptions.retrieveOption(STREAM_USE_CASE_OPTION)
                    != null
                ) {
                    mapping[surface] =
                        sessionConfig.implementationOptions
                            .retrieveOption(STREAM_USE_CASE_OPTION)!!
                    continue
                }

                if (shouldSetStreamUseCaseByDefault) {
                    // TODO(b/266879290) This is currently gated out because of camera device
                    // crashing due to unsupported stream useCase combinations.
                    val streamUseCase = getStreamUseCaseForContainerClass(surface.containerClass)
                    mapping[surface] = streamUseCase
                }
            }
        }
        return mapping
    }

    /**
     * Populates the mapping between surfaces of a capture session and the Stream Use Hint of their
     * associated stream.
     *
     * @param sessionConfigs collection of all session configs for this capture session
     * @return the mapping between surfaces and Stream Use Hint flag
     */
    @VisibleForTesting
    fun getSurfaceToStreamUseHintMapping(
        sessionConfigs: Collection<SessionConfig>
    ): Map<DeferrableSurface, Long> {
        val mapping = mutableMapOf<DeferrableSurface, Long>()
        for (sessionConfig in sessionConfigs) {
            for (surface in sessionConfig.surfaces) {
                if (sessionConfig.implementationOptions.containsOption(STREAM_USE_HINT_OPTION) &&
                    sessionConfig.implementationOptions.retrieveOption(STREAM_USE_HINT_OPTION)
                    != null
                ) {
                    mapping[surface] =
                        sessionConfig.implementationOptions
                            .retrieveOption(STREAM_USE_HINT_OPTION)!!
                    continue
                }
            }
        }
        return mapping
    }

    private fun getStreamUseCaseForContainerClass(kClass: Class<*>?): Long {
        return when (kClass) {
            ImageAnalysis::class.java -> OutputStream.StreamUseCase.PREVIEW.value
            Preview::class.java -> OutputStream.StreamUseCase.PREVIEW.value
            ImageCapture::class.java -> OutputStream.StreamUseCase.STILL_CAPTURE.value
            MediaCodec::class.java -> OutputStream.StreamUseCase.VIDEO_RECORD.value
            StreamSharing::class.java -> OutputStream.StreamUseCase.VIDEO_RECORD.value
            else -> OutputStream.StreamUseCase.DEFAULT.value
        }
    }

    private fun getStreamUseHintForContainerClass(kClass: Class<*>?): Long {
        return when (kClass) {
            MediaCodec::class.java -> OutputStream.StreamUseHint.VIDEO_RECORD.value
            StreamSharing::class.java -> OutputStream.StreamUseHint.VIDEO_RECORD.value
            else -> OutputStream.StreamUseHint.DEFAULT.value
        }
    }

    companion object {
        fun SessionConfig.toCamera2ImplConfig(): Camera2ImplConfig {
            return Camera2ImplConfig(implementationOptions)
        }
    }
}
