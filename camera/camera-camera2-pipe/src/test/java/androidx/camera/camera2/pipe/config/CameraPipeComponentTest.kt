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

package androidx.camera.camera2.pipe.config

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.compat.Camera2Backend
import androidx.camera.camera2.pipe.compat.Camera2DeviceCache
import androidx.camera.camera2.pipe.compat.Camera2MetadataCache
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class CameraPipeComponentTest {
    private val fakeCameraId = RobolectricCameras.create()

    @Test
    fun createCameraPipeComponent() {
        val context = ApplicationProvider.getApplicationContext() as Context
        assertThat(context).isNotNull()

        val builder = DaggerCameraPipeComponent.builder()
        val config = CameraPipe.Config(context)
        val module = CameraPipeConfigModule(config)
        builder.cameraPipeConfigModule(module)
        builder.threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
        val component = builder.build()
        assertThat(component).isNotNull()
    }

    @Test
    fun createCameraGraphComponent() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val component =
            DaggerCameraPipeComponent.builder()
                .cameraPipeConfigModule(CameraPipeConfigModule(CameraPipe.Config(context)))
                .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
                .build()

        val cameraId = fakeCameraId
        val config = CameraGraph.Config(camera = cameraId, streams = listOf())
        val module = CameraGraphConfigModule(config, CameraGraphId.nextId())
        val builder = component.cameraGraphComponentBuilder()
        builder.cameraGraphConfigModule(module)
        val graphComponent = builder.build()
        assertThat(graphComponent).isNotNull()
    }

    @Test
    fun createCameraGraph() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val component =
            DaggerCameraPipeComponent.builder()
                .cameraPipeConfigModule(CameraPipeConfigModule(CameraPipe.Config(context)))
                .threadConfigModule(ThreadConfigModule(CameraPipe.ThreadConfig()))
                .build()

        val graphComponent =
            component
                .cameraGraphComponentBuilder()
                .cameraGraphConfigModule(
                    CameraGraphConfigModule(
                        CameraGraph.Config(camera = fakeCameraId, streams = listOf()),
                        CameraGraphId.nextId(),
                    )
                )
                .build()

        val graph = graphComponent.cameraGraph()
        assertThat(graph).isNotNull()
    }

    @Test
    fun createCameraControllerRequestsNewBuilderEachTime() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val threads = FakeThreads.fromTestScope(TestScope())
        val metadataCache =
            Camera2MetadataCache(context, threads, Permissions(context), SystemTimeSource())
        val cameraPipeJob = Job()
        val deviceCache =
            Camera2DeviceCache(
                Provider { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager },
                metadataCache,
                threads,
                context,
                context.packageManager,
                cameraErrorListener = mock(),
                cameraDeviceSetupCompatFactoryProvider = mock(),
                CameraPipeLifetime(cameraPipeJob),
                cameraPipeJob,
            )
        val createdBuilders = mutableListOf<Camera2ControllerComponent.Builder>()
        val builderProvider =
            Provider<Camera2ControllerComponent.Builder> {
                object : Camera2ControllerComponent.Builder {
                        override fun camera2ControllerConfig(
                            config: Camera2ControllerConfig
                        ): Camera2ControllerComponent.Builder = this

                        override fun build(): Camera2ControllerComponent =
                            object : Camera2ControllerComponent {
                                override fun cameraController(): CameraController = mock()
                            }
                    }
                    .also { createdBuilders.add(it) }
            }
        val backend =
            Camera2Backend(
                threads = threads,
                camera2DeviceCache = deviceCache,
                camera2MetadataCache = metadataCache,
                camera2DeviceManager = mock(),
                camera2CameraControllerComponent = builderProvider,
                cameraPipeContext = context,
            )

        assertThat(createdBuilders).isEmpty()

        val graphConfig = CameraGraph.Config(camera = fakeCameraId, streams = listOf())
        val streamGraph =
            StreamGraphImpl(
                cameraMetadata = FakeCameraMetadata(),
                graphConfig = graphConfig,
                imageSources = mock(),
                cameraControllerProvider = { mock() },
                memoryEstimator = mock(),
            )
        backend.createCameraController(
            cameraContext = mock(),
            graphId = CameraGraphId.nextId(),
            graphConfig = graphConfig,
            graphListener = mock(),
            streamGraph = streamGraph,
            surfaceTracker = mock(),
        )
        backend.createCameraController(
            cameraContext = mock(),
            graphId = CameraGraphId.nextId(),
            graphConfig = graphConfig,
            graphListener = mock(),
            streamGraph = streamGraph,
            surfaceTracker = mock(),
        )

        assertThat(createdBuilders).hasSize(2)
        assertThat(createdBuilders[0]).isNotSameInstanceAs(createdBuilders[1])
    }
}
