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

import androidx.room.Query
import androidx.room.SkipQueryVerification
import androidx.room.Transaction
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.ext.isNotError
import androidx.room.parser.ParsedQuery
import androidx.room.parser.QueryType
import androidx.room.parser.SqlParser
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.verifier.DatabaseVerificationErrors
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.QueryMethod
import androidx.room.vo.QueryParameter
import androidx.room.vo.ReadQueryMethod
import androidx.room.vo.Warning
import androidx.room.vo.WriteQueryMethod

class QueryMethodProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement,
    val dbVerifier: DatabaseVerifier? = null
) {
    val context = baseContext.fork(executableElement)

    /**
     * The processing of the method might happen in multiple steps if we decide to rewrite the
     * query after the first processing. To allow it while respecting the Context, it is
     * implemented as a sub procedure in [InternalQueryProcessor].
     */
    fun process(): QueryMethod {
        val annotation = executableElement.getAnnotation(Query::class)?.value
        context.checker.check(
            annotation != null, executableElement,
            ProcessorErrors.MISSING_QUERY_ANNOTATION
        )

        /**
         * Run the first process without reporting any errors / warnings as we might be able to
         * fix them for the developer.
         */
        val (initialResult, logs) = context.collectLogs {
            InternalQueryProcessor(
                context = it,
                executableElement = executableElement,
                dbVerifier = dbVerifier,
                containing = containing
            ).processQuery(annotation?.value)
        }
        // check if want to swap the query for a better one
        val finalResult = if (initialResult is ReadQueryMethod) {
            val resultAdapter = initialResult.queryResultBinder.adapter
            val originalQuery = initialResult.query
            val finalQuery = resultAdapter?.let {
                context.queryRewriter.rewrite(originalQuery, resultAdapter)
            } ?: originalQuery
            if (finalQuery != originalQuery) {
                // ok parse again
                return InternalQueryProcessor(
                    context = context,
                    executableElement = executableElement,
                    dbVerifier = dbVerifier,
                    containing = containing
                ).processQuery(finalQuery.original)
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
    val dbVerifier: DatabaseVerifier? = null
) {
    fun processQuery(input: String?): QueryMethod {
        val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)
        val returnType = delegate.extractReturnType()

        val query = if (input != null) {
            val query = SqlParser.parse(input)
            context.checker.check(
                query.errors.isEmpty(), executableElement,
                query.errors.joinToString("\n")
            )
            validateQuery(query)
            context.checker.check(
                returnType.isNotError(),
                executableElement, ProcessorErrors.CANNOT_RESOLVE_RETURN_TYPE,
                executableElement
            )
            query
        } else {
            ParsedQuery.MISSING
        }

        val returnTypeName = returnType.typeName
        context.checker.notUnbound(
            returnTypeName, executableElement,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS
        )

        val isPreparedQuery = PREPARED_TYPES.contains(query.type)
        val queryMethod = if (isPreparedQuery) {
            getPreparedQueryMethod(delegate, returnType, query)
        } else {
            getQueryMethod(delegate, returnType, query)
        }

        return processQueryMethod(queryMethod)
    }

    private fun processQueryMethod(queryMethod: QueryMethod): QueryMethod {
        val missing = queryMethod.sectionToParamMapping
            .filter { it.second == null }
            .map { it.first.text }
        if (missing.isNotEmpty()) {
            context.logger.e(
                executableElement,
                ProcessorErrors.missingParameterForBindVariable(missing)
            )
        }

        val unused = queryMethod.parameters.filterNot { param ->
            queryMethod.sectionToParamMapping.any { it.second == param }
        }.map(QueryParameter::sqlName)

        if (unused.isNotEmpty()) {
            context.logger.e(executableElement, ProcessorErrors.unusedQueryMethodParameter(unused))
        }
        return queryMethod
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
                DatabaseVerificationErrors.cannotVerifyQuery(query.resultInfo!!.error!!)
            )
        }
    }

    private fun getPreparedQueryMethod(
        delegate: MethodProcessorDelegate,
        returnType: XType,
        query: ParsedQuery
    ): WriteQueryMethod {
        val resultBinder = delegate.findPreparedResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null,
            executableElement,
            ProcessorErrors.cannotFindPreparedQueryResultAdapter(returnType.typeName, query.type)
        )

        val parameters = delegate.extractQueryParams(query)
        return WriteQueryMethod(
            element = executableElement,
            query = query,
            name = executableElement.name,
            returnType = returnType,
            parameters = parameters,
            preparedQueryResultBinder = resultBinder
        )
    }

    private fun getQueryMethod(
        delegate: MethodProcessorDelegate,
        returnType: XType,
        query: ParsedQuery
    ): QueryMethod {
        val resultBinder = delegate.findResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null,
            executableElement,
            ProcessorErrors.cannotFindQueryResultAdapter(returnType.typeName)
        )

        val inTransaction = executableElement.hasAnnotation(Transaction::class)
        if (query.type == QueryType.SELECT && !inTransaction) {
            // put a warning if it is has relations and not annotated w/ transaction
            val hasRelations =
                resultBinder.adapter?.rowAdapters?.any { adapter ->
                    adapter is PojoRowAdapter && adapter.relationCollectors.isNotEmpty()
                } == true
            if (hasRelations) {
                context.logger.w(
                    Warning.RELATION_QUERY_WITHOUT_TRANSACTION,
                    executableElement, ProcessorErrors.TRANSACTION_MISSING_ON_RELATION
                )
            }
        }

        query.resultInfo?.let { queryResultInfo ->
            val mappings = resultBinder.adapter?.mappings ?: return@let
            // If there are no mapping (e.g. might be a primitive return type result), then we
            // can't reasonable determine cursor mismatch.
            if (mappings.isEmpty()) {
                return@let
            }
            val usedColumns = mappings.flatMap { mapping ->
                mapping.matchedFields.map { it.columnName }
            }
            val columnNames = queryResultInfo.columns.map { it.name }
            val unusedColumns = columnNames - usedColumns
            val pojoUnusedFields = mappings
                .filter { it.unusedFields.isNotEmpty() }
                .associate { it.pojo.typeName to it.unusedFields }
            if (unusedColumns.isNotEmpty() || pojoUnusedFields.isNotEmpty()) {
                val warningMsg = ProcessorErrors.cursorPojoMismatch(
                    pojoTypeNames = mappings.map { it.pojo.typeName },
                    unusedColumns = unusedColumns,
                    allColumns = columnNames,
                    pojoUnusedFields = pojoUnusedFields,
                )
                context.logger.w(Warning.CURSOR_MISMATCH, executableElement, warningMsg)
            }
        }

        val parameters = delegate.extractQueryParams(query)

        return ReadQueryMethod(
            element = executableElement,
            query = query,
            name = executableElement.name,
            returnType = returnType,
            parameters = parameters,
            inTransaction = inTransaction,
            queryResultBinder = resultBinder
        )
    }

    companion object {
        val PREPARED_TYPES = arrayOf(QueryType.INSERT, QueryType.DELETE, QueryType.UPDATE)
    }
}
