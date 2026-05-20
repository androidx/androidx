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

/** Represents a stream profile with a specific format and size. */
public abstract class StreamProfile internal constructor() {

    /** Returns the stream format. */
    @ImageFormat public abstract val format: Int

    /** Returns the stream size. */
    public abstract val size: StreamSize

    /** Resolves the [StreamSize] to a concrete 2D [Size] using the provided [DeviceLimits]. */
    public fun resolve(deviceLimits: DeviceLimits): Size? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    /**
     * Checks if this stream profile satisfies the requirements of a [target] stream profile.
     *
     * It performs strict 2D bounding box math: target.width <= max.width && target.height <=
     * max.height.
     */
    public fun satisfies(target: StreamProfile, deviceLimits: DeviceLimits): Boolean {
        throw UnsupportedOperationException("Not yet implemented")
    }

    public companion object {
        /** Creates a new [StreamProfile] using the specified format and size. */
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
