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

import java.io.Closeable

/**
 * A CameraGraph represents the combined configuration and state of a camera.
 */
interface CameraGraph {
    fun start()
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

    data class Config(
        val camera: CameraId,
        val streams: List<StreamConfig>,
        val defaultTemplate: Int,
        val metadataTransform: MetadataTransform = MetadataTransform()
    )

    /**
     * A lock on CameraGraph. It facilitates an exclusive access to the managed camera device. Once
     * this is acquired, a well ordered set of requests can be sent to the camera device without the
     * possibility of being intermixed with any other request to the camera from non lock holders.
     */
    interface Session : Closeable {
        fun submit(request: Request)
        fun submit(requests: List<Request>)
        fun setRepeating(request: Request)
    }
}