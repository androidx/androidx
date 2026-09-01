/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [FileUtil]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class FileUtilTest {

    private lateinit var tempDir: File
    private val testFiles = mutableListOf<File>()
    private val testBitmaps = mutableListOf<Bitmap>()

    @Before
    fun setUp() {
        val cacheDir = ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir
        tempDir = File(cacheDir, "file_util_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        for (bitmap in testBitmaps) {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        for (file in testFiles) {
            if (file.exists()) {
                file.delete()
            }
        }
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    private fun createTestBitmap(color: Int = Color.RED): Bitmap {
        return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            testBitmaps.add(this)
        }
    }

    @Test
    fun saveBitmap_withDirectoryAndName_createsPngFileAndSavesContent() {
        val bitmap = createTestBitmap(Color.RED)
        val testName = "test_save_png_${System.currentTimeMillis()}"
        val savedFile = FileUtil.saveBitmap(bitmap, tempDir, testName)

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        val expectedFile = File(tempDir, "$testName.png")
        assertThat(savedFile).isEqualTo(expectedFile)
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.length()).isGreaterThan(0L)
    }

    @Test
    fun saveBitmap_withDirectoryAndName_supportsJpegFormat() {
        val bitmap = createTestBitmap(Color.GREEN)
        val testName = "test_save_jpeg_${System.currentTimeMillis()}"
        val savedFile =
            FileUtil.saveBitmap(bitmap, tempDir, testName, Bitmap.CompressFormat.JPEG, 90)

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        val expectedFile = File(tempDir, "$testName.jpg")
        assertThat(savedFile).isEqualTo(expectedFile)
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.length()).isGreaterThan(0L)
    }

    @Test
    fun saveBitmap_withDirectoryAndName_supportsWebpFormat() {
        val bitmap = createTestBitmap(Color.YELLOW)
        val testName = "test_save_webp_${System.currentTimeMillis()}"
        @Suppress("DEPRECATION") val format = Bitmap.CompressFormat.WEBP
        val savedFile = FileUtil.saveBitmap(bitmap, tempDir, testName, format, 90)

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        val expectedFile = File(tempDir, "$testName.webp")
        assertThat(savedFile).isEqualTo(expectedFile)
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.length()).isGreaterThan(0L)
    }

    @Test
    fun saveBitmap_withDirectoryAndName_sanitizesInvalidCharactersAndPathTraversal() {
        val bitmap = createTestBitmap(Color.BLUE)
        val inputName = "../../invalid:path*name?test.png"
        val savedFile = FileUtil.saveBitmap(bitmap, tempDir, inputName)

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        assertThat(savedFile?.parentFile?.canonicalPath).isEqualTo(tempDir.canonicalPath)
        assertThat(savedFile?.name).doesNotContain("..")
        assertThat(savedFile?.name).doesNotContain(":")
        assertThat(savedFile?.name).doesNotContain("*")
        assertThat(savedFile?.name).doesNotContain("?")
        assertThat(savedFile?.exists()).isTrue()
    }

    @Test
    fun saveBitmap_withDirectoryAndName_stripsUppercaseExtension() {
        val bitmap = createTestBitmap(Color.MAGENTA)
        val inputName = "sample_image.PNG"
        val savedFile = FileUtil.saveBitmap(bitmap, tempDir, inputName)

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        val expectedFile = File(tempDir, "sample_image.png")
        assertThat(savedFile).isEqualTo(expectedFile)
        assertThat(expectedFile.exists()).isTrue()
    }
}
