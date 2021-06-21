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

import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec

/**
 * Code generation interface for XProcessing.
 */
interface XFiler {

    fun write(javaFile: JavaFile, mode: Mode = Mode.Isolating)

    fun write(fileSpec: FileSpec, mode: Mode = Mode.Isolating)

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
