/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.support.room.processor

import com.android.support.room.Ignore
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Field
import com.android.support.room.vo.FieldGetter
import com.android.support.room.vo.FieldSetter
import com.android.support.room.vo.Pojo
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Processes any class as if it is a Pojo.
 */
class PojoProcessor(baseContext : Context, val element: TypeElement) {
    val context = baseContext.fork(element)
    fun process(): Pojo {
        val declaredType = MoreTypes.asDeclared(element.asType())
        val allMembers = context.processingEnv.elementUtils.getAllMembers(element)
        val fields = allMembers
                .filter {
                    it.kind == ElementKind.FIELD
                            && !it.hasAnnotation(Ignore::class)
                            && !it.hasAnyOf(Modifier.STATIC)
                }
                .map { FieldProcessor(
                        baseContext = context,
                        containing = declaredType,
                        element = it).process() }

        val methods = allMembers
                .filter {
                    it.kind == ElementKind.METHOD
                            && !it.hasAnyOf(PRIVATE, ABSTRACT, STATIC)
                            && !it.hasAnnotation(Ignore::class)
                }
                .map { MoreElements.asExecutable(it) }

        val getterCandidates = methods.filter {
            it.parameters.size == 0 && it.returnType.kind != TypeKind.VOID
        }

        val setterCandidates = methods.filter {
            it.parameters.size == 1 && it.returnType.kind == TypeKind.VOID
        }

        assignGetters(fields, getterCandidates)
        assignSetters(fields, setterCandidates)

        val pojo = Pojo(element, declaredType, fields)
        return pojo
    }

    private fun assignGetters(fields: List<Field>, getterCandidates: List<ExecutableElement>) {
        fields.forEach { field ->
            val success = chooseAssignment(field = field,
                    candidates = getterCandidates,
                    nameVariations = field.getterNameWithVariations,
                    getType = { method ->
                        method.returnType
                    },
                    assignFromField = {
                        field.getter = FieldGetter(
                                name = field.name,
                                type = field.type,
                                callType = CallType.FIELD,
                                columnAdapter = context.typeAdapterStore
                                        .findColumnTypeAdapter(field.type))
                    },
                    assignFromMethod = { match ->
                        field.getter = FieldGetter(
                                name = match.simpleName.toString(),
                                type = match.returnType,
                                callType = CallType.METHOD,
                                columnAdapter = context.typeAdapterStore
                                        .findColumnTypeAdapter(match.returnType))
                    },
                    reportAmbiguity = { matching ->
                        context.logger.e(field.element,
                                ProcessorErrors.tooManyMatchingGetters(field, matching))
                    })
            context.checker.check(success, field.element, CANNOT_FIND_GETTER_FOR_FIELD)
        }
    }

    private fun assignSetters(fields: List<Field>, setterCandidates: List<ExecutableElement>) {
        fields.forEach { field ->
            val success = chooseAssignment(field = field,
                    candidates = setterCandidates,
                    nameVariations = field.setterNameWithVariations,
                    getType = { method ->
                        method.parameters.first().asType()
                    },
                    assignFromField = {
                        field.setter = FieldSetter(
                                name = field.name,
                                type = field.type,
                                callType = CallType.FIELD,
                                columnAdapter = context.typeAdapterStore
                                        .findColumnTypeAdapter(field.type))
                    },
                    assignFromMethod = { match ->
                        val paramType = match.parameters.first().asType()
                        field.setter = FieldSetter(
                                name = match.simpleName.toString(),
                                type = paramType,
                                callType = CallType.METHOD,
                                columnAdapter = context.typeAdapterStore
                                        .findColumnTypeAdapter(paramType))
                    },
                    reportAmbiguity = { matching ->
                        context.logger.e(field.element,
                                ProcessorErrors.tooManyMatchingSetter(field, matching))
                    })
            context.checker.check(success, field.element, CANNOT_FIND_SETTER_FOR_FIELD)
        }
    }

    /**
     * Finds a setter/getter from available list of methods.
     * It returns true if assignment is successful, false otherwise.
     * At worst case, it sets to the field as if it is accessible so that the rest of the
     * compilation can continue.
     */
    private fun chooseAssignment(field: Field, candidates: List<ExecutableElement>,
                                 nameVariations : List<String>,
                                 getType : (ExecutableElement) -> TypeMirror,
                                 assignFromField: () -> Unit,
                                 assignFromMethod: (ExecutableElement) -> Unit,
                                 reportAmbiguity: (List<String>) -> Unit): Boolean {
        if (field.element.hasAnyOf(PUBLIC)) {
            assignFromField()
            return true
        }
        val types = context.processingEnv.typeUtils
        val matching = candidates
                .filter {
                    types.isSameType(field.element.asType(), getType(it))
                            && field.nameWithVariations.contains(it.simpleName.toString())
                            || nameVariations.contains(it.simpleName.toString())
                }
                .groupBy {
                    if (it.hasAnyOf(PUBLIC)) PUBLIC else PROTECTED
                }
        if (matching.isEmpty()) {
            // we always assign to avoid NPEs in the rest of the compilation.
            assignFromField()
            // if field is not private, assume it works (if we are on the same package).
            // if not, compiler will tell, we didn't have any better alternative anyways.
            return !field.element.hasAnyOf(PRIVATE)
        }
        val match = verifyAndChooseOneFrom(matching[PUBLIC], reportAmbiguity) ?:
                verifyAndChooseOneFrom(matching[PROTECTED], reportAmbiguity)
        if (match == null) {
            assignFromField()
            return false
        } else {
            assignFromMethod(match)
            return true
        }
    }

    private fun verifyAndChooseOneFrom(candidates: List<ExecutableElement>?,
                                       reportAmbiguity: (List<String>) -> Unit)
            : ExecutableElement? {
        if (candidates == null) {
            return null
        }
        if (candidates.size > 1) {
            reportAmbiguity(candidates.map { it.simpleName.toString() })
        }
        return candidates.first()
    }
}
