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

import com.android.support.room.vo.Parameter
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.TypeName
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType

class ParameterParser(val context: Context) {
    fun parse(containing: DeclaredType, element: VariableElement): Parameter {
        val asMember = MoreTypes.asMemberOf(context.processingEnv.typeUtils, containing, element)
        val parameterAdapter = context.typeAdapterStore.findQueryParameterAdapter(asMember)
        context.checker.check(parameterAdapter != null, element,
                ProcessorErrors.CANNOT_CONVERT_QUERY_PARAMETER_TO_STRING)

        val name = element.simpleName.toString()
        context.checker.check(!name.startsWith("_"), element,
                ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE)

        return Parameter(name, asMember, parameterAdapter)
    }
}
