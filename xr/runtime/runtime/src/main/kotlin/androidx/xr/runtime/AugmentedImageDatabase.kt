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
 * Defines an augmented image database that is going to be used by the image tracker to detect
 * images by setting it through the [androidx.xr.runtime.Config.augmentedImageDatabase] parameter
 */
public class AugmentedImageDatabase {

    private val _entries: MutableList<AugmentedImageDatabaseEntry> = mutableListOf()

    /**
     * @property entries The list of [AugmentedImageDatabaseEntry] objects currently in the database
     */
    public val entries: List<AugmentedImageDatabaseEntry>
        get() = _entries.toList()

    /**
     * Creates a copy of this [AugmentedImageDatabase]
     *
     * @return A new [AugmentedImageDatabase] instance with the same entries
     */
    public fun copy(): AugmentedImageDatabase {
        val newDatabase = AugmentedImageDatabase()
        newDatabase._entries.addAll(this._entries)
        return newDatabase
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AugmentedImageDatabase) return false

        if (_entries != other._entries) return false

        return true
    }

    override fun hashCode(): Int {
        return _entries.hashCode()
    }

    /**
     * Creates a new augmented image database entry from a bitmap and adds it to the augmented image
     * database
     *
     * @param mode The mode used to detect the image
     * @param bitmap The bitmap of the image in [android.graphics.Bitmap.Config.ARGB_8888] format
     * @param widthInMeters The physical width of the image in meters. If zero, the physical width
     *   will be estimated if the device supports it. If physical size estimation is not supported,
     *   configuring the [Session] adding an entry with widthInMeters being 0f or lower will throw
     *   an [IllegalArgumentException]
     * @return The zero-based positional index of the image within the database
     * @throws IllegalArgumentException if the bitmap format is different from `ARGB_8888`
     */
    @JvmOverloads
    public fun addAugmentedImageDatabaseEntry(
        mode: AugmentedImageDatabaseEntryMode,
        bitmap: Bitmap,
        widthInMeters: Float = 0f,
    ): Int {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            throw IllegalArgumentException("Unsupported bitmap format.")
        }

        val imageDatabaseEntry = AugmentedImageDatabaseEntry(bitmap, mode, widthInMeters)
        _entries.add(imageDatabaseEntry)
        return _entries.indexOf(imageDatabaseEntry)
    }
}
