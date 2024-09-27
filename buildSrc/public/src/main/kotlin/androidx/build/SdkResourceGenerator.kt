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

import com.google.common.annotations.VisibleForTesting
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Simply generates a small file and doesn't benefit from caching")
abstract class SdkResourceGenerator : DefaultTask() {
    @get:Input lateinit var tipOfTreeMavenRepoRelativePath: String

    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val debugKeystore: RegularFileProperty

    @get:Input abstract val compileSdk: Property<Int>

    @get:Input abstract val buildToolsVersion: Property<String>

    @get:Input abstract val minSdkVersion: Property<Int>

    @get:Input abstract val agpDependency: Property<String>

    @get:Input abstract val kotlinStdlib: Property<String>

    @get:Input abstract val kgpVersion: Property<String>

    @get:Input abstract val kspVersion: Property<String>

    @get:Input lateinit var repositoryUrls: List<String>

    @get:Input
    val rootProjectRelativePath: String =
        project.rootProject.rootDir.toRelativeString(project.projectDir)

    @get:Input
    @get:Optional
    val prebuiltsRelativePath: String? =
        if (ProjectLayoutType.isPlayground(project)) {
            null
        } else {
            project.getPrebuiltsRoot().toRelativeString(project.projectDir)
        }

    private val projectDir: File = project.projectDir

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateFile() {
        // Note all the paths in sdk.prop have to be relative to projectDir to make this task
        // cacheable between different computers
        val outputFile = outputDir.file("sdk.prop")
        outputFile.get().asFile.writer().use { writer ->
            writer.write("tipOfTreeMavenRepoRelativePath=$tipOfTreeMavenRepoRelativePath\n")
            writer.write(
                "debugKeystoreRelativePath=${
                    debugKeystore.get().asFile.toRelativeString(projectDir)
                }\n"
            )
            writer.write("rootProjectRelativePath=$rootProjectRelativePath\n")
            val encodedRepositoryUrls = repositoryUrls.joinToString(",")
            writer.write("repositoryUrls=$encodedRepositoryUrls\n")

            writer.write("agpDependency=${agpDependency.get()}\n")
            writer.write("kotlinStdlib=${kotlinStdlib.get()}\n")
            writer.write("compileSdk=${compileSdk.get()}\n")
            writer.write("buildToolsVersion=${buildToolsVersion.get()}\n")
            writer.write("minSdkVersion=${minSdkVersion.get()}\n")
            writer.write("kgpVersion=${kgpVersion.get()}\n")
            writer.write("kspVersion=${kspVersion.get()}\n")
            if (prebuiltsRelativePath != null) {
                writer.write("prebuiltsRelativePath=$prebuiltsRelativePath\n")
            }
        }
    }

    companion object {
        const val TASK_NAME = "generateSdkResource"

        @JvmStatic
        fun generateForHostTest(project: Project) {
            val provider = registerSdkResourceGeneratorTask(project)
            val extension = project.extensions.getByType<JavaPluginExtension>()
            val testSources = extension.sourceSets.getByName("test")
            testSources.output.dir(provider.flatMap { it.outputDir })
        }

        @VisibleForTesting
        fun registerSdkResourceGeneratorTask(
            project: Project,
            kspVersion: String = project.getVersionByName("ksp"),
            agpVersion: String = project.getVersionByName("androidGradlePlugin"),
            kgpVersion: String = project.getVersionByName("kotlin")
        ): TaskProvider<SdkResourceGenerator> {
            val generatedDirectory = project.layout.buildDirectory.dir("generated/resources")
            return project.tasks.register(TASK_NAME, SdkResourceGenerator::class.java) {
                it.tipOfTreeMavenRepoRelativePath =
                    project.getRepositoryDirectory().toRelativeString(project.projectDir)
                it.debugKeystore.set(project.getKeystore())
                it.outputDir.set(generatedDirectory)
                it.buildToolsVersion.set(
                    project.provider { project.defaultAndroidConfig.buildToolsVersion }
                )
                it.minSdkVersion.set(project.defaultAndroidConfig.minSdk)
                it.compileSdk.set(project.defaultAndroidConfig.compileSdk)
                it.kotlinStdlib.set(
                    project.androidXConfiguration.kotlinBomVersion.map { version ->
                        "org.jetbrains.kotlin:kotlin-stdlib:$version"
                    }
                )
                it.kspVersion.set(kspVersion)
                it.agpDependency.set("com.android.tools.build:gradle:$agpVersion")
                it.kgpVersion.set(kgpVersion)
                // Copy repositories used for the library project so that it can replicate the same
                // maven structure in test.
                it.repositoryUrls =
                    project.repositories.filterIsInstance<MavenArtifactRepository>().map {
                        if (it.url.scheme == "file") {
                            // Make file paths relative to projectDir
                            File(it.url.path).toRelativeString(project.projectDir)
                        } else {
                            it.url.toString()
                        }
                    }
            }
        }
    }
}
