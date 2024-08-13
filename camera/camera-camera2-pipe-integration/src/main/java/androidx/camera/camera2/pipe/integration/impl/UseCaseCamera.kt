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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.adapter.RequestProcessorAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.core.UseCase
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessorSurface
import dagger.Binds
import dagger.Module
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal val useCaseCameraIds = atomic(0)
internal val defaultOptionPriority = Config.OptionPriority.OPTIONAL
internal const val defaultTemplate = CameraDevice.TEMPLATE_PREVIEW

@JvmDefaultWithCompatibility
public interface UseCaseCamera {
    // UseCases
    public var runningUseCases: Set<UseCase>

    public var isPrimary: Boolean

    public interface RunningUseCasesChangeListener {
        /** Invoked when value of [UseCaseCamera.runningUseCases] has been changed. */
        public fun onRunningUseCasesChanged()
    }

    // RequestControl of the UseCaseCamera
    public val requestControl: UseCaseCameraRequestControl

    // Parameters
    public fun <T> setParameterAsync(
        key: CaptureRequest.Key<T>,
        value: T,
        priority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

    public fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        priority: Config.OptionPriority = defaultOptionPriority,
    ): Deferred<Unit>

    public fun setActiveResumeMode(enabled: Boolean) {}

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
    private val controls: java.util.Set<UseCaseCameraControl>,
    private val useCaseGraphConfig: UseCaseGraphConfig,
    private val useCases: java.util.ArrayList<UseCase>,
    private val useCaseSurfaceManager: UseCaseSurfaceManager,
    private val threads: UseCaseThreads,
    private val sessionProcessorManager: SessionProcessorManager?,
    private val sessionConfigAdapter: SessionConfigAdapter,
    override val requestControl: UseCaseCameraRequestControl,
) : UseCaseCamera {
    private val debugId = useCaseCameraIds.incrementAndGet()
    private val closed = atomic(false)

    override var runningUseCases: Set<UseCase> = setOf<UseCase>()
        set(value) {
            field = value

            // Note: This may be called with the same set of values that was previously set. This
            // is used as a signal to indicate the properties of the UseCase may have changed.
            SessionConfigAdapter(value, isPrimary = isPrimary).getValidSessionConfigOrNull()?.let {
                requestControl.setSessionConfigAsync(it)
            }
                ?: run {
                    debug { "Unable to reset the session due to invalid config" }
                    requestControl.setSessionConfigAsync(
                        SessionConfig.Builder().apply { setTemplateType(defaultTemplate) }.build()
                    )
                }

            controls.forEach { control ->
                if (control is UseCaseCamera.RunningUseCasesChangeListener) {
                    control.onRunningUseCasesChanged()
                }
            }
        }

    override var isPrimary: Boolean = true
        set(value) {
            field = value
        }

    init {
        debug { "Configured $this for $useCases" }
        useCaseGraphConfig.apply { cameraStateAdapter.onGraphUpdated(graph) }
        threads.scope.launch {
            useCaseGraphConfig.apply {
                graph.graphState.collect {
                    cameraStateAdapter.onGraphStateUpdated(graph, it)

                    // Even if the UseCaseCamera is closed, we should still update the GraphState
                    // before cancelling the job, because it could be the last UseCaseCamera created
                    // (i.e., no new UseCaseCamera to update CameraStateAdapter that this one as
                    // stopped/closed).
                    if (closed.value && it is GraphStateStopped || it is GraphStateError) {
                        this@launch.coroutineContext[Job]?.cancel()
                    }

                    // TODO: b/323614735: Technically our RequestProcessor implementation could be
                    //   given to the SessionProcessor through onCaptureSessionStart after the
                    //   new set of configurations (CameraGraph) is created. However, this seems to
                    //   be causing occasional SIGBUS on the Android platform level. Delaying this
                    //   seems to be mitigating the issue, but does result in overhead in startup
                    //   latencies. Move this back to UseCaseManager once we understand more about
                    //   the situation.
                    if (sessionProcessorManager != null && it is GraphStateStarted) {
                        val sessionProcessorSurfaces =
                            sessionConfigAdapter.deferrableSurfaces.map {
                                it as SessionProcessorSurface
                            }
                        val requestProcessorAdapter =
                            RequestProcessorAdapter(
                                useCaseGraphConfig,
                                sessionProcessorSurfaces,
                                threads.scope,
                            )
                        sessionProcessorManager.onCaptureSessionStart(requestProcessorAdapter)
                    }
                }
            }
        }
    }

    override fun close(): Job {
        return if (closed.compareAndSet(expect = false, update = true)) {
            threads.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                debug { "Closing $this" }
                requestControl.close()
                sessionProcessorManager?.prepareClose()
                useCaseGraphConfig.graph.close()
                if (sessionProcessorManager != null) {
                    useCaseGraphConfig.graph.graphState.first {
                        it is GraphStateStopped || it is GraphStateError
                    }
                    sessionProcessorManager.close()
                }
                useCaseSurfaceManager.stopAsync().await()
            }
        } else {
            CompletableDeferred(Unit)
        }
    }

    override fun <T> setParameterAsync(
        key: CaptureRequest.Key<T>,
        value: T,
        priority: Config.OptionPriority,
    ): Deferred<Unit> =
        runIfNotClosed { setParametersAsync(mapOf(key to (value as Any)), priority) }
            ?: canceledResult

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        priority: Config.OptionPriority,
    ): Deferred<Unit> =
        runIfNotClosed {
            requestControl.addParametersAsync(values = values, optionPriority = priority)
        } ?: canceledResult

    override fun setActiveResumeMode(enabled: Boolean) {
        useCaseGraphConfig.graph.isForeground = enabled
    }

    private fun UseCaseCameraRequestControl.setSessionConfigAsync(
        sessionConfig: SessionConfig
    ): Deferred<Unit> =
        runIfNotClosed {
            setConfigAsync(
                type = UseCaseCameraRequestControl.Type.SESSION_CONFIG,
                config = sessionConfig.implementationOptions,
                tags = sessionConfig.repeatingCaptureConfig.tagBundle.toMap(),
                listeners =
                    setOf(
                        CameraCallbackMap.createFor(
                            sessionConfig.repeatingCameraCaptureCallbacks,
                            threads.backgroundExecutor
                        )
                    ),
                template = RequestTemplate(sessionConfig.repeatingCaptureConfig.templateType),
                streams =
                    useCaseGraphConfig.getStreamIdsFromSurfaces(
                        sessionConfig.repeatingCaptureConfig.surfaces
                    ),
                sessionConfig = sessionConfig,
            )
        } ?: canceledResult

    private inline fun <R> runIfNotClosed(crossinline block: () -> R): R? {
        return if (!closed.value) block() else null
    }

    override fun toString(): String = "UseCaseCamera-$debugId"

    @Module
    public abstract class Bindings {
        @UseCaseCameraScope
        @Binds
        public abstract fun provideUseCaseCamera(useCaseCamera: UseCaseCameraImpl): UseCaseCamera
    }

    public companion object {
        private val canceledResult = CompletableDeferred<Unit>().apply { cancel() }
    }
}
