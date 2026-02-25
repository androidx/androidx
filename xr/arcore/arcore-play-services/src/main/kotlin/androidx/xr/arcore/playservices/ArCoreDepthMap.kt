/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.playservices

import android.media.Image
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.DepthMap
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.math.IntSize2d
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.LinkedList
import java.util.Queue

/**
 * Provides depth map data from ARCore through the [DepthMap] interface.
 *
 * @property width the width of the depth map
 * @property height the height of the depth map
 * @property rawDepthMap the raw depth map
 * @property rawConfidenceMap the raw confidence map
 * @property smoothDepthMap the smooth depth map
 * @property smoothConfidenceMap the smooth confidence map
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreDepthMap internal constructor() : DepthMap {
    override val width: Int
        get() = resolution.width

    override val height: Int
        get() = resolution.height

    override var rawDepthMap: FloatBuffer? = null
        private set

    override var rawConfidenceMap: ByteBuffer? = null
        private set

    override var smoothDepthMap: FloatBuffer? = null
        private set

    override var smoothConfidenceMap: ByteBuffer? = null
        private set

    private var resolution: IntSize2d = IntSize2d()

    internal var depthEstimationMode: DepthEstimationMode = DepthEstimationMode.DISABLED
    private var depthConfidenceImageBuffer: Queue<Image> = LinkedList()

    internal fun updateDepthEstimationMode(depthEstimationMode: DepthEstimationMode) {
        resolution = IntSize2d()
        if (depthEstimationMode == DepthEstimationMode.DISABLED) {
            rawDepthMap = null
            rawConfidenceMap = null
            smoothDepthMap = null
            smoothConfidenceMap = null
        }
        this.depthEstimationMode = depthEstimationMode
    }

    internal fun update(lastFrame: Frame) {
        if (depthEstimationMode == DepthEstimationMode.DISABLED) {
            popDepthImageElement()
            return
        }
        try {
            val currentRawDepthImage = lastFrame.acquireRawDepthImage16Bits()
            val currentRawConfidenceImage = lastFrame.acquireRawDepthConfidenceImage()
            val currentDepthImage =
                if (depthEstimationMode != DepthEstimationMode.RAW_ONLY) {
                    lastFrame.acquireDepthImage16Bits()
                } else {
                    null
                }
            if (currentRawConfidenceImage.planes.size > 0) {
                rawConfidenceMap =
                    currentRawConfidenceImage.planes[0].buffer.order(ByteOrder.nativeOrder())
                if (depthEstimationMode != DepthEstimationMode.RAW_ONLY) {
                    smoothConfidenceMap = rawConfidenceMap
                }
                pushDepthImageElement(currentRawConfidenceImage)
            } else {
                rawConfidenceMap = null
                smoothConfidenceMap = null
            }

            if (currentRawDepthImage.planes.size == 0) {
                rawDepthMap = null
                rawConfidenceMap = null
                // TODO(b/447462709): Use Jetpack XR Logging API
                return
            }

            if (currentDepthImage != null && currentDepthImage.planes.size == 0) {
                smoothDepthMap = null
                smoothConfidenceMap = null
                // TODO(b/447462709): Use Jetpack XR Logging API
                return
            }

            if (
                (depthEstimationMode == DepthEstimationMode.SMOOTH_AND_RAW ||
                    depthEstimationMode == DepthEstimationMode.RAW_ONLY) &&
                    (currentRawDepthImage.width != resolution.width ||
                        currentRawDepthImage.height != resolution.height)
            ) {
                resolution = IntSize2d(currentRawDepthImage.width, currentRawDepthImage.height)
                rawDepthMap = FloatBuffer.allocate(resolution.width * resolution.height)
                if (depthEstimationMode == DepthEstimationMode.SMOOTH_AND_RAW) {
                    smoothDepthMap = FloatBuffer.allocate(resolution.width * resolution.height)
                }
            }

            if (
                depthEstimationMode == DepthEstimationMode.SMOOTH_ONLY &&
                    (currentDepthImage!!.width != resolution.width ||
                        currentDepthImage!!.height != resolution.height)
            ) {
                resolution = IntSize2d(currentDepthImage!!.width, currentDepthImage!!.height)
                smoothDepthMap = FloatBuffer.allocate(resolution.width * resolution.height)
            }

            when (depthEstimationMode) {
                DepthEstimationMode.RAW_ONLY -> {

                    val rawPlane = currentRawDepthImage.planes[0]
                    convertDepthMapBuffer(
                        rawPlane.buffer.order(ByteOrder.nativeOrder()),
                        resolution.height,
                        resolution.width,
                    )
                    smoothDepthMap = null
                    smoothConfidenceMap = null
                }

                DepthEstimationMode.SMOOTH_ONLY -> {

                    val smoothPlane = currentDepthImage!!.planes[0]
                    convertDepthMapBuffer(
                        smoothPlane.buffer.order(ByteOrder.nativeOrder()),
                        resolution.height,
                        resolution.width,
                        false,
                    )
                    rawDepthMap = null
                    rawConfidenceMap = null
                }

                DepthEstimationMode.SMOOTH_AND_RAW -> {

                    val rawPlane = currentRawDepthImage.planes[0]
                    convertDepthMapBuffer(
                        rawPlane.buffer.order(ByteOrder.nativeOrder()),
                        resolution.height,
                        resolution.width,
                    )

                    val smoothPlane = currentDepthImage!!.planes[0]
                    convertDepthMapBuffer(
                        smoothPlane.buffer.order(ByteOrder.nativeOrder()),
                        resolution.height,
                        resolution.width,
                        false,
                    )
                }
            }
        } catch (e: NotYetAvailableException) {
            // TODO(b/447462709): Use Jetpack XR Logging API
        }
    }

    private fun pushDepthImageElement(image: Image) {
        depthConfidenceImageBuffer.add(image)

        if (depthConfidenceImageBuffer.size > MAX_IMAGE_QUEUE_SIZE) {
            popDepthImageElement()
        }
    }

    private fun popDepthImageElement() {
        if (depthConfidenceImageBuffer.isNotEmpty()) {
            val removeImage = depthConfidenceImageBuffer.poll()
            removeImage!!.close()
        }
    }

    private fun clearDepthImagesQueue() {
        while (depthConfidenceImageBuffer.isNotEmpty()) {
            popDepthImageElement()
        }
    }

    // TODO(b/444221417): Remove this once meters support has been implemented.
    private fun convertDepthMapBuffer(
        depthMapShortBuffer: ByteBuffer,
        height: Int,
        width: Int,
        bufferIsRaw: Boolean = true,
    ) {
        val depthMap = if (bufferIsRaw) rawDepthMap!! else smoothDepthMap!!
        val millimetersBuffer = depthMapShortBuffer.asShortBuffer()
        for (x in 0..<width) {
            for (y in 0..<height) {
                val byteIndex = x + (y * width)
                val depthSample = millimetersBuffer.get(byteIndex)
                depthMap.put(byteIndex, depthSample.toFloat() / MILLIMETERS_PER_METER)
            }
        }
    }

    internal fun dispose() {
        clearDepthImagesQueue()
    }

    private companion object {
        /** Represents the value needed to convert millimeters to meters. */
        private const val MILLIMETERS_PER_METER: Float = 1000.0F
        /**
         * Represents the maximum number of images to keep in the depth map image queue.
         *
         * @see depthConfidenceImageBuffer
         */
        private const val MAX_IMAGE_QUEUE_SIZE: Int = 10
    }
}
