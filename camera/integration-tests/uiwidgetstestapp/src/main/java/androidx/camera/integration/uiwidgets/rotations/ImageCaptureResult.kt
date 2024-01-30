/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.rotations

import android.content.ContentResolver
import android.net.Uri
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.Exif
import java.io.File

/** A wrapper around an ImageCapture result, used for testing. */
sealed class ImageCaptureResult {

    abstract fun getResolution(): Size

    abstract fun getRotation(): Int

    abstract fun delete()

    class InMemory(private val imageProxy: ImageProxy) : ImageCaptureResult() {
        override fun getResolution() = Size(imageProxy.width, imageProxy.height)
        override fun getRotation() = imageProxy.imageInfo.rotationDegrees
        override fun delete() {}
    }

    class FileOrOutputStream(private val file: File) : ImageCaptureResult() {
        private val exif = Exif.createFromFile(file)
        override fun getResolution() = Size(exif.width, exif.height)
        override fun getRotation() = exif.rotation
        override fun delete() {
            file.delete()
        }
    }

    class MediaStore(private val contentResolver: ContentResolver, private val uri: Uri) :
        ImageCaptureResult() {

        private val exif: Exif

        init {
            val inputStream = contentResolver.openInputStream(uri)
            exif = Exif.createFromInputStream(inputStream!!)
        }

        override fun getResolution() = Size(exif.width, exif.height)
        override fun getRotation() = exif.rotation
        override fun delete() {
            contentResolver.delete(uri, null, null)
        }
    }
}
