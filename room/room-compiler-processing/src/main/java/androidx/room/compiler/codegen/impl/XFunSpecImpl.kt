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

import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XParameterSpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.java.JavaFunSpec
import androidx.room.compiler.codegen.kotlin.KotlinFunSpec

internal class XFunSpecImpl(override val java: JavaFunSpec, override val kotlin: KotlinFunSpec) :
    ImplSpec<JavaFunSpec, KotlinFunSpec>(), XFunSpec {

    override val name = XName(java.name.java, kotlin.name.kotlin)

    override fun toBuilder() = Builder(java.toBuilder(), kotlin.toBuilder())

    internal class Builder(val java: JavaFunSpec.Builder, val kotlin: KotlinFunSpec.Builder) :
        XSpec.Builder(), XFunSpec.Builder {
        private val delegates: List<XFunSpec.Builder> = listOf(java, kotlin)

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            delegates.forEach { it.addAnnotation(annotation) }
        }

        override fun addTypeVariable(typeVariable: XTypeName) = apply {
            delegates.forEach { it.addTypeVariable(typeVariable) }
        }

        override fun addAbstractModifier() = apply {
            delegates.forEach { it.addAbstractModifier() }
        }

        override fun addParameter(parameter: XParameterSpec) = apply {
            delegates.forEach { it.addParameter(parameter) }
        }

        override fun addParameter(name: String, typeName: XTypeName) = apply {
            delegates.forEach { it.addParameter(name, typeName) }
        }

        override fun addCode(code: XCodeBlock) = apply { delegates.forEach { it.addCode(code) } }

        override fun callSuperConstructor(vararg args: XCodeBlock) = apply {
            delegates.forEach { it.callSuperConstructor(*args) }
        }

        override fun returns(typeName: XTypeName) = apply {
            delegates.forEach { it.returns(typeName) }
        }

        override fun build() = XFunSpecImpl(java.build(), kotlin.build())
    }
}
