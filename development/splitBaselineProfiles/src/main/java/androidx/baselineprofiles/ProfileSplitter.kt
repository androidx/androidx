/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.baselineprofiles

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

/**
 * A Utility that can split a monolithic baseline profiles to its constituent modules.
 *
 * This command helps split the large baseline profile produced via the `BaselineProfileRule`,
 * to its constituent library modules by scanning the `checkoutPath` for source files, and matching
 * the rules in the baseline profiles, to the packages discovered.
 */
class SplitBaselineProfiles : CliktCommand() {

    private val profilePath by option(
        "-p",
        "--profilePath",
        help = "The baseline profile path"
    ).required()

    private val checkoutPath by option(
        "-c",
        "--checkoutPath",
        help = "The checkout path"
    ).required()

    private val outputName by option(
        "-o",
        "--outputName",
        help = "The name of the output file"
    ).default("output-prof.txt")

    override fun run() {
        val profileFile = File(profilePath).requiredExists()
        val checkoutFile = File(checkoutPath).requiredExists()
        val outputFile = File(profileFile.parentFile, outputName)
        val index = buildIndex(checkoutFile)
        val contents = profileFile.readLines()
        val groupedProfiles = contents.groupBy { line ->
            val matchResult = CLASS_PATTERN.find(line)
            val groupBy = if (matchResult != null) {
                // The simplified class name
                // Looks something like androidx/Foo/Bar
                val name = matchResult.groupValues[2]
                index[name] ?: name.split("/").dropLast(1).joinToString(separator = "/")
            } else {
                null
            }
            groupBy ?: "Unknown"
        }
        println("Writing to ${outputFile.absolutePath}")
        writeOutputProfile(groupedProfiles, outputFile)
        println("All Done.")
    }

    private fun buildIndex(checkoutFile: File): Map<String, String> {
        val sourceFiles = mutableListOf<File>()
        findSourceFiles(listOf(checkoutFile), sourceFiles)
        println("Found ${sourceFiles.size} files.")
        return buildIndexFromFiles(sourceFiles)
    }

    private fun buildIndexFromFiles(sourceFiles: List<File>): Map<String, String> {
        val grouped = sourceFiles.groupBy { file ->
            var currentFile = file
            var group: String? = null
            while (group == null) {
                // Group by path to src/main because that is where the baseline-prof.txt
                // Needs to end up in.
                if (currentFile.isDirectory && currentFile.name == "main") {
                    group = currentFile.absolutePath
                } else {
                    currentFile = currentFile.parentFile
                }
            }
            group
        }
        // Invert
        val inverted = mutableMapOf<String, String>()
        grouped.forEach { (pathToMain, entries) ->
            entries.forEach { file ->
                val matchResult = PATH_PATTERN.find(file.absolutePath)
                if (matchResult != null) {
                    val packagePath = matchResult.groupValues[3]
                    inverted[packagePath] = pathToMain
                }
            }
        }
        return inverted
    }

    private fun findSourceFiles(paths: List<File>, sourceFiles: MutableList<File>) {
        if (paths.isEmpty()) {
            return
        }
        val nextPaths = mutableListOf<File>()
        for (path in paths) {
            val files = path.listFiles()
            files?.forEach { file ->
                if (file.isFile) {
                    val name = file.name
                    // Only look at source files with a "src/main/" prefix
                    // They are gradle modules we might be interested in.
                    if ((name.endsWith("java") || name.endsWith("kt")) &&
                        file.absolutePath.contains("src/main")
                    ) {
                        sourceFiles += file
                    }
                } else if (file.isDirectory) {
                    nextPaths += file
                }
            }
        }
        return findSourceFiles(nextPaths, sourceFiles)
    }

    private fun writeOutputProfile(groupedProfiles: Map<String, List<String>>, outputFile: File) {
        val writer = outputFile.printWriter()
        writer.use {
            groupedProfiles.forEach { entry ->
                val groupName = entry.key
                val profileEntries = entry.value
                writer.write("# $groupName\n\n")
                profileEntries.forEach { line ->
                    writer.write("$line\n")
                }
                writer.write("\n")
            }
        }
    }

    companion object {
        // A class file in the ART HRF format.
        // We don't care about static inner classes, or methods to locate them in the source tree.
        val CLASS_PATTERN = Regex("""(H|S|P)?L(\w+/[^$;]*)""")

        // Identifies a Java | Kotlin Source folder from a checkout.
        val PATH_PATTERN = Regex("""(.*)/src/main/(java|kotlin)/(.*)(\.(java|kt)?)""")

        internal fun File.requiredExists(): File {
            require(exists()) {
                "$absolutePath does not exist."
            }
            return this
        }
    }
}

fun main(args: Array<String>) {
    SplitBaselineProfiles().main(args)
}
