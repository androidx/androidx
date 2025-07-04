/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.compiler.codegen.impl

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFileSpec
import androidx.room.compiler.codegen.java.JavaFileSpec
import androidx.room.compiler.codegen.kotlin.KotlinFileSpec
import androidx.room.compiler.processing.XFiler

internal class XFileSpecImpl(override val java: JavaFileSpec, override val kotlin: KotlinFileSpec) :
    ImplSpec<JavaFileSpec, KotlinFileSpec>(), XFileSpec {

    override fun writeTo(language: CodeLanguage, generator: XFiler, mode: XFiler.Mode) {
        when (language) {
            CodeLanguage.JAVA -> java.writeTo(language, generator, mode)
            CodeLanguage.KOTLIN -> kotlin.writeTo(language, generator, mode)
        }
    }

    override fun toBuilder() = Builder(java.toBuilder(), kotlin.toBuilder())

    internal class Builder(val java: JavaFileSpec.Builder, val kotlin: KotlinFileSpec.Builder) :
        XFileSpec.Builder {
        private val delegates: List<XFileSpec.Builder> = listOf(java, kotlin)

        override fun addFileComment(code: XCodeBlock) = apply {
            delegates.forEach { it.addFileComment(code) }
        }

        override fun build() = XFileSpecImpl(java.build(), kotlin.build())
    }
}
