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

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.java.JavaCodeBlock
import androidx.room.compiler.codegen.kotlin.KotlinCodeBlock

internal class XCodeBlockImpl(
    override val java: JavaCodeBlock,
    override val kotlin: KotlinCodeBlock,
) : ImplSpec<JavaCodeBlock, KotlinCodeBlock>(), XCodeBlock {

    override fun toBuilder() = Builder(java.toBuilder(), kotlin.toBuilder())

    internal class Builder(val java: JavaCodeBlock.Builder, val kotlin: KotlinCodeBlock.Builder) :
        XSpec.Builder(), XCodeBlock.Builder {
        private val delegates: List<XCodeBlock.Builder> = listOf(java, kotlin)

        override fun add(code: XCodeBlock) = apply { delegates.forEach { it.add(code) } }

        override fun add(format: String, vararg args: Any?) = apply {
            delegates.forEach { it.add(format, *args) }
        }

        override fun addStatement(format: String, vararg args: Any?) = apply {
            delegates.forEach { it.addStatement(format, *args) }
        }

        override fun addLocalVariable(
            name: String,
            typeName: XTypeName,
            isMutable: Boolean,
            assignExpr: XCodeBlock?,
        ) = apply {
            delegates.forEach { it.addLocalVariable(name, typeName, isMutable, assignExpr) }
        }

        override fun beginControlFlow(controlFlow: String, vararg args: Any?) = apply {
            delegates.forEach { it.beginControlFlow(controlFlow, *args) }
        }

        override fun nextControlFlow(controlFlow: String, vararg args: Any?) = apply {
            delegates.forEach { it.nextControlFlow(controlFlow, *args) }
        }

        override fun endControlFlow() = apply { delegates.forEach { it.endControlFlow() } }

        override fun indent() = apply { delegates.forEach { it.indent() } }

        override fun unindent() = apply { delegates.forEach { it.unindent() } }

        override fun build() = XCodeBlockImpl(java.build(), kotlin.build())
    }
}
