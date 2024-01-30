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
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * DataStore object that store {@link DiagnosisTask} results (text and images) to files in a Zip File.
 */
class DataStore(context: Context, zipFileName: String) {

    private val zipFile: File
    private val fout: FileOutputStream
    private val zout: ZipOutputStream
    private val logger: StringBuffer

    init {
        // create zip file
        zipFile = createZip(context, zipFileName)
        zipFile.deleteOnExit()

        // open output streams
        fout = FileOutputStream(zipFile)
        zout = ZipOutputStream(fout)

        // single logger used for all tasks
        logger = StringBuffer()
    }

    /**
     * Append text to store in logger
     */
    fun appendText(text: String) {
        logger.appendLine(text)
    }

    fun appendTitle(taskName: String) {
        appendText("Task name: $taskName")
    }

    /**
     * Copy and flush file as an image file to a ZipEntry
     */
    @WorkerThread
    fun flushTempFileToImageFile(file: File, filename: String) {
        val inputStream = FileInputStream(file)
        zout.putNextEntry(ZipEntry("$filename.jpeg"))
        inputStream.copyTo(zout)
        zout.closeEntry()
    }

    /**
     * Flush text in logger as a text file to a ZipEntry and clear logger
     */
    @WorkerThread
    fun flushTextToTextFile(filename: String) {
        if (logger.isEmpty()) {
            return
        }
        zout.putNextEntry(ZipEntry("$filename.txt"))
        zout.write(logger.toString().toByteArray())
        zout.closeEntry()
        // clear logger
        logger.setLength(0)
    }

    /**
     * Flush bitmap as JPEG to a ZipEntry
     */
    @WorkerThread
    fun flushBitmapToFile(imageName: String, bitmap: Bitmap) {
        zout.putNextEntry(ZipEntry(("$imageName.jpeg")))
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, zout)
        zout.closeEntry()
    }

    /**
     * Close output streams and return zip file
     */
    fun flushZip(): File {
        zout.close()
        fout.close()

        return zipFile
    }

    private fun createZip(context: Context, filename: String): File {
        val dir = File("${context.externalCacheDir}")
        if (!dir.exists()) {
            assert(dir.mkdirs())
        }
        return File(dir, filename)
    }
}
