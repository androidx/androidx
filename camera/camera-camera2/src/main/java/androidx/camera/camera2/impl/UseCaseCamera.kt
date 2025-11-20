/*
 * Copyright 2020 The Android Open Source Project
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

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Pair
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.config.UseCaseCameraScope
import androidx.camera.camera2.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionProcessor
import dagger.Binds
import dagger.Module
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit.NANOSECONDS
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val useCaseCameraIds = atomic(0)
internal val defaultOptionPriority = Config.OptionPriority.OPTIONAL
internal const val defaultTemplate = CameraDevice.TEMPLATE_PREVIEW

@JvmDefaultWithCompatibility
public interface UseCaseCamera {
    // RequestControl of the UseCaseCamera
    public val requestControl: UseCaseCameraRequestControl

    public fun start()

    public suspend fun getCameraCapturePipeline(
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashMode flashMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): CameraCapturePipeline

    public fun setActiveResumeMode(enabled: Boolean) {}

    public fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Job

    // Lifecycle
    public fun close(): Job
}

/** API for interacting with a [CameraGraph] that has been configured with a set of [UseCase]'s */
@UseCaseCameraScope
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
// Java version required for Dagger
public class UseCaseCameraImpl
@Inject
constructor(
    private val useCaseGraphConfig: UseCaseGraphConfig,
    private val useCases: java.util.ArrayList<UseCase>,
    private val useCaseSurfaceManager: UseCaseSurfaceManager,
    private val threads: UseCaseThreads,
    private val sessionConfigAdapter: SessionConfigAdapter,
    override val requestControl: UseCaseCameraRequestControl,
    private val capturePipeline: CapturePipeline,
    private val sessionProcessor: SessionProcessor?,
) : UseCaseCamera {
    private val debugId = useCaseCameraIds.incrementAndGet()
    private val closed = atomic(false)

    init {
        Camera2Logger.debug { "Configured $this for $useCases" }
    }

    override fun start(): Unit =
        with(useCaseGraphConfig) {
            // Start the CameraGraph first before setting up Surfaces. Surfaces can be closed, and
            // we will close the CameraGraph when that happens, and we cannot start a closed
            // CameraGraph.
            graph.start()

            sessionProcessor?.let { processor ->
                val stillCaptureStreamId = findStillCaptureStreamId()
                processor.setCaptureSessionRequestProcessor(
                    object : SessionProcessor.CaptureSessionRequestProcessor {
                        override fun getRealtimeStillCaptureLatency(): Pair<Long, Long>? {
                            // Still capture stream ID might be null if no image capture use case
                            if (stillCaptureStreamId == null) return null

                            val outputLatency =
                                graph.streams.getOutputLatency(stillCaptureStreamId) ?: return null
                            val captureLatencyMs =
                                NANOSECONDS.toMillis(outputLatency.estimatedCaptureLatencyNs)
                            val processingLatencyMs =
                                NANOSECONDS.toMillis(outputLatency.estimatedProcessingLatencyNs)
                            return Pair.create(captureLatencyMs, processingLatencyMs)
                        }

                        override fun setExtensionStrength(strength: Int) {
                            if (Build.VERSION.SDK_INT >= 34) {
                                requestControl.setParametersAsync(
                                    values =
                                        mutableMapOf(CaptureRequest.EXTENSION_STRENGTH to strength)
                                )
                            }
                        }
                    }
                )
            }

            Camera2Logger.debug { "Setting up Surfaces with UseCaseSurfaceManager" }
            if (sessionConfigAdapter.isSessionConfigValid()) {
                useCaseSurfaceManager
                    .setupAsync(graph, sessionConfigAdapter, surfaceToStreamMap)
                    .invokeOnCompletion { throwable ->
                        // Only show logs for error cases, ignore CancellationException since the
                        // task could be cancelled by UseCaseSurfaceManager#stopAsync().
                        if (throwable != null && throwable !is CancellationException) {
                            Camera2Logger.error(throwable) { "Surface setup error!" }
                        }
                    }
            } else {
                Camera2Logger.error {
                    "Unable to create capture session due to conflicting configurations"
                }
            }
        }

    private fun findStillCaptureStreamId(): StreamId? {
        val sessionConfig = sessionConfigAdapter.getValidSessionConfigOrNull() ?: return null
        val repeatingSurfaces = sessionConfig.repeatingCaptureConfig.surfaces

        // Find the first surface that is not part of the repeating set
        val stillCaptureSurface =
            sessionConfig.surfaces.firstOrNull { it !in repeatingSurfaces } ?: return null

        // Convert the surface back to a StreamId
        return useCaseGraphConfig
            .getStreamIdsFromSurfaces(listOf(stillCaptureSurface))
            .firstOrNull()
    }

    override fun close(): Job {
        return if (closed.compareAndSet(expect = false, update = true)) {
            threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                Camera2Logger.debug { "Closing $this" }
                requestControl.close()
                sessionProcessor?.setCaptureSessionRequestProcessor(null)
                useCaseGraphConfig.graph.close()
                useCaseSurfaceManager.stopAsync().await()
            }
        } else {
            CompletableDeferred(Unit)
        }
    }

    override fun setActiveResumeMode(enabled: Boolean) {
        useCaseGraphConfig.graph.isForeground = enabled
    }

    override fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Job {
        return requestControl.updateRepeatingRequestAsync(isPrimary, runningUseCases)
    }

    override fun toString(): String = "UseCaseCamera-$debugId"

    override suspend fun getCameraCapturePipeline(
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashMode flashMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): CameraCapturePipeline =
        capturePipeline.getCameraCapturePipeline(captureMode, flashMode, flashType)

    @Module
    public abstract class Bindings {
        @UseCaseCameraScope
        @Binds
        public abstract fun provideUseCaseCamera(useCaseCamera: UseCaseCameraImpl): UseCaseCamera
    }
}
