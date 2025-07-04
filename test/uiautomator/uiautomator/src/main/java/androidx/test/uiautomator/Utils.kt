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

package androidx.test.uiautomator

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

/**
 * Utility function to save a bitmap onto the given [file].
 *
 * @param file the file to store the bitmap.
 * @param compressFormat the [Bitmap.CompressFormat] to use when storing the bitmap. Default is jpg.
 * @param quality the quality for the compression. This is always 100 for png.
 * @return whether the bitmap was correctly saved onto a file.
 */
public fun Bitmap.saveToFile(
    file: File,
    compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 100,
): Boolean = compress(compressFormat, quality, file.outputStream())

/**
 * Utility function to save a bitmap onto the given [parcelFileDescriptor].
 *
 * @param parcelFileDescriptor the file descriptor pointing to a file to store the bitmap.
 * @param compressFormat the [Bitmap.CompressFormat] to use when storing the bitmap. Default is jpg.
 * @param quality the quality for the compression. This is always 100 for png.
 * @return whether the bitmap was correctly saved onto a file.
 */
public fun Bitmap.saveToFile(
    parcelFileDescriptor: ParcelFileDescriptor,
    compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 100,
): Boolean =
    compress(compressFormat, quality, FileOutputStream(parcelFileDescriptor.fileDescriptor))

/**
 * Thrown when an element is not found after invoking [androidx.test.uiautomator.onElement] or
 * [androidx.test.uiautomator.onElements].
 */
public class ElementNotFoundException(msg: String? = null) : Exception(msg)
