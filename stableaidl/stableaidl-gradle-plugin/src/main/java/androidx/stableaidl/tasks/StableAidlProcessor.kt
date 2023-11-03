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

package androidx.stableaidl.tasks

import androidx.stableaidl.internal.compiling.DependencyFileProcessor
import androidx.stableaidl.internal.process.GradleProcessExecutor
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessInfoBuilder
import com.android.ide.common.process.ProcessOutputHandler
import com.android.repository.io.FileOpUtils
import com.android.utils.FileUtils
import com.google.common.io.Files
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

@Throws(IOException::class)
fun callStableAidlProcessor(
    aidlExecutable: String,
    @Suppress("UNUSED_PARAMETER")
    frameworkLocation: String, // TODO: Unused until the framework has been fully annotated.
    importFolders: Iterable<File>,
    extraArgs: List<String?>,
    processExecutor: GradleProcessExecutor,
    processOutputHandler: ProcessOutputHandler,
    sourceOutputDir: File? = null,
    packagedOutputDir: File? = null,
    dependencyFileProcessor: DependencyFileProcessor? = null,
    startDir: Path? = null,
    inputFilePath: Path? = null
) {
    val builder = ProcessInfoBuilder()
    builder.setExecutable(aidlExecutable)

    // Specify the base output directory for generated language (e.g. Java) source files.
    if (sourceOutputDir != null) {
        builder.addArgs("-o" + sourceOutputDir.absolutePath)
    }

    // TODO: Remove when the framework has been fully annotated.
    // Specify the framework as a pre-processed file for use in import statements.
    // builder.addArgs("-p$frameworkLocation")

    // Specify all library AIDL directories for use in import statements.
    for (f in importFolders) {
        builder.addArgs("-I" + f.absolutePath)
    }

    // Specify the dependency file output as a temporary file. This will contains a list of
    // generated file paths, some of which will be parcelable headers need to be copied to
    // packagedOutputDir.
    val depFile = File.createTempFile("aidl", ".d")
    builder.addArgs("-d" + depFile.absolutePath)

    // Specify additional arguments, ex. those used for Stable AIDL operations.
    builder.addArgs(extraArgs)

    // Specify a single input file.
    if (inputFilePath != null) {
        builder.addArgs(inputFilePath.toAbsolutePath().toString())
    }

    val result = processExecutor.execute(builder.createProcess(), processOutputHandler)
    try {
        result.rethrowFailure().assertNormalExitValue()
    } catch (pe: ProcessException) {
        throw IOException(pe)
    }

    val relativeInputFile = if (startDir != null && inputFilePath != null) {
        FileUtils.toSystemIndependentPath(
            FileOpUtils.makeRelative(startDir.toFile(), inputFilePath.toFile())
        )
    } else {
        null
    }

    // Process the dependency file by deleting empty generated source files and copying parcelable
    // headers to secondary output for AAR packaging.
    val data = dependencyFileProcessor?.processFile(depFile)
    if (data != null) {
        // As of build tools 29.0.2, Aidl no longer produces an empty list of output files
        // so we need to check each file in it for content and delete the empty java files
        var isParcelable = true
        val outputFiles = data.outputFiles
        if (outputFiles.isNotEmpty()) {
            for (path in outputFiles) {
                val outputFileContent = Files.readLines(File(path), StandardCharsets.UTF_8)
                val emptyFileLine = "// This file is intentionally left blank as placeholder for " +
                    "parcel declaration."
                if (outputFileContent.size <= 2 && outputFileContent[0].equals(emptyFileLine)) {
                    FileUtils.delete(File(path))
                } else {
                    isParcelable = false
                }
            }
        }
        if (inputFilePath != null && relativeInputFile != null) {
            if (packagedOutputDir != null && isParcelable) {
                // looks like a parcelable or is listed for packaging
                // Store it in the secondary output of the DependencyData object.
                val destFile = File(packagedOutputDir, relativeInputFile)
                destFile.parentFile.mkdirs()
                Files.copy(inputFilePath.toFile(), destFile)
                data.addSecondaryOutputFile(destFile.path)
            }
        }
    }
    FileUtils.delete(depFile)
}
