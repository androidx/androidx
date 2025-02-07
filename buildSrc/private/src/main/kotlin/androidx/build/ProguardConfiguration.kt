/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.build.api.dsl.ConsumerKeepRules
import com.android.build.api.dsl.LibraryBuildType
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Add a blank consumer proguard rules file to the JAR if the library has not set up an explicit set
 * of rules.
 */
internal fun Project.setUpBlankProguardFileForJarIfNeeded(javaExtension: JavaPluginExtension) {
    if (project.multiplatformExtension != null) return // skip KMP projects
    val mainSources = javaExtension.sourceSets.getByName("main")
    val provider =
        tasks.register("emptyProguardFileCopy", BlankProguardFileGenerator::class.java) {
            it.blankProguardFile.set(blankProguardRules())
            it.outputDirectory.set(layout.buildDirectory.dir("blankProguard"))
            it.nonGeneratedResources.from(mainSources.resources.sourceDirectories)
            // unique name like "androidx-arch-core-core-common"
            it.libraryName.set("androidx${project.path.replace(":", "-")}")
        }
    mainSources.output.dir(provider.flatMap { it.outputDirectory })
}

/**
 * Add a blank consumer proguard rules file to the AAR if the library has not set up an explicit set
 * of rules.
 */
internal fun Project.setUpBlankProguardFileForAarIfNeeded(buildType: LibraryBuildType) {
    if (buildType.consumerProguardFiles.isEmpty()) {
        buildType.consumerProguardFiles.add(blankProguardRules())
    }
}

/**
 * Add a blank consumer proguard rules file to the AAR if the library has not set up an explicit set
 * of rules.
 */
@Suppress("UnstableApiUsage")
internal fun Project.setUpBlankProguardFileForKmpAarIfNeeded(consumerKeepRules: ConsumerKeepRules) {
    if (consumerKeepRules.files.isEmpty()) {
        file(project.blankProguardRules())
        consumerKeepRules.publish = true
    }
}

@DisableCachingByDefault
abstract class BlankProguardFileGenerator : DefaultTask() {
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val blankProguardFile: RegularFileProperty

    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val nonGeneratedResources: ConfigurableFileCollection

    @get:Input abstract val libraryName: Property<String>

    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyEmptyFile() {
        outputDirectory.get().asFile.deleteRecursively()
        val hasExplicitProguardFile =
            nonGeneratedResources.any { File(it, "META-INF/proguard").exists() }
        // Check if the library already contains explicit proguard file
        if (hasExplicitProguardFile) return
        blankProguardFile
            .get()
            .asFile
            .copyTo(
                File(outputDirectory.get().asFile, "META-INF/proguard/${libraryName.get()}.pro")
            )
    }
}

private fun Project.blankProguardRules(): File =
    project.getSupportRootFolder().resolve("buildSrc/blank-proguard-rules/proguard-rules.pro")
