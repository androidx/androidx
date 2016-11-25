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
import com.android.support.room.parser.SqlParser
import com.android.support.room.preconditions.Checks
import com.android.support.room.vo.QueryMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

class QueryMethodProcessor(val roundEnv: RoundEnvironment,
                           val processingEnvironment: ProcessingEnvironment) {
    val parameterParser = ParameterParser(roundEnv, processingEnvironment)
    fun parse(containing: DeclaredType, executableElement: ExecutableElement): QueryMethod {
        val asMember = processingEnvironment.typeUtils.asMemberOf(containing, executableElement)
        val executableType = MoreTypes.asExecutable(asMember)
        Checks.check(MoreElements.isAnnotationPresent(executableElement, Query::class.java),
                executableElement, ProcessorErrors.MISSING_QUERY_ANNOTATION)
        val annotation = executableElement.getAnnotation(Query::class.java)
        val query = SqlParser.parse(annotation.value)
        Checks.check(query.errors.isEmpty(), executableElement, query.errors.joinToString("\n"))
        Checks.check(executableType.returnType.kind != TypeKind.ERROR, executableElement,
                ProcessorErrors.CANNOT_RESOLVE_RETURN_TYPE, executableElement)

        val returnTypeName = TypeName.get(executableType.returnType)

        Checks.assertNotUnbound(returnTypeName, executableElement,
                ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)
        return QueryMethod(
                element = executableElement,
                query = query,
                name = executableElement.simpleName.toString(),
                returnType = returnTypeName,
                parameters = executableElement.parameters
                        .map { parameterParser.parse(containing, it) })
    }
}
