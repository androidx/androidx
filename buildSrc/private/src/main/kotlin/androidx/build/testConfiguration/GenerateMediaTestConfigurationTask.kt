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
import androidx.build.renameApkForTesting
import com.android.build.api.variant.BuiltArtifact
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
    abstract val clientToTPath: Property<String>

    @get:Input
    abstract val clientPreviousPath: Property<String>

    @get:Input
    abstract val serviceToTPath: Property<String>

    @get:Input
    abstract val servicePreviousPath: Property<String>

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

    @TaskAction
    fun generateAndroidTestZip() {
        val clientToTApk = resolveApk(clientToTFolder, clientToTLoader)
        val clientPreviousApk = resolveApk(clientPreviousFolder, clientPreviousLoader)
        val serviceToTApk = resolveApk(serviceToTFolder, serviceToTLoader)
        val servicePreviousApk = resolveApk(
            servicePreviousFolder, servicePreviousLoader
        )
        writeConfigFileContent(
            clientApk = clientToTApk,
            serviceApk = serviceToTApk,
            clientPath = clientToTPath.get(),
            servicePath = serviceToTPath.get(),
            jsonClientOutputFile = jsonClientToTServiceToTClientTests,
            jsonServiceOutputFile = jsonClientToTServiceToTServiceTests,
            isClientPrevious = false,
            isServicePrevious = false
        )
        writeConfigFileContent(
            clientApk = clientToTApk,
            serviceApk = servicePreviousApk,
            clientPath = clientToTPath.get(),
            servicePath = servicePreviousPath.get(),
            jsonClientOutputFile = jsonClientToTServicePreviousClientTests,
            jsonServiceOutputFile = jsonClientToTServicePreviousServiceTests,
            isClientPrevious = false,
            isServicePrevious = true
        )
        writeConfigFileContent(
            clientApk = clientPreviousApk,
            serviceApk = serviceToTApk,
            clientPath = clientPreviousPath.get(),
            servicePath = serviceToTPath.get(),
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

    private fun BuiltArtifact.resolveName(path: String): String {
        return outputFile.substringAfterLast("/").renameApkForTesting(path)
    }

    private fun writeConfigFileContent(
        clientApk: BuiltArtifacts,
        serviceApk: BuiltArtifacts,
        clientPath: String,
        servicePath: String,
        jsonClientOutputFile: RegularFileProperty,
        jsonServiceOutputFile: RegularFileProperty,
        isClientPrevious: Boolean,
        isServicePrevious: Boolean,
    ) {
        val clientBuiltArtifact = clientApk.elements.single()
        val serviceBuiltArtifact = serviceApk.elements.single()
        val clientApkName = clientBuiltArtifact.resolveName(clientPath)
        val clientApkSha256 = sha256(File(clientBuiltArtifact.outputFile))
        val serviceApkName = serviceBuiltArtifact.resolveName(servicePath)
        val serviceApkSha256 = sha256(File(serviceBuiltArtifact.outputFile))
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
