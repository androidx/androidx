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

package androidx.ui.material.icons.generator.tasks

import androidx.ui.material.icons.generator.Icon
import androidx.ui.material.icons.generator.IconProcessor
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Base [org.gradle.api.Task] for tasks relating to icon generation.
 */
abstract class IconGenerationTask : DefaultTask() {
    /**
     * Directory containing raw drawables. These icons will be processed to generate programmatic
     * representations.
     */
    @InputDirectory
    val iconDirectory =
        project.rootProject.project(GeneratorProject).projectDir.resolve("raw-icons")

    /**
     * Checked-in API file for the generator module, where we will track all the generated icons
     */
    @InputFile
    val expectedApiFile =
        project.rootProject.project(GeneratorProject).projectDir.resolve("api/icons.txt")

    /**
     * Root build directory for this task, where outputs will be placed into.
     */
    @OutputDirectory
    lateinit var buildDirectory: File

    /**
     * Generated API file that will be placed in the build directory. This can be copied manually
     * to [expectedApiFile] to confirm that API changes were intended.
     */
    @get:OutputFile
    val generatedApiFile: File
        get() = buildDirectory.resolve("api/icons.txt")

    /**
     * @return a list of all processed [Icon]s from [iconDirectory].
     */
    fun loadIcons(): List<Icon> =
        IconProcessor(iconDirectory, expectedApiFile, generatedApiFile).process()

    @get:OutputDirectory
    val generatedSrcMainDirectory: File
        get() = buildDirectory.resolve("src/main/kotlin")

    @get:OutputDirectory
    val generatedSrcAndroidTestDirectory: File
        get() = buildDirectory.resolve("src/androidTest/kotlin")

    @get:OutputDirectory
    val generatedResourceDirectory: File
        get() = buildDirectory.resolve("generatedIcons/res")

    /**
     * The action for this task
     */
    @TaskAction
    abstract fun run()

    companion object {
        /**
         * Registers the core [project]. The core project contains only the icons defined in
         * [androidx.ui.material.icons.generator.CoreIcons], and no tests.
         */
        @JvmStatic
        fun registerCoreIconProject(project: Project, libraryExtension: LibraryExtension) {
            libraryExtension.libraryVariants.all { variant ->
                CoreIconGenerationTask.register(project, variant)
            }
        }

        /**
         * Registers the extended [project]. The core project contains all icons except for the
         * icons defined in [androidx.ui.material.icons.generator.CoreIcons], as well as a bitmap comparison
         * test for every icon in both the core and extended project.
         */
        @JvmStatic
        fun registerExtendedIconProject(project: Project, libraryExtension: LibraryExtension) {
            libraryExtension.libraryVariants.all { variant ->
                ExtendedIconGenerationTask.register(project, variant)
            }

            libraryExtension.testVariants.all { variant ->
                IconTestingGenerationTask.register(project, variant)
            }
        }
    }
}

// Path to the generator project
private const val GeneratorProject = ":ui:ui-material:icons:generator"

/**
 * Registers a new [T] in [this], and sets [IconGenerationTask.buildDirectory] depending on
 * [variant].
 *
 * @return the created [T] of [IconGenerationTask]
 */
fun <T : IconGenerationTask> Project.createGenerationTask(
    taskName: String,
    variant: BaseVariant,
    taskClass: Class<T>
): T {
    return tasks.create("$taskName${variant.name.capitalize()}", taskClass) {
        it.buildDirectory = project.buildDir.resolve("generatedIcons/${variant.name}")
    }
}
