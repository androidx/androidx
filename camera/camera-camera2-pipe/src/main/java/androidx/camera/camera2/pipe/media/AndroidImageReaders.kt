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

package androidx.camera.camera2.pipe.media

import android.hardware.camera2.MultiResolutionImageReader
import android.hardware.camera2.params.MultiResolutionStreamInfo
import android.hardware.camera2.params.OutputConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraOnActiveOutputSurfacesListener
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.PlatformApiCompat
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.Api28Compat
import androidx.camera.camera2.pipe.compat.Api29Compat
import androidx.camera.camera2.pipe.compat.Api33Compat
import androidx.camera.camera2.pipe.core.Log
import java.util.concurrent.Executor
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

/** Implements an [ImageReaderWrapper] using an [ImageReader]. */
public class AndroidImageReader
private constructor(
    private val imageReader: ImageReader,
    override val capacity: Int,
    private val streamId: StreamId,
    private val outputId: OutputId,
) : ImageReaderWrapper, ImageReader.OnImageAvailableListener {
    private val outputIdSet = setOf(outputId)

    override val surface: Surface = imageReader.surface

    override var onImageListener: ImageReaderWrapper.OnImageListener? by atomic(null)

    override var onExpectedOutputsListener: ImageReaderWrapper.OnExpectedOutputsListener? by
        atomic(null)

    override fun onImageAvailable(reader: ImageReader?) {
        val image = reader?.acquireNextImage()
        if (image != null) {
            val imageListener = onImageListener
            if (imageListener == null) {
                image.close()
                return
            }

            onExpectedOutputsListener?.onExpectedOutputs(image.timestamp, outputIdSet)

            imageListener.onImage(streamId, outputId, AndroidImage(image))
        }
    }

    override fun close(): Unit = imageReader.close()

    override fun flush() {
        // acquireLatestImage will acquire the most recent image and internally close any image that
        // is older, this call ensures any pending images are closed before calling
        // discardFreeBuffers to ensure we release as much memory as possible.
        imageReader.acquireLatestImage()?.close()

        // ImageReaders are pools of shared memory that is not actively released until the
        // ImageReader is closed. This method call actively frees these unused buffers from the
        // internal buffer pool.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28Compat.discardFreeBuffers(imageReader)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            ImageReader::class -> imageReader as T?
            else -> null
        }

    override fun toString(): String {
        return "ImageReader@${super.hashCode().toString(16)}" +
            "-${StreamFormat(imageReader.imageFormat).name}" +
            "-w${imageReader.width}h${imageReader.height}"
    }

    public companion object {
        // See: b/172464059
        //
        // The ImageReader has an internal limit of 64 images by design, but depending on the device
        // specific camera HAL (Which can be different per device) there is an additional number of
        // images that are reserved by the Camera HAL which reduces this number. If, for example,
        // the HAL reserves 8 images, you have a maximum of 56 (64 - 8).
        //
        // One of the worst cases observed is the HAL reserving 10 images, which gives a maximum
        // capacity of 54 (64 - 10). For safety and compatibility reasons, set the maximum capacity
        // to be 54, which leaves headroom for an app configured limit of 50.
        internal const val IMAGEREADER_MAX_CAPACITY = 54

        /**
         * Create and configure a new ImageReader instance as an [ImageReaderWrapper].
         *
         * See [ImageReader.newInstance] for details.
         */
        public fun create(
            width: Int,
            height: Int,
            format: Int,
            capacity: Int,
            usageFlags: Long?,
            defaultDataSpace: Int?,
            defaultHardwareBufferFormat: Int?,
            streamId: StreamId,
            outputId: OutputId,
            handler: Handler,
        ): ImageReaderWrapper {
            require(width > 0) { "Width ($width) must be > 0" }
            require(height > 0) { "Height ($height) must be > 0" }
            require(capacity > 0) { "Capacity ($capacity) must be > 0" }
            require(capacity <= IMAGEREADER_MAX_CAPACITY) {
                "Capacity for creating new ImageSources is restricted to " +
                    "$IMAGEREADER_MAX_CAPACITY. Android has undocumented internal limits that " +
                    "are different depending on which device the ImageReader is created on."
            }

            // Warnings for unsupported features:
            if (usageFlags != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Log.warn {
                    "Ignoring ImageReader usage ($usageFlags) " +
                        "for $outputId. Android ${Build.VERSION.SDK_INT} does not " +
                        "support creating ImageReaders with usage flags. " +
                        "This may lead to unexpected behaviors."
                }
            }
            if (defaultDataSpace != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Log.warn {
                    "Ignoring defaultDataSpace ($defaultDataSpace) " +
                        "for $outputId. Android ${Build.VERSION.SDK_INT} does not " +
                        "support creating ImageReaders with defaultDataSpace. " +
                        "This may lead to unexpected behaviors."
                }
            }
            if (
                defaultHardwareBufferFormat != null &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                Log.warn {
                    "Ignoring defaultHardwareBufferFormat ($defaultHardwareBufferFormat) " +
                        "for $outputId. Android ${Build.VERSION.SDK_INT} does not " +
                        "support creating ImageReaders with defaultHardwareBufferFormat. " +
                        "This may lead to unexpected behaviors."
                }
            }

            // Create and configure a new ImageReader based on the current Android SDK
            val imageReader =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Api33Compat.newImageReaderFromImageReaderBuilder(
                        width = width,
                        height = height,
                        imageFormat = format,
                        maxImages = capacity,
                        usage = usageFlags,
                        defaultDataSpace = defaultDataSpace,
                        defaultHardwareBufferFormat = defaultHardwareBufferFormat,
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (usageFlags != null) {
                        Api29Compat.imageReaderNewInstance(
                            width,
                            height,
                            format,
                            capacity,
                            usageFlags,
                        )
                    } else {
                        ImageReader.newInstance(width, height, format, capacity)
                    }
                } else {
                    ImageReader.newInstance(width, height, format, capacity)
                }

            // Create the ImageSource and wire it up the onImageAvailableListener
            val androidImageReader = AndroidImageReader(imageReader, capacity, streamId, outputId)
            imageReader.setOnImageAvailableListener(androidImageReader, handler)
            return androidImageReader
        }
    }
}

