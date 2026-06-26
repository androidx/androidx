/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.benchmark.perfetto

import androidx.benchmark.InMemoryTracing
import androidx.benchmark.VirtualFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Write the output trace file in [outputPath].
 *
 * If there is only one input provided in [absoluteTracePaths] (and there are no in-memory tracing
 * events) we end up building a single output file. Otherwise, we build a ZIP container that
 * contains all the provided inputs + in memory tracing data so Perfetto can merge them
 * automatically.
 *
 * Note: [outputPath] is a path accessible or owned by the test process writing the trace outputs.
 * All other paths are not necessarily owned, which means that we have to copy the files using the
 * `Shell` user to an owned location before we can manipulate the contents of the file.
 */
internal fun writeOutput(
    absoluteTracePaths: List<String>,
    outputPath: String,
    inMemoryLabel: String? = null,
) {
    val outputFile = File(outputPath)
    val outputDirectoryPath = outputFile.parent
    check(value = outputDirectoryPath != null) {
        "Must provide an output directory. Provided path is $outputPath. "
    }
    check(value = absoluteTracePaths.isNotEmpty()) { "No inputs have been provided." }
    val outputDirectory = requireOutputDirectory(outputDirectoryPath)
    // Input traces (potentially owned)
    val inputs = absoluteTracePaths.map { input -> VirtualFile.fromPath(input) }
    // Create the ZIP container with all the provided inputs.
    val zip = ZipOutputStream(outputFile.outputStream())
    zip.use {
        inputs.forEach { file -> zip.entry(file) }
        // In Memory traces
        if (inMemoryLabel != null) {
            zip.entry(virtualFile = inMemoryTrace(label = inMemoryLabel, parent = outputDirectory))
        }
    }
}

private fun ZipOutputStream.entry(virtualFile: VirtualFile) {
    val file = File(virtualFile.absolutePath)
    val name = file.nameWithoutExtension.capitalized()
    putNextEntry(ZipEntry("$name.pb"))
    virtualFile.copyTo(otherOutputStream = this)
    closeEntry()
}

private fun inMemoryTrace(label: String, parent: File): VirtualFile {
    val output = File(parent, label)
    output.outputStream().use { InMemoryTracing.commitToTrace(label).encode(stream = it) }
    return VirtualFile.fromPath(output.absolutePath)
}

private fun requireOutputDirectory(directoryPath: String): File {
    val directory = File(directoryPath)
    if (!directory.exists()) {
        check(value = directory.mkdirs()) {
            "Result output directory $directory not created successfully."
        }
    }
    return directory
}

// Names must start with a capital letter.
// For additional context: b/421473521
private fun String.capitalized(): String {
    return replaceFirstChar { char -> if (char.isLowerCase()) char.titlecaseChar() else char }
}
