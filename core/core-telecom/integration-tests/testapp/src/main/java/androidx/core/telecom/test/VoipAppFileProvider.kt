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

package androidx.core.telecom.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.telecom.util.ExperimentalAppActions
import java.io.File
import java.io.FileOutputStream

/**
 * `VoipAppFileProvider` is a helper class for the`FileProvider` designed to handle file operations
 * specifically for storing and retrieving bitmaps associated with `VoipCall` objects. It ensures
 * that bitmaps are stored in a private directory accessible only to the app.
 */
@OptIn(ExperimentalAppActions::class)
class VoipAppFileProvider() {
    companion object {
        val TAG: String = VoipAppFileProvider::class.java.simpleName

        /**
         * The authority of this `FileProvider`, used in defining the content URI. This should match
         * the authority defined in the app's manifest file.
         */
        const val FILE_PROVIDER_AUTHORITIES = "androidx.core.telecom.test.fileprovider"

        /**
         * The name of the subdirectory within the app's files directory where bitmaps will be
         * stored.
         */
        const val FILE_DIR_NAME = "images"

        /**
         * Creates the directory for storing files if it doesn't exist.
         *
         * @param context The application context.
         * @return The created directory `File` object or throws an exception if creation failed.
         */
        private fun createDirectory(context: Context): File {
            val dir = File(context.filesDir, FILE_DIR_NAME)
            if (!dir.exists()) {
                val success = dir.mkdirs()
                if (success) {
                    Log.i(TAG, "Files directory created successfully")
                } else {
                    Log.e(TAG, "Failed to create files directory")
                    throw IllegalStateException("Failed to create directory: $dir")
                }
            }
            return dir
        }

        /**
         * Writes a bitmap associated with a `VoipCall` to a file and updates the call's icon URI.
         * This method now lazily creates the directory if it doesn't exist.
         *
         * @param context The application context.
         * @param call The `VoipCall` object containing the bitmap to be written.
         * @return The URI of the newly created file, or null if an error occurred.
         */
        fun writeCallIconBitMapToFile(context: Context, call: VoipCall): Uri? =
            try {
                val directory = createDirectory(context)
                val imageFile = File(directory, "${call.getIconFileName()}.png")

                FileOutputStream(imageFile).use { outputStream ->
                    call.getIconBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITIES, imageFile)
                call.setIconUri(uri)

                Log.d(TAG, "directory=[$directory] --> imageFile=[$imageFile] = uri=[$uri]")
                uri
            } catch (e: Exception) {
                Log.e(TAG, "Error writing bitmap to file", e)
                null
            }

        /**
         * Reads a bitmap from a file specified by its URI.
         *
         * @param context The application context.
         * @param uri The URI of the file to read.
         * @return The bitmap read from the file, or null if an error occurred.
         */
        fun readCallIconUriFromFile(context: Context, uri: Uri): Bitmap? =
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading bitmap from file", e)
                null
            }
    }
}
