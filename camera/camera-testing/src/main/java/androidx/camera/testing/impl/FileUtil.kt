/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Logger
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

private const val TAG = "FileUtil"
private const val EXTENSION_MP4 = "mp4"
private const val EXTENSION_TEXT = "txt"

public object FileUtil {

    /**
     * Write the given text to the external storage.
     *
     * @param text the text to write to the external storage.
     * @param fileName the file name to save the text.
     * @param extension the file extension to save the text, [EXTENSION_TEXT] will be used by
     *   default.
     * @return the [FileOutputOptions] instance.
     */
    @JvmStatic
    public fun writeTextToExternalFile(
        text: String,
        fileName: String,
        extension: String = EXTENSION_TEXT
    ) {
        val fileNameWithExtension = "$fileName.$extension"
        val folder = getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)
        if (!folder.exists() && !folder.mkdirs()) {
            Logger.e(TAG, "Failed to create directory: $folder")
        }

        val file = File(folder, fileNameWithExtension)
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(text)
                writer.flush()
                fos.fd.sync()
                writer.close()
                fos.close()
            }
        }
        Logger.d(TAG, "Wrote [$text] to: ${file.path}")
    }

    /**
     * Check if the media store is available to save video recordings.
     *
     * @return true if the media store can be used, false otherwise.
     * @see MediaStoreVideoCannotWrite
     */
    @JvmStatic
    public fun canDeviceWriteToMediaStore(): Boolean {
        return DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null
    }

    /**
     * Create a [FileOutputOptions] for video recording with some default values.
     *
     * @param fileName the file name of the video recording.
     * @param extension the file extension of the video recording, [EXTENSION_MP4] will be used by
     *   default.
     * @return the [FileOutputOptions] instance.
     */
    @JvmStatic
    public fun generateVideoFileOutputOptions(
        fileName: String,
        extension: String = EXTENSION_MP4
    ): FileOutputOptions {
        val fileNameWithExtension = "$fileName.$extension"
        val folder = getExternalStoragePublicDirectory(DIRECTORY_MOVIES)
        if (!createFolder(folder)) {
            Logger.e(TAG, "Failed to create directory: $folder")
        }
        return FileOutputOptions.Builder(File(folder, fileNameWithExtension)).build()
    }

    /**
     * Create a [MediaStoreOutputOptions] for video recording with some default values.
     *
     * @param contentResolver the [ContentResolver] instance.
     * @param fileName the file name of the video recording.
     * @return the [MediaStoreOutputOptions] instance.
     */
    @JvmStatic
    public fun generateVideoMediaStoreOptions(
        contentResolver: ContentResolver,
        fileName: String
    ): MediaStoreOutputOptions =
        MediaStoreOutputOptions.Builder(
                contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            .setContentValues(generateVideoContentValues(fileName))
            .build()

    /**
     * Check if the given file name string is valid.
     *
     * Currently a file name is invalid if:
     * - it is `null`.
     * - its length is zero.
     * - it contains only whitespace character.
     *
     * @param fileName the file name string to be checked.
     * @return `true` if the given file name is valid, otherwise `false`.
     */
    @JvmStatic
    public fun isFileNameValid(fileName: String?): Boolean {
        return !fileName.isNullOrBlank()
    }

    /**
     * Creates parent folder for the input file path.
     *
     * @param filePath the input file path to create its parent folder.
     * @return `true` if the parent folder already exists or is created successfully. `false` if the
     *   existing parent folder path is not a folder or failed to create.
     */
    @JvmStatic
    public fun createParentFolder(filePath: String): Boolean {
        return createParentFolder(File(filePath))
    }

    /**
     * Creates parent folder for the input file.
     *
     * @param file the input file to create its parent folder
     * @return `true` if the parent folder already exists or is created successfully. `false` if the
     *   existing parent folder path is not a folder or failed to create.
     */
    @JvmStatic
    public fun createParentFolder(file: File): Boolean =
        file.parentFile?.let { createFolder(it) } ?: false

    /**
     * Creates folder for the input file.
     *
     * @param file the input file to create folder
     * @return `true` if the folder already exists or is created successfully. `false` if the
     *   existing folder path is not a folder or failed to create.
     */
    @JvmStatic
    public fun createFolder(file: File): Boolean =
        if (file.exists()) {
            file.isDirectory
        } else {
            file.mkdirs()
        }

    /**
     * Gets the absolute path from a Uri.
     *
     * @param resolver the content resolver.
     * @param contentUri the content uri.
     * @return the file path of the content uri or null if not found.
     */
    @JvmStatic
    public fun getAbsolutePathFromUri(resolver: ContentResolver, contentUri: Uri): String? {
        // MediaStore.Video.Media.DATA was deprecated in API level 29.
        val column = MediaStore.Video.Media.DATA
        try {
            resolver.query(contentUri, arrayOf(column), null, null, null)!!.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                cursor.moveToFirst()
                return cursor.getString(columnIndex)
            }
        } catch (e: RuntimeException) {
            Log.e(
                TAG,
                String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri,
                    e
                ),
                e
            )
            return null
        }
    }

    private fun generateVideoContentValues(fileName: String) =
        ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, fileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            val currentTimeMs = System.currentTimeMillis()
            put(MediaStore.Video.Media.DATE_ADDED, currentTimeMs / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, currentTimeMs)
        }
}
