/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.intellij

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import org.gradle.process.ExecOperations

/**
 * Utility class containing helper functions and values that change between Linux and OSX
 *
 * @property projectRoot the root directory of the current project
 * @property intellijInstallationDir the directory where IJ is installed to
 */
sealed class IntelliJPlatformUtilities(val projectRoot: File, val intellijInstallationDir: File) {
    /** The file extension used for this platform's IntelliJ archive */
    abstract val archiveExtension: String

    /** The binary directory of the IntelliJ installation. */
    abstract val IntelliJTask.binaryDirectory: File

    /** A list of arguments that will be executed in a shell to launch IntelliJ. */
    abstract val IntelliJTask.launchCommandArguments: List<String>

    /** The lib directory of the IntelliJ installation. */
    abstract val IntelliJTask.libDirectory: File

    /** The license path for the IntelliJ installation. */
    abstract val IntelliJTask.licensePath: String

    /** Extracts an archive at [fromPath] with [archiveExtension] to [toPath] */
    abstract fun extractArchive(fromPath: String, toPath: String, execOperations: ExecOperations)

    /** Returns the PID of the process started by this task, or `null` if not running. */
    abstract fun findProcess(): Int?

    companion object {
        val osName =
            if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("linux")) {
                "linux"
            } else {
                // Only works when using native version of JDK, otherwise it will fallback to x86_64
                if (System.getProperty("os.arch") == "aarch64") {
                    "mac_arm"
                } else {
                    "mac"
                }
            }

        fun get(projectRoot: File, intellijInstallationDir: File): IntelliJPlatformUtilities {
            return if (IntelliJPlatformUtilities.Companion.osName == "linux") {
                LinuxUtilities(projectRoot, intellijInstallationDir)
            } else {
                MacOsUtilities(projectRoot, intellijInstallationDir)
            }
        }
    }
}

private class MacOsUtilities(projectRoot: File, intellijInstallationDir: File) :
    IntelliJPlatformUtilities(projectRoot, intellijInstallationDir) {
    override val archiveExtension: String
        get() = ".dmg"

    override val IntelliJTask.binaryDirectory: File
        get() {
            val file =
                intellijInstallationDir.walk().maxDepth(1).find { file ->
                    file.nameWithoutExtension.startsWith("IntelliJ IDEA") && file.extension == "app"
                }
            return requireNotNull(file) { "IntelliJ IDEA*.app not found!" }
        }

    override val IntelliJTask.launchCommandArguments: List<String>
        get() {
            val intellijBinary = File(binaryDirectory.absolutePath, "Contents/MacOS/intellij")
            return listOf(intellijBinary.absolutePath, projectRoot.absolutePath)
        }

    override val IntelliJTask.libDirectory: File
        get() = File(binaryDirectory, "Contents/lib")

    override val IntelliJTask.licensePath: String
        get() = File(binaryDirectory, "Contents/Resources/LICENSE.txt").absolutePath

    override fun extractArchive(fromPath: String, toPath: String, execOperations: ExecOperations) {
        val mountPoint = File.createTempFile("mount", null)
        mountPoint.delete()
        mountPoint.mkdir()
        execOperations.exec { execOperation ->
            with(execOperation) {
                executable("hdiutil")
                args("attach", fromPath, "-noverify", "-mountpoint", mountPoint.absolutePath)
            }
        }
        execOperations.exec { execOperation ->
            with(execOperation) {
                commandLine("sh", "-c", "cp -R ${mountPoint.absolutePath}/*.app $toPath")
            }
        }
        execOperations.exec { execOperation ->
            with(execOperation) {
                executable("hdiutil")
                args("detach", mountPoint.absolutePath)
            }
        }
        mountPoint.delete()
    }

    override fun findProcess(): Int? {
        println("Detecting active managed IntelliJ instances...")
        val process =
            ProcessBuilder().let {
                it.command(listOf("ps", "-x"))
                it.redirectError(ProcessBuilder.Redirect.INHERIT)
                it.start()
            }
        val stdout =
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().toList()
            }
        process.waitFor()
        val projectRootPath = projectRoot.absolutePath
        return stdout
            .firstOrNull { line -> line.endsWith("Contents/MacOS/intellij $projectRootPath") }
            ?.substringBefore(' ')
            ?.toIntOrNull()
    }
}

private class LinuxUtilities(projectRoot: File, intellijInstallationDir: File) :
    IntelliJPlatformUtilities(projectRoot, intellijInstallationDir) {
    override val archiveExtension: String
        get() = ".tar.gz"

    override val IntelliJTask.binaryDirectory: File
        get() = File(intellijInstallationDir, "intellij")

    override val IntelliJTask.launchCommandArguments: List<String>
        get() {
            val intellijBinary = File(binaryDirectory, "bin/intellij")
            return listOf(intellijBinary.absolutePath, projectRoot.absolutePath)
        }

    override val IntelliJTask.libDirectory: File
        get() = File(binaryDirectory, "lib")

    override val IntelliJTask.licensePath: String
        get() = File(binaryDirectory, "LICENSE.txt").absolutePath

    override fun extractArchive(fromPath: String, toPath: String, execOperations: ExecOperations) {
        execOperations.exec { execOperation ->
            with(execOperation) {
                executable("tar")
                args("-xf", fromPath, "-C", toPath)
            }
        }
    }

    override fun findProcess(): Int? {
        println("Detecting active managed IntelliJ instances...")
        val process =
            ProcessBuilder().let {
                it.command(listOf("ps", "-x"))
                it.redirectError(ProcessBuilder.Redirect.INHERIT)
                it.start()
            }
        val stdout =
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().toList()
            }
        process.waitFor()
        val projectRootPath = projectRoot.absolutePath
        return stdout
            .firstOrNull { line -> line.endsWith("com.intellij.idea.Main $projectRootPath") }
            ?.substringBefore(' ')
            ?.toIntOrNull()
    }
}
