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

import androidx.navigation.safe.args.generator.ext.toClassName
import androidx.navigation.safe.args.generator.models.Destination
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import java.io.File

fun generateSafeArgs(
    rFilePackage: String,
    applicationId: String,
    navigationXml: File,
    outputDir: File,
    useAndroidX: Boolean = false
): GeneratorOutput {
    val context = Context()
    val rawDestination = NavParser.parseNavigationFile(navigationXml, rFilePackage, applicationId,
            context)
    val resolvedDestination = resolveArguments(rawDestination)
    val javaFiles = mutableSetOf<JavaFile>()
    fun writeJavaFiles(
        destination: Destination,
        parentDirectionName: ClassName?
    ) {
        val directionsJavaFile = if (destination.actions.isNotEmpty() ||
                parentDirectionName != null) {
            generateDirectionsJavaFile(destination, parentDirectionName, useAndroidX)
        } else {
            null
        }
        val argsJavaFile = if (destination.args.isNotEmpty()) {
            generateArgsJavaFile(destination, useAndroidX)
        } else {
            null
        }
        directionsJavaFile?.let { javaFiles.add(it) }
        argsJavaFile?.let { javaFiles.add(it) }
        destination.nested.forEach { it ->
            writeJavaFiles(it, directionsJavaFile?.toClassName())
        }
    }
    writeJavaFiles(resolvedDestination, null)
    javaFiles.forEach { javaFile -> javaFile.writeTo(outputDir) }
    return GeneratorOutput(javaFiles.toList(), context.logger.allMessages())
}
