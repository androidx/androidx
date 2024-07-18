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

package androidx.stableaidl

import androidx.stableaidl.api.Action
import androidx.stableaidl.api.StableAidlExtension
import androidx.stableaidl.tasks.StableAidlCheckApi
import androidx.stableaidl.tasks.UpdateStableAidlApiTask
import com.android.build.api.variant.SourceDirectories
import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.File
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Internal implementation of [StableAidlExtension] that wraps task providers.
 */
open class StableAidlExtensionImpl : StableAidlExtension {
    override val checkAction: Action = object : Action {
        override fun <T : Task> before(task: TaskProvider<T>) {
            task.dependsOn(checkTaskProvider)
        }
    }

    override val updateAction: Action = object : Action {
        override fun <T : Task> before(task: TaskProvider<T>) {
            task.dependsOn(updateTaskProvider)
        }
    }

    override var taskGroup: String? = null
        set(taskGroup) {
            allTasks.forEach { (_, tasks) ->
                tasks.forEach { task ->
                    task.configure {
                        it.group = taskGroup
                    }
                }
            }
            field = taskGroup
        }

    override fun addStaticImportDirs(vararg dirs: File) {
        importSourceDirs.forEach { importSourceDir ->
            dirs.forEach { dir ->
                importSourceDir.addStaticSourceDirectory(dir.absolutePath)
            }
        }
    }

    internal lateinit var updateTaskProvider: TaskProvider<UpdateStableAidlApiTask>
    internal lateinit var checkTaskProvider: TaskProvider<StableAidlCheckApi>

    internal val importSourceDirs = mutableListOf<SourceDirectories.Flat>()
    internal val allTasks = mutableMapOf<String, Set<TaskProvider<*>>>()
}
