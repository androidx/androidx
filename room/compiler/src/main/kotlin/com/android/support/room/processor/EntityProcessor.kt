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
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Entity
import com.android.support.room.vo.Field
import com.android.support.room.vo.FieldGetter
import com.android.support.room.vo.FieldSetter
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind

class EntityProcessor(val context: Context) {
    val fieldParser = FieldProcessor(context)

    fun parse(element: TypeElement): Entity {
        context.checker.hasAnnotation(element, com.android.support.room.Entity::class,
                ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY)
        val declaredType = MoreTypes.asDeclared(element.asType())
        val allMembers = context.processingEnv.elementUtils.getAllMembers(element)
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
        val annotation = MoreElements.getAnnotationMirror(element,
                com.android.support.room.Entity::class.java).orNull()
        val tableName : String
        if (annotation != null) {
            val annotationValue = AnnotationMirrors
                    .getAnnotationValue(annotation, "tableName").value.toString()
            if (annotationValue == "") {
                tableName = element.simpleName.toString()
            } else {
                tableName = annotationValue
            }
        } else {
            tableName = element.simpleName.toString()
        }
        context.checker.notBlank(tableName, element,
                ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY)
        val entity = Entity(element, tableName, declaredType, fields)
        context.checker.check(entity.primaryKeys.isNotEmpty(), element,
                ProcessorErrors.MISSING_PRIMARY_KEY)
        return entity
    }

    private fun assignGetters(fields: List<Field>, getterCandidates: List<ExecutableElement>) {
        val types = context.processingEnv.typeUtils

        fields.forEach { field ->
            if (!field.element.hasAnyOf(PRIVATE)) {
                field.getter = FieldGetter(
                        name = field.name,
                        type = field.type,
                        callType = CallType.FIELD,
                        columnAdapter = context.typeAdapterStore.findColumnTypeAdapter(field.type))
            } else {
                val matching = getterCandidates
                        .filter {
                            types.isSameType(field.element.asType(), it.returnType)
                                    && field.nameWithVariations.contains(it.simpleName.toString())
                                    || field.getterNameWithVariations
                                    .contains(it.simpleName.toString())
                        }
                context.checker.check(matching.isNotEmpty(), field.element,
                        ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD)
                context.checker.check(matching.size < 2, field.element,
                        ProcessorErrors.tooManyMatchingGetters(field,
                                matching.map { it.simpleName.toString() }))
                val match = matching.firstOrNull()
                if (match == null) {
                    // just assume we can set it. the error will block javac anyways.
                    field.getter = FieldGetter(
                            name = field.name,
                            type = field.type,
                            callType = CallType.FIELD,
                            columnAdapter = context.typeAdapterStore
                                    .findColumnTypeAdapter(field.type))
                } else {
                    field.getter = FieldGetter(
                            name = match.simpleName.toString(),
                            type = match.returnType,
                            callType = CallType.METHOD,
                            columnAdapter = context.typeAdapterStore
                                    .findColumnTypeAdapter(match.returnType))
                }
            }
        }
    }

    private fun assignSetters(fields: List<Field>, setterCandidates: List<ExecutableElement>) {
        val types = context.processingEnv.typeUtils

        fields.forEach { field ->
            if (!field.element.hasAnyOf(PRIVATE)) {
                field.setter = FieldSetter(
                        name = field.name,
                        type = field.type,
                        callType = CallType.FIELD,
                        columnAdapter = context.typeAdapterStore.findColumnTypeAdapter(field.type))
            } else {
                val matching = setterCandidates
                        .filter {
                            types.isSameType(field.element.asType(), it.parameters.first().asType())
                                    && field.nameWithVariations.contains(it.simpleName.toString())
                                    || field.setterNameWithVariations
                                    .contains(it.simpleName.toString())
                        }
                context.checker.check(matching.isNotEmpty(), field.element,
                        ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD)
                context.checker.check(matching.size < 2, field.element,
                        ProcessorErrors.tooManyMatchingSetter(field,
                                matching.map { it.simpleName.toString() }))
                val match = matching.firstOrNull()
                if (match == null) {
                    // default to field setter
                    field.setter = FieldSetter(
                            name = field.name,
                            type = field.type,
                            callType = CallType.FIELD,
                            columnAdapter = context.typeAdapterStore
                                    .findColumnTypeAdapter(field.type))
                } else {
                    val paramType = match.parameters.first().asType()
                    field.setter = FieldSetter(
                            name = match.simpleName.toString(),
                            type = paramType,
                            callType = CallType.METHOD,
                            columnAdapter = context.typeAdapterStore
                                    .findColumnTypeAdapter(paramType))
                }
            }
        }
    }
}
