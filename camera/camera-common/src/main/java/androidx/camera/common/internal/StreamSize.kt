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

/**
 * Standard stream profile size constants.
 *
 * This class provides a stable set of supported stream sizes for use in stream profile
 * combinations. It manages both absolute resolutions and abstract size categories (e.g., MAXIMUM,
 * PREVIEW).
 */
public abstract class StreamSize internal constructor() {

    /**
     * The concrete [Size] associated with this [StreamSize].
     *
     * @return The absolute resolution, or `null` if this is an abstract size (e.g., [MAXIMUM],
     *   [PREVIEW]) that needs to be resolved against a specific device's limits.
     */
    public abstract val absoluteSize: Size?

    public companion object {
        /** Default VGA size is 640x480. */
        @JvmField public val VGA: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.VGA)

        /** XGA size refers to 1024x768. */
        @JvmField public val XGA: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.XGA)

        /** Represents 720P (1280x720) resolution of 16:9 resolution. */
        @JvmField
        public val S720P_16_9: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S720P_16_9)

        /** PREVIEW refers to the best size match to the device's screen resolution. */
        @JvmField public val PREVIEW: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.PREVIEW)

        /** Represents 1080P (1440x1080) resolution of 4:3 aspect ratio. */
        @JvmField
        public val S1080P_4_3: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1080P_4_3)

        /** Represents 1080P (1920x1080) resolution of 16:9 aspect ratio. */
        @JvmField
        public val S1080P_16_9: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1080P_16_9)

        /** Represents 1440P (1920x1440) resolution of 4:3 aspect ratio. */
        @JvmField
        public val S1440P_4_3: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1440P_4_3)

        /** Represents 1440P (2560x1440) resolution of 16:9 aspect ratio. */
        @JvmField
        public val S1440P_16_9: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1440P_16_9)

        /** Represents UHD (3840x2160) resolution, which is of 16:9 aspect ratio. */
        @JvmField public val UHD: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.UHD)

        /** RECORD refers to the camera device's maximum supported recording resolution. */
        @JvmField public val RECORD: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.RECORD)

        /** MAXIMUM refers to the camera device's maximum output resolution. */
        @JvmField public val MAXIMUM: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.MAXIMUM)

        /** Refers to the camera device's maximum 4:3 output resolution. */
        @JvmField
        public val MAXIMUM_4_3: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.MAXIMUM_4_3)

        /** Refers to the camera device's maximum 16:9 output resolution. */
        @JvmField
        public val MAXIMUM_16_9: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.MAXIMUM_16_9)

        /** ULTRA_MAXIMUM refers to the camera device's maximum resolution sensor pixel mode. */
        @JvmField
        public val ULTRA_MAXIMUM: StreamSize =
            StreamSizeImpl.Abstract(AbstractSizeEnum.ULTRA_MAXIMUM)

        /** Creates a [StreamSize] from a concrete [Size]. */
        @JvmStatic
        public fun from(size: Size): StreamSize {
            val absoluteEnum = AbsoluteSizeEnum.entries.firstOrNull { it.size == size }
            return if (absoluteEnum != null) {
                when (absoluteEnum) {
                    AbsoluteSizeEnum.VGA -> VGA
                    AbsoluteSizeEnum.XGA -> XGA
                    AbsoluteSizeEnum.S720P_16_9 -> S720P_16_9
                    AbsoluteSizeEnum.S1080P_4_3 -> S1080P_4_3
                    AbsoluteSizeEnum.S1080P_16_9 -> S1080P_16_9
                    AbsoluteSizeEnum.S1440P_4_3 -> S1440P_4_3
                    AbsoluteSizeEnum.S1440P_16_9 -> S1440P_16_9
                    AbsoluteSizeEnum.UHD -> UHD
                }
            } else {
                StreamSizeImpl.Custom(size)
            }
        }
    }
}

/**
 * Internal implementation of [StreamSize].
 *
 * This sealed class allows [StreamSize] to distinguish between absolute resolutions and abstract
 * size categories that require device-specific resolution.
 */
internal sealed class StreamSizeImpl : StreamSize() {
    /**
     * Represents a fixed resolution size.
     *
     * This allows internal logic to perform exhaustive checks using [AbsoluteSizeEnum].
     */
    data class Absolute(val value: AbsoluteSizeEnum) : StreamSizeImpl() {
        override val absoluteSize: Size
            get() = value.size

        override fun toString(): String = value.name + "(${value.size})"
    }

    /** Represents a custom fixed resolution size. */
    data class Custom(val size: Size) : StreamSizeImpl() {
        override val absoluteSize: Size
            get() = size

        override fun toString(): String = "CUSTOM($size)"
    }

    /**
     * Represents a dynamically resolved size category.
     *
     * This allows internal logic to perform exhaustive checks using [AbstractSizeEnum].
     */
    data class Abstract(val value: AbstractSizeEnum) : StreamSizeImpl() {
        override val absoluteSize: Size?
            get() = null

        override fun toString(): String = value.name
    }
}

/**
 * Internal enum representing fixed resolution sizes.
 *
 * This allows for exhaustive internal logic when handling well-known resolution constants.
 */
internal enum class AbsoluteSizeEnum(val size: Size) {
    VGA(Size(640, 480)),
    XGA(Size(1024, 768)),
    S720P_16_9(Size(1280, 720)),
    S1080P_4_3(Size(1440, 1080)),
    S1080P_16_9(Size(1920, 1080)),
    S1440P_4_3(Size(1920, 1440)),
    S1440P_16_9(Size(2560, 1440)),
    UHD(Size(3840, 2160)),
}

/**
 * Internal enum representing abstract size categories.
 *
 * This allows for exhaustive internal logic when resolving abstract sizes against device limits.
 */
internal enum class AbstractSizeEnum {
    PREVIEW,
    RECORD,
    MAXIMUM,
    MAXIMUM_4_3,
    MAXIMUM_16_9,
    ULTRA_MAXIMUM,
}
