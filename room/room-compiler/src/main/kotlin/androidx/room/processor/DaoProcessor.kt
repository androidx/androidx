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
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import androidx.room.vo.KotlinBoxedPrimitiveMethodDelegate
import androidx.room.vo.KotlinDefaultMethodDelegate

class DaoProcessor(
    baseContext: Context,
    val element: XTypeElement,
    val dbType: XType,
    val dbVerifier: DatabaseVerifier?
) {
    val context = baseContext.fork(element)

    companion object {
        val PROCESSED_ANNOTATIONS = listOf(
            Insert::class, Delete::class, Query::class,
            Update::class, RawQuery::class
        )
    }

    fun process(): Dao {
        context.checker.hasAnnotation(
            element, androidx.room.Dao::class,
            ProcessorErrors.DAO_MUST_BE_ANNOTATED_WITH_DAO
        )
        context.checker.check(
            element.isAbstract() || element.isInterface(),
            element, ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE
        )

        val declaredType = element.type
        val allMethods = element.getAllMethods()
        val methods = allMethods
            .filter {
                it.isAbstract() && !it.hasKotlinDefaultImpl()
            }.groupBy { method ->
                context.checker.check(
                    PROCESSED_ANNOTATIONS.count { method.hasAnnotation(it) } <= 1, method,
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
            element.hasAnnotation(RawQuery::class)
        ) {
            null
        } else {
            dbVerifier
        }

        val queryMethods = methods[Query::class]?.map {
            QueryMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it,
                dbVerifier = processorVerifier
            ).process()
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
                executableElement = it
            ).process()
        } ?: emptyList()

        val deletionMethods = methods[Delete::class]?.map {
            DeletionMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val updateMethods = methods[Update::class]?.map {
            UpdateMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val transactionMethods = allMethods.filter { member ->
            member.hasAnnotation(Transaction::class) &&
                PROCESSED_ANNOTATIONS.none { member.hasAnnotation(it) }
        }.map {
            TransactionMethodProcessor(
                baseContext = context,
                containingElement = element,
                containingType = declaredType,
                executableElement = it
            ).process()
        }

        // Only try to find kotlin boxed delegating methods when the dao extends a class or
        // implements an interface since otherwise there are no duplicated method generated by
        // Kotlin.
        val unannotatedMethods = methods[Any::class] ?: emptyList<XMethodElement>()
        val delegatingMethods =
            if (element.superType != null ||
                element.getSuperInterfaceElements().isNotEmpty()
            ) {
                matchKotlinBoxedPrimitiveMethods(
                    unannotatedMethods,
                    methods.values.flatten() - unannotatedMethods
                )
            } else {
                emptyList()
            }

        val kotlinDefaultMethodDelegates = if (element.isInterface()) {
            val allProcessedMethods =
                methods.values.flatten() + transactionMethods.map { it.element }
            allMethods.filterNot {
                allProcessedMethods.contains(it)
            }.mapNotNull { method ->
                if (method.hasKotlinDefaultImpl()) {
                    KotlinDefaultMethodDelegate(
                        element = method
                    )
                } else {
                    null
                }
            }
        } else {
            emptySequence()
        }

        val constructors = element.getConstructors()
        val goodConstructor = constructors.firstOrNull {
            it.parameters.size == 1 &&
                it.parameters[0].type.isAssignableFrom(dbType)
        }
        val constructorParamType = if (goodConstructor != null) {
            goodConstructor.parameters[0].type.typeName
        } else {
            validateEmptyConstructor(constructors)
            null
        }

        val type = declaredType.typeName
        context.checker.notUnbound(
            type, element,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES
        )

        (unannotatedMethods - delegatingMethods.map { it.element }).forEach { method ->
            context.logger.e(method, ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
        }

        return Dao(
            element = element,
            type = declaredType,
            queryMethods = queryMethods,
            rawQueryMethods = rawQueryMethods,
            insertionMethods = insertionMethods,
            deletionMethods = deletionMethods,
            updateMethods = updateMethods,
            transactionMethods = transactionMethods.toList(),
            delegatingMethods = delegatingMethods,
            kotlinDefaultMethodDelegates = kotlinDefaultMethodDelegates.toList(),
            constructorParamType = constructorParamType
        )
    }

    private fun validateEmptyConstructor(constructors: List<XConstructorElement>) {
        if (constructors.isNotEmpty() && constructors.all { it.parameters.isNotEmpty() }) {
            context.logger.e(
                element,
                ProcessorErrors.daoMustHaveMatchingConstructor(
                    element.qualifiedName, dbType.typeName.toString()
                )
            )
        }
    }

    private fun matchKotlinBoxedPrimitiveMethods(
        unannotatedMethods: List<XMethodElement>,
        annotatedMethods: List<XMethodElement>
    ) = unannotatedMethods.mapNotNull { unannotated ->
        annotatedMethods.firstOrNull {
            if (it.name != unannotated.name) {
                return@firstOrNull false
            }
            if (!it.returnType.boxed().isSameType(unannotated.returnType.boxed())) {
                return@firstOrNull false
            }
            if (it.parameters.size != unannotated.parameters.size) {
                return@firstOrNull false
            }
            for (i in it.parameters.indices) {
                if (it.parameters[i].type.boxed() != unannotated.parameters[i].type.boxed()) {
                    return@firstOrNull false
                }
            }
            return@firstOrNull true
        }?.let { matchingMethod -> KotlinBoxedPrimitiveMethodDelegate(unannotated, matchingMethod) }
    }
}