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
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
        val originatingElements = javaFile.typeSpec.originatingElements
            .toOriginatingElements()

        createNewFile(
            originatingElements = originatingElements,
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
        val originatingElements = fileSpec.members
            .filterIsInstance<OriginatingElementsHolder>()
            .flatMap { it.originatingElements }
            .toOriginatingElements()

        createNewFile(
            originatingElements = originatingElements,
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

    private fun createNewFile(
        originatingElements: OriginatingElements,
        packageName: String,
        fileName: String,
        extensionName: String,
        aggregating: Boolean
    ): OutputStream {
        val dependencies = if (originatingElements.isEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.WARNING,
                """
                    No dependencies are reported for $fileName which will prevent
                    incremental compilation.
                    Please file a bug at $ISSUE_TRACKER_LINK.
                """.trimIndent()
            )
            Dependencies.ALL_FILES
        } else {
            Dependencies(
                aggregating = aggregating,
                sources = originatingElements.files.distinct().toTypedArray()
            )
        }

        if (originatingElements.classes.isNotEmpty()) {
            delegate.associateWithClasses(
                classes = originatingElements.classes,
                packageName = packageName,
                fileName = fileName,
                extensionName = extensionName
            )
        }

        return delegate.createNewFile(
            dependencies = dependencies,
            packageName = packageName,
            fileName = fileName,
            extensionName = extensionName
        )
    }

    private data class OriginatingElements(
        val files: List<KSFile>,
        val classes: List<KSClassDeclaration>,
    ) {
        fun isEmpty(): Boolean = files.isEmpty() && classes.isEmpty()
    }

    private fun List<Element>.toOriginatingElements(): OriginatingElements {
        val files = mutableListOf<KSFile>()
        val classes = mutableListOf<KSClassDeclaration>()

        forEach { element ->
            when (element) {
                is KSFileAsOriginatingElement -> files.add(element.ksFile)
                is KSClassDeclarationAsOriginatingElement -> classes.add(element.ksClassDeclaration)
                else -> error("Unexpected element type in originating elements. $element")
            }
        }

        return OriginatingElements(files, classes)
    }
}
