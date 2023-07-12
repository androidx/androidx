/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.build.dependencyTracker.ProjectSubset
import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes three configuration files to test combinations of media client & service in
 * <a href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested. The combinations are of previous and
 * tip-of-tree versions client and service. We want to test every possible pairing that includes
 * tip-of-tree.
 *
 * This config gets ingested by Tradefed.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class GenerateMediaTestConfigurationTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clientToTFolder: DirectoryProperty

    @get:Internal
    abstract val clientToTLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clientPreviousFolder: DirectoryProperty

    @get:Internal
    abstract val clientPreviousLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val serviceToTFolder: DirectoryProperty

    @get:Internal
    abstract val serviceToTLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val servicePreviousFolder: DirectoryProperty

    @get:Internal
    abstract val servicePreviousLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val affectedModuleDetectorSubset: Property<ProjectSubset>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:Input
    abstract val testRunner: Property<String>

    @get:Input
    abstract val presubmit: Property<Boolean>

    @get:OutputFile
    abstract val jsonClientPreviousServiceToTClientTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientPreviousServiceToTServiceTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServicePreviousClientTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServicePreviousServiceTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServiceToTClientTests: RegularFileProperty

    @get:OutputFile
    abstract val jsonClientToTServiceToTServiceTests: RegularFileProperty

    @get:OutputFile
    abstract val previousClientApk: RegularFileProperty

    @get:OutputFile
    abstract val totClientApk: RegularFileProperty

    @get:OutputFile
    abstract val previousServiceApk: RegularFileProperty

    @get:OutputFile
    abstract val totServiceApk: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        val clientToTApk = totClientApk.get().asFile
        val clientToTSha256 = copyApkAndGetSha256(clientToTFolder, clientToTLoader, clientToTApk)
        val clientPreviousApk = previousClientApk.get().asFile
        val clientPreviousSha256 = copyApkAndGetSha256(
            clientPreviousFolder, clientPreviousLoader, clientPreviousApk
        )
        val serviceToTApk = totServiceApk.get().asFile
        val serviceToTSha256 = copyApkAndGetSha256(
            serviceToTFolder, serviceToTLoader, serviceToTApk
        )
        val servicePreviousApk = previousServiceApk.get().asFile
        val servicePreviousSha256 = copyApkAndGetSha256(
            servicePreviousFolder, servicePreviousLoader, servicePreviousApk
        )

        writeConfigFileContent(
            clientApkName = clientToTApk.name,
            serviceApkName = serviceToTApk.name,
            clientApkSha256 = clientToTSha256,
            serviceApkSha256 = serviceToTSha256,
            jsonClientOutputFile = jsonClientToTServiceToTClientTests,
            jsonServiceOutputFile = jsonClientToTServiceToTServiceTests,
            isClientPrevious = false,
            isServicePrevious = false
        )
        writeConfigFileContent(
            clientApkName = clientToTApk.name,
            serviceApkName = servicePreviousApk.name,
            clientApkSha256 = clientToTSha256,
            serviceApkSha256 = servicePreviousSha256,
            jsonClientOutputFile = jsonClientToTServicePreviousClientTests,
            jsonServiceOutputFile = jsonClientToTServicePreviousServiceTests,
            isClientPrevious = false,
            isServicePrevious = true
        )
        writeConfigFileContent(
            clientApkName = clientPreviousApk.name,
            serviceApkName = serviceToTApk.name,
            clientApkSha256 = clientPreviousSha256,
            serviceApkSha256 = serviceToTSha256,
            jsonClientOutputFile = jsonClientPreviousServiceToTClientTests,
            jsonServiceOutputFile = jsonClientPreviousServiceToTServiceTests,
            isClientPrevious = true,
            isServicePrevious = false
        )
    }

    private fun resolveApk(
        apkFolder: DirectoryProperty,
        apkLoader: Property<BuiltArtifactsLoader>
    ): BuiltArtifacts {
        return apkLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
    }

    private fun copyApkAndGetSha256(
        apkFolder: DirectoryProperty,
        apkLoader: Property<BuiltArtifactsLoader>,
        destination: File
    ): String {
        val artifacts = apkLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load required APK for task: $name")
        File(artifacts.elements.single().outputFile).copyTo(destination, overwrite = true)
        return sha256(destination)
    }

    private fun writeConfigFileContent(
        clientApkName: String,
        serviceApkName: String,
        clientApkSha256: String,
        serviceApkSha256: String,
        jsonClientOutputFile: RegularFileProperty,
        jsonServiceOutputFile: RegularFileProperty,
        isClientPrevious: Boolean,
        isServicePrevious: Boolean,
    ) {
        createOrFail(jsonClientOutputFile).writeText(
            buildMediaJson(
                configName = jsonClientOutputFile.asFile.get().name,
                forClient = true,
                clientApkName = clientApkName,
                clientApkSha256 = clientApkSha256,
                isClientPrevious = isClientPrevious,
                isServicePrevious = isServicePrevious,
                minSdk = minSdk.get().toString(),
                serviceApkName = serviceApkName,
                serviceApkSha256 = serviceApkSha256,
                tags = listOf(
                    "androidx_unit_tests",
                    "media_compat"
                ),
            )
        )
        createOrFail(jsonServiceOutputFile).writeText(
            buildMediaJson(
                configName = jsonServiceOutputFile.asFile.get().name,
                forClient = false,
                clientApkName = clientApkName,
                clientApkSha256 = clientApkSha256,
                isClientPrevious = isClientPrevious,
                isServicePrevious = isServicePrevious,
                minSdk = minSdk.get().toString(),
                serviceApkName = serviceApkName,
                serviceApkSha256 = serviceApkSha256,
                tags = listOf(
                    "androidx_unit_tests",
                    "media_compat"
                ),
            )
        )
    }
}
