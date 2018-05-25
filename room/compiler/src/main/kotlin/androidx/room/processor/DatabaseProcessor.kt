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

import androidx.room.SkipQueryVerification
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.getAsBoolean
import androidx.room.ext.getAsInt
import androidx.room.ext.hasAnnotation
import androidx.room.ext.hasAnyOf
import androidx.room.ext.toListOfClassTypes
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import androidx.room.vo.DaoMethod
import androidx.room.vo.Database
import androidx.room.vo.Entity
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

class DatabaseProcessor(baseContext: Context, val element: TypeElement) {
    val context = baseContext.fork(element)

    val baseClassElement: TypeMirror by lazy {
        context.processingEnv.elementUtils.getTypeElement(
                RoomTypeNames.ROOM_DB.packageName() + "." + RoomTypeNames.ROOM_DB.simpleName())
                .asType()
    }

    fun process(): Database {
        try {
            return doProcess()
        } finally {
            context.databaseVerifier?.closeConnection(context)
        }
    }

    private fun doProcess(): Database {
        val dbAnnotation = MoreElements
                .getAnnotationMirror(element, androidx.room.Database::class.java)
                .orNull()
        val entities = processEntities(dbAnnotation, element)
        validateUniqueTableNames(element, entities)
        validateForeignKeys(element, entities)

        val extendsRoomDb = context.processingEnv.typeUtils.isAssignable(
                MoreElements.asType(element).asType(), baseClassElement)
        context.checker.check(extendsRoomDb, element, ProcessorErrors.DB_MUST_EXTEND_ROOM_DB)

        val allMembers = context.processingEnv.elementUtils.getAllMembers(element)

        val dbVerifier = if (element.hasAnnotation(SkipQueryVerification::class)) {
            null
        } else {
            DatabaseVerifier.create(context, element, entities)
        }
        context.databaseVerifier = dbVerifier

        val declaredType = MoreTypes.asDeclared(element.asType())
        val daoMethods = allMembers.filter {
            it.hasAnyOf(Modifier.ABSTRACT) && it.kind == ElementKind.METHOD
        }.filterNot {
            // remove methods that belong to room
            val containing = it.enclosingElement
            MoreElements.isType(containing) &&
                    TypeName.get(containing.asType()) == RoomTypeNames.ROOM_DB
        }.map {
            val executable = MoreElements.asExecutable(it)
            // TODO when we add support for non Dao return types (e.g. database), this code needs
            // to change
            val daoType = MoreTypes.asTypeElement(executable.returnType)
            val dao = DaoProcessor(context, daoType, declaredType, dbVerifier).process()
            DaoMethod(executable, executable.simpleName.toString(), dao)
        }
        validateUniqueDaoClasses(element, daoMethods, entities)
        validateUniqueIndices(element, entities)
        val version = AnnotationMirrors.getAnnotationValue(dbAnnotation, "version")
                .getAsInt(1)!!.toInt()
        val exportSchema = AnnotationMirrors.getAnnotationValue(dbAnnotation, "exportSchema")
                .getAsBoolean(true)

        val hasForeignKeys = entities.any { it.foreignKeys.isNotEmpty() }

        val database = Database(
                version = version,
                element = element,
                type = MoreElements.asType(element).asType(),
                entities = entities,
                daoMethods = daoMethods,
                exportSchema = exportSchema,
                enableForeignKeys = hasForeignKeys)
        return database
    }

    private fun validateForeignKeys(element: TypeElement, entities: List<Entity>) {
        val byTableName = entities.associateBy { it.tableName }
        entities.forEach { entity ->
            entity.foreignKeys.forEach foreignKeyLoop@ { foreignKey ->
                val parent = byTableName[foreignKey.parentTable]
                if (parent == null) {
                    context.logger.e(element, ProcessorErrors
                            .foreignKeyMissingParentEntityInDatabase(foreignKey.parentTable,
                                    entity.element.qualifiedName.toString()))
                    return@foreignKeyLoop
                }
                val parentFields = foreignKey.parentColumns.mapNotNull { columnName ->
                    val parentField = parent.fields.find {
                        it.columnName == columnName
                    }
                    if (parentField == null) {
                        context.logger.e(entity.element,
                                ProcessorErrors.foreignKeyParentColumnDoesNotExist(
                                        parentEntity = parent.element.qualifiedName.toString(),
                                        missingColumn = columnName,
                                        allColumns = parent.fields.map { it.columnName }))
                    }
                    parentField
                }
                if (parentFields.size != foreignKey.parentColumns.size) {
                    return@foreignKeyLoop
                }
                // ensure that it is indexed in the parent
                if (!parent.isUnique(foreignKey.parentColumns)) {
                    context.logger.e(parent.element, ProcessorErrors
                            .foreignKeyMissingIndexInParent(
                                    parentEntity = parent.element.qualifiedName.toString(),
                                    childEntity = entity.element.qualifiedName.toString(),
                                    parentColumns = foreignKey.parentColumns,
                                    childColumns = foreignKey.childFields
                                            .map { it.columnName }))
                    return@foreignKeyLoop
                }
            }
        }
    }

