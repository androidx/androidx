/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XMessager
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.OriginatingElementsHolder
import java.io.OutputStream
import javax.lang.model.element.Element
import javax.tools.Diagnostic

internal class KspFiler(
    private val delegate: CodeGenerator,
    private val messager: XMessager,
) : XFiler {
    override fun write(javaFile: JavaFile, mode: XFiler.Mode) {
        val originatingFiles = javaFile.typeSpec.originatingElements
            .map(::originatingFileFor)

        createNewFile(
            originatingFiles = originatingFiles,
            packageName = javaFile.packageName,
            fileName = javaFile.typeSpec.name,
            extensionName = "java",
            aggregating = mode == XFiler.Mode.Aggregating
        ).use { outputStream ->
            outputStream.bufferedWriter(Charsets.UTF_8).use {
                javaFile.writeTo(it)
            }
        }
    }

    override fun write(fileSpec: FileSpec, mode: XFiler.Mode) {
        val originatingFiles = fileSpec.members
            .filterIsInstance<OriginatingElementsHolder>()
            .flatMap { it.originatingElements }
            .map(::originatingFileFor)

        createNewFile(
            originatingFiles = originatingFiles,
            packageName = fileSpec.packageName,
            fileName = fileSpec.name,
            extensionName = "kt",
            aggregating = mode == XFiler.Mode.Aggregating
        ).use { outputStream ->
            outputStream.bufferedWriter(Charsets.UTF_8).use {
                fileSpec.writeTo(it)
            }
        }
    }

    private fun originatingFileFor(element: Element): KSFile {
        check(element is KSFileAsOriginatingElement) {
            "Unexpected element type in originating elements. $element"
        }
        return element.ksFile
    }

    private fun createNewFile(
        originatingFiles: List<KSFile>,
        packageName: String,
        fileName: String,
        extensionName: String,
        aggregating: Boolean
    ): OutputStream {
        val dependencies = if (originatingFiles.isEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.WARNING,
                """
                    No dependencies are reported for $fileName which will prevent
                    incremental compilation. Please file a bug at:
                    https://issuetracker.google.com/issues/new?component=413107
                """.trimIndent()
            )
            Dependencies.ALL_FILES
        } else {
            Dependencies(
                aggregating = aggregating,
                sources = originatingFiles.distinct().toTypedArray()
            )
        }

        return delegate.createNewFile(
            dependencies = dependencies,
            packageName = packageName,
            fileName = fileName,
            extensionName = extensionName
        )
    }
}
