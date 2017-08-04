/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.plugin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection


/**
 * Defines methods that can be used in gradle on the "jetifier" object and triggers [JetifierTask].
 */
open class JetifierExtension(val project : Project) {

    companion object {
        const val TASK_NAME : String = "Jetifier"
    }

    /**
     * Handles dependency defined via string notation (like group:artifact:version).
     */
    fun process(dependencyNotation: String): FileCollection {
        return process(project.getDependencies().create(dependencyNotation))
    }

    /**
     * Handles dependency.
     */
    fun process(dependency: Dependency): FileCollection {
        val configuration = project.configurations.detachedConfiguration()
        configuration.dependencies.add(dependency)
        return process(configuration)
    }

    /**
     * Handles collections of files. Defined e.g. via files() or directly from a configuration.
     */
    fun process(files: FileCollection): FileCollection {
        var task = project.tasks.findByName(TASK_NAME) as JetifierTask?
        if (task == null) {
            task = project.tasks.create(TASK_NAME, JetifierTask::class.java)
        }
        task!!.addFilesToProcess(files)
        return task.outputs.files
    }

}