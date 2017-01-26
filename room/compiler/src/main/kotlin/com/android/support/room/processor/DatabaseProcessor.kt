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

import com.android.support.room.SkipQueryVerification
import com.android.support.room.ext.RoomTypeNames
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.verifier.DatabaseVerifier
import com.android.support.room.vo.DaoMethod
import com.android.support.room.vo.Database
import com.android.support.room.vo.Entity
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor6


class DatabaseProcessor(val context: Context) {
    val entityParser = EntityProcessor(context)
    val daoParser = DaoProcessor(context)

    val baseClassElement: TypeMirror by lazy {
        context.processingEnv.elementUtils.getTypeElement(
                RoomTypeNames.ROOM_DB.packageName() + "." + RoomTypeNames.ROOM_DB.simpleName())
                .asType()
    }

    fun parse(element: TypeElement): Database {
        val dbAnnotation = MoreElements
                .getAnnotationMirror(element, com.android.support.room.Database::class.java)
                .orNull()
        val entities = processEntities(dbAnnotation, element)
        validateUniqueTableNames(element, entities)
        val extendsRoomDb = context.processingEnv.typeUtils.isAssignable(
                MoreElements.asType(element).asType(), baseClassElement)
        context.checker.check(extendsRoomDb, element, ProcessorErrors.DB_MUST_EXTEND_ROOM_DB)

        val allMembers = context.processingEnv.elementUtils.getAllMembers(element)

        if (!element.hasAnnotation(SkipQueryVerification::class)) {
            daoParser.dbVerifier = DatabaseVerifier.create(context, element, entities)
        }

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
            val dao = daoParser.parse(MoreTypes.asTypeElement(executable.returnType))
            DaoMethod(executable, executable.simpleName.toString(), dao)
        }
        validateUniqueDaoClasses(element, daoMethods)
        val database = Database(element = element,
                type = MoreElements.asType(element).asType(),
                entities = entities,
                daoMethods = daoMethods,
                suppressedWarnings = SuppressWarningProcessor.getSuppressedWarnings(element))
        return database
    }

    private fun validateUniqueDaoClasses(dbElement: TypeElement, daoMethods: List<DaoMethod>) {
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
    }

    private fun validateUniqueTableNames(dbElement : TypeElement, entities : List<Entity>) {
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
        val listOfTypes = TO_LIST_OF_TYPES.visit(entityList, "entities")
        context.checker.check(listOfTypes.isNotEmpty(), element,
                ProcessorErrors.DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES)
        return listOfTypes.map {
            entityParser.parse(MoreTypes.asTypeElement(it))
        }
    }

    // code below taken from dagger2
    // compiler/src/main/java/dagger/internal/codegen/ConfigurationAnnotations.java
    private val TO_LIST_OF_TYPES = object
        : SimpleAnnotationValueVisitor6<List<TypeMirror>, String>() {

        override fun visitArray(values: List<AnnotationValue>, elementName: String)
                : List<TypeMirror> {
            return values.map {
                val tmp = TO_TYPE.visit(it)
                tmp
            }
        }

        override fun defaultAction(o: Any?, elementName: String?): List<TypeMirror> {
            throw IllegalArgumentException(elementName + " is not an array: " + o)
        }
    }

    private val TO_TYPE = object : SimpleAnnotationValueVisitor6<TypeMirror, Void>() {

        override fun visitType(t: TypeMirror, p: Void?): TypeMirror {
            return t
        }

        override fun defaultAction(o: Any?, p: Void?): TypeMirror {
            throw TypeNotPresentException(o!!.toString(), null)
        }
    }

}
