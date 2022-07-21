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
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Diagnosis object that runs diagnosis test on device and save result to a file.
 */
class Diagnosis {

    // TODO: convert to a suspend function for running different tasks within this function
    fun collectDeviceInfo(context: Context): File {
        Log.d(TAG, "calling collectDeviceInfo()")

        // TODO: verify if external storage is available
        val filename = "device_info_${System.currentTimeMillis()}.zip"
        val tempFile = createTemp(context, filename)
        tempFile.deleteOnExit()

        // Open stream
        val fout = FileOutputStream(tempFile)
        val zout = ZipOutputStream(fout)

        // Log device info
        zout.putNextEntry(ZipEntry("device_info.txt"))
        writeLine(zout, "Manufacturer: ${Build.MANUFACTURER}\n")
        writeLine(zout, "Model: ${Build.MODEL}\n")
        writeLine(zout, "Device: ${Build.DEVICE}\n")
        writeLine(zout, "Version code name: ${Build.VERSION.CODENAME}\n")
        writeLine(zout, "Version SDK: ${Build.VERSION.SDK_INT}\n")
        writeLine(zout, "Fingerprint: ${Build.FINGERPRINT}\n")
        zout.closeEntry()

        // Close stream
        zout.close()
        fout.close()

        return tempFile
    }

    private fun createTemp(context: Context, filename: String): File {
        val dir = File(context.externalCacheDir, "/")
        if (!dir.exists()) {
            assert(dir.mkdirs())
        }
        return File(dir, filename)
    }

    private fun writeLine(zipOutputStream: ZipOutputStream, line: String) {
        zipOutputStream.write(line.toByteArray())
    }

    companion object {
        private const val TAG = "Diagnosis"
    }
}