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

package androidx.camera.camera2.config

import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.compat.workaround.CapturePipelineTorchCorrection
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.impl.CameraGraphConfigProvider
import androidx.camera.camera2.impl.CapturePipeline
import androidx.camera.camera2.impl.CapturePipelineImpl
import androidx.camera.camera2.impl.GraphConfigBundle
import androidx.camera.camera2.impl.UseCaseCamera
import androidx.camera.camera2.impl.UseCaseCameraImpl
import androidx.camera.camera2.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionProcessor
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Provider
import javax.inject.Scope

@Scope public annotation class UseCaseCameraScope

/** Dependency bindings for building a [UseCaseCamera] */
@Module(
    includes = [UseCaseCameraImpl.Bindings::class, UseCaseCameraRequestControlImpl.Bindings::class]
)
public abstract class UseCaseCameraModule {
    // Used for dagger provider methods that are static.
    public companion object {

        @UseCaseCameraScope
        @Provides
        public fun provideCapturePipeline(
            capturePipelineImplProvider: Provider<CapturePipelineImpl>,
            capturePipelineTorchCorrectionProvider: Provider<CapturePipelineTorchCorrection>,
        ): CapturePipeline {
            if (CapturePipelineTorchCorrection.isEnabled) {
                return capturePipelineTorchCorrectionProvider.get()
            }

            return capturePipelineImplProvider.get()
        }
    }
}

