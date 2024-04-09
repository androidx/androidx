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
import androidx.room.Upsert
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import androidx.room.vo.KotlinBoxedPrimitiveMethodDelegate
import androidx.room.vo.KotlinDefaultMethodDelegate
import androidx.room.vo.Warning

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
            Update::class, Upsert::class, RawQuery::class
        )
    }

    fun process(): Dao {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
            return Dao(
                element = element,
                type = element.type,
                queryMethods = emptyList(),
                rawQueryMethods = emptyList(),
                insertMethods = emptyList(),
                upsertMethods = emptyList(),
                deleteMethods = emptyList(),
                updateMethods = emptyList(),
                transactionMethods = emptyList(),
                kotlinBoxedPrimitiveMethodDelegates = emptyList(),
                kotlinDefaultMethodDelegates = emptyList(),
                constructorParamType = null
            )
        }
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
                if (method.hasAnnotation(JvmName::class)) {
                    context.logger.w(
                        Warning.JVM_NAME_ON_OVERRIDDEN_METHOD,
                        method,
                        ProcessorErrors.JVM_NAME_ON_OVERRIDDEN_METHOD
                    )
                }
                if (
                    context.codeLanguage == CodeLanguage.KOTLIN &&
                    method.isKotlinPropertyMethod()
                ) {
                    context.logger.e(method, ProcessorErrors.KOTLIN_PROPERTY_OVERRIDE)
                }
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
                } else if (method.hasAnnotation(Upsert::class)) {
                    Upsert::class
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

        val insertMethods = methods[Insert::class]?.map {
            InsertMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val deleteMethods = methods[Delete::class]?.map {
            DeleteMethodProcessor(
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

        val upsertMethods = methods[Upsert::class]?.map {
            UpsertMethodProcessor(
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

        // Only try to find Kotlin boxed bridge methods when the dao extends a class or
        // implements an interface since otherwise there are no bridge method generated by
        // Kotlin.
        val unannotatedMethods = methods[Any::class] ?: emptyList()
        val kotlinBoxedPrimitiveBridgeMethods =
            if (element.superClass != null || element.getSuperInterfaceElements().isNotEmpty()) {
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
            goodConstructor.parameters[0].type.asTypeName()
        } else {
            validateEmptyConstructor(constructors)
            null
        }

        context.checker.notUnbound(
            declaredType, element,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES
        )

        val invalidAnnotatedMethods =
            unannotatedMethods - kotlinBoxedPrimitiveBridgeMethods.map { it.element }
        invalidAnnotatedMethods.forEach {
            context.logger.e(it, ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
        }

        return Dao(
            element = element,
            type = declaredType,
            queryMethods = queryMethods,
            rawQueryMethods = rawQueryMethods,
            insertMethods = insertMethods,
            deleteMethods = deleteMethods,
            updateMethods = updateMethods,
            upsertMethods = upsertMethods,
            transactionMethods = transactionMethods.toList(),
            kotlinBoxedPrimitiveMethodDelegates = kotlinBoxedPrimitiveBridgeMethods,
            kotlinDefaultMethodDelegates = kotlinDefaultMethodDelegates.toList(),
            constructorParamType = constructorParamType
        )
    }

    private fun validateEmptyConstructor(constructors: List<XConstructorElement>) {
        if (constructors.isNotEmpty() && constructors.all { it.parameters.isNotEmpty() }) {
            context.logger.e(
                element,
                ProcessorErrors.daoMustHaveMatchingConstructor(
                    element.qualifiedName, dbType.asTypeName().toString(context.codeLanguage)
                )
            )
        }
    }

    /**
     * Find Kotlin bridge methods generated for overrides of primitives, see KT-46650.
     * When generating the Java implementation of the DAO, Room needs to also override the bridge
     * method generated by Kotlin for the boxed version, it will contain the same name, return type
     * and parameter, but the generic primitive params will be boxed.
     */
    private fun matchKotlinBoxedPrimitiveMethods(
        unannotatedMethods: List<XMethodElement>,
        annotatedMethods: List<XMethodElement>
    ) = unannotatedMethods.mapNotNull { unannotated ->
        annotatedMethods.firstOrNull {
            if (it.jvmName != unannotated.jvmName) {
                return@firstOrNull false
            }
            if (it.parameters.size != unannotated.parameters.size) {
                return@firstOrNull false
            }

            // Get unannotated as a member of annotated's enclosing type before comparing
            // in case unannotated contains type parameters that need to be resolved.
            val annotatedEnclosingType = it.enclosingElement.type
            val unannotatedType = if (annotatedEnclosingType == null) {
                unannotated.executableType
            } else {
                unannotated.asMemberOf(annotatedEnclosingType)
            }

            if (!it.returnType.boxed().isSameType(unannotatedType.returnType.boxed())) {
                return@firstOrNull false
            }
            for (i in it.parameters.indices) {
                if (it.parameters[i].type.boxed() != unannotatedType.parameterTypes[i].boxed()) {
                    return@firstOrNull false
                }
            }
            return@firstOrNull true
        }?.let { matchingMethod -> KotlinBoxedPrimitiveMethodDelegate(unannotated, matchingMethod) }
    }
}
