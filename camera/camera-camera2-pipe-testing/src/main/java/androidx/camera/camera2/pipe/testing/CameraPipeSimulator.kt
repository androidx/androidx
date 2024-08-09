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
import kotlinx.atomicfu.atomic
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
    private val fakeCameraBackend: FakeCameraBackend,
    public val fakeSurfaces: FakeSurfaces,
    public val fakeImageReaders: FakeImageReaders,
    public val fakeImageSources: FakeImageSources,
) : CameraPipe, AutoCloseable {
    private val closed = atomic(false)
    private val _cameraGraphs = mutableListOf<CameraGraphSimulator>()
    public val cameraGraphs: List<CameraGraphSimulator>
        get() = _cameraGraphs

    override fun create(config: CameraGraph.Config): CameraGraphSimulator {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }

        val cameraGraph = cameraPipeInternal.create(config)
        val fakeCameraController =
            checkNotNull(fakeCameraBackend.cameraControllers.lastOrNull()) {
                "Expected cameraPipe.create to create a CameraController instance from " +
                    "$fakeCameraBackend as part of its initialization."
            }
        val cameraMetadata = cameraPipeInternal.cameras().awaitCameraMetadata(config.camera)!!
        val cameraGraphSimulator =
            CameraGraphSimulator(
                cameraMetadata,
                fakeCameraController,
                fakeImageReaders,
                fakeImageSources,
                cameraGraph,
                config,
            )
        _cameraGraphs.add(cameraGraphSimulator)
        return cameraGraphSimulator
    }

    override fun createCameraGraphs(
        config: CameraGraph.ConcurrentConfig
    ): List<CameraGraphSimulator> {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        return config.graphConfigs.map { create(it) }
    }

    override fun cameras(): CameraDevices = cameraPipeInternal.cameras()

    override fun cameraSurfaceManager(): CameraSurfaceManager =
        cameraPipeInternal.cameraSurfaceManager()

    override var globalAudioRestrictionMode: AudioRestrictionMode
        get() = cameraPipeInternal.globalAudioRestrictionMode
        set(value) {
            cameraPipeInternal.globalAudioRestrictionMode = value
        }

    /** Directly create and return a new [CameraGraph] and [CameraGraphSimulator]. */
    public fun createCameraGraphSimulator(graphConfig: CameraGraph.Config): CameraGraphSimulator {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        val cameraGraph = cameraPipeInternal.create(graphConfig)
        val cameraController =
            fakeCameraBackend.cameraControllers.first { it.cameraGraphId == cameraGraph.id }
        val cameraGraphSimulator =
            createCameraGraphSimulator(cameraGraph, graphConfig, cameraController)
        _cameraGraphs.add(cameraGraphSimulator)
        return cameraGraphSimulator
    }

    /** Directly create and return a new set of [CameraGraph]s and [CameraGraphSimulator]s. */
    public fun createCameraGraphSimulators(
        config: CameraGraph.ConcurrentConfig
    ): List<CameraGraphSimulator> = config.graphConfigs.map { createCameraGraphSimulator(it) }

    private fun createCameraGraphSimulator(
        graph: CameraGraph,
        graphConfig: CameraGraph.Config,
        cameraController: CameraControllerSimulator
    ): CameraGraphSimulator {
        check(!closed.value) { "Cannot interact with CameraPipeSimulator after close!" }
        val cameraId = cameraController.cameraId
        val cameraMetadata = fakeCameraBackend.awaitCameraMetadata(cameraController.cameraId)
        checkNotNull(cameraMetadata) { "Failed to retrieve metadata for $cameraId!" }

        val cameraGraphSimulator =
            CameraGraphSimulator(
                cameraMetadata,
                cameraController,
                fakeImageReaders,
                fakeImageSources,
                graph,
                graphConfig,
            )
        return cameraGraphSimulator
    }

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

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            fakeSurfaces.close()
        }
    }

    override fun toString(): String {
        return "CameraPipeSimulator($cameraPipeInternal)"
    }

    public companion object {
        public fun create(
            testScope: TestScope,
            testContext: Context,
            fakeCameras: List<CameraMetadata> = listOf(FakeCameraMetadata())
        ): CameraPipeSimulator {
            val fakeCameraBackend =
                FakeCameraBackend(fakeCameras = fakeCameras.associateBy { it.camera })

            val testScopeDispatcher =
                StandardTestDispatcher(testScope.testScheduler, "CXCP-TestScope")
            val testScopeThreadConfig =
                CameraPipe.ThreadConfig(
                    testOnlyDispatcher = testScopeDispatcher,
                    testOnlyScope = testScope,
                )

            val fakeSurfaces = FakeSurfaces()
            val fakeImageReaders = FakeImageReaders(fakeSurfaces)
            val fakeImageSources = FakeImageSources(fakeImageReaders)

            val cameraPipe =
                CameraPipe(
                    CameraPipe.Config(
                        testContext,
                        cameraBackendConfig =
                            CameraBackendConfig(internalBackend = fakeCameraBackend),
                        threadConfig = testScopeThreadConfig,
                        imageSources = fakeImageSources
                    )
                )
            return CameraPipeSimulator(
                cameraPipe,
                fakeCameraBackend,
                fakeSurfaces,
                fakeImageReaders,
                fakeImageSources
            )
        }
    }
}
