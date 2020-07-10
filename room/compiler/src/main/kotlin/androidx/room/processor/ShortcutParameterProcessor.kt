/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.ext.asMemberOf
import androidx.room.ext.extendsBound
import androidx.room.ext.getAllNonPrivateInstanceMethods
import androidx.room.ext.requireTypeMirror
import androidx.room.vo.ShortcutQueryParameter
import asDeclaredType
import asTypeElement
import erasure
import isAssignableFrom
import isType
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

/**
 * Processes parameters of methods that are annotated with Insert, Update or Delete.
 */
class ShortcutParameterProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val element: VariableElement
) {
    val context = baseContext.fork(element)
    fun process(): ShortcutQueryParameter {
        val asMember = element.asMemberOf(
            context.processingEnv.typeUtils,
            containing
        )
        val name = element.simpleName.toString()
        context.checker.check(!name.startsWith("_"), element,
                ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE)

        val (pojoType, isMultiple) = extractPojoType(asMember)
        return ShortcutQueryParameter(
                element = element,
                name = name,
                type = asMember,
                pojoType = pojoType,
                isMultiple = isMultiple
        )
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun extractPojoType(typeMirror: TypeMirror): Pair<TypeMirror?, Boolean> {

        val processingEnv = context.processingEnv
        val typeUtils = context.processingEnv.typeUtils

        fun verifyAndPair(pojoType: TypeMirror, isMultiple: Boolean): Pair<TypeMirror?, Boolean> {
            if (!pojoType.isType()) {
                // kotlin may generate ? extends T so we should reduce it.
                val boundedVar = pojoType.extendsBound()
                return boundedVar?.let {
                    verifyAndPair(boundedVar, isMultiple)
                } ?: Pair(null, isMultiple)
            }
            return Pair(pojoType, isMultiple)
        }

        fun extractPojoTypeFromIterator(iterableType: DeclaredType): TypeMirror {
            iterableType.asTypeElement().getAllNonPrivateInstanceMethods(processingEnv).forEach {
                if (it.simpleName.toString() == "iterator") {
                    return it.asMemberOf(typeUtils, iterableType)
                        .returnType
                        .asDeclaredType()
                        .typeArguments
                        .first()
                }
            }
            throw IllegalArgumentException("iterator() not found in Iterable $iterableType")
        }

        val iterableType = processingEnv
                .requireTypeMirror("java.lang.Iterable").erasure(typeUtils)
        if (iterableType.isAssignableFrom(typeUtils, typeMirror)) {
            val declared = typeMirror.asDeclaredType()
            val pojo = extractPojoTypeFromIterator(declared)
            return verifyAndPair(pojo, true)
        }
        if (typeMirror is ArrayType) {
            val pojo = typeMirror.componentType
            return verifyAndPair(pojo, true)
        }
        return verifyAndPair(typeMirror, false)
    }
}
