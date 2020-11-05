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
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamConfig
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamType
import kotlin.math.absoluteValue

private const val defaultWidth = 960
private const val defaultHeight = 720
private const val defaultArea = defaultWidth * defaultHeight
private const val defaultAspectRatio = defaultWidth.toDouble() / defaultHeight.toDouble()

class SimpleCamera(
    private val cameraId: CameraId,
    private val cameraMetadata: CameraMetadata,
    private val cameraGraph: CameraGraph,
    private val imageReader: ImageReader
) {
    companion object {
        fun create(
            cameraPipe: CameraPipe,
            cameraId: CameraId,
            viewfinder: Viewfinder,
            listeners: List<Request.Listener> = emptyList()
        ): SimpleCamera {
            // TODO: It may be worthwhile to turn this into a suspending function to avoid running
            //   camera-finding and metadata querying on the main thread.

            Log.i("CXCP-App", "Selected $cameraId to open.")

            val cameraMetadata = cameraPipe.cameras().awaitMetadata(cameraId)
            var yuvSizes = cameraMetadata.streamMap.getOutputSizes(ImageFormat.YUV_420_888).toList()

            val closestAspectRatioSize = yuvSizes.minByOrNull {
                (it.aspectRatio() - defaultAspectRatio).absoluteValue
            }!!
            val closestAspectRatio = closestAspectRatioSize.aspectRatio()
            yuvSizes = yuvSizes.filterIf {
                (it.aspectRatio() - closestAspectRatio).absoluteValue < 0.01
            }

            // Find the size that is the least different
            val yuvSize = yuvSizes.minByOrNull {
                (it.area() - defaultArea).absoluteValue
            }!!

            Log.i("CXCP-App", "Selected $yuvSize as the YUV output size")

            val yuvStreamConfig = StreamConfig(
                yuvSize,
                StreamFormat.YUV_420_888,
                cameraId,
                StreamType.SURFACE
            )

            val viewfinderStreamConfig = StreamConfig(
                yuvSize,
                StreamFormat.UNKNOWN,
                cameraId,
                StreamType.SURFACE_VIEW
            )

            val config = CameraGraph.Config(
                camera = cameraId,
                streams = listOf(
                    viewfinderStreamConfig,
                    yuvStreamConfig
                ),
                listeners = listeners,
                template = RequestTemplate(CameraDevice.TEMPLATE_PREVIEW)
            )

            val cameraGraph = cameraPipe.create(config)

            val viewfinderStream = cameraGraph.streams[viewfinderStreamConfig]!!
            viewfinder.configure(
                viewfinderStream.size,
                object : Viewfinder.SurfaceListener {
                    override fun onSurfaceChanged(surface: Surface?, size: Size?) {
                        Log.i("CXCP-App", "Viewfinder surface changed to $surface at $size")
                        cameraGraph.setSurface(viewfinderStream.id, surface)
                    }
                }
            )
            val yuvStream = cameraGraph.streams[yuvStreamConfig]!!
            cameraGraph.acquireSessionOrNull()!!.use {
                it.setRepeating(
                    Request(
                        streams = listOf(viewfinderStream.id, yuvStream.id)
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
            return SimpleCamera(cameraId, cameraMetadata, cameraGraph, imageReader)
        }

        private fun Size.aspectRatio(): Double {
            return this.width.toDouble() / this.height.toDouble()
        }

        private fun Size.area(): Int {
            return this.width * this.height
        }

        private inline fun <T> List<T>.filterIf(predicate: (T) -> Boolean): List<T> {
            val result = this.filter(predicate)
            if (result.isEmpty()) {
                return this
            }
            return result
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

    fun cameraInfoString(): String {
        val lensFacing = when (cameraMetadata[CameraCharacteristics.LENS_FACING]) {
            CameraCharacteristics.LENS_FACING_FRONT -> "Front"
            CameraCharacteristics.LENS_FACING_BACK -> "Back"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }

        val capabilities = cameraMetadata[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
        val cameraType = if (capabilities != null &&
            capabilities.contains(REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        ) {
            "Logical"
        } else {
            "Physical"
        }

        return StringBuilder().apply {
            append("$cameraGraph (Camera ${cameraId.value})\n")
            append("  Facing:    $lensFacing ($cameraType)\n")
            append("Streams:")
            for (stream in cameraGraph.streams) {
                append("\n  ")
                append(stream.value.id.toString().padEnd(12, ' '))
                append(stream.value.size.toString().padEnd(12, ' '))
                append(stream.value.format.name.padEnd(16, ' '))
                append(stream.value.type.toString().padEnd(16, ' '))
            }

            // TODO: Add static configuration info.
        }.toString()
    }
}
