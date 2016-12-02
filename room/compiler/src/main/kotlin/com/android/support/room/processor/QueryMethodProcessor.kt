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

import com.android.support.room.Query
import com.android.support.room.parser.ParsedQuery
import com.android.support.room.parser.SqlParser
import com.android.support.room.vo.QueryMethod
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

class QueryMethodProcessor(val context: Context) {
    val parameterParser = QueryParameterProcessor(context)
    fun parse(containing: DeclaredType, executableElement: ExecutableElement): QueryMethod {
        val asMember = context.processingEnv.typeUtils.asMemberOf(containing, executableElement)
        val executableType = MoreTypes.asExecutable(asMember)

        val annotation = MoreElements.getAnnotationMirror(executableElement,
                Query::class.java).orNull()
        context.checker.check(annotation != null, executableElement,
                ProcessorErrors.MISSING_QUERY_ANNOTATION)

        val query = if (annotation != null) {
            val query = SqlParser.parse(
                    AnnotationMirrors.getAnnotationValue(annotation, "value").value.toString())
            context.checker.check(query.errors.isEmpty(), executableElement,
                    query.errors.joinToString("\n"))
            context.checker.check(executableType.returnType.kind != TypeKind.ERROR,
                    executableElement, ProcessorErrors.CANNOT_RESOLVE_RETURN_TYPE,
                    executableElement)
            query
        } else {
            ParsedQuery.MISSING
        }

        val returnTypeName = TypeName.get(executableType.returnType)
        context.checker.notUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)

        val resultAdapter = context.typeAdapterStore
                .findQueryResultAdapter(executableType.returnType)
        context.checker.check(resultAdapter != null, executableElement,
                ProcessorErrors.CANNOT_FIND_QUERY_RESULT_ADAPTER)
        val queryMethod = QueryMethod(
                element = executableElement,
                query = query,
                name = executableElement.simpleName.toString(),
                returnType = executableType.returnType,
                parameters = executableElement.parameters
                        .map { parameterParser.parse(containing, it) },
                resultAdapter = resultAdapter)

        val missing = queryMethod.sectionToParamMapping
                .filter { it.second == null }
                .map { it.first.text }
        if (missing.isNotEmpty()) {
            context.logger.e(executableElement,
                    ProcessorErrors.missingParameterForBindVariable(missing))
        }

        val unused = queryMethod.parameters.filterNot { param ->
            queryMethod.sectionToParamMapping.any { it.second == param }
        }.map { it.name }
        if (unused.isNotEmpty()) {
            context.logger.w(executableElement,
                    ProcessorErrors.unusedQueryMethodParameter(unused))
        }
        return queryMethod
    }
}
