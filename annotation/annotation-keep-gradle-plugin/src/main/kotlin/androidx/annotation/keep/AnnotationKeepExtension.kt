/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.annotation.keep

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

abstract class AnnotationKeepExtension @Inject constructor(private val project: Project) {
    /**
     * Registers a [TransformOutputTask] that transforms the given [input] JAR to inject keep rules.
     *
     * @param taskName name of the [TransformOutputTask] to register
     * @param input [Provider] of the JAR [RegularFile] to transform
     * @param fileName [Provider] of the transformed output JAR file name
     * @return [TaskProvider] for the registered [TransformOutputTask]
     */
    fun registerJavaArchiveTransform(
        taskName: String,
        input: Provider<RegularFile>,
        fileName: Provider<String>,
    ): TaskProvider<TransformOutputTask> {
        val taskProvider =
            project.tasks.register(taskName, JarModificationTask::class.java) { task ->
                task.inputJar.set(input)
                task.outputJar.set(
                    project.layout.buildDirectory.file(
                        fileName.map { "intermediates/annotation-keep/$taskName/$it" }
                    )
                )
            }

        @Suppress("UNCHECKED_CAST")
        return taskProvider as TaskProvider<TransformOutputTask>
    }

    /**
     * Registers a [TransformOutputTask] that transforms the given [input] JAR to inject KeepAnno
     * rules.
     *
     * Overload for accepting a [Provider] of [RegularFileProperty].
     */
    @JvmName("registerJavaArchiveTransformProperty")
    fun registerJavaArchiveTransform(
        taskName: String,
        input: Provider<RegularFileProperty>,
        fileName: Provider<String>,
    ): TaskProvider<TransformOutputTask> =
        registerJavaArchiveTransform(taskName, input.flatMap { it }, fileName)
}
