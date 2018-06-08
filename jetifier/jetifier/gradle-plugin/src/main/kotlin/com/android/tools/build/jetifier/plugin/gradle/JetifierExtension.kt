/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.plugin.gradle

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import java.nio.file.Paths

/**
 * Defines methods that can be used in gradle on the "jetifier" object and triggers [JetifyLibsTask]
 * or [JetifyGlobalTask] based on its usage.
 */
open class JetifierExtension(val project: Project) {

    /**
     * Adds dependency defined via string notation to be processed by jetifyLibs task.
     *
     * Example usage in Gradle:
     * dependencies {
     *   compile jetifier.process('groupId:artifactId:1.0')
     * }
     */
    fun process(dependencyNotation: String): FileCollection {
        return process(project.dependencies.create(dependencyNotation))
    }

    /**
     * Adds dependency defined via string notation to be processed by jetifyLibs task. This version
     * supports Gradle's configuration closure that is passed to the Gradle's DependencyHandler.
     *
     * Example usage in Gradle:
     * dependencies {
     *   compile jetifier.process('groupId:artifactId:1.0') {
     *     exclude group: 'groupId'
     *
     *     transitive = false
     *   }
     * }
     */
    fun process(dependencyNotation: String, closure: Closure<Any>): FileCollection {
        return process(project.dependencies.create(dependencyNotation, closure))
    }

    /**
     * Adds dependency to be processed by jetifyLibs task.
     */
    fun process(dependency: Dependency): FileCollection {
        val configuration = project.configurations.detachedConfiguration()
        configuration.dependencies.add(dependency)
        return process(configuration)
    }

    /**
     * Adds dependencies defined via file collection to be processed by jetifyLibs task.
     *
     * Example usage in Gradle for a single file:
     * dependencies {
     *   compile jetifier.process(files('../myFile1.jar'))
     *   compile jetifier.process(files('../myFile2.jar'))
     * }
     *
     * Example usage in Gradle for a configuration:
     * configurations.create('depToRefactor')
     *
     * dependencies {
     *    depToRefactor 'test:myDependency:1.0'
     *    depToRefactor 'test:myDependency2:1.0'
     * }
     *
     * dependencies {
     *   compile jetifier.process(configurations.depToRefactor)
     * }
     */
    fun process(files: FileCollection): FileCollection {
        return JetifyLibsTask.resolveTask(project).addFilesToProcess(files)
    }

    /**
     * Adds a whole configuration to be processed by jetifyGlobal task. This is the recommended way
     * if processing a set of dependencies where it is unknown which exactly need to be rewritten.
     *
     * This will create a new detached configuration and resolve all the dependencies = obtaining
     * all the files. Jetifier is then run with all the files and only the files that were rewritten
     * are added to the given configuration and the original dependencies that didn't have to be
     * changed are kept.
     *
     * Advantage is that all the dependencies that didn't have to be changed are kept intact so
     * their artifactsIds and groupIds are kept (instead of replacing them with files) which allows
     * other steps in the build process to use the artifacts information to generate pom files
     * and other stuff.
     *
     * This will NOT resolve the given configuration as the dependencies are resolved in a detached
     * configuration. If you give it a configuration that was already resolved the process will
     * end up with exception saying that resolved configuration cannot be changed. This is expected
     * as Jetifier cannot add new files to an already resolved configuration.
     *
     *
     * Example usage in Gradle:
     * jetifier.addConfigurationToProcess(configurations.implementation)
     * afterEvaluate {
     *   tasks.preBuild.dependsOn tasks.jetifyGlobal
     * }
     *
     *
     */
    fun addConfigurationToProcess(config: Configuration) {
        JetifyGlobalTask.resolveTask(project).addConfigurationToProcess(config)
    }

    /**
     * Sets a custom configuration file to be used by Jetifier.
     */
    fun setConfigFile(configFilePath: String) {
        TasksCommon.configFilePath = Paths.get(configFilePath)
    }
}