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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XFiler
import androidx.room.compiler.processing.XProcessingEnv
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStream
import java.nio.file.Path
import javax.annotation.processing.Filer
import javax.tools.StandardLocation
import kotlin.io.path.extension

internal class JavacFiler(
    private val processingEnv: XProcessingEnv,
    val delegate: Filer
) : XFiler {

    // "mode" is ignored in javac, and only applicable in KSP
    override fun write(javaFile: JavaFile, mode: XFiler.Mode) {
        javaFile.writeTo(delegate)
    }

    override fun write(fileSpec: FileSpec, mode: XFiler.Mode) {
        require(processingEnv.options.containsKey("kapt.kotlin.generated")) {
            val filePath = fileSpec.packageName.replace('.', '/')
            "Could not generate kotlin file $filePath/${fileSpec.name}.kt. The " +
                "annotation processing environment is not set to generate Kotlin files."
        }
        fileSpec.writeTo(delegate)
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
        val javaOriginatingElements =
            originatingElements.filterIsInstance<JavacElement>().map { it.element }.toTypedArray()
        return when (extension) {
            "java" -> {
                delegate.createSourceFile(
                    "$packageName.$fileNameWithoutExtension",
                    *javaOriginatingElements
                ).openOutputStream()
            }
            "kt" -> {
                delegate.createResource(
                    StandardLocation.SOURCE_OUTPUT,
                    packageName,
                    "$fileNameWithoutExtension.$extension",
                    *javaOriginatingElements
                ).openOutputStream()
            }
            else -> error("file type not supported: $extension")
        }
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
        val javaOriginatingElements =
            originatingElements.filterIsInstance<JavacElement>().map { it.element }.toTypedArray()
        val fileObject = delegate.createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            filePath.toString(),
            *javaOriginatingElements
        )
        return fileObject.openOutputStream()
    }
}
