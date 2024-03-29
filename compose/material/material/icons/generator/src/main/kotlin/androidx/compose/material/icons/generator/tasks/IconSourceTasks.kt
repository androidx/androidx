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

package androidx.compose.material.icons.generator.tasks

import androidx.compose.material.icons.generator.CoreIcons
import androidx.compose.material.icons.generator.IconWriter
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Task responsible for converting core icons from xml to a programmatic representation.
 */
@CacheableTask
open class CoreIconGenerationTask : IconGenerationTask() {
    override fun run() =
        IconWriter(loadIcons()).generateTo(generatedSrcMainDirectory) { it in CoreIcons }

    companion object {
        /**
         * Registers [CoreIconGenerationTask] in [project].
         */
        @Suppress("DEPRECATION") // BaseVariant
        fun register(project: Project, variant: com.android.build.gradle.api.BaseVariant? = null) {
            val (task, buildDirectory) = project.registerGenerationTask(
                "generateCoreIcons",
                CoreIconGenerationTask::class.java,
                variant
            )
            registerIconGenerationTask(project, task, buildDirectory)
        }
    }
}

/**
 * Task responsible for converting extended icons from xml to a programmatic representation.
 */
@CacheableTask
open class ExtendedIconGenerationTask : IconGenerationTask() {
    override fun run() =
        IconWriter(loadIcons()).generateTo(generatedSrcMainDirectory) { it !in CoreIcons }

    companion object {
        /**
         * Registers [ExtendedIconGenerationTask] in [project]. (for use with mpp)
         */
        @Suppress("DEPRECATION") // BaseVariant
        fun register(project: Project, variant: com.android.build.gradle.api.BaseVariant? = null) {
            val (task, buildDirectory) = project.registerGenerationTask(
                "generateExtendedIcons",
                ExtendedIconGenerationTask::class.java,
                variant
            )
            registerIconGenerationTask(project, task, buildDirectory)
        }
    }
}

/**
 * Helper to register [task] that outputs to [buildDirectory] as the Kotlin source generating
 * task for [project].
 */
private fun registerIconGenerationTask(
    project: Project,
    task: TaskProvider<*>,
    buildDirectory: File
) {
    val sourceSet = project.getMultiplatformSourceSet(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
    val generatedSrcMainDirectory = buildDirectory.resolve(IconGenerationTask.GeneratedSrcMain)
    sourceSet.kotlin.srcDir(project.files(generatedSrcMainDirectory).builtBy(task))
    // add it to the multiplatform sources as well.
    project.tasks.named("multiplatformSourceJar", Jar::class.java).configure {
        it.from(task.map { generatedSrcMainDirectory })
    }
}
