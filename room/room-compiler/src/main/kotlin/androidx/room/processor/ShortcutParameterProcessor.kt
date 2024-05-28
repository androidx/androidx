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

import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isArray
import androidx.room.vo.ShortcutQueryParameter

/** Processes parameters of methods that are annotated with Insert, Update or Delete. */
class ShortcutParameterProcessor(
    baseContext: Context,
    val containing: XType,
    val element: XExecutableParameterElement
) {
    val context = baseContext.fork(element)

    fun process(): ShortcutQueryParameter {
        val asMember = element.asMemberOf(containing)
        if (isParamNullable(asMember)) {
            context.logger.e(
                element = element,
                msg =
                    ProcessorErrors.nullableParamInShortcutMethod(
                        asMember.asTypeName().toString(context.codeLanguage)
                    )
            )
        }

        val name = element.name
        context.checker.check(
            !name.startsWith("_"),
            element = element,
            errorMsg = ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE
        )

        val (pojoType, isMultiple) = extractPojoType(asMember)
        return ShortcutQueryParameter(
            element = element,
            name = name,
            type = asMember,
            pojoType = pojoType,
            isMultiple = isMultiple
        )
    }

    private fun isParamNullable(paramType: XType): Boolean {
        if (element.isVarArgs()) {
            // Special case vararg params, they are never nullable in Kotlin and even though they
            // are nullable in Java a user can't make them non-null so we accept them as-is and
            // generate null-safe code.
            return false
        }
        if (paramType.nullability == XNullability.NULLABLE) {
            return true
        }
        if (paramType.isArray() && paramType.componentType.nullability == XNullability.NULLABLE) {
            return true
        }
        return paramType.typeArguments.any { it.nullability == XNullability.NULLABLE }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun extractPojoType(typeMirror: XType): Pair<XType?, Boolean> {

        val processingEnv = context.processingEnv

        fun verifyAndPair(pojoType: XType, isMultiple: Boolean): Pair<XType?, Boolean> {
            // kotlin may generate ? extends T so we should reduce it.
            val boundedVar = pojoType.extendsBound()
            return if (boundedVar != null) {
                verifyAndPair(boundedVar, isMultiple)
            } else {
                Pair(pojoType, isMultiple)
            }
        }

        fun extractPojoTypeFromIterator(iterableType: XType): XType {
            iterableType.typeElement!!.getAllNonPrivateInstanceMethods().forEach {
                if (it.jvmName == "iterator") {
                    return it.asMemberOf(iterableType).returnType.typeArguments.first()
                }
            }
            throw IllegalArgumentException("iterator() not found in Iterable $iterableType")
        }

        val iterableType = processingEnv.requireType("java.lang.Iterable").rawType
        if (iterableType.isAssignableFrom(typeMirror)) {
            val pojo = extractPojoTypeFromIterator(typeMirror)
            return verifyAndPair(pojo, true)
        }
        if (typeMirror.isArray()) {
            val pojo = typeMirror.componentType
            return verifyAndPair(pojo, true)
        }
        return verifyAndPair(typeMirror, false)
    }
}
