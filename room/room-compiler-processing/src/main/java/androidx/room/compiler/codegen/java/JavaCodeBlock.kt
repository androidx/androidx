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

import androidx.room.compiler.codegen.JCodeBlock
import androidx.room.compiler.codegen.JCodeBlockBuilder
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XMemberName
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XParameterSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room.compiler.codegen.impl.XCodeBlockImpl
import androidx.room.compiler.codegen.impl.XFunSpecImpl
import androidx.room.compiler.codegen.impl.XParameterSpecImpl
import androidx.room.compiler.codegen.impl.XPropertySpecImpl
import androidx.room.compiler.codegen.impl.XTypeSpecImpl

internal class JavaCodeBlock(override val actual: JCodeBlock) : JavaSpec<JCodeBlock>(), XCodeBlock {

    override fun toBuilder() = Builder(actual.toBuilder())

    internal class Builder(internal val actual: JCodeBlockBuilder) :
        XSpec.Builder(), XCodeBlock.Builder {

        override fun add(code: XCodeBlock) = apply {
            require(code is XCodeBlockImpl)
            actual.add(code.java.actual)
        }

        override fun add(format: String, vararg args: Any?) = apply {
            actual.add(formatString(format), *formatArgs(args))
        }

        override fun addStatement(format: String, vararg args: Any?) = apply {
            actual.addStatement(formatString(format), *formatArgs(args))
        }

        override fun addLocalVariable(
            name: String,
            typeName: XTypeName,
            isMutable: Boolean,
            assignExpr: XCodeBlock?,
        ) = apply {
            val finalKeyword = if (isMutable) "" else "final "
            if (assignExpr != null) {
                addStatement("$finalKeyword%T %L = %L", typeName, name, assignExpr)
            } else {
                addStatement("$finalKeyword%T %L", typeName, name)
            }
        }

        override fun beginControlFlow(controlFlow: String, vararg args: Any?) = apply {
            actual.beginControlFlow(formatString(controlFlow), *formatArgs(args))
        }

        override fun nextControlFlow(controlFlow: String, vararg args: Any?) = apply {
            actual.nextControlFlow(formatString(controlFlow), *formatArgs(args))
        }

        override fun endControlFlow() = apply { actual.endControlFlow() }

        override fun indent() = apply { actual.indent() }

        override fun unindent() = apply { actual.unindent() }

        override fun build() = JavaCodeBlock(actual.build())

        // Converts '%' place holders to '$' for JavaPoet
        private fun formatString(format: String): String {
            // Replace KPoet's member name placeholder for a JPoet literal for a XMemberName arg.
            return format
                .replace("%M", "\$L")
                // TODO(b/247241415): Very simple replace for now, but this will not work when
                //  emitting modulo expressions!
                .replace('%', '$')
        }

        // Unwraps room.compiler.codegen types to their JavaPoet actual
        // TODO(b/247242375): Consider improving by wrapping args.
        private fun formatArgs(args: Array<out Any?>): Array<Any?> {
            return Array(args.size) { index ->
                when (val arg = args[index]) {
                    is XTypeName -> arg.java
                    is XMemberName -> arg.java
                    is XName -> arg.java
                    is XTypeSpec -> (arg as XTypeSpecImpl).java.actual
                    is XParameterSpec -> (arg as XParameterSpecImpl).java.actual
                    is XPropertySpec -> (arg as XPropertySpecImpl).java.actual
                    is XFunSpec -> (arg as XFunSpecImpl).java.actual
                    is XCodeBlock -> (arg as XCodeBlockImpl).java.actual
                    is XAnnotationSpec -> (arg as XAnnotationSpecImpl).java.actual
                    is XTypeSpec.Builder,
                    is XPropertySpec.Builder,
                    is XFunSpec.Builder,
                    is XCodeBlock.Builder,
                    is XAnnotationSpec.Builder ->
                        error("Found builder, ${arg.javaClass}. Did you forget to call .build()?")
                    else -> arg
                }
            }
        }
    }
}
