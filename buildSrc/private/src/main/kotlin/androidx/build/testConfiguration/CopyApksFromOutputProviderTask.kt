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

package androidx.build.testConfiguration

import com.android.build.api.variant.ApkInstallGroup
import com.android.build.api.variant.ApkOutput
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Copy APKs (from ApkOutputProviders) needed for building androidTest.zip */
@DisableCachingByDefault(because = "Only filesystem operations")
abstract class CopyApksFromOutputProviderTask
@Inject
constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {

    /** File existence check to determine whether to run this task. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidTestSourceCode: ConfigurableFileCollection

    /** ApkOutputProviders output, contains all App APKs. */
    @get:Internal abstract val apkOutput: Property<ApkOutput>

    /** Some Variants includes test APK into ApkOutput, excluding it by using this parameter. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val excludeTestApk: RegularFileProperty

    /**
     * Filename prefix for all output apks. Required for producing unique filenames over all
     * projects.
     *
     * Resulting filename: <outputFilenamesPrefix>-<groupIndex>-<fileIndex>-<originalFileName>
     */
    @get:Input abstract val outputFilenamesPrefix: Property<String>

    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @get:OutputFile abstract val outputAppApksModel: RegularFileProperty

    @TaskAction
    fun createApks() {
        val outputDir = outputDirectory.get()

        // Cleanup old files - to remove stale APKs
        fileSystemOperations.delete { it.delete(outputDir) }

        val fileNamePrefix = outputFilenamesPrefix.get()
        val testApkSha256 = sha256(excludeTestApk.get().asFile)

        val resultApkGroups =
            apkOutput.get().apkInstallGroups.mapIndexedNotNull { groupIndex, installGroup ->
                processApkInstallGroup(
                    installGroup,
                    testApkSha256,
                    fileNamePrefix = "$fileNamePrefix-$groupIndex",
                )
            }

        val model = AppApksModel(resultApkGroups)
        outputAppApksModel.get().asFile.writeText(model.toJson())
    }

    private fun processApkInstallGroup(
        installGroup: ApkInstallGroup,
        excludeSha256: String,
        fileNamePrefix: String,
    ): ApkFileGroup? {
        val outputDir = outputDirectory.get()
        val resultApkFiles =
            installGroup.apks.mapIndexedNotNull { fileIndex, file ->
                val inputFile = file.asFile
                val fileSha256 = sha256(inputFile)
                if (fileSha256 == excludeSha256) {
                    // Some Variants includes test APK into ApkOutput, filter it.
                    return@mapIndexedNotNull null
                }

                val outputFileName = "$fileNamePrefix-$fileIndex-${inputFile.name}"
                val outputFile = outputDir.file(outputFileName).asFile

                inputFile.copyTo(outputFile, overwrite = true)

                return@mapIndexedNotNull ApkFile(name = outputFileName, sha256 = fileSha256)
            }
        return if (resultApkFiles.isEmpty()) {
            null
        } else {
            ApkFileGroup(resultApkFiles)
        }
    }
}
