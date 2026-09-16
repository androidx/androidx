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

package androidx.xr.arcore.testapp.helloar.ui

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** Immutable snapshot of a captured camera frame ready for tracking ingestion. */
internal data class FrameSnapshot(
    val buffer: ByteBuffer,
    val width: Int,
    val height: Int,
    val timestampNs: Long,
    val rotationDegrees: Int = 0,
    val isRotated: Boolean = false,
)

/**
 * CameraX image analyzer that extracts grayscale luma buffers, detects orientation, and produces
 * thread-safe frame copies for tracking.
 */
internal class CameraFrameAnalyzer(
    private val onResolutionChanged: (activeWidth: Float, activeHeight: Float) -> Unit
) : ImageAnalysis.Analyzer {

    private data class FrameDimensions(val width: Int, val height: Int) {
        val byteCount: Int
            get() = width * height
    }

    private val bufferLock = Any()
    private var frameBuffer: ByteBuffer? = null
    private var lastProcessedMark: ComparableTimeMark? = null
    private var activeFrameWidth = 0f
    private var activeFrameHeight = 0f

    // Frame timestamp (nanoseconds) synchronized to System.nanoTime() required by JNI tracking API
    // contract.
    private var latestFrameTimestampNs = 0L
    private var latestFrameWidth = 0
    private var latestFrameHeight = 0
    private var latestFrameRotationDegrees = 0
    private var latestFrameIsRotated = false

    private fun ensureBufferCapacityLocked(width: Int, height: Int) {
        val requiredCapacity = width * height
        val currentFrameBuffer = frameBuffer
        if (currentFrameBuffer == null || currentFrameBuffer.capacity() < requiredCapacity) {
            frameBuffer = ByteBuffer.allocateDirect(requiredCapacity).order(ByteOrder.nativeOrder())
        }
    }

    /** Clears cached buffers upon teardown. */
    internal fun clearBuffers() {
        synchronized(bufferLock) {
            frameBuffer = null
            latestFrameTimestampNs = 0L
            latestFrameWidth = 0
            latestFrameHeight = 0
            latestFrameRotationDegrees = 0
            latestFrameIsRotated = false
        }
    }

    /** Creates an independent direct [ByteBuffer] copy of the latest captured camera frame. */
    internal fun copyLatestFrame(): FrameSnapshot? {
        val dimensions =
            synchronized(bufferLock) {
                if (frameBuffer == null) {
                    return null
                }
                val width = latestFrameWidth
                val height = latestFrameHeight
                if (width <= 0 || height <= 0) {
                    Log.w(TAG, "Cannot copy frame: invalid dimensions (${width}x${height})")
                    return null
                }
                FrameDimensions(width = width, height = height)
            }

        val frameCopy =
            ByteBuffer.allocateDirect(dimensions.byteCount).order(ByteOrder.nativeOrder())

        return synchronized(bufferLock) {
            val orig = frameBuffer ?: return null
            if (latestFrameWidth != dimensions.width || latestFrameHeight != dimensions.height) {
                Log.w(TAG, "Frame dimensions changed during buffer allocation; dropping snapshot")
                return null
            }
            val origCopy = orig.asReadOnlyBuffer()
            origCopy.rewind()
            origCopy.limit(dimensions.byteCount)
            frameCopy.put(origCopy)
            frameCopy.rewind()

            FrameSnapshot(
                buffer = frameCopy,
                width = dimensions.width,
                height = dimensions.height,
                timestampNs = latestFrameTimestampNs,
                rotationDegrees = latestFrameRotationDegrees,
                isRotated = latestFrameIsRotated,
            )
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.use { proxy ->
            val now = TimeSource.Monotonic.markNow()
            val previousMark = lastProcessedMark
            if (previousMark != null && now - previousMark < FRAME_THROTTLE_INTERVAL) {
                return
            }
            lastProcessedMark = now

            try {
                val width = proxy.width
                val height = proxy.height
                val isRotated = abs(proxy.imageInfo.rotationDegrees) % 180 == 90
                val currentWidth = if (isRotated) height.toFloat() else width.toFloat()
                val currentHeight = if (isRotated) width.toFloat() else height.toFloat()

                if (activeFrameWidth != currentWidth || activeFrameHeight != currentHeight) {
                    Log.i(
                        TAG,
                        "Camera resolution updated: ${width}x${height} (rotated: ${proxy.imageInfo.rotationDegrees} deg) -> active frame size: ${currentWidth}x${currentHeight}",
                    )
                    activeFrameWidth = currentWidth
                    activeFrameHeight = currentHeight
                    onResolutionChanged(currentWidth, currentHeight)
                }

                val sensorTimestamp = proxy.imageInfo.timestamp
                val captureTimestampNs =
                    if (sensorTimestamp > 0L) {
                        val elapsedSinceCapture =
                            SystemClock.elapsedRealtimeNanos() - sensorTimestamp
                        (System.nanoTime() - elapsedSinceCapture).coerceAtLeast(0L)
                    } else {
                        System.nanoTime()
                    }

                val plane = proxy.planes[LUMA_PLANE_INDEX]
                val buffer = plane.buffer
                val rowStride = plane.rowStride

                synchronized(bufferLock) {
                    ensureBufferCapacityLocked(width, height)
                    val trackingBuf = frameBuffer ?: return

                    copyLumaPlane(
                        source = buffer,
                        destination = trackingBuf,
                        width = width,
                        height = height,
                        rowStride = rowStride,
                    )

                    latestFrameTimestampNs = captureTimestampNs
                    latestFrameWidth = width
                    latestFrameHeight = height
                    latestFrameRotationDegrees = proxy.imageInfo.rotationDegrees
                    latestFrameIsRotated = isRotated
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime error copying image plane", e)
                synchronized(bufferLock) {
                    latestFrameWidth = 0
                    latestFrameHeight = 0
                    latestFrameRotationDegrees = 0
                    latestFrameTimestampNs = 0L
                }
            }
        }
    }

    /**
     * Copies the luma plane from [source] into [destination], handling row strides and packing
     * contiguous pixel rows. Restores [source] position and limit upon completion.
     */
    private fun copyLumaPlane(
        source: ByteBuffer,
        destination: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
    ) {
        destination.clear()
        val originalPosition = source.position()
        val originalLimit = source.limit()
        try {
            source.rewind()
            if (rowStride == width) {
                source.limit(width * height)
                destination.put(source)
            } else {
                for (row in 0 until height) {
                    val rowStart = row * rowStride
                    source.limit(source.capacity())
                    source.position(rowStart)
                    source.limit(rowStart + width)
                    destination.put(source)
                }
            }
            destination.flip()
        } finally {
            source.position(0)
            source.limit(originalLimit)
            source.position(originalPosition)
        }
    }

    companion object {
        private const val TAG = "CameraFrameAnalyzer"
        private const val LUMA_PLANE_INDEX = 0
        internal const val DEFAULT_FRAME_WIDTH = 1920
        internal const val DEFAULT_FRAME_HEIGHT = 1080
        private val FRAME_THROTTLE_INTERVAL: Duration = 50.milliseconds
    }
}
