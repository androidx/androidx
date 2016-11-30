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

package com.android.support.room.solver.query

import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.solver.types.TypeConverter
import com.squareup.javapoet.TypeName

/**
 * Binds Collection<T> (e.g. List<T>) into String[] query args.
 */
class CollectionQueryParameterAdapter(val converter : TypeConverter) : QueryParameterAdapter(true) {
    override fun getArgCount(inputVarName: String, outputVarName : String, scope: CodeGenScope) {
        scope.builder()
                .addStatement("final $T $L = $L.size()", TypeName.INT, outputVarName, inputVarName)
    }

    override fun convert(inputVarName: String, outputVarName: String, startIndexVarName: String,
                         scope: CodeGenScope) {
        scope.builder().apply {
            val itrVar = scope.getTmpVar("_item")
            beginControlFlow("for ($T $L : $L)", converter.from.typeName(), itrVar, inputVarName)
                converter.convertForward(
                        itrVar, "$outputVarName[$startIndexVarName]", scope)
                addStatement("$L ++", startIndexVarName)
            endControlFlow()
        }
    }
}
