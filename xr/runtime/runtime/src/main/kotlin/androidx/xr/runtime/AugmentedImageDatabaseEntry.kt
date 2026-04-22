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

package androidx.xr.runtime

import android.graphics.Bitmap

/**
 * Defines an augmented image database entry that is going to be added to an
 * [AugmentedImageDatabase]
 *
 * @property bitmap The bitmap of the image in [android.graphics.Bitmap.Config.ARGB_8888] format
 * @property mode The [AugmentedImageDatabaseEntryMode] used to detect the image
 * @property widthInMeters The physical width of the image in meters. If zero, the physical width
 *   will be estimated if the device supports it. If physical size estimation is not supported,
 *   configuring the [Session] adding an entry with widthInMeters being 0f or lower will throw an
 *   [IllegalArgumentException]
 */
public class AugmentedImageDatabaseEntry
constructor(
    public val bitmap: Bitmap,
    public val mode: AugmentedImageDatabaseEntryMode = AugmentedImageDatabaseEntryMode.DYNAMIC,
    public val widthInMeters: Float = 0f,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AugmentedImageDatabaseEntry) return false

        if (bitmap != other.bitmap) return false
        if (mode != other.mode) return false
        if (widthInMeters != other.widthInMeters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bitmap.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + widthInMeters.hashCode()
        return result
    }

    @JvmOverloads
    public fun copy(
        bitmap: Bitmap = this.bitmap,
        mode: AugmentedImageDatabaseEntryMode = this.mode,
        widthInMeters: Float = this.widthInMeters,
    ): AugmentedImageDatabaseEntry {
        return AugmentedImageDatabaseEntry(
            bitmap = bitmap,
            mode = mode,
            widthInMeters = widthInMeters,
        )
    }
}

/** The mode the augmented image is being detected with */
public class AugmentedImageDatabaseEntryMode private constructor(public val mode: Int) {
    public companion object {
        /**
         * Mode with the highest accuracy and the lowest latency. Used for tracking moving images.
         * It has the highest power consumption
         */
        @JvmField
        public val DYNAMIC: AugmentedImageDatabaseEntryMode = AugmentedImageDatabaseEntryMode(0)

        /**
         * Mode used to track images that are known to be static or semi-static. It has less power
         * consumption in comparison to dynamic mode. If a static image is moving, it will be
         * updated with a much higher latency
         */
        @JvmField
        public val STATIC: AugmentedImageDatabaseEntryMode = AugmentedImageDatabaseEntryMode(1)
    }
}
