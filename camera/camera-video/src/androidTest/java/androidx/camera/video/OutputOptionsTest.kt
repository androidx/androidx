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

@SmallTest
@RunWith(AndroidJUnit4::class)
class OutputOptionsTest {

    companion object {
        private const val FILE_SIZE_LIMIT = 1024L
    }

    @Test
    fun canBuildFileOutputOptions() {
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        val fileOutputOptions = FileOutputOptions.builder()
            .setFile(savedFile)
            .setFileSizeLimit(FILE_SIZE_LIMIT)
            .build()

        assertThat(fileOutputOptions).isNotNull()
        assertThat(fileOutputOptions.type).isEqualTo(OutputOptions.OPTIONS_TYPE_FILE)
        assertThat(fileOutputOptions.file).isNotNull()
        assertThat(fileOutputOptions.fileSizeLimit).isEqualTo(FILE_SIZE_LIMIT)
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

        val mediaStoreOutputOptions = MediaStoreOutputOptions.builder()
            .setContentResolver(contentResolver)
            .setFileSizeLimit(FILE_SIZE_LIMIT)
            .setCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        assertThat(mediaStoreOutputOptions.contentResolver).isEqualTo(contentResolver)
        assertThat(mediaStoreOutputOptions.collection).isEqualTo(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
        assertThat(mediaStoreOutputOptions.contentValues).isEqualTo(contentValues)
        assertThat(mediaStoreOutputOptions.type).isEqualTo(OutputOptions.OPTIONS_TYPE_MEDIA_STORE)
        assertThat(mediaStoreOutputOptions.fileSizeLimit).isEqualTo(FILE_SIZE_LIMIT)
    }

    @Test
    fun canBuildFileDescriptorOutputOptions() {
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()
        ParcelFileDescriptor.open(
            savedFile,
            ParcelFileDescriptor.MODE_READ_WRITE
        ).use { pfd ->
            val fdOutputOptions = FileDescriptorOutputOptions.builder()
                .setParcelFileDescriptor(pfd)
                .setFileSizeLimit(FILE_SIZE_LIMIT)
                .build()

            assertThat(fdOutputOptions).isNotNull()
            assertThat(fdOutputOptions.type).isEqualTo(OutputOptions.OPTIONS_TYPE_FILE_DESCRIPTOR)
            assertThat(fdOutputOptions.parcelFileDescriptor).isNotNull()
            assertThat(fdOutputOptions.fileSizeLimit).isEqualTo(FILE_SIZE_LIMIT)
        }
        savedFile.delete()
    }

    @Test
    fun file_builderContainsCorrectDefaults() {
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()
        val fileOutputOptions = FileOutputOptions.builder()
            .setFile(savedFile)
            .build()

        assertThat(fileOutputOptions.fileSizeLimit).isEqualTo(OutputOptions.FILE_SIZE_UNLIMITED)
        savedFile.delete()
    }

    @Test
    fun mediaStore_builderContainsCorrectDefaults() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        val fileName = "OutputOptionTest"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.TITLE, fileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.builder()
            .setContentResolver(contentResolver)
            .setCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        assertThat(mediaStoreOutputOptions.fileSizeLimit)
            .isEqualTo(OutputOptions.FILE_SIZE_UNLIMITED)
    }

    @Test
    fun fileDescriptor_builderContainsCorrectDefaults() {
        val savedFile = File.createTempFile("CameraX", ".tmp")
        ParcelFileDescriptor.open(
            savedFile,
            ParcelFileDescriptor.MODE_READ_WRITE
        ).use { pfd ->
            val fdOutputOptions = FileDescriptorOutputOptions.builder()
                .setParcelFileDescriptor(pfd)
                .build()

            assertThat(fdOutputOptions.fileSizeLimit).isEqualTo(OutputOptions.FILE_SIZE_UNLIMITED)
        }
        savedFile.delete()
    }
}