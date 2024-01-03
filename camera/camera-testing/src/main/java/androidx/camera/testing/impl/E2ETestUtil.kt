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
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

private const val TAG = "E2ETestUtil"
private const val EXTENSION_MP4 = "mp4"
private const val EXTENSION_TEXT = "txt"

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
object E2ETestUtil {

    /**
     * Write the given text to the external storage.
     *
     * @param text the text to write to the external storage.
     * @param fileName the file name to save the text.
     * @param extension the file extension to save the text, [EXTENSION_TEXT] will be used by
     * default.
     *
     * @return the [FileOutputOptions] instance.
     */
    fun writeTextToExternalFile(
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
        Logger.d(TAG, "Export test information to: ${file.path}")
    }

    /**
     * Check if the media store is available to save video recordings.
     *
     * @return true if the media store can be used, false otherwise.
     * @see MediaStoreVideoCannotWrite
     */
    fun canDeviceWriteToMediaStore(): Boolean {
        return DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null
    }

    /**
     * Create a [FileOutputOptions] for video recording with some default values.
     *
     * @param fileName the file name of the video recording.
     * @param extension the file extension of the video recording, [EXTENSION_MP4] will be used by
     * default.
     *
     * @return the [FileOutputOptions] instance.
     */
    fun generateVideoFileOutputOptions(
        fileName: String,
        extension: String = EXTENSION_MP4
    ): FileOutputOptions {
        val fileNameWithExtension = "$fileName.$extension"
        val folder = getExternalStoragePublicDirectory(DIRECTORY_MOVIES)
        if (!folder.exists() && !folder.mkdirs()) {
            Logger.e(TAG, "Failed to create directory: $folder")
        }
        return FileOutputOptions.Builder(File(folder, fileNameWithExtension)).build()
    }

    /**
     * Create a [MediaStoreOutputOptions] for video recording with some default values.
     *
     * @param contentResolver the [ContentResolver] instance.
     * @param fileName the file name of the video recording.
     *
     * @return the [MediaStoreOutputOptions] instance.
     */
    fun generateVideoMediaStoreOptions(
        contentResolver: ContentResolver,
        fileName: String
    ): MediaStoreOutputOptions {
        val contentValues = generateVideoContentValues(fileName)

        return MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
    }

    private fun generateVideoContentValues(fileName: String): ContentValues {
        val res = ContentValues()
        res.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        res.put(MediaStore.Video.Media.TITLE, fileName)
        res.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        res.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        res.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())

        return res
    }
}