/** Dagger module for binding the [UseCase]'s to the [UseCaseCamera]. */
@Module
public data class UseCaseCameraConfig(
    private val cameraGraphFactory: (CameraGraph.Config) -> CameraGraph,
    private val graphStateToCameraStateAdapter: GraphStateToCameraStateAdapter,
    private val sessionConfigAdapter: SessionConfigAdapter,
    private val sessionProcessor: SessionProcessor?,
    private val lazyGraphConfigBundle: Lazy<GraphConfigBundle>,
) {
    public val cameraGraphConfig: CameraGraph.Config
        get() = lazyGraphConfigBundle.value.graphConfig

    @UseCaseCameraScope
    @Provides
    public fun provideSessionConfigAdapter(): SessionConfigAdapter {
        return sessionConfigAdapter
    }

    @UseCaseCameraScope
    @Provides
    public fun provideSessionProcessor(): SessionProcessor? {
        return sessionProcessor
    }

    /**
     * [UseCaseCameraContext] would store the CameraGraph and related surface map that would be used
     * for [UseCaseCamera].
     */
    @UseCaseCameraScope
    @Provides
    public fun provideUseCaseCameraContext(
        cameraStateAdapter: CameraStateAdapter
    ): UseCaseCameraContext {
        Camera2Logger.debug { "Prepared UseCaseCameraContext (Deferred)" }
        return UseCaseCameraContext(
            cameraGraphProvider = { cameraGraphFactory(cameraGraphConfig) },
            cameraStateAdapter = cameraStateAdapter,
            streamConfigMapProvider = { lazyGraphConfigBundle.value.streamToSurfaceMap },
            graphStateToCameraStateAdapter = graphStateToCameraStateAdapter,
        )
    }

    /**
     * Manually implemented equals() to ignore [lazyGraphConfigBundle] and [cameraGraphFactory].
     *
     * The [lazyGraphConfigBundle] is an object instance that uses reference equality. If we use the
     * default data class equals, two configs with identical inputs will never be equal because the
     * lazy instances are different objects.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UseCaseCameraConfig

        if (sessionConfigAdapter != other.sessionConfigAdapter) return false
        if (graphStateToCameraStateAdapter != other.graphStateToCameraStateAdapter) return false
        if (sessionProcessor != other.sessionProcessor) return false

        // Intentionally exclude:
        // 1. lazyCreationResult: It is a stateful wrapper, not a configuration input.
        // 2. cameraGraphFactory: Lambdas generally do not support value equality.
        return true
    }

    override fun hashCode(): Int {
        var result = sessionConfigAdapter.hashCode()
        result = 31 * result + graphStateToCameraStateAdapter.hashCode()
        result = 31 * result + (sessionProcessor?.hashCode() ?: 0)
        // Intentionally exclude lazyCreationResult and cameraGraphFactory from hash
        return result
    }

    public companion object {
        public fun create(
            sessionConfigAdapter: SessionConfigAdapter,
            cameraGraphConfigProvider: CameraGraphConfigProvider,
            cameraGraphFactory: (CameraGraph.Config) -> CameraGraph,
            cameraStateAdapter: CameraStateAdapter,
            sessionProcessor: SessionProcessor?,
            extensionMode: Int?,
        ): UseCaseCameraConfig {
            val graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter)
            // We use a lazy value here to defer the heavy creation of CameraGraph.Config.
            // Importantly, we pass this lazy instance into the constructor.
            // This ensures that if this data class is copied (via .copy()), the *same*
            // lazy instance (and its cached result) is carried over to the new object,
            // preserving the identity of the StreamIds within the GraphConfig.
            val lazyGraphConfigBundle = lazy {
                val sessionConfig = sessionConfigAdapter.getValidSessionConfigOrNull()

                val operatingMode =
                    when {
                        extensionMode != null -> OperatingMode.EXTENSION
                        sessionConfig == null -> OperatingMode.NORMAL
                        sessionConfig.sessionType == SESSION_HIGH_SPEED -> OperatingMode.HIGH_SPEED
                        sessionConfig.sessionType == SESSION_REGULAR -> OperatingMode.NORMAL
                        else -> OperatingMode.custom(sessionConfig.sessionType)
                    }

                cameraGraphConfigProvider.create(
                    operatingMode = operatingMode,
                    sessionConfig = sessionConfig,
                    setOutputType = false,
                    graphStateToCameraStateAdapter = graphStateToCameraStateAdapter,
                    camera2ExtensionMode = extensionMode,
                    surfaceToStreamUseCaseMap = sessionConfigAdapter.surfaceToStreamUseCaseMap,
                    surfaceToStreamUseHintMap = sessionConfigAdapter.surfaceToStreamUseHintMap,
                )
            }

            return UseCaseCameraConfig(
                sessionConfigAdapter = sessionConfigAdapter,
                graphStateToCameraStateAdapter = graphStateToCameraStateAdapter,
                cameraGraphFactory = cameraGraphFactory,
                sessionProcessor = sessionProcessor,
                lazyGraphConfigBundle = lazyGraphConfigBundle,
            )
        }
    }
}

public class UseCaseCameraContext(
    private val cameraGraphProvider: Provider<CameraGraph>,
    private val cameraStateAdapter: CameraStateAdapter,
    private val graphStateToCameraStateAdapter: GraphStateToCameraStateAdapter,
    private val streamConfigMapProvider: Provider<Map<CameraStream.Config, DeferrableSurface>>,
    defaultSurfaceToStreamMap: Map<DeferrableSurface, StreamId>? = null,
) {
    private val _graph = lazy { cameraGraphProvider.get() }

    public val graph: CameraGraph by _graph

    public val surfaceToStreamMap: Map<DeferrableSurface, StreamId> by lazy {
        defaultSurfaceToStreamMap
            ?: run {
                val map = mutableMapOf<DeferrableSurface, StreamId>()
                streamConfigMapProvider.get().forEach { (streamConfig, deferrableSurface) ->
                    graph.streams[streamConfig]?.let { map[deferrableSurface] = it.id }
                }
                map.toMap()
            }
    }

    public fun closeGraph() {
        if (_graph.isInitialized()) {
            val initializedGraph = graph
            initializedGraph.close()
            cameraStateAdapter.onGraphClosed(initializedGraph)
        }
    }

    public fun getStreamIdsFromSurfaces(
        deferrableSurfaces: Collection<DeferrableSurface>
    ): Set<StreamId> {
        val streamIds = mutableSetOf<StreamId>()
        deferrableSurfaces.forEach {
            surfaceToStreamMap[it]?.let { streamId -> streamIds.add(streamId) }
        }
        return streamIds
    }

    public fun configureCameraStateListener() {
        graphStateToCameraStateAdapter.cameraGraph = graph
        cameraStateAdapter.onGraphUpdated(graph)
    }

    public suspend inline fun <T> useGraphSession(block: (CameraGraph.Session) -> T): T {
        return graph.acquireSession().use { block(it) }
    }
}

/** Dagger subcomponent for a single [UseCaseCamera] instance. */
@UseCaseCameraScope
@Subcomponent(modules = [UseCaseCameraModule::class, UseCaseCameraConfig::class])
public interface UseCaseCameraComponent {
    public fun getUseCaseCamera(): UseCaseCamera

    public fun getUseCaseCameraContext(): UseCaseCameraContext

    @Subcomponent.Builder
    public interface Builder {
        public fun config(config: UseCaseCameraConfig): Builder

        public fun build(): UseCaseCameraComponent
    }
}
