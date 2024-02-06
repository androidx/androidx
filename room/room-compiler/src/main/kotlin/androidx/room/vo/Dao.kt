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
    val queryMethods: List<QueryMethod>,
    val rawQueryMethods: List<RawQueryMethod>,
    val insertMethods: List<InsertMethod>,
    val deleteMethods: List<DeleteMethod>,
    val updateMethods: List<UpdateMethod>,
    val upsertMethods: List<UpsertMethod>,
    val transactionMethods: List<TransactionMethod>,
    val kotlinBoxedPrimitiveMethodDelegates: List<KotlinBoxedPrimitiveMethodDelegate>,
    val kotlinDefaultMethodDelegates: List<KotlinDefaultMethodDelegate>,
    val constructorParamType: XTypeName?
) {
    // parsed dao might have a suffix if it is used in multiple databases.
    private var suffix: String? = null

    fun setSuffix(newSuffix: String) {
        check(this.suffix == null) { "cannot set suffix twice" }
        require(newSuffix.isNotEmpty()) { "suffix can't be empty" }
        this.suffix = "_$newSuffix"
    }

    val typeName: XClassName by lazy { element.asClassName() }

    val deleteOrUpdateShortcutMethods: List<DeleteOrUpdateShortcutMethod> by lazy {
        deleteMethods + updateMethods
    }

    val insertOrUpsertShortcutMethods: List<InsertOrUpsertShortcutMethod> by lazy {
        insertMethods + upsertMethods
    }

    val implTypeName: XClassName by lazy {
        XClassName.get(
            typeName.packageName,
            typeName.simpleNames.joinToString("_") + (suffix ?: "") + "_Impl"
        )
    }
}
