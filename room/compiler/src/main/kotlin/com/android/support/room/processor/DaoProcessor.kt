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

import com.android.support.room.Delete
import com.android.support.room.Insert
import com.android.support.room.Query
import com.android.support.room.SkipQueryVerification
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.hasAnyOf
import com.android.support.room.verifier.DatabaseVerifier
import com.android.support.room.vo.Dao
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.TypeElement

class DaoProcessor(val context : Context) {
    // TODO we should start injecting these to avoid parsing the same class multiple times
    val queryProcessor = QueryMethodProcessor(context)
    val insertionProcessor = InsertionMethodProcessor(context)
    val deletionProcessor = DeletionMethodProcessor(context)

    var dbVerifier: DatabaseVerifier? = null

    companion object {
        val PROCESSED_ANNOTATIONS = listOf(Insert::class, Delete::class, Query::class)
    }

    fun parse(element: TypeElement) : Dao {
        context.checker.hasAnnotation(element, com.android.support.room.Dao::class,
                ProcessorErrors.DAO_MUST_BE_ANNOTATED_WITH_DAO)
        context.checker.check(element.hasAnyOf(ABSTRACT) || element.kind == ElementKind.INTERFACE,
                element, ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE)

        val declaredType = MoreTypes.asDeclared(element.asType())
        val methods = context.processingEnv.elementUtils.getAllMembers(element)
            .filter {
                it.hasAnyOf(ABSTRACT) && it.kind == ElementKind.METHOD
            }.map {
                MoreElements.asExecutable(it)
            }.groupBy { method ->
                context.checker.check(
                        PROCESSED_ANNOTATIONS.count { method.hasAnnotation(it) } == 1, method,
                        ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_DAO_METHOD_ANNOTATION
                )
                if (method.hasAnnotation(Query::class)) {
                    Query::class
                } else if (method.hasAnnotation(Insert::class)) {
                    Insert::class
                } else if (method.hasAnnotation(Delete::class)) {
                    Delete::class
                } else {
                    Any::class
                }
            }
        queryProcessor.dbVerifier = if (element.hasAnnotation(SkipQueryVerification::class)) {
            null
        } else {
            dbVerifier
        }
        val queryMethods = methods[Query::class]?.map {
            queryProcessor.parse(declaredType, it)
        } ?: emptyList()

        val insertionMethods = methods[Insert::class]?.map {
            insertionProcessor.parse(declaredType, it)
        } ?: emptyList()

        val deletionMethods = methods[Delete::class]?.map {
            deletionProcessor.parse(declaredType, it)
        } ?: emptyList()

        context.checker.check(methods[Any::class] == null, element,
                ProcessorErrors.ABSTRACT_METHOD_IN_DAO_MISSING_ANY_ANNOTATION)

        val type = TypeName.get(declaredType)
        context.checker.notUnbound(type, element,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES)
        return Dao(element = element,
                type = declaredType,
                queryMethods = queryMethods,
                insertionMethods = insertionMethods,
                deletionMethods = deletionMethods)
    }
}