    private fun validateUniqueIndices(element: TypeElement, entities: List<Entity>) {
        entities
                .flatMap { entity ->
                    // associate each index with its entity
                    entity.indices.map { Pair(it.name, entity) }
                }
                .groupBy { it.first } // group by index name
                .filter { it.value.size > 1 } // get the ones with duplicate names
                .forEach {
                    // do not report duplicates from the same entity
                    if (it.value.distinctBy { it.second.typeName }.size > 1) {
                        context.logger.e(element,
                                ProcessorErrors.duplicateIndexInDatabase(it.key,
                                        it.value.map { "${it.second.typeName} > ${it.first}" }))
                    }
                }
    }

    private fun validateUniqueDaoClasses(dbElement: TypeElement, daoMethods: List<DaoMethod>,
                                         entities: List<Entity>) {
        val entityTypeNames = entities.map { it.typeName }.toSet()
        daoMethods.groupBy { it.dao.typeName }
                .forEach {
                    if (it.value.size > 1) {
                        val error = ProcessorErrors.duplicateDao(it.key, it.value.map { it.name })
                        it.value.forEach { daoMethod ->
                            context.logger.e(daoMethod.element,
                                    ProcessorErrors.DAO_METHOD_CONFLICTS_WITH_OTHERS)
                        }
                        // also report the full error for the database
                        context.logger.e(dbElement, error)
                    }
                }
        val check = fun(element: Element, dao: Dao,
                        typeName: TypeName?) {
            typeName?.let {
                if (!entityTypeNames.contains(typeName)) {
                    context.logger.e(element,
                            ProcessorErrors.shortcutEntityIsNotInDatabase(
                                    database = dbElement.qualifiedName.toString(),
                                    dao = dao.typeName.toString(),
                                    entity = typeName.toString()
                            ))
                }
            }
        }
        daoMethods.forEach { daoMethod ->
            daoMethod.dao.shortcutMethods.forEach { method ->
                method.entities.forEach {
                    check(method.element, daoMethod.dao, it.value.typeName)
                }
            }
            daoMethod.dao.insertionMethods.forEach { method ->
                method.entities.forEach {
                    check(method.element, daoMethod.dao, it.value.typeName)
                }
            }
        }
    }

    private fun validateUniqueTableNames(dbElement: TypeElement, entities: List<Entity>) {
        entities
                .groupBy {
                    it.tableName.toLowerCase()
                }.filter {
            it.value.size > 1
        }.forEach { byTableName ->
            val error = ProcessorErrors.duplicateTableNames(byTableName.key,
                    byTableName.value.map { it.typeName.toString() })
            // report it for each of them and the database to make it easier
            // for the developer
            byTableName.value.forEach { entity ->
                context.logger.e(entity.element, error)
            }
            context.logger.e(dbElement, error)
        }
    }

    private fun processEntities(dbAnnotation: AnnotationMirror?, element: TypeElement):
            List<Entity> {
        if (!context.checker.check(dbAnnotation != null, element,
                ProcessorErrors.DATABASE_MUST_BE_ANNOTATED_WITH_DATABASE)) {
            return listOf()
        }

        val entityList = AnnotationMirrors.getAnnotationValue(dbAnnotation, "entities")
        val listOfTypes = entityList.toListOfClassTypes()
        context.checker.check(listOfTypes.isNotEmpty(), element,
                ProcessorErrors.DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES)
        return listOfTypes.map {
            EntityProcessor(context, MoreTypes.asTypeElement(it)).process()
        }
    }
}
