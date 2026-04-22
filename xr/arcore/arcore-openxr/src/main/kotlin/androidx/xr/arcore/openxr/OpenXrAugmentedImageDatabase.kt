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

package androidx.xr.arcore.openxr

import android.graphics.Bitmap
import androidx.xr.runtime.AugmentedImageDatabase
import androidx.xr.runtime.AugmentedImageDatabaseEntry

/**
 * Wraps a native
 * [XrTrackableImageDatabaseANDROID](https://registry.khronos.org/OpenXR/specs/1.1/man/html/XrTrackableImageDatabaseANDROID.html)
 * with the [AugmentedImageDatabase] interface.
 *
 * @property entries the list of images included in the database
 */
internal class OpenXrAugmentedImageDatabase(
    internal val entries: List<OpenXrAugmentedImageDatabaseEntry> = emptyList()
) {
    /**
     * Wraps a native
     * [XrTrackableImageDatabaseEntryANDROID](https://registry.khronos.org/OpenXR/specs/1.1/man/html/XrTrackableImageDatabaseEntryANDROID.html)
     * with the [AugmentedImageDatabaseEntry] interface.
     *
     * @property mode the [androidx.xr.runtime.AugmentedImageDatabaseEntryMode] used to detect the
     *   image
     * @property width the width of the image
     * @property height the height of the image
     * @property bufferSize the size of image's buffer
     * @property buffer the [ByteArray] of the image
     * @property widthInMeters the physical width of the image in meters
     */
    internal data class OpenXrAugmentedImageDatabaseEntry(
        val mode: Int,
        val width: Int,
        val height: Int,
        val bufferSize: Int,
        val buffer: ByteArray,
        val widthInMeters: Float,
    )

    internal companion object {
        /**
         * Converts the augmentedImageDatabase into an OpenXrAugmentedImageDatabase object.
         *
         * @param database the [AugmentedImageDatabase] to convert.
         * @return an instance of [OpenXrAugmentedImageDatabase] or null if the database has no
         *   entries.
         * @throws [IllegalArgumentException] if the bitmap is not in ARGB_8888 format.
         */
        internal fun fromAugmentedImageDatabase(
            database: AugmentedImageDatabase
        ): OpenXrAugmentedImageDatabase? {
            if (database.entries.isEmpty()) {
                return null
            }

            return OpenXrAugmentedImageDatabase(
                entries =
                    database.entries.map { entry ->
                        if (entry.bitmap.config != Bitmap.Config.ARGB_8888) {
                            throw IllegalArgumentException("Unsupported bitmap format.")
                        }

                        val width = entry.bitmap.width
                        val height = entry.bitmap.height

                        val pixels = IntArray(width * height)
                        entry.bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                        val buffer = ByteArray(width * height * 4)
                        for (i in pixels.indices) {
                            val pixel = pixels[i]
                            buffer[i * 4] = ((pixel shr 16) and 0xFF).toByte()
                            buffer[i * 4 + 1] = ((pixel shr 8) and 0xFF).toByte()
                            buffer[i * 4 + 2] = (pixel and 0xFF).toByte()
                            buffer[i * 4 + 3] = ((pixel shr 24) and 0xFF).toByte()
                        }

                        OpenXrAugmentedImageDatabaseEntry(
                            mode = entry.mode.mode,
                            width = width,
                            height = height,
                            bufferSize = buffer.size,
                            buffer = buffer,
                            widthInMeters = entry.widthInMeters,
                        )
                    }
            )
        }
    }
}
