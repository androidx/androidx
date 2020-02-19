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

import androidx.ui.material.icons.generator.CoreIcons
import androidx.ui.material.icons.generator.IconWriter
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project

/**
 * Task responsible for converting core icons from xml to a programmatic representation.
 */
open class CoreIconGenerationTask : IconGenerationTask() {
    override fun run() =
        IconWriter(loadIcons()).generateTo(generatedSrcMainDirectory) { it in CoreIcons }

    companion object {
        /**
         * Registers [CoreIconGenerationTask] in [project] for [variant].
         */
        fun register(project: Project, variant: BaseVariant) {
            val task = project.createGenerationTask(
                "generateCoreIcons",
                variant,
                CoreIconGenerationTask::class.java
            )
            variant.registerIconGenerationTask(project, task)
        }
    }
}

/**
 * Task responsible for converting extended icons from xml to a programmatic representation.
 */
open class ExtendedIconGenerationTask : IconGenerationTask() {
    override fun run() =
        IconWriter(loadIcons()).generateTo(generatedSrcMainDirectory) { it !in CoreIcons }

    companion object {
        /**
         * Registers [ExtendedIconGenerationTask] in [project] for [variant].
         */
        fun register(project: Project, variant: BaseVariant) {
            val task = project.createGenerationTask(
                "generateExtendedIcons",
                variant,
                ExtendedIconGenerationTask::class.java
            )
            variant.registerIconGenerationTask(project, task)
        }
    }
}

/**
 * Helper to register [task] as the java source generating task for [project].
 */
private fun BaseVariant.registerIconGenerationTask(
    project: Project,
    task: IconGenerationTask
) {
    registerJavaGeneratingTask(task, task.generatedSrcMainDirectory)
    // TODO: b/144249620 - fixed in AGP 4.0.0 alpha 4 +
    javaCompileProvider.configure { it.enabled = false }
    project.tasks.named("runErrorProne").configure { it.enabled = false }
}
