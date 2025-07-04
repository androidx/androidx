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

import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.java.JavaTypeSpec
import androidx.room.compiler.codegen.kotlin.KotlinTypeSpec
import androidx.room.compiler.processing.XElement

internal class XTypeSpecImpl(override val java: JavaTypeSpec, override val kotlin: KotlinTypeSpec) :
    ImplSpec<JavaTypeSpec, KotlinTypeSpec>(), XTypeSpec {
    override val name by lazy {
        if (java.name == null || kotlin.name == null) {
            check(java.name == null && kotlin.name == null) {
                "If either Java or Kotlin names are null, the other must also be null. "
            }
            return@lazy null
        }
        XName(java.name.java, kotlin.name.kotlin)
    }

    override fun toBuilder() = Builder(java.toBuilder(), kotlin.toBuilder())

    internal class Builder(val java: JavaTypeSpec.Builder, val kotlin: KotlinTypeSpec.Builder) :
        XSpec.Builder(), XTypeSpec.Builder {
        private val delegates: List<XTypeSpec.Builder> = listOf(java, kotlin)

        override fun superclass(typeName: XTypeName) = apply {
            delegates.forEach { it.superclass(typeName) }
        }

        override fun addSuperinterface(typeName: XTypeName) = apply {
            delegates.forEach { it.addSuperinterface(typeName) }
        }

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            delegates.forEach { it.addAnnotation(annotation) }
        }

        override fun addProperty(propertySpec: XPropertySpec) = apply {
            delegates.forEach { it.addProperty(propertySpec) }
        }

        override fun addFunction(functionSpec: XFunSpec) = apply {
            delegates.forEach { it.addFunction(functionSpec) }
        }

        override fun addType(typeSpec: XTypeSpec) = apply {
            delegates.forEach { it.addType(typeSpec) }
        }

        override fun addTypeVariable(typeVariable: XTypeName) = apply {
            delegates.forEach { it.addTypeVariable(typeVariable) }
        }

        override fun setPrimaryConstructor(functionSpec: XFunSpec) = apply {
            delegates.forEach { it.setPrimaryConstructor(functionSpec) }
        }

        override fun setVisibility(visibility: VisibilityModifier) = apply {
            delegates.forEach { it.setVisibility(visibility) }
        }

        override fun addAbstractModifier() = apply {
            delegates.forEach { it.addAbstractModifier() }
        }

        override fun addOriginatingElement(element: XElement) = apply {
            delegates.forEach { it.addOriginatingElement(element) }
        }

        override fun build() = XTypeSpecImpl(java.build(), kotlin.build())
    }
}
