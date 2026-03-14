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

package androidx.room3.processor

import androidx.room3.Query
import androidx.room3.SkipQueryVerification
import androidx.room3.Transaction
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.ext.isNotError
import androidx.room3.parser.ParsedQuery
import androidx.room3.parser.QueryType
import androidx.room3.parser.SqlParser
import androidx.room3.solver.prepared.binder.InstantPreparedQueryResultBinder
import androidx.room3.solver.query.result.DataClassRowAdapter
import androidx.room3.solver.query.result.InstantQueryResultBinder
import androidx.room3.verifier.DatabaseVerificationErrors
import androidx.room3.verifier.DatabaseVerifier
import androidx.room3.vo.QueryFunction
import androidx.room3.vo.QueryParameter
import androidx.room3.vo.ReadQueryFunction
import androidx.room3.vo.Warning
import androidx.room3.vo.WriteQueryFunction

class QueryFunctionProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement,
    val dbVerifier: DatabaseVerifier? = null,
) {
    val context = baseContext.fork(executableElement)

    /**
     * The processing of the function might happen in multiple steps if we decide to rewrite the
     * query after the first processing. To allow it while respecting the Context, it is implemented
     * as a sub procedure in [InternalQueryProcessor].
     */
    fun process(): QueryFunction {
        val annotation = executableElement.getAnnotation(Query::class)
        context.checker.check(
            annotation != null,
            executableElement,
            ProcessorErrors.MISSING_QUERY_ANNOTATION,
        )

        /**
         * Run the first process without reporting any errors / warnings as we might be able to fix
         * them for the developer.
         */
        val (initialResult, logs) =
            context.collectLogs {
                InternalQueryProcessor(
                        context = it,
                        executableElement = executableElement,
                        dbVerifier = dbVerifier,
                        containing = containing,
                    )
                    .processQuery(annotation?.getAsString("value"))
            }
        // check if want to swap the query for a better one
        val finalResult =
            if (initialResult is ReadQueryFunction) {
                val resultAdapter = initialResult.queryResultBinder.adapter
                val originalQuery = initialResult.query
                val finalQuery =
                    resultAdapter?.let {
                        context.queryRewriter.rewrite(originalQuery, resultAdapter)
                    } ?: originalQuery
                if (finalQuery != originalQuery) {
                    // ok parse again
                    return InternalQueryProcessor(
                            context = context,
                            executableElement = executableElement,
                            containing = containing,
                            dbVerifier = dbVerifier,
                        )
                        .processQuery(finalQuery.original)
                } else {
                    initialResult
                }
            } else {
                initialResult
            }
        if (finalResult == initialResult) {
            // if we didn't rewrite it, send all logs to the calling context.
            logs.writeTo(context)
        }
        return finalResult
    }
}

