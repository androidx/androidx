/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.build.dependencies.AGP_LATEST
import androidx.build.dependencies.KOTLIN_STDLIB
import androidx.build.dependencies.KOTLIN_VERSION
import androidx.build.dependencies.KSP_VERSION
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.util.Properties

abstract class SdkResourceGenerator : DefaultTask() {
    @get:Input
    lateinit var prebuiltsRoot: String

    @get:Input
    lateinit var localSupportRepo: String

    @get:InputFile
    abstract val debugKeystore: RegularFileProperty

    @get:Input
    val compileSdkVersion: Int = SupportConfig.TARGET_SDK_VERSION

    @get:Input
    val buildToolsVersion: String = SupportConfig.BUILD_TOOLS_VERSION

    @get:Input
    val minSdkVersion: Int = SupportConfig.DEFAULT_MIN_SDK_VERSION

    @get:Input
    val agpDependency: String = AGP_LATEST

    @get:Input
    val navigationRuntime: String = "androidx.navigation:navigation-runtime:2.4.0-alpha01"

    @get:Input
    val kotlinStdlib: String = KOTLIN_STDLIB

    @get:Input
    val kotlinVersion: String = KOTLIN_VERSION

    @get:Input
    val kspVersion: String = KSP_VERSION

    @get:Input
    lateinit var repositoryUrls: List<String>

    @get:Input
    val rootProjectPath: String = project.rootProject.rootDir.absolutePath

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val gradleWrapperProperties: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateFile() {
        val outputFile = outputDir.file("sdk.prop")
        outputFile.get().asFile.writer().use { writer ->
            writer.write("prebuiltsRoot=$prebuiltsRoot\n")
            writer.write("localSupportRepo=$localSupportRepo\n")
            writer.write("compileSdkVersion=$compileSdkVersion\n")
            writer.write("buildToolsVersion=$buildToolsVersion\n")
            writer.write("minSdkVersion=$minSdkVersion\n")
            writer.write("debugKeystore=${debugKeystore.get().asFile.canonicalPath}\n")
            writer.write("agpDependency=$agpDependency\n")
            writer.write("navigationRuntime=$navigationRuntime\n")
            writer.write("kotlinStdlib=$kotlinStdlib\n")

            val gradlewPropFile = gradleWrapperProperties.asFile.get()
            val distributionUrl = gradlewPropFile.reader().use {
                Properties().apply { load(it) }.getProperty("distributionUrl")
            }.let { File(gradlewPropFile.parentFile, it).canonicalPath }

            writer.write("gradleDistributionUrl=$distributionUrl\n")
            writer.write("kotlinVersion=$kotlinVersion\n")
            writer.write("kspVersion=$kspVersion\n")
            writer.write("rootProjectPath=$rootProjectPath\n")
            val encodedRepositoryUrls = repositoryUrls.joinToString(",")
            writer.write("repositoryUrls=$encodedRepositoryUrls\n")
        }
    }

    companion object {
        @JvmStatic
        fun generateForHostTest(project: Project) {
            val generatedDirectory = File(project.buildDir, "generated/resources")
            val provider = project.tasks.register(
                "generateSdkResource",
                SdkResourceGenerator::class.java
            ) {
                it.prebuiltsRoot = project.getPrebuiltsRoot().canonicalPath
                it.debugKeystore.set(project.getKeystore())
                it.localSupportRepo = project.getRepositoryDirectory().canonicalPath
                it.gradleWrapperProperties.set(
                    File(project.rootDir, "gradle/wrapper/gradle-wrapper.properties")
                )
                it.outputDir.set(generatedDirectory)
                // Copy repositories used for the library project so that it can replicate the same
                // maven structure in test.
                it.repositoryUrls = project.repositories.filterIsInstance<MavenArtifactRepository>()
                    .map {
                        it.url.toString()
                    }
            }

            val extension = project.extensions.getByType<JavaPluginExtension>()
            val testSources = extension.sourceSets.getByName("test")
            testSources.getOutput().dir(provider.flatMap { it.outputDir })
        }
    }
}
