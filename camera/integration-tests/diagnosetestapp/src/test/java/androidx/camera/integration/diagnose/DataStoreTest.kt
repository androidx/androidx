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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M, maxSdk = 32)
class DataStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dataStore = DataStore(context, "DataStoreTest.zip")

    @Test
    fun appendText_flushTextToTextFile_returnFileWithMatchingString() {
        // Act: add test string to data store and flush to file
        dataStore.appendText(TEST_STRING)
        dataStore.flushTextToTextFile(TEST_STRING_FILE)
        val file = dataStore.flushZip()

        // Open file to read text appended
        val zipFile = ZipFile(file)
        val zipEntries = zipFile.entries().toList()
        val inputStream = zipFile.getInputStream(zipEntries[0])
        val br = BufferedReader(InputStreamReader(inputStream))
        val textRead = readText(br)

        // Assert: correct text file contains test string
        assertThat(zipEntries.size).isEqualTo(1)
        assertThat(zipEntries[0].name).isEqualTo("$TEST_STRING_FILE.txt")
        assertThat(textRead).isEqualTo(TEST_STRING)

        inputStream.close()
    }

    @Test
    fun flushBitmapToFile_returnFileWithMatchingBitmap() {
        // Arrange: create testing Bitmap
        val testBitmap = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        for (x in 0..4) {
            for (y in 0..4) {
                testBitmap.setPixel(x, y, Color.BLUE)
            }
        }

        // Act: create zip entry with the test Bitmap and read bitmap from generated file
        dataStore.flushBitmapToFile(TEST_BITMAP_FILE, testBitmap)
        val file = dataStore.flushZip()
        val bitmaps = loadBitmapsFromZipFile(file)

        // Assert: correct Bitmap stored in zip
        assertThat(bitmaps.size).isEqualTo(1)
        assertThat(bitmaps).containsKey("$TEST_BITMAP_FILE.jpeg")
        bitmaps["$TEST_BITMAP_FILE.jpeg"]?.let {
            assertBitmapColorAndSize(it, Color.BLUE, 5, 5)
        }
    }

    /**
     * Returns a map of jpeg filename and bitmap contained in the file
     */
    fun loadBitmapsFromZipFile(file: File): Map<String, Bitmap> {
        val zipFile = ZipFile(file)
        val zipEntries = zipFile.entries().toList()
        val bitmaps = mutableMapOf<String, Bitmap>()

        zipEntries.forEach { zipEntry ->
            if (zipEntry.name.endsWith(".jpeg")) {
                val inputStream = zipFile.getInputStream(zipEntry)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmaps[zipEntry.name] = bitmap
                inputStream.close()
            }
        }
        return bitmaps
    }

    companion object {
        private const val TEST_STRING = "test string"
        private const val TEST_STRING_FILE = "test_string_file"
        private const val TEST_BITMAP_FILE = "test_bitmap_file"
    }
}
