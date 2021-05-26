/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.isEntityElement
import androidx.room.parser.SqlParser
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XVariableElement
import androidx.room.processor.ProcessorErrors.RAW_QUERY_STRING_PARAMETER_REMOVED
import androidx.room.vo.RawQueryMethod

class RawQueryMethodProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): RawQueryMethod {
        val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)
        val returnType = delegate.extractReturnType()

        context.checker.check(
            executableElement.hasAnnotation(RawQuery::class), executableElement,
            ProcessorErrors.MISSING_RAWQUERY_ANNOTATION
        )

        val returnTypeName = returnType.typeName
        context.checker.notUnbound(
            returnTypeName, executableElement,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS
        )
        val observedTableNames = processObservedTables()
        val query = SqlParser.rawQueryForTables(observedTableNames)
        // build the query but don't calculate result info since we just guessed it.
        val resultBinder = delegate.findResultBinder(returnType, query)
        val runtimeQueryParam = findRuntimeQueryParameter(delegate.extractParams())
        val inTransaction = executableElement.hasAnnotation(Transaction::class)
        val rawQueryMethod = RawQueryMethod(
            element = executableElement,
            name = executableElement.name,
            observedTableNames = observedTableNames,
            returnType = returnType,
            runtimeQueryParam = runtimeQueryParam,
            inTransaction = inTransaction,
            queryResultBinder = resultBinder
        )
        // TODO: Lift this restriction, to allow for INSERT, UPDATE and DELETE raw statements.
        context.checker.check(
            rawQueryMethod.returnsValue, executableElement,
            ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE
        )
        return rawQueryMethod
    }

    private fun processObservedTables(): Set<String> {
        val annotation = executableElement.getAnnotation(RawQuery::class)
        return annotation?.getAsTypeList("observedEntities")
            ?.mapNotNull {
                it.typeElement.also { typeElement ->
                    if (typeElement == null) {
                        context.logger.e(
                            executableElement,
                            ProcessorErrors.NOT_ENTITY_OR_VIEW
                        )
                    }
                }
            }
            ?.flatMap {
                if (it.isEntityElement()) {
                    val entity = EntityProcessor(
                        context = context,
                        element = it
                    ).process()
                    arrayListOf(entity.tableName)
                } else {
                    val pojo = PojoProcessor.createFor(
                        context = context,
                        element = it,
                        bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                        parent = null
                    ).process()
                    val tableNames = pojo.accessedTableNames()
                    // if it is empty, report error as it does not make sense
                    if (tableNames.isEmpty()) {
                        context.logger.e(
                            executableElement,
                            ProcessorErrors.rawQueryBadEntity(it.type.typeName)
                        )
                    }
                    tableNames
                }
            }?.toSet() ?: emptySet()
    }

    private fun findRuntimeQueryParameter(
        extractParams: List<XVariableElement>
    ): RawQueryMethod.RuntimeQueryParameter? {
        if (extractParams.size == 1 && !executableElement.isVarArgs()) {
            val param = extractParams.first().asMemberOf(containing)
            val processingEnv = context.processingEnv
            if (param.nullability == XNullability.NULLABLE) {
                context.logger.e(
                    element = extractParams.first(),
                    msg = ProcessorErrors.parameterCannotBeNullable(
                        parameterName = extractParams.first().name
                    )
                )
            }
            // use nullable type to catch bad nullability. Because it is non-null by default in
            // KSP, assignability will fail and we'll print a generic error instead of a specific
            // one
            val supportQueryType = processingEnv.requireType(SupportDbTypeNames.QUERY)
            val isSupportSql = supportQueryType.isAssignableFrom(param)
            if (isSupportSql) {
                return RawQueryMethod.RuntimeQueryParameter(
                    paramName = extractParams[0].name,
                    type = supportQueryType.typeName
                )
            }
            val stringType = processingEnv.requireType("java.lang.String")
            val isString = stringType.isAssignableFrom(param)
            if (isString) {
                // special error since this was initially allowed but removed in 1.1 beta1
                context.logger.e(executableElement, RAW_QUERY_STRING_PARAMETER_REMOVED)
                return null
            }
        }
        context.logger.e(executableElement, ProcessorErrors.RAW_QUERY_BAD_PARAMS)
        return null
    }
}