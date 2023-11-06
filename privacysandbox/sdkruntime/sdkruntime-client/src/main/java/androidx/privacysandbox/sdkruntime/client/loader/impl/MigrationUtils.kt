/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.loader.impl

import android.os.Build
import android.os.FileUtils
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal object MigrationUtils {

    private const val LOG_TAG = "LocalSdkMigrationUtils"

    /**
     * Try to migrate all files from source to target that match requested prefix.
     * Skip failed files.
     *
     * @return true if all files moved, or false if some fails happened.
     */
    fun moveFiles(srcDir: File, destDir: File, prefix: String): Boolean {
        if (srcDir == destDir) {
            return true
        }

        val sourceFiles = srcDir.listFiles { _, name -> name.startsWith(prefix) }
            ?: emptyArray()

        var hadFails = false
        for (sourceFile in sourceFiles) {
            val targetFile = File(destDir, sourceFile.name)
            Log.d(LOG_TAG, "Migrating $sourceFile to $targetFile")
            try {
                copyFile(sourceFile, targetFile)
                copyPermissions(sourceFile, targetFile)
                if (!sourceFile.delete()) {
                    Log.w(LOG_TAG, "Failed to clean up $sourceFile")
                    hadFails = true
                }
            } catch (e: IOException) {
                Log.w(LOG_TAG, "Failed to migrate $sourceFile", e)
                hadFails = true
            } catch (e: ErrnoException) {
                Log.w(LOG_TAG, "Failed to migrate $sourceFile", e)
                hadFails = true
            }
        }
        return !hadFails
    }

    private fun copyFile(sourceFile: File, targetFile: File) {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        FileInputStream(sourceFile).use { sourceStream ->
            FileOutputStream(targetFile).use { targetStream ->
                copy(sourceStream, targetStream)
                Os.fsync(targetStream.fd)
            }
        }
    }

    private fun copyPermissions(sourceFile: File, targetFile: File) {
        val stat = Os.stat(sourceFile.absolutePath)
        Os.chmod(targetFile.absolutePath, stat.st_mode)
        Os.chown(targetFile.absolutePath, stat.st_uid, stat.st_gid)
    }

    private fun copy(from: InputStream, to: OutputStream): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Api29.copy(from, to)
        }
        return from.copyTo(to)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private object Api29 {
        @DoNotInline
        fun copy(from: InputStream, to: OutputStream): Long =
            FileUtils.copy(from, to)
    }
}
