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

package androidx.room.compiler.codegen.java

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.JCodeBlock
import androidx.room.compiler.codegen.TargetLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XMemberName
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec

internal class JavaCodeBlock(internal val actual: JCodeBlock) : JavaLang(), XCodeBlock {

    override fun toString() = actual.toString()

    internal class Builder : JavaLang(), XCodeBlock.Builder {
        internal val actual = JCodeBlock.builder()

        override fun add(code: XCodeBlock) = apply {
            require(code is JavaCodeBlock)
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
            val finalKeyword = if (isMutable) "" else "final "
            if (assignExpr != null) {
                require(assignExpr is JavaCodeBlock)
                actual.addStatement(
                    "$finalKeyword\$T \$L = \$L",
                    typeName.java,
                    name,
                    assignExpr.actual
                )
            } else {
                actual.addStatement("$finalKeyword\$T \$L", typeName.java, name)
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
            return JavaCodeBlock(actual.build())
        }

        // Converts '%' place holders to '$' for JavaPoet
        private fun processFormatString(format: String): String {
            // Replace KPoet's member name placeholder for a JPoet literal for a XMemberName arg.
            return format
                .replace("%M", "\$L")
                // TODO(b/247241415): Very simple replace for now, but this will not work when
                //  emitting modulo expressions!
                .replace('%', '$')
        }

        // Unwraps room.compiler.codegen types to their JavaPoet actual
        // TODO(b/247242375): Consider improving by wrapping args.
        private fun processArgs(args: Array<out Any?>): Array<Any?> {
            return Array(args.size) { index ->
                val arg = args[index]
                if (arg is TargetLanguage) {
                    check(arg.language == CodeLanguage.JAVA) { "$arg is not JavaCode" }
                }
                when (arg) {
                    is XTypeName -> arg.java
                    is XMemberName -> arg.java
                    is XTypeSpec -> (arg as JavaTypeSpec).actual
                    is XPropertySpec -> (arg as JavaPropertySpec).actual
                    is XFunSpec -> (arg as JavaFunSpec).actual
                    is XCodeBlock -> (arg as JavaCodeBlock).actual
                    else -> arg
                }
            }
        }
    }
}
