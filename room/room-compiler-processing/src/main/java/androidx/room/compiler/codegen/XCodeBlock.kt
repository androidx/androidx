/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.java.JavaCodeBlock
import androidx.room.compiler.codegen.kotlin.KotlinCodeBlock

interface XCodeBlock : TargetLanguage {

    interface Builder : TargetLanguage {

        fun add(code: XCodeBlock): Builder

        fun add(format: String, vararg args: Any?): Builder

        fun addStatement(format: String, vararg args: Any?): Builder

        fun addLocalVariable(
            name: String,
            type: XTypeName,
            isMutable: Boolean = false,
            assignExpr: XCodeBlock
        ): Builder

        fun build(): XCodeBlock

        companion object {
            fun Builder.apply(
                javaCodeBuilder: com.squareup.javapoet.CodeBlock.Builder.() -> Unit,
                kotlinCodeBuilder: com.squareup.kotlinpoet.CodeBlock.Builder.() -> Unit,
            ): Builder = apply {
                when (language) {
                    CodeLanguage.JAVA -> {
                        check(this is JavaCodeBlock.Builder)
                        this.actual.javaCodeBuilder()
                    }
                    CodeLanguage.KOTLIN -> {
                        check(this is KotlinCodeBlock.Builder)
                        this.actual.kotlinCodeBuilder()
                    }
                }
            }
        }
    }

    companion object {
        fun builder(language: CodeLanguage): Builder {
            return when (language) {
                CodeLanguage.JAVA -> JavaCodeBlock.Builder()
                CodeLanguage.KOTLIN -> KotlinCodeBlock.Builder()
            }
        }

        fun of(language: CodeLanguage, format: String, vararg args: Any?): XCodeBlock {
            return builder(language).add(format, *args).build()
        }
    }
}