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

import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
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
class StreamMap @Inject constructor(
    cameraMetadata: CameraMetadata,
    graphConfig: CameraGraph.Config
) {
    private val surfaceMap: MutableMap<StreamId, Surface> = mutableMapOf()
    private val deferrableStreams: Set<StreamId>

    val streamConfigMap: Map<StreamConfig, Stream>

    init {
        val streamBuilder = mutableMapOf<StreamConfig, Stream>()
        val deferrableStreamBuilder = mutableSetOf<StreamId>()

        val hardwareLevel = cameraMetadata[INFO_SUPPORTED_HARDWARE_LEVEL]
        val deferredStreamsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            graphConfig.operatingMode == CameraGraph.OperatingMode.NORMAL &&
            hardwareLevel != INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY &&
            hardwareLevel != INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                    hardwareLevel != INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
                )

        for (streamConfig in graphConfig.streams) {
            // Using an inline class generates a synthetic constructor
            @Suppress("SyntheticAccessor")
            val stream = StreamImpl(
                nextStreamId(),
                streamConfig.size,
                streamConfig.format,
                streamConfig.camera,
                streamConfig.type
            )

            streamBuilder[streamConfig] = stream

            if (deferredStreamsSupported &&
                streamConfig.deferrable &&
                (
                    streamConfig.type == StreamType.SURFACE_TEXTURE ||
                        streamConfig.type == StreamType.SURFACE_VIEW
                    )
            ) {
                deferrableStreamBuilder.add(stream.id)
            }
        }
        deferrableStreams = deferrableStreamBuilder
        streamConfigMap = streamBuilder
    }

    private var _listener: SurfaceListener? = null
    var listener: SurfaceListener?
        get() = _listener
        set(value) {
            _listener = value
            if (value != null) {
                maybeUpdateSurfaces()
            }
        }

    operator fun set(stream: StreamId, surface: Surface?) {
        Log.info {
            if (surface != null) {
                "Configured $stream to use $surface"
            } else {
                "Removed surface for $stream"
            }
        }
        if (surface == null) {
            // TODO: Tell the graph processor that it should resubmit the repeating request or
            //  reconfigure the camera2 captureSession
            surfaceMap.remove(stream)
        } else {
            surfaceMap[stream] = surface
        }
        maybeUpdateSurfaces()
    }

    private fun maybeUpdateSurfaces() {
        val surfaceListener = _listener ?: return

        // Rules:
        // 1. In order to tell the captureSession that we have surfaces, we should wait until we
        //    have at least one valid surface.
        // 2. All streams that are not deferrable, must have a valid surface.

        val surfaces = mutableMapOf<StreamId, Surface>()
        for (stream in streamConfigMap) {
            val surface = surfaceMap[stream.value.id]

            if (surface == null) {
                if (!deferrableStreams.contains(stream.value.id)) {
                    // Break early if no surface is defined, and the stream is not deferrable.
                    return
                }
            } else {
                surfaces[stream.value.id] = surface
            }
        }

        if (surfaces.isEmpty()) {
            return
        }

        surfaceListener.setSurfaceMap(surfaces)
    }

    // Using an inline class generates a synthetic constructor
    @Suppress("SyntheticAccessor")
    data class StreamImpl(
        override val id: StreamId,
        override val size: Size,
        override val format: StreamFormat,
        override val camera: CameraId,
        override val type: StreamType
    ) : Stream {
        override fun toString(): String = id.toString()
    }
}