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

import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.isNotNone
import androidx.room.processor.EntityProcessor.Companion.createIndexName
import androidx.room.processor.EntityProcessor.Companion.extractForeignKeys
import androidx.room.processor.EntityProcessor.Companion.extractIndices
import androidx.room.processor.EntityProcessor.Companion.extractTableName
import androidx.room.processor.ProcessorErrors.INDEX_COLUMNS_CANNOT_BE_EMPTY
import androidx.room.processor.ProcessorErrors.RELATION_IN_ENTITY
import androidx.room.processor.cache.Cache
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Entity
import androidx.room.vo.Field
import androidx.room.vo.Fields
import androidx.room.vo.ForeignKey
import androidx.room.vo.Index
import androidx.room.vo.Pojo
import androidx.room.vo.PrimaryKey
import androidx.room.vo.Warning
import androidx.room.vo.columnNames
import androidx.room.vo.findFieldByColumnName

class TableEntityProcessor internal constructor(
    baseContext: Context,
    val element: XTypeElement,
    private val referenceStack: LinkedHashSet<String> = LinkedHashSet()
) : EntityProcessor {
    val context = baseContext.fork(element)

    override fun process(): Entity {
        return context.cache.entities.get(Cache.EntityKey(element)) {
            doProcess()
        }
    }

    private fun doProcess(): Entity {
        context.checker.hasAnnotation(
            element, androidx.room.Entity::class,
            ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY
        )
        val annotationBox = element.getAnnotation(androidx.room.Entity::class)
        val tableName: String
        val entityIndices: List<IndexInput>
        val foreignKeyInputs: List<ForeignKeyInput>
        val inheritSuperIndices: Boolean
        if (annotationBox != null) {
            tableName = extractTableName(element, annotationBox.value)
            entityIndices = extractIndices(annotationBox, tableName)
            inheritSuperIndices = annotationBox.value.inheritSuperIndices
            foreignKeyInputs = extractForeignKeys(annotationBox)
        } else {
            tableName = element.name
            foreignKeyInputs = emptyList()
            entityIndices = emptyList()
            inheritSuperIndices = false
        }
        context.checker.notBlank(
            tableName, element,
            ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY
        )
        context.checker.check(
            !tableName.startsWith("sqlite_", true), element,
            ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_START_WITH_SQLITE
        )

        val pojo = PojoProcessor.createFor(
            context = context,
            element = element,
            bindingScope = FieldProcessor.BindingScope.TWO_WAY,
            parent = null,
            referenceStack = referenceStack
        ).process()
        context.checker.check(pojo.relations.isEmpty(), element, RELATION_IN_ENTITY)

        val fieldIndices = pojo.fields
            .filter { it.indexed }.mapNotNull {
                if (it.parent != null) {
                    it.indexed = false
                    context.logger.w(
                        Warning.INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED, it.element,
                        ProcessorErrors.droppedEmbeddedFieldIndex(
                            it.getPath(), element.qualifiedName
                        )
                    )
                    null
                } else if (it.element.enclosingElement != element && !inheritSuperIndices) {
                    it.indexed = false
                    context.logger.w(
                        Warning.INDEX_FROM_PARENT_FIELD_IS_DROPPED,
                        ProcessorErrors.droppedSuperClassFieldIndex(
                            it.columnName, element.qualifiedName,
                            it.element.enclosingElement.className.toString()
                        )
                    )
                    null
                } else {
                    IndexInput(
                        name = createIndexName(listOf(it.columnName), tableName),
                        unique = false,
                        columnNames = listOf(it.columnName)
                    )
                }
            }
        val superIndices = loadSuperIndices(element.superType, tableName, inheritSuperIndices)
        val indexInputs = entityIndices + fieldIndices + superIndices
        val indices = validateAndCreateIndices(indexInputs, pojo)

        val primaryKey = findAndValidatePrimaryKey(pojo.fields, pojo.embeddedFields)
        val affinity = primaryKey.fields.firstOrNull()?.affinity ?: SQLTypeAffinity.TEXT
        context.checker.check(
            !primaryKey.autoGenerateId || affinity == SQLTypeAffinity.INTEGER,
            primaryKey.fields.firstOrNull()?.element ?: element,
            ProcessorErrors.AUTO_INCREMENTED_PRIMARY_KEY_IS_NOT_INT
        )

        val entityForeignKeys = validateAndCreateForeignKeyReferences(foreignKeyInputs, pojo)
        checkIndicesForForeignKeys(entityForeignKeys, primaryKey, indices)

        context.checker.check(
            SqlParser.isValidIdentifier(tableName), element,
            ProcessorErrors.INVALID_TABLE_NAME
        )
        pojo.fields.forEach {
            context.checker.check(
                SqlParser.isValidIdentifier(it.columnName), it.element,
                ProcessorErrors.INVALID_COLUMN_NAME
            )
        }

        val entity = Entity(
            element = element,
            tableName = tableName,
            type = pojo.type,
            fields = pojo.fields,
            embeddedFields = pojo.embeddedFields,
            indices = indices,
            primaryKey = primaryKey,
            foreignKeys = entityForeignKeys,
            constructor = pojo.constructor,
            shadowTableName = null
        )

        return entity
    }

    private fun checkIndicesForForeignKeys(
        entityForeignKeys: List<ForeignKey>,
        primaryKey: PrimaryKey,
        indices: List<Index>
    ) {
        fun covers(columnNames: List<String>, fields: List<Field>): Boolean =
            fields.size >= columnNames.size && columnNames.withIndex().all {
                fields[it.index].columnName == it.value
            }

        entityForeignKeys.forEach { fKey ->
            val columnNames = fKey.childFields.map { it.columnName }
            val exists = covers(columnNames, primaryKey.fields) || indices.any { index ->
                covers(columnNames, index.fields)
            }
            if (!exists) {
                if (columnNames.size == 1) {
                    context.logger.w(
                        Warning.MISSING_INDEX_ON_FOREIGN_KEY_CHILD, element,
                        ProcessorErrors.foreignKeyMissingIndexInChildColumn(columnNames[0])
                    )
                } else {
                    context.logger.w(
                        Warning.MISSING_INDEX_ON_FOREIGN_KEY_CHILD, element,
                        ProcessorErrors.foreignKeyMissingIndexInChildColumns(columnNames)
                    )
                }
            }
        }
    }

    /**
     * Does a validation on foreign keys except the parent table's columns.
     */
    private fun validateAndCreateForeignKeyReferences(
        foreignKeyInputs: List<ForeignKeyInput>,
        pojo: Pojo
    ): List<ForeignKey> {
        return foreignKeyInputs.map {
            if (it.onUpdate == null) {
                context.logger.e(element, ProcessorErrors.INVALID_FOREIGN_KEY_ACTION)
                return@map null
            }
            if (it.onDelete == null) {
                context.logger.e(element, ProcessorErrors.INVALID_FOREIGN_KEY_ACTION)
                return@map null
            }
            if (it.childColumns.isEmpty()) {
                context.logger.e(element, ProcessorErrors.FOREIGN_KEY_EMPTY_CHILD_COLUMN_LIST)
                return@map null
            }
            if (it.parentColumns.isEmpty()) {
                context.logger.e(element, ProcessorErrors.FOREIGN_KEY_EMPTY_PARENT_COLUMN_LIST)
                return@map null
            }
            if (it.childColumns.size != it.parentColumns.size) {
                context.logger.e(
                    element,
                    ProcessorErrors.foreignKeyColumnNumberMismatch(
                        it.childColumns, it.parentColumns
                    )
                )
                return@map null
            }
            val parentElement = it.parent.typeElement
            if (parentElement == null) {
                context.logger.e(element, ProcessorErrors.FOREIGN_KEY_CANNOT_FIND_PARENT)
                return@map null
            }
            val parentAnnotation = parentElement.getAnnotation(androidx.room.Entity::class)
            if (parentAnnotation == null) {
                context.logger.e(
                    element,
                    ProcessorErrors.foreignKeyNotAnEntity(parentElement.qualifiedName)
                )
                return@map null
            }
            val tableName = extractTableName(parentElement, parentAnnotation.value)
            val fields = it.childColumns.mapNotNull { columnName ->
                val field = pojo.findFieldByColumnName(columnName)
                if (field == null) {
                    context.logger.e(
                        pojo.element,
                        ProcessorErrors.foreignKeyChildColumnDoesNotExist(
                            columnName,
                            pojo.columnNames
                        )
                    )
                }
                field
            }
            if (fields.size != it.childColumns.size) {
                return@map null
            }
            ForeignKey(
                parentTable = tableName,
                childFields = fields,
                parentColumns = it.parentColumns,
                onDelete = it.onDelete,
                onUpdate = it.onUpdate,
                deferred = it.deferred
            )
        }.filterNotNull()
    }

    private fun findAndValidatePrimaryKey(
        fields: List<Field>,
        embeddedFields: List<EmbeddedField>
    ): PrimaryKey {
        val candidates = collectPrimaryKeysFromEntityAnnotations(element, fields) +
            collectPrimaryKeysFromPrimaryKeyAnnotations(fields) +
            collectPrimaryKeysFromEmbeddedFields(embeddedFields)

        context.checker.check(candidates.isNotEmpty(), element, ProcessorErrors.MISSING_PRIMARY_KEY)

        // 1. If a key is not autogenerated, but is Primary key or is part of Primary key we
        // force the @NonNull annotation. If the key is a single Primary Key, Integer or Long, we
        // don't force the @NonNull annotation since SQLite will automatically generate IDs.
        // 2. If a key is autogenerate, we generate NOT NULL in table spec, but we don't require
        // @NonNull annotation on the field itself.
        val verifiedFields = mutableSetOf<Field>() // track verified fields to not over report
        candidates.filterNot { it.autoGenerateId }.forEach { candidate ->
            candidate.fields.forEach { field ->
                if (candidate.fields.size > 1 ||
                    (candidate.fields.size == 1 && field.affinity != SQLTypeAffinity.INTEGER)
                ) {
                    if (!verifiedFields.contains(field)) {
                        context.checker.check(
                            field.nonNull,
                            field.element,
                            ProcessorErrors.primaryKeyNull(field.getPath())
                        )
                        verifiedFields.add(field)
                    }
                    // Validate parents for nullability
                    var parent = field.parent
                    while (parent != null) {
                        val parentField = parent.field
                        if (!verifiedFields.contains(parentField)) {
                            context.checker.check(
                                parentField.nonNull,
                                parentField.element,
                                ProcessorErrors.primaryKeyNull(parentField.getPath())
                            )
                            verifiedFields.add(parentField)
                        }
                        parent = parentField.parent
                    }
                }
            }
        }

        if (candidates.size == 1) {
            // easy :)
            return candidates.first()
        }

        return choosePrimaryKey(candidates, element)
    }

    /**
     * Check fields for @PrimaryKey.
     */
    private fun collectPrimaryKeysFromPrimaryKeyAnnotations(fields: List<Field>): List<PrimaryKey> {
        return fields.mapNotNull { field ->
            field.element.getAnnotation(androidx.room.PrimaryKey::class)?.let {
                if (field.parent != null) {
                    // the field in the entity that contains this error.
                    val grandParentField = field.parent.mRootParent.field.element
                    // bound for entity.
                    context.fork(grandParentField).logger.w(
                        Warning.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED,
                        grandParentField,
                        ProcessorErrors.embeddedPrimaryKeyIsDropped(
                            element.qualifiedName, field.name
                        )
                    )
                    null
                } else {
                    PrimaryKey(
                        declaredIn = field.element.enclosingElement,
                        fields = Fields(field),
                        autoGenerateId = it.value.autoGenerate
                    )
                }
            }
        }
    }

    /**
     * Check classes for @Entity(primaryKeys = ?).
     */
    private fun collectPrimaryKeysFromEntityAnnotations(
        typeElement: XTypeElement,
        availableFields: List<Field>
    ): List<PrimaryKey> {
        val myPkeys = typeElement.getAnnotation(androidx.room.Entity::class)?.let {
            val primaryKeyColumns = it.value.primaryKeys
            if (primaryKeyColumns.isEmpty()) {
                emptyList()
            } else {
                val fields = primaryKeyColumns.mapNotNull { pKeyColumnName ->
                    val field = availableFields.firstOrNull { it.columnName == pKeyColumnName }
                    context.checker.check(
                        field != null, typeElement,
                        ProcessorErrors.primaryKeyColumnDoesNotExist(
                            pKeyColumnName,
                            availableFields.map { it.columnName }
                        )
                    )
                    field
                }
                listOf(
                    PrimaryKey(
                        declaredIn = typeElement,
                        fields = Fields(fields),
                        autoGenerateId = false
                    )
                )
            }
        } ?: emptyList()
        // checks supers.
        val mySuper = typeElement.superType
        val superPKeys = if (mySuper != null && mySuper.isNotNone()) {
            // my super cannot see my fields so remove them.
            val remainingFields = availableFields.filterNot {
                it.element.enclosingElement == typeElement
            }
            collectPrimaryKeysFromEntityAnnotations(mySuper.typeElement!!, remainingFields)
        } else {
            emptyList()
        }
        return superPKeys + myPkeys
    }

    private fun collectPrimaryKeysFromEmbeddedFields(
        embeddedFields: List<EmbeddedField>
    ): List<PrimaryKey> {
        return embeddedFields.mapNotNull { embeddedField ->
            embeddedField.field.element.getAnnotation(androidx.room.PrimaryKey::class)?.let {
                context.checker.check(
                    !it.value.autoGenerate || embeddedField.pojo.fields.size == 1,
                    embeddedField.field.element,
                    ProcessorErrors.AUTO_INCREMENT_EMBEDDED_HAS_MULTIPLE_FIELDS
                )
                PrimaryKey(
                    declaredIn = embeddedField.field.element.enclosingElement,
                    fields = embeddedField.pojo.fields,
                    autoGenerateId = it.value.autoGenerate
                )
            }
        }
    }

    // start from my element and check if anywhere in the list we can find the only well defined
    // pkey, if so, use it.
    private fun choosePrimaryKey(
        candidates: List<PrimaryKey>,
        typeElement: XTypeElement
    ): PrimaryKey {
        // If 1 of these primary keys is declared in this class, then it is the winner. Just print
        //    a note for the others.
        // If 0 is declared, check the parent.
        // If more than 1 primary key is declared in this class, it is an error.
        val myPKeys = candidates.filter { candidate ->
            candidate.declaredIn == typeElement
        }
        return if (myPKeys.size == 1) {
            // just note, this is not worth an error or warning
            (candidates - myPKeys).forEach {
                context.logger.d(
                    element,
                    "${it.toHumanReadableString()} is" +
                        " overridden by ${myPKeys.first().toHumanReadableString()}"
                )
            }
            myPKeys.first()
        } else if (myPKeys.isEmpty()) {
            // i have not declared anything, delegate to super
            val mySuper = typeElement.superType
            if (mySuper != null && mySuper.isNotNone()) {
                return choosePrimaryKey(candidates, mySuper.typeElement!!)
            }
            PrimaryKey.MISSING
        } else {
            context.logger.e(
                element,
                ProcessorErrors.multiplePrimaryKeyAnnotations(
                    myPKeys.map(PrimaryKey::toHumanReadableString)
                )
            )
            PrimaryKey.MISSING
        }
    }

    private fun validateAndCreateIndices(
        inputs: List<IndexInput>,
        pojo: Pojo
    ): List<Index> {
        // check for columns
        val indices = inputs.mapNotNull { input ->
            context.checker.check(
                input.columnNames.isNotEmpty(), element,
                INDEX_COLUMNS_CANNOT_BE_EMPTY
            )
            val fields = input.columnNames.mapNotNull { columnName ->
                val field = pojo.findFieldByColumnName(columnName)
                context.checker.check(
                    field != null, element,
                    ProcessorErrors.indexColumnDoesNotExist(columnName, pojo.columnNames)
                )
                field
            }
            if (fields.isEmpty()) {
                null
            } else {
                Index(name = input.name, unique = input.unique, fields = fields)
            }
        }

        // check for duplicate indices
        indices
            .groupBy { it.name }
            .filter { it.value.size > 1 }
            .forEach {
                context.logger.e(element, ProcessorErrors.duplicateIndexInEntity(it.key))
            }

        // see if any embedded field is an entity with indices, if so, report a warning
        pojo.embeddedFields.forEach { embedded ->
            val embeddedElement = embedded.pojo.element
            embeddedElement.getAnnotation(androidx.room.Entity::class)?.let {
                val subIndices = extractIndices(it, "")
                if (subIndices.isNotEmpty()) {
                    context.logger.w(
                        Warning.INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED,
                        embedded.field.element,
                        ProcessorErrors.droppedEmbeddedIndex(
                            entityName = embedded.pojo.typeName.toString(),
                            fieldPath = embedded.field.getPath(),
                            grandParent = element.qualifiedName
                        )
                    )
                }
            }
        }
        return indices
    }

    // check if parent is an Entity, if so, report its annotation indices
    private fun loadSuperIndices(
        typeMirror: XType?,
        tableName: String,
        inherit: Boolean
    ): List<IndexInput> {
        if (typeMirror == null || typeMirror.isNone()) {
            return emptyList()
        }
        val parentTypeElement = typeMirror.typeElement
        @Suppress("FoldInitializerAndIfToElvis")
        if (parentTypeElement == null) {
            // this is coming from a parent, shouldn't happen so no reason to report an error
            return emptyList()
        }
        val myIndices = parentTypeElement
            .getAnnotation(androidx.room.Entity::class)?.let { annotation ->
                val indices = extractIndices(annotation, tableName = "super")
                if (indices.isEmpty()) {
                    emptyList()
                } else if (inherit) {
                    // rename them
                    indices.map {
                        IndexInput(
                            name = createIndexName(it.columnNames, tableName),
                            unique = it.unique,
                            columnNames = it.columnNames
                        )
                    }
                } else {
                    context.logger.w(
                        Warning.INDEX_FROM_PARENT_IS_DROPPED,
                        parentTypeElement,
                        ProcessorErrors.droppedSuperClassIndex(
                            childEntity = element.qualifiedName,
                            superEntity = parentTypeElement.qualifiedName
                        )
                    )
                    emptyList()
                }
            } ?: emptyList()
        return myIndices + loadSuperIndices(parentTypeElement.superType, tableName, inherit)
    }
}
