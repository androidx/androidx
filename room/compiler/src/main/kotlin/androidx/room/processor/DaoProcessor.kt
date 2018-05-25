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

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.SkipQueryVerification
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.ext.findKotlinDefaultImpl
import androidx.room.ext.hasAnnotation
import androidx.room.ext.hasAnyOf
import androidx.room.ext.typeName
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

class DaoProcessor(baseContext: Context, val element: TypeElement, val dbType: DeclaredType,
                   val dbVerifier: DatabaseVerifier?) {
    val context = baseContext.fork(element)

    companion object {
        val PROCESSED_ANNOTATIONS = listOf(Insert::class, Delete::class, Query::class,
                Update::class, RawQuery::class)
    }

    fun process(): Dao {
        context.checker.hasAnnotation(element, androidx.room.Dao::class,
                ProcessorErrors.DAO_MUST_BE_ANNOTATED_WITH_DAO)
        context.checker.check(element.hasAnyOf(ABSTRACT) || element.kind == ElementKind.INTERFACE,
                element, ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE)

        val declaredType = MoreTypes.asDeclared(element.asType())
        val allMembers = context.processingEnv.elementUtils.getAllMembers(element)
        val methods = allMembers
            .filter {
                it.hasAnyOf(ABSTRACT) && it.kind == ElementKind.METHOD
                        && it.findKotlinDefaultImpl(context.processingEnv.typeUtils) == null
            }.map {
                MoreElements.asExecutable(it)
            }.groupBy { method ->
                context.checker.check(
                        PROCESSED_ANNOTATIONS.count { method.hasAnnotation(it) } == 1, method,
                        ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD
                )
                if (method.hasAnnotation(Query::class)) {
                    Query::class
                } else if (method.hasAnnotation(Insert::class)) {
                    Insert::class
                } else if (method.hasAnnotation(Delete::class)) {
                    Delete::class
                } else if (method.hasAnnotation(Update::class)) {
                    Update::class
                } else if (method.hasAnnotation(RawQuery::class)) {
                    RawQuery::class
                } else {
                    Any::class
                }
            }
        val processorVerifier = if (element.hasAnnotation(SkipQueryVerification::class) ||
                element.hasAnnotation(RawQuery::class)) {
            null
        } else {
            dbVerifier
        }

        val queryMethods = methods[Query::class]?.map {
            QueryMethodProcessor(
                    baseContext = context,
                    containing = declaredType,
                    executableElement = it,
                    dbVerifier = processorVerifier).process()
        } ?: emptyList()

        val rawQueryMethods = methods[RawQuery::class]?.map {
            RawQueryMethodProcessor(
                    baseContext = context,
                    containing = declaredType,
                    executableElement = it
            ).process()
        } ?: emptyList()

        val insertionMethods = methods[Insert::class]?.map {
            InsertionMethodProcessor(
                    baseContext = context,
                    containing = declaredType,
                    executableElement = it).process()
        } ?: emptyList()

        val deletionMethods = methods[Delete::class]?.map {
            DeletionMethodProcessor(
                    baseContext = context,
                    containing = declaredType,
                    executableElement = it).process()
        } ?: emptyList()

        val updateMethods = methods[Update::class]?.map {
            UpdateMethodProcessor(
                    baseContext = context,
                    containing = declaredType,
                    executableElement = it).process()
        } ?: emptyList()

        val transactionMethods = allMembers.filter { member ->
            member.hasAnnotation(Transaction::class)
                    && member.kind == ElementKind.METHOD
                    && PROCESSED_ANNOTATIONS.none { member.hasAnnotation(it) }
        }.map {
            TransactionMethodProcessor(
                    baseContext = context,
                    containing = declaredType,
                    executableElement = MoreElements.asExecutable(it)).process()
        }

        val constructors = allMembers
                .filter { it.kind == ElementKind.CONSTRUCTOR }
                .map { MoreElements.asExecutable(it) }
        val typeUtils = context.processingEnv.typeUtils
        val goodConstructor = constructors.firstOrNull {
            it.parameters.size == 1
                    && typeUtils.isAssignable(dbType, it.parameters[0].asType())
        }
        val constructorParamType = if (goodConstructor != null) {
            goodConstructor.parameters[0].asType().typeName()
        } else {
            validateEmptyConstructor(constructors)
            null
        }

        context.checker.check(methods[Any::class] == null, element,
                ProcessorErrors.ABSTRACT_METHOD_IN_DAO_MISSING_ANY_ANNOTATION)

        val type = TypeName.get(declaredType)
        context.checker.notUnbound(type, element,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES)

        return Dao(element = element,
                type = declaredType,
                queryMethods = queryMethods,
                rawQueryMethods = rawQueryMethods,
                insertionMethods = insertionMethods,
                deletionMethods = deletionMethods,
                updateMethods = updateMethods,
                transactionMethods = transactionMethods,
                constructorParamType = constructorParamType)
    }

    private fun validateEmptyConstructor(constructors: List<ExecutableElement>) {
        if (constructors.isNotEmpty() && constructors.all { it.parameters.isNotEmpty() }) {
            context.logger.e(element, ProcessorErrors.daoMustHaveMatchingConstructor(
                    element.toString(), dbType.toString()))
        }
    }
}
