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

package androidx.build

import java.io.File
import java.util.Locale
import javax.inject.Inject
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/** A build service that unzips Chrome prebuilts for use in other Gradle tasks. */
abstract class UnzipChromeBuildService
@Inject
constructor(
    private val archiveOperations: ArchiveOperations,
    private val fileSystemOperations: FileSystemOperations,
) : BuildService<UnzipChromeBuildService.Parameters> {

    interface Parameters : BuildServiceParameters {
        /** Location of Chrome prebuilts. */
        val browserDir: DirectoryProperty

        /** Location to unzip to. */
        val unzipToDir: DirectoryProperty
    }

    val chromePath: String by lazy { unzipChrome() }

    /** Unzips the Chrome prebuilt for the current OS and returns the path of the executable. */
    private fun unzipChrome(): String {
        val osName = chromeBinOsSuffix()
        val chromeZip =
            File(parameters.browserDir.get().asFile, "chrome-headless-shell-$osName.zip")

        fileSystemOperations.copy {
            it.from(archiveOperations.zipTree(chromeZip))
            it.into(parameters.unzipToDir)
        }
        return parameters.unzipToDir
            .get()
            .asFile
            .resolve("chrome-headless-shell-$osName/chrome-headless-shell")
            .path
    }
}

private fun chromeBinOsSuffix() =
    when {
        System.getProperty("os.name").lowercase(Locale.ROOT).contains("linux") -> "linux64"
        System.getProperty("os.arch") == "aarch64" -> "mac-arm64"
        else -> "mac-x64"
    }
