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

import androidx.ui.material.icons.generator.IconTestingManifestGenerator
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.tasks.OutputDirectory
import java.io.File

/**
 * Task responsible for flattening all the input icons into a drawable folder that will be used
 * in comparison tests.
 */
open class IconTestingDrawableGenerationTask : IconGenerationTask() {
    /**
     * Directory to generate the flattened drawables used for testing to.
     */
    @get:OutputDirectory
    val drawableDirectory: File
        get() = generatedResourceDirectory.resolve("drawable")

    override fun run() {
        loadIcons().forEach { icon ->
            drawableDirectory.resolve("${icon.xmlFileName}.xml").apply {
                createNewFile()
                writeText(icon.fileContent)
            }
        }
    }

    companion object {
        /**
         * Registers [IconTestingDrawableGenerationTask] in [project] for [variant].
         */
        fun register(project: Project, variant: BaseVariant) {
            val task = project.tasks.create(
                "copyIcons${variant.name.capitalize()}",
                IconTestingDrawableGenerationTask::class.java
            )
            variant.registerGeneratedResFolders(
                project.files(getGeneratedResourceDirectory(project)).builtBy(task)
            )
        }
    }
}

/**
 * Task responsible for generating a list of all icons mapped to the drawable ID used in
 * testing, so we can bitmap compare the programmatic icon with the original source drawable.
 */
open class IconTestingManifestGenerationTask : IconGenerationTask() {
    override fun run() =
        IconTestingManifestGenerator(loadIcons()).generateTo(generatedSrcAndroidTestDirectory)

    companion object {
        /**
         * Registers [IconTestingManifestGenerationTask] in [project] for [variant].
         */
        fun register(project: Project, variant: BaseVariant) {
            val task = project.tasks.create(
                "generateAllIcons${variant.name.capitalize()}",
                IconTestingManifestGenerationTask::class.java
            )
            variant.registerJavaGeneratingTask(task, getGeneratedSrcAndroidTestDirectory(project))
        }
    }
}
