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

import androidx.glance.appwidget.layoutgenerator.GeneratedFiles
import androidx.glance.appwidget.layoutgenerator.LayoutGenerator
import androidx.glance.appwidget.layoutgenerator.cleanResources
import androidx.glance.appwidget.layoutgenerator.generateRegistry
import com.android.build.api.variant.AndroidComponentsExtension
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Task generating the layouts from a set of Layout templates.
 *
 * See [LayoutGenerator] for details on the template format, and [generateRegistry] for details
 * on the Kotlin code generated to access those layouts from code.
 */
@CacheableTask
abstract class LayoutGeneratorTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val containerLayoutDirectory: DirectoryProperty

    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val childLayoutDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputResourcesDir: DirectoryProperty

    @TaskAction
    fun execute() {

        outputSourceDir.asFile.get().mkdirs()
        outputResourcesDir.asFile.get().mkdirs()

        val generatedLayouts = LayoutGenerator().generateAllFiles(
            checkNotNull(containerLayoutDirectory.get().asFile.listFiles()).asList(),
            checkNotNull(childLayoutDirectory.get().asFile.listFiles()).asList(),
            outputResourcesDir.get().asFile
        )
        generateRegistry(
            packageName = outputModule,
            layouts = generatedLayouts.generatedContainers,
            boxChildLayouts = generatedLayouts.generatedBoxChildren,
            rowColumnChildLayouts = generatedLayouts.generatedRowColumnChildren,
            outputSourceDir = outputSourceDir.get().asFile
        )
        cleanResources(
            outputResourcesDir.get().asFile, generatedLayouts.extractGeneratedFiles()
        )
    }

    private fun GeneratedFiles.extractGeneratedFiles(): Set<File> =
        generatedContainers.values.flatMap { container ->
            container.map { it.generatedFile }
        }.toSet() + generatedBoxChildren.values.flatMap { child ->
            child.map { it.generatedFile }
        }.toSet() + generatedRowColumnChildren.values.flatMap { child ->
            child.map { it.generatedFile }
        }.toSet() + extraFiles

    companion object {
        /**
         * Registers [LayoutGeneratorTask] in [project] for all variants.
         */
        @JvmStatic
        fun registerLayoutGenerator(
            project: Project,
            containerLayoutDirectory: File,
            childLayoutDirectory: File,
        ) {

            val outputDirectory = "generatedLayouts"
            val buildDirectory = project.layout.buildDirectory

            val taskName = "generateLayouts"

            val task = project.tasks.register(taskName, LayoutGeneratorTask::class.java) {
                it.containerLayoutDirectory.set(containerLayoutDirectory)
                it.childLayoutDirectory.set(childLayoutDirectory)
                it.outputSourceDir.set(buildDirectory.dir("$outputDirectory/kotlin"))
                it.outputResourcesDir.set(buildDirectory.dir("$outputDirectory/res/layouts"))
            }

            project.extensions.getByType(AndroidComponentsExtension::class.java)
                .onVariants { variant ->
                    variant.sources.java?.addGeneratedSourceDirectory(
                        task, LayoutGeneratorTask::outputSourceDir
                    )
                    variant.sources.res?.addGeneratedSourceDirectory(
                        task, LayoutGeneratorTask::outputResourcesDir
                    )
                }
        }
    }
}

private const val outputModule: String = "androidx.glance.appwidget"
