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

package com.android.support.room.processor

import com.android.support.room.Ignore
import com.android.support.room.errors.ElementBoundException
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.preconditions.Checks
import com.android.support.room.vo.*
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.*
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind

class EntityParser(val roundEnv: RoundEnvironment,
                   val processingEnvironment: ProcessingEnvironment) {
    val fieldParser = FieldParser(roundEnv, processingEnvironment)

    fun parse(element: TypeElement): Entity {
        val declaredType = MoreTypes.asDeclared(element.asType())
        val allMembers = processingEnvironment.elementUtils.getAllMembers(element)
        val fields = allMembers
                .filter {
                    it.kind == ElementKind.FIELD
                            && !it.hasAnnotation(Ignore::class)
                            && !it.hasAnyOf(Modifier.STATIC)
                }
                .map { fieldParser.parse(declaredType, it) }

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
        val entity = Entity(TypeName.get(declaredType), fields)
        Checks.check(entity.primaryKeys.isNotEmpty(), element, ProcessorErrors.MISSING_PRIMARY_KEY)
        return entity
    }

    private fun assignGetters(fields: List<Field>, getterCandidates: List<ExecutableElement>) {
        val types = processingEnvironment.typeUtils

        fields.forEach { field ->
            if (!field.element.hasAnyOf(PRIVATE)) {
                field.getter = FieldGetter(field.name, CallType.FIELD)
            } else {
                val matching = getterCandidates
                        .filter {
                            types.isSameType(field.element.asType(), it.returnType)
                                    && field.nameWithVariations.contains(it.simpleName.toString())
                                    || field.getterNameWithVariations
                                    .contains(it.simpleName.toString())
                        }
                if (matching.isEmpty()) {
                    throw ElementBoundException(field.element,
                            ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD)
                }
                if (matching.size > 1) {
                    throw ElementBoundException(field.element,
                            ProcessorErrors.tooManyMatchingGetters(field,
                                    matching.map { it.simpleName.toString() }))
                }
                val match = matching.first()
                field.getter = FieldGetter(match.simpleName.toString(), CallType.METHOD)
            }
        }
    }

    private fun assignSetters(fields: List<Field>, setterCandidates: List<ExecutableElement>) {
        val types = processingEnvironment.typeUtils

        fields.forEach { field ->
            if (!field.element.hasAnyOf(PRIVATE)) {
                field.setter = FieldSetter(field.name, CallType.FIELD)
            } else {
                val matching = setterCandidates
                        .filter {
                            types.isSameType(field.element.asType(), it.parameters.first().asType())
                                    && field.nameWithVariations.contains(it.simpleName.toString())
                                    || field.setterNameWithVariations
                                    .contains(it.simpleName.toString())
                        }
                if (matching.isEmpty()) {
                    throw ElementBoundException(field.element,
                            ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD)
                }
                if (matching.size > 1) {
                    throw ElementBoundException(field.element,
                            ProcessorErrors.tooManyMatchingSetter(field,
                                    matching.map { it.simpleName.toString() }))
                }
                val match = matching.first()
                field.setter = FieldSetter(match.simpleName.toString(), CallType.METHOD)
            }
        }
    }
}
