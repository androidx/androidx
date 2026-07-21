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

package androidx.camera.common.internal

import android.util.Size
import androidx.camera.common.ImageFormat

/**
 * Represents a camera stream profile, defined by its image format and size.
 *
 * A stream profile is a template for a camera stream. It specifies the pixel format (e.g., YUV,
 * JPEG) and the size of the stream. The size can be either a concrete resolution (e.g., 1080p) or
 * an abstract size category (e.g., [StreamSize.PREVIEW]) that needs to be resolved based on the
 * capabilities of a specific camera device.
 *
 * Use [StreamProfile.create] to instantiate a stream profile.
 *
 * @see StreamSize
 * @see DeviceLimits
 */
public abstract class StreamProfile internal constructor() {

    /**
     * The image format of the stream.
     *
     * @return The format, which must be one of the constants defined in [ImageFormat].
     */
    @ImageFormat public abstract val format: Int

    /**
     * The size of the stream.
     *
     * This can be an absolute size or an abstract size that depends on device capabilities.
     *
     * @see StreamSize
     */
    public abstract val size: StreamSize

    /**
     * Resolves the [StreamSize] of this profile to a concrete [Size] using the provided
     * [deviceLimits].
     *
     * If the profile's [size] is already an absolute size (i.e., its [StreamSize.absoluteSize] is
     * non-null), that size is returned.
     *
     * If the profile's [size] is an abstract size (e.g., [StreamSize.PREVIEW],
     * [StreamSize.MAXIMUM]), it is resolved to a concrete size using the corresponding limit in
     * [deviceLimits] for this profile's [format].
     *
     * @param deviceLimits The device hardware limits used to resolve abstract sizes.
     * @return The resolved concrete [Size], or `null` if the size cannot be resolved (for example,
     *   if this profile's [format] is not supported in the relevant limit map of [deviceLimits]).
     */
    public fun resolve(deviceLimits: DeviceLimits): Size? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    /**
     * Checks if this stream profile satisfies the requirements of a [target] stream profile.
     *
     * A profile satisfies the target if:
     * 1. They have the same [format].
     * 2. Both profiles can be successfully resolved to concrete [Size]s using the provided
     *    [deviceLimits].
     * 3. The resolved size of the target profile fits within the resolved size of this profile
     *    (i.e., `targetSize.width <= thisSize.width` and `targetSize.height <= thisSize.height`).
     *
     * @param target The target stream profile requirement to check.
     * @param deviceLimits The device hardware limits used to resolve the sizes.
     * @return `true` if this profile satisfies the target profile; `false` otherwise.
     */
    public fun satisfies(target: StreamProfile, deviceLimits: DeviceLimits): Boolean {
        throw UnsupportedOperationException("Not yet implemented")
    }

    public companion object {
        /**
         * Creates a new [StreamProfile] with the specified format and size.
         *
         * @param format The image format, which must be one of the constants defined in
         *   [ImageFormat].
         * @param size The stream size, which can be absolute or abstract.
         * @return A new [StreamProfile] instance.
         */
        @JvmStatic
        public fun create(@ImageFormat format: Int, size: StreamSize): StreamProfile {
            return StreamProfileImpl(format, size)
        }
    }
}

/**
 * Internal implementation of [StreamProfile] that provides [equals], [hashCode], and [toString].
 */
internal data class StreamProfileImpl(
    @ImageFormat override val format: Int,
    override val size: StreamSize,
) : StreamProfile()
