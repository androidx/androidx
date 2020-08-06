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

package androidx.camera.camera2.pipe

import android.view.Surface
import androidx.camera.camera2.pipe.wrapper.InputConfigData
import java.io.Closeable

/**
 * A CameraGraph represents the combined configuration and state of a camera.
 */
interface CameraGraph : Closeable {
    val streams: Map<StreamConfig, Stream>

    /**
     * This will cause the CameraGraph to start opening the camera and configuring the Camera2
     * CaptureSession. While the CameraGraph is started it will attempt to keep the camera alive,
     * active, and in a configured running state.
     */
    fun start()

    /**
     * This will cause the CameraGraph to stop executing requests and close the current Camera2
     * CaptureSession (if one is active). The current repeating request is preserved, and any
     * call to submit a request to a session will be enqueued. To prevent requests from being
     * enqueued, close the CameraGraph.
     */
    fun stop()

    /**
     * Acquire and exclusive access to the CameraGraph in a suspending fashion.
     */
    suspend fun acquireSession(): Session

    /**
     * Try acquiring an exclusive access the CameraGraph. Returns null if it can't be acquired
     * immediately.
     */
    fun acquireSessionOrNull(): Session?

    /**
     * This configures the camera graph to use a specific Surface for the given stream.
     *
     * Changing a surface may cause the camera to stall and/or reconfigure.
     */
    fun setSurface(stream: StreamId, surface: Surface?)

    /**
     * This defines the configuration, flags, and pre-defined behavior of a CameraGraph instance.
     */
    data class Config(
        val camera: CameraId,
        val streams: List<StreamConfig>,
        val template: RequestTemplate,
        val defaultParameters: Map<Any?, Any> = emptyMap(),
        val inputStream: InputConfigData? = null,
        val operatingMode: OperatingMode = OperatingMode.NORMAL,
        val listeners: List<Request.Listener> = listOf(),
        val metadataTransform: MetadataTransform = MetadataTransform(),
        val flags: Flags = Flags()
    )

    /**
     * Flags define boolean values that are used to adjust the behavior and interactions with
     * camera2. These flags should default to the ideal behavior and should be overridden on
     * specific devices to be faster or to work around bad behavior.
     */
    data class Flags(
        val configureBlankSessionOnStop: Boolean = false,
        val abortCapturesOnStop: Boolean = false,
        val allowMultipleActiveCameras: Boolean = false
    )

    enum class OperatingMode {
        NORMAL,
        HIGH_SPEED,
    }

    /**
     * A lock on CameraGraph. It facilitates an exclusive access to the managed camera device. Once
     * this is acquired, a well ordered set of requests can be sent to the camera device without the
     * possibility of being intermixed with any other request to the camera from non lock holders.
     */
    interface Session : Closeable {
        fun submit(request: Request)
        fun submit(requests: List<Request>)
        fun setRepeating(request: Request)

        /**
         * Abort in-flight requests. This will abort *all* requests in the current
         * CameraCaptureSession as well as any requests that are currently enqueued.
         */
        fun abort()
    }
}