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

package androidx.room.processor

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.isCollection
import androidx.room.compiler.processing.isVoid
import androidx.room.ext.isNotVoid
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD
import androidx.room.processor.ProcessorErrors.POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
import androidx.room.processor.autovalue.AutoValuePojoProcessorDelegate
import androidx.room.processor.cache.Cache
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Entity
import androidx.room.vo.EntityOrView
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import androidx.room.vo.Pojo
import androidx.room.vo.PojoMethod
import androidx.room.vo.Warning
import androidx.room.vo.columnNames
import androidx.room.vo.findFieldByColumnName
import com.google.auto.value.AutoValue

/**
 * Processes any class as if it is a Pojo.
 */
class PojoProcessor private constructor(
    baseContext: Context,
    val element: XTypeElement,
    val bindingScope: FieldProcessor.BindingScope,
    val parent: EmbeddedField?,
    val referenceStack: LinkedHashSet<String> = LinkedHashSet(),
    private val delegate: Delegate
) {
    val context = baseContext.fork(element)

    companion object {
        val PROCESSED_ANNOTATIONS = listOf(ColumnInfo::class, Embedded::class, Relation::class)

        val TARGET_METHOD_ANNOTATIONS = arrayOf(
            PrimaryKey::class, ColumnInfo::class,
            Embedded::class, Relation::class
        )

        fun createFor(
            context: Context,
            element: XTypeElement,
            bindingScope: FieldProcessor.BindingScope,
            parent: EmbeddedField?,
            referenceStack: LinkedHashSet<String> = LinkedHashSet()
        ): PojoProcessor {
            val (pojoElement, delegate) = if (element.hasAnnotation(AutoValue::class)) {
                val processingEnv = context.processingEnv
                val autoValueGeneratedTypeName =
                    AutoValuePojoProcessorDelegate.getGeneratedClassName(element)
                val autoValueGeneratedElement =
                    processingEnv.findTypeElement(autoValueGeneratedTypeName)
                if (autoValueGeneratedElement != null) {
                    autoValueGeneratedElement to AutoValuePojoProcessorDelegate(context, element)
                } else {
                    context.reportMissingType(autoValueGeneratedTypeName)
                    element to EmptyDelegate
                }
            } else {
                element to DefaultDelegate(context)
            }

            return PojoProcessor(
                baseContext = context,
                element = pojoElement,
                bindingScope = bindingScope,
                parent = parent,
                referenceStack = referenceStack,
                delegate = delegate
            )
        }
    }

    fun process(): Pojo {
        return context.cache.pojos.get(Cache.PojoKey(element, bindingScope, parent)) {
            referenceStack.add(element.qualifiedName)
            try {
                doProcess()
            } finally {
                referenceStack.remove(element.qualifiedName)
            }
        }
    }

    private fun doProcess(): Pojo {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
            return delegate.createPojo(
                element = element,
                declaredType = element.type,
                fields = emptyList(),
                embeddedFields = emptyList(),
                relations = emptyList(),
                constructor = null
            )
        }
        delegate.onPreProcess(element)

        val declaredType = element.type
        // TODO handle conflicts with super: b/35568142
        val allFields = element.getAllFieldsIncludingPrivateSupers()
            .filter {
                !it.hasAnnotation(Ignore::class) &&
                    !it.isStatic() &&
                    (
                        !it.isTransient() ||
                            it.hasAnyAnnotation(ColumnInfo::class, Embedded::class, Relation::class)
                        )
            }
            .groupBy { field ->
                context.checker.check(
                    PROCESSED_ANNOTATIONS.count { field.hasAnnotation(it) } < 2, field,
                    ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_POJO_FIELD_ANNOTATION
                )
                if (field.hasAnnotation(Embedded::class)) {
                    Embedded::class
                } else if (field.hasAnnotation(Relation::class)) {
                    Relation::class
                } else {
                    null
                }
            }

        val ignoredColumns =
            element.getAnnotation(androidx.room.Entity::class)?.value?.ignoredColumns?.toSet()
                ?: emptySet()
        val fieldBindingErrors = mutableMapOf<Field, String>()
        val unfilteredMyFields = allFields[null]
            ?.map {
                FieldProcessor(
                    baseContext = context,
                    containing = declaredType,
                    element = it,
                    bindingScope = bindingScope,
                    fieldParent = parent,
                    onBindingError = { field, errorMsg ->
                        fieldBindingErrors[field] = errorMsg
                    }
                ).process()
            } ?: emptyList()
        val myFields = unfilteredMyFields.filterNot { ignoredColumns.contains(it.columnName) }
        myFields.forEach { field ->
            fieldBindingErrors[field]?.let {
                context.logger.e(field.element, it)
            }
        }
        val unfilteredEmbeddedFields =
            allFields[Embedded::class]
                ?.mapNotNull {
                    processEmbeddedField(declaredType, it)
                }
                ?: emptyList()
        val embeddedFields =
            unfilteredEmbeddedFields.filterNot { ignoredColumns.contains(it.field.columnName) }

        val subFields = embeddedFields.flatMap { it.pojo.fields }
        val fields = myFields + subFields

        val unfilteredCombinedFields =
            unfilteredMyFields + unfilteredEmbeddedFields.map { it.field }
        val missingIgnoredColumns = ignoredColumns.filterNot { ignoredColumn ->
            unfilteredCombinedFields.any { it.columnName == ignoredColumn }
        }
        context.checker.check(
            missingIgnoredColumns.isEmpty(), element,
            ProcessorErrors.missingIgnoredColumns(missingIgnoredColumns)
        )

        val myRelationsList = allFields[Relation::class]
            ?.mapNotNull {
                processRelationField(fields, declaredType, it)
            }
            ?: emptyList()

        val subRelations = embeddedFields.flatMap { it.pojo.relations }
        val relations = myRelationsList + subRelations

        fields.groupBy { it.columnName }
            .filter { it.value.size > 1 }
            .forEach {
                context.logger.e(
                    element,
                    ProcessorErrors.pojoDuplicateFieldNames(
                        it.key, it.value.map(Field::getPath)
                    )
                )
                it.value.forEach {
                    context.logger.e(it.element, POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME)
                }
            }

        val methods = element.getAllNonPrivateInstanceMethods()
            .asSequence()
            .filter {
                !it.isAbstract() && !it.hasAnnotation(Ignore::class)
            }.map {
                PojoMethodProcessor(
                    context = context,
                    element = it,
                    owner = declaredType
                ).process()
            }.toList()

        val getterCandidates = methods.filter {
            it.element.parameters.size == 0 && it.resolvedType.returnType.isNotVoid()
        }

        val setterCandidates = methods.filter {
            it.element.parameters.size == 1 && it.resolvedType.returnType.isVoid()
        }

        // don't try to find a constructor for binding to statement.
        val constructor = if (bindingScope == FieldProcessor.BindingScope.BIND_TO_STMT) {
            // we don't need to construct this POJO.
            null
        } else {
            chooseConstructor(myFields, embeddedFields, relations)
        }

        assignGetters(myFields, getterCandidates)
        assignSetters(myFields, setterCandidates, constructor)

        embeddedFields.forEach {
            assignGetter(it.field, getterCandidates)
            assignSetter(it.field, setterCandidates, constructor)
        }

        myRelationsList.forEach {
            assignGetter(it.field, getterCandidates)
            assignSetter(it.field, setterCandidates, constructor)
        }

        return delegate.createPojo(
            element, declaredType, fields, embeddedFields, relations,
            constructor
        )
    }

    private fun chooseConstructor(
        myFields: List<Field>,
        embedded: List<EmbeddedField>,
        relations: List<androidx.room.vo.Relation>
    ): Constructor? {
        val constructors = delegate.findConstructors(element)
        val fieldMap = myFields.associateBy { it.name }
        val embeddedMap = embedded.associateBy { it.field.name }
        val relationMap = relations.associateBy { it.field.name }
        // list of param names -> matched params pairs for each failed constructor
        val failedConstructors = arrayListOf<FailedConstructor>()
        val goodConstructors = constructors.map { constructor ->
            val parameterNames = constructor.parameters.map { it.name }
            val params = constructor.parameters.mapIndexed param@{ index, param ->
                val paramName = parameterNames[index]
                val paramType = param.type

                val matches = fun(field: Field?): Boolean {
                    return if (field == null) {
                        false
                    } else if (!field.nameWithVariations.contains(paramName)) {
                        false
                    } else {
                        // see: b/69164099
                        field.type.isAssignableFromWithoutVariance(paramType)
                    }
                }

                val exactFieldMatch = fieldMap[paramName]
                if (matches(exactFieldMatch)) {
                    return@param Constructor.Param.FieldParam(exactFieldMatch!!)
                }
                val exactEmbeddedMatch = embeddedMap[paramName]
                if (matches(exactEmbeddedMatch?.field)) {
                    return@param Constructor.Param.EmbeddedParam(exactEmbeddedMatch!!)
                }
                val exactRelationMatch = relationMap[paramName]
                if (matches(exactRelationMatch?.field)) {
                    return@param Constructor.Param.RelationParam(exactRelationMatch!!)
                }

                val matchingFields = myFields.filter {
                    matches(it)
                }
                val embeddedMatches = embedded.filter {
                    matches(it.field)
                }
                val relationMatches = relations.filter {
                    matches(it.field)
                }
                when (matchingFields.size + embeddedMatches.size + relationMatches.size) {
                    0 -> null
                    1 -> when {
                        matchingFields.isNotEmpty() ->
                            Constructor.Param.FieldParam(matchingFields.first())
                        embeddedMatches.isNotEmpty() ->
                            Constructor.Param.EmbeddedParam(embeddedMatches.first())
                        else ->
                            Constructor.Param.RelationParam(relationMatches.first())
                    }
                    else -> {
                        context.logger.e(
                            param,
                            ProcessorErrors.ambiguousConstructor(
                                pojo = element.qualifiedName,
                                paramName = paramName,
                                matchingFields = matchingFields.map { it.getPath() } +
                                    embeddedMatches.map { it.field.getPath() } +
                                    relationMatches.map { it.field.getPath() }
                            )
                        )
                        null
                    }
                }
            }
            if (params.any { it == null }) {
                failedConstructors.add(FailedConstructor(constructor, parameterNames, params))
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                Constructor(constructor, params as List<Constructor.Param>)
            }
        }.filterNotNull()
        when {
            goodConstructors.isEmpty() -> {
                if (failedConstructors.isNotEmpty()) {
                    val failureMsg = failedConstructors.joinToString("\n") { entry ->
                        entry.log()
                    }
                    context.logger.e(
                        element,
                        ProcessorErrors.MISSING_POJO_CONSTRUCTOR +
                            "\nTried the following constructors but they failed to match:" +
                            "\n$failureMsg"
                    )
                }
                context.logger.e(element, ProcessorErrors.MISSING_POJO_CONSTRUCTOR)
                return null
            }
            goodConstructors.size > 1 -> {
                // if the Pojo is a Kotlin data class then pick its primary constructor. This is
                // better than picking the no-arg constructor and forcing users to define fields as
                // vars.
                val primaryConstructor =
                    element.findPrimaryConstructor()?.let { primary ->
                        goodConstructors.firstOrNull { candidate ->
                            candidate.element == primary
                        }
                    }
                if (primaryConstructor != null) {
                    return primaryConstructor
                }
                // if there is a no-arg constructor, pick it. Even though it is weird, easily happens
                // with kotlin data classes.
                val noArg = goodConstructors.firstOrNull { it.params.isEmpty() }
                if (noArg != null) {
                    context.logger.w(
                        Warning.DEFAULT_CONSTRUCTOR, element,
                        ProcessorErrors.TOO_MANY_POJO_CONSTRUCTORS_CHOOSING_NO_ARG
                    )
                    return noArg
                }
                goodConstructors.forEach {
                    context.logger.e(it.element, ProcessorErrors.TOO_MANY_POJO_CONSTRUCTORS)
                }
                return null
            }
            else -> return goodConstructors.first()
        }
    }

    private fun processEmbeddedField(
        declaredType: XType,
        variableElement: XFieldElement
    ): EmbeddedField? {
        val asMemberType = variableElement.asMemberOf(declaredType)
        val asTypeElement = asMemberType.typeElement
        if (asTypeElement == null) {
            context.logger.e(
                variableElement,
                ProcessorErrors.EMBEDDED_TYPES_MUST_BE_A_CLASS_OR_INTERFACE
            )
            return null
        }

        if (detectReferenceRecursion(asTypeElement)) {
            return null
        }

        val fieldPrefix = variableElement.getAnnotation(Embedded::class)?.value?.prefix ?: ""
        val inheritedPrefix = parent?.prefix ?: ""
        val embeddedField = Field(
            variableElement,
            variableElement.name,
            type = asMemberType,
            affinity = null,
            parent = parent
        )
        val subParent = EmbeddedField(
            field = embeddedField,
            prefix = inheritedPrefix + fieldPrefix,
            parent = parent
        )
        subParent.pojo = createFor(
            context = context.fork(variableElement),
            element = asTypeElement,
            bindingScope = bindingScope,
            parent = subParent,
            referenceStack = referenceStack
        ).process()
        return subParent
    }

    private fun processRelationField(
        myFields: List<Field>,
        container: XType,
        relationElement: XFieldElement
    ): androidx.room.vo.Relation? {
        val annotation = relationElement.getAnnotation(Relation::class)!!

        val parentField = myFields.firstOrNull {
            it.columnName == annotation.value.parentColumn
        }
        if (parentField == null) {
            context.logger.e(
                relationElement,
                ProcessorErrors.relationCannotFindParentEntityField(
                    entityName = element.qualifiedName,
                    columnName = annotation.value.parentColumn,
                    availableColumns = myFields.map { it.columnName }
                )
            )
            return null
        }
        // parse it as an entity.
        val asMember = relationElement.asMemberOf(container)
        val asType = if (asMember.isCollection()) {
            asMember.typeArguments.first().extendsBoundOrSelf()
        } else {
            asMember
        }
        val typeElement = asType.typeElement
        if (typeElement == null) {
            context.logger.e(
                relationElement,
                ProcessorErrors.RELATION_TYPE_MUST_BE_A_CLASS_OR_INTERFACE
            )
            return null
        }

        val entityClassInput = annotation.getAsType("entity")

        // do we need to decide on the entity?
        val inferEntity = (entityClassInput == null || entityClassInput.isTypeOf(Any::class))
        val entityElement = if (inferEntity) {
            typeElement
        } else {
            entityClassInput!!.typeElement
        }
        if (entityElement == null) {
            // this should not happen as we check for declared above but for compile time
            // null safety, it is still good to have this additional check here.
            context.logger.e(
                typeElement,
                ProcessorErrors.RELATION_TYPE_MUST_BE_A_CLASS_OR_INTERFACE
            )
            return null
        }

        if (detectReferenceRecursion(entityElement)) {
            return null
        }

        val entity = EntityOrViewProcessor(context, entityElement, referenceStack).process()

        // now find the field in the entity.
        val entityField = entity.findFieldByColumnName(annotation.value.entityColumn)
        if (entityField == null) {
            context.logger.e(
                relationElement,
                ProcessorErrors.relationCannotFindEntityField(
                    entityName = entity.typeName.toString(),
                    columnName = annotation.value.entityColumn,
                    availableColumns = entity.columnNames
                )
            )
            return null
        }

        // do we have a join entity?
        val junctionAnnotation = annotation.getAsAnnotationBox<Junction>("associateBy")
        val junctionClassInput = junctionAnnotation.getAsType("value")
        val junctionElement: XTypeElement? = if (junctionClassInput != null &&
            !junctionClassInput.isTypeOf(Any::class)
        ) {
            junctionClassInput.typeElement.also {
                if (it == null) {
                    context.logger.e(
                        relationElement,
                        ProcessorErrors.NOT_ENTITY_OR_VIEW
                    )
                }
            }
        } else {
            null
        }
        val junction = junctionElement?.let {
            val entityOrView = EntityOrViewProcessor(context, it, referenceStack).process()

            fun findAndValidateJunctionColumn(
                columnName: String,
                onMissingField: () -> Unit
            ): Field? {
                val field = entityOrView.findFieldByColumnName(columnName)
                if (field == null) {
                    onMissingField()
                    return null
                }
                if (entityOrView is Entity) {
                    // warn about not having indices in the junction columns, only considering
                    // 1st column in composite primary key and indices, since order matters.
                    val coveredColumns = entityOrView.primaryKey.fields.columnNames.first() +
                        entityOrView.indices.map { it.columnNames.first() }
                    if (!coveredColumns.contains(field.columnName)) {
                        context.logger.w(
                            Warning.MISSING_INDEX_ON_JUNCTION, field.element,
                            ProcessorErrors.junctionColumnWithoutIndex(
                                entityName = entityOrView.typeName.toString(),
                                columnName = columnName
                            )
                        )
                    }
                }
                return field
            }

            val junctionParentColumn = if (junctionAnnotation.value.parentColumn.isNotEmpty()) {
                junctionAnnotation.value.parentColumn
            } else {
                parentField.columnName
            }
            val junctionParentField = findAndValidateJunctionColumn(
                columnName = junctionParentColumn,
                onMissingField = {
                    context.logger.e(
                        junctionElement,
                        ProcessorErrors.relationCannotFindJunctionParentField(
                            entityName = entityOrView.typeName.toString(),
                            columnName = junctionParentColumn,
                            availableColumns = entityOrView.columnNames
                        )
                    )
                }
            )

            val junctionEntityColumn = if (junctionAnnotation.value.entityColumn.isNotEmpty()) {
                junctionAnnotation.value.entityColumn
            } else {
                entityField.columnName
            }
            val junctionEntityField = findAndValidateJunctionColumn(
                columnName = junctionEntityColumn,
                onMissingField = {
                    context.logger.e(
                        junctionElement,
                        ProcessorErrors.relationCannotFindJunctionEntityField(
                            entityName = entityOrView.typeName.toString(),
                            columnName = junctionEntityColumn,
                            availableColumns = entityOrView.columnNames
                        )
                    )
                }
            )

            if (junctionParentField == null || junctionEntityField == null) {
                return null
            }

            androidx.room.vo.Junction(
                entity = entityOrView,
                parentField = junctionParentField,
                entityField = junctionEntityField
            )
        }

        val field = Field(
            element = relationElement,
            name = relationElement.name,
            type = relationElement.asMemberOf(container),
            affinity = null,
            parent = parent
        )

        val projection = if (annotation.value.projection.isEmpty()) {
            // we need to infer the projection from inputs.
            createRelationshipProjection(inferEntity, asType, entity, entityField, typeElement)
        } else {
            // make sure projection makes sense
            validateRelationshipProjection(annotation.value.projection, entity, relationElement)
            annotation.value.projection.asList()
        }
        // if types don't match, row adapter prints a warning
        return androidx.room.vo.Relation(
            entity = entity,
            pojoType = asType,
            field = field,
            parentField = parentField,
            entityField = entityField,
            junction = junction,
            projection = projection
        )
    }

    private fun validateRelationshipProjection(
        projectionInput: Array<String>,
        entity: EntityOrView,
        relationElement: XVariableElement
    ) {
        val missingColumns = projectionInput.toList() - entity.columnNames
        if (missingColumns.isNotEmpty()) {
            context.logger.e(
                relationElement,
                ProcessorErrors.relationBadProject(
                    entity.typeName.toString(),
                    missingColumns, entity.columnNames
                )
            )
        }
    }

    /**
     * Create the projection column list based on the relationship args.
     *
     *  if entity field in the annotation is not specified, it is the method return type
     *  if it is specified in the annotation:
     *       still check the method return type, if the same, use it
     *       if not, check to see if we can find a column Adapter, if so use the childField
     *       last resort, try to parse it as a pojo to infer it.
     */
    private fun createRelationshipProjection(
        inferEntity: Boolean,
        typeArg: XType,
        entity: EntityOrView,
        entityField: Field,
        typeArgElement: XTypeElement
    ): List<String> {
        return if (inferEntity || typeArg.typeName == entity.typeName) {
            entity.columnNames
        } else {
            val columnAdapter = context.typeAdapterStore.findCursorValueReader(typeArg, null)
            if (columnAdapter != null) {
                // nice, there is a column adapter for this, assume single column response
                listOf(entityField.name)
            } else {
                // last resort, it needs to be a pojo
                val pojo = createFor(
                    context = context,
                    element = typeArgElement,
                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                    parent = parent,
                    referenceStack = referenceStack
                ).process()
                pojo.columnNames
            }
        }
    }

    private fun detectReferenceRecursion(typeElement: XTypeElement): Boolean {
        if (referenceStack.contains(typeElement.qualifiedName)) {
            context.logger.e(
                typeElement,
                ProcessorErrors
                    .RECURSIVE_REFERENCE_DETECTED
                    .format(computeReferenceRecursionString(typeElement))
            )
            return true
        }
        return false
    }

    private fun computeReferenceRecursionString(typeElement: XTypeElement): String {
        val recursiveTailTypeName = typeElement.qualifiedName

        val referenceRecursionList = mutableListOf<String>()
        with(referenceRecursionList) {
            add(recursiveTailTypeName)
            addAll(referenceStack.toList().takeLastWhile { it != recursiveTailTypeName })
            add(recursiveTailTypeName)
        }

        return referenceRecursionList.joinToString(" -> ")
    }

    private fun assignGetters(fields: List<Field>, getterCandidates: List<PojoMethod>) {
        fields.forEach { field ->
            assignGetter(field, getterCandidates)
        }
    }

    private fun assignGetter(field: Field, getterCandidates: List<PojoMethod>) {
        val success = chooseAssignment(
            field = field,
            candidates = getterCandidates,
            nameVariations = field.getterNameWithVariations,
            getType = { method ->
                method.resolvedType.returnType
            },
            assignFromField = {
                field.getter = FieldGetter(
                    name = field.name,
                    type = field.type,
                    callType = CallType.FIELD
                )
            },
            assignFromMethod = { match ->
                field.getter = FieldGetter(
                    name = match.name,
                    type = match.resolvedType.returnType,
                    callType = CallType.METHOD
                )
            },
            reportAmbiguity = { matching ->
                context.logger.e(
                    field.element,
                    ProcessorErrors.tooManyMatchingGetters(field, matching)
                )
            }
        )
        context.checker.check(
            success || bindingScope == FieldProcessor.BindingScope.READ_FROM_CURSOR,
            field.element, CANNOT_FIND_GETTER_FOR_FIELD
        )
        if (success && !field.getter.type.isSameType(field.type)) {
            // getter's parameter type is not exactly the same as the field type.
            // put a warning and update the value statement binder.
            context.logger.w(
                warning = Warning.MISMATCHED_GETTER_TYPE,
                element = field.element,
                msg = ProcessorErrors.mismatchedGetter(
                    fieldName = field.name,
                    ownerType = element.type.typeName,
                    getterType = field.getter.type.typeName,
                    fieldType = field.typeName
                )
            )
            field.statementBinder = context.typeAdapterStore.findStatementValueBinder(
                input = field.getter.type,
                affinity = field.affinity
            )
        }
    }

    private fun assignSetters(
        fields: List<Field>,
        setterCandidates: List<PojoMethod>,
        constructor: Constructor?
    ) {
        fields.forEach { field ->
            assignSetter(field, setterCandidates, constructor)
        }
    }

    private fun assignSetter(
        field: Field,
        setterCandidates: List<PojoMethod>,
        constructor: Constructor?
    ) {
        if (constructor != null && constructor.hasField(field)) {
            field.setter = FieldSetter(
                name = field.name,
                type = field.type,
                callType = CallType.CONSTRUCTOR
            )
            return
        }
        val success = chooseAssignment(
            field = field,
            candidates = setterCandidates,
            nameVariations = field.setterNameWithVariations,
            getType = { method ->
                method.resolvedType.parameterTypes.first()
            },
            assignFromField = {
                field.setter = FieldSetter(
                    name = field.name,
                    type = field.type,
                    callType = CallType.FIELD
                )
            },
            assignFromMethod = { match ->
                val paramType = match.resolvedType.parameterTypes.first()
                field.setter = FieldSetter(
                    name = match.name,
                    type = paramType,
                    callType = CallType.METHOD
                )
            },
            reportAmbiguity = { matching ->
                context.logger.e(
                    field.element,
                    ProcessorErrors.tooManyMatchingSetter(field, matching)
                )
            }
        )
        context.checker.check(
            success || bindingScope == FieldProcessor.BindingScope.BIND_TO_STMT,
            field.element, CANNOT_FIND_SETTER_FOR_FIELD
        )
        if (success && !field.setter.type.isSameType(field.type)) {
            // setter's parameter type is not exactly the same as the field type.
            // put a warning and update the value reader adapter.
            context.logger.w(
                warning = Warning.MISMATCHED_SETTER_TYPE,
                element = field.element,
                msg = ProcessorErrors.mismatchedSetter(
                    fieldName = field.name,
                    ownerType = element.type.typeName,
                    setterType = field.setter.type.typeName,
                    fieldType = field.typeName
                )
            )
            field.cursorValueReader = context.typeAdapterStore.findCursorValueReader(
                output = field.setter.type,
                affinity = field.affinity
            )
        }
    }

    /**
     * Finds a setter/getter from available list of methods.
     * It returns true if assignment is successful, false otherwise.
     * At worst case, it sets to the field as if it is accessible so that the rest of the
     * compilation can continue.
     */
    private fun chooseAssignment(
        field: Field,
        candidates: List<PojoMethod>,
        nameVariations: List<String>,
        getType: (PojoMethod) -> XType,
        assignFromField: () -> Unit,
        assignFromMethod: (PojoMethod) -> Unit,
        reportAmbiguity: (List<String>) -> Unit
    ): Boolean {
        if (field.element.isPublic()) {
            assignFromField()
            return true
        }

        val matching = candidates
            .filter {
                // b/69164099
                field.type.isAssignableFromWithoutVariance(getType(it)) &&
                    (
                        field.nameWithVariations.contains(it.name) ||
                            nameVariations.contains(it.name)
                        )
            }
            .groupBy {
                it.element.isPublic()
            }
        if (matching.isEmpty()) {
            // we always assign to avoid NPEs in the rest of the compilation.
            assignFromField()
            // if field is not private, assume it works (if we are on the same package).
            // if not, compiler will tell, we didn't have any better alternative anyways.
            return !field.element.isPrivate()
        }
        // first try public ones, then try non-public
        val match = verifyAndChooseOneFrom(matching[true], reportAmbiguity)
            ?: verifyAndChooseOneFrom(matching[false], reportAmbiguity)
        if (match == null) {
            assignFromField()
            return false
        } else {
            assignFromMethod(match)
            return true
        }
    }

    private fun verifyAndChooseOneFrom(
        candidates: List<PojoMethod>?,
        reportAmbiguity: (List<String>) -> Unit
    ): PojoMethod? {
        if (candidates == null) {
            return null
        }
        if (candidates.size > 1) {
            reportAmbiguity(candidates.map { it.name })
        }
        return candidates.first()
    }

    interface Delegate {

        fun onPreProcess(element: XTypeElement)

        /**
         * Constructors are XExecutableElement rather than XConstrcutorElement to account for
         * factory methods.
         */
        fun findConstructors(element: XTypeElement): List<XExecutableElement>

        fun createPojo(
            element: XTypeElement,
            declaredType: XType,
            fields: List<Field>,
            embeddedFields: List<EmbeddedField>,
            relations: List<androidx.room.vo.Relation>,
            constructor: Constructor?
        ): Pojo
    }

    private class DefaultDelegate(private val context: Context) : Delegate {
        override fun onPreProcess(element: XTypeElement) {
            // Check that certain Room annotations with @Target(METHOD) are not used in the POJO
            // since it is not annotated with AutoValue.
            element.getAllMethods()
                .filter { it.hasAnyAnnotation(*TARGET_METHOD_ANNOTATIONS) }
                .forEach { method ->
                    val annotationName = TARGET_METHOD_ANNOTATIONS
                        .first { method.hasAnnotation(it) }
                        .java.simpleName
                    context.logger.e(
                        method,
                        ProcessorErrors.invalidAnnotationTarget(annotationName, method.kindName())
                    )
                }
        }

        override fun findConstructors(element: XTypeElement) = element.getConstructors().filterNot {
            it.hasAnnotation(Ignore::class) || it.isPrivate()
        }

        override fun createPojo(
            element: XTypeElement,
            declaredType: XType,
            fields: List<Field>,
            embeddedFields: List<EmbeddedField>,
            relations: List<androidx.room.vo.Relation>,
            constructor: Constructor?
        ): Pojo {
            return Pojo(
                element = element,
                type = declaredType,
                fields = fields,
                embeddedFields = embeddedFields,
                relations = relations,
                constructor = constructor
            )
        }
    }

    private object EmptyDelegate : Delegate {
        override fun onPreProcess(element: XTypeElement) {}

        override fun findConstructors(element: XTypeElement): List<XExecutableElement> = emptyList()

        override fun createPojo(
            element: XTypeElement,
            declaredType: XType,
            fields: List<Field>,
            embeddedFields: List<EmbeddedField>,
            relations: List<androidx.room.vo.Relation>,
            constructor: Constructor?
        ): Pojo {
            return Pojo(
                element = element,
                type = declaredType,
                fields = emptyList(),
                embeddedFields = emptyList(),
                relations = emptyList(),
                constructor = null
            )
        }
    }

    private data class FailedConstructor(
        val method: XExecutableElement,
        val params: List<String>,
        val matches: List<Constructor.Param?>
    ) {
        fun log(): String {
            val logPerParam = params.withIndex().joinToString(", ") {
                "param:${it.value} -> matched field:" + (matches[it.index]?.log() ?: "unmatched")
            }
            return "$method -> [$logPerParam]"
        }
    }
}
