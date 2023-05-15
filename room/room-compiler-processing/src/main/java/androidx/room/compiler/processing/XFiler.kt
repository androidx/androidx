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

package androidx.room.compiler.processing

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.java.JavaTypeSpec
import androidx.room.compiler.codegen.kotlin.KotlinTypeSpec
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStream
import java.nio.file.Path

/**
 * Code generation interface for XProcessing.
 */
interface XFiler {

    fun write(javaFile: JavaFile, mode: Mode = Mode.Isolating)

    fun write(fileSpec: FileSpec, mode: Mode = Mode.Isolating)

    /**
     * Writes a source file that will be part of the output artifact (e.g. jar).
     *
     * Only source files should be written via this function, if the extension is not `.java` or
     * `.kt` this function will throw an exception.
     *
     * @return the output stream to write the resource file.
     */
    fun writeSource(
        packageName: String,
        fileNameWithoutExtension: String,
        extension: String,
        originatingElements: List<XElement>,
        mode: Mode = Mode.Isolating
    ): OutputStream

    /**
     * Writes a resource file that will be part of the output artifact (e.g. jar).
     *
     * Only non-source files should be written via this function, if the file path corresponds to a
     * source file, `.java` or `.kt` this function will throw an exception.
     *
     * @return the output stream to write the resource file.
     */
    fun writeResource(
        filePath: Path,
        originatingElements: List<XElement>,
        mode: Mode = Mode.Isolating
    ): OutputStream

    /**
     * Specifies whether a file represents aggregating or isolating inputs for incremental
     * build purposes. This does not apply in Javac processing because aggregating vs isolating
     * is set on the processor level. For more on KSP's definitions of isolating vs aggregating
     * see the documentation at
     * https://github.com/google/ksp/blob/master/docs/incremental.md
     */
    enum class Mode {
        Aggregating, Isolating
    }
}

fun JavaFile.writeTo(generator: XFiler, mode: XFiler.Mode = XFiler.Mode.Isolating) {
    generator.write(this, mode)
}

fun FileSpec.writeTo(generator: XFiler, mode: XFiler.Mode = XFiler.Mode.Isolating) {
    generator.write(this, mode)
}

fun XTypeSpec.writeTo(generator: XFiler, mode: XFiler.Mode = XFiler.Mode.Isolating) {
    require(this.className.simpleNames.size == 1) { "XTypeSpec must be a top-level class." }
    when (this.language) {
        CodeLanguage.JAVA -> {
            check(this is JavaTypeSpec)
            JavaFile.builder(this.className.packageName, this.actual)
                .build()
                .writeTo(generator, mode)
        }
        CodeLanguage.KOTLIN -> {
            check(this is KotlinTypeSpec)
            FileSpec.builder(this.className.packageName, this.className.simpleNames.single())
                .addType(this.actual)
                .build()
                .writeTo(generator, mode)
        }
    }
}
