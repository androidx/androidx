/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileDescriptor

@SmallTest
@RunWith(AndroidJUnit4::class)
class OutputOptionsTest {

    @Test
    fun canBuildFileOutputOptions() {
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        val fileOutputOptions = FileOutputOptions.builder()
            .setFile(savedFile)
            .setFileSizeLimit(0)
            .build()

        assertThat(fileOutputOptions).isNotNull()
        assertThat(fileOutputOptions.type).isEqualTo(OutputOptions.Type.FILE)
        assertThat(fileOutputOptions.file).isNotNull()
        assertThat(fileOutputOptions.fileSizeLimit).isEqualTo(0)
        savedFile.delete()
    }

    @Test
    fun canBuildMediaStoreOutputOptions() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        val fileName = "OutputOptionTest"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, fileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        }

        val uri = contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        assertThat(uri).isNotNull()

        val mediaStoreOutputOptions = MediaStoreOutputOptions.builder()
            .setContentResolver(contentResolver)
            .setFileSizeLimit(0)
            .setUri(uri!!)
            .build()

        assertThat(mediaStoreOutputOptions.uri).isNotNull()
        assertThat(mediaStoreOutputOptions.type).isEqualTo(OutputOptions.Type.MEDIA_STORE)
        assertThat(mediaStoreOutputOptions.fileSizeLimit).isEqualTo(0)
        contentResolver.delete(uri, null, null)
    }

    @Test
    fun canBuildFileDescriptorOutputOptions() {
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()
        val pfd: ParcelFileDescriptor = ParcelFileDescriptor.open(
            savedFile,
            ParcelFileDescriptor.MODE_READ_WRITE
        )
        val fd: FileDescriptor = pfd.fileDescriptor

        val fileOutputOptions = FileDescriptorOutputOptions.builder()
            .setFileDescriptor(fd)
            .setFileSizeLimit(0)
            .build()

        assertThat(fileOutputOptions).isNotNull()
        assertThat(fileOutputOptions.type).isEqualTo(OutputOptions.Type.FILE_DESCRIPTOR)
        assertThat(fileOutputOptions.fileDescriptor).isNotNull()
        assertThat(fileOutputOptions.fileSizeLimit).isEqualTo(0)
        pfd.close()
        savedFile.delete()
    }
}