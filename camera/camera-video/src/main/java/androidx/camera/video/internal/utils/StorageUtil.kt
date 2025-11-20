/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.video.internal.utils

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.camera.core.Logger
import java.io.File
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.pow

/** A utility class for storage-related operations. */
public object StorageUtil {
    private const val TAG = "StorageUtil"
    public const val NO_SPACE_LEFT_MESSAGE: String = "No space left on device"

    /**
     * Gets the available bytes for a given [File].
     *
     * @param file The file to check.
     * @return The available bytes.
     * @throws IllegalArgumentException if the file system access fails.
     */
    @JvmStatic
    public fun getAvailableBytes(file: File): Long {
        return getAvailableBytes(file.path)
    }

    /**
     * Gets the available bytes for a given file path.
     *
     * @param path The file path to check.
     * @return The available bytes.
     * @throws IllegalArgumentException if the file system access fails.
     */
    @JvmStatic
    public fun getAvailableBytes(path: String): Long {
        return StatFs(path).availableBytes
    }

    /**
     * Gets the available bytes for a given MediaStore URI.
     *
     * @param uri The MediaStore URI to check.
     * @return The available bytes for the storage associated with the URI. If the URI points to an
     *   unknown volume, returns [Long.MAX_VALUE].
     * @throws IllegalArgumentException if the file system access fails.
     */
    @JvmStatic
    public fun getAvailableBytesForMediaStoreUri(uri: Uri): Long {
        check(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Not a content uri: $uri" }
        return when (uri.pathSegments[0]) {
            MediaStore.VOLUME_EXTERNAL,
            MediaStore.VOLUME_EXTERNAL_PRIMARY ->
                getAvailableBytes(Environment.getExternalStorageDirectory())
            MediaStore.VOLUME_INTERNAL -> getAvailableBytes(Environment.getDataDirectory())
            else -> {
                Logger.w(TAG, "Unknown MediaStore URI: $uri")
                Long.MAX_VALUE // Assume unlimited space for unknown volumes
            }
        }
    }

    /**
     * Formats a byte size into a human-readable string.
     *
     * @param bytes The byte size to format.
     * @return The formatted string representation of the size.
     * @throws IllegalArgumentException if `bytes` is negative.
     */
    @JvmStatic
    public fun formatSize(bytes: Long): String {
        require(bytes >= 0) { "Bytes cannot be negative" }
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val decimalFormat = DecimalFormat("#.##")
        var unitIndex = 0
        var size = bytes.toDouble()

        // Find the largest unit that's at least 1
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024.0
            unitIndex++
        }

        // If the size is less than 1024, just return the formatted value with the largest unit
        if (unitIndex == 0) {
            return decimalFormat.format(size) + " " + units[unitIndex]
        }

        // Otherwise, build a string with multiple units
        val result = java.lang.StringBuilder()
        var remainingSize = bytes.toDouble()
        for (i in unitIndex downTo 0) {
            val unitSize = 1024.0.pow(i.toDouble())
            val unitValue = floor(remainingSize / unitSize)
            if (unitValue > 0) {
                result
                    .append(decimalFormat.format(unitValue))
                    .append(" ")
                    .append(units[i])
                    .append(" ")
                remainingSize -= unitValue * unitSize
            }
        }
        return result.trim().toString()
    }

    /**
     * Checks if a given throwable, or any of its causes, indicates a storage full condition.
     *
     * See b/43859084 for more detail.
     */
    @JvmStatic
    public fun isStorageFullException(throwable: Throwable?): Boolean {
        if (throwable == null) {
            return false
        }

        if (throwable.message?.contains(NO_SPACE_LEFT_MESSAGE) == true) {
            return true
        }

        return isStorageFullException(throwable.cause)
    }
}
