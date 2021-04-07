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

package androidx.camera.view.transform

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.impl.utils.Exif
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val WIDTH = 80
private const val HEIGHT = 60

/**
 * Instrument test for [FileTransformFactory].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class FileTransformFactoryDeviceTest {

    private lateinit var factory: FileTransformFactory
    private val contentResolver = getApplicationContext<Context>().contentResolver

    @get:Rule
    public val runtimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Before
    public fun setUp() {
        factory = FileTransformFactory()
    }

    @Test
    public fun setUseRotationDegrees_getterReturnsTrue() {
        factory.isUsingExifOrientation = true
        assertThat(factory.isUsingExifOrientation).isTrue()
    }

    @Test
    public fun extractFromFile() {
        factory.getOutputTransform(createImageFile()).assertMapping(1f, 1f, WIDTH, HEIGHT)
    }

    @Test
    public fun extractFromFileWithExifInfo() {
        factory.isUsingExifOrientation = true
        factory.getOutputTransform(createImageFile(ExifInterface.ORIENTATION_ROTATE_90))
            .assertMapping(1f, 1f, 0, WIDTH)
    }

    @Test
    public fun extractFromInputStream() {
        FileInputStream(createImageFile()).use {
            factory.getOutputTransform(it).assertMapping(1f, 1f, WIDTH, HEIGHT)
        }
    }

    @Test
    public fun extractFromMediaStoreUri() {
        val uri = createMediaStoreImage()
        factory.getOutputTransform(contentResolver, uri).assertMapping(1f, 1f, WIDTH, HEIGHT)
        contentResolver.delete(uri, null, null)
    }

    /**
     * Asserts that the [OutputTransform] maps normalized (x, y) to image (x, y).
     */
    private fun OutputTransform.assertMapping(
        normalizedX: Float,
        normalizedY: Float,
        imageX: Int,
        imageY: Int
    ) {
        val point = floatArrayOf(normalizedX, normalizedY)
        matrix.mapPoints(point)
        assertThat(point).usingTolerance(0.001)
            .containsExactly((floatArrayOf(imageX.toFloat(), imageY.toFloat())))
    }

    private fun createImageFile(): File {
        return createImageFile(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun createImageFile(exifOrientation: Int): File {
        // Create bitmap file.
        val tempFile = File.createTempFile("FileTransformFactoryDeviceTest", "jpeg")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use {
            createBitmap().compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        // Add exif to the file.
        val exif = Exif.createFromFile(tempFile)
        exif.orientation = exifOrientation
        exif.save()
        return tempFile
    }

    private fun createMediaStoreImage(): Uri {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        contentResolver.openOutputStream(uri!!).use {
            createBitmap().compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        return uri
    }

    private fun createBitmap(): Bitmap {
        return Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    }
}