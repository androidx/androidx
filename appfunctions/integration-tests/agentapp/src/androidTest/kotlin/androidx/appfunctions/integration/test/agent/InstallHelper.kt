/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appfunctions.integration.test.agent

import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Helper class to install and uninstall APKs on the device using shell commands.
 *
 * This is primarily used for integration tests where an agent app needs to install or uninstall
 * other APKs for testing purposes.
 */
object InstallHelper {
    private const val TAG = "InstallHelper"
    private const val TEMP_DIR = "/data/local/tmp"
    private val REGEX_SESSION_ID = """\[(\d+)\]""".toRegex()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation = instrumentation.uiAutomation

    /** Uninstalls an APK with the given package name. */
    fun uninstall(packageName: String) = executeCommand("pm uninstall $packageName")

    /**
     * Installs an APK from the test resources.
     *
     * The APK is read from a Java resources/ directory and installed on the device using `adb shell
     * pm` install session. This is done because the generated APK from gradle is marked
     * testOnly=true and can't be installed using PackageInstaller APIs.
     */
    fun install(apkName: String) {
        val cleanUpBlocks = mutableListOf<() -> Unit>()

        try {
            // Create Install Session
            val installCreateOutput = executeCommand("pm install-create -t").first().trim()
            val sessionId =
                REGEX_SESSION_ID.find(installCreateOutput)?.groups?.get(1)?.value?.toLong()
                    ?: throw IllegalStateException(
                        "Failed to create install session: $installCreateOutput"
                    )

            // Prepare /data/local/tmp directory (Shell side)
            val baseTmpFolder = "$TEMP_DIR/$sessionId"
            executeCommand("mkdir -p $baseTmpFolder")
            cleanUpBlocks.add { executeCommand("rm -Rf $baseTmpFolder") }

            // Write APK to App's External Storage
            val bridgeDir =
                instrumentation.targetContext.getExternalFilesDir(null)
                    ?: throw IllegalStateException("External files dir not found")

            val bridgeFile = File(bridgeDir, "bridge_$apkName")
            bridgeFile.outputStream().use { bridgeFileStream ->
                val originalTargetApkFile =
                    Thread.currentThread().contextClassLoader?.getResourceAsStream(apkName)
                        ?: throw IllegalArgumentException("Asset '$apkName' not found")
                originalTargetApkFile.copyTo(bridgeFileStream)
            }

            // Ensure we delete this local bridge file later
            cleanUpBlocks.add { bridgeFile.delete() }

            // Copy from App Storage -> Shell Storage
            // Shell can read the bridgeFile. We move it to /data/local/tmp for the PM to install.
            val destApkPath = "$baseTmpFolder/base.apk"

            val cpOutput = executeCommand("cp ${bridgeFile.absolutePath} $destApkPath")
            if (cpOutput.any { it.contains("denied") || it.contains("No such file") }) {
                throw IllegalStateException("Shell failed to copy APK: $cpOutput")
            }

            // Add to Install Session
            val writeOutput = executeCommand("pm install-write $sessionId base.apk $destApkPath")
            if (!writeOutput.any { it.trim().contains("Success") }) {
                throw IllegalStateException("pm install-write failed: $writeOutput")
            }

            // Commit Session
            val commitOutput = executeCommand("pm install-commit $sessionId")
            if (!commitOutput.any { it.trim() == "Success" }) {
                throw IllegalStateException("pm install-commit failed: $commitOutput")
            }
        } finally {
            // Execute cleanup in reverse order
            cleanUpBlocks.reversed().forEach {
                try {
                    it()
                } catch (e: Exception) {
                    Log.w(TAG, "Cleanup warning", e)
                }
            }
        }
    }

    private fun executeCommand(command: String): List<String> {
        Log.d(TAG, "Executing command: `$command`")
        return ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command))
            .bufferedReader()
            .useLines { it.toList() }
    }
}
