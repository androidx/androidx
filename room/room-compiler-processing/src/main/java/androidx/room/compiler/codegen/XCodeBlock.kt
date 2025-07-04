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

import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.codegen.impl.XCodeBlockImpl
import androidx.room.compiler.codegen.java.JavaCodeBlock
import androidx.room.compiler.codegen.kotlin.KotlinCodeBlock

/**
 * A fragment of a .java or .kt file, potentially containing declarations, statements.
 *
 * Code blocks support placeholders like [java.text.Format]. This uses a percent sign `%` but has
 * its own set of permitted placeholders:
 * * `%L` emits a *literal* value with no escaping. Arguments for literals may be strings,
 *   primitives, [type declarations][XTypeSpec], [annotations][XAnnotationSpec] and even other code
 *   blocks.
 * * `%N` emits a *name*, using name collision avoidance where necessary. Arguments for names may be
 *   strings (actually any [character sequence][CharSequence]), [parameters][XParameterSpec],
 *   [properties][XPropertySpec], [functions][XFunSpec], and [types][XTypeSpec].
 * * `%S` escapes the value as a *string*, wraps it with double quotes, and emits that.
 * * `%T` emits a *type* reference. Types will be imported if possible. Arguments for types are
 *   their [names][XTypeName].
 * * `%M` emits a *member* reference. A member is either a function or a property. If the member is
 *   importable, e.g. it's a top-level function or a property declared inside an object, the import
 *   will be resolved if possible. Arguments for members must be of type [XMemberName].
 */
interface XCodeBlock {

    fun toBuilder(): Builder

    interface Builder {

        fun add(code: XCodeBlock): Builder

        fun add(format: String, vararg args: Any?): Builder

        fun addStatement(format: String, vararg args: Any?): Builder

        fun addLocalVariable(
            name: String,
            typeName: XTypeName,
            isMutable: Boolean = false,
            assignExpr: XCodeBlock? = null,
        ): Builder

        fun beginControlFlow(controlFlow: String, vararg args: Any?): Builder

        fun nextControlFlow(controlFlow: String, vararg args: Any?): Builder

        fun endControlFlow(): Builder

        fun indent(): Builder

        fun unindent(): Builder

