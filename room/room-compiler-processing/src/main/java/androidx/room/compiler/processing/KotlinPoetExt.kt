/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.room.compiler.codegen.XTypeName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.OriginatingElementsHolder
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.javapoet.KClassName

internal val KOTLIN_NONE_TYPE_NAME: KClassName =
    KClassName("androidx.room.compiler.processing.error", "NotAType")

/**
 * Adds the given element as an originating element for compilation.
 * see [OriginatingElementsHolder.Builder.addOriginatingElement].
 */
fun <T : OriginatingElementsHolder.Builder<T>> T.addOriginatingElement(
    element: XElement
): T {
    element.originatingElementForPoet()?.let(this::addOriginatingElement)
    return this
}

internal fun TypeName.rawTypeName(): TypeName {
    return if (this is ParameterizedTypeName) {
        this.rawType
    } else {
        this
    }
}

object FunSpecHelper {
    fun overriding(
        elm: XMethodElement,
        owner: XType
    ): FunSpec.Builder {
        val asMember = elm.asMemberOf(owner)
        return overriding(
            executableElement = elm,
            resolvedType = asMember
        )
    }

    private fun overriding(
        executableElement: XMethodElement,
        resolvedType: XMethodType
    ): FunSpec.Builder {
        return FunSpec.builder(executableElement.name).apply {
            addModifiers(KModifier.OVERRIDE)
            if (executableElement.isInternal()) {
                addModifiers(KModifier.INTERNAL)
            } else if (executableElement.isProtected()) {
                addModifiers(KModifier.PROTECTED)
            } else if (executableElement.isPublic()) {
                addModifiers(KModifier.PUBLIC)
            }
            if (executableElement.isSuspendFunction()) {
                addModifiers(KModifier.SUSPEND)
            }
            // TODO(b/251316420): Add type variable names
            val parameterTypes = resolvedType.parameterTypes.let {
                // Drop the synthetic Continuation param of suspend functions, always at the last
                // position.
                // TODO(b/254135327): Revisit with the introduction of a target language.
                if (resolvedType.isSuspendFunction()) it.dropLast(1) else it
            }
            parameterTypes.forEachIndexed { index, paramType ->
                val typeName: XTypeName
                val modifiers: Array<KModifier>
                // TODO(b/253268357): In Kotlin the vararg is not always the last param
                if (executableElement.parameters.get(index).isVarArgs()) {
                    typeName = (paramType as XArrayType).componentType.asTypeName()
                    modifiers = arrayOf(KModifier.VARARG)
                } else {
                    typeName = paramType.asTypeName()
                    modifiers = emptyArray()
                }
                addParameter(
                    executableElement.parameters[index].name,
                    typeName.kotlin,
                    *modifiers
                )
            }
            returns(
                if (resolvedType.isSuspendFunction()) {
                    resolvedType.getSuspendFunctionReturnType()
                } else {
                    resolvedType.returnType
                }.asTypeName().kotlin
            )
        }
    }
}
