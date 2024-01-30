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

package androidx.tracing.perfetto.handshake

import java.io.File
import java.util.zip.ZipFile

/**
 * Sideloads the `libtracing_perfetto.so` file to a location available to the traced app
 *
 * The class solves the following sub-problems:
 * - knowing the right location to place the binaries
 * - knowing how to extract the binaries from an AAR or APK, including choosing the right build
 *   variant for the device (e.g. arm64-v8a) from the archive
 * - knowing how to handle device IO permissions, e.g. to allow a Benchmark app place
 *   the Perfetto binaries in a location accessible by the benchmarked app (we use `shell` for this)
 */
internal class PerfettoSdkSideloader(private val packageName: String) {

    /**
     * Sideloads `libtracing_perfetto.so` from a ZIP source to a location available to the traced
     * app
     *
     * @param sourceZipFile either an AAR or an APK containing `libtracing_perfetto.so`
     * @param shellCommandExecutor function capable of executing adb shell commands (used to
     * determine the device ABI)
     * @param tempDirectory a directory directly accessible to the process (used for extraction
     * of the binaries from the zip)
     * @param moveLibFileFromTmpDirToAppDir a function capable of moving the binary file from
     * the [tempDirectory] and an app accessible folder
     *
     * @return location where the library file was sideloaded to
     */
    fun sideloadFromZipFile(
        sourceZipFile: File,
        tempDirectory: File,
        shellCommandExecutor: ShellCommandExecutor,
        moveLibFileFromTmpDirToAppDir: FileMover
    ): File {
        val abi = getDeviceAbi(shellCommandExecutor)
        val tmpFile = extractPerfettoBinaryFromZip(sourceZipFile, tempDirectory, abi)
        return sideloadSoFile(tmpFile, moveLibFileFromTmpDirToAppDir)
    }

    /**
     * Sideloads `libtracing_perfetto.so` to a location available to the traced app
     *
     * @param libFile `libtracing_perfetto.so` file
     * @param moveLibFileToAppDir a function moving the [libFile] to an app accessible folder
     *
     * @return location where the library file was sideloaded to
     */
    private fun sideloadSoFile(libFile: File, moveLibFileToAppDir: FileMover): File {
        val dstFile = libFileForPackageName(packageName)
        moveLibFileToAppDir(libFile, dstFile)
        return dstFile
    }

    private fun extractPerfettoBinaryFromZip(
        sourceZip: File,
        outputDir: File,
        abi: String
    ): File {
        val outputFile = outputDir.resolve(libFileName)
        val rxLibPathInsideZip = Regex(".*(lib|jni)/[^/]*$abi[^/]*/$libFileName")
        val zipFile = ZipFile(sourceZip)
        val entry = zipFile
            .entries()
            .asSequence()
            .firstOrNull { it.name.matches(rxLibPathInsideZip) }
            ?: throw IllegalStateException(
                "Unable to locate $libFileName required to enable Perfetto SDK. " +
                    "Tried inside ${sourceZip.absolutePath}."
            )
        zipFile.getInputStream(entry).use { inputStream ->
            outputFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return outputFile
    }

    private fun getDeviceAbi(executeShellCommand: ShellCommandExecutor): String =
        executeShellCommand("getprop ro.product.cpu.abilist").split(",")
            .plus(executeShellCommand("getprop ro.product.cpu.abi"))
            .first()
            .trim()

    private companion object {
        private const val libFileName = "libtracing_perfetto.so"

        fun libFileForPackageName(packageName: String) =
            File("/sdcard/Android/media/$packageName/$libFileName")
    }
}

internal typealias FileMover = (srcFile: File, dstFile: File) -> Unit

internal typealias ShellCommandExecutor = (command: String) -> String
