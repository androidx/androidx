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

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XVariableElement
import androidx.room.parser.Section
import androidx.room.vo.QueryParameter

class QueryParameterProcessor(
    baseContext: Context,
    val containing: XType,
    val element: XVariableElement,
    private val sqlName: String,
    private val bindVarSection: Section.BindVar?
) {
    val context = baseContext.fork(element)
    fun process(): QueryParameter {
        val asMember = element.asMemberOf(containing)
        val parameterAdapter = context.typeAdapterStore.findQueryParameterAdapter(
            typeMirror = asMember,
            isMultipleParameter = bindVarSection?.isMultiple ?: false
        )
        context.checker.check(
            parameterAdapter != null, element,
            ProcessorErrors.CANNOT_BIND_QUERY_PARAMETER_INTO_STMT
        )

        val name = element.name
        context.checker.check(
            !name.startsWith("_"), element,
            ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE
        )
        return QueryParameter(
            name = name,
            sqlName = sqlName,
            type = asMember,
            queryParamAdapter = parameterAdapter
        )
    }
}
