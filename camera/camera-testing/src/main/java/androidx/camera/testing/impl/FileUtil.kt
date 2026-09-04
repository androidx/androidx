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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
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
private val SANITIZE_FILENAME_REGEX = Regex("[^a-zA-Z0-9_-]")
private val STRIP_IMAGE_EXTENSION_REGEX = Regex("\\.(png|jpe?g|webp)$", RegexOption.IGNORE_CASE)

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
        extension: String = EXTENSION_TEXT,
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
     * Saves a [Bitmap] to the specified directory with the given file name.
     *
     * The file name will be sanitized to remove invalid characters and stripped of extensions
     * before appending the appropriate extension based on the [format].
     *
     * @param bitmap the [Bitmap] to save.
     * @param directory the directory [File] where the bitmap should be saved.
     * @param name the file name to save the bitmap as (e.g., "testName_methodName").
     * @param format the [Bitmap.CompressFormat] to use (default: [Bitmap.CompressFormat.PNG]).
     * @param quality the compression quality from 0 to 100 (default: 100).
     * @return the saved [File] if successful, or `null` if failed.
     */
    @JvmStatic
    @JvmOverloads
    public fun saveBitmap(
        bitmap: Bitmap,
        directory: File,
        name: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
    ): File? {
        if (bitmap.isRecycled) {
            Logger.e(TAG, "Cannot save a recycled bitmap for: $name")
            return null
        }
        var testFile: File? = null
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                Logger.e(TAG, "Failed to create directory: $directory")
                return null
            }
            val fileName = buildSanitizedFileName(name, format)
            testFile = File(directory, fileName)

            val compressed =
                FileOutputStream(testFile).use { fos ->
                    bitmap.compress(format, quality, fos)
                }
            if (!compressed) {
                Logger.e(TAG, "Failed to compress bitmap for: $name")
                testFile.delete()
                return null
            }

            Logger.d(TAG, "Saved bitmap to: $testFile")
            return testFile
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to save bitmap for: $name", t)
            testFile?.let {
                if (it.exists() && it.length() == 0L) {
                    it.delete()
                }
            }
            return null
        }
    }

    /**
     * Saves a [Bitmap] to the MediaStore under the specified relative path (Android 10+ / API 29+).
     *
     * An entry is created in MediaStore primary storage volume
     * ([MediaStore.VOLUME_EXTERNAL_PRIMARY]) with [MediaStore.Images.Media.IS_PENDING] set to 1
     * while streaming content. Once finished, [MediaStore.Images.Media.IS_PENDING] is set back to 0
     * to make it available to the system.
     *
     * @param contentResolver the [ContentResolver] used to access the MediaStore.
     * @param bitmap the [Bitmap] to save.
     * @param relativePath the relative path under external storage (e.g., "Pictures/test_output").
     * @param name the file name to save the bitmap as (e.g., "testName_methodName").
     * @param format the [Bitmap.CompressFormat] to use (default: [Bitmap.CompressFormat.PNG]).
     * @param quality the compression quality from 0 to 100 (default: 100).
     * @return the saved [File] if successful, or `null` if failed.
     */
    @RequiresApi(29)
    @JvmStatic
    @JvmOverloads
    public fun saveBitmapToMediaStore(
        contentResolver: ContentResolver,
        bitmap: Bitmap,
        relativePath: String,
        name: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
    ): File? {
        if (bitmap.isRecycled) {
            Logger.e(TAG, "Cannot save a recycled bitmap for: $name")
            return null
        }
        val fileName = buildSanitizedFileName(name, format)
        val mimeType = getMimeTypeForFormat(format)

        var uri: Uri? = null
        val resolver = contentResolver
        val normalizedRelativePath =
            if (relativePath.endsWith("/")) relativePath else "$relativePath/"
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        try {
            // Remove any pre-existing MediaStore entry with same display name and relative path to
            // avoid duplicate "(1)" files.
            try {
                val selection =
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
                        "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, normalizedRelativePath)
                resolver.delete(collection, selection, selectionArgs)
            } catch (e: Exception) {
                Logger.w(
                    TAG,
                    "Failed to delete existing entry for $fileName in $normalizedRelativePath",
                    e,
                )
            }

            val currentTimeMs = System.currentTimeMillis()
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, normalizedRelativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    put(MediaStore.MediaColumns.DATE_ADDED, currentTimeMs / 1000)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimeMs / 1000)
                }

            uri = resolver.insert(collection, contentValues)
            if (uri == null) {
                Logger.e(TAG, "Failed to create MediaStore entry for $fileName")
                return null
            }

            val compressed =
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(format, quality, outputStream)
                } ?: false

            if (!compressed) {
                Logger.e(TAG, "Failed to compress bitmap to MediaStore for: $name")
                resolver.delete(uri, null, null)
                return null
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            val targetFile =
                try {
                    @Suppress("DEPRECATION")
                    resolver
                        .query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DATA),
                            null,
                            null,
                            null,
                        )
                        ?.use { cursor ->
                            @Suppress("DEPRECATION")
                            val columnIndex =
                                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                            if (cursor.moveToFirst()) {
                                File(cursor.getString(columnIndex))
                            } else {
                                null
                            }
                        }
                } catch (e: RuntimeException) {
                    Logger.e(TAG, "Failed to resolve file path for MediaStore Uri: $uri", e)
                    null
                }

            if (targetFile == null) {
                Logger.e(TAG, "Failed to resolve file path for: $fileName (uri: $uri)")
                try {
                    resolver.delete(uri, null, null)
                } catch (ignored: Exception) {}
                return null
            }

            Logger.d(TAG, "Saved bitmap to MediaStore: $targetFile (uri: $uri)")
            return targetFile
        } catch (t: Throwable) {
            Logger.e(TAG, "Failed to save bitmap to MediaStore for: $name", t)
            uri?.let {
                try {
                    resolver.delete(it, null, null)
                } catch (ignored: Exception) {}
            }
            return null
        }
    }

    private fun getExtensionForFormat(format: Bitmap.CompressFormat): String =
        when {
            format == Bitmap.CompressFormat.JPEG -> "jpg"
            format == Bitmap.CompressFormat.PNG -> "png"
            format.name.startsWith("WEBP") -> "webp"
            else -> format.name.lowercase()
        }

    private fun getMimeTypeForFormat(format: Bitmap.CompressFormat): String =
        when {
            format == Bitmap.CompressFormat.JPEG -> "image/jpeg"
            format == Bitmap.CompressFormat.PNG -> "image/png"
            format.name.startsWith("WEBP") -> "image/webp"
            else -> "image/${getExtensionForFormat(format)}"
        }

    private fun buildSanitizedFileName(name: String, format: Bitmap.CompressFormat): String {
        val ext = getExtensionForFormat(format)
        val sanitizedName =
            File(name)
                .name
                .replace(STRIP_IMAGE_EXTENSION_REGEX, "")
                .replace(SANITIZE_FILENAME_REGEX, "_")
        return "$sanitizedName.$ext"
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
        extension: String = EXTENSION_MP4,
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
        fileName: String,
    ): MediaStoreOutputOptions =
        MediaStoreOutputOptions.Builder(
                contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
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
                    e,
                ),
                e,
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
