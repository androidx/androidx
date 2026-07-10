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

import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Pair
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.config.UseCaseCameraScope
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

internal val useCaseCameraIds = atomic(0)
internal val defaultOptionPriority = Config.OptionPriority.OPTIONAL

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
    private val useCaseCameraContext: UseCaseCameraContext,
    private val threads: UseCaseThreads,
    private val sessionProcessor: SessionProcessor?,
    override val requestControl: UseCaseCameraRequestControl,
    private val useCaseSurfaceManagerProvider: Provider<UseCaseSurfaceManager>,
    private val sessionConfigAdapterProvider: Provider<SessionConfigAdapter>,
    private val capturePipelineProvider: Provider<CapturePipeline>,
) : UseCaseCamera {
    private val debugId = useCaseCameraIds.incrementAndGet()
    private val closed = atomic(false)

    init {
        Camera2Logger.debug { "Configured $this" }
    }

    private val useCaseSurfaceManager by lazy { useCaseSurfaceManagerProvider.get() }
    private val sessionConfigAdapter by lazy { sessionConfigAdapterProvider.get() }
    private val capturePipeline by lazy { capturePipelineProvider.get() }

    override fun start() {
        threads.confineLaunch {
            if (closed.value) {
                Camera2Logger.debug {
                    "UseCaseCamera is closed before starting the CameraGraph, skipping setup."
                }
                return@confineLaunch
            }
            val graph = useCaseCameraContext.graph

            // Configure state listeners now that graph is ready
            useCaseCameraContext.configureCameraStateListener()

            // Start the CameraGraph first before setting up Surfaces.
            graph.start()

            val surfaces = useCaseCameraContext.surfaceToStreamMap

            // Calculate stream ID for session processor
            val stillCaptureStreamId = findStillCaptureStreamId()

            Camera2Logger.debug { "Setting up Surfaces with UseCaseSurfaceManager" }
            if (sessionConfigAdapter.isSessionConfigValid()) {
                useCaseSurfaceManager
                    .setupAsync(graph, sessionConfigAdapter, surfaces)
                    .invokeOnCompletion { throwable ->
                        // Only show logs for error cases, ignore CancellationException since
                        // the task could be cancelled by UseCaseSurfaceManager#stopAsync().
                        if (throwable != null && throwable !is CancellationException) {
                            Camera2Logger.error(throwable) { "Surface setup error!" }
                        }
                    }
            } else {
                Camera2Logger.error {
                    "Unable to create capture session due to conflicting configurations"
                }
            }

            // Update Session Processor
            setCaptureSessionRequestProcessor(stillCaptureStreamId, graph)
        }
    }

    private fun findStillCaptureStreamId(): StreamId? {
        val sessionConfig = sessionConfigAdapter.getValidSessionConfigOrNull() ?: return null
        val repeatingSurfaces = sessionConfig.repeatingCaptureConfig.surfaces

        // Find the first surface that is not part of the repeating set
        val stillCaptureSurface =
            sessionConfig.surfaces.firstOrNull { it !in repeatingSurfaces } ?: return null

        // Convert the surface back to a StreamId
        return useCaseCameraContext
            .getStreamIdsFromSurfaces(listOf(stillCaptureSurface))
            .firstOrNull()
    }

    private fun setCaptureSessionRequestProcessor(
        stillCaptureStreamId: StreamId?,
        cameraGraph: CameraGraph,
    ) {
        sessionProcessor?.setCaptureSessionRequestProcessor(
            object : SessionProcessor.CaptureSessionRequestProcessor {
                override fun getRealtimeStillCaptureLatency(): Pair<Long, Long>? {
                    if (stillCaptureStreamId == null) return null
                    val outputLatency =
                        cameraGraph.streams.getOutputLatency(stillCaptureStreamId) ?: return null
                    val captureLatencyMs =
                        TimeUnit.NANOSECONDS.toMillis(outputLatency.estimatedCaptureLatencyNs)
                    val processingLatencyMs =
                        TimeUnit.NANOSECONDS.toMillis(outputLatency.estimatedProcessingLatencyNs)
                    return Pair.create(captureLatencyMs, processingLatencyMs)
                }

                override fun setExtensionStrength(strength: Int) {
                    if (Build.VERSION.SDK_INT >= 34) {
                        requestControl.setParametersAsync(
                            values = mutableMapOf(CaptureRequest.EXTENSION_STRENGTH to strength)
                        )
                    }
                }
            }
        )
    }

    override fun close(): Job {
        return if (closed.compareAndSet(expect = false, update = true)) {
            requestControl.close()
            threads.confineLaunch {
                Camera2Logger.debug { "Closing $this" }
                sessionProcessor?.setCaptureSessionRequestProcessor(null)
                useCaseCameraContext.closeGraph()
                useCaseSurfaceManager.stopAsync().await()
            }
        } else {
            CompletableDeferred(Unit)
        }
    }

    override fun setActiveResumeMode(enabled: Boolean) {
        threads.confineLaunch {
            if (closed.value) {
                Camera2Logger.debug {
                    "UseCaseCamera is closed before setActiveResumeMode, skipping setup."
                }
                return@confineLaunch
            }
            useCaseCameraContext.graph.isForeground = enabled
        }
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
