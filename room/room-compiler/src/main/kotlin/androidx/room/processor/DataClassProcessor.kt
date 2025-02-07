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
import androidx.room.compiler.processing.isVoid
import androidx.room.ext.isCollection
import androidx.room.ext.isNotVoid
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_GETTER_FOR_PROPERTY
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_SETTER_FOR_PROPERTY
import androidx.room.processor.ProcessorErrors.DATA_CLASS_PROPERTY_HAS_DUPLICATE_COLUMN_NAME
import androidx.room.processor.autovalue.AutoValueDataClassProcessorDelegate
import androidx.room.processor.cache.Cache
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.DataClass
import androidx.room.vo.DataClassFunction
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Entity
import androidx.room.vo.EntityOrView
import androidx.room.vo.Property
import androidx.room.vo.PropertyGetter
import androidx.room.vo.PropertySetter
import androidx.room.vo.Warning
import androidx.room.vo.columnNames
import androidx.room.vo.findPropertyByColumnName
import com.google.auto.value.AutoValue

/** Processes any class as if it is a data class. */
class DataClassProcessor
private constructor(
    baseContext: Context,
    val element: XTypeElement,
    val bindingScope: PropertyProcessor.BindingScope,
    val parent: EmbeddedProperty?,
    val referenceStack: LinkedHashSet<String> = LinkedHashSet(),
    private val delegate: Delegate
) {
    val context = baseContext.fork(element)

    companion object {
        val PROCESSED_ANNOTATIONS = listOf(ColumnInfo::class, Embedded::class, Relation::class)

        val TARGET_METHOD_ANNOTATIONS =
            arrayOf(PrimaryKey::class, ColumnInfo::class, Embedded::class, Relation::class)

        fun createFor(
            context: Context,
            element: XTypeElement,
            bindingScope: PropertyProcessor.BindingScope,
            parent: EmbeddedProperty?,
            referenceStack: LinkedHashSet<String> = LinkedHashSet()
        ): DataClassProcessor {
            val (dataClassElement, delegate) =
                if (element.hasAnnotation(AutoValue::class)) {
                    val processingEnv = context.processingEnv
                    val autoValueGeneratedTypeName =
                        AutoValueDataClassProcessorDelegate.getGeneratedClassName(element)
                    val autoValueGeneratedElement =
                        processingEnv.findTypeElement(autoValueGeneratedTypeName)
                    if (autoValueGeneratedElement != null) {
                        autoValueGeneratedElement to
                            AutoValueDataClassProcessorDelegate(context, element)
                    } else {
                        context.reportMissingType(autoValueGeneratedTypeName)
                        element to EmptyDelegate
                    }
                } else {
                    element to DefaultDelegate(context)
                }

            return DataClassProcessor(
                baseContext = context,
                element = dataClassElement,
                bindingScope = bindingScope,
                parent = parent,
                referenceStack = referenceStack,
                delegate = delegate
            )
        }
    }

    fun process(): DataClass {
        return context.cache.dataClasses.get(Cache.DataClassKey(element, bindingScope, parent)) {
            referenceStack.add(element.qualifiedName)
            try {
                doProcess()
            } finally {
                referenceStack.remove(element.qualifiedName)
            }
        }
    }

    private fun doProcess(): DataClass {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
            return delegate.createDataClass(
                element = element,
                declaredType = element.type,
                properties = emptyList(),
                embeddedProperties = emptyList(),
                relations = emptyList(),
                constructor = null
            )
        }
        delegate.onPreProcess(element)

        val declaredType = element.type
        // TODO handle conflicts with super: b/35568142
        val allProperties =
            element
                .getAllFieldsIncludingPrivateSupers()
                .filter {
                    !it.hasAnnotation(Ignore::class) &&
                        !it.isStatic() &&
                        (!it.isTransient() ||
                            it.hasAnyAnnotation(
                                ColumnInfo::class,
                                Embedded::class,
                                Relation::class
                            ))
                }
                .groupBy { property ->
                    context.checker.check(
                        PROCESSED_ANNOTATIONS.count { property.hasAnnotation(it) } < 2,
                        property,
                        ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_DATA_CLASS_PROPERTY_ANNOTATION
                    )
                    if (property.hasAnnotation(Embedded::class)) {
                        Embedded::class
                    } else if (property.hasAnnotation(Relation::class)) {
                        Relation::class
                    } else {
                        null
                    }
                }

        val ignoredColumns =
            element
                .getAnnotation(androidx.room.Entity::class)
                ?.get("ignoredColumns")
                ?.asStringList()
                ?.toSet() ?: emptySet()
        val propertyBindingErrors = mutableMapOf<Property, String>()
        val unfilteredMyProperties =
            allProperties[null]?.map {
                PropertyProcessor(
                        baseContext = context,
                        containing = declaredType,
                        element = it,
                        bindingScope = bindingScope,
                        propertyParent = parent,
                        onBindingError = { property, errorMsg ->
                            propertyBindingErrors[property] = errorMsg
                        }
                    )
                    .process()
            } ?: emptyList()
        val myProperties =
            unfilteredMyProperties.filterNot { ignoredColumns.contains(it.columnName) }
        myProperties.forEach { property ->
            propertyBindingErrors[property]?.let { context.logger.e(property.element, it) }
        }
        val unfilteredEmbeddedProperties =
            allProperties[Embedded::class]?.mapNotNull { processEmbeddedField(declaredType, it) }
                ?: emptyList()
        val embeddedProperties =
            unfilteredEmbeddedProperties.filterNot {
                ignoredColumns.contains(it.property.columnName)
            }

        val subProperties = embeddedProperties.flatMap { it.dataClass.properties }
        val propertys = myProperties + subProperties

        val unfilteredCombinedProperties =
            unfilteredMyProperties + unfilteredEmbeddedProperties.map { it.property }
        val missingIgnoredColumns =
            ignoredColumns.filterNot { ignoredColumn ->
                unfilteredCombinedProperties.any { it.columnName == ignoredColumn }
            }
        context.checker.check(
            missingIgnoredColumns.isEmpty(),
            element,
            ProcessorErrors.missingIgnoredColumns(missingIgnoredColumns)
        )

        val myRelationsList =
            allProperties[Relation::class]?.mapNotNull {
                processRelationField(propertys, declaredType, it)
            } ?: emptyList()

        val subRelations = embeddedProperties.flatMap { it.dataClass.relations }
        val relations = myRelationsList + subRelations

        propertys
            .groupBy { it.columnName }
            .filter { it.value.size > 1 }
            .forEach {
                context.logger.e(
                    element,
                    ProcessorErrors.dataClassDuplicatePropertyNames(
                        it.key,
                        it.value.map(Property::getPath)
                    )
                )
                it.value.forEach {
                    context.logger.e(it.element, DATA_CLASS_PROPERTY_HAS_DUPLICATE_COLUMN_NAME)
                }
            }

        val methods =
            element
                .getAllNonPrivateInstanceMethods()
                .asSequence()
                .filter { !it.isAbstract() && !it.hasAnnotation(Ignore::class) }
                .map {
                    DataClassFunctionProcessor(
                            context = context,
                            element = it,
                            owner = declaredType
                        )
                        .process()
                }
                .toList()

        val getterCandidates =
            methods.filter {
                it.element.parameters.size == 0 && it.resolvedType.returnType.isNotVoid()
            }

        val setterCandidates =
            methods.filter {
                it.element.parameters.size == 1 && it.resolvedType.returnType.isVoid()
            }

        // don't try to find a constructor for binding to statement.
        val constructor =
            if (bindingScope == PropertyProcessor.BindingScope.BIND_TO_STMT) {
                // we don't need to construct this data class.
                null
            } else {
                chooseConstructor(myProperties, embeddedProperties, relations)
            }

        assignGetters(myProperties, getterCandidates)
        assignSetters(myProperties, setterCandidates, constructor)

        embeddedProperties.forEach {
            assignGetter(it.property, getterCandidates)
            assignSetter(it.property, setterCandidates, constructor)
        }

        myRelationsList.forEach {
            assignGetter(it.property, getterCandidates)
            assignSetter(it.property, setterCandidates, constructor)
        }

        return delegate.createDataClass(
            element,
            declaredType,
            propertys,
            embeddedProperties,
            relations,
            constructor
        )
    }

    private fun chooseConstructor(
        myProperties: List<Property>,
        embedded: List<EmbeddedProperty>,
        relations: List<androidx.room.vo.Relation>
    ): Constructor? {
        val constructors = delegate.findConstructors(element)
        val propertyMap = myProperties.associateBy { it.name }
        val embeddedMap = embedded.associateBy { it.property.name }
        val relationMap = relations.associateBy { it.property.name }
        // list of param names -> matched params pairs for each failed constructor
        val failedConstructors = arrayListOf<FailedConstructor>()
        val goodConstructors =
            constructors
                .map { constructor ->
                    val parameterNames = constructor.parameters.map { it.name }
                    val params =
                        constructor.parameters.mapIndexed param@{ index, param ->
                            val paramName = parameterNames[index]
                            val paramType = param.type

                            val matches =
                                fun(property: Property?): Boolean {
                                    return if (property == null) {
                                        false
                                    } else if (!property.nameWithVariations.contains(paramName)) {
                                        false
                                    } else {
                                        // see: b/69164099
                                        property.type.isAssignableFromWithoutVariance(paramType)
                                    }
                                }

                            val exactFieldMatch = propertyMap[paramName]
                            if (matches(exactFieldMatch)) {
                                return@param Constructor.Param.PropertyParam(exactFieldMatch!!)
                            }
                            val exactEmbeddedMatch = embeddedMap[paramName]
                            if (matches(exactEmbeddedMatch?.property)) {
                                return@param Constructor.Param.EmbeddedParam(exactEmbeddedMatch!!)
                            }
                            val exactRelationMatch = relationMap[paramName]
                            if (matches(exactRelationMatch?.property)) {
                                return@param Constructor.Param.RelationParam(exactRelationMatch!!)
                            }

                            val matchingProperties = myProperties.filter { matches(it) }
                            val embeddedMatches = embedded.filter { matches(it.property) }
                            val relationMatches = relations.filter { matches(it.property) }
                            when (
                                matchingProperties.size +
                                    embeddedMatches.size +
                                    relationMatches.size
                            ) {
                                0 -> null
                                1 ->
                                    when {
                                        matchingProperties.isNotEmpty() ->
                                            Constructor.Param.PropertyParam(
                                                matchingProperties.first()
                                            )
                                        embeddedMatches.isNotEmpty() ->
                                            Constructor.Param.EmbeddedParam(embeddedMatches.first())
                                        else ->
                                            Constructor.Param.RelationParam(relationMatches.first())
                                    }
                                else -> {
                                    context.logger.e(
                                        param,
                                        ProcessorErrors.ambiguousConstructor(
                                            dataClass = element.qualifiedName,
                                            paramName = paramName,
                                            matchingProperties =
                                                matchingProperties.map { it.getPath() } +
                                                    embeddedMatches.map { it.property.getPath() } +
                                                    relationMatches.map { it.property.getPath() }
                                        )
                                    )
                                    null
                                }
                            }
                        }
                    if (params.any { it == null }) {
                        failedConstructors.add(
                            FailedConstructor(constructor, parameterNames, params)
                        )
                        null
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        Constructor(constructor, params as List<Constructor.Param>)
                    }
                }
                .filterNotNull()
        when {
            goodConstructors.isEmpty() -> {
                if (failedConstructors.isNotEmpty()) {
                    val failureMsg = failedConstructors.joinToString("\n") { entry -> entry.log() }
                    context.logger.e(
                        element,
                        ProcessorErrors.MISSING_DATA_CLASS_CONSTRUCTOR +
                            "\nTried the following constructors but they failed to match:" +
                            "\n$failureMsg"
                    )
                }
                context.logger.e(element, ProcessorErrors.MISSING_DATA_CLASS_CONSTRUCTOR)
                return null
            }
            goodConstructors.size > 1 -> {
                // if the class is a Kotlin data class (not a POJO) then pick its primary
                // constructor. This is better than picking the no-arg constructor and forcing
                // users to define propertys as
                // vars.
                val primaryConstructor =
                    element.findPrimaryConstructor()?.let { primary ->
                        goodConstructors.firstOrNull { candidate -> candidate.element == primary }
                    }
                if (primaryConstructor != null) {
                    return primaryConstructor
                }
                // if there is a no-arg constructor, pick it. Even though it is weird, easily
                // happens
                // with kotlin data classes.
                val noArg = goodConstructors.firstOrNull { it.params.isEmpty() }
                if (noArg != null) {
                    context.logger.w(
                        Warning.DEFAULT_CONSTRUCTOR,
                        element,
                        ProcessorErrors.TOO_MANY_DATA_CLASS_CONSTRUCTORS_CHOOSING_NO_ARG
                    )
                    return noArg
                }
                goodConstructors.forEach {
                    context.logger.e(it.element, ProcessorErrors.TOO_MANY_DATA_CLASS_CONSTRUCTORS)
                }
                return null
            }
            else -> return goodConstructors.first()
        }
    }

    private fun processEmbeddedField(
        declaredType: XType,
        variableElement: XFieldElement
    ): EmbeddedProperty? {
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

        val embeddedAnnotation = variableElement.getAnnotation(Embedded::class)
        val propertyPrefix = embeddedAnnotation?.get("prefix")?.asString() ?: ""
        val inheritedPrefix = parent?.prefix ?: ""
        val embeddedProperty =
            Property(
                variableElement,
                variableElement.name,
                type = asMemberType,
                affinity = null,
                parent = parent
            )
        val subParent =
            EmbeddedProperty(
                property = embeddedProperty,
                prefix = inheritedPrefix + propertyPrefix,
                parent = parent
            )
        subParent.dataClass =
            createFor(
                    context = context.fork(variableElement),
                    element = asTypeElement,
                    bindingScope = bindingScope,
                    parent = subParent,
                    referenceStack = referenceStack
                )
                .process()
        return subParent
    }

    private fun processRelationField(
        myProperties: List<Property>,
        container: XType,
        relationElement: XFieldElement
    ): androidx.room.vo.Relation? {
        val annotation = relationElement.requireAnnotation(Relation::class)

        val parentColumnName = annotation.getAsString("parentColumn")
        val parentField = myProperties.firstOrNull { it.columnName == parentColumnName }
        if (parentField == null) {
            context.logger.e(
                relationElement,
                ProcessorErrors.relationCannotFindParentEntityProperty(
                    entityName = element.qualifiedName,
                    columnName = parentColumnName,
                    availableColumns = myProperties.map { it.columnName }
                )
            )
            return null
        }
        // parse it as an entity.
        val asMember = relationElement.asMemberOf(container)
        val asType =
            if (asMember.isCollection()) {
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

        val entityClassInput = annotation["entity"]?.asType()

        // do we need to decide on the entity?
        val inferEntity = (entityClassInput == null || entityClassInput.isTypeOf(Any::class))
        val entityElement =
            if (inferEntity) {
                typeElement
            } else {
                entityClassInput.typeElement
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

        // now find the property in the entity.
        val entityColumnName = annotation.getAsString("entityColumn")
        val entityField = entity.findPropertyByColumnName(entityColumnName)
        if (entityField == null) {
            context.logger.e(
                relationElement,
                ProcessorErrors.relationCannotFindEntityProperty(
                    entityName = entity.typeName.toString(context.codeLanguage),
                    columnName = entityColumnName,
                    availableColumns = entity.columnNames
                )
            )
            return null
        }

        // do we have a join entity?
        val junctionAnnotation = annotation["associateBy"]?.asAnnotation()
        val junctionClassInput = junctionAnnotation?.getAsType("value")
        val junctionElement: XTypeElement? =
            if (junctionClassInput != null && !junctionClassInput.isTypeOf(Any::class)) {
                junctionClassInput.typeElement.also {
                    if (it == null) {
                        context.logger.e(relationElement, ProcessorErrors.NOT_ENTITY_OR_VIEW)
                    }
                }
            } else {
                null
            }
        val junction =
            if (junctionAnnotation != null && junctionElement != null) {
                val entityOrView =
                    EntityOrViewProcessor(context, junctionElement, referenceStack).process()

                fun findAndValidateJunctionColumn(
                    columnName: String,
                    onMissingField: () -> Unit
                ): Property? {
                    val property = entityOrView.findPropertyByColumnName(columnName)
                    if (property == null) {
                        onMissingField()
                        return null
                    }
                    if (entityOrView is Entity) {
                        // warn about not having indices in the junction columns, only considering
                        // 1st column in composite primary key and indices, since order matters.
                        val coveredColumns =
                            entityOrView.primaryKey.properties.columnNames.first() +
                                entityOrView.indices.map { it.columnNames.first() }
                        if (!coveredColumns.contains(property.columnName)) {
                            context.logger.w(
                                Warning.MISSING_INDEX_ON_JUNCTION,
                                property.element,
                                ProcessorErrors.junctionColumnWithoutIndex(
                                    entityName =
                                        entityOrView.typeName.toString(context.codeLanguage),
                                    columnName = columnName
                                )
                            )
                        }
                    }
                    return property
                }

                val junctionParentColumnName = junctionAnnotation["parentColumn"]?.asString() ?: ""
                val junctionParentColumn =
                    if (junctionParentColumnName.isNotEmpty()) {
                        junctionParentColumnName
                    } else {
                        parentField.columnName
                    }
                val junctionParentField =
                    findAndValidateJunctionColumn(
                        columnName = junctionParentColumn,
                        onMissingField = {
                            context.logger.e(
                                junctionElement,
                                ProcessorErrors.relationCannotFindJunctionParentProperty(
                                    entityName =
                                        entityOrView.typeName.toString(context.codeLanguage),
                                    columnName = junctionParentColumn,
                                    availableColumns = entityOrView.columnNames
                                )
                            )
                        }
                    )

                val junctionEntityColumnName = junctionAnnotation["entityColumn"]?.asString() ?: ""
                val junctionEntityColumn =
                    if (junctionEntityColumnName.isNotEmpty()) {
                        junctionEntityColumnName
                    } else {
                        entityField.columnName
                    }
                val junctionEntityField =
                    findAndValidateJunctionColumn(
                        columnName = junctionEntityColumn,
                        onMissingField = {
                            context.logger.e(
                                junctionElement,
                                ProcessorErrors.relationCannotFindJunctionEntityProperty(
                                    entityName =
                                        entityOrView.typeName.toString(context.codeLanguage),
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
                    parentProperty = junctionParentField,
                    entityProperty = junctionEntityField
                )
            } else {
                null
            }

        val property =
            Property(
                element = relationElement,
                name = relationElement.name,
                type = relationElement.asMemberOf(container),
                affinity = null,
                parent = parent
            )

        val projectionNames = annotation["projection"]?.asStringList() ?: emptyList()
        val projection =
            if (projectionNames.isEmpty()) {
                // we need to infer the projection from inputs.
                createRelationshipProjection(inferEntity, asType, entity, entityField, typeElement)
            } else {
                // make sure projection makes sense
                validateRelationshipProjection(projectionNames, entity, relationElement)
                projectionNames
            }
        // if types don't match, row adapter prints a warning
        return androidx.room.vo.Relation(
            entity = entity,
            dataClassType = asType,
            property = property,
            parentProperty = parentField,
            entityProperty = entityField,
            junction = junction,
            projection = projection
        )
    }

    private fun validateRelationshipProjection(
        projectionInput: List<String>,
        entity: EntityOrView,
        relationElement: XVariableElement
    ) {
        val missingColumns = projectionInput.toList() - entity.columnNames
        if (missingColumns.isNotEmpty()) {
            context.logger.e(
                relationElement,
                ProcessorErrors.relationBadProject(
                    entity.typeName.toString(context.codeLanguage),
                    missingColumns,
                    entity.columnNames
                )
            )
        }
    }

    /**
     * Create the projection column list based on the relationship args.
     *
     * if entity property in the annotation is not specified, it is the method return type if it is
     * specified in the annotation: still check the method return type, if the same, use it if not,
     * check to see if we can find a column Adapter, if so use the childField last resort, try to
     * parse it as a data class to infer it.
     */
    private fun createRelationshipProjection(
        inferEntity: Boolean,
        typeArg: XType,
        entity: EntityOrView,
        entityField: Property,
        typeArgElement: XTypeElement
    ): List<String> {
        return if (inferEntity || typeArg.asTypeName() == entity.typeName) {
            entity.columnNames
        } else {
            val columnAdapter = context.typeAdapterStore.findStatementValueReader(typeArg, null)
            if (columnAdapter != null) {
                // nice, there is a column adapter for this, assume single column response
                listOf(entityField.name)
            } else {
                // last resort, it needs to be a data class
                val dataClass =
                    createFor(
                            context = context,
                            element = typeArgElement,
                            bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                            parent = parent,
                            referenceStack = referenceStack
                        )
                        .process()
                dataClass.columnNames
            }
        }
    }

    private fun detectReferenceRecursion(typeElement: XTypeElement): Boolean {
        if (referenceStack.contains(typeElement.qualifiedName)) {
            context.logger.e(
                typeElement,
                ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                    computeReferenceRecursionString(typeElement)
                )
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

    private fun assignGetters(
        propertys: List<Property>,
        getterCandidates: List<DataClassFunction>
    ) {
        propertys.forEach { property -> assignGetter(property, getterCandidates) }
    }

    private fun assignGetter(property: Property, getterCandidates: List<DataClassFunction>) {
        val success =
            chooseAssignment(
                property = property,
                candidates = getterCandidates,
                nameVariations = property.getterNameWithVariations,
                getType = { method -> method.resolvedType.returnType },
                assignFromField = {
                    property.getter =
                        PropertyGetter(
                            propertyName = property.name,
                            jvmName = property.name,
                            type = property.type,
                            callType = CallType.PROPERTY
                        )
                },
                assignFromMethod = { match ->
                    property.getter =
                        PropertyGetter(
                            propertyName = property.name,
                            jvmName = match.element.jvmName,
                            type = match.resolvedType.returnType,
                            callType =
                                if (match.element.isKotlinPropertyMethod()) {
                                    CallType.SYNTHETIC_FUNCTION
                                } else {
                                    CallType.FUNCTION
                                }
                        )
                },
                reportAmbiguity = { matching ->
                    context.logger.e(
                        property.element,
                        ProcessorErrors.tooManyMatchingGetters(property, matching)
                    )
                }
            )
        context.checker.check(
            success || bindingScope == PropertyProcessor.BindingScope.READ_FROM_STMT,
            property.element,
            CANNOT_FIND_GETTER_FOR_PROPERTY
        )
        if (success && !property.getter.type.isSameType(property.type)) {
            // getter's parameter type is not exactly the same as the property type.
            // put a warning and update the value statement binder.
            context.logger.w(
                warning = Warning.MISMATCHED_GETTER_TYPE,
                element = property.element,
                msg =
                    ProcessorErrors.mismatchedGetter(
                        propertyName = property.name,
                        ownerType = element.type.asTypeName().toString(context.codeLanguage),
                        getterType =
                            property.getter.type.asTypeName().toString(context.codeLanguage),
                        propertyType = property.typeName.toString(context.codeLanguage)
                    )
            )
            property.statementBinder =
                context.typeAdapterStore.findStatementValueBinder(
                    input = property.getter.type,
                    affinity = property.affinity
                )
        }
    }

    private fun assignSetters(
        propertys: List<Property>,
        setterCandidates: List<DataClassFunction>,
        constructor: Constructor?
    ) {
        propertys.forEach { property -> assignSetter(property, setterCandidates, constructor) }
    }

    private fun assignSetter(
        property: Property,
        setterCandidates: List<DataClassFunction>,
        constructor: Constructor?
    ) {
        if (constructor != null && constructor.hasProperty(property)) {
            property.setter =
                PropertySetter(
                    propertyName = property.name,
                    jvmName = property.name,
                    type = property.type,
                    callType = CallType.CONSTRUCTOR
                )
            return
        }
        val success =
            chooseAssignment(
                property = property,
                candidates = setterCandidates,
                nameVariations = property.setterNameWithVariations,
                getType = { method -> method.resolvedType.parameterTypes.first() },
                assignFromField = {
                    property.setter =
                        PropertySetter(
                            propertyName = property.name,
                            jvmName = property.name,
                            type = property.type,
                            callType = CallType.PROPERTY
                        )
                },
                assignFromMethod = { match ->
                    val paramType = match.resolvedType.parameterTypes.first()
                    property.setter =
                        PropertySetter(
                            propertyName = property.name,
                            jvmName = match.element.jvmName,
                            type = paramType,
                            callType =
                                if (match.element.isKotlinPropertyMethod()) {
                                    CallType.SYNTHETIC_FUNCTION
                                } else {
                                    CallType.FUNCTION
                                }
                        )
                },
                reportAmbiguity = { matching ->
                    context.logger.e(
                        property.element,
                        ProcessorErrors.tooManyMatchingSetter(property, matching)
                    )
                }
            )
        context.checker.check(
            success || bindingScope == PropertyProcessor.BindingScope.BIND_TO_STMT,
            property.element,
            CANNOT_FIND_SETTER_FOR_PROPERTY
        )
        if (success && !property.setter.type.isSameType(property.type)) {
            // setter's parameter type is not exactly the same as the property type.
            // put a warning and update the value reader adapter.
            context.logger.w(
                warning = Warning.MISMATCHED_SETTER_TYPE,
                element = property.element,
                msg =
                    ProcessorErrors.mismatchedSetter(
                        propertyName = property.name,
                        ownerType = element.type.asTypeName().toString(context.codeLanguage),
                        setterType =
                            property.setter.type.asTypeName().toString(context.codeLanguage),
                        propertyType = property.typeName.toString(context.codeLanguage)
                    )
            )
            property.statementValueReader =
                context.typeAdapterStore.findStatementValueReader(
                    output = property.setter.type,
                    affinity = property.affinity
                )
        }
    }

    /**
     * Finds a setter/getter from available list of methods. It returns true if assignment is
     * successful, false otherwise. At worst case, it sets to the property as if it is accessible so
     * that the rest of the compilation can continue.
     */
    private fun chooseAssignment(
        property: Property,
        candidates: List<DataClassFunction>,
        nameVariations: List<String>,
        getType: (DataClassFunction) -> XType,
        assignFromField: () -> Unit,
        assignFromMethod: (DataClassFunction) -> Unit,
        reportAmbiguity: (List<String>) -> Unit
    ): Boolean {
        if (property.element.isPublic()) {
            assignFromField()
            return true
        }

        val matching =
            candidates
                .filter {
                    // b/69164099
                    // use names in source (rather than jvmName) for matching since that is what
                    // user
                    // sees in code
                    property.type.isAssignableFromWithoutVariance(getType(it)) &&
                        (property.nameWithVariations.contains(it.element.name) ||
                            nameVariations.contains(it.element.name))
                }
                .groupBy { it.element.isPublic() }
        if (matching.isEmpty()) {
            // we always assign to avoid NPEs in the rest of the compilation.
            assignFromField()
            // if property is not private, assume it works (if we are on the same package).
            // if not, compiler will tell, we didn't have any better alternative anyways.
            return !property.element.isPrivate()
        }
        // first try public ones, then try non-public
        val match =
            verifyAndChooseOneFrom(matching[true], reportAmbiguity)
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
        candidates: List<DataClassFunction>?,
        reportAmbiguity: (List<String>) -> Unit
    ): DataClassFunction? {
        if (candidates == null) {
            return null
        }
        if (candidates.size > 1) {
            reportAmbiguity(candidates.map { it.element.name })
        }
        return candidates.first()
    }

    interface Delegate {

        fun onPreProcess(element: XTypeElement)

        /**
         * Constructors are XExecutableElement rather than XConstructorElement to account for
         * factory methods.
         */
        fun findConstructors(element: XTypeElement): List<XExecutableElement>

        fun createDataClass(
            element: XTypeElement,
            declaredType: XType,
            properties: List<Property>,
            embeddedProperties: List<EmbeddedProperty>,
            relations: List<androidx.room.vo.Relation>,
            constructor: Constructor?
        ): DataClass
    }

    private class DefaultDelegate(private val context: Context) : Delegate {
        override fun onPreProcess(element: XTypeElement) {
            // If data class is not a record then check that certain Room annotations with
            // @Target(METHOD) are not used since the data class is not annotated with AutoValue
            // where Room column annotations are allowed in methods.
            if (!element.isRecordClass()) {
                element
                    .getAllMethods()
                    .filter { it.hasAnyAnnotation(*TARGET_METHOD_ANNOTATIONS) }
                    .forEach { method ->
                        val annotationName =
                            TARGET_METHOD_ANNOTATIONS.first { columnAnnotation ->
                                    method.hasAnnotation(columnAnnotation)
                                }
                                .java
                                .simpleName
                        context.logger.e(
                            method,
                            ProcessorErrors.invalidAnnotationTarget(
                                annotationName,
                                method.kindName()
                            )
                        )
                    }
            }
        }

        override fun findConstructors(element: XTypeElement) =
            element.getConstructors().filterNot {
                it.hasAnnotation(Ignore::class) || it.isPrivate()
            }

        override fun createDataClass(
            element: XTypeElement,
            declaredType: XType,
            properties: List<Property>,
            embeddedProperties: List<EmbeddedProperty>,
            relations: List<androidx.room.vo.Relation>,
            constructor: Constructor?
        ): DataClass {
            return DataClass(
                element = element,
                type = declaredType,
                properties = properties,
                embeddedProperties = embeddedProperties,
                relations = relations,
                constructor = constructor
            )
        }
    }

    private object EmptyDelegate : Delegate {
        override fun onPreProcess(element: XTypeElement) {}

        override fun findConstructors(element: XTypeElement): List<XExecutableElement> = emptyList()

        override fun createDataClass(
            element: XTypeElement,
            declaredType: XType,
            properties: List<Property>,
            embeddedProperties: List<EmbeddedProperty>,
            relations: List<androidx.room.vo.Relation>,
            constructor: Constructor?
        ): DataClass {
            return DataClass(
                element = element,
                type = declaredType,
                properties = emptyList(),
                embeddedProperties = emptyList(),
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
            val logPerParam =
                params.withIndex().joinToString(", ") {
                    "param:${it.value} -> matched property:" +
                        (matches[it.index]?.log() ?: "unmatched")
                }
            return "$method -> [$logPerParam]"
        }
    }
}
