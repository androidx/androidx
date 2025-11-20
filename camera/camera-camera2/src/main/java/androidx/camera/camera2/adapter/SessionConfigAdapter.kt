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

package androidx.camera.camera2.adapter

import android.hardware.camera2.CameraDevice
import android.media.MediaCodec
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.internal.StreamUseCaseUtil
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.streamsharing.StreamSharing
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Aggregate the SessionConfig from a List of [UseCase]s, and provide a validated SessionConfig for
 * operation.
 */
public class SessionConfigAdapter(
    private val useCases: Collection<UseCase>,
    private val isPrimary: Boolean = true,
) {
    public val surfaceToStreamUseCaseMap: Map<DeferrableSurface, Long> by lazy {
        val sessionConfigs = mutableListOf<SessionConfig>()
        val useCaseConfigs = mutableListOf<UseCaseConfig<*>>()
        for (useCase in useCases) {
            sessionConfigs.add(useCase.getSessionConfig(isPrimary))
            useCaseConfigs.add(useCase.currentConfig)
        }
        getSurfaceToStreamUseCaseMapping(sessionConfigs, useCaseConfigs)
    }
    public val surfaceToStreamUseHintMap: Map<DeferrableSurface, Long> by lazy {
        val sessionConfigs = useCases.map { it.getSessionConfig(isPrimary) }
        getSurfaceToStreamUseHintMapping(sessionConfigs)
    }
    private val validatingBuilder: SessionConfig.ValidatingBuilder by lazy {
        val validatingBuilder = SessionConfig.ValidatingBuilder()

        for (useCase in useCases) {
            validatingBuilder.add(useCase.getSessionConfig(isPrimary))
        }

        validatingBuilder
    }

    private val sessionConfig: SessionConfig by lazy {
        check(validatingBuilder.isValid)

        validatingBuilder.build()
    }

    public val deferrableSurfaces: List<DeferrableSurface> by lazy {
        check(validatingBuilder.isValid)

        sessionConfig.postviewOutputConfig?.let {
            Collections.unmodifiableList(
                mutableListOf<DeferrableSurface>().apply {
                    addAll(sessionConfig.surfaces)
                    add(it.surface)
                }
            )
        } ?: sessionConfig.surfaces
    }

    public fun getValidSessionConfigOrNull(): SessionConfig? {
        return if (isSessionConfigValid()) sessionConfig else null
    }

    public fun isSessionConfigValid(): Boolean {
        return validatingBuilder.isValid
    }

    public fun reportSurfaceInvalid(deferrableSurface: DeferrableSurface) {
        Camera2Logger.debug { "Unavailable $deferrableSurface, notify SessionConfig invalid" }

        // Only report error to one SessionConfig, CameraInternal#onUseCaseReset()
        // will handle the other failed Surfaces if there are any.
        val sessionConfig =
            useCases
                .firstOrNull { useCase ->
                    val sessionConfig = useCase.getSessionConfig(isPrimary)
                    sessionConfig.surfaces.contains(deferrableSurface)
                }
                ?.sessionConfig

        CoroutineScope(Dispatchers.Main.immediate).launch {
            // The error listener is used to notify the UseCase to recreate the pipeline,
            // and the create pipeline task would be executed on the main thread.
            sessionConfig?.errorListener?.apply {
                onError(sessionConfig, SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET)
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
    public fun getSurfaceToStreamUseCaseMapping(
        sessionConfigs: Collection<SessionConfig>,
        useCaseConfigs: Collection<UseCaseConfig<*>>,
    ): Map<DeferrableSurface, Long> {
        if (sessionConfigs.any { it.templateType == CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG }) {
            // If is ZSL, do not populate anything.
            Camera2Logger.error { "ZSL in populateSurfaceToStreamUseCaseMapping()" }
            return emptyMap()
        }

        val mapping = mutableMapOf<DeferrableSurface, Long>()
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
            sessionConfigs,
            useCaseConfigs,
            mapping,
        )

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
    public fun getSurfaceToStreamUseHintMapping(
        sessionConfigs: Collection<SessionConfig>
    ): Map<DeferrableSurface, Long> {
        val mapping = mutableMapOf<DeferrableSurface, Long>()
        for (sessionConfig in sessionConfigs) {
            for (surface in sessionConfig.surfaces) {
                if (
                    sessionConfig.implementationOptions.containsOption(
                        Camera2ImplConfig.STREAM_USE_HINT_OPTION
                    ) &&
                        sessionConfig.implementationOptions.retrieveOption(
                            Camera2ImplConfig.STREAM_USE_HINT_OPTION
                        ) != null
                ) {
                    mapping[surface] =
                        sessionConfig.implementationOptions.retrieveOption(
                            Camera2ImplConfig.STREAM_USE_HINT_OPTION
                        )!!
                    continue
                }

                mapping[surface] = getStreamUseHintForContainerClass(surface.containerClass)
            }
        }
        return mapping
    }

    /**
     * Determines the appropriate [OutputStream.StreamUseHint] value based on the provided container
     * class.
     *
     * StreamUseHint is used for the following purposes:
     *
     * (1) **Surface Ordering:** To ensure [MediaCodec] surfaces are placed at the end of the output
     * list within [androidx.camera.camera2.pipe.graph.StreamGraphImpl]. Note: [StreamSharing] uses
     * [android.graphics.SurfaceTexture], not [MediaCodec] surface.
     *
     * (2) **High-Speed Session Operation:** To identify the presence of a [MediaCodec] surface in
     * high-speed capture session scenarios within
     * [androidx.camera.camera2.pipe.compat.Camera2CaptureSequenceProcessor].
     *
     * @param kClass The Kotlin [Class] of the container.
     * @return The corresponding [OutputStream.StreamUseHint] value.
     */
    private fun getStreamUseHintForContainerClass(kClass: Class<*>?): Long {
        return when (kClass) {
            MediaCodec::class.java -> OutputStream.StreamUseHint.VIDEO_RECORD.value
            else -> OutputStream.StreamUseHint.DEFAULT.value
        }
    }

    public companion object {
        public fun UseCase.getSessionConfig(isPrimary: Boolean): SessionConfig {
            return if (isPrimary) sessionConfig else secondarySessionConfig
        }
    }
}
