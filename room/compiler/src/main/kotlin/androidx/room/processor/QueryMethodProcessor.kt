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
import androidx.room.solver.query.result.LiveDataQueryResultBinder
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.verifier.DatabaseVerificaitonErrors
import androidx.room.verifier.DatabaseVerifier
import androidx.room.vo.QueryMethod
import androidx.room.vo.QueryParameter
import androidx.room.vo.Warning
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

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
                        DatabaseVerificaitonErrors.cannotVerifyQuery(query.resultInfo!!.error!!))
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

        if (query.type == QueryType.DELETE) {
            context.checker.check(
                returnTypeName == TypeName.VOID || returnTypeName == TypeName.INT,
                executableElement,
                ProcessorErrors.DELETION_METHODS_MUST_RETURN_VOID_OR_INT
            )
        } else if (query.type == QueryType.INSERT) {
            context.checker.check(
                returnTypeName == TypeName.VOID || returnTypeName == TypeName.LONG,
                executableElement,
                ProcessorErrors.PREPARED_INSERT_METHOD_INVALID_RETURN_TYPE
            )
        }
        val resultBinder = delegate.findResultBinder(returnType, query)
        context.checker.check(
            resultBinder.adapter != null || query.type != QueryType.SELECT,
                executableElement,
            ProcessorErrors.cannotFindQueryResultAdapter(returnType.toString()))
        if (resultBinder is LiveDataQueryResultBinder) {
            context.checker.check(query.type == QueryType.SELECT, executableElement,
                    ProcessorErrors.LIVE_DATA_QUERY_WITHOUT_SELECT)
        }

        val inTransaction = when (query.type) {
            QueryType.SELECT -> executableElement.hasAnnotation(Transaction::class)
            else -> true
        }

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
        val queryMethod = QueryMethod(
                element = executableElement,
                query = query,
                name = executableElement.simpleName.toString(),
                returnType = returnType,
                parameters = parameters,
                inTransaction = inTransaction,
                queryResultBinder = resultBinder)

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
}
