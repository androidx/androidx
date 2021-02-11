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

package androidx.room.compiler.processing.util

import com.tschuchort.compiletesting.KotlinCompilation
import java.io.File
import java.io.OutputStream

/**
 * Helper class for Kotlin Compile Testing library to have common setup for room.
 */
internal object KotlinCompilationUtil {
    fun prepareCompilation(
        sources: List<Source>,
        outputStream: OutputStream,
        classpaths: List<File> = emptyList()
    ): KotlinCompilation {
        val compilation = KotlinCompilation()
        val srcRoot = compilation.workingDir.resolve("ksp/srcInput")
        val javaSrcRoot = srcRoot.resolve("java")
        val kotlinSrcRoot = srcRoot.resolve("kotlin")
        compilation.sources = sources.map {
            when (it) {
                is Source.JavaSource -> it.toKotlinSourceFile(javaSrcRoot)
                is Source.KotlinSource -> it.toKotlinSourceFile(kotlinSrcRoot)
            }
        }
        // workaround for https://github.com/tschuchortdev/kotlin-compile-testing/issues/105
        compilation.kotlincArguments += "-Xjava-source-roots=${javaSrcRoot.absolutePath}"
        compilation.jvmDefault = "enable"
        compilation.jvmTarget = "1.8"
        compilation.inheritClassPath = true
        compilation.verbose = false
        compilation.classpaths += classpaths
        compilation.messageOutputStream = outputStream
        return compilation
    }
}