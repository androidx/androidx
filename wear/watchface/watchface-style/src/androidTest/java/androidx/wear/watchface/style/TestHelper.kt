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

package androidx.wear.watchface.style

import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.io.File

// Test helpers forked from http://aosp/2218950

fun withPackageName(packageName: String, block: WithPackageBlock.() -> Unit) {
    block(WithPackageBlock(packageName))
}

class WithPackageBlock internal constructor(private val packageName: String) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation = instrumentation.uiAutomation

    // `externalMediaDirs` is deprecated. Docs encourage to put media in MediaStore instead. Here
    // we just need a folder that can be accessed by both app and shell user so should be fine.
    @Suppress("deprecation")
    private val dirUsableByAppAndShell by lazy {
        when {
            Build.VERSION.SDK_INT >= 29 -> {
                // On Android Q+ we are using the media directory because that is
                // the directory that the shell has access to. Context: b/181601156
                // This is the same logic of Outputs#init to determine `dirUsableByAppAndShell`.
                InstrumentationRegistry.getInstrumentation().context.externalMediaDirs.firstOrNull {
                    Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
                }
            }
            Build.VERSION.SDK_INT <= 22 -> {
                // prior to API 23, shell didn't have access to externalCacheDir
                InstrumentationRegistry.getInstrumentation().context.cacheDir
            }
            else -> InstrumentationRegistry.getInstrumentation().context.externalCacheDir
        } ?: throw IllegalStateException("Unable to select a directory for writing files.")
    }

    public fun uninstall() = executeCommand("pm uninstall $packageName")

    public fun install(apkName: String) {
        // Contains all the clean up actions to perform at the end of the execution
        val cleanUpBlocks = mutableListOf<() -> (Unit)>()

        try {
            // First writes in a temp file the apk from the assets
            val tmpApkFile =
                File(dirUsableByAppAndShell, "tmp_$apkName").also { file ->
                    file.delete()
                    file.createNewFile()
                    file.deleteOnExit()
                    file.outputStream().use {
                        instrumentation.context.assets.open(apkName).copyTo(it)
                    }
                }
            cleanUpBlocks.add { tmpApkFile.delete() }

            // Then moves it to a destination that can be used to install it
            val destApkPath = "$TEMP_DIR/$apkName"
            Truth.assertThat(executeCommand("mv ${tmpApkFile.absolutePath} $destApkPath")).isEmpty()
            cleanUpBlocks.add { executeCommand("rm $destApkPath") }

            // This mimes the behaviour of `adb install-multiple` using an install session.
            // For reference:
            // https://source.corp.google.com/android-internal/packages/modules/adb/client/adb_install.cpp

            // Creates an install session
            val installCreateOutput = executeCommand("pm install-create -t").first().trim()
            val sessionId = REGEX_SESSION_ID.find(installCreateOutput)!!.groups[1]!!.value.toLong()

            // Adds the base apk to the install session
            val successBaseApk =
                executeCommand("pm install-write $sessionId base.apk $TEMP_DIR/$apkName")
                    .first()
                    .trim()
                    .startsWith("Success")
            if (!successBaseApk) {
                throw IllegalStateException("Could not add $apkName to install session $sessionId")
            }

            // Commit the install transaction. Note that install-commit may not print any output
            // if it fails.
            val commitCommandOutput = executeCommand("pm install-commit $sessionId")
            val firstLine = commitCommandOutput.firstOrNull()?.trim()
            if (firstLine == null || firstLine != "Success") {
                throw IllegalStateException(
                    "pm install-commit failed: ${commitCommandOutput.joinToString("\n")}"
                )
            }
        } finally {
            // Runs all the clean up blocks. This will clean up also partial operations in case
            // there is an issue during install
            cleanUpBlocks.forEach { it() }
        }
    }

    private fun executeCommand(command: String): List<String> {
        Log.d(TAG, "Executing command: `$command`")
        return ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command))
            .bufferedReader()
            .lineSequence()
            .toList()
    }

    companion object {
        private const val TAG = "TestManager"
        private const val TEMP_DIR = "/data/local/tmp/"
        private val REGEX_SESSION_ID = """\[(\d+)\]""".toRegex()
    }
}
