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

import androidx.room.ext.getAsBoolean
import androidx.room.ext.getAsInt
import androidx.room.ext.getAsString
import androidx.room.ext.getAsStringList
import androidx.room.ext.toType
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.processor.ProcessorErrors.INDEX_COLUMNS_CANNOT_BE_EMPTY
import androidx.room.processor.ProcessorErrors.RELATION_IN_ENTITY
import androidx.room.processor.cache.Cache
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Entity
import androidx.room.vo.Field
import androidx.room.vo.ForeignKey
import androidx.room.vo.ForeignKeyAction
import androidx.room.vo.Index
import androidx.room.vo.Pojo
import androidx.room.vo.PrimaryKey
import androidx.room.vo.Warning
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor6

class EntityProcessor(baseContext: Context,
                      val element: TypeElement,
                      private val referenceStack: LinkedHashSet<Name> = LinkedHashSet()) {
    val context = baseContext.fork(element)

    fun process(): Entity {
        return context.cache.entities.get(Cache.EntityKey(element), {
            doProcess()
        })
    }
    private fun doProcess(): Entity {
        context.checker.hasAnnotation(element, androidx.room.Entity::class,
                ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY)
        val pojo = PojoProcessor(
                baseContext = context,
                element = element,
                bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                parent = null,
                referenceStack = referenceStack).process()
        context.checker.check(pojo.relations.isEmpty(), element, RELATION_IN_ENTITY)
        val annotation = MoreElements.getAnnotationMirror(element,
                androidx.room.Entity::class.java).orNull()
        val tableName: String
        val entityIndices: List<IndexInput>
        val foreignKeyInputs: List<ForeignKeyInput>
        val inheritSuperIndices: Boolean
        if (annotation != null) {
            tableName = extractTableName(element, annotation)
            entityIndices = extractIndices(annotation, tableName)
            inheritSuperIndices = AnnotationMirrors
                    .getAnnotationValue(annotation, "inheritSuperIndices").getAsBoolean(false)
            foreignKeyInputs = extractForeignKeys(annotation)
        } else {
            tableName = element.simpleName.toString()
            foreignKeyInputs = emptyList()
            entityIndices = emptyList()
            inheritSuperIndices = false
        }
        context.checker.notBlank(tableName, element,
                ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY)

        val fieldIndices = pojo.fields
                .filter { it.indexed }.mapNotNull {
                    if (it.parent != null) {
                        it.indexed = false
                        context.logger.w(Warning.INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED, it.element,
                                ProcessorErrors.droppedEmbeddedFieldIndex(
                                        it.getPath(), element.qualifiedName.toString()))
                        null
                    } else if (it.element.enclosingElement != element && !inheritSuperIndices) {
                        it.indexed = false
                        context.logger.w(Warning.INDEX_FROM_PARENT_FIELD_IS_DROPPED,
                                ProcessorErrors.droppedSuperClassFieldIndex(
                                        it.columnName, element.toString(),
                                        it.element.enclosingElement.toString()
                                ))
                        null
                    } else {
                        IndexInput(
                                name = createIndexName(listOf(it.columnName), tableName),
                                unique = false,
                                columnNames = listOf(it.columnName)
                        )
                    }
                }
        val superIndices = loadSuperIndices(element.superclass, tableName, inheritSuperIndices)
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

        context.checker.check(SqlParser.isValidIdentifier(tableName), element,
                ProcessorErrors.INVALID_TABLE_NAME)
        pojo.fields.forEach {
            context.checker.check(SqlParser.isValidIdentifier(it.columnName), it.element,
                    ProcessorErrors.INVALID_COLUMN_NAME)
        }

        val entity = Entity(element = element,
                tableName = tableName,
                type = pojo.type,
                fields = pojo.fields,
                embeddedFields = pojo.embeddedFields,
                indices = indices,
                primaryKey = primaryKey,
                foreignKeys = entityForeignKeys,
                constructor = pojo.constructor)

        return entity
    }

    private fun checkIndicesForForeignKeys(entityForeignKeys: List<ForeignKey>,
                                           primaryKey: PrimaryKey,
                                           indices: List<Index>) {
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
                    context.logger.w(Warning.MISSING_INDEX_ON_FOREIGN_KEY_CHILD, element,
                            ProcessorErrors.foreignKeyMissingIndexInChildColumn(columnNames[0]))
                } else {
                    context.logger.w(Warning.MISSING_INDEX_ON_FOREIGN_KEY_CHILD, element,
                            ProcessorErrors.foreignKeyMissingIndexInChildColumns(columnNames))
                }
            }
        }
    }

    /**
     * Does a validation on foreign keys except the parent table's columns.
     */
    private fun validateAndCreateForeignKeyReferences(foreignKeyInputs: List<ForeignKeyInput>,
                                                      pojo: Pojo): List<ForeignKey> {
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
                context.logger.e(element, ProcessorErrors.foreignKeyColumnNumberMismatch(
                        it.childColumns, it.parentColumns
                ))
                return@map null
            }
            val parentElement = try {
                MoreTypes.asElement(it.parent) as TypeElement
            } catch (noClass: IllegalArgumentException) {
                context.logger.e(element, ProcessorErrors.FOREIGN_KEY_CANNOT_FIND_PARENT)
                return@map null
            }
            val parentAnnotation = MoreElements.getAnnotationMirror(parentElement,
                    androidx.room.Entity::class.java).orNull()
            if (parentAnnotation == null) {
                context.logger.e(element,
                        ProcessorErrors.foreignKeyNotAnEntity(parentElement.toString()))
                return@map null
            }
            val tableName = extractTableName(parentElement, parentAnnotation)
            val fields = it.childColumns.mapNotNull { columnName ->
                val field = pojo.fields.find { it.columnName == columnName }
                if (field == null) {
                    context.logger.e(pojo.element,
                            ProcessorErrors.foreignKeyChildColumnDoesNotExist(columnName,
                                    pojo.fields.map { it.columnName }))
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
            fields: List<Field>, embeddedFields: List<EmbeddedField>): PrimaryKey {
        val candidates = collectPrimaryKeysFromEntityAnnotations(element, fields) +
                collectPrimaryKeysFromPrimaryKeyAnnotations(fields) +
                collectPrimaryKeysFromEmbeddedFields(embeddedFields)

        context.checker.check(candidates.isNotEmpty(), element, ProcessorErrors.MISSING_PRIMARY_KEY)

        // 1. If a key is not autogenerated, but is Primary key or is part of Primary key we
        // force the @NonNull annotation. If the key is a single Primary Key, Integer or Long, we
        // don't force the @NonNull annotation since SQLite will automatically generate IDs.
        // 2. If a key is autogenerate, we generate NOT NULL in table spec, but we don't require
        // @NonNull annotation on the field itself.
        candidates.filter { candidate -> !candidate.autoGenerateId }
                .map { candidate ->
                    candidate.fields.map { field ->
                        if (candidate.fields.size > 1 ||
                                (candidate.fields.size == 1
                                        && field.affinity != SQLTypeAffinity.INTEGER)) {
                            context.checker.check(field.nonNull, field.element,
                                    ProcessorErrors.primaryKeyNull(field.getPath()))
                            // Validate parents for nullability
                            var parent = field.parent
                            while (parent != null) {
                                val parentField = parent.field
                                context.checker.check(parentField.nonNull,
                                        parentField.element,
                                        ProcessorErrors.primaryKeyNull(parentField.getPath()))
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
            MoreElements.getAnnotationMirror(field.element,
                    androidx.room.PrimaryKey::class.java).orNull()?.let {
                if (field.parent != null) {
                    // the field in the entity that contains this error.
                    val grandParentField = field.parent.mRootParent.field.element
                    // bound for entity.
                    context.fork(grandParentField).logger.w(
                            Warning.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED,
                            grandParentField,
                            ProcessorErrors.embeddedPrimaryKeyIsDropped(
                                    element.qualifiedName.toString(), field.name))
                    null
                } else {
                    PrimaryKey(declaredIn = field.element.enclosingElement,
                            fields = listOf(field),
                            autoGenerateId = AnnotationMirrors
                                    .getAnnotationValue(it, "autoGenerate")
                                    .getAsBoolean(false))
                }
            }
        }
    }

    /**
     * Check classes for @Entity(primaryKeys = ?).
     */
    private fun collectPrimaryKeysFromEntityAnnotations(
            typeElement: TypeElement, availableFields: List<Field>): List<PrimaryKey> {
        val myPkeys = MoreElements.getAnnotationMirror(typeElement,
                androidx.room.Entity::class.java).orNull()?.let {
            val primaryKeyColumns = AnnotationMirrors.getAnnotationValue(it, "primaryKeys")
                    .getAsStringList()
            if (primaryKeyColumns.isEmpty()) {
                emptyList()
            } else {
                val fields = primaryKeyColumns.mapNotNull { pKeyColumnName ->
                    val field = availableFields.firstOrNull { it.columnName == pKeyColumnName }
                    context.checker.check(field != null, typeElement,
                            ProcessorErrors.primaryKeyColumnDoesNotExist(pKeyColumnName,
                                    availableFields.map { it.columnName }))
                    field
                }
                listOf(PrimaryKey(declaredIn = typeElement,
                        fields = fields,
                        autoGenerateId = false))
            }
        } ?: emptyList()
        // checks supers.
        val mySuper = typeElement.superclass
        val superPKeys = if (mySuper != null && mySuper.kind != TypeKind.NONE) {
            // my super cannot see my fields so remove them.
            val remainingFields = availableFields.filterNot {
                it.element.enclosingElement == typeElement
            }
            collectPrimaryKeysFromEntityAnnotations(
                    MoreTypes.asTypeElement(mySuper), remainingFields)
        } else {
            emptyList()
        }
        return superPKeys + myPkeys
    }

    private fun collectPrimaryKeysFromEmbeddedFields(
            embeddedFields: List<EmbeddedField>): List<PrimaryKey> {
        return embeddedFields.mapNotNull { embeddedField ->
            MoreElements.getAnnotationMirror(embeddedField.field.element,
                    androidx.room.PrimaryKey::class.java).orNull()?.let {
                val autoGenerate = AnnotationMirrors
                        .getAnnotationValue(it, "autoGenerate").getAsBoolean(false)
                context.checker.check(!autoGenerate || embeddedField.pojo.fields.size == 1,
                        embeddedField.field.element,
                        ProcessorErrors.AUTO_INCREMENT_EMBEDDED_HAS_MULTIPLE_FIELDS)
                PrimaryKey(declaredIn = embeddedField.field.element.enclosingElement,
                        fields = embeddedField.pojo.fields,
                        autoGenerateId = autoGenerate)
            }
        }
    }

    // start from my element and check if anywhere in the list we can find the only well defined
    // pkey, if so, use it.
    private fun choosePrimaryKey(
            candidates: List<PrimaryKey>, typeElement: TypeElement): PrimaryKey {
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
                context.logger.d(element,
                        "${it.toHumanReadableString()} is" +
                                " overridden by ${myPKeys.first().toHumanReadableString()}")
            }
            myPKeys.first()
        } else if (myPKeys.isEmpty()) {
            // i have not declared anything, delegate to super
            val mySuper = typeElement.superclass
            if (mySuper != null && mySuper.kind != TypeKind.NONE) {
                return choosePrimaryKey(candidates, MoreTypes.asTypeElement(mySuper))
            }
            PrimaryKey.MISSING
        } else {
            context.logger.e(element, ProcessorErrors.multiplePrimaryKeyAnnotations(
                    myPKeys.map(PrimaryKey::toHumanReadableString)))
            PrimaryKey.MISSING
        }
    }

    private fun validateAndCreateIndices(
            inputs: List<IndexInput>, pojo: Pojo): List<Index> {
        // check for columns
        val indices = inputs.mapNotNull { input ->
            context.checker.check(input.columnNames.isNotEmpty(), element,
                    INDEX_COLUMNS_CANNOT_BE_EMPTY)
            val fields = input.columnNames.mapNotNull { columnName ->
                val field = pojo.fields.firstOrNull {
                    it.columnName == columnName
                }
                context.checker.check(field != null, element,
                        ProcessorErrors.indexColumnDoesNotExist(
                                columnName, pojo.fields.map { it.columnName }
                        ))
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
            val subEntityAnnotation = MoreElements.getAnnotationMirror(embeddedElement,
                    androidx.room.Entity::class.java).orNull()
            subEntityAnnotation?.let {
                val subIndices = extractIndices(subEntityAnnotation, "")
                if (subIndices.isNotEmpty()) {
                    context.logger.w(Warning.INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED,
                            embedded.field.element, ProcessorErrors.droppedEmbeddedIndex(
                            entityName = embedded.pojo.typeName.toString(),
                            fieldPath = embedded.field.getPath(),
                            grandParent = element.qualifiedName.toString()))
                }
            }
        }
        return indices
    }

    // check if parent is an Entity, if so, report its annotation indices
    private fun loadSuperIndices(
            typeMirror: TypeMirror?, tableName: String, inherit: Boolean): List<IndexInput> {
        if (typeMirror == null || typeMirror.kind == TypeKind.NONE) {
            return emptyList()
        }
        val parentElement = MoreTypes.asTypeElement(typeMirror)
        val myIndices = MoreElements.getAnnotationMirror(parentElement,
                androidx.room.Entity::class.java).orNull()?.let { annotation ->
            val indices = extractIndices(annotation, tableName = "super")
            if (indices.isEmpty()) {
                emptyList()
            } else if (inherit) {
                // rename them
                indices.map {
                    IndexInput(
                            name = createIndexName(it.columnNames, tableName),
                            unique = it.unique,
                            columnNames = it.columnNames)
                }
            } else {
                context.logger.w(Warning.INDEX_FROM_PARENT_IS_DROPPED,
                        parentElement,
                        ProcessorErrors.droppedSuperClassIndex(
                                childEntity = element.qualifiedName.toString(),
                                superEntity = parentElement.qualifiedName.toString()))
                emptyList()
            }
        } ?: emptyList()
        return myIndices + loadSuperIndices(parentElement.superclass, tableName, inherit)
    }

    companion object {
        fun extractTableName(element: TypeElement, annotation: AnnotationMirror): String {
            val annotationValue = AnnotationMirrors
                    .getAnnotationValue(annotation, "tableName").value.toString()
            return if (annotationValue == "") {
                element.simpleName.toString()
            } else {
                annotationValue
            }
        }

        private fun extractIndices(
                annotation: AnnotationMirror, tableName: String): List<IndexInput> {
            val arrayOfIndexAnnotations = AnnotationMirrors.getAnnotationValue(annotation,
                    "indices")
            return INDEX_LIST_VISITOR.visit(arrayOfIndexAnnotations, tableName)
        }

        private val INDEX_LIST_VISITOR = object
            : SimpleAnnotationValueVisitor6<List<IndexInput>, String>() {
            override fun visitArray(
                    values: MutableList<out AnnotationValue>?,
                    tableName: String
            ): List<IndexInput> {
                return values?.mapNotNull {
                    INDEX_VISITOR.visit(it, tableName)
                } ?: emptyList()
            }
        }

        private val INDEX_VISITOR = object : SimpleAnnotationValueVisitor6<IndexInput?, String>() {
            override fun visitAnnotation(a: AnnotationMirror?, tableName: String): IndexInput? {
                val fieldInput = getAnnotationValue(a, "value").getAsStringList()
                val unique = getAnnotationValue(a, "unique").getAsBoolean(false)
                val nameValue = getAnnotationValue(a, "name")
                        .getAsString("")
                val name = if (nameValue == null || nameValue == "") {
                    createIndexName(fieldInput, tableName)
                } else {
                    nameValue
                }
                return IndexInput(name, unique, fieldInput)
            }
        }

        private fun createIndexName(columnNames: List<String>, tableName: String): String {
            return Index.DEFAULT_PREFIX + tableName + "_" + columnNames.joinToString("_")
        }

        private fun extractForeignKeys(annotation: AnnotationMirror): List<ForeignKeyInput> {
            val arrayOfForeignKeyAnnotations = getAnnotationValue(annotation, "foreignKeys")
            return FOREIGN_KEY_LIST_VISITOR.visit(arrayOfForeignKeyAnnotations)
        }

        private val FOREIGN_KEY_LIST_VISITOR = object
            : SimpleAnnotationValueVisitor6<List<ForeignKeyInput>, Void?>() {
            override fun visitArray(
                    values: MutableList<out AnnotationValue>?,
                    void: Void?
            ): List<ForeignKeyInput> {
                return values?.mapNotNull {
                    FOREIGN_KEY_VISITOR.visit(it)
                } ?: emptyList()
            }
        }

        private val FOREIGN_KEY_VISITOR = object : SimpleAnnotationValueVisitor6<ForeignKeyInput?,
                Void?>() {
            override fun visitAnnotation(a: AnnotationMirror?, void: Void?): ForeignKeyInput? {
                val entityClass = try {
                    getAnnotationValue(a, "entity").toType()
                } catch (notPresent: TypeNotPresentException) {
                    return null
                }
                val parentColumns = getAnnotationValue(a, "parentColumns").getAsStringList()
                val childColumns = getAnnotationValue(a, "childColumns").getAsStringList()
                val onDeleteInput = getAnnotationValue(a, "onDelete").getAsInt()
                val onUpdateInput = getAnnotationValue(a, "onUpdate").getAsInt()
                val deferred = getAnnotationValue(a, "deferred").getAsBoolean(true)
                val onDelete = ForeignKeyAction.fromAnnotationValue(onDeleteInput)
                val onUpdate = ForeignKeyAction.fromAnnotationValue(onUpdateInput)
                return ForeignKeyInput(
                        parent = entityClass,
                        parentColumns = parentColumns,
                        childColumns = childColumns,
                        onDelete = onDelete,
                        onUpdate = onUpdate,
                        deferred = deferred)
            }
        }
    }

    /**
     * processed Index annotation output
     */
    data class IndexInput(val name: String, val unique: Boolean, val columnNames: List<String>)

    /**
     * ForeignKey, before it is processed in the context of a database.
     */
    data class ForeignKeyInput(
            val parent: TypeMirror,
            val parentColumns: List<String>,
            val childColumns: List<String>,
            val onDelete: ForeignKeyAction?,
            val onUpdate: ForeignKeyAction?,
            val deferred: Boolean)
}
