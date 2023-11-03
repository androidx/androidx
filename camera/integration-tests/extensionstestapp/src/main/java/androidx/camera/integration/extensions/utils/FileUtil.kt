/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.extensions.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.ImageFormat
import android.media.Image
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.impl.utils.Exif
import androidx.core.net.toUri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TAG = "FileUtil"

/**
 * File util functions
 */
object FileUtil {

    /**
     * Saves an [Image] to the specified file path. The format of the input [Image] must be JPEG or
     * YUV_420_888 format.
     */
    @JvmStatic
    fun saveImage(
        image: Image,
        fileNamePrefix: String,
        fileNameSuffix: String,
        relativePath: String,
        contentResolver: ContentResolver,
        rotationDegrees: Int
    ): Uri? {
        require((image.format == ImageFormat.JPEG) or (image.format == ImageFormat.YUV_420_888)) {
            "Incorrect image format of the input image proxy: ${image.format}"
        }

        val fileName = if (fileNameSuffix.isNotEmpty() && fileNameSuffix[0] == '.') {
            fileNamePrefix + fileNameSuffix
        } else {
            "$fileNamePrefix.$fileNameSuffix"
        }

        // Saves the image to the temp file
        val tempFileUri =
            saveImageToTempFile(image, fileNamePrefix, fileNameSuffix, null, rotationDegrees)
                ?: return null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        // Copies the temp file to the final output path
        return copyTempFileToOutputLocation(
            contentResolver,
            tempFileUri,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    /**
     * Saves an [Image] to a temp file.
     */
    @JvmStatic
    fun saveImageToTempFile(
        image: Image,
        prefix: String,
        suffix: String,
        cacheDir: File? = null,
        rotationDegrees: Int = 0
    ): Uri? {
        val tempFile = File.createTempFile(
            prefix,
            suffix,
            cacheDir
        )

        val byteArray = when (image.format) {
            ImageFormat.JPEG -> {
                ImageUtil.jpegImageToJpegByteArray(image)
            }
            ImageFormat.YUV_420_888 -> {
                ImageUtil.yuvImageToJpegByteArray(image, 100)
            }
            else -> {
                Log.e(TAG, "Incorrect image format of the input image proxy: ${image.format}")
                return null
            }
        }

        val outputStream = FileOutputStream(tempFile)
        outputStream.write(byteArray)
        outputStream.close()

        // Updates Exif rotation tag info
        val exif = Exif.createFromFile(tempFile)
        exif.rotate(rotationDegrees)
        exif.save()

        return tempFile.toUri()
    }

    /**
     * Copies temp file to the destination location.
     *
     * @return null if the copy process is failed.
     */
    @JvmStatic
    fun copyTempFileToOutputLocation(
        contentResolver: ContentResolver,
        tempFileUri: Uri,
        targetUrl: Uri,
        contentValues: ContentValues,
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.e(TAG, "The known devices which support Extensions should be at least" +
                " Android Q!")
            return null
        }

        contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)

        val outputUri = contentResolver.insert(targetUrl, contentValues) ?: return null

        if (copyTempFileByteArrayToOutputLocation(
                contentResolver,
                tempFileUri,
                outputUri
            )
        ) {
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(outputUri, contentValues, null, null)
            return outputUri
        } else {
            Log.e(TAG, "Failed to copy the temp file to the output path!")
        }

        return null
    }

    /**
     * Copies temp file byte array to output [Uri].
     *
     * @return false if the [Uri] is not writable.
     */
    @JvmStatic
    private fun copyTempFileByteArrayToOutputLocation(
        contentResolver: ContentResolver,
        tempFileUri: Uri,
        uri: Uri
    ): Boolean {
        contentResolver.openOutputStream(uri).use { outputStream ->
            if (tempFileUri.path == null || outputStream == null) {
                return false
            }

            val tempFile = File(tempFileUri.path!!)

            FileInputStream(tempFile).use { inputStream ->
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) {
                    outputStream.write(buf, 0, len)
                }
            }
        }
        return true
    }
}
