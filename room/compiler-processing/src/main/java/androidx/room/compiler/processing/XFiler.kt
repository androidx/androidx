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
    /**
     * @param aggregating Specifies whether this
     * file represents aggregating or isolating inputs for incremental build purposes. This does
     * not apply in Javac processing because aggregating vs isolating is set on the processor
     * level. For more on KSP's definitions of isolating vs aggregating see the documentation at
     * https://github.com/google/ksp/blob/master/docs/incremental.md
     */
    fun write(javaFile: JavaFile, aggregating: Boolean = false)

    fun write(fileSpec: FileSpec, aggregating: Boolean = false)
}

fun JavaFile.writeTo(generator: XFiler, aggregating: Boolean = false) {
    generator.write(this, aggregating)
}

fun FileSpec.writeTo(generator: XFiler, aggregating: Boolean = false) {
    generator.write(this, aggregating)
}
