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

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

/**
 * Common-use stubs of the Gradle API for use in tests. If a test requires additional definitions to
 * run, these should be added to.
 */
internal val STUBS =
    arrayOf(
        kotlin(
            """
                package org.gradle.api.artifacts

                interface Configuration
            """
                .trimIndent()
        ),
        java(
            """
                package org.gradle.api.file;

                interface ConfigurableFileCollection {
                    ConfigurableFileCollection from(Object... paths);
                }
            """
                .trimIndent()
        ),
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

                interface TaskDependency
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.api.artifacts

                import org.gradle.api.NamedDomainObjectContainer
                import org.gradle.api.provider.Provider

                class ConfigurationContainer : NamedDomainObjectContainer<Configuration> {
                    override fun create(name: String): Configuration = TODO()
                    override fun maybeCreate(name: String): Configuration = TODO()
                    override fun register(name: String): Provider<Configuration> = TODO()
                }
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
                import org.gradle.api.artifacts.ConfigurationContainer
                import org.gradle.api.tasks.TaskContainer
                import java.lang.Class

                interface IsolatedProject {
                    fun getRootProject(): IsolatedProject
                    val tasks: TaskContainer
                }

                class Project {
                    val tasks: TaskContainer
                    val configurations: ConfigurationContainer
                    fun getIsolated(): IsolatedProject
                    fun getRootProject(): Project = Project()
                    fun findProperty(propertyName: String): Object? = null
                    fun evaluationDependsOn(path: String): Project = Project()
                    fun evaluationDependsOnChildren() { }
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

                interface NamedDomainObjectContainer<T> {
                    fun create(name: String): T
                    fun register(name: String): Provider<T>
                }

                interface Action<T>

                interface Task {
                    fun shouldRunAfter(vararg paths: Object): TaskDependency
                    fun setShouldRunAfter(value: Iterable<Any>)
                    fun mustRunAfter(vararg paths: Object): Task
                    fun setMustRunAfter(value: Iterable<Any>)
                }
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
        ),
        kotlin(
            "src/org/gradle/kotlin/dsl/DomainObjectCollectionExtensions.kt",
            """
                package org.gradle.kotlin.dsl
                import org.gradle.api.DomainObjectCollection

                inline fun <reified S : Any> DomainObjectCollection<in S>.withType(): DomainObjectCollection<S> = TODO()
                inline fun <reified S : Any> DomainObjectCollection<in S>.withType(noinline configuration: S.() -> Unit): DomainObjectCollection<S> = TODO()
            """
                .trimIndent(),
        ),
        kotlin(
            """
                package com.android.build.gradle.internal.tasks
                annotation class BuildAnalyzer
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.gradle.process.internal
                class ExecException : Exception()
            """
                .trimIndent()
        ),
        kotlin(
            """
                package org.jetbrains.kotlin.gradle.internal

                import java.io.File

                fun File.ensureParentDirsCreated() {
                    val parentFile = parentFile
                    if (!parentFile.exists()) {
                        check(parentFile.mkdirs()) {
                            "Cannot create parent directories"
                        }
                    }
                }
            """
                .trimIndent()
        ),
    )
