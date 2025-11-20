/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.utils

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Opens a [ParcelFileDescriptor] for the given [Uri].
 *
 * @param context The application context.
 * @param uri The [Uri] of the file to open.
 * @param fileOpenMode The mode in which to open the file (e.g., "r" for read, "w" for write).
 * @return A [ParcelFileDescriptor] for the opened file.
 * @throws IOException If the file cannot be opened.
 */
internal fun openFileDescriptor(
    context: Context,
    uri: Uri,
    fileOpenMode: String,
): ParcelFileDescriptor {
    return context.contentResolver.openFileDescriptor(uri, fileOpenMode)
        ?: throw IOException("Failed to open PDF file")
}

/**
 * Creates a [ParcelFileDescriptor] for reading and writing to a file in the app's internal storage.
 *
 * @param context The application context.
 * @param fileName The name of the file to create.
 * @param fileOpenMode The mode in which to open the file (e.g., "w" for write, "rw" for
 *   read-write).
 * @return A [ParcelFileDescriptor] for the opened file.
 * @throws IOException If the file cannot be created or opened.
 */
internal fun createPfd(
    context: Context,
    fileName: String,
    fileOpenMode: String,
): ParcelFileDescriptor {
    val appSpecificDir = context.filesDir
    val file = File(appSpecificDir, fileName)
    if (!file.exists()) {
        file.createNewFile()
    }

    val uri = Uri.fromFile(file)
    return openFileDescriptor(context, uri, fileOpenMode)
}

/**
 * Reads the content of a file descriptor as a string.
 *
 * @param pfd The [ParcelFileDescriptor] to read from.
 * @return The content of the file as a string.
 */
internal fun readFromPfd(pfd: ParcelFileDescriptor): String {
    // It is the responsibility of the caller to close this pfd.
    FileInputStream(pfd.fileDescriptor).use { fileInputStream ->
        val reader = InputStreamReader(fileInputStream)
        // TODO: b/434864732 use streams to read annotations from file.
        val result = reader.readText()

        // To reuse the same PFD for reading after writing, we need to reset its file pointer
        pfd.resetToStartingPosition()
        return result
    }
}

/** Resets the given [ParcelFileDescriptor]'s position to the beginning of the file. */
internal fun ParcelFileDescriptor.resetToStartingPosition() {
    FileInputStream(this.fileDescriptor).channel.position(0)
}
