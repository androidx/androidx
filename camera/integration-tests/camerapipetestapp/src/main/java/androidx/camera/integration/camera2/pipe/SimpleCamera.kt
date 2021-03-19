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
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.CameraStream.Config
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.core.Debug
import kotlin.math.absoluteValue

private const val defaultWidth = 960
private const val defaultHeight = 720
private const val defaultArea = defaultWidth * defaultHeight
private const val defaultAspectRatio = defaultWidth.toDouble() / defaultHeight.toDouble()

class SimpleCamera(
    private val cameraConfig: CameraGraph.Config,
    private val cameraGraph: CameraGraph,
    private val cameraMetadata: CameraMetadata,
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
            var yuvSizes =
                cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!
                    .getOutputSizes(ImageFormat.YUV_420_888).toList()

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

            val viewfinderStreamConfig = Config.create(
                yuvSize,
                StreamFormat.UNKNOWN,
                outputType = OutputStream.OutputType.SURFACE_VIEW
            )

            val yuvStreamConfig = Config.create(
                yuvSize,
                StreamFormat.YUV_420_888
            )

            val config = CameraGraph.Config(
                camera = cameraId,
                streams = listOf(
                    viewfinderStreamConfig,
                    yuvStreamConfig
                ),
                defaultListeners = listeners,
                defaultTemplate = RequestTemplate(CameraDevice.TEMPLATE_PREVIEW)
            )

            val cameraGraph = cameraPipe.create(config)

            val viewfinderStream = cameraGraph.streams[viewfinderStreamConfig]!!
            val viewfinderOutput = viewfinderStream.outputs.single()

            viewfinder.configure(
                viewfinderOutput.size,
                object : Viewfinder.SurfaceListener {
                    override fun onSurfaceChanged(surface: Surface?, size: Size?) {
                        Log.i("CXCP-App", "Viewfinder surface changed to $surface at $size")
                        cameraGraph.setSurface(viewfinderStream.id, surface)
                    }
                }
            )
            val yuvStream = cameraGraph.streams[yuvStreamConfig]!!
            val yuvOutput = yuvStream.outputs.single()

            val imageReader = ImageReader.newInstance(
                yuvOutput.size.width,
                yuvOutput.size.height,
                yuvOutput.format.value,
                10
            )
            cameraGraph.setSurface(yuvStream.id, imageReader.surface)

            cameraGraph.acquireSessionOrNull()!!.use {
                it.startRepeating(
                    Request(
                        streams = listOf(viewfinderStream.id, yuvStream.id)
                    )
                )
            }

            return SimpleCamera(
                config,
                cameraGraph,
                cameraMetadata,
                imageReader
            )
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

    fun cameraInfoString(): String =
        Debug.formatCameraGraphProperties(cameraMetadata, cameraConfig, cameraGraph)
}
