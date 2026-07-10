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

package androidx.binarycompatibilityvalidator

import java.io.File
import kotlin.text.trim
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi
import org.jetbrains.kotlin.library.abi.parser.KlibDumpParser

/**
 * Parses a merged Klib dump text that contains ABI information for multiple Kotlin Native targets.
 *
 * The merged dump format includes sections delineated by `// Targets: [...]` comments, indicating
 * which target(s) the subsequent lines apply to. This parser separates the content for each target
 * and uses the base [KlibDumpParser] to produce a [LibraryAbi] for each.
 *
 * This class requires `@OptIn(ExperimentalLibraryAbiReader::class)` due to its dependency on
 * [KlibDumpParser] and [LibraryAbi].
 *
 * @property dumpText The full text content of the merged Klib dump.
 * @property filePath Optional path to the file from which the dumpText originated, used for error
 *   reporting.
 */
@OptIn(ExperimentalLibraryAbiReader::class)
class MergedKlibDumpParser(private val dumpText: String, private val filePath: String? = null) {
    constructor(file: File) : this(file.readText(), file.path)

    fun parse(): Map<String, LibraryAbi> {
        val lines = dumpText.lines()
        val aliases = mutableMapOf<String, Set<String>>()
        val targetContent = mutableMapOf<String, MutableList<String>>()
        val globalLines = mutableListOf<String>()
        var isGlobalHeaderParsed = false
        var currentTargets = setOf<String>()

        for (line in lines) {
            if (line.trim().startsWith("// Targets: [")) {
                val targets = parseTargets(line)
                val targetsAccountingForAliases =
                    targets.flatMap { aliases[it] ?: setOf(it) }.toSet()
                currentTargets = targetsAccountingForAliases
                if (!isGlobalHeaderParsed) {
                    isGlobalHeaderParsed = true
                    for (target in targets) {
                        targetContent[target] = globalLines.toMutableList()
                    }
                }
                // Skip the "// Targets: " lines in output
                continue
            } else if (line.trim().startsWith("// Alias:")) {
                val alias = line.substringAfter("Alias:").substringBefore("=>").trim()
                val aliasTargets = parseTargets(line)
                aliases[alias] = aliasTargets

                // Skip the "// Alias: " lines in output
                continue
            }

            if (!isGlobalHeaderParsed) {
                // We are at the top of the file before the global targets are declared
                globalLines.add(line)
            } else {
                // Line only applies to targets currently in scope
                for (currentTarget in currentTargets) {
                    val currentTargetContent =
                        targetContent[currentTarget]
                            ?: throw IllegalStateException(
                                "Current target '${currentTarget}' is not present in target set"
                            )
                    currentTargetContent.add(line)
                }
            }
        }
        return targetContent.mapValues { (_, list) ->
            val cleaned = cleanInputFile(list.joinToString("\n"))
            KlibDumpParser(cleaned, filePath).parse()
        }
    }

    private fun parseTargets(line: String): Set<String> {
        val targetsStr = line.substringAfter("[").substringBeforeLast("]")
        return targetsStr.split(",").map { extractTargetName(it) }.toSet()
    }

    // For example, take 'linuxX64' from 'linuxX64.linuxx64Stubs'
    private fun extractTargetName(target: String) = target.trim().split(".").first()
}

fun cleanInputFile(input: String): String =
    input.split("\n").joinToString("\n") { line ->
        // b/493871040
        if (line.trim().startsWith("enum entry")) {
            val indexOfExtraSpace = line.indexOf("//") - 1
            line.removeRange(indexOfExtraSpace, indexOfExtraSpace + 1)
        } else {
            line
        }
    }
