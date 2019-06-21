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
import androidx.room.ext.hasAnnotation
import androidx.room.ext.toAnnotationBox
import androidx.room.ext.typeName
import androidx.room.parser.ParsedQuery
import androidx.room.parser.QueryType
import androidx.room.parser.SqlParser
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.verifier.DatabaseVerificationErrors
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.WriteQueryMethod
import androidx.room.vo.QueryMethod
import androidx.room.vo.QueryParameter
import androidx.room.vo.ReadQueryMethod
import androidx.room.vo.Warning
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class QueryMethodProcessor(
    baseContext: Context,
    val containing: DeclaredType,
    val executableElement: ExecutableElement,
    val dbVerifier: DatabaseVerifier? = null
) {
    val context = baseContext.fork(executableElement)

    fun process(): QueryMethod {
        val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)
        val returnType = delegate.extractReturnType()

        val annotation = executableElement.toAnnotationBox(Query::class)?.value
        context.checker.check(annotation != null, executableElement,
                ProcessorErrors.MISSING_QUERY_ANNOTATION)

        val query = if (annotation != null) {
            val query = SqlParser.parse(annotation.value)
            context.checker.check(query.errors.isEmpty(), executableElement,
                    query.errors.joinToString("\n"))
            if (!executableElement.hasAnnotation(SkipQueryVerification::class)) {
                query.resultInfo = dbVerifier?.analyze(query.original)
            }
            if (query.resultInfo?.error != null) {
                context.logger.e(executableElement,
                        DatabaseVerificationErrors.cannotVerifyQuery(query.resultInfo!!.error!!))
            }

            context.checker.check(returnType.kind != TypeKind.ERROR,
                    executableElement, ProcessorErrors.CANNOT_RESOLVE_RETURN_TYPE,
                    executableElement)
            query
        } else {
            ParsedQuery.MISSING
        }

        val returnTypeName = returnType.typeName()
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)

        val isPreparedQuery = PREPARED_TYPES.contains(query.type)
        val queryMethod = if (isPreparedQuery) {
            getPreparedQueryMethod(delegate, returnType, query)
        } else {
            getQueryMethod(delegate, returnType, query)
        }

        val missing = queryMethod.sectionToParamMapping
                .filter { it.second == null }
                .map { it.first.text }
        if (missing.isNotEmpty()) {
            context.logger.e(executableElement,
                    ProcessorErrors.missingParameterForBindVariable(missing))
        }

        val unused = queryMethod.parameters.filterNot { param ->
            queryMethod.sectionToParamMapping.any { it.second == param }
        }.map(QueryParameter::sqlName)

        if (unused.isNotEmpty()) {
            context.logger.e(executableElement, ProcessorErrors.unusedQueryMethodParameter(unused))
        }
        return queryMethod
    }

    private fun getPreparedQueryMethod(
        delegate: MethodProcessorDelegate,
        returnType: TypeMirror,
        query: ParsedQuery
    ): WriteQueryMethod {
        val resultBinder = delegate.findPreparedResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null,
            executableElement,
            ProcessorErrors.cannotFindPreparedQueryResultAdapter(returnType.toString(), query.type))

        val parameters = delegate.extractQueryParams()

        return WriteQueryMethod(
            element = executableElement,
            query = query,
            name = executableElement.simpleName.toString(),
            returnType = returnType,
            parameters = parameters,
            preparedQueryResultBinder = resultBinder)
    }

    private fun getQueryMethod(
        delegate: MethodProcessorDelegate,
        returnType: TypeMirror,
        query: ParsedQuery
    ): QueryMethod {
        val resultBinder = delegate.findResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null,
            executableElement,
            ProcessorErrors.cannotFindQueryResultAdapter(returnType.toString()))

        val inTransaction = executableElement.hasAnnotation(Transaction::class)
        if (query.type == QueryType.SELECT && !inTransaction) {
            // put a warning if it is has relations and not annotated w/ transaction
            resultBinder.adapter?.rowAdapter?.let { rowAdapter ->
                if (rowAdapter is PojoRowAdapter && rowAdapter.relationCollectors.isNotEmpty()) {
                    context.logger.w(Warning.RELATION_QUERY_WITHOUT_TRANSACTION,
                        executableElement, ProcessorErrors.TRANSACTION_MISSING_ON_RELATION)
                }
            }
        }

        val parameters = delegate.extractQueryParams()

        return ReadQueryMethod(
            element = executableElement,
            query = query,
            name = executableElement.simpleName.toString(),
            returnType = returnType,
            parameters = parameters,
            inTransaction = inTransaction,
            queryResultBinder = resultBinder)
    }

    companion object {
        val PREPARED_TYPES = arrayOf(QueryType.INSERT, QueryType.DELETE, QueryType.UPDATE)
    }
}
