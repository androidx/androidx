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

import com.android.support.room.ext.hasAnyOf
import com.android.support.room.preconditions.Checks
import com.android.support.room.vo.DaoMethod
import com.android.support.room.vo.Database
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor6


class DatabaseProcessor(val context: Context) {
    val entityParser = EntityProcessor(context)
    val daoParser = DaoProcessor(context)

    fun parse(element: TypeElement): Database {
        Checks.hasAnnotation(element, com.android.support.room.Database::class,
                ProcessorErrors.DATABASE_MUST_BE_ANNOTATED_WITH_DATABASE)
        val dbAnnotation = MoreElements
                .getAnnotationMirror(element, com.android.support.room.Database::class.java)
                .get()
        val entityList = AnnotationMirrors.getAnnotationValue(dbAnnotation, "entities")
        val listOfTypes = TO_LIST_OF_TYPES.visit(entityList, "entities")
        Checks.check(listOfTypes.isNotEmpty(), element,
                ProcessorErrors.DATABASE_ANNOTATION_MUST_HAVE_LIST_OF_ENTITIES)

        val entities = listOfTypes.map {
            entityParser.parse(MoreTypes.asTypeElement(it))
        }

        val allMembers = context.processingEnv.elementUtils.getAllMembers(element)
        val daoMethods = allMembers.filter {
            it.hasAnyOf(Modifier.ABSTRACT) && it.kind == ElementKind.METHOD
        }.map {
            val executable = MoreElements.asExecutable(it)
            // TODO when we add support for non Dao return types (e.g. database), this code needs
            // to change
            val dao = daoParser.parse(MoreTypes.asTypeElement(executable.returnType))
            DaoMethod(executable, executable.simpleName.toString(), dao)
        }
        return Database(element = element,
                entities = entities,
                daoMethods = daoMethods)
    }

    // code below taken from dagger2
    // compiler/src/main/java/dagger/internal/codegen/ConfigurationAnnotations.java
    private val TO_LIST_OF_TYPES = object
        : SimpleAnnotationValueVisitor6<List<TypeMirror>, String>() {

        override fun visitArray(vals: List<AnnotationValue>, elementName: String)
                : List<TypeMirror> {
            return vals.map {
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
