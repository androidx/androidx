/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.stableaidl.api

import java.io.File
import org.gradle.api.Incubating
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Extension that allows access to the StableAidl plugin's public APIs.
 */
interface StableAidlExtension {
    /**
     * An action representing the task that checks the current Stable AIDL API surface for
     * compatibility against the previously-frozen API surface.
     */
    @get:Incubating
    val checkAction: Action

    /**
     * An action representing the task that updates the frozen Stable AIDL API surface.
     */
    @get:Incubating
    val updateAction: Action

    /**
     * The task group to use for Stable AIDL tasks, or `null` to hide them.
     */
    @get:Incubating
    var taskGroup: String?

    /**
     * Adds static import directories to be passed to Stable AIDL.
     *
     * Static imports may be used in `import` statements, but are not exported to dependencies.
     */
    fun addStaticImportDirs(vararg dirs: File)
}

interface Action {
    /**
     * Runs the action before the specified [task].
     */
    fun <T : Task> before(task: TaskProvider<T>)
}
