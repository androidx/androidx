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

package androidx.room3.compiler.codegen.java

import androidx.room3.compiler.codegen.JTypeSpecBuilder
import androidx.room3.compiler.codegen.VisibilityModifier
import androidx.room3.compiler.codegen.XAnnotationSpec
import androidx.room3.compiler.codegen.XFunSpec
import androidx.room3.compiler.codegen.XName
import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XSpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.XTypeSpec
import androidx.room3.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room3.compiler.codegen.impl.XFunSpecImpl
import androidx.room3.compiler.codegen.impl.XPropertySpecImpl
import androidx.room3.compiler.codegen.impl.XTypeSpecImpl
import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.addOriginatingElement
import com.squareup.kotlinpoet.javapoet.JTypeSpec
import com.squareup.kotlinpoet.javapoet.JTypeVariableName
import javax.lang.model.element.Modifier

internal class JavaTypeSpec(override val actual: JTypeSpec) : JavaSpec<JTypeSpec>(), XTypeSpec {

    override val name = actual.name?.let { XName.of(it) }

    override fun toBuilder() = Builder(actual.toBuilder())

    internal class Builder(internal val actual: JTypeSpecBuilder) :
        XSpec.Builder(), XTypeSpec.Builder {
        override fun superclass(typeName: XTypeName) = apply { actual.superclass(typeName.java) }

        override fun addSuperinterface(typeName: XTypeName) = apply {
            actual.addSuperinterface(typeName.java)
        }

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is XAnnotationSpecImpl)
            actual.addAnnotation(annotation.java.actual)
        }

        override fun addProperty(propertySpec: XPropertySpec) = apply {
            require(propertySpec is XPropertySpecImpl)
            actual.addField(propertySpec.java.actual)
        }

        override fun addFunction(functionSpec: XFunSpec) = apply {
            require(functionSpec is XFunSpecImpl)
            actual.addMethod(functionSpec.java.actual)
        }

        override fun addType(typeSpec: XTypeSpec) = apply {
            require(typeSpec is XTypeSpecImpl)
            actual.addType(typeSpec.java.actual)
        }

        override fun addTypeVariable(typeVariable: XTypeName) = apply {
            require(typeVariable.java is JTypeVariableName)
            actual.addTypeVariable(typeVariable.java as JTypeVariableName)
        }

        override fun setPrimaryConstructor(functionSpec: XFunSpec) = addFunction(functionSpec)

        override fun setVisibility(visibility: VisibilityModifier) = apply {
            actual.addModifiers(visibility.toJavaVisibilityModifier())
        }

        override fun addAbstractModifier(): XTypeSpec.Builder = apply {
            actual.addModifiers(Modifier.ABSTRACT)
        }

        override fun addOriginatingElement(element: XElement) = apply {
            actual.addOriginatingElement(element)
        }

        override fun build() = JavaTypeSpec(actual.build())
    }
}
