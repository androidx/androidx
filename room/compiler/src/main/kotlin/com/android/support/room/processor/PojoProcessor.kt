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

import com.android.support.room.Relation
import com.android.support.room.ColumnInfo
import com.android.support.room.Decompose
import com.android.support.room.Ignore
import com.android.support.room.ext.getAllFieldsIncludingPrivateSupers
import com.android.support.room.ext.getAnnotationValue
import com.android.support.room.ext.getAsString
import com.android.support.room.ext.getAsStringList
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.ext.isCollection
import com.android.support.room.ext.toClassType
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_TYPE
import com.android.support.room.processor.ProcessorErrors.POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
import com.android.support.room.vo.CallType
import com.android.support.room.vo.Field
import com.android.support.room.vo.FieldGetter
import com.android.support.room.vo.DecomposedField
import com.android.support.room.vo.Entity
import com.android.support.room.vo.FieldSetter
import com.android.support.room.vo.Pojo
import com.google.auto.common.AnnotationMirrors
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
import javax.lang.model.element.VariableElement
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
    companion object {
        val PROCESSED_ANNOTATIONS = listOf(ColumnInfo::class, Decompose::class,
                    Relation::class)
    }
    fun process(): Pojo {
        // TODO handle recursion: b/35980205
        val declaredType = MoreTypes.asDeclared(element.asType())
        // TODO handle conflicts with super: b/35568142
        val allFields = element.getAllFieldsIncludingPrivateSupers(context.processingEnv)
                .filter {
                    !it.hasAnnotation(Ignore::class) && !it.hasAnyOf(Modifier.STATIC)
                }
                .groupBy { field ->
                    context.checker.check(
                            PROCESSED_ANNOTATIONS.count { field.hasAnnotation(it) } < 2, field,
                            ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_POJO_FIELD_ANNOTATION
                    )
                    if (field.hasAnnotation(Decompose::class)) {
                        Decompose::class
                    } else if (field.hasAnnotation(Relation::class)) {
                        Relation::class
                    } else {
                        null
                    }
                }
        val myFields = allFields[null]
                ?.map {
                    FieldProcessor(
                            baseContext = context,
                            containing = declaredType,
                            element = it,
                            bindingScope = bindingScope,
                            fieldParent = parent).process()
                } ?: emptyList()

        val decomposedFields = allFields[Decompose::class]
                ?.map {
                    processDecomposedField(declaredType, it)
                } ?: emptyList()
        val subFields = decomposedFields.flatMap { it.pojo.fields }

        val fields = myFields + subFields

        val myRelationsList = allFields[Relation::class]
                ?.map {
                    processRelationField(fields, declaredType, it)
                }
                ?.filterNotNull() ?: emptyList()

        val subRelations = decomposedFields.flatMap { it.pojo.relations }

        val relations = myRelationsList + subRelations

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

        decomposedFields.forEach {
            assignGetter(it.field, getterCandidates)
            assignSetter(it.field, setterCandidates)
        }

        myRelationsList.forEach {
            assignGetter(it.field, getterCandidates)
            assignSetter(it.field, setterCandidates)
        }

        val pojo = Pojo(element = element,
                type = declaredType,
                fields = fields,
                decomposedFields = decomposedFields,
                relations = relations)
        return pojo
    }

    private fun processDecomposedField(declaredType: DeclaredType?, it: Element): DecomposedField {
        val fieldPrefix = it.getAnnotationValue(Decompose::class.java, "prefix")
                ?.toString() ?: ""
        val inheritedPrefix = parent?.prefix ?: ""
        val decomposedField = Field(
                it,
                it.simpleName.toString(),
                type = context.processingEnv.typeUtils.asMemberOf(declaredType, it),
                affinity = null,
                parent = parent)
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

    private fun processRelationField(myFields : List<Field>, container: DeclaredType?,
                                     relationElement: VariableElement)
            : com.android.support.room.vo.Relation? {
        val annotation = MoreElements.getAnnotationMirror(relationElement, Relation::class.java)
                .orNull()!!
        val parentColumnInput = AnnotationMirrors.getAnnotationValue(annotation, "parentColumn")
                .getAsString("") ?: ""

        val parentField = myFields.firstOrNull {
            it.columnName == parentColumnInput
        }
        if (parentField == null) {
            context.logger.e(relationElement,
                    ProcessorErrors.relationCannotFindParentEntityField(
                            entityName = element.qualifiedName.toString(),
                            columnName = parentColumnInput,
                            availableColumns = myFields.map { it.columnName }))
            return null
        }
        // parse it as an entity.
        val asMember = MoreTypes
                .asMemberOf(context.processingEnv.typeUtils, container, relationElement)
        if (asMember.kind == TypeKind.ERROR) {
            context.logger.e(ProcessorErrors.CANNOT_FIND_TYPE, element)
            return null
        }
        val declared = MoreTypes.asDeclared(asMember)
        if (!declared.isCollection()) {
            context.logger.e(relationElement, ProcessorErrors.RELATION_NOT_COLLECTION)
            return null
        }
        val typeArg = declared.typeArguments.first()
        if (typeArg.kind == TypeKind.ERROR) {
            context.logger.e(MoreTypes.asTypeElement(typeArg), CANNOT_FIND_TYPE)
            return null
        }
        val typeArgElement = MoreTypes.asTypeElement(typeArg)
        val entityClassInput = AnnotationMirrors
                .getAnnotationValue(annotation, "entity").toClassType()
        val pojo : Pojo
        val entity : Entity
        if (entityClassInput == null
                || MoreTypes.isTypeOf(Any::class.java, entityClassInput)) {
            entity = EntityProcessor(context, typeArgElement).process()
            pojo = entity
        } else {
            entity = EntityProcessor(context, MoreTypes.asTypeElement(entityClassInput)).process()
            pojo = PojoProcessor(baseContext = context,
                    element = typeArgElement,
                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                    parent = parent).process()
        }
        // now find the field in the entity.
        val entityColumnInput = AnnotationMirrors.getAnnotationValue(annotation, "entityColumn")
                .getAsString() ?: ""
        val entityField = entity.fields.firstOrNull {
            it.columnName == entityColumnInput
        }

        if (entityField == null) {
            context.logger.e(relationElement,
                    ProcessorErrors.relationCannotFindEntityField(
                            entityName = entity.typeName.toString(),
                            columnName = entityColumnInput,
                            availableColumns = entity.fields.map { it.columnName }))
            return null
        }

        val field = Field(
                element = relationElement,
                name = relationElement.simpleName.toString(),
                type = context.processingEnv.typeUtils.asMemberOf(container, relationElement),
                affinity = null,
                parent = parent)

        val projection = AnnotationMirrors.getAnnotationValue(annotation, "projection")
                .getAsStringList()
        if(projection.isNotEmpty()) {
            val missingColumns = projection.filterNot { columnName ->
                entity.fields.any { columnName == it.columnName }
            }
            if (missingColumns.isNotEmpty()) {
                context.logger.e(relationElement,
                        ProcessorErrors.relationBadProject(entity.typeName.toString(),
                                missingColumns, entity.fields.map { it.columnName }))
            }
        }

        // if types don't match, row adapter prints a warning
        return com.android.support.room.vo.Relation(
                entity = entity,
                pojo = pojo,
                field = field,
                parentField = parentField,
                entityField = entityField,
                projection = projection
        )
    }

    private fun assignGetters(fields: List<Field>, getterCandidates: List<ExecutableElement>) {
        fields.forEach { field ->
            assignGetter(field, getterCandidates)
        }
    }

    private fun assignGetter(field: Field, getterCandidates: List<ExecutableElement>) {
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

    private fun assignSetters(fields: List<Field>, setterCandidates: List<ExecutableElement>) {
        fields.forEach { field ->
            assignSetter(field, setterCandidates)
        }
    }

    private fun assignSetter(field: Field, setterCandidates: List<ExecutableElement>) {
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
