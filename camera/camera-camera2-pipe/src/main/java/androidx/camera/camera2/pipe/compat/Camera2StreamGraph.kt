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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.InputStream
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.OutputStream.Config.ExternalOutputConfig
import androidx.camera.camera2.pipe.OutputStream.Config.LazyOutputConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.core.Log
import kotlinx.atomicfu.atomic
import javax.inject.Inject

private val streamIds = atomic(0)
internal fun nextStreamId(): StreamId = StreamId(streamIds.incrementAndGet())

private val outputIds = atomic(0)
internal fun nextOutputId(): OutputId = OutputId(outputIds.incrementAndGet())

private val configIds = atomic(0)
internal fun nextConfigId(): CameraConfigId = CameraConfigId(configIds.incrementAndGet())

private val groupIds = atomic(0)
internal fun nextGroupId(): Int = groupIds.incrementAndGet()

@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
internal inline class CameraConfigId(val value: Int) {
    override fun toString(): String = "OutputConfig-$value"
}

/**
 * This object keeps track of which surfaces have been configured for each stream. In addition,
 * it will keep track of which surfaces have changed or replaced so that the CaptureSession can be
 * reconfigured if the configured surfaces change.
 */
@CameraGraphScope
internal class Camera2StreamGraph @Inject constructor(
    cameraMetadata: CameraMetadata,
    graphConfig: CameraGraph.Config
) : StreamGraph {
    private val surfaceMap: MutableMap<StreamId, Surface> = mutableMapOf()
    private val _streamMap: Map<CameraStream.Config, CameraStream>

    internal val outputConfigs: List<OutputConfig>

    // TODO: Build InputStream(s)
    override val input: InputStream? = null
    override val streams: List<CameraStream>
    override val outputs: List<OutputStream>

    override fun get(config: CameraStream.Config): CameraStream? = _streamMap[config]

    init {
        val outputConfigListBuilder = mutableListOf<OutputConfig>()
        val outputConfigMap = mutableMapOf<OutputStream.Config, OutputConfig>()

        val streamListBuilder = mutableListOf<CameraStream>()
        val streamMapBuilder = mutableMapOf<CameraStream.Config, CameraStream>()

        val deferredOutputsAllowed = computeIfDeferredStreamsAreSupported(
            cameraMetadata,
            graphConfig
        )

        // Compute groupNumbers for buffer sharing.
        val groupNumbers = mutableMapOf<CameraStream.Config, Int>()
        for (group in graphConfig.streamSharingGroups) {
            check(group.size > 1)
            val surfaceGroupId = computeNextSurfaceGroupId(graphConfig)
            for (config in group) {
                check(!groupNumbers.containsKey(config))
                groupNumbers[config] = surfaceGroupId
            }
        }

        // Create outputConfigs. If outputs are shared there can be fewer entries in map than there
        // are streams.
        for (streamConfig in graphConfig.streams) {
            for (output in streamConfig.outputs) {
                if (outputConfigMap.containsKey(output)) {
                    continue
                }

                @SuppressWarnings("SyntheticAccessor")
                val outputConfig = OutputConfig(
                    nextConfigId(),
                    output.size,
                    output.format,
                    output.camera ?: graphConfig.camera,
                    groupNumber = groupNumbers[streamConfig],
                    deferredOutputType = if (deferredOutputsAllowed) {
                        (output as? LazyOutputConfig)?.outputType
                    } else {
                        null
                    },
                    externalOutputConfig = (output as? ExternalOutputConfig)?.output
                )
                outputConfigMap[output] = outputConfig
                outputConfigListBuilder.add(outputConfig)
            }
        }

        // Build the streams
        for (streamConfigIdx in graphConfig.streams.indices) {
            val streamConfig = graphConfig.streams[streamConfigIdx]

            val outputs = streamConfig.outputs.map {
                val outputConfig = outputConfigMap[it]!!

                @SuppressWarnings("SyntheticAccessor")
                val outputStream = OutputStreamImpl(
                    nextOutputId(),
                    outputConfig.size,
                    outputConfig.format,
                    outputConfig.camera
                )
                outputStream
            }

            val stream = CameraStream(nextStreamId(), outputs)
            streamMapBuilder[streamConfig] = stream
            streamListBuilder.add(stream)
            for (output in outputs) {
                output.stream = stream
            }
            for (cameraOutputConfig in streamConfig.outputs) {
                outputConfigMap[cameraOutputConfig]!!.streamBuilder.add(stream)
            }
        }

        // TODO: Sort outputs by type to try and put the viewfinder output first in the list
        //   This is important as some devices assume that the first surface is the viewfinder and
        //   will treat it differently.

        streams = streamListBuilder
        _streamMap = streamMapBuilder
        outputs = streams.flatMap { it.outputs }
        outputConfigs = outputConfigListBuilder
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
        for (outputConfig in outputConfigs) {
            for (stream in outputConfig.streamBuilder) {
                val surface = surfaceMap[stream.id]
                if (surface == null) {
                    if (!outputConfig.deferrable) {
                        return
                    }
                } else {
                    surfaces[stream.id] = surface
                }
            }
        }

        if (surfaces.isEmpty()) {
            return
        }

        surfaceListener.onSurfaceMapUpdated(surfaces)
    }

    @Suppress("SyntheticAccessor") // StreamId generates a synthetic constructor
    class OutputConfig(
        val id: CameraConfigId,
        val size: Size,
        val format: StreamFormat,
        val camera: CameraId,
        val groupNumber: Int?,
        val externalOutputConfig: OutputConfiguration?,
        val deferredOutputType: OutputStream.OutputType?,
    ) {
        internal val streamBuilder = mutableListOf<CameraStream>()
        val streams: List<CameraStream>
            get() = streamBuilder
        val deferrable: Boolean
            get() = deferredOutputType != null
        val surfaceSharing = streamBuilder.size > 1
        override fun toString(): String = id.toString()
    }

    @Suppress("SyntheticAccessor") // OutputId generates a synthetic constructor
    private class OutputStreamImpl(
        override val id: OutputId,
        override val size: Size,
        override val format: StreamFormat,
        override val camera: CameraId,
    ) : OutputStream {
        override lateinit var stream: CameraStream
        override fun toString(): String = id.toString()
    }

    interface SurfaceListener {
        fun onSurfaceMapUpdated(surfaces: Map<StreamId, Surface>)
    }

    private fun computeNextSurfaceGroupId(graphConfig: CameraGraph.Config): Int {
        // If there are any existing surfaceGroups, make sure the groups we define do not overlap
        // with any existing values.
        val existingGroupNumbers: List<Int> = readExistingGroupNumbers(graphConfig.streams)

        // Loop until we produce a groupId that was not already used.
        var number = nextGroupId()
        while (existingGroupNumbers.contains(number)) {
            number = nextGroupId()
        }
        return number
    }

    private fun readExistingGroupNumbers(outputs: List<CameraStream.Config>): List<Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outputs
                .flatMap { it.outputs }
                .filterIsInstance<ExternalOutputConfig>()
                .fold(mutableListOf()) { values, config ->
                    val groupId = Api24Compat.getSurfaceGroupId(config.output)
                    if (!values.contains(groupId)) {
                        values.add(groupId)
                    }
                    values
                }
        } else {
            emptyList()
        }
    }

    private fun computeIfDeferredStreamsAreSupported(
        cameraMetadata: CameraMetadata,
        graphConfig: CameraGraph.Config
    ): Boolean {
        val hardwareLevel = cameraMetadata[INFO_SUPPORTED_HARDWARE_LEVEL]
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            graphConfig.sessionMode == CameraGraph.OperatingMode.NORMAL &&
            hardwareLevel != INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY &&
            hardwareLevel != INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                    hardwareLevel != INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
                )
    }
}
