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

package androidx.camera.core.impl

/**
 * A stream that would be configured on the camera, as resolved by a simulation of adding use cases
 * (see [UseCaseAdditionSimulator]).
 *
 * @property useCaseConfig The merged [UseCaseConfig] of the camera use case that owns the stream,
 *   which provides e.g. the input format and capture type.
 * @property streamSpec The resolved [StreamSpec] of the stream, which provides e.g. the resolution,
 *   dynamic range, expected frame rate range and implementation options.
 * @property physicalCameraId The physical camera ID associated with the stream, or `null` if none.
 */
public class ResolvedStreamInfo
@JvmOverloads
constructor(
    public val useCaseConfig: UseCaseConfig<*>,
    public val streamSpec: StreamSpec,
    public val physicalCameraId: String? = null,
) {
    override fun toString(): String =
        "ResolvedStreamInfo(useCaseConfig=${useCaseConfig.javaClass.simpleName}, " +
            "streamSpec=$streamSpec, physicalCameraId=$physicalCameraId)"
}

/** The result of querying the camera device whether a session configuration is supported. */
public enum class HardwareConfigQueryResult {
    /** The camera device reported that the session configuration is supported. */
    SUPPORTED,

    /** The camera device reported that the session configuration is not supported. */
    UNSUPPORTED,

    /**
     * It is unknown whether the session configuration is supported, e.g. because the query is not
     * available on the device.
     */
    UNKNOWN,
}
