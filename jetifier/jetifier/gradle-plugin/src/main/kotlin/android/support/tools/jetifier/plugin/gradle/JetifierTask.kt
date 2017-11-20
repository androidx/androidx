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

import android.support.tools.jetifier.core.Processor
import android.support.tools.jetifier.core.config.ConfigParser
import android.support.tools.jetifier.core.utils.Log
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File


/**
 * The jetifier task that is run by gradle.
 */
open class JetifierTask : DefaultTask() {

    companion object {
        const val OUTPUT_DIR_APPENDIX = "jetifier"
        const val GROUP_ID = "Pre-build"
        // TODO: Get back to this once the name of the library is decided.
        const val DESCRIPTION = "Rewrites input libraries to run with jetpack"
    }

    private var inputFiles = project.files()
    private val outputDir = File(project.buildDir, OUTPUT_DIR_APPENDIX)

    override fun getGroup() = GROUP_ID

    override fun getDescription() = DESCRIPTION

    fun addFilesToProcess(files: FileCollection) {
        inputFiles = project.files(inputFiles.files.plus(files))
    }

    @InputFiles
    fun getInputFiles(): FileCollection {
        return inputFiles
    }

    @OutputFiles
    fun getOutputFiles(): FileCollection {
        return project.files(inputFiles.map { File(outputDir, it.name) }.toList())
    }

    @TaskAction
    @Throws(Exception::class)
    fun run() {
        // Hook to the gradle logger
        Log.logConsumer = JetifierLoggerAdapter(logger)

        val config = ConfigParser.loadDefaultConfig()
                ?: throw RuntimeException("Failed to load the default config!")

        val processor = Processor(config)

        for(inputFile in inputFiles) {
            val inputPath = inputFile.toPath()
            val outputPath = File(outputDir, inputFile.name).toPath()
            processor.transform(inputPath, outputPath)
        }
    }

}