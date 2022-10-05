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
            typeName: XTypeName,
            isMutable: Boolean = false,
            assignExpr: XCodeBlock? = null
        ): Builder

        fun beginControlFlow(controlFlow: String, vararg args: Any?): Builder
        fun nextControlFlow(controlFlow: String, vararg args: Any?): Builder
        fun endControlFlow(): Builder

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

        /**
         * Convenience code block of a new instantiation expression.
         *
         * Shouldn't contain parenthesis.
         */
        fun ofNewInstance(
            language: CodeLanguage,
            typeName: XTypeName,
            argsFormat: String = "",
            vararg args: Any?
        ): XCodeBlock {
            return builder(language).apply {
                val newKeyword = when (language) {
                    CodeLanguage.JAVA -> "new "
                    CodeLanguage.KOTLIN -> ""
                }
                add("$newKeyword%T($argsFormat)", typeName, *args)
            }.build()
        }

        /**
         * Convenience code block of an unsafe cast expression.
         */
        fun ofCast(
            language: CodeLanguage,
            typeName: XTypeName,
            expressionBlock: XCodeBlock
        ): XCodeBlock {
            return builder(language).apply {
                when (language) {
                    CodeLanguage.JAVA -> {
                        add("(%T) (%L)", typeName, expressionBlock)
                    }
                    CodeLanguage.KOTLIN -> {
                        add("(%L) as %T", expressionBlock, typeName)
                    }
                }
            }.build()
        }

        /**
         * Convenience code block of a Java class literal.
         */
        fun ofJavaClassLiteral(
            language: CodeLanguage,
            typeName: XClassName,
        ): XCodeBlock {
            return when (language) {
                CodeLanguage.JAVA -> of(language, "%T.class", typeName)
                CodeLanguage.KOTLIN -> of(language, "%T::class.java", typeName)
            }
        }
    }
}