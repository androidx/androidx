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

import com.android.support.room.ColumnInfo
import com.android.support.room.Decompose
import com.android.support.room.Ignore
import com.android.support.room.ext.getAllFieldsIncludingPrivateSupers
import com.android.support.room.ext.getAnnotationValue
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD
import com.android.support.room.processor.ProcessorErrors.FIELD_WITH_DECOMPOSE_AND_COLUMN_INFO
import com.android.support.room.processor.ProcessorErrors.POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Field
import com.android.support.room.vo.FieldGetter
import com.android.support.room.vo.DecomposedField
import com.android.support.room.vo.FieldSetter
import com.android.support.room.vo.Pojo
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/**
 * Processes any class as if it is a Pojo.
 */
class PojoProcessor(baseContext: Context, val element: TypeElement,
                    val bindingScope: FieldProcessor.BindingScope,
                    val parent: DecomposedField?) {
    val context = baseContext.fork(element)
    fun process(): Pojo {
        val declaredType = MoreTypes.asDeclared(element.asType())
        // TODO handle conflicts with super: b/35568142
        val allFields = element.getAllFieldsIncludingPrivateSupers(context.processingEnv)
                .filter {
                    !it.hasAnnotation(Ignore::class) && !it.hasAnyOf(Modifier.STATIC)
                }
        val myFields = allFields
                .filterNot { it.hasAnnotation(Decompose::class) }
                .map {
                    FieldProcessor(
                            baseContext = context,
                            containing = declaredType,
                            element = it,
                            bindingScope = bindingScope,
                            fieldParent = parent).process()
                }
        val decomposedFields = allFields
                .filter { it.hasAnnotation(Decompose::class) }
                .map {
                    processDecomposedField(declaredType, it)
                }
        val subFields = decomposedFields.flatMap { it.pojo.fields }

        val fields = myFields + subFields
        fields.groupBy { it.columnName }
                .filter { it.value.size > 1 }
                .forEach {
                    context.logger.e(element, ProcessorErrors.pojoDuplicateFieldNames(
                            it.key, it.value.map(Field::getPath)
                    ))
                    it.value.forEach {
                        context.logger.e(it.element, POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME)
                    }
                }
        val methods = MoreElements.getLocalAndInheritedMethods(element,
                context.processingEnv.elementUtils)
                .filter {
                    !it.hasAnyOf(PRIVATE, ABSTRACT, STATIC)
                            && !it.hasAnnotation(Ignore::class)
                }
                .map { MoreElements.asExecutable(it) }

        val getterCandidates = methods.filter {
            it.parameters.size == 0 && it.returnType.kind != TypeKind.VOID
        }

        val setterCandidates = methods.filter {
            it.parameters.size == 1 && it.returnType.kind == TypeKind.VOID
        }

        assignGetters(myFields, getterCandidates)
        assignSetters(myFields, setterCandidates)
        val decomposedsAsFields = decomposedFields.map { it.field }
        assignGetters(decomposedsAsFields, getterCandidates)
        assignSetters(decomposedsAsFields, setterCandidates)
        val pojo = Pojo(element = element,
                type = declaredType,
                fields = fields,
                decomposedFields = decomposedFields)
        return pojo
    }

    private fun processDecomposedField(declaredType: DeclaredType?, it: Element): DecomposedField {
        context.checker.check(!it.hasAnnotation(ColumnInfo::class), it,
                FIELD_WITH_DECOMPOSE_AND_COLUMN_INFO)
        val fieldPrefix = it.getAnnotationValue(Decompose::class.java, "prefix")
                ?.toString() ?: ""
        val inheritedPrefix = parent?.prefix ?: ""
        val decomposedField = Field(
                it,
                it.simpleName.toString(),
                type = context.processingEnv.typeUtils.asMemberOf(declaredType, it),
                affinity = null,
                parent = parent,
                primaryKey = false)
        val subParent = DecomposedField(
                field = decomposedField,
                prefix = inheritedPrefix + fieldPrefix,
                parent = parent)
        val asVariable = MoreElements.asVariable(it)
        subParent.pojo = PojoProcessor(baseContext = context.fork(it),
                element = MoreTypes.asTypeElement(asVariable.asType()),
                bindingScope = bindingScope,
                parent = subParent).process()
        return subParent
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
                                callType = CallType.FIELD)
                    },
                    assignFromMethod = { match ->
                        field.getter = FieldGetter(
                                name = match.simpleName.toString(),
                                type = match.returnType,
                                callType = CallType.METHOD)
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
                                callType = CallType.FIELD)
                    },
                    assignFromMethod = { match ->
                        val paramType = match.parameters.first().asType()
                        field.setter = FieldSetter(
                                name = match.simpleName.toString(),
                                type = paramType,
                                callType = CallType.METHOD)
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
                                 nameVariations: List<String>,
                                 getType: (ExecutableElement) -> TypeMirror,
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
