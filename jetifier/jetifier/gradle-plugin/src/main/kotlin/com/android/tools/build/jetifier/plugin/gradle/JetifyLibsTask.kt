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

import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.processor.FileMapping
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task that processes given file collections using Jetifier.
 *
 * This task also utilizes Gradle caching so it's run only when needed.
 *
 * Example usage in Gradle:
 * dependencies {
 *   compile jetifier.process('groupId:artifactId:1.0')
 * }
 */
open class JetifyLibsTask : DefaultTask() {

    companion object {
        const val TASK_NAME = "jetifyLibs"
        const val GROUP_ID = "Pre-build"
        const val DESCRIPTION = "Rewrites input libraries to run with jetpack"

        const val OUTPUT_DIR_APPENDIX = "jetifier"

        fun resolveTask(project: Project): JetifyLibsTask {
            val task = project.tasks.findByName(TASK_NAME) as? JetifyLibsTask
            if (task != null) {
                return task
            }
            return project.tasks.create(TASK_NAME, JetifyLibsTask::class.java)
        }
    }

    private val outputDir = File(project.buildDir, OUTPUT_DIR_APPENDIX)

    private val filesToProcess = mutableSetOf<FileMapping>()

    override fun getGroup() = GROUP_ID

    override fun getDescription() = DESCRIPTION

    /**
     * Adds individual files collection to be processed by Jetifier.
     *
     * See [JetifierExtension] for details on how to use this.
     */
    fun addFilesToProcess(files: FileCollection): FileCollection {
        return project.files(files.map { addFile(it).to }.toList())
    }

    private fun addFile(file: File): FileMapping {
        val mappingMaybe = filesToProcess.firstOrNull { it.from == file }
        if (mappingMaybe != null) {
            return mappingMaybe
        }

        val newFile = File(outputDir, file.hashCode().toString() + "_" + file.name)
        val mapping = FileMapping(file, newFile)
        filesToProcess.add(mapping)
        return mapping
    }

    /**
     * Used by Gradle to figure out whether this task should be re-run. If the result of this method
     * is different then the task is re-run.
     */
    @InputFiles
    fun getInputFiles(): FileCollection {
        return project.files(filesToProcess.map { it.from }.toList())
    }

    /**
     * Used by Gradle to figure out whether this task should be re-run and if other tasks that are
     * relying on files from this directory should be re-run. Actually not having this and only
     * having [InputFiles] annotation would disable the whole incremental mechanism for this task
     * and lead to constant re-runs.
     */
    @OutputDirectory
    fun getOutputDir(): File {
        return outputDir
    }

    @TaskAction
    @Throws(Exception::class)
    fun run() {
        val config = ConfigParser.loadConfigOrFail(TasksCommon.configFilePath)

        // Process the files using Jetifier
        TasksCommon.processFiles(config, filesToProcess, project.logger)
    }
}