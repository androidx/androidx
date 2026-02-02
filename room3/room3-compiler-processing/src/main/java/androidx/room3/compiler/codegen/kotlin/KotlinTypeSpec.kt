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

package androidx.room3.compiler.codegen.kotlin

import androidx.room3.compiler.codegen.KFunSpec
import androidx.room3.compiler.codegen.KTypeSpecBuilder
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
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.javapoet.KTypeSpec
import com.squareup.kotlinpoet.javapoet.KTypeVariableName

internal class KotlinTypeSpec(override val actual: KTypeSpec) : KotlinSpec<KTypeSpec>(), XTypeSpec {

    override val name = actual.name?.let { XName.of(it) }

    override fun toBuilder() = Builder(actual.toBuilder())

    internal class Builder(internal val actual: KTypeSpecBuilder) :
        XSpec.Builder(), XTypeSpec.Builder {

        override fun superclass(typeName: XTypeName) = apply { actual.superclass(typeName.kotlin) }

        override fun addSuperinterface(typeName: XTypeName) = apply {
            actual.addSuperinterface(typeName.kotlin)
        }

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is XAnnotationSpecImpl)
            actual.addAnnotation(annotation.kotlin.actual)
        }

        override fun addProperty(propertySpec: XPropertySpec) = apply {
            require(propertySpec is XPropertySpecImpl)
            actual.addProperty(propertySpec.kotlin.actual)
        }

        override fun addFunction(functionSpec: XFunSpec) = apply {
            require(functionSpec is XFunSpecImpl)
            actual.addFunction(functionSpec.kotlin.actual)
        }

        override fun addType(typeSpec: XTypeSpec) = apply {
            require(typeSpec is XTypeSpecImpl)
            actual.addType(typeSpec.kotlin.actual)
        }

        override fun addTypeVariable(typeVariable: XTypeName) = apply {
            require(typeVariable.kotlin is KTypeVariableName)
            actual.addTypeVariable(typeVariable.kotlin as KTypeVariableName)
        }

        override fun setPrimaryConstructor(functionSpec: XFunSpec) = apply {
            require(functionSpec is XFunSpecImpl)
            if (
                functionSpec.kotlin.actual.delegateConstructor != null ||
                    functionSpec.kotlin.actual.delegateConstructorArguments.isNotEmpty()
            ) {
                // If ctor has super call, create a new spec without the super call as
                // KotlinPoet disallows it and instead add the super params to the type spec.
                // See https://github.com/square/kotlinpoet/pull/1859
                val currentSpec = functionSpec.kotlin.actual
                val newSpec =
                    KFunSpec.constructorBuilder()
                        .addModifiers(currentSpec.modifiers)
                        .addAnnotations(currentSpec.annotations)
                        .addParameters(currentSpec.parameters)
                        .addCode(currentSpec.body)
                        .build()
                actual.primaryConstructor(newSpec)
                currentSpec.delegateConstructorArguments.forEach {
                    actual.addSuperclassConstructorParameter(it)
                }
            } else {
                actual.primaryConstructor(functionSpec.kotlin.actual)
            }
        }

        override fun setVisibility(visibility: VisibilityModifier) = apply {
            actual.addModifiers(visibility.toKotlinVisibilityModifier())
        }

        override fun addAbstractModifier(): XTypeSpec.Builder = apply {
            actual.addModifiers(KModifier.ABSTRACT)
        }

        override fun addOriginatingElement(element: XElement) = apply {
            actual.addOriginatingElement(element)
        }

        override fun build() = KotlinTypeSpec(actual.build())
    }
}