        /**
         * Convenience local immutable variable emitter.
         *
         * Shouldn't contain declaration, only right hand assignment expression.
         */
        fun addLocalVal(
            name: String,
            typeName: XTypeName,
            assignExprFormat: String,
            vararg assignExprArgs: Any?,
        ) = apply {
            addLocalVariable(
                name = name,
                typeName = typeName,
                isMutable = false,
                assignExpr = of(assignExprFormat, *assignExprArgs),
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
        fun beginForEachControlFlow(
            itemVarName: String,
            typeName: XTypeName,
            iteratorVarName: String,
        ) = applyTo { language ->
            when (language) {
                CodeLanguage.JAVA ->
                    beginControlFlow("for (%T %L : %L)", typeName, itemVarName, iteratorVarName)
                CodeLanguage.KOTLIN ->
                    beginControlFlow("for (%L: %T in %L)", itemVarName, typeName, iteratorVarName)
            }
        }

        fun build(): XCodeBlock

        companion object {
            fun Builder.applyTo(block: Builder.(CodeLanguage) -> Unit) = apply {
                when (this) {
                    is XCodeBlockImpl.Builder -> {
                        this.java.block(CodeLanguage.JAVA)
                        this.kotlin.block(CodeLanguage.KOTLIN)
                    }
                    is JavaCodeBlock.Builder -> block(CodeLanguage.JAVA)
                    is KotlinCodeBlock.Builder -> block(CodeLanguage.KOTLIN)
                }
            }

            fun Builder.applyTo(language: CodeLanguage, block: Builder.() -> Unit) =
                applyTo { codeLanguage ->
                    if (codeLanguage == language) {
                        block()
                    }
                }
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder =
            XCodeBlockImpl.Builder(
                JavaCodeBlock.Builder(JCodeBlock.builder()),
                KotlinCodeBlock.Builder(KCodeBlock.builder()),
            )

        @JvmStatic fun of(format: String, vararg args: Any?) = builder().add(format, *args).build()

        @JvmStatic
        fun ofString(java: String, kotlin: String) = buildCodeBlock { language ->
            when (language) {
                CodeLanguage.JAVA -> add(java)
                CodeLanguage.KOTLIN -> add(kotlin)
            }
        }

        /**
         * Convenience code block of a new instantiation expression.
         *
         * Shouldn't contain parenthesis.
         */
        @JvmStatic
        fun ofNewInstance(typeName: XTypeName, argsFormat: String = "", vararg args: Any?) =
            buildCodeBlock { language ->
                when (language) {
                    CodeLanguage.JAVA ->
                        add("new %T($argsFormat)", typeName.copy(nullable = false), *args)
                    CodeLanguage.KOTLIN ->
                        add("%T($argsFormat)", typeName.copy(nullable = false), *args)
                }
            }

        /** Convenience code block of an unsafe cast expression. */
        @JvmStatic
        fun ofCast(typeName: XTypeName, expressionBlock: XCodeBlock) = buildCodeBlock { language ->
            when (language) {
                CodeLanguage.JAVA -> add("(%T) (%L)", typeName, expressionBlock)
                CodeLanguage.KOTLIN -> add("(%L) as %T", expressionBlock, typeName)
            }
        }

        /** Convenience code block of a Java class literal. */
        @JvmStatic
        fun ofJavaClassLiteral(typeName: XClassName) = buildCodeBlock { language ->
            when (language) {
                CodeLanguage.JAVA -> add("%T.class", typeName)
                CodeLanguage.KOTLIN -> add("%T::class.java", typeName)
            }
        }

        /** Convenience code block of a Kotlin class literal. */
        @JvmStatic
        fun ofKotlinClassLiteral(typeName: XClassName) = buildCodeBlock { language ->
            when (language) {
                CodeLanguage.JAVA ->
                    add(
                        "%T.getKotlinClass(%T.class)",
                        XClassName.get("kotlin.jvm", "JvmClassMappingKt"),
                        typeName,
                    )
                CodeLanguage.KOTLIN -> add("%T::class", typeName)
            }
        }

        /**
         * Convenience code block of a conditional expression representing a ternary if.
         *
         * For Java this will emit: ` <condition> ? <leftExpr> : <rightExpr>)`
         *
         * For Kotlin this will emit: `if (<condition>) <leftExpr> else <rightExpr>)`
         */
        @JvmStatic
        fun ofTernaryIf(condition: XCodeBlock, leftExpr: XCodeBlock, rightExpr: XCodeBlock) =
            buildCodeBlock { language ->
                when (language) {
                    CodeLanguage.JAVA -> add("%L ? %L : %L", condition, leftExpr, rightExpr)
                    CodeLanguage.KOTLIN -> add("if (%L) %L else %L", condition, leftExpr, rightExpr)
                }
            }

        /**
         * Convenience code block of an extension function call.
         *
         * For Java this will emit: ` <memberName>(<receiverVariableName>, <args>)`
         *
         * For Kotlin this will emit: `<receiverVarName>.<memberName>(<args>)`
         */
        @JvmStatic
        fun ofExtensionCall(memberName: XMemberName, receiverVarName: String, args: XCodeBlock) =
            buildCodeBlock { language ->
                when (language) {
                    CodeLanguage.JAVA -> add("%M(%L, %L)", memberName, receiverVarName, args)
                    CodeLanguage.KOTLIN -> add("%L.%M(%L)", receiverVarName, memberName, args)
                }
            }
    }
}

fun buildCodeBlock(block: XCodeBlock.Builder.(CodeLanguage) -> Unit) =
    XCodeBlock.builder().applyTo { language -> block(language) }.build()
