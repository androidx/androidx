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
import android.location.Location
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OutputOptionsTest {

    companion object {
        private const val FILE_SIZE_LIMIT = 1024L
        private const val DURATION_LIMIT = 10000L
        private const val INVALID_FILE_SIZE_LIMIT = -1L
        private const val INVALID_DURATION_LIMIT = -1L
    }

    @Test
    fun canBuildFileOutputOptions() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()

        val fileOutputOptions = FileOutputOptions.Builder(savedFile).build()

        assertThat(fileOutputOptions).isNotNull()
        assertThat(fileOutputOptions.file).isEqualTo(savedFile)
        savedFile.delete()
    }

    @Test
    fun canBuildMediaStoreOutputOptions() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val context: Context = ApplicationProvider.getApplicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        val fileName = "OutputOptionTest"
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.TITLE, fileName)
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            }

        val mediaStoreOutputOptions =
            MediaStoreOutputOptions.Builder(
                    contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                )
                .setContentValues(contentValues)
                .build()

        assertThat(mediaStoreOutputOptions).isNotNull()
        assertThat(mediaStoreOutputOptions.contentResolver).isEqualTo(contentResolver)
        assertThat(mediaStoreOutputOptions.collectionUri)
            .isEqualTo(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        assertThat(mediaStoreOutputOptions.contentValues).isEqualTo(contentValues)
    }

    @Test
    fun canBuildFileDescriptorOutputOptions() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val savedFile = File.createTempFile("CameraX", ".tmp")
        savedFile.deleteOnExit()
        ParcelFileDescriptor.open(savedFile, ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
            val fdOutputOptions = FileDescriptorOutputOptions.Builder(pfd).build()

            assertThat(fdOutputOptions).isNotNull()
            assertThat(fdOutputOptions.parcelFileDescriptor).isEqualTo(pfd)
        }
        savedFile.delete()
    }

    @Test
    fun mediaStore_builderContainsCorrectDefaults() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val context: Context = ApplicationProvider.getApplicationContext()
        val contentResolver: ContentResolver = context.contentResolver

        val mediaStoreOutputOptions =
            MediaStoreOutputOptions.Builder(
                    contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                )
                .build()

        assertThat(mediaStoreOutputOptions.contentValues)
            .isEqualTo(MediaStoreOutputOptions.EMPTY_CONTENT_VALUES)
    }

    @Test
    fun canBuildOutputOptions() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val outputOptions =
            FakeOutputOptions.Builder()
                .setFileSizeLimit(FILE_SIZE_LIMIT)
                .setDurationLimitMillis(DURATION_LIMIT)
                .build()

        assertThat(outputOptions).isNotNull()
        assertThat(outputOptions.fileSizeLimit).isEqualTo(FILE_SIZE_LIMIT)
        assertThat(outputOptions.durationLimitMillis).isEqualTo(DURATION_LIMIT)
    }

    @Test
    fun defaultValuesCorrect() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        val outputOptions = FakeOutputOptions.Builder().build()

        assertThat(outputOptions.location).isNull()
        assertThat(outputOptions.fileSizeLimit).isEqualTo(OutputOptions.FILE_SIZE_UNLIMITED)
        assertThat(outputOptions.durationLimitMillis).isEqualTo(OutputOptions.DURATION_UNLIMITED)
    }

    @Test
    fun invalidFileSizeLimit_throwsException() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            FakeOutputOptions.Builder().setFileSizeLimit(INVALID_FILE_SIZE_LIMIT)
        }
    }

    @Test
    fun invalidDurationLimit_throwsException() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            FakeOutputOptions.Builder().setDurationLimitMillis(INVALID_DURATION_LIMIT)
        }
    }

    @Test
    fun setValidLocation() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        listOf(
                createLocation(0.0, 0.0),
                createLocation(90.0, 180.0),
                createLocation(-90.0, -180.0),
                createLocation(10.1234, -100.5678),
            )
            .forEach { location ->
                val outputOptions = FakeOutputOptions.Builder().setLocation(location).build()

                assertWithMessage("Test $location failed")
                    .that(outputOptions.location)
                    .isEqualTo(location)
            }
    }

    @Test
    fun setInvalidLocation() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )
        listOf(
                createLocation(Double.NaN, 0.0),
                createLocation(0.0, Double.NaN),
                createLocation(90.5, 0.0),
                createLocation(-90.5, 0.0),
                createLocation(0.0, 180.5),
                createLocation(0.0, -180.5),
            )
            .forEach { location ->
                assertThrows(IllegalArgumentException::class.java) {
                    FakeOutputOptions.Builder().setLocation(location)
                }
            }
    }

    private fun createLocation(
        latitude: Double,
        longitude: Double,
        provider: String = "FakeProvider",
    ): Location =
        Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
        }
}
