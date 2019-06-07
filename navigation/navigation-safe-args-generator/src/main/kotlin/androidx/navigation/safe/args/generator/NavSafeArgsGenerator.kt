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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.java.JavaNavWriter
import androidx.navigation.safe.args.generator.kotlin.KotlinNavWriter
import androidx.navigation.safe.args.generator.models.Destination
import java.io.File

fun SafeArgsGenerator(
    rFilePackage: String,
    applicationId: String,
    navigationXml: File,
    outputDir: File,
    useAndroidX: Boolean = true,
    generateKotlin: Boolean
) = NavSafeArgsGenerator(
    rFilePackage,
    applicationId,
    navigationXml,
    outputDir,
    if (generateKotlin) {
        KotlinNavWriter(useAndroidX)
    } else {
        JavaNavWriter(useAndroidX)
    }
)

class NavSafeArgsGenerator<T : CodeFile> internal constructor(
    private val rFilePackage: String,
    private val applicationId: String,
    private val navigationXml: File,
    private val outputDir: File,
    private val writer: NavWriter<T>
) {
    fun generate(): GeneratorOutput {
        val context = Context()
        val rawDestination = NavParser.parseNavigationFile(
            navigationXml,
            rFilePackage,
            applicationId,
            context
        )
        val resolvedDestination = resolveArguments(rawDestination)
        val codeFiles = mutableSetOf<CodeFile>()
        fun writeCodeFiles(
            destination: Destination,
            parentDirectionsFileList: List<T>
        ) {
            val newParentDirectionFile =
                if (destination.actions.isNotEmpty() || parentDirectionsFileList.isNotEmpty()) {
                    writer.generateDirectionsCodeFile(destination, parentDirectionsFileList)
                } else {
                    null
                }?.also { codeFiles.add(it) }
            if (destination.args.isNotEmpty()) {
                codeFiles.add(writer.generateArgsCodeFile(destination))
            }
            destination.nested.forEach { nestedDestination ->
                writeCodeFiles(
                    destination = nestedDestination,
                    parentDirectionsFileList = newParentDirectionFile?.let {
                        listOf(it) + parentDirectionsFileList } ?: parentDirectionsFileList)
            }
        }
        writeCodeFiles(resolvedDestination, emptyList())
        codeFiles.forEach { it.writeTo(outputDir) }
        return GeneratorOutput(codeFiles.toList(), context.logger.allMessages())
    }
}
