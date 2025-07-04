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
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.Dao
import androidx.room.vo.KotlinBoxedPrimitiveFunctionDelegate
import androidx.room.vo.KotlinDefaultFunctionDelegate
import androidx.room.vo.Warning

class DaoProcessor(
    baseContext: Context,
    val element: XTypeElement,
    val dbType: XType,
    val dbVerifier: DatabaseVerifier?,
) {
    val context = baseContext.fork(element)

    companion object {
        val PROCESSED_ANNOTATIONS =
            listOf(
                Insert::class,
                Delete::class,
                Query::class,
                Update::class,
                Upsert::class,
                RawQuery::class,
            )
    }

    fun process(): Dao {
        if (!element.validate()) {
            context.reportMissingTypeReference(element.qualifiedName)
            return Dao(
                element = element,
                type = element.type,
                queryFunctions = emptyList(),
                rawQueryFunctions = emptyList(),
                insertFunctions = emptyList(),
                upsertFunctions = emptyList(),
                deleteFunctions = emptyList(),
                updateFunctions = emptyList(),
                transactionFunctions = emptyList(),
                kotlinBoxedPrimitiveFunctionDelegates = emptyList(),
                kotlinDefaultFunctionDelegates = emptyList(),
                constructorParamType = null,
            )
        }
        context.checker.hasAnnotation(
            element,
            androidx.room.Dao::class,
            ProcessorErrors.DAO_MUST_BE_ANNOTATED_WITH_DAO,
        )
        context.checker.check(
            element.isAbstract() || element.isInterface(),
            element,
            ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE,
        )

        val declaredType = element.type
        val allFunctions = element.getAllMethods()
        val functions =
            allFunctions
                .filter { it.isAbstract() && !it.hasKotlinDefaultImpl() }
                .groupBy { function ->
                    if (function.isKotlinPropertyMethod()) {
                        context.checker.check(
                            predicate = function.hasAnnotation(Query::class),
                            element = function,
                            errorMsg = ProcessorErrors.INVALID_ANNOTATION_IN_DAO_PROPERTY,
                        )
                    } else {
                        context.checker.check(
                            predicate =
                                PROCESSED_ANNOTATIONS.count { function.hasAnnotation(it) } <= 1,
                            element = function,
                            errorMsg = ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_FUNCTION,
                        )
                    }
                    if (function.hasAnnotation(JvmName::class)) {
                        context.logger.w(
                            Warning.JVM_NAME_ON_OVERRIDDEN_FUNCTION,
                            function,
                            ProcessorErrors.JVM_NAME_ON_OVERRIDDEN_FUNCTION,
                        )
                    }
                    if (function.hasAnnotation(Query::class)) {
                        Query::class
                    } else if (function.hasAnnotation(Insert::class)) {
                        Insert::class
                    } else if (function.hasAnnotation(Delete::class)) {
                        Delete::class
                    } else if (function.hasAnnotation(Update::class)) {
                        Update::class
                    } else if (function.hasAnnotation(RawQuery::class)) {
                        RawQuery::class
                    } else if (function.hasAnnotation(Upsert::class)) {
                        Upsert::class
                    } else {
                        Any::class
                    }
                }

        val processorVerifier =
            if (
                element.hasAnnotation(SkipQueryVerification::class) ||
                    element.hasAnnotation(RawQuery::class)
            ) {
                null
            } else {
                dbVerifier
            }

        val queryFunctions =
            functions[Query::class]?.map {
                QueryFunctionProcessor(
                        baseContext = context,
                        containing = declaredType,
                        executableElement = it,
                        dbVerifier = processorVerifier,
                    )
                    .process()
            } ?: emptyList()

        val rawQueryFunctions =
            functions[RawQuery::class]?.map {
                RawQueryFunctionProcessor(
                        baseContext = context,
                        containing = declaredType,
                        executableElement = it,
                    )
                    .process()
            } ?: emptyList()

        val insertFunctions =
            functions[Insert::class]?.map {
                InsertFunctionProcessor(
                        baseContext = context,
                        containing = declaredType,
                        executableElement = it,
                    )
                    .process()
            } ?: emptyList()

        val deleteFunctions =
            functions[Delete::class]?.map {
                DeleteFunctionProcessor(
                        baseContext = context,
                        containing = declaredType,
                        executableElement = it,
                    )
                    .process()
            } ?: emptyList()

        val updateFunctions =
            functions[Update::class]?.map {
                UpdateFunctionProcessor(
                        baseContext = context,
                        containing = declaredType,
                        executableElement = it,
                    )
                    .process()
            } ?: emptyList()

        val upsertFunctions =
            functions[Upsert::class]?.map {
                UpsertFunctionProcessor(
                        baseContext = context,
                        containing = declaredType,
                        executableElement = it,
                    )
                    .process()
            } ?: emptyList()

        val transactionFunctions =
            allFunctions
                .filter { member ->
                    member.hasAnnotation(Transaction::class) &&
                        PROCESSED_ANNOTATIONS.none { member.hasAnnotation(it) }
                }
                .map {
                    TransactionFunctionProcessor(
                            baseContext = context,
                            containingElement = element,
                            containingType = declaredType,
                            executableElement = it,
                        )
                        .process()
                }

        // Only try to find Kotlin boxed bridge functions when the dao extends a class or
        // implements an interface since otherwise there are no bridge function generated by
        // Kotlin.
        val unannotatedFunctions = functions[Any::class] ?: emptyList()
        val kotlinBoxedPrimitiveBridgeFunctions =
            if (element.superClass != null || element.getSuperInterfaceElements().isNotEmpty()) {
                matchKotlinBoxedPrimitiveFunctions(
                    unannotatedFunctions,
                    functions.values.flatten() - unannotatedFunctions,
                )
            } else {
                emptyList()
            }

        val kotlinDefaultFunctionDelegates =
            if (element.isInterface()) {
                val allProcessedFunctions =
                    functions.values.flatten() + transactionFunctions.map { it.element }
                allFunctions
                    .filterNot { allProcessedFunctions.contains(it) }
                    .mapNotNull { function ->
                        if (function.hasKotlinDefaultImpl()) {
                            KotlinDefaultFunctionDelegate(element = function)
                        } else {
                            null
                        }
                    }
            } else {
                emptySequence()
            }

        val constructors = element.getConstructors()
        val goodConstructor =
            constructors.firstOrNull {
                it.parameters.size == 1 && it.parameters[0].type.isAssignableFrom(dbType)
            }
        val constructorParamType =
            if (goodConstructor != null) {
                goodConstructor.parameters[0].type.asTypeName()
            } else {
                validateEmptyConstructor(constructors)
                null
            }

        context.checker.notUnbound(
            declaredType,
            element,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES,
        )

        val invalidAnnotatedFunctions =
            unannotatedFunctions - kotlinBoxedPrimitiveBridgeFunctions.map { it.element }
        invalidAnnotatedFunctions.forEach {
            context.logger.e(it, ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_FUNCTION)
        }

        return Dao(
            element = element,
            type = declaredType,
            queryFunctions = queryFunctions,
            rawQueryFunctions = rawQueryFunctions,
            insertFunctions = insertFunctions,
            deleteFunctions = deleteFunctions,
            updateFunctions = updateFunctions,
            upsertFunctions = upsertFunctions,
            transactionFunctions = transactionFunctions.toList(),
            kotlinBoxedPrimitiveFunctionDelegates = kotlinBoxedPrimitiveBridgeFunctions,
            kotlinDefaultFunctionDelegates = kotlinDefaultFunctionDelegates.toList(),
            constructorParamType = constructorParamType,
        )
    }

    private fun validateEmptyConstructor(constructors: List<XConstructorElement>) {
        if (constructors.isNotEmpty() && constructors.all { it.parameters.isNotEmpty() }) {
            context.logger.e(
                element,
                ProcessorErrors.daoMustHaveMatchingConstructor(
                    element.qualifiedName,
                    dbType.asTypeName().toString(context.codeLanguage),
                ),
            )
        }
    }

    /**
     * Find Kotlin bridge functions generated for overrides of primitives, see KT-46650. When
     * generating the Java implementation of the DAO, Room needs to also override the bridge
     * function generated by Kotlin for the boxed version, it will contain the same name, return
     * type and parameter, but the generic primitive params will be boxed.
     */
    private fun matchKotlinBoxedPrimitiveFunctions(
        unannotatedFunctions: List<XMethodElement>,
        annotatedFunctions: List<XMethodElement>,
    ) =
        unannotatedFunctions.mapNotNull { unannotated ->
            annotatedFunctions
                .firstOrNull {
                    if (it.jvmName != unannotated.jvmName) {
                        return@firstOrNull false
                    }
                    if (it.parameters.size != unannotated.parameters.size) {
                        return@firstOrNull false
                    }

                    // Get unannotated as a member of annotated's enclosing type before comparing
                    // in case unannotated contains type parameters that need to be resolved.
                    val annotatedEnclosingType = it.enclosingElement.type
                    val unannotatedType =
                        if (annotatedEnclosingType == null) {
                            unannotated.executableType
                        } else {
                            unannotated.asMemberOf(annotatedEnclosingType)
                        }

                    if (!it.returnType.boxed().isSameType(unannotatedType.returnType.boxed())) {
                        return@firstOrNull false
                    }
                    for (i in it.parameters.indices) {
                        if (
                            it.parameters[i].type.boxed() !=
                                unannotatedType.parameterTypes[i].boxed()
                        ) {
                            return@firstOrNull false
                        }
                    }
                    return@firstOrNull true
                }
                ?.let { matchingFunction ->
                    KotlinBoxedPrimitiveFunctionDelegate(unannotated, matchingFunction)
                }
        }
}
