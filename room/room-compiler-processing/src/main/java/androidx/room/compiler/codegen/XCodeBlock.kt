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

/**
 * A fragment of a .java or .kt file, potentially containing declarations, statements.
 *
 * Code blocks support placeholders like [java.text.Format]. This uses a percent sign `%` but has
 * its own set of permitted placeholders:
 *
 *  * `%L` emits a *literal* value with no escaping. Arguments for literals may be strings,
 *    primitives, [type declarations][XTypeSpec], [annotations][XAnnotationSpec] and even other code
 *    blocks.
 *  * `%N` emits a *name*, using name collision avoidance where necessary. Arguments for names may
 *    be strings (actually any [character sequence][CharSequence]), [parameters][XParameterSpec],
 *    [properties][XPropertySpec], [functions][XFunSpec], and [types][XTypeSpec].
 *  * `%S` escapes the value as a *string*, wraps it with double quotes, and emits that.
 *  * `%T` emits a *type* reference. Types will be imported if possible. Arguments for types are
 *    their [names][XTypeName].
 *  * `%M` emits a *member* reference. A member is either a function or a property. If the member is
 *    importable, e.g. it's a top-level function or a property declared inside an object, the import
 *    will be resolved if possible. Arguments for members must be of type [XMemberName].
 */
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

        fun indent(): Builder
        fun unindent(): Builder

        fun build(): XCodeBlock

        companion object {
            /**
             * Convenience local immutable variable emitter.
             *
             * Shouldn't contain declaration, only right hand assignment expression.
             */
            fun Builder.addLocalVal(
                name: String,
                typeName: XTypeName,
                assignExprFormat: String,
                vararg assignExprArgs: Any?
            ) = apply {
                addLocalVariable(
                    name = name,
                    typeName = typeName,
                    isMutable = false,
                    assignExpr = of(language, assignExprFormat, *assignExprArgs)
                )
            }

            /**
             * Convenience for-each control flow emitter taking into account the receiver's
             * [CodeLanguage].
             *
             * For Java this will emit: `for (<typeName> <itemVarName> : <iteratorVarName>)`
             *
             * For Kotlin this will emit: `for (<itemVarName>: <typeName> in <iteratorVarName>)`
             */
            fun Builder.beginForEachControlFlow(
                itemVarName: String,
                typeName: XTypeName,
                iteratorVarName: String
            ) = apply {
                when (language) {
                    CodeLanguage.JAVA -> beginControlFlow(
                        "for (%T %L : %L)",
                        typeName, itemVarName, iteratorVarName
                    )
                    CodeLanguage.KOTLIN -> beginControlFlow(
                        "for (%L: %T in %L)",
                        itemVarName, typeName, iteratorVarName
                    )
                }
            }

            fun Builder.apply(
                javaCodeBuilder: com.squareup.javapoet.CodeBlock.Builder.() -> Unit,
                kotlinCodeBuilder: com.squareup.kotlinpoet.CodeBlock.Builder.() -> Unit,
            ) = apply {
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
                add("$newKeyword%T($argsFormat)", typeName.copy(nullable = false), *args)
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

        /**
         * Convenience code block of a conditional expression representing a ternary if.
         *
         * For Java this will emit: ` <condition> ? <leftExpr> : <rightExpr>)`
         *
         * For Kotlin this will emit: `if (<condition>) <leftExpr> else <rightExpr>)`
         */
        fun ofTernaryIf(
            language: CodeLanguage,
            condition: XCodeBlock,
            leftExpr: XCodeBlock,
            rightExpr: XCodeBlock,
        ): XCodeBlock {
            return when (language) {
                CodeLanguage.JAVA ->
                    of(language, "%L ? %L : %L", condition, leftExpr, rightExpr)
                CodeLanguage.KOTLIN ->
                    of(language, "if (%L) %L else %L", condition, leftExpr, rightExpr)
            }
        }
    }
}
