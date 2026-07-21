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
 * Represents the size of a stream in a stream profile.
 *
 * A `StreamSize` can represent either:
 * - A **concrete (absolute) size** (e.g., [VGA], [S1080P_16_9]): These have a fixed resolution that
 *   is independent of the device.
 * - An **abstract size** (e.g., [PREVIEW], [RECORD], [MAXIMUM]): These represent device-dependent
 *   limits and must be resolved to a concrete [Size] using [StreamProfile.resolve] with a
 *   [DeviceLimits] instance.
 *
 * Use [from] to create a [StreamSize] from a concrete [Size].
 *
 * @see DeviceLimits
 * @see StreamProfile
 */
public abstract class StreamSize internal constructor() {

    /**
     * The concrete [Size] associated with this [StreamSize], or `null` if this is an abstract size.
     *
     * Abstract sizes (like [PREVIEW] or [MAXIMUM]) must be resolved using [StreamProfile.resolve]
     * with [DeviceLimits] to get their concrete size.
     */
    public abstract val absoluteSize: Size?

    public companion object {
        /** Concrete size representing VGA resolution (640x480). */
        @JvmField public val VGA: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.VGA)

        /** Concrete size representing XGA resolution (1024x768). */
        @JvmField public val XGA: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.XGA)

        /** Concrete size representing 720p resolution (1280x720) with a 16:9 aspect ratio. */
        @JvmField
        public val S720P_16_9: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S720P_16_9)

        /**
         * Abstract size representing the preview resolution.
         *
         * Resolved using [DeviceLimits.maxPreviewSize].
         */
        @JvmField public val PREVIEW: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.PREVIEW)

        /** Concrete size representing 1080p resolution (1440x1080) with a 4:3 aspect ratio. */
        @JvmField
        public val S1080P_4_3: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1080P_4_3)

        /** Concrete size representing 1080p resolution (1920x1080) with a 16:9 aspect ratio. */
        @JvmField
        public val S1080P_16_9: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1080P_16_9)

        /** Concrete size representing 1440p resolution (1920x1440) with a 4:3 aspect ratio. */
        @JvmField
        public val S1440P_4_3: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1440P_4_3)

        /** Concrete size representing 1440p resolution (2560x1440) with a 16:9 aspect ratio. */
        @JvmField
        public val S1440P_16_9: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.S1440P_16_9)

        /** Concrete size representing UHD resolution (3840x2160) with a 16:9 aspect ratio. */
        @JvmField public val UHD: StreamSize = StreamSizeImpl.Absolute(AbsoluteSizeEnum.UHD)

        /**
         * Abstract size representing the camera device's maximum supported recording resolution.
         *
         * Resolved using [DeviceLimits.maxRecordSize].
         */
        @JvmField public val RECORD: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.RECORD)

        /**
         * Abstract size representing the camera device's maximum output resolution.
         *
         * Resolved using [DeviceLimits.maxOutputSizes] for the corresponding format.
         */
        @JvmField public val MAXIMUM: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.MAXIMUM)

        /**
         * Abstract size representing the camera device's maximum 4:3 output resolution.
         *
         * Resolved using [DeviceLimits.maxOutputSizes4by3] for the corresponding format.
         */
        @JvmField
        public val MAXIMUM_4_3: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.MAXIMUM_4_3)

        /**
         * Abstract size representing the camera device's maximum 16:9 output resolution.
         *
         * Resolved using [DeviceLimits.maxOutputSizes16by9] for the corresponding format.
         */
        @JvmField
        public val MAXIMUM_16_9: StreamSize = StreamSizeImpl.Abstract(AbstractSizeEnum.MAXIMUM_16_9)

        /**
         * Abstract size representing the camera device's maximum resolution sensor pixel mode.
         *
         * Resolved using [DeviceLimits.maxUltraOutputSizes] for the corresponding format.
         */
        @JvmField
        public val ULTRA_MAXIMUM: StreamSize =
            StreamSizeImpl.Abstract(AbstractSizeEnum.ULTRA_MAXIMUM)

        /**
         * Creates a [StreamSize] from a concrete [Size].
         *
         * If the [size] matches one of the predefined absolute sizes (e.g., [VGA], [S1080P_16_9]),
         * the corresponding shared instance is returned. Otherwise, a new custom [StreamSize]
         * instance is created.
         */
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
