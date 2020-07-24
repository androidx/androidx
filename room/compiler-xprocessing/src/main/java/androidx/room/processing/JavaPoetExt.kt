/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.room.processing

import androidx.room.processing.javac.JavacElement
import androidx.room.processing.javac.JavacExecutableElement
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Javapoet does not model NonType, unlike javac, which makes it hard to rely on TypeName for
 * common functionality (e.g. ability to implement XType.isLong as typename() == TypeName.LONG
 * instead of in the base class)
 *
 * For those cases, we have this hacky type so that we can always query TypeName on an XType.
 *
 * We should still strive to avoid these cases, maybe turn it to an error in tests.
 */
private val NONE_TYPE_NAME = ClassName.get("androidx.room.processing.error", "NotAType")

internal fun TypeMirror.safeTypeName(): TypeName = if (kind == TypeKind.NONE) {
    NONE_TYPE_NAME
} else {
    TypeName.get(this)
}

/**
 * Adds the given element as the originating element for compilation.
 * see [TypeSpec.Builder.addOriginatingElement].
 */
fun TypeSpec.Builder.addOriginatingElement(element: XElement) {
    if (element is JavacElement) {
        this.addOriginatingElement(element.element)
    }
}

/**
 * Helper class to create overrides for XExecutableElements with final parameters and correct
 * parameter names read from Kotlin Metadata.
 */
object MethodSpecHelper {
    /**
     * Creates an overriding [MethodSpec] for the given [XExecutableElement] where:
     * * all parameters are marked as final
     * * parameter names are copied from KotlinMetadata when available
     * * [Override] annotation is added and other annotations are dropped
     * * thrown types are copied if the backing element is from java
     */
    fun overridingWithFinalParams(
        elm: XMethodElement,
        owner: XDeclaredType
    ): MethodSpec.Builder {
        return overridingWithFinalParams(
            elm,
            elm.asMemberOf(owner)
        )
    }

    private fun overridingWithFinalParams(
        executableElement: XMethodElement,
        resolvedType: XMethodType = executableElement.executableType
    ): MethodSpec.Builder {
        return MethodSpec.methodBuilder(executableElement.name).apply {
            addTypeVariables(
                resolvedType.typeVariableNames
            )
            resolvedType.parameterTypes.forEachIndexed { index, paramType ->
                addParameter(
                    ParameterSpec.builder(
                        paramType.typeName,
                        executableElement.parameters[index].name,
                        Modifier.FINAL
                    ).build()
                )
            }
            if (executableElement.isPublic()) {
                addModifiers(Modifier.PUBLIC)
            } else if (executableElement.isProtected()) {
                addModifiers(Modifier.PROTECTED)
            }
            addAnnotation(Override::class.java)
            varargs(executableElement.isVarArgs())
            if (executableElement is JavacExecutableElement) {
                // copy throws for java
                executableElement.element.thrownTypes.forEach {
                    addException(TypeName.get(it))
                }
            }
            returns(resolvedType.returnType.typeName)
        }
    }
}
