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

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.originatingElementForPoet
import androidx.room.compiler.processing.util.ISSUE_TRACKER_LINK
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.OriginatingElementsHolder
import java.io.OutputStream
import java.nio.file.Path
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

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

    override fun writeSource(
        packageName: String,
        fileNameWithoutExtension: String,
        extension: String,
        originatingElements: List<XElement>,
        mode: XFiler.Mode
    ): OutputStream {
        require(extension == "java" || extension == "kt") {
            "Source file extension must be either 'java' or 'kt', but was: $extension"
        }
        val kspFilerOriginatingElements = originatingElements
            .mapNotNull { it.originatingElementForPoet() }
            .toOriginatingElements()
        return createNewFile(
            originatingElements = kspFilerOriginatingElements,
            packageName = packageName,
            fileName = fileNameWithoutExtension,
            extensionName = extension,
            aggregating = mode == XFiler.Mode.Aggregating
        )
    }

    override fun writeResource(
        filePath: Path,
        originatingElements: List<XElement>,
        mode: XFiler.Mode
    ): OutputStream {
        require(filePath.extension != "java" && filePath.extension != "kt") {
            "Could not create resource file with a source type extension. File must not be " +
                "neither '.java' nor '.kt', but was: $filePath"
        }
        val kspFilerOriginatingElements = originatingElements
            .mapNotNull { it.originatingElementForPoet() }
            .toOriginatingElements()
        return createNewFile(
            originatingElements = kspFilerOriginatingElements,
            packageName = filePath.parent?.toString() ?: "",
            fileName = filePath.nameWithoutExtension,
            extensionName = filePath.extension,
            aggregating = mode == XFiler.Mode.Aggregating
        )
    }

    private fun createNewFile(
        originatingElements: OriginatingElements,
        packageName: String,
        fileName: String,
        extensionName: String,
        aggregating: Boolean
    ): OutputStream {
        val dependencies = if (originatingElements.isEmpty()) {
            val isSourceFile = extensionName == "java" || extensionName == "kt"
            if (isSourceFile) {
                val filePath = "$packageName.$fileName.$extensionName"
                messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    "No dependencies reported for generated source $filePath which will" +
                        "prevent incremental compilation.\n" +
                        "Please file a bug at $ISSUE_TRACKER_LINK."
                )
            }
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
