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

package androidx.profileinstaller.integration.profileverification

import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.concurrent.futures.DirectExecutor
import androidx.profileinstaller.DeviceProfileWriter
import androidx.profileinstaller.ProfileInstaller
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun withPackageName(packageName: String, block: WithPackageBlock.() -> Unit) {
    block(WithPackageBlock(packageName))
}

class WithPackageBlock internal constructor(private val packageName: String) {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiAutomation = instrumentation.uiAutomation
    private val uiDevice = UiDevice.getInstance(instrumentation)

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

    fun uninstall() = executeCommand("pm uninstall $packageName")

    fun install(apkName: String, withProfile: Boolean) {

        // Contains all the clean up actions to perform at the end of the execution
        val cleanUpBlocks = mutableListOf<() -> (Unit)>()

        try {

            // Creates an install session
            val installCreateOutput = executeCommand("pm install-create -t").first().trim()
            val sessionId = REGEX_SESSION_ID
                .find(installCreateOutput)
                .throwIfNull("pm install session is invalid.")
                .groups[1]
                .throwIfNull("pm install session is invalid.")
                .value
                .toLong()

            // Creates tmp dir for this session
            val baseTmpFolder = "$TEMP_DIR/$sessionId"
            assertThat(executeCommand("mkdir -p $baseTmpFolder")).isEmpty()
            cleanUpBlocks.add { executeCommand("rm -Rf $baseTmpFolder") }

            // First writes in a temp file the apk from the assets
            val tmpApkFile = File(dirUsableByAppAndShell, "tmp_$apkName").also { file ->
                file.delete()
                file.createNewFile()
                file.deleteOnExit()
                file.outputStream().use { instrumentation.context.assets.open(apkName).copyTo(it) }
            }
            cleanUpBlocks.add { tmpApkFile.delete() }

            // Then moves it to a destination that can be used to install it
            val destApkPath = "$baseTmpFolder/base.apk"
            assertThat(executeCommand("mv ${tmpApkFile.absolutePath} $destApkPath")).isEmpty()
            cleanUpBlocks.add { executeCommand("rm $destApkPath") }

            // This mimes the behaviour of `adb install-multiple` using an install session.
            // For reference:
            // https://source.corp.google.com/android-internal/packages/modules/adb/client/adb_install.cpp

            // Adds the base apk to the install session
            val successBaseApk =
                executeCommand("pm install-write $sessionId base.apk $destApkPath")
                    .first()
                    .trim()
                    .startsWith("Success")
            if (!successBaseApk) {
                throw IllegalStateException("Could not add $apkName to install session $sessionId")
            }

            // Adds the base dm (profile) to the install session
            if (withProfile) {

                // Generates the profiles using device profile writer
                val tmpProfileProfFile = File(dirUsableByAppAndShell, "tmp_profile.prof")
                    .also {
                        it.delete()
                        it.createNewFile()
                        it.deleteOnExit()
                    }
                cleanUpBlocks.add { tmpProfileProfFile.delete() }

                val deviceProfileWriter = DeviceProfileWriter(
                    instrumentation.context.assets,
                    DirectExecutor.INSTANCE,
                    EMPTY_DIAGNOSTICS,
                    apkName,
                    "${apkName}_$BASELINE_PROF",
                    "${apkName}_$BASELINE_PROFM",
                    tmpProfileProfFile
                )
                if (!deviceProfileWriter.deviceAllowsProfileInstallerAotWrites()) {
                    throw IllegalStateException(
                        "The device does not allow profile installer aot writes"
                    )
                }
                val success = deviceProfileWriter.read()
                    .transcodeIfNeeded()
                    .write()
                if (!success) {
                    throw IllegalStateException(
                        "Profile was not installed correctly."
                    )
                }

                // Compress the profile to generate the dex metadata file
                val tmpDmFile = File(dirUsableByAppAndShell, "tmp_base.dm")
                    .also {
                        it.delete()
                        it.createNewFile()
                        it.deleteOnExit()
                        it.outputStream().use { os ->
                            ZipOutputStream(os).use {

                                // One single zip entry named `primary.prof`
                                it.putNextEntry(ZipEntry("primary.prof"))
                                it.write(tmpProfileProfFile.readBytes())
                            }
                        }
                    }
                cleanUpBlocks.add { tmpDmFile.delete() }

                // Then moves it to a destination that can be used to install it
                val dmFilePath = "$baseTmpFolder/$DM_FILE_NAME"
                executeCommand("mv ${tmpDmFile.absolutePath} $dmFilePath")
                cleanUpBlocks.add { executeCommand("rm $dmFilePath") }

                // Tries to install using pm install write
                val successBaseDm =
                    executeCommand("pm install-write $sessionId base.dm $dmFilePath")
                        .first()
                        .trim()
                        .startsWith("Success")

                if (!successBaseDm) {
                    throw IllegalStateException(
                        "Could not add $dmFilePath to install session $sessionId"
                    )
                }
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
            cleanUpBlocks.reversed().forEach { it() }
        }
    }

    fun start(activityName: String) {
        val error = executeCommand("am start -n $packageName/$activityName")
            .any { it.startsWith("Error") }
        assertThat(error).isFalse()
        uiDevice.waitForIdle()
    }

    fun stop() {
        val error = executeCommand("am force-stop $packageName")
            .any { it.startsWith("Error") }
        assertThat(error).isFalse()
        uiDevice.waitForIdle()
    }

    fun compileCurrentProfile() {
        val stdout = executeCommand("cmd package compile -f -m speed-profile $packageName")
        val success = stdout.first().trim() == "Success"
        assertWithMessage("Profile compilation failed: `$stdout`").that(success).isTrue()
    }

    fun broadcastProfileInstallAction() {
        val result = broadcast(
            packageName = packageName,
            action = "androidx.profileinstaller.action.INSTALL_PROFILE",
            receiverClass = "androidx.profileinstaller.ProfileInstallReceiver"
        )
        assertWithMessage("Profile install action broadcast failed with code.")
            .that(result)
            .isEqualTo(ProfileInstaller.RESULT_INSTALL_SUCCESS)
    }

    private fun broadcast(packageName: String, action: String, receiverClass: String) =
        executeCommand("am broadcast -a $action $packageName/$receiverClass")
            .first { it.contains("Broadcast completed: result=") }
            .split("=")[1]
            .trim()
            .toInt()

    private fun executeCommand(command: String): List<String> {
        Log.d(TAG, "Executing command: `$command`")
        return ParcelFileDescriptor
            .AutoCloseInputStream(uiAutomation.executeShellCommand(command))
            .bufferedReader()
            .lineSequence()
            .toList()
    }

    fun evaluateUI(block: AssertUiBlock.() -> Unit) {
        val resourceId = "id/txtNotice"
        val lines = uiDevice
            .wait(Until.findObject(By.res("$packageName:$resourceId")), UI_TIMEOUT)
            .text
            .lines()
            .map { it.split(":")[1].trim() }
        assertThat(lines).hasSize(3)
        block(AssertUiBlock(lines))
    }

    companion object {
        private const val TAG = "TestManager"
        private const val TEMP_DIR = "/data/local/tmp"
        private const val UI_TIMEOUT = 20000L
        private const val BASELINE_PROF = "baseline.prof"
        private const val BASELINE_PROFM = "baseline.profm"
        private const val DM_FILE_NAME = "base.dm"
        private val REGEX_SESSION_ID = """\[(\d+)\]""".toRegex()
    }

    class AssertUiBlock(private val lines: List<String>) {
        fun profileInstalled(resultCode: Int) =
            assertWithMessage("Unexpected profile verification result code")
                .that(lines[0].toInt())
                .isEqualTo(resultCode)

        fun hasReferenceProfile(value: Boolean) =
            assertWithMessage("Unexpected hasReferenceProfile value")
                .that(lines[1].toBoolean())
                .isEqualTo(value)

        fun hasCurrentProfile(value: Boolean) =
            assertWithMessage("Unexpected hasCurrentProfile value")
                .that(lines[2].toBoolean())
                .isEqualTo(value)
    }

    val isCuttlefish by lazy {
        executeCommand("getprop ro.product.product.model").any { "cuttlefish" in it.lowercase() }
    }
}

private val EMPTY_DIAGNOSTICS: ProfileInstaller.DiagnosticsCallback =
    object : ProfileInstaller.DiagnosticsCallback {
        private val TAG = "ProfileVerifierDiagnosticsCallback"
        override fun onDiagnosticReceived(code: Int, data: Any?) {
            Log.d(TAG, "onDiagnosticReceived: $code")
        }

