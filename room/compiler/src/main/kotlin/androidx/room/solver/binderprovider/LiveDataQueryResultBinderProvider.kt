/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.solver.binderprovider

import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.findTypeMirror
import androidx.room.processor.Context
import androidx.room.solver.ObservableQueryResultBinderProvider
import androidx.room.solver.query.result.LiveDataQueryResultBinder
import androidx.room.solver.query.result.QueryResultAdapter
import androidx.room.solver.query.result.QueryResultBinder
import erasure
import isAssignableFrom
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

class LiveDataQueryResultBinderProvider(context: Context) :
    ObservableQueryResultBinderProvider(context) {
    private val liveDataTypeMirror: TypeMirror? by lazy {
        context.processingEnv.findTypeMirror(LifecyclesTypeNames.LIVE_DATA)
    }

    override fun extractTypeArg(declared: DeclaredType): TypeMirror = declared.typeArguments.first()

    override fun create(
        typeArg: TypeMirror,
        resultAdapter: QueryResultAdapter?,
        tableNames: Set<String>
    ): QueryResultBinder {
        return LiveDataQueryResultBinder(
                typeArg = typeArg,
                tableNames = tableNames,
                adapter = resultAdapter)
    }

    override fun matches(declared: DeclaredType): Boolean =
            declared.typeArguments.size == 1 && isLiveData(declared)

    private fun isLiveData(declared: DeclaredType): Boolean {
        if (liveDataTypeMirror == null) {
            return false
        }
        val typeUtils = context.processingEnv.typeUtils
        val erasure = declared.erasure(typeUtils)
        return erasure.isAssignableFrom(typeUtils, liveDataTypeMirror!!)
    }
}