private class InternalQueryProcessor(
    val context: Context,
    val executableElement: XMethodElement,
    val containing: XType,
    val dbVerifier: DatabaseVerifier? = null,
) {
    fun processQuery(input: String?): QueryFunction {
        val delegate = FunctionProcessorDelegate.createFor(context, containing, executableElement)
        val returnType = delegate.extractReturnType()

        val returnsDeferredType = delegate.returnsDeferredType()
        val isSuspendFunction = delegate.executableElement.isSuspendFunction()
        context.checker.check(
            !isSuspendFunction || !returnsDeferredType,
            executableElement,
            ProcessorErrors.suspendReturnsDeferredType(returnType.rawType.typeName.toString()),
        )

        val query =
            if (input != null) {
                val query = SqlParser.parse(input)
                context.checker.check(
                    query.errors.isEmpty(),
                    executableElement,
                    query.errors.joinToString("\n"),
                )
                validateQuery(query)
                context.checker.check(
                    returnType.isNotError(),
                    executableElement,
                    ProcessorErrors.CANNOT_RESOLVE_RETURN_TYPE,
                    executableElement,
                )
                query
            } else {
                ParsedQuery.MISSING
            }

        context.checker.notUnbound(
            returnType,
            executableElement,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_FUNCTIONS,
        )

        val isPreparedQuery = PREPARED_TYPES.contains(query.type)
        val queryFunction =
            if (isPreparedQuery) {
                getPreparedQueryFunction(delegate, returnType, query)
            } else {
                getQueryFunction(delegate, returnType, query)
            }

        return processQueryFunction(queryFunction)
    }

    private fun processQueryFunction(queryFunction: QueryFunction): QueryFunction {
        val missing =
            queryFunction.sectionToParamMapping.filter { it.second == null }.map { it.first.text }
        if (missing.isNotEmpty()) {
            context.logger.e(
                executableElement,
                ProcessorErrors.missingParameterForBindVariable(missing),
            )
        }

        val unused =
            queryFunction.parameters
                .filterNot { param ->
                    queryFunction.sectionToParamMapping.any { it.second == param }
                }
                .map(QueryParameter::sqlName)

        if (unused.isNotEmpty()) {
            context.logger.e(
                executableElement,
                ProcessorErrors.unusedQueryFunctionParameter(unused),
            )
        }
        return queryFunction
    }

    private fun validateQuery(query: ParsedQuery) {
        val skipQueryVerification = executableElement.hasAnnotation(SkipQueryVerification::class)
        if (skipQueryVerification) {
            return
        }
        query.resultInfo = dbVerifier?.analyze(query.original)
        if (query.resultInfo?.error != null) {
            context.logger.e(
                executableElement,
                DatabaseVerificationErrors.cannotVerifyQuery(query.resultInfo!!.error!!),
            )
        }
    }

    private fun getPreparedQueryFunction(
        delegate: FunctionProcessorDelegate,
        returnType: XType,
        query: ParsedQuery,
    ): WriteQueryFunction {
        val resultBinder = delegate.findPreparedResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null,
            executableElement,
            ProcessorErrors.cannotFindPreparedQueryResultAdapter(
                returnType.asTypeName().toString(context.codeLanguage),
                query.type,
            ),
        )
        // A blocking function that does not return a deferred return type is not allowed
        // if the target platforms include non-Android targets.
        if (resultBinder is InstantPreparedQueryResultBinder && !context.isAndroidOnlyTarget()) {
            context.logger.e(
                executableElement,
                ProcessorErrors.INVALID_BLOCKING_DAO_FUNCTION_NON_ANDROID,
            )
        }

        val parameters = delegate.extractQueryParams(query)
        return WriteQueryFunction(
            element = executableElement,
            query = query,
            returnType = returnType,
            parameters = parameters,
            preparedQueryResultBinder = resultBinder,
        )
    }

    private fun getQueryFunction(
        delegate: FunctionProcessorDelegate,
        returnType: XType,
        query: ParsedQuery,
    ): QueryFunction {
        val resultBinder = delegate.findResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null,
            executableElement,
            ProcessorErrors.cannotFindQueryResultAdapter(
                returnType.asTypeName().toString(context.codeLanguage)
            ),
        )
        // A blocking function that does not return a deferred return type is not allowed
        // if the target platforms include non-Android targets.
        if (resultBinder is InstantQueryResultBinder && !context.isAndroidOnlyTarget()) {
            context.logger.e(
                executableElement,
                ProcessorErrors.INVALID_BLOCKING_DAO_FUNCTION_NON_ANDROID,
            )
        }

        val inTransaction = executableElement.hasAnnotation(Transaction::class)
        if (query.type == QueryType.SELECT && !inTransaction) {
            // put a warning if it is has relations and not annotated w/ transaction
            val hasRelations =
                resultBinder.adapter?.rowAdapters?.any { adapter ->
                    adapter is DataClassRowAdapter && adapter.relationCollectors.isNotEmpty()
                } == true
            if (hasRelations) {
                context.logger.w(
                    Warning.RELATION_QUERY_WITHOUT_TRANSACTION,
                    executableElement,
                    ProcessorErrors.TRANSACTION_MISSING_ON_RELATION,
                )
            }
        }

        query.resultInfo?.let { queryResultInfo ->
            val mappings = resultBinder.adapter?.mappings ?: return@let
            // If there are no mapping (e.g. might be a primitive return type result), then we
            // can't reasonably determine result mismatch.
            if (
                mappings.isEmpty() || mappings.none { it is DataClassRowAdapter.DataClassMapping }
            ) {
                return@let
            }
            val usedColumns = mappings.flatMap { it.usedColumns }
            val columnNames = queryResultInfo.columns.map { it.name }
            val unusedColumns = columnNames - usedColumns.toSet()
            val dataClassMappings =
                mappings.filterIsInstance<DataClassRowAdapter.DataClassMapping>()
            val dataClassUnusedProperties =
                dataClassMappings
                    .filter { it.unusedProperties.isNotEmpty() }
                    .associate {
                        it.dataClass.typeName.toString(context.codeLanguage) to it.unusedProperties
                    }
            // Warn if there are unused columns in the query result or unused properties in the
            // return type (properties with no matching column in the query result).
            if (unusedColumns.isNotEmpty() || dataClassUnusedProperties.isNotEmpty()) {
                val warningMsg =
                    ProcessorErrors.queryPropertyDataClassMismatch(
                        dataClassTypeNames =
                            dataClassMappings.map {
                                it.dataClass.typeName.toString(context.codeLanguage)
                            },
                        unusedColumns = unusedColumns,
                        allColumns = columnNames,
                        dataClassUnusedProperties = dataClassUnusedProperties,
                    )
                context.logger.w(Warning.QUERY_MISMATCH, executableElement, warningMsg)
            }

            val duplicateColumns = buildSet {
                val visitedColumns = mutableSetOf<String>()
                columnNames.forEach {
                    // When Set.add() returns false the column is already visited and a dupe.
                    if (!visitedColumns.add(it)) {
                        add(it)
                    }
                }
            }
            if (duplicateColumns.isNotEmpty()) {
                val singleMatchedUsedColumns =
                    usedColumns.groupingBy { it }.eachCount().filter { it.value == 1 }.keys
                usedColumns
                    .filter { it in duplicateColumns && it in singleMatchedUsedColumns }
                    .forEach { duplicatedUsedColumn ->
                        // Warn if there are duplicate columns in the query result that match
                        // a single data class property.
                        val warningMsg =
                            ProcessorErrors.ambiguousDuplicateColumn(
                                dataClassTypeNames =
                                    dataClassMappings.map {
                                        it.dataClass.typeName.toString(context.codeLanguage)
                                    },
                                columnName = duplicatedUsedColumn,
                            )
                        context.logger.w(
                            warning = Warning.AMBIGUOUS_COLUMN_IN_RESULT,
                            element = executableElement,
                            msg = warningMsg,
                        )
                    }
            }
        }

        val parameters = delegate.extractQueryParams(query)

        return ReadQueryFunction(
            element = executableElement,
            query = query,
            returnType = returnType,
            parameters = parameters,
            inTransaction = inTransaction,
            queryResultBinder = resultBinder,
        )
    }

    companion object {
        val PREPARED_TYPES = arrayOf(QueryType.INSERT, QueryType.DELETE, QueryType.UPDATE)
    }
}
