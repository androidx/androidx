/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.shell.internal

import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.io.copyTo
import kotlin.io.outputStream
import kotlin.use

/**
 * Installs the native cli copying it from the library assets to /data/local/tmp. It supports non
 * primary users (except for api 30).
 */
internal object ShellInstaller {

    private const val DEFAULT_INSTALL_FILE_PATH = "/data/local/tmp/socket-shell"

    val shellExecutableFile by lazy {
        val outputFile = File(DEFAULT_INSTALL_FILE_PATH)
        install(outputFile = outputFile)
        outputFile.deleteOnExit()
        outputFile
    }

    private fun install(outputFile: File) {

        val outputFilePath = outputFile.absolutePath

        // Input stream to the shell exec in the assets
        val inputStream = inputStreamForFirstSupportedAbi()

        if (UserInfo.isUnsupportedNonPrimaryUser) {

            // In this case we cannot copy the apk to a location where it can be executed.
            throw IllegalStateException("Shell is not supported on non-primary users on API 30.")
        } else if (UserInfo.isNonPrimaryUser) {

            val (_, inDescriptor) =
                uiAutomation.executeShellCommandRw("cp /dev/stdin $outputFilePath")
            ParcelFileDescriptor.AutoCloseOutputStream(inDescriptor).use { oStream ->
                inputStream.use { iStream -> iStream.copyTo(oStream) }
            }

            // The file might still be in cache. Sync will force the writing.
            // Without this, trying to read the file just written may result in file not found.
            command("sync")
        } else {

            // If this is a primary user, shell can read user 0 space so we can just
            // decompress to a temp location and then copy the file to /data/local/tmp.
            // This is compatible with any version of android.
            val tmpFile =
                File(instrumentationPackageMediaDir, randomHexString(8)).also { it.deleteOnExit() }
            inputStream.use { it.copyTo(tmpFile.outputStream()) }
            command("cp ${tmpFile.absolutePath} $outputFilePath")
            tmpFile.delete()
        }

        command("chmod +x $outputFilePath")
    }

    private fun inputStreamForFirstSupportedAbi(): InputStream {
        val unsupportedAbis = mutableListOf<String>()
        for (abi in Build.SUPPORTED_ABIS) {
            abi.trim().let {
                try {
                    return instrumentation.context.assets.open("$it/shell")
                } catch (_: IOException) {
                    unsupportedAbis.add(it)
                }
            }
        }
        val errorStr = unsupportedAbis.joinToString(separator = ", ") { "`$it`" }
        throw IOException("Shell native not found for abi $errorStr. ")
    }
}
