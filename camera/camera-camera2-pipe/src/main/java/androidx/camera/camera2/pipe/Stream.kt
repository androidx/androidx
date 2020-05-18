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

import android.util.Size

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class StreamId(val value: Int)

/**
 * Standard stream properties that should be specified for all configured streams.
 */
interface StreamProperties {
    val size: Size
    val format: StreamFormat
    val camera: CameraId
    val type: StreamType
}

/**
 * A Stream is an identifier for a specific stream as well as the properties that were used to
 * create this stream instance. This allows StreamConfig's to be used to build multiple
 * [CameraGraph]'s  while enforcing that each [Stream] will only work with that specific camera
 * [CameraGraph] instance.
 */
data class Stream(
    val config: StreamConfig,
    val id: StreamId
) : StreamProperties by config

/**
 * Configuration object that provides the parameters for a specific input / output stream on Camera.
 */
data class StreamConfig(
    override val size: Size,
    override val format: StreamFormat,
    override val camera: CameraId,
    override val type: StreamType
) : StreamProperties

/**
 * Camera2 allows the camera to be configured with outputs that are not immediately available.
 * This allows the camera to configure the internal pipeline with additional information about
 * the surface that has not yet been provided to the camera.
 */
enum class StreamType {
    SURFACE,
    SURFACE_VIEW,
    SURFACE_TEXTURE
}
