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

package android.arch.persistence.room.solver.query.result

import android.arch.persistence.room.ext.L
import android.arch.persistence.room.ext.PagingTypeNames
import android.arch.persistence.room.ext.RoomTypeNames
import android.arch.persistence.room.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class LiveLazyListQueryResultBinder(
        val countedDataSourceQueryResultBinder: CountedDataSourceQueryResultBinder)
    : QueryResultBinder(countedDataSourceQueryResultBinder.listAdapter) {
    @Suppress("HasPlatformType")
    val typeName = countedDataSourceQueryResultBinder.itemTypeName
    override fun convertAndReturn(roomSQLiteQueryVar: String, dbField: FieldSpec,
                                  scope: CodeGenScope) {
        scope.builder().apply {
            val lazyListProvider = TypeSpec
                    .anonymousClassBuilder("").apply {
                superclass(ParameterizedTypeName.get(PagingTypeNames.LIVE_LAZY_LIST_PROVIDER,
                        typeName))
                addMethod(createCreateDataSourceMethod(roomSQLiteQueryVar, dbField, scope))
            }.build()
            addStatement("return $L", lazyListProvider)
        }
    }

    private fun createCreateDataSourceMethod(roomSQLiteQueryVar: String,
                                             dbField: FieldSpec,
                                             scope: CodeGenScope): MethodSpec
            = MethodSpec.methodBuilder("createDataSource").apply {
        addAnnotation(Override::class.java)
        addModifiers(Modifier.PROTECTED)
        returns(countedDataSourceQueryResultBinder.typeName)
        val countedBinderScope = scope.fork()
        countedDataSourceQueryResultBinder.convertAndReturn(roomSQLiteQueryVar, dbField,
                countedBinderScope)
        addCode(countedBinderScope.builder().build())
    }.build()
}
