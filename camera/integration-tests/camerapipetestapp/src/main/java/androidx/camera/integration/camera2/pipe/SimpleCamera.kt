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

package androidx.camera.integration.camera2.pipe

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamType
import kotlin.math.absoluteValue

class SimpleCamera(private val cameraGraph: CameraGraph, private val imageReader: ImageReader) {
    companion object {
        private val defaultResolution = Size(1280, 720)
        private const val defaultAspectRatio = 4.0 / 3.0

        fun create(
            cameraPipe: CameraPipe,
            listeners: List<Request.Listener> = emptyList()
        ): SimpleCamera {
            // TODO: It may be worthwhile to turn this into a suspending function to avoid running
            //   camera-finding and metadata querying on the main thread.

            // Find first back facing camera, or any camera id at all if the back facing camera is
            // not available.
            val cameraId = cameraPipe.cameras().findAll().firstOrNull() {
                cameraPipe.cameras().awaitMetadata(it)[CameraCharacteristics.LENS_FACING] ==
                        CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraPipe.cameras().findAll().first()

            Log.i("CXCP-App", "Selected $cameraId to open.")

            val metadata = cameraPipe.cameras().awaitMetadata(cameraId)
            val yuvSizes = metadata.streamMap.getOutputSizes(ImageFormat.YUV_420_888)
            val yuv43Sizes = yuvSizes.filter {
                (((it.width.toDouble() / it.height.toDouble()) - defaultAspectRatio).absoluteValue
                        < 0.001)
            }

            // Find the size that is the least different
            val yuvSize = yuv43Sizes.minBy {
                ((it.width * it.height) - (defaultResolution.width *
                        defaultResolution.height)).absoluteValue
            }!!

            Log.i("CXCP-App", "Selected $yuvSize as the YUV output size")

            val yuvStreamConfig = StreamConfig(
                yuvSize,
                StreamFormat(ImageFormat.YUV_420_888),
                cameraId,
                StreamType.SURFACE
            )

            val config = CameraGraph.Config(
                camera = cameraId,
                streams = listOf(
                    yuvStreamConfig
                ),
                listeners = listeners,
                template = RequestTemplate(CameraDevice.TEMPLATE_PREVIEW)
            )

            val cameraGraph = cameraPipe.create(config)

            Log.i("CXCP-App", "Created $cameraGraph")

            val yuvStream = cameraGraph.streams[yuvStreamConfig]!!
            cameraGraph.acquireSessionOrNull()!!.use {
                it.setRepeating(
                    Request(
                        streams = listOf(yuvStream.id)
                    )
                )
            }

            val imageReader = ImageReader.newInstance(
                yuvSize.width,
                yuvSize.height,
                ImageFormat.YUV_420_888,
                10
            )
            cameraGraph.setSurface(yuvStream.id, imageReader.surface)

            Log.i("CXCP-App", "Configured ${yuvStream.id} to use ${imageReader.surface}")

            return SimpleCamera(cameraGraph, imageReader)
        }
    }

    init {
        // This forces the image reader to cycle images (otherwise it might stall the camera)
        @Suppress("DEPRECATION") val handler = Handler()
        imageReader.setOnImageAvailableListener(
            {
                val image = imageReader.acquireNextImage()
                image?.close()
            },
            handler
        )
    }

    fun start() {
        Log.i("CXCP-App", "Starting $cameraGraph")
        cameraGraph.start()
    }

    fun stop() {
        Log.i("CXCP-App", "Stopping $cameraGraph")
        cameraGraph.stop()
    }

    fun close() {
        Log.i("CXCP-App", "Closing $cameraGraph")
        cameraGraph.close()
        imageReader.close()
    }
}