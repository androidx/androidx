/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lint.gradle

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

/**
 * Common-use stubs of the Gradle API for use in tests. If a test requires additional definitions to
 * run, these should be added to.
 */
internal val STUBS =
    arrayOf(
        kotlin(
            """
                package org.gradle.api.tasks

                import org.gradle.api.DomainObjectCollection
                import org.gradle.api.Task

                class TaskContainer : DomainObjectCollection<Task>, TaskCollection<Task> {
                    fun create(name: String) = Unit
                    fun register(name: String) = Unit
                    fun getByName(name: String) = Unit
                    fun named(name: String) = Unit
                    fun whenTaskAdded(action: Action<in T>)
                }

                interface TaskCollection<T : Task> {
                    fun getAt(name: String) = Unit
                }
            """.trimIndent()
        ),
        kotlin(
            """
                package org.gradle.api

                import org.gradle.api.tasks.TaskContainer

                class Project {
                    val tasks: TaskContainer
                }

                interface DomainObjectCollection<T> {
                    fun all(action: Action<in T>)
                    fun configureEach(action: Action<in T>)
                    fun whenObjectAdded(action: Action<in T>)
                }

                interface Action<T>

                interface Task
            """.trimIndent()
        )
    )