        override fun onResultReceived(code: Int, data: Any?) {
            Log.d(TAG, "onResultReceived: $code")
        }
    }

private fun <T> T?.throwIfNull(message: String): T = this ?: throw Exception(message)

val isApi30 by lazy { Build.VERSION.SDK_INT == Build.VERSION_CODES.R }
val isApi28 by lazy { Build.VERSION.SDK_INT == Build.VERSION_CODES.P }
val isApi29 by lazy { Build.VERSION.SDK_INT == Build.VERSION_CODES.Q }

const val PACKAGE_NAME_WITH_INITIALIZER =
    "androidx.profileinstaller.integration.profileverification.target"
const val PACKAGE_NAME_WITHOUT_INITIALIZER =
    "androidx.profileinstaller.integration.profileverification.target.no_initializer"
const val ACTIVITY_NAME = ".SampleActivity"
const val APK_WITH_INITIALIZER_V1 = "profile-verification-sample-v1-release.apk"
const val APK_WITH_INITIALIZER_V2 = "profile-verification-sample-v2-release.apk"
const val APK_WITH_INITIALIZER_V3 = "profile-verification-sample-v3-release.apk"
const val APK_WITHOUT_INITIALIZER_V1 = "profile-verification-sample-no-initializer-v1-release.apk"
const val APK_WITHOUT_INITIALIZER_V2 = "profile-verification-sample-no-initializer-v2-release.apk"
const val APK_WITHOUT_INITIALIZER_V3 = "profile-verification-sample-no-initializer-v3-release.apk"