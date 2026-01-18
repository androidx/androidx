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

package androidx.pdf

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import org.hamcrest.Matcher
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

object TestUtils {
    private val TEMP_FILE_NAME = "temp"
    private val TEMP_FILE_TYPE = ".pdf"

    fun saveStream(inputStream: InputStream, context: Context): Uri {
        val tempFile = File.createTempFile(TEMP_FILE_NAME, TEMP_FILE_TYPE, context.cacheDir)
        FileOutputStream(tempFile).use { outputStream -> inputStream.copyTo(outputStream) }
        return Uri.fromFile(tempFile)
    }

    fun waitFor(delay: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String = "wait for $delay milliseconds"

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(delay)
            }
        }
    }

    fun extractFromLabel(value: String, onExtractionCompleted: (Int, Int) -> Unit) {
        val regex = "(\\d+)\\s?/\\s?(\\d+)".toRegex()
        val matchResult = regex.find(value)

        assertNotNull("No results found for matching page indicator label $value", matchResult)
        assertTrue("Invalid page indicator label $value", matchResult!!.groups.size >= 3)
        assertNotNull("Could not extract current page number $value", matchResult.groups[1])
        assertNotNull("Could not extract total pages $value", matchResult.groups[2])
        assertNotNull(
            "Invalid current page number string $value",
            matchResult.groups[1]!!.value.toIntOrNull(),
        )
        assertNotNull(
            "Invalid total pages string $value",
            matchResult.groups[2]!!.value.toIntOrNull(),
        )

        onExtractionCompleted(
            matchResult.groups[1]!!.value.toInt(),
            matchResult.groups[2]!!.value.toInt(),
        )
    }

    /**
     * Returns a [Uri] for a new file in the app's cache directory, deleting any existing file
     * first.
     *
     * @param fileName The name of the file.
     */
    fun createFile(fileName: String): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return Uri.fromFile(file)
    }
}

/** Converts a [Uri] to a [ParcelFileDescriptor] in read/write mode. */
fun Uri.toPfd(): ParcelFileDescriptor {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return context.contentResolver.openFileDescriptor(this, "rw")!!
}
