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

package androidx.camera.camera2.pipe.impl

import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.Stream
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StreamType
import kotlinx.atomicfu.atomic
import javax.inject.Inject

private val streamIds = atomic(0)
internal fun nextStreamId(): StreamId = StreamId(streamIds.incrementAndGet())

/**
 * This object keeps track of which surfaces have been configured for each stream. In addition,
 * it will keep track of which surfaces have changed or replaced so that the CaptureSession can be
 * reconfigured if the configured surfaces change.
 */
@CameraGraphScope
class StreamMap @Inject constructor(graphConfig: CameraGraph.Config) {
    private val activeSurfaceMap: MutableMap<StreamId, Surface> = mutableMapOf()
    val streamConfigMap: Map<StreamConfig, Stream>

    init {
        val streamIdMapBuilder = mutableMapOf<StreamConfig, Stream>()
        for (streamConfig in graphConfig.streams) {
            // Using an inline class generates a synthetic constructor
            @Suppress("SyntheticAccessor")
            streamIdMapBuilder[streamConfig] = StreamImpl(
                nextStreamId(),
                streamConfig.size,
                streamConfig.format,
                streamConfig.camera,
                streamConfig.type
            )
        }
        streamConfigMap = streamIdMapBuilder
    }

    operator fun set(stream: StreamId, surface: Surface?) {
        Log.debug { "Configured $stream to use $surface" }
        if (surface == null) {
            // TODO: Tell the graph processor that it should resubmit the repeating request or
            //  reconfigure the camera2 captureSession
            activeSurfaceMap.remove(stream)
        } else {
            activeSurfaceMap[stream] = surface
        }
    }

    operator fun get(stream: StreamId): Surface? = activeSurfaceMap[stream]

    // Using an inline class generates a synthetic constructor
    @Suppress("SyntheticAccessor")
    data class StreamImpl(
        override val id: StreamId,
        override val size: Size,
        override val format: StreamFormat,
        override val camera: CameraId,
        override val type: StreamType
    ) : Stream
}