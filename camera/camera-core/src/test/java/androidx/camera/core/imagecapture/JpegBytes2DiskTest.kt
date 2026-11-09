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

package androidx.camera.core.imagecapture

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.location.Location
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ERROR_FILE_IO
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.imagecapture.FileUtil.moveFileToTarget
import androidx.camera.core.imagecapture.Utils.ALTITUDE
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.TEMP_FILE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.Exif.createFromFileString
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil.createExif
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowContentResolver

/** Unit tests for [JpegBytes2Disk] and [FileUtil]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class JpegBytes2DiskTest {

    private val operation = JpegBytes2Disk()

    @Test
    fun copyToDestination_tempFileDeleted() {
        // Arrange: create a file with a string.
        val fileContent = "fileContent"
        TEMP_FILE.writeText(fileContent, Charsets.UTF_8)
        val destination =
            File.createTempFile("unit_test_" + UUID.randomUUID().toString(), ".temp").also {
                it.deleteOnExit()
            }
        // Act: move the file to the destination.
        moveFileToTarget(TEMP_FILE, OutputFileOptions.Builder(destination).build())
        // Assert: the temp file is deleted and the destination file has the same content.
        assertThat(File(TEMP_FILE.absolutePath).exists()).isFalse()
        assertThat(File(destination.absolutePath).readText(Charsets.UTF_8)).isEqualTo(fileContent)
    }

    @Test
    fun saveWithExif_verifyExifIsCopied() {
        // Arrange.
        val exif = createExif(createJpegBytes(WIDTH, HEIGHT))
        exif.description = EXIF_DESCRIPTION
        // Act.
        val path = saveFileAndGetPath(exif)
        // Assert.
        val restoredExif = createFromFileString(path)
        assertThat(restoredExif.description).isEqualTo(EXIF_DESCRIPTION)
    }

    @Test
    fun saveWithHorizontalFlip_verifyExif() {
        // Arrange.
        val metadata = ImageCapture.Metadata().apply { this.isReversedHorizontal = true }
        // Act.
        val path = saveFileAndGetPath(metadata)
        // Assert.
        assertThat(createFromFileString(path).isFlippedHorizontally).isTrue()
    }

    @Test
    fun saveWithVerticalFlip_verifyExif() {
        // Arrange.
        val metadata = ImageCapture.Metadata().apply { this.isReversedVertical = true }
        // Act.
        val path = saveFileAndGetPath(metadata)
        // Assert.
        assertThat(createFromFileString(path).isFlippedVertically).isTrue()
    }

    @Test
    fun saveWithLocation_verifyExif() {
        // Arrange.
        val location = Location(null).apply { this.altitude = ALTITUDE }
        val metadata = ImageCapture.Metadata().apply { this.location = location }
        // Act.
        val path = saveFileAndGetPath(metadata)
        // Assert.
        assertThat(createFromFileString(path).location!!.altitude).isEqualTo(ALTITUDE)
    }

    @Test
    fun copyToOutputStream_writeThrowsIoException_preservesCause() {
        TEMP_FILE.writeText("fileContent", Charsets.UTF_8)
        val ioFailure = IOException("Simulated stream write failure")
        val failingStream =
            object : OutputStream() {
                override fun write(b: Int) {
                    throw ioFailure
                }
            }

        val exception =
            assertThrows(ImageCaptureException::class.java) {
                moveFileToTarget(TEMP_FILE, OutputFileOptions.Builder(failingStream).build())
            }

        assertThat(exception.imageCaptureError).isEqualTo(ERROR_FILE_IO)
        assertThat(exception.cause).isSameInstanceAs(ioFailure)
        assertThat(TEMP_FILE.exists()).isFalse()
    }

    @Test
    fun copyToMediaStore_insertThrowsIllegalArgumentException_throwsImageCaptureException() {
        TEMP_FILE.writeText("fileContent", Charsets.UTF_8)
        val insertFailure =
            IllegalArgumentException("Unknown URL content://media/external/images/media")
        val provider = FakeMediaStoreProvider(onInsert = { _, _ -> throw insertFailure })
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        val contentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val collection = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media")

        val exception =
            assertThrows(ImageCaptureException::class.java) {
                moveFileToTarget(
                    TEMP_FILE,
                    OutputFileOptions.Builder(contentResolver, collection, ContentValues()).build(),
                )
            }

        assertThat(exception.imageCaptureError).isEqualTo(ERROR_FILE_IO)
        assertThat(exception.cause).isSameInstanceAs(insertFailure)
        assertThat(provider.updatedUris).isEmpty()
        assertThat(provider.deletedUris).isEmpty()
        assertThat(TEMP_FILE.exists()).isFalse()
    }

    @Test
    fun copyToMediaStore_writeThrowsIoException_deletesOrphanUriWithoutClearingPending() {
        TEMP_FILE.writeText("fileContent", Charsets.UTF_8)
        val insertedUri = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media/42")
        val provider = FakeMediaStoreProvider(onInsert = { _, _ -> insertedUri })
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        val contentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val ioFailure = IOException("Simulated ENOSPC while writing MediaStore stream")
        shadowOf(contentResolver)
            .registerOutputStream(
                insertedUri,
                object : OutputStream() {
                    override fun write(b: Int) {
                        throw ioFailure
                    }
                },
            )
        val collection = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media")

        val exception =
            assertThrows(ImageCaptureException::class.java) {
                moveFileToTarget(
                    TEMP_FILE,
                    OutputFileOptions.Builder(contentResolver, collection, ContentValues()).build(),
                )
            }

        assertThat(exception.imageCaptureError).isEqualTo(ERROR_FILE_IO)
        assertThat(exception.cause).isSameInstanceAs(ioFailure)
        assertThat(provider.updatedUris).isEmpty()
        assertThat(provider.deletedUris).containsExactly(insertedUri)
        assertThat(TEMP_FILE.exists()).isFalse()
    }

    @Test
    fun copyToMediaStore_deleteThrowsDuringRollback_preservesCauseAndSuppressesDeleteFailure() {
        TEMP_FILE.writeText("fileContent", Charsets.UTF_8)
        val insertedUri = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media/44")
        val deleteFailure = SecurityException("Simulated delete failure during rollback")
        val provider =
            FakeMediaStoreProvider(
                onInsert = { _, _ -> insertedUri },
                onDelete = { throw deleteFailure },
            )
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        val contentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val ioFailure = IOException("Simulated stream write failure")
        shadowOf(contentResolver)
            .registerOutputStream(
                insertedUri,
                object : OutputStream() {
                    override fun write(b: Int) {
                        throw ioFailure
                    }
                },
            )
        val collection = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media")

        val exception =
            assertThrows(ImageCaptureException::class.java) {
                moveFileToTarget(
                    TEMP_FILE,
                    OutputFileOptions.Builder(contentResolver, collection, ContentValues()).build(),
                )
            }

        assertThat(exception.imageCaptureError).isEqualTo(ERROR_FILE_IO)
        assertThat(exception.cause).isSameInstanceAs(ioFailure)
        assertThat(ioFailure.suppressed).asList().containsExactly(deleteFailure)
        assertThat(provider.updatedUris).isEmpty()
        assertThat(provider.deletedUris).containsExactly(insertedUri)
        assertThat(TEMP_FILE.exists()).isFalse()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun copyToMediaStore_updatePendingThrowsRuntimeException_deletesOrphanUri() {
        TEMP_FILE.writeText("fileContent", Charsets.UTF_8)
        val insertedUri = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media/45")
        val updateFailure = IllegalStateException("Simulated MediaStore IS_PENDING update failure")
        val provider =
            FakeMediaStoreProvider(
                onInsert = { _, _ -> insertedUri },
                onUpdate = { _, _ -> throw updateFailure },
            )
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        val contentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        shadowOf(contentResolver).registerOutputStream(insertedUri, ByteArrayOutputStream())
        val collection = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media")

        val exception =
            assertThrows(ImageCaptureException::class.java) {
                moveFileToTarget(
                    TEMP_FILE,
                    OutputFileOptions.Builder(contentResolver, collection, ContentValues()).build(),
                )
            }

        assertThat(exception.imageCaptureError).isEqualTo(ERROR_FILE_IO)
        assertThat(exception.cause).isSameInstanceAs(updateFailure)
        assertThat(provider.updatedUris).containsExactly(insertedUri)
        assertThat(provider.deletedUris).containsExactly(insertedUri)
        assertThat(TEMP_FILE.exists()).isFalse()
    }

    @Test
    fun copyToMediaStore_success_clearsPendingStatusWithoutDeletingUri() {
        val fileContent = "validJpegPayload"
        TEMP_FILE.writeText(fileContent, Charsets.UTF_8)
        val insertedUri = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media/43")
        val provider = FakeMediaStoreProvider(onInsert = { _, _ -> insertedUri })
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        val contentResolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val capturedOutput = ByteArrayOutputStream()
        shadowOf(contentResolver).registerOutputStream(insertedUri, capturedOutput)
        val collection = Uri.parse("content://${MediaStore.AUTHORITY}/external/images/media")

        val resultUri =
            moveFileToTarget(
                TEMP_FILE,
                OutputFileOptions.Builder(contentResolver, collection, ContentValues()).build(),
            )

        assertThat(resultUri).isEqualTo(insertedUri)
        assertThat(capturedOutput.toString(Charsets.UTF_8.name())).isEqualTo(fileContent)
        assertThat(provider.deletedUris).isEmpty()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertThat(
                    provider.insertedValues
                        .single()
                        .getAsInteger(MediaStore.Images.Media.IS_PENDING)
                )
                .isEqualTo(1)
            assertThat(provider.updatedUris).containsExactly(insertedUri)
            assertThat(
                    provider.updatedValues.single().getAsInteger(MediaStore.Images.Media.IS_PENDING)
                )
                .isEqualTo(0)
        } else {
            assertThat(provider.updatedUris).isEmpty()
        }
        assertThat(TEMP_FILE.exists()).isFalse()
    }

    private class FakeMediaStoreProvider(
        private val onInsert: (Uri, ContentValues?) -> Uri?,
        private val onUpdate: (Uri, ContentValues?) -> Int = { _, _ -> 1 },
        private val onDelete: (Uri) -> Int = { 1 },
    ) : ContentProvider() {
        val insertedValues = mutableListOf<ContentValues>()
        val updatedUris = mutableListOf<Uri>()
        val updatedValues = mutableListOf<ContentValues>()
        val deletedUris = mutableListOf<Uri>()

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            if (values != null) {
                insertedValues.add(ContentValues(values))
            }
            return onInsert(uri, values)
        }

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int {
            deletedUris.add(uri)
            return onDelete(uri)
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int {
            updatedUris.add(uri)
            if (values != null) {
                updatedValues.add(ContentValues(values))
            }
            return onUpdate(uri, values)
        }
    }

    private fun saveFileAndGetPath(metadata: ImageCapture.Metadata): String {
        return saveFileAndGetPath(createExif(createJpegBytes(WIDTH, HEIGHT)), metadata, 0)
    }

    private fun saveFileAndGetPath(
        exif: Exif = createExif(createJpegBytes(WIDTH, HEIGHT)),
        metadata: ImageCapture.Metadata = ImageCapture.Metadata(),
        rotation: Int = ROTATION_DEGREES,
    ): String {
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val inputPacket =
            Packet.of(
                jpegBytes,
                exif,
                ImageFormat.JPEG,
                Size(WIDTH, HEIGHT),
                Rect(0, 0, WIDTH, HEIGHT),
                rotation,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )
        val options = OutputFileOptions.Builder(TEMP_FILE).setMetadata(metadata).build()
        val input = JpegBytes2Disk.In.of(inputPacket, options)
        return operation.apply(input).savedUri!!.path!!
    }
}
