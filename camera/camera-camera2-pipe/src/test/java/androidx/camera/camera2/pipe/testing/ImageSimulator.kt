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
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.media.ImageSource
import org.mockito.kotlin.mock

class ImageSimulator(
    streamConfigs: List<CameraStream.Config>,
    imageStreams: Set<CameraStream.Config>? = null,
    defaultCameraMetadata: CameraMetadata? = null,
    defaultStreamGraph: StreamGraph? = null
) : AutoCloseable {
    private val fakeSurfaces = FakeSurfaces()

    val cameraMetadata = defaultCameraMetadata ?: FakeCameraMetadata()
    val graphConfig = CameraGraph.Config(camera = cameraMetadata.camera, streams = streamConfigs)
    val streamGraph = defaultStreamGraph ?: StreamGraphImpl(cameraMetadata, graphConfig, mock())

    private val fakeImageSources = buildMap {
        for (config in graphConfig.streams) {
            if (imageStreams != null && !imageStreams.contains(config)) continue
            val cameraStream = streamGraph[config]!!
            val fakeImageSource =
                FakeImageSource(
                    cameraStream.id,
                    config.outputs.first().format,
                    cameraStream.outputs.associate { it.id to it.size }
                )
            check(this[cameraStream.id] == null)
            this[cameraStream.id] = fakeImageSource
        }
    }

    val imageSources: Map<StreamId, ImageSource> = fakeImageSources
    val imageStreams = imageSources.keys
    val streamToSurfaceMap = buildMap {
        for (config in graphConfig.streams) {
            val cameraStream = streamGraph[config]!!
            this[cameraStream.id] =
                imageSources[cameraStream.id]?.surface
                    ?: fakeSurfaces.createFakeSurface(cameraStream.outputs.first().size)
        }
    }

    fun simulateImage(streamId: StreamId, timestamp: Long, outputId: OutputId? = null): FakeImage {
        return fakeImageSources[streamId]!!.simulateImage(timestamp, outputId)
    }

    override fun close() {
        for (imageSource in fakeImageSources.values) {
            imageSource.close()
        }
        fakeSurfaces.close()
    }
}
