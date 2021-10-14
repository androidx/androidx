/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.layoutgenerator.gradle

import androidx.glance.appwidget.layoutgenerator.LayoutGenerator
import androidx.glance.appwidget.layoutgenerator.cleanResources
import androidx.glance.appwidget.layoutgenerator.generateRegistry
import com.android.build.gradle.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task generating the layouts from a set of Layout templates.
 *
 * See [LayoutGenerator] for details on the template format, and [generateRegistry] for details
 * on the Kotlin code generated to access those layouts from code.
 */
abstract class LayoutGeneratorTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val layoutDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputResourcesDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val generatedFiles = LayoutGenerator().generateAllFiles(
            checkNotNull(layoutDirectory.get().asFile.listFiles()).asList(),
            outputResourcesDir.get().asFile
        )
        generateRegistry(outputModule, generatedFiles, outputSourceDir.get().asFile)
        cleanResources(outputResourcesDir.get().asFile, generatedFiles.keys)
    }

    companion object {
        /**
         * Registers [LayoutGeneratorTask] in [project] for all variants in [libraryExtension].
         */
        @JvmStatic
        fun registerLayoutGenerator(
            project: Project,
            libraryExtension: LibraryExtension,
            layoutDirectory: File
        ) {
            libraryExtension.libraryVariants.all { variant ->
                val variantName = variant.name
                val outputDirectory = project.buildDir.resolve("generatedLayouts/$variantName")
                val outputResourcesDir = outputDirectory.resolve("res/layouts")
                val outputSourceDir = outputDirectory.resolve("kotlin")
                val taskName =
                    "generateLayouts" + variantName.replaceFirstChar { it.uppercaseChar() }
                outputResourcesDir.mkdirs()
                outputSourceDir.mkdirs()
                val task = project.tasks.register(taskName, LayoutGeneratorTask::class.java) {
                    it.layoutDirectory.set(layoutDirectory)
                    it.outputResourcesDir.set(outputResourcesDir)
                    it.outputSourceDir.set(outputSourceDir)
                }
                variant.registerGeneratedResFolders(
                    project.files(outputResourcesDir).builtBy(task)
                )
                variant.registerJavaGeneratingTask(task, outputSourceDir)
            }
        }
    }
}

private const val outputModule: String = "androidx.glance.appwidget"
