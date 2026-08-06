/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageReaderImageSource
import androidx.camera.camera2.pipe.media.ImageSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

/**
 * Aggregates capacity and memory information for streams across the entire StreamGraph. This can be
 * used to estimate how many frames can be requested for a given set of streams.
 */
internal class StreamGraphCapacity(
    private val memoryEstimator: MemoryEstimator,
    imageSourceMap: Map<StreamId, ImageSource>,
    cameraStreams: List<CameraStream>,
) {
    private val imageReaderSourceMap: Map<StreamId, ImageReaderImageSource> = buildMap {
        for ((streamId, imageSource) in imageSourceMap) {
            put(streamId, checkNotNull(imageSource.unwrapAs(ImageReaderImageSource::class.java)))
        }
    }

    private val outputBytesMap = mutableMapOf<OutputId, Long>()

    // Track the first output size for each stream. This is used as a fallback whenever the primary
    // output for a stream is not yet determined.
    private val defaultOutputBytesPerStream = mutableMapOf<StreamId, Long>()

    init {
        for (cameraStream in cameraStreams) {
            for (output in cameraStream.outputs) {
                val bytes =
                    StreamFormat.bytesPerImage(output.format, output.size.width, output.size.height)
                outputBytesMap[output.id] = bytes
            }

            // Use the first output in the stream's list of outputs as the fallback.
            val firstOutputId = cameraStream.outputs.first().id
            defaultOutputBytesPerStream[cameraStream.id] =
                checkNotNull(outputBytesMap[firstOutputId])
        }
    }

    fun estimateAvailableFramesFlow(streamIds: Set<StreamId>): Flow<Int> {
        require(streamIds.isNotEmpty()) { "StreamId set is empty." }

        // Filter out streams that do not have an ImageSource.
        val validStreamIds =
            streamIds.filter { imageReaderSourceMap.containsKey(it) }.toTypedArray()

        // If none of the requested streams have an ImageSource, they have infinite capacity because
        // they do not consume managed memory or ImageReader slots.
        if (validStreamIds.isEmpty()) {
            return flowOf(Int.MAX_VALUE)
        }

        val sources =
            Array(validStreamIds.size) { i ->
                checkNotNull(imageReaderSourceMap[validStreamIds[i]])
            }

        val openImageCountFlows = sources.map { it.openImages }.combineArray()
        val evictableImageCountFlows = sources.map { it.evictableImageCountFlow }.combineArray()
        val primaryOutputIdFlows = sources.map { it.primaryOutputIdFlow }.combineArray()

        return combine(
                openImageCountFlows,
                evictableImageCountFlows,
                primaryOutputIdFlows,
                memoryEstimator.capacityFlow,
                memoryEstimator.evictableMemory,
            ) { openImageCounts, evictableImageCounts, primaryOutputIds, memCapacity, memEvictable
                ->
                estimate(
                    validStreamIds,
                    sources,
                    openImageCounts,
                    evictableImageCounts,
                    primaryOutputIds,
                    availableMemory = memCapacity + memEvictable,
                )
            }
            .distinctUntilChanged()
    }

    fun estimateAvailableFrames(streamIds: Set<StreamId>): Int {
        require(streamIds.isNotEmpty()) { "StreamId set is empty." }

        // Filter out streams that do not have an ImageSource. We treat the streams without an
        // ImageSource as external and having infinite capacity.
        val validStreamIds =
            streamIds.filter { imageReaderSourceMap.containsKey(it) }.toTypedArray()

        if (validStreamIds.isEmpty()) {
            return Int.MAX_VALUE
        }

        val imageSources =
            Array(validStreamIds.size) { i ->
                checkNotNull(imageReaderSourceMap[validStreamIds[i]])
            }

        val size = imageSources.size
        val openImageCounts = Array(size) { i -> imageSources[i].openImages.value }
        val evictableImageCounts =
            Array(size) { i -> imageSources[i].evictableImageCountFlow.value }
        val primaryOutputIds = Array(size) { i -> imageSources[i].primaryOutputIdFlow.value }

        return estimate(
            validStreamIds,
            imageSources,
            openImageCounts,
            evictableImageCounts,
            primaryOutputIds,
            availableMemory = memoryEstimator.availableMemory,
        )
    }

    private fun estimate(
        streamIds: Array<StreamId>,
        imageSources: Array<ImageReaderImageSource>,
        openImageCounts: Array<Int>,
        evictableImageCounts: Array<Int>,
        primaryOutputIds: Array<OutputId?>,
        availableMemory: Long,
    ): Int {
        var totalBytesRequired = 0L

        for (i in primaryOutputIds.indices) {
            val outputId = primaryOutputIds[i]
            if (outputId == null) {
                // If primary output is null, e.g. if camera has not started producing image(s)
                // from the given stream, we use the size of the outputId that is first in the list
                // of outputs for a given camera stream as fallback.
                val streamId = streamIds[i]
                totalBytesRequired += checkNotNull(defaultOutputBytesPerStream[streamId])
            } else {
                totalBytesRequired += checkNotNull(outputBytesMap[outputId])
            }
        }

        var minAvailableAtSource = Int.MAX_VALUE
        for (i in imageSources.indices) {
            val source = imageSources[i]
            val availableAtSource =
                maxOf(0, source.maxImages - openImageCounts[i] + evictableImageCounts[i])
            minAvailableAtSource = minOf(minAvailableAtSource, availableAtSource)
        }

        val availableMemorySlots =
            if (totalBytesRequired <= 0L) {
                Int.MAX_VALUE
            } else {
                (availableMemory / totalBytesRequired).toInt()
            }
        return minOf(minAvailableAtSource, maxOf(0, availableMemorySlots))
    }

    private inline fun <reified T> List<Flow<T>>.combineArray(): Flow<Array<T>> =
        if (isEmpty()) flowOf(emptyArray()) else combine(this) { it }
}
