/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.content.Context
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraPipe.CameraBackendConfig
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.ConfigQueryResult
import androidx.camera.camera2.pipe.FrameGraph
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * This class enables a developer to simulate interactions with [CameraPipe].
 *
 * This simulator is a realistic fake of a real CameraPipe object with methods that enable a
 * developer to query and interact with the simulated camera subsystem(s). This is primarily used to
 * test complicated interactions with [CameraPipe] and [CameraGraph] and to simulate how code
 * responds to a range of behaviors by the underlying camera within unit tests.
 */
public class CameraPipeSimulator
private constructor(
    private val cameraPipeInternal: CameraPipe,
    public val fakeCameraBackend: FakeCameraBackend,
    public val fakeSurfaces: FakeSurfaces,
    public val fakeImageReaders: FakeImageReaders,
    public val fakeImageSources: FakeImageSources,
) : CameraPipe, AutoCloseable {
    private val closed = atomic(false)
    private val _cameraGraphs = mutableListOf<CameraGraphSimulator>()
    private val _frameGraphs = mutableListOf<FrameGraphSimulator>()
    private val _isConfigSupportedHistory = mutableListOf<CameraGraph.Config>()

    public val cameraGraphs: List<CameraGraphSimulator>
        get() = _cameraGraphs

    public val frameGraphs: List<FrameGraphSimulator>
        get() = _frameGraphs

    public val isConfigSupportedHistory: List<CameraGraph.Config>
        get() = _isConfigSupportedHistory

    @Deprecated(
        "Use createCameraGraph instead.",
        replaceWith = ReplaceWith("createCameraGraph(config)"),
    )
    override fun create(config: CameraGraph.Config): CameraGraphSimulator =
        createCameraGraph(config)

    override fun createCameraGraph(config: CameraGraph.Config): CameraGraphSimulator {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        val cameraGraph = cameraPipeInternal.createCameraGraph(config)

        return createCameraGraphSimulator(cameraGraphConfig = config, cameraGraph = cameraGraph)
    }

    override fun createCameraGraphs(
        config: CameraGraph.ConcurrentConfig
    ): List<CameraGraphSimulator> {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        return config.graphConfigs.map { createCameraGraph(it) }
    }

    override fun createFrameGraph(frameGraphConfig: FrameGraph.Config): FrameGraphSimulator {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        val frameGraph = cameraPipeInternal.createFrameGraph(frameGraphConfig)
        val cameraGraph = frameGraph.unwrapAs(CameraGraph::class)
        checkNotNull(cameraGraph) { "Failed to unwrap $frameGraph as a CameraGraph!" }

        val cameraGraphSimulator =
            createCameraGraphSimulator(
                cameraGraphConfig = frameGraphConfig.cameraGraphConfig,
                cameraGraph = cameraGraph,
            )

        val frameGraphSimulator = FrameGraphSimulator(frameGraph, cameraGraphSimulator)
        _frameGraphs.add(frameGraphSimulator)
        return frameGraphSimulator
    }

    override fun createFrameGraphs(
        frameGraphConfigs: FrameGraph.ConcurrentConfig
    ): List<FrameGraphSimulator> {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        return frameGraphConfigs.frameGraphConfigs.map { createFrameGraph(it) }
    }

    override fun cameras(): CameraDevices = cameraPipeInternal.cameras()

    override fun cameraSurfaceManager(): CameraSurfaceManager =
        cameraPipeInternal.cameraSurfaceManager()

    override suspend fun isConfigSupported(graphConfig: CameraGraph.Config): ConfigQueryResult {
        _isConfigSupportedHistory.add(graphConfig)
        return cameraPipeInternal.isConfigSupported(graphConfig)
    }

    override fun prewarmIsConfigSupported(graphConfig: CameraGraph.Config) {
        cameraPipeInternal.prewarmIsConfigSupported(graphConfig)
    }

    override var globalAudioRestrictionMode: AudioRestrictionMode
        get() = cameraPipeInternal.globalAudioRestrictionMode
        set(value) {
            cameraPipeInternal.globalAudioRestrictionMode = value
        }

    override fun shutdown() {
        // Nothing to shutdown
    }

    /** Directly create and return a new [CameraGraph] and [CameraGraphSimulator]. */
    public fun createCameraGraphSimulator(graphConfig: CameraGraph.Config): CameraGraphSimulator {
        return createCameraGraph(graphConfig)
    }

    /** Directly create and return a new set of [CameraGraph]s and [CameraGraphSimulator]s. */
    public fun createCameraGraphSimulators(
        config: CameraGraph.ConcurrentConfig
    ): List<CameraGraphSimulator> = createCameraGraphs(config)

    public fun checkImageReadersClosed() {
        fakeImageSources.checkImageSourcesClosed()
        fakeImageReaders.checkImageReadersClosed()
    }

    public fun checkImagesClosed() {
        fakeImageSources.checkImagesClosed()
        fakeImageReaders.checkImagesClosed()
    }

    public fun checkCameraGraphsClosed() {
        for (cameraGraph in _cameraGraphs) {
            check(cameraGraph.isClosed) { "$cameraGraph was not closed!" }
        }
    }

    private fun createCameraGraphSimulator(
        cameraGraphConfig: CameraGraph.Config,
        cameraGraph: CameraGraph,
    ): CameraGraphSimulator {

        val fakeCameraController =
            checkNotNull(fakeCameraBackend.cameraControllers.lastOrNull()) {
                "Expected CameraPipe.create to create a CameraController instance from " +
                    "$fakeCameraBackend as part of its initialization."
            }
        val cameraId = cameraGraphConfig.camera
        val cameraMetadata = cameraPipeInternal.cameras().awaitCameraMetadata(cameraId)
        checkNotNull(cameraMetadata) { "Failed to retrieve metadata for $cameraId!" }

        val cameraGraphSimulator =
            CameraGraphSimulator(
                cameraMetadata,
                fakeCameraController,
                fakeImageReaders,
                fakeImageSources,
                cameraGraph,
                cameraGraphConfig,
            )
        _cameraGraphs.add(cameraGraphSimulator)
        return cameraGraphSimulator
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            fakeSurfaces.close()
        }
    }

    override fun toString(): String {
        return "CameraPipeSimulator($cameraPipeInternal)"
    }

    public companion object {
        /**
         * Create a [CameraPipeSimulator] that intercepts nearly all interactions with the Camera
         * and allows those interactions to be precisely simulated.
         */
        public fun create(
            testContext: Context,
            testThreads: CameraPipe.ThreadConfig,
            testCameras: List<CameraMetadata>,
        ): CameraPipeSimulator {
            val fakeSurfaces = FakeSurfaces()
            val fakeImageReaders = FakeImageReaders(fakeSurfaces)
            val fakeImageSources = FakeImageSources(fakeImageReaders)
            val fakeCameraBackend =
                FakeCameraBackend(fakeCameras = testCameras.associateBy { it.camera })

            val cameraPipe =
                CameraPipe(
                    CameraPipe.Config(
                        testContext,
                        cameraBackendConfig =
                            CameraBackendConfig(internalBackend = fakeCameraBackend),
                        threadConfig = testThreads,
                        imageSources = fakeImageSources,
                    )
                )
            return CameraPipeSimulator(
                cameraPipe,
                fakeCameraBackend,
                fakeSurfaces,
                fakeImageReaders,
                fakeImageSources,
            )
        }

        public fun create(
            testScope: TestScope,
            testContext: Context,
            fakeCameras: List<CameraMetadata> = listOf(FakeCameraMetadata()),
        ): CameraPipeSimulator {
            val testScopeDispatcher =
                StandardTestDispatcher(testScope.testScheduler, "CXCP-TestScope")
            val testScopeExecutor = testScopeDispatcher.asExecutor()

            val testScopeThreadConfig =
                CameraPipe.ThreadConfig(
                    defaultLightweightExecutor = testScopeExecutor,
                    defaultBackgroundExecutor = testScopeExecutor,
                    defaultBlockingExecutor = testScopeExecutor,
                    defaultCameraExecutor = testScopeExecutor,
                    defaultCameraHandlerFn = {
                        throw IllegalStateException(
                            "Handler should never be accessed from simulator."
                        )
                    },
                    testOnlyScope = testScope,
                )
            return create(
                testContext = testContext,
                testThreads = testScopeThreadConfig,
                testCameras = fakeCameras,
            )
        }
    }
}
