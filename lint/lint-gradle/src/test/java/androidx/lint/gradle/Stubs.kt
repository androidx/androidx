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

                import groovy.lang.Closure
                import java.lang.Class
                import org.gradle.api.DomainObjectCollection
                import org.gradle.api.NamedDomainObjectCollection
                import org.gradle.api.provider.Provider
                import org.gradle.api.Task
                import org.gradle.api.tasks.TaskProvider

                class TaskContainer : DomainObjectCollection<Task>, TaskCollection<Task>, NamedDomainObjectCollection<Task>, NamedDomainObjectSet<Task> {
                    fun create(name: String) = Unit
                    fun register(name: String): TaskProvider<Task> = TODO()
                    fun getByName(name: String) = Unit
                    fun named(name: String) = Unit
                    fun whenTaskAdded(action: Action<in T>)
                    fun getByPath(path: String) = Unit
                    fun findByPath(path: String) = Unit
                    fun replace(name: String) = Unit
                    fun remove(task: Task) = Unit
                }

                interface TaskCollection<T : Task> {
                    fun getAt(name: String) = Unit
                    fun matching(closure: Closure) = Unit
                }
                interface TaskProvider<T : Task> : Provider<T>

                @Retention
                @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
                annotation class Internal(
                    val value: String = ""
                )

            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.api.provider
                interface Provider<T> {
                    fun get() : T
                }
                interface Property<T> : Provider<T> {
                    fun set(value: T)
                }
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.api

                import groovy.lang.Closure
                import org.gradle.api.tasks.TaskContainer
                import java.lang.Class

                class Project {
                    val tasks: TaskContainer
                    fun findProperty(propertyName: String): Object? = null
                }

                interface NamedDomainObjectCollection<T> : Collection<T>, DomainObjectCollection<T>, Iterable<T> {
                    fun findByName(name: String) = Unit
                    fun findAll(closure: Closure) = Unit
                }

                interface DomainObjectCollection<T> {
                    fun all(action: Action<in T>)
                    fun configureEach(action: Action<in T>)
                    fun whenObjectAdded(action: Action<in T>)
                    fun withType(type: Class<S>)
                }

                interface Action<T>

                interface Task
            """
                .trimIndent()
        ),
        kotlin(
            """
                package groovy.lang

                class Closure
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.api.component

                interface SoftwareComponent
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.api.internal.component

                import org.gradle.api.component.SoftwareComponent

                interface SoftwareComponentInternal : SoftwareComponent {
                    fun getUsages() : Set<out UsageContext>
                }

                interface UsageContext
            """
                .trimIndent()
        ),
        kotlin(
            """
                package com.android.build.gradle.internal.lint
                abstract class VariantInputs
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.testkit.runner

                class GradleRunner {
                    companion object {
                         fun create(): GradleRunner = GradleRunner()
                    }
                    fun withProjectDir(projectDir: java.io.File): GradleRunner = this
                    fun withPluginClasspath(): GradleRunner = this
                    fun build(): org.gradle.testkit.runner.BuildResult = TODO()
                }
            """
                .trimIndent()
        )
    )
