/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelExternal
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLimited
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.InputStream
import androidx.camera.camera2.pipe.InputStreamId
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.Api24Compat
import androidx.camera.camera2.pipe.config.CameraGraphScope
import javax.inject.Inject
import kotlinx.atomicfu.atomic

/**
 * This object builds an internal graph of inputs and outputs from a graphConfig. It is responsible
 * for defining the identifiers for each input and output stream, and for building an abstract
 * representation of the internal camera output configuration(s).
 */
@CameraGraphScope
internal class StreamGraphImpl
@Inject
constructor(
    cameraMetadata: CameraMetadata,
    graphConfig: CameraGraph.Config,
) : StreamGraph {
    private val _streamMap: Map<CameraStream.Config, CameraStream>

    internal val outputConfigs: List<OutputConfig>

    // TODO: Build InputStream(s)
    override val inputs: List<InputStream>
    override val streams: List<CameraStream>
    override val streamIds: Set<StreamId>
    override val outputs: List<OutputStream>

    override fun get(config: CameraStream.Config): CameraStream? = _streamMap[config]

    init {
        val outputConfigListBuilder = mutableListOf<OutputConfig>()
        val outputConfigMap = mutableMapOf<OutputStream.Config, OutputConfig>()

        val streamListBuilder = mutableListOf<CameraStream>()
        val streamMapBuilder = mutableMapOf<CameraStream.Config, CameraStream>()

        val deferredOutputsAllowed =
            computeIfDeferredStreamsAreSupported(cameraMetadata, graphConfig)

        // Compute groupNumbers for buffer sharing.
        val groupNumbers = mutableMapOf<CameraStream.Config, Int>()
        for (group in graphConfig.exclusiveStreamGroups) {
            check(group.isNotEmpty())
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

                val outputConfig =
                    OutputConfig(
                        nextConfigId(),
                        output.size,
                        output.format,
                        output.camera ?: graphConfig.camera,
                        groupNumber = groupNumbers[streamConfig],
                        deferredOutputType =
                            if (deferredOutputsAllowed) {
                                (output as? OutputStream.Config.LazyOutputConfig)?.outputType
                            } else {
                                null
                            },
                        mirrorMode = output.mirrorMode,
                        timestampBase = output.timestampBase,
                        dynamicRangeProfile = output.dynamicRangeProfile,
                        streamUseCase = output.streamUseCase,
                        streamUseHint = output.streamUseHint,
                        sensorPixelModes = output.sensorPixelModes,
                        externalOutputConfig = getOutputConfigurationOrNull(output),
                    )
                outputConfigMap[output] = outputConfig
                outputConfigListBuilder.add(outputConfig)
            }
        }

        // Build the streams
        for (streamConfigIdx in graphConfig.streams.indices) {
            val streamConfig = graphConfig.streams[streamConfigIdx]

            val outputs =
                streamConfig.outputs.map {
                    val outputConfig = outputConfigMap[it]!!

                    val outputStream =
                        OutputStreamImpl(
                            nextOutputId(),
                            outputConfig.size,
                            outputConfig.format,
                            outputConfig.camera,
                            outputConfig.mirrorMode,
                            outputConfig.timestampBase,
                            outputConfig.dynamicRangeProfile,
                            outputConfig.streamUseCase,
                            outputConfig.deferredOutputType,
                            outputConfig.streamUseHint
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
        inputs =
            graphConfig.input?.map {
                InputStreamImpl(
                    nextInputId(),
                    it.maxImages,
                    it.streamFormat,
                )
            } ?: emptyList()

        val streamSortedByPreview = sortOutputsByPreviewStream(streamListBuilder)
        val streamSortedByVideo = sortOutputsByVideoStream(streamSortedByPreview)

        streams = streamSortedByVideo
        streamIds = streams.map { it.id }.toSet()
        _streamMap = streamMapBuilder
        outputConfigs =
            outputConfigListBuilder.sortedBy {
                it.streams.minOf { stream -> streams.indexOf(stream) }
            }
        outputs = streams.flatMap { it.outputs }
    }

    class OutputConfig(
        val id: OutputConfigId,
        val size: Size,
        val format: StreamFormat,
        val camera: CameraId,
        val groupNumber: Int?,
        val externalOutputConfig: OutputConfiguration?,
        val deferredOutputType: OutputStream.OutputType?,
        val mirrorMode: OutputStream.MirrorMode?,
        val timestampBase: OutputStream.TimestampBase?,
        val dynamicRangeProfile: OutputStream.DynamicRangeProfile?,
        val streamUseCase: OutputStream.StreamUseCase?,
        val streamUseHint: OutputStream.StreamUseHint?,
        val sensorPixelModes: List<OutputStream.SensorPixelMode>,
    ) {
        internal val streamBuilder = mutableListOf<CameraStream>()
        val streams: List<CameraStream>
            get() = streamBuilder

        val deferrable: Boolean
            get() = deferredOutputType != null

        val surfaceSharing = streamBuilder.size > 1

        override fun toString(): String = id.toString()
    }

    private class OutputStreamImpl(
        override val id: OutputId,
        override val size: Size,
        override val format: StreamFormat,
        override val camera: CameraId,
        override val mirrorMode: OutputStream.MirrorMode?,
        override val timestampBase: OutputStream.TimestampBase?,
        override val dynamicRangeProfile: OutputStream.DynamicRangeProfile?,
        override val streamUseCase: OutputStream.StreamUseCase?,
        override val outputType: OutputStream.OutputType?,
        override val streamUseHint: OutputStream.StreamUseHint?
    ) : OutputStream {
        override lateinit var stream: CameraStream

        override fun toString(): String = id.toString()
    }

    private class InputStreamImpl(
        override val id: InputStreamId,
        override val maxImages: Int,
        override val format: StreamFormat
    ) : InputStream

    interface SurfaceListener {
        fun onSurfaceMapUpdated(surfaces: Map<StreamId, Surface>)
    }

    private fun getOutputConfigurationOrNull(
        outputConfig: OutputStream.Config
    ): OutputConfiguration? {
        if (Build.VERSION.SDK_INT >= 33) {
            return (outputConfig as? OutputStream.Config.ExternalOutputConfig)?.output
        }
        return null
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
                .filterIsInstance<OutputStream.Config.ExternalOutputConfig>()
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            graphConfig.sessionMode == CameraGraph.OperatingMode.NORMAL &&
            !cameraMetadata.isHardwareLevelLegacy &&
            !cameraMetadata.isHardwareLevelLimited &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                !cameraMetadata.isHardwareLevelExternal)
    }

    override fun toString(): String {
        return "StreamGraph($_streamMap)"
    }

    /**
     * Sort the output streams to move preview streams to the head of the list. The order of the
     * outputs is determined by the following:
     * 1. StreamUseCase: Check if any streams have PREVIEW StreamUseCase set and move them to the
     *    head of the list. Otherwise, go to step 2.
     * 2. OutputType: Check if any streams have SURFACE_VIEW and SURFACE_TEXTURE OutputType and move
     *    them in respective order to the head of the list. Otherwise, go to step 3.
     * 3. StreamFormat: Check if any streams have UNKNOWN and PRIVATE StreamFormats and move them in
     *    respective order to the head of the list. Otherwise, return list in original order.
     */
    private fun sortOutputsByPreviewStream(
        unsortedStreams: List<CameraStream>
    ): List<CameraStream> {

        // If any stream explicitly specifies "PREVIEW" for its use case, prioritize those streams
        val (previewStreamPartition, nonPreviewStreamPartition) =
            unsortedStreams.partition {
                it.outputs.any { output ->
                    output.streamUseCase == OutputStream.StreamUseCase.PREVIEW
                }
            }
        if (previewStreamPartition.isNotEmpty()) {
            return previewStreamPartition + nonPreviewStreamPartition
        }

        // If no streams explicitly specify the PREVIEW UseCase, fall back to ordering by
        // SURFACE_VIEW / SURFACE_TEXTURE output types.
        val (previewTypePartition, nonPreviewTypePartition) =
            unsortedStreams.partition {
                it.outputs.any { output -> output.outputType in previewOutputTypes }
            }
        if (previewTypePartition.isNotEmpty()) {
            return previewTypePartition.sortedWith(previewOutputTypesComparator) +
                nonPreviewTypePartition
        }

        // Check if any streams have UNKNOWN and PRIVATE StreamFormats
        val (previewFormatPartition, nonPreviewFormatPartition) =
            unsortedStreams.partition {
                it.outputs.any { output -> output.format in previewFormats }
            }
        // Move streams with UNKNOWN and PRIVATE StreamFormats to head of list
        if (previewFormatPartition.isNotEmpty()) {
            return previewFormatPartition.sortedWith(previewFormatComparator) +
                nonPreviewFormatPartition
        }

        // Return outputs in original order if no preview streams found
        return unsortedStreams
    }

    /**
     * Sort the output streams to move video streams to the bottom of the list. The order of the
     * outputs is determined by the following:
     * 1. StreamUseCase: Check if any streams have StreamUseCase.VIDEO_RECORD and move these to the
     *    bottom of the list. Otherwise, go to step 2.
     * 2. StreamUseHint: Check if any streams have StreamUseHint.VIDEO_RECORD and move these to the
     *    bottom of the list.
     */
    private fun sortOutputsByVideoStream(unsortedOutputs: List<CameraStream>): List<CameraStream> {

        // Check if any streams have VIDEO StreamUseCase set
        val (videoStreamPartition, nonVideoStreamPartition) =
            unsortedOutputs.partition {
                it.outputs.any { output ->
                    output.streamUseCase == OutputStream.StreamUseCase.VIDEO_RECORD
                }
            }
        // Move streams with VIDEO StreamUseCase to end of list
        if (videoStreamPartition.isNotEmpty()) {
            return nonVideoStreamPartition + videoStreamPartition
        }

        // Check if any streams have VIDEO StreamUseCaseHint set
        val (videoStreamHintPartition, nonVideoStreamHintPartition) =
            unsortedOutputs.partition {
                it.outputs.any { output ->
                    output.streamUseHint == OutputStream.StreamUseHint.VIDEO_RECORD
                }
            }

        // Move streams with VIDEO StreamUseCaseHint to end of list
        if (videoStreamHintPartition.isNotEmpty()) {
            return nonVideoStreamHintPartition + videoStreamHintPartition
        }

        // Return outputs in original order if no video streams found
        return unsortedOutputs
    }

    companion object {
        private val streamIds = atomic(0)

        internal fun nextStreamId(): StreamId = StreamId(streamIds.incrementAndGet())

        private val outputIds = atomic(0)

        internal fun nextOutputId(): OutputId = OutputId(outputIds.incrementAndGet())

        private val inputIds = atomic(0)

        internal fun nextInputId(): InputStreamId = InputStreamId(inputIds.incrementAndGet())

        private val configIds = atomic(0)

        internal fun nextConfigId(): OutputConfigId = OutputConfigId(configIds.incrementAndGet())

        private val groupIds = atomic(0)

        internal fun nextGroupId(): Int = groupIds.incrementAndGet()

        private val previewOutputTypes =
            listOf(OutputStream.OutputType.SURFACE_VIEW, OutputStream.OutputType.SURFACE_TEXTURE)

        private val previewOutputTypesComparator =
            compareBy<CameraStream> {
                it.outputs.maxOf { output -> previewOutputTypes.indexOf(output.outputType) }
            }

        private val previewFormats = listOf(StreamFormat.UNKNOWN, StreamFormat.PRIVATE)

        private val previewFormatComparator =
            compareBy<CameraStream> {
                it.outputs.maxOf { output -> previewFormats.indexOf(output.format) }
            }
    }
}

@JvmInline
internal value class OutputConfigId(val value: Int) {
    override fun toString(): String = "OutputConfig-$value"
}
