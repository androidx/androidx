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

package androidx.camera.core.imagecapture

import android.graphics.ImageFormat
import android.hardware.camera2.DngCreator
import android.media.ExifInterface
import android.media.Image
import android.os.Build
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.imagecapture.FileUtil.moveFileToTarget
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.TEMP_FILE
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.OutputStream
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [DngImage2Disk] */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DngImage2DiskTest {

    private val dngCreator = mock(DngCreator::class.java)
    private val operation = DngImage2Disk(dngCreator)

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
    fun writeImageToFile_dngCreatorCalled() {
        val options = OutputFileOptions.Builder(TEMP_FILE).build()
        val imageProxy = FakeImageProxy(FakeImageInfo())
        imageProxy.format = ImageFormat.RAW_SENSOR
        imageProxy.image = mock(Image::class.java)
        val input = DngImage2Disk.In.of(imageProxy, ROTATION_DEGREES, options)

        val result = operation.apply(input)
        assertThat(result.savedUri).isNotNull()
        assertThat(result.savedUri?.path).isEqualTo(TEMP_FILE.absolutePath)
        verify(dngCreator).setOrientation(ExifInterface.ORIENTATION_ROTATE_180)
        verify(dngCreator).writeImage(any(OutputStream::class.java), eq(imageProxy.image!!))
    }
}
