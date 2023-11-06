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

package androidx.camera.integration.diagnose

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.view.LifecycleCameraController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M, maxSdk = 32)
class DiagnosisTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraController: LifecycleCameraController
    private var taskList = mutableListOf<DiagnosisTask>()

    @Before
    fun setUp() {
        cameraController = LifecycleCameraController(context)
        cameraController.bindToLifecycle(FakeLifecycleOwner())
        taskList.add(FakeTextSavingTask())
        taskList.add(FakeTextAndImageSavingTask())
    }

    @Test
    fun diagnoseAndIsAggregated_returnsOneTextFileOneJEPG() {
        // Act: running aggregated diagnose on taskList
        val file: File?
        runBlocking {
            file = Diagnosis().diagnose(context, taskList, cameraController, true)
        }

        // Assert: file exits with correct contents
        if (file != null) {
            val zipFile = ZipFile(file)
            val zipEntries = zipFile.entries().toList()
            var correctTextFileCount = 0
            var correctImageFileCount = 0

            // Confirm 2 zip entries
            assertThat(zipEntries.size).isEqualTo(2)

            // Checking correct filename and content for each entry
            zipEntries.forEach {
                val inputStream = zipFile.getInputStream(it)

                if (it.name.endsWith(".txt")) {
                    val br = BufferedReader(InputStreamReader(inputStream))
                    val textRead = readText(br)

                    assertThat(it.name).isEqualTo(AGGREGATED_TEXT_FILENAME)
                    assertThat(textRead).isEqualTo(FAKE_TEXT_SAVING_TASK_STRING +
                        FAKE_TEXT_AND_IMAGE_SAVING_TASK_STRING)
                    correctTextFileCount ++
                } else if (it.name.endsWith(".jpeg")) {
                    assertThat(it.name).isEqualTo(FAKE_IMAGE_NAME)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    assertBitmapColorAndSize(bitmap, Color.BLUE, 5, 5)
                    correctImageFileCount++
                }
                inputStream.close()
            }

            // Check for one text file and one image file
            assertThat(correctTextFileCount).isEqualTo(1)
            assertThat(correctImageFileCount).isEqualTo(1)
        }
    }

    @Test
    fun diagnoseAndIsNotAggregated_returnsTwoTextFileOneJPEG() {
        // Act: running non-aggregated diagnose on taskList
        val file: File?
        runBlocking {
            file = Diagnosis().diagnose(context, taskList, cameraController, false)
        }

        // Assert: file exits with correct contents
        if (file != null) {
            val zipFile = ZipFile(file)
            val zipEntries = zipFile.entries().toList()
            var correctImageFileCount = 0
            var correctTextFileCount = 0

            // Confirm there are 3 zip entries
            assertThat(zipEntries.size).isEqualTo(3)

            // Checking correct filename and content
            zipEntries.forEach {
                val inputStream = zipFile.getInputStream(it)

                if (it.name.endsWith(".txt")) {
                    val br = BufferedReader(InputStreamReader(inputStream))
                    val textRead = readText(br)
                    // check it contains 2 correct file name // check which file it is
                    if (it.name.equals(FAKE_TEXT_SAVING_TASK_TXT_NAME)) {
                        assertThat(textRead).isEqualTo(FAKE_TEXT_SAVING_TASK_STRING)
                        correctTextFileCount++
                    } else if (it.name.equals(FAKE_TEXT_AND_IMAGE_SAVING_TASK_TXT_NAME)) {
                        assertThat(textRead).isEqualTo(FAKE_TEXT_AND_IMAGE_SAVING_TASK_STRING)
                        correctTextFileCount++
                    }
                } else if (it.name.endsWith(".jpeg")) {
                    // todo : assert image is correct bitmap
                    assertThat(it.name).isEqualTo(FAKE_IMAGE_NAME)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    assertBitmapColorAndSize(bitmap, Color.BLUE, 5, 5)
                    correctImageFileCount++
                }
                inputStream.close()
            }
            // Check for 2 text file and 1 image file
            assertThat(correctTextFileCount).isEqualTo(2)
            assertThat(correctImageFileCount).isEqualTo(1)
        }
    }

    companion object {
        // Strings for filenames
        private const val AGGREGATED_TEXT_FILENAME = "text_report.txt"
        private const val FAKE_TEXT_SAVING_TASK_TXT_NAME = "FakeTextSavingTask.txt"
        private const val FAKE_TEXT_AND_IMAGE_SAVING_TASK_TXT_NAME =
            "FakeTextAndImageSavingTask.txt"
        private const val FAKE_IMAGE_NAME = "FakeImage.jpeg"
        // Strings for text appended in each task
        private const val FAKE_TEXT_SAVING_TASK_STRING = "This is a fake test 1.Line 2."
        private const val FAKE_TEXT_AND_IMAGE_SAVING_TASK_STRING = "This is fake task 2."
    }
}
