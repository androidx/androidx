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
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowLog

/** Unit tests for [LabTestUtil]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class LabTestUtilTest {

    private val testFiles = mutableListOf<File>()
    private val testBitmaps = mutableListOf<Bitmap>()

    @Before
    fun setUp() {
        ShadowLog.clear()
        ShadowLog.reset()
    }

    @After
    fun tearDown() {
        LabTestUtil.isLabEnvironmentOverride = null
        ShadowLog.reset()
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
    }

    private fun createTestBitmap(color: Int = Color.RED): Bitmap {
        return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            testBitmaps.add(this)
        }
    }

    @Test
    fun isLabEnvironment_returnsTrueWhenOverrideIsTrue() {
        LabTestUtil.isLabEnvironmentOverride = true
        assertThat(LabTestUtil.isLabEnvironment()).isTrue()
    }

    @Test
    fun isLabEnvironment_returnsFalseWhenOverrideIsFalse() {
        LabTestUtil.isLabEnvironmentOverride = false
        assertThat(LabTestUtil.isLabEnvironment()).isFalse()
    }

    @Test
    fun isLabEnvironment_returnsTrueWhenMhLoggable() {
        LabTestUtil.isLabEnvironmentOverride = null
        ShadowLog.setLoggable("MH", Log.DEBUG)
        assertThat(LabTestUtil.isLabEnvironment()).isTrue()
    }

    @Test
    fun isLabEnvironment_returnsTrueWhenRearCameraE2ELoggable() {
        LabTestUtil.isLabEnvironmentOverride = null
        ShadowLog.setLoggable("rearCameraE2E", Log.DEBUG)
        assertThat(LabTestUtil.isLabEnvironment()).isTrue()
    }

    @Test
    fun isLabEnvironment_returnsTrueWhenFrontCameraE2ELoggable() {
        LabTestUtil.isLabEnvironmentOverride = null
        ShadowLog.setLoggable("frontCameraE2E", Log.DEBUG)
        assertThat(LabTestUtil.isLabEnvironment()).isTrue()
    }

    @Test
    fun isLabEnvironment_returnsFalseWhenNoLabPropertySet() {
        LabTestUtil.isLabEnvironmentOverride = null
        assertThat(LabTestUtil.isLabEnvironment()).isFalse()
    }

    @Test
    fun saveTestBitmap_savesWhenInLabEnvironment() {
        LabTestUtil.isLabEnvironmentOverride = true
        val bitmap = createTestBitmap(Color.RED)
        val testName = "test_lab_save_${System.currentTimeMillis()}"
        val savedFile = LabTestUtil.saveTestBitmap(bitmap, testName)

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        val outputDir = File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "test_output")
        val expectedFile = File(outputDir, "$testName.png")
        assertThat(savedFile).isEqualTo(expectedFile)
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.length()).isGreaterThan(0L)
    }

    @Test
    fun saveTestBitmap_supportsCustomDirectoryAndJpeg() {
        LabTestUtil.isLabEnvironmentOverride = true
        val bitmap = createTestBitmap(Color.GREEN)
        val testName = "test_custom_dir_${System.currentTimeMillis()}"
        val customDir =
            File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "custom_dir").apply {
                mkdirs()
            }
        val savedFile =
            LabTestUtil.saveTestBitmap(
                bitmap,
                testName,
                format = Bitmap.CompressFormat.JPEG,
                quality = 90,
                directory = customDir,
            )

        assertThat(savedFile).isNotNull()
        savedFile?.let { testFiles.add(it) }

        val expectedFile = File(customDir, "$testName.jpg")
        assertThat(savedFile).isEqualTo(expectedFile)
        assertThat(expectedFile.exists()).isTrue()
        assertThat(expectedFile.length()).isGreaterThan(0L)
    }

    @Test
    fun saveTestBitmap_skipsWhenNotInLabEnvironment() {
        LabTestUtil.isLabEnvironmentOverride = false
        val bitmap = createTestBitmap(Color.BLUE)
        val testName = "test_no_lab_skip_${System.currentTimeMillis()}"
        val savedFile = LabTestUtil.saveTestBitmap(bitmap, testName)

        assertThat(savedFile).isNull()

        val outputDir = File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "test_output")
        val expectedFile = File(outputDir, "$testName.png")
        testFiles.add(expectedFile)

        assertThat(expectedFile.exists()).isFalse()
        val logs = ShadowLog.getLogsForTag("LabTestUtil")
        assertThat(
                logs.any {
                    it.msg.contains("Not in lab environment, skip saving test output bitmap") &&
                        it.msg.contains("adb shell setprop log.tag.MH DEBUG")
                }
            )
            .isTrue()
    }
}
