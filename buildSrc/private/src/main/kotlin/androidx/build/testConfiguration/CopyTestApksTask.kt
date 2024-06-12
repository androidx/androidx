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

import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Copy APKs needed for building androidTest.zip */
@DisableCachingByDefault(because = "Only filesystem operations")
abstract class CopyTestApksTask @Inject constructor(private val objects: ObjectFactory) :
    DefaultTask() {

    /** File existence check to determine whether to run this task. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidTestSourceCode: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val testFolder: DirectoryProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appFolder: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appFileCollection: ConfigurableFileCollection

    /**
     * Extracted APKs for PrivacySandbox SDKs dependencies. Produced by AGP.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE) // We use parent folder for file name generation
    abstract val privacySandboxSdkApks: ConfigurableFileCollection

    /**
     * Extracted split with manifest containing <uses-sdk-library> tag. Produced by AGP.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val privacySandboxUsesSdkSplit: ConfigurableFileCollection

    /**
     * Extracted compat splits for PrivacySandbox SDKs dependencies. Produced by AGP.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val privacySandboxSdkCompatSplits: ConfigurableFileCollection

    /**
     * Filename prefix for all PrivacySandbox related output files. Required for producing unique
     * filenames over all projects.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:Input @get:Optional abstract val filenamePrefixForPrivacySandboxFiles: Property<String>

    @get:Internal abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:Internal abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:OutputFile abstract val outputApplicationId: RegularFileProperty

    @get:OutputFile abstract val outputTestApk: RegularFileProperty

    @get:[OutputFile Optional]
    abstract val outputAppApk: RegularFileProperty

    /**
     * Output directory for PrivacySandbox SDKs APKs.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:[OutputDirectory Optional]
    abstract val outputPrivacySandboxSdkApks: DirectoryProperty

    /**
     * Output directory for App splits required for devices with PrivacySandbox support.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:[OutputDirectory Optional]
    abstract val outputPrivacySandboxAppSplits: DirectoryProperty

    /**
     * Output directory for App splits required for devices without PrivacySandbox support.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:[OutputDirectory Optional]
    abstract val outputPrivacySandboxCompatAppSplits: DirectoryProperty

    @TaskAction
    fun createApks() {
        if (appLoader.isPresent) {
            // Decides where to load the app apk from, depending on whether appFolder or
            // appFileCollection has been set.
            val appDir =
                if (appFolder.isPresent && appFileCollection.files.isEmpty()) {
                    appFolder.get()
                } else if (!appFolder.isPresent && appFileCollection.files.size == 1) {
                    objects
                        .directoryProperty()
                        .also { it.set(appFileCollection.files.first()) }
                        .get()
                } else {
                    throw IllegalStateException(
                        """
                    App apk not specified or both appFileCollection and appFolder specified.
                """
                            .trimIndent()
                    )
                }

            val appApk =
                appLoader.get().load(appDir)
                    ?: throw RuntimeException("Cannot load required APK for task: $name")
            // We don't need to check hasBenchmarkPlugin because benchmarks shouldn't have test apps
            val appApkBuiltArtifact = appApk.elements.single()
            val destinationApk = outputAppApk.get().asFile
            File(appApkBuiltArtifact.outputFile).copyTo(destinationApk, overwrite = true)

            createPrivacySandboxFiles()
        }

        val testApk =
            testLoader.get().load(testFolder.get())
                ?: throw RuntimeException("Cannot load required APK for task: $name")
        val testApkBuiltArtifact = testApk.elements.single()
        val destinationApk = outputTestApk.get().asFile
        File(testApkBuiltArtifact.outputFile).copyTo(destinationApk, overwrite = true)

        val outputApplicationIdFile = outputApplicationId.get().asFile
        outputApplicationIdFile.bufferedWriter().use { out -> out.write(testApk.applicationId) }
    }

    /**
     * Creates APKs required for running App with PrivacySandbox SDKs. Do nothing if project doesn't
     * have dependencies on PrivacySandbox SDKs.
     */
    private fun createPrivacySandboxFiles() {
        if (privacySandboxSdkApks.isEmpty) {
            return
        }

        val prefix = filenamePrefixForPrivacySandboxFiles.get()

        privacySandboxSdkApks.asFileTree.map { sdkApk ->
            // TODO (b/309610890): Remove after supporting unique filenames on bundletool side.
            val sdkProjectName = sdkApk.parentFile?.name
            val outputFileName = "$prefix-$sdkProjectName-${sdkApk.name}"
            val outputFile = outputPrivacySandboxSdkApks.get().file(outputFileName)
            sdkApk.copyTo(outputFile.asFile, overwrite = true)
        }

        val usesSdkSplitArtifact =
            appLoader.get().load(privacySandboxUsesSdkSplit)?.elements?.single()
        if (usesSdkSplitArtifact != null) {
            val splitApk = File(usesSdkSplitArtifact.outputFile)
            val outputFileName = "$prefix-${splitApk.name}"
            val outputFile = outputPrivacySandboxAppSplits.get().file(outputFileName)
            splitApk.copyTo(outputFile.asFile, overwrite = true)
        }

        appLoader.get().load(privacySandboxSdkCompatSplits)?.elements?.forEach { splitArtifact ->
            val splitApk = File(splitArtifact.outputFile)
            val outputFileName = "$prefix-${splitApk.name}"
            val outputFile = outputPrivacySandboxCompatAppSplits.get().file(outputFileName)
            splitApk.copyTo(outputFile.asFile, overwrite = true)
        }
    }
}
