/*
 * Copyright 2025 The Android Open Source Project
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

import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi

/**
 * Because FunctionN interfaces are provided by the compiler and not actually part of the stdlib, we
 * need to provide them ourselves so that we can properly check classes that extend them.
 *
 * To avoid accessing internal impl classes, we are writing a klib dump file with these interfaces
 * and then parsing it back into a [LibraryAbi]
 */
@OptIn(ExperimentalLibraryAbiReader::class)
object FictionalFunctionAbiBuilder {

    fun build(): LibraryAbi {
        val dumpText = createFictionalFunctionAbiDump()
        return KlibDumpParser(dumpText).parse().values.single()
    }

    // build from dump file to avoid accessing internal symbols
    internal fun createFictionalFunctionAbiDump(): String {
        val builder = StringBuilder()
        builder.append(metadata)
        builder.append("\n")
        builder.append("abstract interface <#A: out kotlin/Any?> kotlin/Function\n")
        repeat(NUM_FUNCTIONS_TO_CREATE) { num ->
            val outTag = tags[num]
            val typeParams = (0..num).joinToString(", ") { idx -> "#${tags[idx]}: in kotlin/Any?" }
            val valueParams = (0..num).joinToString(", ") { idx -> "#${tags[idx]}" }

            builder.append("abstract interface <")
            builder.append(typeParams)
            builder.append(", ")
            builder.append("#$outTag: out kotlin/Any?>")
            builder.append("kotlin/Function${num + 1} : kotlin/Function<#$outTag> {")
            builder.append("\n")
            builder.append("abstract fun invoke($valueParams): #$outTag".prependIndent())
            builder.append("\n")
            builder.append("}")
            builder.append("\n")
        }
        return builder.toString()
    }

    private const val NUM_FUNCTIONS_TO_CREATE = 22
    // We need tags for each type parameter in addition to the initial return value
    // so one per number of params to the function. We are reserving A for the
    // return value
    private val tags = ('B'..'Z').toList().subList(0, NUM_FUNCTIONS_TO_CREATE)
    private val metadata =
        """
        // KLib ABI Dump
        // Targets: [stub]
        // Rendering settings:
        // - Signature version: 2
        // - Show manifest properties: true
        // - Show declarations: true
        // Library unique name: <stub:stub>
    """
            .trimIndent()
}