/** Implements an [ImageReaderWrapper] using a [MultiResolutionImageReader]. */
@RequiresApi(31)
public class AndroidMultiResolutionImageReader(
    private val multiResolutionImageReader: MultiResolutionImageReader,
    private val streamFormat: StreamFormat,
    override val capacity: Int,
    private val streamId: StreamId,
    internal val outputConfigurations: List<OutputConfiguration>,
    private val streamInfoToOutputIdMap: Map<MultiResolutionStreamInfo, OutputId>,
    private val surfaceToOutputIdMap: Map<Surface, OutputId>,
    private val concurrentOutputsEnabled: Boolean,
) : ImageReaderWrapper, ImageReader.OnImageAvailableListener, CameraOnActiveOutputSurfacesListener {
    private val singleOutputIdSets = surfaceToOutputIdMap.mapValues { setOf(it.value) }

    override val surface: Surface
        get() = multiResolutionImageReader.surface

    override var onImageListener: ImageReaderWrapper.OnImageListener? by atomic(null)

    override var onExpectedOutputsListener: ImageReaderWrapper.OnExpectedOutputsListener? by
        atomic(null)

    override fun onImageAvailable(reader: ImageReader?) {
        val image = reader?.acquireNextImage()
        if (image != null) {
            val imageListener = onImageListener
            if (imageListener == null) {
                image.close()
                return
            }

            // MultiResolutionImageReaders produce images from multiple sub-ImageReaders, in order
            // to figure out which output the image is from, we have to first look up the
            // StreamInfo from the MultiResolutionImageReader instance, and then use it to look it
            // up in the outputMap that was used to create the MultiResolutionImageReader.
            val streamInfo = multiResolutionImageReader.getStreamInfoForImageReader(reader)
            val outputId =
                checkNotNull(streamInfoToOutputIdMap[streamInfo]) {
                    "$this: Failed to find OutputId for $reader based on streamInfo $streamInfo!"
                }

            if (!concurrentOutputsEnabled) {
                // For non-concurrent streams, we cannot rely on onActiveOutputSurfaces. Hence, we
                // fire the expected outputs listener here.
                onExpectedOutputsListener?.onExpectedOutputs(image.timestamp, setOf(outputId))
            }

            // Note: During camera switches, MultiResolutionImageReaders does not guarantee that
            // images will always be in monotonically increasing order. The primary reason for this
            // is when a camera switches from one lens to another, which can cause the camera
            // to produce overlapping images from each sensor and can be delivered out of order.
            imageListener.onImage(streamId, outputId, AndroidImage(image))
        }
    }

    override fun onActiveOutputSurfaces(
        activeOutputSurfaces: List<Surface>,
        timestamp: Long,
        frameNumber: Long,
    ) {
        val expectedOutputs =
            if (activeOutputSurfaces.size == 1) {
                // Optimization: Since most calls on this listener are expected to have just one
                // Surface. Use the precomputed sets to avoid creating a new one each time.
                val surface = activeOutputSurfaces[0]
                checkNotNull(singleOutputIdSets[surface]) {
                    "Unrecognized active surface in $streamId: $surface"
                }
            } else {
                activeOutputSurfaces
                    .map { surface ->
                        checkNotNull(surfaceToOutputIdMap[surface]) {
                            "Unrecognized active surface in $streamId: $surface"
                        }
                    }
                    .toSet()
            }
        onExpectedOutputsListener?.onExpectedOutputs(timestamp, expectedOutputs)
    }

    override fun close(): Unit = multiResolutionImageReader.close()

    override fun flush() {
        // ImageReaders are pools of shared memory that is not actively released until the
        // ImageReader is closed. This method call actively frees these unused buffers from the
        // internal buffer pool(s).
        multiResolutionImageReader.flush()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            AndroidMultiResolutionImageReader::class -> this as T?
            MultiResolutionImageReader::class -> multiResolutionImageReader as T?
            else -> null
        }

    override fun toString(): String {
        val sizeString =
            streamInfoToOutputIdMap.keys.joinToString(prefix = "[", postfix = "]") {
                "${it.physicalCameraId}:w${it.width}h${it.height}"
            }
        return "MultiResolutionImageReader@${super.hashCode().toString(16)}" +
            "-${streamFormat.name}" +
            "-$sizeString"
    }

    public companion object {
        @RequiresApi(31)
        public fun create(
            outputFormat: Int,
            streamId: StreamId,
            outputs: List<OutputStream>,
            capacity: Int,
            executor: Executor,
            usageFlags: Long?,
            enableConcurrentOutputs: Boolean,
            plaformApiCompat: PlatformApiCompat?,
        ): ImageReaderWrapper {
            require(capacity > 0) { "Capacity ($capacity) must be > 0" }
            require(capacity <= AndroidImageReader.IMAGEREADER_MAX_CAPACITY) {
                "Capacity for creating new ImageSources is restricted to " +
                    "${AndroidImageReader.IMAGEREADER_MAX_CAPACITY}. Android has undocumented " +
                    "internal limits that are different depending on which device the " +
                    "MultiResolutionImageReader is created on."
            }
            if (enableConcurrentOutputs) {
                require(plaformApiCompat?.isMultiResolutionConcurrentReadersEnabled() == true) {
                    "Concurrent MultiResolutionImageReaders are not supported on this device"
                }
            }

            // Create and configure a new MultiResolutionImageReader
            if (usageFlags != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
                Log.warn {
                    "Usage flags are only supported for API >= 36. Creating multiresolution image reader without usage flag."
                }
            }

            val streamInfoToOutputIdMap =
                outputs.associate {
                    MultiResolutionStreamInfo(it.size.width, it.size.height, it.camera.value) to
                        it.id
                }
            val streamInfos = streamInfoToOutputIdMap.keys

            val multiResolutionImageReader =
                if (plaformApiCompat?.isMultiResolutionConcurrentReadersEnabled() == true) {
                    plaformApiCompat.buildMultiResolutionImageReader(
                        streamInfos,
                        outputFormat,
                        capacity,
                        usageFlags,
                        enableConcurrentOutputs,
                    )
                } else if (
                    usageFlags != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                ) {
                    MultiResolutionImageReader(streamInfos, outputFormat, capacity, usageFlags)
                } else {
                    MultiResolutionImageReader(streamInfos, outputFormat, capacity)
                }

            val outputConfigurations =
                OutputConfiguration.createInstancesForMultiResolutionOutput(
                        multiResolutionImageReader
                    )
                    .toList()
            check(outputConfigurations.size == outputs.size)

            val surfaceToOutputIdMap = buildMap {
                for ((outputConfiguration, output) in outputConfigurations.zip(outputs)) {
                    put(checkNotNull(outputConfiguration.surface), output.id)
                }
            }

            val androidMultiResolutionImageReader =
                AndroidMultiResolutionImageReader(
                    multiResolutionImageReader,
                    StreamFormat(outputFormat),
                    capacity,
                    streamId,
                    outputConfigurations,
                    streamInfoToOutputIdMap,
                    surfaceToOutputIdMap,
                    enableConcurrentOutputs,
                )
            if (
                plaformApiCompat?.isMultiResolutionConcurrentReadersEnabled() == true &&
                    enableConcurrentOutputs
            ) {
                plaformApiCompat.setOnActiveOutputSurfacesListener(
                    multiResolutionImageReader,
                    executor,
                    androidMultiResolutionImageReader,
                )
            }

            multiResolutionImageReader.setOnImageAvailableListener(
                androidMultiResolutionImageReader,
                executor,
            )

            return androidMultiResolutionImageReader
        }

        @RequiresApi(31)
        public fun create(
            cameraStream: CameraStream,
            capacity: Int,
            executor: Executor,
            usageFlags: Long?,
            enableConcurrentOutputs: Boolean,
            platformApiCompat: PlatformApiCompat?,
        ): ImageReaderWrapper {
            val outputs = cameraStream.outputs
            require(outputs.isNotEmpty()) { "$cameraStream outputs cannot be empty!" }
            val format = outputs.first().format
            return create(
                format.value,
                cameraStream.id,
                outputs,
                capacity,
                executor,
                usageFlags,
                enableConcurrentOutputs,
                platformApiCompat,
            )
        }
    }
}
