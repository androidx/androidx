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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.KCodeBlock
import androidx.room.compiler.codegen.KCodeBlockBuilder
import androidx.room.compiler.codegen.TargetLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XMemberName
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec

internal class KotlinCodeBlock(internal val actual: KCodeBlock) : KotlinLang(), XCodeBlock {

    override fun toString() = actual.toString()

    internal class Builder : KotlinLang(), XCodeBlock.Builder {

        internal val actual = KCodeBlockBuilder()

        override fun add(code: XCodeBlock) = apply {
            require(code is KotlinCodeBlock)
            actual.add(code.actual)
        }

        override fun add(format: String, vararg args: Any?) = apply {
            val processedFormat = processFormatString(format)
            val processedArgs = processArgs(args)
            actual.add(processedFormat, *processedArgs)
        }

        override fun addStatement(format: String, vararg args: Any?) = apply {
            val processedFormat = processFormatString(format)
            val processedArgs = processArgs(args)
            actual.addStatement(processedFormat, *processedArgs)
        }

        override fun addLocalVariable(
            name: String,
            typeName: XTypeName,
            isMutable: Boolean,
            assignExpr: XCodeBlock?
        ) = apply {
            val varOrVal = if (isMutable) "var" else "val"
            if (assignExpr != null) {
                require(assignExpr is KotlinCodeBlock)
                actual.addStatement(
                    "$varOrVal %L: %T = %L",
                    name,
                    typeName.kotlin,
                    assignExpr.actual
                )
            } else {
                actual.addStatement(
                    "$varOrVal %L: %T",
                    name,
                    typeName.kotlin,
                )
            }
        }

        override fun beginControlFlow(controlFlow: String, vararg args: Any?) = apply {
            val processedControlFlow = processFormatString(controlFlow)
            val processedArgs = processArgs(args)
            actual.beginControlFlow(processedControlFlow, *processedArgs)
        }

        override fun nextControlFlow(controlFlow: String, vararg args: Any?) = apply {
            val processedControlFlow = processFormatString(controlFlow)
            val processedArgs = processArgs(args)
            actual.nextControlFlow(processedControlFlow, *processedArgs)
        }

        override fun endControlFlow() = apply { actual.endControlFlow() }

        override fun indent() = apply { actual.indent() }

        override fun unindent() = apply { actual.unindent() }

        override fun build(): XCodeBlock {
            return KotlinCodeBlock(actual.build())
        }

        // No need to really process 'format' since we use '%' as placeholders, but check for
        // JavaPoet placeholders to hunt down bad migrations to XPoet.
        private fun processFormatString(format: String): String {
            JAVA_POET_PLACEHOLDER_REGEX.find(format)?.let {
                error("Bad JavaPoet placeholder in XPoet at range ${it.range} of input: '$format'")
            }
            return format
        }

        // Unwraps room.compiler.codegen types to their KotlinPoet actual
        // TODO(b/247242375): Consider improving by wrapping args.
        private fun processArgs(args: Array<out Any?>): Array<Any?> {
            return Array(args.size) { index ->
                val arg = args[index]
                if (arg is TargetLanguage) {
                    check(arg.language == CodeLanguage.KOTLIN) { "$arg is not KotlinCode" }
                }
                when (arg) {
                    is XTypeName -> arg.kotlin
                    is XMemberName -> arg.kotlin
                    is XTypeSpec -> (arg as KotlinTypeSpec).actual
                    is XPropertySpec -> (arg as KotlinPropertySpec).actual
                    is XFunSpec -> (arg as KotlinFunSpec).actual
                    is XCodeBlock -> (arg as KotlinCodeBlock).actual
                    else -> arg
                }
            }
        }
    }

    companion object {
        private val JAVA_POET_PLACEHOLDER_REGEX =
            "(\\\$L)|(\\\$T)|(\\\$N)|(\\\$S)|(\\\$W)".toRegex()
    }
}
