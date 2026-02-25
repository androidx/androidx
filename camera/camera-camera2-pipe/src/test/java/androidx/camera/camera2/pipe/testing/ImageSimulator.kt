/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import org.mockito.kotlin.mock

internal class ImageSimulator(
    streamConfigs: List<CameraStream.Config>,
    defaultCameraMetadata: CameraMetadata? = null,
) : AutoCloseable {
    private val fakeSurfaces = FakeSurfaces()
    private val fakeImageReaders = FakeImageReaders(fakeSurfaces)
    private val fakeImageSources = FakeImageSources(fakeImageReaders)

    val cameraMetadata = defaultCameraMetadata ?: FakeCameraMetadata()
    val graphConfig = CameraGraph.Config(camera = cameraMetadata.camera, streams = streamConfigs)

    val streamGraph = StreamGraphImpl(cameraMetadata, graphConfig, fakeImageSources, mock())

    val streamToSurfaceMap = buildMap {
        for (config in graphConfig.streams) {
            val cameraStream = streamGraph[config]!!
            this[cameraStream.id] =
                fakeImageSources[cameraStream.id]?.surface
                    ?: fakeSurfaces.createFakeSurface(cameraStream.outputs.first().size)
        }
    }

    fun simulateImage(streamId: StreamId, timestamp: Long, outputId: OutputId? = null): FakeImage {
        return fakeImageSources[streamId]!!.simulateImage(timestamp, outputId)
    }

    fun simulateExpectedOutputs(streamId: StreamId, timestamp: Long, outputIds: Set<OutputId>) {
        return fakeImageSources[streamId]!!.simulateExpectedOutputs(timestamp, outputIds)
    }

    override fun close() {
        fakeSurfaces.close()
    }
}
