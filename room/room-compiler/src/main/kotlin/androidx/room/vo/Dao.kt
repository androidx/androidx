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

package androidx.room.vo

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement

data class Dao(
    val element: XTypeElement,
    val type: XType,
    val queryFunctions: List<QueryFunction>,
    val rawQueryFunctions: List<RawQueryFunction>,
    val insertFunctions: List<InsertFunction>,
    val deleteFunctions: List<DeleteFunction>,
    val updateFunctions: List<UpdateFunction>,
    val upsertFunctions: List<UpsertFunction>,
    val transactionFunctions: List<TransactionFunction>,
    val kotlinBoxedPrimitiveFunctionDelegates: List<KotlinBoxedPrimitiveFunctionDelegate>,
    val kotlinDefaultFunctionDelegates: List<KotlinDefaultFunctionDelegate>,
    val constructorParamType: XTypeName?,
) {
    // parsed dao might have a suffix if it is used in multiple databases.
    private var suffix: String? = null

    fun setSuffix(newSuffix: String) {
        check(this.suffix == null) { "cannot set suffix twice" }
        require(newSuffix.isNotEmpty()) { "suffix can't be empty" }
        this.suffix = "_$newSuffix"
    }

    val typeName: XClassName by lazy { element.asClassName() }

    val mDeleteOrUpdateShortcutFunctions: List<DeleteOrUpdateShortcutFunction> by lazy {
        deleteFunctions + updateFunctions
    }

    val mInsertOrUpsertShortcutFunctions: List<InsertOrUpsertShortcutFunction> by lazy {
        insertFunctions + upsertFunctions
    }

    val implTypeName: XClassName by lazy {
        XClassName.get(
            typeName.packageName,
            typeName.simpleNames.joinToString("_") + (suffix ?: "") + "_Impl",
        )
    }
}
