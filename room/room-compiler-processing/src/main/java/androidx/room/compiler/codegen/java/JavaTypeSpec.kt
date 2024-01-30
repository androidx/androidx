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

import androidx.room.compiler.codegen.JTypeSpecBuilder
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import com.squareup.kotlinpoet.javapoet.JTypeSpec
import javax.lang.model.element.Modifier

internal class JavaTypeSpec(
    private val _className: XClassName?,
    internal val actual: JTypeSpec
) : JavaLang(), XTypeSpec {
    override fun toString() = actual.toString()

    override val className: XClassName
        get() {
            checkNotNull(_className) { "Anonymous classes have no name." }
            return _className
        }

    internal class Builder(
        private val className: XClassName?,
        internal val actual: JTypeSpecBuilder
    ) : JavaLang(), XTypeSpec.Builder {
        override fun superclass(typeName: XTypeName) = apply {
            actual.superclass(typeName.java)
        }

        override fun addSuperinterface(typeName: XTypeName) = apply {
            actual.addSuperinterface(typeName.java)
        }

        override fun addAnnotation(annotation: XAnnotationSpec) {
            require(annotation is JavaAnnotationSpec)
            actual.addAnnotation(annotation.actual)
        }

        override fun addProperty(propertySpec: XPropertySpec) = apply {
            require(propertySpec is JavaPropertySpec)
            actual.addField(propertySpec.actual)
        }

        override fun addFunction(functionSpec: XFunSpec) = apply {
            require(functionSpec is JavaFunSpec)
            actual.addMethod(functionSpec.actual)
        }

        override fun addType(typeSpec: XTypeSpec) = apply {
            require(typeSpec is JavaTypeSpec)
            actual.addType(typeSpec.actual)
        }

        override fun setPrimaryConstructor(functionSpec: XFunSpec) = addFunction(functionSpec)

        override fun setVisibility(visibility: VisibilityModifier) {
            actual.addModifiers(visibility.toJavaVisibilityModifier())
        }

        override fun build(): XTypeSpec {
            return JavaTypeSpec(className, actual.build())
        }

        override fun addAbstractModifier(): XTypeSpec.Builder = apply {
            actual.addModifiers(Modifier.ABSTRACT)
        }
    }
}
